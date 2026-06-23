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
import lombok.Data;

@Data
@Entity
@Table(name = "produto", uniqueConstraints = {
		@UniqueConstraint(name = "unique_sku", columnNames = "sku"),
})
@SequenceGenerator(name = "seq_produto", sequenceName = "seq_produto", allocationSize = 1, initialValue = 1)
public class Produto {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_produto")
	private Long id;
	
	
	@NotBlank(message = "Nome deve ser informado")
	@NotNull(message = "Nome não pode ser nulo")
	@Column(nullable = false)
	private String nome;
	
	private String descricao;
	
	@NotBlank(message = "Imagem deve ser informado")
	@NotNull(message = "Imagem não pode ser nulo")
	@Column(columnDefinition = "text")
	private String imagem;
	
	@Min(value = 1, message = "Valor deve ser maior ou igual a 1")
	@Column(nullable = false)
	private Double preco;
	
	@Min(value = 1, message = "Estoque deve ser maior ou igual a 1")
	@Column(nullable = false)
	private Double estoque;
	
	@Min(value = 1, message = "Estoque Minimo deve ser maior ou igual a 1")
	@Column(nullable = false)
	private Double estoqueMinimo;
	
	@Column(unique = true)
	private String sku;
	
	private String codigoBarra;
	
	
    @NotNull(message = "Unidade de medida deve ser informado")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false)
	private UnidadeMedida unidadeMedida;
    
    
    
    @NotNull(message = "Categoria deve ser informada corretamente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "categoria_fk"))
    private Categoria categoria;
    
    
    
    /*Refere-se ao cadastro da emrpreda em multitanenti*/
    @NotNull(message = "Empresa deve ser informada corretamente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "empresa_fk"))
    private Empresa empresa;
    

}
