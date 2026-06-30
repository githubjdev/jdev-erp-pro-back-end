package br.com.jdeverp.pro.enums;

public enum UnidadeMedida {
	
	CENTIMETRO("Centimeto"), METRO("Metro"), UNIDADE("Unidade"), QUILO("Quilo");

	private final String descricao;

	private UnidadeMedida(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}


}
