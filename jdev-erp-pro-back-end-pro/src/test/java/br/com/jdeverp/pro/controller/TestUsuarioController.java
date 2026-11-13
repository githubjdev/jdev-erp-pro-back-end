package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import br.com.jdeverp.pro.contexto.TestContextoApi;
import br.com.jdeverp.pro.enums.TipoClienteFuncionario;
import br.com.jdeverp.pro.enums.TipoPessoa;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.security.JwtAccessDeniedHandler;

public class TestUsuarioController extends TestContextoApi {

	private static final String NOVA_SENHA = "NovaSenha@456";

	private Pessoa pessoa;
	private Long idClienteFuncionario;
	private Long idUsuario;
	private String login;

	@BeforeAll
	void criarCadastroSemUsuario() {
		Empresa empresa = empresaRepository.findById(empresaId).orElseThrow();
		pessoa = pessoaRepository.save(novaPessoa("Usuario Novo", TipoPessoa.FISICA, empresa));
		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setTipoClienteFuncionario(TipoClienteFuncionario.CLIENTE);
		clienteFuncionario.setPessoa(pessoa);
		clienteFuncionario.setEmpresa(empresa);
		idClienteFuncionario = clienteFuncionarioRepository.save(clienteFuncionario).getId();
		login = "usuario.api." + sufixo;
	}

	private Map<String, Object> usuarioJson(Long id, String login) {
		return mapa("id", id, "login", login, "senha", SENHA, "liberado", true, "pessoaId", pessoa.getId());
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idUsuario = id(post("/api/usuario/salvar", usuarioJson(null, login))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.login").value(login))
				.andExpect(jsonPath("$.senha").value("***Ocultada***"))
				.andExpect(jsonPath("$.pessoa").value(pessoa.getNome()))
				.andExpect(jsonPath("$.pessoaId").value(pessoa.getId()))
				.andExpect(jsonPath("$.clienteFuncionarioId").value(idClienteFuncionario))
				.andExpect(jsonPath("$.tipoClienteFuncionario").value("CLIENTE")));

		/* Recebe a ROLE_USER e fica ligado ao cadastro */
		assertEquals(1L, jdbcTemplate.queryForObject("select count(*) from role_usuario where usuario_id = ?", Long.class, idUsuario));
		assertEquals(idUsuario, jdbcTemplate.queryForObject("select usuario_id from cliente_funcionario where id = ?", Long.class,
				idClienteFuncionario));
	}

