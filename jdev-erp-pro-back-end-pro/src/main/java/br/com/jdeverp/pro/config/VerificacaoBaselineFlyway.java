package br.com.jdeverp.pro.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Confere o banco antes do baseline automático do Flyway
 * ({@code spring.flyway.baseline-on-migrate=true}).
 *
 * <p>O baseline acontece uma vez só, num banco que já tem tabelas e ainda não
 * tem a {@code flyway_schema_history}: um banco criado antes do Flyway, pelo
 * Hibernate e pelos scripts manuais {@code V1_INIT} e
 * {@code V2_PESSOA_DOCUMENTO_POR_TIPO}. O Flyway grava a versão de baseline
 * como aplicada e nunca executa as migrações até ela. Sem esta conferência, um
 * banco sem o V2 seria marcado como se o tivesse.
 *
 * <p>Versões de baseline aceitas:
 * <ul>
 * <li>{@code 20261112.162000} (padrão): V1 e V2 aplicados à mão;</li>
 * <li>{@code 20261112.161000}: só o V1 aplicado. O Flyway executa a
 * {@code V20261112_162000} logo depois do baseline.</li>
 * </ul>
 * Qualquer outra é recusada. Abaixo da carga inicial, o Flyway tentaria criar
 * as tabelas e rodar a carga, que apaga dados. Acima da última migração antiga,
 * pularia migrações criadas já com o Flyway, sem conferir nada.
 *
 * <p>Se algo não bate, lança {@link FlywayException}: o baseline não é gravado
 * e a aplicação não sobe. Banco vazio não passa por aqui: recebe todas as
 * migrações. Ver {@code docs/Flyway.md}.
 */
@Component
public class VerificacaoBaselineFlyway implements Callback {

	private static final Logger log = LoggerFactory.getLogger(VerificacaoBaselineFlyway.class);

	/** Versão da carga inicial, o antigo {@code V1_INIT.sql}. */
	public static final MigrationVersion VERSAO_CARGA_INICIAL = MigrationVersion.fromVersion("20261112.161000");

	/** Versão do antigo {@code V2_PESSOA_DOCUMENTO_POR_TIPO.sql}, a última anterior ao Flyway. */
	public static final MigrationVersion VERSAO_PESSOA_DOCUMENTO_POR_TIPO = MigrationVersion.fromVersion("20261112.162000");

	/** Tabelas criadas pela {@code V20261112_160000__criacao_estrutura_inicial}. Cada uma tem a sua {@code seq_<tabela>}. */
	static final List<String> TABELAS = List.of("categoria", "chamado", "cliente_funcionario", "empresa", "item_pedido",
			"mensagem", "movimentacao_produto", "pedido", "pessoa", "plano", "produto", "role", "role_usuario",
			"usuario");

	@Override
	public boolean supports(Event event, Context context) {
		return event == Event.BEFORE_BASELINE;
	}

	@Override
	public boolean canHandleInTransaction(Event event, Context context) {
		return true;
	}

	@Override
	public void handle(Event event, Context context) {
		MigrationVersion versao = context.getConfiguration().getBaselineVersion();
		log.info("Banco existente sem flyway_schema_history: conferindo o banco antes do baseline na versão {}.", versao);

		List<String> divergencias;
		try {
			divergencias = verificar(context.getConnection(), versao);
		} catch (SQLException e) {
			throw new FlywayException("Baseline do Flyway recusado: não foi possível conferir o banco. " + e.getMessage(), e);
		}

		if (!divergencias.isEmpty()) {
			throw new FlywayException(mensagemDeRecusa(versao, divergencias));
		}
		log.info("Baseline do Flyway na versão {} conferido: o banco tem a estrutura e os scripts antigos até essa versão.", versao);
	}

	@Override
	public String getCallbackName() {
		return "VerificacaoBaselineFlyway";
	}

