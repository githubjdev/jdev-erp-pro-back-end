package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;

/** Todas as consultas de item filtram pela empresa e pelo pedido. */
public class TestItemPedidoRepository extends TestContextoRepository {

	private Pedido pedido;
	private Pedido outroPedido;
	private Produto produto;
	private ItemPedido item;

	@BeforeEach
	public void criarItem() {
		pedido = novoPedido(unico("PED").replace(' ', '-'));
		outroPedido = novoPedido(unico("PED").replace(' ', '-'));
		produto = novoProduto(unico("Cabo Ótico"));
		item = novoItem(pedido, produto);
	}

	private static List<Long> ids(List<ItemPedido> itens) {
		return itens.stream().map(ItemPedido::getId).toList();
	}

	@Test
	public void testFindAll() {
		assertEquals(List.of(item.getId()), ids(itemPedidoRepository.findAll(pedido.getId(), EMPRESA_ID)));
		assertTrue(itemPedidoRepository.findAll(outroPedido.getId(), EMPRESA_ID).isEmpty());
		assertTrue(itemPedidoRepository.findAll(pedido.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorNome() {
		/* Pelo nome do produto, sem acento */
		assertEquals(List.of(item.getId()), ids(itemPedidoRepository.buscaPorNome("cabo otico", pedido.getId(), EMPRESA_ID)));
		assertTrue(itemPedidoRepository.buscaPorNome("cabo otico", outroPedido.getId(), EMPRESA_ID).isEmpty());
		assertTrue(itemPedidoRepository.buscaPorNome("cabo otico", pedido.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorNome() {
		assertTrue(itemPedidoRepository.existePorNome(produto.getNome(), pedido.getId(), EMPRESA_ID));
		assertFalse(itemPedidoRepository.existePorNome(produto.getNome(), outroPedido.getId(), EMPRESA_ID));
		assertFalse(itemPedidoRepository.existePorNome(produto.getNome(), pedido.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		assertFalse(itemPedidoRepository.existePorNomeDiferenteId(item.getId(), produto.getNome(), pedido.getId(), EMPRESA_ID));
		assertTrue(itemPedidoRepository.existePorNomeDiferenteId(-1L, produto.getNome(), pedido.getId(), EMPRESA_ID));
		assertFalse(itemPedidoRepository.existePorNomeDiferenteId(-1L, produto.getNome(), pedido.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		itemPedidoRepository.deleteById(item.getId(), outroPedido.getId(), EMPRESA_ID);
		itemPedidoRepository.deleteById(item.getId(), pedido.getId(), EMPRESA_INEXISTENTE);
		assertTrue(itemPedidoRepository.existsById(item.getId(), EMPRESA_ID));

		itemPedidoRepository.deleteById(item.getId(), pedido.getId(), EMPRESA_ID);
		assertFalse(itemPedidoRepository.existsById(item.getId(), EMPRESA_ID));
	}

	@Test
	public void testFindAllByPedido() {
		assertEquals(List.of(item.getId()), ids(itemPedidoRepository.findAllByPedido(pedido.getId(), EMPRESA_ID)));
		assertTrue(itemPedidoRepository.findAllByPedido(outroPedido.getId(), EMPRESA_ID).isEmpty());
		assertTrue(itemPedidoRepository.findAllByPedido(pedido.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorNomePorPedido() {
		assertEquals(List.of(item.getId()), ids(itemPedidoRepository.buscaPorNomePorPedido("CABO", pedido.getId(), EMPRESA_ID)));
		assertTrue(itemPedidoRepository.buscaPorNomePorPedido("CABO", outroPedido.getId(), EMPRESA_ID).isEmpty());
		assertTrue(itemPedidoRepository.buscaPorNomePorPedido("CABO", pedido.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorNomePorPedido() {
		assertTrue(itemPedidoRepository.existePorNomePorPedido(produto.getNome(), pedido.getId(), EMPRESA_ID));
		assertFalse(itemPedidoRepository.existePorNomePorPedido(produto.getNome(), outroPedido.getId(), EMPRESA_ID));
		assertFalse(itemPedidoRepository.existePorNomePorPedido(produto.getNome(), pedido.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorNomeDiferenteIdPorPedido() {
		assertFalse(itemPedidoRepository.existePorNomeDiferenteIdPorPedido(item.getId(), produto.getNome(), pedido.getId(), EMPRESA_ID));
		assertTrue(itemPedidoRepository.existePorNomeDiferenteIdPorPedido(-1L, produto.getNome(), pedido.getId(), EMPRESA_ID));
		assertFalse(itemPedidoRepository.existePorNomeDiferenteIdPorPedido(-1L, produto.getNome(), outroPedido.getId(), EMPRESA_ID));
	}

	@Test
	public void testDeleteByIdAndPedido() {
		itemPedidoRepository.deleteByIdAndPedido(item.getId(), outroPedido.getId(), EMPRESA_ID);
		assertTrue(itemPedidoRepository.existsById(item.getId(), EMPRESA_ID));

		itemPedidoRepository.deleteByIdAndPedido(item.getId(), pedido.getId(), EMPRESA_ID);
		assertFalse(itemPedidoRepository.existsById(item.getId(), EMPRESA_ID));
	}

}
