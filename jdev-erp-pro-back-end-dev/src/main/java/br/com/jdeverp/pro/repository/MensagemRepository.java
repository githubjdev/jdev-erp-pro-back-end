package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.Mensagem;
import jakarta.transaction.Transactional;

@Repository
public interface MensagemRepository extends JpaJdevRepository<Mensagem, Long> {

    /*
     * Busca todas as mensagens da empresa passada como parametro
     */
    @Query("select m from Mensagem m where m.empresa.id = :idEmpresa")
    List<Mensagem> findAll(@Param("idEmpresa") Long idEmpresa);


    /*Busca as mensagens por partes ou conteudo completo passado por parametro e da empresa passada por parametro*/
    @Query("select m from Mensagem m where m.empresa.id = :idEmpresa "
                                + " and unaccent(upper(trim(m.conteudo))) "
                                + " like unaccent(upper(concat('%', trim(:conteudo) ,'%')))" )
    List<Mensagem> buscaPorConteudo(@Param("conteudo") String conteudo, @Param("idEmpresa") Long idEmpresa);
    
    
    /*Retorna true se já existir mensagem com o mesmo conteudo para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
    @Query("select count(m.id) > 0 from Mensagem m where m.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(m.conteudo))) "
            + " = unaccent(upper(trim(:conteudo)))")
    boolean existePorConteudo(@Param("conteudo") String conteudo, @Param("idEmpresa") Long idEmpresa);
    
    /*Verifica se existe outra mensagem no banco de dados com o mesmo conteudo mas ID diferentes da que está tentando atualizar*/
    @Query("select count(m.id) > 0 from Mensagem m where m.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(m.conteudo))) "
            + " = unaccent(upper(trim(:conteudo))) and m.id <> :id")
    boolean existePorConteudoDiferenteId(@Param("id") Long id, @Param("conteudo") String conteudo, @Param("idEmpresa") Long idEmpresa);    
    
    /*Delete de uma mensagem de uma determinada empresa*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Mensagem m where m.empresa.id = :idEmpresa and m.id = :id")
    void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);


    /*
     * Consultas que filtram também pelo id do chamado (chamado.id)
     */
    @Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado")
    List<Mensagem> findAllByChamado(@Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

    /*Busca as mensagens de um chamado por partes ou conteudo completo passado por parametro e da empresa passada por parametro*/
    @Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado "
                                + " and unaccent(upper(trim(m.conteudo))) "
                                + " like unaccent(upper(concat('%', trim(:conteudo) ,'%')))" )
    List<Mensagem> buscaPorConteudoByChamado(@Param("conteudo") String conteudo, @Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

    /*Retorna true se já existir mensagem com o mesmo conteudo para a mesma empresa e chamado*/
    @Query("select count(m.id) > 0 from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado "
            + " and unaccent(upper(trim(m.conteudo))) "
            + " = unaccent(upper(trim(:conteudo)))")
    boolean existePorConteudoByChamado(@Param("conteudo") String conteudo, @Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

    /*Verifica se existe outra mensagem no mesmo chamado com o mesmo conteudo mas ID diferentes da que está tentando atualizar*/
    @Query("select count(m.id) > 0 from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado "
            + " and unaccent(upper(trim(m.conteudo))) "
            + " = unaccent(upper(trim(:conteudo))) and m.id <> :id")
    boolean existePorConteudoDiferenteIdByChamado(@Param("id") Long id, @Param("conteudo") String conteudo, @Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

    /*Delete de uma mensagem de uma determinada empresa e chamado*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado and m.id = :id")
    void deleteByIdAndChamado(@Param("id") Long id, @Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

}
