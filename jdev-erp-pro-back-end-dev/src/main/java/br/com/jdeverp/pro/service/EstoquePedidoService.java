package br.com.jdeverp.pro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.enums.TipoMovimentacaoProduto;
import br.com.jdeverp.pro.enums.TipoPedido;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.model.MovimentacaoProduto;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.repository.ItemPedidoRepository;
import br.com.jdeverp.pro.repository.MovimentacaoProdutoRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;

/**
 * Movimento de estoque dos itens de um pedido: VENDA e BRINDE saem, COMPRA entra; cancelar ou devolver desfaz.
 * Os produtos são lidos com lock pessimista (ordenados por id) para duas operações simultâneas não passarem do estoque.
 * Cada movimento gera uma MovimentacaoProduto ligada ao pedido.
 */
@Service
public class EstoquePedidoService {

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private ItemPedidoRepository itemPedidoRepository;

	@Autowired
	private MovimentacaoProdutoRepository movimentacaoProdutoRepository;

	/*Diferença de estoque a aplicar: entrada soma, saída subtrai, reserva/liberação não mexem*/
	public static double delta(TipoMovimentacaoProduto tipo, Double quantidade) {
		if (tipo == null || quantidade == null) {
			return 0;
		}
		if (tipo.isEntrada()) {
			return quantidade;
		}
		if (tipo.isSaida()) {
			return -quantidade;
		}
		return 0;
	}

	/*Tipo de movimento que o pedido gera ao ter o pagamento aprovado; null = o tipo de pedido não mexe no estoque*/
	public static TipoMovimentacaoProduto movimentoDoPedido(TipoPedido tipoPedido) {
		if (tipoPedido == null) {
			return null;
		}
		return switch (tipoPedido) {
			case VENDA -> TipoMovimentacaoProduto.SAIDA_VENDA;
			case BRINDE -> TipoMovimentacaoProduto.SAIDA_BRINDE;
			case COMPRA -> TipoMovimentacaoProduto.ENTRADA_COMPRA;
			default -> null;
		};
	}

	public static boolean movimentaEstoque(TipoPedido tipoPedido) {
		return movimentoDoPedido(tipoPedido) != null;
	}

	/*Movimento que desfaz o do pedido (cancelamento ou devolução)*/
	static TipoMovimentacaoProduto movimentoDeEstorno(TipoMovimentacaoProduto original) {
		return switch (original) {
			case SAIDA_VENDA -> TipoMovimentacaoProduto.ENTRADA_DEVOLUCAO_CLIENTE;
			case SAIDA_BRINDE -> TipoMovimentacaoProduto.ENTRADA_AJUSTE;
			case ENTRADA_COMPRA -> TipoMovimentacaoProduto.SAIDA_DEVOLUCAO_FORNECEDOR;
			default -> throw new MsgApiException("Tipo de movimento sem estorno definido: " + original);
		};
	}

	/*Aplica no estoque o movimento do pedido: venda e brinde saem, compra entra. Valida o estoque antes de gravar.*/
	@Transactional
	public void baixar(Pedido pedido) {
		TipoMovimentacaoProduto tipo = movimentoDoPedido(pedido.getTipoPedido());
		if (tipo == null) {
			return;
		}
		aplicar(pedido, tipo);
		pedido.setEstoqueBaixado(true);
	}

	/*Desfaz o movimento do pedido (só chamado se o estoque já foi movimentado)*/
	@Transactional
	public void estornar(Pedido pedido) {
		TipoMovimentacaoProduto original = movimentoDoPedido(pedido.getTipoPedido());
		if (original == null) {
			return;
		}
		aplicar(pedido, movimentoDeEstorno(original));
		pedido.setEstoqueBaixado(false);
	}

	private void aplicar(Pedido pedido, TipoMovimentacaoProduto tipo) {
		List<ItemPedido> itens = itensDoPedido(pedido);
		if (itens.isEmpty()) {
			throw new MsgApiException("O pedido não possui itens para movimentar o estoque.");
		}

		Map<Long, Produto> produtos = produtosTravados(itens, pedido);

		if (tipo.isSaida()) {
			List<String> insuficientes = new ArrayList<>();
			for (ItemPedido item : itens) {
				Produto produto = produtos.get(item.getProduto().getId());
				if (disponivel(produto).compareTo(BigDecimal.valueOf(item.getQuantidade())) < 0) {
					insuficientes.add(produto.getNome() + " (disponível: " + produto.getEstoque() + ", solicitado: " + item.getQuantidade() + ")");
				}
			}
			if (!insuficientes.isEmpty()) {
				throw new MsgApiException("Estoque insuficiente para os produtos: " + String.join("; ", insuficientes));
			}
		}

		movimentar(pedido, itens, produtos, tipo);
	}

	private void movimentar(Pedido pedido, List<ItemPedido> itens, Map<Long, Produto> produtos, TipoMovimentacaoProduto tipo) {
		List<MovimentacaoProduto> movimentos = new ArrayList<>();
		for (ItemPedido item : itens) {
			Produto produto = produtos.get(item.getProduto().getId());
			BigDecimal novo = disponivel(produto).add(BigDecimal.valueOf(delta(tipo, item.getQuantidade())));
			produto.setEstoque(novo.doubleValue());

			MovimentacaoProduto mov = new MovimentacaoProduto();
			mov.setProduto(produto);
			mov.setPedido(pedido);
			mov.setEmpresa(pedido.getEmpresa());
			mov.setQuantidade(item.getQuantidade());
			mov.setValor(item.getTotal());
			mov.setTipoMovimentacaoProduto(tipo);
			mov.setDataMovimento(LocalDate.now());
			movimentos.add(mov);
		}
		produtoRepository.saveAll(produtos.values());
		movimentacaoProdutoRepository.saveAll(movimentos);
	}

	private BigDecimal disponivel(Produto produto) {
		return BigDecimal.valueOf(produto.getEstoque() == null ? 0d : produto.getEstoque());
	}

	private List<ItemPedido> itensDoPedido(Pedido pedido) {
		return itemPedidoRepository.findAll(pedido.getId(), pedido.getEmpresa().getId());
	}

	private Map<Long, Produto> produtosTravados(List<ItemPedido> itens, Pedido pedido) {
		List<Long> ids = itens.stream().map(i -> i.getProduto().getId()).distinct().toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		return produtoRepository.buscarPorIdsComLock(ids, pedido.getEmpresa().getId()).stream()
				.collect(Collectors.toMap(Produto::getId, p -> p));
	}
}
