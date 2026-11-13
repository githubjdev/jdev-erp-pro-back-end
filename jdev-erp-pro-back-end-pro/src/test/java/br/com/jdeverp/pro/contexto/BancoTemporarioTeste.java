package br.com.jdeverp.pro.contexto;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import br.com.jdeverp.pro.config.VerificacaoBaselineFlyway;

/**
 * Banco PostgreSQL temporário para os testes do Flyway.
 *
 * <p>Os testes do Flyway precisam de banco vazio, de banco "anterior ao
 * Flyway" e de migrações quebradas. Nada disso pode rodar no banco dev. Como
 * não há Docker na máquina (Testcontainers fica de fora), cada teste cria um
 * banco próprio no mesmo servidor do perfil ativo, com o prefixo
 * {@value #PREFIXO}, e o apaga no {@link #close()}. O {@code close()} só apaga
 * banco com esse prefixo, criado por esta classe.
 *
 * <p>A conexão vem do {@code application-<perfil>.properties} (perfil de
 * {@code SPRING_PROFILES_JDEV_ERP_PRO}, padrão {@code dev}) ou das variáveis
 * {@code SPRING_DATASOURCE_URL}, {@code SPRING_DATASOURCE_USERNAME} e
 * {@code SPRING_DATASOURCE_PASSWORD}.
 */
public class BancoTemporarioTeste implements AutoCloseable {

	public static final String PREFIXO = "jdev_flyway_teste_";

	/** Mesma versão de {@code spring.flyway.baseline-version} nos properties. */
	public static final String VERSAO_BASELINE_PADRAO = "20261112.162000";

	private static final Pattern URL_POSTGRES = Pattern.compile("^(jdbc:postgresql://[^/]+/)([^?]+)(\\?.*)?$");

	private static final AtomicInteger SEQUENCIA = new AtomicInteger();

	private static final Properties CONFIGURACAO = carregarConfiguracao();

	private final String nome;

	private final String url;

	private BancoTemporarioTeste(String nome) {
		this.nome = nome;
		this.url = urlDoBanco(nome);
	}

	/** Cria um banco vazio (a partir do template0). */
	public static BancoTemporarioTeste criar(String apelido) {
		String nome = (PREFIXO + apelido + "_" + System.currentTimeMillis() + "_" + SEQUENCIA.incrementAndGet())
				.toLowerCase(Locale.ROOT);
		if (!nome.matches("[a-z0-9_]{1,63}")) {
			throw new IllegalArgumentException("Nome de banco temporário inválido: " + nome);
		}
		executarNoServidor("CREATE DATABASE " + nome + " TEMPLATE template0 ENCODING 'UTF8'");
		return new BancoTemporarioTeste(nome);
	}

	public String getNome() {
		return nome;
	}

	public String getUrl() {
		return url;
	}

	public static String getUsuario() {
		return CONFIGURACAO.getProperty("spring.datasource.username");
	}

	public static String getSenha() {
		return CONFIGURACAO.getProperty("spring.datasource.password");
	}

	/** URL do banco configurado no perfil (o banco dev). Os testes só leem dele. */
	public static String getUrlBancoDoPerfil() {
		return CONFIGURACAO.getProperty("spring.datasource.url");
	}

	/**
	 * Flyway com a mesma configuração dos {@code application-*.properties}:
	 * {@code classpath:db/migration}, validação na migração e de nomes, sem
	 * clean, sem out-of-order, baseline automático em
	 * {@value #VERSAO_BASELINE_PADRAO} conferido por
	 * {@link VerificacaoBaselineFlyway}.
	 */
	public FluentConfiguration configuracaoFlyway() {
		return Flyway.configure()
				.dataSource(url, getUsuario(), getSenha())
				.locations("classpath:db/migration")
				.failOnMissingLocations(true)
				.validateOnMigrate(true)
				.validateMigrationNaming(true)
				.outOfOrder(false)
				.cleanDisabled(true)
				.baselineOnMigrate(true)
				.baselineVersion(VERSAO_BASELINE_PADRAO)
				.baselineDescription("Banco anterior ao Flyway (scripts aplicados manualmente)")
				.callbacks(new VerificacaoBaselineFlyway());
	}

