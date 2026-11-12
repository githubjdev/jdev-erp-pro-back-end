package br.com.jdeverp.pro.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "item_pedido")
@SequenceGenerator(name = "seq_item_pedido", sequenceName = "seq_item_pedido", allocationSize = 1, initialValue = 1)
public class ItemPedido extends EntidadeBase {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_item_pedido")
	@ToString.Include
	private Long id;
	
	@DecimalMin(value = "0.1", message = "Valor mínimo de 0.1 deve ser informado")
	@Column(nullable = false)
	@ToString.Include
	private Double quantidade = 1.0;
	
	@ToString.Include
	private BigDecimal subTotal = BigDecimal.ZERO;
	@ToString.Include
	private BigDecimal desconto = BigDecimal.ZERO;
	@ToString.Include
	private BigDecimal total = BigDecimal.ZERO;
	
	@NotNull(message = "Produto deve ser informado")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "produto_id", 
	        nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                                name = "produto_fk"))
	private Produto produto;
	
	
	@NotNull(message = "Pedido deve ser informado")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pedido_id", 
	        nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                                name = "pedido_fk"))
	private Pedido pedido;
	
	
	/* Sem @NotNull: a empresa vem do usuário logado (service), nunca do request. A coluna continua NOT NULL. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id", 
	        nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                                name = "empresa_fk"))
	private Empresa empresa;

}
