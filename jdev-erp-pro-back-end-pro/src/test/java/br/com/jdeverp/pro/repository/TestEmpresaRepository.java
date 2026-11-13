package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;

/** Empresa é global: as consultas valem para o banco inteiro e o nome vem da pessoa da empresa. */
public class TestEmpresaRepository extends TestContextoRepository {

	@Autowired
	private PlanoRepository planoRepository;

	/** Empresa nova, com uma pessoa nova (a pessoa nasce na empresa 1, como em TestContextoApi). */
	private Empresa novaEmpresa(String nome) {
		Pessoa pessoa = novaPessoa(nome);
		Empresa empresa = new Empresa();
		empresa.setPlano(planoRepository.findAll().get(0));
		empresa.setPessoa(pessoa);
		empresa.setLogoMarca("logo");
		empresa.setPlanoAtivo(true);
		empresa.setBloqueio(false);
		return empresaRepository.saveAndFlush(empresa);
	}

	@Test
	public void testFindAll() {
		Empresa empresa = novaEmpresa(unico("Empresa Lista"));

		assertTrue(empresaRepository.findAll().stream().anyMatch(e -> e.getId().equals(empresa.getId())));
		assertTrue(empresaRepository.findAll().stream().anyMatch(e -> e.getId().equals(EMPRESA_ID)));
	}

	@Test
	public void testBuscarPorId() {
		assertEquals(EMPRESA_ID, empresaRepository.buscarPorId(EMPRESA_ID).getId());
		assertNull(empresaRepository.buscarPorId(EMPRESA_INEXISTENTE));
	}

	@Test
	public void testBuscaPorNome() {
		Empresa empresa = novaEmpresa(unico("Comércio São João"));

		assertEquals(empresa.getId(), empresaRepository.buscaPorNome("comercio sao joao " + sufixo).get(0).getId());
		assertTrue(empresaRepository.buscaPorNome("nao-existe-" + sufixo).isEmpty());
	}

	@Test
	public void testExistePorNome() {
		Empresa empresa = novaEmpresa(unico("Empresa Existe"));

		assertTrue(empresaRepository.existePorNome(empresa.getPessoa().getNome().toLowerCase()));
		assertFalse(empresaRepository.existePorNome("Empresa Existe"));
	}

	@Test
	public void testExistePorPessoa() {
		Empresa empresa = novaEmpresa(unico("Empresa Pessoa"));
		Pessoa semEmpresa = novaPessoa(unico("Pessoa Sem Empresa"));

		assertTrue(empresaRepository.existePorPessoa(empresa.getPessoa().getId()));
		assertFalse(empresaRepository.existePorPessoa(semEmpresa.getId()));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		Empresa empresa = novaEmpresa(unico("Empresa Única"));
		String nome = empresa.getPessoa().getNome();

		assertFalse(empresaRepository.existePorNomeDiferenteId(empresa.getId(), nome));
		assertTrue(empresaRepository.existePorNomeDiferenteId(EMPRESA_INEXISTENTE, nome));
	}

	@Test
	public void testDeleteById() {
		Empresa empresa = novaEmpresa(unico("Empresa Apagar"));

		empresaRepository.deleteById(empresa.getId());

		assertNull(empresaRepository.buscarPorId(empresa.getId()));
	}

}
