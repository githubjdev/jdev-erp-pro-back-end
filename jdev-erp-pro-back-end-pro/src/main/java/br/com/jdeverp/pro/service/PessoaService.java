package br.com.jdeverp.pro.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.PessoaDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.repository.PessoaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
@Transactional(readOnly = true)
public class PessoaService {

	@Autowired /* Injeção de dependência */
	private PessoaRepository pessoaRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	@Transactional
	public PessoaDTO salvar(Pessoa pessoa) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (pessoaRepository.existePorNome(pessoa.getNome(), empresaId)) {
			throw new MsgApiException("Já existe uma pessoa com o mesmo nome para a empresa logada.");
		}

		pessoa.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		/*A data de cadastro é sempre a de hoje, nunca a do request (P20)*/
		pessoa.setDataCadastro(LocalDate.now());
		return converterParaDto(pessoaRepository.saveAndFlush(pessoa));
	}

	@Transactional
	public PessoaDTO atualizar(Pessoa pessoa) {
		if (pessoa.getId() == null) {
			throw new MsgApiException("Id da pessoa não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		Pessoa existente = pessoaRepository.buscarPorId(pessoa.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Pessoa não encontrada para a empresa logada."));

		if (pessoaRepository.existePorNomeDiferenteId(pessoa.getId(), pessoa.getNome(), empresaId)) {
			throw new MsgApiException("Já existe outra pessoa com o mesmo nome para a empresa logada.");
		}

		pessoa.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		/*A data de cadastro não muda na edição (P20)*/
		pessoa.setDataCadastro(existente.getDataCadastro());
		return converterParaDto(pessoaRepository.saveAndFlush(pessoa));
	}

	public List<PessoaDTO> findAll(Long idEmpresa) {

		return converterListaParaDto(pessoaRepository.findAll(idEmpresa));
	}

	public List<PessoaDTO> buscaPorNome(String nome, Long idEmpresa) {
		return converterListaParaDto(pessoaRepository.buscaPorNome(nome, idEmpresa));
	}

	public boolean existePorNome(String nome, Long idEmpresa) {
		return pessoaRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return pessoaRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	@Transactional
	public void deleteById(Long id, Long idEmpresa) {
		if (!pessoaRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Pessoa não encontrada ou já foi deletada.");
		}

		pessoaRepository.deleteById(id, idEmpresa);
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

		if (pessoaRepository.total(empresaID) == 0) {
			throw new MsgApiException("Nenhuma pessoa encontrada para deletar ou todas já foram deletadas.");
		}

		return pessoaRepository.deleteAll(empresaID);
	}

	@Transactional
	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		exigirAdminParaExclusaoEmLote();

		List<Long> encontrados = pessoaRepository.buscarPorIds(ids, empresaId).stream().map(Pessoa::getId).toList();
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
			throw new MsgApiException("Pessoas não encontradas ou já deletadas: " + naoEncontrados);
		}

		pessoaRepository.deletarAllById(ids, empresaId);
	}

	public List<PessoaDTO> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return converterListaParaDto(pessoaRepository.buscarPorIds(ids, empresaId));
	}

	public boolean existsById(Long id, Long empresaId) {
		return pessoaRepository.existsById(id, empresaId);
	}

	public List<PessoaDTO> listar(Long empresaId) {
		return converterListaParaDto(pessoaRepository.listar(empresaId));
	}

	/**
	 * Devolve a entidade: é usado por outros services (ex.: EmpresaService).
	 * Para o controller, use buscarPorIdDto.
	 */
	public Optional<Pessoa> buscarPorId(Long id, Long empresaId) {
		return pessoaRepository.buscarPorId(id, empresaId);
	}

	public Optional<PessoaDTO> buscarPorIdDto(Long id, Long empresaId) {
		return pessoaRepository.buscarPorId(id, empresaId).map(this::converterParaDto);
	}

	public long total(Long empresaId) {
		return pessoaRepository.total(empresaId);
	}

	public Page<PessoaDTO> listarPaginado(Long empresaId, Pageable pageable) {
		return pessoaRepository.listarPaginado(empresaId, pageable).map(this::converterParaDto);
	}

	// ====================Conversão para DTO====================

	private PessoaDTO converterParaDto(Pessoa pessoa) {
		PessoaDTO dto = new PessoaDTO();
		dto.setId(pessoa.getId());
		dto.setNome(pessoa.getNome());
		dto.setRazaoSocial(pessoa.getRazaoSocial());
		dto.setNomeFantasia(pessoa.getNomeFantasia());
		dto.setInscricaoEstadual(pessoa.getInscricaoEstadual());
		dto.setCnpj(pessoa.getCnpj());
		dto.setTelefone(pessoa.getTelefone());
		dto.setCpf(pessoa.getCpf());
		dto.setEmail(pessoa.getEmail());
		dto.setTipoPessoa(pessoa.getTipoPessoa());
		dto.setAtivo(pessoa.getAtivo());
		dto.setDataCadastro(pessoa.getDataCadastro());
		dto.setObservacao(pessoa.getObservacao());
		dto.setCep(pessoa.getCep());
		dto.setLogradouro(pessoa.getLogradouro());
		dto.setBairro(pessoa.getBairro());
		dto.setEstado(pessoa.getEstado());
		dto.setCidade(pessoa.getCidade());
		dto.setPais(pessoa.getPais());
		dto.setComplemento(pessoa.getComplemento());
		dto.setEmpresaId(pessoa.getEmpresa().getId());
		return dto;
	}

	private List<PessoaDTO> converterListaParaDto(List<Pessoa> pessoas) {
		return pessoas.stream().map(this::converterParaDto).toList();
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
