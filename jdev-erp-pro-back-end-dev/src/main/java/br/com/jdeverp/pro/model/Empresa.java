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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "empresa")
@SequenceGenerator(name = "seq_empresa", sequenceName = "seq_empresa", allocationSize = 1, initialValue = 1)
public class Empresa {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_empresa")
	private Long id;

	@NotNull(message = "Plano deve ser informado")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plano_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "plano_fk"))
	private Plano plano;

	@Column(nullable = true)
	private Integer totalUsuario = 0;

	@Column(nullable = true)
	private Integer totalCliente = 0;

	private Boolean planoAtivo = false;

	private Boolean bloqueio = false;

	@NotEmpty(message = "Informe a logo marca da empresa")
	@NotNull(message = "Logo marca deve ser informado")
	@Column(columnDefinition = "text", nullable = false)
	private String logoMarca;

	@Column(nullable = true)
	private LocalDate vigenciaPlano;
	

	

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Plano getPlano() {
		return plano;
	}

	public void setPlano(Plano plano) {
		this.plano = plano;
	}

	public Integer getTotalUsuario() {
		return totalUsuario;
	}

	public void setTotalUsuario(Integer totalUsuario) {
		this.totalUsuario = totalUsuario;
	}

	public Integer getTotalCliente() {
		return totalCliente;
	}

	public void setTotalCliente(Integer totalCliente) {
		this.totalCliente = totalCliente;
	}

	public Boolean getPlanoAtivo() {
		return planoAtivo;
	}

	public void setPlanoAtivo(Boolean planoAtivo) {
		this.planoAtivo = planoAtivo;
	}

	public Boolean getBloqueio() {
		return bloqueio;
	}

	public void setBloqueio(Boolean bloqueio) {
		this.bloqueio = bloqueio;
	}

	public String getLogoMarca() {
		return logoMarca;
	}

	public void setLogoMarca(String logoMarca) {
		this.logoMarca = logoMarca;
	}

	public LocalDate getVigenciaPlano() {
		return vigenciaPlano;
	}

	public void setVigenciaPlano(LocalDate vigenciaPlano) {
		this.vigenciaPlano = vigenciaPlano;
	}

}
