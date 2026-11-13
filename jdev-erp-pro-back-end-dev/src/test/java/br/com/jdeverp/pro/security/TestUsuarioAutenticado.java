package br.com.jdeverp.pro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.Usuario;

/** O principal da requisição delega tudo para o usuário carregado no JwtFilter. */
public class TestUsuarioAutenticado {

	private static Usuario usuario(boolean liberado) {
		Role role = new Role();
		role.setAcesso("ROLE_USER");
		Usuario usuario = new Usuario();
		usuario.setLogin("maria");
		usuario.setSenha("hash");
		usuario.setLiberado(liberado);
		usuario.setAcessos(List.of(role));
		return usuario;
	}

	@Test
	public void testGetUsuario() {
		Usuario usuario = usuario(true);

		assertSame(usuario, new UsuarioAutenticado(usuario).getUsuario());
	}

	@Test
	public void testGetAuthorities() {
		List<String> autoridades = new UsuarioAutenticado(usuario(true)).getAuthorities().stream()
				.map(GrantedAuthority::getAuthority).toList();

		assertEquals(List.of("ROLE_USER"), autoridades);
	}

	@Test
	public void testGetUsernameEPassword() {
		UsuarioAutenticado autenticado = new UsuarioAutenticado(usuario(true));

		assertEquals("maria", autenticado.getUsername());
		assertEquals("hash", autenticado.getPassword());
	}

	@Test
	public void testEstadoDaConta() {
		UsuarioAutenticado liberado = new UsuarioAutenticado(usuario(true));
		UsuarioAutenticado bloqueado = new UsuarioAutenticado(usuario(false));

		assertTrue(liberado.isEnabled());
		assertFalse(bloqueado.isEnabled());
		/* Conta, bloqueio e credencial não expiram: o bloqueio é pelo "liberado" */
		assertTrue(bloqueado.isAccountNonExpired());
		assertTrue(bloqueado.isAccountNonLocked());
		assertTrue(bloqueado.isCredentialsNonExpired());
	}

}
