package br.com.jdeverp.pro.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import br.com.jdeverp.pro.contexto.TestContextoApi;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Autenticação de ponta a ponta: login, JwtFilter, token de sessão e
 * checagem de administrador.
 *
 * Token recusado responde 401 pelo {@code JwtAuthenticationEntryPoint}, com o
 * motivo na mensagem. A rota de login continua aberta mesmo com um token
 * recusado no cabeçalho.
 */
public class TestJwtFilter extends TestContextoApi {

	private static final String LOGIN = "/api/usuario/login";
	private static final String ROTA_PROTEGIDA = "/api/categoria/total";
	private static final String SEM_AUTENTICACAO = "Usuário não autenticado ou token inválido";
	private static final String TOKEN_INVALIDO = "Token de acesso inválido.";
	private static final String TOKEN_EXPIRADO = "Token de acesso expirado, faça login novamente.";
	private static final String SESSAO_ENCERRADA = "Sessão encerrada ou substituída por um novo login, faça login novamente.";

	@Value("${jwt.secret}")
	private String segredoJwt;

	@Test
	@Order(1)
	public void testLoginComSucesso() throws Exception {
		String novoToken = logar(admin.login(), SENHA);
		assertFalse(novoToken.isBlank());
		token = novoToken;

		get(ROTA_PROTEGIDA).andExpect(status().isOk());
	}

	@Test
	@Order(2)
	public void testLoginSenhaErrada() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content(json(mapa("login", admin.login(), "senha", "senha-errada"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Senha digitada é inválida."));
	}

	@Test
	@Order(3)
	public void testLoginUsuarioInexistente() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content(json(mapa("login", "nao.existe." + sufixo, "senha", SENHA))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Usuário não encontrado."));
	}

	@Test
	@Order(4)
	public void testLoginSemDados() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content(json(mapa("login", "", "senha", ""))))
				.andExpect(status().isBadRequest());
	}

	@Test
	@Order(5)
	public void testLoginUsuarioBloqueado() throws Exception {
		UsuarioTeste bloqueado = criarUsuario("Bloqueado", false);
		executarSql("update usuario set liberado = false where id = ?", bloqueado.usuarioId());

		mockMvc.perform(MockMvcRequestBuilders.post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content(json(mapa("login", bloqueado.login(), "senha", SENHA))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Usuário bloqueado, entre em contato com o administrador do sistema."));
	}

	@Test
	@Order(6)
	public void testLoginEmpresaBloqueada() throws Exception {
		Long empresaBloqueada = criarEmpresa();
		UsuarioTeste usuario = criarUsuario("EmpresaBloqueada", false, empresaBloqueada);
		executarSql("update empresa set bloqueio = true where id = ?", empresaBloqueada);

		mockMvc.perform(MockMvcRequestBuilders.post(LOGIN).contentType(MediaType.APPLICATION_JSON)
				.content(json(mapa("login", usuario.login(), "senha", SENHA))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Empresa bloqueada, entre em contato com o administrador do sistema."));
	}

	@Test
	@Order(7)
	public void testRequisicaoSemToken() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get(ROTA_PROTEGIDA))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(SEM_AUTENTICACAO));
	}

	@Test
	@Order(8)
	public void testRequisicaoComTokenInvalido() throws Exception {
		esperar401(comCabecalho("Bearer token-invalido"), TOKEN_INVALIDO);
	}

	@Test
	@Order(9)
	public void testTokenDeSessaoAnteriorNaoValeDepoisDeNovoLogin() throws Exception {
		UsuarioTeste usuario = criarUsuario("DuasSessoes", false);
		String primeiroToken = logar(usuario.login(), SENHA);
		Thread.sleep(1100); /* o token leva a data de emissão em segundos */
		String segundoToken = logar(usuario.login(), SENHA);
		assertNotEquals(primeiroToken, segundoToken);

		comToken(segundoToken, MockMvcRequestBuilders.get(ROTA_PROTEGIDA), null).andExpect(status().isOk());

		esperar401(comToken(primeiroToken, MockMvcRequestBuilders.get(ROTA_PROTEGIDA), null), SESSAO_ENCERRADA);
	}

	@Test
	@Order(10)
	public void testUsuarioSemRoleAdminNaoCadastraEmpresa() throws Exception {
		UsuarioTeste comum = criarUsuario("Comum", false);
		String tokenComum = logar(comum.login(), SENHA);

		comToken(tokenComum, MockMvcRequestBuilders.post("/api/empresa/salvar"),
				mapa("plano", ref(1L), "pessoa", ref(comum.pessoaId()), "logoMarca", "logo"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(JwtAccessDeniedHandler.MSG_SEM_PERMISSAO));
	}

	@Test
	@Order(11)
	public void testRequisicaoComTokenExpirado() throws Exception {
		long agora = System.currentTimeMillis();
		String expirado = Jwts.builder().subject(admin.login()).claim("usuarioId", admin.usuarioId())
				.claim("empresaId", empresaId).claim("login", admin.login())
				.issuedAt(new Date(agora - 120_000)).expiration(new Date(agora - 60_000))
				.signWith(Keys.hmacShaKeyFor(segredoJwt.getBytes(StandardCharsets.ISO_8859_1))).compact();

		esperar401(comToken(expirado, MockMvcRequestBuilders.get(ROTA_PROTEGIDA), null), TOKEN_EXPIRADO);
	}

	@Test
	@Order(12)
	public void testRequisicaoComTokenAssinadoPorOutraChave() throws Exception {
		String falsificado = Jwts.builder().subject(admin.login()).claim("usuarioId", admin.usuarioId())
				.claim("empresaId", empresaId).claim("login", admin.login()).issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 60_000))
				.signWith(Jwts.SIG.HS256.key().build()).compact();

		esperar401(comToken(falsificado, MockMvcRequestBuilders.get(ROTA_PROTEGIDA), null), TOKEN_INVALIDO);
	}

