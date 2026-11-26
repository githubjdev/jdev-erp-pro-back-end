package br.com.jdeverp.pro.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.ItemPedidoDTO;
import br.com.jdeverp.pro.dto.ItemPedidoEntradaDTO;
import br.com.jdeverp.pro.dto.PedidoComItensDTO;
import br.com.jdeverp.pro.dto.PedidoDTO;
import br.com.jdeverp.pro.enums.StatusPedido;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ItemPedidoRepository;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;
import br.com.jdeverp.pro.util.DtoUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
@Transactional(readOnly = true)
public class PedidoService {

	@Autowired /* Injeção de dependência */
	private PedidoRepository pedidoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	@Autowired
	private ItemPedidoRepository itemPedidoRepository;

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private PedidoCalculoService pedidoCalculoService;

	@Autowired
	private EstoquePedidoService estoquePedidoService;

	/*Perfis que podem mudar o status do pedido*/
	public static final String[] ACESSOS_ALTERAR_STATUS = { "ROLE_ADMIN", "ROLE_VENDEDOR", "ROLE_GERENTE" };

	/*Endpoint antigo: o servidor define status inicial, datas e totais (não aceita do cliente)*/
	@Transactional
	public PedidoDTO salvar(Pedido pedido) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (pedidoRepository.existePorNumeroPedido(pedido.getNumeroPedido(), empresaId)) {
			throw new MsgApiException("Já existe um pedido com o mesmo número para a empresa logada.");
		}

