package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.repository.ClienteFuncionarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class ClienteFuncionarioService {

	@Autowired /* Injeção de dependência */
	private ClienteFuncionarioRepository clienteFuncionarioRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	public List<ClienteFuncionario> findAll(Long idEmpresa) {
		
		return clienteFuncionarioRepository.findAll(idEmpresa);
	}

	public List<ClienteFuncionario> buscaPorNome(String nome, Long idEmpresa) {
		return clienteFuncionarioRepository.buscaPorNome(nome, idEmpresa);
	}

	public boolean existePorNome(String nome, Long idEmpresa) {
		return clienteFuncionarioRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return clienteFuncionarioRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {
		clienteFuncionarioRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return clienteFuncionarioRepository.deleteAll(empresaID);
	}

	void deletarAllById(Iterable<Long> ids, Long empresaId) {
		clienteFuncionarioRepository.deletarAllById(ids, empresaId);
	}

	public List<ClienteFuncionario> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return clienteFuncionarioRepository.buscarPorIds(ids, empresaId);
	}

	boolean existsById(Long id, Long empresaId) {
		return clienteFuncionarioRepository.existsById(id, empresaId);
	}

	public List<ClienteFuncionario> listar(Long empresaId) {
		return clienteFuncionarioRepository.listar(empresaId);
	}

	public Optional<ClienteFuncionario> buscarPorId(Long id, Long empresaId) {
		return clienteFuncionarioRepository.buscarPorId(id, empresaId);
	}

	public long total(Long empresaId) {
		return clienteFuncionarioRepository.total(empresaId);
	}

	public Page<ClienteFuncionario> listarPaginado(Long empresaId, Pageable pageable) {
		return clienteFuncionarioRepository.listarPaginado(empresaId, pageable);
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
