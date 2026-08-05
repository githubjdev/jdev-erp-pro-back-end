package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.Pedido;
import jakarta.transaction.Transactional;

@Repository
public interface PedidoRepository extends JpaJdevRepository<Pedido, Long> {

    /*
     * Busca todos os pedidos da empresa passada como parametro
     */
    @Query("select p from Pedido p where p.empresa.id = :idEmpresa")
    List<Pedido> findAll(@Param("idEmpresa") Long idEmpresa);


    /*Busca os pedidos por partes ou numero do pedido completo passado por parametro e da empresa passada por parametro*/
    @Query("select p from Pedido p where p.empresa.id = :idEmpresa "
                                + " and unaccent(upper(trim(p.numeroPedido))) "
                                + " like unaccent(upper(concat('%', trim(:numeroPedido) ,'%')))" )
    List<Pedido> buscaPorNumeroPedido(@Param("numeroPedido") String numeroPedido, @Param("idEmpresa") Long idEmpresa);
    
    
    /*Retorna true se já existir pedido com o mesmo numero para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
    @Query("select count(p.id) > 0 from Pedido p where p.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(p.numeroPedido))) "
            + " = unaccent(upper(trim(:numeroPedido)))")
    boolean existePorNumeroPedido(@Param("numeroPedido") String numeroPedido, @Param("idEmpresa") Long idEmpresa);
    
    /*Verifica se existe outro pedido no banco de dados com o mesmo numero mas ID diferentes da que está tentando atualizar*/
    @Query("select count(p.id) > 0 from Pedido p where p.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(p.numeroPedido))) "
            + " = unaccent(upper(trim(:numeroPedido))) and p.id <> :id")
    boolean existePorNumeroPedidoDiferenteId(@Param("id") Long id, @Param("numeroPedido") String numeroPedido, @Param("idEmpresa") Long idEmpresa);    
    
    /*Delete de um pedido de uma determinada empresa*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Pedido p where p.empresa.id = :idEmpresa and p.id = :id")
    void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

}
