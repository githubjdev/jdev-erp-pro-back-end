package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Pessoa;

public class TestPessoaRepository extends TestContextoRepository {

	@Test
	public void testFindAll() {
		Pessoa pessoa = novaPessoa(unico("Pessoa Lista"));

		assertTrue(pessoaRepository.findAll(EMPRESA_ID).stream().anyMatch(p -> p.getId().equals(pessoa.getId())));
		assertTrue(pessoaRepository.findAll(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorNome() {
		Pessoa pessoa = novaPessoa(unico("José Antônio"));

		/* Parte do nome, sem acento e em minúsculas */
		assertEquals(pessoa.getId(), pessoaRepository.buscaPorNome("jose antonio " + sufixo, EMPRESA_ID).get(0).getId());
		assertTrue(pessoaRepository.buscaPorNome("jose antonio " + sufixo, EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorNome() {
		Pessoa pessoa = novaPessoa(unico("Conceição"));

		/* Nome inteiro, ignorando acento, caixa e espaços nas pontas */
		assertTrue(pessoaRepository.existePorNome("  " + pessoa.getNome().replace("ç", "c").replace("ã", "a").toUpperCase() + " ", EMPRESA_ID));
		assertFalse(pessoaRepository.existePorNome(pessoa.getNome(), EMPRESA_INEXISTENTE));
		/* Parte do nome não conta como o mesmo nome */
		assertFalse(pessoaRepository.existePorNome("Conceição", EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		Pessoa pessoa = novaPessoa(unico("Pessoa Única"));

		assertFalse(pessoaRepository.existePorNomeDiferenteId(pessoa.getId(), pessoa.getNome(), EMPRESA_ID));
		assertTrue(pessoaRepository.existePorNomeDiferenteId(-1L, pessoa.getNome(), EMPRESA_ID));
		assertFalse(pessoaRepository.existePorNomeDiferenteId(-1L, pessoa.getNome(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		Pessoa pessoa = novaPessoa(unico("Pessoa Apagar"));

		/* Em outra empresa, nada é apagado */
		pessoaRepository.deleteById(pessoa.getId(), EMPRESA_INEXISTENTE);
		assertTrue(pessoaRepository.existsById(pessoa.getId(), EMPRESA_ID));

		pessoaRepository.deleteById(pessoa.getId(), EMPRESA_ID);
		assertFalse(pessoaRepository.existsById(pessoa.getId(), EMPRESA_ID));
	}

}
