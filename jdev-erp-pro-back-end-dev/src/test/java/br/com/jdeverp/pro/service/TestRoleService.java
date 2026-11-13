package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import br.com.jdeverp.pro.dto.RoleDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.repository.RoleRepository;

/**
 * Acesso decide quem é administrador: só ROLE_ADMIN cadastra, altera ou apaga
 * (P10). Renomear ROLE_USER para ROLE_ADMIN tornaria todos administradores,
 * ROLE_USER e ROLE_ADMIN não são renomeadas, e o admin logado não renomeia nem
 * apaga um acesso que ele mesmo tem (P25).
 */
@ExtendWith(MockitoExtension.class)
public class TestRoleService {

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private UsuarioLogadoService usuarioLogadoService;

	@InjectMocks
	private RoleService roleService;

	private static Role role(Long id, String acesso) {
		Role role = new Role();
		role.setId(id);
		role.setAcesso(acesso);
		return role;
	}

	@Test
	public void testSalvar() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.existePorAcesso("ROLE_GERENTE")).thenReturn(false);
		when(roleRepository.saveAndFlush(any(Role.class))).thenAnswer(i -> {
			Role salva = i.getArgument(0);
			salva.setId(9L);
			return salva;
		});

		RoleDTO dto = roleService.salvar(role(null, "role_gerente"));

		assertEquals(9L, dto.getId());
		assertEquals("ROLE_GERENTE", dto.getAcesso());
	}

	@Test
	public void testSalvarSemPrefixoRole() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.salvar(role(null, "GERENTE")));

		assertEquals("Nomenclatura de acesso não contém ROLE_ no incio", erro.getMessage());
		verifyNoInteractions(roleRepository);
	}

	@Test
	public void testSalvarDuplicado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.existePorAcesso("ROLE_GERENTE")).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.salvar(role(null, "ROLE_GERENTE")));

		assertEquals("Já existe acesso com a mesma descrição: ROLE_GERENTE", erro.getMessage());
		verify(roleRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testSalvarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.salvar(role(null, "ROLE_GERENTE")));

		assertEquals("Apenas administradores podem cadastrar acessos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(roleRepository);
	}

	@Test
	public void testAtualizar() {
		Role existente = role(3L, "ROLE_FINANCEIRO_ANTIGO");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(3L)).thenReturn(Optional.of(existente));
		when(roleRepository.existePorAcessoDiferenteId(3L, "ROLE_FINANCEIRO")).thenReturn(false);
		when(roleRepository.saveAndFlush(existente)).thenReturn(existente);

		RoleDTO dto = roleService.atualizar(role(3L, "ROLE_FINANCEIRO"));

		assertEquals(3L, dto.getId());
		assertEquals("ROLE_FINANCEIRO", dto.getAcesso());
		/* Grava o registro do banco, não o objeto do request */
		assertEquals("ROLE_FINANCEIRO", existente.getAcesso());
		verify(roleRepository).saveAndFlush(existente);
	}

	@Test
	public void testAtualizarNaoEncontrado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(3L)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.atualizar(role(3L, "ROLE_FINANCEIRO")));

		/* Com um id que não existe, o save criaria um acesso novo */
		assertEquals("Acesso com id: 3 não foi encontrado.", erro.getMessage());
		verify(roleRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarAcessoDoProprioUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(3L)).thenReturn(Optional.of(role(3L, "ROLE_GERENTE")));
		when(usuarioLogadoService.possuiAcesso(3L)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.atualizar(role(3L, "ROLE_SUPERVISOR")));

		/* P25: renomear um acesso que o admin logado tem tiraria o acesso dele */
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		assertTrue(erro.getMessage().startsWith("Você não pode renomear o acesso ROLE_GERENTE: ele faz parte do seu usuário"),
				erro.getMessage());
		assertTrue(erro.getMessage().contains("cadastre um acesso novo"), erro.getMessage());
		verify(roleRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarRenomeandoAcessoDoSistema() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "ROLE_USER")));
		when(roleRepository.findById(2L)).thenReturn(Optional.of(role(2L, "ROLE_ADMIN")));

		/* O cadastro de usuário procura a ROLE_USER pelo nome; o admin é reconhecido pela ROLE_ADMIN */
		MsgApiException erroUser = assertThrows(MsgApiException.class, () -> roleService.atualizar(role(1L, "ROLE_VENDEDOR")));
		MsgApiException erroAdmin = assertThrows(MsgApiException.class, () -> roleService.atualizar(role(2L, "ROLE_SUPER")));

		assertEquals("O acesso ROLE_USER é do sistema e não pode ser renomeado: o cadastro de usuários e a identificação "
				+ "do administrador dependem desse nome. Para usar outro nome, cadastre um acesso novo.", erroUser.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erroUser.getStatus());
		assertTrue(erroAdmin.getMessage().startsWith("O acesso ROLE_ADMIN é do sistema e não pode ser renomeado"),
				erroAdmin.getMessage());
		/* A recusa vem antes de qualquer outra conferência */
		verify(usuarioLogadoService, never()).possuiAcesso(anyLong());
		verify(roleRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarAcessoDoSistemaSemRenomear() {
		Role user = role(1L, "ROLE_USER");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(1L)).thenReturn(Optional.of(user));
		when(roleRepository.saveAndFlush(user)).thenReturn(user);

		/* Mesmo nome: nada muda */
		assertEquals("ROLE_USER", roleService.atualizar(role(1L, "ROLE_USER")).getAcesso());
	}

	@Test
	public void testAtualizarAcessoDoProprioUsuarioSemRenomear() {
		Role admin = role(2L, "ROLE_ADMIN");
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(2L)).thenReturn(Optional.of(admin));
		when(roleRepository.saveAndFlush(admin)).thenReturn(admin);

		/* Mesmo nome: não muda o acesso de ninguém */
		assertEquals("ROLE_ADMIN", roleService.atualizar(role(2L, "ROLE_ADMIN")).getAcesso());
	}

	@Test
	public void testAtualizarSemId() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.atualizar(role(null, "ROLE_X")));

		assertEquals("Id do acesso deve ser informado para edição.", erro.getMessage());
		verifyNoInteractions(roleRepository);
	}

	@Test
	public void testAtualizarSemPrefixoRole() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(3L)).thenReturn(Optional.of(role(3L, "ROLE_FINANCEIRO")));

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.atualizar(role(3L, "FINANCEIRO")));

		assertEquals("Nomenclatura de acesso não contém ROLE_ no incio", erro.getMessage());
		verify(roleRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarDuplicado() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(3L)).thenReturn(Optional.of(role(3L, "ROLE_OUTRO")));
		when(roleRepository.existePorAcessoDiferenteId(3L, "ROLE_ADMIN")).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.atualizar(role(3L, "ROLE_ADMIN")));

		assertEquals("Já exite outro acesso com a mesma descrição: ROLE_ADMIN", erro.getMessage());
		verify(roleRepository, never()).saveAndFlush(any());
	}

	@Test
	public void testAtualizarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		/* A tentativa de escalada: renomear o acesso comum para ROLE_ADMIN */
		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.atualizar(role(1L, "ROLE_ADMIN")));

		assertEquals("Apenas administradores podem atualizar acessos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(roleRepository);
	}

	@Test
	public void testListar() {
		when(usuarioLogadoService.getEmpresaIdLogada()).thenReturn(10L);
		when(roleRepository.listar(10L)).thenReturn(List.of(role(1L, "ROLE_USER"), role(2L, "ROLE_ADMIN")));

		List<RoleDTO> lista = roleService.listar();

		assertEquals(List.of("ROLE_USER", "ROLE_ADMIN"), lista.stream().map(RoleDTO::getAcesso).toList());
	}

	@Test
	public void testBuscarPorId() {
		when(roleRepository.findById(2L)).thenReturn(Optional.of(role(2L, "ROLE_ADMIN")));
		when(roleRepository.findById(3L)).thenReturn(Optional.empty());

		assertEquals("ROLE_ADMIN", roleService.buscarPorId(2L).orElseThrow().getAcesso());
		assertTrue(roleService.buscarPorId(3L).isEmpty());
	}

	@Test
	public void testBuscaPorAcesso() {
		Role admin = role(2L, "ROLE_ADMIN");
		when(roleRepository.buscaPorAcesso("admin")).thenReturn(List.of(admin));

		/* Devolve a entidade: é usado pelo UsuarioService para associar a role ao usuário */
		assertSame(admin, roleService.buscaPorAcesso("admin").get(0));
	}

	@Test
	public void testBuscaPorAcessoDto() {
		when(roleRepository.buscaPorAcesso("admin")).thenReturn(List.of(role(2L, "ROLE_ADMIN")));

		RoleDTO dto = roleService.buscaPorAcessoDto("admin").get(0);

		assertEquals(2L, dto.getId());
		assertEquals("ROLE_ADMIN", dto.getAcesso());
	}

	@Test
	public void testExistePorAcesso() {
		when(roleRepository.existePorAcesso("ROLE_ADMIN")).thenReturn(true);

		assertTrue(roleService.existePorAcesso("ROLE_ADMIN"));
		assertFalse(roleService.existePorAcesso("ROLE_OUTRA"));
	}

	@Test
	public void testExistePorAcessoDiferenteId() {
		when(roleRepository.existePorAcessoDiferenteId(1L, "ROLE_ADMIN")).thenReturn(true);

		assertTrue(roleService.existePorAcessoDiferenteId(1L, "ROLE_ADMIN"));
		assertFalse(roleService.existePorAcessoDiferenteId(2L, "ROLE_ADMIN"));
	}

	@Test
	public void testDeleteById() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(5L)).thenReturn(Optional.of(role(5L, "ROLE_TEMPORARIA")));

		roleService.deleteById(5L);

		verify(roleRepository).deleteById(5L);
	}

	@Test
	public void testDeleteByIdJaRemovido() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(5L)).thenReturn(Optional.empty());

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.deleteById(5L));

		assertEquals("Acesso com id: 5 já foi removido do sistema.", erro.getMessage());
		verify(roleRepository, never()).deleteById(anyLong());
	}

	@Test
	public void testDeleteByIdAcessoDoProprioUsuario() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(3L)).thenReturn(Optional.of(role(3L, "ROLE_GERENTE")));
		when(usuarioLogadoService.possuiAcesso(3L)).thenReturn(true);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.deleteById(3L));

		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		assertTrue(erro.getMessage().startsWith("Você não pode apagar o acesso ROLE_GERENTE: ele faz parte do seu usuário"),
				erro.getMessage());
		assertTrue(erro.getMessage().contains("peça a outro administrador"), erro.getMessage());
		verify(roleRepository, never()).deleteById(anyLong());
	}

	@Test
	public void testDeleteByIdAcessoDoSistema() {
		when(usuarioLogadoService.isAdmin()).thenReturn(true);
		when(roleRepository.findById(1L)).thenReturn(Optional.of(role(1L, "ROLE_USER")));
		when(roleRepository.findById(2L)).thenReturn(Optional.of(role(2L, "ROLE_ADMIN")));

		/* Sem a ROLE_USER, os usuários novos nasceriam sem acesso */
		MsgApiException erroUser = assertThrows(MsgApiException.class, () -> roleService.deleteById(1L));
		MsgApiException erroAdmin = assertThrows(MsgApiException.class, () -> roleService.deleteById(2L));

		assertEquals("O acesso ROLE_USER é do sistema e não pode ser apagado: o cadastro de usuários e a identificação "
				+ "do administrador dependem desse nome. Para tirá-lo de um usuário, remova a associação em Acessos do usuário.",
				erroUser.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, erroUser.getStatus());
		assertTrue(erroAdmin.getMessage().startsWith("O acesso ROLE_ADMIN é do sistema e não pode ser apagado"),
				erroAdmin.getMessage());
		verify(usuarioLogadoService, never()).possuiAcesso(anyLong());
		verify(roleRepository, never()).deleteById(anyLong());
	}

	@Test
	public void testDeleteByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.deleteById(2L));

		assertEquals("Apenas administradores podem deletar acessos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(roleRepository);
	}

}
