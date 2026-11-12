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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "mensagem")
@SequenceGenerator(name = "seq_mensagem", sequenceName = "seq_mensagem", allocationSize = 1, initialValue = 1)
public class Mensagem extends EntidadeBase {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_mensagem")
	@ToString.Include
	private Long id;
	
	@Column(nullable = false)
	@ToString.Include
	private LocalDate dataEnvio;
	
	@ToString.Include
	private Boolean lida = false;
	
	@NotNull(message = "Conteúdo da mensagem deve ser informado")
	@Column(nullable = false, columnDefinition = "text")
	private String conteudo;
	
	/*Obrigatório, como a coluna NOT NULL do banco (P20)*/
	@NotBlank(message = "Arquivo da mensagem deve ser informado")
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
	@JoinColumn(name = "cliente_id", 
	        nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                                name = "cliente_fk"))
	private Usuario cliente;
	
	
	/* Sem @NotNull: a empresa vem do usuário logado (service), nunca do request. A coluna continua NOT NULL. */
	@ManyToOne(fetch = FetchType.LAZY) /* LAZY -> Carrega a empresa quando tiver necessidade */
	@JoinColumn(name = "empresa_id", 
	        nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                                name = "empresa_fk"))
	private Empresa empresa;
	
	
	

}
