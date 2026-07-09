package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.model.Chamado;

@Repository
public interface ChamadoRepository extends JpaJdevRepository<Chamado, Long> {

	/*
	 * Busca todas as chamados da empresa passada como parametro
	 * */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa")
	List<Chamado> findAll(@Param("idEmpresa") Long idEmpresa);
	
	
	/*Busca as chamados por partes ou titulo completo passdo por parametro e da empresa passada por parametro*/
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa "
								+ " and upper(unaccent(trim(c.titulo))) "
								+ " like upper(concat('%', unaccent(trim(:titulo)), '%')) ")
	List<Chamado> buscaPorNome(@Param("idEmpresa") Long idEmpresa, @Param("titulo") String titulo);
	
	
	
	/*Retorna true se já existir chamdo com o mesmo titulo para a mesma empresa, no caso não podemso deixar salvar para não ficar repetido no banco de dados*/
	@Query("select count(c) > 0 from Chamado c where c.empresa.id = :idEmpresa "
			+ " and upper(unaccent(trim(c.titulo))) "
			+ " = upper(concat('%', unaccent(trim(:titulo)), '%')) ")
    boolean existePorNome(@Param("idEmpresa") Long idEmpresa, @Param("titulo") String titulo);
	
	
	/*Verifica se existe outra chamndo no banco de dados com o mesmo titulo mas ID diferentes da que está tentando atualizar*/
	@Query("select count(c) > 0 from Chamado c where c.empresa.id = :idEmpresa "
			+ " and upper(unaccent(trim(c.titulo))) "
			+ " = upper(concat('%', unaccent(trim(:titulo)), '%')) and c.id <> = :id")
    boolean existePorNomeDiferenteId(@Param("idEmpresa") Long idEmpresa,
    		                              @Param("titulo") String titulo,
    		                              @Param("id") Long id);
	
	
	/*Delete de um categoria de uma determinada empresa*/
	@Transactional
	@Modifying
	@Query("delete from Chamado c where c.empresa.id = :idEmpresa and c.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);
	
	
	
}
