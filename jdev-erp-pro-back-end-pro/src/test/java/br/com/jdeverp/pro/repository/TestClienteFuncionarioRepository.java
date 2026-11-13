package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;

public class TestClienteFuncionarioRepository extends TestContextoRepository {

	private ClienteFuncionario novo(String nome) {
		return novoClienteFuncionario(novaPessoa(unico(nome)));
	}

	@Test
	public void testFindAll() {
		ClienteFuncionario clienteFuncionario = novo("Cliente Lista");

		assertTrue(clienteFuncionarioRepository.findAll(EMPRESA_ID).stream().anyMatch(c -> c.getId().equals(clienteFuncionario.getId())));
		assertTrue(clienteFuncionarioRepository.findAll(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testFindByPessoa() {
		ClienteFuncionario clienteFuncionario = novo("Cliente Por Pessoa");
		Long pessoaId = clienteFuncionario.getPessoa().getId();

		assertEquals(clienteFuncionario.getId(), clienteFuncionarioRepository.findByPessoa(pessoaId, EMPRESA_ID).getId());
		assertNull(clienteFuncionarioRepository.findByPessoa(pessoaId, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testBuscaPorNome() {
		ClienteFuncionario clienteFuncionario = novo("Fátima Gonçalves");

		/* Busca pelo nome da pessoa ligada */
		assertEquals(clienteFuncionario.getId(),
				clienteFuncionarioRepository.buscaPorNome("fatima goncalves " + sufixo, EMPRESA_ID).get(0).getId());
		assertTrue(clienteFuncionarioRepository.buscaPorNome("fatima goncalves " + sufixo, EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorNome() {
		ClienteFuncionario clienteFuncionario = novo("Cliente Existe");
		String nome = clienteFuncionario.getPessoa().getNome();

		assertTrue(clienteFuncionarioRepository.existePorNome(nome.toUpperCase(), EMPRESA_ID));
		assertFalse(clienteFuncionarioRepository.existePorNome(nome, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExisteParaPessoa() {
		ClienteFuncionario clienteFuncionario = novo("Cliente Para Pessoa");
		Pessoa semCadastro = novaPessoa(unico("Pessoa Sem Cliente"));

		assertTrue(clienteFuncionarioRepository.existeParaPessoa(clienteFuncionario.getPessoa().getId(), EMPRESA_ID));
		assertFalse(clienteFuncionarioRepository.existeParaPessoa(semCadastro.getId(), EMPRESA_ID));
		assertFalse(clienteFuncionarioRepository.existeParaPessoa(clienteFuncionario.getPessoa().getId(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		ClienteFuncionario clienteFuncionario = novo("Cliente Único");
		String nome = clienteFuncionario.getPessoa().getNome();

		assertFalse(clienteFuncionarioRepository.existePorNomeDiferenteId(clienteFuncionario.getId(), nome, EMPRESA_ID));
		assertTrue(clienteFuncionarioRepository.existePorNomeDiferenteId(-1L, nome, EMPRESA_ID));
		assertFalse(clienteFuncionarioRepository.existePorNomeDiferenteId(-1L, nome, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		ClienteFuncionario clienteFuncionario = novo("Cliente Apagar");

		clienteFuncionarioRepository.deleteById(clienteFuncionario.getId(), EMPRESA_INEXISTENTE);
		assertTrue(clienteFuncionarioRepository.existsById(clienteFuncionario.getId(), EMPRESA_ID));

		clienteFuncionarioRepository.deleteById(clienteFuncionario.getId(), EMPRESA_ID);
		assertFalse(clienteFuncionarioRepository.existsById(clienteFuncionario.getId(), EMPRESA_ID));
	}

	@Test
	public void testRemoveUserClienteFuncionarioId() {
		Usuario usuario = novoUsuario(unico("Usuário Desvincular"));
		Long clienteFuncionarioId = usuario.getClienteFuncionario().getId();

		/* Em outra empresa, o vínculo fica */
		clienteFuncionarioRepository.removeUserClienteFuncionarioId(usuario.getId(), EMPRESA_INEXISTENTE);
		assertNotNull(clienteFuncionarioRepository.buscarPorId(clienteFuncionarioId, EMPRESA_ID).orElseThrow().getUsuario());

		clienteFuncionarioRepository.removeUserClienteFuncionarioId(usuario.getId(), EMPRESA_ID);
		assertNull(clienteFuncionarioRepository.buscarPorId(clienteFuncionarioId, EMPRESA_ID).orElseThrow().getUsuario());
	}

}
