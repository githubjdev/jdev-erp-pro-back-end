package br.com.jdeverp.pro.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;

import br.com.jdeverp.pro.anotacoes.IgnoreEmpresaId;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.repository.JpaJdevRepository;

/**
 * Validação da subida: todo método declarado num repository precisa de @Query
 * com empresa.id ou empresa_id, ou de @IgnoreEmpresaId. Os repositories reais
 * são conferidos em toda subida; aqui ficam os casos que derrubariam a aplicação.
 */
public class TestRepositoryEmpresaValidator {

	interface RepositorioCorreto extends JpaJdevRepository<Categoria, Long> {
		@Query("select c from Categoria c where c.empresa.id = :idEmpresa")
		List<Categoria> porEmpresa(Long idEmpresa);

		@Query(value = "select * from categoria where empresa_id = :idEmpresa", nativeQuery = true)
		List<Categoria> nativaPorEmpresa(Long idEmpresa);

		@IgnoreEmpresaId(motivo = "teste")
		@Query("select c from Categoria c")
		List<Categoria> todasIgnorando();
	}

	@IgnoreEmpresaId(motivo = "repositório global")
	interface RepositorioGlobal {
		@Query("select c from Categoria c")
		List<Categoria> todas();
	}

	interface RepositorioSemQuery {
		List<Categoria> buscarTudo();
	}

	interface RepositorioSemEmpresa {
		@Query("select c from Categoria c where c.nome = :nome")
		List<Categoria> porNome(String nome);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static RepositoryEmpresaValidator validador(Class<?>... interfaces) {
		Map<String, RepositoryFactoryInformation> beans = new LinkedHashMap<>();
		for (Class<?> interfaceRepository : interfaces) {
			RepositoryInformation informacao = mock(RepositoryInformation.class);
			when(informacao.getRepositoryInterface()).thenReturn((Class) interfaceRepository);
			RepositoryFactoryInformation fabrica = mock(RepositoryFactoryInformation.class);
			when(fabrica.getRepositoryInformation()).thenReturn(informacao);
			beans.put(interfaceRepository.getSimpleName(), fabrica);
		}
		ApplicationContext contexto = mock(ApplicationContext.class);
		when(contexto.getBeansOfType(RepositoryFactoryInformation.class)).thenReturn(beans);
		return new RepositoryEmpresaValidator(contexto);
	}

	@Test
	public void testAfterSingletonsInstantiatedRepositoriosCorretos() {
		/* Métodos herdados do JpaRepository não são conferidos; os declarados filtram pela empresa ou são ignorados */
		assertDoesNotThrow(() -> validador(RepositorioCorreto.class, RepositorioGlobal.class).afterSingletonsInstantiated());
	}

	@Test
	public void testAfterSingletonsInstantiatedMetodoSemQuery() {
		IllegalStateException erro = assertThrows(IllegalStateException.class,
				() -> validador(RepositorioSemQuery.class).afterSingletonsInstantiated());

		/* Derived query não passa */
		assertTrue(erro.getMessage().contains("buscarTudo"), erro.getMessage());
		assertTrue(erro.getMessage().contains("deve possuir Query escrita."), erro.getMessage());
	}

	@Test
	public void testAfterSingletonsInstantiatedQuerySemEmpresa() {
		IllegalStateException erro = assertThrows(IllegalStateException.class,
				() -> validador(RepositorioCorreto.class, RepositorioSemEmpresa.class).afterSingletonsInstantiated());

		assertTrue(erro.getMessage().contains("ERRO DE SEGURANÇA"), erro.getMessage());
		assertTrue(erro.getMessage().contains("RepositorioSemEmpresa"), erro.getMessage());
		assertTrue(erro.getMessage().contains("porNome"), erro.getMessage());
	}

}
