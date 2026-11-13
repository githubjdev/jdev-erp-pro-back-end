package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import br.com.jdeverp.pro.dto.AlterarSenhaDTO;
import br.com.jdeverp.pro.dto.LoginDTO;
import br.com.jdeverp.pro.dto.TokenDTO;
import br.com.jdeverp.pro.dto.UsuarioDto;
import br.com.jdeverp.pro.enums.TipoClienteFuncionario;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.UsuarioRepository;

/** Cadastro, edição e exclusão de usuários são só de ROLE_ADMIN; senha, só a própria. */
@ExtendWith(MockitoExtension.class)
public class TestUsuarioService {

	private static final Long EMPRESA_ID = 10L;
	private static final Long PESSOA_ID = 3L;
	private static final String HASH_ATUAL = "hash-da-senha-atual";

	@Mock
	private UsuarioRepository usuarioRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private JwtService jwtService;

	@Mock
	private ClienteFuncionarioService clienteFuncionarioService;

	@Mock
	private RoleService roleService;

	@Mock
	private RoleUsuarioService roleUsuarioService;

	@InjectMocks
	private UsuarioService usuarioService;

	private Empresa empresa;
	private ClienteFuncionario clienteFuncionario;

	@BeforeEach
	public void configurar() {
		Pessoa pessoaEmpresa = new Pessoa();
		pessoaEmpresa.setNome("JDev Treinamento");
		empresa = new Empresa();
		empresa.setId(EMPRESA_ID);
		empresa.setPessoa(pessoaEmpresa);
		empresa.setBloqueio(false);

		Pessoa pessoa = new Pessoa();
		pessoa.setId(PESSOA_ID);
		pessoa.setNome("Maria da Silva");
		clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setId(6L);
		clienteFuncionario.setPessoa(pessoa);
		clienteFuncionario.setEmpresa(empresa);
		clienteFuncionario.setTipoClienteFuncionario(TipoClienteFuncionario.FUNCIONARIO);

		lenient().when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(EMPRESA_ID);
		lenient().when(usuarioLogadoService.getEmpresaLogada()).thenReturn(empresa);
	}

