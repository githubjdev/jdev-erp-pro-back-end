package br.com.jdeverp.pro.enums;
public enum StatusPedido {

    AGUARDANDO_PAGAMENTO("Aguardando Pagamento"),
    PAGAMENTO_APROVADO("Pagamento Aprovado"),
    PAGAMENTO_RECUSADO("Pagamento Recusado"),
    EM_SEPARACAO("Em Separação"),
    SEPARADO("Separado"),
    FATURADO("Faturado"),
    AGUARDANDO_ENVIO("Aguardando Envio"),
    ENVIADO("Enviado"),
    EM_TRANSPORTE("Em Transporte"),
    ENTREGUE("Entregue"),
    CANCELADO("Cancelado"),
    DEVOLVIDO("Devolvido"),
    REEMBOLSADO("Reembolsado");

    private final String descricao;

    StatusPedido(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /*Estados em que itens, desconto, frete e taxas ainda podem ser alterados*/
    public boolean isEditavel() {
        return this == AGUARDANDO_PAGAMENTO || this == PAGAMENTO_RECUSADO;
    }

    /*Próximos status permitidos a partir deste (máquina de status do pedido)*/
    public java.util.Set<StatusPedido> proximosPermitidos() {
        return switch (this) {
            case AGUARDANDO_PAGAMENTO -> java.util.EnumSet.of(PAGAMENTO_APROVADO, PAGAMENTO_RECUSADO, CANCELADO);
            case PAGAMENTO_RECUSADO -> java.util.EnumSet.of(AGUARDANDO_PAGAMENTO, CANCELADO);
            case PAGAMENTO_APROVADO -> java.util.EnumSet.of(EM_SEPARACAO, CANCELADO);
            case EM_SEPARACAO -> java.util.EnumSet.of(SEPARADO, CANCELADO);
            case SEPARADO -> java.util.EnumSet.of(FATURADO);
            case FATURADO -> java.util.EnumSet.of(AGUARDANDO_ENVIO);
            case AGUARDANDO_ENVIO -> java.util.EnumSet.of(ENVIADO);
            case ENVIADO -> java.util.EnumSet.of(EM_TRANSPORTE, ENTREGUE);
            case EM_TRANSPORTE -> java.util.EnumSet.of(ENTREGUE);
            case ENTREGUE -> java.util.EnumSet.of(DEVOLVIDO);
            case DEVOLVIDO -> java.util.EnumSet.of(REEMBOLSADO);
            case CANCELADO, REEMBOLSADO -> java.util.EnumSet.noneOf(StatusPedido.class);
        };
    }

    public boolean podeIrPara(StatusPedido novo) {
        return novo != null && proximosPermitidos().contains(novo);
    }
}