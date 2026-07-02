package br.com.jdeverp.pro.model;

import java.time.LocalDate;

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
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "mensagem")
@SequenceGenerator(name = "seq_mensagem", sequenceName = "seq_mensagem", allocationSize = 1, initialValue = 1)

public class Mensagem {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_mensagem")
	private Long id;
	
	
	@Column(nullable = true)
	private LocalDate dataEnvio;

	private Boolean lida = false;

	@NotNull(message = "Conteúdo da mensagem deve ser informado")
	@Column(nullable = true, columnDefinition = "text")
	private String conteudo;
	
	@Column(nullable = false, columnDefinition = "text")
	private String arquivo;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chamado_id", 
    nullable = false,
    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
          name = "chamado_fk"))
    private Chamado chamado;
	

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atendente_id", 
    nullable = false,
    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
          name = "atendente_fk"))
    private Usuario atendente;

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
