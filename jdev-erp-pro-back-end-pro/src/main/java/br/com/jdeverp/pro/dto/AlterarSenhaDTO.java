package br.com.jdeverp.pro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlterarSenhaDTO {
	
	@NotNull(message = "Id do usuário deve ser informado")
	private Long id;
	
	@NotBlank(message = "Senha atual deve ser informada")
	@Size(min = 5, max = 50, message = "Senha atual ter entre 5 e 50 caracteres")
	private String senhaAtual;
	
	@NotBlank(message = "Nova senha deve ser informada")
	@Size(min = 5, max = 50, message = "Senha deve ter entre 5 e 50 caracteres")
	private String novaSenha;
	
	@NotBlank(message = "Confirmação da nova senha deve ser informada")
	@Size(min = 5, max = 50, message = "Confirmação da senha deve ter entre 5 e 50 caracteres")
	private String confirmarSenha;

}