		pedido.setVendedor(validarUsuario(pedido.getVendedor(), empresaId));
		pedido.setCliente(validarUsuario(pedido.getCliente(), empresaId));
		pedido.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		pedido.setStatusPedido(StatusPedido.AGUARDANDO_PAGAMENTO);
		pedido.setDataPagamento(null);
		pedido.setDataCancelamento(null);
		pedido.setEstoqueBaixado(false);
		pedidoCalculoService.calcularPedido(pedido, List.of());
		return converterParaDto(pedidoRepository.saveAndFlush(pedido));
	}

	/*Endpoint antigo: só atualiza dados do pedido editável; status, datas e totais não vêm do cliente*/
	@Transactional
	public PedidoDTO atualizar(Pedido pedido) {
		if (pedido.getId() == null) {
			throw new MsgApiException("Id do pedido não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		Pedido existente = pedidoRepository.buscarPorId(pedido.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Pedido não encontrado para a empresa logada."));

		if (pedidoRepository.existePorNumeroPedidoDiferenteId(pedido.getId(), pedido.getNumeroPedido(), empresaId)) {
			throw new MsgApiException("Já existe outro pedido com o mesmo número para a empresa logada.");
		}

		exigirEditavel(existente);

		List<ItemPedido> itensAtuais = itemPedidoRepository.findAll(existente.getId(), empresaId);
		if (!itensAtuais.isEmpty() && existente.getTipoPedido() != pedido.getTipoPedido()) {
			throw new MsgApiException("Não é possível trocar o tipo de um pedido que já tem itens, pois o preço dos itens depende do tipo. Use atualizarComItens.");
		}

		existente.setNumeroPedido(pedido.getNumeroPedido());
		existente.setFormaPagamento(pedido.getFormaPagamento());
		existente.setTipoPedido(pedido.getTipoPedido());
		existente.setDataPedido(pedido.getDataPedido());
		existente.setDesconto(pedido.getDesconto());
		existente.setFrete(pedido.getFrete());
		existente.setTaxas(pedido.getTaxas());
		existente.setObservacao(pedido.getObservacao());
		existente.setVendedor(validarUsuario(pedido.getVendedor(), empresaId));
		existente.setCliente(validarUsuario(pedido.getCliente(), empresaId));
		pedidoCalculoService.calcularPedido(existente, itensAtuais);
		return converterParaDto(pedidoRepository.saveAndFlush(existente));
	}

	/*Cria o pedido já com os itens, tudo na mesma transação. Preço e totais são calculados aqui.*/
	@Transactional
	public PedidoDTO salvarComItens(PedidoComItensDTO dto) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (pedidoRepository.existePorNumeroPedido(dto.getNumeroPedido(), empresaId)) {
			throw new MsgApiException("Já existe um pedido com o mesmo número para a empresa logada.");
		}

		Pedido pedido = new Pedido();
		preencherDados(pedido, dto, empresaId);
		pedido.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		pedido.setStatusPedido(StatusPedido.AGUARDANDO_PAGAMENTO);
		pedido.setEstoqueBaixado(false);

		List<ItemPedido> itens = montarItens(pedido, dto.getItens(), empresaId);
		pedidoCalculoService.calcularPedido(pedido, itens);

		pedido = pedidoRepository.saveAndFlush(pedido);
		for (ItemPedido item : itens) {
			item.setPedido(pedido);
		}
		itemPedidoRepository.saveAllAndFlush(itens);
		return converterComItens(pedido, itens);
	}

	/*Substitui os dados e a lista de itens de um pedido ainda editável*/
	@Transactional
	public PedidoDTO atualizarComItens(PedidoComItensDTO dto) {
		if (dto.getId() == null) {
			throw new MsgApiException("Id do pedido não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		Pedido pedido = pedidoRepository.buscarPorId(dto.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Pedido não encontrado para a empresa logada."));

		if (pedidoRepository.existePorNumeroPedidoDiferenteId(dto.getId(), dto.getNumeroPedido(), empresaId)) {
			throw new MsgApiException("Já existe outro pedido com o mesmo número para a empresa logada.");
		}

		exigirEditavel(pedido);
		preencherDados(pedido, dto, empresaId);

		List<ItemPedido> itens = montarItens(pedido, dto.getItens(), empresaId);
		pedidoCalculoService.calcularPedido(pedido, itens);

		List<Long> idsAntigos = itemPedidoRepository.findAll(pedido.getId(), empresaId).stream().map(ItemPedido::getId).toList();
		if (!idsAntigos.isEmpty()) {
			itemPedidoRepository.deletarAllById(idsAntigos, empresaId);
		}
		pedido = pedidoRepository.saveAndFlush(pedido);
		itemPedidoRepository.saveAllAndFlush(itens);
		return converterComItens(pedido, itens);
	}

	/**
	 * Muda o status respeitando a máquina de status. Só ROLE_ADMIN, ROLE_VENDEDOR e ROLE_GERENTE.
	 * PAGAMENTO_APROVADO movimenta o estoque (VENDA e BRINDE saem, COMPRA entra); CANCELADO e DEVOLVIDO estornam se já baixou.
	 */
	@Transactional
	public PedidoDTO alterarStatus(Long id, StatusPedido novoStatus) {
		if (!usuarioLogadoService.possuiAlgumAcesso(ACESSOS_ALTERAR_STATUS)) {
			throw new MsgApiException("Apenas ADMIN, VENDEDOR ou GERENTE podem alterar o status do pedido.", HttpStatus.FORBIDDEN);
		}
		if (novoStatus == null) {
			throw new MsgApiException("Novo status do pedido deve ser informado.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		Pedido pedido = pedidoRepository.buscarPorId(id, empresaId)
				.orElseThrow(() -> new MsgApiException("Pedido não encontrado para a empresa logada."));
		/*Trava o pedido: dois pedidos de mudança de status simultâneos não baixam o estoque duas vezes*/
		entityManager.lock(pedido, LockModeType.PESSIMISTIC_WRITE);
		entityManager.refresh(pedido);

		StatusPedido atual = pedido.getStatusPedido();
		if (!atual.podeIrPara(novoStatus)) {
			throw new MsgApiException("Não é possível mudar o pedido de '" + atual.getDescricao() + "' para '"
					+ novoStatus.getDescricao() + "'. Permitidos: " + atual.proximosPermitidos().stream()
							.map(StatusPedido::getDescricao).toList());
		}

		boolean movimentaEstoque = EstoquePedidoService.movimentaEstoque(pedido.getTipoPedido());

		if (novoStatus == StatusPedido.PAGAMENTO_APROVADO) {
			if (movimentaEstoque && !Boolean.TRUE.equals(pedido.getEstoqueBaixado())) {
				estoquePedidoService.baixar(pedido);
			}
			pedido.setDataPagamento(LocalDate.now());
		}

		if ((novoStatus == StatusPedido.CANCELADO || novoStatus == StatusPedido.DEVOLVIDO)
				&& Boolean.TRUE.equals(pedido.getEstoqueBaixado())) {
			estoquePedidoService.estornar(pedido);
		}

		if (novoStatus == StatusPedido.CANCELADO) {
			pedido.setDataCancelamento(LocalDate.now());
		}

		pedido.setStatusPedido(novoStatus);
		return converterParaDto(pedidoRepository.saveAndFlush(pedido));
	}

	/*Só pedido aguardando pagamento ou com pagamento recusado aceita alterar itens e valores*/
	private void exigirEditavel(Pedido pedido) {
		pedidoCalculoService.exigirEditavel(pedido);
	}


	private void preencherDados(Pedido pedido, PedidoComItensDTO dto, Long empresaId) {
		pedido.setNumeroPedido(dto.getNumeroPedido());
		pedido.setFormaPagamento(dto.getFormaPagamento());
		pedido.setTipoPedido(dto.getTipoPedido());
		pedido.setDataPedido(dto.getDataPedido() != null ? dto.getDataPedido() : LocalDate.now());
		pedido.setDesconto(dto.getDesconto());
		pedido.setFrete(dto.getFrete());
		pedido.setTaxas(dto.getTaxas());
		pedido.setObservacao(dto.getObservacao());

		Usuario vendedor = new Usuario();
		vendedor.setId(dto.getVendedorId());
		Usuario cliente = new Usuario();
		cliente.setId(dto.getClienteId());
		pedido.setVendedor(validarUsuario(vendedor, empresaId));
		pedido.setCliente(validarUsuario(cliente, empresaId));
	}

	/*Monta os itens: preço vem do produto, sem produto repetido, quantidade e desconto validados*/
	private List<ItemPedido> montarItens(Pedido pedido, List<ItemPedidoEntradaDTO> entradas, Long empresaId) {
		if (entradas == null || entradas.isEmpty()) {
			throw new MsgApiException("O pedido deve ter pelo menos um item.");
		}

		Set<Long> ids = new HashSet<>();
		for (ItemPedidoEntradaDTO entrada : entradas) {
			if (!ids.add(entrada.getProdutoId())) {
				throw new MsgApiException("O produto " + entrada.getProdutoId() + " foi informado mais de uma vez no pedido.");
			}
		}

		Map<Long, Produto> produtos = produtoRepository.buscarPorIds(ids, empresaId).stream()
				.collect(Collectors.toMap(Produto::getId, p -> p));

		List<ItemPedido> itens = new ArrayList<>();
		for (ItemPedidoEntradaDTO entrada : entradas) {
			Produto produto = produtos.get(entrada.getProdutoId());
			if (produto == null) {
				throw new MsgApiException("Produto " + entrada.getProdutoId() + " não encontrado para a empresa logada.");
			}

			ItemPedido item = new ItemPedido();
			item.setPedido(pedido);
			item.setProduto(produto);
			item.setEmpresa(pedido.getEmpresa() != null ? pedido.getEmpresa() : usuarioLogadoService.getEmpresaLogada());
			item.setQuantidade(entrada.getQuantidade());
			item.setPrecoUnitario(pedidoCalculoService.precoDoItem(produto, pedido.getTipoPedido()));
			item.setDesconto(entrada.getDesconto());
			pedidoCalculoService.calcularItem(item);
			itens.add(item);
		}
		return itens;
	}

	private PedidoDTO converterComItens(Pedido pedido, List<ItemPedido> itens) {
		PedidoDTO dto = converterParaDto(pedido);
		dto.setItens(itens.stream().map(i -> {
			ItemPedidoDTO d = new ItemPedidoDTO();
			d.setId(i.getId());
			d.setQuantidade(i.getQuantidade());
			d.setPrecoUnitario(i.getPrecoUnitario());
			d.setSubTotal(i.getSubTotal());
			d.setDesconto(i.getDesconto());
			d.setTotal(i.getTotal());
			d.setProdutoId(i.getProduto().getId());
			d.setProdutoNome(i.getProduto().getNome());
			d.setPedidoId(pedido.getId());
			d.setNumeroPedido(pedido.getNumeroPedido());
			d.setEmpresaId(pedido.getEmpresa().getId());
			return d;
		}).toList());
		return dto;
	}

	private Usuario validarUsuario(Usuario usuario, Long empresaId) {
		if (usuario == null || usuario.getId() == null) {
			throw new MsgApiException("Cliente e vendedor devem ser informados para o pedido.");
		}

		return usuarioRepository.buscarPorId(usuario.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Usuário relacionado não encontrado para a empresa logada."));
	}

	public List<PedidoDTO> findAll(Long idEmpresa) {

		return converterListaParaDto(pedidoRepository.findAll(idEmpresa));
	}

	public List<PedidoDTO> buscaPorNumeroPedido(String numeroPedido, Long idEmpresa) {
		return converterListaParaDto(pedidoRepository.buscaPorNumeroPedido(numeroPedido, idEmpresa));
	}

	public boolean existePorNumeroPedido(String numeroPedido, Long idEmpresa) {
		return pedidoRepository.existePorNumeroPedido(numeroPedido, idEmpresa);
	}

	public boolean existePorNumeroPedidoDiferenteId(Long id, String numeroPedido, Long idEmpresa) {
		return pedidoRepository.existePorNumeroPedidoDiferenteId(id, numeroPedido, idEmpresa);
	}

	@Transactional
	public void deleteById(Long id, Long idEmpresa) {
		if (!pedidoRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Pedido não encontrado ou já foi deletado.");
		}

		pedidoRepository.deleteById(id, idEmpresa);
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

		if (pedidoRepository.total(empresaID) == 0) {
			throw new MsgApiException("Nenhum pedido encontrado para deletar ou todos já foram deletados.");
		}

		return pedidoRepository.deleteAll(empresaID);
	}

	@Transactional
	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		exigirAdminParaExclusaoEmLote();

		List<Long> encontrados = pedidoRepository.buscarPorIds(ids, empresaId).stream().map(Pedido::getId).toList();
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
			throw new MsgApiException("Pedidos não encontrados ou já deletados: " + naoEncontrados);
		}

		pedidoRepository.deletarAllById(ids, empresaId);
	}

	public List<PedidoDTO> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return converterListaParaDto(pedidoRepository.buscarPorIds(ids, empresaId));
	}

	public boolean existsById(Long id, Long empresaId) {
		return pedidoRepository.existsById(id, empresaId);
	}

	public List<PedidoDTO> listar(Long empresaId) {
		return converterListaParaDto(pedidoRepository.listar(empresaId));
	}

	public Optional<PedidoDTO> buscarPorId(Long id, Long empresaId) {
		return pedidoRepository.buscarPorId(id, empresaId).map(this::converterParaDto);
	}

	public long total(Long empresaId) {
		return pedidoRepository.total(empresaId);
	}

	public Page<PedidoDTO> listarPaginado(Long empresaId, Pageable pageable) {
		return pedidoRepository.listarPaginado(empresaId, pageable).map(this::converterParaDto);
	}

	// ====================Conversão para DTO====================

	private PedidoDTO converterParaDto(Pedido pedido) {
		PedidoDTO dto = new PedidoDTO();
		dto.setId(pedido.getId());
		dto.setNumeroPedido(pedido.getNumeroPedido());
		dto.setStatusPedido(pedido.getStatusPedido());
		dto.setFormaPagamento(pedido.getFormaPagamento());
		dto.setTipoPedido(pedido.getTipoPedido());
		dto.setDataPedido(pedido.getDataPedido());
		dto.setDataPagamento(pedido.getDataPagamento());
		dto.setDataCancelamento(pedido.getDataCancelamento());
		dto.setSubTotal(pedido.getSubTotal());
		dto.setDesconto(pedido.getDesconto());
		dto.setFrete(pedido.getFrete());
		dto.setTaxas(pedido.getTaxas());
		dto.setTotal(pedido.getTotal());
		dto.setObservacao(pedido.getObservacao());
		dto.setVendedorId(pedido.getVendedor().getId());
		dto.setVendedorNome(DtoUtil.nomeUsuario(pedido.getVendedor()));
		dto.setClienteId(pedido.getCliente().getId());
		dto.setClienteNome(DtoUtil.nomeUsuario(pedido.getCliente()));
		dto.setEmpresaId(pedido.getEmpresa().getId());
		dto.setEstoqueBaixado(pedido.getEstoqueBaixado());
		return dto;
	}

	private List<PedidoDTO> converterListaParaDto(List<Pedido> pedidos) {
		return pedidos.stream().map(this::converterParaDto).toList();
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
