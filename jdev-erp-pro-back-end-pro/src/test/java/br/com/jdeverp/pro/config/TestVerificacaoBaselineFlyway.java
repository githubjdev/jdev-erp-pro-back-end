package br.com.jdeverp.pro.config;

import static br.com.jdeverp.pro.contexto.BancoTemporarioTeste.mensagens;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.BancoTemporarioTeste;

/**
 * Teste 2: adoção do Flyway em banco existente (criado antes do Flyway, sem
 * {@code flyway_schema_history}). O banco "existente" é simulado num banco
 * temporário: as migrações até a versão desejada e, depois, a tabela de
 * histórico apagada, como num banco em que os scripts antigos foram rodados à
 * mão.
 */
public class TestVerificacaoBaselineFlyway {

	private static final String CONTAGEM_DADOS = """
			SELECT (SELECT count(*) FROM public.plano) || ',' || (SELECT count(*) FROM public.empresa) || ','
			       || (SELECT count(*) FROM public.pessoa) || ',' || (SELECT count(*) FROM public.usuario) || ','
			       || (SELECT count(*) FROM public.categoria) || ',' || (SELECT count(*) FROM public.role)""";

	private static List<String> historico(BancoTemporarioTeste banco) {
		return banco.consultar("SELECT version || '|' || type FROM public.flyway_schema_history ORDER BY installed_rank");
	}

	/** Histórico de um banco com baseline em {@code versao}: o baseline e as migrações criadas depois do Flyway. */
	private static List<String> esperadoDepoisDoBaseline(String versao) {
		List<String> esperado = new ArrayList<>(List.of(versao + "|BASELINE"));
		TestMigracoesFlyway.VERSOES_POSTERIORES_AO_BASELINE.forEach(v -> esperado.add(v + "|SQL"));
		return esperado;
	}

	@Test
	public void testSupports() {
		VerificacaoBaselineFlyway verificacao = new VerificacaoBaselineFlyway();
		assertTrue(verificacao.supports(Event.BEFORE_BASELINE, null));
		assertFalse(verificacao.supports(Event.BEFORE_MIGRATE, null));
		assertFalse(verificacao.supports(Event.BEFORE_EACH_MIGRATE, null));
	}

