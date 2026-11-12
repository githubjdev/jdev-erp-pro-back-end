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
import br.com.jdeverp.pro.repository.PedidoRepository;

/** Exclusão em lote apaga tudo da empresa: só ROLE_ADMIN (P21). */
@ExtendWith(MockitoExtension.class)
public class TestPedidoService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private PedidoRepository pedidoRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private PedidoService pedidoService;

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(pedidoRepository.total(EMPRESA_ID)).thenReturn(2L);
		when(pedidoRepository.deleteAll(EMPRESA_ID)).thenReturn(2L);

		assertEquals(2L, pedidoService.deleteAll(EMPRESA_ID));
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
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> pedidoService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(pedidoRepository);
	}

}
