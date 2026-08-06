package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class ProdutoService {

	@Autowired /* Injeção de dependência */
	private ProdutoRepository produtoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	public List<Produto> findAll(Long idEmpresa) {
		
		return produtoRepository.findAll(idEmpresa);
	}

	public List<Produto> buscaPorNome(String nome, Long idEmpresa) {
		return produtoRepository.buscaPorNome(nome, idEmpresa);
	}

	public boolean existePorNome(String nome, Long idEmpresa) {
		return produtoRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return produtoRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {
		produtoRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return produtoRepository.deleteAll(empresaID);
	}

	void deletarAllById(Iterable<Long> ids, Long empresaId) {
		produtoRepository.deletarAllById(ids, empresaId);
	}

	public List<Produto> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return produtoRepository.buscarPorIds(ids, empresaId);
	}

	boolean existsById(Long id, Long empresaId) {
		return produtoRepository.existsById(id, empresaId);
	}

	public List<Produto> listar(Long empresaId) {
		return produtoRepository.listar(empresaId);
	}

	public Optional<Produto> buscarPorId(Long id, Long empresaId) {
		return produtoRepository.buscarPorId(id, empresaId);
	}

	public long total(Long empresaId) {
		return produtoRepository.total(empresaId);
	}

	public Page<Produto> listarPaginado(Long empresaId, Pageable pageable) {
		return produtoRepository.listarPaginado(empresaId, pageable);
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
