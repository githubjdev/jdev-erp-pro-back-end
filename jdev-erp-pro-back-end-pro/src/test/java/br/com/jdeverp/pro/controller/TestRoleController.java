package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;

/** Role é global: o teste cria um acesso próprio e apaga no final. */
public class TestRoleController extends TestContextoApi {

	private Long idRole;
	private String acesso;

	@AfterAll
	void apagarRole() {
		if (idRole != null) {
			executarSql("delete from role_usuario where acesso_id = ?", idRole);
			executarSql("delete from role where id = ?", idRole);
		}
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		acesso = "ROLE_TESTE_API_" + sufixo.toUpperCase();
		idRole = id(post("/api/role/salvar", mapa("acesso", acesso))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.acesso").value(acesso)));
	}

	@Test
	@Order(2)
	public void testSalvarInvalido() throws Exception {
		post("/api/role/salvar", mapa("acesso", "TESTE_SEM_PREFIXO"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Nomenclatura de acesso não contém ROLE_ no incio"));
		post("/api/role/salvar", mapa("acesso", acesso))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe acesso com a mesma descrição: " + acesso));
	}

	@Test
	@Order(3)
	public void testAtualizar() throws Exception {
		acesso = acesso + "_EDITADO";
		post("/api/role/atualizar", mapa("id", idRole, "acesso", acesso))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idRole))
				.andExpect(jsonPath("$.acesso").value(acesso));
	}

	@Test
	@Order(4)
	public void testListarEBuscas() throws Exception {
		get("/api/role/listar").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(idRole.intValue())));
		get("/api/role/buscarPorId/{id}", idRole).andExpect(status().isOk()).andExpect(jsonPath("$.acesso").value(acesso));
		get("/api/role/buscarPorId/0").andExpect(status().isNotFound());
		get("/api/role/buscaPorAcesso/{acesso}", acesso).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idRole));
		get("/api/role/existePorAcesso/{acesso}", acesso).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/role/existePorAcessoDiferenteId/{id}/{acesso}", idRole, acesso).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(5)
	public void testDeletar() throws Exception {
		delete("/api/role/deletar/" + idRole).andExpect(status().isOk())
				.andExpect(content().string("Role deletado com sucesso."));
		delete("/api/role/deletar/" + idRole).andExpect(status().isBadRequest());
		idRole = null;
	}

	@Test
	@Order(6)
	public void testAtualizarAcessoInexistente() throws Exception {
		String novo = "ROLE_INEXISTENTE_" + sufixo.toUpperCase();

		post("/api/role/atualizar", mapa("id", 0, "acesso", novo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Acesso com id: 0 não foi encontrado."));

		/* Não criou um acesso novo com o id inexistente */
		get("/api/role/existePorAcesso/{acesso}", novo).andExpect(jsonPath("$").value(false));
	}

	/** ROLE_USER e ROLE_ADMIN são do sistema: ninguém renomeia nem apaga. */
	@Test
	@Order(7)
	public void testAlterarOuApagarOProprioAcesso() throws Exception {
		Long roleAdmin = role("ROLE_ADMIN").getId();
		Long roleUser = role("ROLE_USER").getId();
		try {
			/* ROLE_USER e ROLE_ADMIN são do sistema: ninguém renomeia */
			post("/api/role/atualizar", mapa("id", roleAdmin, "acesso", "ROLE_ADMIN_RENOMEADO"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").value(startsWith("O acesso ROLE_ADMIN é do sistema e não pode ser renomeado")));
			post("/api/role/atualizar", mapa("id", roleUser, "acesso", "ROLE_USER_RENOMEADO"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").value(startsWith("O acesso ROLE_USER é do sistema e não pode ser renomeado")));
			/* Nem são apagadas. Garantia extra: a ROLE_USER está em uso, então a FK também barraria a exclusão */
			assertTrue(jdbcTemplate.queryForObject("select count(*) from role_usuario where acesso_id = ?", Long.class, roleUser) > 0);
			delete("/api/role/deletar/" + roleAdmin)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").value(startsWith("O acesso ROLE_ADMIN é do sistema e não pode ser apagado")));
			delete("/api/role/deletar/" + roleUser)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").value(startsWith("O acesso ROLE_USER é do sistema e não pode ser apagado")));

			get("/api/role/buscarPorId/{id}", roleAdmin).andExpect(jsonPath("$.acesso").value("ROLE_ADMIN"));
			get("/api/role/buscarPorId/{id}", roleUser).andExpect(jsonPath("$.acesso").value("ROLE_USER"));
		} finally {
			/* Se a proteção falhar, o banco dev não pode ficar sem a ROLE_ADMIN e a ROLE_USER */
			executarSql("update role set acesso = 'ROLE_ADMIN' where id = ?", roleAdmin);
			executarSql("update role set acesso = 'ROLE_USER' where id = ?", roleUser);
		}
	}

}
