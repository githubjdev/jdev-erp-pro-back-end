package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import br.com.jdeverp.pro.contexto.TestContextoApi;
import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.model.Usuario;

/** Fluxo novo do pedido: itens na criação, cálculo no servidor, máquina de status e baixa/estorno de estoque. */
public class TestPedidoItensEstoqueController extends TestContextoApi {

	private Long idCategoria;
	private Long idProdutoA; // preço 50, estoque 10
	private Long idProdutoB; // preço 20, estoque 2
	private UsuarioTeste cliente;
	private Long idPedido;

	@BeforeAll
	void preparar() throws Exception {
		idCategoria = id(post("/api/categoria/salvar", mapa("nome", "Cat Estoque " + sufixo)).andExpect(status().isOk()));
		idProdutoA = id(post("/api/produto/salvar", produtoJson("Produto A " + sufixo, "SKU-A-" + sufixo, 50.0, 10)).andExpect(status().isOk()));
		idProdutoB = id(post("/api/produto/salvar", produtoJson("Produto B " + sufixo, "SKU-B-" + sufixo, 20.0, 2)).andExpect(status().isOk()));
		cliente = criarUsuario("Cliente", false);

		/* O estoque só entra por movimentação: inventário inicial lançado pela API */
		post("/api/movimentacaoProduto/salvar", mapa("quantidade", 10, "valor", 25, "tipoMovimentacaoProduto", "ENTRADA_INVENTARIO",
				"produto", ref(idProdutoA))).andExpect(status().isOk());
		post("/api/movimentacaoProduto/salvar", mapa("quantidade", 2, "valor", 10, "tipoMovimentacaoProduto", "ENTRADA_INVENTARIO",
				"produto", ref(idProdutoB))).andExpect(status().isOk());
	}

	private Map<String, Object> produtoJson(String nome, String sku, double preco, int estoque) {
		return mapa("nome", nome, "imagem", "img", "preco", preco, "precoCusto", preco / 2, "estoque", estoque, "estoqueMinimo", 1, "sku", sku,
				"unidadeMedida", "UNIDADE", "categoria", ref(idCategoria));
	}

	private Map<String, Object> pedidoJson(Long id, String numero, List<Map<String, Object>> itens) {
		return mapa("id", id, "numeroPedido", numero, "formaPagamento", "PIX", "tipoPedido", "VENDA",
				"dataPedido", LocalDate.now().toString(), "frete", 10, "vendedorId", admin.usuarioId(),
				"clienteId", cliente.usuarioId(), "itens", itens);
	}

	private Map<String, Object> item(Long produtoId, double quantidade, double desconto) {
		return mapa("produtoId", produtoId, "quantidade", quantidade, "desconto", desconto);
	}

	private double estoque(Long produtoId) throws Exception {
		return numero(get("/api/produto/buscarPorId/{id}", produtoId).andExpect(status().isOk()), "$.estoque").doubleValue();
	}

