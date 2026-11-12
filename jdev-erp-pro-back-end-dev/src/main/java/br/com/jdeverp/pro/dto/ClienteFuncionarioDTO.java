package br.com.jdeverp.pro.dto;

import br.com.jdeverp.pro.enums.TipoClienteFuncionario;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ClienteFuncionarioDTO {

	private Long id;
	private TipoClienteFuncionario tipoClienteFuncionario;
	private Long usuarioId;
	private Long pessoaId;
	private String nomePessoa;
	private Long empresaId;

}
