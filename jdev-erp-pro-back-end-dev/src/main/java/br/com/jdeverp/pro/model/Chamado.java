package br.com.jdeverp.pro.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.jdeverp.pro.enums.PrioridadeChamado;
import br.com.jdeverp.pro.enums.StatusChamado;
import br.com.jdeverp.pro.enums.TipoChamado;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "chamado")
@SequenceGenerator(name = "seq_chamado", sequenceName = "seq_chamado", allocationSize = 1, initialValue = 1)
public class Chamado {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chamado")
	private Long id;
	
	@NotNull(message = "Tírulo do chamado deve ser informado")
	@Column(nullable = false)
	private String titulo;
	
	@NotNull(message = "descrição do chamado deve ser informado")
	@Column(nullable = false)
	private String descricao;
	
	@NotNull(message = "Tipo do chamado deve ser informado")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoChamado tipoChamado;
	
	@NotNull(message = "Prioridade do chamado deve ser informado")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PrioridadeChamado prioridadeChamado;
	
	@NotNull(message = "Status do chamado deve ser informado")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private StatusChamado statusChamado;
	
	@NotNull(message = "Data de abertura do chamado é obrigatória")
	@Column(nullable = false)
	private LocalDate dataAbertura;
	
	private LocalDate dataFechamento;
	
	
	/*Coluna que liga o atendente/cliente que abriu o chamado*/
	/*Tando o cliente quando o atendente pode abrir chamado*/
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "aberto_user_id", 
    nullable = true,
    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
          name = "aberto_user_fk"))
    private Usuario abertodoUser;
	
	/*Coluna que liga o atendente que encerrou o chamado*/
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fechado_user_id", 
    nullable = true,
    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
          name = "fechado_user_fk"))
    private Usuario fechadoUser;
	
	
	/*Coluna que liga com o atendente que está resolvendo o chamado*/
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atendente_id", 
    nullable = false,
    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
          name = "atendente_fk"))
    private Usuario atendente;
	
	/*Coluna do cliente que está sendo atendido*/
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_id", 
    nullable = false,
    foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
          name = "cliente_fk"))
    private Usuario cliente;
	
	
	
	@OneToMany(mappedBy = "chamado", fetch = FetchType.LAZY)
	private List<Mensagem> mensagems = new ArrayList<Mensagem>();
	
	
    @NotNull(message = "Empresa deve ser informada corretamente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "empresa_fk"))
    private Empresa empresa;


}
