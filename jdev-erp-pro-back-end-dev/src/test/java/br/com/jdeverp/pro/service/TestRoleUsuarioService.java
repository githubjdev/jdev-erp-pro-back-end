package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.RoleUsuarioDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.RoleRepository;
import br.com.jdeverp.pro.repository.RoleUsuarioRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
public class TestRoleUsuarioService {

	private static final Long EMPRESA_ID = 10L;

	@Mock
	private RoleUsuarioRepository roleUsuarioRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private RoleUsuarioService roleUsuarioService;

	private Role roleAdmin;
	private Usuario usuarioDoBanco;

	@BeforeEach
	public void configurar() {
		roleAdmin = new Role();
		roleAdmin.setId(2L);
		roleAdmin.setAcesso("ROLE_ADMIN");

		Pessoa pessoa = new Pessoa();
		pessoa.setNome("João Souza");
		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setPessoa(pessoa);
		usuarioDoBanco = new Usuario();
		usuarioDoBanco.setId(7L);
		usuarioDoBanco.setClienteFuncionario(clienteFuncionario);

		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.isAdmin()).thenReturn(true);
	}

	@Test
	public void testSalvarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.salvar(doRequest(null, 2L, 7L)));

		assertEquals("Apenas administradores podem associar acessos a usuários.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(roleUsuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.atualizar(doRequest(1L, 2L, 7L)));

		assertEquals("Apenas administradores podem alterar acessos de usuários.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(roleUsuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testDeleteByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.deleteById(1L));

		assertEquals("Apenas administradores podem remover acessos de usuários.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(roleUsuarioRepository, never()).deleteById(anyLong());
	}

	/** Como chega do request: só os ids. */
	private RoleUsuario doRequest(Long id, Long roleId, Long usuarioId) {
		RoleUsuario roleUsuario = new RoleUsuario();
		roleUsuario.setId(id);
		if (roleId != null) {
			Role role = new Role();
			role.setId(roleId);
			roleUsuario.setAcesso(role);
		}
		if (usuarioId != null) {
			Usuario usuario = new Usuario();
			usuario.setId(usuarioId);
			roleUsuario.setUsuario(usuario);
		}
		return roleUsuario;
	}

	private void acessoEUsuarioValidos() {
		when(roleRepository.findById(2L)).thenReturn(Optional.of(roleAdmin));
		when(usuarioRepository.buscarPorId(7L, EMPRESA_ID)).thenReturn(Optional.of(usuarioDoBanco));
	}

	@Test
	public void testSalvar() {
		acessoEUsuarioValidos();
		when(roleUsuarioRepository.existePorUsuarioERole(7L, 2L, EMPRESA_ID)).thenReturn(false);
		when(roleUsuarioRepository.saveAndFlush(any(RoleUsuario.class))).thenAnswer(i -> {
			RoleUsuario salvo = i.getArgument(0);
			salvo.setId(1L);
			return salvo;
		});

		RoleUsuarioDTO dto = roleUsuarioService.salvarDto(doRequest(null, 2L, 7L));

		assertEquals(1L, dto.getId());
		assertEquals(2L, dto.getAcessoId());
		/* Os nomes vêm do acesso e do usuário buscados no banco */
		assertEquals("ROLE_ADMIN", dto.getAcessoNome());
		assertEquals(7L, dto.getUsuarioId());
		assertEquals("João Souza", dto.getUsuarioNome());
	}

	@Test
	public void testSalvarSemAcesso() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.salvar(doRequest(null, null, 7L)));

		assertEquals("Acesso deve ser informado para associar ao usuário.", erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
	}

	@Test
	public void testSalvarAcessoInexistente() {
		when(roleRepository.findById(0L)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.salvar(doRequest(null, 0L, 7L)));

		assertEquals("Acesso não encontrado.", erro.getMessage());
	}

	@Test
	public void testSalvarSemUsuario() {
		when(roleRepository.findById(2L)).thenReturn(Optional.of(roleAdmin));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.salvar(doRequest(null, 2L, null)));

		assertEquals("Usuário deve ser informado para associar acesso.", erro.getMessage());
	}

	@Test
	public void testSalvarUsuarioDeOutraEmpresa() {
		when(roleRepository.findById(2L)).thenReturn(Optional.of(roleAdmin));
		when(usuarioRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.salvar(doRequest(null, 2L, 99L)));

		assertEquals("Usuário não encontrado para a empresa logada.", erro.getMessage());
		verify(roleUsuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarDuplicado() {
		acessoEUsuarioValidos();
		when(roleUsuarioRepository.existePorUsuarioERole(7L, 2L, EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.salvar(doRequest(null, 2L, 7L)));

		assertEquals("Já existe o acesso de: ROLE_ADMIN associado para o mesmo usuário: João Souza", erro.getMessage());
		verify(roleUsuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizar() {
		RoleUsuario existente = new RoleUsuario();
		existente.setId(1L);
		when(roleUsuarioRepository.buscarPorIdEEmpresa(1L, EMPRESA_ID)).thenReturn(Optional.of(existente));
		acessoEUsuarioValidos();
		when(roleUsuarioRepository.existePorUsuarioDiferenreId(7L, 2L, 1L, EMPRESA_ID)).thenReturn(false);
		when(roleUsuarioRepository.saveAndFlush(existente)).thenReturn(existente);

		RoleUsuarioDTO dto = roleUsuarioService.atualizar(doRequest(1L, 2L, 7L));

		assertEquals(1L, dto.getId());
		assertEquals("ROLE_ADMIN", dto.getAcessoNome());
		assertEquals("João Souza", dto.getUsuarioNome());
		verify(roleUsuarioRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarSemId() {
		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.atualizar(doRequest(null, 2L, 7L)));

		assertEquals("Deve ser informado o registro para editart o acesso do usuário.", erro.getMessage());
	}

	@Test
	public void testAtualizarRegistroDeOutraEmpresa() {
		when(roleUsuarioRepository.buscarPorIdEEmpresa(50L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.atualizar(doRequest(50L, 2L, 7L)));

		assertEquals("Registro de acesso não encontrado para a empresa logada.", erro.getMessage());
		verify(roleUsuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarDuplicado() {
		when(roleUsuarioRepository.buscarPorIdEEmpresa(1L, EMPRESA_ID)).thenReturn(Optional.of(new RoleUsuario()));
		acessoEUsuarioValidos();
		when(roleUsuarioRepository.existePorUsuarioDiferenreId(7L, 2L, 1L, EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleUsuarioService.atualizar(doRequest(1L, 2L, 7L)));

		assertEquals("Associação de usuário com acesso é duplicada e não pode ser permitida.", erro.getMessage());
		verify(roleUsuarioRepository, never()).saveAndFlush(any());
	}

}
