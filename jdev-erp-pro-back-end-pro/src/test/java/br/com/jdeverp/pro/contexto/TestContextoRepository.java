package br.com.jdeverp.pro.contexto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;

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
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.CategoriaRepository;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import br.com.jdeverp.pro.repository.ClienteFuncionarioRepository;
import br.com.jdeverp.pro.repository.EmpresaRepository;
import br.com.jdeverp.pro.repository.ItemPedidoRepository;
import br.com.jdeverp.pro.repository.MensagemRepository;
import br.com.jdeverp.pro.repository.MovimentacaoProdutoRepository;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.PessoaRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;

/**
 * Base dos testes de repository: {@link TestContextoSpring} (banco dev, com
 * rollback no fim de cada teste) mais a criação dos registros de que as
 * consultas precisam, sempre na empresa 1 do seed.
 *
 * <p>Os nomes levam um sufixo único, para não colidir com os dados que já
 * existem no banco dev. Toda consulta filtrada por empresa também é conferida
 * com {@link #EMPRESA_INEXISTENTE}: a mesma busca, em outra empresa, não pode
 * devolver o registro.
 */
public abstract class TestContextoRepository extends TestContextoSpring {

	/** Empresa do seed (carga inicial). */
	protected static final Long EMPRESA_ID = 1L;

	/** Empresa que não existe: uma consulta filtrada por ela nunca devolve nada. */
	protected static final Long EMPRESA_INEXISTENTE = -1L;

	private static final AtomicInteger CONTADOR = new AtomicInteger();

	@Autowired protected EmpresaRepository empresaRepository;
	@Autowired protected PessoaRepository pessoaRepository;
	@Autowired protected ClienteFuncionarioRepository clienteFuncionarioRepository;
	@Autowired protected UsuarioRepository usuarioRepository;
	@Autowired protected CategoriaRepository categoriaRepository;
	@Autowired protected ProdutoRepository produtoRepository;
	@Autowired protected PedidoRepository pedidoRepository;
	@Autowired protected ItemPedidoRepository itemPedidoRepository;
	@Autowired protected MovimentacaoProdutoRepository movimentacaoProdutoRepository;
	@Autowired protected ChamadoRepository chamadoRepository;
	@Autowired protected MensagemRepository mensagemRepository;

	/** Sufixo desta execução. */
	protected final String sufixo = Long.toString(System.nanoTime(), 36);

	protected Empresa empresa() {
		return empresaRepository.findById(EMPRESA_ID).orElseThrow();
	}

	/** Texto único nesta execução: "texto sufixo n". */
	protected String unico(String texto) {
		return texto + " " + sufixo + " " + CONTADOR.incrementAndGet();
	}

	protected Pessoa novaPessoa(String nome) {
		Pessoa pessoa = new Pessoa();
		pessoa.setNome(nome);
		pessoa.setTipoPessoa(TipoPessoa.FISICA);
		pessoa.setCpf(TestContextoApi.gerarCpf());
		pessoa.setEmail("repo." + sufixo + "." + CONTADOR.incrementAndGet() + "@teste.com.br");
		pessoa.setTelefone("44 99999-0000");
		pessoa.setAtivo(true);
		pessoa.setDataCadastro(LocalDate.now());
		pessoa.setCep("87000-000");
		pessoa.setLogradouro("Rua Teste");
		pessoa.setBairro("Centro");
		pessoa.setEstado("PR");
		pessoa.setCidade("Maringá");
		pessoa.setPais("Brasil");
		pessoa.setEmpresa(empresa());
		return pessoaRepository.saveAndFlush(pessoa);
	}

	protected ClienteFuncionario novoClienteFuncionario(Pessoa pessoa) {
		ClienteFuncionario clienteFuncionario = new ClienteFuncionario();
		clienteFuncionario.setTipoClienteFuncionario(TipoClienteFuncionario.CLIENTE);
		clienteFuncionario.setPessoa(pessoa);
		clienteFuncionario.setEmpresa(empresa());
		return clienteFuncionarioRepository.saveAndFlush(clienteFuncionario);
	}

	/** Pessoa, cliente/funcionário e usuário (sem roles), ligados como no cadastro real. */
	protected Usuario novoUsuario(String nomePessoa) {
		ClienteFuncionario clienteFuncionario = novoClienteFuncionario(novaPessoa(nomePessoa));

		Usuario usuario = new Usuario();
		usuario.setLogin("repo." + sufixo + "." + CONTADOR.incrementAndGet());
		/* senha tem unique no banco */
		usuario.setSenha("hash-de-teste-" + sufixo + "-" + CONTADOR.incrementAndGet());
		usuario.setLiberado(true);
		usuario.setClienteFuncionario(clienteFuncionario);
		usuario.setEmpresa(empresa());
		usuario = usuarioRepository.saveAndFlush(usuario);

		clienteFuncionario.setUsuario(usuario);
		clienteFuncionarioRepository.saveAndFlush(clienteFuncionario);
		return usuario;
	}

