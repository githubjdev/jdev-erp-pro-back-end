package br.com.jdeverp.pro.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.ItemPedidoDTO;
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
@Transactional(readOnly = true)
public class ItemPedidoService {

	@Autowired /* Injeção de dependência */
	private ItemPedidoRepository itemPedidoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private PedidoRepository pedidoRepository;

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	@Transactional
	public ItemPedidoDTO salvar(ItemPedido itemPedido) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		Pedido pedido = validarPedido(itemPedido.getPedido(), empresaId);
		Produto produto = validarProduto(itemPedido.getProduto(), empresaId);

		if (itemPedidoRepository.existePorNomePorPedido(produto.getNome(), pedido.getId(), empresaId)) {
			throw new MsgApiException("O produto já foi incluído neste pedido.");
		}

		itemPedido.setPedido(pedido);
		itemPedido.setProduto(produto);
		itemPedido.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return converterParaDto(itemPedidoRepository.saveAndFlush(itemPedido));
	}

	@Transactional
	public ItemPedidoDTO atualizar(ItemPedido itemPedido) {
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
		return converterParaDto(itemPedidoRepository.saveAndFlush(existente));
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

	public List<ItemPedidoDTO> findAll(Long idPedido, Long idEmpresa) {

		return converterListaParaDto(itemPedidoRepository.findAll(idPedido, idEmpresa));
	}

	public List<ItemPedidoDTO> buscaPorNome(String nome, Long idPedido, Long idEmpresa) {
		return converterListaParaDto(itemPedidoRepository.buscaPorNome(nome, idPedido, idEmpresa));
	}

	public boolean existePorNome(String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.existePorNome(nome, idPedido, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.existePorNomeDiferenteId(id, nome, idPedido, idEmpresa);
	}

	@Transactional
	public void deleteById(Long id, Long idPedido, Long idEmpresa) {
		if (!itemPedidoRepository.buscarPorId(id, idEmpresa).filter(item -> item.getPedido() != null && item.getPedido().getId().equals(idPedido)).isPresent()) {
			throw new MsgApiException("Item do pedido não encontrado ou já foi deletado.");
		}

		itemPedidoRepository.deleteById(id, idPedido, idEmpresa);
	}

	/*Exclusão em lote apaga tudo da empresa: só ROLE_ADMIN (P21)*/
	private void exigirAdminParaExclusaoEmLote() {
		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem excluir registros em lote.", HttpStatus.FORBIDDEN);
		}
	}

	@Transactional
	public long deleteAll(Long empresaID) {
		exigirAdminParaExclusaoEmLote();

		if (itemPedidoRepository.total(empresaID) == 0) {
			throw new MsgApiException("Nenhum item de pedido encontrado para deletar ou todos já foram deletados.");
		}

		return itemPedidoRepository.deleteAll(empresaID);
	}

	@Transactional
	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		exigirAdminParaExclusaoEmLote();

		List<Long> encontrados = itemPedidoRepository.buscarPorIds(ids, empresaId).stream().map(ItemPedido::getId).toList();
		List<Long> naoEncontrados = new ArrayList<>();
		ids.forEach(id -> {
			if (!encontrados.contains(id)) {
				naoEncontrados.add(id);
			}
		});

		if (encontrados.isEmpty() && naoEncontrados.isEmpty()) {
			throw new MsgApiException("Nenhum registro informado para deletar.");
		}

		if (!naoEncontrados.isEmpty()) {
			throw new MsgApiException("Itens de pedido não encontrados ou já deletados: " + naoEncontrados);
		}

		itemPedidoRepository.deletarAllById(ids, empresaId);
	}

	public List<ItemPedidoDTO> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return converterListaParaDto(itemPedidoRepository.buscarPorIds(ids, empresaId));
	}

	public boolean existsById(Long id, Long empresaId) {
		return itemPedidoRepository.existsById(id, empresaId);
	}

	public List<ItemPedidoDTO> listar(Long empresaId) {
		return converterListaParaDto(itemPedidoRepository.listar(empresaId));
	}

	public Optional<ItemPedidoDTO> buscarPorId(Long id, Long empresaId) {
		return itemPedidoRepository.buscarPorId(id, empresaId).map(this::converterParaDto);
	}

	public long total(Long empresaId) {
		return itemPedidoRepository.total(empresaId);
	}

	public Page<ItemPedidoDTO> listarPaginado(Long empresaId, Pageable pageable) {
		return itemPedidoRepository.listarPaginado(empresaId, pageable).map(this::converterParaDto);
	}

	// ====================Métodos específicos para Pedido====================

	public List<ItemPedidoDTO> findAllByPedido(Long idPedido, Long idEmpresa) {
		return converterListaParaDto(itemPedidoRepository.findAllByPedido(idPedido, idEmpresa));
	}

	public List<ItemPedidoDTO> buscaPorNomePorPedido(String nome, Long idPedido, Long idEmpresa) {
		return converterListaParaDto(itemPedidoRepository.buscaPorNomePorPedido(nome, idPedido, idEmpresa));
	}

	public boolean existePorNomePorPedido(String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.existePorNomePorPedido(nome, idPedido, idEmpresa);
	}

	public boolean existePorNomeDiferenteIdPorPedido(Long id, String nome, Long idPedido, Long idEmpresa) {
		return itemPedidoRepository.existePorNomeDiferenteIdPorPedido(id, nome, idPedido, idEmpresa);
	}

	@Transactional
	public void deleteByIdAndPedido(Long id, Long idPedido, Long idEmpresa) {
		if (!itemPedidoRepository.buscarPorId(id, idEmpresa).filter(item -> item.getPedido() != null && item.getPedido().getId().equals(idPedido)).isPresent()) {
			throw new MsgApiException("Item do pedido não encontrado ou já foi deletado.");
		}

		itemPedidoRepository.deleteByIdAndPedido(id, idPedido, idEmpresa);
	}

	// ====================Conversão para DTO====================

	private ItemPedidoDTO converterParaDto(ItemPedido itemPedido) {
		ItemPedidoDTO dto = new ItemPedidoDTO();
		dto.setId(itemPedido.getId());
		dto.setQuantidade(itemPedido.getQuantidade());
		dto.setSubTotal(itemPedido.getSubTotal());
		dto.setDesconto(itemPedido.getDesconto());
		dto.setTotal(itemPedido.getTotal());
		dto.setProdutoId(itemPedido.getProduto().getId());
		dto.setProdutoNome(itemPedido.getProduto().getNome());
		dto.setPedidoId(itemPedido.getPedido().getId());
		dto.setNumeroPedido(itemPedido.getPedido().getNumeroPedido());
		dto.setEmpresaId(itemPedido.getEmpresa().getId());
		return dto;
	}

	private List<ItemPedidoDTO> converterListaParaDto(List<ItemPedido> itens) {
		return itens.stream().map(this::converterParaDto).toList();
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
