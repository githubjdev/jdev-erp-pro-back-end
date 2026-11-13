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

import br.com.jdeverp.pro.dto.PedidoDTO;
import br.com.jdeverp.pro.enums.FormaPagamento;
import br.com.jdeverp.pro.enums.StatusPedido;
import br.com.jdeverp.pro.enums.TipoPedido;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;

/** Pedido da empresa logada, com vendedor e cliente da mesma empresa. Exclusão em lote só por ROLE_ADMIN (P21). */
@ExtendWith(MockitoExtension.class)
public class TestPedidoService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private PedidoRepository pedidoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private PedidoService pedidoService;

	private Empresa empresa;

	@BeforeEach
	public void configurar() {
		empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
		lenient().when(usuarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(usuario(1L, "Vera Vendedora")));
		lenient().when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(usuario(2L, "Carlos Cliente")));
	}

	private static Usuario usuario(Long id, String nome) {
		Pessoa pessoa = new Pessoa();
		pessoa.setNome(nome);
		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setPessoa(pessoa);
		Usuario usuario = referencia(id);
		usuario.setClienteFuncionario(clienteFuncionario);
		return usuario;
	}

	private static Usuario referencia(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		return usuario;
	}

	/** Pedido como vem do request: vendedor (1) e cliente (2) só com o id. */
	private static Pedido pedido(Long id, String numero) {
		Pedido pedido = new Pedido();
		pedido.setId(id);
		pedido.setNumeroPedido(numero);
		pedido.setStatusPedido(StatusPedido.AGUARDANDO_PAGAMENTO);
		pedido.setFormaPagamento(FormaPagamento.PIX);
		pedido.setTipoPedido(TipoPedido.VENDA);
		pedido.setDataPedido(LocalDate.of(2026, 11, 10));
		pedido.setSubTotal(new BigDecimal("100.00"));
		pedido.setDesconto(new BigDecimal("5.00"));
		pedido.setFrete(new BigDecimal("10.00"));
		pedido.setTaxas(new BigDecimal("1.00"));
		pedido.setTotal(new BigDecimal("106.00"));
		pedido.setObservacao("Entregar à tarde");
		pedido.setVendedor(referencia(1L));
		pedido.setCliente(referencia(2L));
		return pedido;
	}

	private Pedido salvo(Long id, String numero) {
		Pedido pedido = pedido(id, numero);
		pedido.setVendedor(usuario(1L, "Vera Vendedora"));
		pedido.setCliente(usuario(2L, "Carlos Cliente"));
		pedido.setEmpresa(empresa);
		return pedido;
	}

	@Test
	public void testSalvar() {
		when(pedidoRepository.existePorNumeroPedido("PED-1", EMPRESA_ID)).thenReturn(false);
		when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

		PedidoDTO dto = pedidoService.salvar(pedido(null, "PED-1"));

		assertEquals("PED-1", dto.getNumeroPedido());
		/* Vendedor e cliente trocados pelos do banco: o DTO sai com os nomes */
		assertEquals("Vera Vendedora", dto.getVendedorNome());
		assertEquals("Carlos Cliente", dto.getClienteNome());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
		assertEquals(new BigDecimal("106.00"), dto.getTotal());
		assertEquals(new BigDecimal("10.00"), dto.getFrete());
		assertEquals(FormaPagamento.PIX, dto.getFormaPagamento());
		assertEquals(TipoPedido.VENDA, dto.getTipoPedido());
		assertEquals("Entregar à tarde", dto.getObservacao());
	}

	@Test
	public void testSalvarNumeroDuplicado() {
		when(pedidoRepository.existePorNumeroPedido("PED-1", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.salvar(pedido(null, "PED-1")));

		assertEquals("Já existe um pedido com o mesmo número para a empresa logada.", erro.getMessage());
		verify(pedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSemVendedor() {
		Pedido pedido = pedido(null, "PED-1");
		pedido.setVendedor(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.salvar(pedido));

		assertEquals("Cliente e vendedor devem ser informados para o pedido.", erro.getMessage());
		verify(pedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarClienteSemId() {
		Pedido pedido = pedido(null, "PED-1");
		pedido.setCliente(new Usuario());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.salvar(pedido));

		assertEquals("Cliente e vendedor devem ser informados para o pedido.", erro.getMessage());
	}

	@Test
	public void testSalvarClienteDeOutraEmpresa() {
		when(usuarioRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());
		Pedido pedido = pedido(null, "PED-1");
		pedido.setCliente(referencia(99L));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.salvar(pedido));

		assertEquals("Usuário relacionado não encontrado para a empresa logada.", erro.getMessage());
		verify(pedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizar() {
		Pedido existente = salvo(5L, "PED-1");
		when(pedidoRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		when(pedidoRepository.existePorNumeroPedidoDiferenteId(5L, "PED-2", EMPRESA_ID)).thenReturn(false);
		when(pedidoRepository.saveAndFlush(existente)).thenReturn(existente);
		Pedido alteracao = pedido(5L, "PED-2");
		alteracao.setStatusPedido(StatusPedido.CANCELADO);
		alteracao.setDataPagamento(LocalDate.of(2026, 11, 11));
		alteracao.setDataCancelamento(LocalDate.of(2026, 11, 12));
		alteracao.setTotal(new BigDecimal("50.00"));

		PedidoDTO dto = pedidoService.atualizar(alteracao);

		assertEquals("PED-2", dto.getNumeroPedido());
		assertEquals(StatusPedido.CANCELADO, dto.getStatusPedido());
		assertEquals(LocalDate.of(2026, 11, 11), dto.getDataPagamento());
		assertEquals(LocalDate.of(2026, 11, 12), dto.getDataCancelamento());
		assertEquals(new BigDecimal("50.00"), dto.getTotal());
		assertEquals("Vera Vendedora", dto.getVendedorNome());
		/* Grava o registro da empresa logada, não o objeto do request */
		verify(pedidoRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.atualizar(pedido(null, "PED-1")));

		assertEquals("Id do pedido não informado para atualizar.", erro.getMessage());
		verifyNoInteractions(pedidoRepository);
	}

	@Test
	public void testAtualizarPedidoDeOutraEmpresa() {
		when(pedidoRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.atualizar(pedido(99L, "PED-1")));

		assertEquals("Pedido não encontrado para a empresa logada.", erro.getMessage());
		verify(pedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarNumeroDuplicado() {
		when(pedidoRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(salvo(5L, "PED-1")));
		when(pedidoRepository.existePorNumeroPedidoDiferenteId(5L, "PED-2", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.atualizar(pedido(5L, "PED-2")));

		assertEquals("Já existe outro pedido com o mesmo número para a empresa logada.", erro.getMessage());
		verify(pedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarVendedorDeOutraEmpresa() {
		when(pedidoRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(salvo(5L, "PED-1")));
		when(usuarioRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());
		Pedido alteracao = pedido(5L, "PED-1");
		alteracao.setVendedor(referencia(99L));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.atualizar(alteracao));

		assertEquals("Usuário relacionado não encontrado para a empresa logada.", erro.getMessage());
		verify(pedidoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testFindAll() {
		when(pedidoRepository.findAll(EMPRESA_ID)).thenReturn(List.of(salvo(1L, "PED-1"), salvo(2L, "PED-2")));

		assertEquals(List.of("PED-1", "PED-2"), pedidoService.findAll(EMPRESA_ID).stream().map(PedidoDTO::getNumeroPedido).toList());
	}

	@Test
	public void testBuscaPorNumeroPedido() {
		when(pedidoRepository.buscaPorNumeroPedido("PED", EMPRESA_ID)).thenReturn(List.of(salvo(1L, "PED-1")));

		assertEquals("Carlos Cliente", pedidoService.buscaPorNumeroPedido("PED", EMPRESA_ID).get(0).getClienteNome());
	}

	@Test
	public void testExistePorNumeroPedido() {
		when(pedidoRepository.existePorNumeroPedido("PED-1", EMPRESA_ID)).thenReturn(true);

		assertTrue(pedidoService.existePorNumeroPedido("PED-1", EMPRESA_ID));
		assertFalse(pedidoService.existePorNumeroPedido("PED-2", EMPRESA_ID));
	}

	@Test
	public void testExistePorNumeroPedidoDiferenteId() {
		when(pedidoRepository.existePorNumeroPedidoDiferenteId(1L, "PED-1", EMPRESA_ID)).thenReturn(true);

		assertTrue(pedidoService.existePorNumeroPedidoDiferenteId(1L, "PED-1", EMPRESA_ID));
		assertFalse(pedidoService.existePorNumeroPedidoDiferenteId(2L, "PED-1", EMPRESA_ID));
	}

	@Test
	public void testDeleteById() {
		when(pedidoRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		pedidoService.deleteById(1L, EMPRESA_ID);

		verify(pedidoRepository).deleteById(1L, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdNaoEncontrado() {
		when(pedidoRepository.existsById(1L, EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.deleteById(1L, EMPRESA_ID));

		assertEquals("Pedido não encontrado ou já foi deletado.", erro.getMessage());
		verify(pedidoRepository, never()).deleteById(anyLong(), anyLong());
	}

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pedidoRepository.total(EMPRESA_ID)).thenReturn(2L);
		when(pedidoRepository.deleteAll(EMPRESA_ID)).thenReturn(2L);

		assertEquals(2L, pedidoService.deleteAll(EMPRESA_ID));
	}

	@Test
	public void testDeleteAllSemRegistros() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pedidoRepository.total(EMPRESA_ID)).thenReturn(0L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.deleteAll(EMPRESA_ID));

		assertEquals("Nenhum pedido encontrado para deletar ou todos já foram deletados.", erro.getMessage());
		verify(pedidoRepository, never()).deleteAll(anyLong());
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(pedidoRepository);
	}

	@Test
	public void testDeletarAllById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pedidoRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salvo(1L, "A"), salvo(2L, "B")));

		pedidoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID);

		verify(pedidoRepository).deletarAllById(List.of(1L, 2L), EMPRESA_ID);
	}

	@Test
	public void testDeletarAllByIdComIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pedidoRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salvo(2L, "B")));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Pedidos não encontrados ou já deletados: [1]", erro.getMessage());
		verify(pedidoRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdListaVazia() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pedidoRepository.buscarPorIds(List.of(), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.deletarAllById(List.of(), EMPRESA_ID));

		assertEquals("Nenhum registro informado para deletar.", erro.getMessage());
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(pedidoRepository);
	}

	@Test
	public void testBuscarPorIds() {
		when(pedidoRepository.buscarPorIds(List.of(1L), EMPRESA_ID)).thenReturn(List.of(salvo(1L, "PED-1")));

		assertEquals("PED-1", pedidoService.buscarPorIds(List.of(1L), EMPRESA_ID).get(0).getNumeroPedido());
	}

	@Test
	public void testExistsById() {
		when(pedidoRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		assertTrue(pedidoService.existsById(1L, EMPRESA_ID));
		assertFalse(pedidoService.existsById(2L, EMPRESA_ID));
	}

	@Test
	public void testListar() {
		when(pedidoRepository.listar(EMPRESA_ID)).thenReturn(List.of(salvo(1L, "PED-1")));

		assertEquals(1, pedidoService.listar(EMPRESA_ID).size());
	}

	@Test
	public void testBuscarPorId() {
		when(pedidoRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L, "PED-1")));
		when(pedidoRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		assertEquals("PED-1", pedidoService.buscarPorId(1L, EMPRESA_ID).orElseThrow().getNumeroPedido());
		assertTrue(pedidoService.buscarPorId(2L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testTotal() {
		when(pedidoRepository.total(EMPRESA_ID)).thenReturn(7L);

		assertEquals(7L, pedidoService.total(EMPRESA_ID));
	}

	@Test
	public void testListarPaginado() {
		Pageable pagina = PageRequest.of(0, 1);
		when(pedidoRepository.listarPaginado(EMPRESA_ID, pagina)).thenReturn(new PageImpl<>(List.of(salvo(1L, "PED-1")), pagina, 4));

		assertEquals(4, pedidoService.listarPaginado(EMPRESA_ID, pagina).getTotalElements());
	}

}
