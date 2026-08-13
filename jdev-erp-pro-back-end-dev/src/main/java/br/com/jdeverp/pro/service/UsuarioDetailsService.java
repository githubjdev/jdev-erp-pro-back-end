package br.com.jdeverp.pro.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService  {
	
	@Autowired
	private UsuarioRepository usuarioRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		Usuario usuario = usuarioRepository.buscaPorLogin(username);
		
		if(usuario != null) {
			throw new UsernameNotFoundException("Usuário não encontrado no banco de dados.");
		}
		
		return usuario;
	}

}
