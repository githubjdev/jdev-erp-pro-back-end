package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Role;

/** Role é global (sem empresa). */
public class TestRoleRepository extends TestContextoRepository {

	@Autowired
	private RoleRepository roleRepository;

	private Role novaRole() {
		Role role = new Role();
		role.setAcesso("ROLE_TESTE_" + sufixo.toUpperCase());
		return roleRepository.saveAndFlush(role);
	}

	private Role roleAdmin() {
		return roleRepository.findAll().stream().filter(r -> r.getAcesso().equals("ROLE_ADMIN")).findFirst().orElseThrow();
	}

	@Test
	public void testFindAll() {
		List<String> acessos = roleRepository.findAll().stream().map(Role::getAcesso).toList();

		assertTrue(acessos.containsAll(List.of("ROLE_USER", "ROLE_ADMIN")));
	}

	@Test
	public void testBuscaPorAcesso() {
		/* Parte do nome, em minúsculas */
		assertTrue(roleRepository.buscaPorAcesso("admin").stream().anyMatch(r -> r.getAcesso().equals("ROLE_ADMIN")));
		assertTrue(roleRepository.buscaPorAcesso("nao-existe-" + sufixo).isEmpty());
	}

	@Test
	public void testExistePorAcesso() {
		assertTrue(roleRepository.existePorAcesso(" role_admin "));
		assertFalse(roleRepository.existePorAcesso("ROLE"));
	}

	@Test
	public void testExistePorAcessoDiferenteId() {
		Role admin = roleAdmin();

		assertFalse(roleRepository.existePorAcessoDiferenteId(admin.getId(), "ROLE_ADMIN"));
		assertTrue(roleRepository.existePorAcessoDiferenteId(-1L, "ROLE_ADMIN"));
	}

	@Test
	public void testExistePorId() {
		assertTrue(roleRepository.existePorId(roleAdmin().getId()));
		assertFalse(roleRepository.existePorId(-1L));
	}

	@Test
	public void testDeleteById() {
		Role role = novaRole();
		assertEquals(role.getAcesso(), roleRepository.findById(role.getId()).orElseThrow().getAcesso());

		roleRepository.deleteById(role.getId());

		assertFalse(roleRepository.existePorId(role.getId()));
	}

}
