package br.com.jdeverp.pro.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.jdeverp.pro.enums.TipoMovimentacaoProduto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovimentacaoProdutoDTO {

	private Long id;
	private Double quantidade;
	private LocalDate dataMovimento;
	private BigDecimal valor;
	private TipoMovimentacaoProduto tipoMovimentacaoProduto;
	private Long produtoId;
	private String produtoNome;
	private Long pedidoId;
	private String numeroPedido;
	private Long empresaId;

}
