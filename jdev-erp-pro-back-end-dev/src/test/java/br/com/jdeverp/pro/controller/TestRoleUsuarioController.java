package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;
import br.com.jdeverp.pro.model.Role;

public class TestRoleUsuarioController extends TestContextoApi {

	private UsuarioTeste operador;
	private Role roleUser;
	private Role roleAdmin;
	private Role roleTeste;
	private Long idRoleUsuario;

	@BeforeAll
	void criarUsuarioERole() {
		operador = criarUsuario("Operador", false);
		roleUser = role("ROLE_USER");
		roleAdmin = role("ROLE_ADMIN");
		Role nova = new Role();
		nova.setAcesso("ROLE_TESTE_API_RU_" + sufixo.toUpperCase());
		roleTeste = roleRepository.save(nova);
	}

	@AfterAll
	void apagarRole() {
		executarSql("delete from role_usuario where acesso_id = ?", roleTeste.getId());
		executarSql("delete from role where id = ?", roleTeste.getId());
	}

	private Map<String, Object> roleUsuarioJson(Long id, Long role, Long usuario) {
		return mapa("id", id, "acesso", ref(role), "usuario", ref(usuario));
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idRoleUsuario = id(post("/api/roleUsuario/salvar", roleUsuarioJson(null, roleAdmin.getId(), operador.usuarioId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.acessoId").value(roleAdmin.getId()))
				.andExpect(jsonPath("$.acessoNome").value("ROLE_ADMIN"))
				.andExpect(jsonPath("$.usuarioId").value(operador.usuarioId()))
				.andExpect(jsonPath("$.usuarioNome").value(operador.nome())));
	}

	@Test
	@Order(2)
	public void testSalvarDuplicado() throws Exception {
		post("/api/roleUsuario/salvar", roleUsuarioJson(null, roleAdmin.getId(), operador.usuarioId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe o acesso de: ROLE_ADMIN associado para o mesmo usuário: " + operador.nome()));
	}

	@Test
	@Order(3)
	public void testSalvarUsuarioDeOutraEmpresa() throws Exception {
		UsuarioTeste externo = criarUsuario("Externo", false, criarEmpresa());
		post("/api/roleUsuario/salvar", roleUsuarioJson(null, roleAdmin.getId(), externo.usuarioId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Usuário não encontrado para a empresa logada."));

		long acessosDoExterno = jdbcTemplate.queryForObject("select count(*) from role_usuario where usuario_id = ?",
				Long.class, externo.usuarioId());
		org.junit.jupiter.api.Assertions.assertEquals(1, acessosDoExterno, "Só o ROLE_USER criado no teste");
	}

	@Test
	@Order(4)
	public void testSalvarAcessoInexistente() throws Exception {
		post("/api/roleUsuario/salvar", roleUsuarioJson(null, 0L, operador.usuarioId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Acesso não encontrado."));
	}

	@Test
	@Order(5)
	public void testAtualizar() throws Exception {
		post("/api/roleUsuario/atualizar", roleUsuarioJson(idRoleUsuario, roleTeste.getId(), operador.usuarioId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idRoleUsuario))
				.andExpect(jsonPath("$.acessoNome").value(roleTeste.getAcesso()))
				.andExpect(jsonPath("$.usuarioNome").value(operador.nome()));
	}

	@Test
	@Order(6)
	public void testAtualizarRegistroDeOutraEmpresa() throws Exception {
		UsuarioTeste externo = criarUsuario("Externo2", false, criarEmpresa());
		Long idExterno = jdbcTemplate.queryForObject("select id from role_usuario where usuario_id = ?", Long.class,
				externo.usuarioId());

		post("/api/roleUsuario/atualizar", roleUsuarioJson(idExterno, roleAdmin.getId(), operador.usuarioId()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Registro de acesso não encontrado para a empresa logada."));
	}

	@Test
	@Order(7)
	public void testListarEBuscas() throws Exception {
		get("/api/roleUsuario/listar").andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(idRoleUsuario.intValue())));
		get("/api/roleUsuario/buscarPorId/{id}", idRoleUsuario).andExpect(status().isOk())
				.andExpect(jsonPath("$.usuarioNome").value(operador.nome()));
		get("/api/roleUsuario/buscarPorId/0").andExpect(status().isNotFound());
		get("/api/roleUsuario/listarPorUsuario/{id}", operador.usuarioId()).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
		get("/api/roleUsuario/listarPorRole/{id}", roleTeste.getId()).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(idRoleUsuario));
		get("/api/roleUsuario/existePorUsuarioERole/{u}/{r}", operador.usuarioId(), roleTeste.getId())
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(8)
	public void testDeletarPorUsuarioERole() throws Exception {
		delete("/api/roleUsuario/deletarPorUsuarioERole/" + operador.usuarioId() + "/" + roleUser.getId())
				.andExpect(status().isOk())
				.andExpect(content().string("Role do usuário deletado com sucesso."));
		get("/api/roleUsuario/existePorUsuarioERole/{u}/{r}", operador.usuarioId(), roleUser.getId())
				.andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(9)
	public void testDeletar() throws Exception {
		delete("/api/roleUsuario/deletar/" + idRoleUsuario).andExpect(status().isOk())
				.andExpect(content().string("Role do usuário deletado com sucesso."));
		delete("/api/roleUsuario/deletar/" + idRoleUsuario).andExpect(status().isBadRequest());
	}

	/** P25: o admin logado não remove nem altera os próprios acessos. */
	@Test
	@Order(10)
	public void testRemoverOsPropriosAcessos() throws Exception {
		Long idProprio = jdbcTemplate.queryForObject("select id from role_usuario where usuario_id = ? and acesso_id = ?",
				Long.class, admin.usuarioId(), roleAdmin.getId());

		delete("/api/roleUsuario/deletar/" + idProprio).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(startsWith("Você não pode remover ou alterar os seus próprios acessos")));
		delete("/api/roleUsuario/deletarPorUsuarioERole/" + admin.usuarioId() + "/" + roleAdmin.getId())
				.andExpect(status().isForbidden());
		post("/api/roleUsuario/atualizar", roleUsuarioJson(idProprio, roleTeste.getId(), operador.usuarioId()))
				.andExpect(status().isForbidden());

		/* Continua com a ROLE_ADMIN e com acesso à área de administração */
		get("/api/roleUsuario/existePorUsuarioERole/{u}/{r}", admin.usuarioId(), roleAdmin.getId())
				.andExpect(jsonPath("$").value(true));
		get("/api/role/listar").andExpect(status().isOk());
	}

}
