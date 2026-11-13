package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.model.Usuario;

/**
 * RoleUsuario não tem empresa própria, mas toda consulta filtra pela empresa
 * do usuário (r.usuario.empresa.id).
 */
public class TestRoleUsuarioRepository extends TestContextoRepository {

	@Autowired
	private RoleRepository roleRepository;

	@Autowired
	private RoleUsuarioRepository roleUsuarioRepository;

	private Role roleUser;
	private Role roleAdmin;
	private Usuario usuario;
	private RoleUsuario associacao;

	@BeforeEach
	public void associar() {
		roleUser = role("ROLE_USER");
		roleAdmin = role("ROLE_ADMIN");
		usuario = novoUsuario(unico("Usuário Role"));

		RoleUsuario roleUsuario = new RoleUsuario();
		roleUsuario.setAcesso(roleUser);
		roleUsuario.setUsuario(usuario);
		associacao = roleUsuarioRepository.saveAndFlush(roleUsuario);
	}

	private Role role(String acesso) {
		return roleRepository.findAll().stream().filter(r -> r.getAcesso().equals(acesso)).findFirst().orElseThrow();
	}

	private static List<Long> ids(List<RoleUsuario> lista) {
		return lista.stream().map(RoleUsuario::getId).toList();
	}

	@Test
	public void testFindAllByUsuario() {
		assertEquals(List.of(associacao.getId()), ids(roleUsuarioRepository.findAllByUsuario(usuario.getId(), EMPRESA_ID)));
		assertTrue(roleUsuarioRepository.findAllByUsuario(usuario.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testFindAllByRoleAndEmpresa() {
		assertTrue(ids(roleUsuarioRepository.findAllByRoleAndEmpresa(roleUser.getId(), EMPRESA_ID)).contains(associacao.getId()));
		assertFalse(ids(roleUsuarioRepository.findAllByRoleAndEmpresa(roleAdmin.getId(), EMPRESA_ID)).contains(associacao.getId()));
		assertTrue(roleUsuarioRepository.findAllByRoleAndEmpresa(roleUser.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testFindAllByEmpresa() {
		assertTrue(ids(roleUsuarioRepository.findAllByEmpresa(EMPRESA_ID)).contains(associacao.getId()));
		assertTrue(roleUsuarioRepository.findAllByEmpresa(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscarPorIdEEmpresa() {
		assertTrue(roleUsuarioRepository.buscarPorIdEEmpresa(associacao.getId(), EMPRESA_ID).isPresent());
		assertTrue(roleUsuarioRepository.buscarPorIdEEmpresa(associacao.getId(), EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorUsuarioERole() {
		assertTrue(roleUsuarioRepository.existePorUsuarioERole(usuario.getId(), roleUser.getId(), EMPRESA_ID));
		assertFalse(roleUsuarioRepository.existePorUsuarioERole(usuario.getId(), roleAdmin.getId(), EMPRESA_ID));
		assertFalse(roleUsuarioRepository.existePorUsuarioERole(usuario.getId(), roleUser.getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorUsuarioDiferenreId() {
		/* A própria associação não conta */
		assertFalse(roleUsuarioRepository.existePorUsuarioDiferenreId(usuario.getId(), roleUser.getId(), associacao.getId(), EMPRESA_ID));
		assertTrue(roleUsuarioRepository.existePorUsuarioDiferenreId(usuario.getId(), roleUser.getId(), -1L, EMPRESA_ID));
		assertFalse(roleUsuarioRepository.existePorUsuarioDiferenreId(usuario.getId(), roleUser.getId(), -1L, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		roleUsuarioRepository.deleteById(associacao.getId());

		assertTrue(roleUsuarioRepository.buscarPorIdEEmpresa(associacao.getId(), EMPRESA_ID).isEmpty());
	}

	@Test
	public void testDeleteByUsuarioAndRole() {
		roleUsuarioRepository.deleteByUsuarioAndRole(usuario.getId(), roleUser.getId(), EMPRESA_INEXISTENTE);
		assertTrue(roleUsuarioRepository.existePorUsuarioERole(usuario.getId(), roleUser.getId(), EMPRESA_ID));

		roleUsuarioRepository.deleteByUsuarioAndRole(usuario.getId(), roleUser.getId(), EMPRESA_ID);
		assertFalse(roleUsuarioRepository.existePorUsuarioERole(usuario.getId(), roleUser.getId(), EMPRESA_ID));
	}

}
