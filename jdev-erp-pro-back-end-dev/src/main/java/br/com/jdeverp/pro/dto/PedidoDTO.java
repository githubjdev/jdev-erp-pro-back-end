package br.com.jdeverp.pro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.jdeverp.pro.enums.FormaPagamento;
import br.com.jdeverp.pro.enums.StatusPedido;
import br.com.jdeverp.pro.enums.TipoPedido;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoDTO {

	private Long id;
	private String numeroPedido;
	private StatusPedido statusPedido;
	private FormaPagamento formaPagamento;
	private TipoPedido tipoPedido;
	private LocalDate dataPedido;
	private LocalDate dataPagamento;
	private LocalDate dataCancelamento;
	private BigDecimal subTotal;
	private BigDecimal desconto;
	private BigDecimal frete;
	private BigDecimal taxas;
	private BigDecimal total;
	private String observacao;
	private Long vendedorId;
	private String vendedorNome;
	private Long clienteId;
	private String clienteNome;
	private Long empresaId;

}
