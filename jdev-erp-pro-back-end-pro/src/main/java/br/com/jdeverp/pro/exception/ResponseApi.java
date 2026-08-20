package br.com.jdeverp.pro.exception;

import java.util.Date;

/**
 * Classe de resposta de erro da API
 * 
 * @author jdeverp
 *
 */

public class ResponseApi {

	private Date localDateTime;
	private int status;
	private String error;
	private String message;
	private String path;

	public ResponseApi(Date localDateTime, int status, String error, String message, String path) {
		super();
		this.localDateTime = localDateTime;
		this.status = status;
		this.error = error;
		this.message = message;
		this.path = path;
	}

	public Date getLocalDateTime() {
		return localDateTime;
	}

	public int getStatus() {
		return status;
	}

	public String getError() {
		return error;
	}

	public String getMessage() {
		return message;
	}

	public String getPath() {
		return path;
	}

}
