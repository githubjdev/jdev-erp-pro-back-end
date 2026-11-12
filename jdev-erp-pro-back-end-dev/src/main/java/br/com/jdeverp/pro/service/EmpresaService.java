package br.com.jdeverp.pro.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.EmpresaDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Plano;
import br.com.jdeverp.pro.repository.EmpresaRepository;
import br.com.jdeverp.pro.repository.PlanoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
@Transactional(readOnly = true)
public class EmpresaService {

	@Autowired /* Injeção de dependência */
	private EmpresaRepository empresaRepository;

	/*
	 * Posso escrever query customizadas e dinâmicas, mais complexas do que no
	 * Repository
	 */
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private PlanoRepository planoRepository;

	@Autowired
	private PessoaService pessoaService;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	/*Empresa é dado global (cadastro dos clientes do SaaS): só ROLE_ADMIN cadastra, altera, apaga ou lista*/
	private void exigirAdmin(String operacao) {
		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem " + operacao + " empresas.", HttpStatus.FORBIDDEN);
		}
	}

	@Transactional
	public EmpresaDTO salvar(Empresa empresa) {

		/*Permissão antes de tudo: usuário comum não pode nem descobrir se a pessoa já tem empresa*/
		exigirAdmin("cadastrar");

		if (empresa.getPessoa() == null || empresa.getPessoa().getId() == null) {
			throw new MsgApiException("Pessoa deve ser informada para cadastrar a empresa.");
		}

		if (empresaRepository.existePorPessoa(empresa.getPessoa().getId())) {
			/*O nome só aparece se a pessoa for da empresa logada; de outra empresa, a mensagem fica sem nome*/
			String nome = pessoaService.buscarPorId(empresa.getPessoa().getId(), usuarioLogadoService.getEmpresaIdLogada())
					.map(Pessoa::getNome)
					.orElse("informada");
			throw new MsgApiException("Já existe uma empresa cadastrada para a pessoa: " + nome);
		}

		empresa.setPlano(validarPlano(empresa.getPlano()));
		/*Referência gerenciada no lugar do objeto do request (só com id), para o DTO ler o nome da pessoa*/
		empresa.setPessoa(entityManager.getReference(Pessoa.class, empresa.getPessoa().getId()));

		return converterParaDto(empresaRepository.saveAndFlush(empresa));
	}

	@Transactional
	public EmpresaDTO atualizar(Empresa empresa) {

		exigirAdmin("atualizar");

		if (empresa.getId() == null) {
			throw new MsgApiException("Id da empresa deve ser informado para edição.");
		}

		Empresa existente = empresaRepository.buscarPorId(empresa.getId());

		if (existente == null) {
			throw new MsgApiException("Empresa com id: " + empresa.getId() + " não foi encontrada.");
		}

		if (empresa.getPessoa() == null || empresa.getPessoa().getId() == null) {
			throw new MsgApiException("Pessoa deve ser informada para atualizar a empresa.");
		}

		existente.setPlano(validarPlano(empresa.getPlano()));
		existente.setPessoa(entityManager.getReference(Pessoa.class, empresa.getPessoa().getId()));
		existente.setTotalUsuario(empresa.getTotalUsuario());
		existente.setTotalCliente(empresa.getTotalCliente());
		existente.setPlanoAtivo(empresa.getPlanoAtivo());
		existente.setBloqueio(empresa.getBloqueio());
		existente.setLogoMarca(empresa.getLogoMarca());
		existente.setVigenciaPlano(empresa.getVigenciaPlano());

		return converterParaDto(empresaRepository.saveAndFlush(existente));

	}

	private Plano validarPlano(Plano plano) {
		if (plano == null || plano.getId() == null) {
			throw new MsgApiException("Plano deve ser informado para a empresa.");
		}

		return planoRepository.findById(plano.getId())
				.orElseThrow(() -> new MsgApiException("Plano com id: " + plano.getId() + " não foi encontrado."));
	}

	/** Administrador vê qualquer empresa; o usuário comum, só a dele. */
	public EmpresaDTO buscaPorId(Long id) {

		if (!usuarioLogadoService.isAdmin() && !usuarioLogadoService.getEmpresaIdLogada().equals(id)) {
			throw new MsgApiException("Apenas administradores podem consultar outras empresas.", HttpStatus.FORBIDDEN);
		}

		Empresa empresa = empresaRepository.buscarPorId(id);

		if (empresa == null) {
			throw new MsgApiException("Empresa com id: " + id + " não foi encontrada.");
		}

		return converterParaDto(empresa);
	}

	public List<EmpresaDTO> findAll() {
		exigirAdmin("consultar");
		return converterListaParaDto(empresaRepository.findAll());
	}

	public List<EmpresaDTO> buscaPorNome(String nome) {
		exigirAdmin("consultar");
		return converterListaParaDto(empresaRepository.buscaPorNome(nome));
	}

	public boolean existePorNome(String nome) {
		exigirAdmin("consultar");
		return empresaRepository.existePorNome(nome);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome) {
		exigirAdmin("consultar");
		return empresaRepository.existePorNomeDiferenteId(id, nome);
	}

	@Transactional
	public void deleteById(Long id) {
		exigirAdmin("deletar");

		if (empresaRepository.buscarPorId(id) == null) {
			throw new MsgApiException("Empresa não encontrada ou já foi deletada.");
		}

		empresaRepository.deleteById(id);

	}

	// ====================Conversão para DTO====================

	private EmpresaDTO converterParaDto(Empresa empresa) {
		EmpresaDTO dto = new EmpresaDTO();
		dto.setId(empresa.getId());
		dto.setPlanoId(empresa.getPlano().getId());
		dto.setPlanoNome(empresa.getPlano().getNome());
		dto.setTotalUsuario(empresa.getTotalUsuario());
		dto.setTotalCliente(empresa.getTotalCliente());
		dto.setPlanoAtivo(empresa.getPlanoAtivo());
		dto.setBloqueio(empresa.getBloqueio());
		dto.setLogoMarca(empresa.getLogoMarca());
		dto.setVigenciaPlano(empresa.getVigenciaPlano());
		dto.setPessoaId(empresa.getPessoa().getId());
		dto.setPessoaNome(empresa.getPessoa().getNome());
		return dto;
	}

	private List<EmpresaDTO> converterListaParaDto(List<Empresa> empresas) {
		return empresas.stream().map(this::converterParaDto).toList();
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
