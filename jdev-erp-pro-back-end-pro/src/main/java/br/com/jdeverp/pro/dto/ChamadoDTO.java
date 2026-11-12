package br.com.jdeverp.pro.dto;

import java.time.LocalDate;

import br.com.jdeverp.pro.enums.PrioridadeChamado;
import br.com.jdeverp.pro.enums.StatusChamado;
import br.com.jdeverp.pro.enums.TipoChamado;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChamadoDTO {

	private Long id;
	private String titulo;
	private String descricao;
	private TipoChamado tipoChamado;
	private PrioridadeChamado prioridadeChamado;
	private StatusChamado statusChamado;
	private LocalDate dataAbertura;
	private LocalDate dataFechamento;
	private Long abertoUserId;
	private String abertoUserNome;
	private Long fechadoUserId;
	private String fechadoUserNome;
	private Long atendenteId;
	private String atendenteNome;
	private Long clienteId;
	private String clienteNome;
	private Long empresaId;

}