	protected Categoria novaCategoria(String nome) {
		Categoria categoria = new Categoria();
		categoria.setNome(nome);
		categoria.setEmpresa(empresa());
		return categoriaRepository.saveAndFlush(categoria);
	}

	protected Produto novoProduto(String nome) {
		Produto produto = new Produto();
		produto.setNome(nome);
		produto.setDescricao("Produto de teste");
		produto.setImagem("imagem-base64");
		produto.setPreco(10.0);
		produto.setEstoque(5.0);
		produto.setEstoqueMinimo(1.0);
		produto.setSku("SKU-" + sufixo + "-" + CONTADOR.incrementAndGet());
		produto.setUnidadeMedida(UnidadeMedida.UNIDADE);
		produto.setCategoria(novaCategoria(unico("Categoria")));
		produto.setEmpresa(empresa());
		return produtoRepository.saveAndFlush(produto);
	}

	protected Pedido novoPedido(String numero, Usuario vendedor, Usuario cliente) {
		Pedido pedido = new Pedido();
		pedido.setNumeroPedido(numero);
		pedido.setStatusPedido(StatusPedido.AGUARDANDO_PAGAMENTO);
		pedido.setFormaPagamento(FormaPagamento.PIX);
		pedido.setTipoPedido(TipoPedido.VENDA);
		pedido.setDataPedido(LocalDate.now());
		pedido.setTotal(new BigDecimal("10.00"));
		pedido.setVendedor(vendedor);
		pedido.setCliente(cliente);
		pedido.setEmpresa(empresa());
		return pedidoRepository.saveAndFlush(pedido);
	}

	protected Pedido novoPedido(String numero) {
		Usuario usuario = novoUsuario(unico("Vendedor"));
		return novoPedido(numero, usuario, usuario);
	}

	protected ItemPedido novoItem(Pedido pedido, Produto produto) {
		ItemPedido item = new ItemPedido();
		item.setQuantidade(2.0);
		item.setTotal(new BigDecimal("20.00"));
		item.setPedido(pedido);
		item.setProduto(produto);
		item.setEmpresa(empresa());
		return itemPedidoRepository.saveAndFlush(item);
	}

	protected MovimentacaoProduto novaMovimentacao(Produto produto, Pedido pedido) {
		MovimentacaoProduto movimentacao = new MovimentacaoProduto();
		movimentacao.setQuantidade(3.0);
		movimentacao.setValor(new BigDecimal("30.00"));
		movimentacao.setDataMovimento(LocalDate.now());
		movimentacao.setTipoMovimentacaoProduto(TipoMovimentacaoProduto.ENTRADA_COMPRA);
		movimentacao.setProduto(produto);
		movimentacao.setPedido(pedido);
		movimentacao.setEmpresa(empresa());
		return movimentacaoProdutoRepository.saveAndFlush(movimentacao);
	}

	protected Chamado novoChamado(String titulo, Usuario atendente, Usuario cliente) {
		Chamado chamado = new Chamado();
		chamado.setTitulo(titulo);
		chamado.setDescricao("Descrição do chamado de teste");
		chamado.setTipoChamado(TipoChamado.SUPORTE);
		chamado.setPrioridadeChamado(PrioridadeChamado.MEDIA);
		chamado.setStatusChamado(StatusChamado.ABERTO);
		chamado.setDataAbertura(LocalDate.now());
		chamado.setAbertoUser(atendente);
		chamado.setAtendente(atendente);
		chamado.setCliente(cliente);
		chamado.setEmpresa(empresa());
		return chamadoRepository.saveAndFlush(chamado);
	}

	protected Mensagem novaMensagem(String conteudo, Chamado chamado, Usuario atendente, Usuario cliente, boolean lida) {
		Mensagem mensagem = new Mensagem();
		mensagem.setConteudo(conteudo);
		mensagem.setArquivo("arquivo-base64");
		mensagem.setDataEnvio(LocalDate.now());
		mensagem.setLida(lida);
		mensagem.setChamado(chamado);
		mensagem.setAtendente(atendente);
		mensagem.setCliente(cliente);
		mensagem.setEmpresa(empresa());
		return mensagemRepository.saveAndFlush(mensagem);
	}

}
