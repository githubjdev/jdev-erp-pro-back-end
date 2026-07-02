package br.com.jdeverp.pro.enums;

public enum PrioridadeChamado {

	BAIXA("Baixa"),
	MEDIA("Média"), 
	ALTA("Alta"), 
	CRITICA("Crítica");

	private final String descricao;

	PrioridadeChamado(String descricao) {
		this.descricao = descricao;
	}

	public String getDescricao() {
		return descricao;
	}

	@Override
	public String toString() {
		return descricao;
	}
}