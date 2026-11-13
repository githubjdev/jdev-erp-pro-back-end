package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;

/** Geração e leitura do JWT, sem Spring: a chave e a expiração entram por reflexão, como faria o @Value. */
public class TestJwtService {

	private static final String SEGREDO = "segredo-de-teste-com-pelo-menos-64-caracteres-para-o-HS512-0123456789";

	private JwtService jwtService;

	@BeforeEach
	public void configurar() {
		jwtService = new JwtService();
		ReflectionTestUtils.setField(jwtService, "secret", SEGREDO);
		ReflectionTestUtils.setField(jwtService, "expiration", 60_000L);
	}

	@AfterEach
	public void limparContexto() {
		SecurityContextHolder.clearContext();
	}

	private static Usuario usuario() {
		Empresa empresa = new Empresa();
		empresa.setId(10L);
		Usuario usuario = new Usuario();
		usuario.setId(7L);
		usuario.setLogin("maria@teste.com.br");
		usuario.setEmpresa(empresa);
		return usuario;
	}

	@Test
	public void testGerarToken() {
		String token = jwtService.gerarToken(usuario());

		Claims claims = jwtService.extrairClaims(token);

		assertEquals("maria@teste.com.br", claims.getSubject());
		assertEquals("maria@teste.com.br", claims.get("login", String.class));
		assertEquals(7L, claims.get("usuarioId", Long.class));
		assertEquals(10L, claims.get("empresaId", Long.class));
		/* Expira em jwt.expiration milissegundos */
		long validade = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
		assertTrue(validade >= 59_000 && validade <= 61_000, "validade: " + validade);
	}

	@Test
	public void testExtrairEmpresaId() {
		assertEquals(10L, jwtService.extrairEmpresaId(jwtService.gerarToken(usuario())));
	}

	@Test
	public void testExtrairUsuarioId() {
		assertEquals(7L, jwtService.extrairUsuarioId(jwtService.gerarToken(usuario())));
	}

	@Test
	public void testExtrairLogin() {
		assertEquals("maria@teste.com.br", jwtService.extrairLogin(jwtService.gerarToken(usuario())));
	}

	@Test
	public void testExtrairDoTokenDaRequisicao() {
		/* O JwtFilter guarda o token nas credenciais da autenticação */
		String token = jwtService.gerarToken(usuario());
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("maria", token));

		assertEquals(10L, jwtService.extrairEmpresaId());
		assertEquals(7L, jwtService.extrairUsuarioId());
	}

	@Test
	public void testExtrairClaimsTokenExpirado() {
		ReflectionTestUtils.setField(jwtService, "expiration", -1_000L);
		String expirado = jwtService.gerarToken(usuario());

		assertThrows(ExpiredJwtException.class, () -> jwtService.extrairClaims(expirado));
	}

	@Test
	public void testExtrairClaimsAssinadoComOutraChave() {
		String falsificado = Jwts.builder()
				.subject("maria@teste.com.br")
				.claim("usuarioId", 7L)
				.claim("empresaId", 99L)
				.expiration(new Date(System.currentTimeMillis() + 60_000))
				.signWith(Keys.hmacShaKeyFor(("outra-chave-" + SEGREDO).getBytes(StandardCharsets.ISO_8859_1)))
				.compact();

		/* Trocar a empresa no token exige a chave do servidor */
		assertThrows(JwtException.class, () -> jwtService.extrairEmpresaId(falsificado));
	}

	@Test
	public void testExtrairClaimsTokenMalformado() {
		assertThrows(MalformedJwtException.class, () -> jwtService.extrairClaims("nao.e.um-jwt"));
	}

}
