package br.com.jdeverp.pro.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.com.jdeverp.pro.enums.TipoMovimentacaoProduto;
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
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "movimentacao_produto")
@SequenceGenerator(name = "seq_movimentacao_produto", sequenceName = "seq_movimentacao_produto", allocationSize = 1, initialValue = 1)
public class MovimentacaoProduto {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_movimentacao_produto")
	private Long id;
	
	@DecimalMin(value = "0.1", message = "Valor minímo de 0.1 deve ser infromado")
	@Column(nullable = false)
	private Double quantidade = 1.0;
	
	@Column(nullable = false)
	private LocalDate dataMovimento;

	private BigDecimal valor = BigDecimal.ZERO;

	@NotNull(message = "Informe o tipo da movimentação")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
    private TipoMovimentacaoProduto tipoMovimentacaoProduto;	
	
    @NotNull(message = "Produto deve ser informada corretamente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "produto_fk"))
    private Produto produto;


    /*Pode ser movimentação de perda, extravio, descarte e não ter ligação com pedido*/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", 
            nullable = true,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "pedido_fk"))
    private Pedido pedido;


	
    @NotNull(message = "Empresa deve ser informada corretamente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "empresa_fk"))
    private Empresa empresa;



}
