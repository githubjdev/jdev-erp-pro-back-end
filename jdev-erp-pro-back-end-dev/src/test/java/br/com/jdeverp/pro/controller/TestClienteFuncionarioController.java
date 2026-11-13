package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;
import br.com.jdeverp.pro.enums.TipoPessoa;
import br.com.jdeverp.pro.model.Pessoa;

public class TestClienteFuncionarioController extends TestContextoApi {

	private Pessoa pessoa;
	private Pessoa pessoa2;
	private Long idClienteFuncionario;
	private Long idClienteFuncionario2;
	private Long idUsuario;

	@BeforeAll
	void criarPessoas() {
		var empresa = empresaRepository.findById(empresaId).orElseThrow();
		pessoa = pessoaRepository.save(novaPessoa("Cliente CF", TipoPessoa.FISICA, empresa));
		pessoa2 = pessoaRepository.save(novaPessoa("Fornecedor CF", TipoPessoa.JURIDICA, empresa));
	}

	private Map<String, Object> clienteFuncionarioJson(Long id, String tipo, Long pessoa, Long usuario) {
		return mapa("id", id, "tipoClienteFuncionario", tipo, "pessoa", ref(pessoa),
				"usuario", usuario == null ? null : ref(usuario));
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idClienteFuncionario = id(post("/api/clienteFuncionario/salvar", clienteFuncionarioJson(null, "CLIENTE", pessoa.getId(), null))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoClienteFuncionario").value("CLIENTE"))
				.andExpect(jsonPath("$.pessoaId").value(pessoa.getId()))
				.andExpect(jsonPath("$.nomePessoa").value(pessoa.getNome()))
				.andExpect(jsonPath("$.usuarioId").value(nullValue()))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));
	}

	@Test
	@Order(2)
	public void testSalvarPessoaJaCadastrada() throws Exception {
		post("/api/clienteFuncionario/salvar", clienteFuncionarioJson(null, "CLIENTE", pessoa.getId(), null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe um cliente/funcionário cadastrado para a pessoa informada."));
	}

	@Test
	@Order(3)
	public void testSalvarPessoaDeOutraEmpresa() throws Exception {
		Long outraEmpresa = criarEmpresa();
		Pessoa externa = pessoaRepository.save(novaPessoa("Externa CF", TipoPessoa.FISICA,
				empresaRepository.findById(outraEmpresa).orElseThrow()));

		post("/api/clienteFuncionario/salvar", clienteFuncionarioJson(null, "CLIENTE", externa.getId(), null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Pessoa não encontrada para a empresa logada."));
	}

	@Test
	@Order(4)
	public void testAtualizarSemUsuario() throws Exception {
		post("/api/clienteFuncionario/atualizar", clienteFuncionarioJson(idClienteFuncionario, "FUNCIONARIO", pessoa.getId(), null))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Usuário deve ser informado para atualizar o cadastro."));
	}

	@Test
	@Order(5)
	public void testAtualizar() throws Exception {
		/* Cria o usuário de acesso da pessoa (o UsuarioService liga o usuário ao cadastro) */
		idUsuario = id(post("/api/usuario/salvar", mapa("login", "cf." + sufixo, "senha", SENHA, "liberado", true,
				"pessoaId", pessoa.getId())).andExpect(status().isOk()));

		post("/api/clienteFuncionario/atualizar", clienteFuncionarioJson(idClienteFuncionario, "FUNCIONARIO", pessoa.getId(), idUsuario))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idClienteFuncionario))
				.andExpect(jsonPath("$.tipoClienteFuncionario").value("FUNCIONARIO"))
				.andExpect(jsonPath("$.usuarioId").value(idUsuario))
				.andExpect(jsonPath("$.nomePessoa").value(pessoa.getNome()));
	}

	@Test
	@Order(6)
	public void testListarEBuscas() throws Exception {
		get("/api/clienteFuncionario/listar").andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(idClienteFuncionario.intValue())));
		get("/api/clienteFuncionario/buscaPorNome/{nome}", pessoa.getNome()).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(idClienteFuncionario));
		get("/api/clienteFuncionario/existePorNome/{nome}", pessoa.getNome()).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(true));
		get("/api/clienteFuncionario/existePorNomeDiferenteId/{id}/{nome}", idClienteFuncionario, pessoa.getNome())
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(7)
	public void testBuscarPorId() throws Exception {
		get("/api/clienteFuncionario/buscarPorId/{id}", idClienteFuncionario).andExpect(status().isOk())
				.andExpect(jsonPath("$.nomePessoa").value(pessoa.getNome()))
				.andExpect(jsonPath("$.usuarioId").value(idUsuario));
		get("/api/clienteFuncionario/buscarPorId/0").andExpect(status().isNotFound());
		get("/api/clienteFuncionario/existePorId/{id}", idClienteFuncionario).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(true));
		get("/api/clienteFuncionario/buscarPorPessoa/{id}", pessoa.getId()).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idClienteFuncionario));
	}

	@Test
	@Order(8)
	public void testBuscarPorIdsTotalEPaginado() throws Exception {
		post("/api/clienteFuncionario/buscarPorIds", ids(idClienteFuncionario, admin.clienteFuncionarioId()))
				.andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		long total = jdbcTemplate.queryForObject("select count(*) from cliente_funcionario where empresa_id = ?", Long.class, empresaId);
		get("/api/clienteFuncionario/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(total));
		get("/api/clienteFuncionario/paginado?page=0&size=1&sort=id&direction=desc").andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(idClienteFuncionario));
	}

	@Test
	@Order(9)
	public void testRemoverUsuario() throws Exception {
		delete("/api/clienteFuncionario/removerUsuario/" + idUsuario).andExpect(status().isNoContent());
		get("/api/clienteFuncionario/buscarPorId/{id}", idClienteFuncionario)
				.andExpect(jsonPath("$.usuarioId").value(nullValue()));

		/* O usuário ainda aponta para o cadastro: apaga antes de apagar o cadastro */
		delete("/api/usuario/deletar/" + idUsuario).andExpect(status().isOk());
	}

	@Test
	@Order(10)
	public void testDeletarTodosPorIds() throws Exception {
		idClienteFuncionario2 = id(post("/api/clienteFuncionario/salvar",
				clienteFuncionarioJson(null, "PRESTADOR_SERVICO", pessoa2.getId(), null)).andExpect(status().isOk()));
		delete("/api/clienteFuncionario/deletarTodosPorIds", ids(idClienteFuncionario2)).andExpect(status().isNoContent());
		get("/api/clienteFuncionario/existePorId/{id}", idClienteFuncionario2).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(11)
	public void testDeletar() throws Exception {
		delete("/api/clienteFuncionario/deletar/" + idClienteFuncionario).andExpect(status().isOk())
				.andExpect(content().string("Cliente funcionário deletado com sucesso."));
		delete("/api/clienteFuncionario/deletar/" + idClienteFuncionario).andExpect(status().isBadRequest());
	}

	@Test
	@Order(12)
	public void testDeletarTodosComCadastroEmUso() throws Exception {
		/* O cadastro do administrador está ligado ao usuário dele: o banco recusa e nada é apagado */
		delete("/api/clienteFuncionario/deletarTodos").andExpect(status().isBadRequest());
		get("/api/clienteFuncionario/existePorId/{id}", admin.clienteFuncionarioId()).andExpect(jsonPath("$").value(true));
	}


	@Test
	@Order(100)
	public void testListarPaginado() throws Exception {
		conferirPaginado("/api/clienteFuncionario/paginado");
	}

}
