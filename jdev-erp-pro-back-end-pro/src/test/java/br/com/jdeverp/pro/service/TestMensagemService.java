package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

import br.com.jdeverp.pro.dto.MensagemDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Mensagem;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import br.com.jdeverp.pro.repository.MensagemRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;

/**
 * Mensagem de chamado: chamado, atendente e cliente da empresa logada.
 * Exclusão em lote só por ROLE_ADMIN (P21).
 */
@ExtendWith(MockitoExtension.class)
public class TestMensagemService {

	private static final Long EMPRESA_ID = 10L;
	private static final Long CHAMADO_ID = 3L;

	@Mock
	private MensagemRepository mensagemRepository;

	@Mock
	private ChamadoRepository chamadoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private MensagemService mensagemService;

	private Empresa empresa;

	@BeforeEach
	public void configurar() {
		empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
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

	private static Chamado chamado(Long id, String titulo) {
		Chamado chamado = new Chamado();
		chamado.setId(id);
		chamado.setTitulo(titulo);
		return chamado;
	}

	/** Mensagem como vem do request: chamado (3), atendente (1) e cliente (2) só com o id. */
	private static Mensagem mensagem(Long id) {
		Mensagem mensagem = new Mensagem();
		mensagem.setId(id);
		mensagem.setConteudo("A impressora voltou?");
		mensagem.setArquivo("arquivo-base64");
		mensagem.setLida(false);
		mensagem.setChamado(chamado(CHAMADO_ID, null));
		mensagem.setAtendente(referencia(1L));
		mensagem.setCliente(referencia(2L));
		return mensagem;
	}

	private Mensagem salva(Long id, Long chamadoId) {
		Mensagem mensagem = mensagem(id);
		mensagem.setDataEnvio(LocalDate.of(2026, 11, 1));
		mensagem.setChamado(chamado(chamadoId, "Impressora"));
		mensagem.setAtendente(usuario(1L, "Ana Atendente"));
		mensagem.setCliente(usuario(2L, "Carlos Cliente"));
		mensagem.setEmpresa(empresa);
		return mensagem;
	}

	private void relacionamentosDaEmpresa() {
		when(chamadoRepository.buscarPorId(CHAMADO_ID, EMPRESA_ID)).thenReturn(Optional.of(chamado(CHAMADO_ID, "Impressora")));
		when(usuarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(usuario(1L, "Ana Atendente")));
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(usuario(2L, "Carlos Cliente")));
	}

	@Test
	public void testSalvar() {
		relacionamentosDaEmpresa();
		when(mensagemRepository.saveAndFlush(any(Mensagem.class))).thenAnswer(i -> i.getArgument(0));
		Mensagem mensagem = mensagem(null);
		mensagem.setDataEnvio(LocalDate.of(2000, 1, 1));

		MensagemDTO dto = mensagemService.salvar(mensagem);

		/* Data de envio é sempre a de hoje */
		assertEquals(LocalDate.now(), dto.getDataEnvio());
		assertEquals("Impressora", dto.getChamadoTitulo());
		assertEquals(CHAMADO_ID, dto.getChamadoId());
		assertEquals("Ana Atendente", dto.getAtendenteNome());
		assertEquals("Carlos Cliente", dto.getClienteNome());
		assertEquals("A impressora voltou?", dto.getConteudo());
		assertEquals("arquivo-base64", dto.getArquivo());
		assertEquals(false, dto.getLida());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testSalvarSemChamado() {
		Mensagem mensagem = mensagem(null);
		mensagem.setChamado(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.salvar(mensagem));

		assertEquals("Chamado deve ser informado para a mensagem.", erro.getMessage());
		verifyNoInteractions(chamadoRepository, mensagemRepository);
	}

	@Test
	public void testSalvarChamadoDeOutraEmpresa() {
		when(chamadoRepository.buscarPorId(CHAMADO_ID, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.salvar(mensagem(null)));

		assertEquals("Chamado não encontrado para a empresa logada.", erro.getMessage());
		verifyNoInteractions(mensagemRepository);
	}

	@Test
	public void testSalvarSemAtendente() {
		when(chamadoRepository.buscarPorId(CHAMADO_ID, EMPRESA_ID)).thenReturn(Optional.of(chamado(CHAMADO_ID, "Impressora")));
		Mensagem mensagem = mensagem(null);
		mensagem.setAtendente(new Usuario());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.salvar(mensagem));

		assertEquals("Cliente e atendente devem ser informados para a mensagem.", erro.getMessage());
		verifyNoInteractions(mensagemRepository);
	}

	@Test
	public void testSalvarClienteDeOutraEmpresa() {
		when(chamadoRepository.buscarPorId(CHAMADO_ID, EMPRESA_ID)).thenReturn(Optional.of(chamado(CHAMADO_ID, "Impressora")));
		when(usuarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(usuario(1L, "Ana Atendente")));
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.salvar(mensagem(null)));

		assertEquals("Usuário relacionado não encontrado para a empresa logada.", erro.getMessage());
		verifyNoInteractions(mensagemRepository);
	}

