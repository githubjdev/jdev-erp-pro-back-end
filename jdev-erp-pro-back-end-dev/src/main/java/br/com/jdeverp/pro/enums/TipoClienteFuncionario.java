package br.com.jdeverp.pro.enums;

public enum TipoClienteFuncionario {
	
	
	CLIENTE("Cliente"),
	FUNCIONARIO("Funcionário"),
	PRESTADOR_SERVICO("Prestador de serviço");

	private final String descricao;

	private TipoClienteFuncionario(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}


}
