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

public class TestMensagemController extends TestContextoApi {

	private UsuarioTeste cliente;
	private Long idChamado;
	private String tituloChamado;
	private Long idMensagem;
	private Long idMensagem2;
	private String conteudo;

	@BeforeAll
	void criarChamado() throws Exception {
		cliente = criarUsuario("Cliente", false);
		tituloChamado = "Chamado Mensagem " + sufixo;
		idChamado = id(post("/api/chamado/salvar", mapa("titulo", tituloChamado, "descricao", "Chamado para mensagens",
				"tipoChamado", "SUPORTE", "prioridadeChamado", "MEDIA", "statusChamado", "EM_ATENDIMENTO",
				"atendente", ref(admin.usuarioId()), "cliente", ref(cliente.usuarioId()))).andExpect(status().isOk()));
		conteudo = "Mensagem API " + sufixo;
	}

	private Map<String, Object> mensagemJson(Long id, String conteudo, boolean lida) {
		return mapa("id", id, "conteudo", conteudo, "arquivo", "anexo.pdf", "lida", lida, "chamado", ref(idChamado),
				"atendente", ref(admin.usuarioId()), "cliente", ref(cliente.usuarioId()));
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idMensagem = id(post("/api/mensagem/salvar", mensagemJson(null, conteudo, false))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.conteudo").value(conteudo))
				.andExpect(jsonPath("$.dataEnvio").value(LocalDate.now().toString()))
				.andExpect(jsonPath("$.lida").value(false))
				.andExpect(jsonPath("$.chamadoId").value(idChamado))
				.andExpect(jsonPath("$.chamadoTitulo").value(tituloChamado))
				.andExpect(jsonPath("$.atendenteNome").value(admin.nome()))
				.andExpect(jsonPath("$.clienteNome").value(cliente.nome()))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));

