package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;

public class TestMovimentacaoProdutoController extends TestContextoApi {

	private Long idPedido;
	private String numeroPedido;
	private Long idProduto;
	private String nomeProduto;
	private Long idMovimentacao;
	private Long idMovimentacaoSemPedido;

	@BeforeAll
	void criarProdutoEPedido() throws Exception {
		Long categoria = id(post("/api/categoria/salvar", mapa("nome", "Categoria Mov " + sufixo)).andExpect(status().isOk()));
		nomeProduto = "Monitor " + sufixo;
		idProduto = id(post("/api/produto/salvar", mapa("nome", nomeProduto, "imagem", "img", "preco", 900.0, "estoque", 5,
				"estoqueMinimo", 1, "sku", "SKU-M-" + sufixo, "unidadeMedida", "UNIDADE", "categoria", ref(categoria)))
				.andExpect(status().isOk()));

		UsuarioTeste cliente = criarUsuario("Cliente", false);
		numeroPedido = "PED-MOV-" + sufixo;
		idPedido = id(post("/api/pedido/salvar", mapa("numeroPedido", numeroPedido, "statusPedido", "FATURADO",
				"formaPagamento", "BOLETO", "tipoPedido", "VENDA", "dataPedido", LocalDate.now().toString(),
				"vendedor", ref(admin.usuarioId()), "cliente", ref(cliente.usuarioId()))).andExpect(status().isOk()));
	}

	private Map<String, Object> movimentacaoJson(Long id, String tipo, double quantidade, Long pedido) {
		return mapa("id", id, "quantidade", quantidade, "valor", 900.0 * quantidade, "tipoMovimentacaoProduto", tipo,
				"produto", ref(idProduto), "pedido", pedido == null ? null : ref(pedido));
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idMovimentacao = id(post("/api/movimentacaoProduto/salvar", movimentacaoJson(null, "SAIDA_VENDA", 1, idPedido))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoMovimentacaoProduto").value("SAIDA_VENDA"))
				.andExpect(jsonPath("$.dataMovimento").value(LocalDate.now().toString()))
				.andExpect(jsonPath("$.produtoNome").value(nomeProduto))
				.andExpect(jsonPath("$.pedidoId").value(idPedido))
				.andExpect(jsonPath("$.numeroPedido").value(numeroPedido))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));

		/* Pedido é opcional (perda, extravio, descarte) */
		idMovimentacaoSemPedido = id(post("/api/movimentacaoProduto/salvar", movimentacaoJson(null, "SAIDA_PERDA", 1, null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoMovimentacaoProduto").value("SAIDA_PERDA"))
				.andExpect(jsonPath("$.pedidoId").value(nullValue())));
	}

	@Test
	@Order(2)
	public void testSalvarSemProduto() throws Exception {
		Map<String, Object> json = movimentacaoJson(null, "ENTRADA_COMPRA", 1, null);
		json.remove("produto");
		post("/api/movimentacaoProduto/salvar", json).andExpect(status().isBadRequest());
	}

	@Test
	@Order(3)
	public void testAtualizar() throws Exception {
		post("/api/movimentacaoProduto/atualizar", movimentacaoJson(idMovimentacao, "SAIDA_VENDA", 2, idPedido))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idMovimentacao))
				.andExpect(jsonPath("$.quantidade").value(2.0))
				.andExpect(jsonPath("$.numeroPedido").value(numeroPedido));
	}

	@Test
	@Order(4)
	public void testListar() throws Exception {
		get("/api/movimentacaoProduto/listar").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		get("/api/movimentacaoProduto/listarTodos").andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(idMovimentacao.intValue())));
		get("/api/movimentacaoProduto/listarPorPedido/{idPedido}", idPedido).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	@Order(5)
	public void testBuscaPorNome() throws Exception {
		get("/api/movimentacaoProduto/buscaPorNome/{nome}", nomeProduto).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
		get("/api/movimentacaoProduto/buscaPorNomePorPedido/{idPedido}/{nome}", idPedido, nomeProduto)
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idMovimentacao));
	}

	@Test
	@Order(6)
	public void testExistePorNome() throws Exception {
		get("/api/movimentacaoProduto/existePorNome/{nome}", nomeProduto).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(true));
		get("/api/movimentacaoProduto/existePorNomeDiferenteId/{id}/{nome}", idMovimentacao, nomeProduto)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/movimentacaoProduto/existePorNomePorPedido/{idPedido}/{nome}", idPedido, nomeProduto)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/movimentacaoProduto/existePorNomeDiferenteIdPorPedido/{id}/{idPedido}/{nome}", idMovimentacao, idPedido,
				nomeProduto).andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(7)
	public void testBuscarPorId() throws Exception {
		get("/api/movimentacaoProduto/buscarPorId/{id}", idMovimentacaoSemPedido)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoMovimentacaoProduto").value("SAIDA_PERDA"));
		get("/api/movimentacaoProduto/buscarPorId/0").andExpect(status().isNotFound());
		get("/api/movimentacaoProduto/existePorId/{id}", idMovimentacao).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(8)
	public void testBuscarPorIdsTotalEPaginado() throws Exception {
		post("/api/movimentacaoProduto/buscarPorIds", ids(idMovimentacao, idMovimentacaoSemPedido))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		get("/api/movimentacaoProduto/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(2));
		get("/api/movimentacaoProduto/paginado?page=0&size=10&sort=id&direction=desc")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(idMovimentacaoSemPedido));
	}

	@Test
	@Order(9)
	public void testDeletarTodosPorIds() throws Exception {
		delete("/api/movimentacaoProduto/deletarTodosPorIds", ids(idMovimentacaoSemPedido)).andExpect(status().isNoContent());
		get("/api/movimentacaoProduto/existePorId/{id}", idMovimentacaoSemPedido).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(10)
	public void testDeletarPorPedido() throws Exception {
		delete("/api/movimentacaoProduto/deletarPorPedido/" + idMovimentacao + "/" + idPedido)
				.andExpect(status().isOk())
				.andExpect(content().string("Movimentação de produto deletada com sucesso."));
		delete("/api/movimentacaoProduto/deletarPorPedido/" + idMovimentacao + "/" + idPedido)
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(11)
	public void testDeletar() throws Exception {
		Long id = id(post("/api/movimentacaoProduto/salvar", movimentacaoJson(null, "ENTRADA_COMPRA", 3, null))
				.andExpect(status().isOk()));
		delete("/api/movimentacaoProduto/deletar/" + id)
				.andExpect(status().isOk())
				.andExpect(content().string("Movimentação de produto deletada com sucesso."));
	}

	@Test
	@Order(12)
	public void testDeletarTodos() throws Exception {
		post("/api/movimentacaoProduto/salvar", movimentacaoJson(null, "ENTRADA_AJUSTE", 1, null)).andExpect(status().isOk());
		delete("/api/movimentacaoProduto/deletarTodos").andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
		delete("/api/movimentacaoProduto/deletarTodos").andExpect(status().isBadRequest());
	}


	@Test
	@Order(100)
	public void testListarPaginado() throws Exception {
		conferirPaginado("/api/movimentacaoProduto/paginado");
	}

}
