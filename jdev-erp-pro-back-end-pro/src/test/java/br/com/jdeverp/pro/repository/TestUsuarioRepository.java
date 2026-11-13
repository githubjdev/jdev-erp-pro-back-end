package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoRepository;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;

public class TestUsuarioRepository extends TestContextoRepository {

	@Test
	public void testBuscaPorLogin() {
		Usuario usuario = novoUsuario(unico("Login"));

		/* Usado no login, antes de existir empresa logada: busca no banco inteiro */
		assertEquals(usuario.getId(), usuarioRepository.buscaPorLogin(usuario.getLogin()).getId());
		assertNull(usuarioRepository.buscaPorLogin("login-que-nao-existe-" + sufixo));
	}

	@Test
	public void testFindAll() {
		Usuario usuario = novoUsuario(unico("Usuário Lista"));

		assertTrue(usuarioRepository.findAll(EMPRESA_ID).stream().anyMatch(u -> u.getId().equals(usuario.getId())));
		assertTrue(usuarioRepository.findAll(EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testBuscaPorNome() {
		Usuario usuario = novoUsuario(unico("Márcia Brandão"));

		/* Pelo nome da pessoa ligada ao usuário */
		assertEquals(usuario.getId(), usuarioRepository.buscaPorNome("marcia brandao " + sufixo, EMPRESA_ID).get(0).getId());
		assertTrue(usuarioRepository.buscaPorNome("marcia brandao " + sufixo, EMPRESA_INEXISTENTE).isEmpty());
	}

	@Test
	public void testExistePorLogin() {
		Usuario usuario = novoUsuario(unico("Existe Login"));

		assertTrue(usuarioRepository.existePorLogin(" " + usuario.getLogin().toUpperCase() + " ", EMPRESA_ID));
		assertFalse(usuarioRepository.existePorLogin(usuario.getLogin(), EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorPessoa() {
		Usuario usuario = novoUsuario(unico("Existe Pessoa"));
		Long pessoaId = usuario.getClienteFuncionario().getPessoa().getId();
		Pessoa semUsuario = novaPessoa(unico("Pessoa Sem Usuário"));

		assertTrue(usuarioRepository.existePorPessoa(pessoaId, EMPRESA_ID));
		assertFalse(usuarioRepository.existePorPessoa(semUsuario.getId(), EMPRESA_ID));
		assertFalse(usuarioRepository.existePorPessoa(pessoaId, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorNome() {
		Usuario usuario = novoUsuario(unico("Existe Nome"));
		String nome = usuario.getClienteFuncionario().getPessoa().getNome();

		assertTrue(usuarioRepository.existePorNome(nome.toLowerCase(), EMPRESA_ID));
		assertFalse(usuarioRepository.existePorNome(nome, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExistePorNomeDiferenteId() {
		Usuario usuario = novoUsuario(unico("Nome Diferente Id"));
		String nome = usuario.getClienteFuncionario().getPessoa().getNome();

		assertFalse(usuarioRepository.existePorNomeDiferenteId(usuario.getId(), nome, EMPRESA_ID));
		assertTrue(usuarioRepository.existePorNomeDiferenteId(-1L, nome, EMPRESA_ID));
		assertFalse(usuarioRepository.existePorNomeDiferenteId(-1L, nome, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testExisteOutroUsuarioComPessoa() {
		Usuario usuario = novoUsuario(unico("Outro Usuário"));
		Long pessoaId = usuario.getClienteFuncionario().getPessoa().getId();

		/* O próprio usuário não conta */
		assertFalse(usuarioRepository.existeOutroUsuarioComPessoa(pessoaId, usuario.getId(), EMPRESA_ID));
		assertTrue(usuarioRepository.existeOutroUsuarioComPessoa(pessoaId, -1L, EMPRESA_ID));
		assertFalse(usuarioRepository.existeOutroUsuarioComPessoa(pessoaId, -1L, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testDeleteById() {
		Usuario usuario = novoUsuario(unico("Usuário Apagar"));
		/* O cliente/funcionário aponta para o usuário (FK): o UsuarioService solta o vínculo antes de apagar */
		clienteFuncionarioRepository.removeUserClienteFuncionarioId(usuario.getId(), EMPRESA_ID);

		usuarioRepository.deleteById(usuario.getId(), EMPRESA_INEXISTENTE);
		assertTrue(usuarioRepository.existsById(usuario.getId(), EMPRESA_ID));

		usuarioRepository.deleteById(usuario.getId(), EMPRESA_ID);
		assertFalse(usuarioRepository.existsById(usuario.getId(), EMPRESA_ID));
	}

	@Test
	public void testUpdateTokenSessaoLogin() {
		Usuario usuario = novoUsuario(unico("Token"));

		/* Em outra empresa, o token não é gravado */
		usuarioRepository.updateTokenSessaoLogin(usuario.getId(), "token-outra-empresa", EMPRESA_INEXISTENTE);
		assertFalse(usuarioRepository.existsByTokenParaUser("token-outra-empresa", usuario.getId(), EMPRESA_ID));

		usuarioRepository.updateTokenSessaoLogin(usuario.getId(), "token-" + sufixo, EMPRESA_ID);

		assertEquals("token-" + sufixo, usuarioRepository.buscarPorId(usuario.getId(), EMPRESA_ID).orElseThrow().getTokenSessao());
	}

	@Test
	public void testExistsByTokenParaUser() {
		Usuario usuario = novoUsuario(unico("Sessão"));
		usuarioRepository.updateTokenSessaoLogin(usuario.getId(), "token-1-" + sufixo, EMPRESA_ID);

		assertTrue(usuarioRepository.existsByTokenParaUser("token-1-" + sufixo, usuario.getId(), EMPRESA_ID));
		assertFalse(usuarioRepository.existsByTokenParaUser("token-1-" + sufixo, usuario.getId(), EMPRESA_INEXISTENTE));
		assertFalse(usuarioRepository.existsByTokenParaUser("token-1-" + sufixo, -1L, EMPRESA_ID));

		/* Login novo: o token anterior deixa de valer */
		usuarioRepository.updateTokenSessaoLogin(usuario.getId(), "token-2-" + sufixo, EMPRESA_ID);
		assertFalse(usuarioRepository.existsByTokenParaUser("token-1-" + sufixo, usuario.getId(), EMPRESA_ID));
		assertTrue(usuarioRepository.existsByTokenParaUser("token-2-" + sufixo, usuario.getId(), EMPRESA_ID));
	}

}
