package br.com.jdeverp.pro.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.enums.TipoPessoa;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Documentos da pessoa por tipo (P13, script V2), só com Bean Validation, sem
 * banco: e-mail obrigatório para todos, CPF só para pessoa física e CNPJ só
 * para pessoa jurídica.
 */
public class TestPessoa {

	private static final String CPF_VALIDO = "529.982.247-25";
	private static final String CNPJ_VALIDO = "11.222.333/0001-81";

	private static ValidatorFactory fabrica;
	private static Validator validador;

	@BeforeAll
	static void criarValidador() {
		fabrica = Validation.buildDefaultValidatorFactory();
		validador = fabrica.getValidator();
	}

	@AfterAll
	static void fecharValidador() {
		fabrica.close();
	}

	private static Pessoa pessoa(TipoPessoa tipo, String cpf, String cnpj, String email) {
		Pessoa pessoa = new Pessoa();
		pessoa.setNome("Pessoa Teste");
		pessoa.setTipoPessoa(tipo);
		pessoa.setCpf(cpf);
		pessoa.setCnpj(cnpj);
		pessoa.setEmail(email);
		pessoa.setTelefone("44 99999-0000");
		pessoa.setCep("87000-000");
		pessoa.setLogradouro("Rua Teste");
		pessoa.setBairro("Centro");
		pessoa.setEstado("PR");
		pessoa.setCidade("Maringá");
		pessoa.setPais("Brasil");
		return pessoa;
	}

	private static List<String> erros(Pessoa pessoa) {
		return validador.validate(pessoa).stream().map(ConstraintViolation::getMessage).sorted().toList();
	}

	@Test
	public void testPessoaFisicaSemCnpj() {
		assertEquals(List.of(), erros(pessoa(TipoPessoa.FISICA, CPF_VALIDO, null, "fisica@teste.com.br")));
	}

	@Test
	public void testPessoaJuridicaSemCpf() {
		assertEquals(List.of(), erros(pessoa(TipoPessoa.JURIDICA, null, CNPJ_VALIDO, "juridica@teste.com.br")));
	}

	@Test
	public void testPessoaFisicaSemCpf() {
		assertEquals(List.of("Informe o CPF para pessoa física"),
				erros(pessoa(TipoPessoa.FISICA, null, CNPJ_VALIDO, "fisica@teste.com.br")));
	}

	@Test
	public void testPessoaJuridicaSemCnpj() {
		assertEquals(List.of("Informe o CNPJ para pessoa jurídica"),
				erros(pessoa(TipoPessoa.JURIDICA, CPF_VALIDO, null, "juridica@teste.com.br")));
	}

	@Test
	public void testSemEmail() {
		for (TipoPessoa tipo : TipoPessoa.values()) {
			assertEquals(List.of("Informe o e-mail corretamente"), erros(pessoa(tipo, CPF_VALIDO, CNPJ_VALIDO, null)),
					tipo.name());
		}
	}

	@Test
	public void testDocumentoEmBrancoViraNulo() {
		/* Em branco conta como não informado e vai NULL para o banco, sem bater na unique_cpf/unique_cnpj */
		Pessoa fisica = pessoa(TipoPessoa.FISICA, "  ", "", "fisica@teste.com.br");
		assertNull(fisica.getCpf());
		assertNull(fisica.getCnpj());
		assertEquals(List.of("Informe o CPF para pessoa física"), erros(fisica));

		assertEquals(CPF_VALIDO, pessoa(TipoPessoa.FISICA, " " + CPF_VALIDO + " ", null, "x@teste.com.br").getCpf());
	}

	@Test
	public void testDocumentoInvalido() {
		assertEquals(List.of("Informe o CPF corretamente"),
				erros(pessoa(TipoPessoa.FISICA, "111.111.111-11", null, "fisica@teste.com.br")));
		assertEquals(List.of("Informe o CNPJ corretamente"),
				erros(pessoa(TipoPessoa.JURIDICA, null, "11.222.333/0001-00", "juridica@teste.com.br")));
	}

	@Test
	public void testSemTipo() {
		/* Sem tipo, só a mensagem do tipo: a regra do documento depende dele */
		assertEquals(List.of("Informe o tipo da pessoa PF ou PJ"), erros(pessoa(null, null, null, "x@teste.com.br")));
	}

}
