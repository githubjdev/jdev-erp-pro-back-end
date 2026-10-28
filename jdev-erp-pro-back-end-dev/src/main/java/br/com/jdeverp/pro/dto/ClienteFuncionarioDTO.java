package br.com.jdeverp.pro.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ClienteFuncionarioDTO {

	private Long id;
	private String tipoClienteFuncionario;
	private Long usuarioId;
	private Long pessoaId;
	private String nomePessoa;
	private Long empresaId;

}
