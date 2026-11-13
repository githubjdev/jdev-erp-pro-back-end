package br.com.jdeverp.pro.contexto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.jayway.jsonpath.JsonPath;

import br.com.jdeverp.pro.app.JDevERPPROApplication;
import br.com.jdeverp.pro.enums.TipoClienteFuncionario;
import br.com.jdeverp.pro.enums.TipoPessoa;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Plano;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ClienteFuncionarioRepository;
import br.com.jdeverp.pro.repository.EmpresaRepository;
import br.com.jdeverp.pro.repository.PessoaRepository;
import br.com.jdeverp.pro.repository.PlanoRepository;
import br.com.jdeverp.pro.repository.RoleRepository;
import br.com.jdeverp.pro.repository.RoleUsuarioRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;
import br.com.jdeverp.pro.util.ObjectMapperUtil;

/**
 * Base dos testes de API: controller + segurança no fluxo real.
 *
 * <ul>
 * <li>Filtros ligados: toda requisição passa pelo {@code JwtFilter} com o token
 * obtido em {@code POST /api/usuario/login}.</li>
 * <li>{@code spring.jpa.open-in-view=false}: nada de sessão aberta durante a
 * requisição. Se algum service ler relacionamento LAZY fora da transação, o
 * teste quebra com {@code LazyInitializationException}.</li>
 * <li>Sem {@code @Transactional} no teste, de propósito: uma transação do teste
 * manteria a sessão aberta e esconderia o problema acima. As requisições
 * gravam de verdade no banco dev.</li>
 * </ul>
 *
 * Para não tocar nos dados reais (os endpoints {@code deletarTodos} apagam
 * tudo da empresa), cada classe cria a sua própria empresa de teste, com um
 * usuário administrador, e apaga tudo dela no final ({@link #apagarEmpresaDeTeste()}).
 */
