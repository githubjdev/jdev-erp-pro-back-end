package br.com.jdeverp.pro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import br.com.jdeverp.pro.contexto.TestContextoApi;
import br.com.jdeverp.pro.model.Plano;

/**
 * Área de administração (P9, P10 e P21): usuário sem ROLE_ADMIN recebe 403 em
 * todo endpoint de dado global (Plano, Role, Empresa), de atribuição de acesso
 * (RoleUsuario), de cadastro, edição e exclusão de usuários e de exclusão em
 * lote ({@code deletarTodos} e {@code deletarTodosPorIds}).
 *
 * Os corpos são válidos para o {@code @Valid} (senão o 400 viria antes do 403),
 * mas o service os recusaria se a permissão falhasse: nome repetido, id
 * inexistente, acesso sem o prefixo ROLE_. Assim, um furo de permissão quebra o
 * teste sem alterar dado global do banco dev.
 */
public class TestPermissoesAdmin extends TestContextoApi {

	private UsuarioTeste comum;
	private String tokenComum;
	private Long roleAdminId;
	private Long pessoaDaEmpresaId;
	private Plano plano;

	@BeforeAll
	void criarUsuarioComum() throws Exception {
		comum = criarUsuario("Comum", false);
		tokenComum = logar(comum.login(), SENHA);
		roleAdminId = role("ROLE_ADMIN").getId();
		pessoaDaEmpresaId = empresaRepository.findById(empresaId).orElseThrow().getPessoa().getId();
		plano = planoRepository.findAll().get(0);
	}

	private Map<String, Object> planoJson(Long id) {
		return mapa("id", id, "nome", plano.getNome(), "descricao", "Plano de teste", "ativo", true, "valorMensal", 99,
				"limiteUsuario", 5, "limiteCliente", 5, "tipoPlano", "PRO");
	}

	private Map<String, Object> empresaJson(Long id) {
		return mapa("id", id, "plano", ref(plano.getId()), "pessoa", ref(pessoaDaEmpresaId), "logoMarca", "logo");
	}

	private Map<String, Object> roleUsuarioJson(Long id) {
		return mapa("id", id, "acesso", ref(roleAdminId), "usuario", ref(comum.usuarioId()));
	}

	private Map<String, Object> usuarioJson(Long id) {
		return mapa("id", id, "login", admin.login(), "senha", SENHA, "pessoaId", comum.pessoaId());
	}

	/* Exclusão em lote: apaga tudo da empresa, só admin (P21) */
	private static Stream<Arguments> exclusaoEmLote(String modulo) {
		return Stream.of(Arguments.of(HttpMethod.DELETE, "/api/" + modulo + "/deletarTodos", null),
				Arguments.of(HttpMethod.DELETE, "/api/" + modulo + "/deletarTodosPorIds", List.of(0L)));
	}

	Stream<Arguments> endpointsDeAdministrador() {
		return Stream.concat(endpointsDeDadosGlobaisEUsuarios(),
				Stream.of("clienteFuncionario", "itemPedido", "mensagem", "movimentacaoProduto", "pedido", "pessoa", "produto")
						.flatMap(TestPermissoesAdmin::exclusaoEmLote));
	}

	private Stream<Arguments> endpointsDeDadosGlobaisEUsuarios() {
		return Stream.of(
				/* Plano: dado global */
				Arguments.of(HttpMethod.POST, "/api/plano/salvar", planoJson(null)),
				Arguments.of(HttpMethod.POST, "/api/plano/atualizar", planoJson(0L)),
				Arguments.of(HttpMethod.GET, "/api/plano/listar", null),
				Arguments.of(HttpMethod.GET, "/api/plano/buscarPorId/" + plano.getId(), null),
				Arguments.of(HttpMethod.GET, "/api/plano/buscaPorNome/a", null),
				Arguments.of(HttpMethod.GET, "/api/plano/existePorNome/a", null),
				Arguments.of(HttpMethod.GET, "/api/plano/existePorNomeDiferenteId/0/a", null),
				Arguments.of(HttpMethod.DELETE, "/api/plano/deletar/0", null),

				/* Role: decide quem é administrador */
				Arguments.of(HttpMethod.POST, "/api/role/salvar", mapa("acesso", "SEM_PREFIXO")),
				Arguments.of(HttpMethod.POST, "/api/role/atualizar", mapa("id", 0, "acesso", "SEM_PREFIXO")),
				Arguments.of(HttpMethod.GET, "/api/role/listar", null),
				Arguments.of(HttpMethod.GET, "/api/role/buscarPorId/" + roleAdminId, null),
				Arguments.of(HttpMethod.GET, "/api/role/buscaPorAcesso/ROLE_ADMIN", null),
				Arguments.of(HttpMethod.GET, "/api/role/existePorAcesso/ROLE_ADMIN", null),
				Arguments.of(HttpMethod.GET, "/api/role/existePorAcessoDiferenteId/0/ROLE_ADMIN", null),
				Arguments.of(HttpMethod.DELETE, "/api/role/deletar/0", null),

				/* RoleUsuario: dar ou tirar acesso (o salvar é a tentativa de virar admin) */
				Arguments.of(HttpMethod.POST, "/api/roleUsuario/salvar", roleUsuarioJson(null)),
				Arguments.of(HttpMethod.POST, "/api/roleUsuario/atualizar", roleUsuarioJson(0L)),
				Arguments.of(HttpMethod.GET, "/api/roleUsuario/listar", null),
				Arguments.of(HttpMethod.GET, "/api/roleUsuario/buscarPorId/0", null),
				Arguments.of(HttpMethod.GET, "/api/roleUsuario/listarPorUsuario/" + admin.usuarioId(), null),
				Arguments.of(HttpMethod.GET, "/api/roleUsuario/listarPorRole/" + roleAdminId, null),
				Arguments.of(HttpMethod.GET, "/api/roleUsuario/existePorUsuarioERole/" + admin.usuarioId() + "/" + roleAdminId, null),
				Arguments.of(HttpMethod.DELETE, "/api/roleUsuario/deletar/0", null),
				Arguments.of(HttpMethod.DELETE, "/api/roleUsuario/deletarPorUsuarioERole/0/0", null),

				/* Empresa: dado global (buscarPorId da própria empresa fica liberado, testado abaixo) */
				Arguments.of(HttpMethod.POST, "/api/empresa/salvar", empresaJson(null)),
				Arguments.of(HttpMethod.POST, "/api/empresa/atualizar", empresaJson(0L)),
				Arguments.of(HttpMethod.GET, "/api/empresa/listar", null),
				Arguments.of(HttpMethod.GET, "/api/empresa/buscaPorNome/a", null),
				Arguments.of(HttpMethod.GET, "/api/empresa/existePorNome/a", null),
				Arguments.of(HttpMethod.GET, "/api/empresa/existePorNomeDiferenteId/0/a", null),
				Arguments.of(HttpMethod.DELETE, "/api/empresa/deletar/0", null),

				/* Usuários: cadastro, edição e exclusão */
				Arguments.of(HttpMethod.POST, "/api/usuario/salvar", usuarioJson(null)),
				Arguments.of(HttpMethod.POST, "/api/usuario/atualizar", usuarioJson(0L)),
				Arguments.of(HttpMethod.DELETE, "/api/usuario/deletar/0", null),
				Arguments.of(HttpMethod.DELETE, "/api/usuario/deletar-todos", null));
	}

