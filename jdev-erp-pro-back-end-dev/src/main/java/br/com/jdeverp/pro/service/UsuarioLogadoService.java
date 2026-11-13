package br.com.jdeverp.pro.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.security.UsuarioAutenticado;

@Service
public class UsuarioLogadoService {

	public UsuarioAutenticado getUsuarioAutenticado() {
		return (UsuarioAutenticado) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
	}

	public Usuario getUsuarioLogado() {
		return getUsuarioAutenticado().getUsuario();
	}

	public Empresa getEmpresaLogada() {
		return getUsuarioLogado().getEmpresa();
	}

	public Long getEmpresaIdLogada() {
		return getEmpresaLogada().getId();
	}
	
	
	public Long getUsuarioLogadoId() {
		return getUsuarioLogado().getId();
	}
	
	public boolean isAdmin() {
		return getUsuarioLogado().idAdmin();
	}

	/**
	 * Se o usuário logado tem o acesso (role) informado. As roles vêm carregadas
	 * com o usuário no JwtFilter (Usuario.acessos é EAGER).
	 */
	public boolean possuiAcesso(Long roleId) {
		return roleId != null && getUsuarioLogado().getAcessos().stream().anyMatch(role -> roleId.equals(role.getId()));
	}

	/** Se o id é o do próprio usuário logado. */
	public boolean isUsuarioLogado(Long usuarioId) {
		return usuarioId != null && usuarioId.equals(getUsuarioLogadoId());
	}

}
