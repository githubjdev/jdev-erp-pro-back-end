package br.com.jdeverp.pro.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.NoHandlerFoundException;

import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.security.JwtAccessDeniedHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Cada handler do {@link GlobalExceptionHandler}, chamado direto: status, título, mensagem e caminho do ResponseApi. */
public class TestGlobalExceptionHandler {

	private static final String CAMINHO = "/jdev-erp-pro-back-end-dev/api/categoria/salvar";

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	private final MockHttpServletRequest request = new MockHttpServletRequest("POST", CAMINHO);

	@AfterEach
	public void limparContexto() {
		SecurityContextHolder.clearContext();
	}

	private static ResponseApi conferir(ResponseEntity<ResponseApi> resposta, HttpStatus status) {
		assertEquals(status.value(), resposta.getStatusCode().value());
		assertEquals(MediaType.APPLICATION_JSON, resposta.getHeaders().getContentType());
		ResponseApi corpo = resposta.getBody();
		assertNotNull(corpo);
		assertEquals(status.value(), corpo.getStatus());
		assertEquals(CAMINHO, corpo.getPath());
		assertNotNull(corpo.getLocalDateTime());
		return corpo;
	}

	@Test
	public void testErroGeralMsgApiException() {
		ResponseApi corpo = conferir(handler.erroGeralMsgApiException(new MsgApiException("Categoria não encontrada."), request),
				HttpStatus.BAD_REQUEST);

		assertEquals("Bad Request", corpo.getError());
		assertEquals("Categoria não encontrada.", corpo.getMessage());
	}

	@Test
	public void testErroGeralMsgApiExceptionComOutroStatus() {
		ResponseApi corpo = conferir(handler.erroGeralMsgApiException(
				new MsgApiException("Apenas administradores.", HttpStatus.FORBIDDEN), request), HttpStatus.FORBIDDEN);

		assertEquals("Forbidden", corpo.getError());
	}

	@Test
	public void testAcessoNegado() {
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("joao", "token"));

		ResponseApi corpo = conferir(handler.acessoNegado(new AccessDeniedException("negado"), request), HttpStatus.FORBIDDEN);