	@Test
	@Order(13)
	public void testUsuarioBloqueadoDepoisDoLogin() throws Exception {
		UsuarioTeste usuario = criarUsuario("BloqueadoLogado", false);
		String tokenUsuario = logar(usuario.login(), SENHA);
		executarSql("update usuario set liberado = false where id = ?", usuario.usuarioId());

		esperar401(comToken(tokenUsuario, MockMvcRequestBuilders.get(ROTA_PROTEGIDA), null),
				"Usuário bloqueado, entre em contato com o administrador do sistema.");
	}

	@Test
	@Order(14)
	public void testEmpresaBloqueadaDepoisDoLogin() throws Exception {
		Long outraEmpresa = criarEmpresa();
		UsuarioTeste usuario = criarUsuario("EmpresaBloqueadaLogado", false, outraEmpresa);
		String tokenUsuario = logar(usuario.login(), SENHA);
		executarSql("update empresa set bloqueio = true where id = ?", outraEmpresa);

		esperar401(comToken(tokenUsuario, MockMvcRequestBuilders.get(ROTA_PROTEGIDA), null),
				"Empresa bloqueada, entre em contato com o administrador do sistema.");
	}

	@Test
	@Order(15)
	public void testLoginComTokenRecusadoNoCabecalho() throws Exception {
		/* O front pode mandar o token vencido junto no login: o login continua funcionando */
		UsuarioTeste usuario = criarUsuario("LoginTokenVelho", false);

		mockMvc.perform(MockMvcRequestBuilders.post(LOGIN).header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido")
				.contentType(MediaType.APPLICATION_JSON).content(json(mapa("login", usuario.login(), "senha", SENHA))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty());
	}

	private ResultActions comCabecalho(String authorization) throws Exception {
		return mockMvc.perform(MockMvcRequestBuilders.get(ROTA_PROTEGIDA).header(HttpHeaders.AUTHORIZATION, authorization));
	}

	private static void esperar401(ResultActions resultado, String mensagem) throws Exception {
		resultado.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value(mensagem))
				.andExpect(jsonPath("$.path").value(ROTA_PROTEGIDA));
	}

}
