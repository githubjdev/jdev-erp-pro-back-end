package br.com.jdeverp.pro.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.collections4.IterableUtils;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.PredicateSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

public class JpaJdevRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
		implements JpaJdevRepository<T, ID> {

	private static final String MSG_BLOQUEIO_QUERY = "Use ou crie um método que tenha o empresa.id incluída para a separação dos dados por empresa e ativar o multitanent.";
	private final Class<T> domainClass; /* Classe model ou entidade */
	private final EntityManager entityManager; /* É o nucleo da persistencia do JPA */
	private final boolean multiEmpresa;

	/**
	 * Cria uma nova instância do repositório genérico utilizando apenas a classe da
	 * entidade e o {@link EntityManager}.
	 *
	 * <p>
	 * Este construtor é responsável por inicializar a implementação base do
	 * {@link SimpleJpaRepository}, permitindo que todos os métodos padrões do
	 * Spring Data JPA (save, findById, delete, findAll, entre outros) funcionem
	 * corretamente.
	 * </p>
	 *
	 * <p>
	 * Além disso, armazena a classe da entidade e a referência do
	 * {@link EntityManager}, que poderão ser utilizadas pelos métodos
	 * personalizados implementados nesta classe.
	 * </p>
	 *
	 * @param domainClass   Classe da entidade JPA que será manipulada pelo
	 *                      repositório. É utilizada para identificar o tipo da
	 *                      entidade durante operações genéricas.
	 *
	 * @param entityManager Gerenciador de persistência do JPA responsável pela
	 *                      comunicação com o banco de dados. Permite executar
	 *                      consultas, persistir, atualizar, remover entidades e
	 *                      controlar o contexto de persistência.
	 */
	public JpaJdevRepositoryImpl(Class<T> domainClass, EntityManager entityManager) {
		super(domainClass, entityManager);
		this.domainClass = domainClass;
		this.entityManager = entityManager;
		multiEmpresa = possuiEmpresa();
	}

	/**
	 * Cria uma nova instância do repositório genérico utilizando os metadados da
	 * entidade fornecidos pelo Spring Data JPA.
	 *
	 * <p>
	 * Diferentemente do outro construtor, este recebe um objeto
	 * {@link JpaEntityInformation}, que contém informações completas da entidade,
	 * como:
	 * </p>
	 *
	 * <ul>
	 * <li>Classe da entidade.</li>
	 * <li>Tipo da chave primária.</li>
	 * <li>Campo anotado com {@code @Id}.</li>
	 * <li>Estratégia de geração do identificador.</li>
	 * <li>Metadados utilizados internamente pelo Spring Data JPA.</li>
	 * </ul>
	 *
	 * <p>
	 * Este construtor é o mais utilizado internamente pelo Spring durante a criação
	 * automática dos repositórios através da {@code JpaRepositoryFactory}.
	 * </p>
	 *
	 * @param entityInformation Objeto contendo todos os metadados da entidade
	 *                          gerenciados pelo Spring Data JPA.
	 *
	 * @param entityManager     Gerenciador de persistência responsável pelas
	 *                          operações de acesso ao banco de dados.
	 */
	public JpaJdevRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {

		super(entityInformation, entityManager);
		this.domainClass = entityInformation.getJavaType();
		this.entityManager = entityManager;
		multiEmpresa = possuiEmpresa();
	}

	@Override
	public Page<T> listarPaginado(Long empresaId, Pageable pageable) {
		
		
		String jpql = "from " + domainClass.getSimpleName();
		
		if (multiEmpresa) {
			jpql += " where empresa.id = :empresaId";
		}
		
		
		if (pageable.getSort().isSorted()) {
			jpql += " order by ";

			List<String> orders = new ArrayList<String>();

			for (Sort.Order order : pageable.getSort()) {
				orders.add(order.getProperty() + " " + order.getDirection().name());
			}

			jpql += String.join(",", orders);
		}
		
		TypedQuery<T> query = entityManager.createQuery(jpql, domainClass);
		
		if(multiEmpresa) {
			query.setParameter("empresaId", empresaId);
		}
		
		List<T> lista = query.setFirstResult((int)pageable.getOffset())
				             .setMaxResults(pageable.getPageSize())
				             .getResultList();

		return new PageImpl<T>(lista, pageable, total(empresaId));
	}

	/*Total de registros cadastrados de acordo com a empresa e a tabela/classe/entidade/model atual*/
	@Override
	public long total(Long empresaId) {
		
		
		String jpql = "select count(*) from " + domainClass.getSimpleName();
		
		if (multiEmpresa) {
			jpql += " where empresa.id = :empresaId";
		}
		
		TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
		
		if(multiEmpresa) {
			query.setParameter("empresaId", empresaId);
		}

		return query.getSingleResult();
	}

	private boolean possuiEmpresa() {
		try {
			return domainClass.getDeclaredField("empresa") != null;
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	@Override
	public Optional<T> buscarPorId(ID id, Long empresaId) {
	
		String jpql = "from " + domainClass.getSimpleName() + " where id = :id";
		
		if (multiEmpresa) {
			jpql += " and empresa.id = :empresaId";
		}
		
		TypedQuery<T> query = entityManager.createQuery(jpql, domainClass);
		query.setParameter("id", id);

		if (multiEmpresa) {
			query.setParameter("empresaId", empresaId);
		}
		
		return query.getResultStream().findFirst();
		
	}

	@Override
	public List<T> listar(Long empresaId) {
		
		String jpql = "from " + domainClass.getSimpleName();
		
		if (multiEmpresa) {
			jpql += " where empresa.id = :empresaId";
		}
		
		TypedQuery<T> query = entityManager.createQuery(jpql, domainClass);

		if (multiEmpresa) {
			query.setParameter("empresaId", empresaId);
		}
		
		return query.getResultList();
	}

	@Override
	public boolean existsById(ID id, Long empresaId) {
		
		String jpql = "select 1 from " + domainClass.getSimpleName() + " e where e.id = :id";
		
		if (multiEmpresa) {
			jpql += " and e.empresa.id = :empresaId";
		}
		
		TypedQuery<Integer> query = entityManager.createQuery(jpql, Integer.class);
		query.setParameter("id", id);
		
		 if (multiEmpresa) {
		        query.setParameter("empresaId", empresaId);
		 }
		 
		 query.setMaxResults(1);
		
		return !query.getResultList().isEmpty();
	}

	@Override
	public List<T> buscarPorIds(Iterable<ID> ids, Long empresaId) {
		
		if(IterableUtils.isEmpty(ids)) {
			return Collections.emptyList();
		}
		
		String jpql = "from " + domainClass.getSimpleName() + " where id in :ids";
		
		if (multiEmpresa) {
			jpql += " and empresa.id = :empresaId";
		}
		
		TypedQuery<T> query = entityManager.createQuery(jpql, domainClass);
		query.setParameter("ids", ids);

		if (multiEmpresa) {
			query.setParameter("empresaId", empresaId);
		}
		
		return query.getResultList();
	}

	@Override
	public void deletarAllById(Iterable<ID> ids, Long empresaId) {
		
		String jpql = "delete from " + domainClass.getSimpleName() + " where id in :ids";
		
		if (multiEmpresa) {
			jpql += " and empresa.id = :empresaId";
		}

		Query query = entityManager.createQuery(jpql);
		query.setParameter("ids", ids);

		if (multiEmpresa) {
			query.setParameter("empresaId", empresaId);
		}

		 query.executeUpdate();
	}

	@Override
	public long deleteAll(Long empresaID) {

		String jpql = "delete from " + domainClass.getSimpleName();
		if (multiEmpresa) {
			jpql += " where empresa.id = :empresaId";
		}

		Query query = entityManager.createQuery(jpql);

		if (multiEmpresa) {
			query.setParameter("empresaId", empresaID);
		}

		return query.executeUpdate();
	}
	
	
	@Override
	public List<T> findAll() {
		validar("findAll");
		return super.findAll();
	}
	
	@Override
	public List<T> findAll(Sort sort) {
		validar("findAll");
		return super.findAll(sort);
	}
	
	@Override
	public Page<T> findAll(Pageable pageable) {
		validar("findAll");
		return super.findAll(pageable);
	}
	
	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		validar("findAll");
		return super.findAll(example);
	}
	
	
	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		validar("findAll");
		return super.findAll(example, pageable);
	}

	@Override
	public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
		validar("findAll");
		return super.findAll(example, sort);
	}
	
	
	@Override
	public List<T> findAll(PredicateSpecification<T> spec) {
		validar("findAll");
		return super.findAll(spec);
	}
	
	
	@Override
	public List<T> findAll(Specification<T> spec) {
		validar("findAll");
		return super.findAll(spec);
	}

	@Override
	public Page<T> findAll(Specification<T> spec, Pageable pageable) {
		validar("findAll");
		return super.findAll(spec, pageable);
	}

	@Override
	public List<T> findAll(Specification<T> spec, Sort sort) {
		validar("findAll");
		return super.findAll(spec, sort);
	}

	@Override
	public Page<T> findAll(Specification<T> spec, Specification<T> countSpec, Pageable pageable) {
		validar("findAll");
		return super.findAll(spec, countSpec, pageable);
	}

	@Override
	public Optional<T> findById(ID id) {
		validar("findById");
		return super.findById(id);
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {
		validar("findAllById");
		return super.findAllById(ids);
	}
	
	
	@Override
	public T getReferenceById(ID id) {
		validar("getReferenceById");
		return super.getReferenceById(id);
	}
	
	@Override
	public boolean existsById(ID id) {
		validar("existsById");
		return super.existsById(id);
	}
	
	@Override
	public long count() {
		validar("count");
		return super.count();
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		validar("count");
		return super.count(example);
	}

	@Override
	public long count(PredicateSpecification<T> spec) {
		validar("count");
		return super.count(spec);
	}

	@Override
	public long count(Specification<T> spec) {
		validar("count");
		return super.count(spec);
	}
	
	
	@Override
	public void delete(T entity) {
		validar("delete");
		super.delete(entity);
	}

	@Override
	public void deleteAll() {
		validar("delete");
		super.deleteAll();
	}
	
	@Override
	public void deleteById(ID id) {
		validar("deleteById");
		super.deleteById(id);
	}
	
	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		validar("findOne");
		return super.findOne(example);
	}
	
	@Override
	public <S extends T> boolean exists(Example<S> example) {
		validar("exists");
		return super.exists(example);
	}
	
	@Override
	public void deleteAllInBatch() {
		validar("deleteAllInBatch");
		super.deleteAllInBatch();
	}
	
	@Override
	public <S extends T, R> R findBy(Example<S> example, Function<FetchableFluentQuery<S>, R> queryFunction) {
		validar("findBy");
		return super.findBy(example, queryFunction);
	}

	private void validar(String metodo) {
		
		if (multiEmpresa) {
			throw new UnsupportedOperationException("o método: " + metodo + " não pode ser usado. " + MSG_BLOQUEIO_QUERY);
		}
	}
	

}
