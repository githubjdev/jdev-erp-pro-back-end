package br.com.jdeverp.pro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/*DTO (ou Record) - Data Transfer Object = Objeto de transferencia de dados*/
@Data
public class UsuarioDto {

	private Long id;
	private String pessoa;
	private Boolean liberado = true;
	private String empresa;
	private String tipoClienteFuncionario;
	
	@NotBlank(message = "Login deve ser informado.")
	private String login;
	
	private String senha;
	
	private Long clienteFuncionarioId;

	@NotNull(message = "Pessoa deve ser informada para cadastrar o usuário de acesso ao sistema.")
	private Long pessoaId;

}
