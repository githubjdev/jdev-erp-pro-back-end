package br.com.jdeverp.pro.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.service.JwtService;
import br.com.jdeverp.pro.service.UsuarioDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
	
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private UsuarioDetailsService userService;

	/*HttpServletRequest = vem todos os dados da requisição (Dados da tela)*/
	/*HttpServletResponse = resposta que será dada para o usuário*/
	/*FilterChain = Classe do filtro SPring*/
	@Override
	protected void doFilterInternal(HttpServletRequest request, 
			HttpServletResponse response, 
			FilterChain filterChain)
			throws ServletException, IOException {
		
		try {
			
			String header = request.getHeader("Authorization");
			
			/*Se não tem token o acesso pode ser pubico e será valido pelo Spring Security*/
			if(header == null || !header.startsWith("Bearer ")) {
				filterChain.doFilter(request, response);
				return;
			}
			
			String token = header.substring(7);
			
			if (token.isBlank()) {
				filterChain.doFilter(request, response);
				return;
			}
			
			/*Token pode esar inválido mas o user pode acessar a parte de login*/
			if(!jwtService.validarToken(token)) {
				filterChain.doFilter(request, response); 
				return;
			}
			
			String login = jwtService.extrairLogin(token);
			
			/*Vamos fazer a autenticação se o usuário não estiver logado*/
			if (login != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				
				/*Busca no banco o usuário*/
				UserDetails userDetails = userService.loadUserByUsername(login);
				
				/*Crie o objeto pra carregar o user*/
				UsuarioAutenticado principal = new UsuarioAutenticado((Usuario)userDetails);
				
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
			
		}catch (Exception e) {
			e.printStackTrace();
			SecurityContextHolder.clearContext();
			throw new MsgApiException("Erro ao validar JWT do Usuário no Sistema.");
		}
		
		/*Continua para o back-end*/
		filterChain.doFilter(request, response);
		
	}

}
