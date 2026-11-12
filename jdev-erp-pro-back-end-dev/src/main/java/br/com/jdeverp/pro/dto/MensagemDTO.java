package br.com.jdeverp.pro.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MensagemDTO {

	private Long id;
	private LocalDate dataEnvio;
	private Boolean lida;
	private String conteudo;
	private String arquivo;
	private Long chamadoId;
	private String chamadoTitulo;
	private Long atendenteId;
	private String atendenteNome;
	private Long clienteId;
	private String clienteNome;
	private Long empresaId;

}
