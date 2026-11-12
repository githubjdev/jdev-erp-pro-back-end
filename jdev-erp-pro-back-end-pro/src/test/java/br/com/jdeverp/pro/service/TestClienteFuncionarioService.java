package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
			Usuario usuario = new Usuario();
			usuario.setId(usuarioId);
			clienteFuncionario.setUsuario(usuario);
		}
		return clienteFuncionario;
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
	public void testSalvarSemPessoa() {
		MsgApiException erro = assertThrows(MsgApiException.class,
				() -> clienteFuncionarioService.salvar(doRequest(null, null)));

		assertEquals("Pessoa deve ser informada para o cadastro.", erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
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
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> clienteFuncionarioService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem excluir registros em lote.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(clienteFuncionarioRepository);
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

}
