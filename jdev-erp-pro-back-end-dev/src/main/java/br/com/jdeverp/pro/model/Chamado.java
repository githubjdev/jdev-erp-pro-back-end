package br.com.jdeverp.pro.model;

import java.time.LocalDate;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "chamado")
@SequenceGenerator(name = "seq_chamado", sequenceName = "seq_chamado", allocationSize = 1, initialValue = 1)
public class Chamado extends EntidadeBase {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_chamado")
	@ToString.Include
	private Long id;
	
	@NotNull(message = "Título do chamado deve ser informado")
	@Column(nullable = false)
	@ToString.Include
	private String titulo;
	
	@NotNull(message = "Descrição do chamado deve ser informado")
	@Column(nullable = false)
	private String descricao;
	
	@NotNull(message = "Tipo do chamado deve ser informado")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@ToString.Include
	private TipoChamado tipoChamado;;
	
	@NotNull(message = "Prioridade do chamado deve ser informado")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@ToString.Include
	private PrioridadeChamado prioridadeChamado;
	
	@NotNull(message = "Status do chamado deve ser informado")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@ToString.Include
	private StatusChamado statusChamado;
	
	/* Sem @NotNull: o service preenche com a data de hoje. A coluna continua NOT NULL. */
	@Column(nullable = false)
	@ToString.Include
	private LocalDate dataAbertura;
	
	@ToString.Include
	private LocalDate dataFechamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aberto_user_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "aberto_user_fk"))
    private Usuario abertoUser;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fechado_user_id", 
            nullable = true,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "fechado_user_fk"))
    private Usuario fechadoUser;
	
	
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "empresa_fk"))
    private Empresa empresa;

}
