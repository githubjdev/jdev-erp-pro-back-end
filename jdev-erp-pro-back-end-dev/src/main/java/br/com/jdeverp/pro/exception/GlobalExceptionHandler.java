package br.com.jdeverp.pro.exception;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(MsgApiException.class)
	public ResponseEntity<ResponseApi> erroGeralMsgApiException(MsgApiException ex,  
			HttpServletRequest request ){
		
		
		ResponseApi responseApi = new ResponseApi(new Date(),
												ex.getStatus().value(), 
												ex.getStatus().getReasonPhrase(), 
												ex.getMessage(), 
												request.getRequestURI());
		
		return ResponseEntity.
				status(ex.getStatus())
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseApi);
		
	}
	
	
	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<ResponseApi> erroUserNaoEncontradoException(UsernameNotFoundException ex,  
			HttpServletRequest request ){
		
		
		ResponseApi responseApi = new ResponseApi(new Date(),
												HttpStatus.UNAUTHORIZED.value(), 
												"Usuário não pode ser autenticado", 
												ex.getMessage(), 
												request.getRequestURI());
		
		return ResponseEntity.
				status(HttpStatus.UNAUTHORIZED.value())
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseApi);
		
	}
	
	
	

}
