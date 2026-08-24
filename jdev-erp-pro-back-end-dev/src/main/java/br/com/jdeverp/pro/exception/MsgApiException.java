package br.com.jdeverp.pro.exception;

import org.springframework.http.HttpStatus;

/**
 * Classe de exceção da API
 * 
 * @author jdeverp
 *
 */
public class MsgApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private HttpStatus status;

	public MsgApiException(String message, HttpStatus status) {
		super(message);
		this.status = status;
	}

	public MsgApiException(String message) {
		super(message);
		this.status = HttpStatus.BAD_REQUEST;
	}

	public HttpStatus getStatus() {
		return status;
	}

}
