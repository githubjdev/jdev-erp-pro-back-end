package br.com.jdeverp.pro.exception;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import br.com.jdeverp.pro.util.ExceptionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

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
												"Usuário não pode ser autenticado.", 
												ex.getMessage(), 
												request.getRequestURI());
		
		return ResponseEntity.
				status(HttpStatus.UNAUTHORIZED.value())
				.contentType(MediaType.APPLICATION_JSON)
				.body(responseApi);
		
	}
	
	
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ResponseApi> erroGeralRuntime(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

		ResponseApi responseApi = new ResponseApi(new Date(),
										HttpStatus.METHOD_NOT_ALLOWED.value(),
				                        "Erro de chamada ao método",
										"Método não permitido ou inválido: -> " + ex.getMessage(), 
										request.getRequestURI());

		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				             .contentType(MediaType.APPLICATION_JSON)
				             .body(responseApi);
	}
	
	
	
	
	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ResponseApi> handlerNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
		
		
		ResponseApi responseApi = new ResponseApi(new Date(),
				                                  HttpStatus.NOT_FOUND.value(),
								                 "URL Inválida",
								                 "Endpoint não encontrado ou parâmetro obrigatório não informado.", 
								                  request.getRequestURI());
										
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				             .contentType(MediaType.APPLICATION_JSON)
				             .body(responseApi);

	}
	
	
	
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ResponseApi> tratarDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {

		ResponseApi responseApi = new ResponseApi(new Date(),
												HttpStatus.BAD_REQUEST.value(),
				                             	"Erro de integridade de dados.", 
				                             	ExceptionUtil.getMensagemValidacaoConstraint(ex), 
				                             	request.getRequestURI());

		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(responseApi);

	}
	
	
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ResponseApi> erroGeralRuntime(MissingServletRequestParameterException ex, HttpServletRequest request) {
		
		
		ResponseApi responseApi = new ResponseApi(new Date(),
												  HttpStatus.BAD_REQUEST.value(),
									              "Payload e demais dados não foram enviados corretamente.", 
									              ExceptionUtil.getMensagemParametros(ex), 
									              request.getRequestURI());

		
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(responseApi);
	}
	
	
	/* Para erro que não estamos esperando RuntimeException*/
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ResponseApi> erroGeralRuntime(HttpMessageNotReadableException ex, HttpServletRequest request) {

		StringBuilder msgErro = new StringBuilder();

		if (ex.getMessage().startsWith("Required request body is missing")) {
			msgErro.append("Os dados da requisição não foram enviados.");
		}
		
		ResponseApi responseApi = new ResponseApi(new Date(),
												  HttpStatus.BAD_REQUEST.value(),
									              "Payload e demais dados não foram enviados corretamente.", 
									              msgErro.toString(), 
									              request.getRequestURI());

		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(responseApi);
	}
	
	
	
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ResponseApi> lockVersion(ObjectOptimisticLockingFailureException  ex, HttpServletRequest request) {
		
		
		ResponseApi responseApi = new ResponseApi(new Date(), HttpStatus.BAD_REQUEST.value(),
	              "Uma outra atualização foi identificada pelo sistema para esse cadastro.", 
	              "Este registro foi alterado por outro usuário. Consule novamente os dados, atualize e salve suas alterações.", 
	              request.getRequestURI());
		
		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(responseApi);
	}
	
	
	@ExceptionHandler({RuntimeException.class, Exception.class})
	public ResponseEntity<ResponseApi> erroGeralRuntime(RuntimeException ex, HttpServletRequest request) {
		
		
		ResponseApi responseApi = new ResponseApi(new Date(),HttpStatus.INTERNAL_SERVER_ERROR.value(),
								                 "Erro geral ocorrido no sistema.", 
								                 ExceptionUtil.getMensagemValidacaoConstraint(ex), 
								                 request.getRequestURI());

		return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON).body(responseApi);
	}
	
	
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ResponseApi> erroGeralConstrainBv(ConstraintViolationException ex, HttpServletRequest request) {
		
		ResponseApi responseApi = new ResponseApi(new Date(),HttpStatus.INTERNAL_SERVER_ERROR.value(),
								                "Mensagem do sistema.", 
								                ExceptionUtil.getMensagemValidacaoConstraint(ex), 
								                request.getRequestURI());

		
		return ResponseEntity.internalServerError().contentType(MediaType.APPLICATION_JSON)
				                                    .body(responseApi);
	}
	
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ResponseApi> methodArgumentNotValidException(MethodArgumentNotValidException ex, HttpServletRequest request) {

		List<String> lista = new ArrayList<String>();

		for (ObjectError erro : ex.getAllErrors()) {
			lista.add(erro.getDefaultMessage());
		}
		
		ResponseApi responseApi = new ResponseApi(new Date(), HttpStatus.BAD_REQUEST.value(),
								                 "Valores não correspodem e não passaram nas validações do sistema.", 
								                  String.join(", ", lista), 
								                  request.getRequestURI());

		return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(responseApi);
	}
	
	
	
	

}