	@Test
	public void testAtualizar() {
		Mensagem existente = salva(5L, CHAMADO_ID);
		relacionamentosDaEmpresa();
		when(mensagemRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		when(mensagemRepository.saveAndFlush(existente)).thenReturn(existente);
		Mensagem alteracao = mensagem(5L);
		alteracao.setConteudo("Conteúdo novo");
		alteracao.setLida(true);
		alteracao.setDataEnvio(LocalDate.of(2000, 1, 1));

		MensagemDTO dto = mensagemService.atualizar(alteracao);

		assertEquals("Conteúdo novo", dto.getConteudo());
		assertEquals(true, dto.getLida());
		/* Data de envio não é editável */
		assertEquals(LocalDate.of(2026, 11, 1), dto.getDataEnvio());
		verify(mensagemRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.atualizar(mensagem(null)));

		assertEquals("Id da mensagem não informado para atualizar.", erro.getMessage());
		verifyNoInteractions(mensagemRepository);
	}

	@Test
	public void testAtualizarDeOutraEmpresa() {
		when(mensagemRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.atualizar(mensagem(99L)));

		assertEquals("Mensagem não encontrada para a empresa logada.", erro.getMessage());
		verify(mensagemRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarChamadoDeOutraEmpresa() {
		when(mensagemRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(salva(5L, CHAMADO_ID)));
		when(chamadoRepository.buscarPorId(CHAMADO_ID, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.atualizar(mensagem(5L)));

		assertEquals("Chamado não encontrado para a empresa logada.", erro.getMessage());
		verify(mensagemRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testFindAll() {
		when(mensagemRepository.findAll(EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID), salva(2L, CHAMADO_ID)));

		assertEquals(2, mensagemService.findAll(EMPRESA_ID).size());
	}

	@Test
	public void testBuscaPorConteudo() {
		when(mensagemRepository.buscaPorConteudo("impressora", EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(1L, mensagemService.buscaPorConteudo("impressora", EMPRESA_ID).get(0).getId());
	}

	@Test
	public void testExistePorConteudo() {
		when(mensagemRepository.existePorConteudo("Oi", EMPRESA_ID)).thenReturn(true);

		assertTrue(mensagemService.existePorConteudo("Oi", EMPRESA_ID));
		assertFalse(mensagemService.existePorConteudo("Tchau", EMPRESA_ID));
	}

	@Test
	public void testExistePorConteudoDiferenteId() {
		when(mensagemRepository.existePorConteudoDiferenteId(1L, "Oi", EMPRESA_ID)).thenReturn(true);

		assertTrue(mensagemService.existePorConteudoDiferenteId(1L, "Oi", EMPRESA_ID));
		assertFalse(mensagemService.existePorConteudoDiferenteId(2L, "Oi", EMPRESA_ID));
	}

	@Test
	public void testDeleteById() {
		when(mensagemRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		mensagemService.deleteById(1L, EMPRESA_ID);

		verify(mensagemRepository).deleteById(1L, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdNaoEncontrada() {
		when(mensagemRepository.existsById(1L, EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.deleteById(1L, EMPRESA_ID));

		assertEquals("Mensagem não encontrada ou já foi deletada.", erro.getMessage());
		verify(mensagemRepository, never()).deleteById(anyLong(), anyLong());
	}

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(mensagemRepository.total(EMPRESA_ID)).thenReturn(5L);
		when(mensagemRepository.deleteAll(EMPRESA_ID)).thenReturn(5L);

		assertEquals(5L, mensagemService.deleteAll(EMPRESA_ID));
	}

	@Test
	public void testDeleteAllSemRegistros() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(mensagemRepository.total(EMPRESA_ID)).thenReturn(0L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.deleteAll(EMPRESA_ID));

		assertEquals("Nenhuma mensagem encontrada para deletar ou todas já foram deletadas.", erro.getMessage());
		verify(mensagemRepository, never()).deleteAll(anyLong());
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
	public void testDeletarAllById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(mensagemRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID), salva(2L, CHAMADO_ID)));

		mensagemService.deletarAllById(List.of(1L, 2L), EMPRESA_ID);

		verify(mensagemRepository).deletarAllById(List.of(1L, 2L), EMPRESA_ID);
	}

	@Test
	public void testDeletarAllByIdComIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(mensagemRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		assertEquals("Mensagens não encontradas ou já deletadas: [2]", erro.getMessage());
		verify(mensagemRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdListaVazia() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(mensagemRepository.buscarPorIds(List.of(), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.deletarAllById(List.of(), EMPRESA_ID));

		assertEquals("Nenhum registro informado para deletar.", erro.getMessage());
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.deletarAllById(List.of(1L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(mensagemRepository);
	}

	@Test
	public void testBuscarPorIds() {
		when(mensagemRepository.buscarPorIds(List.of(1L), EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(1, mensagemService.buscarPorIds(List.of(1L), EMPRESA_ID).size());
	}

	@Test
	public void testExistsById() {
		when(mensagemRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		assertTrue(mensagemService.existsById(1L, EMPRESA_ID));
		assertFalse(mensagemService.existsById(2L, EMPRESA_ID));
	}

	@Test
	public void testListar() {
		when(mensagemRepository.listar(EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(1, mensagemService.listar(EMPRESA_ID).size());
	}

	@Test
	public void testBuscarPorId() {
		when(mensagemRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L, CHAMADO_ID)));
		when(mensagemRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		assertEquals("Impressora", mensagemService.buscarPorId(1L, EMPRESA_ID).orElseThrow().getChamadoTitulo());
		assertTrue(mensagemService.buscarPorId(2L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testTotal() {
		when(mensagemRepository.total(EMPRESA_ID)).thenReturn(11L);

		assertEquals(11L, mensagemService.total(EMPRESA_ID));
	}

	@Test
	public void testListarPaginado() {
		Pageable pagina = PageRequest.of(0, 1);
		when(mensagemRepository.listarPaginado(EMPRESA_ID, pagina)).thenReturn(new PageImpl<>(List.of(salva(1L, CHAMADO_ID)), pagina, 2));

		assertEquals(2, mensagemService.listarPaginado(EMPRESA_ID, pagina).getTotalElements());
	}

	@Test
	public void testFindAllByChamado() {
		when(mensagemRepository.findAllByChamado(CHAMADO_ID, EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(CHAMADO_ID, mensagemService.findAllByChamado(CHAMADO_ID, EMPRESA_ID).get(0).getChamadoId());
	}

	@Test
	public void testBuscaPorConteudoByChamado() {
		when(mensagemRepository.buscaPorConteudoByChamado("imp", CHAMADO_ID, EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(1, mensagemService.buscaPorConteudoByChamado("imp", CHAMADO_ID, EMPRESA_ID).size());
	}

	@Test
	public void testExistePorConteudoByChamado() {
		when(mensagemRepository.existePorConteudoByChamado("Oi", CHAMADO_ID, EMPRESA_ID)).thenReturn(true);

		assertTrue(mensagemService.existePorConteudoByChamado("Oi", CHAMADO_ID, EMPRESA_ID));
		assertFalse(mensagemService.existePorConteudoByChamado("Oi", 8L, EMPRESA_ID));
	}

	@Test
	public void testExistePorConteudoDiferenteIdByChamado() {
		when(mensagemRepository.existePorConteudoDiferenteIdByChamado(1L, "Oi", CHAMADO_ID, EMPRESA_ID)).thenReturn(true);

		assertTrue(mensagemService.existePorConteudoDiferenteIdByChamado(1L, "Oi", CHAMADO_ID, EMPRESA_ID));
		assertFalse(mensagemService.existePorConteudoDiferenteIdByChamado(2L, "Oi", CHAMADO_ID, EMPRESA_ID));
	}

	@Test
	public void testCountByChamado() {
		when(mensagemRepository.countByChamado(CHAMADO_ID, EMPRESA_ID)).thenReturn(4L);

		assertEquals(4L, mensagemService.countByChamado(CHAMADO_ID, EMPRESA_ID));
	}

	@Test
	public void testDeleteAllByChamado() {
		when(mensagemRepository.countByChamado(CHAMADO_ID, EMPRESA_ID)).thenReturn(4L);

		mensagemService.deleteAllByChamado(CHAMADO_ID, EMPRESA_ID);

		verify(mensagemRepository).deleteAllByChamado(CHAMADO_ID, EMPRESA_ID);
	}

	@Test
	public void testDeleteAllByChamadoSemMensagens() {
		when(mensagemRepository.countByChamado(CHAMADO_ID, EMPRESA_ID)).thenReturn(0L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.deleteAllByChamado(CHAMADO_ID, EMPRESA_ID));

		assertEquals("Nenhuma mensagem encontrada para o chamado ou todas já foram deletadas.", erro.getMessage());
		verify(mensagemRepository, never()).deleteAllByChamado(anyLong(), anyLong());
	}

	@Test
	public void testDeleteByIdAndChamado() {
		when(mensagemRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L, CHAMADO_ID)));

		mensagemService.deleteByIdAndChamado(1L, CHAMADO_ID, EMPRESA_ID);

		verify(mensagemRepository).deleteByIdAndChamado(1L, CHAMADO_ID, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdAndChamadoMensagemDeOutroChamado() {
		when(mensagemRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salva(1L, 77L)));

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> mensagemService.deleteByIdAndChamado(1L, CHAMADO_ID, EMPRESA_ID));

		assertEquals("Mensagem não encontrada ou já foi deletada.", erro.getMessage());
		verify(mensagemRepository, never()).deleteByIdAndChamado(anyLong(), anyLong(), anyLong());
	}

	@Test
	public void testDeleteByIdAndChamadoMensagemSemChamado() {
		Mensagem semChamado = salva(1L, CHAMADO_ID);
		semChamado.setChamado(null);
		when(mensagemRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(semChamado));

		assertThrows(MsgApiException.class, () -> mensagemService.deleteByIdAndChamado(1L, CHAMADO_ID, EMPRESA_ID));
		verify(mensagemRepository, never()).deleteByIdAndChamado(anyLong(), anyLong(), anyLong());
	}

	@Test
	public void testFindAllNaoLidas() {
		when(mensagemRepository.findAllNaoLidas(EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(false, mensagemService.findAllNaoLidas(EMPRESA_ID).get(0).getLida());
	}

	@Test
	public void testFindAllNaoLidasByChamado() {
		when(mensagemRepository.findAllNaoLidasByChamado(CHAMADO_ID, EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(1, mensagemService.findAllNaoLidasByChamado(CHAMADO_ID, EMPRESA_ID).size());
	}

	@Test
	public void testCountNaoLidasByChamado() {
		when(mensagemRepository.countNaoLidasByChamado(CHAMADO_ID, EMPRESA_ID)).thenReturn(2L);

		assertEquals(2L, mensagemService.countNaoLidasByChamado(CHAMADO_ID, EMPRESA_ID));
	}

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
	public void testFindAllByAtendente() {
		when(mensagemRepository.findAllByAtendente(1L, EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(1L, mensagemService.findAllByAtendente(1L, EMPRESA_ID).get(0).getAtendenteId());
	}

	@Test
	public void testCountByAtendente() {
		when(mensagemRepository.countByAtendente(1L, EMPRESA_ID)).thenReturn(3L);

		assertEquals(3L, mensagemService.countByAtendente(1L, EMPRESA_ID));
	}

	@Test
	public void testFindAllByCliente() {
		when(mensagemRepository.findAllByCliente(2L, EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(2L, mensagemService.findAllByCliente(2L, EMPRESA_ID).get(0).getClienteId());
	}

	@Test
	public void testCountByCliente() {
		when(mensagemRepository.countByCliente(2L, EMPRESA_ID)).thenReturn(1L);

		assertEquals(1L, mensagemService.countByCliente(2L, EMPRESA_ID));
	}

	@Test
	public void testFindAllByChamadoAndAtendente() {
		when(mensagemRepository.findAllByChamadoAndAtendente(CHAMADO_ID, 1L, EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(1, mensagemService.findAllByChamadoAndAtendente(CHAMADO_ID, 1L, EMPRESA_ID).size());
	}

	@Test
	public void testFindAllNaoLidasByAtendente() {
		when(mensagemRepository.findAllNaoLidasByAtendente(1L, EMPRESA_ID)).thenReturn(List.of(salva(1L, CHAMADO_ID)));

		assertEquals(1, mensagemService.findAllNaoLidasByAtendente(1L, EMPRESA_ID).size());
	}

	@Test
	public void testFindAllNaoLidasByCliente() {
		when(mensagemRepository.findAllNaoLidasByCliente(2L, EMPRESA_ID)).thenReturn(List.of());

		assertTrue(mensagemService.findAllNaoLidasByCliente(2L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testSalvarChamadoSemId() {
		Mensagem mensagem = mensagem(null);
		mensagem.setChamado(new Chamado());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.salvar(mensagem));

		assertEquals("Chamado deve ser informado para a mensagem.", erro.getMessage());
		verifyNoInteractions(chamadoRepository);
	}

	@Test
	public void testSalvarSemCliente() {
		when(chamadoRepository.buscarPorId(CHAMADO_ID, EMPRESA_ID)).thenReturn(Optional.of(chamado(CHAMADO_ID, "Impressora")));
		when(usuarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(usuario(1L, "Ana Atendente")));
		Mensagem mensagem = mensagem(null);
		mensagem.setCliente(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> mensagemService.salvar(mensagem));

		assertEquals("Cliente e atendente devem ser informados para a mensagem.", erro.getMessage());
	}

}
