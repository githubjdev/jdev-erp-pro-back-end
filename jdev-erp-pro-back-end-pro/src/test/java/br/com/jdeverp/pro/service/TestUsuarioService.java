package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.AlterarSenhaDTO;
import br.com.jdeverp.pro.dto.UsuarioDto;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.repository.UsuarioRepository;

/** Cadastro, edição e exclusão de usuários são só de ROLE_ADMIN; senha, só a própria. */
@ExtendWith(MockitoExtension.class)
public class TestUsuarioService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private UsuarioService usuarioService;

	private static AlterarSenhaDTO alterarSenha(Long id) {
		AlterarSenhaDTO dto = new AlterarSenhaDTO();
		dto.setId(id);
		dto.setSenhaAtual("Senha@Atual1");
		dto.setNovaSenha("Senha@Nova1");
		dto.setConfirmarSenha("Senha@Nova1");
		return dto;
	}

	@Test
	public void testSalvarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.salvar(new UsuarioDto()));

		assertEquals("Apenas administradores podem cadastrar novos usuários.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testAtualizarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.atualizar(new UsuarioDto()));

		assertEquals("Apenas administradores podem atualizar usuários.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testAlterarSenhaDeOutroUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);
		when(usuarioLogadoService.getUsuarioLogadoId()).thenReturn(1L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.alterarSenha(alterarSenha(2L)));

		assertEquals("Você só pode alterar a sua própria senha.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testDeleteByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deleteById(2L, EMPRESA_ID));

		assertEquals("Apenas administradores podem deletar usuários.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem deletar usuários.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(usuarioRepository);
	}

}
