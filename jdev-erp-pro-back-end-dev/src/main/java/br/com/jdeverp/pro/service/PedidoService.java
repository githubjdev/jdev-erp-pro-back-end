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

import br.com.jdeverp.pro.dto.PedidoDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;
import br.com.jdeverp.pro.util.DtoUtil;
import jakarta.persistence.EntityManager;
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

	@Transactional
	public PedidoDTO salvar(Pedido pedido) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (pedidoRepository.existePorNumeroPedido(pedido.getNumeroPedido(), empresaId)) {
			throw new MsgApiException("Já existe um pedido com o mesmo número para a empresa logada.");
		}

		pedido.setVendedor(validarUsuario(pedido.getVendedor(), empresaId));
		pedido.setCliente(validarUsuario(pedido.getCliente(), empresaId));
		pedido.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return converterParaDto(pedidoRepository.saveAndFlush(pedido));
	}

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

		existente.setNumeroPedido(pedido.getNumeroPedido());
		existente.setStatusPedido(pedido.getStatusPedido());
		existente.setFormaPagamento(pedido.getFormaPagamento());
		existente.setTipoPedido(pedido.getTipoPedido());
		existente.setDataPedido(pedido.getDataPedido());
		existente.setDataPagamento(pedido.getDataPagamento());
		existente.setDataCancelamento(pedido.getDataCancelamento());
		existente.setSubTotal(pedido.getSubTotal());
		existente.setDesconto(pedido.getDesconto());
		existente.setFrete(pedido.getFrete());
		existente.setTaxas(pedido.getTaxas());
		existente.setTotal(pedido.getTotal());
		existente.setObservacao(pedido.getObservacao());
		existente.setVendedor(validarUsuario(pedido.getVendedor(), empresaId));
		existente.setCliente(validarUsuario(pedido.getCliente(), empresaId));
		return converterParaDto(pedidoRepository.saveAndFlush(existente));
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
