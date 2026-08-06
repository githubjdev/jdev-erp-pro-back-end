package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.Produto;
import jakarta.transaction.Transactional;

@Repository
public interface ProdutoRepository extends JpaJdevRepository<Produto, Long> {

	/*
	 * Busca todos os produtos da empresa passada como parametro
	 */
	@Query("select p from Produto p where p.empresa.id = :idEmpresa")
	List<Produto> findAll(@Param("idEmpresa") Long idEmpresa); 

	
	/*Busca os produtos por partes ou nome completo passado por parametro e da empresa passada por parametro*/
	@Query("select p from Produto p where p.empresa.id = :idEmpresa "
								+ " and unaccent(upper(trim(p.nome))) "
								+ " like unaccent(upper(concat('%', trim(:nome) ,'%')))")
	List<Produto> buscaPorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
	
	
	/*Retorna true se já existir produto com o mesmo nome para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
	@Query("select count(p.id) > 0 from Produto p where p.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(p.nome))) "
			+ " = unaccent(upper(trim(:nome)))")
	boolean existePorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
	
	/*Verifica se existe outro produto no banco de dados com o mesmo nome mas ID diferentes da que está tentando atualizar*/
	@Query("select count(p.id) > 0 from Produto p where p.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(p.nome))) "
			+ " = unaccent(upper(trim(:nome))) and p.id <> :id")
    boolean existePorNomeDiferenteId(@Param("id") Long id, @Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);	
	
	/*Delete de um produto de uma determinada empresa*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Produto p where p.empresa.id = :idEmpresa and p.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

}
