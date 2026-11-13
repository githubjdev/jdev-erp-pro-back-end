package br.com.jdeverp.pro.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.jdeverp.pro.contexto.TestContextoSpring;

/**
 * Cada enum gravado como texto tem um CHECK no banco com os valores aceitos.
 * O ddl-auto=update não altera CHECK existente: um valor novo no enum sem
 * migração só quebraria em produção, na hora de gravar. Este teste compara os
 * valores do enum com os do CHECK (banco dev, só leitura).
 */
public class TestEnumsNoBanco extends TestContextoSpring {

	private static final Pattern VALOR = Pattern.compile("'([A-Z_]+)'");

	/** Constraint "tabela_coluna_check" e o enum da coluna. */
	private static final Map<String, Class<? extends Enum<?>>> CHECKS = Map.ofEntries(
			Map.entry("chamado_prioridade_chamado_check", PrioridadeChamado.class),
			Map.entry("chamado_status_chamado_check", StatusChamado.class),
			Map.entry("chamado_tipo_chamado_check", TipoChamado.class),
			Map.entry("cliente_funcionario_tipo_cliente_funcionario_check", TipoClienteFuncionario.class),
			Map.entry("movimentacao_produto_tipo_movimentacao_produto_check", TipoMovimentacaoProduto.class),
			Map.entry("pedido_forma_pagamento_check", FormaPagamento.class),
			Map.entry("pedido_status_pedido_check", StatusPedido.class),
			Map.entry("pedido_tipo_pedido_check", TipoPedido.class),
			Map.entry("pessoa_tipo_pessoa_check", TipoPessoa.class),
			Map.entry("plano_tipo_plano_check", TipoPlano.class),
			Map.entry("produto_unidade_medida_check", UnidadeMedida.class));

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	public void testValoresDosEnumsIguaisAosDoCheck() {
		/* Todos os enums de TestEnums têm CHECK */
		assertEquals(Set.copyOf(TestEnums.ENUMS), Set.copyOf(CHECKS.values()));

		CHECKS.forEach((constraint, tipo) -> {
			String definicao = jdbcTemplate.queryForObject("""
					SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c
					  JOIN pg_namespace n ON n.oid = c.connamespace
					 WHERE n.nspname = 'public' AND c.conname = ?""", String.class, constraint);

			Set<String> doBanco = new TreeSet<>();
			Matcher matcher = VALOR.matcher(definicao);
			while (matcher.find()) {
				doBanco.add(matcher.group(1));
			}
			Set<String> doEnum = new TreeSet<>(Arrays.stream(tipo.getEnumConstants()).map(Enum::name).toList());

			assertEquals(doEnum, doBanco, constraint + ": enum " + tipo.getSimpleName()
					+ " e CHECK do banco diferem. Valor novo no enum precisa de migração que recrie o CHECK.");
		});
	}

}
