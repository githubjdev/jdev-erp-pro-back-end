package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.Produto;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

@Repository
public interface ProdutoRepository extends JpaJdevRepository<Produto, Long> {

	/*
	 * Busca todos os produtos da empresa passada como parametro
	 */
	@Query("select p from Produto p where p.empresa.id = :idEmpresa")
	List<Produto> findAll(@Param("idEmpresa") Long idEmpresa); 

	
	/*Produtos da empresa travados para escrita (baixa/estorno de estoque sem venda dupla). Ordenado por id para evitar deadlock.*/
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Produto p where p.empresa.id = :idEmpresa and p.id in :ids order by p.id")
	List<Produto> buscarPorIdsComLock(@Param("ids") java.util.Collection<Long> ids, @Param("idEmpresa") Long idEmpresa);

	/*Produtos (entre os ids) que já têm item de pedido ou movimentação de estoque: não podem ser excluídos em lote*/
	@Query("select p from Produto p where p.empresa.id = :idEmpresa and p.id in :ids "
			+ " and (exists (select 1 from ItemPedido i where i.empresa.id = :idEmpresa and i.produto.id = p.id) "
			+ " or exists (select 1 from MovimentacaoProduto m where m.empresa.id = :idEmpresa and m.produto.id = p.id)) "
			+ " order by p.nome")
	List<Produto> emUsoPorIds(@Param("ids") java.util.Collection<Long> ids, @Param("idEmpresa") Long idEmpresa);

	/*Ids dos produtos da empresa sem item de pedido nem movimentação: são os únicos que a exclusão em lote apaga*/
	@Query("select p.id from Produto p where p.empresa.id = :idEmpresa "
			+ " and not exists (select 1 from ItemPedido i where i.empresa.id = :idEmpresa and i.produto.id = p.id) "
			+ " and not exists (select 1 from MovimentacaoProduto m where m.empresa.id = :idEmpresa and m.produto.id = p.id)")
	List<Long> idsSemUso(@Param("idEmpresa") Long idEmpresa);

	/*Produtos com estoque igual ou abaixo do mínimo (alerta de estoque baixo)*/
	@Query("select p from Produto p where p.empresa.id = :idEmpresa and p.estoque <= p.estoqueMinimo order by p.nome")
	List<Produto> estoqueBaixo(@Param("idEmpresa") Long idEmpresa);

	/*Busca os produtos por partes ou nome completo passado por parametro e da empresa passada por parametro*/
	@Query("select p from Produto p where p.empresa.id = :idEmpresa "
								+ " and unaccent(upper(trim(p.nome))) "
								+ " like unaccent(upper(concat('%', trim(:nome) ,'%')))")
	List<Produto> buscaPorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
	
	
	/*Retorna true se já existir produto com o mesmo nome para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
	@Query("select count(p.id) > 0 from Produto p where p.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(p.nome))) "
			+ " = unaccent(upper(trim(:nome)))")
	boolean existePorNome(@Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);
	
	/*Verifica se existe outro produto no banco de dados com o mesmo nome mas ID diferentes da que está tentando atualizar*/
	@Query("select count(p.id) > 0 from Produto p where p.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(p.nome))) "
			+ " = unaccent(upper(trim(:nome))) and p.id <> :id")
    boolean existePorNomeDiferenteId(@Param("id") Long id, @Param("nome") String nome, @Param("idEmpresa") Long idEmpresa);	
	
	/*Delete de um produto de uma determinada empresa*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Produto p where p.empresa.id = :idEmpresa and p.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

}
