package br.com.jdeverp.pro.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import br.com.jdeverp.pro.contexto.TestContextoApi;
import br.com.jdeverp.pro.dto.ChamadoDTO;
import br.com.jdeverp.pro.dto.ClienteFuncionarioDTO;
import br.com.jdeverp.pro.dto.EmpresaDTO;
import br.com.jdeverp.pro.dto.ItemPedidoDTO;
import br.com.jdeverp.pro.dto.MensagemDTO;
import br.com.jdeverp.pro.dto.MovimentacaoProdutoDTO;
import br.com.jdeverp.pro.dto.PedidoDTO;
import br.com.jdeverp.pro.dto.PessoaDTO;
import br.com.jdeverp.pro.dto.ProdutoDTO;
import br.com.jdeverp.pro.dto.RoleUsuarioDTO;
import br.com.jdeverp.pro.dto.UsuarioDto;
import br.com.jdeverp.pro.enums.FormaPagamento;
import br.com.jdeverp.pro.enums.PrioridadeChamado;
import br.com.jdeverp.pro.enums.StatusChamado;
import br.com.jdeverp.pro.enums.StatusPedido;
import br.com.jdeverp.pro.enums.TipoChamado;
import br.com.jdeverp.pro.enums.TipoClienteFuncionario;
import br.com.jdeverp.pro.enums.TipoMovimentacaoProduto;
import br.com.jdeverp.pro.enums.TipoPedido;
import br.com.jdeverp.pro.enums.TipoPessoa;
import br.com.jdeverp.pro.enums.UnidadeMedida;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.model.Mensagem;
import br.com.jdeverp.pro.model.MovimentacaoProduto;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Plano;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.CategoriaRepository;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import br.com.jdeverp.pro.repository.ItemPedidoRepository;
import br.com.jdeverp.pro.repository.MensagemRepository;
import br.com.jdeverp.pro.repository.MovimentacaoProdutoRepository;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;
import br.com.jdeverp.pro.security.UsuarioAutenticado;
import jakarta.persistence.EntityManagerFactory;

/**
 * Services chamados direto, com sessão real do Hibernate no banco dev.
 *
 * <ul>
 * <li>Sem transação e sem sessão no teste: cada chamada de service abre e
 * fecha a sua, como numa requisição com {@code open-in-view=false}.</li>
 * <li>O usuário logado é montado como no {@code JwtFilter}: carregado numa
 * sessão que já fechou.</li>
 * <li>Se um service ler relacionamento LAZY depois que a sessão dele fechou
 * (na conversão para DTO, por exemplo), o teste quebra com
 * {@link LazyInitializationException}. O primeiro teste prova que este
 * ambiente detecta esse erro.</li>
 * <li>Os últimos testes leem as entidades fora da sessão e conferem que
 * {@code toString}, {@code equals} e {@code hashCode} não tocam nos
 * relacionamentos LAZY.</li>
 * </ul>
 *
 * Os dados ficam na empresa de teste da classe e são apagados no final.
 */
public class TestSessaoHibernate extends TestContextoApi {

	@Autowired private EntityManagerFactory entityManagerFactory;
	@Autowired private UsuarioDetailsService usuarioDetailsService;

	@Autowired private CategoriaService categoriaService;
	@Autowired private ProdutoService produtoService;
	@Autowired private PessoaService pessoaService;
	@Autowired private ClienteFuncionarioService clienteFuncionarioService;
	@Autowired private UsuarioService usuarioService;
	@Autowired private RoleUsuarioService roleUsuarioService;
	@Autowired private EmpresaService empresaService;
	@Autowired private ChamadoService chamadoService;
	@Autowired private MensagemService mensagemService;
	@Autowired private PedidoService pedidoService;
	@Autowired private ItemPedidoService itemPedidoService;
	@Autowired private MovimentacaoProdutoService movimentacaoProdutoService;

	@Autowired private CategoriaRepository categoriaRepository;
	@Autowired private ProdutoRepository produtoRepository;
	@Autowired private ChamadoRepository chamadoRepository;
	@Autowired private MensagemRepository mensagemRepository;
	@Autowired private PedidoRepository pedidoRepository;
	@Autowired private ItemPedidoRepository itemPedidoRepository;
	@Autowired private MovimentacaoProdutoRepository movimentacaoProdutoRepository;

	private UsuarioTeste cliente;

