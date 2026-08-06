package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.MovimentacaoProduto;
import jakarta.transaction.Transactional;

@Repository
public interface MovimentacaoProdutoRepository extends JpaJdevRepository<MovimentacaoProduto, Long> {

    /*
     * Busca todas as movimentacoes de produtos da empresa passada como parametro
     */
    @Query("select m from MovimentacaoProduto m where m.empresa.id = :idEmpresa")
    List<MovimentacaoProduto> findAll(@Param("idEmpresa") Long idEmpresa);


    /*Busca as movimentacoes por partes ou nome do produto completo passado por parametro e da empresa passada por parametro*/
    @Query("select m from MovimentacaoProduto m where m.empresa.id = :idEmpresa "
                                + " and unaccent(upper(trim(m.produto.nome))) "
                                + " like unaccent(upper(concat('%', trim(:nome) ,'%')))" )
    List<MovimentacaoProduto> buscaPorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
    
    
    /*Retorna true se já existir movimentacao com o mesmo produto (nome) para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
    @Query("select count(m.id) > 0 from MovimentacaoProduto m where m.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(m.produto.nome))) "
            + " = unaccent(upper(trim(:nome)))")
    boolean existePorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
    
    /*Verifica se existe outra movimentacao no banco de dados com o mesmo produto (nome) mas ID diferentes da que está tentando atualizar*/
    @Query("select count(m.id) > 0 from MovimentacaoProduto m where m.empresa.id = :idEmpresa "
            + " and unaccent(upper(trim(m.produto.nome))) "
            + " = unaccent(upper(trim(:nome))) and m.id <> :id")
    boolean existePorNomeDiferenteId(@Param("id") Long id, @Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);    
    
    /*Delete de uma movimentacao de uma determinada empresa*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MovimentacaoProduto m where m.empresa.id = :idEmpresa and m.id = :id")
    void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);


    /*
     * Consultas que filtram também pelo id do pedido (pedido.id)
     */
    @Query("select m from MovimentacaoProduto m where m.empresa.id = :idEmpresa and m.pedido.id = :idPedido")
    List<MovimentacaoProduto> findAllByPedido(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

    /*Busca as movimentacoes de um pedido por partes ou nome do produto completo passado por parametro e da empresa passada por parametro*/
    @Query("select m from MovimentacaoProduto m where m.empresa.id = :idEmpresa and m.pedido.id = :idPedido "
                                + " and unaccent(upper(trim(m.produto.nome))) "
                                + " like unaccent(upper(concat('%', trim(:nome) ,'%')))" )
    List<MovimentacaoProduto> buscaPorNomeByPedido(@Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

    /*Retorna true se já existir movimentacao com o mesmo produto (nome) para a mesma empresa e pedido*/
    @Query("select count(m.id) > 0 from MovimentacaoProduto m where m.empresa.id = :idEmpresa and m.pedido.id = :idPedido "
            + " and unaccent(upper(trim(m.produto.nome))) "
            + " = unaccent(upper(trim(:nome)))")
    boolean existePorNomeByPedido(@Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

    /*Verifica se existe outra movimentacao no mesmo pedido com o mesmo produto (nome) mas ID diferentes da que está tentando atualizar*/
    @Query("select count(m.id) > 0 from MovimentacaoProduto m where m.empresa.id = :idEmpresa and m.pedido.id = :idPedido "
            + " and unaccent(upper(trim(m.produto.nome))) "
            + " = unaccent(upper(trim(:nome))) and m.id <> :id")
    boolean existePorNomeDiferenteIdByPedido(@Param("id") Long id, @Param("nome") String nome, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

    /*Delete de uma movimentacao de uma determinada empresa e pedido*/
    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from MovimentacaoProduto m where m.empresa.id = :idEmpresa and m.pedido.id = :idPedido and m.id = :id")
    void deleteByIdAndPedido(@Param("id") Long id, @Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

}
