package br.com.jdeverp.pro.repository;

import java.util.List;
import java.util.Optional;

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
	
	/*
	 * Busca todas as empresas
	 */
	@Query("select c from Empresa c ")
	List<Empresa> findAll();

	/*
	 * Busca uma empresa pelo ID da pessoa associada
	 */
	@Query("select e from Empresa e where e.pessoa.id = :idPessoa")
	Optional<Empresa> buscaPorPessoa(@Param("idPessoa") Long idPessoa);

	/*
	 * Busca empresas pelo ID do plano
	 */
	@Query("select e from Empresa e where e.plano.id = :idPlano")
	List<Empresa> buscaPorPlano(@Param("idPlano") Long idPlano);

	/*
	 * Busca empresas ativas (plano ativo = true)
	 */
	@Query("select e from Empresa e where e.planoAtivo = true")
	List<Empresa> buscarEmpresasAtivas();

	/*
	 * Busca empresas inativas (plano ativo = false)
	 */
	@Query("select e from Empresa e where e.planoAtivo = false")
	List<Empresa> buscarEmpresasInativas();

	/*
	 * Busca empresas bloqueadas
	 */
	@Query("select e from Empresa e where e.bloqueio = true")
	List<Empresa> buscarEmpresasBloqueadas();

	/*
	 * Busca empresas desbloqueadas
	 */
	@Query("select e from Empresa e where e.bloqueio = false")
	List<Empresa> buscarEmpresasDesbloqueadas();

	/*
	 * Verifica se existe empresa com a mesma pessoa associada
	 */
	@Query("select count(e.id) > 0 from Empresa e where e.pessoa.id = :idPessoa")
	boolean existePorPessoa(@Param("idPessoa") Long idPessoa);

	/*
	 * Conta total de empresas ativas
	 */
	@Query("select count(e.id) from Empresa e where e.planoAtivo = true")
	long contarEmpresasAtivas();

	/*
	 * Conta total de empresas inativas
	 */
	@Query("select count(e.id) from Empresa e where e.planoAtivo = false")
	long contarEmpresasInativas();

	/*
	 * Conta total de empresas bloqueadas
	 */
	@Query("select count(e.id) from Empresa e where e.bloqueio = true")
	long contarEmpresasBloqueadas();

	/*
	 * Busca empresas que estão próximas do vencimento de plano (próximos 30 dias)
	 */
	@Query("select e from Empresa e where e.vigenciaPlano <= CURRENT_DATE + 30 and e.vigenciaPlano >= CURRENT_DATE")
	List<Empresa> buscarEmpresasProximasVencimento();

	/*
	 * Busca empresas com plano vencido
	 */
	@Query("select e from Empresa e where e.vigenciaPlano < CURRENT_DATE")
	List<Empresa> buscarEmpresasComPlanoVencido();

	/*
	 * Update para ativar plano de uma empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Empresa e set e.planoAtivo = true where e.id = :id")
	void ativarPlano(@Param("id") Long id);

	/*
	 * Update para desativar plano de uma empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Empresa e set e.planoAtivo = false where e.id = :id")
	void desativarPlano(@Param("id") Long id);

	/*
	 * Update para bloquear uma empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Empresa e set e.bloqueio = true where e.id = :id")
	void bloquearEmpresa(@Param("id") Long id);

	/*
	 * Update para desbloquear uma empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Empresa e set e.bloqueio = false where e.id = :id")
	void desbloquearEmpresa(@Param("id") Long id);

	/*
	 * Delete de uma empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Empresa e where e.id = :id")
	void deleteById(@Param("id") Long id);

}
