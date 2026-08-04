package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.Categoria;
import jakarta.transaction.Transactional;

@Repository
public interface CategoriaRepository extends JpaJdevRepository<Categoria, Long> {

	/*
	 * Busca todas as categorias da empresa passada como parametro
	 */
	@Query("select c from Categoria c where c.empresa.id = :idEmpresa")
	List<Categoria> findAll(@Param("idEmpresa") Long idEmpresa); 

	
	/*Busca as categroias por partes ou nome completo passado por parametro e da empresa passada por parametro*/
	@Query("select c from Categoria c where c.empresa.id = :idEmpresa "
								+ " and unaccent(upper(trim(c.nome))) "
								+ " like unaccent(upper(concat('%', trim(:nome) ,'%')))")
	List<Categoria> buscaPorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
	
	
	/*Retorna true se já existir categoria com o mesmo nome para a mesma empresa, no caso não podemso deixar salvar para não ficar repetido no banco de dados*/
	@Query("select count(c.id) > 0 from Categoria c where c.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(c.nome))) "
			+ " = unaccent(upper(trim(:nome)))")
	boolean existePorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
	
	/*Verifica se existe outra categoria no banco de dados com o mesmo nome mas ID diferentes da que está tentando atualizar*/
	@Query("select count(c.id) > 0 from Categoria c where c.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(c.nome))) "
			+ " = unaccent(upper(trim(:nome))) and c.id <> :id")
    boolean existePorNomeDiferenteId(@Param("id") Long id, @Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);	
	
	/*Delete de um categoria de uma determinada empresa*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Categoria c where c.empresa.id = :idEmpresa and c.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

}