	@Order(1)
	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("endpointsDeAdministrador")
	public void testUsuarioComumNaoAcessaAreaAdmin(HttpMethod metodo, String url, Object corpo) throws Exception {
		comToken(tokenComum, MockMvcRequestBuilders.request(metodo, url), corpo)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value(JwtAccessDeniedHandler.MSG_SEM_PERMISSAO))
				.andExpect(jsonPath("$.path").value(url));
	}

	@Test
	@Order(2)
	public void testUsuarioComumContinuaSemRoleAdmin() {
		Long acessosAdmin = jdbcTemplate.queryForObject("select count(*) from role_usuario ru join role r on r.id = ru.acesso_id "
				+ "where ru.usuario_id = ? and r.acesso = 'ROLE_ADMIN'", Long.class, comum.usuarioId());
		assertEquals(0L, acessosAdmin);
	}

	@Test
	@Order(3)
	public void testUsuarioComumUsaRotasDaEmpresa() throws Exception {
		comToken(tokenComum, MockMvcRequestBuilders.get("/api/categoria/listar"), null).andExpect(status().isOk());
		comToken(tokenComum, MockMvcRequestBuilders.get("/api/usuario/listar"), null).andExpect(status().isOk());
		comToken(tokenComum, MockMvcRequestBuilders.get("/api/usuario/buscarPorId/{id}", comum.usuarioId()), null)
				.andExpect(status().isOk());
	}

	@Test
	@Order(4)
	public void testUsuarioComumConsultaSoAPropriaEmpresa() throws Exception {
		comToken(tokenComum, MockMvcRequestBuilders.get("/api/empresa/buscarPorId/{id}", empresaId), null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(empresaId));

		Long outraEmpresa = criarEmpresa();
		comToken(tokenComum, MockMvcRequestBuilders.get("/api/empresa/buscarPorId/{id}", outraEmpresa), null)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Apenas administradores podem consultar outras empresas."));

		/* O administrador vê qualquer empresa */
		get("/api/empresa/buscarPorId/{id}", outraEmpresa).andExpect(status().isOk());
	}

	@Test
	@Order(5)
	public void testAdministradorAcessaAreaAdmin() throws Exception {
		get("/api/plano/listar").andExpect(status().isOk());
		get("/api/role/listar").andExpect(status().isOk());
		get("/api/roleUsuario/listar").andExpect(status().isOk());
		get("/api/empresa/listar").andExpect(status().isOk());
	}

	@Test
	@Order(6)
	public void testOrdenacaoSoComCampoDaEntidade() throws Exception {
		get("/api/categoria/paginado?sort=nome").andExpect(status().isOk());
		get("/api/usuario/paginado?sort=login").andExpect(status().isOk());

		/* Subconsulta no order by: leria o token de sessão do administrador, um caractere por vez */
		String injecao = "(case when (select substring(u.tokenSessao, 1, 1) from Usuario u where u.id = " + admin.usuarioId()
				+ ") = 'e' then id else nome end)";
		for (String campo : new String[] { injecao, "empresa", "nome desc, id", "naoExiste" }) {
			comToken(tokenComum, MockMvcRequestBuilders.get("/api/categoria/paginado").param("sort", campo), null)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").value("Campo de ordenação inválido."));
		}
	}

	@Test
	@Order(7)
	public void testUsuarioComumAlteraSoAPropriaSenha() throws Exception {
		comToken(tokenComum, MockMvcRequestBuilders.post("/api/usuario/alterarSenha"),
				mapa("id", admin.usuarioId(), "senhaAtual", SENHA, "novaSenha", "Outra@Senha1", "confirmarSenha", "Outra@Senha1"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Você só pode alterar a sua própria senha."));

		comToken(tokenComum, MockMvcRequestBuilders.post("/api/usuario/alterarSenha"),
				mapa("id", comum.usuarioId(), "senhaAtual", SENHA, "novaSenha", "Outra@Senha1", "confirmarSenha", "Outra@Senha1"))
				.andExpect(status().isOk())
				.andExpect(content().string("Senha alterada com sucesso!"));
	}

}
