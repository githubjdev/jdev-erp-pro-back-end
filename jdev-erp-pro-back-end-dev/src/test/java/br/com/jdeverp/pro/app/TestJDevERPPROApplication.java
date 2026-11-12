package br.com.jdeverp.pro.app;

import static br.com.jdeverp.pro.contexto.BancoTemporarioTeste.mensagens;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import br.com.jdeverp.pro.contexto.BancoTemporarioTeste;

/**
 * Subida da aplicação inteira (Flyway, Hibernate com {@code ddl-auto=update},
 * Tomcat numa porta livre) com a configuração real dos properties, apontando
 * para bancos temporários. Cobre os testes 1, 2, 3, 6 e 8 do Flyway no fluxo da
 * aplicação: o Flyway roda antes do Hibernate e um erro impede a subida.
 */
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class TestJDevERPPROApplication {

	private BancoTemporarioTeste banco;

	private List<String> historico;

	private List<String> estrutura;

	@BeforeAll
	public void criarBanco() {
		banco = BancoTemporarioTeste.criar("app");
	}

	@AfterAll
	public void apagarBanco() {
		if (banco != null) {
			banco.close();
		}
	}

	/** Teste 1: banco vazio. O Flyway aplica tudo e o Hibernate não precisa criar nada. */
	@Test
	@Order(1)
	public void testMainBancoVazio() {
		try (ConfigurableApplicationContext contexto = subir(banco)) {
			Flyway flyway = contexto.getBean(Flyway.class);
			assertEquals(3, flyway.info().applied().length);
			assertEquals(0, flyway.info().pending().length);
			assertEquals("20261112.162000", flyway.info().current().getVersion().getVersion());
		}

		historico = banco.historicoFlyway();
		estrutura = banco.estrutura();
		assertEquals(3, historico.size());

		try (BancoTemporarioTeste soFlyway = BancoTemporarioTeste.criar("so_flyway")) {
			soFlyway.flyway().migrate();
			assertEquals(soFlyway.estrutura(), estrutura, "O Hibernate (ddl-auto=update) mudou o banco depois do Flyway: "
					+ "entidade e migrações divergem. " + BancoTemporarioTeste.diferencas(soFlyway.estrutura(), estrutura));
		}
	}

	/** Testes 3 e 8: segunda subida não executa nada e não muda histórico nem estrutura. */
	@Test
	@Order(2)
	public void testMainSegundaInicializacao() {
		for (int subida = 0; subida < 2; subida++) {
			try (ConfigurableApplicationContext contexto = subir(banco)) {
				assertEquals(0, contexto.getBean(Flyway.class).info().pending().length);
			}
		}

		assertEquals(historico, banco.historicoFlyway());
		assertEquals(estrutura, banco.estrutura());
	}

	/** Teste 6: migração com erro impede a subida e não deixa rastro. */
	@Test
	@Order(3)
	public void testMainMigracaoInvalidaImpedeSubida() {
		Exception erro = assertThrows(Exception.class, () -> subir(banco,
				"--spring.flyway.locations=classpath:db/migration,classpath:db/migracao-teste/invalida").close());

		assertTrue(mensagens(erro).contains("tabela_que_nao_existe"), mensagens(erro));
		assertEquals(historico, banco.historicoFlyway());
		assertFalse(banco.existeTabela("flyway_teste_migracao_invalida"));
	}

	/** Teste 2: banco existente com os scripts antigos. Sobe com baseline, sem executar nem apagar nada. */
	@Test
	@Order(4)
	public void testMainBancoExistente() {
		try (BancoTemporarioTeste existente = BancoTemporarioTeste.criar("app_existente")) {
			existente.prepararComoBancoAnteriorAoFlyway("20261112.162000");
			existente.executar("INSERT INTO public.categoria (id, nome, empresa_id) VALUES (nextval('seq_categoria'), 'Cadastrada em uso', 1)");
			List<String> estruturaAntes = existente.estrutura();
			int categorias = existente.contar("SELECT count(*) FROM public.categoria");

			try (ConfigurableApplicationContext contexto = subir(existente)) {
				assertEquals(0, contexto.getBean(Flyway.class).info().pending().length);
			}

			assertEquals(List.of("20261112.162000|BASELINE"),
					existente.consultar("SELECT version || '|' || type FROM public.flyway_schema_history"));
			assertEquals(estruturaAntes, existente.estrutura());
			assertEquals(categorias, existente.contar("SELECT count(*) FROM public.categoria"));
		}
	}

	/** Banco existente que não bate com a versão de baseline: a aplicação não sobe e nada é gravado. */
	@Test
	@Order(5)
	public void testMainBancoExistenteDivergenteNaoSobe() {
		try (BancoTemporarioTeste existente = BancoTemporarioTeste.criar("app_divergente")) {
			existente.prepararComoBancoAnteriorAoFlyway("20261112.161000");

			Exception erro = assertThrows(Exception.class, () -> subir(existente).close());

			assertTrue(mensagens(erro).contains("Baseline do Flyway recusado"), mensagens(erro));
			assertFalse(existente.existeTabela("flyway_schema_history"));
		}
	}

	private static ConfigurableApplicationContext subir(BancoTemporarioTeste banco, String... argumentos) {
		List<String> args = new ArrayList<>(List.of(
				"--spring.datasource.url=" + banco.getUrl(),
				"--server.port=0",
				"--spring.main.banner-mode=off"));
		args.addAll(List.of(argumentos));
		return new SpringApplicationBuilder(JDevERPPROApplication.class).run(args.toArray(String[]::new));
	}

}