	public Flyway flyway() {
		return configuracaoFlyway().load();
	}

	/**
	 * Simula um banco criado antes do Flyway: aplica as migrações até
	 * {@code versao} (o mesmo SQL dos scripts antigos) e apaga a
	 * {@code flyway_schema_history} deste banco temporário.
	 */
	public void prepararComoBancoAnteriorAoFlyway(String versao) {
		configuracaoFlyway().target(versao).load().migrate();
		executar("DROP TABLE public.flyway_schema_history");
	}

	public Connection conectar() throws SQLException {
		return DriverManager.getConnection(url, getUsuario(), getSenha());
	}

	/** Executa SQL com commit (o pool da aplicação usa auto-commit=false, aqui é conexão direta). */
	public void executar(String sql) {
		try (Connection con = conectar(); Statement st = con.createStatement()) {
			con.setAutoCommit(true);
			st.execute(sql);
		} catch (SQLException e) {
			throw new IllegalStateException("Erro ao executar no banco temporário " + nome + ": " + sql, e);
		}
	}

	public List<String> consultar(String sql) {
		return consultar(url, sql);
	}

	public int contar(String sql) {
		return Integer.parseInt(consultar(sql).get(0));
	}

	public boolean existeTabela(String tabela) {
		return contar("SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '"
				+ tabela + "'") > 0;
	}

	/** Linhas da flyway_schema_history, na ordem de instalação. */
	public List<String> historicoFlyway() {
		return consultar("""
				SELECT installed_rank || '|' || coalesce(version, '') || '|' || description || '|' || type || '|' || script
				       || '|' || coalesce(checksum::text, '') || '|' || installed_on || '|' || execution_time || '|' || success
				  FROM public.flyway_schema_history ORDER BY installed_rank""");
	}

	public List<String> estrutura() {
		return estrutura(url);
	}

	/**
	 * Retrato da estrutura do schema public (tabelas, colunas, constraints com a
	 * definição, índices, sequences, extensões, funções e triggers próprios),
	 * sem a {@code flyway_schema_history} e sem dados. Serve para comparar dois
	 * bancos.
	 */
	public static List<String> estrutura(String url) {
		return consultar(url, """
				SELECT 'tabela ' || table_name || ' ' || table_type
				  FROM information_schema.tables
				 WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
				UNION ALL
				SELECT 'coluna ' || table_name || '.' || column_name || ' ' || data_type
				       || coalesce('(' || character_maximum_length || ')', '')
				       || coalesce(' precisao=' || numeric_precision || ',' || numeric_scale, '')
				       || ' nulo=' || is_nullable || ' default=' || coalesce(column_default, '-')
				  FROM information_schema.columns
				 WHERE table_schema = 'public' AND table_name <> 'flyway_schema_history'
				UNION ALL
				SELECT 'constraint ' || t.relname || '.' || c.conname || ' ' || pg_get_constraintdef(c.oid)
				  FROM pg_constraint c
				  JOIN pg_class t ON t.oid = c.conrelid
				  JOIN pg_namespace n ON n.oid = t.relnamespace
				 WHERE n.nspname = 'public' AND t.relname <> 'flyway_schema_history'
				UNION ALL
				SELECT 'indice ' || indexdef
				  FROM pg_indexes
				 WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'
				UNION ALL
				SELECT 'sequence ' || sequencename || ' ' || data_type || ' inicio=' || start_value || ' incremento=' || increment_by
				       || ' min=' || min_value || ' max=' || max_value || ' cache=' || cache_size
				  FROM pg_sequences
				 WHERE schemaname = 'public'
				UNION ALL
				SELECT 'extensao ' || extname FROM pg_extension WHERE extname <> 'plpgsql'
				UNION ALL
				SELECT 'funcao ' || p.proname || '(' || pg_get_function_identity_arguments(p.oid) || ')'
				  FROM pg_proc p
				  JOIN pg_namespace n ON n.oid = p.pronamespace
				 WHERE n.nspname = 'public'
				   AND NOT EXISTS (SELECT 1 FROM pg_depend d WHERE d.objid = p.oid AND d.deptype = 'e')
				UNION ALL
				SELECT 'trigger ' || c.relname || '.' || t.tgname
				  FROM pg_trigger t
				  JOIN pg_class c ON c.oid = t.tgrelid
				  JOIN pg_namespace n ON n.oid = c.relnamespace
				 WHERE n.nspname = 'public' AND NOT t.tgisinternal
				ORDER BY 1""");
	}

