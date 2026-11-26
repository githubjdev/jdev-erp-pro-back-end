package br.com.jdeverp.pro.model;

import br.com.jdeverp.pro.enums.UnidadeMedida;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "produto", uniqueConstraints = {
		@UniqueConstraint(name="unique_sku_empresa", columnNames = {"sku", "empresa_id"}),
		@UniqueConstraint(name="unique_nome_produto_empresa", columnNames = {"nome", "empresa_id"}),
})
@SequenceGenerator(name = "seq_produto", sequenceName = "seq_produto", allocationSize = 1, initialValue = 1)
public class Produto extends EntidadeBase {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_produto")
	@ToString.Include
	private Long id;
	
	@NotBlank(message = "Nome deve ser informado")
	@Column(nullable = false)
	@ToString.Include
	private String nome;
	
	private String descricao;
	
	@NotBlank(message = "Imagem deve ser informada")
	@Column(nullable = false, columnDefinition = "text")
	private String imagem;
	
	/*Preço, estoque e estoque mínimo: no mínimo 1, como os CHECK do banco (P15)*/
	@NotNull(message = "Preço deve ser informado")
	@Min(value = 1, message = "Preço deve ser de no mínimo R$ 1,00")
	@Column(nullable = false)
	@ToString.Include
	private Double preco;

	/*Preço de custo (opcional, padrão 0). Usado como preço do item em pedido de COMPRA.*/
	@NotNull(message = "Preço de custo não pode ser nulo")
	@Min(value = 0, message = "Preço de custo não pode ser negativo")
	@Column(name = "preco_custo", nullable = false)
	@ToString.Include
	private Double precoCusto = 0.0;

	/*Estoque nunca vem do cadastro do produto: só muda por pedido ou por movimentação de estoque lançada.
	 * Se o request trouxer valor, o service ignora (produto novo começa em 0; na atualização mantém o atual).*/
	@Min(value = 0, message = "Estoque não pode ser negativo")
	@Column(nullable = false)
	@ToString.Include
	private Double estoque = 0.0;

	@NotNull(message = "Estoque mínimo deve ser informado")
	@Min(value = 1, message = "Estoque mínimo deve ser de no mínimo 1")
	@Column(nullable = false)
	@ToString.Include
	private Double estoqueMinimo;
	
	@NotBlank(message = "Código SKU deve ser informado")
	@Column(nullable = false)
	@ToString.Include
	private String sku;

	private String codigoBarra; /*Varios tipo de parafuso*/

	@NotNull(message = "Unidade de medida de ser informado")
	@Enumerated(EnumType.STRING)
	@Column(name = "unidade_medida", nullable = false)
	@ToString.Include
	private UnidadeMedida unidadeMedida;
	
	@NotNull(message = "Categoria deve ser informada.")
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "categoria_id", nullable = false, 
	          foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                    name = "categoria_fk"))
	private Categoria categoria;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id", 
	        nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                                name = "empresa_fk"))
	private Empresa empresa;

}
