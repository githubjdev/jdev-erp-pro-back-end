package br.com.jdeverp.pro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.service.JwtService;
import br.com.jdeverp.pro.service.UsuarioDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;

/**
 * Caminhos do {@link JwtFilter} com mocks. O fluxo real (login, 401 pelo
 * entry point) está em {@link TestJwtFilter}; aqui fica o que não dá para
 * provocar de ponta a ponta, como um erro de banco no meio da autenticação.
 */
@ExtendWith(MockitoExtension.class)
public class TestJwtFilterUnitario {

	private static final String TOKEN = "token-de-teste";

	@Mock
	private JwtService jwtService;

	@Mock
	private UsuarioDetailsService usuarioDetailsService;

	@Mock
	private HandlerExceptionResolver handlerExceptionResolver;

	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private JwtFilter jwtFilter;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	public void configurar() {
		request = new MockHttpServletRequest("GET", "/api/categoria/total");
		response = new MockHttpServletResponse();
	}

	@AfterEach
	public void limpar() {
		SecurityContextHolder.clearContext();
	}

	private void comToken() {
		request.addHeader("Authorization", "Bearer " + TOKEN);
	}

	@Test
	public void testDoFilterInternalSemToken() throws Exception {
		jwtFilter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verifyNoInteractions(jwtService, usuarioDetailsService, handlerExceptionResolver);
		assertNull(request.getAttribute(JwtFilter.ATRIBUTO_MOTIVO_RECUSA));
	}

	@Test
	public void testDoFilterInternalTokenValido() throws Exception {
		comToken();
		Role role = new Role();
		role.setAcesso("ROLE_USER");
		Usuario usuario = new Usuario();
		usuario.setId(1L);
		usuario.setLogin("joao");
		usuario.setAcessos(List.of(role));
		when(jwtService.extrairLogin(TOKEN)).thenReturn("joao");
		when(usuarioDetailsService.loadUserByUsername("joao")).thenReturn(usuario);

		jwtFilter.doFilter(request, response, filterChain);

		verify(usuarioDetailsService).existsByTokenParaUser(TOKEN);
		verify(filterChain).doFilter(request, response);
		Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
		UsuarioAutenticado principal = assertInstanceOf(UsuarioAutenticado.class, autenticacao.getPrincipal());
		assertEquals(usuario, principal.getUsuario());
		assertNull(request.getAttribute(JwtFilter.ATRIBUTO_MOTIVO_RECUSA));
	}

	@Test
	public void testDoFilterInternalTokenExpirado() throws Exception {
		comToken();
		when(jwtService.extrairLogin(TOKEN)).thenThrow(new ExpiredJwtException(null, null, "expirado"));

		jwtFilter.doFilter(request, response, filterChain);

		/* Segue sem autenticação: o entry point responde 401 com o motivo */
		verify(filterChain).doFilter(request, response);
		assertEquals(JwtFilter.MSG_TOKEN_EXPIRADO, request.getAttribute(JwtFilter.ATRIBUTO_MOTIVO_RECUSA));
		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verifyNoInteractions(usuarioDetailsService, handlerExceptionResolver);
	}

	@Test
	public void testDoFilterInternalTokenMalFormado() throws Exception {
		comToken();
		when(jwtService.extrairLogin(TOKEN)).thenThrow(new MalformedJwtException("mal formado"));

		jwtFilter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertEquals(JwtFilter.MSG_TOKEN_INVALIDO, request.getAttribute(JwtFilter.ATRIBUTO_MOTIVO_RECUSA));
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	public void testDoFilterInternalSessaoEncerrada() throws Exception {
		comToken();
		when(jwtService.extrairLogin(TOKEN)).thenReturn("joao");
		doThrow(new MsgApiException("Sessão encerrada ou substituída por um novo login, faça login novamente.",
				HttpStatus.UNAUTHORIZED)).when(usuarioDetailsService).existsByTokenParaUser(TOKEN);

		jwtFilter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		verify(usuarioDetailsService, never()).loadUserByUsername("joao");
		assertEquals("Sessão encerrada ou substituída por um novo login, faça login novamente.",
				request.getAttribute(JwtFilter.ATRIBUTO_MOTIVO_RECUSA));
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	public void testDoFilterInternalErroInesperado() throws Exception {
		comToken();
		when(jwtService.extrairLogin(TOKEN)).thenReturn("joao");
		DataAccessResourceFailureException erro = new DataAccessResourceFailureException("Banco fora do ar");
		doThrow(erro).when(usuarioDetailsService).existsByTokenParaUser(TOKEN);

		jwtFilter.doFilter(request, response, filterChain);

		/* Não é recusa de token: responde pelo GlobalExceptionHandler (500) e não segue a cadeia */
		verify(handlerExceptionResolver).resolveException(request, response, null, erro);
		verifyNoInteractions(filterChain);
		assertNull(request.getAttribute(JwtFilter.ATRIBUTO_MOTIVO_RECUSA));
		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

}
