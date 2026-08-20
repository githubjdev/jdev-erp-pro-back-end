package br.jdev.estoque.erp.multitanent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import br.com.jdeverp.pro.service.UsuarioLogadoService;

@Component
public class EmpresaContext {
	
	@Autowired
	private UsuarioLogadoService usuarioLogadoService;
	
	public Long getEmpresaId() {
		return usuarioLogadoService.getEmpresaIdLogada();
	}

}
