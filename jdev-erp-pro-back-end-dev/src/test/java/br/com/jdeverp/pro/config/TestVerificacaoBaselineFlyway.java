package br.com.jdeverp.pro.config;

import static br.com.jdeverp.pro.contexto.BancoTemporarioTeste.mensagens;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.callback.Event;
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

			Flyway flyway = banco.flyway();
			MigrateResult resultado = flyway.migrate();

			assertEquals(0, resultado.migrationsExecuted, "nenhuma migração roda sobre o banco existente");
			List<String> historico = banco.consultar("SELECT version || '|' || type FROM public.flyway_schema_history");
			assertEquals(List.of("20261112.162000|BASELINE"), historico);
			assertEquals(0, flyway.info().pending().length);
			assertEquals(dados, banco.consultar(CONTAGEM_DADOS), "nenhum dado apagado nem incluído");
			assertEquals(estrutura, banco.estrutura(), "nenhum objeto recriado ou alterado");

			/* Segunda subida: histórico já existe, não há baseline de novo. */
			assertEquals(0, banco.flyway().migrate().migrationsExecuted);
			assertEquals(historico, banco.consultar("SELECT version || '|' || type FROM public.flyway_schema_history"));
		}
	}

	/** Banco só com o V1 (o V2 nunca foi aplicado): baseline em 20261112.161000, e o Flyway aplica o V2. */
	@Test
	public void testHandleBancoSemV2ComBaselineNaCargaInicial() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("sem_v2")) {
			banco.prepararComoBancoAnteriorAoFlyway("20261112.161000");
			List<String> dados = banco.consultar(CONTAGEM_DADOS);

			MigrateResult resultado = banco.configuracaoFlyway().baselineVersion("20261112.161000").load().migrate();

			assertEquals(1, resultado.migrationsExecuted);
			assertEquals(List.of("20261112.161000|BASELINE", "20261112.162000|SQL"),
					banco.consultar("SELECT version || '|' || type FROM public.flyway_schema_history ORDER BY installed_rank"));
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