		/* Mesma mensagem do 403 do filtro de segurança */
		assertEquals(JwtAccessDeniedHandler.MSG_SEM_PERMISSAO, corpo.getMessage());
		assertEquals("Forbidden", corpo.getError());
	}

	@Test
	public void testAcessoNegadoSemAutenticacao() {
		ResponseApi corpo = conferir(handler.acessoNegado(new AccessDeniedException("negado"), request), HttpStatus.FORBIDDEN);

		assertEquals(JwtAccessDeniedHandler.MSG_SEM_PERMISSAO, corpo.getMessage());
	}

	@Test
	public void testErroUserNaoEncontradoException() {
		ResponseApi corpo = conferir(handler.erroUserNaoEncontradoException(
				new UsernameNotFoundException("Usuário não encontrado no banco de dados."), request), HttpStatus.UNAUTHORIZED);

		assertEquals("Usuário não pode ser autenticado.", corpo.getError());
		assertEquals("Usuário não encontrado no banco de dados.", corpo.getMessage());
	}

	@Test
	public void testErroGeralRuntimeMetodoNaoSuportado() {
		ResponseApi corpo = conferir(handler.erroGeralRuntime(new HttpRequestMethodNotSupportedException("DELETE"), request),
				HttpStatus.METHOD_NOT_ALLOWED);

		assertEquals("Erro de chamada ao método", corpo.getError());
		assertTrue(corpo.getMessage().startsWith("Método não permitido ou inválido: -> "), corpo.getMessage());
		assertTrue(corpo.getMessage().contains("DELETE"), corpo.getMessage());
	}

	@Test
	public void testHandlerNotFound() {
		ResponseApi corpo = conferir(handler.handlerNotFound(new NoHandlerFoundException("GET", "/api/nada", HttpHeaders.EMPTY),
				request), HttpStatus.NOT_FOUND);

		assertEquals("URL Inválida", corpo.getError());
		assertEquals("Endpoint não encontrado ou parâmetro obrigatório não informado.", corpo.getMessage());
	}

	@Test
	public void testTratarDataIntegrityViolation() {
		DataIntegrityViolationException erro = new DataIntegrityViolationException("could not execute statement",
				new RuntimeException("ERROR: duplicate key value violates unique constraint \"unique_email\"\n"
						+ "  Detalhe: Key (email)=(maria@teste.com.br) already exists."));

		ResponseApi corpo = conferir(handler.tratarDataIntegrityViolation(erro, request), HttpStatus.BAD_REQUEST);

		assertEquals("Erro de integridade de dados.", corpo.getError());
		assertTrue(corpo.getMessage().endsWith("-> campo: email valor: 'maria@teste.com.br'."), corpo.getMessage());
	}

	@Test
	public void testErroGeralRuntimeParametroObrigatorio() {
		ResponseApi corpo = conferir(handler.erroGeralRuntime(new MissingServletRequestParameterException("nome", "String"), request),
				HttpStatus.BAD_REQUEST);

		assertEquals("Payload e demais dados não foram enviados corretamente.", corpo.getError());
		assertEquals("O parâmetro 'nome' é obrigatório.", corpo.getMessage());
	}

	@Test
	public void testErroGeralRuntimeCorpoAusente() {
		HttpMessageNotReadableException erro = new HttpMessageNotReadableException(
				"Required request body is missing: public ResponseEntity salvar(Categoria)", new MockHttpInputMessage(new byte[0]));

		ResponseApi corpo = conferir(handler.erroGeralRuntime(erro, request), HttpStatus.BAD_REQUEST);

		assertEquals("Os dados da requisição não foram enviados.", corpo.getMessage());
	}

	@Test
	public void testErroGeralRuntimeJsonInvalido() {
		HttpMessageNotReadableException erro = new HttpMessageNotReadableException("JSON parse error: Unexpected character",
				new MockHttpInputMessage(new byte[0]));

		ResponseApi corpo = conferir(handler.erroGeralRuntime(erro, request), HttpStatus.BAD_REQUEST);

		/* Sem detalhe do parser na resposta */
		assertEquals("", corpo.getMessage());
	}

	@Test
	public void testLockVersion() {
		ResponseApi corpo = conferir(handler.lockVersion(new ObjectOptimisticLockingFailureException(Categoria.class, 1L), request),
				HttpStatus.BAD_REQUEST);

		assertEquals("Uma outra atualização foi identificada pelo sistema para esse cadastro.", corpo.getError());
	}

	@Test
	public void testErroGeralRuntime() {
		ResponseApi corpo = conferir(handler.erroGeralRuntime(new IllegalStateException("Falha inesperada"), request),
				HttpStatus.INTERNAL_SERVER_ERROR);

		assertEquals("Erro geral ocorrido no sistema.", corpo.getError());
		assertEquals("Falha inesperada", corpo.getMessage());
	}

	@Test
	public void testErroGeralRuntimeRecursoEstaticoInexistente() {
		ResponseApi corpo = conferir(handler.erroGeralRuntime(new RuntimeException("No static resource api/nada."), request),
				HttpStatus.INTERNAL_SERVER_ERROR);

		assertEquals("URl ou caminho não existe no Controller. No static resource api/nada.", corpo.getMessage());
	}

	/** Comportamento atual: violação de Bean Validation fora do @Valid responde 500 (dívida registrada em analise-projeto.md §10). */
	@Test
	public void testErroGeralConstrainBv() {
		Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
		Set<ConstraintViolation<Categoria>> violacoes = validator.validate(new Categoria());

		ResponseApi corpo = conferir(handler.erroGeralConstrainBv(new ConstraintViolationException(violacoes), request),
				HttpStatus.INTERNAL_SERVER_ERROR);

		assertEquals("Mensagem do sistema.", corpo.getError());
		assertTrue(!corpo.getMessage().isBlank(), "mensagens de validação da Categoria");
	}

	@Test
	public void testMethodArgumentNotValidException() throws Exception {
		BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Categoria(), "categoria");
		resultado.addError(new ObjectError("categoria", "Nome da categoria deve ser informado"));
		resultado.addError(new ObjectError("categoria", "Empresa deve ser informada"));
		MethodParameter parametro = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);

		ResponseApi corpo = conferir(handler.methodArgumentNotValidException(new MethodArgumentNotValidException(parametro, resultado),
				request), HttpStatus.BAD_REQUEST);

		assertEquals("Valores não correspodem e não passaram nas validações do sistema.", corpo.getError());
		assertEquals("Nome da categoria deve ser informado, Empresa deve ser informada", corpo.getMessage());
	}

}
