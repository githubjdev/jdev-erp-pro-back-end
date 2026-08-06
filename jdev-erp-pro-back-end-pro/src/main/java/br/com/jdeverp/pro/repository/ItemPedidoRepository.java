package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.ItemPedido;
import jakarta.transaction.Transactional;

@Repository
public interface ItemPedidoRepository extends JpaJdevRepository<ItemPedido, Long> {

    /*
     * Busca todos os itens de pedido da empresa passada como parametro
     */
    @Query("select c from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido")
    List<ItemPedido> findAll(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa); 


    /*Busca os itens por partes ou nome do produto completo passado por parametro e da empresa passada por parametro*/
    @Query("select c from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido "
                                + " and unaccent(upper(trim(c.produto.nome))) "
                                + " like unaccent(upper(concat('%', trim(:nome) ,'%')))" )
    List<ItemPedido> buscaPorNome(@Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);
    
    
    /*Retorna true se já existir item com o mesmo produto (nome) para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
    @Query("select count(c.id) > 0 from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido "
            + " and unaccent(upper(trim(c.produto.nome))) "
            + " = unaccent(upper(trim(:nome)))")
    boolean existePorNome(@Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);
    
    /*Verifica se existe outro item no banco de dados com o mesmo produto (nome) mas ID diferentes da que está tentando atualizar*/
    @Query("select count(c.id) > 0 from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido "
            + " and unaccent(upper(trim(c.produto.nome))) "
            + " = unaccent(upper(trim(:nome))) and c.id <> :id")
    boolean existePorNomeDiferenteId(@Param("id") Long id, @Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);    
    
    /*Delete de um item de pedido de uma determinada empresa*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido and c.id = :id")
    void deleteById(@Param("id") Long id, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);


    /*
     * Consultas que filtram também pelo id do pedido (pedido.id)
     */
    @Query("select c from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido")
    List<ItemPedido> findAllByPedido(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

    /*Busca os itens de um pedido por partes ou nome do produto completo passado por parametro e da empresa passada por parametro*/
    @Query("select c from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido "
                                + " and unaccent(upper(trim(c.produto.nome))) "
                                + " like unaccent(upper(concat('%', trim(:nome) ,'%')))" )
    List<ItemPedido> buscaPorNomePorPedido(@Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

    /*Retorna true se já existir item com o mesmo produto (nome) para a mesma empresa e pedido*/
    @Query("select count(c.id) > 0 from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido "
            + " and unaccent(upper(trim(c.produto.nome))) "
            + " = unaccent(upper(trim(:nome)))")
    boolean existePorNomePorPedido(@Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

    /*Verifica se existe outro item no mesmo pedido com o mesmo produto (nome) mas ID diferentes da que está tentando atualizar*/
    @Query("select count(c.id) > 0 from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido "
            + " and unaccent(upper(trim(c.produto.nome))) "
            + " = unaccent(upper(trim(:nome))) and c.id <> :id")
    boolean existePorNomeDiferenteIdPorPedido(@Param("id") Long id, @Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

    /*Delete de um item de pedido de uma determinada empresa e pedido*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ItemPedido c where c.empresa.id = :idEmpresa and c.pedido.id = :idPedido and c.id = :id")
    void deleteByIdAndPedido(@Param("id") Long id, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

}
