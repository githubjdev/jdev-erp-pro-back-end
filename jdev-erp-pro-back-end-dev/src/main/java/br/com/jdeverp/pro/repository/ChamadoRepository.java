package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.Chamado;
import jakarta.transaction.Transactional;

@Repository
public interface ChamadoRepository extends JpaJdevRepository<Chamado, Long> {

    /*
     * Busca todos os chamados da empresa passada como parametro
     */
    @Query("select c from Chamado c where c.empresa.id = :idEmpresa")
    List<Chamado> findAll(@Param("idEmpresa") Long idEmpresa);


    /*Busca os chamados por partes ou titulo completo passado por parametro e da empresa passada por parametro*/
    @Query("select c from Chamado c where c.empresa.id = :idEmpresa "
                                + " and unaccent(upper(trim(c.titulo))) "
                                + " like unaccent(upper(concat('%', trim(:titulo) ,'%')))" )
    List<Chamado> buscaPorTitulo(@Param("titulo") String titulo, @Param("idEmpresa") Long idEmpresa);
    
    
    /*Retorna true se já existir chamado com o mesmo titulo para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
    @Query("select count(c.id) > 0 from Chamado c where c.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(c.titulo))) "
            + " = unaccent(upper(trim(:titulo)))")
    boolean existePorTitulo(@Param("titulo") String titulo, @Param("idEmpresa") Long idEmpresa);
    
    /*Verifica se existe outro chamado no banco de dados com o mesmo titulo mas ID diferentes da que está tentando atualizar*/
    @Query("select count(c.id) > 0 from Chamado c where c.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(c.titulo))) "
            + " = unaccent(upper(trim(:titulo))) and c.id <> :id")
    boolean existePorTituloDiferenteId(@Param("id") Long id, @Param("titulo") String titulo, @Param("idEmpresa") Long idEmpresa);    
    
    /*Delete de um chamado de uma determinada empresa*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Chamado c where c.empresa.id = :idEmpresa and c.id = :id")
    void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

}
