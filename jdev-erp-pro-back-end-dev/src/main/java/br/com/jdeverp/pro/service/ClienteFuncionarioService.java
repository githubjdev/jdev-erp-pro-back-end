package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ClienteFuncionarioRepository;
import br.com.jdeverp.pro.repository.PessoaRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class ClienteFuncionarioService {

	@Autowired /* Injeção de dependência */
	private ClienteFuncionarioRepository clienteFuncionarioRepository;

	@Autowired
	private PessoaRepository pessoaRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;
	
	
	public ClienteFuncionario salvar(ClienteFuncionario clienteFuncionario ) {
		return clienteFuncionarioRepository.saveAndFlush(clienteFuncionario);
	} 

	public ClienteFuncionario atualizar(ClienteFuncionario clienteFuncionario) {
		if (clienteFuncionario.getId() == null) {
			throw new MsgApiException("Id do cliente/funcionário não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (!clienteFuncionarioRepository.buscarPorId(clienteFuncionario.getId(), empresaId).isPresent()) {
			throw new MsgApiException("Cliente/funcionário não encontrado para a empresa logada.");
		}

		if (clienteFuncionario.getPessoa() == null || clienteFuncionario.getPessoa().getId() == null) {
			throw new MsgApiException("Pessoa deve ser informada para atualizar o cadastro.");
		}

		Pessoa pessoa = pessoaRepository.buscarPorId(clienteFuncionario.getPessoa().getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Pessoa não encontrada para a empresa logada."));

		if (clienteFuncionario.getUsuario() == null || clienteFuncionario.getUsuario().getId() == null) {
			throw new MsgApiException("Usuário deve ser informado para atualizar o cadastro.");
		}

		Usuario usuario = usuarioRepository.buscarPorId(clienteFuncionario.getUsuario().getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Usuário não encontrado para a empresa logada."));

		clienteFuncionario.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		clienteFuncionario.setPessoa(pessoa);
		clienteFuncionario.setUsuario(usuario);
		return clienteFuncionarioRepository.saveAndFlush(clienteFuncionario);
	}

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

		if (!clienteFuncionarioRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Cliente/funcionário não encontrado para a empresa logada, portanto não pode ser deletado.");
		}

		clienteFuncionarioRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return clienteFuncionarioRepository.deleteAll(empresaID);
	}

	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		clienteFuncionarioRepository.deletarAllById(ids, empresaId);
	}

	public List<ClienteFuncionario> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return clienteFuncionarioRepository.buscarPorIds(ids, empresaId);
	}

	public boolean existsById(Long id, Long empresaId) {
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
	
	
	public ClienteFuncionario findByPessoa(Long idPessoa, Long idEmpresa) {
		return clienteFuncionarioRepository.findByPessoa(idPessoa, idEmpresa);
	}
	
	
	public void removeUserClienteFuncionarioId(Long id, Long idEmpresa) {
		clienteFuncionarioRepository.removeUserClienteFuncionarioId(id, idEmpresa);
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
