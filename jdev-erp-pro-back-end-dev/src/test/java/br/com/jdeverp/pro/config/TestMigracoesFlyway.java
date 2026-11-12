package br.com.jdeverp.pro.config;

import static br.com.jdeverp.pro.contexto.BancoTemporarioTeste.mensagens;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.MigrateResult;
import org.flywaydb.core.api.output.ValidateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import br.com.jdeverp.pro.contexto.BancoTemporarioTeste;

/**
 * Migrações de {@code src/main/resources/db/migration} executadas pelo Flyway,
 * sempre num banco temporário ({@link BancoTemporarioTeste}). O banco dev só é
 * lido, para comparar a estrutura.
 */
public class TestMigracoesFlyway {

	static final List<String> VERSOES = List.of("20261112.160000", "20261112.161000", "20261112.162000");

	private static final String LOCAL_NOVA = "classpath:db/migracao-teste/nova";

	private static final String LOCAL_INVALIDA = "classpath:db/migracao-teste/invalida";

	/** Teste 1: banco vazio recebe todas as migrações, registradas no histórico, sem baseline. */
	@Test
	public void testMigrarBancoVazio() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("vazio")) {
			Flyway flyway = banco.flyway();
			assertEquals(VERSOES.size(), flyway.info().pending().length);

			MigrateResult resultado = flyway.migrate();

			assertTrue(resultado.success);
			assertEquals(VERSOES.size(), resultado.migrationsExecuted);
			assertEquals(0, flyway.info().pending().length);
			assertEquals("20261112.162000", flyway.info().current().getVersion().getVersion());

			List<String> versoes = new ArrayList<>();
			for (MigrationInfo info : flyway.info().applied()) {
				assertEquals("SQL", info.getType().name(), "banco vazio não recebe baseline");
				assertTrue(info.getState().isApplied() && !info.getState().isFailed());
				assertNotNull(info.getChecksum());
				assertNotNull(info.getInstalledOn());
				assertNotNull(info.getExecutionTime());
				versoes.add(info.getVersion().getVersion());
			}
			assertEquals(VERSOES, versoes);
			assertEquals(VERSOES.size(), banco.contar("SELECT count(*) FROM public.flyway_schema_history WHERE success"));

			/* Carga inicial (antigo V1_INIT) */
			assertEquals(2, banco.contar("SELECT count(*) FROM public.plano"));
			assertEquals(1, banco.contar("SELECT count(*) FROM public.empresa"));
			assertEquals(30, banco.contar("SELECT count(*) FROM public.categoria WHERE empresa_id = 1"));
			assertEquals(List.of("ROLE_ADMIN", "ROLE_USER"), banco.consultar("SELECT acesso FROM public.role ORDER BY acesso"));
			assertEquals(List.of("true"), banco.consultar("SELECT liberado::text FROM public.usuario WHERE id = 1"));
			assertEquals(2, banco.contar("SELECT last_value FROM public.seq_plano"), "a carga avança a sequence: o próximo plano é o 3");
			/* Antigo V2_PESSOA_DOCUMENTO_POR_TIPO */
			assertEquals(3, banco.contar("""
					SELECT count(*) FROM pg_constraint
					 WHERE conname IN ('pessoa_cpf_pessoa_fisica_check', 'pessoa_cnpj_pessoa_juridica_check', 'pessoa_email_preenchido_check')"""));
		}
	}

	/**
	 * Teste 2 (equivalência): as migrações produzem a mesma estrutura do banco
	 * dev, que existia antes do Flyway. É o que garante que o baseline do banco
	 * existente não esconde diferença. O banco dev é só lido.
	 */
	@Test
	public void testMigrarProduzEstruturaDoBancoDev() {
		String urlDev = BancoTemporarioTeste.getUrlBancoDoPerfil();
		String versaoDoDev = versaoAtual(urlDev);

		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("igual_dev")) {
			banco.configuracaoFlyway().target(versaoDoDev).load().migrate();

			List<String> esperado = BancoTemporarioTeste.estrutura(urlDev);
			List<String> atual = banco.estrutura();
			assertEquals(esperado, atual, "Migrações até " + versaoDoDev + " diferem do banco dev. "
					+ "Entidade alterada sem migração? " + BancoTemporarioTeste.diferencas(esperado, atual));
		}
	}

	/** Testes 3 e 8: rodar de novo não repete migração nem mexe no histórico. */
	@Test
	public void testMigrarDeNovoNaoRepete() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("reexecucao")) {
			banco.flyway().migrate();
			List<String> historico = banco.historicoFlyway();
			List<String> estrutura = banco.estrutura();

			for (int i = 0; i < 2; i++) {
				Flyway flyway = banco.flyway();
				MigrateResult resultado = flyway.migrate();
				assertEquals(0, resultado.migrationsExecuted);
				assertTrue(flyway.validateWithResult().validationSuccessful);
				assertEquals(0, flyway.info().pending().length);
			}

			assertEquals(historico, banco.historicoFlyway());
			assertEquals(estrutura, banco.estrutura());
			assertEquals(2, banco.contar("SELECT count(*) FROM public.plano"), "a carga inicial não rodou de novo");
		}
	}

	/** Teste 4: migração nova (só nos recursos de teste) roda depois das existentes. */
	@Test
	public void testMigrarNovaMigracao() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("nova")) {
			banco.flyway().migrate();

			Flyway flyway = banco.configuracaoFlyway().locations("classpath:db/migration", LOCAL_NOVA).load();
			assertEquals(1, flyway.info().pending().length);

			MigrateResult resultado = flyway.migrate();

			assertEquals(1, resultado.migrationsExecuted);
			assertEquals("20991231.235900", flyway.info().current().getVersion().getVersion());
			assertEquals(List.of("aplicada pelo Flyway"),
					banco.consultar("SELECT descricao FROM public.flyway_teste_nova_migracao"));
			assertEquals(VERSOES.size() + 1, banco.historicoFlyway().size());
		}
	}

	/** Teste 5: script já aplicado e depois alterado é recusado pela validação (checksum). */
	@Test
	public void testValidarChecksumAlterado(@TempDir Path pasta) throws IOException {
		copiarMigracoes(pasta);
		String local = "filesystem:" + pasta.toAbsolutePath().toString().replace('\\', '/');

		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("checksum")) {
			banco.configuracaoFlyway().locations(local).load().migrate();
			List<String> historico = banco.historicoFlyway();

			Files.writeString(pasta.resolve("V20261112_162000__pessoa_documento_por_tipo.sql"),
					System.lineSeparator() + "-- alteração depois de aplicada" + System.lineSeparator(),
					StandardCharsets.UTF_8, StandardOpenOption.APPEND);

			Flyway flyway = banco.configuracaoFlyway().locations(local).load();
			ValidateResult validacao = flyway.validateWithResult();
			assertFalse(validacao.validationSuccessful);
			assertTrue(validacao.getAllErrorMessages().toLowerCase().contains("checksum mismatch"),
					validacao.getAllErrorMessages());
			assertTrue(validacao.getAllErrorMessages().contains("20261112.162000"), validacao.getAllErrorMessages());

			FlywayException erro = assertThrows(FlywayException.class, flyway::migrate);
			assertTrue(mensagens(erro).toLowerCase().contains("checksum mismatch"), mensagens(erro));
			assertEquals(historico, banco.historicoFlyway(), "a validação falhou antes de mexer no histórico");
		}
	}

	/** Teste 6: migração com SQL inválido falha, é desfeita por inteiro e não entra no histórico. */
	@Test
	public void testMigrarMigracaoInvalida() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("invalida")) {
			banco.flyway().migrate();
			List<String> historico = banco.historicoFlyway();

			Flyway flyway = banco.configuracaoFlyway().locations("classpath:db/migration", LOCAL_INVALIDA).load();
			FlywayException erro = assertThrows(FlywayException.class, flyway::migrate);

			assertTrue(mensagens(erro).contains("tabela_que_nao_existe"), mensagens(erro));
			assertFalse(banco.existeTabela("flyway_teste_migracao_invalida"), "a primeira instrução foi desfeita");
			assertEquals(historico, banco.historicoFlyway(), "migração com erro não fica registrada");
			assertEquals(1, flyway.info().pending().length, "continua pendente");
		}
	}

	/** A carga inicial (apaga plano, empresa e pessoa) se recusa a rodar num banco com dados. */
	@Test
	public void testMigrarCargaInicialRecusaBancoComDados() {
		try (BancoTemporarioTeste banco = BancoTemporarioTeste.criar("carga_com_dados")) {
			banco.configuracaoFlyway().target("20261112.160000").load().migrate();
			banco.executar("""
					INSERT INTO public.plano (id, ativo, descricao, limite_cliente, limite_usuario, nome, tipo_plano, valor_mensal)
					VALUES (99, true, 'Plano já existente', 5, 5, 'Existente', 'PRO', 99)""");

			FlywayException erro = assertThrows(FlywayException.class, () -> banco.flyway().migrate());

			assertTrue(mensagens(erro).contains("Carga inicial recusada"), mensagens(erro));
			assertEquals(List.of("Plano já existente"), banco.consultar("SELECT descricao FROM public.plano"));
			assertEquals("20261112.160000", banco.flyway().info().current().getVersion().getVersion());
		}
	}

	/**
	 * Teste 7 (classpath): as migrações estão no classpath, com o nome no padrão
	 * {@code VYYYYMMDD_HHMMSS__descricao_em_minusculas.sql}, data e hora válidas
	 * e versões sem repetição. O conteúdo do JAR é conferido no build (ver
	 * docs/Flyway.md).
	 */
	@Test
	public void testNomesDasMigracoes() throws IOException {
		Pattern padrao = Pattern.compile("^V(\\d{8})_(\\d{6})__([a-z0-9]+(?:_[a-z0-9]+)*)\\.sql$");
		DateTimeFormatter formato = DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);

		Resource[] arquivos = new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/*");
		Set<String> versoes = new HashSet<>();
		List<String> nomes = new ArrayList<>();
		for (Resource arquivo : arquivos) {
			String nome = arquivo.getFilename();
			Matcher m = padrao.matcher(nome);
			assertTrue(m.matches(), "Nome fora do padrão VYYYYMMDD_HHMMSS__descricao.sql: " + nome);
			LocalDateTime.parse(m.group(1) + m.group(2), formato);
			assertTrue(versoes.add(m.group(1) + "." + m.group(2)), "Versão repetida: " + nome);
			nomes.add(nome);
		}

		assertTrue(nomes.containsAll(List.of("V20261112_160000__criacao_estrutura_inicial.sql",
				"V20261112_161000__carga_inicial.sql", "V20261112_162000__pessoa_documento_por_tipo.sql")), nomes.toString());
	}

	/** Versão atual de um banco pelo Flyway; sem histórico, a versão de baseline padrão. */
	static String versaoAtual(String url) {
		boolean temHistorico = !BancoTemporarioTeste.consultar(url, """
				SELECT 1 FROM information_schema.tables
				 WHERE table_schema = 'public' AND table_name = 'flyway_schema_history'""").isEmpty();
		if (!temHistorico) {
			return BancoTemporarioTeste.VERSAO_BASELINE_PADRAO;
		}
		return BancoTemporarioTeste.consultar(url, """
				SELECT version FROM public.flyway_schema_history
				 WHERE success AND version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1""").get(0);
	}

	private static void copiarMigracoes(Path pasta) throws IOException {
		for (Resource arquivo : new PathMatchingResourcePatternResolver().getResources("classpath:db/migration/*.sql")) {
			try (InputStream in = arquivo.getInputStream()) {
				Files.copy(in, pasta.resolve(arquivo.getFilename()));
			}
		}
	}

}
