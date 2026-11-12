package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.jdeverp.pro.contexto.TestContextoSpring;
import br.com.jdeverp.pro.model.Categoria;

/**
 * Exclusão em lote do repositório base ({@code deleteAll(empresaId)} e
 * {@code deletarAllById(ids, empresaId)}): sempre filtrada pela empresa. Roda
 * com rollback e nunca apaga a empresa 1 inteira: só um id criado no próprio
 * teste ou uma empresa que não existe.
 */
public class TestJpaJdevRepositoryImpl extends TestContextoSpring {

	private static final long EMPRESA_INEXISTENTE = -1L;

	@Autowired private PlanoRepository planoRepository;
	@Autowired private RoleRepository roleRepository;
	@Autowired private RoleUsuarioRepository roleUsuarioRepository;
	@Autowired private EmpresaRepository empresaRepository;

	@Autowired private CategoriaRepository categoriaRepository;
	@Autowired private ChamadoRepository chamadoRepository;
	@Autowired private ClienteFuncionarioRepository clienteFuncionarioRepository;
	@Autowired private ItemPedidoRepository itemPedidoRepository;
	@Autowired private MensagemRepository mensagemRepository;
	@Autowired private MovimentacaoProdutoRepository movimentacaoProdutoRepository;
	@Autowired private PedidoRepository pedidoRepository;
	@Autowired private PessoaRepository pessoaRepository;
	@Autowired private ProdutoRepository produtoRepository;
	@Autowired private UsuarioRepository usuarioRepository;

	private List<JpaJdevRepository<?, Long>> repositoriosComEmpresa() {
		return List.of(categoriaRepository, chamadoRepository, clienteFuncionarioRepository, itemPedidoRepository,
				mensagemRepository, movimentacaoProdutoRepository, pedidoRepository, pessoaRepository, produtoRepository,
				usuarioRepository);
	}

	@Test
	public void testDeleteAllEntidadeGlobal() {
		/* Sem empresa na entidade, o delete não teria filtro e apagaria os registros de todas as empresas */
		List<JpaJdevRepository<?, Long>> globais = List.of(planoRepository, roleRepository, roleUsuarioRepository,
				empresaRepository);

		for (JpaJdevRepository<?, Long> repositorio : globais) {
			UnsupportedOperationException todos = assertThrows(UnsupportedOperationException.class,
					() -> repositorio.deleteAll(1L));
			assertTrue(todos.getMessage().startsWith("Exclusão em lote não permitida em "), todos.getMessage());

			assertThrows(UnsupportedOperationException.class, () -> repositorio.deletarAllById(List.of(1L), 1L));
		}
	}

	@Test
	public void testDeleteAllSemEmpresa() {
		for (JpaJdevRepository<?, Long> repositorio : repositoriosComEmpresa()) {
			RuntimeException todos = assertThrows(RuntimeException.class, () -> repositorio.deleteAll((Long) null));
			assertTrue(todos.getMessage().startsWith("Empresa não informada para a exclusão em lote de "), todos.getMessage());

			RuntimeException porIds = assertThrows(RuntimeException.class, () -> repositorio.deletarAllById(List.of(1L), null));
			assertTrue(porIds.getMessage().startsWith("Empresa não informada para a exclusão em lote de "), porIds.getMessage());
		}
	}

	@Test
	public void testExclusaoEmLoteDoSpringDataSemEmpresa() {
		/* Métodos herdados do JpaRepository que apagariam sem filtrar pela empresa */
		for (JpaJdevRepository<?, Long> repositorio : repositoriosComEmpresa()) {
			assertThrows(UnsupportedOperationException.class, () -> repositorio.deleteAllByIdInBatch(List.of(1L)));
			assertThrows(UnsupportedOperationException.class, () -> repositorio.deleteAllInBatch(List.of()));
			assertThrows(UnsupportedOperationException.class, () -> repositorio.deleteAllById(List.of(1L)));
			assertThrows(UnsupportedOperationException.class, () -> repositorio.deleteAll(List.of()));
			assertThrows(UnsupportedOperationException.class, () -> repositorio.deleteAllInBatch());
			assertThrows(UnsupportedOperationException.class, () -> repositorio.deleteAll());
		}
	}

	@Test
	public void testDeleteAllFiltraPelaEmpresa() {
		/*
		 * Toda entidade com empresa é reconhecida pelo repositório: se não fosse, a
		 * chamada daria UnsupportedOperationException em vez de filtrar. Numa
		 * empresa que não existe, nada é apagado.
		 */
		for (JpaJdevRepository<?, Long> repositorio : repositoriosComEmpresa()) {
			assertEquals(0L, repositorio.deleteAll(EMPRESA_INEXISTENTE));
		}
	}

	@Test
	public void testDeletarAllByIdFiltraPelaEmpresa() {
		Categoria categoria = new Categoria();
		categoria.setNome("Categoria exclusão em lote");
		categoria.setEmpresa(empresaRepository.findById(1L).orElseThrow());
		Long id = categoriaRepository.saveAndFlush(categoria).getId();

		/* O id existe, mas é de outra empresa: fica */
		categoriaRepository.deletarAllById(List.of(id), EMPRESA_INEXISTENTE);
		assertTrue(categoriaRepository.existsById(id, 1L));

		categoriaRepository.deletarAllById(List.of(id), 1L);
		assertFalse(categoriaRepository.existsById(id, 1L));
	}

}
