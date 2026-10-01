package br.com.jdeverp.pro.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.MovimentacaoProduto;
import br.com.jdeverp.pro.model.Pedido;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.repository.MovimentacaoProdutoRepository;
import br.com.jdeverp.pro.repository.PedidoRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class MovimentacaoProdutoService {

	@Autowired /* Injeção de dependência */
	private MovimentacaoProdutoRepository movimentacaoProdutoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private PedidoRepository pedidoRepository;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	public MovimentacaoProduto salvar(MovimentacaoProduto movimentacaoProduto) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();

		movimentacaoProduto.setProduto(validarProduto(movimentacaoProduto.getProduto(), empresaId));
		movimentacaoProduto.setPedido(validarPedido(movimentacaoProduto.getPedido(), empresaId));

		if (movimentacaoProduto.getDataMovimento() == null) {
			movimentacaoProduto.setDataMovimento(LocalDate.now());
		}

		movimentacaoProduto.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return movimentacaoProdutoRepository.saveAndFlush(movimentacaoProduto);
	}

	public MovimentacaoProduto atualizar(MovimentacaoProduto movimentacaoProduto) {
		if (movimentacaoProduto.getId() == null) {
			throw new MsgApiException("Id da movimentação de produto não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		MovimentacaoProduto existente = movimentacaoProdutoRepository.buscarPorId(movimentacaoProduto.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Movimentação de produto não encontrada para a empresa logada."));

		existente.setQuantidade(movimentacaoProduto.getQuantidade());
		existente.setValor(movimentacaoProduto.getValor());
		existente.setTipoMovimentacaoProduto(movimentacaoProduto.getTipoMovimentacaoProduto());
		if (movimentacaoProduto.getDataMovimento() != null) {
			existente.setDataMovimento(movimentacaoProduto.getDataMovimento());
		}
		existente.setProduto(validarProduto(movimentacaoProduto.getProduto(), empresaId));
		existente.setPedido(validarPedido(movimentacaoProduto.getPedido(), empresaId));
		return movimentacaoProdutoRepository.saveAndFlush(existente);
	}

	private Produto validarProduto(Produto produto, Long empresaId) {
		if (produto == null || produto.getId() == null) {
			throw new MsgApiException("Produto deve ser informado para a movimentação.");
		}

		return produtoRepository.buscarPorId(produto.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Produto não encontrado para a empresa logada."));
	}

	/*Pedido é opcional: pode ser movimentação de perda, extravio, descarte e não ter ligação com pedido*/
	private Pedido validarPedido(Pedido pedido, Long empresaId) {
		if (pedido == null || pedido.getId() == null) {
			return null;
		}

		return pedidoRepository.buscarPorId(pedido.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Pedido não encontrado para a empresa logada."));
	}

	public List<MovimentacaoProduto> findAll(Long idEmpresa) {
		
		return movimentacaoProdutoRepository.findAll(idEmpresa);
	}

	public List<MovimentacaoProduto> buscaPorNome(String nome, Long idEmpresa) {
		return movimentacaoProdutoRepository.buscaPorNome(nome, idEmpresa);
	}

	public boolean existePorNome(String nome, Long idEmpresa) {
		return movimentacaoProdutoRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return movimentacaoProdutoRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {
		if (!movimentacaoProdutoRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Movimentação de produto não encontrada ou já foi deletada.");
		}

		movimentacaoProdutoRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		if (movimentacaoProdutoRepository.total(empresaID) == 0) {
			throw new MsgApiException("Nenhuma movimentação de produto encontrada para deletar ou todas já foram deletadas.");
		}

		return movimentacaoProdutoRepository.deleteAll(empresaID);
	}

	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		List<Long> encontrados = movimentacaoProdutoRepository.buscarPorIds(ids, empresaId).stream().map(MovimentacaoProduto::getId).toList();
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
			throw new MsgApiException("Movimentações de produto não encontradas ou já deletadas: " + naoEncontrados);
		}

		movimentacaoProdutoRepository.deletarAllById(ids, empresaId);
	}

	public List<MovimentacaoProduto> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return movimentacaoProdutoRepository.buscarPorIds(ids, empresaId);
	}

	public boolean existsById(Long id, Long empresaId) {
		return movimentacaoProdutoRepository.existsById(id, empresaId);
	}

	public List<MovimentacaoProduto> listar(Long empresaId) {
		return movimentacaoProdutoRepository.listar(empresaId);
	}

	public Optional<MovimentacaoProduto> buscarPorId(Long id, Long empresaId) {
		return movimentacaoProdutoRepository.buscarPorId(id, empresaId);
	}

	public long total(Long empresaId) {
		return movimentacaoProdutoRepository.total(empresaId);
	}

	public Page<MovimentacaoProduto> listarPaginado(Long empresaId, Pageable pageable) {
		return movimentacaoProdutoRepository.listarPaginado(empresaId, pageable);
	}

	// ====================Métodos específicos para Pedido====================

	public List<MovimentacaoProduto> findAllByPedido(Long idPedido, Long idEmpresa) {
		return movimentacaoProdutoRepository.findAllByPedido(idPedido, idEmpresa);
	}

	public List<MovimentacaoProduto> buscaPorNomeByPedido(String nome, Long idPedido, Long idEmpresa) {
		return movimentacaoProdutoRepository.buscaPorNomeByPedido(nome, idPedido, idEmpresa);
	}

	public boolean existePorNomeByPedido(String nome, Long idPedido, Long idEmpresa) {
		return movimentacaoProdutoRepository.existePorNomeByPedido(nome, idPedido, idEmpresa);
	}

	public boolean existePorNomeDiferenteIdByPedido(Long id, String nome, Long idPedido, Long idEmpresa) {
		return movimentacaoProdutoRepository.existePorNomeDiferenteIdByPedido(id, nome, idPedido, idEmpresa);
	}

	public void deleteByIdAndPedido(Long id, Long idPedido, Long idEmpresa) {
		if (!movimentacaoProdutoRepository.buscarPorId(id, idEmpresa).filter(movimentacao -> movimentacao.getPedido() != null && movimentacao.getPedido().getId().equals(idPedido)).isPresent()) {
			throw new MsgApiException("Movimentação de produto não encontrada ou já foi deletada.");
		}

		movimentacaoProdutoRepository.deleteByIdAndPedido(id, idPedido, idEmpresa);
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
