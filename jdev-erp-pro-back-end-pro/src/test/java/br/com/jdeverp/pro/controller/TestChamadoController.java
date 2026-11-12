package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
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

public class TestChamadoController extends TestContextoApi {

	private UsuarioTeste cliente;
	private Long idChamado;
	private String titulo;

	@BeforeAll
	void criarCliente() {
		cliente = criarUsuario("Cliente", false);
		titulo = "Chamado API " + sufixo;
	}

	private Map<String, Object> chamadoJson(Long id, String titulo, String status, Long clienteId) {
		return mapa("id", id, "titulo", titulo, "descricao", "Descrição do chamado de teste",
				"tipoChamado", "DUVIDA", "prioridadeChamado", "ALTA", "statusChamado", status,
				"atendente", ref(admin.usuarioId()), "cliente", ref(clienteId));
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idChamado = id(post("/api/chamado/salvar", chamadoJson(null, titulo, "ABERTO", cliente.usuarioId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value(titulo))
				.andExpect(jsonPath("$.tipoChamado").value("DUVIDA"))
				.andExpect(jsonPath("$.prioridadeChamado").value("ALTA"))
				.andExpect(jsonPath("$.statusChamado").value("ABERTO"))
				.andExpect(jsonPath("$.dataAbertura").value(LocalDate.now().toString()))
				.andExpect(jsonPath("$.abertoUserId").value(admin.usuarioId()))
				.andExpect(jsonPath("$.abertoUserNome").value(admin.nome()))
				.andExpect(jsonPath("$.atendenteNome").value(admin.nome()))
				.andExpect(jsonPath("$.clienteId").value(cliente.usuarioId()))
				.andExpect(jsonPath("$.clienteNome").value(cliente.nome()))
				.andExpect(jsonPath("$.fechadoUserId").value(nullValue()))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));
	}

	@Test
	@Order(2)
	public void testSalvarTituloDuplicado() throws Exception {
		post("/api/chamado/salvar", chamadoJson(null, titulo, "ABERTO", cliente.usuarioId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe um chamado com o mesmo título para a empresa logada."));
	}

	@Test
	@Order(3)
	public void testSalvarClienteDeOutraEmpresa() throws Exception {
		UsuarioTeste deOutraEmpresa = criarUsuario("Externo", false, criarEmpresa());
		post("/api/chamado/salvar", chamadoJson(null, "Outro " + titulo, "ABERTO", deOutraEmpresa.usuarioId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Usuário relacionado não encontrado para a empresa logada."));
	}

	@Test
	@Order(4)
	public void testAtualizar() throws Exception {
		Map<String, Object> json = chamadoJson(idChamado, titulo, "FECHADO", cliente.usuarioId());
		json.put("fechadoUser", ref(admin.usuarioId()));
		json.put("dataFechamento", LocalDate.now().toString());

		post("/api/chamado/atualizar", json)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idChamado))
				.andExpect(jsonPath("$.statusChamado").value("FECHADO"))
				.andExpect(jsonPath("$.fechadoUserId").value(admin.usuarioId()))
				.andExpect(jsonPath("$.fechadoUserNome").value(admin.nome()))
				.andExpect(jsonPath("$.abertoUserNome").value(admin.nome()));
	}

	@Test
	@Order(5)
	public void testListar() throws Exception {
		get("/api/chamado/listar")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(idChamado.intValue())))
				.andExpect(jsonPath("$[0].clienteNome").value(cliente.nome()));
	}

	@Test
	@Order(6)
	public void testBuscarPorId() throws Exception {
		get("/api/chamado/buscarPorId/{id}", idChamado)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.titulo").value(titulo))
				.andExpect(jsonPath("$.atendenteNome").value(admin.nome()));

		get("/api/chamado/buscarPorId/0").andExpect(status().isNotFound());
	}

	@Test
	@Order(7)
	public void testBuscaPorTitulo() throws Exception {
		get("/api/chamado/buscaPorTitulo/{titulo}", titulo)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(idChamado));
	}

	@Test
	@Order(8)
	public void testExistePorTitulo() throws Exception {
		get("/api/chamado/existePorTitulo/{titulo}", titulo).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(9)
	public void testExistePorTituloDiferenteId() throws Exception {
		get("/api/chamado/existePorTituloDiferenteId/{id}/{titulo}", idChamado, titulo)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(10)
	public void testDeletar() throws Exception {
		delete("/api/chamado/deletar/" + idChamado)
				.andExpect(status().isOk())
				.andExpect(content().string("Chamado deletado com sucesso."));

		delete("/api/chamado/deletar/" + idChamado).andExpect(status().isBadRequest());
	}

}
