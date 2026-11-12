package br.com.jdeverp.pro.security;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import br.com.jdeverp.pro.exception.ResponseApi;
import br.com.jdeverp.pro.util.ObjectMapperUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
	
	@Autowired
	private ObjectMapperUtil objectMapperUtil;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");

		/*Motivo deixado pelo JwtFilter quando recusou o token (expirado, inválido, sessão encerrada...)*/
		Object motivoRecusa = request.getAttribute(JwtFilter.ATRIBUTO_MOTIVO_RECUSA);
		String mensagem = motivoRecusa != null ? motivoRecusa.toString() : "Usuário não autenticado ou token inválido";

		ResponseApi responseApi = new ResponseApi(new Date(),
													401,
													"Unauthorized",
													mensagem,
													request.getRequestURI());
		
		response.getWriter().write(objectMapperUtil.objetoParaJson(responseApi));
		
	}

}
