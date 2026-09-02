package br.com.jdeverp.pro.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.dto.AlterarSenhaDTO;
import br.com.jdeverp.pro.dto.LoginDTO;
import br.com.jdeverp.pro.dto.TokenDTO;
import br.com.jdeverp.pro.dto.UsuarioDto;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class UsuarioService {

	@Autowired /* Injeção de dependência */
	private UsuarioRepository usuarioRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	
	@Autowired
	private AuthenticationManager authenticationManager;
	
	@Autowired
	private JwtService jwtService;
	
	@Autowired
	private UsuarioLogadoService usuarioLogadoService;
	
	@Autowired
	private ClienteFuncionarioService clienteFuncionarioService;
	
	@Autowired
	private RoleService roleService;
	
	/**
	 * Retorna o token de acesso para o usuário que fez o login
	 * @param dto
	 * @return TokenDTO
	 */
	public TokenDTO login(LoginDTO dto) {
		
		Usuario usuario = buscaPorLogin(dto.getLogin());
		
		if(usuario == null) {
			throw new MsgApiException("Usuário não encontrado.", HttpStatus.UNAUTHORIZED);
		}
		
		if(!usuario.isEnabled()) {
			throw new MsgApiException("Usuário bloqueado, entre em contato com o administrador do sistema.", HttpStatus.UNAUTHORIZED);
		}
		
		if(usuario.getEmpresa().getBloqueio()) {
			throw new MsgApiException("Empresa bloqueada, entre em contato com o administrador do sistema.", HttpStatus.UNAUTHORIZED);
		}
		
		boolean senhaValida = passwordEncoder.matches(dto.getSenha(), usuario.getSenha());
		
		if(!senhaValida) {
			throw new MsgApiException("Senha digitada é inválida.", HttpStatus.UNAUTHORIZED);
		}
		
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(dto.getLogin(), dto.getSenha()));
		
		String token = jwtService.gerarToken(usuario);
		
		usuarioRepository.updateTokenSessaoLogin(usuario.getId(), token, usuario.getEmpresa().getId());
		
		return new TokenDTO(token);
		
	}
	
	
	public Usuario salvar(Usuario usuario) {
		if(usuarioRepository.existePorLogin(usuario.getLogin(), usuarioLogadoService.getEmpresaIdLogada())) { 
			throw new MsgApiException("O login escolhido já existe, escolha outro login para cadastrar um novo usuário.");
		}
		
		
		if(usuario.getSenha().length() < 5) {
			throw new MsgApiException("A senha deve ter mais de 5 caracteres.");
		}
		
		if (usuarioRepository.existePorPessoa(usuario.getClienteFuncionario().getPessoa().getId(), usuarioLogadoService.getEmpresaIdLogada())) {
			 throw new MsgApiException("Já existe um usuário vinculado a esta pessoa.");
		}
		
		
		if (usuario.getClienteFuncionario() == null) {
			throw new MsgApiException("Não foi informado o registro de pessoa/ cliente ou funcioário para o usuário.");
		}
		
		ClienteFuncionario clienteFuncionario =  clienteFuncionarioService.findByPessoa(usuario.getClienteFuncionario().getPessoa().getId(), usuarioLogadoService.getEmpresaIdLogada());
		
		List<Role>  roles = roleService.buscaPorAcesso("ROLE_USER");
		usuario.setAcessos(roles);
		usuario.setClienteFuncionario(clienteFuncionario);
		usuario.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		usuario = usuarioRepository.saveAndFlush(usuario);
		
		clienteFuncionario.setUsuario(usuario);
		clienteFuncionarioService.salvar(clienteFuncionario);
		
		return usuario;
	}
	
	public Usuario atualizar(Usuario usuario) {
		
		
		if (usuarioRepository.existeOutroUsuarioComPessoa(usuario.getClienteFuncionario().getPessoa().getId(), usuario.getId(), usuarioLogadoService.getEmpresaIdLogada())) {
			throw new MsgApiException("Existe outro usuário associado a pessoa que foi selecionada.");
		}
		
		Usuario usuarioBanco = buscarUserPorId(usuario.getId(), usuarioLogadoService.getEmpresaIdLogada());
		
		if (usuario.getAcessos() == null || usuario.getAcessos().isEmpty()) {
			usuario.setAcessos(usuarioBanco.getAcessos());
		}
		
		
		ClienteFuncionario clienteFuncionario =  clienteFuncionarioService.findByPessoa(usuario.getClienteFuncionario().getPessoa().getId(), usuarioLogadoService.getEmpresaIdLogada());
		
		usuario.setSenha(usuarioBanco.getSenha());
		usuario.setClienteFuncionario(clienteFuncionario);
		usuario.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		
		return usuarioRepository.save(usuario); 
		
	}
	
	
	public void alterarSenha(AlterarSenhaDTO dto) {
		
		Usuario usuario = usuarioRepository.buscarPorId(dto.getId(), usuarioLogadoService.getEmpresaIdLogada()).get();
		
	  if (usuario == null) {
		  throw new MsgApiException("Usuário não encontrado.");
	  }	
	  
	  if (!dto.getNovaSenha().equals(dto.getConfirmarSenha())) {
		  throw new MsgApiException("A confirmação da senha não confere.");
	  }
	  
	  /*COnferencia se a nova senha igual a do banco e emite msg*/
	  if (passwordEncoder.matches(dto.getNovaSenha(), usuario.getSenha())) {
		  throw new MsgApiException("A nova senha deve ser diferente da atual");
	  }
	  
	  /*Conferencia se senha atual é mesma do banco e autoriza a troca de senha*/
	  if (!passwordEncoder.matches(dto.getSenhaAtual(), usuario.getSenha())) {
		  throw new MsgApiException("Senha atual inválida");
	  }
	  
	  usuario.setSenha(passwordEncoder.encode(dto.getNovaSenha()));
	  
	  usuarioRepository.saveAndFlush(usuario);
		
	}
	

	public List<Usuario> findAll(Long idEmpresa) {
		
		return usuarioRepository.findAll(idEmpresa);
	}

	public Usuario buscaPorLogin(String login) {
		return usuarioRepository.buscaPorLogin(login);
	}

	public List<Usuario> buscaPorNome(String nome, Long idEmpresa) {
		return usuarioRepository.buscaPorNome(nome, idEmpresa);
	}

	public boolean existePorNome(String nome, Long idEmpresa) {
		return usuarioRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return usuarioRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {
		
		 Optional<Usuario> optional = usuarioRepository.buscarPorId(id, idEmpresa);
		
		 if (!optional.isPresent()) {
			 throw new MsgApiException("Usuário não encontrado para deletar.");
		 }
		
		clienteFuncionarioService.removeUserClienteFuncionarioId(id, idEmpresa);
		
		usuarioRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		return usuarioRepository.deleteAll(empresaID);
	}

	void deletarAllById(Iterable<Long> ids, Long empresaId) {
		usuarioRepository.deletarAllById(ids, empresaId);
	}

	public List<Usuario> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return usuarioRepository.buscarPorIds(ids, empresaId);
	}

	boolean existsById(Long id, Long empresaId) {
		return usuarioRepository.existsById(id, empresaId);
	}

	public List<UsuarioDto> listar(Long empresaId) {
		
		List<UsuarioDto> dtos = new ArrayList<UsuarioDto>();
		List<Usuario> list = usuarioRepository.listar(empresaId);
		
		for (Usuario usuario : list) {
			
			UsuarioDto dto = new UsuarioDto();
			dto.setBloqueado(usuario.getLiberado());
			dto.setEmpresa(usuario.getEmpresa().getPessoa().getNomeFantasia());
			dto.setPessoa(usuario.getClienteFuncionario().getPessoa().getNome());
			dtos.add(dto);
		}
		
		return dtos;
	}

	public UsuarioDto buscarPorId(Long id, Long empresaId) {
		
		
		Optional<Usuario> usuario = usuarioRepository.buscarPorId(id, empresaId);
		
		if (!usuario.isPresent()) {
			throw new MsgApiException("Usuário não foi encontrado na busca.");
		}
		
		UsuarioDto dto = new UsuarioDto();
		dto.setBloqueado(usuario.get().getLiberado());
		dto.setEmpresa(usuario.get().getEmpresa().getPessoa().getNomeFantasia());
		dto.setPessoa(usuario.get().getClienteFuncionario().getPessoa().getNome());
		
		return dto;
	}
	
	
	private Usuario buscarUserPorId(Long id, Long empresaId) {
		return usuarioRepository.buscarPorId(id, empresaId).get();
	}

	public long total(Long empresaId) {
		return usuarioRepository.total(empresaId);
	}

	public Page<Usuario> listarPaginado(Long empresaId, Pageable pageable) {
		return usuarioRepository.listarPaginado(empresaId, pageable);
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
