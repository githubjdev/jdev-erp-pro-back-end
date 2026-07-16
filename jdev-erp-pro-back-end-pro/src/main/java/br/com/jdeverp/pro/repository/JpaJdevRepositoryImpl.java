package br.com.jdeverp.pro.repository;

import java.io.Serializable;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import jakarta.persistence.EntityManager;

public class JpaJdevRepositoryImpl<T, ID extends Serializable> extends SimpleJpaRepository<T, ID>
		implements JpaJdevRepository<T, ID> {

	private final Class<T> domainClass; /* Classe model ou entidade */
	private final EntityManager entityManager; /* É o nucleo da persistencia do JPA */

	/**
	 * Cria uma nova instância do repositório genérico utilizando apenas a classe
	 * da entidade e o {@link EntityManager}.
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
	 * @param domainClass Classe da entidade JPA que será manipulada pelo
	 *                    repositório. É utilizada para identificar o tipo da
	 *                    entidade durante operações genéricas.
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
	 *     <li>Classe da entidade.</li>
	 *     <li>Tipo da chave primária.</li>
	 *     <li>Campo anotado com {@code @Id}.</li>
	 *     <li>Estratégia de geração do identificador.</li>
	 *     <li>Metadados utilizados internamente pelo Spring Data JPA.</li>
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
	 * @param entityManager Gerenciador de persistência responsável pelas operações
	 *                      de acesso ao banco de dados.
	 */
	public JpaJdevRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {

	    super(entityInformation, entityManager);
	    this.domainClass = entityInformation.getJavaType();
	    this.entityManager = entityManager;
	}

}
