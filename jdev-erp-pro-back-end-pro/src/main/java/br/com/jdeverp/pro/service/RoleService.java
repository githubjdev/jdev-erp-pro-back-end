package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.RoleDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.repository.RoleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
@Transactional(readOnly = true)
public class RoleService {

	@Autowired /* Injeção de dependência */
	private RoleRepository roleRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;


	@Autowired
	private UsuarioLogadoService usuarioLogadoService;


	/*
	 * Acesso é dado global e decide quem é administrador: só ROLE_ADMIN cadastra,
	 * altera ou apaga (renomear ROLE_USER para ROLE_ADMIN tornaria todos admin).
	 */
	private void exigirAdmin(String operacao) {
		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem " + operacao + " acessos.", HttpStatus.FORBIDDEN);
		}
	}

	@Transactional
	public RoleDTO salvar(Role role) {
		exigirAdmin("cadastrar");

		if (!role.getAcesso().startsWith("ROLE_")) {
			throw new MsgApiException("Nomenclatura de acesso não contém ROLE_ no incio");
		}

		if (roleRepository.existePorAcesso(role.getAcesso())) {
			throw new MsgApiException("Já existe acesso com a mesma descrição: " + role.getAcesso());
		}

		return converterParaDto(roleRepository.saveAndFlush(role));
	}


	@Transactional
	public RoleDTO atualizar(Role role) {

		exigirAdmin("atualizar");

		if (role.getId() == null) {
			throw new MsgApiException("Id do acesso deve ser informado para edição.");
		}


		if (!role.getAcesso().startsWith("ROLE_")) {
			throw new MsgApiException("Nomenclatura de acesso não contém ROLE_ no incio");
		}

		if (roleRepository.existePorAcessoDiferenteId(role.getId(), role.getAcesso())) {
			throw new MsgApiException("Já exite outro acesso com a mesma descrição: " + role.getAcesso());
		}


		return converterParaDto(roleRepository.saveAndFlush(role));

	}

	public List<RoleDTO> listar() {
		return converterListaParaDto(roleRepository.listar(usuarioLogadoService.getEmpresaIdLogada()));
	}

	public Optional<RoleDTO> buscarPorId(Long id) {
		return roleRepository.findById(id).map(this::converterParaDto);
	}

	/**
	 * Devolve a entidade: é usado por outros services (ex.: UsuarioService, para
	 * associar a ROLE_USER ao novo usuário). Para o controller, use buscaPorAcessoDto.
	 */
	public List<Role> buscaPorAcesso(String acesso) {
		return roleRepository.buscaPorAcesso(acesso);
	}

	public List<RoleDTO> buscaPorAcessoDto(String acesso) {
		return converterListaParaDto(roleRepository.buscaPorAcesso(acesso));
	}

	public boolean existePorAcesso(String acesso) {
		return roleRepository.existePorAcesso(acesso);
	}

	public boolean existePorAcessoDiferenteId(Long id, String acesso) {
		return roleRepository.existePorAcessoDiferenteId(id, acesso);
	}

	@Transactional
	public void deleteById(Long id) {

		exigirAdmin("deletar");

		if(!roleRepository.existePorId(id)) {
			throw new MsgApiException("Acesso com id: " + id + " já foi removido do sistema.");
		}

		roleRepository.deleteById(id);
	}

	// ====================Conversão para DTO====================

	private RoleDTO converterParaDto(Role role) {
		RoleDTO dto = new RoleDTO();
		dto.setId(role.getId());
		dto.setAcesso(role.getAcesso());
		return dto;
	}

	private List<RoleDTO> converterListaParaDto(List<Role> roles) {
		return roles.stream().map(this::converterParaDto).toList();
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
