package br.com.jdeverp.pro.enums;


public enum FormaPagamento {

    DINHEIRO("Dinheiro"),
    PIX("PIX"),
    CARTAO_CREDITO("Cartão de Crédito"),
    CARTAO_DEBITO("Cartão de Débito"),
    BOLETO("Boleto Bancário"),
    TRANSFERENCIA_BANCARIA("Transferência Bancária"),
    DEPOSITO_BANCARIO("Depósito Bancário"),
    CHEQUE("Cheque"),
    CREDIARIO("Crediário"),
    VALE_ALIMENTACAO("Vale Alimentação"),
    VALE_REFEICAO("Vale Refeição"),
    VALE_PRESENTE("Vale Presente"),
    CARTEIRA_DIGITAL("Carteira Digital"),
    FATURADO("Faturado"),
    CREDITO_LOJA("Crédito na Loja"),
    OUTROS("Outros");

    private final String descricao;

    FormaPagamento(String descricao) {
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