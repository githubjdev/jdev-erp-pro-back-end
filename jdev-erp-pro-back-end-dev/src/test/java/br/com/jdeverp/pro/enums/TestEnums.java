package br.com.jdeverp.pro.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Descrição de todos os enums e a classificação de TipoMovimentacaoProduto. */
public class TestEnums {

	static final List<Class<? extends Enum<?>>> ENUMS = List.of(FormaPagamento.class, PrioridadeChamado.class,
			StatusChamado.class, StatusPedido.class, TipoChamado.class, TipoClienteFuncionario.class,
			TipoMovimentacaoProduto.class, TipoPedido.class, TipoPessoa.class, TipoPlano.class, UnidadeMedida.class);

	@Test
	public void testGetDescricao() throws Exception {
		for (Class<? extends Enum<?>> tipo : ENUMS) {
			Method getDescricao = tipo.getMethod("getDescricao");
			for (Enum<?> valor : tipo.getEnumConstants()) {
				String descricao = (String) getDescricao.invoke(valor);
				assertNotNull(descricao, tipo.getSimpleName() + "." + valor.name());
				assertFalse(descricao.isBlank(), tipo.getSimpleName() + "." + valor.name());
			}
		}
	}

	@Test
	public void testTipoPessoaDescricao() {
		assertEquals("Física", TipoPessoa.FISICA.getDescricao());
		assertEquals("Jurídica", TipoPessoa.JURIDICA.getDescricao());
	}

	@Test
	public void testTipoMovimentacaoProdutoClassificacao() {
		for (TipoMovimentacaoProduto tipo : TipoMovimentacaoProduto.values()) {
			/* Cada tipo é exatamente uma coisa: entrada, saída ou reserva */
			long classificacoes = Arrays.asList(tipo.isEntrada(), tipo.isSaida(), tipo.isReserva()).stream().filter(b -> b).count();
			assertEquals(1, classificacoes, tipo.name());
		}

		assertTrue(TipoMovimentacaoProduto.ENTRADA_COMPRA.isEntrada());
		assertTrue(TipoMovimentacaoProduto.SAIDA_VENDA.isSaida());
		assertTrue(TipoMovimentacaoProduto.RESERVA_ESTOQUE.isReserva());
		assertTrue(TipoMovimentacaoProduto.LIBERACAO_RESERVA.isReserva());
		assertFalse(TipoMovimentacaoProduto.ENTRADA_INVENTARIO.isSaida());
	}

	@Test
	public void testTipoMovimentacaoProdutoToString() {
		/* toString devolve a descrição; o JSON usa o nome (ConfiguracaoJackson) */
		assertEquals("Saída por Venda", TipoMovimentacaoProduto.SAIDA_VENDA.toString());
		assertEquals(TipoMovimentacaoProduto.SAIDA_VENDA.getDescricao(), TipoMovimentacaoProduto.SAIDA_VENDA.toString());
	}

}
