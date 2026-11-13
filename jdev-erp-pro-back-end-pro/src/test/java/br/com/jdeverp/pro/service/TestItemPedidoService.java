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

import java.math.BigDecimal;
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

import br.com.jdeverp.pro.dto.ItemPedidoDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.repository.ItemPedidoRepository;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;

/**
 * Item de pedido: pedido e produto da empresa logada, sem repetir o produto no
 * mesmo pedido. Exclusão em lote só por ROLE_ADMIN (P21).
 */
@ExtendWith(MockitoExtension.class)
public class TestItemPedidoService {

	private static final Long EMPRESA_ID = 10L;
	private static final Long PEDIDO_ID = 3L;
	private static final Long PRODUTO_ID = 4L;

	@Mock
	private ItemPedidoRepository itemPedidoRepository;

	@Mock
	private PedidoRepository pedidoRepository;

	@Mock
	private ProdutoRepository produtoRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private ItemPedidoService itemPedidoService;

	private Empresa empresa;

	@BeforeEach
	public void configurar() {
		empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
	}

	private static Pedido pedido(Long id, String numero) {
		Pedido pedido = new Pedido();
		pedido.setId(id);
		pedido.setNumeroPedido(numero);
		return pedido;
	}

	private static Produto produto(Long id, String nome) {
		Produto produto = new Produto();
		produto.setId(id);
		produto.setNome(nome);
		return produto;
	}

	/** Item como vem do request: pedido e produto só com o id. */
	private static ItemPedido item(Long id) {
		ItemPedido item = new ItemPedido();
		item.setId(id);
		item.setQuantidade(2.0);
		item.setSubTotal(new BigDecimal("20.00"));
		item.setDesconto(new BigDecimal("1.00"));
		item.setTotal(new BigDecimal("19.00"));
		item.setPedido(pedido(PEDIDO_ID, null));
		item.setProduto(produto(PRODUTO_ID, null));
		return item;
	}

	private ItemPedido salvo(Long id, Long pedidoId) {
		ItemPedido item = item(id);
		item.setPedido(pedido(pedidoId, "PED-" + pedidoId));
		item.setProduto(produto(PRODUTO_ID, "Mouse"));
		item.setEmpresa(empresa);
		return item;
	}

