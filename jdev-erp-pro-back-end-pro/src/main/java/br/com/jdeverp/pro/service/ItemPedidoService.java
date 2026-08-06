package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.ItemPedido;
import br.com.jdeverp.pro.repository.ItemPedidoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class ItemPedidoService {

	@Autowired /* Injeção de dependência */
	private ItemPedidoRepository itemPedidoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

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
		itemPedidoRepository.deleteById(id, idPedido, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return itemPedidoRepository.deleteAll(empresaID);
	}

	void deletarAllById(Iterable<Long> ids, Long empresaId) {
		itemPedidoRepository.deletarAllById(ids, empresaId);
	}

	public List<ItemPedido> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return itemPedidoRepository.buscarPorIds(ids, empresaId);
	}

	boolean existsById(Long id, Long empresaId) {
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
