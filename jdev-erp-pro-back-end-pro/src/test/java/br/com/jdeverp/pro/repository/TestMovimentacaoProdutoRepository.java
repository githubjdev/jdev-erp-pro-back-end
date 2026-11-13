package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.MovimentacaoProduto;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;

public class TestMovimentacaoProdutoRepository extends TestContextoRepository {

	private Pedido pedido;
	private Pedido outroPedido;
	private Produto produto;
	private MovimentacaoProduto doPedido;
	private MovimentacaoProduto avulsa;

	@BeforeEach
	public void criarMovimentacoes() {
		pedido = novoPedido(unico("PED").replace(' ', '-'));
		outroPedido = novoPedido(unico("PED").replace(' ', '-'));
		produto = novoProduto(unico("Pneu Aro"));
		doPedido = novaMovimentacao(produto, pedido);
		avulsa = novaMovimentacao(produto, null);
	}

	private static List<Long> ids(List<MovimentacaoProduto> movimentacoes) {
		return movimentacoes.stream().map(MovimentacaoProduto::getId).sorted().toList();
	}

	@Test
	public void testFindAll() {
		assertTrue(ids(movimentacaoProdutoRepository.findAll(EMPRESA_ID)).containsAll(List.of(doPedido.getId(), avulsa.getId())));
		assertTrue(movimentacaoProdutoRepository.findAll(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorNome() {
		assertEquals(List.of(doPedido.getId(), avulsa.getId()),
				ids(movimentacaoProdutoRepository.buscaPorNome("pneu aro " + sufixo, EMPRESA_ID)));
		assertTrue(movimentacaoProdutoRepository.buscaPorNome("pneu aro " + sufixo, EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorNome() {
		assertTrue(movimentacaoProdutoRepository.existePorNome(produto.getNome(), EMPRESA_ID));
		assertFalse(movimentacaoProdutoRepository.existePorNome(produto.getNome(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		/* Há duas movimentações do produto: qualquer uma é "outra" em relação à primeira */
		assertTrue(movimentacaoProdutoRepository.existePorNomeDiferenteId(doPedido.getId(), produto.getNome(), EMPRESA_ID));
		assertFalse(movimentacaoProdutoRepository.existePorNomeDiferenteId(doPedido.getId(), produto.getNome(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		movimentacaoProdutoRepository.deleteById(avulsa.getId(), EMPRESA_INEXISTENTE);
		assertTrue(movimentacaoProdutoRepository.existsById(avulsa.getId(), EMPRESA_ID));

		movimentacaoProdutoRepository.deleteById(avulsa.getId(), EMPRESA_ID);
		assertFalse(movimentacaoProdutoRepository.existsById(avulsa.getId(), EMPRESA_ID));
	}

	@Test
	public void testFindAllByPedido() {
		assertEquals(List.of(doPedido.getId()), ids(movimentacaoProdutoRepository.findAllByPedido(pedido.getId(), EMPRESA_ID)));
		assertTrue(movimentacaoProdutoRepository.findAllByPedido(outroPedido.getId(), EMPRESA_ID).isEmpty());
		assertTrue(movimentacaoProdutoRepository.findAllByPedido(pedido.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorNomeByPedido() {
		assertEquals(List.of(doPedido.getId()), ids(movimentacaoProdutoRepository.buscaPorNomeByPedido("PNEU", pedido.getId(), EMPRESA_ID)));
		assertTrue(movimentacaoProdutoRepository.buscaPorNomeByPedido("PNEU", outroPedido.getId(), EMPRESA_ID).isEmpty());
	}

	@Test
	public void testExistePorNomeByPedido() {
		assertTrue(movimentacaoProdutoRepository.existePorNomeByPedido(produto.getNome(), pedido.getId(), EMPRESA_ID));
		assertFalse(movimentacaoProdutoRepository.existePorNomeByPedido(produto.getNome(), outroPedido.getId(), EMPRESA_ID));
		assertFalse(movimentacaoProdutoRepository.existePorNomeByPedido(produto.getNome(), pedido.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorNomeDiferenteIdByPedido() {
		/* No pedido só há uma movimentação do produto */
		assertFalse(movimentacaoProdutoRepository.existePorNomeDiferenteIdByPedido(doPedido.getId(), produto.getNome(), pedido.getId(), EMPRESA_ID));
		assertTrue(movimentacaoProdutoRepository.existePorNomeDiferenteIdByPedido(-1L, produto.getNome(), pedido.getId(), EMPRESA_ID));
	}

	@Test
	public void testDeleteByIdAndPedido() {
		movimentacaoProdutoRepository.deleteByIdAndPedido(doPedido.getId(), outroPedido.getId(), EMPRESA_ID);
		assertTrue(movimentacaoProdutoRepository.existsById(doPedido.getId(), EMPRESA_ID));

		movimentacaoProdutoRepository.deleteByIdAndPedido(doPedido.getId(), pedido.getId(), EMPRESA_ID);
		assertFalse(movimentacaoProdutoRepository.existsById(doPedido.getId(), EMPRESA_ID));
	}

}
