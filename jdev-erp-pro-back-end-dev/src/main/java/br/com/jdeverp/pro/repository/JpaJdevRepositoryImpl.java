package br.com.jdeverp.pro.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class JpaJdevRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
		implements JpaJdevRepository<T, ID> {

	private final Class<T> domainClass; /* Classe model ou entidade */
	private final EntityManager entityManager; /* É o nucleo da persistencia do JPA */

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
	}

	@Override
	public Page<T> listarPaginado(Long empresaId, Pageable pageable) {
		
		boolean possuiEmpresa = possuiEmpresa();
		
		String jpql = "from " + domainClass.getSimpleName();
		
		if (possuiEmpresa) {
			jpql += " where empresa.id = :empresaId";
		}
		
		if (pageable.getSort().isSorted()) {
			jpql += " order by ";
			
			 List<String> orders = new ArrayList<String>();
			 
			  for (Sort.Order order : pageable.getSort()) {
		            orders.add(order.getProperty() + " " + order.getDirection().name());
		        }

			  jpql += String.join(", ", orders);
		}
		
		TypedQuery<T> query = entityManager.createQuery(jpql, domainClass);
		
		if(possuiEmpresa) {
			query.setParameter("empresaId", empresaId);
		}
		
		List<T> lista = query.setFirstResult((int)pageable.getOffset())
				             .setMaxResults(pageable.getPageSize())
				             .getResultList();

		return new PageImpl<T>(lista, pageable, total(empresaId));
	}

	@Override
	public long total(Long empresaId) {
		
		String entidade = domainClass.getSimpleName();
		boolean possuiEmpresa = possuiEmpresa();

		String jpql = "select count(*) from " + entidade;

		if (possuiEmpresa) {
			jpql += " where empresa.id = :empresaId";
		}

		TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);

		if (possuiEmpresa) {
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
	public void deleteAll() {
		validar("deleteAll", "deleteByEmpresa(empresaId)");
		super.deleteAll();
	}
	
	@Override
	public List<T> findAll() {
		validar("findAll", "findAll(empresaId)");
		return super.findAll();
	}
	
	@Override
	public long count() {
		validar("findAll", "total(empresaId)");
		return super.count();
	}
	
	
	@Override
	public <S extends T> long count(Example<S> example) {
		validar("findAll", "total(empresaId)");
		return super.count(example);
	}
	
	@Override
	public void delete(T entity) {
		validar("delete", "delete(id, empresaId)");
		super.delete(entity);
	}
	
	@Override
	public void deleteAllInBatch() {
		validar("deleteAllInBatch", "deleteAllInBatch(empresaId)");
		super.deleteAllInBatch();
	}
	
	@Override
	public <S extends T> boolean exists(Example<S> example) {
		validar("exists", "exists(empresaId)");
		return super.exists(example);
	}
	
	
	@Override
	public <S extends T> List<S> findAll(Example<S> example) {
		validar("findAll", "findAll(empresaId)");
		return super.findAll(example);
	}
	
	
	
	@Override
	public void deleteById(ID id) {
		validar("deleteById", "deleteById(id, empresaId)");
		super.deleteById(id);
	}
	
	
	
	/**
	 * Bloqueia métodos perigosos somente para entidades multiempresa.
	 */
	private void validar(String metodo, String metodoCorreto) {
         if (possuiEmpresa()) {
        	      throw new UnsupportedOperationException("o Método %s() não pode ser utilizado. Utilize o %s -> Motivo: A entidade %s possui relacionamento com empresa".formatted(metodo, metodoCorreto, domainClass.getSimpleName()));
         }
	}

}
