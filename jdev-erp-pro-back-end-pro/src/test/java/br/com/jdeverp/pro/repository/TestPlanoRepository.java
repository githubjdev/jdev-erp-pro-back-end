package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.enums.TipoPlano;
import br.com.jdeverp.pro.model.Plano;

/** Plano é global (sem empresa): as consultas valem para o banco inteiro. */
public class TestPlanoRepository extends TestContextoRepository {

	@Autowired
	private PlanoRepository planoRepository;

	private Plano novoPlano(String nome) {
		Plano plano = new Plano();
		plano.setNome(nome);
		plano.setDescricao("Plano de teste");
		plano.setAtivo(true);
		plano.setValorMensal(99.0);
		plano.setLimiteUsuario(5);
		plano.setLimiteCliente(10);
		plano.setTipoPlano(TipoPlano.ENTERPRISE);
		return planoRepository.saveAndFlush(plano);
	}

	private Plano planoFree() {
		return planoRepository.findAll().stream().filter(p -> p.getTipoPlano() == TipoPlano.FREE).findFirst().orElseThrow();
	}

	@Test
	public void testFindAll() {
		Plano plano = novoPlano(unico("Plano Lista"));

		assertTrue(planoRepository.findAll().stream().anyMatch(p -> p.getId().equals(plano.getId())));
		/* O plano FREE da carga inicial */
		assertEquals(TipoPlano.FREE, planoFree().getTipoPlano());
	}

	@Test
	public void testBuscaPorNome() {
		Plano plano = novoPlano(unico("Plano Avançado"));

		assertEquals(plano.getId(), planoRepository.buscaPorNome("plano avancado " + sufixo).get(0).getId());
	}

	@Test
	public void testExistePorNome() {
		Plano plano = novoPlano(unico("Plano Existe"));

		assertTrue(planoRepository.existePorNome(plano.getNome().toUpperCase()));
		assertFalse(planoRepository.existePorNome("Plano que não existe " + sufixo));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		Plano plano = novoPlano(unico("Plano Único"));

		assertFalse(planoRepository.existePorNomeDiferenteId(plano.getId(), plano.getNome()));
		assertTrue(planoRepository.existePorNomeDiferenteId(-1L, plano.getNome()));
	}

	@Test
	public void testExistePorTipo() {
		assertTrue(planoRepository.existePorTipo(TipoPlano.FREE));
	}

	@Test
	public void testExistePorTipoDiferenteId() {
		Plano free = planoFree();
		Plano outro = novoPlano(unico("Plano Outro"));

		/* O próprio FREE não conta: é o que deixa editá-lo */
		assertFalse(planoRepository.existePorTipoDiferenteId(free.getId(), TipoPlano.FREE));
		/* Para outro plano, já existe um FREE */
		assertTrue(planoRepository.existePorTipoDiferenteId(outro.getId(), TipoPlano.FREE));
	}

	@Test
	public void testDeleteById() {
		Plano plano = novoPlano(unico("Plano Apagar"));

		planoRepository.deleteById(plano.getId());

		assertTrue(planoRepository.findById(plano.getId()).isEmpty());
	}

	@Test
	public void testMetodosDoSpringDataLiberadosParaEntidadeGlobal() {
		Plano plano = novoPlano(unico("Plano Spring Data"));

		/* Sem empresa na entidade, findById, count e existsById não são bloqueados */
		assertTrue(planoRepository.findById(plano.getId()).isPresent());
		assertTrue(planoRepository.existsById(plano.getId()));
		assertTrue(planoRepository.count() >= 3);
		/* E os métodos por empresa ignoram a empresa */
		assertTrue(planoRepository.buscarPorId(plano.getId(), EMPRESA_INEXISTENTE).isPresent());
		assertEquals(planoRepository.count(), planoRepository.total(EMPRESA_INEXISTENTE));
	}

}
