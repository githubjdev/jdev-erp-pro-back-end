package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.ProdutoDTO;
import br.com.jdeverp.pro.enums.UnidadeMedida;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.repository.CategoriaRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;

/** Produto pertence à empresa logada e sempre a uma categoria dela. Exclusão em lote só por ROLE_ADMIN (P21). */
@ExtendWith(MockitoExtension.class)
public class TestProdutoService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private ProdutoRepository produtoRepository;

	@Mock
	private CategoriaRepository categoriaRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private ProdutoService produtoService;

	private Empresa empresa;
	private Categoria categoriaDoBanco;

	@BeforeEach
	public void configurar() {
		empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		categoriaDoBanco = new Categoria();
		categoriaDoBanco.setId(4L);
		categoriaDoBanco.setNome("Informática");
		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
	}

	/** Produto como vem do request: a categoria só com o id. */
	private static Produto produto(Long id, String nome, Long categoriaId) {
		Produto produto = new Produto();
		produto.setId(id);
		produto.setNome(nome);
		produto.setDescricao("Descrição");
		produto.setImagem("imagem-base64");
		produto.setPreco(10.0);
		produto.setEstoque(5.0);
		produto.setEstoqueMinimo(1.0);
		produto.setSku("SKU-" + nome);
		produto.setCodigoBarra("789");
		produto.setUnidadeMedida(UnidadeMedida.UNIDADE);
		if (categoriaId != null) {
			Categoria categoria = new Categoria();
			categoria.setId(categoriaId);
			produto.setCategoria(categoria);
		}
		return produto;
	}

	private Produto salvo(Long id, String nome) {
		Produto produto = produto(id, nome, null);
		produto.setCategoria(categoriaDoBanco);
		produto.setEmpresa(empresa);
		return produto;
	}

	private void salvarDevolveOProprio() {
		when(produtoRepository.saveAndFlush(any(Produto.class))).thenAnswer(i -> i.getArgument(0));
	}

	@Test
	public void testSalvar() {
		when(produtoRepository.existePorNome("Mouse", EMPRESA_ID)).thenReturn(false);
		when(categoriaRepository.buscarPorId(4L, EMPRESA_ID)).thenReturn(Optional.of(categoriaDoBanco));
		salvarDevolveOProprio();

		ProdutoDTO dto = produtoService.salvar(produto(null, "Mouse", 4L));

		assertEquals("Mouse", dto.getNome());
		/* Categoria trocada pela do banco: o DTO sai com o nome */
		assertEquals(4L, dto.getCategoriaId());
		assertEquals("Informática", dto.getCategoriaNome());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
		assertEquals(10.0, dto.getPreco());
		assertEquals(5.0, dto.getEstoque());
		assertEquals(1.0, dto.getEstoqueMinimo());
		assertEquals("SKU-Mouse", dto.getSku());
		assertEquals("789", dto.getCodigoBarra());
		assertEquals("imagem-base64", dto.getImagem());
		assertEquals(UnidadeMedida.UNIDADE, dto.getUnidadeMedida());
	}

	@Test
	public void testSalvarNomeDuplicado() {
		when(produtoRepository.existePorNome("Mouse", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.salvar(produto(null, "Mouse", 4L)));

		assertEquals("Já existe um produto com o mesmo nome para a empresa logada.", erro.getMessage());
		verify(produtoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSemCategoria() {
		when(produtoRepository.existePorNome("Mouse", EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.salvar(produto(null, "Mouse", null)));

		assertEquals("Categoria deve ser informada para o produto.", erro.getMessage());
		verifyNoInteractions(categoriaRepository);
	}

	@Test
	public void testSalvarCategoriaDeOutraEmpresa() {
		when(produtoRepository.existePorNome("Mouse", EMPRESA_ID)).thenReturn(false);
		when(categoriaRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.salvar(produto(null, "Mouse", 99L)));

		assertEquals("Categoria não encontrada para a empresa logada.", erro.getMessage());
		verify(produtoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizar() {
		when(produtoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, "Antigo")));
		when(produtoRepository.existePorNomeDiferenteId(1L, "Mouse", EMPRESA_ID)).thenReturn(false);
		when(categoriaRepository.buscarPorId(4L, EMPRESA_ID)).thenReturn(Optional.of(categoriaDoBanco));
		salvarDevolveOProprio();

		ProdutoDTO dto = produtoService.atualizar(produto(1L, "Mouse", 4L));

		assertEquals(1L, dto.getId());
		assertEquals("Mouse", dto.getNome());
		assertEquals("Informática", dto.getCategoriaNome());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.atualizar(produto(null, "Mouse", 4L)));

		assertEquals("Id do produto não informado para atualizar.", erro.getMessage());
		verifyNoInteractions(produtoRepository);
	}

	@Test
	public void testAtualizarProdutoDeOutraEmpresa() {
		when(produtoRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.atualizar(produto(99L, "Mouse", 4L)));

		assertEquals("Produto não encontrado para a empresa logada.", erro.getMessage());
		verify(produtoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarNomeDuplicado() {
		when(produtoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, "Antigo")));
		when(produtoRepository.existePorNomeDiferenteId(1L, "Mouse", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.atualizar(produto(1L, "Mouse", 4L)));

		assertEquals("Já existe outro produto com o mesmo nome para a empresa logada.", erro.getMessage());
		verify(produtoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarCategoriaDeOutraEmpresa() {
		when(produtoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, "Antigo")));
		when(categoriaRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.atualizar(produto(1L, "Mouse", 99L)));

		assertEquals("Categoria não encontrada para a empresa logada.", erro.getMessage());
		verify(produtoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testFindAll() {
		when(produtoRepository.findAll(EMPRESA_ID)).thenReturn(List.of(salvo(1L, "Mouse"), salvo(2L, "Teclado")));

		assertEquals(List.of("Mouse", "Teclado"), produtoService.findAll(EMPRESA_ID).stream().map(ProdutoDTO::getNome).toList());
	}

	@Test
	public void testBuscaPorNome() {
		when(produtoRepository.buscaPorNome("mou", EMPRESA_ID)).thenReturn(List.of(salvo(1L, "Mouse")));

		assertEquals(1L, produtoService.buscaPorNome("mou", EMPRESA_ID).get(0).getId());
	}

	@Test
	public void testExistePorNome() {
		when(produtoRepository.existePorNome("Mouse", EMPRESA_ID)).thenReturn(true);

		assertTrue(produtoService.existePorNome("Mouse", EMPRESA_ID));
		assertFalse(produtoService.existePorNome("Teclado", EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		when(produtoRepository.existePorNomeDiferenteId(1L, "Mouse", EMPRESA_ID)).thenReturn(true);

		assertTrue(produtoService.existePorNomeDiferenteId(1L, "Mouse", EMPRESA_ID));
		assertFalse(produtoService.existePorNomeDiferenteId(2L, "Mouse", EMPRESA_ID));
	}

	@Test
	public void testDeleteById() {
		when(produtoRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		produtoService.deleteById(1L, EMPRESA_ID);

		verify(produtoRepository).deleteById(1L, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdNaoEncontrado() {
		when(produtoRepository.existsById(1L, EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.deleteById(1L, EMPRESA_ID));

		assertEquals("Produto não encontrado ou já foi deletado.", erro.getMessage());
		verify(produtoRepository, never()).deleteById(anyLong(), anyLong());
	}

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(produtoRepository.total(EMPRESA_ID)).thenReturn(2L);
		when(produtoRepository.deleteAll(EMPRESA_ID)).thenReturn(2L);

		assertEquals(2L, produtoService.deleteAll(EMPRESA_ID));
	}

	@Test
	public void testDeleteAllSemRegistros() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(produtoRepository.total(EMPRESA_ID)).thenReturn(0L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.deleteAll(EMPRESA_ID));

		assertEquals("Nenhum produto encontrado para deletar ou todos já foram deletados.", erro.getMessage());
		verify(produtoRepository, never()).deleteAll(anyLong());
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(produtoRepository);
	}

	@Test
	public void testDeletarAllById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(produtoRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salvo(1L, "A"), salvo(2L, "B")));

		produtoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID);

		verify(produtoRepository).deletarAllById(List.of(1L, 2L), EMPRESA_ID);
	}

	@Test
	public void testDeletarAllByIdComIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(produtoRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salvo(1L, "A")));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Produtos não encontrados ou já deletados: [2]", erro.getMessage());
		verify(produtoRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdListaVazia() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(produtoRepository.buscarPorIds(List.of(), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.deletarAllById(List.of(), EMPRESA_ID));

		assertEquals("Nenhum registro informado para deletar.", erro.getMessage());
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(produtoRepository);
	}

	@Test
	public void testBuscarPorIds() {
		when(produtoRepository.buscarPorIds(List.of(1L), EMPRESA_ID)).thenReturn(List.of(salvo(1L, "Mouse")));

		assertEquals("Mouse", produtoService.buscarPorIds(List.of(1L), EMPRESA_ID).get(0).getNome());
	}

	@Test
	public void testExistsById() {
		when(produtoRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		assertTrue(produtoService.existsById(1L, EMPRESA_ID));
		assertFalse(produtoService.existsById(2L, EMPRESA_ID));
	}

	@Test
	public void testListar() {
		when(produtoRepository.listar(EMPRESA_ID)).thenReturn(List.of(salvo(1L, "Mouse")));

		assertEquals(1, produtoService.listar(EMPRESA_ID).size());
	}

	@Test
	public void testBuscarPorId() {
		when(produtoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, "Mouse")));
		when(produtoRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		assertEquals("Mouse", produtoService.buscarPorId(1L, EMPRESA_ID).orElseThrow().getNome());
		assertTrue(produtoService.buscarPorId(2L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testTotal() {
		when(produtoRepository.total(EMPRESA_ID)).thenReturn(12L);

		assertEquals(12L, produtoService.total(EMPRESA_ID));
	}

	@Test
	public void testListarPaginado() {
		Pageable pagina = PageRequest.of(0, 1);
		when(produtoRepository.listarPaginado(EMPRESA_ID, pagina)).thenReturn(new PageImpl<>(List.of(salvo(1L, "Mouse")), pagina, 3));

		assertEquals(3, produtoService.listarPaginado(EMPRESA_ID, pagina).getTotalElements());
	}

	@Test
	public void testSalvarCategoriaSemId() {
		when(produtoRepository.existePorNome("Mouse", EMPRESA_ID)).thenReturn(false);
		Produto produto = produto(null, "Mouse", null);
		produto.setCategoria(new Categoria());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> produtoService.salvar(produto));

		assertEquals("Categoria deve ser informada para o produto.", erro.getMessage());
		verifyNoInteractions(categoriaRepository);
	}

}
