package br.com.jdeverp.pro.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** O acesso é gravado sempre em maiúsculas; nulo vira texto vazio. */
public class TestRole {

	@Test
	public void testSetAcesso() {
		Role role = new Role();

		role.setAcesso("role_gerente");
		assertEquals("ROLE_GERENTE", role.getAcesso());
		assertEquals("ROLE_GERENTE", role.getAuthority());
	}

	@Test
	public void testSetAcessoNulo() {
		Role role = new Role();

		role.setAcesso(null);

		assertEquals("", role.getAcesso());
	}

}
