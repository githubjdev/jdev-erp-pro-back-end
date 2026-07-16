package br.com.jdeverp.pro.enums;

public enum TipoPessoa {

	FISICA("Pessoa Fisica"),
	JURIDICA("Pessoa Juridica");

	private final String descricao;

	private TipoPessoa(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

}
