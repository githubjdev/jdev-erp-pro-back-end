package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
 * (P10). Renomear ROLE_USER para ROLE_ADMIN tornaria todos administradores.
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
	public void testSalvarSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.salvar(role(null, "ROLE_GERENTE")));

		assertEquals("Apenas administradores podem cadastrar acessos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(roleRepository);
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
	public void testDeleteByIdSemSerAdministrador() {
		when(usuarioLogadoService.isAdmin()).thenReturn(false);

		MsgApiException erro = assertThrows(MsgApiException.class, () -> roleService.deleteById(2L));

		assertEquals("Apenas administradores podem deletar acessos.", erro.getMessage());
		assertEquals(HttpStatus.FORBIDDEN, erro.getStatus());
		verifyNoInteractions(roleRepository);
	}

}
