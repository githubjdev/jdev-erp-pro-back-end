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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.ClienteFuncionarioDTO;
import br.com.jdeverp.pro.enums.TipoClienteFuncionario;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ClienteFuncionarioRepository;
import br.com.jdeverp.pro.repository.PessoaRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class TestClienteFuncionarioService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private ClienteFuncionarioRepository clienteFuncionarioRepository;

	@Mock
	private PessoaRepository pessoaRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private ClienteFuncionarioService clienteFuncionarioService;

	private Empresa empresa;
	private Pessoa pessoaDoBanco;

	@BeforeEach
	public void configurar() {
		empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		pessoaDoBanco = new Pessoa();
		pessoaDoBanco.setId(3L);
		pessoaDoBanco.setNome("Maria da Silva");
		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
	}

	/** Como chega do request: só os ids das relações. */
	private ClienteFuncionario doRequest(Long pessoaId, Long usuarioId) {
		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setTipoClienteFuncionario(TipoClienteFuncionario.CLIENTE);
		if (pessoaId != null) {
			Pessoa pessoa = new Pessoa();
			pessoa.setId(pessoaId);
			clienteFuncionario.setPessoa(pessoa);
		}
		if (usuarioId != null) {
			clienteFuncionario.setUsuario(usuario(usuarioId));
		}
		return clienteFuncionario;
	}

	private static Usuario usuario(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		return usuario;
	}

	/** Como vem do banco: pessoa carregada e empresa. */
	private ClienteFuncionario salvo(Long id) {
		ClienteFuncionario clienteFuncionario = doRequest(null, null);
		clienteFuncionario.setId(id);
		clienteFuncionario.setPessoa(pessoaDoBanco);
		clienteFuncionario.setEmpresa(empresa);
		return clienteFuncionario;
	}

	private void salvarDevolveOProprio() {
		when(clienteFuncionarioRepository.saveAndFlush(any(ClienteFuncionario.class))).thenAnswer(i -> i.getArgument(0));
	}

	@Test
	public void testSalvar() {
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));
		when(clienteFuncionarioRepository.existeParaPessoa(3L, EMPRESA_ID)).thenReturn(false);
		when(clienteFuncionarioRepository.saveAndFlush(any(ClienteFuncionario.class))).thenAnswer(i -> {
			ClienteFuncionario salvo = i.getArgument(0);
			salvo.setId(1L);
			return salvo;
		});

		ClienteFuncionarioDTO dto = clienteFuncionarioService.salvar(doRequest(3L, null));

		assertEquals(1L, dto.getId());
		assertEquals(TipoClienteFuncionario.CLIENTE, dto.getTipoClienteFuncionario());
		assertEquals(3L, dto.getPessoaId());
		/* O nome vem da pessoa buscada no banco, não do objeto do request */
		assertEquals("Maria da Silva", dto.getNomePessoa());
		assertNull(dto.getUsuarioId());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testSalvarComUsuario() {
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));
		when(clienteFuncionarioRepository.existeParaPessoa(3L, EMPRESA_ID)).thenReturn(false);
		when(usuarioRepository.buscarPorId(7L, EMPRESA_ID)).thenReturn(Optional.of(usuario(7L)));
		salvarDevolveOProprio();

		assertEquals(7L, clienteFuncionarioService.salvar(doRequest(3L, 7L)).getUsuarioId());
	}

	@Test
	public void testSalvarUsuarioSemId() {
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));
		when(clienteFuncionarioRepository.existeParaPessoa(3L, EMPRESA_ID)).thenReturn(false);
		salvarDevolveOProprio();
		ClienteFuncionario requisicao = doRequest(3L, null);
		requisicao.setUsuario(new Usuario());

		/* Usuário é opcional no cadastro: sem id, é tratado como não informado */
		assertNull(clienteFuncionarioService.salvar(requisicao).getUsuarioId());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testSalvarSemPessoa() {
		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.salvar(doRequest(null, null)));

		assertEquals("Pessoa deve ser informada para o cadastro.", erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
	}

	@Test
	public void testSalvarPessoaSemId() {
		ClienteFuncionario requisicao = doRequest(null, null);
		requisicao.setPessoa(new Pessoa());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.salvar(requisicao));

		assertEquals("Pessoa deve ser informada para o cadastro.", erro.getMessage());
	}

	@Test
	public void testSalvarPessoaDeOutraEmpresa() {
		when(pessoaRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.salvar(doRequest(99L, null)));

		assertEquals("Pessoa não encontrada para a empresa logada.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarPessoaJaCadastrada() {
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));
		when(clienteFuncionarioRepository.existeParaPessoa(3L, EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.salvar(doRequest(3L, null)));

		assertEquals("Já existe um cliente/funcionário cadastrado para a pessoa informada.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarUsuarioDeOutraEmpresa() {
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));
		when(clienteFuncionarioRepository.existeParaPessoa(3L, EMPRESA_ID)).thenReturn(false);
		when(usuarioRepository.buscarPorId(7L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.salvar(doRequest(3L, 7L)));

		assertEquals("Usuário não encontrado para a empresa logada.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarClienteFunUser() {
		ClienteFuncionario clienteFuncionario = salvo(1L);
		when(clienteFuncionarioRepository.saveAndFlush(clienteFuncionario)).thenReturn(clienteFuncionario);

		/* Usado pelo UsuarioService: grava sem validar e devolve a entidade */
		assertSame(clienteFuncionario, clienteFuncionarioService.salvarClienteFunUser(clienteFuncionario));
	}

	@Test
	public void testAtualizar() {
		ClienteFuncionario requisicao = doRequest(3L, 7L);
		requisicao.setId(1L);
		requisicao.setTipoClienteFuncionario(TipoClienteFuncionario.FUNCIONARIO);
		when(clienteFuncionarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L)));
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));
		when(usuarioRepository.buscarPorId(7L, EMPRESA_ID)).thenReturn(Optional.of(usuario(7L)));
		salvarDevolveOProprio();

		ClienteFuncionarioDTO dto = clienteFuncionarioService.atualizar(requisicao);

		assertEquals(TipoClienteFuncionario.FUNCIONARIO, dto.getTipoClienteFuncionario());
		assertEquals("Maria da Silva", dto.getNomePessoa());
		assertEquals(7L, dto.getUsuarioId());
		assertEquals(EMPRESA_ID, dto.getEmpresaId());
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.atualizar(doRequest(3L, 7L)));

		assertEquals("Id do cliente/funcionário não informado para atualizar.", erro.getMessage());
		verifyNoInteractions(clienteFuncionarioRepository);
	}

	@Test
	public void testAtualizarDeOutraEmpresa() {
		ClienteFuncionario requisicao = doRequest(3L, 7L);
		requisicao.setId(99L);
		when(clienteFuncionarioRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.atualizar(requisicao));

		assertEquals("Cliente/funcionário não encontrado para a empresa logada.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarSemPessoa() {
		ClienteFuncionario requisicao = doRequest(null, 7L);
		requisicao.setId(1L);
		when(clienteFuncionarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.atualizar(requisicao));

		assertEquals("Pessoa deve ser informada para atualizar o cadastro.", erro.getMessage());
	}

	@Test
	public void testAtualizarPessoaDeOutraEmpresa() {
		ClienteFuncionario requisicao = doRequest(99L, 7L);
		requisicao.setId(1L);
		when(clienteFuncionarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L)));
		when(pessoaRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.atualizar(requisicao));

		assertEquals("Pessoa não encontrada para a empresa logada.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).saveAndFlush(any());
	}

	/** Comportamento atual: o usuário é obrigatório na edição. A P14 decidiu torná-lo opcional (ainda não implementado). */
	@Test
	public void testAtualizarSemUsuario() {
		ClienteFuncionario requisicao = doRequest(3L, null);
		requisicao.setId(1L);
		when(clienteFuncionarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L)));
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.atualizar(requisicao));

		assertEquals("Usuário deve ser informado para atualizar o cadastro.", erro.getMessage());
	}

	@Test
	public void testAtualizarUsuarioDeOutraEmpresa() {
		ClienteFuncionario requisicao = doRequest(3L, 99L);
		requisicao.setId(1L);
		when(clienteFuncionarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L)));
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));
		when(usuarioRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.atualizar(requisicao));

		assertEquals("Usuário não encontrado para a empresa logada.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testFindAll() {
		ClienteFuncionario clienteFuncionario = salvo(1L);
		when(clienteFuncionarioRepository.findAll(EMPRESA_ID)).thenReturn(List.of(clienteFuncionario));

		assertSame(clienteFuncionario, clienteFuncionarioService.findAll(EMPRESA_ID).get(0));
	}

	@Test
	public void testBuscaPorNome() {
		when(clienteFuncionarioRepository.buscaPorNome("maria", EMPRESA_ID)).thenReturn(List.of(salvo(1L)));

		assertEquals("Maria da Silva", clienteFuncionarioService.buscaPorNome("maria", EMPRESA_ID).get(0).getNomePessoa());
	}

	@Test
	public void testExistePorNome() {
		when(clienteFuncionarioRepository.existePorNome("Maria", EMPRESA_ID)).thenReturn(true);

		assertTrue(clienteFuncionarioService.existePorNome("Maria", EMPRESA_ID));
		assertFalse(clienteFuncionarioService.existePorNome("João", EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		when(clienteFuncionarioRepository.existePorNomeDiferenteId(1L, "Maria", EMPRESA_ID)).thenReturn(true);

		assertTrue(clienteFuncionarioService.existePorNomeDiferenteId(1L, "Maria", EMPRESA_ID));
		assertFalse(clienteFuncionarioService.existePorNomeDiferenteId(2L, "Maria", EMPRESA_ID));
	}

	@Test
	public void testDeleteById() {
		when(clienteFuncionarioRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		clienteFuncionarioService.deleteById(1L, EMPRESA_ID);

		verify(clienteFuncionarioRepository).deleteById(1L, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdNaoEncontrado() {
		when(clienteFuncionarioRepository.existsById(1L, EMPRESA_ID)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.deleteById(1L, EMPRESA_ID));

		assertEquals("Cliente/funcionário não encontrado ou já foi deletado.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).deleteById(anyLong(), anyLong());
	}

	@Test
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(clienteFuncionarioRepository.total(EMPRESA_ID)).thenReturn(3L);
		when(clienteFuncionarioRepository.deleteAll(EMPRESA_ID)).thenReturn(3L);

		assertEquals(3L, clienteFuncionarioService.deleteAll(EMPRESA_ID));
	}

	@Test
	public void testDeleteAllSemRegistros() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(clienteFuncionarioRepository.total(EMPRESA_ID)).thenReturn(0L);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.deleteAll(EMPRESA_ID));

		assertEquals("Nenhum cliente/funcionário encontrado para deletar ou todos já foram deletados.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).deleteAll(anyLong());
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(clienteFuncionarioRepository);
	}

	@Test
	public void testDeletarAllById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(clienteFuncionarioRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salvo(1L), salvo(2L)));

		clienteFuncionarioService.deletarAllById(List.of(1L, 2L), EMPRESA_ID);

		verify(clienteFuncionarioRepository).deletarAllById(List.of(1L, 2L), EMPRESA_ID);
	}

	@Test
	public void testDeletarAllByIdComIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(clienteFuncionarioRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salvo(1L)));

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.deletarAllById(List.of(1L, 2L), EMPRESA_ID));

		/* Tudo ou nada: o id 1, que existe, também fica */
		assertEquals("Clientes/funcionários não encontrados ou já deletados: [2]", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdListaVazia() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(clienteFuncionarioRepository.buscarPorIds(List.of(), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.deletarAllById(List.of(), EMPRESA_ID));

		assertEquals("Nenhum registro informado para deletar.", erro.getMessage());
		verify(clienteFuncionarioRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.deletarAllById(List.of(1L), EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(clienteFuncionarioRepository);
	}

	@Test
	public void testBuscarPorIds() {
		when(clienteFuncionarioRepository.buscarPorIds(List.of(1L, 2L), EMPRESA_ID)).thenReturn(List.of(salvo(1L), salvo(2L)));

		assertEquals(List.of(1L, 2L), clienteFuncionarioService.buscarPorIds(List.of(1L, 2L), EMPRESA_ID).stream()
				.map(ClienteFuncionarioDTO::getId).toList());
	}

	@Test
	public void testExistsById() {
		when(clienteFuncionarioRepository.existsById(1L, EMPRESA_ID)).thenReturn(true);

		assertTrue(clienteFuncionarioService.existsById(1L, EMPRESA_ID));
		assertFalse(clienteFuncionarioService.existsById(2L, EMPRESA_ID));
	}

	@Test
	public void testListar() {
		when(clienteFuncionarioRepository.listar(EMPRESA_ID)).thenReturn(List.of(salvo(1L)));

		assertEquals(1, clienteFuncionarioService.listar(EMPRESA_ID).size());
	}

	@Test
	public void testBuscarPorId() {
		when(clienteFuncionarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L)));
		when(clienteFuncionarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		assertEquals(3L, clienteFuncionarioService.buscarPorId(1L, EMPRESA_ID).orElseThrow().getPessoaId());
		assertTrue(clienteFuncionarioService.buscarPorId(2L, EMPRESA_ID).isEmpty());
	}

	@Test
	public void testTotal() {
		when(clienteFuncionarioRepository.total(EMPRESA_ID)).thenReturn(4L);

		assertEquals(4L, clienteFuncionarioService.total(EMPRESA_ID));
	}

	@Test
	public void testListarPaginado() {
		Pageable pagina = PageRequest.of(0, 1);
		when(clienteFuncionarioRepository.listarPaginado(EMPRESA_ID, pagina))
				.thenReturn(new PageImpl<>(List.of(salvo(1L)), pagina, 2));

		Page<ClienteFuncionarioDTO> page = clienteFuncionarioService.listarPaginado(EMPRESA_ID, pagina);

		assertEquals(2, page.getTotalElements());
		assertEquals("Maria da Silva", page.getContent().get(0).getNomePessoa());
	}

	@Test
	public void testFindByPessoa() {
		ClienteFuncionario clienteFuncionario = salvo(1L);
		when(clienteFuncionarioRepository.findByPessoa(3L, EMPRESA_ID)).thenReturn(clienteFuncionario);

		assertSame(clienteFuncionario, clienteFuncionarioService.findByPessoa(3L, EMPRESA_ID));
	}

	@Test
	public void testFindByPessoaDto() {
		when(clienteFuncionarioRepository.findByPessoa(3L, EMPRESA_ID)).thenReturn(salvo(1L));
		when(clienteFuncionarioRepository.findByPessoa(4L, EMPRESA_ID)).thenReturn(null);

		assertEquals(1L, clienteFuncionarioService.findByPessoaDto(3L, EMPRESA_ID).getId());
		assertNull(clienteFuncionarioService.findByPessoaDto(4L, EMPRESA_ID));
	}

	@Test
	public void testRemoveUserClienteFuncionarioId() {
		clienteFuncionarioService.removeUserClienteFuncionarioId(7L, EMPRESA_ID);

		verify(clienteFuncionarioRepository).removeUserClienteFuncionarioId(7L, EMPRESA_ID);
	}

	@Test
	public void testAtualizarPessoaSemId() {
		ClienteFuncionario requisicao = doRequest(null, 7L);
		requisicao.setId(1L);
		requisicao.setPessoa(new Pessoa());
		when(clienteFuncionarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.atualizar(requisicao));

		assertEquals("Pessoa deve ser informada para atualizar o cadastro.", erro.getMessage());
		verifyNoInteractions(pessoaRepository);
	}

	@Test
	public void testAtualizarUsuarioSemId() {
		ClienteFuncionario requisicao = doRequest(3L, null);
		requisicao.setId(1L);
		requisicao.setUsuario(new Usuario());
		when(clienteFuncionarioRepository.buscarPorId(1L, EMPRESA_ID)).thenReturn(Optional.of(salvo(1L)));
		when(pessoaRepository.buscarPorId(3L, EMPRESA_ID)).thenReturn(Optional.of(pessoaDoBanco));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.atualizar(requisicao));

		assertEquals("Usuário deve ser informado para atualizar o cadastro.", erro.getMessage());
		verifyNoInteractions(usuarioRepository);
	}

}
