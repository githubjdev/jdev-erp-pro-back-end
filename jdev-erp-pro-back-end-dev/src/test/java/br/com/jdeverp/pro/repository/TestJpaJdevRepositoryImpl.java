package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;

import br.com.jdeverp.pro.contexto.TestContextoSpring;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.model.Plano;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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

	// ==================== Métodos genéricos por empresa ====================

	@PersistenceContext
	private EntityManager entityManager;

	private Categoria novaCategoria(String nome) {
		Categoria categoria = new Categoria();
		categoria.setNome(nome + " " + System.nanoTime());
		categoria.setEmpresa(empresaRepository.findById(1L).orElseThrow());
		return categoriaRepository.saveAndFlush(categoria);
	}

	@Test
	public void testListarPaginado() {
		Categoria ultima = novaCategoria("Categoria paginação");

		Page<Categoria> page = categoriaRepository.listarPaginado(1L, PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "id")));

		/* A categoria mais nova vem primeiro, e o total é o da empresa inteira */
		assertEquals(ultima.getId(), page.getContent().get(0).getId());
		assertEquals(3, page.getContent().size());
		assertEquals(categoriaRepository.total(1L), page.getTotalElements());
		List<Long> ids = page.getContent().stream().map(Categoria::getId).toList();
		assertEquals(ids.stream().sorted(Comparator.reverseOrder()).toList(), ids);
	}

	@Test
	public void testListarPaginadoComVariasOrdenacoes() {
		novaCategoria("Categoria ordenação");

		Page<Categoria> page = categoriaRepository.listarPaginado(1L,
				PageRequest.of(1, 2, Sort.by(Sort.Order.asc("nome"), Sort.Order.desc("id"))));

		assertEquals(2, page.getContent().size());
		assertEquals(1, page.getNumber());
	}

	@Test
	public void testListarPaginadoSemOrdenacao() {
		novaCategoria("Categoria sem ordenação");

		Page<Categoria> page = categoriaRepository.listarPaginado(1L, PageRequest.of(0, 2));

		assertEquals(2, page.getContent().size());
		assertTrue(page.getContent().stream().allMatch(c -> c.getEmpresa().getId().equals(1L)));
	}

	@Test
	public void testListarPaginadoFiltraPelaEmpresa() {
		novaCategoria("Categoria de outra empresa");

		Page<Categoria> page = categoriaRepository.listarPaginado(EMPRESA_INEXISTENTE, PageRequest.of(0, 5, Sort.by("nome")));

		assertTrue(page.getContent().isEmpty());
		assertEquals(0, page.getTotalElements());
	}

	@Test
	public void testListarPaginadoOrdenacaoInvalida() {
		/*
		 * O campo vem do request e é concatenado no JPQL: só passa atributo simples da
		 * entidade. Relacionamento, campo inexistente ou subconsulta são recusados.
		 */
		for (String campo : List.of("empresa", "campoInexistente", "nome, (select u.tokenSessao from Usuario u)", "empresa.id")) {
			MsgApiException erro = assertThrows(MsgApiException.class,
					() -> categoriaRepository.listarPaginado(1L, PageRequest.of(0, 5, Sort.by(campo))));
			/* A mensagem não repete o valor recebido */
			assertEquals("Campo de ordenação inválido.", erro.getMessage());
		}
	}

	@Test
	public void testListarPaginadoEntidadeGlobal() {
		Page<Plano> page = planoRepository.listarPaginado(EMPRESA_INEXISTENTE, PageRequest.of(0, 50, Sort.by("nome")));

		/* Sem empresa na entidade, a empresa informada é ignorada */
		assertEquals(planoRepository.count(), page.getTotalElements());
		assertEquals(planoRepository.count(), page.getContent().size());
	}

	@Test
	public void testTotal() {
		novaCategoria("Categoria total");

		assertEquals(categoriaRepository.listar(1L).size(), categoriaRepository.total(1L));
		assertEquals(0L, categoriaRepository.total(EMPRESA_INEXISTENTE));
		assertEquals(planoRepository.count(), planoRepository.total(EMPRESA_INEXISTENTE));
	}

	@Test
	public void testBuscarPorId() {
		Categoria categoria = novaCategoria("Categoria buscar");

		assertEquals(categoria.getNome(), categoriaRepository.buscarPorId(categoria.getId(), 1L).orElseThrow().getNome());
		assertTrue(categoriaRepository.buscarPorId(categoria.getId(), EMPRESA_INEXISTENTE).isEmpty());
		assertTrue(categoriaRepository.buscarPorId(-1L, 1L).isEmpty());
		/* Entidade global ignora a empresa */
		assertTrue(planoRepository.buscarPorId(1L, EMPRESA_INEXISTENTE).isPresent());
	}

	@Test
	public void testListar() {
		Categoria categoria = novaCategoria("Categoria listar");

		assertTrue(categoriaRepository.listar(1L).stream().anyMatch(c -> c.getId().equals(categoria.getId())));
		assertTrue(categoriaRepository.listar(EMPRESA_INEXISTENTE).isEmpty());
		assertEquals(planoRepository.count(), planoRepository.listar(EMPRESA_INEXISTENTE).size());
	}

	@Test
	public void testExistsByIdPorEmpresa() {
		Categoria categoria = novaCategoria("Categoria existe");

		assertTrue(categoriaRepository.existsById(categoria.getId(), 1L));
		assertFalse(categoriaRepository.existsById(categoria.getId(), EMPRESA_INEXISTENTE));
		assertFalse(categoriaRepository.existsById(-1L, 1L));
		assertTrue(planoRepository.existsById(1L, EMPRESA_INEXISTENTE));
	}

	@Test
	public void testBuscarPorIds() {
		Categoria categoria = novaCategoria("Categoria ids");

		assertEquals(List.of(categoria.getId()),
				categoriaRepository.buscarPorIds(List.of(categoria.getId(), -99L), 1L).stream().map(Categoria::getId).toList());
		assertTrue(categoriaRepository.buscarPorIds(List.of(categoria.getId()), EMPRESA_INEXISTENTE).isEmpty());
		/* Lista vazia não vai ao banco ("in ()" seria inválido) */
		assertTrue(categoriaRepository.buscarPorIds(List.of(), 1L).isEmpty());
		assertEquals(1, planoRepository.buscarPorIds(List.of(1L), EMPRESA_INEXISTENTE).size());
	}

	// ==================== Métodos do Spring Data bloqueados em entidade com empresa ====================

	private static void bloqueado(Executable chamada) {
		UnsupportedOperationException erro = assertThrows(UnsupportedOperationException.class, chamada);
		assertTrue(erro.getMessage().contains("não pode ser usado"), erro.getMessage());
		assertTrue(erro.getMessage().contains("empresa.id"), erro.getMessage());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testMetodosDoSpringDataBloqueados() {
		Categoria categoria = novaCategoria("Categoria bloqueio");
		Example<Categoria> exemplo = Example.of(new Categoria());

		bloqueado(() -> categoriaRepository.findAll());
		bloqueado(() -> categoriaRepository.findAll(Sort.by("nome")));
		bloqueado(() -> categoriaRepository.findAll(PageRequest.of(0, 5)));
		bloqueado(() -> categoriaRepository.findAll(exemplo));
		bloqueado(() -> categoriaRepository.findAll(exemplo, PageRequest.of(0, 5)));
		bloqueado(() -> categoriaRepository.findAll(exemplo, Sort.by("nome")));
		bloqueado(() -> categoriaRepository.findById(categoria.getId()));
		bloqueado(() -> categoriaRepository.findAllById(List.of(categoria.getId())));
		bloqueado(() -> categoriaRepository.getReferenceById(categoria.getId()));
		bloqueado(() -> categoriaRepository.getOne(categoria.getId()));
		bloqueado(() -> categoriaRepository.getById(categoria.getId()));
		bloqueado(() -> categoriaRepository.existsById(categoria.getId()));
		bloqueado(() -> categoriaRepository.count());
		bloqueado(() -> categoriaRepository.count(exemplo));
		bloqueado(() -> categoriaRepository.findOne(exemplo));
		bloqueado(() -> categoriaRepository.exists(exemplo));
		bloqueado(() -> categoriaRepository.findBy(exemplo, consulta -> consulta.all()));
		bloqueado(() -> categoriaRepository.delete(categoria));
		bloqueado(() -> categoriaRepository.deleteById(categoria.getId()));

		/* Nada foi apagado */
		assertTrue(categoriaRepository.existsById(categoria.getId(), 1L));
	}

	@Test
	public void testMetodosDeSpecificationBloqueados() {
		/* O JpaJdevRepository não expõe Specification; os overrides valem para quem instanciar o repositório direto */
		JpaJdevRepositoryImpl<Categoria, Long> repositorio = new JpaJdevRepositoryImpl<>(Categoria.class, entityManager);

		bloqueado(() -> repositorio.findAll((Specification<Categoria>) null));
		bloqueado(() -> repositorio.findAll((Specification<Categoria>) null, PageRequest.of(0, 5)));
		bloqueado(() -> repositorio.findAll((Specification<Categoria>) null, Sort.by("nome")));
		bloqueado(() -> repositorio.findAll((Specification<Categoria>) null, (Specification<Categoria>) null, PageRequest.of(0, 5)));
		bloqueado(() -> repositorio.findAll((PredicateSpecification<Categoria>) null));
		bloqueado(() -> repositorio.count((Specification<Categoria>) null));
		bloqueado(() -> repositorio.count((PredicateSpecification<Categoria>) null));
	}

	private Plano novoPlano() {
		Plano plano = new Plano();
		plano.setNome("Plano Spring Data " + System.nanoTime());
		plano.setDescricao("Plano de teste");
		plano.setAtivo(true);
		plano.setValorMensal(99.0);
		plano.setLimiteUsuario(5);
		plano.setLimiteCliente(10);
		plano.setTipoPlano(br.com.jdeverp.pro.enums.TipoPlano.ENTERPRISE);
		return planoRepository.saveAndFlush(plano);
	}

	@Test
	@SuppressWarnings("deprecation")
	public void testMetodosDoSpringDataLiberadosEmEntidadeGlobal() {
		/* Sem empresa na entidade, o bloqueio não se aplica: tudo passa para o SimpleJpaRepository */
		Plano plano = novoPlano();
		long total = planoRepository.count();
		Example<Plano> exemplo = Example.of(plano);

		assertEquals(total, planoRepository.findAll(Sort.by("nome")).size());
		assertEquals(total, planoRepository.findAll(PageRequest.of(0, 50)).getTotalElements());
		assertEquals(1, planoRepository.findAll(exemplo).size());
		assertEquals(1, planoRepository.findAll(exemplo, PageRequest.of(0, 5)).getTotalElements());
		assertEquals(1, planoRepository.findAll(exemplo, Sort.by("nome")).size());
		assertEquals(1, planoRepository.findAllById(List.of(plano.getId())).size());
		assertEquals(plano.getId(), planoRepository.getReferenceById(plano.getId()).getId());
		assertEquals(plano.getId(), planoRepository.getOne(plano.getId()).getId());
		assertEquals(plano.getId(), planoRepository.getById(plano.getId()).getId());
		assertEquals(1L, planoRepository.count(exemplo));
		assertEquals(plano.getId(), planoRepository.findOne(exemplo).orElseThrow().getId());
		assertTrue(planoRepository.exists(exemplo));
		Long contagem = planoRepository.findBy(exemplo, consulta -> consulta.count());
		assertEquals(1L, contagem);

		/* Specification só pelo repositório instanciado direto */
		JpaJdevRepositoryImpl<Plano, Long> global = new JpaJdevRepositoryImpl<>(Plano.class, entityManager);
		Specification<Plano> todos = (raiz, consulta, cb) -> cb.conjunction();
		PredicateSpecification<Plano> todosPredicado = (raiz, cb) -> cb.conjunction();
		assertEquals(total, global.findAll(todos).size());
		assertEquals(total, global.findAll(todos, PageRequest.of(0, 50)).getTotalElements());
		assertEquals(total, global.findAll(todos, Sort.by("nome")).size());
		assertEquals(total, global.findAll(todos, todos, PageRequest.of(0, 50)).getTotalElements());
		assertEquals(total, global.findAll(todosPredicado).size());
		assertEquals(total, global.count(todos));
		assertEquals(total, global.count(todosPredicado));
	}

	@Test
	public void testExclusoesDoSpringDataLiberadasEmEntidadeGlobal() {
		Plano porEntidade = novoPlano();
		Plano porId = novoPlano();
		Plano porIds = novoPlano();
		Plano porIdsEmLote = novoPlano();
		Plano emLote = novoPlano();
		Plano porLista = novoPlano();

		planoRepository.delete(porEntidade);
		planoRepository.deleteById(porId.getId());
		planoRepository.deleteAllById(List.of(porIds.getId()));
		planoRepository.deleteAllByIdInBatch(List.of(porIdsEmLote.getId()));
		planoRepository.deleteAllInBatch(List.of(emLote));
		planoRepository.deleteAll(List.of(porLista));
		planoRepository.flush();
		entityManager.clear();

		for (Plano plano : List.of(porEntidade, porId, porIds, porIdsEmLote, emLote, porLista)) {
			assertFalse(planoRepository.existsById(plano.getId()), plano.getNome());
		}
		/* deleteAll() e deleteAllInBatch() sem filtro apagariam todos os planos: não são chamados aqui */
	}

	@Test
	public void testConstrutorComClasseDaEntidade() {
		/* O construtor (classe, EntityManager) também detecta a empresa pelo metamodelo */
		JpaJdevRepositoryImpl<Categoria, Long> comEmpresa = new JpaJdevRepositoryImpl<>(Categoria.class, entityManager);
		JpaJdevRepositoryImpl<Plano, Long> global = new JpaJdevRepositoryImpl<>(Plano.class, entityManager);

		assertThrows(IllegalArgumentException.class, () -> comEmpresa.deleteAll((Long) null));
		assertEquals(0L, comEmpresa.total(EMPRESA_INEXISTENTE));
		assertEquals(planoRepository.count(), global.findAll().size());
		assertThrows(UnsupportedOperationException.class, () -> global.deleteAll(1L));
	}

}
