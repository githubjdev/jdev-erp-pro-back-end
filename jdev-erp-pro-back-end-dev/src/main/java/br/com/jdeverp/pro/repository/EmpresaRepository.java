package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.anotacoes.IgnoreEmpresaId;
import br.com.jdeverp.pro.model.Empresa;
import jakarta.transaction.Transactional;

@IgnoreEmpresaId
@Repository
public interface EmpresaRepository extends JpaJdevRepository<Empresa, Long> {
	
	@Query("select c from Empresa c ")
	List<Empresa> findAll();


	/*Busca as empresas por partes ou nome (pessoa.nome) completo passado por parametro*/
	@Query("select e from Empresa e where unaccent(upper(trim(e.pessoa.nome))) "
			+ " like unaccent(upper(concat('%', trim(:nome) ,'%')))" )
	List<Empresa> buscaPorNome(@Param("nome") String nome);

	/*Retorna true se já existir empresa com o mesmo nome (pessoa.nome), para evitar duplicidade*/
	@Query("select count(e.id) > 0 from Empresa e "
			+ " where unaccent(upper(trim(e.pessoa.nome))) "
			+ " = unaccent(upper(trim(:nome)))")
	boolean existePorNome(@Param("nome") String nome);

	/*Verifica se existe outra empresa no banco de dados com o mesmo nome (pessoa.nome) mas ID diferentes da que está tentando atualizar*/
	@Query("select count(e.id) > 0 from Empresa e "
			+ " where unaccent(upper(trim(e.pessoa.nome))) "
			+ " = unaccent(upper(trim(:nome))) and e.id <> :id")
	boolean existePorNomeDiferenteId(@Param("id") Long id, @Param("nome") String nome);

	/*Delete de uma empresa (sem filtro por empresa, pois esta entidade representa a própria empresa)*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Empresa e where e.id = :id")
	void deleteById(@Param("id") Long id);

}
