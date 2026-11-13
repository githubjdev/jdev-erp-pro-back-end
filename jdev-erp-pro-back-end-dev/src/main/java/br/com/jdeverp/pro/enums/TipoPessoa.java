package br.com.jdeverp.pro.enums;

public enum TipoPessoa {

	JURIDICA("Jurídica"), FISICA("Física");

	private final String descricao;

	private TipoPessoa(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
