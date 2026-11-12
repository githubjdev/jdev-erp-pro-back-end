package br.com.jdeverp.pro.model;

import br.com.jdeverp.pro.enums.TipoPlano;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.ToString;

@Entity
@Table(name = "plano")
@SequenceGenerator(name = "seq_plano", sequenceName = "seq_plano", allocationSize = 1, initialValue = 1)
@ToString(onlyExplicitlyIncluded = true)
public class Plano extends EntidadeBase {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_plano")
	@ToString.Include
	private Long id;

	@NotBlank(message = "Nome deve ser informado")
	@NotEmpty(message = "Nome não pode ser nulo")
	@Column(nullable = false)
	@ToString.Include
	private String nome;

	@NotBlank(message = "Descrição deve ser informado")
	@NotEmpty(message = "Descrição não pode ser nulo")
	@Column(nullable = false)
	private String descricao;

	@ToString.Include
	private Boolean ativo;

	/*
	 * Igual ao CHECK plano_valor_mensal_check do banco: FREE vale 0; os demais,
	 * de R$ 49 a R$ 200. A regra por tipo fica no PlanoService.validarValorMensal().
	 */
	@NotNull(message = "Valor mensal não pode ser nulo")
	@Min(value = 0, message = "Valor mensal do plano não pode ser negativo")
	@Max(value = 200, message = "Valor do plano deve ser no máximo de R$ 200 reais")
	@Column(nullable = false)
	@ToString.Include
	private Double valorMensal;

	@NotNull(message = "Limite de usuário não pode ser nulo")
	@Min(value = 1, message = "Limite minimo de usuário é 1")
	@Max(value = 150, message = "Limite máximo do usuário é 150")
	@Column(nullable = false)
	@ToString.Include
	private Integer limiteUsuario;

	@NotNull(message = "Limite de cliente não pode ser nulo")
	@Min(value = 1, message = "Limite minimo de cliente é 1")
	@Max(value = 150, message = "Limite máximo de cliente é 150")
	@Column(nullable = false)
	@ToString.Include
	private Integer limiteCliente;

	@NotNull(message = "Tipo do plano não pode ser nulo")
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	@ToString.Include
	private TipoPlano tipoPlano;
	
	/*Deixamos comentado porque o banco pode crescer muito e ter 500, 1000, 2000 empresaas para um 1 plano e ficar lento
	  @OneToMany(mappedBy = "plano")
	  private List<Empresa> empresas = new ArrayList<Empresa>();
	*/
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public Boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(Boolean ativo) {
		this.ativo = ativo;
	}

	public Double getValorMensal() {
		return valorMensal;
	}

	public void setValorMensal(Double valorMensal) {
		this.valorMensal = valorMensal;
	}

	public Integer getLimiteUsuario() {
		return limiteUsuario;
	}

	public void setLimiteUsuario(Integer limiteUsuario) {
		this.limiteUsuario = limiteUsuario;
	}

	public Integer getLimiteCliente() {
		return limiteCliente;
	}

	public void setLimiteCliente(Integer limiteCliente) {
		this.limiteCliente = limiteCliente;
	}

	public TipoPlano getTipoPlano() {
		return tipoPlano;
	}

	public void setTipoPlano(TipoPlano tipoPlano) {
		this.tipoPlano = tipoPlano;
	}

}
