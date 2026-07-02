package br.com.jdeverp.pro.enums;

public enum TipoChamado {

	    DUVIDA("Dúvida"),
	    SUPORTE("Suporte"),
	    INCIDENTE("Incidente"),
	    BUG("Bug"),
	    MELHORIA("Melhoria"),
	    NOVA_FUNCIONALIDADE("Nova Funcionalidade"),
	    INTEGRACAO("Integração"),
	    CADASTRO("Cadastro"),
	    PERMISSAO_ACESSO("Permissão de Acesso"),
	    INFRAESTRUTURA("Infraestrutura"),
	    FINANCEIRO("Financeiro"),
	    TREINAMENTO("Treinamento"),
	    CONSULTORIA("Consultoria");

	    private final String descricao;
	
	    TipoChamado(String descricao) {
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