	@Test
	@Order(1)
	public void testSalvarComItensCalculaNoServidor() throws Exception {
		/* A: 3 x 50 - 10 = 140; B: 1 x 20 = 20; subtotal 160; total = 160 + frete 10 = 170 */
		idPedido = id(post("/api/pedido/salvarComItens", pedidoJson(null, "PED-IE-" + sufixo,
				List.of(item(idProdutoA, 3, 10), item(idProdutoB, 1, 0))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusPedido").value("AGUARDANDO_PAGAMENTO"))
				.andExpect(jsonPath("$.subTotal").value(160.0))
				.andExpect(jsonPath("$.total").value(170.0))
				.andExpect(jsonPath("$.estoqueBaixado").value(false))
				.andExpect(jsonPath("$.itens", hasSize(2)))
				.andExpect(jsonPath("$.itens[0].precoUnitario").value(50.0))
				.andExpect(jsonPath("$.itens[0].total").value(140.0)));

		/* Criar o pedido não mexe no estoque */
		assertEquals(10.0, estoque(idProdutoA));
	}

	@Test
	@Order(2)
	public void testSalvarComItensValidacoes() throws Exception {
		post("/api/pedido/salvarComItens", pedidoJson(null, "PED-V1-" + sufixo, List.of()))
				.andExpect(status().isBadRequest());
		post("/api/pedido/salvarComItens", pedidoJson(null, "PED-V2-" + sufixo,
				List.of(item(idProdutoA, 1, 0), item(idProdutoA, 2, 0))))
				.andExpect(status().isBadRequest());
		post("/api/pedido/salvarComItens", pedidoJson(null, "PED-V3-" + sufixo, List.of(item(idProdutoA, 1, 999))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Desconto do item não pode ser maior que o subtotal do item."));
	}

	@Test
	@Order(3)
	public void testAtualizarComItens() throws Exception {
		post("/api/pedido/atualizarComItens", pedidoJson(idPedido, "PED-IE-" + sufixo, List.of(item(idProdutoA, 3, 10))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.itens", hasSize(1)))
				.andExpect(jsonPath("$.total").value(150.0));
		/* Volta para os dois itens */
		post("/api/pedido/atualizarComItens", pedidoJson(idPedido, "PED-IE-" + sufixo,
				List.of(item(idProdutoA, 3, 10), item(idProdutoB, 1, 0))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(170.0));
	}

	@Test
	@Order(4)
	public void testStatusInvalidoENaoPermitidoParaQuemNaoTemPerfil() throws Exception {
		/* AGUARDANDO_PAGAMENTO não vai direto para ENTREGUE */
		patchStatus(idPedido, "ENTREGUE", token).andExpect(status().isBadRequest());

		/* Usuário só com ROLE_USER não altera status */
		UsuarioTeste comum = criarUsuario("Comum", false);
		patchStatus(idPedido, "PAGAMENTO_APROVADO", logar(comum.login(), SENHA)).andExpect(status().isForbidden());
	}

	@Test
	@Order(5)
	public void testAprovarPagamentoBaixaEstoque() throws Exception {
		/* Vendedor pode alterar status */
		UsuarioTeste vendedor = criarUsuario("Vendedor", false);
		Usuario usuario = usuarioRepository.buscarPorId(vendedor.usuarioId(), empresaId).orElseThrow();
		RoleUsuario ru = new RoleUsuario();
		ru.setAcesso(role("ROLE_VENDEDOR"));
		ru.setUsuario(usuario);
		roleUsuarioRepository.save(ru);

		patchStatus(idPedido, "PAGAMENTO_APROVADO", logar(vendedor.login(), SENHA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusPedido").value("PAGAMENTO_APROVADO"))
				.andExpect(jsonPath("$.estoqueBaixado").value(true))
				.andExpect(jsonPath("$.dataPagamento").value(LocalDate.now().toString()));

		assertEquals(7.0, estoque(idProdutoA));
		assertEquals(1.0, estoque(idProdutoB));
		assertEquals(2, jdbcTemplate.queryForObject(
				"select count(*) from movimentacao_produto where pedido_id = ? and tipo_movimentacao_produto = 'SAIDA_VENDA'",
				Integer.class, idPedido));
	}

	@Test
	@Order(6)
	public void testPedidoAprovadoNaoPodeSerEditado() throws Exception {
		post("/api/pedido/atualizarComItens", pedidoJson(idPedido, "PED-IE-" + sufixo, List.of(item(idProdutoA, 1, 0))))
				.andExpect(status().isBadRequest());
		post("/api/itemPedido/salvar", mapa("quantidade", 1, "produto", ref(idProdutoB), "pedido", ref(idPedido)))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(7)
	public void testCancelarEstornaEstoque() throws Exception {
		patchStatus(idPedido, "CANCELADO", token)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.statusPedido").value("CANCELADO"))
				.andExpect(jsonPath("$.estoqueBaixado").value(false))
				.andExpect(jsonPath("$.dataCancelamento").value(LocalDate.now().toString()));

		assertEquals(10.0, estoque(idProdutoA));
		assertEquals(2.0, estoque(idProdutoB));

		/* Cancelado é final */
		patchStatus(idPedido, "PAGAMENTO_APROVADO", token).andExpect(status().isBadRequest());
	}

	@Test
	@Order(8)
	public void testEstoqueInsuficienteNaoBaixaNada() throws Exception {
		Long pedido = id(post("/api/pedido/salvarComItens", pedidoJson(null, "PED-INS-" + sufixo,
				List.of(item(idProdutoA, 2, 0), item(idProdutoB, 5, 0)))).andExpect(status().isOk()));

		patchStatus(pedido, "PAGAMENTO_APROVADO", token)
				.andExpect(status().isBadRequest());

		/* Transação desfeita: nenhum produto foi baixado e o status continua o mesmo */
		assertEquals(10.0, estoque(idProdutoA));
		assertEquals(2.0, estoque(idProdutoB));
		get("/api/pedido/buscarPorId/{id}", pedido).andExpect(jsonPath("$.statusPedido").value("AGUARDANDO_PAGAMENTO"));
	}

	@Test
	@Order(9)
	public void testVendaDaUltimaUnidadeZeraEstoque() throws Exception {
		Long pedido = id(post("/api/pedido/salvarComItens", pedidoJson(null, "PED-ULT-" + sufixo,
				List.of(item(idProdutoB, 2, 0)))).andExpect(status().isOk()));
		patchStatus(pedido, "PAGAMENTO_APROVADO", token).andExpect(status().isOk());
		assertEquals(0.0, estoque(idProdutoB));

		get("/api/produto/estoqueBaixo").andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", org.hamcrest.Matchers.hasItem(idProdutoB.intValue())));
	}

	@Test
	@Order(10)
	public void testItemPedidoAntigoCalculaTotais() throws Exception {
		Long pedido = id(post("/api/pedido/salvarComItens", pedidoJson(null, "PED-LEG-" + sufixo,
				List.of(item(idProdutoA, 1, 0)))).andExpect(status().isOk()));

		/* Endpoint antigo: totais enviados pelo cliente são ignorados; o pedido é recalculado */
		post("/api/itemPedido/salvar", mapa("quantidade", 2, "subTotal", 1, "total", 1, "desconto", 0,
				"produto", ref(idProdutoB), "pedido", ref(pedido)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.precoUnitario").value(20.0))
				.andExpect(jsonPath("$.total").value(40.0));
		get("/api/pedido/buscarPorId/{id}", pedido).andExpect(jsonPath("$.subTotal").value(90.0))
				.andExpect(jsonPath("$.total").value(100.0));
	}


	private Map<String, Object> pedidoTipo(String numero, String tipo, List<Map<String, Object>> itens) {
		Map<String, Object> json = pedidoJson(null, numero, itens);
		json.put("tipoPedido", tipo);
		return json;
	}

	private Map<String, Object> movimentacaoJson(String tipo, double quantidade, Long produtoId) {
		return mapa("quantidade", quantidade, "valor", 10, "tipoMovimentacaoProduto", tipo,
				"dataMovimento", LocalDate.now().toString(), "produto", ref(produtoId));
	}

	@Test
	@Order(11)
	public void testPedidoDeCompraEntraNoEstoqueEEstornaNoCancelamento() throws Exception {
		double antes = estoque(idProdutoB);
		Long compra = id(post("/api/pedido/salvarComItens", pedidoTipo("PED-COM-" + sufixo, "COMPRA",
				List.of(item(idProdutoB, 5, 0)))).andExpect(status().isOk()));

		/* Item de compra usa o preço de custo (10,00), não o de venda (20,00) */
		get("/api/pedido/buscarPorId/{id}", compra).andExpect(jsonPath("$.subTotal").value(50.0))
				.andExpect(jsonPath("$.total").value(60.0));

		patchStatus(compra, "PAGAMENTO_APROVADO", token).andExpect(status().isOk());
		assertEquals(antes + 5, estoque(idProdutoB));
		assertEquals(1, jdbcTemplate.queryForObject(
				"select count(*) from movimentacao_produto where pedido_id = ? and tipo_movimentacao_produto = 'ENTRADA_COMPRA'",
				Integer.class, compra));

		patchStatus(compra, "CANCELADO", token).andExpect(status().isOk());
		assertEquals(antes, estoque(idProdutoB));
		assertEquals(1, jdbcTemplate.queryForObject(
				"select count(*) from movimentacao_produto where pedido_id = ? and tipo_movimentacao_produto = 'SAIDA_DEVOLUCAO_FORNECEDOR'",
				Integer.class, compra));
	}

	@Test
	@Order(12)
	public void testPedidoDeBrindeSaiDoEstoque() throws Exception {
		double antes = estoque(idProdutoA);
		Long brinde = id(post("/api/pedido/salvarComItens", pedidoTipo("PED-BRI-" + sufixo, "BRINDE",
				List.of(item(idProdutoA, 2, 0)))).andExpect(status().isOk()));

		patchStatus(brinde, "PAGAMENTO_APROVADO", token).andExpect(status().isOk());
		assertEquals(antes - 2, estoque(idProdutoA));
		assertEquals(1, jdbcTemplate.queryForObject(
				"select count(*) from movimentacao_produto where pedido_id = ? and tipo_movimentacao_produto = 'SAIDA_BRINDE'",
				Integer.class, brinde));
	}

	@Test
	@Order(13)
	public void testMovimentacaoManualDeCompraEBrinde() throws Exception {
		double antes = estoque(idProdutoA);

		post("/api/movimentacaoProduto/salvar", movimentacaoJson("ENTRADA_COMPRA", 4, idProdutoA)).andExpect(status().isOk());
		assertEquals(antes + 4, estoque(idProdutoA));

		post("/api/movimentacaoProduto/salvar", movimentacaoJson("SAIDA_BRINDE", 1, idProdutoA)).andExpect(status().isOk());
		assertEquals(antes + 3, estoque(idProdutoA));

		/* Saída maior que o estoque é recusada e nada muda */
		post("/api/movimentacaoProduto/salvar", movimentacaoJson("SAIDA_BRINDE", 1000, idProdutoA))
				.andExpect(status().isBadRequest());
		assertEquals(antes + 3, estoque(idProdutoA));
	}

	@Test
	@Order(14)
	public void testExclusaoEmLoteSoDeProdutoSemUso() throws Exception {
		Long livre = id(post("/api/produto/salvar", produtoJson("Produto Livre " + sufixo, "SKU-L-" + sufixo, 5.0, 3))
				.andExpect(status().isOk()));

		/* Produto A tem item de pedido e movimentação: nada é apagado */
		delete("/api/produto/deletarTodosPorIds", List.of(idProdutoA, livre)).andExpect(status().isBadRequest());
		get("/api/produto/existePorId/{id}", livre).andExpect(jsonPath("$").value(true));

		/* Só o produto sem uso sai em lote */
		delete("/api/produto/deletarTodosPorIds", List.of(livre)).andExpect(status().isNoContent());
		get("/api/produto/existePorId/{id}", livre).andExpect(jsonPath("$").value(false));

		/* Sobraram só produtos em uso: deletarTodos não apaga nenhum */
		delete("/api/produto/deletarTodos").andExpect(status().isBadRequest());
		get("/api/produto/existePorId/{id}", idProdutoA).andExpect(jsonPath("$").value(true));
	}


	@Test
	@Order(15)
	public void testCompraComProdutoSemCustoETrocaDeTipo() throws Exception {
		Map<String, Object> semCusto = produtoJson("Sem Custo " + sufixo, "SKU-SC-" + sufixo, 30.0, 5);
		semCusto.put("precoCusto", 0);
		Long idSemCusto = id(post("/api/produto/salvar", semCusto).andExpect(status().isOk()));

		post("/api/pedido/salvarComItens", pedidoTipo("PED-SC-" + sufixo, "COMPRA", List.of(item(idSemCusto, 1, 0))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("O produto Sem Custo " + sufixo
						+ " não tem preço de custo. Informe o custo no cadastro do produto para usá-lo em pedido de compra."));

		/* Pedido com itens não troca de tipo pelo endpoint antigo (o preço depende do tipo) */
		Long venda = id(post("/api/pedido/salvarComItens", pedidoTipo("PED-TT-" + sufixo, "VENDA",
				List.of(item(idProdutoA, 1, 0)))).andExpect(status().isOk()));
		Map<String, Object> antigo = mapa("id", venda, "numeroPedido", "PED-TT-" + sufixo, "formaPagamento", "PIX",
				"tipoPedido", "COMPRA", "dataPedido", LocalDate.now().toString(), "vendedor", ref(admin.usuarioId()),
				"cliente", ref(cliente.usuarioId()));
		post("/api/pedido/atualizar", antigo).andExpect(status().isBadRequest());
	}


	@Test
	@Order(16)
	public void testEstoqueNaoMudaPeloCadastroDoProduto() throws Exception {
		/* Produto novo: o estoque enviado é ignorado, começa em 0 */
		Long novo = id(post("/api/produto/salvar", produtoJson("Cadastro Estoque " + sufixo, "SKU-CE-" + sufixo, 40.0, 50))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estoque").value(0.0)));
		assertEquals(0.0, estoque(novo));

		/* Só a movimentação lançada altera o estoque */
		post("/api/movimentacaoProduto/salvar", movimentacaoJson("ENTRADA_INVENTARIO", 7, novo)).andExpect(status().isOk());
		assertEquals(7.0, estoque(novo));

		/* Atualizar o produto com outro estoque: o valor enviado é ignorado, o do banco é mantido */
		Map<String, Object> edicao = produtoJson("Cadastro Estoque Editado " + sufixo, "SKU-CE-" + sufixo, 41.0, 999);
		edicao.put("id", novo);
		post("/api/produto/atualizar", edicao).andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Cadastro Estoque Editado " + sufixo))
				.andExpect(jsonPath("$.preco").value(41.0))
				.andExpect(jsonPath("$.estoque").value(7.0));
		assertEquals(7.0, estoque(novo));
	}

	private org.springframework.test.web.servlet.ResultActions patchStatus(Long id, String status, String tokenUsuario)
			throws Exception {
		return comToken(tokenUsuario, MockMvcRequestBuilders.patch("/api/pedido/alterarStatus/{id}/{s}", id, status), null);
	}
}
