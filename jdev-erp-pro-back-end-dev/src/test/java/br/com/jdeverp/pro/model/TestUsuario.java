package br.com.jdeverp.pro.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Usuario como UserDetails: login, senha, liberado e roles. */
public class TestUsuario {

	private static Role role(String acesso) {
		Role role = new Role();
		role.setAcesso(acesso);
		return role;
	}

	@Test
	public void testIdAdmin() {
		Usuario usuario = new Usuario();
		usuario.setAcessos(new ArrayList<>(List.of(role("ROLE_USER"))));
		assertFalse(usuario.idAdmin());

		usuario.getAcessos().add(role("ROLE_ADMIN"));
		assertTrue(usuario.idAdmin());
	}

	@Test
	public void testIdAdminSemRoles() {
		Usuario semLista = new Usuario();
		semLista.setAcessos(null);
		Usuario listaVazia = new Usuario();

		assertFalse(semLista.idAdmin());
		assertFalse(listaVazia.idAdmin());
	}

	@Test
	public void testUserDetails() {
		Usuario usuario = new Usuario();
		usuario.setLogin("maria");
		usuario.setSenha("hash");
		List<Role> roles = List.of(role("ROLE_USER"));
		usuario.setAcessos(roles);

		assertEquals("maria", usuario.getUsername());
		assertEquals("hash", usuario.getPassword());
		assertSame(roles, usuario.getAuthorities());
		assertTrue(usuario.isAccountNonExpired());
		assertTrue(usuario.isAccountNonLocked());
		assertTrue(usuario.isCredentialsNonExpired());
	}

	@Test
	public void testIsEnabled() {
		Usuario usuario = new Usuario();
		/* Usuário novo nasce liberado */
		assertTrue(usuario.isEnabled());

		usuario.setLiberado(false);
		assertFalse(usuario.isEnabled());

		/* Nulo conta como bloqueado: liberar é decisão explícita do admin */
		usuario.setLiberado(null);
		assertFalse(usuario.isEnabled());
	}

}
