package br.com.jdeverp.pro.enums;

public enum TipoPlano {
	
	FREE("Plano Gratuito"),
	PRO("Plano Profissional"),
	ENTERPRISE("Plano Corporativo");

	private final String descricao;
	
	private TipoPlano(String descricao) {
		this.descricao = descricao;
	}
	
	public String getDescricao() {
		return descricao;
	}
	
}
