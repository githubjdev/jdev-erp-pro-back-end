package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.enums.StatusPedido;
import br.com.jdeverp.pro.enums.TipoMovimentacaoProduto;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.model.Pedido;

public class TestPedidoCalculoService {

	private final PedidoCalculoService calculo = new PedidoCalculoService();

	private static ItemPedido item(String preco, double quantidade, String desconto) {
		ItemPedido item = new ItemPedido();
		item.setPrecoUnitario(new BigDecimal(preco));
		item.setQuantidade(quantidade);
		item.setDesconto(new BigDecimal(desconto));
		return item;
	}

	@Test
	public void testCalcularItem() {
		ItemPedido item = item("19.99", 3, "5.00");
		calculo.calcularItem(item);
		assertEquals(new BigDecimal("59.97"), item.getSubTotal());
		assertEquals(new BigDecimal("54.97"), item.getTotal());
	}

	@Test
	public void testCalcularItemArredondaHalfUp() {
		ItemPedido item = item("0.335", 1.5, "0");
		calculo.calcularItem(item);
		/* 0.34 (preço arredondado) x 1.5 = 0.51 */
		assertEquals(new BigDecimal("0.51"), item.getSubTotal());
	}

	@Test
	public void testCalcularItemQuantidadeInvalidaEDescontoMaiorQueSubtotal() {
		assertThrows(MsgApiException.class, () -> calculo.calcularItem(item("10", 0, "0")));
		assertThrows(MsgApiException.class, () -> calculo.calcularItem(item("10", 1, "-1")));
		assertThrows(MsgApiException.class, () -> calculo.calcularItem(item("10", 1, "10.01")));
	}

	@Test
	public void testCalcularPedido() {
		ItemPedido a = item("50", 3, "10");
		ItemPedido b = item("20", 1, "0");
		calculo.calcularItem(a);
		calculo.calcularItem(b);
		Pedido pedido = new Pedido();
		pedido.setDesconto(new BigDecimal("5"));
		pedido.setFrete(new BigDecimal("10"));
		pedido.setTaxas(new BigDecimal("1.50"));

		calculo.calcularPedido(pedido, List.of(a, b));

		assertEquals(new BigDecimal("160.00"), pedido.getSubTotal());
		assertEquals(new BigDecimal("166.50"), pedido.getTotal());
	}

	@Test
	public void testCalcularPedidoDescontoMaiorQueItensENegativos() {
		Pedido pedido = new Pedido();
		pedido.setDesconto(new BigDecimal("1"));
		assertThrows(MsgApiException.class, () -> calculo.calcularPedido(pedido, List.of()));

		Pedido negativo = new Pedido();
		negativo.setFrete(new BigDecimal("-1"));
		assertThrows(MsgApiException.class, () -> calculo.calcularPedido(negativo, List.of()));
	}

	@Test
	public void testPedidoSemItensENulosViramZero() {
		Pedido pedido = new Pedido();
		pedido.setSubTotal(null);
		pedido.setDesconto(null);
		pedido.setFrete(null);
		pedido.setTaxas(null);
		calculo.calcularPedido(pedido, List.of());
		assertEquals(new BigDecimal("0.00"), pedido.getTotal());
	}

	@Test
	public void testExigirEditavel() {
		Pedido pedido = new Pedido();
		pedido.setStatusPedido(StatusPedido.AGUARDANDO_PAGAMENTO);
		calculo.exigirEditavel(pedido);
		pedido.setStatusPedido(StatusPedido.PAGAMENTO_APROVADO);
		assertThrows(MsgApiException.class, () -> calculo.exigirEditavel(pedido));
	}

	@Test
	public void testMaquinaDeStatus() {
		assertTrue(StatusPedido.AGUARDANDO_PAGAMENTO.podeIrPara(StatusPedido.PAGAMENTO_APROVADO));
		assertTrue(StatusPedido.PAGAMENTO_RECUSADO.podeIrPara(StatusPedido.AGUARDANDO_PAGAMENTO));
		assertTrue(StatusPedido.ENTREGUE.podeIrPara(StatusPedido.DEVOLVIDO));
		assertFalse(StatusPedido.AGUARDANDO_PAGAMENTO.podeIrPara(StatusPedido.ENTREGUE));
		assertFalse(StatusPedido.PAGAMENTO_APROVADO.podeIrPara(StatusPedido.PAGAMENTO_APROVADO));
		assertFalse(StatusPedido.AGUARDANDO_PAGAMENTO.podeIrPara(null));
		/* Estados finais não têm saída */
		assertTrue(StatusPedido.CANCELADO.proximosPermitidos().isEmpty());
		assertTrue(StatusPedido.REEMBOLSADO.proximosPermitidos().isEmpty());
		/* Todo status é tratado pelo switch (nenhum lança exceção) */
		for (StatusPedido s : StatusPedido.values()) {
			s.proximosPermitidos();
		}
	}

	@Test
	public void testDeltaDeEstoque() {
		assertEquals(5.0, EstoquePedidoService.delta(TipoMovimentacaoProduto.ENTRADA_COMPRA, 5.0));
		assertEquals(-5.0, EstoquePedidoService.delta(TipoMovimentacaoProduto.SAIDA_VENDA, 5.0));
		assertEquals(0.0, EstoquePedidoService.delta(TipoMovimentacaoProduto.RESERVA_ESTOQUE, 5.0));
		assertEquals(0.0, EstoquePedidoService.delta(null, 5.0));
	}
}
