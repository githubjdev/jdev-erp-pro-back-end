package br.com.jdeverp.pro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import br.com.jdeverp.pro.contexto.TestContextoApi;

/**
 * Exclusão em lote e multiempresa: o ROLE_ADMIN é global, mas o
 * {@code deletarTodos} e o {@code deletarTodosPorIds} só alcançam a empresa
 * logada. As duas empresas de teste recebem os mesmos cadastros, e depois de
 * cada exclusão feita pelo administrador da primeira, os registros de todas as
 * outras empresas (inclusive os dados reais do banco dev) precisam continuar
 * iguais.
 */
public class TestIsolamentoExclusaoEmLote extends TestContextoApi {

	/** Módulo com exclusão em lote: caminho na API, tabela e a mensagem do service para id não encontrado. */
	record Modulo(String api, String tabela, String naoEncontrados) {
		@Override
		public String toString() {
			return api;
		}
	}

	private static final Modulo MENSAGEM = new Modulo("mensagem", "mensagem", "Mensagens não encontradas ou já deletadas: ");
	private static final Modulo MOVIMENTACAO = new Modulo("movimentacaoProduto", "movimentacao_produto",
			"Movimentações de produto não encontradas ou já deletadas: ");
	private static final Modulo ITEM_PEDIDO = new Modulo("itemPedido", "item_pedido", "Itens de pedido não encontrados ou já deletados: ");
	private static final Modulo PEDIDO = new Modulo("pedido", "pedido", "Pedidos não encontrados ou já deletados: ");
	private static final Modulo PRODUTO = new Modulo("produto", "produto", "Produtos não encontrados ou já deletados: ");
	private static final Modulo CLIENTE_FUNCIONARIO = new Modulo("clienteFuncionario", "cliente_funcionario",
			"Clientes/funcionários não encontrados ou já deletados: ");
	private static final Modulo PESSOA = new Modulo("pessoa", "pessoa", "Pessoas não encontradas ou já deletadas: ");

	/** Tabelas com empresa_id. */
	private static final List<String> TABELAS = List.of("pessoa", "cliente_funcionario", "usuario", "categoria", "produto",
			"pedido", "item_pedido", "movimentacao_produto", "chamado", "mensagem");

	private Long outraEmpresa;
	private Map<Modulo, Long> daEmpresa;
	private Map<Modulo, Long> daOutraEmpresa;

	@BeforeAll
	void cadastrarNasDuasEmpresas() throws Exception {
		daEmpresa = cadastrar(token, admin, empresaId);

		outraEmpresa = criarEmpresa();
		UsuarioTeste adminDaOutra = criarUsuario("AdminOutra", true, outraEmpresa);
		daOutraEmpresa = cadastrar(logar(adminDaOutra.login(), SENHA), adminDaOutra, outraEmpresa);
	}

	/** Um registro de cada módulo, pela API, com o administrador da empresa. */
	private Map<Modulo, Long> cadastrar(String tokenAdmin, UsuarioTeste administrador, Long idEmpresa) throws Exception {
		String marca = idEmpresa + "-" + sufixo;
		UsuarioTeste cliente = criarUsuario("Cliente", false, idEmpresa);
		Map<Modulo, Long> ids = new LinkedHashMap<>();

		Long categoria = salvar(tokenAdmin, "categoria", mapa("nome", "Categoria Lote " + marca));
		Long produto = salvar(tokenAdmin, PRODUTO.api(), mapa("nome", "Produto Lote " + marca, "imagem", "img", "preco", 10.0,
				"estoque", 10, "estoqueMinimo", 1, "sku", "SKU-LOTE-" + marca, "unidadeMedida", "UNIDADE", "categoria", ref(categoria)));
		Long pedido = salvar(tokenAdmin, PEDIDO.api(), mapa("numeroPedido", "PED-LOTE-" + marca, "statusPedido", "AGUARDANDO_PAGAMENTO",
				"formaPagamento", "PIX", "tipoPedido", "VENDA", "dataPedido", LocalDate.now().toString(),
				"vendedor", ref(administrador.usuarioId()), "cliente", ref(cliente.usuarioId())));
		ids.put(PRODUTO, produto);
		ids.put(PEDIDO, pedido);
		ids.put(ITEM_PEDIDO, salvar(tokenAdmin, ITEM_PEDIDO.api(), mapa("quantidade", 1.0, "subTotal", 10.0, "desconto", 0,
				"total", 10.0, "produto", ref(produto), "pedido", ref(pedido))));
		ids.put(MOVIMENTACAO, salvar(tokenAdmin, MOVIMENTACAO.api(), mapa("quantidade", 1.0, "valor", 10.0,
				"tipoMovimentacaoProduto", "SAIDA_PERDA", "produto", ref(produto), "pedido", null)));

		Long chamado = salvar(tokenAdmin, "chamado", mapa("titulo", "Chamado Lote " + marca, "descricao", "Chamado da exclusão em lote",
				"tipoChamado", "SUPORTE", "prioridadeChamado", "MEDIA", "statusChamado", "EM_ATENDIMENTO",
				"atendente", ref(administrador.usuarioId()), "cliente", ref(cliente.usuarioId())));
		ids.put(MENSAGEM, salvar(tokenAdmin, MENSAGEM.api(), mapa("conteudo", "Mensagem Lote " + marca, "arquivo", "anexo.pdf",
				"lida", false, "chamado", ref(chamado), "atendente", ref(administrador.usuarioId()), "cliente", ref(cliente.usuarioId()))));

		ids.put(PESSOA, salvar(tokenAdmin, PESSOA.api(), pessoaJson("Pessoa Lote")));
		Long pessoaDoCadastro = salvar(tokenAdmin, PESSOA.api(), pessoaJson("Pessoa Cadastro Lote"));
		ids.put(CLIENTE_FUNCIONARIO, salvar(tokenAdmin, CLIENTE_FUNCIONARIO.api(),
				mapa("tipoClienteFuncionario", "CLIENTE", "pessoa", ref(pessoaDoCadastro), "usuario", null)));
		return ids;
	}

