package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.jdeverp.pro.dto.ChamadoDTO;
import br.com.jdeverp.pro.enums.PrioridadeChamado;
import br.com.jdeverp.pro.enums.StatusChamado;
import br.com.jdeverp.pro.enums.TipoChamado;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class TestChamadoService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private ChamadoRepository chamadoRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private ChamadoService chamadoService;

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
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setClienteFuncionario(clienteFuncionario);
		return usuario;
	}

	private static Usuario referencia(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		return usuario;
	}

	/** Chamado como vem do request: atendente (1) e cliente (2) só com o id. */
	private static Chamado chamado(Long id, String titulo) {
		Chamado chamado = new Chamado();
		chamado.setId(id);
		chamado.setTitulo(titulo);
		chamado.setDescricao("Não imprime");
		chamado.setTipoChamado(TipoChamado.SUPORTE);
		chamado.setPrioridadeChamado(PrioridadeChamado.ALTA);
		chamado.setStatusChamado(StatusChamado.ABERTO);
		chamado.setAtendente(referencia(1L));
		chamado.setCliente(referencia(2L));
		return chamado;
	}

	/** Chamado como vem do banco, com os usuários carregados e a empresa. */
	private Chamado salvo(Long id, String titulo) {
		Chamado chamado = chamado(id, titulo);
		chamado.setAbertoUser(usuario(1L, "Ana Atendente"));
		chamado.setAtendente(usuario(1L, "Ana Atendente"));
		chamado.setCliente(usuario(2L, "Carlos Cliente"));
		chamado.setDataAbertura(LocalDate.of(2026, 11, 1));
		chamado.setEmpresa(empresa);
		return chamado;
	}

	private void usuariosDaEmpresa() {
		lenient().when(usuarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(usuario(1L, "Ana Atendente")));
		lenient().when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(usuario(2L, "Carlos Cliente")));
		lenient().when(usuarioRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(usuario(3L, "Fábio Fechou")));
	}

	@Test
	public void testSalvarBuscaUsuarioLogadoNaSessao() {
		/* O usuário do JwtFilter vem sem o cadastro carregado (fora da sessão da requisição) */
		Usuario logadoDoFiltro = referencia(1L);
		Usuario logadoDoBanco = usuario(1L, "Ana Atendente");
		Usuario cliente = usuario(2L, "Carlos Cliente");

		when(usuarioLogadoService.getUsuarioLogado()).thenReturn(logadoDoFiltro);
		when(chamadoRepository.existePorTitulo("Impressora", EMPRESA_ID)).thenReturn(false);
		when(usuarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(logadoDoBanco));
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(cliente));
		when(chamadoRepository.saveAndFlush(any(Chamado.class))).thenAnswer(i -> i.getArgument(0));

		ChamadoDTO dto = chamadoService.salvar(chamado(null, "Impressora"));

		ArgumentCaptor<Chamado> salvo = ArgumentCaptor.forClass(Chamado.class);
		Mockito.verify(chamadoRepository).saveAndFlush(salvo.capture());
		assertSame(logadoDoBanco, salvo.getValue().getAbertoUser());
		assertEquals("Ana Atendente", dto.getAbertoUserNome());
		assertEquals("Carlos Cliente", dto.getClienteNome());
		assertEquals(LocalDate.now(), dto.getDataAbertura());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testSalvarIgnoraFechamentoDoRequest() {
		usuariosDaEmpresa();
		when(usuarioLogadoService.getUsuarioLogado()).thenReturn(referencia(1L));
		when(chamadoRepository.existePorTitulo("Impressora", EMPRESA_ID)).thenReturn(false);
		when(chamadoRepository.saveAndFlush(any(Chamado.class))).thenAnswer(i -> i.getArgument(0));
		Chamado chamado = chamado(null, "Impressora");
		chamado.setFechadoUser(referencia(3L));
		chamado.setDataFechamento(LocalDate.of(2026, 1, 1));

		ChamadoDTO dto = chamadoService.salvar(chamado);

		/* Chamado novo nasce aberto: fechamento do request é descartado */
		assertNull(dto.getFechadoUserId());
		assertNull(dto.getFechadoUserNome());
		assertNull(dto.getDataFechamento());
	}

	@Test
	public void testSalvarTituloDuplicado() {
		when(chamadoRepository.existePorTitulo("Impressora", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.salvar(chamado(null, "Impressora")));

		assertEquals("Já existe um chamado com o mesmo título para a empresa logada.", erro.getMessage());
		verify(chamadoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSemCliente() {
		usuariosDaEmpresa();
		when(usuarioLogadoService.getUsuarioLogado()).thenReturn(referencia(1L));
		when(chamadoRepository.existePorTitulo("Impressora", EMPRESA_ID)).thenReturn(false);
		Chamado chamado = chamado(null, "Impressora");
		chamado.setCliente(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.salvar(chamado));

		assertEquals("Cliente e atendente devem ser informados para o chamado.", erro.getMessage());
		verify(chamadoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarAtendenteSemId() {
		usuariosDaEmpresa();
		when(usuarioLogadoService.getUsuarioLogado()).thenReturn(referencia(1L));
		when(chamadoRepository.existePorTitulo("Impressora", EMPRESA_ID)).thenReturn(false);
		Chamado chamado = chamado(null, "Impressora");
		chamado.setAtendente(new Usuario());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.salvar(chamado));

		assertEquals("Cliente e atendente devem ser informados para o chamado.", erro.getMessage());
	}

	@Test
	public void testSalvarClienteDeOutraEmpresa() {
		usuariosDaEmpresa();
		when(usuarioLogadoService.getUsuarioLogado()).thenReturn(referencia(1L));
		when(chamadoRepository.existePorTitulo("Impressora", EMPRESA_ID)).thenReturn(false);
		when(usuarioRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());
		Chamado chamado = chamado(null, "Impressora");
		chamado.setCliente(referencia(99L));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.salvar(chamado));

		assertEquals("Usuário relacionado não encontrado para a empresa logada.", erro.getMessage());
		verify(chamadoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizar() {
		usuariosDaEmpresa();
		Chamado existente = salvo(5L, "Antigo");
		when(chamadoRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		when(chamadoRepository.existePorTituloDiferenteId(5L, "Novo", EMPRESA_ID)).thenReturn(false);
		when(chamadoRepository.saveAndFlush(existente)).thenReturn(existente);

		Chamado alteracao = chamado(5L, "Novo");
		alteracao.setDescricao("Descrição nova");
		alteracao.setStatusChamado(StatusChamado.FECHADO);
		alteracao.setPrioridadeChamado(PrioridadeChamado.BAIXA);
		alteracao.setTipoChamado(TipoChamado.BUG);
		alteracao.setFechadoUser(referencia(3L));
		alteracao.setDataFechamento(LocalDate.of(2026, 11, 12));
		/* Data de abertura não é editável */
		alteracao.setDataAbertura(LocalDate.of(2000, 1, 1));

		ChamadoDTO dto = chamadoService.atualizar(alteracao);

		assertEquals("Novo", dto.getTitulo());
		assertEquals("Descrição nova", dto.getDescricao());
		assertEquals(StatusChamado.FECHADO, dto.getStatusChamado());
		assertEquals(PrioridadeChamado.BAIXA, dto.getPrioridadeChamado());
		assertEquals(TipoChamado.BUG, dto.getTipoChamado());
		assertEquals(3L, dto.getFechadoUserId());
		assertEquals("Fábio Fechou", dto.getFechadoUserNome());
		assertEquals(LocalDate.of(2026, 11, 12), dto.getDataFechamento());
		assertEquals(LocalDate.of(2026, 11, 1), dto.getDataAbertura());
		verify(chamadoRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarSemFechamento() {
		usuariosDaEmpresa();
		Chamado existente = salvo(5L, "Antigo");
		existente.setFechadoUser(usuario(3L, "Fábio Fechou"));
		when(chamadoRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		when(chamadoRepository.saveAndFlush(existente)).thenReturn(existente);

		ChamadoDTO dto = chamadoService.atualizar(chamado(5L, "Antigo"));

		/* Reaberto: o request sem fechadoUser limpa o fechamento */
		assertNull(dto.getFechadoUserId());
		assertNull(existente.getFechadoUser());
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.atualizar(chamado(null, "X")));

		assertEquals("Id do chamado não informado para atualizar.", erro.getMessage());
		verify(chamadoRepository, never()).buscarPorId(anyLong(), anyLong());
	}

	@Test
	public void testAtualizarChamadoDeOutraEmpresa() {
		when(chamadoRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.atualizar(chamado(99L, "X")));

		assertEquals("Chamado não encontrado para a empresa logada.", erro.getMessage());
		verify(chamadoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarTituloDuplicado() {
		when(chamadoRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(salvo(5L, "Antigo")));
		when(chamadoRepository.existePorTituloDiferenteId(5L, "Repetido", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.atualizar(chamado(5L, "Repetido")));

		assertEquals("Já existe outro chamado com o mesmo título para a empresa logada.", erro.getMessage());
		verify(chamadoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarFechadoPorUsuarioDeOutraEmpresa() {
		usuariosDaEmpresa();
		when(chamadoRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(salvo(5L, "Antigo")));
		when(usuarioRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());
		Chamado alteracao = chamado(5L, "Antigo");
		alteracao.setFechadoUser(referencia(99L));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.atualizar(alteracao));

		assertEquals("Usuário relacionado não encontrado para a empresa logada.", erro.getMessage());
		verify(chamadoRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testBuscarPorId() {
		when(chamadoRepository.buscarPorId(5L, EMPRESA_ID)).thenReturn(Optional.of(salvo(5L, "Impressora")));
		when(chamadoRepository.buscarPorId(6L, EMPRESA_ID)).thenReturn(Optional.empty());

		Optional<ChamadoDTO> dto = chamadoService.buscarPorId(5L, EMPRESA_ID);

		assertTrue(dto.isPresent());
		assertEquals("Impressora", dto.get().getTitulo());
		assertEquals("Ana Atendente", dto.get().getAtendenteNome());
		assertTrue(chamadoService.buscarPorId(6L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testFindAll() {
		when(chamadoRepository.findAll(EMPRESA_ID)).thenReturn(List.of(salvo(5L, "A"), salvo(6L, "B")));

		assertEquals(List.of("A", "B"), chamadoService.findAll(EMPRESA_ID).stream().map(ChamadoDTO::getTitulo).toList());
	}

	@Test
	public void testBuscaPorTitulo() {
		when(chamadoRepository.buscaPorTitulo("impr", EMPRESA_ID)).thenReturn(List.of(salvo(5L, "Impressora")));

		List<ChamadoDTO> lista = chamadoService.buscaPorTitulo("impr", EMPRESA_ID);

		assertEquals(1, lista.size());
		assertEquals("Carlos Cliente", lista.get(0).getClienteNome());
	}

	@Test
	public void testExistePorTitulo() {
		when(chamadoRepository.existePorTitulo("Impressora", EMPRESA_ID)).thenReturn(true);

		assertTrue(chamadoService.existePorTitulo("Impressora", EMPRESA_ID));
		assertFalse(chamadoService.existePorTitulo("Rede", EMPRESA_ID));
	}

	@Test
	public void testExistePorTituloDiferenteId() {
		when(chamadoRepository.existePorTituloDiferenteId(5L, "Impressora", EMPRESA_ID)).thenReturn(true);

		assertTrue(chamadoService.existePorTituloDiferenteId(5L, "Impressora", EMPRESA_ID));
		assertFalse(chamadoService.existePorTituloDiferenteId(6L, "Impressora", EMPRESA_ID));
	}

	@Test
	public void testDeleteById() {
		when(chamadoRepository.existsById(5L, EMPRESA_ID)).thenReturn(true);

		chamadoService.deleteById(5L, EMPRESA_ID);

		verify(chamadoRepository).deleteById(5L, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdNaoEncontrado() {
		when(chamadoRepository.existsById(5L, EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> chamadoService.deleteById(5L, EMPRESA_ID));

		assertEquals("Chamado não encontrado ou já foi deletado.", erro.getMessage());
		verify(chamadoRepository, never()).deleteById(anyLong(), anyLong());
	}

}
