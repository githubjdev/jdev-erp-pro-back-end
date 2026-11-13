package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.security.UsuarioAutenticado;

/** Usuário e empresa logados vêm do SecurityContext preenchido pelo JwtFilter, nunca do request. */
public class TestUsuarioLogadoService {

	private final UsuarioLogadoService usuarioLogadoService = new UsuarioLogadoService();

	private Usuario usuario;
	private Empresa empresa;

	@BeforeEach
	public void autenticar() {
		empresa = new Empresa();
		empresa.setId(10L);
		usuario = new Usuario();
		usuario.setId(7L);
		usuario.setLogin("maria");
		usuario.setEmpresa(empresa);
		usuario.setAcessos(new ArrayList<>(List.of(role("ROLE_USER"))));

		UsuarioAutenticado autenticado = new UsuarioAutenticado(usuario);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(autenticado, "token", autenticado.getAuthorities()));
	}

	@AfterEach
	public void limparContexto() {
		SecurityContextHolder.clearContext();
	}

	private static Role role(String acesso) {
		Role role = new Role();
		role.setAcesso(acesso);
		return role;
	}

	@Test
	public void testGetUsuarioAutenticado() {
		assertSame(usuario, usuarioLogadoService.getUsuarioAutenticado().getUsuario());
	}

	@Test
	public void testGetUsuarioLogado() {
		assertSame(usuario, usuarioLogadoService.getUsuarioLogado());
	}

	@Test
	public void testGetEmpresaLogada() {
		assertSame(empresa, usuarioLogadoService.getEmpresaLogada());
	}

	@Test
	public void testGetEmpresaIdLogada() {
		assertEquals(10L, usuarioLogadoService.getEmpresaIdLogada());
	}

	@Test
	public void testGetUsuarioLogadoId() {
		assertEquals(7L, usuarioLogadoService.getUsuarioLogadoId());
	}

	@Test
	public void testIsAdmin() {
		assertFalse(usuarioLogadoService.isAdmin());

		usuario.getAcessos().add(role("ROLE_ADMIN"));

		assertTrue(usuarioLogadoService.isAdmin());
	}

	@Test
	public void testPossuiAcesso() {
		Role admin = role("ROLE_ADMIN");
		admin.setId(2L);
		usuario.getAcessos().add(admin);

		assertTrue(usuarioLogadoService.possuiAcesso(2L));
		assertFalse(usuarioLogadoService.possuiAcesso(3L));
		assertFalse(usuarioLogadoService.possuiAcesso(null));
	}

	@Test
	public void testIsUsuarioLogado() {
		assertTrue(usuarioLogadoService.isUsuarioLogado(7L));
		assertFalse(usuarioLogadoService.isUsuarioLogado(8L));
		assertFalse(usuarioLogadoService.isUsuarioLogado(null));
	}

}