	/** Diferenças entre dois retratos de {@link #estrutura(String)}, para a mensagem do teste. */
	public static String diferencas(List<String> esperado, List<String> atual) {
		List<String> faltando = new ArrayList<>(esperado);
		faltando.removeAll(atual);
		List<String> sobrando = new ArrayList<>(atual);
		sobrando.removeAll(esperado);
		return "Faltando: " + faltando + System.lineSeparator() + "Sobrando: " + sobrando;
	}

	public static List<String> consultar(String url, String sql) {
		List<String> linhas = new ArrayList<>();
		try (Connection con = DriverManager.getConnection(url, getUsuario(), getSenha());
				Statement st = con.createStatement();
				ResultSet rs = st.executeQuery(sql)) {
			while (rs.next()) {
				linhas.add(rs.getString(1));
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Erro ao consultar " + url + ": " + sql, e);
		}
		return linhas;
	}

	/** Mensagens da exceção e das causas, para conferir o motivo de uma falha. */
	public static String mensagens(Throwable erro) {
		StringBuilder texto = new StringBuilder();
		for (Throwable t = erro; t != null; t = t.getCause()) {
			texto.append(t.getMessage()).append(System.lineSeparator());
		}
		return texto.toString();
	}

	/** Apaga o banco temporário. Recusa qualquer nome sem o prefixo {@value #PREFIXO}. */
	@Override
	public void close() {
		if (!nome.startsWith(PREFIXO)) {
			throw new IllegalStateException("Recusado: " + nome + " não é um banco temporário de teste.");
		}
		executarNoServidor("DROP DATABASE IF EXISTS " + nome + " WITH (FORCE)");
	}

	private static void executarNoServidor(String sql) {
		try (Connection con = DriverManager.getConnection(urlDoBanco("postgres"), getUsuario(), getSenha());
				Statement st = con.createStatement()) {
			con.setAutoCommit(true);
			st.execute(sql);
		} catch (SQLException e) {
			throw new IllegalStateException("Erro no servidor PostgreSQL: " + sql, e);
		}
	}

	private static String urlDoBanco(String banco) {
		Matcher m = URL_POSTGRES.matcher(getUrlBancoDoPerfil());
		if (!m.matches()) {
			throw new IllegalStateException("URL do PostgreSQL não reconhecida: " + getUrlBancoDoPerfil());
		}
		return m.group(1) + banco + (m.group(3) == null ? "" : m.group(3));
	}

	private static Properties carregarConfiguracao() {
		String perfil = System.getenv().getOrDefault("SPRING_PROFILES_JDEV_ERP_PRO", "dev");
		try {
			Properties props = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-" + perfil + ".properties"));
			sobrescreverPeloAmbiente(props, "spring.datasource.url", "SPRING_DATASOURCE_URL");
			sobrescreverPeloAmbiente(props, "spring.datasource.username", "SPRING_DATASOURCE_USERNAME");
			sobrescreverPeloAmbiente(props, "spring.datasource.password", "SPRING_DATASOURCE_PASSWORD");
			return props;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void sobrescreverPeloAmbiente(Properties props, String propriedade, String variavel) {
		String valor = System.getenv(variavel);
		if (valor != null && !valor.isBlank()) {
			props.setProperty(propriedade, valor);
		}
	}

}
