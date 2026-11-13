package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.model.Usuario;

public class TestChamadoRepository extends TestContextoRepository {

	private Usuario atendente;
	private Usuario cliente;

	@BeforeEach
	public void criarUsuarios() {
		atendente = novoUsuario(unico("Atendente"));
		cliente = novoUsuario(unico("Cliente"));
	}

	private Chamado chamado(String titulo) {
		return novoChamado(unico(titulo), atendente, cliente);
	}

	@Test
	public void testFindAll() {
		Chamado chamado = chamado("Chamado Lista");

		assertTrue(chamadoRepository.findAll(EMPRESA_ID).stream().anyMatch(c -> c.getId().equals(chamado.getId())));
		assertTrue(chamadoRepository.findAll(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorTitulo() {
		Chamado chamado = chamado("Impressão não funciona");

		assertEquals(chamado.getId(), chamadoRepository.buscaPorTitulo("impressao nao funciona " + sufixo, EMPRESA_ID).get(0).getId());
		assertTrue(chamadoRepository.buscaPorTitulo("impressao nao funciona " + sufixo, EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorTitulo() {
		Chamado chamado = chamado("Rede caiu");

		assertTrue(chamadoRepository.existePorTitulo(chamado.getTitulo().toUpperCase(), EMPRESA_ID));
		assertFalse(chamadoRepository.existePorTitulo(chamado.getTitulo(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorTituloDiferenteId() {
		Chamado chamado = chamado("Sem acesso");

		assertFalse(chamadoRepository.existePorTituloDiferenteId(chamado.getId(), chamado.getTitulo(), EMPRESA_ID));
		assertTrue(chamadoRepository.existePorTituloDiferenteId(-1L, chamado.getTitulo(), EMPRESA_ID));
		assertFalse(chamadoRepository.existePorTituloDiferenteId(-1L, chamado.getTitulo(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		Chamado chamado = chamado("Chamado Apagar");

		chamadoRepository.deleteById(chamado.getId(), EMPRESA_INEXISTENTE);
		assertTrue(chamadoRepository.existsById(chamado.getId(), EMPRESA_ID));

		chamadoRepository.deleteById(chamado.getId(), EMPRESA_ID);
		assertFalse(chamadoRepository.existsById(chamado.getId(), EMPRESA_ID));
	}

}