	private void pedidoEProdutoDaEmpresa() {
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.of(pedido(PEDIDO_ID, "PED-3")));
		when(produtoRepository.buscarPorId(PRODUTO_ID, EMPRESA_ID)).thenReturn(Optional.of(produto(PRODUTO_ID, "Mouse")));
	}

	@Test
	public void testSalvar() {
		pedidoEProdutoDaEmpresa();
		when(itemPedidoRepository.existePorNomePorPedido("Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(false);
		when(itemPedidoRepository.saveAndFlush(any(ItemPedido.class))).thenAnswer(i -> {
			ItemPedido salvo = i.getArgument(0);
			salvo.setId(1L);
			return salvo;
		});

		ItemPedidoDTO dto = itemPedidoService.salvar(item(null));

		assertEquals(1L, dto.getId());
		/* Pedido e produto trocados pelos do banco: o DTO sai com nome e número */
		assertEquals("Mouse", dto.getProdutoNome());
		assertEquals(PRODUTO_ID, dto.getProdutoId());
		assertEquals("PED-3", dto.getNumeroPedido());
		assertEquals(PEDIDO_ID, dto.getPedidoId());
		assertEquals(2.0, dto.getQuantidade());
		assertEquals(new BigDecimal("19.00"), dto.getTotal());
		assertEquals(new BigDecimal("1.00"), dto.getDesconto());
		assertEquals(new BigDecimal("20.00"), dto.getSubTotal());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testSalvarProdutoJaIncluido() {
		pedidoEProdutoDaEmpresa();
		when(itemPedidoRepository.existePorNomePorPedido("Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.salvar(item(null)));

		assertEquals("O produto já foi incluído neste pedido.", erro.getMessage());
		verify(itemPedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSemPedido() {
		ItemPedido item = item(null);
		item.setPedido(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.salvar(item));

		assertEquals("Pedido deve ser informado para o item.", erro.getMessage());
		verifyNoInteractions(pedidoRepository, itemPedidoRepository);
	}

	@Test
	public void testSalvarPedidoDeOutraEmpresa() {
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.salvar(item(null)));

		assertEquals("Pedido não encontrado para a empresa logada.", erro.getMessage());
		verify(itemPedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSemProduto() {
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.of(pedido(PEDIDO_ID, "PED-3")));
		ItemPedido item = item(null);
		item.setProduto(new Produto());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.salvar(item));

		assertEquals("Produto deve ser informado para o item.", erro.getMessage());
		verifyNoInteractions(produtoRepository);
	}

	@Test
	public void testSalvarProdutoDeOutraEmpresa() {
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.of(pedido(PEDIDO_ID, "PED-3")));
		when(produtoRepository.buscarPorId(PRODUTO_ID, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.salvar(item(null)));

		assertEquals("Produto não encontrado para a empresa logada.", erro.getMessage());
		verify(itemPedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizar() {
		ItemPedido existente = salvo(1L, PEDIDO_ID);
		existente.setQuantidade(1.0);
		pedidoEProdutoDaEmpresa();
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		when(itemPedidoRepository.existePorNomeDiferenteIdPorPedido(1L, "Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(false);
		when(itemPedidoRepository.saveAndFlush(existente)).thenReturn(existente);
		ItemPedido alteracao = item(1L);
		alteracao.setQuantidade(5.0);
		alteracao.setTotal(new BigDecimal("50.00"));

		ItemPedidoDTO dto = itemPedidoService.atualizar(alteracao);

		assertEquals(5.0, dto.getQuantidade());
		assertEquals(new BigDecimal("50.00"), dto.getTotal());
		assertEquals("Mouse", dto.getProdutoNome());
		verify(itemPedidoRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.atualizar(item(null)));

		assertEquals("Id do item do pedido não informado para atualizar.", erro.getMessage());
		verifyNoInteractions(itemPedidoRepository);
	}

	@Test
	public void testAtualizarItemDeOutraEmpresa() {
		when(itemPedidoRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.atualizar(item(99L)));

		assertEquals("Item do pedido não encontrado para a empresa logada.", erro.getMessage());
		verify(itemPedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarProdutoJaIncluido() {
		pedidoEProdutoDaEmpresa();
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, PEDIDO_ID)));
		when(itemPedidoRepository.existePorNomeDiferenteIdPorPedido(1L, "Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.atualizar(item(1L)));

		assertEquals("O produto já foi incluído neste pedido.", erro.getMessage());
		verify(itemPedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarPedidoDeOutraEmpresa() {
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, PEDIDO_ID)));
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.atualizar(item(1L)));

		assertEquals("Pedido não encontrado para a empresa logada.", erro.getMessage());
	}

	@Test
	public void testFindAll() {
		when(itemPedidoRepository.findAll(PEDIDO_ID, EMPRESA_ID)).thenReturn(List.of(salvo(1L, PEDIDO_ID)));

		assertEquals(1L, itemPedidoService.findAll(PEDIDO_ID, EMPRESA_ID).get(0).getId());
	}

	@Test
	public void testBuscaPorNome() {
		when(itemPedidoRepository.buscaPorNome("mou", PEDIDO_ID, EMPRESA_ID)).thenReturn(List.of(salvo(1L, PEDIDO_ID)));

		assertEquals("Mouse", itemPedidoService.buscaPorNome("mou", PEDIDO_ID, EMPRESA_ID).get(0).getProdutoNome());
	}

	@Test
	public void testExistePorNome() {
		when(itemPedidoRepository.existePorNome("Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(true);

		assertTrue(itemPedidoService.existePorNome("Mouse", PEDIDO_ID, EMPRESA_ID));
		assertFalse(itemPedidoService.existePorNome("Teclado", PEDIDO_ID, EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		when(itemPedidoRepository.existePorNomeDiferenteId(1L, "Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(true);

		assertTrue(itemPedidoService.existePorNomeDiferenteId(1L, "Mouse", PEDIDO_ID, EMPRESA_ID));
		assertFalse(itemPedidoService.existePorNomeDiferenteId(2L, "Mouse", PEDIDO_ID, EMPRESA_ID));
	}

	@Test
	public void testDeleteById() {
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, PEDIDO_ID)));

		itemPedidoService.deleteById(1L, PEDIDO_ID, EMPRESA_ID);

		verify(itemPedidoRepository).deleteById(1L, PEDIDO_ID, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdItemDeOutroPedido() {
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, 77L)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.deleteById(1L, PEDIDO_ID, EMPRESA_ID));

		assertEquals("Item do pedido não encontrado ou já foi deletado.", erro.getMessage());
		verify(itemPedidoRepository, never()).deleteById(anyLong(), anyLong(), anyLong());
	}

	@Test
	public void testDeleteByIdItemSemPedido() {
		ItemPedido semPedido = salvo(1L, PEDIDO_ID);
		semPedido.setPedido(null);
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(semPedido));

		assertThrows(MsgApiException.class, () -> itemPedidoService.deleteById(1L, PEDIDO_ID, EMPRESA_ID));
		verify(itemPedidoRepository, never()).deleteById(anyLong(), anyLong(), anyLong());
	}

	@Test
	public void testDeleteByIdNaoEncontrado() {
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.deleteById(1L, PEDIDO_ID, EMPRESA_ID));

		assertEquals("Item do pedido não encontrado ou já foi deletado.", erro.getMessage());
	}

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(itemPedidoRepository.total(EMPRESA_ID)).thenReturn(2L);
		when(itemPedidoRepository.deleteAll(EMPRESA_ID)).thenReturn(2L);

		assertEquals(2L, itemPedidoService.deleteAll(EMPRESA_ID));
	}

	@Test
	public void testDeleteAllSemRegistros() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(itemPedidoRepository.total(EMPRESA_ID)).thenReturn(0L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.deleteAll(EMPRESA_ID));

		assertEquals("Nenhum item de pedido encontrado para deletar ou todos já foram deletados.", erro.getMessage());
		verify(itemPedidoRepository, never()).deleteAll(anyLong());
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(itemPedidoRepository);
	}

	@Test
	public void testDeletarAllById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(itemPedidoRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID))
				.thenReturn(List.of(salvo(1L, PEDIDO_ID), salvo(2L, PEDIDO_ID)));

		itemPedidoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID);

		verify(itemPedidoRepository).deletarAllById(List.of(1L, 2L), EMPRESA_ID);
	}

	@Test
	public void testDeletarAllByIdComIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(itemPedidoRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salvo(1L, PEDIDO_ID)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Itens de pedido não encontrados ou já deletados: [2]", erro.getMessage());
		verify(itemPedidoRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdListaVazia() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(itemPedidoRepository.buscarPorIds(List.of(), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.deletarAllById(List.of(), EMPRESA_ID));

		assertEquals("Nenhum registro informado para deletar.", erro.getMessage());
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(itemPedidoRepository);
	}

	@Test
	public void testBuscarPorIds() {
		when(itemPedidoRepository.buscarPorIds(List.of(1L), EMPRESA_ID)).thenReturn(List.of(salvo(1L, PEDIDO_ID)));

		assertEquals(1, itemPedidoService.buscarPorIds(List.of(1L), EMPRESA_ID).size());
	}

	@Test
	public void testExistsById() {
		when(itemPedidoRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		assertTrue(itemPedidoService.existsById(1L, EMPRESA_ID));
		assertFalse(itemPedidoService.existsById(2L, EMPRESA_ID));
	}

	@Test
	public void testListar() {
		when(itemPedidoRepository.listar(EMPRESA_ID)).thenReturn(List.of(salvo(1L, PEDIDO_ID), salvo(2L, 8L)));

		assertEquals(2, itemPedidoService.listar(EMPRESA_ID).size());
	}

	@Test
	public void testBuscarPorId() {
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, PEDIDO_ID)));
		when(itemPedidoRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		assertEquals("PED-3", itemPedidoService.buscarPorId(1L, EMPRESA_ID).orElseThrow().getNumeroPedido());
		assertTrue(itemPedidoService.buscarPorId(2L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testTotal() {
		when(itemPedidoRepository.total(EMPRESA_ID)).thenReturn(9L);

		assertEquals(9L, itemPedidoService.total(EMPRESA_ID));
	}

	@Test
	public void testListarPaginado() {
		Pageable pagina = PageRequest.of(0, 1);
		when(itemPedidoRepository.listarPaginado(EMPRESA_ID, pagina)).thenReturn(new PageImpl<>(List.of(salvo(1L, PEDIDO_ID)), pagina, 2));

		assertEquals(2, itemPedidoService.listarPaginado(EMPRESA_ID, pagina).getTotalElements());
	}

	@Test
	public void testFindAllByPedido() {
		when(itemPedidoRepository.findAllByPedido(PEDIDO_ID, EMPRESA_ID)).thenReturn(List.of(salvo(1L, PEDIDO_ID)));

		assertEquals(PEDIDO_ID, itemPedidoService.findAllByPedido(PEDIDO_ID, EMPRESA_ID).get(0).getPedidoId());
	}

	@Test
	public void testBuscaPorNomePorPedido() {
		when(itemPedidoRepository.buscaPorNomePorPedido("mou", PEDIDO_ID, EMPRESA_ID)).thenReturn(List.of(salvo(1L, PEDIDO_ID)));

		assertEquals("Mouse", itemPedidoService.buscaPorNomePorPedido("mou", PEDIDO_ID, EMPRESA_ID).get(0).getProdutoNome());
	}

	@Test
	public void testExistePorNomePorPedido() {
		when(itemPedidoRepository.existePorNomePorPedido("Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(true);

		assertTrue(itemPedidoService.existePorNomePorPedido("Mouse", PEDIDO_ID, EMPRESA_ID));
		assertFalse(itemPedidoService.existePorNomePorPedido("Mouse", 8L, EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteIdPorPedido() {
		when(itemPedidoRepository.existePorNomeDiferenteIdPorPedido(1L, "Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(true);

		assertTrue(itemPedidoService.existePorNomeDiferenteIdPorPedido(1L, "Mouse", PEDIDO_ID, EMPRESA_ID));
		assertFalse(itemPedidoService.existePorNomeDiferenteIdPorPedido(2L, "Mouse", PEDIDO_ID, EMPRESA_ID));
	}

	@Test
	public void testDeleteByIdAndPedido() {
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, PEDIDO_ID)));

		itemPedidoService.deleteByIdAndPedido(1L, PEDIDO_ID, EMPRESA_ID);

		verify(itemPedidoRepository).deleteByIdAndPedido(1L, PEDIDO_ID, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdAndPedidoItemDeOutroPedido() {
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, 77L)));

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> itemPedidoService.deleteByIdAndPedido(1L, PEDIDO_ID, EMPRESA_ID));

		assertEquals("Item do pedido não encontrado ou já foi deletado.", erro.getMessage());
		verify(itemPedidoRepository, never()).deleteByIdAndPedido(anyLong(), anyLong(), anyLong());
	}

	@Test
	public void testDeleteByIdAndPedidoItemSemPedido() {
		ItemPedido semPedido = salvo(1L, PEDIDO_ID);
		semPedido.setPedido(null);
		when(itemPedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(semPedido));

		assertThrows(MsgApiException.class, () -> itemPedidoService.deleteByIdAndPedido(1L, PEDIDO_ID, EMPRESA_ID));
		verify(itemPedidoRepository, never()).deleteByIdAndPedido(anyLong(), anyLong(), anyLong());
	}

	@Test
	public void testSalvarPedidoSemId() {
		ItemPedido item = item(null);
		item.setPedido(new Pedido());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.salvar(item));

		assertEquals("Pedido deve ser informado para o item.", erro.getMessage());
		verifyNoInteractions(pedidoRepository);
	}

	@Test
	public void testSalvarProdutoNulo() {
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.of(pedido(PEDIDO_ID, "PED-3")));
		ItemPedido item = item(null);
		item.setProduto(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.salvar(item));

		assertEquals("Produto deve ser informado para o item.", erro.getMessage());
	}

}
