package br.com.jdeverp.pro.dto;

import java.time.LocalDate;

import br.com.jdeverp.pro.enums.TipoPessoa;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PessoaDTO {

	private Long id;
	private String nome;
	private String razaoSocial;
	private String nomeFantasia;
	private String inscricaoEstadual;
	private String cnpj;
	private String telefone;
	private String cpf;
	private String email;
	private TipoPessoa tipoPessoa;
	private Boolean ativo;
	private LocalDate dataCadastro;
	private String observacao;
	private String cep;
	private String logradouro;
	private String bairro;
	private String estado;
	private String cidade;
	private String pais;
	private String complemento;
	private Long empresaId;

}
