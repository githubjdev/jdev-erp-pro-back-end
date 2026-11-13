package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Produto;

public class TestProdutoRepository extends TestContextoRepository {

	@Test
	public void testFindAll() {
		Produto produto = novoProduto(unico("Produto Lista"));

		assertTrue(produtoRepository.findAll(EMPRESA_ID).stream().anyMatch(p -> p.getId().equals(produto.getId())));
		assertTrue(produtoRepository.findAll(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorNome() {
		Produto produto = novoProduto(unico("Câmera Fotográfica"));

		assertEquals(produto.getId(), produtoRepository.buscaPorNome("camera fotografica " + sufixo, EMPRESA_ID).get(0).getId());
		assertTrue(produtoRepository.buscaPorNome("camera fotografica " + sufixo, EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorNome() {
		Produto produto = novoProduto(unico("Monitor"));

		assertTrue(produtoRepository.existePorNome(produto.getNome().toLowerCase(), EMPRESA_ID));
		assertFalse(produtoRepository.existePorNome(produto.getNome(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		Produto produto = novoProduto(unico("Teclado"));

		assertFalse(produtoRepository.existePorNomeDiferenteId(produto.getId(), produto.getNome(), EMPRESA_ID));
		assertTrue(produtoRepository.existePorNomeDiferenteId(-1L, produto.getNome(), EMPRESA_ID));
		assertFalse(produtoRepository.existePorNomeDiferenteId(-1L, produto.getNome(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		Produto produto = novoProduto(unico("Produto Apagar"));

		produtoRepository.deleteById(produto.getId(), EMPRESA_INEXISTENTE);
		assertTrue(produtoRepository.existsById(produto.getId(), EMPRESA_ID));

		produtoRepository.deleteById(produto.getId(), EMPRESA_ID);
		assertFalse(produtoRepository.existsById(produto.getId(), EMPRESA_ID));
	}

}
