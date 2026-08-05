package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class ChamadoService {

	@Autowired /* Injeção de dependência */
	private ChamadoRepository chamadoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	public List<Chamado> findAll(Long idEmpresa) {
		
		return chamadoRepository.findAll(idEmpresa);
	}

	public List<Chamado> buscaPorTitulo(String titulo, Long idEmpresa) {
		return chamadoRepository.buscaPorTitulo(titulo, idEmpresa);
	}

	public boolean existePorTitulo(String titulo, Long idEmpresa) {
		return chamadoRepository.existePorTitulo(titulo, idEmpresa);
	}

	public boolean existePorTituloDiferenteId(Long id, String titulo, Long idEmpresa) {
		return chamadoRepository.existePorTituloDiferenteId(id, titulo, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {
		chamadoRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return chamadoRepository.deleteAll(empresaID);
	}

	void deletarAllById(Iterable<Long> ids, Long empresaId) {
		chamadoRepository.deletarAllById(ids, empresaId);
	}

	public List<Chamado> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return chamadoRepository.buscarPorIds(ids, empresaId);
	}

	boolean existsById(Long id, Long empresaId) {
		return chamadoRepository.existsById(id, empresaId);
	}

	public List<Chamado> listar(Long empresaId) {
		return chamadoRepository.listar(empresaId);
	}

	public Optional<Chamado> buscarPorId(Long id, Long empresaId) {
		return chamadoRepository.buscarPorId(id, empresaId);
	}

	public long total(Long empresaId) {
		return chamadoRepository.total(empresaId);
	}

	public Page<Chamado> listarPaginado(Long empresaId, Pageable pageable) {
		return chamadoRepository.listarPaginado(empresaId, pageable);
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
