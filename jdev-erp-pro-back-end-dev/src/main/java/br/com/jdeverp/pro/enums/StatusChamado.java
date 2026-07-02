package br.com.jdeverp.pro.enums;

public enum StatusChamado {

    ABERTO("Aberto"),
    EM_ANALISE("Em Análise"),
    AGUARDANDO_CLIENTE("Aguardando Cliente"),
    EM_ATENDIMENTO("Em Atendimento"),
    EM_DESENVOLVIMENTO("Em Desenvolvimento"),
    EM_TESTES("Em Testes"),
    AGUARDANDO_HOMOLOGACAO("Aguardando Homologação"),
    RESOLVIDO("Resolvido"),
    FECHADO("Fechado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusChamado(String descricao) {
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