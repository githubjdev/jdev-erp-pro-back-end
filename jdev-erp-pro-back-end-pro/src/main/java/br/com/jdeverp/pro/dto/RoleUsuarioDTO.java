package br.com.jdeverp.pro.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUsuarioDTO {

	private Long id;
	private Long acessoId;
	private String acessoNome;
	private Long usuarioId;
	private String usuarioNome;

}