	private Usuario usuario(Long id, String login) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		usuario.setLogin(login);
		usuario.setSenha(HASH_ATUAL);
		usuario.setLiberado(true);
		usuario.setEmpresa(empresa);
		usuario.setClienteFuncionario(clienteFuncionario);
		return usuario;
	}

	private static AlterarSenhaDTO alterarSenha(Long id) {
		AlterarSenhaDTO dto = new AlterarSenhaDTO();
		dto.setId(id);
		dto.setSenhaAtual("Senha@Atual1");
		dto.setNovaSenha("Senha@Nova1");
		dto.setConfirmarSenha("Senha@Nova1");
		return dto;
	}

	private static UsuarioDto usuarioDto(Long id, String login) {
		UsuarioDto dto = new UsuarioDto();
		dto.setId(id);
		dto.setLogin(login);
		dto.setSenha("Senha@123");
		dto.setLiberado(true);
		dto.setPessoaId(PESSOA_ID);
		return dto;
	}

	private static LoginDTO login(String login, String senha) {
		LoginDTO dto = new LoginDTO();
		dto.setLogin(login);
		dto.setSenha(senha);
		return dto;
	}

	// ==================== login ====================

	@Test
	public void testLogin() {
		Usuario usuario = usuario(9L, "maria");
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario);
		when(passwordEncoder.matches("Senha@123", HASH_ATUAL)).thenReturn(true);
		when(jwtService.gerarToken(usuario)).thenReturn("token-novo");

		TokenDTO token = usuarioService.login(login("maria", "Senha@123"));

		assertEquals("token-novo", token.getToken());
		verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken("maria", "Senha@123"));
		/* O token fica gravado no usuário: um login novo derruba o anterior */
		verify(usuarioRepository).updateTokenSessaoLogin(9L, "token-novo", EMPRESA_ID);
	}

	@Test
	public void testLoginUsuarioNaoEncontrado() {
		when(usuarioRepository.buscaPorLogin("ninguem")).thenReturn(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.login(login("ninguem", "x")));

		assertEquals("Usuário não encontrado.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
		verifyNoInteractions(passwordEncoder, authenticationManager, jwtService);
	}

	@Test
	public void testLoginUsuarioBloqueado() {
		Usuario usuario = usuario(9L, "maria");
		usuario.setLiberado(false);
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.login(login("maria", "Senha@123")));

		assertEquals("Usuário bloqueado, entre em contato com o administrador do sistema.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
		verifyNoInteractions(jwtService);
	}

	@Test
	public void testLoginEmpresaBloqueada() {
		empresa.setBloqueio(true);
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario(9L, "maria"));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.login(login("maria", "Senha@123")));

		assertEquals("Empresa bloqueada, entre em contato com o administrador do sistema.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
		verifyNoInteractions(jwtService);
	}

	@Test
	public void testLoginSenhaInvalida() {
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario(9L, "maria"));
		when(passwordEncoder.matches("errada", HASH_ATUAL)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.login(login("maria", "errada")));

		assertEquals("Senha digitada é inválida.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
		verifyNoInteractions(authenticationManager, jwtService);
		verify(usuarioRepository, never()).updateTokenSessaoLogin(anyLong(), anyString(), anyLong());
	}

	// ==================== salvar ====================

	@Test
	public void testSalvar() {
		Role roleUser = new Role();
		roleUser.setId(1L);
		roleUser.setAcesso("ROLE_USER");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.existePorLogin("maria", EMPRESA_ID)).thenReturn(false);
		when(usuarioRepository.existePorPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(false);
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(clienteFuncionario);
		when(roleService.buscaPorAcesso("ROLE_USER")).thenReturn(List.of(roleUser));
		when(passwordEncoder.encode("Senha@123")).thenReturn("hash-novo");
		when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> {
			Usuario salvo = i.getArgument(0);
			salvo.setId(9L);
			return salvo;
		});

		UsuarioDto dto = usuarioService.salvar(usuarioDto(null, "maria"));

		ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).saveAndFlush(usuario.capture());
		assertEquals("hash-novo", usuario.getValue().getSenha(), "grava só o hash da senha");
		assertSame(empresa, usuario.getValue().getEmpresa());
		assertSame(clienteFuncionario, usuario.getValue().getClienteFuncionario());

		/* Todo usuário novo recebe ROLE_USER */
		ArgumentCaptor<RoleUsuario> roleUsuario = ArgumentCaptor.forClass(RoleUsuario.class);
		verify(roleUsuarioService).salvar(roleUsuario.capture());
		assertSame(roleUser, roleUsuario.getValue().getAcesso());
		assertEquals(9L, roleUsuario.getValue().getUsuario().getId());

		/* O cliente/funcionário passa a apontar para o usuário */
		assertEquals(9L, clienteFuncionario.getUsuario().getId());
		verify(clienteFuncionarioService).salvarClienteFunUser(clienteFuncionario);

		assertEquals(9L, dto.getId());
		assertEquals("***Ocultada***", dto.getSenha());
		assertEquals(6L, dto.getClienteFuncionarioId());
		assertEquals("JDev Treinamento", dto.getEmpresa());
		assertEquals("Maria da Silva", dto.getPessoa());
		assertEquals(TipoClienteFuncionario.FUNCIONARIO, dto.getTipoClienteFuncionario());
		assertTrue(usuario.getValue().getLiberado());
	}

	@Test
	public void testSalvarComLiberadoNulo() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.existePorLogin("maria", EMPRESA_ID)).thenReturn(false);
		when(usuarioRepository.existePorPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(false);
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(clienteFuncionario);
		when(roleService.buscaPorAcesso("ROLE_USER")).thenReturn(List.of());
		when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
		UsuarioDto novo = usuarioDto(null, "maria");
		novo.setLiberado(null);

		UsuarioDto dto = usuarioService.salvar(novo);

		/* Nulo vira false: liberar o usuário é decisão explícita do admin */
		ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
		verify(usuarioRepository).saveAndFlush(usuario.capture());
		assertFalse(usuario.getValue().getLiberado());
		assertFalse(dto.getLiberado());
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
	public void testSalvarLoginDuplicado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.existePorLogin("maria", EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.salvar(usuarioDto(null, "maria")));

		assertEquals("O login escolhido já existe, escolha outro login para cadastrar um novo usuário.", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSenhaCurta() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		UsuarioDto curta = usuarioDto(null, "maria");
		curta.setSenha("1234");
		UsuarioDto semSenha = usuarioDto(null, "maria");
		semSenha.setSenha(null);

		for (UsuarioDto dto : List.of(curta, semSenha)) {
			MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.salvar(dto));
			assertEquals("A senha deve ter no mínimo 5 caracteres.", erro.getMessage());
		}
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSenhaComCincoCaracteres() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(clienteFuncionario);
		when(passwordEncoder.encode("12345")).thenReturn("hash-novo");
		when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));
		UsuarioDto dto = usuarioDto(null, "maria");
		dto.setSenha("12345");

		/* O mínimo é 5: exatamente 5 caracteres é aceito */
		assertEquals("***Ocultada***", usuarioService.salvar(dto).getSenha());
	}

	@Test
	public void testSalvarPessoaJaTemUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.existePorPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.salvar(usuarioDto(null, "maria")));

		assertEquals("Já existe um usuário vinculado a esta pessoa.", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSemClienteFuncionario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.salvar(usuarioDto(null, "maria")));

		assertEquals("Não foi informado o registro de pessoa/ cliente ou funcioário para o usuário.", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
		verifyNoInteractions(roleUsuarioService);
	}

	// ==================== atualizar ====================

	@Test
	public void testAtualizar() {
		Usuario banco = usuario(9L, "login.antigo");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(banco));
		when(usuarioRepository.existeOutroUsuarioComPessoa(PESSOA_ID, 9L, EMPRESA_ID)).thenReturn(false);
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(clienteFuncionario);
		when(usuarioRepository.saveAndFlush(banco)).thenReturn(banco);
		UsuarioDto alteracao = usuarioDto(9L, "login.novo");
		alteracao.setLiberado(false);

		UsuarioDto dto = usuarioService.atualizar(alteracao);

		assertEquals("login.novo", banco.getLogin());
		assertFalse(banco.getLiberado());
		/* A edição não troca a senha: isso é só no alterarSenha */
		assertEquals(HASH_ATUAL, banco.getSenha());
		verifyNoInteractions(passwordEncoder);
		assertEquals("***Ocultada***", dto.getSenha());
		assertEquals("Maria da Silva", dto.getPessoa());
		assertEquals(6L, dto.getClienteFuncionarioId());
	}

	@Test
	public void testAtualizarBloqueandoOProprioUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.isUsuarioLogado(9L)).thenReturn(true);
		UsuarioDto alteracao = usuarioDto(9L, "maria");
		alteracao.setLiberado(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.atualizar(alteracao));

		/* P25: o admin logado não tira o próprio acesso */
		assertEquals(UsuarioService.MSG_BLOQUEAR_A_SI_MESMO, erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testAtualizarComLiberadoNuloNoProprioUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.isUsuarioLogado(9L)).thenReturn(true);
		UsuarioDto alteracao = usuarioDto(9L, "maria");
		alteracao.setLiberado(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.atualizar(alteracao));

		/* liberado nulo também tira o acesso */
		assertEquals(UsuarioService.MSG_BLOQUEAR_A_SI_MESMO, erro.getMessage());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testAtualizarComLiberadoNulo() {
		Usuario banco = usuario(9L, "maria");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(banco));
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(clienteFuncionario);
		when(usuarioRepository.saveAndFlush(banco)).thenReturn(banco);
		UsuarioDto alteracao = usuarioDto(9L, "maria");
		alteracao.setLiberado(null);

		UsuarioDto dto = usuarioService.atualizar(alteracao);

		/* Nulo vira false: liberar o usuário é decisão explícita do admin */
		assertFalse(banco.getLiberado());
		assertFalse(dto.getLiberado());
	}

	@Test
	public void testAtualizarBloqueandoOutroAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(administrador(9L)));
		UsuarioDto alteracao = usuarioDto(9L, "maria");
		alteracao.setLiberado(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.atualizar(alteracao));

		/* P25: um admin não bloqueia outro */
		assertEquals(UsuarioService.MSG_ADMIN_NAO_BLOQUEIA, erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarAdministradorJaBloqueado() {
		Usuario banco = administrador(9L);
		banco.setLiberado(false);
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(banco));
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(clienteFuncionario);
		when(usuarioRepository.saveAndFlush(banco)).thenReturn(banco);
		UsuarioDto alteracao = usuarioDto(9L, "login.novo");
		alteracao.setLiberado(false);

		/* Quem já estava bloqueado pode ser editado sem ser desbloqueado */
		usuarioService.atualizar(alteracao);

		assertEquals("login.novo", banco.getLogin());
		assertFalse(banco.getLiberado());
	}

	@Test
	public void testAtualizarOutroAdministradorSemBloquear() {
		Usuario banco = administrador(9L);
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(banco));
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(clienteFuncionario);
		when(usuarioRepository.saveAndFlush(banco)).thenReturn(banco);

		/* Editar os dados de outro admin, sem bloqueá-lo, é permitido */
		assertEquals("login.novo", usuarioService.atualizar(usuarioDto(9L, "login.novo")).getLogin());
		assertTrue(banco.getLiberado());
	}

	@Test
	public void testAtualizarOProprioUsuarioSemBloquear() {
		Usuario banco = usuario(9L, "login.antigo");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		/* lenient: sem bloqueio, o service nem pergunta se é o usuário logado */
		lenient().when(usuarioLogadoService.isUsuarioLogado(9L)).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(banco));
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(clienteFuncionario);
		when(usuarioRepository.saveAndFlush(banco)).thenReturn(banco);

		/* Editar os próprios dados, sem se bloquear, é permitido */
		assertEquals("login.novo", usuarioService.atualizar(usuarioDto(9L, "login.novo")).getLogin());
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
	public void testAtualizarSemId() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.atualizar(usuarioDto(null, "maria")));

		assertEquals("Id do usuário deve ser informado para atualizar.", erro.getMessage());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testAtualizarUsuarioDeOutraEmpresa() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(99L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.atualizar(usuarioDto(99L, "maria")));

		assertEquals("Usuário não encontrado para a empresa logada.", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarPessoaDeOutroUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(usuario(9L, "maria")));
		when(usuarioRepository.existeOutroUsuarioComPessoa(PESSOA_ID, 9L, EMPRESA_ID)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.atualizar(usuarioDto(9L, "maria")));

		assertEquals("Existe outro usuário associado a pessoa que foi selecionada.", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarSemClienteFuncionario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(usuario(9L, "maria")));
		when(clienteFuncionarioService.findByPessoa(PESSOA_ID, EMPRESA_ID)).thenReturn(null);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.atualizar(usuarioDto(9L, "maria")));

		assertEquals("Não foi informado o registro de pessoa/ cliente ou funcioário para o usuário.", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	// ==================== alterarSenha ====================

	@Test
	public void testAlterarSenha() {
		Usuario usuario = usuario(9L, "maria");
		when(usuarioLogadoService.isAdmin()).thenReturn(false);
		when(usuarioLogadoService.getUsuarioLogadoId()).thenReturn(9L);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(usuario));
		when(passwordEncoder.matches("Senha@Nova1", HASH_ATUAL)).thenReturn(false);
		when(passwordEncoder.matches("Senha@Atual1", HASH_ATUAL)).thenReturn(true);
		when(passwordEncoder.encode("Senha@Nova1")).thenReturn("hash-da-senha-nova");

		usuarioService.alterarSenha(alterarSenha(9L));

		assertEquals("hash-da-senha-nova", usuario.getSenha());
		verify(usuarioRepository).saveAndFlush(usuario);
	}

	@Test
	public void testAlterarSenhaAdministradorDeOutroUsuario() {
		Usuario outro = usuario(2L, "joao");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(outro));
		when(passwordEncoder.matches("Senha@Nova1", HASH_ATUAL)).thenReturn(false);
		when(passwordEncoder.matches("Senha@Atual1", HASH_ATUAL)).thenReturn(true);
		when(passwordEncoder.encode("Senha@Nova1")).thenReturn("hash-da-senha-nova");

		usuarioService.alterarSenha(alterarSenha(2L));

		verify(usuarioRepository).saveAndFlush(outro);
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
	public void testAlterarSenhaUsuarioNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.alterarSenha(alterarSenha(2L)));

		assertEquals("Usuário não encontrado.", erro.getMessage());
	}

	@Test
	public void testAlterarSenhaUsuarioBloqueado() {
		Usuario bloqueado = usuario(2L, "joao");
		bloqueado.setLiberado(false);
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(bloqueado));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.alterarSenha(alterarSenha(2L)));

		assertEquals("Usuário bloqueado, entre em contato com o administrador do sistema.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
	}

	@Test
	public void testAlterarSenhaEmpresaBloqueada() {
		empresa.setBloqueio(true);
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(usuario(2L, "joao")));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.alterarSenha(alterarSenha(2L)));

		assertEquals("Empresa bloqueada, entre em contato com o administrador do sistema.", erro.getMessage());
		assertEquals(HttpStatus.UNAUTHORIZED, erro.getStatus());
	}

	@Test
	public void testAlterarSenhaConfirmacaoDiferente() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(usuario(2L, "joao")));
		AlterarSenhaDTO dto = alterarSenha(2L);
		dto.setConfirmarSenha("Outra@Senha1");

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.alterarSenha(dto));

		assertEquals("A confirmação da senha não confere.", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAlterarSenhaIgualAAtual() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(usuario(2L, "joao")));
		when(passwordEncoder.matches("Senha@Nova1", HASH_ATUAL)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.alterarSenha(alterarSenha(2L)));

		assertEquals("A nova senha deve ser diferente da atual", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAlterarSenhaAtualInvalida() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(2L, EMPRESA_ID)).thenReturn(Optional.of(usuario(2L, "joao")));
		when(passwordEncoder.matches("Senha@Nova1", HASH_ATUAL)).thenReturn(false);
		when(passwordEncoder.matches("Senha@Atual1", HASH_ATUAL)).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.alterarSenha(alterarSenha(2L)));

		assertEquals("Senha atual inválida", erro.getMessage());
		verify(usuarioRepository, never()).saveAndFlush(any());
	}

	// ==================== consultas que devolvem a entidade ====================

	@Test
	public void testFindAll() {
		Usuario usuario = usuario(9L, "maria");
		when(usuarioRepository.findAll(EMPRESA_ID)).thenReturn(List.of(usuario));

		assertSame(usuario, usuarioService.findAll(EMPRESA_ID).get(0));
	}

	@Test
	public void testBuscaPorLogin() {
		Usuario usuario = usuario(9L, "maria");
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario);

		assertSame(usuario, usuarioService.buscaPorLogin("maria"));
	}

	@Test
	public void testBuscaPorNome() {
		when(usuarioRepository.buscaPorNome("maria", EMPRESA_ID)).thenReturn(List.of(usuario(9L, "maria")));

		assertEquals(9L, usuarioService.buscaPorNome("maria", EMPRESA_ID).get(0).getId());
	}

	@Test
	public void testExistePorNome() {
		when(usuarioRepository.existePorNome("Maria", EMPRESA_ID)).thenReturn(true);

		assertTrue(usuarioService.existePorNome("Maria", EMPRESA_ID));
		assertFalse(usuarioService.existePorNome("João", EMPRESA_ID));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		when(usuarioRepository.existePorNomeDiferenteId(9L, "Maria", EMPRESA_ID)).thenReturn(true);

		assertTrue(usuarioService.existePorNomeDiferenteId(9L, "Maria", EMPRESA_ID));
		assertFalse(usuarioService.existePorNomeDiferenteId(8L, "Maria", EMPRESA_ID));
	}

	@Test
	public void testBuscarPorIds() {
		when(usuarioRepository.buscarPorIds(List.of(9L), EMPRESA_ID)).thenReturn(List.of(usuario(9L, "maria")));

		assertEquals(1, usuarioService.buscarPorIds(List.of(9L), EMPRESA_ID).size());
	}

	@Test
	public void testExistsById() {
		when(usuarioRepository.existsById(9L, EMPRESA_ID)).thenReturn(true);

		assertTrue(usuarioService.existsById(9L, EMPRESA_ID));
		assertFalse(usuarioService.existsById(8L, EMPRESA_ID));
	}

	@Test
	public void testBuscarPorId() {
		Usuario usuario = usuario(9L, "maria");
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(usuario));

		assertSame(usuario, usuarioService.buscarPorId(9L, EMPRESA_ID).orElseThrow());
	}

	@Test
	public void testTotal() {
		when(usuarioRepository.total(EMPRESA_ID)).thenReturn(3L);

		assertEquals(3L, usuarioService.total(EMPRESA_ID));
	}

	@Test
	public void testListarPaginado() {
		Pageable pagina = PageRequest.of(0, 1);
		when(usuarioRepository.listarPaginado(EMPRESA_ID, pagina)).thenReturn(new PageImpl<>(List.of(usuario(9L, "maria")), pagina, 3));

		assertEquals(3, usuarioService.listarPaginado(EMPRESA_ID, pagina).getTotalElements());
	}

	// ==================== exclusão ====================

	/** Usuário com ROLE_ADMIN (e ROLE_USER). */
	private Usuario administrador(Long id) {
		Role user = new Role();
		user.setAcesso("ROLE_USER");
		Role admin = new Role();
		admin.setAcesso("ROLE_ADMIN");
		Usuario usuario = usuario(id, "admin." + id);
		usuario.setAcessos(List.of(user, admin));
		return usuario;
	}

	@Test
	public void testDeleteById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(usuario(9L, "maria")));

		usuarioService.deleteById(9L, EMPRESA_ID);

		/* Primeiro solta o cliente/funcionário (FK), depois apaga o usuário */
		InOrder ordem = inOrder(clienteFuncionarioService, usuarioRepository);
		ordem.verify(clienteFuncionarioService).removeUserClienteFuncionarioId(9L, EMPRESA_ID);
		ordem.verify(usuarioRepository).deleteById(9L, EMPRESA_ID);
	}

	@Test
	public void testDeleteByIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deleteById(9L, EMPRESA_ID));

		assertEquals("Usuário não encontrado ou já foi deletado.", erro.getMessage());
		verify(usuarioRepository, never()).deleteById(anyLong(), anyLong());
		verifyNoInteractions(clienteFuncionarioService);
	}

	@Test
	public void testDeleteByIdDoProprioUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.isUsuarioLogado(9L)).thenReturn(true);
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(administrador(9L)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deleteById(9L, EMPRESA_ID));

		/* P25: o admin logado não se apaga */
		assertEquals(UsuarioService.MSG_APAGAR_A_SI_MESMO, erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(usuarioRepository, never()).deleteById(anyLong(), anyLong());
		verifyNoInteractions(clienteFuncionarioService);
	}

	@Test
	public void testDeleteByIdDeOutroAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorId(8L, EMPRESA_ID)).thenReturn(Optional.of(administrador(8L)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deleteById(8L, EMPRESA_ID));

		/* P25: nenhum admin é apagado; a mensagem diz como fazer */
		assertEquals(UsuarioService.MSG_ADMIN_NAO_APAGA, erro.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
		verify(usuarioRepository, never()).deleteById(anyLong(), anyLong());
		verifyNoInteractions(clienteFuncionarioService);
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
	public void testDeleteAll() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.findAll(EMPRESA_ID)).thenReturn(List.of(usuario(8L, "a"), administrador(9L), usuario(10L, "b")));

		/* P25: apaga só quem não é admin (o logado e os outros admins ficam) */
		assertEquals(2L, usuarioService.deleteAll(EMPRESA_ID));

		/* Solta os vínculos de cliente/funcionário antes (FK), como no deleteById */
		verify(clienteFuncionarioService).removeUserClienteFuncionarioId(8L, EMPRESA_ID);
		verify(clienteFuncionarioService).removeUserClienteFuncionarioId(10L, EMPRESA_ID);
		verify(clienteFuncionarioService, never()).removeUserClienteFuncionarioId(9L, EMPRESA_ID);
		verify(usuarioRepository).deletarAllById(List.of(8L, 10L), EMPRESA_ID);
		verify(usuarioRepository, never()).deleteAll(anyLong());
	}

	@Test
	public void testDeleteAllSoComAdministradores() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.findAll(EMPRESA_ID)).thenReturn(List.of(administrador(9L)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deleteAll(EMPRESA_ID));

		assertEquals(UsuarioService.MSG_NADA_A_APAGAR, erro.getMessage());
		verify(usuarioRepository, never()).deletarAllById(any(), any());
		verifyNoInteractions(clienteFuncionarioService);
	}

	@Test
	public void testDeleteAllSemRegistros() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.findAll(EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deleteAll(EMPRESA_ID));

		assertEquals(UsuarioService.MSG_NADA_A_APAGAR, erro.getMessage());
		verify(usuarioRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeleteAllSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deleteAll(EMPRESA_ID));

		assertEquals("Apenas administradores podem deletar usuários.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(usuarioRepository);
	}

	@Test
	public void testDeletarAllById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorIds(List.of(8L, 9L), EMPRESA_ID)).thenReturn(List.of(usuario(8L, "a"), usuario(9L, "b")));

		usuarioService.deletarAllById(List.of(8L, 9L), EMPRESA_ID);

		verify(clienteFuncionarioService).removeUserClienteFuncionarioId(8L, EMPRESA_ID);
		verify(clienteFuncionarioService).removeUserClienteFuncionarioId(9L, EMPRESA_ID);
		verify(usuarioRepository).deletarAllById(List.of(8L, 9L), EMPRESA_ID);
	}

	@Test
	public void testDeletarAllByIdComOProprioUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioLogadoService.isUsuarioLogado(8L)).thenReturn(true);
		when(usuarioRepository.buscarPorIds(List.of(8L, 9L), EMPRESA_ID)).thenReturn(List.of(administrador(8L), usuario(9L, "b")));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deletarAllById(List.of(8L, 9L), EMPRESA_ID));

		assertEquals(UsuarioService.MSG_APAGAR_A_SI_MESMO, erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verify(usuarioRepository, never()).deletarAllById(any(), any());
		verifyNoInteractions(clienteFuncionarioService);
	}

	@Test
	public void testDeletarAllByIdComAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorIds(List.of(8L, 9L), EMPRESA_ID)).thenReturn(List.of(usuario(8L, "a"), administrador(9L)));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deletarAllById(List.of(8L, 9L), EMPRESA_ID));

		/* Tudo ou nada: o usuário comum da lista também fica */
		assertEquals("Usuários administradores não podem ser apagados: [9]. Remova antes o acesso ROLE_ADMIN deles em Acessos do "
				+ "usuário ou tire-os da lista.", erro.getMessage());
		verify(usuarioRepository, never()).deletarAllById(any(), any());
		verifyNoInteractions(clienteFuncionarioService);
	}

	@Test
	public void testDeletarAllByIdComIdNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorIds(List.of(8L, 9L), EMPRESA_ID)).thenReturn(List.of(usuario(9L, "b")));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deletarAllById(List.of(8L, 9L), EMPRESA_ID));

		assertEquals("Usuários não encontrados ou já deletados: [8]", erro.getMessage());
		verify(usuarioRepository, never()).deletarAllById(any(), any());
	}

	@Test
	public void testDeletarAllByIdNenhumEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorIds(List.of(8L), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deletarAllById(List.of(8L), EMPRESA_ID));

		assertEquals("Usuários não encontrados ou já deletados: [8]", erro.getMessage());
	}

	@Test
	public void testDeletarAllByIdListaVazia() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(usuarioRepository.buscarPorIds(List.of(), EMPRESA_ID)).thenReturn(List.of());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deletarAllById(List.of(), EMPRESA_ID));

		assertEquals("Nenhum registro informado para deletar.", erro.getMessage());
	}

	@Test
	public void testDeletarAllByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.deletarAllById(List.of(9L), EMPRESA_ID));

		assertEquals("Apenas administradores podem deletar usuários.", erro.getMessage());
		verifyNoInteractions(usuarioRepository);
	}

	// ==================== consultas que devolvem DTO ====================

	@Test
	public void testListar() {
		when(usuarioRepository.listar(EMPRESA_ID)).thenReturn(List.of(usuario(9L, "maria")));

		UsuarioDto dto = usuarioService.listar(EMPRESA_ID).get(0);

		assertEquals(9L, dto.getId());
		assertEquals("maria", dto.getLogin());
		assertEquals(true, dto.getLiberado());
		assertEquals("JDev Treinamento", dto.getEmpresa());
		assertEquals(6L, dto.getClienteFuncionarioId());
		assertEquals(TipoClienteFuncionario.FUNCIONARIO, dto.getTipoClienteFuncionario());
		assertEquals(PESSOA_ID, dto.getPessoaId());
		assertEquals("Maria da Silva", dto.getPessoa());
		/* A senha nunca vai para o DTO */
		assertNull(dto.getSenha());
	}

	@Test
	public void testBuscarPorIdDto() {
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.of(usuario(9L, "maria")));

		UsuarioDto dto = usuarioService.buscarPorIdDto(9L, EMPRESA_ID);

		assertEquals("maria", dto.getLogin());
		assertNull(dto.getSenha());
	}

	@Test
	public void testBuscarPorIdDtoNaoEncontrado() {
		when(usuarioRepository.buscarPorId(9L, EMPRESA_ID)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> usuarioService.buscarPorIdDto(9L, EMPRESA_ID));

		assertEquals("Usuário não foi encontrado na busca.", erro.getMessage());
	}

	@Test
	public void testListarPaginadoDto() {
		Pageable pagina = PageRequest.of(0, 1);
		when(usuarioRepository.listarPaginado(EMPRESA_ID, pagina)).thenReturn(new PageImpl<>(List.of(usuario(9L, "maria")), pagina, 3));

		List<UsuarioDto> lista = usuarioService.listarPaginadoDto(EMPRESA_ID, pagina);

		assertEquals(1, lista.size());
		assertNull(lista.get(0).getSenha());
	}

	@Test
	public void testBuscaPorNomeDto() {
		when(usuarioRepository.buscaPorNome("maria", EMPRESA_ID)).thenReturn(List.of(usuario(9L, "maria")));

		assertEquals("Maria da Silva", usuarioService.buscaPorNomeDto("maria", EMPRESA_ID).get(0).getPessoa());
	}

	@Test
	public void testBuscaPorLoginDto() {
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario(9L, "maria"));

		assertEquals(9L, usuarioService.buscaPorLoginDto("maria", EMPRESA_ID).getId());
	}

	@Test
	public void testBuscaPorLoginDtoDeOutraEmpresa() {
		when(usuarioRepository.buscaPorLogin("maria")).thenReturn(usuario(9L, "maria"));

		/* O login é único no banco inteiro, mas o usuário de outra empresa não aparece */
		assertNull(usuarioService.buscaPorLoginDto("maria", 20L));
	}

	@Test
	public void testBuscaPorLoginDtoNaoEncontrado() {
		when(usuarioRepository.buscaPorLogin("ninguem")).thenReturn(null);

		assertNull(usuarioService.buscaPorLoginDto("ninguem", EMPRESA_ID));
	}

	@Test
	public void testBuscarPorIdsDto() {
		when(usuarioRepository.buscarPorIds(List.of(9L), EMPRESA_ID)).thenReturn(List.of(usuario(9L, "maria")));

		assertEquals("maria", usuarioService.buscarPorIdsDto(List.of(9L), EMPRESA_ID).get(0).getLogin());
	}

}
