package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.enums.PrioridadeChamado;
import br.com.jdeverp.pro.enums.StatusChamado;
import br.com.jdeverp.pro.enums.TipoChamado;
import br.com.jdeverp.pro.model.Chamado;
import jakarta.transaction.Transactional;

@Repository
public interface ChamadoRepository extends JpaJdevRepository<Chamado, Long> {

	/*
	 * Busca todos os chamados da empresa passada como parâmetro
	 */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa")
	List<Chamado> findAll(@Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca chamados por título ou parte do título da empresa passada por parâmetro
	 */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(c.titulo))) "
			+ " like unaccent(upper(concat('%', trim(:titulo) ,'%')))")
	List<Chamado> buscaPorTitulo(@Param("titulo") String titulo, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca chamados por status da empresa passada por parâmetro
	 */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa and c.statusChamado = :statusChamado")
	List<Chamado> buscaPorStatus(@Param("statusChamado") StatusChamado statusChamado, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca chamados por prioridade da empresa passada por parâmetro
	 */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa and c.prioridadeChamado = :prioridadeChamado")
	List<Chamado> buscaPorPrioridade(@Param("prioridadeChamado") PrioridadeChamado prioridadeChamado, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca chamados por tipo da empresa passada por parâmetro
	 */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa and c.tipoChamado = :tipoChamado")
	List<Chamado> buscaPorTipo(@Param("tipoChamado") TipoChamado tipoChamado, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca chamados por status e prioridade da empresa passada por parâmetro
	 */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa and c.statusChamado = :statusChamado and c.prioridadeChamado = :prioridadeChamado")
	List<Chamado> buscaPorStatusEPrioridade(@Param("statusChamado") StatusChamado statusChamado, @Param("prioridadeChamado") PrioridadeChamado prioridadeChamado, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca chamados abertos por um usuário específico na empresa
	 */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa and c.abertoUser.id = :idUsuario")
	List<Chamado> buscaPorAbertoPor(@Param("idUsuario") Long idUsuario, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca chamados atribuídos a um atendente específico na empresa
	 */
	@Query("select c from Chamado c where c.empresa.id = :idEmpresa and c.atendente.id = :idAtendente")
	List<Chamado> buscaPorAtendente(@Param("idAtendente") Long idAtendente, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Delete de um chamado de uma determinada empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Chamado c where c.empresa.id = :idEmpresa and c.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

}