	/**
	 * Lista o que não bate entre o banco e a versão de baseline. Lista vazia
	 * significa que o baseline pode ser gravado.
	 */
	List<String> verificar(Connection con, MigrationVersion versao) throws SQLException {
		List<String> divergencias = new ArrayList<>();

		boolean versaoAceita = versao.compareTo(VERSAO_CARGA_INICIAL) == 0
				|| versao.compareTo(VERSAO_PESSOA_DOCUMENTO_POR_TIPO) == 0;
		if (!versaoAceita) {
			divergencias.add("versão de baseline " + versao + " não aceita. Use " + VERSAO_PESSOA_DOCUMENTO_POR_TIPO
					+ " (V1_INIT e V2_PESSOA_DOCUMENTO_POR_TIPO aplicados à mão) ou " + VERSAO_CARGA_INICIAL
					+ " (só o V1_INIT). Abaixo disso, o Flyway criaria as tabelas de novo e rodaria a carga inicial, que apaga dados;"
					+ " acima, pularia migrações sem conferir.");
			return divergencias;
		}

		verificarEstrutura(con, divergencias);
		verificarCargaInicial(con, divergencias);
		if (versao.compareTo(VERSAO_PESSOA_DOCUMENTO_POR_TIPO) == 0) {
			verificarPessoaDocumentoPorTipo(con, divergencias);
		}
		return divergencias;
	}

	/** Tabelas e sequences da estrutura inicial. */
	private void verificarEstrutura(Connection con, List<String> divergencias) throws SQLException {
		Set<String> tabelas = consultarNomes(con, """
				SELECT table_name FROM information_schema.tables
				 WHERE table_schema = current_schema() AND table_type = 'BASE TABLE'""");
		Set<String> sequences = consultarNomes(con, """
				SELECT sequence_name FROM information_schema.sequences
				 WHERE sequence_schema = current_schema()""");

		for (String tabela : TABELAS) {
			if (!tabelas.contains(tabela)) {
				divergencias.add("tabela ausente: " + tabela);
			}
			if (!sequences.contains("seq_" + tabela)) {
				divergencias.add("sequence ausente: seq_" + tabela);
			}
		}
	}

	/** O que o antigo V1_INIT deixa no banco: CHECK do plano, unaccent, colunas do usuário e roles. */
	private void verificarCargaInicial(Connection con, List<String> divergencias) throws SQLException {
		String checkPlano = definicaoDaConstraint(con, "plano", "plano_valor_mensal_check");
		if (checkPlano == null || !checkPlano.contains("FREE")) {
			divergencias.add("V1_INIT: CHECK plano_valor_mensal_check com a regra do plano FREE ausente ou diferente");
		}

		if (consultarNomes(con, "SELECT extname FROM pg_extension WHERE extname = 'unaccent'").isEmpty()) {
			divergencias.add("V1_INIT: extensão unaccent não instalada");
		}

		Coluna liberado = coluna(con, "usuario", "liberado");
		if (liberado == null) {
			divergencias.add("V1_INIT: coluna usuario.liberado ausente");
		}
		if (coluna(con, "usuario", "bloqueado") != null) {
			divergencias.add("V1_INIT: coluna usuario.bloqueado ainda existe (o V1 a remove)");
		}
		for (String nome : List.of("refresh_token", "token_sessao")) {
			Coluna token = coluna(con, "usuario", nome);
			if (token == null || !"text".equals(token.tipo())) {
				divergencias.add("V1_INIT: coluna usuario." + nome + " ausente ou com tipo diferente de text");
			}
		}

		Coluna usuarioDoClienteFuncionario = coluna(con, "cliente_funcionario", "usuario_id");
		if (usuarioDoClienteFuncionario == null || !usuarioDoClienteFuncionario.aceitaNulo()) {
			divergencias.add("V1_INIT: coluna cliente_funcionario.usuario_id ausente ou NOT NULL");
		}

		if (existeTabela(con, "role")) {
			Set<String> roles = consultarNomes(con,
					"SELECT acesso FROM role WHERE acesso IN ('ROLE_USER', 'ROLE_ADMIN')");
			for (String role : List.of("ROLE_USER", "ROLE_ADMIN")) {
				if (!roles.contains(role)) {
					divergencias.add("V1_INIT: role " + role + " ausente");
				}
			}
		}
	}

