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

public class TestPedidoController extends TestContextoApi {

	private UsuarioTeste cliente;
	private Long idPedido;
	private Long idPedido2;
	private String numero;

	@BeforeAll
	void criarCliente() {
		cliente = criarUsuario("Cliente", false);
		numero = "PED-" + sufixo;
	}

	private Map<String, Object> pedidoJson(Long id, String numero, String status) {
		return mapa("id", id, "numeroPedido", numero, "statusPedido", status, "formaPagamento", "CARTAO_CREDITO",
				"tipoPedido", "VENDA", "dataPedido", LocalDate.now().toString(), "subTotal", 100.0, "desconto", 0,
				"frete", 10.5, "taxas", 0, "total", 110.5, "observacao", "Pedido de teste",
				"vendedor", ref(admin.usuarioId()), "cliente", ref(cliente.usuarioId()));
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idPedido = id(post("/api/pedido/salvar", pedidoJson(null, numero, "AGUARDANDO_PAGAMENTO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroPedido").value(numero))
				.andExpect(jsonPath("$.statusPedido").value("AGUARDANDO_PAGAMENTO"))
				.andExpect(jsonPath("$.formaPagamento").value("CARTAO_CREDITO"))
				.andExpect(jsonPath("$.tipoPedido").value("VENDA"))
				.andExpect(jsonPath("$.total").value(110.5))
				.andExpect(jsonPath("$.vendedorId").value(admin.usuarioId()))
				.andExpect(jsonPath("$.vendedorNome").value(admin.nome()))
				.andExpect(jsonPath("$.clienteNome").value(cliente.nome()))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));

		idPedido2 = id(post("/api/pedido/salvar", pedidoJson(null, numero + "-2", "AGUARDANDO_PAGAMENTO"))
				.andExpect(status().isOk()));
	}

	@Test
	@Order(2)
	public void testSalvarNumeroDuplicado() throws Exception {
		post("/api/pedido/salvar", pedidoJson(null, numero, "AGUARDANDO_PAGAMENTO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe um pedido com o mesmo número para a empresa logada."));
	}

	@Test
	@Order(3)
	public void testSalvarComEnumInvalido() throws Exception {
		post("/api/pedido/salvar", pedidoJson(null, numero + "-x", "STATUS_QUE_NAO_EXISTE"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(4)
	public void testAtualizar() throws Exception {
		post("/api/pedido/atualizar", pedidoJson(idPedido, numero, "PAGAMENTO_APROVADO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idPedido))
				.andExpect(jsonPath("$.statusPedido").value("PAGAMENTO_APROVADO"))
				.andExpect(jsonPath("$.vendedorNome").value(admin.nome()));
	}

	@Test
	@Order(5)
	public void testListar() throws Exception {
		get("/api/pedido/listar").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		get("/api/pedido/listarTodos").andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(idPedido.intValue())));
	}

	@Test
	@Order(6)
	public void testBuscaPorNumeroPedido() throws Exception {
		get("/api/pedido/buscaPorNumeroPedido/{n}", numero + "-2")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(idPedido2));
	}

	@Test
	@Order(7)
	public void testExistePorNumeroPedido() throws Exception {
		get("/api/pedido/existePorNumeroPedido/{n}", numero).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/pedido/existePorNumeroPedidoDiferenteId/{id}/{n}", idPedido2, numero + "-2")
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(8)
	public void testBuscarPorId() throws Exception {
		get("/api/pedido/buscarPorId/{id}", idPedido)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.numeroPedido").value(numero))
				.andExpect(jsonPath("$.clienteId").value(cliente.usuarioId()));

		get("/api/pedido/buscarPorId/0").andExpect(status().isNotFound());
	}

	@Test
	@Order(9)
	public void testExistePorId() throws Exception {
		get("/api/pedido/existePorId/{id}", idPedido).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(10)
	public void testBuscarPorIds() throws Exception {
		post("/api/pedido/buscarPorIds", ids(idPedido, idPedido2))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].vendedorNome").value(admin.nome()));
	}

	@Test
	@Order(11)
	public void testTotalEPaginado() throws Exception {
		get("/api/pedido/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(2));
		get("/api/pedido/paginado?page=0&size=1&sort=id&direction=desc")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id").value(idPedido2));
	}

	@Test
	@Order(12)
	public void testDeletarTodosPorIds() throws Exception {
		delete("/api/pedido/deletarTodosPorIds", ids(idPedido2)).andExpect(status().isNoContent());
		get("/api/pedido/existePorId/{id}", idPedido2).andExpect(jsonPath("$").value(false));

		delete("/api/pedido/deletarTodosPorIds", ids(idPedido2))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Pedidos não encontrados ou já deletados: [" + idPedido2 + "]"));
	}

	@Test
	@Order(13)
	public void testDeletar() throws Exception {
		delete("/api/pedido/deletar/" + idPedido)
				.andExpect(status().isOk())
				.andExpect(content().string("Pedido deletado com sucesso."));
		delete("/api/pedido/deletar/" + idPedido).andExpect(status().isBadRequest());
	}

	@Test
	@Order(14)
	public void testDeletarTodos() throws Exception {
		post("/api/pedido/salvar", pedidoJson(null, numero + "-3", "AGUARDANDO_PAGAMENTO")).andExpect(status().isOk());

		delete("/api/pedido/deletarTodos").andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
		get("/api/pedido/total").andExpect(jsonPath("$").value(0));
		delete("/api/pedido/deletarTodos").andExpect(status().isBadRequest());
	}

}
