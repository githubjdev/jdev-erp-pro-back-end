package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.UsuarioRepository;

/** Carga do usuário para o Spring Security e conferência do token de sessão gravado. */
@ExtendWith(MockitoExtension.class)
public class TestUsuarioDetailsService {

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private JwtService jwtService;

	@InjectMocks
	private UsuarioDetailsService usuarioDetailsService;

	private static Usuario usuario(boolean liberado, boolean empresaBloqueada) {
		Empresa empresa = new Empresa();
		empresa.setId(10L);
		empresa.setBloqueio(empresaBloqueada);
		Usuario usuario = new Usuario();
		usuario.setId(7L);
		usuario.setLogin("maria");
		usuario.setLiberado(liberado);
		usuario.setEmpresa(empresa);
		return usuario;
	}

	@Test
	public void testLoadUserByUsername() {
		Usuario usuario = usuario(true, false);
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario);

		assertSame(usuario, usuarioDetailsService.loadUserByUsername("maria"));
	}

	@Test
	public void testLoadUserByUsernameNaoEncontrado() {
		when(usuarioRepository.buscaPorLogin("ninguem")).thenReturn(null);

		UsernameNotFoundException erro = assertThrows(UsernameNotFoundException.class,
				() -> usuarioDetailsService.loadUserByUsername("ninguem"));

		assertEquals("Usuário não encontrado no banco de dados.", erro.getMessage());
	}

	@Test
	public void testLoadUserByUsernameUsuarioBloqueado() {
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario(false, false));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioDetailsService.loadUserByUsername("maria"));

		assertEquals("Usuário bloqueado, entre em contato com o administrador do sistema.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
	}

	@Test
	public void testLoadUserByUsernameEmpresaBloqueada() {
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario(true, true));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioDetailsService.loadUserByUsername("maria"));

		assertEquals("Empresa bloqueada, entre em contato com o administrador do sistema.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
	}

	@Test
	public void testExistsByTokenParaUser() {
		when(jwtService.extrairEmpresaId("token")).thenReturn(10L);
		when(jwtService.extrairUsuarioId("token")).thenReturn(7L);
		when(usuarioRepository.existsByTokenParaUser("token", 7L, 10L)).thenReturn(true);

		assertDoesNotThrow(() -> usuarioDetailsService.existsByTokenParaUser("token"));
	}

	@Test
	public void testExistsByTokenParaUserSessaoSubstituida() {
		when(jwtService.extrairEmpresaId("token-antigo")).thenReturn(10L);
		when(jwtService.extrairUsuarioId("token-antigo")).thenReturn(7L);
		when(usuarioRepository.existsByTokenParaUser("token-antigo", 7L, 10L)).thenReturn(false);

		/* Um login mais novo trocou o token gravado no usuário */
		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioDetailsService.existsByTokenParaUser("token-antigo"));

		assertEquals("Sessão encerrada ou substituída por um novo login, faça login novamente.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
	}

}
