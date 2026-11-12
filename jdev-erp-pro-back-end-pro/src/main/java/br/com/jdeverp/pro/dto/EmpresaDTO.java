package br.com.jdeverp.pro.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpresaDTO {
	
	private Long id;
	private Long planoId;
	private String planoNome;
	private Integer totalUsuario = 0;
	private Integer totalCliente = 0;
	private Boolean planoAtivo = false;
	private Boolean bloqueio = false;
	private String logoMarca;
	private LocalDate vigenciaPlano;
	private Long pessoaId;
	private String pessoaNome;
}
