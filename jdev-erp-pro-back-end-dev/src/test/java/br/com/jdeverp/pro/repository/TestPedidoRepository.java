package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Pedido;

public class TestPedidoRepository extends TestContextoRepository {

	private String numero() {
		return unico("PED").replace(' ', '-');
	}

	@Test
	public void testFindAll() {
		Pedido pedido = novoPedido(numero());

		assertTrue(pedidoRepository.findAll(EMPRESA_ID).stream().anyMatch(p -> p.getId().equals(pedido.getId())));
		assertTrue(pedidoRepository.findAll(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorNumeroPedido() {
		Pedido pedido = novoPedido(numero());

		assertEquals(pedido.getId(), pedidoRepository.buscaPorNumeroPedido("ped-" + sufixo, EMPRESA_ID).stream()
				.filter(p -> p.getId().equals(pedido.getId())).findFirst().orElseThrow().getId());
		assertTrue(pedidoRepository.buscaPorNumeroPedido(pedido.getNumeroPedido(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorNumeroPedido() {
		Pedido pedido = novoPedido(numero());

		assertTrue(pedidoRepository.existePorNumeroPedido(pedido.getNumeroPedido().toLowerCase(), EMPRESA_ID));
		assertFalse(pedidoRepository.existePorNumeroPedido(pedido.getNumeroPedido(), EMPRESA_INEXISTENTE));
		assertFalse(pedidoRepository.existePorNumeroPedido("PED", EMPRESA_ID));
	}

	@Test
	public void testExistePorNumeroPedidoDiferenteId() {
		Pedido pedido = novoPedido(numero());

		assertFalse(pedidoRepository.existePorNumeroPedidoDiferenteId(pedido.getId(), pedido.getNumeroPedido(), EMPRESA_ID));
		assertTrue(pedidoRepository.existePorNumeroPedidoDiferenteId(-1L, pedido.getNumeroPedido(), EMPRESA_ID));
		assertFalse(pedidoRepository.existePorNumeroPedidoDiferenteId(-1L, pedido.getNumeroPedido(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		Pedido pedido = novoPedido(numero());

		pedidoRepository.deleteById(pedido.getId(), EMPRESA_INEXISTENTE);
		assertTrue(pedidoRepository.existsById(pedido.getId(), EMPRESA_ID));

		pedidoRepository.deleteById(pedido.getId(), EMPRESA_ID);
		assertFalse(pedidoRepository.existsById(pedido.getId(), EMPRESA_ID));
	}

}
