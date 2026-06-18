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
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "plano")
@SequenceGenerator(name = "seq_plano", sequenceName = "seq_plano", allocationSize = 1, initialValue = 1)
public class Plano {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_plano")
	private Long id;

	@NotBlank(message = "Nome deve ser informado")
	@NotNull(message = "Nome não pode ser nulo")
	@Column(nullable = false)
	private String nome;

	@NotBlank(message = "Descrição deve ser informado")
	@NotNull(message = "Descrição não pode ser nulo")
	@Column(nullable = false)
	private String descricao;

	private Boolean ativo;

	@NotNull(message = "Valor Mensal não pode ser nulo")
	@Min(value = 49, message = "Valor minímo do plano deve ser de R$ 49 reais")
	@Max(value = 200, message = "Valor do plano deve ser no máximo R$ 200 reais")
	private Double valorMensal;

	@NotNull(message = "Limite de usuário não pode ser nulo")
	@Min(value = 1, message = "Limite minimo de usuário é 1")
	@Max(value = 150, message = "Limite máximo de usuário é 150")
	private Integer limiteUsuario;

	@NotNull(message = "Limite de cliente não pode ser nulo")
	@Min(value = 1, message = "Limite minimo de usuário é 1")
	@Max(value = 150, message = "Limite máximo de usuário é 150")
	private Integer limiteCliente;

	@NotNull(message = "Tipo do plano não pode ser nulo")
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private TipoPlano tipoPlano;
	
	
	/*Deixamos comentado porque o banco pode crescer muito e ter 500, 1000, 2000 empresaas para um 1 plano e ficar lento
	  @OneToMany(mappedBy = "plano")
	  private List<Empresa> empresas = new ArrayList<Empresa>();
	*/
	

}
