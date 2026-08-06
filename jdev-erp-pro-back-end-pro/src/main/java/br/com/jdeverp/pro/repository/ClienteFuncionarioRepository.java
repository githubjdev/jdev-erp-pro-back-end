package br.com.jdeverp.pro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.enums.TipoClienteFuncionario;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import jakarta.transaction.Transactional;

@Repository
public interface ClienteFuncionarioRepository extends JpaJdevRepository<ClienteFuncionario, Long> {

	/*
	 * Busca todos os clientes/funcionários da empresa passada como parâmetro
	 */
	@Query("select cf from ClienteFuncionario cf where cf.empresa.id = :idEmpresa")
	List<ClienteFuncionario> findAll(@Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca clientes/funcionários por tipo da empresa passada por parâmetro
	 */
	@Query("select cf from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.tipoClienteFuncionario = :tipo")
	List<ClienteFuncionario> buscaPorTipo(@Param("tipo") TipoClienteFuncionario tipo, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca um cliente/funcionário pelo ID do usuário da empresa passada por parâmetro
	 */
	@Query("select cf from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.usuario.id = :idUsuario")
	Optional<ClienteFuncionario> buscaPorUsuario(@Param("idUsuario") Long idUsuario, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca um cliente/funcionário pelo ID da pessoa da empresa passada por parâmetro
	 */
	@Query("select cf from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.pessoa.id = :idPessoa")
	Optional<ClienteFuncionario> buscaPorPessoa(@Param("idPessoa") Long idPessoa, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca clientes/funcionários por tipo e usuário da empresa passada por parâmetro
	 */
	@Query("select cf from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.tipoClienteFuncionario = :tipo and cf.usuario.id = :idUsuario")
	Optional<ClienteFuncionario> buscaPorTipoEUsuario(@Param("tipo") TipoClienteFuncionario tipo, @Param("idUsuario") Long idUsuario, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca clientes/funcionários por tipo e pessoa da empresa passada por parâmetro
	 */
	@Query("select cf from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.tipoClienteFuncionario = :tipo and cf.pessoa.id = :idPessoa")
	Optional<ClienteFuncionario> buscaPorTipoEPessoa(@Param("tipo") TipoClienteFuncionario tipo, @Param("idPessoa") Long idPessoa, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Verifica se existe um cliente/funcionário com o mesmo usuário da empresa passada por parâmetro
	 */
	@Query("select count(cf.id) > 0 from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.usuario.id = :idUsuario")
	boolean existePorUsuario(@Param("idUsuario") Long idUsuario, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Verifica se existe um cliente/funcionário com a mesma pessoa da empresa passada por parâmetro
	 */
	@Query("select count(cf.id) > 0 from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.pessoa.id = :idPessoa")
	boolean existePorPessoa(@Param("idPessoa") Long idPessoa, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Conta quantos clientes da empresa passada por parâmetro
	 */
	@Query("select count(cf.id) from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.tipoClienteFuncionario = 'CLIENTE'")
	long contarClientes(@Param("idEmpresa") Long idEmpresa);

	/*
	 * Conta quantos funcionários da empresa passada por parâmetro
	 */
	@Query("select count(cf.id) from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.tipoClienteFuncionario = 'FUNCIONARIO'")
	long contarFuncionarios(@Param("idEmpresa") Long idEmpresa);

	/*
	 * Delete de um cliente/funcionário de uma determinada empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from ClienteFuncionario cf where cf.empresa.id = :idEmpresa and cf.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

}
