package br.com.jdeverp.pro.dto;

import br.com.jdeverp.pro.enums.UnidadeMedida;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProdutoDTO {

	private Long id;
	private String nome;
	private String descricao;
	private String imagem;
	private Double preco;
	private Double estoque;
	private Double estoqueMinimo;
	private String sku;
	private String codigoBarra;
	private UnidadeMedida unidadeMedida;
	private Long categoriaId;
	private String categoriaNome;
	private Long empresaId;

}
