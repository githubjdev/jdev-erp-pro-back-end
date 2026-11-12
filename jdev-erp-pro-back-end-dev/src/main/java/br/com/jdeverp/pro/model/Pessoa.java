package br.com.jdeverp.pro.model;

import java.time.LocalDate;

import org.hibernate.validator.constraints.br.CNPJ;
import org.hibernate.validator.constraints.br.CNPJ.Format;
import org.hibernate.validator.constraints.br.CPF;

import com.fasterxml.jackson.annotation.JsonIgnore;

import br.com.jdeverp.pro.enums.TipoPessoa;
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
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "pessoa", uniqueConstraints = {
		@UniqueConstraint(name = "unique_incricao_estadual", columnNames = "inscricaoEstadual"),
		@UniqueConstraint(name = "unique_cnpj", columnNames = "cnpj"),
		@UniqueConstraint(name = "unique_cpf", columnNames = "cpf"),
		@UniqueConstraint(name = "unique_email", columnNames = "email"),
})
@SequenceGenerator(name = "seq_pessoa", sequenceName = "seq_pessoa", allocationSize = 1, initialValue = 1)
public class Pessoa extends EntidadeBase {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pessoa")
	@ToString.Include
	private Long id;

	@NotBlank(message = "Nome deve ser informado")
	@Column(nullable = false)
	@ToString.Include
	private String nome;

	@Column
	@ToString.Include
	private String razaoSocial;

	@Column
	@ToString.Include
	private String nomeFantasia;

	@Column(unique = true, name = "inscricao_estadual")
	private String inscricaoEstadual;

	/*
	 * Documentos (P13, script V2): e-mail obrigatório para todos, CPF só para
	 * pessoa física e CNPJ só para pessoa jurídica. Os três são únicos no banco
	 * inteiro (P12); a unique do PostgreSQL aceita vários NULL. Em branco vira
	 * NULL nos setters, senão dois cadastros sem o documento bateriam na unique.
	 */
	@CNPJ(message = "Informe o CNPJ corretamente",format = Format.ALPHANUMERIC)
	@Column(unique = true)
	private String cnpj;

	@NotBlank(message = "Informe o telefone corretamente")
	@Column(nullable = false)
	private String telefone;

	@CPF(message = "Informe o CPF corretamente")
	@Column(unique = true)
	private String cpf;

	@NotBlank(message = "Informe o e-mail corretamente")
	@Email(message = "Informe o e-mail corretamente")
	@Column(nullable = false, unique = true)
	private String email;

	@NotNull(message = "Informe o tipo da pessoa PF ou PJ")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@ToString.Include
	private TipoPessoa tipoPessoa;

	@NotNull(message = "Informe se a pessoa está ativa")
	@Column(nullable = false)
	@ToString.Include
	private Boolean ativo = true;

	/*Preenchida pelo PessoaService no cadastro e mantida na edição (P20). A coluna é NOT NULL.*/
	@Column(nullable = false)
	@ToString.Include
	private LocalDate dataCadastro;

	@Column(length = 1000)
	private String observacao;

	@NotBlank(message = "Informe o CEP corretamente")
	@Column(nullable = false)
	private String cep;

	@NotBlank(message = "Informe a rua/logradouro da forma correta")
	@Column(nullable = false)
	private String logradouro;

	@NotBlank(message = "Informe o Bairro corretamente")
	@Column(nullable = false)
	private String bairro;

	@NotBlank(message = "Informe o Estado corretamente")
	@Column(nullable = false)
	private String estado;

	@NotBlank(message = "Informe o Cidade corretamente")
	@Column(nullable = false)
	private String cidade;

	@NotBlank(message = "Informe o País corretamente")
	@Column(nullable = false)
	private String pais;

	@Column
	private String complemento;

	/*Refere-se ao cadastro da empresa em multitanci*/
	@ManyToOne(fetch = FetchType.LAZY) /* LAZY -> Carrega a empresa quando tiver necessidade */
	@JoinColumn(name = "empresa_id", 
	        nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                                name = "empresa_fk"))
	private Empresa empresa;

	public void setCpf(String cpf) {
		this.cpf = vazioParaNulo(cpf);
	}

	public void setCnpj(String cnpj) {
		this.cnpj = vazioParaNulo(cnpj);
	}

	/*Mesma regra das CHECKs pessoa_cpf_pessoa_fisica_check e pessoa_cnpj_pessoa_juridica_check do banco (V2)*/
	@JsonIgnore
	@AssertTrue(message = "Informe o CPF para pessoa física")
	public boolean isCpfInformadoParaPessoaFisica() {
		return tipoPessoa != TipoPessoa.FISICA || cpf != null;
	}

	@JsonIgnore
	@AssertTrue(message = "Informe o CNPJ para pessoa jurídica")
	public boolean isCnpjInformadoParaPessoaJuridica() {
		return tipoPessoa != TipoPessoa.JURIDICA || cnpj != null;
	}

	private static String vazioParaNulo(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

}
