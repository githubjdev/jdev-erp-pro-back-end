package br.com.jdeverp.pro.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;

/** Nome do usuário nos DTOs: usuario.clienteFuncionario.pessoa.nome, ou null se faltar algum elo. */
public class TestDtoUtil {

	@Test
	public void testNomeUsuario() {
		Pessoa pessoa = new Pessoa();
		pessoa.setNome("Maria da Silva");
		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setPessoa(pessoa);
		Usuario usuario = new Usuario();
		usuario.setClienteFuncionario(clienteFuncionario);

		assertEquals("Maria da Silva", DtoUtil.nomeUsuario(usuario));
	}

	@Test
	public void testNomeUsuarioSemDados() {
		Usuario semClienteFuncionario = new Usuario();
		Usuario semPessoa = new Usuario();
		semPessoa.setClienteFuncionario(new ClienteFuncionario());

		assertNull(DtoUtil.nomeUsuario(null));
		assertNull(DtoUtil.nomeUsuario(semClienteFuncionario));
		assertNull(DtoUtil.nomeUsuario(semPessoa));
	}

}
