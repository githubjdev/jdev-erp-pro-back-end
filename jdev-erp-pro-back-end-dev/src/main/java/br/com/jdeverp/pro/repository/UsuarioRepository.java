package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.anotacoes.IgnoreEmpresaId;
import br.com.jdeverp.pro.model.Usuario;
import jakarta.transaction.Transactional;

@Repository
public interface UsuarioRepository extends JpaJdevRepository<Usuario, Long> {

	@IgnoreEmpresaId(ignorar = true, motivo = "Usado pra login então a empresa é estabelecida depois do login do usuário")
	@Query("select u from Usuario u where u.login = :login")
	Usuario buscaPorLogin(@Param("login") String login);

	/*
	 * Busca todos os usuários da empresa passada como parametro
	 */
	@Query("select u from Usuario u where u.empresa.id = :idEmpresa")
	List<Usuario> findAll(@Param("idEmpresa") Long idEmpresa);

	
	/*Busca os usuários por partes ou nome completo da pessoa vinculada e da empresa passada por parametro*/
	@Query("select u from Usuario u where u.empresa.id = :idEmpresa "
								+ " and unaccent(upper(trim(u.clienteFuncionario.pessoa.nome))) "
								+ " like unaccent(upper(concat('%', trim(:nome) ,'%')))")
	List<Usuario> buscaPorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
	
	
	/*Retorna true se já existir usuário com o mesmo nome da pessoa para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
	@Query("select count(u.id) > 0 from Usuario u where u.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(u.login))) "
			+ " = unaccent(upper(trim(:login)))")
	boolean existePorLogin(@Param("login") String login, @Param("idEmpresa") Long idEmpresa);
	
	
	
	@Query("select count(u.id) > 0 from Usuario u where u.empresa.id = :idEmpresa "
			+ " and u.clienteFuncionario.pessoa.id =: idPessoa")
	boolean existePorPessoa(@Param("nome") Long idPessoa, @Param("idEmpresa") Long idEmpresa);
	
	
	@Query("select count(u.id) > 0 from Usuario u where u.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(u.clienteFuncionario.pessoa.nome))) "
			+ " = unaccent(upper(trim(:nome)))")
	boolean existePorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
	
	
	/*Verifica se existe outro usuário no banco de dados com o mesmo nome da pessoa mas ID diferentes da que está tentando atualizar*/
	@Query("select count(u.id) > 0 from Usuario u where u.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(u.clienteFuncionario.pessoa.nome))) "
			+ " = unaccent(upper(trim(:nome))) and u.id <> :id")
    boolean existePorNomeDiferenteId(@Param("id") Long id, @Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);	
	
	/*Verifica se existe outro usuário no banco de dados com o mesmo nome da pessoa mas ID diferentes da que está tentando atualizar*/
	@Query("select count(u.id) > 0 from Usuario u where u.empresa.id = :idEmpresa "
			+ " and u.clienteFuncionario.pessoa.id = :pessoaId and u.id <> :usuarioId")
    boolean existeOutroUsuarioComPessoa(@Param("pessoaId") Long pessoaId, @Param("usuarioId") Long usuarioId, @Param("idEmpresa") Long idEmpresa);	
	
	
	/*Delete de um usuário de uma determinada empresa*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Usuario u where u.empresa.id = :idEmpresa and u.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);
	
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Usuario set tokenSessao = :token where id = :id")
	void updateTokenSessaoLogin(@Param("id") Long id, @Param("token") String token);

}
