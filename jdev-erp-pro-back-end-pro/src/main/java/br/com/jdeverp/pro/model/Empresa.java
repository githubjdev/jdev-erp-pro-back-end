package br.com.jdeverp.pro.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@JsonIdentityInfo(
	    generator = ObjectIdGenerators.PropertyGenerator.class,
	    property = "id"
)
@JsonIdentityReference(alwaysAsId = true)
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "empresa", uniqueConstraints = {
		@UniqueConstraint(name="unique_pessoa_empresa", columnNames = {"pessoa_id"})
})
@SequenceGenerator(name = "seq_empresa", sequenceName = "seq_empresa", allocationSize = 1, initialValue = 1)
public class Empresa extends EntidadeBase {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_empresa")
	@ToString.Include
	private Long id;

	@NotNull(message = "Plano deve ser informado")
	@ManyToOne(fetch = FetchType.LAZY) /* LAZY -> Carrega o plano quando tiver necessidade */
	@JoinColumn(name = "plano_id", nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	            name = "plano_fk"))
	private Plano plano;

	@Column(nullable = true)
	@ToString.Include
	private Integer totalUsuario = 0;

	@Column(nullable = true)
	@ToString.Include
	private Integer totalCliente = 0;

	@ToString.Include
	private Boolean planoAtivo = false;

	@ToString.Include
	private Boolean bloqueio = false;

	@NotEmpty(message = "Informe a logo marca da empresa")
	@NotNull(message = "Logo marca deve ser informado")
	@Column(columnDefinition = "text", nullable = false)
	private String logoMarca;

	@Column(nullable = true)
	@ToString.Include
	private LocalDate vigenciaPlano;

	@NotNull(message = "Pessoa deve ser informada para cadastrar a instituição juridíca (PJ)")
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pessoa_id", nullable = false, 
	     foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "pessoa_fk"))
	private Pessoa pessoa;

}
