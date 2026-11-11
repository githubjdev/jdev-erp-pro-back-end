package br.com.jdeverp.pro.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

	public EmpresaDTO salvar(Empresa empresa) {
		
		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem cadastrar empresas.", HttpStatus.FORBIDDEN);
		}
		
		if (empresaRepository.existePorPessoa(empresa.getPessoa().getId())) {
			Pessoa pessoa = pessoaService.buscarPorId(empresa.getPessoa().getId(), usuarioLogadoService.getEmpresaIdLogada()).get();
			throw new MsgApiException("Já existe uma empresa cadastrada para a pessoa: " + pessoa.getNome());
		}


		if (empresa.getPessoa() == null || empresa.getPessoa().getId() == null) {
			throw new MsgApiException("Pessoa deve ser informada para cadastrar a empresa.");
		}

		empresa.setPlano(validarPlano(empresa.getPlano()));
		
		empresa =  empresaRepository.saveAndFlush(empresa);
		
		EmpresaDTO dto = new EmpresaDTO();
		dto.setId(empresa.getId());
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

	public EmpresaDTO atualizar(Empresa empresa) {

		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem atualizar empresas.", HttpStatus.FORBIDDEN);
		}

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
		existente.setPessoa(empresa.getPessoa());
		existente.setTotalUsuario(empresa.getTotalUsuario());
		existente.setTotalCliente(empresa.getTotalCliente());
		existente.setPlanoAtivo(empresa.getPlanoAtivo());
		existente.setBloqueio(empresa.getBloqueio());
		existente.setLogoMarca(empresa.getLogoMarca());
		existente.setVigenciaPlano(empresa.getVigenciaPlano());
		
		
		existente =  empresaRepository.saveAndFlush(existente);
		
		EmpresaDTO dto = new EmpresaDTO();
		dto.setId(existente.getId());
		dto.setTotalUsuario(existente.getTotalUsuario());
		dto.setTotalCliente(existente.getTotalCliente());
		dto.setPlanoAtivo(existente.getPlanoAtivo());
		dto.setBloqueio(existente.getBloqueio());
		dto.setLogoMarca(existente.getLogoMarca());
		dto.setVigenciaPlano(existente.getVigenciaPlano());
		dto.setPessoaId(existente.getPlano().getId());
		dto.setPessoaNome(existente.getPessoa().getNome());

		return dto;

	}

	private Plano validarPlano(Plano plano) {
		if (plano == null || plano.getId() == null) {
			throw new MsgApiException("Plano deve ser informado para a empresa.");
		}

		return planoRepository.findById(plano.getId())
				.orElseThrow(() -> new MsgApiException("Plano com id: " + plano.getId() + " não foi encontrado."));
	}

	public EmpresaDTO buscaPorId(Long id) {
		
		Empresa empresa = empresaRepository.buscarPorId(id);
		
		EmpresaDTO dto = new EmpresaDTO();
		dto.setId(empresa.getId());
		dto.setTotalUsuario(empresa.getTotalUsuario());
		dto.setTotalCliente(empresa.getTotalCliente());
		dto.setPlanoAtivo(empresa.getPlanoAtivo());
		dto.setBloqueio(empresa.getBloqueio());
		dto.setLogoMarca(empresa.getLogoMarca());
		dto.setVigenciaPlano(empresa.getVigenciaPlano());
		dto.setPessoaId(empresa.getPlano().getId());
		dto.setPessoaNome(empresa.getPessoa().getNome());
		
		return dto;
	}

	public List<EmpresaDTO> findAll() {

		List<Empresa> empresas = empresaRepository.findAll();
		List<EmpresaDTO> dtos = new ArrayList<EmpresaDTO>();

		for (Empresa empresa : empresas) {
			EmpresaDTO dto = new EmpresaDTO();
			dto.setId(empresa.getId());
			dto.setTotalUsuario(empresa.getTotalUsuario());
			dto.setTotalCliente(empresa.getTotalCliente());
			dto.setPlanoAtivo(empresa.getPlanoAtivo());
			dto.setBloqueio(empresa.getBloqueio());
			dto.setLogoMarca(empresa.getLogoMarca());
			dto.setVigenciaPlano(empresa.getVigenciaPlano());
			dto.setPessoaId(empresa.getPlano().getId());
			dto.setPessoaNome(empresa.getPessoa().getNome());
			dtos.add(dto);
		}

		return dtos;
	}

	public List<EmpresaDTO> buscaPorNome(String nome) {
		
		List<Empresa> empresas = empresaRepository.buscaPorNome(nome);
		List<EmpresaDTO> dtos = new ArrayList<EmpresaDTO>();

		for (Empresa empresa : empresas) {
			EmpresaDTO dto = new EmpresaDTO();
			dto.setId(empresa.getId());
			dto.setTotalUsuario(empresa.getTotalUsuario());
			dto.setTotalCliente(empresa.getTotalCliente());
			dto.setPlanoAtivo(empresa.getPlanoAtivo());
			dto.setBloqueio(empresa.getBloqueio());
			dto.setLogoMarca(empresa.getLogoMarca());
			dto.setVigenciaPlano(empresa.getVigenciaPlano());
			dto.setPessoaId(empresa.getPlano().getId());
			dto.setPessoaNome(empresa.getPessoa().getNome());
			dtos.add(dto);
		}

		return dtos;
		
	}

	public boolean existePorNome(String nome) {
		return empresaRepository.existePorNome(nome);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome) {
		return empresaRepository.existePorNomeDiferenteId(id, nome);
	}

	public void deleteById(Long id) {
		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem deletar empresas.", HttpStatus.FORBIDDEN);
		}

		if (empresaRepository.buscarPorId(id) == null) {
			throw new MsgApiException("Empresa não encontrada ou já foi deletada.");
		}

		empresaRepository.deleteById(id);

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