@SpringBootTest(classes = JDevERPPROApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties", properties = "spring.jpa.open-in-view=false")
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public abstract class TestContextoApi {

	protected static final String SENHA = "Senha@Teste123";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final AtomicInteger CONTADOR = new AtomicInteger();

	@Autowired protected MockMvc mockMvc;
	@Autowired protected ObjectMapperUtil objectMapperUtil;
	@Autowired protected JdbcTemplate jdbcTemplate;
	@Autowired protected PasswordEncoder passwordEncoder;
	@Autowired protected PessoaRepository pessoaRepository;
	@Autowired protected EmpresaRepository empresaRepository;
	@Autowired protected PlanoRepository planoRepository;
	@Autowired protected ClienteFuncionarioRepository clienteFuncionarioRepository;
	@Autowired protected UsuarioRepository usuarioRepository;
	@Autowired protected RoleRepository roleRepository;
	@Autowired protected RoleUsuarioRepository roleUsuarioRepository;
	@Autowired private PlatformTransactionManager transactionManager;
	@Autowired private Environment environment;

	/** Sufixo único desta execução, para nomes, logins e e-mails não colidirem. */
	protected final String sufixo = Long.toString(System.nanoTime(), 36);

	/** Empresa de teste criada para a classe. */
	protected Long empresaId;

	/** Usuário administrador da empresa de teste, já logado. */
	protected UsuarioTeste admin;
	protected String token;

	/** Empresas criadas pela classe (a principal e as "outras", dos testes de isolamento), apagadas no final. */
	private final Map<Long, Long> empresasCriadas = new LinkedHashMap<>();

	protected record UsuarioTeste(Long usuarioId, Long clienteFuncionarioId, Long pessoaId, String login, String nome) {
	}

	// ==================== Empresa de teste ====================

	@BeforeAll
	void criarEmpresaDeTeste() throws Exception {
		assertEquals("false", environment.getProperty("spring.jpa.open-in-view"),
				"Os testes de API precisam rodar com open-in-view desligado");

		empresaId = criarEmpresa();
		admin = criarUsuario("Admin", true);
		token = logar(admin.login(), SENHA);
	}

	/**
	 * Cria uma empresa de teste. Use também para ter uma "outra empresa" nos
	 * testes de isolamento: ela é apagada no final, como a principal.
	 */
	protected Long criarEmpresa() {
		Empresa empresaPrincipal = empresaRepository.findById(1L).orElseThrow();
		Plano plano = planoRepository.findAll().get(0);

		/* Empresa e pessoa apontam uma para a outra: a pessoa nasce na empresa 1 e depois passa para a empresa de teste */
		Pessoa pessoaEmpresa = pessoaRepository.save(novaPessoa("Empresa Teste API", TipoPessoa.JURIDICA, empresaPrincipal));

		Empresa empresa = new Empresa();
		empresa.setPlano(plano);
		empresa.setPessoa(pessoaEmpresa);
		empresa.setLogoMarca("logo-teste");
		empresa.setPlanoAtivo(true);
		empresa.setBloqueio(false);
		empresa = empresaRepository.save(empresa);
		empresasCriadas.put(empresa.getId(), pessoaEmpresa.getId());

		pessoaEmpresa.setEmpresa(empresa);
		pessoaRepository.save(pessoaEmpresa);
		return empresa.getId();
	}

	@AfterAll
	void apagarEmpresasDeTeste() {
		empresasCriadas.forEach(this::apagarEmpresa);
	}

	private void apagarEmpresa(Long e, Long pessoaEmpresaId) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
			jdbcTemplate.update("delete from mensagem where empresa_id = ?", e);
			jdbcTemplate.update("delete from chamado where empresa_id = ?", e);
			jdbcTemplate.update("delete from item_pedido where empresa_id = ?", e);
			jdbcTemplate.update("delete from movimentacao_produto where empresa_id = ?", e);
			jdbcTemplate.update("delete from pedido where empresa_id = ?", e);
			jdbcTemplate.update("delete from produto where empresa_id = ?", e);
			jdbcTemplate.update("delete from categoria where empresa_id = ?", e);
			jdbcTemplate.update("delete from role_usuario where usuario_id in (select id from usuario where empresa_id = ?)", e);
			jdbcTemplate.update("update cliente_funcionario set usuario_id = null where empresa_id = ?", e);
			jdbcTemplate.update("delete from usuario where empresa_id = ?", e);
			jdbcTemplate.update("delete from cliente_funcionario where empresa_id = ?", e);
			/* Empresa e pessoa se referenciam: a pessoa da empresa volta para a empresa 1 antes de apagar */
			jdbcTemplate.update("update pessoa set empresa_id = 1 where id = ?", pessoaEmpresaId);
			jdbcTemplate.update("delete from empresa where id <> ? and pessoa_id in (select id from pessoa where empresa_id = ?)", e, e);
			jdbcTemplate.update("delete from pessoa where empresa_id = ?", e);
			jdbcTemplate.update("delete from empresa where id = ?", e);
			jdbcTemplate.update("delete from pessoa where id = ?", pessoaEmpresaId);
		});
	}

	/** Roda SQL de preparação ou limpeza numa transação (o pool está com auto-commit desligado). */
	protected void executarSql(String sql, Object... parametros) {
		new TransactionTemplate(transactionManager).executeWithoutResult(status -> jdbcTemplate.update(sql, parametros));
	}

	// ==================== Usuários ====================

	/**
	 * Cria pessoa, cliente/funcionário e usuário na empresa de teste, com
	 * ROLE_USER e, se pedido, ROLE_ADMIN. A senha é {@link #SENHA}.
	 */
	protected UsuarioTeste criarUsuario(String nome, boolean administrador) {
		return criarUsuario(nome, administrador, empresaId);
	}

	protected UsuarioTeste criarUsuario(String nome, boolean administrador, Long idEmpresa) {
		Empresa empresa = empresaRepository.findById(idEmpresa).orElseThrow();
		Pessoa pessoa = pessoaRepository.save(novaPessoa(nome + " Teste API", TipoPessoa.FISICA, empresa));

		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setTipoClienteFuncionario(TipoClienteFuncionario.FUNCIONARIO);
		clienteFuncionario.setPessoa(pessoa);
		clienteFuncionario.setEmpresa(empresa);
		clienteFuncionario = clienteFuncionarioRepository.save(clienteFuncionario);

		Usuario usuario = new Usuario();
		usuario.setLogin("api." + nome.toLowerCase() + "." + sufixo + "." + CONTADOR.incrementAndGet());
		usuario.setSenha(passwordEncoder.encode(SENHA));
		usuario.setLiberado(true);
		usuario.setClienteFuncionario(clienteFuncionario);
		usuario.setEmpresa(empresa);
		usuario = usuarioRepository.save(usuario);

		clienteFuncionario.setUsuario(usuario);
		clienteFuncionarioRepository.save(clienteFuncionario);

		associarRole(usuario, "ROLE_USER");
		if (administrador) {
			associarRole(usuario, "ROLE_ADMIN");
		}

		return new UsuarioTeste(usuario.getId(), clienteFuncionario.getId(), pessoa.getId(), usuario.getLogin(),
				pessoa.getNome());
	}

	private void associarRole(Usuario usuario, String acesso) {
		RoleUsuario roleUsuario = new RoleUsuario();
		roleUsuario.setAcesso(role(acesso));
		roleUsuario.setUsuario(usuario);
		roleUsuarioRepository.save(roleUsuario);
	}

	protected Role role(String acesso) {
		return roleRepository.buscaPorAcesso(acesso).stream().filter(r -> r.getAcesso().equals(acesso)).findFirst()
				.orElseThrow();
	}

	protected String logar(String login, String senha) throws Exception {
		String resposta = mockMvc
				.perform(MockMvcRequestBuilders.post("/api/usuario/login").contentType(MediaType.APPLICATION_JSON)
						.content(json(mapa("login", login, "senha", senha))))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		return JsonPath.read(resposta, "$.token");
	}

	// ==================== Pessoas ====================

	protected Pessoa novaPessoa(String nome, TipoPessoa tipo, Empresa empresa) {
		Pessoa pessoa = new Pessoa();
		pessoa.setNome(nome + " " + sufixo + " " + CONTADOR.incrementAndGet());
		pessoa.setTipoPessoa(tipo);
		pessoa.setCpf(gerarCpf());
		pessoa.setCnpj(gerarCnpj());
		pessoa.setEmail("api." + sufixo + "." + CONTADOR.incrementAndGet() + "@teste.com.br");
		pessoa.setTelefone("44 99999-0000");
		pessoa.setAtivo(true);
		pessoa.setDataCadastro(LocalDate.now());
		pessoa.setCep("87000-000");
		pessoa.setLogradouro("Rua Teste");
		pessoa.setBairro("Centro");
		pessoa.setEstado("PR");
		pessoa.setCidade("Maringá");
		pessoa.setPais("Brasil");
		pessoa.setEmpresa(empresa);
		return pessoa;
	}

	/** Corpo JSON de uma pessoa nova para {@code /api/pessoa/salvar}. */
	protected Map<String, Object> pessoaJson(String nome) {
		return mapa("nome", nome + " " + sufixo + " " + CONTADOR.incrementAndGet(), "tipoPessoa", "FISICA",
				"cpf", gerarCpf(), "cnpj", gerarCnpj(),
				"email", "api." + sufixo + "." + CONTADOR.incrementAndGet() + "@teste.com.br",
				"telefone", "44 99999-0000", "ativo", true, "dataCadastro", LocalDate.now().toString(),
				"cep", "87000-000", "logradouro", "Rua Teste", "bairro", "Centro", "estado", "PR",
				"cidade", "Maringá", "pais", "Brasil");
	}

	protected static String gerarCpf() {
		int[] n = new int[11];
		for (int i = 0; i < 9; i++) {
			n[i] = RANDOM.nextInt(10);
		}
		n[0] = 1 + RANDOM.nextInt(9); /* evita todos os dígitos iguais */
		n[9] = digitoCpf(n, 9);
		n[10] = digitoCpf(n, 10);
		return String.format("%d%d%d.%d%d%d.%d%d%d-%d%d", n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8], n[9],
				n[10]);
	}

	private static int digitoCpf(int[] n, int tamanho) {
		int soma = 0;
		for (int i = 0; i < tamanho; i++) {
			soma += n[i] * (tamanho + 1 - i);
		}
		int resto = 11 - soma % 11;
		return resto >= 10 ? 0 : resto;
	}

	protected static String gerarCnpj() {
		int[] n = new int[14];
		for (int i = 0; i < 8; i++) {
			n[i] = RANDOM.nextInt(10);
		}
		n[0] = 1 + RANDOM.nextInt(9);
		n[8] = 0;
		n[9] = 0;
		n[10] = 0;
		n[11] = 1;
		n[12] = digitoCnpj(n, new int[] { 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 });
		n[13] = digitoCnpj(n, new int[] { 6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2 });
		return String.format("%d%d.%d%d%d.%d%d%d/%d%d%d%d-%d%d", n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8],
				n[9], n[10], n[11], n[12], n[13]);
	}

	private static int digitoCnpj(int[] n, int[] pesos) {
		int soma = 0;
		for (int i = 0; i < pesos.length; i++) {
			soma += n[i] * pesos[i];
		}
		int resto = soma % 11;
		return resto < 2 ? 0 : 11 - resto;
	}

	// ==================== HTTP ====================

	/** GET com variáveis de URI, ex.: get("/api/x/buscaPorNome/{nome}", nome). */
	protected ResultActions get(String url, Object... variaveis) throws Exception {
		return executar(MockMvcRequestBuilders.get(url, variaveis), null);
	}

	protected ResultActions post(String url, Object corpo) throws Exception {
		return executar(MockMvcRequestBuilders.post(url), corpo);
	}

	protected ResultActions delete(String url) throws Exception {
		return executar(MockMvcRequestBuilders.delete(url), null);
	}

	protected ResultActions delete(String url, Object corpo) throws Exception {
		return executar(MockMvcRequestBuilders.delete(url), corpo);
	}

	protected ResultActions patch(String url) throws Exception {
		return executar(MockMvcRequestBuilders.patch(url), null);
	}

	/** Requisição com o token de outro usuário (ex.: um usuário sem ROLE_ADMIN). */
	protected ResultActions comToken(String outroToken, MockHttpServletRequestBuilder requisicao, Object corpo)
			throws Exception {
		requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + outroToken);
		if (corpo != null) {
			requisicao.contentType(MediaType.APPLICATION_JSON).content(json(corpo));
		}
		return mockMvc.perform(requisicao);
	}

	private ResultActions executar(MockHttpServletRequestBuilder requisicao, Object corpo) throws Exception {
		return comToken(token, requisicao, corpo);
	}

	protected String json(Object objeto) throws Exception {
		return objectMapperUtil.objetoParaJson(objeto);
	}

	protected static String corpo(ResultActions resultado) throws Exception {
		return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	/** Lê o {@code id} da resposta. */
	protected static Long id(ResultActions resultado) throws Exception {
		return ((Number) JsonPath.read(corpo(resultado), "$.id")).longValue();
	}

	protected static Long numero(ResultActions resultado, String caminho) throws Exception {
		return ((Number) JsonPath.read(corpo(resultado), caminho)).longValue();
	}

	/** Mapa que aceita {@code null} (o {@code Map.of} não aceita). Uso: mapa("chave", valor, ...). */
	protected static Map<String, Object> mapa(Object... chaveValor) {
		Map<String, Object> mapa = new LinkedHashMap<>();
		for (int i = 0; i < chaveValor.length; i += 2) {
			mapa.put((String) chaveValor[i], chaveValor[i + 1]);
		}
		return mapa;
	}

	/**
	 * Endpoint {@code /paginado}: valores padrão, as duas direções (asc e
	 * qualquer outra, que vira desc) e campo de ordenação inválido. O {@code sort}
	 * vai para o JPQL e só aceita atributo simples da entidade.
	 */
	protected void conferirPaginado(String url) throws Exception {
		get(url).andExpect(status().isOk());
		get(url + "?page=0&size=2&sort=id&direction=ASC").andExpect(status().isOk());
		get(url + "?page=0&size=2&sort=id&direction=desc").andExpect(status().isOk());
		get(url + "?sort=empresa").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Campo de ordenação inválido."));
	}

	/** Referência a outra entidade no JSON: {"id": x}. */
	protected static Map<String, Object> ref(Long id) {
		return mapa("id", id);
	}

	protected static List<Long> ids(Long... ids) {
		return List.of(ids);
	}

}
