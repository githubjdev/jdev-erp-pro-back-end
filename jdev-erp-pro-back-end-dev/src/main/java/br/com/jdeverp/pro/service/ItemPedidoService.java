package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.repository.ItemPedidoRepository;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class ItemPedidoService {

	@Autowired /* Injeção de dependência */
	private ItemPedidoRepository itemPedidoRepository;

	@Autowired
	private PedidoRepository pedidoRepository;

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	public ItemPedido salvar(ItemPedido itemPedido) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		Pedido pedido = validarPedido(itemPedido.getPedido(), empresaId);
		Produto produto = validarProduto(itemPedido.getProduto(), empresaId);

		if (itemPedidoRepository.existePorNomePorPedido(produto.getNome(), pedido.getId(), empresaId)) {
			throw new MsgApiException("O produto já foi incluído neste pedido.");
		}

		itemPedido.setPedido(pedido);
		itemPedido.setProduto(produto);
		itemPedido.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return itemPedidoRepository.saveAndFlush(itemPedido);
	}

	public ItemPedido atualizar(ItemPedido itemPedido) {
		if (itemPedido.getId() == null) {
			throw new MsgApiException("Id do item do pedido não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		ItemPedido existente = itemPedidoRepository.buscarPorId(itemPedido.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Item do pedido não encontrado para a empresa logada."));
		Pedido pedido = validarPedido(itemPedido.getPedido(), empresaId);
		Produto produto = validarProduto(itemPedido.getProduto(), empresaId);

		if (itemPedidoRepository.existePorNomeDiferenteIdPorPedido(
				itemPedido.getId(), produto.getNome(), pedido.getId(), empresaId)) {
			throw new MsgApiException("O produto já foi incluído neste pedido.");
		}

		existente.setQuantidade(itemPedido.getQuantidade());
		existente.setSubTotal(itemPedido.getSubTotal());
		existente.setDesconto(itemPedido.getDesconto());
		existente.setTotal(itemPedido.getTotal());
		existente.setProduto(produto);
		existente.setPedido(pedido);
		return itemPedidoRepository.saveAndFlush(existente);
	}

	private Pedido validarPedido(Pedido pedido, Long empresaId) {
		if (pedido == null || pedido.getId() == null) {
			throw new MsgApiException("Pedido deve ser informado para o item.");
		}

		return pedidoRepository.buscarPorId(pedido.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Pedido não encontrado para a empresa logada."));
	}

	private Produto validarProduto(Produto produto, Long empresaId) {
		if (produto == null || produto.getId() == null) {
			throw new MsgApiException("Produto deve ser informado para o item.");
		}

		return produtoRepository.buscarPorId(produto.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Produto não encontrado para a empresa logada."));
	}

	public List<ItemPedido> findAll(Long idPedido, Long idEmpresa) {
		
		return itemPedidoRepository.findAll(idPedido, idEmpresa);
	}

	public List<ItemPedido> buscaPorNome(String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.buscaPorNome(nome, idPedido, idEmpresa);
	}

	public boolean existePorNome(String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.existePorNome(nome, idPedido, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.existePorNomeDiferenteId(id, nome, idPedido, idEmpresa);
	}

	public void deleteById(Long id, Long idPedido, Long idEmpresa) {

		if (!itemPedidoRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Item do pedido não encontrado para a empresa logada, portanto não pode ser deletado.");
		}

		itemPedidoRepository.deleteById(id, idPedido, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return itemPedidoRepository.deleteAll(empresaID);
	}

	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		itemPedidoRepository.deletarAllById(ids, empresaId);
	}

	public List<ItemPedido> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return itemPedidoRepository.buscarPorIds(ids, empresaId);
	}

	public boolean existsById(Long id, Long empresaId) {
		return itemPedidoRepository.existsById(id, empresaId);
	}

	public List<ItemPedido> listar(Long empresaId) {
		return itemPedidoRepository.listar(empresaId);
	}

	public Optional<ItemPedido> buscarPorId(Long id, Long empresaId) {
		return itemPedidoRepository.buscarPorId(id, empresaId);
	}

	public long total(Long empresaId) {
		return itemPedidoRepository.total(empresaId);
	}

	public Page<ItemPedido> listarPaginado(Long empresaId, Pageable pageable) {
		return itemPedidoRepository.listarPaginado(empresaId, pageable);
	}

	// ====================Métodos específicos para Pedido====================

	public List<ItemPedido> findAllByPedido(Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.findAllByPedido(idPedido, idEmpresa);
	}

	public List<ItemPedido> buscaPorNomePorPedido(String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.buscaPorNomePorPedido(nome, idPedido, idEmpresa);
	}

	public boolean existePorNomePorPedido(String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.existePorNomePorPedido(nome, idPedido, idEmpresa);
	}

	public boolean existePorNomeDiferenteIdPorPedido(Long id, String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.existePorNomeDiferenteIdPorPedido(id, nome, idPedido, idEmpresa);
	}

	public void deleteByIdAndPedido(Long id, Long idPedido, Long idEmpresa) {
		itemPedidoRepository.deleteByIdAndPedido(id, idPedido, idEmpresa);
	}

	// ====================dentro dos métodos do
	// service===============================

	// Verificar se está em uso
	// Realizar um consulta com integração para saber se pode deletar
	// Fazer copia e backup
	// Fazer inumeras validações de regra de negocio
	// Fazer validações
	// Lançar exeções
	// Escrever regras de negócio

}
