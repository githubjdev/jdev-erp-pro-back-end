package br.com.jdeverp.pro.enums;

public enum TipoMovimentacaoProduto {

    ENTRADA_COMPRA("Entrada por Compra"),
    ENTRADA_DEVOLUCAO_CLIENTE("Entrada por Devolução de Cliente"),
    ENTRADA_TRANSFERENCIA("Entrada por Transferência"),
    ENTRADA_PRODUCAO("Entrada por Produção"),
    ENTRADA_AJUSTE("Entrada por Ajuste de Estoque"),
    ENTRADA_BONIFICACAO("Entrada por Bonificação"),
    ENTRADA_INVENTARIO("Entrada por Inventário"),

    SAIDA_VENDA("Saída por Venda"),
    SAIDA_DEVOLUCAO_FORNECEDOR("Saída por Devolução ao Fornecedor"),
    SAIDA_TRANSFERENCIA("Saída por Transferência"),
    SAIDA_CONSUMO_INTERNO("Saída para Consumo Interno"),
    SAIDA_PERDA("Saída por Perda"),
    SAIDA_AVARIA("Saída por Avaria"),
    SAIDA_FURTO("Saída por Furto/Roubo"),
    SAIDA_BONIFICACAO("Saída por Bonificação"),
    SAIDA_BRINDE("Saída por Brinde"),
    SAIDA_AJUSTE("Saída por Ajuste de Estoque"),
    SAIDA_INVENTARIO("Saída por Inventário"),

    RESERVA_ESTOQUE("Reserva de Estoque"),
    LIBERACAO_RESERVA("Liberação de Reserva");

    private final String descricao;

    TipoMovimentacaoProduto(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }

    public boolean isEntrada() {
        return name().startsWith("ENTRADA");
    }

    public boolean isSaida() {
        return name().startsWith("SAIDA");
    }

    public boolean isReserva() {
        return this == RESERVA_ESTOQUE || this == LIBERACAO_RESERVA;
    }
}