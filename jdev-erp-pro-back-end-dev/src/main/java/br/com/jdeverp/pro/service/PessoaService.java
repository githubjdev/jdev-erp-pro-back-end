package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.repository.PessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class PessoaService {

	@Autowired /* Injeção de dependência */
	private PessoaRepository pessoaRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	public Pessoa salvar(Pessoa pessoa) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (pessoaRepository.existePorNome(pessoa.getNome(), empresaId)) {
			throw new MsgApiException("Já existe uma pessoa com o mesmo nome para a empresa logada.");
		}

		pessoa.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return pessoaRepository.save(pessoa);
	}

	public Pessoa atualizar(Pessoa pessoa) {
		if (pessoa.getId() == null) {
			throw new MsgApiException("Id da pessoa não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (!pessoaRepository.existsById(pessoa.getId(), empresaId)) {
			throw new MsgApiException("Pessoa não encontrada para a empresa logada.");
		}

		if (pessoaRepository.existePorNomeDiferenteId(pessoa.getId(), pessoa.getNome(), empresaId)) {
			throw new MsgApiException("Já existe outra pessoa com o mesmo nome para a empresa logada.");
		}

		pessoa.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return pessoaRepository.save(pessoa);
	}

	public List<Pessoa> findAll(Long idEmpresa) {
		
		return pessoaRepository.findAll(idEmpresa);
	}

	public List<Pessoa> buscaPorNome(String nome, Long idEmpresa) {
		return pessoaRepository.buscaPorNome(nome, idEmpresa);
	}

	public boolean existePorNome(String nome, Long idEmpresa) {
		return pessoaRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return pessoaRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {

		if (!pessoaRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Pessoa não encontrada para a empresa logada, portanto não pode ser deletada.");
		}

		pessoaRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return pessoaRepository.deleteAll(empresaID);
	}

	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		pessoaRepository.deletarAllById(ids, empresaId);
	}

	public List<Pessoa> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return pessoaRepository.buscarPorIds(ids, empresaId);
	}

	public boolean existsById(Long id, Long empresaId) {
		return pessoaRepository.existsById(id, empresaId);
	}

	public List<Pessoa> listar(Long empresaId) {
		return pessoaRepository.listar(empresaId);
	}

	public Optional<Pessoa> buscarPorId(Long id, Long empresaId) {
		return pessoaRepository.buscarPorId(id, empresaId);
	}

	public long total(Long empresaId) {
		return pessoaRepository.total(empresaId);
	}

	public Page<Pessoa> listarPaginado(Long empresaId, Pageable pageable) {
		return pessoaRepository.listarPaginado(empresaId, pageable);
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
