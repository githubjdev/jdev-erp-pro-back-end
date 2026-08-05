package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.ClienteFuncionario;
import jakarta.transaction.Transactional;

@Repository
public interface ClienteFuncionarioRepository extends JpaJdevRepository<ClienteFuncionario, Long> {

    /*
     * Busca todas as relações cliente/funcionario da empresa passada como parametro
     */
    @Query("select c from ClienteFuncionario c where c.empresa.id = :idEmpresa")
    List<ClienteFuncionario> findAll(@Param("idEmpresa") Long idEmpresa);


    /*Busca por partes ou nome completo da pessoa relacionada e da empresa passada por parametro*/
    @Query("select c from ClienteFuncionario c where c.empresa.id = :idEmpresa "
                                + " and unaccent(upper(trim(c.pessoa.nome))) "
                                + " like unaccent(upper(concat('%', trim(:nome) ,'%')))" )
    List<ClienteFuncionario> buscaPorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
    
    
    /*Retorna true se já existir relacionamento com a mesma pessoa para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
    @Query("select count(c.id) > 0 from ClienteFuncionario c where c.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(c.pessoa.nome))) "
            + " = unaccent(upper(trim(:nome)))")
    boolean existePorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
    
    /*Verifica se existe outro registro no banco de dados com a mesma pessoa (nome) mas ID diferentes da que está tentando atualizar*/
    @Query("select count(c.id) > 0 from ClienteFuncionario c where c.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(c.pessoa.nome))) "
            + " = unaccent(upper(trim(:nome))) and c.id <> :id")
    boolean existePorNomeDiferenteId(@Param("id") Long id, @Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);    
    
    /*Delete de um relacionamento cliente/funcionario de uma determinada empresa*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ClienteFuncionario c where c.empresa.id = :idEmpresa and c.id = :id")
    void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

}
