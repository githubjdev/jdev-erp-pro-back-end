package br.com.jdeverp.pro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.enums.TipoPedido;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;

/**
 * Cálculo dos valores do pedido (regra de negócio, sem acesso a banco).
 * Item: subTotal = preço × quantidade; total = subTotal - desconto.
 * Pedido: subTotal = soma(total dos itens); total = subTotal - desconto + frete + taxas.
 * Tudo em BigDecimal, 2 casas, HALF_UP.
 */
@Service
public class PedidoCalculoService {

	private static final int ESCALA = 2;

	/*Só pedido aguardando pagamento ou com pagamento recusado aceita alterar itens e valores*/
	public void exigirEditavel(Pedido pedido) {
		if (!pedido.getStatusPedido().isEditavel()) {
			throw new MsgApiException("O pedido no status '" + pedido.getStatusPedido().getDescricao()
					+ "' não pode ser alterado. Só é possível alterar pedidos aguardando pagamento ou com pagamento recusado.");
		}
	}

	/*Preço do item: pedido de COMPRA usa o preço de custo do produto; os demais, o preço de venda*/
	public BigDecimal precoDoItem(Produto produto, TipoPedido tipoPedido) {
		if (TipoPedido.COMPRA.equals(tipoPedido)) {
			if (produto.getPrecoCusto() == null || produto.getPrecoCusto() <= 0) {
				throw new MsgApiException("O produto " + produto.getNome()
						+ " não tem preço de custo. Informe o custo no cadastro do produto para usá-lo em pedido de compra.");
			}
			return BigDecimal.valueOf(produto.getPrecoCusto());
		}
		return BigDecimal.valueOf(produto.getPreco());
	}

	public static BigDecimal valor(BigDecimal v) {
		return v == null ? BigDecimal.ZERO.setScale(ESCALA) : v.setScale(ESCALA, RoundingMode.HALF_UP);
	}

	/*Calcula subTotal e total do item a partir do precoUnitario, da quantidade e do desconto já definidos nele*/
	public void calcularItem(ItemPedido item) {
		if (item.getQuantidade() == null || item.getQuantidade() <= 0) {
			throw new MsgApiException("Quantidade do item deve ser maior que zero.");
		}

		BigDecimal preco = valor(item.getPrecoUnitario());
		BigDecimal desconto = valor(item.getDesconto());
		if (desconto.signum() < 0) {
			throw new MsgApiException("Desconto do item não pode ser negativo.");
		}

		BigDecimal subTotal = preco.multiply(BigDecimal.valueOf(item.getQuantidade())).setScale(ESCALA, RoundingMode.HALF_UP);
		if (desconto.compareTo(subTotal) > 0) {
			throw new MsgApiException("Desconto do item não pode ser maior que o subtotal do item.");
		}

		item.setPrecoUnitario(preco);
		item.setSubTotal(subTotal);
		item.setDesconto(desconto);
		item.setTotal(subTotal.subtract(desconto));
	}

	/*Calcula subTotal e total do pedido a partir dos itens (já calculados) e de desconto, frete e taxas do pedido*/
	public void calcularPedido(Pedido pedido, List<ItemPedido> itens) {
		BigDecimal subTotal = itens.stream().map(i -> valor(i.getTotal())).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal desconto = valor(pedido.getDesconto());
		BigDecimal frete = valor(pedido.getFrete());
		BigDecimal taxas = valor(pedido.getTaxas());

		if (desconto.signum() < 0 || frete.signum() < 0 || taxas.signum() < 0) {
			throw new MsgApiException("Desconto, frete e taxas não podem ser negativos.");
		}

		BigDecimal total = subTotal.subtract(desconto).add(frete).add(taxas);
		if (total.signum() < 0) {
			throw new MsgApiException("O desconto do pedido não pode ser maior que o valor dos itens.");
		}

		pedido.setSubTotal(valor(subTotal));
		pedido.setDesconto(desconto);
		pedido.setFrete(frete);
		pedido.setTaxas(taxas);
		pedido.setTotal(valor(total));
	}
}
