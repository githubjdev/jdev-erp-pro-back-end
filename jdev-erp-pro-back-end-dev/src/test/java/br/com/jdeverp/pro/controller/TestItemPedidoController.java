package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;

public class TestItemPedidoController extends TestContextoApi {

	private Long idPedido;
	private String numeroPedido;
	private Long idProdutoA;
	private Long idProdutoB;
	private String nomeProdutoA;
	private String nomeProdutoB;
	private Long idItem;
	private Long idItem2;

	@BeforeAll
	void criarPedidoEProdutos() throws Exception {
		Long categoria = id(post("/api/categoria/salvar", mapa("nome", "Categoria Item " + sufixo)).andExpect(status().isOk()));
		nomeProdutoA = "Teclado " + sufixo;
		nomeProdutoB = "Mouse " + sufixo;
		idProdutoA = id(post("/api/produto/salvar", produtoJson(nomeProdutoA, "SKU-A-" + sufixo, categoria)).andExpect(status().isOk()));
		idProdutoB = id(post("/api/produto/salvar", produtoJson(nomeProdutoB, "SKU-B-" + sufixo, categoria)).andExpect(status().isOk()));

		UsuarioTeste cliente = criarUsuario("Cliente", false);
		numeroPedido = "PED-ITEM-" + sufixo;
		idPedido = id(post("/api/pedido/salvar", mapa("numeroPedido", numeroPedido, "statusPedido", "AGUARDANDO_PAGAMENTO",
				"formaPagamento", "PIX", "tipoPedido", "VENDA", "dataPedido", LocalDate.now().toString(),
				"vendedor", ref(admin.usuarioId()), "cliente", ref(cliente.usuarioId()))).andExpect(status().isOk()));
	}

	private Map<String, Object> produtoJson(String nome, String sku, Long categoria) {
		return mapa("nome", nome, "imagem", "img", "preco", 50.0, "estoque", 10, "estoqueMinimo", 1, "sku", sku,
				"unidadeMedida", "UNIDADE", "categoria", ref(categoria));
	}

	private Map<String, Object> itemJson(Long id, Long produto, double quantidade) {
		return mapa("id", id, "quantidade", quantidade, "subTotal", 50.0 * quantidade, "desconto", 0,
				"total", 50.0 * quantidade, "produto", ref(produto), "pedido", ref(idPedido));
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idItem = id(post("/api/itemPedido/salvar", itemJson(null, idProdutoA, 2))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantidade").value(2.0))
				.andExpect(jsonPath("$.total").value(100.0))
				.andExpect(jsonPath("$.produtoId").value(idProdutoA))
				.andExpect(jsonPath("$.produtoNome").value(nomeProdutoA))
				.andExpect(jsonPath("$.pedidoId").value(idPedido))
				.andExpect(jsonPath("$.numeroPedido").value(numeroPedido))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));

		idItem2 = id(post("/api/itemPedido/salvar", itemJson(null, idProdutoB, 1)).andExpect(status().isOk()));
	}

	@Test
	@Order(2)
	public void testSalvarProdutoRepetidoNoPedido() throws Exception {
		post("/api/itemPedido/salvar", itemJson(null, idProdutoA, 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("O produto já foi incluído neste pedido."));
	}

	@Test
	@Order(3)
	public void testAtualizar() throws Exception {
		post("/api/itemPedido/atualizar", itemJson(idItem, idProdutoA, 3))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idItem))
				.andExpect(jsonPath("$.quantidade").value(3.0))
				.andExpect(jsonPath("$.produtoNome").value(nomeProdutoA));
	}

	@Test
	@Order(4)
	public void testListar() throws Exception {
		get("/api/itemPedido/listar/{idPedido}", idPedido).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		get("/api/itemPedido/listarTodos").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(idItem.intValue())));
		get("/api/itemPedido/listarPorPedido/{idPedido}", idPedido).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	@Order(5)
	public void testBuscaPorNome() throws Exception {
		get("/api/itemPedido/buscaPorNome/{idPedido}/{nome}", idPedido, nomeProdutoB)
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idItem2));
		get("/api/itemPedido/buscaPorNomePorPedido/{idPedido}/{nome}", idPedido, nomeProdutoA)
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idItem));
	}

	@Test
	@Order(6)
	public void testExistePorNome() throws Exception {
		get("/api/itemPedido/existePorNome/{idPedido}/{nome}", idPedido, nomeProdutoA)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/itemPedido/existePorNomeDiferenteId/{id}/{idPedido}/{nome}", idItem, idPedido, nomeProdutoA)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
		get("/api/itemPedido/existePorNomePorPedido/{idPedido}/{nome}", idPedido, nomeProdutoB)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/itemPedido/existePorNomeDiferenteIdPorPedido/{id}/{idPedido}/{nome}", idItem, idPedido, nomeProdutoB)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(7)
	public void testBuscarPorId() throws Exception {
		get("/api/itemPedido/buscarPorId/{id}", idItem)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroPedido").value(numeroPedido));
		get("/api/itemPedido/buscarPorId/0").andExpect(status().isNotFound());
		get("/api/itemPedido/existePorId/{id}", idItem).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(8)
	public void testBuscarPorIds() throws Exception {
		post("/api/itemPedido/buscarPorIds", ids(idItem, idItem2))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	@Order(9)
	public void testTotalEPaginado() throws Exception {
		get("/api/itemPedido/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(2));
		get("/api/itemPedido/paginado?page=0&size=10&sort=id&direction=asc")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].produtoNome").value(nomeProdutoA));
	}

	@Test
	@Order(10)
	public void testDeletarTodosPorIds() throws Exception {
		delete("/api/itemPedido/deletarTodosPorIds", ids(idItem2)).andExpect(status().isNoContent());
		get("/api/itemPedido/existePorId/{id}", idItem2).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(11)
	public void testDeletar() throws Exception {
		delete("/api/itemPedido/deletar/" + idItem + "/" + idPedido)
				.andExpect(status().isOk())
				.andExpect(content().string("Item do pedido deletado com sucesso."));
		delete("/api/itemPedido/deletar/" + idItem + "/" + idPedido).andExpect(status().isBadRequest());
	}

	@Test
	@Order(12)
	public void testDeletarPorPedido() throws Exception {
		Long item = id(post("/api/itemPedido/salvar", itemJson(null, idProdutoA, 1)).andExpect(status().isOk()));
		delete("/api/itemPedido/deletarPorPedido/" + item + "/" + idPedido)
				.andExpect(status().isOk())
				.andExpect(content().string("Item do pedido deletado com sucesso."));
	}

	@Test
	@Order(13)
	public void testDeletarTodos() throws Exception {
		post("/api/itemPedido/salvar", itemJson(null, idProdutoB, 1)).andExpect(status().isOk());
		delete("/api/itemPedido/deletarTodos").andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
		delete("/api/itemPedido/deletarTodos").andExpect(status().isBadRequest());
	}


	@Test
	@Order(100)
	public void testListarPaginado() throws Exception {
		conferirPaginado("/api/itemPedido/paginado");
	}

}
