package br.com.jdeverp.pro.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.ItemPedido;
import jakarta.transaction.Transactional;

@Repository
public interface ItemPedidoRepository extends JpaJdevRepository<ItemPedido, Long> {

	/*
	 * Busca todos os itens de pedido da empresa passada como parâmetro
	 */
	@Query("select ip from ItemPedido ip where ip.empresa.id = :idEmpresa")
	List<ItemPedido> findAll(@Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca todos os itens de um pedido específico da empresa passada por parâmetro
	 */
	@Query("select ip from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.pedido.id = :idPedido")
	List<ItemPedido> buscarPorPedido(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca itens de pedido por produto da empresa passada por parâmetro
	 */
	@Query("select ip from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.produto.id = :idProduto")
	List<ItemPedido> buscarPorProduto(@Param("idProduto") Long idProduto, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca um item de pedido específico pelo pedido e produto da empresa
	 */
	@Query("select ip from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.pedido.id = :idPedido and ip.produto.id = :idProduto")
	Optional<ItemPedido> buscarPorPedidoEProduto(@Param("idPedido") Long idPedido, @Param("idProduto") Long idProduto, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Conta total de itens de um pedido específico
	 */
	@Query("select count(ip.id) from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.pedido.id = :idPedido")
	long contarItensPorPedido(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Soma o total geral de todos os itens de um pedido específico
	 */
	@Query("select coalesce(sum(ip.total), 0) from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.pedido.id = :idPedido")
	java.math.BigDecimal somarTotalPorPedido(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Soma o subtotal de todos os itens de um pedido específico
	 */
	@Query("select coalesce(sum(ip.subTotal), 0) from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.pedido.id = :idPedido")
	java.math.BigDecimal somarSubTotalPorPedido(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Soma o desconto de todos os itens de um pedido específico
	 */
	@Query("select coalesce(sum(ip.desconto), 0) from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.pedido.id = :idPedido")
	java.math.BigDecimal somarDescontoPorPedido(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Verifica se existe um item do produto no pedido
	 */
	@Query("select count(ip.id) > 0 from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.pedido.id = :idPedido and ip.produto.id = :idProduto")
	boolean existeItemNoPedido(@Param("idPedido") Long idPedido, @Param("idProduto") Long idProduto, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Conta quantas vezes um produto foi vendido na empresa
	 */
	@Query("select count(ip.id) from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.produto.id = :idProduto")
	long contarVendasPorProduto(@Param("idProduto") Long idProduto, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Busca quantidade total vendida de um produto
	 */
	@Query("select coalesce(sum(ip.quantidade), 0) from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.produto.id = :idProduto")
	Double somarQuantidadePorProduto(@Param("idProduto") Long idProduto, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Delete de um item de pedido de uma determinada empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

	/*
	 * Delete de todos os itens de um pedido específico da empresa
	 */
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from ItemPedido ip where ip.empresa.id = :idEmpresa and ip.pedido.id = :idPedido")
	void deletarItensPorPedido(@Param("idPedido") Long idPedido, @Param("idEmpresa") Long idEmpresa);

}
