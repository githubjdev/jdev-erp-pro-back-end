package br.com.jdeverp.pro.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.jdeverp.pro.enums.FormaPagamento;
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
import lombok.Data;

@Data
@Entity
@Table(name = "pedido")
@SequenceGenerator(name = "seq_pedido", sequenceName = "seq_pedido", allocationSize = 1, initialValue = 1)
public class Pedido {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pedido")
	private Long id;
	
	@NotBlank(message = "Inform o número do pedido")
	@Column(nullable = false)
	private String numeroPedido;
	
	@NotNull(message = "Informe a forma de pagamento")
	@Enumerated(EnumType.STRING)
	private FormaPagamento formaPagamento;
	
	@NotNull(message = "Informe o tipo do pedido")
	@Enumerated(EnumType.STRING)
	private TipoPedido tipoPedido;
	
	@NotNull(message = "Informe a forma de pagamento")
	@Column(nullable = false)
	private LocalDate dataPedido;
	
	private LocalDate dataPagamento;
	private LocalDate dataCancelamento;
	
	private BigDecimal subTotal = BigDecimal.ZERO;
	private BigDecimal desconto = BigDecimal.ZERO;
	private BigDecimal frete = BigDecimal.ZERO;
	private BigDecimal taxas = BigDecimal.ZERO;
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

	
    @NotNull(message = "Empresa deve ser informada corretamente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "empresa_fk"))
    private Empresa empresa;

	
	

}
