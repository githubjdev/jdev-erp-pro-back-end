package br.com.jdeverp.pro.dto;

import lombok.Data;

/*Função de um DTO: Mostrar apenas os dados necessário ou carregar e evitar recursividade de entidades*/
@Data
public class UsuarioDto {

	private String pessoa;
	private Boolean bloqueado;
	private String empresa;

}
