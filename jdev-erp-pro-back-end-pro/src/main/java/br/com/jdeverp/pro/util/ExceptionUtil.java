package br.com.jdeverp.pro.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

public class ExceptionUtil {

	private static final Pattern UNIQUE_PATTERN = Pattern.compile("Key \\((.*?)\\)=\\((.*?)\\)");
	private static final Pattern FK_PATTERN = Pattern.compile("(?:still referenced from table|ainda é referenciada pela tabela) \"(.*?)\"", Pattern.CASE_INSENSITIVE);

	/**
	 * Extrai mensagens de validações em Bean Validator
	 * 
	 * @param ex
	 * @return String
	 */
	public static String getMensagemAmigavel(Throwable ex) {

		String msg = getRootCause(ex);

		if (msg == null || msg.isBlank()) {
			return "Ocorreu um erro ao processar a operação.";
		}
		
		Matcher fk = FK_PATTERN.matcher(msg);
		if (fk.find()) {

			String tabela = fk.group(1);

			return "Este cadastro não pode ser removido porque está sendo utilizado por " + tabela.replace("_", " ") + ".";
		}
		
		Matcher matcher = UNIQUE_PATTERN.matcher(msg);
		if (matcher.find()) {

			String campo = matcher.group(1);
			String valor = matcher.group(2);

			return String.format(msg + " -> campo: %s valor: '%s'.", campo, valor);
		}

		return msg;
	}

	/**
	 * Extrai mensagem de validações de constraint do bean validator
	 * 
	 * @param ex
	 * @return String
	 */
	public static String getMensagemValidacaoConstraint(Throwable ex) {

		Throwable causa = ex;

		while (causa != null) {

			if (causa instanceof ConstraintViolationException) {

				ConstraintViolationException cve = (ConstraintViolationException) causa;

				return cve.getConstraintViolations().stream().map(ConstraintViolation::getMessage)
						.collect(Collectors.joining(", "));

			} else if (causa instanceof PSQLException || causa instanceof DataIntegrityViolationException) {
				return getMensagemAmigavel(ex);
			} else if (causa instanceof EmptyResultDataAccessException) {
				return getMensagemObsInexistente(ex);
			}else if (causa instanceof MethodArgumentTypeMismatchException) {
				return getMensagemObsInexistente(ex);
			}

			causa = causa.getCause();
		}

		return ex.getMessage();
	}

	/**
	 * Extrai a mensagem de qunado o objeto já foi deletado e não existe mais no
	 * banco de dados
	 * 
	 * @param ex
	 * @return String
	 */
	public static String getMensagemObsInexistente(Throwable ex) {

		String msg = getRootCause(ex);

		Pattern pattern = Pattern.compile("No class (.*?) entity with id (\\d+) exists!");

		Matcher matcher = pattern.matcher(msg);

		if (matcher.find()) {

			String entidadeCompleta = matcher.group(1);
			String id = matcher.group(2);

			String entidade = entidadeCompleta.substring(entidadeCompleta.lastIndexOf(".") + 1);

			return String.format("%s com ID %s não foi encontrado.", entidade, id);
		}

		return msg;
	}

	/**
	 * Extrai as mensagem de parametros que deveriam estar sendo enviados e não estãoi presentes na requisição
	 * @param ex
	 * @return String
	 */
	public static String getMensagemParametros(Throwable ex) {

		String msg = getRootCause(ex);

		if (msg == null || msg.isBlank()) {
			return "Erro interno do sistema.";
		}

		Matcher matcher;

		matcher = Pattern.compile("Required request parameter '(.*?)' .*").matcher(msg);

		if (matcher.find()) {
			return "O parâmetro '" + matcher.group(1) + "' é obrigatório.";
		}

		matcher = Pattern.compile("No class .*\\.(.*?) entity with id (\\d+) exists!").matcher(msg);

		if (matcher.find()) {
			return matcher.group(1) + " com código " + matcher.group(2) + " não foi encontrado.";
		}

		matcher = Pattern.compile("Key \\((.*?)\\)=\\((.*?)\\)").matcher(msg);

		if (matcher.find()) {
			return "Já existe um registro com " + matcher.group(1) + " '" + matcher.group(2) + "'.";
		}

		return msg;
	}

	private static String getRootCause(Throwable ex) {

		Throwable causa = ex;

		while (causa.getCause() != null) {
			causa = causa.getCause();
		}

		return causa.getMessage();
	}
}