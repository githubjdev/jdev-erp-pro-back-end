package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import br.com.jdeverp.pro.repository.MensagemRepository;

@ExtendWith(MockitoExtension.class)
public class TestMensagemService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private MensagemRepository mensagemRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private MensagemService mensagemService;

	@Test
	public void testUpdateLida() {
		when(mensagemRepository.existsById(5L, EMPRESA_ID)).thenReturn(true);

		mensagemService.updateLida(5L, true, EMPRESA_ID);

		verify(mensagemRepository).updateLida(5L, true, EMPRESA_ID);
	}

	@Test
	public void testUpdateLidaMensagemDeOutraEmpresa() {
		when(mensagemRepository.existsById(99L, EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.updateLida(99L, true, EMPRESA_ID));

		assertEquals("Mensagem não encontrada para a empresa logada.", erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
		verify(mensagemRepository, never()).updateLida(anyLong(), anyBoolean(), anyLong());
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(mensagemRepository);
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.deletarAllById(List.of(1L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(mensagemRepository);
	}

}