		idMensagem2 = id(post("/api/mensagem/salvar", mensagemJson(null, "Segunda " + conteudo, false))
				.andExpect(status().isOk()));
	}

	@Test
	@Order(2)
	public void testSalvarSemArquivo() throws Exception {
		/* Obrigatório, como a coluna NOT NULL do banco (P20) */
		Map<String, Object> semArquivo = mensagemJson(null, "Sem arquivo " + conteudo, false);
		semArquivo.remove("arquivo");
		post("/api/mensagem/salvar", semArquivo)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Arquivo da mensagem deve ser informado"));
	}

	@Test
	@Order(2)
	public void testAtualizar() throws Exception {
		conteudo = conteudo + " editada";
		post("/api/mensagem/atualizar", mensagemJson(idMensagem, conteudo, false))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idMensagem))
				.andExpect(jsonPath("$.conteudo").value(conteudo))
				.andExpect(jsonPath("$.chamadoTitulo").value(tituloChamado));
	}

	@Test
	@Order(3)
	public void testListar() throws Exception {
		get("/api/mensagem/listar").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		get("/api/mensagem/listarTodos").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(idMensagem.intValue())));
		get("/api/mensagem/listarPorChamado/{id}", idChamado).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	@Order(4)
	public void testBuscaPorConteudo() throws Exception {
		get("/api/mensagem/buscaPorConteudo/{c}", conteudo).andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(idMensagem.intValue())));
		get("/api/mensagem/buscaPorConteudoPorChamado/{idChamado}/{c}", idChamado, "Segunda").andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(idMensagem2));
	}

	@Test
	@Order(5)
	public void testExistePorConteudo() throws Exception {
		get("/api/mensagem/existePorConteudo/{c}", conteudo).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/mensagem/existePorConteudoDiferenteId/{id}/{c}", idMensagem, conteudo).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(false));
		get("/api/mensagem/existePorConteudoPorChamado/{idChamado}/{c}", idChamado, conteudo).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(true));
		get("/api/mensagem/existePorConteudoDiferenteIdPorChamado/{id}/{idChamado}/{c}", idMensagem, idChamado, conteudo)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(6)
	public void testBuscarPorId() throws Exception {
		get("/api/mensagem/buscarPorId/{id}", idMensagem).andExpect(status().isOk())
				.andExpect(jsonPath("$.clienteNome").value(cliente.nome()));
		get("/api/mensagem/buscarPorId/0").andExpect(status().isNotFound());
		get("/api/mensagem/existePorId/{id}", idMensagem).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(7)
	public void testBuscarPorIdsTotalEPaginado() throws Exception {
		post("/api/mensagem/buscarPorIds", ids(idMensagem, idMensagem2)).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
		get("/api/mensagem/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(2));
		get("/api/mensagem/totalPorChamado/{id}", idChamado).andExpect(status().isOk()).andExpect(jsonPath("$").value(2));
		get("/api/mensagem/paginado?page=0&size=1&sort=id&direction=asc").andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(idMensagem));
	}

	@Test
	@Order(8)
	public void testAtualizarLeitura() throws Exception {
		patch("/api/mensagem/atualizarLeitura/" + idMensagem + "/true").andExpect(status().isNoContent());
		get("/api/mensagem/buscarPorId/{id}", idMensagem).andExpect(jsonPath("$.lida").value(true));
	}

	@Test
	@Order(9)
	public void testAtualizarLeituraMensagemDeOutraEmpresa() throws Exception {
		patch("/api/mensagem/atualizarLeitura/0/true")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Mensagem não encontrada para a empresa logada."));
	}

	@Test
	@Order(10)
	public void testNaoLidas() throws Exception {
		get("/api/mensagem/listarNaoLidas").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(idMensagem2));
		get("/api/mensagem/listarNaoLidasPorChamado/{id}", idChamado).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
		get("/api/mensagem/totalNaoLidasPorChamado/{id}", idChamado).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(1));
		get("/api/mensagem/listarNaoLidasPorAtendente/{id}", admin.usuarioId()).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
		get("/api/mensagem/listarNaoLidasPorCliente/{id}", cliente.usuarioId()).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
	}

	@Test
	@Order(11)
	public void testPorAtendenteECliente() throws Exception {
		get("/api/mensagem/listarPorAtendente/{id}", admin.usuarioId()).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
		get("/api/mensagem/totalPorAtendente/{id}", admin.usuarioId()).andExpect(status().isOk()).andExpect(jsonPath("$").value(2));
		get("/api/mensagem/listarPorCliente/{id}", cliente.usuarioId()).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
		get("/api/mensagem/totalPorCliente/{id}", cliente.usuarioId()).andExpect(status().isOk()).andExpect(jsonPath("$").value(2));
		get("/api/mensagem/listarPorChamadoEAtendente/{idChamado}/{idAtendente}", idChamado, admin.usuarioId())
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	@Order(12)
	public void testDeletarTodosPorIds() throws Exception {
		delete("/api/mensagem/deletarTodosPorIds", ids(idMensagem2)).andExpect(status().isNoContent());
		get("/api/mensagem/existePorId/{id}", idMensagem2).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(13)
	public void testDeletarPorChamado() throws Exception {
		delete("/api/mensagem/deletarPorChamado/" + idMensagem + "/" + idChamado).andExpect(status().isNoContent());
		delete("/api/mensagem/deletarPorChamado/" + idMensagem + "/" + idChamado).andExpect(status().isBadRequest());
	}

	@Test
	@Order(14)
	public void testDeletar() throws Exception {
		Long id = id(post("/api/mensagem/salvar", mensagemJson(null, "Terceira " + conteudo, false)).andExpect(status().isOk()));
		delete("/api/mensagem/deletar/" + id).andExpect(status().isOk())
				.andExpect(content().string("Mensagem deletada com sucesso."));
		delete("/api/mensagem/deletar/" + id).andExpect(status().isBadRequest());
	}

	@Test
	@Order(15)
	public void testDeletarTodosPorChamado() throws Exception {
		post("/api/mensagem/salvar", mensagemJson(null, "Quarta " + conteudo, false)).andExpect(status().isOk());
		post("/api/mensagem/salvar", mensagemJson(null, "Quinta " + conteudo, false)).andExpect(status().isOk());
		delete("/api/mensagem/deletarTodosPorChamado/" + idChamado).andExpect(status().isNoContent());
		get("/api/mensagem/totalPorChamado/{id}", idChamado).andExpect(jsonPath("$").value(0));
		delete("/api/mensagem/deletarTodosPorChamado/" + idChamado).andExpect(status().isBadRequest());
	}

	@Test
	@Order(16)
	public void testDeletarTodos() throws Exception {
		post("/api/mensagem/salvar", mensagemJson(null, "Sexta " + conteudo, false)).andExpect(status().isOk());
		delete("/api/mensagem/deletarTodos").andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
		delete("/api/mensagem/deletarTodos").andExpect(status().isBadRequest());
	}

}
