package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.repository.ItemPedidoRepository;

/** Exclusão em lote apaga tudo da empresa: só ROLE_ADMIN (P21). */
@ExtendWith(MockitoExtension.class)
public class TestItemPedidoService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private ItemPedidoRepository itemPedidoRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private ItemPedidoService itemPedidoService;

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(itemPedidoRepository.total(EMPRESA_ID)).thenReturn(2L);
		when(itemPedidoRepository.deleteAll(EMPRESA_ID)).thenReturn(2L);

		assertEquals(2L, itemPedidoService.deleteAll(EMPRESA_ID));
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
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> itemPedidoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(itemPedidoRepository);
	}

}
