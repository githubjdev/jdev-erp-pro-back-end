package br.com.jdeverp.pro.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.jdeverp.pro.enums.FormaPagamento;
import br.com.jdeverp.pro.enums.StatusPedido;
import br.com.jdeverp.pro.enums.TipoPedido;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "pedido")
@SequenceGenerator(name = "seq_pedido", sequenceName = "seq_pedido", allocationSize = 1, initialValue = 1)
public class Pedido extends EntidadeBase {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pedido")
	@ToString.Include
	private Long id;
	
	@NotBlank(message = "Inform o número do pedido")
	@Column(nullable = false)
	@ToString.Include
	private String numeroPedido;
	
	
	@NotNull(message = "Informa o status do pedido")
	@Enumerated(EnumType.STRING)
	@ToString.Include
	private StatusPedido statusPedido;
	
	@NotNull(message = "Informe a forma de pagamento")
	@Enumerated(EnumType.STRING)
	@ToString.Include
	private FormaPagamento formaPagamento;
	
	@NotNull(message = "Informe o tipo do pedido")
	@Enumerated(EnumType.STRING)
	@ToString.Include
	private TipoPedido tipoPedido;
	
	@NotNull(message = "Informe a forma de pagamento")
	@Column(nullable = false)
	@ToString.Include
	private LocalDate dataPedido;
	
	@ToString.Include
	private LocalDate dataPagamento;
	@ToString.Include
	private LocalDate dataCancelamento;
	
	@ToString.Include
	private BigDecimal subTotal = BigDecimal.ZERO;
	@ToString.Include
	private BigDecimal desconto = BigDecimal.ZERO;
	@ToString.Include
	private BigDecimal frete = BigDecimal.ZERO;
	@ToString.Include
	private BigDecimal taxas = BigDecimal.ZERO;
	@ToString.Include
	private BigDecimal total = BigDecimal.ZERO;
	
	private String observacao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vendedor_id", nullable = false,
    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
          name = "vendedor_fk"))
    private Usuario vendedor;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id", nullable = false,
    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
          name = "cliente_fk"))
    private Usuario cliente;

	
    /* Sem @NotNull: a empresa vem do usuário logado (service), nunca do request. A coluna continua NOT NULL. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "empresa_fk"))
    private Empresa empresa;

	
	

}
