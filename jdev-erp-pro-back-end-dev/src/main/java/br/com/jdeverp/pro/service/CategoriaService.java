package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.repository.CategoriaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class CategoriaService {

	@Autowired /* Injeção de dependência */
	private CategoriaRepository categoriaRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	public List<Categoria> findAll(Long idEmpresa) {
		
		return categoriaRepository.findAll(idEmpresa);
	}

	public List<Categoria> buscaPorNome(String nome, Long idEmpresa) {
		return categoriaRepository.buscaPorNome(nome, idEmpresa);
	}

	public boolean existePorNome(String nome, Long idEmpresa) {
		return categoriaRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return categoriaRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {
		categoriaRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return categoriaRepository.deleteAll(empresaID);
	}

	void deletarAllById(Iterable<Long> ids, Long empresaId) {
		categoriaRepository.deletarAllById(ids, empresaId);
	}

	public List<Categoria> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return categoriaRepository.buscarPorIds(ids, empresaId);
	}

	boolean existsById(Long id, Long empresaId) {
		return categoriaRepository.existsById(id, empresaId);
	}

	public List<Categoria> listar(Long empresaId) {
		return categoriaRepository.listar(empresaId);
	}

	public Optional<Categoria> buscarPorId(Long id, Long empresaId) {
		return categoriaRepository.buscarPorId(id, empresaId);
	}

	public long total(Long empresaId) {
		return categoriaRepository.total(empresaId);
	}

	public Page<Categoria> listarPaginado(Long empresaId, Pageable pageable) {
		return categoriaRepository.listarPaginado(empresaId, pageable);
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
