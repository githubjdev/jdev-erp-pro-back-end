package br.com.jdeverp.pro.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemPedidoDTO {

	private Long id;
	private Double quantidade;
	private BigDecimal subTotal;
	private BigDecimal desconto;
	private BigDecimal total;
	private Long produtoId;
	private String produtoNome;
	private Long pedidoId;
	private String numeroPedido;
	private Long empresaId;

}
