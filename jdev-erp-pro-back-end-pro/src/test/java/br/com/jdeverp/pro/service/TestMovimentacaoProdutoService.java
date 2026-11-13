package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.time.LocalDate;
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

import br.com.jdeverp.pro.dto.MovimentacaoProdutoDTO;
import br.com.jdeverp.pro.enums.TipoMovimentacaoProduto;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.MovimentacaoProduto;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.repository.MovimentacaoProdutoRepository;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;

/**
 * Movimentação de estoque: produto obrigatório e pedido opcional, os dois da
 * empresa logada. Exclusão em lote só por ROLE_ADMIN (P21).
 */
@ExtendWith(MockitoExtension.class)
public class TestMovimentacaoProdutoService {

	private static final Long EMPRESA_ID = 10L;
	private static final Long PEDIDO_ID = 3L;
	private static final Long PRODUTO_ID = 4L;

	@Mock
	private MovimentacaoProdutoRepository movimentacaoProdutoRepository;

	@Mock
	private ProdutoRepository produtoRepository;

	@Mock
	private PedidoRepository pedidoRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private MovimentacaoProdutoService movimentacaoProdutoService;

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

	/** Movimentação como vem do request: produto e pedido só com o id. */
	private static MovimentacaoProduto movimentacao(Long id, Long pedidoId) {
		MovimentacaoProduto movimentacao = new MovimentacaoProduto();
		movimentacao.setId(id);
		movimentacao.setQuantidade(3.0);
		movimentacao.setValor(new BigDecimal("30.00"));
		movimentacao.setTipoMovimentacaoProduto(TipoMovimentacaoProduto.ENTRADA_COMPRA);
		movimentacao.setProduto(produto(PRODUTO_ID, null));
		if (pedidoId != null) {
			movimentacao.setPedido(pedido(pedidoId, null));
		}
		return movimentacao;
	}

	private MovimentacaoProduto salva(Long id, Long pedidoId) {
		MovimentacaoProduto movimentacao = movimentacao(id, null);
		movimentacao.setDataMovimento(LocalDate.of(2026, 11, 1));
		movimentacao.setProduto(produto(PRODUTO_ID, "Mouse"));
		movimentacao.setPedido(pedidoId == null ? null : pedido(pedidoId, "PED-" + pedidoId));
		movimentacao.setEmpresa(empresa);
		return movimentacao;
	}

	private void produtoDaEmpresa() {
		when(produtoRepository.buscarPorId(PRODUTO_ID, EMPRESA_ID)).thenReturn(Optional.of(produto(PRODUTO_ID, "Mouse")));
	}

	private void salvarDevolveOProprio() {
		when(movimentacaoProdutoRepository.saveAndFlush(any(MovimentacaoProduto.class))).thenAnswer(i -> i.getArgument(0));
	}

	@Test
	public void testSalvarSemPedido() {
		produtoDaEmpresa();
		salvarDevolveOProprio();

		MovimentacaoProdutoDTO dto = movimentacaoProdutoService.salvar(movimentacao(null, null));

		assertEquals("Mouse", dto.getProdutoNome());
		assertEquals(PRODUTO_ID, dto.getProdutoId());
		/* Pedido é opcional: movimentação avulsa */
		assertNull(dto.getPedidoId());
		assertNull(dto.getNumeroPedido());
		/* Sem data no request, vale a de hoje */
		assertEquals(LocalDate.now(), dto.getDataMovimento());
		assertEquals(3.0, dto.getQuantidade());
		assertEquals(new BigDecimal("30.00"), dto.getValor());
		assertEquals(TipoMovimentacaoProduto.ENTRADA_COMPRA, dto.getTipoMovimentacaoProduto());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
		verifyNoInteractions(pedidoRepository);
	}

	@Test
	public void testSalvarComPedidoEData() {
		produtoDaEmpresa();
		salvarDevolveOProprio();
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.of(pedido(PEDIDO_ID, "PED-3")));
		MovimentacaoProduto movimentacao = movimentacao(null, PEDIDO_ID);
		movimentacao.setDataMovimento(LocalDate.of(2026, 10, 5));

		MovimentacaoProdutoDTO dto = movimentacaoProdutoService.salvar(movimentacao);

