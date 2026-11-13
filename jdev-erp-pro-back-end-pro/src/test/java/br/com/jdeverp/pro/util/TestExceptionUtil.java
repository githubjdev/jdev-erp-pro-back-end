package br.com.jdeverp.pro.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.PSQLState;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import br.com.jdeverp.pro.model.Categoria;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;

/** Tradução das mensagens de erro do banco e do Spring para o usuário. */
public class TestExceptionUtil {

	private static final String UNIQUE = "ERROR: duplicate key value violates unique constraint \"unique_email\"\n"
			+ "  Detail: Key (email)=(maria@teste.com.br) already exists.";

	/** Exceção do Spring com a do banco como causa, como chega no GlobalExceptionHandler. */
	private static RuntimeException comCausa(String mensagemDoBanco) {
		return new DataIntegrityViolationException("could not execute statement", new RuntimeException(mensagemDoBanco));
	}

	@Test
	public void testGetMensagemAmigavelUnique() {
		assertEquals(UNIQUE + " -> campo: email valor: 'maria@teste.com.br'.", ExceptionUtil.getMensagemAmigavel(comCausa(UNIQUE)));
	}

	@Test
	public void testGetMensagemAmigavelUniqueComPorcentoNoValor() {
		String mensagem = "ERROR: duplicate key value violates unique constraint \"unique_nome_produto_empresa\"\n"
				+ "  Detail: Key (nome, empresa_id)=(Desconto 50% off, 1) already exists.";

		/* O valor vem do usuário: um "%" nele não pode quebrar a montagem da mensagem */
		assertEquals(mensagem + " -> campo: nome, empresa_id valor: 'Desconto 50% off, 1'.",
				ExceptionUtil.getMensagemAmigavel(comCausa(mensagem)));
	}

	@Test
	public void testGetMensagemAmigavelChaveEstrangeira() {
		String emIngles = "ERROR: update or delete on table \"categoria\" violates foreign key constraint \"categoria_fk\" on table \"produto\"\n"
				+ "  Detail: Key (id)=(5) is still referenced from table \"item_pedido\".";
		String emPortugues = "ERRO: atualização ou exclusão em tabela \"pedido\" viola restrição de chave estrangeira\n"
				+ "  Detalhe: Chave (id)=(9) ainda é referenciada pela tabela \"movimentacao_produto\".";

		assertEquals("Este cadastro não pode ser removido porque está sendo utilizado por item pedido.",
				ExceptionUtil.getMensagemAmigavel(comCausa(emIngles)));
		assertEquals("Este cadastro não pode ser removido porque está sendo utilizado por movimentacao produto.",
				ExceptionUtil.getMensagemAmigavel(comCausa(emPortugues)));
	}

	@Test
	public void testGetMensagemAmigavelSemMensagem() {
		assertEquals("Ocorreu um erro ao processar a operação.", ExceptionUtil.getMensagemAmigavel(new RuntimeException()));
		assertEquals("Ocorreu um erro ao processar a operação.", ExceptionUtil.getMensagemAmigavel(comCausa("  ")));
	}

	@Test
	public void testGetMensagemAmigavelOutraMensagem() {
		/* Sem padrão conhecido: devolve a mensagem da causa raiz */
		assertEquals("ERROR: value too long", ExceptionUtil.getMensagemAmigavel(comCausa("ERROR: value too long")));
	}

	@Test
	public void testGetMensagemValidacaoConstraintBeanValidation() {
		Set<ConstraintViolation<Categoria>> violacoes = Validation.buildDefaultValidatorFactory().getValidator()
				.validate(new Categoria());
		RuntimeException erro = new RuntimeException("rollback", new ConstraintViolationException(violacoes));

		String mensagem = ExceptionUtil.getMensagemValidacaoConstraint(erro);

		/* As mensagens das anotações, separadas por vírgula */
		for (ConstraintViolation<Categoria> violacao : violacoes) {
			assertTrue(mensagem.contains(violacao.getMessage()), mensagem);
		}
	}

	@Test
	public void testGetMensagemValidacaoConstraintBanco() {
		PSQLException doBanco = new PSQLException(UNIQUE, PSQLState.UNIQUE_VIOLATION);

		assertTrue(ExceptionUtil.getMensagemValidacaoConstraint(new RuntimeException("falhou", doBanco)).endsWith("valor: 'maria@teste.com.br'."));
		assertTrue(ExceptionUtil.getMensagemValidacaoConstraint(comCausa(UNIQUE)).endsWith("valor: 'maria@teste.com.br'."));
	}

	@Test
	public void testGetMensagemValidacaoConstraintRegistroInexistente() {
		EmptyResultDataAccessException erro = new EmptyResultDataAccessException(
				"No class br.com.jdeverp.pro.model.Categoria entity with id 5 exists!", 1);

		assertEquals("Categoria com ID 5 não foi encontrado.", ExceptionUtil.getMensagemValidacaoConstraint(erro));
	}

	@Test
	public void testGetMensagemValidacaoConstraintTipoDeParametroInvalido() throws Exception {
		MethodParameter parametro = new MethodParameter(Object.class.getMethod("equals", Object.class), 0);
		MethodArgumentTypeMismatchException erro = new MethodArgumentTypeMismatchException("abc", Long.class, "id", parametro,
				new NumberFormatException("For input string: \"abc\""));

		/* Sem o padrão de entidade: devolve a mensagem da causa raiz */
		assertEquals("For input string: \"abc\"", ExceptionUtil.getMensagemValidacaoConstraint(erro));
	}

	@Test
	public void testGetMensagemValidacaoConstraintOutroErro() {
		assertEquals("Falha qualquer", ExceptionUtil.getMensagemValidacaoConstraint(new IllegalStateException("Falha qualquer")));
	}

	@Test
	public void testGetMensagemObsInexistente() {
		assertEquals("Pedido com ID 9 não foi encontrado.", ExceptionUtil.getMensagemObsInexistente(
				new RuntimeException("x", new RuntimeException("No class br.com.jdeverp.pro.model.Pedido entity with id 9 exists!"))));
		assertEquals("outra coisa", ExceptionUtil.getMensagemObsInexistente(new RuntimeException("outra coisa")));
	}

	@Test
	public void testGetMensagemParametros() {
		assertEquals("O parâmetro 'nome' é obrigatório.", ExceptionUtil.getMensagemParametros(
				new RuntimeException("Required request parameter 'nome' for method parameter type String is not present")));
		assertEquals("Pedido com código 9 não foi encontrado.", ExceptionUtil.getMensagemParametros(
				new RuntimeException("No class br.com.jdeverp.pro.model.Pedido entity with id 9 exists!")));
		assertEquals("Já existe um registro com sku 'ABC-1'.", ExceptionUtil.getMensagemParametros(
				new RuntimeException("Key (sku)=(ABC-1) already exists.")));
		assertEquals("Mensagem sem padrão", ExceptionUtil.getMensagemParametros(new RuntimeException("Mensagem sem padrão")));
	}

	@Test
	public void testGetMensagemParametrosSemMensagem() {
		assertEquals("Erro interno do sistema.", ExceptionUtil.getMensagemParametros(new RuntimeException()));
		assertEquals("Erro interno do sistema.", ExceptionUtil.getMensagemParametros(new RuntimeException("x", new RuntimeException(" "))));
	}

}
