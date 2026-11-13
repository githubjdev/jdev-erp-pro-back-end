package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.model.Mensagem;
import br.com.jdeverp.pro.model.Usuario;

/**
 * Mensagens de chamado. Os usuários e o chamado são criados no teste, então as
 * contagens por atendente, cliente e chamado são exatas.
 */
public class TestMensagemRepository extends TestContextoRepository {

	private Usuario atendente;
	private Usuario cliente;
	private Chamado chamado;
	private Chamado outroChamado;
	private Mensagem naoLida;
	private Mensagem lida;

	@BeforeEach
	public void criarMensagens() {
		atendente = novoUsuario(unico("Atendente"));
		cliente = novoUsuario(unico("Cliente"));
		chamado = novoChamado(unico("Chamado"), atendente, cliente);
		outroChamado = novoChamado(unico("Outro Chamado"), atendente, cliente);
		naoLida = novaMensagem(unico("Atualização disponível"), chamado, atendente, cliente, false);
		lida = novaMensagem(unico("Problema resolvido"), chamado, atendente, cliente, true);
	}

	private static List<Long> ids(List<Mensagem> mensagens) {
		return mensagens.stream().map(Mensagem::getId).sorted().toList();
	}

	private List<Long> ambas() {
		return List.of(naoLida.getId(), lida.getId()).stream().sorted().toList();
	}