	private Long idCategoria;
	private Long idProduto;
	private Long idPessoa;
	private Long idClienteFuncionario;
	private Long idUsuario;
	private Long idRoleUsuario;
	private Long idChamado;
	private Long idMensagem;
	private Long idPedido;
	private Long idItem;
	private Long idMovimentacao;

	private String nomeCategoria;
	private String nomeProduto;
	private String nomePessoa;
	private String tituloChamado;
	private String numeroPedido;

	@BeforeAll
	void criarCliente() {
		cliente = criarUsuario("Cliente", false);
	}

	/** Igual ao JwtFilter: o usuário vem de uma sessão que já fechou. */
	@BeforeEach
	void autenticarComoNoJwtFilter() {
		Usuario usuario = (Usuario) usuarioDetailsService.loadUserByUsername(admin.login());
		UsuarioAutenticado principal = new UsuarioAutenticado(usuario);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities()));
	}

	@AfterEach
	void limparAutenticacao() {
		SecurityContextHolder.clearContext();
	}

	// ==================== Ambiente ====================

	@Test
	@Order(1)
	public void testAmbienteSemSessaoAberta() {
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive(), "O teste não pode ter transação aberta");
		assertFalse(TransactionSynchronizationManager.hasResource(entityManagerFactory), "O teste não pode ter sessão aberta");

		/* Controle: a entidade lida fora de um service já está sem sessão, e o LAZY dela falha */
		Pessoa pessoa = pessoaRepository.buscarPorId(admin.pessoaId(), empresaId).orElseThrow();
		assertFalse(Hibernate.isInitialized(pessoa.getEmpresa()));
		assertThrows(LazyInitializationException.class, () -> pessoa.getEmpresa().getLogoMarca());
	}

	// ==================== Services: gravam e leem com a própria sessão ====================

	@Test
	@Order(2)
	public void testCategoriaService() {
		nomeCategoria = "Categoria Sessão " + sufixo;
		Categoria categoria = new Categoria();
		categoria.setNome(nomeCategoria);

		idCategoria = categoriaService.salvar(categoria).getId();

		assertEquals(nomeCategoria, categoriaService.buscarPorId(idCategoria, empresaId).getNome());
		assertEquals(empresaId, categoriaService.buscarPorId(idCategoria, empresaId).getEmpresaId());
		assertEquals(1, categoriaService.listarPaginado(empresaId, PageRequest.of(0, 10)).getTotalElements());
	}

	@Test
	@Order(3)
	public void testProdutoService() {
		nomeProduto = "Produto Sessão " + sufixo;
		Produto produto = new Produto();
		produto.setNome(nomeProduto);
		produto.setImagem("imagem");
		produto.setPreco(50.0);
		produto.setEstoque(10.0);
		produto.setEstoqueMinimo(1.0);
		produto.setSku("SKU-SESSAO-" + sufixo);
		produto.setUnidadeMedida(UnidadeMedida.UNIDADE);
		produto.setCategoria(categoria(idCategoria));

		ProdutoDTO salvo = produtoService.salvar(produto);
		idProduto = salvo.getId();
		assertEquals(nomeCategoria, salvo.getCategoriaNome());

		assertEquals(nomeCategoria, produtoService.buscarPorId(idProduto, empresaId).orElseThrow().getCategoriaNome());
		assertEquals(nomeCategoria, produtoService.listar(empresaId).get(0).getCategoriaNome());
	}

	@Test
	@Order(4)
	public void testPessoaService() {
		PessoaDTO salva = pessoaService.salvar(novaPessoa("Pessoa Sessão", TipoPessoa.FISICA, null));
		idPessoa = salva.getId();
		nomePessoa = salva.getNome();
		assertEquals(empresaId, salva.getEmpresaId());

		assertEquals(empresaId, pessoaService.buscarPorIdDto(idPessoa, empresaId).orElseThrow().getEmpresaId());
	}

	@Test
	@Order(5)
	public void testClienteFuncionarioService() {
		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setTipoClienteFuncionario(TipoClienteFuncionario.CLIENTE);
		clienteFuncionario.setPessoa(pessoa(idPessoa));

		ClienteFuncionarioDTO salvo = clienteFuncionarioService.salvar(clienteFuncionario);
		idClienteFuncionario = salvo.getId();
		assertEquals(nomePessoa, salvo.getNomePessoa());

		assertEquals(nomePessoa, clienteFuncionarioService.buscarPorId(idClienteFuncionario, empresaId).orElseThrow().getNomePessoa());
		assertEquals(nomePessoa, clienteFuncionarioService.findByPessoaDto(idPessoa, empresaId).getNomePessoa());
		assertTrue(clienteFuncionarioService.listar(empresaId).stream().allMatch(c -> c.getNomePessoa() != null));
	}

	@Test
	@Order(6)
	public void testUsuarioService() {
		String login = "sessao." + sufixo;
		UsuarioDto novo = new UsuarioDto();
		novo.setLogin(login);
		novo.setSenha(SENHA);
		novo.setLiberado(true);
		novo.setPessoaId(idPessoa);

		UsuarioDto salvo = usuarioService.salvar(novo);
		idUsuario = salvo.getId();
		assertEquals(nomePessoa, salvo.getPessoa());
		assertNotNull(salvo.getEmpresa());

		assertEquals(nomePessoa, usuarioService.buscarPorIdDto(idUsuario, empresaId).getPessoa());
		assertEquals(nomePessoa, usuarioService.buscaPorLoginDto(login, empresaId).getPessoa());
		assertTrue(usuarioService.listar(empresaId).stream().allMatch(u -> u.getPessoa() != null));
		assertTrue(usuarioService.listarPaginadoDto(empresaId, PageRequest.of(0, 10)).stream().allMatch(u -> u.getPessoa() != null));
	}

	@Test
	@Order(7)
	public void testRoleUsuarioService() {
		RoleUsuario roleUsuario = new RoleUsuario();
		Role acesso = new Role();
		acesso.setId(role("ROLE_ADMIN").getId());
		roleUsuario.setAcesso(acesso);
		roleUsuario.setUsuario(usuario(idUsuario));

		RoleUsuarioDTO salvo = roleUsuarioService.salvarDto(roleUsuario);
		idRoleUsuario = salvo.getId();
		assertEquals("ROLE_ADMIN", salvo.getAcessoNome());
		assertEquals(nomePessoa, salvo.getUsuarioNome());

		/* ROLE_USER, do cadastro do usuário, e ROLE_ADMIN */
		List<RoleUsuarioDTO> acessos = roleUsuarioService.findAllByUsuario(idUsuario, empresaId);
		assertEquals(2, acessos.size());
		assertTrue(acessos.stream().allMatch(r -> nomePessoa.equals(r.getUsuarioNome()) && r.getAcessoNome() != null));
	}

	@Test
	@Order(8)
	public void testEmpresaService() {
		EmpresaDTO atual = empresaService.buscaPorId(empresaId);
		assertNotNull(atual.getPlanoNome());
		assertTrue(atual.getPessoaNome().startsWith("Empresa Teste API"));

		Empresa empresa = new Empresa();
		empresa.setId(empresaId);
		Plano plano = new Plano();
		plano.setId(atual.getPlanoId());
		empresa.setPlano(plano);
		empresa.setPessoa(pessoa(atual.getPessoaId()));
		empresa.setLogoMarca("logo-sessao");
		empresa.setPlanoAtivo(true);
		empresa.setBloqueio(false);

		EmpresaDTO atualizada = empresaService.atualizar(empresa);
		assertEquals("logo-sessao", atualizada.getLogoMarca());
		assertEquals(atual.getPlanoNome(), atualizada.getPlanoNome());
		assertEquals(atual.getPessoaNome(), atualizada.getPessoaNome());
	}

	@Test
	@Order(9)
	public void testChamadoService() {
		tituloChamado = "Chamado Sessão " + sufixo;
		Chamado chamado = new Chamado();
		chamado.setTitulo(tituloChamado);
		chamado.setDescricao("Chamado do teste de sessão");
		chamado.setTipoChamado(TipoChamado.SUPORTE);
		chamado.setPrioridadeChamado(PrioridadeChamado.ALTA);
		chamado.setStatusChamado(StatusChamado.ABERTO);
		chamado.setAtendente(usuario(admin.usuarioId()));
		chamado.setCliente(usuario(cliente.usuarioId()));

		ChamadoDTO salvo = chamadoService.salvar(chamado);
		idChamado = salvo.getId();
		/* abertoUser é o usuário logado, que veio de uma sessão já fechada */
		assertEquals(admin.nome(), salvo.getAbertoUserNome());
		assertEquals(admin.nome(), salvo.getAtendenteNome());
		assertEquals(cliente.nome(), salvo.getClienteNome());

		ChamadoDTO lido = chamadoService.buscarPorId(idChamado, empresaId).orElseThrow();
		assertEquals(admin.nome(), lido.getAbertoUserNome());
		assertEquals(cliente.nome(), lido.getClienteNome());
	}

	@Test
	@Order(10)
	public void testMensagemService() {
		Mensagem mensagem = new Mensagem();
		mensagem.setConteudo("Mensagem Sessão " + sufixo);
		mensagem.setArquivo("anexo.pdf");
		mensagem.setChamado(chamado(idChamado));
		mensagem.setAtendente(usuario(admin.usuarioId()));
		mensagem.setCliente(usuario(cliente.usuarioId()));

		MensagemDTO salva = mensagemService.salvar(mensagem);
		idMensagem = salva.getId();
		assertEquals(tituloChamado, salva.getChamadoTitulo());
		assertEquals(admin.nome(), salva.getAtendenteNome());
		assertEquals(cliente.nome(), salva.getClienteNome());

		MensagemDTO lida = mensagemService.findAllByChamado(idChamado, empresaId).get(0);
		assertEquals(tituloChamado, lida.getChamadoTitulo());
		assertEquals(cliente.nome(), lida.getClienteNome());
	}

	@Test
	@Order(11)
	public void testPedidoService() {
		numeroPedido = "PED-SESSAO-" + sufixo;
		Pedido pedido = new Pedido();
		pedido.setNumeroPedido(numeroPedido);
		pedido.setStatusPedido(StatusPedido.AGUARDANDO_PAGAMENTO);
		pedido.setFormaPagamento(FormaPagamento.PIX);
		pedido.setTipoPedido(TipoPedido.VENDA);
		pedido.setDataPedido(LocalDate.now());
		pedido.setVendedor(usuario(admin.usuarioId()));
		pedido.setCliente(usuario(cliente.usuarioId()));

		PedidoDTO salvo = pedidoService.salvar(pedido);
		idPedido = salvo.getId();
		assertEquals(admin.nome(), salvo.getVendedorNome());
		assertEquals(cliente.nome(), salvo.getClienteNome());

		PedidoDTO lido = pedidoService.listar(empresaId).get(0);
		assertEquals(admin.nome(), lido.getVendedorNome());
		assertEquals(cliente.nome(), lido.getClienteNome());
	}

	@Test
	@Order(12)
	public void testItemPedidoService() {
		ItemPedido item = new ItemPedido();
		item.setQuantidade(2.0);
		item.setSubTotal(new BigDecimal("100.00"));
		item.setTotal(new BigDecimal("100.00"));
		item.setProduto(produto(idProduto));
		item.setPedido(pedido(idPedido));

		ItemPedidoDTO salvo = itemPedidoService.salvar(item);
		idItem = salvo.getId();
		assertEquals(nomeProduto, salvo.getProdutoNome());
		assertEquals(numeroPedido, salvo.getNumeroPedido());

		ItemPedidoDTO lido = itemPedidoService.findAllByPedido(idPedido, empresaId).get(0);
		assertEquals(nomeProduto, lido.getProdutoNome());
		assertEquals(numeroPedido, lido.getNumeroPedido());
	}

	@Test
	@Order(13)
	public void testMovimentacaoProdutoService() {
		MovimentacaoProduto movimentacao = new MovimentacaoProduto();
		movimentacao.setQuantidade(1.0);
		movimentacao.setValor(new BigDecimal("50.00"));
		movimentacao.setTipoMovimentacaoProduto(TipoMovimentacaoProduto.SAIDA_VENDA);
		movimentacao.setProduto(produto(idProduto));
		movimentacao.setPedido(pedido(idPedido));

		MovimentacaoProdutoDTO salva = movimentacaoProdutoService.salvar(movimentacao);
		idMovimentacao = salva.getId();
		assertEquals(nomeProduto, salva.getProdutoNome());
		assertEquals(numeroPedido, salva.getNumeroPedido());

		MovimentacaoProdutoDTO lida = movimentacaoProdutoService.listar(empresaId).get(0);
		assertEquals(nomeProduto, lida.getProdutoNome());
		assertEquals(numeroPedido, lida.getNumeroPedido());
	}

	// ==================== Entidades lidas fora da sessão ====================

	@Test
	@Order(20)
	public void testToStringEHashCodeForaDaSessao() {
		List<Object> entidades = List.of(
				categoriaRepository.buscarPorId(idCategoria, empresaId).orElseThrow(),
				produtoRepository.buscarPorId(idProduto, empresaId).orElseThrow(),
				pessoaRepository.buscarPorId(idPessoa, empresaId).orElseThrow(),
				clienteFuncionarioRepository.buscarPorId(idClienteFuncionario, empresaId).orElseThrow(),
				usuarioRepository.buscarPorId(idUsuario, empresaId).orElseThrow(),
				roleUsuarioRepository.buscarPorIdEEmpresa(idRoleUsuario, empresaId).orElseThrow(),
				empresaRepository.buscarPorId(empresaId),
				chamadoRepository.buscarPorId(idChamado, empresaId).orElseThrow(),
				mensagemRepository.buscarPorId(idMensagem, empresaId).orElseThrow(),
				pedidoRepository.buscarPorId(idPedido, empresaId).orElseThrow(),
				itemPedidoRepository.buscarPorId(idItem, empresaId).orElseThrow(),
				movimentacaoProdutoRepository.buscarPorId(idMovimentacao, empresaId).orElseThrow(),
				planoRepository.findAll().get(0),
				role("ROLE_ADMIN"));

		assertAll(entidades.stream().map(entidade -> (Executable) () -> {
			String nome = entidade.getClass().getSimpleName();
			String texto = assertDoesNotThrow(entidade::toString, nome + ".toString()");
			assertDoesNotThrow(entidade::hashCode, nome + ".hashCode()");
			assertTrue(texto.startsWith(nome + "("), texto);
			assertFalse(texto.contains("empresa="), "toString não deve trazer relacionamento: " + texto);
		}));
	}

	@Test
	@Order(21)
	public void testToStringSemDadosSensiveis() {
		Usuario usuario = usuarioRepository.buscarPorId(idUsuario, empresaId).orElseThrow();
		String textoUsuario = usuario.toString();
		assertTrue(textoUsuario.contains(usuario.getLogin()), textoUsuario);
		assertFalse(textoUsuario.contains(usuario.getSenha()), "toString não pode trazer a senha");
		assertFalse(textoUsuario.contains("senha"), textoUsuario);
		assertFalse(textoUsuario.contains("token"), textoUsuario);

		Pessoa pessoa = pessoaRepository.buscarPorId(idPessoa, empresaId).orElseThrow();
		String textoPessoa = pessoa.toString();
		assertTrue(textoPessoa.contains(nomePessoa), textoPessoa);
		assertFalse(textoPessoa.contains(pessoa.getCpf()), "toString não pode trazer o CPF");
		assertFalse(textoPessoa.contains(pessoa.getCnpj()), "toString não pode trazer o CNPJ");
		assertFalse(textoPessoa.contains(pessoa.getEmail()), "toString não pode trazer o e-mail");
	}

	@Test
	@Order(22)
	public void testEqualsComProxyLazyForaDaSessao() {
		Chamado chamado = chamadoRepository.buscarPorId(idChamado, empresaId).orElseThrow();
		Usuario clienteProxy = chamado.getCliente();
		Usuario atendenteProxy = chamado.getAtendente();
		Usuario clienteDoBanco = usuarioRepository.buscarPorId(cliente.usuarioId(), empresaId).orElseThrow();
		assertFalse(Hibernate.isInitialized(clienteProxy));

		assertEquals(clienteDoBanco, clienteProxy);
		assertEquals(clienteProxy, clienteDoBanco);
		assertEquals(clienteDoBanco.hashCode(), clienteProxy.hashCode());
		assertTrue(Set.of(clienteDoBanco).contains(clienteProxy));
		assertNotEquals(atendenteProxy, clienteProxy);

		/* Comparar não carregou o proxy */
		assertFalse(Hibernate.isInitialized(clienteProxy));
	}

	// ==================== Objetos como chegam do request: só o id ====================

	private static Categoria categoria(Long id) {
		Categoria categoria = new Categoria();
		categoria.setId(id);
		return categoria;
	}

	private static Pessoa pessoa(Long id) {
		Pessoa pessoa = new Pessoa();
		pessoa.setId(id);
		return pessoa;
	}

	private static Usuario usuario(Long id) {
		Usuario usuario = new Usuario();
		usuario.setId(id);
		return usuario;
	}

	private static Produto produto(Long id) {
		Produto produto = new Produto();
		produto.setId(id);
		return produto;
	}

	private static Pedido pedido(Long id) {
		Pedido pedido = new Pedido();
		pedido.setId(id);
		return pedido;
	}

	private static Chamado chamado(Long id) {
		Chamado chamado = new Chamado();
		chamado.setId(id);
		return chamado;
	}

}