	@Test
	@Order(2)
	public void testSalvarLoginDuplicado() throws Exception {
		post("/api/usuario/salvar", usuarioJson(null, login))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("O login escolhido já existe, escolha outro login para cadastrar um novo usuário."));
	}

	@Test
	@Order(3)
	public void testSalvarSemSerAdministrador() throws Exception {
		UsuarioTeste comum = criarUsuario("Comum", false);
		comToken(logar(comum.login(), SENHA), MockMvcRequestBuilders.post("/api/usuario/salvar"), usuarioJson(null, "outro." + sufixo))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(JwtAccessDeniedHandler.MSG_SEM_PERMISSAO));
	}

	@Test
	@Order(4)
	public void testLogin() throws Exception {
		logar(login, SENHA);
	}

	@Test
	@Order(5)
	public void testAlterarSenha() throws Exception {
		post("/api/usuario/alterarSenha", mapa("id", idUsuario, "senhaAtual", "senha-errada", "novaSenha", NOVA_SENHA,
				"confirmarSenha", NOVA_SENHA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Senha atual inválida"));

		post("/api/usuario/alterarSenha", mapa("id", 0, "senhaAtual", SENHA, "novaSenha", NOVA_SENHA, "confirmarSenha", NOVA_SENHA))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Usuário não encontrado."));

		post("/api/usuario/alterarSenha", mapa("id", idUsuario, "senhaAtual", SENHA, "novaSenha", NOVA_SENHA,
				"confirmarSenha", NOVA_SENHA))
				.andExpect(status().isOk())
				.andExpect(content().string("Senha alterada com sucesso!"));

		logar(login, NOVA_SENHA);
	}

	@Test
	@Order(6)
	public void testAtualizar() throws Exception {
		login = login + ".editado";
		post("/api/usuario/atualizar", usuarioJson(idUsuario, login))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idUsuario))
				.andExpect(jsonPath("$.login").value(login))
				.andExpect(jsonPath("$.senha").value("***Ocultada***"));

		post("/api/usuario/atualizar", usuarioJson(0L, login + ".x"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Usuário não encontrado para a empresa logada."));
	}

	@Test
	@Order(7)
	public void testBuscarPorIdEAtualizarComORetorno() throws Exception {
		/* O retorno do buscarPorId precisa servir para editar: login e pessoaId vêm preenchidos */
		String json = corpo(get("/api/usuario/buscarPorId/{id}", idUsuario)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.login").value(login))
				.andExpect(jsonPath("$.pessoaId").value(pessoa.getId()))
				.andExpect(jsonPath("$.clienteFuncionarioId").value(idClienteFuncionario))
				.andExpect(jsonPath("$.tipoClienteFuncionario").value("CLIENTE"))
				.andExpect(jsonPath("$.senha").doesNotExist()));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/usuario/atualizar")
				.header("Authorization", "Bearer " + token)
				.contentType("application/json").content(json))
				.andExpect(status().isOk());

		get("/api/usuario/buscarPorId/0").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Usuário não foi encontrado na busca."));
	}

	@Test
	@Order(8)
	public void testListarEBuscas() throws Exception {
		get("/api/usuario/listar").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(idUsuario.intValue())));
		get("/api/usuario/paginado?page=0&size=1&sort=id&direction=desc").andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));
		get("/api/usuario/buscar-por-nome?nome={nome}", pessoa.getNome()).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(idUsuario));
		get("/api/usuario/buscar-por-login/{login}", login).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(idUsuario));
		get("/api/usuario/buscar-por-login/{login}", "nao.existe." + sufixo).andExpect(status().isNotFound());
		post("/api/usuario/buscar-por-ids", ids(idUsuario, admin.usuarioId())).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	@Order(9)
	public void testBuscarPorLoginDeOutraEmpresa() throws Exception {
		UsuarioTeste externo = criarUsuario("Externo", false, criarEmpresa());
		get("/api/usuario/buscar-por-login/{login}", externo.login()).andExpect(status().isNotFound());
	}

	@Test
	@Order(10)
	public void testTotalEExistencia() throws Exception {
		long total = jdbcTemplate.queryForObject("select count(*) from usuario where empresa_id = ?", Long.class, empresaId);
		get("/api/usuario/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(total));
		get("/api/usuario/existe/{id}", idUsuario).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/usuario/existe-por-nome?nome={nome}", pessoa.getNome()).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/usuario/existe-por-nome-diferente-id/{id}?nome={nome}", idUsuario, pessoa.getNome()).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(11)
	public void testDeletar() throws Exception {
		delete("/api/usuario/deletar/" + idUsuario).andExpect(status().isOk())
				.andExpect(content().string("Usuário deletado com sucesso!"));

		assertEquals(0L, jdbcTemplate.queryForObject("select count(*) from role_usuario where usuario_id = ?", Long.class, idUsuario));
		get("/api/usuario/existe/{id}", idUsuario).andExpect(jsonPath("$").value(false));
		delete("/api/usuario/deletar/" + idUsuario).andExpect(status().isBadRequest());
	}

	/* P25: usuário com ROLE_ADMIN nunca é apagado pela API, e o admin logado não remove o próprio acesso */

	@Test
	@Order(12)
	public void testDeletarAdministrador() throws Exception {
		UsuarioTeste outroAdmin = criarUsuario("OutroAdmin", true);

		delete("/api/usuario/deletar/" + outroAdmin.usuarioId()).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Usuário administrador não pode ser apagado. Para apagá-lo, remova antes o "
						+ "acesso ROLE_ADMIN dele em Acessos do usuário e tente de novo."));

		delete("/api/usuario/deletar/" + admin.usuarioId()).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(startsWith("Você não pode apagar o seu próprio usuário")));

		get("/api/usuario/existe/{id}", outroAdmin.usuarioId()).andExpect(jsonPath("$").value(true));
		get("/api/usuario/existe/{id}", admin.usuarioId()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(13)
	public void testAtualizarBloqueandoOProprioUsuario() throws Exception {
		post("/api/usuario/atualizar", mapa("id", admin.usuarioId(), "login", admin.login(), "liberado", false,
				"pessoaId", admin.pessoaId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(startsWith("Você não pode bloquear o seu próprio usuário")));

		/* Continua liberado e com a sessão válida */
		assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject("select liberado from usuario where id = ?", Boolean.class,
				admin.usuarioId()));
		get("/api/usuario/total").andExpect(status().isOk());
	}

	private long usuariosNaoAdministradores() {
		return jdbcTemplate.queryForObject("""
				select count(*) from usuario u
				 where u.empresa_id = ?
				   and not exists (select 1 from role_usuario r join role ro on ro.id = r.acesso_id
				                    where r.usuario_id = u.id and ro.acesso = 'ROLE_ADMIN')""", Long.class, empresaId);
	}

	@Test
	@Order(14)
	public void testDeletarTodosUsuarios() throws Exception {
		UsuarioTeste comum = criarUsuario("ComumLote", false);
		long naoAdministradores = usuariosNaoAdministradores();
		long administradores = jdbcTemplate.queryForObject("select count(*) from usuario where empresa_id = ?", Long.class,
				empresaId) - naoAdministradores;

		delete("/api/usuario/deletar-todos").andExpect(status().isOk())
				.andExpect(content().string("Total de " + naoAdministradores + " usuários deletados com sucesso! Os administradores foram mantidos."));

		/* Os usuários comuns saíram, com os acessos; o cadastro de cliente/funcionário fica, sem usuário */
		get("/api/usuario/existe/{id}", comum.usuarioId()).andExpect(jsonPath("$").value(false));
		assertEquals(0L, jdbcTemplate.queryForObject("select count(*) from role_usuario where usuario_id = ?", Long.class,
				comum.usuarioId()));
		assertNull(jdbcTemplate.queryForObject("select usuario_id from cliente_funcionario where id = ?", Long.class,
				comum.clienteFuncionarioId()));

		/* Todos os admins ficaram, e o logado continua com acesso */
		assertEquals(0L, usuariosNaoAdministradores());
		assertEquals(administradores, jdbcTemplate.queryForObject("select count(*) from usuario where empresa_id = ?", Long.class,
				empresaId));
		get("/api/usuario/total").andExpect(status().isOk());

		delete("/api/usuario/deletar-todos").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Nenhum usuário para deletar: os administradores não são apagados e os demais "
						+ "já foram deletados."));
	}

	@Test
	@Order(15)
	public void testBloquearOutroAdministrador() throws Exception {
		UsuarioTeste outroAdmin = criarUsuario("AdminBloqueio", true);

		post("/api/usuario/atualizar", mapa("id", outroAdmin.usuarioId(), "login", outroAdmin.login(), "liberado", false,
				"pessoaId", outroAdmin.pessoaId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Usuário administrador não pode ser bloqueado por outro administrador. "
						+ "Para bloqueá-lo, remova antes o acesso ROLE_ADMIN dele em Acessos do usuário e tente de novo."));

		assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject("select liberado from usuario where id = ?", Boolean.class,
				outroAdmin.usuarioId()));

		/* Editar outro admin sem bloqueá-lo continua permitido */
		post("/api/usuario/atualizar", mapa("id", outroAdmin.usuarioId(), "login", outroAdmin.login() + ".novo", "liberado", true,
				"pessoaId", outroAdmin.pessoaId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.login").value(outroAdmin.login() + ".novo"));
	}

	@Test
	@Order(100)
	public void testListarPaginado() throws Exception {
		conferirPaginado("/api/usuario/paginado");
	}

}
