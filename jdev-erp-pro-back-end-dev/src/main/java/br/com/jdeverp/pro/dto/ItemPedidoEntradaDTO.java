package br.com.jdeverp.pro.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/*Item enviado pelo front: só produto, quantidade e desconto. Preço e totais são calculados no servidor.*/
@Getter
@Setter
public class ItemPedidoEntradaDTO {

	@NotNull(message = "Produto deve ser informado")
	private Long produtoId;

	@NotNull(message = "Quantidade deve ser informada")
	@DecimalMin(value = "0.1", message = "Valor mínimo de 0.1 deve ser informado")
	private Double quantidade;

	/*Desconto do item em R$ (opcional)*/
	@DecimalMin(value = "0", message = "Desconto não pode ser negativo")
	private BigDecimal desconto;
}