	/** O que o antigo V2_PESSOA_DOCUMENTO_POR_TIPO deixa no banco. */
	private void verificarPessoaDocumentoPorTipo(Connection con, List<String> divergencias) throws SQLException {
		int antes = divergencias.size();

		for (String nome : List.of("cpf", "cnpj")) {
			Coluna documento = coluna(con, "pessoa", nome);
			if (documento == null || !documento.aceitaNulo()) {
				divergencias.add("V2_PESSOA_DOCUMENTO_POR_TIPO: coluna pessoa." + nome + " ausente ou ainda NOT NULL");
			}
		}
		Coluna email = coluna(con, "pessoa", "email");
		if (email == null || email.aceitaNulo()) {
			divergencias.add("V2_PESSOA_DOCUMENTO_POR_TIPO: coluna pessoa.email ausente ou sem NOT NULL");
		}
		for (String constraint : List.of("pessoa_cpf_pessoa_fisica_check", "pessoa_cnpj_pessoa_juridica_check",
				"pessoa_email_preenchido_check")) {
			if (definicaoDaConstraint(con, "pessoa", constraint) == null) {
				divergencias.add("V2_PESSOA_DOCUMENTO_POR_TIPO: CHECK " + constraint + " ausente");
			}
		}

		if (divergencias.size() > antes) {
			divergencias.add("se o V2_PESSOA_DOCUMENTO_POR_TIPO nunca foi aplicado neste banco, suba com"
					+ " SPRING_FLYWAY_BASELINE_VERSION=" + VERSAO_CARGA_INICIAL + ": o Flyway grava o baseline antes dele e o aplica.");
		}
	}

	private static String mensagemDeRecusa(MigrationVersion versao, List<String> divergencias) {
		return "Baseline do Flyway recusado na versão " + versao
				+ ": o banco existente não está no estado que essa versão declara. Nada foi gravado na flyway_schema_history"
				+ " e nenhuma migração foi executada. Divergências:\n - " + String.join("\n - ", divergencias)
				+ "\nVeja docs/Flyway.md, seção \"Banco existente\".";
	}

	private record Coluna(String tipo, boolean aceitaNulo) {
	}

	private static Coluna coluna(Connection con, String tabela, String coluna) throws SQLException {
		String sql = """
				SELECT data_type, is_nullable FROM information_schema.columns
				 WHERE table_schema = current_schema() AND table_name = ? AND column_name = ?""";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, tabela);
			ps.setString(2, coluna);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? new Coluna(rs.getString(1), "YES".equals(rs.getString(2))) : null;
			}
		}
	}

	private static String definicaoDaConstraint(Connection con, String tabela, String constraint) throws SQLException {
		String sql = """
				SELECT pg_get_constraintdef(c.oid) FROM pg_constraint c
				  JOIN pg_class t ON t.oid = c.conrelid
				  JOIN pg_namespace n ON n.oid = t.relnamespace
				 WHERE n.nspname = current_schema() AND t.relname = ? AND c.conname = ?""";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, tabela);
			ps.setString(2, constraint);
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next() ? rs.getString(1) : null;
			}
		}
	}

	private static boolean existeTabela(Connection con, String tabela) throws SQLException {
		return coluna(con, tabela, "id") != null;
	}

	private static Set<String> consultarNomes(Connection con, String sql) throws SQLException {
		Set<String> nomes = new HashSet<>();
		try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				nomes.add(rs.getString(1));
			}
		}
		return nomes;
	}

}
