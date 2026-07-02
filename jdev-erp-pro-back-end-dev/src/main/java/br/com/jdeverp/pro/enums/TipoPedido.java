package br.com.jdeverp.pro.enums;

public enum TipoPedido {

    VENDA("Venda"),
    COMPRA("Compra"),
    ORCAMENTO("Orçamento"),
    PEDIDO_PRODUCAO("Pedido de Produção"),
    PEDIDO_TRANSFERENCIA("Transferência"),
    DEVOLUCAO_CLIENTE("Devolução de Cliente"),
    DEVOLUCAO_FORNECEDOR("Devolução para Fornecedor"),
    BONIFICACAO("Bonificação"),
    BRINDE("Brinde"),
    CONSIGNACAO("Consignação"),
    REMESSA("Remessa"),
    RETORNO("Retorno");

    private final String descricao;

    TipoPedido(String descricao) {
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