		assertEquals(PEDIDO_ID, dto.getPedidoId());
		assertEquals("PED-3", dto.getNumeroPedido());
		assertEquals(LocalDate.of(2026, 10, 5), dto.getDataMovimento());
	}

	@Test
	public void testSalvarPedidoSemIdETratadoComoAvulsa() {
		produtoDaEmpresa();
		salvarDevolveOProprio();
		MovimentacaoProduto movimentacao = movimentacao(null, null);
		movimentacao.setPedido(new Pedido());

		assertNull(movimentacaoProdutoService.salvar(movimentacao).getPedidoId());
		verifyNoInteractions(pedidoRepository);
	}

	@Test
	public void testSalvarSemProduto() {
		MovimentacaoProduto movimentacao = movimentacao(null, null);
		movimentacao.setProduto(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.salvar(movimentacao));

		assertEquals("Produto deve ser informado para a movimentação.", erro.getMessage());
		verifyNoInteractions(produtoRepository, movimentacaoProdutoRepository);
	}

	@Test
	public void testSalvarProdutoDeOutraEmpresa() {
		when(produtoRepository.buscarPorId(PRODUTO_ID, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.salvar(movimentacao(null, null)));

		assertEquals("Produto não encontrado para a empresa logada.", erro.getMessage());
		verifyNoInteractions(movimentacaoProdutoRepository);
	}

	@Test
	public void testSalvarPedidoDeOutraEmpresa() {
		produtoDaEmpresa();
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.salvar(movimentacao(null, PEDIDO_ID)));

		assertEquals("Pedido não encontrado para a empresa logada.", erro.getMessage());
		verifyNoInteractions(movimentacaoProdutoRepository);
	}

	@Test
	public void testAtualizar() {
		MovimentacaoProduto existente = salva(1L, null);
		produtoDaEmpresa();
		when(pedidoRepository.buscarPorId(PEDIDO_ID, EMPRESA_ID)).thenReturn(Optional.of(pedido(PEDIDO_ID, "PED-3")));
		when(movimentacaoProdutoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		when(movimentacaoProdutoRepository.saveAndFlush(existente)).thenReturn(existente);
		MovimentacaoProduto alteracao = movimentacao(1L, PEDIDO_ID);
		alteracao.setQuantidade(8.0);
		alteracao.setTipoMovimentacaoProduto(TipoMovimentacaoProduto.SAIDA_VENDA);
		alteracao.setDataMovimento(LocalDate.of(2026, 11, 12));

		MovimentacaoProdutoDTO dto = movimentacaoProdutoService.atualizar(alteracao);

		assertEquals(8.0, dto.getQuantidade());
		assertEquals(TipoMovimentacaoProduto.SAIDA_VENDA, dto.getTipoMovimentacaoProduto());
		assertEquals(LocalDate.of(2026, 11, 12), dto.getDataMovimento());
		assertEquals("PED-3", dto.getNumeroPedido());
		verify(movimentacaoProdutoRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarSemDataMantemADoCadastro() {
		MovimentacaoProduto existente = salva(1L, null);
		produtoDaEmpresa();
		when(movimentacaoProdutoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		when(movimentacaoProdutoRepository.saveAndFlush(existente)).thenReturn(existente);

		MovimentacaoProdutoDTO dto = movimentacaoProdutoService.atualizar(movimentacao(1L, null));

		assertEquals(LocalDate.of(2026, 11, 1), dto.getDataMovimento());
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.atualizar(movimentacao(null, null)));

		assertEquals("Id da movimentação de produto não informado para atualizar.", erro.getMessage());
		verifyNoInteractions(movimentacaoProdutoRepository);
	}

	@Test
	public void testAtualizarDeOutraEmpresa() {
		when(movimentacaoProdutoRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.atualizar(movimentacao(99L, null)));

		assertEquals("Movimentação de produto não encontrada para a empresa logada.", erro.getMessage());
		verify(movimentacaoProdutoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarProdutoDeOutraEmpresa() {
		when(movimentacaoProdutoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L, null)));
		when(produtoRepository.buscarPorId(PRODUTO_ID, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.atualizar(movimentacao(1L, null)));

		assertEquals("Produto não encontrado para a empresa logada.", erro.getMessage());
		verify(movimentacaoProdutoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testFindAll() {
		when(movimentacaoProdutoRepository.findAll(EMPRESA_ID)).thenReturn(List.of(salva(1L, null), salva(2L, PEDIDO_ID)));

		assertEquals(2, movimentacaoProdutoService.findAll(EMPRESA_ID).size());
	}

	@Test
	public void testBuscaPorNome() {
		when(movimentacaoProdutoRepository.buscaPorNome("mou", EMPRESA_ID)).thenReturn(List.of(salva(1L, null)));

		assertEquals("Mouse", movimentacaoProdutoService.buscaPorNome("mou", EMPRESA_ID).get(0).getProdutoNome());
	}

	@Test
	public void testExistePorNome() {
		when(movimentacaoProdutoRepository.existePorNome("Mouse", EMPRESA_ID)).thenReturn(true);

		assertTrue(movimentacaoProdutoService.existePorNome("Mouse", EMPRESA_ID));
		assertFalse(movimentacaoProdutoService.existePorNome("Teclado", EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		when(movimentacaoProdutoRepository.existePorNomeDiferenteId(1L, "Mouse", EMPRESA_ID)).thenReturn(true);

		assertTrue(movimentacaoProdutoService.existePorNomeDiferenteId(1L, "Mouse", EMPRESA_ID));
		assertFalse(movimentacaoProdutoService.existePorNomeDiferenteId(2L, "Mouse", EMPRESA_ID));
	}

	@Test
	public void testDeleteById() {
		when(movimentacaoProdutoRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		movimentacaoProdutoService.deleteById(1L, EMPRESA_ID);

		verify(movimentacaoProdutoRepository).deleteById(1L, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdNaoEncontrada() {
		when(movimentacaoProdutoRepository.existsById(1L, EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.deleteById(1L, EMPRESA_ID));

		assertEquals("Movimentação de produto não encontrada ou já foi deletada.", erro.getMessage());
		verify(movimentacaoProdutoRepository, never()).deleteById(anyLong(), anyLong());
	}

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(movimentacaoProdutoRepository.total(EMPRESA_ID)).thenReturn(2L);
		when(movimentacaoProdutoRepository.deleteAll(EMPRESA_ID)).thenReturn(2L);

		assertEquals(2L, movimentacaoProdutoService.deleteAll(EMPRESA_ID));
	}

	@Test
	public void testDeleteAllSemRegistros() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(movimentacaoProdutoRepository.total(EMPRESA_ID)).thenReturn(0L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.deleteAll(EMPRESA_ID));

		assertEquals("Nenhuma movimentação de produto encontrada para deletar ou todas já foram deletadas.", erro.getMessage());
		verify(movimentacaoProdutoRepository, never()).deleteAll(anyLong());
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(movimentacaoProdutoRepository);
	}

	@Test
	public void testDeletarAllById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(movimentacaoProdutoRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salva(1L, null), salva(2L, null)));

		movimentacaoProdutoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID);

		verify(movimentacaoProdutoRepository).deletarAllById(List.of(1L, 2L), EMPRESA_ID);
	}

	@Test
	public void testDeletarAllByIdComIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(movimentacaoProdutoRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salva(1L, null)));

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> movimentacaoProdutoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Movimentações de produto não encontradas ou já deletadas: [2]", erro.getMessage());
		verify(movimentacaoProdutoRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdListaVazia() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(movimentacaoProdutoRepository.buscarPorIds(List.of(), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.deletarAllById(List.of(), EMPRESA_ID));

		assertEquals("Nenhum registro informado para deletar.", erro.getMessage());
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> movimentacaoProdutoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(movimentacaoProdutoRepository);
	}

	@Test
	public void testBuscarPorIds() {
		when(movimentacaoProdutoRepository.buscarPorIds(List.of(1L), EMPRESA_ID)).thenReturn(List.of(salva(1L, null)));

		assertEquals(1, movimentacaoProdutoService.buscarPorIds(List.of(1L), EMPRESA_ID).size());
	}

	@Test
	public void testExistsById() {
		when(movimentacaoProdutoRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		assertTrue(movimentacaoProdutoService.existsById(1L, EMPRESA_ID));
		assertFalse(movimentacaoProdutoService.existsById(2L, EMPRESA_ID));
	}

	@Test
	public void testListar() {
		when(movimentacaoProdutoRepository.listar(EMPRESA_ID)).thenReturn(List.of(salva(1L, null)));

		assertEquals(1, movimentacaoProdutoService.listar(EMPRESA_ID).size());
	}

	@Test
	public void testBuscarPorId() {
		when(movimentacaoProdutoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L, PEDIDO_ID)));
		when(movimentacaoProdutoRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		assertEquals("PED-3", movimentacaoProdutoService.buscarPorId(1L, EMPRESA_ID).orElseThrow().getNumeroPedido());
		assertTrue(movimentacaoProdutoService.buscarPorId(2L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testTotal() {
		when(movimentacaoProdutoRepository.total(EMPRESA_ID)).thenReturn(6L);

		assertEquals(6L, movimentacaoProdutoService.total(EMPRESA_ID));
	}

	@Test
	public void testListarPaginado() {
		Pageable pagina = PageRequest.of(0, 1);
		when(movimentacaoProdutoRepository.listarPaginado(EMPRESA_ID, pagina)).thenReturn(new PageImpl<>(List.of(salva(1L, null)), pagina, 3));

		assertEquals(3, movimentacaoProdutoService.listarPaginado(EMPRESA_ID, pagina).getTotalElements());
	}

	@Test
	public void testFindAllByPedido() {
		when(movimentacaoProdutoRepository.findAllByPedido(PEDIDO_ID, EMPRESA_ID)).thenReturn(List.of(salva(1L, PEDIDO_ID)));

		assertEquals(PEDIDO_ID, movimentacaoProdutoService.findAllByPedido(PEDIDO_ID, EMPRESA_ID).get(0).getPedidoId());
	}

	@Test
	public void testBuscaPorNomeByPedido() {
		when(movimentacaoProdutoRepository.buscaPorNomeByPedido("mou", PEDIDO_ID, EMPRESA_ID)).thenReturn(List.of(salva(1L, PEDIDO_ID)));

		assertEquals("Mouse", movimentacaoProdutoService.buscaPorNomeByPedido("mou", PEDIDO_ID, EMPRESA_ID).get(0).getProdutoNome());
	}

	@Test
	public void testExistePorNomeByPedido() {
		when(movimentacaoProdutoRepository.existePorNomeByPedido("Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(true);

		assertTrue(movimentacaoProdutoService.existePorNomeByPedido("Mouse", PEDIDO_ID, EMPRESA_ID));
		assertFalse(movimentacaoProdutoService.existePorNomeByPedido("Mouse", 8L, EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteIdByPedido() {
		when(movimentacaoProdutoRepository.existePorNomeDiferenteIdByPedido(1L, "Mouse", PEDIDO_ID, EMPRESA_ID)).thenReturn(true);

		assertTrue(movimentacaoProdutoService.existePorNomeDiferenteIdByPedido(1L, "Mouse", PEDIDO_ID, EMPRESA_ID));
		assertFalse(movimentacaoProdutoService.existePorNomeDiferenteIdByPedido(2L, "Mouse", PEDIDO_ID, EMPRESA_ID));
	}

	@Test
	public void testDeleteByIdAndPedido() {
		when(movimentacaoProdutoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L, PEDIDO_ID)));

		movimentacaoProdutoService.deleteByIdAndPedido(1L, PEDIDO_ID, EMPRESA_ID);

		verify(movimentacaoProdutoRepository).deleteByIdAndPedido(1L, PEDIDO_ID, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdAndPedidoMovimentacaoAvulsa() {
		when(movimentacaoProdutoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L, null)));

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> movimentacaoProdutoService.deleteByIdAndPedido(1L, PEDIDO_ID, EMPRESA_ID));

		assertEquals("Movimentação de produto não encontrada ou já foi deletada.", erro.getMessage());
		verify(movimentacaoProdutoRepository, never()).deleteByIdAndPedido(anyLong(), anyLong(), anyLong());
	}

	@Test
	public void testDeleteByIdAndPedidoDeOutroPedido() {
		when(movimentacaoProdutoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L, 77L)));

		assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.deleteByIdAndPedido(1L, PEDIDO_ID, EMPRESA_ID));
		verify(movimentacaoProdutoRepository, never()).deleteByIdAndPedido(anyLong(), anyLong(), anyLong());
	}

	@Test
	public void testSalvarProdutoSemId() {
		MovimentacaoProduto movimentacao = movimentacao(null, null);
		movimentacao.setProduto(new Produto());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> movimentacaoProdutoService.salvar(movimentacao));

		assertEquals("Produto deve ser informado para a movimentação.", erro.getMessage());
		verifyNoInteractions(produtoRepository);
	}

}
