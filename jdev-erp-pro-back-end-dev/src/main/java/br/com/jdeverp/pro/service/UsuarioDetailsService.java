package br.com.jdeverp.pro.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

	@Autowired
	private UsuarioRepository usuarioRepository;
	
	@Autowired
	private JwtService jwtService;
	
	@Override 
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		Usuario usuario = usuarioRepository.buscaPorLogin(username);

		if (usuario == null) {
			throw new UsernameNotFoundException("Usuário não encontrado no banco de dados.");
		}
		
		
		if(!usuario.isEnabled()) {
			throw new MsgApiException("Usuário bloqueado, entre em contato com o administrador do sistema.", HttpStatus.UNAUTHORIZED);
		}
		
		if(usuario.getEmpresa().getBloqueio()) {
			throw new MsgApiException("Empresa bloqueada, entre em contato com o administrador do sistema.", HttpStatus.UNAUTHORIZED);
		}

		return usuario; 
	}
	
	public void existsByTokenSessaoAndEmpresa(String token) {
		
		Long idEmpresa = jwtService.extrairEmpresaId(token);
		
		boolean existe = usuarioRepository.existsByTokenSessaoAndEmpresa(token, idEmpresa);
		
		if (!existe) {
			throw new MsgApiException("Seu token de acesso não é mais válido, realize o login novamente");	
		}
		
	}

}
