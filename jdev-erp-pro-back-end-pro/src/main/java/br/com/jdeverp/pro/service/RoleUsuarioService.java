package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.repository.RoleUsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class RoleUsuarioService {

	@Autowired /* Injeção de dependência */
	private RoleUsuarioRepository roleUsuarioRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private UsuarioLogadoService usuarioLogadoService;
	
	
	public RoleUsuario salvar(RoleUsuario roleUsuario ) {
		
		if(roleUsuario.getAcesso() == null){
			throw new MsgApiException("Acesso deve ser informado para associar ao usuário.");
		} 
		
		if(roleUsuario.getUsuario() == null){
			throw new MsgApiException("Usuário deve ser informado para associar acesso.");
		} 
		
		if (roleUsuarioRepository.existePorUsuarioERole(roleUsuario.getUsuario().getId(),
				roleUsuario.getAcesso().getId(), usuarioLogadoService.getEmpresaIdLogada())) {
			
			List<RoleUsuario> roleUsuarios = roleUsuarioRepository.findAllByUsuario(roleUsuario.getUsuario().getId(), usuarioLogadoService.getEmpresaIdLogada());
			
			throw new MsgApiException("Já existe o acesso de: " + roleUsuarios.get(0).getAcesso().getAcesso() + " associado para o mesmo usuário: "+ roleUsuarios.get(0).getUsuario().getClienteFuncionario().getPessoa().getNome());
			
		}
		
		
		return roleUsuarioRepository.saveAndFlush(roleUsuario);
	}
	
	
	public RoleUsuario atualizar(RoleUsuario roleUsuario ) {
		
		if(roleUsuario.getId() == null){
			throw new MsgApiException("Deve ser informado o registro para editart o acesso do usuário.");
		} 
		
		
		if(roleUsuario.getAcesso() == null){
			throw new MsgApiException("Acesso deve ser informado para associar ao usuário.");
		} 
		
		if(roleUsuario.getUsuario() == null){
			throw new MsgApiException("Usuário deve ser informado para associar acesso.");
		} 
		
		if (roleUsuarioRepository.existePorUsuarioDiferenreId( roleUsuario.getUsuario().getId(), 
															 roleUsuario.getAcesso().getId(), 
															 roleUsuario.getId(),
															 usuarioLogadoService.getEmpresaIdLogada())) {
			throw new MsgApiException("Associação de usuário com acesso é duplicada e não pode ser permitida.");
		}
		
		
		return roleUsuarioRepository.saveAndFlush(roleUsuario);
		
	}
	
	public List<RoleUsuario> listar() {
		return roleUsuarioRepository.findAllByEmpresa(usuarioLogadoService.getEmpresaIdLogada());
	}

	public Optional<RoleUsuario> buscarPorId(Long id) {
		return roleUsuarioRepository.buscarPorIdEEmpresa(id, usuarioLogadoService.getEmpresaIdLogada());
	}


	// ====================Métodos específicos para Usuário====================

	public List<RoleUsuario> findAllByUsuario(Long idUsuario, Long idEmpresa) {
		return roleUsuarioRepository.findAllByUsuario(idUsuario, idEmpresa);
	}

	// ====================Métodos específicos para Role====================

	public List<RoleUsuario> findAllByRoleAndEmpresa(Long idRole, Long idEmpresa) {
		return roleUsuarioRepository.findAllByRoleAndEmpresa(idRole, idEmpresa);
	}

	// ====================Métodos de validação====================

	public boolean existePorUsuarioERole(Long idUsuario, Long idRole, Long idEmpresa) {
		return roleUsuarioRepository.existePorUsuarioERole(idUsuario, idRole, idEmpresa);
	}

	// ====================Métodos de deleção====================

	public void deleteById(Long id) {
		
		Optional<RoleUsuario> roleUsuario = roleUsuarioRepository.buscarPorIdEEmpresa(id, usuarioLogadoService.getEmpresaIdLogada());
		
		if (!roleUsuario.isPresent()) {
			throw new MsgApiException("Registro de acesso não encontrado ou já foi deletado.");
		}
		
		roleUsuarioRepository.deleteById(id);
	}

	public void deleteByUsuarioAndRole(Long idUsuario, Long idRole, Long idEmpresa) {
		if (!roleUsuarioRepository.existePorUsuarioERole(idUsuario, idRole, idEmpresa)) {
			throw new MsgApiException("Acesso do usuário não encontrado ou já foi deletado.");
		}

		roleUsuarioRepository.deleteByUsuarioAndRole(idUsuario, idRole, idEmpresa);
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
