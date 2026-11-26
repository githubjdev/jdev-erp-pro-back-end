package br.com.jdeverp.pro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import br.com.jdeverp.pro.enums.FormaPagamento;
import br.com.jdeverp.pro.enums.TipoPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/*Entrada para criar/atualizar pedido já com a lista de itens. Status, datas, subtotal e total NÃO são aceitos do cliente.*/
@Getter
@Setter
public class PedidoComItensDTO {

	/*Só usado na atualização*/
	private Long id;

	@NotBlank(message = "Número do pedido deve ser informado")
	private String numeroPedido;

	@NotNull(message = "Forma de pagamento deve ser informada")
	private FormaPagamento formaPagamento;

	@NotNull(message = "Tipo do pedido deve ser informado")
	private TipoPedido tipoPedido;

	/*Se não vier, usa a data de hoje*/
	private LocalDate dataPedido;

	@DecimalMin(value = "0", message = "Desconto não pode ser negativo")
	private BigDecimal desconto;
	@DecimalMin(value = "0", message = "Frete não pode ser negativo")
	private BigDecimal frete;
	@DecimalMin(value = "0", message = "Taxas não podem ser negativas")
	private BigDecimal taxas;

	private String observacao;

	@NotNull(message = "Vendedor deve ser informado")
	private Long vendedorId;

	@NotNull(message = "Cliente deve ser informado")
	private Long clienteId;

	@NotEmpty(message = "O pedido deve ter pelo menos um item")
	@Valid
	private List<ItemPedidoEntradaDTO> itens;
}