	@Test
	public void testFindAll() {
		assertTrue(ids(mensagemRepository.findAll(EMPRESA_ID)).containsAll(ambas()));
		assertTrue(mensagemRepository.findAll(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorConteudo() {
		assertEquals(List.of(naoLida.getId()), ids(mensagemRepository.buscaPorConteudo("atualizacao disponivel " + sufixo, EMPRESA_ID)));
		assertTrue(mensagemRepository.buscaPorConteudo("atualizacao disponivel " + sufixo, EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorConteudo() {
		assertTrue(mensagemRepository.existePorConteudo(naoLida.getConteudo().toUpperCase(), EMPRESA_ID));
		assertFalse(mensagemRepository.existePorConteudo(naoLida.getConteudo(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorConteudoDiferenteId() {
		assertFalse(mensagemRepository.existePorConteudoDiferenteId(naoLida.getId(), naoLida.getConteudo(), EMPRESA_ID));
		assertTrue(mensagemRepository.existePorConteudoDiferenteId(-1L, naoLida.getConteudo(), EMPRESA_ID));
		assertFalse(mensagemRepository.existePorConteudoDiferenteId(-1L, naoLida.getConteudo(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		mensagemRepository.deleteById(lida.getId(), EMPRESA_INEXISTENTE);
		assertTrue(mensagemRepository.existsById(lida.getId(), EMPRESA_ID));

		mensagemRepository.deleteById(lida.getId(), EMPRESA_ID);
		assertFalse(mensagemRepository.existsById(lida.getId(), EMPRESA_ID));
	}

	@Test
	public void testFindAllByChamado() {
		assertEquals(ambas(), ids(mensagemRepository.findAllByChamado(chamado.getId(), EMPRESA_ID)));
		assertTrue(mensagemRepository.findAllByChamado(outroChamado.getId(), EMPRESA_ID).isEmpty());
		assertTrue(mensagemRepository.findAllByChamado(chamado.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorConteudoByChamado() {
		assertEquals(List.of(lida.getId()), ids(mensagemRepository.buscaPorConteudoByChamado("resolvido", chamado.getId(), EMPRESA_ID)));
		assertTrue(mensagemRepository.buscaPorConteudoByChamado("resolvido", outroChamado.getId(), EMPRESA_ID).isEmpty());
	}

	@Test
	public void testExistePorConteudoByChamado() {
		assertTrue(mensagemRepository.existePorConteudoByChamado(lida.getConteudo(), chamado.getId(), EMPRESA_ID));
		assertFalse(mensagemRepository.existePorConteudoByChamado(lida.getConteudo(), outroChamado.getId(), EMPRESA_ID));
		assertFalse(mensagemRepository.existePorConteudoByChamado(lida.getConteudo(), chamado.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorConteudoDiferenteIdByChamado() {
		assertFalse(mensagemRepository.existePorConteudoDiferenteIdByChamado(lida.getId(), lida.getConteudo(), chamado.getId(), EMPRESA_ID));
		assertTrue(mensagemRepository.existePorConteudoDiferenteIdByChamado(-1L, lida.getConteudo(), chamado.getId(), EMPRESA_ID));
		assertFalse(mensagemRepository.existePorConteudoDiferenteIdByChamado(-1L, lida.getConteudo(), outroChamado.getId(), EMPRESA_ID));
	}

	@Test
	public void testCountByChamado() {
		assertEquals(2L, mensagemRepository.countByChamado(chamado.getId(), EMPRESA_ID));
		assertEquals(0L, mensagemRepository.countByChamado(outroChamado.getId(), EMPRESA_ID));
		assertEquals(0L, mensagemRepository.countByChamado(chamado.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteAllByChamado() {
		Mensagem deOutroChamado = novaMensagem(unico("Fica"), outroChamado, atendente, cliente, false);

		mensagemRepository.deleteAllByChamado(chamado.getId(), EMPRESA_INEXISTENTE);
		assertEquals(2L, mensagemRepository.countByChamado(chamado.getId(), EMPRESA_ID));

		mensagemRepository.deleteAllByChamado(chamado.getId(), EMPRESA_ID);
		assertEquals(0L, mensagemRepository.countByChamado(chamado.getId(), EMPRESA_ID));
		/* Só as do chamado informado */
		assertTrue(mensagemRepository.existsById(deOutroChamado.getId(), EMPRESA_ID));
	}

	@Test
	public void testDeleteByIdAndChamado() {
		mensagemRepository.deleteByIdAndChamado(lida.getId(), outroChamado.getId(), EMPRESA_ID);
		assertTrue(mensagemRepository.existsById(lida.getId(), EMPRESA_ID));

		mensagemRepository.deleteByIdAndChamado(lida.getId(), chamado.getId(), EMPRESA_ID);
		assertFalse(mensagemRepository.existsById(lida.getId(), EMPRESA_ID));
	}

	@Test
	public void testFindAllNaoLidas() {
		List<Long> naoLidas = ids(mensagemRepository.findAllNaoLidas(EMPRESA_ID));

		assertTrue(naoLidas.contains(naoLida.getId()));
		assertFalse(naoLidas.contains(lida.getId()));
		assertTrue(mensagemRepository.findAllNaoLidas(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testFindAllNaoLidasByChamado() {
		assertEquals(List.of(naoLida.getId()), ids(mensagemRepository.findAllNaoLidasByChamado(chamado.getId(), EMPRESA_ID)));
		assertTrue(mensagemRepository.findAllNaoLidasByChamado(chamado.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testCountNaoLidasByChamado() {
		assertEquals(1L, mensagemRepository.countNaoLidasByChamado(chamado.getId(), EMPRESA_ID));
		assertEquals(0L, mensagemRepository.countNaoLidasByChamado(chamado.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testUpdateLida() {
		mensagemRepository.updateLida(naoLida.getId(), true, EMPRESA_INEXISTENTE);
		assertEquals(1L, mensagemRepository.countNaoLidasByChamado(chamado.getId(), EMPRESA_ID));

		mensagemRepository.updateLida(naoLida.getId(), true, EMPRESA_ID);
		assertEquals(0L, mensagemRepository.countNaoLidasByChamado(chamado.getId(), EMPRESA_ID));

		mensagemRepository.updateLida(lida.getId(), false, EMPRESA_ID);
		assertEquals(List.of(lida.getId()), ids(mensagemRepository.findAllNaoLidasByChamado(chamado.getId(), EMPRESA_ID)));
	}

	@Test
	public void testFindAllByAtendente() {
		assertEquals(ambas(), ids(mensagemRepository.findAllByAtendente(atendente.getId(), EMPRESA_ID)));
		assertTrue(mensagemRepository.findAllByAtendente(cliente.getId(), EMPRESA_ID).isEmpty());
		assertTrue(mensagemRepository.findAllByAtendente(atendente.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testCountByAtendente() {
		assertEquals(2L, mensagemRepository.countByAtendente(atendente.getId(), EMPRESA_ID));
		assertEquals(0L, mensagemRepository.countByAtendente(atendente.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testFindAllByCliente() {
		assertEquals(ambas(), ids(mensagemRepository.findAllByCliente(cliente.getId(), EMPRESA_ID)));
		assertTrue(mensagemRepository.findAllByCliente(atendente.getId(), EMPRESA_ID).isEmpty());
		assertTrue(mensagemRepository.findAllByCliente(cliente.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testCountByCliente() {
		assertEquals(2L, mensagemRepository.countByCliente(cliente.getId(), EMPRESA_ID));
		assertEquals(0L, mensagemRepository.countByCliente(cliente.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testFindAllByChamadoAndAtendente() {
		assertEquals(ambas(), ids(mensagemRepository.findAllByChamadoAndAtendente(chamado.getId(), atendente.getId(), EMPRESA_ID)));
		assertTrue(mensagemRepository.findAllByChamadoAndAtendente(outroChamado.getId(), atendente.getId(), EMPRESA_ID).isEmpty());
		assertTrue(mensagemRepository.findAllByChamadoAndAtendente(chamado.getId(), cliente.getId(), EMPRESA_ID).isEmpty());
	}

	@Test
	public void testFindAllNaoLidasByAtendente() {
		assertEquals(List.of(naoLida.getId()), ids(mensagemRepository.findAllNaoLidasByAtendente(atendente.getId(), EMPRESA_ID)));
		assertTrue(mensagemRepository.findAllNaoLidasByAtendente(atendente.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testFindAllNaoLidasByCliente() {
		assertEquals(List.of(naoLida.getId()), ids(mensagemRepository.findAllNaoLidasByCliente(cliente.getId(), EMPRESA_ID)));
		assertTrue(mensagemRepository.findAllNaoLidasByCliente(cliente.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

}