	/** Banco com V1 e V2 aplicados à mão: baseline em 20261112.162000, nada executado, nada recriado. */
	@Test
	public void testHandleBancoComScriptsAntigos() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("existente")) {
			banco.prepararComoBancoAnteriorAoFlyway("20261112.162000");
			banco.executar("INSERT INTO public.categoria (id, nome, empresa_id) VALUES (nextval('seq_categoria'), 'Cadastrada em uso', 1)");
			List<String> dados = banco.consultar(CONTAGEM_DADOS);
			List<String> estrutura = banco.estrutura();

			/* Até a versão do baseline: só o baseline, nenhum script antigo roda de novo */
			MigrateResult resultado = banco.configuracaoFlyway().target("20261112.162000").load().migrate();

			assertEquals(0, resultado.migrationsExecuted, "nenhuma migração antiga roda sobre o banco existente");
			assertEquals(List.of("20261112.162000|BASELINE"), historico(banco));
			assertEquals(dados, banco.consultar(CONTAGEM_DADOS), "nenhum dado apagado nem incluído");
			assertEquals(estrutura, banco.estrutura(), "nenhum objeto recriado ou alterado");

			/* Subida normal: só as migrações criadas depois da adoção do Flyway rodam */
			Flyway flyway = banco.flyway();
			resultado = flyway.migrate();
			assertEquals(TestMigracoesFlyway.VERSOES_POSTERIORES_AO_BASELINE.size(), resultado.migrationsExecuted);
			List<String> historico = historico(banco);
			assertEquals(esperadoDepoisDoBaseline("20261112.162000"), historico);
			assertEquals(0, flyway.info().pending().length);
			assertEquals(dados, banco.consultar(CONTAGEM_DADOS), "nenhum dado apagado nem incluído");

			/* Outra subida: histórico já existe, não há baseline de novo nem migração repetida. */
			assertEquals(0, banco.flyway().migrate().migrationsExecuted);
			assertEquals(historico, historico(banco));
		}
	}

	/** Banco só com o V1 (o V2 nunca foi aplicado): baseline em 20261112.161000, e o Flyway aplica o V2. */
	@Test
	public void testHandleBancoSemV2ComBaselineNaCargaInicial() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("sem_v2")) {
			banco.prepararComoBancoAnteriorAoFlyway("20261112.161000");
			List<String> dados = banco.consultar(CONTAGEM_DADOS);

			MigrateResult resultado = banco.configuracaoFlyway().baselineVersion("20261112.161000").load().migrate();

			assertEquals(1 + TestMigracoesFlyway.VERSOES_POSTERIORES_AO_BASELINE.size(), resultado.migrationsExecuted);
			List<String> esperado = new ArrayList<>(List.of("20261112.161000|BASELINE", "20261112.162000|SQL"));
			TestMigracoesFlyway.VERSOES_POSTERIORES_AO_BASELINE.forEach(versao -> esperado.add(versao + "|SQL"));
			assertEquals(esperado, historico(banco));
			assertEquals(3, banco.contar("""
					SELECT count(*) FROM pg_constraint
					 WHERE conname IN ('pessoa_cpf_pessoa_fisica_check', 'pessoa_cnpj_pessoa_juridica_check', 'pessoa_email_preenchido_check')"""));
			assertEquals(dados, banco.consultar(CONTAGEM_DADOS));
		}
	}

	/** Banco sem o V2 com o baseline padrão (que declara o V2 aplicado): recusado, nada gravado. */
	@Test
	public void testHandleRecusaBancoSemV2() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("recusa_v2")) {
			banco.prepararComoBancoAnteriorAoFlyway("20261112.161000");
			List<String> estrutura = banco.estrutura();

			FlywayException erro = assertThrows(FlywayException.class, () -> banco.flyway().migrate());

			String texto = mensagens(erro);
			assertTrue(texto.contains("Baseline do Flyway recusado na versão 20261112.162000"), texto);
			assertTrue(texto.contains("pessoa_cpf_pessoa_fisica_check"), texto);
			assertTrue(texto.contains("SPRING_FLYWAY_BASELINE_VERSION=20261112.161000"), texto);
			assertFalse(banco.existeTabela("flyway_schema_history"), "baseline recusado não grava histórico");
			assertEquals(estrutura, banco.estrutura());
		}
	}

	/** Estrutura criada sem a carga inicial (ex.: só o Hibernate): recusado, com a lista do que falta. */
	@Test
	public void testHandleRecusaBancoSemCargaInicial() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("sem_carga")) {
			banco.prepararComoBancoAnteriorAoFlyway("20261112.160000");

			FlywayException erro = assertThrows(FlywayException.class, () -> banco.flyway().migrate());

			String texto = mensagens(erro);
			assertTrue(texto.contains("plano_valor_mensal_check"), texto);
			assertTrue(texto.contains("unaccent"), texto);
			assertTrue(texto.contains("usuario.liberado ausente"), texto);
			assertTrue(texto.contains("role ROLE_ADMIN ausente"), texto);
			assertFalse(banco.existeTabela("flyway_schema_history"));
		}
	}

	/** Banco que não é do sistema (tabelas faltando): recusado. */
	@Test
	public void testHandleRecusaBancoDeOutroSistema() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("outro")) {
			banco.executar("CREATE TABLE public.cliente_de_outro_sistema (id bigint PRIMARY KEY)");

			FlywayException erro = assertThrows(FlywayException.class, () -> banco.flyway().migrate());

			String texto = mensagens(erro);
			assertTrue(texto.contains("tabela ausente: usuario"), texto);
			assertTrue(texto.contains("sequence ausente: seq_usuario"), texto);
			assertFalse(banco.existeTabela("flyway_schema_history"));
		}
	}

	/** Cada item que os scripts antigos deixam no banco, alterado: todos aparecem na recusa. */
	@Test
	public void testHandleListaCadaDivergencia() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("divergencias")) {
			banco.prepararComoBancoAnteriorAoFlyway("20261112.162000");
			banco.executar("""
					ALTER TABLE public.plano DROP CONSTRAINT plano_valor_mensal_check;
					ALTER TABLE public.plano ADD CONSTRAINT plano_valor_mensal_check CHECK (valor_mensal >= 0);
					DROP EXTENSION unaccent;
					ALTER TABLE public.usuario RENAME COLUMN liberado TO liberado_antigo;
					ALTER TABLE public.usuario ADD COLUMN bloqueado boolean;
					ALTER TABLE public.usuario ALTER COLUMN token_sessao TYPE character varying(255);
					ALTER TABLE public.cliente_funcionario ALTER COLUMN usuario_id SET NOT NULL;
					DELETE FROM public.role WHERE acesso = 'ROLE_USER';
					ALTER TABLE public.pessoa DROP CONSTRAINT pessoa_email_preenchido_check;
					ALTER TABLE public.pessoa ALTER COLUMN email DROP NOT NULL;
					ALTER TABLE public.pessoa ALTER COLUMN cpf SET NOT NULL;
					""");

			FlywayException erro = assertThrows(FlywayException.class, () -> banco.flyway().migrate());

			String texto = mensagens(erro);
			for (String esperado : List.of("plano_valor_mensal_check com a regra do plano FREE ausente ou diferente",
					"extensão unaccent não instalada", "usuario.liberado ausente", "usuario.bloqueado ainda existe",
					"usuario.token_sessao ausente ou com tipo diferente de text", "cliente_funcionario.usuario_id ausente ou NOT NULL",
					"role ROLE_USER ausente", "pessoa_email_preenchido_check ausente", "pessoa.email ausente ou sem NOT NULL",
					"pessoa.cpf ausente ou ainda NOT NULL")) {
				assertTrue(texto.contains(esperado), "faltou \"" + esperado + "\" em: " + texto);
			}
			assertFalse(texto.contains("usuario.refresh_token"), "refresh_token continua text: " + texto);
			assertFalse(banco.existeTabela("flyway_schema_history"));
		}
	}

	/** Falha de banco no meio da conferência: o baseline é recusado com o motivo, sem gravar nada. */
	@Test
	public void testHandleErroDeBanco() throws Exception {
		Configuration configuracao = mock(Configuration.class);
		when(configuracao.getBaselineVersion()).thenReturn(VerificacaoBaselineFlyway.VERSAO_PESSOA_DOCUMENTO_POR_TIPO);
		Connection conexao = mock(Connection.class);
		when(conexao.prepareStatement(anyString())).thenThrow(new SQLException("conexão perdida"));
		Context contexto = mock(Context.class);
		when(contexto.getConfiguration()).thenReturn(configuracao);
		when(contexto.getConnection()).thenReturn(conexao);

		FlywayException erro = assertThrows(FlywayException.class,
				() -> new VerificacaoBaselineFlyway().handle(Event.BEFORE_BASELINE, contexto));

		assertEquals("Baseline do Flyway recusado: não foi possível conferir o banco. conexão perdida", erro.getMessage());
	}

	@Test
	public void testGetCallbackName() {
		VerificacaoBaselineFlyway verificacao = new VerificacaoBaselineFlyway();

		assertEquals("VerificacaoBaselineFlyway", verificacao.getCallbackName());
		assertTrue(verificacao.canHandleInTransaction(Event.BEFORE_BASELINE, null));
	}

	/** Versão de baseline abaixo da carga inicial rodaria a carga (que apaga dados) num banco existente: recusada. */
	@Test
	public void testHandleRecusaVersaoNaoAceita() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("versao")) {
			banco.prepararComoBancoAnteriorAoFlyway("20261112.162000");
			List<String> dados = banco.consultar(CONTAGEM_DADOS);

			for (String versao : List.of("1", "20261112.160000", "20261201.000000")) {
				FlywayException erro = assertThrows(FlywayException.class,
						() -> banco.configuracaoFlyway().baselineVersion(versao).load().migrate());
				assertTrue(mensagens(erro).contains("não aceita"), mensagens(erro));
			}

			assertFalse(banco.existeTabela("flyway_schema_history"));
			assertEquals(dados, banco.consultar(CONTAGEM_DADOS));
		}
	}

}
