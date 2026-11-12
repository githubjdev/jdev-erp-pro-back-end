package br.com.jdeverp.pro.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.service.JwtService;
import br.com.jdeverp.pro.service.UsuarioDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica a requisição pelo token JWT do header {@code Authorization}.
 *
 * <ul>
 * <li>Token válido: o usuário entra no {@code SecurityContextHolder}.</li>
 * <li>Token recusado (inválido, expirado, sessão encerrada, usuário ou empresa
 * bloqueados): a requisição segue sem autenticação e o motivo fica no
 * atributo {@link #ATRIBUTO_MOTIVO_RECUSA}. Rota protegida responde 401 pelo
 * {@link JwtAuthenticationEntryPoint}, com esse motivo; a rota de login
 * continua aberta.</li>
 * <li>Erro que não é de autenticação (ex.: banco fora do ar): responde pelo
 * {@code GlobalExceptionHandler}, como nos controllers.</li>
 * </ul>
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

	/** Atributo da requisição com o motivo da recusa do token, lido pelo {@link JwtAuthenticationEntryPoint}. */
	public static final String ATRIBUTO_MOTIVO_RECUSA = JwtFilter.class.getName() + ".motivoRecusa";

	public static final String MSG_TOKEN_INVALIDO = "Token de acesso inválido.";
	public static final String MSG_TOKEN_EXPIRADO = "Token de acesso expirado, faça login novamente.";

	@Autowired
	private JwtService jwtService;

	@Autowired
	private UsuarioDetailsService userService;

	@Autowired
	@Qualifier("handlerExceptionResolver")
	private HandlerExceptionResolver handlerExceptionResolver;

	/*HttpServletRequest = vem todos os dados da requisição (Dados da tela)*/
	/*HttpServletResponse = resposta que será dada para o usuário*/
	/*FilterChain = Classe do filtro SPring*/
	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain)
			throws ServletException, IOException {

		String header = request.getHeader("Authorization");

		/*Sem token o Spring Security decide: o login é público, as outras rotas respondem 401*/
		if (header == null || !header.startsWith("Bearer ") || header.substring(7).isBlank()) {
			filterChain.doFilter(request, response);
			return;
		}

		if (SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				autenticar(header.substring(7), request);
			} catch (ExpiredJwtException e) {
				recusar(request, MSG_TOKEN_EXPIRADO, e);
			} catch (JwtException | IllegalArgumentException e) {
				recusar(request, MSG_TOKEN_INVALIDO, e);
			} catch (MsgApiException | AuthenticationException e) {
				/*Recusas do UsuarioDetailsService: sessão encerrada, usuário não encontrado, usuário ou empresa bloqueados*/
				recusar(request, e.getMessage(), e);
			} catch (Exception e) {
				/*Não é falha de autenticação (ex.: banco fora do ar): erro do servidor, pelo GlobalExceptionHandler*/
				SecurityContextHolder.clearContext();
				handlerExceptionResolver.resolveException(request, response, null, e);
				return;
			}
		}

		/*Continua para o back-end*/
		filterChain.doFilter(request, response);
	}

	private void autenticar(String token, HttpServletRequest request) {

		/*Confere assinatura e validade: lança ExpiredJwtException ou outra JwtException se o token não vale*/
		String login = jwtService.extrairLogin(token);

		if (login == null) {
			throw new JwtException("Token sem login.");
		}

		/*Só vale o token da sessão mais recente, gravado no usuário*/
		userService.existsByTokenParaUser(token);

		/*Busca no banco o usuário (confere liberado e empresa.bloqueio)*/
		UserDetails userDetails = userService.loadUserByUsername(login);

		/*Crie o objeto pra carregar o user*/
		UsuarioAutenticado principal = new UsuarioAutenticado((Usuario) userDetails);

		/*Cria objeto de autenticação*/
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
				principal,
				token,
				principal.getAuthorities());

		/*Adiciona essa autenticação para a requisição*/
		authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

		/*Estabelece a autenticação do user para nosso spring security*/
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private void recusar(HttpServletRequest request, String motivo, Exception e) {
		SecurityContextHolder.clearContext();
		request.setAttribute(ATRIBUTO_MOTIVO_RECUSA, motivo);

		/*Sem o token e sem a mensagem da exceção no log*/
		log.warn("Token recusado [{} {}]: {} ({})", request.getMethod(), request.getRequestURI(), motivo,
				e.getClass().getSimpleName());
	}

}