	private Long salvar(String tokenAdmin, String modulo, Map<String, Object> corpo) throws Exception {
		return id(comToken(tokenAdmin, MockMvcRequestBuilders.post("/api/" + modulo + "/salvar"), corpo)
				.andExpect(status().isOk()));
	}

	/** Registros por tabela fora da empresa logada: tudo o que a exclusão em lote dela não pode tocar. */
	private Map<String, Long> registrosForaDaEmpresa() {
		Map<String, Long> registros = new LinkedHashMap<>();
		for (String tabela : TABELAS) {
			registros.put(tabela, jdbcTemplate.queryForObject("select count(*) from " + tabela + " where empresa_id <> ?",
					Long.class, empresaId));
		}
		registros.put("role_usuario", jdbcTemplate.queryForObject(
				"select count(*) from role_usuario r join usuario u on u.id = r.usuario_id where u.empresa_id <> ?",
				Long.class, empresaId));
		return registros;
	}

	private long registrosDaEmpresa(String tabela) {
		return jdbcTemplate.queryForObject("select count(*) from " + tabela + " where empresa_id = ?", Long.class, empresaId);
	}

	private boolean existe(Modulo modulo, Long id) {
		return jdbcTemplate.queryForObject("select count(*) from " + modulo.tabela() + " where id = ?", Long.class, id) == 1;
	}

	private void conferirOutraEmpresaIntacta() {
		daOutraEmpresa.forEach((modulo, id) -> assertTrue(existe(modulo, id), modulo + " da outra empresa"));
	}

	Stream<Modulo> modulos() {
		return Stream.of(MENSAGEM, MOVIMENTACAO, ITEM_PEDIDO, PEDIDO, PRODUTO, CLIENTE_FUNCIONARIO, PESSOA);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("modulos")
	@Order(1)
	public void testDeletarTodosPorIdsDeOutraEmpresa(Modulo modulo) throws Exception {
		Long idDaOutra = daOutraEmpresa.get(modulo);
		Map<String, Long> antes = registrosForaDaEmpresa();

		delete("/api/" + modulo.api() + "/deletarTodosPorIds", ids(idDaOutra))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(modulo.naoEncontrados() + "[" + idDaOutra + "]"));

		assertEquals(antes, registrosForaDaEmpresa());
		conferirOutraEmpresaIntacta();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("modulos")
	@Order(2)
	public void testDeletarTodosPorIdsMisturandoEmpresas(Modulo modulo) throws Exception {
		Long idDaEmpresa = daEmpresa.get(modulo);
		Long idDaOutra = daOutraEmpresa.get(modulo);
		Map<String, Long> antes = registrosForaDaEmpresa();

		delete("/api/" + modulo.api() + "/deletarTodosPorIds", ids(idDaEmpresa, idDaOutra))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(modulo.naoEncontrados() + "[" + idDaOutra + "]"));

		/* Tudo ou nada: com um id de outra empresa na lista, nem o da própria empresa é apagado */
		assertTrue(existe(modulo, idDaEmpresa));
		assertEquals(antes, registrosForaDaEmpresa());
		conferirOutraEmpresaIntacta();
	}

	@Test
	@Order(3)
	public void testDeletarTodosSoApagaAEmpresaLogada() throws Exception {
		Map<String, Long> antes = registrosForaDaEmpresa();

		/* Dos filhos para os pais, para o banco não recusar pela FK; o total devolvido é só o da empresa logada */
		for (Modulo modulo : List.of(MENSAGEM, MOVIMENTACAO, ITEM_PEDIDO, PEDIDO, PRODUTO)) {
			long daEmpresaAntes = registrosDaEmpresa(modulo.tabela());
			delete("/api/" + modulo.api() + "/deletarTodos")
					.andExpect(status().isOk())
					.andExpect(jsonPath("$").value(daEmpresaAntes));

			assertEquals(0L, registrosDaEmpresa(modulo.tabela()), modulo.api());
			assertEquals(antes, registrosForaDaEmpresa(), modulo.api());
		}

		/* Ainda em uso na empresa logada (FK): o banco recusa e nada é apagado, em nenhuma empresa */
		for (String url : List.of("/api/clienteFuncionario/deletarTodos", "/api/pessoa/deletarTodos", "/api/usuario/deletar-todos")) {
			delete(url).andExpect(status().isBadRequest());
			assertEquals(antes, registrosForaDaEmpresa(), url);
		}

		conferirOutraEmpresaIntacta();
	}

}
