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
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.AlterarSenhaDTO;
import br.com.jdeverp.pro.dto.LoginDTO;
import br.com.jdeverp.pro.dto.TokenDTO;
import br.com.jdeverp.pro.dto.UsuarioDto;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.ClienteFuncionario;
import br.com.jdeverp.pro.model.Role;
import br.com.jdeverp.pro.model.RoleUsuario;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
@Transactional(readOnly = true)
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

	@Autowired
	private RoleUsuarioService roleUsuarioService;

	/**
	 * Retorna o token de acesso para o usuário que fez o login
	 * @param dto
	 * @return TokenDTO
	 */
	@Transactional
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


	/* P25 (2026-11-12): usuário com ROLE_ADMIN nunca é apagado pela API, e o admin logado não remove o próprio acesso */
	static final String MSG_APAGAR_A_SI_MESMO = "Você não pode apagar o seu próprio usuário: perderia o acesso ao sistema. "
			+ "Se precisar, peça a outro administrador que remova o seu acesso ROLE_ADMIN e depois apague o usuário.";

	static final String MSG_ADMIN_NAO_APAGA = "Usuário administrador não pode ser apagado. Para apagá-lo, remova antes o acesso "
			+ "ROLE_ADMIN dele em Acessos do usuário e tente de novo.";

	static final String MSG_BLOQUEAR_A_SI_MESMO = "Você não pode bloquear o seu próprio usuário: perderia o acesso ao sistema. "
			+ "Se precisar, peça a outro administrador que remova o seu acesso ROLE_ADMIN e depois faça o bloqueio.";

	static final String MSG_ADMIN_NAO_BLOQUEIA = "Usuário administrador não pode ser bloqueado por outro administrador. "
			+ "Para bloqueá-lo, remova antes o acesso ROLE_ADMIN dele em Acessos do usuário e tente de novo.";

	static final String MSG_NADA_A_APAGAR = "Nenhum usuário para deletar: os administradores não são apagados e os demais "
			+ "já foram deletados.";

	/*Cadastro, edição e exclusão de usuários são área de administração*/
	private void exigirAdmin(String operacao) {
		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem " + operacao + ".", HttpStatus.FORBIDDEN);
		}
	}


	@Transactional
	public UsuarioDto salvar(UsuarioDto usuarioDto) {

		exigirAdmin("cadastrar novos usuários");


		if(usuarioRepository.existePorLogin(usuarioDto.getLogin(), usuarioLogadoService.getEmpresaIdLogada())) {
			throw new MsgApiException("O login escolhido já existe, escolha outro login para cadastrar um novo usuário.");
		}


		if(usuarioDto.getSenha() == null || usuarioDto.getSenha().length() < 5) {
			throw new MsgApiException("A senha deve ter no mínimo 5 caracteres.");
		}

		if (usuarioRepository.existePorPessoa(usuarioDto.getPessoaId(), usuarioLogadoService.getEmpresaIdLogada())) {
			 throw new MsgApiException("Já existe um usuário vinculado a esta pessoa.");
		}

		ClienteFuncionario clienteFuncionario =  clienteFuncionarioService.findByPessoa(usuarioDto.getPessoaId(), usuarioLogadoService.getEmpresaIdLogada());

		if (clienteFuncionario == null) {
			throw new MsgApiException("Não foi informado o registro de pessoa/ cliente ou funcioário para o usuário.");
		}


		List<Role>  roles = roleService.buscaPorAcesso("ROLE_USER");

		Usuario usuario = new Usuario();

		/*liberado nulo vira false: liberar o usuário é decisão explícita do admin*/
		usuarioDto.setLiberado(Boolean.TRUE.equals(usuarioDto.getLiberado()));

		usuario.setLogin(usuarioDto.getLogin());
		usuario.setSenha(passwordEncoder.encode(usuarioDto.getSenha()));
		usuario.setLiberado(usuarioDto.getLiberado());
		usuario.setClienteFuncionario(clienteFuncionario);
		usuario.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		usuario = usuarioRepository.saveAndFlush(usuario);

		for (Role role : roles) {
			RoleUsuario roleUsuario = new RoleUsuario();
			roleUsuario.setAcesso(role);
			roleUsuario.setUsuario(usuario);

			roleUsuarioService.salvar(roleUsuario);
		}

		clienteFuncionario.setUsuario(usuario);
		clienteFuncionarioService.salvarClienteFunUser(clienteFuncionario);

		usuarioDto.setSenha("***Ocultada***");/*Não pode expor a senha na rede*/
		usuarioDto.setId(usuario.getId());
		usuarioDto.setClienteFuncionarioId(clienteFuncionario.getId());
		usuarioDto.setEmpresa(clienteFuncionario.getEmpresa().getPessoa().getNome());
		usuarioDto.setPessoa(clienteFuncionario.getPessoa().getNome());
		usuarioDto.setTipoClienteFuncionario(clienteFuncionario.getTipoClienteFuncionario());
		return usuarioDto;
	}

	@Transactional
	public UsuarioDto atualizar(UsuarioDto usuarioDto) {

		exigirAdmin("atualizar usuários");

		if (usuarioDto.getId() == null) {
			throw new MsgApiException("Id do usuário deve ser informado para atualizar.");
		}

		/*liberado nulo vira false: liberar o usuário é decisão explícita do admin*/
		usuarioDto.setLiberado(Boolean.TRUE.equals(usuarioDto.getLiberado()));
		boolean bloqueando = !usuarioDto.getLiberado();

		/*O admin logado não tira o próprio acesso (P25)*/
		if (bloqueando && usuarioLogadoService.isUsuarioLogado(usuarioDto.getId())) {
			throw new MsgApiException(MSG_BLOQUEAR_A_SI_MESMO, HttpStatus.FORBIDDEN);
		}

		Usuario usuarioBanco = buscarPorId(usuarioDto.getId(), usuarioLogadoService.getEmpresaIdLogada())
				.orElseThrow(() -> new MsgApiException("Usuário não encontrado para a empresa logada."));

		/*P25: um admin não bloqueia outro; quem já estava bloqueado pode continuar assim e ser editado*/
		if (bloqueando && Boolean.TRUE.equals(usuarioBanco.getLiberado()) && usuarioBanco.idAdmin()) {
			throw new MsgApiException(MSG_ADMIN_NAO_BLOQUEIA);
		}

		if (usuarioRepository.existeOutroUsuarioComPessoa(usuarioDto.getPessoaId(), usuarioDto.getId(), usuarioLogadoService.getEmpresaIdLogada())) {
			throw new MsgApiException("Existe outro usuário associado a pessoa que foi selecionada.");
		}


		ClienteFuncionario clienteFuncionario =  clienteFuncionarioService.findByPessoa(usuarioDto.getPessoaId(), usuarioLogadoService.getEmpresaIdLogada());

		if (clienteFuncionario == null) {
			throw new MsgApiException("Não foi informado o registro de pessoa/ cliente ou funcioário para o usuário.");
		}

		usuarioBanco.setClienteFuncionario(clienteFuncionario);
		usuarioBanco.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		usuarioBanco.setLogin(usuarioDto.getLogin());
		usuarioBanco.setLiberado(usuarioDto.getLiberado());

		usuarioBanco = usuarioRepository.saveAndFlush(usuarioBanco);

		usuarioDto.setSenha("***Ocultada***");/*Não pode expor a senha na rede*/
		usuarioDto.setId(usuarioBanco.getId());
		usuarioDto.setClienteFuncionarioId(clienteFuncionario.getId());
		usuarioDto.setEmpresa(clienteFuncionario.getEmpresa().getPessoa().getNome());
		usuarioDto.setPessoa(clienteFuncionario.getPessoa().getNome());
		usuarioDto.setTipoClienteFuncionario(clienteFuncionario.getTipoClienteFuncionario());

		return usuarioDto;
	}


	@Transactional
	public void alterarSenha(AlterarSenhaDTO dto) {

		/*O id vem do request: o usuário comum só troca a própria senha*/
		if (!usuarioLogadoService.isAdmin() && !usuarioLogadoService.getUsuarioLogadoId().equals(dto.getId())) {
			throw new MsgApiException("Você só pode alterar a sua própria senha.", HttpStatus.FORBIDDEN);
		}

		Usuario usuario = usuarioRepository.buscarPorId(dto.getId(), usuarioLogadoService.getEmpresaIdLogada())
				.orElseThrow(() -> new MsgApiException("Usuário não encontrado."));

	  if(!usuario.isEnabled()) {
			throw new MsgApiException("Usuário bloqueado, entre em contato com o administrador do sistema.", HttpStatus.UNAUTHORIZED);
		}

		if(usuario.getEmpresa().getBloqueio()) {
			throw new MsgApiException("Empresa bloqueada, entre em contato com o administrador do sistema.", HttpStatus.UNAUTHORIZED);
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

	@Transactional
	public void deleteById(Long id, Long idEmpresa) {
		exigirAdmin("deletar usuários");

		Usuario usuario = usuarioRepository.buscarPorId(id, idEmpresa)
				.orElseThrow(() -> new MsgApiException("Usuário não encontrado ou já foi deletado."));

		/*P25: o admin logado não se apaga, e nenhum admin é apagado*/
		if (usuarioLogadoService.isUsuarioLogado(usuario.getId())) {
			throw new MsgApiException(MSG_APAGAR_A_SI_MESMO, HttpStatus.FORBIDDEN);
		}
		if (usuario.idAdmin()) {
			throw new MsgApiException(MSG_ADMIN_NAO_APAGA);
		}

		clienteFuncionarioService.removeUserClienteFuncionarioId(id, idEmpresa);
		usuarioRepository.deleteById(id, idEmpresa);
	}

	/**
	 * Apaga os usuários da empresa que não são administradores (P25): o admin
	 * logado e os outros admins ficam. Antes, solta o vínculo dos cadastros de
	 * cliente/funcionário com esses usuários, como no deleteById.
	 *
	 * @return quantidade de usuários apagados
	 */
	@Transactional
	public long deleteAll(Long empresaID) {
		exigirAdmin("deletar usuários");

		List<Long> naoAdministradores = usuarioRepository.findAll(empresaID).stream()
				.filter(usuario -> !usuario.idAdmin())
				.map(Usuario::getId)
				.toList();

		if (naoAdministradores.isEmpty()) {
			throw new MsgApiException(MSG_NADA_A_APAGAR);
		}

		apagarSemVinculos(naoAdministradores, empresaID);
		return naoAdministradores.size();
	}

	private void apagarSemVinculos(List<Long> ids, Long empresaId) {
		ids.forEach(id -> clienteFuncionarioService.removeUserClienteFuncionarioId(id, empresaId));
		usuarioRepository.deletarAllById(ids, empresaId);
	}

	@Transactional
	void deletarAllById(Iterable<Long> ids, Long empresaId) {
		exigirAdmin("deletar usuários");

		List<Usuario> usuarios = usuarioRepository.buscarPorIds(ids, empresaId);
		List<Long> encontrados = usuarios.stream().map(Usuario::getId).toList();
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
			throw new MsgApiException("Usuários não encontrados ou já deletados: " + naoEncontrados);
		}

		/*P25: tudo ou nada; com o admin logado ou outro admin na lista, nada é apagado*/
		if (encontrados.stream().anyMatch(usuarioLogadoService::isUsuarioLogado)) {
			throw new MsgApiException(MSG_APAGAR_A_SI_MESMO, HttpStatus.FORBIDDEN);
		}
		List<Long> administradores = usuarios.stream().filter(Usuario::idAdmin).map(Usuario::getId).toList();
		if (!administradores.isEmpty()) {
			throw new MsgApiException("Usuários administradores não podem ser apagados: " + administradores
					+ ". Remova antes o acesso ROLE_ADMIN deles em Acessos do usuário ou tire-os da lista.");
		}

		apagarSemVinculos(encontrados, empresaId);
	}

	public List<Usuario> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return usuarioRepository.buscarPorIds(ids, empresaId);
	}

	public boolean existsById(Long id, Long empresaId) {
		return usuarioRepository.existsById(id, empresaId);
	}

	public List<UsuarioDto> listar(Long empresaId) {
		return converterListaParaDto(usuarioRepository.listar(empresaId));
	}

	public Optional<Usuario> buscarPorId(Long id, Long empresaId) {
		return usuarioRepository.buscarPorId(id, empresaId);
	}


	public UsuarioDto buscarPorIdDto(Long id, Long empresaId) {

		Usuario usuario = usuarioRepository.buscarPorId(id, empresaId)
				.orElseThrow(() -> new MsgApiException("Usuário não foi encontrado na busca."));

		return converterParaDto(usuario);
	}

	public long total(Long empresaId) {
		return usuarioRepository.total(empresaId);
	}

	public Page<Usuario> listarPaginado(Long empresaId, Pageable pageable) {
		return usuarioRepository.listarPaginado(empresaId, pageable);
	}


	public List<UsuarioDto> listarPaginadoDto(Long empresaId, Pageable pageable) {
		return converterListaParaDto(usuarioRepository.listarPaginado(empresaId, pageable).getContent());
	}

	/**
	 * Método auxiliar para converter Usuario em UsuarioDto.
	 * Leva o login e o pessoaId para o retorno servir de base para o atualizar.
	 * Nunca copia a senha.
	 * @param usuario
	 * @return UsuarioDto
	 */
	private UsuarioDto converterParaDto(Usuario usuario) {
		UsuarioDto dto = new UsuarioDto();
		dto.setId(usuario.getId());
		dto.setLogin(usuario.getLogin());
		dto.setLiberado(usuario.isEnabled());
		dto.setEmpresa(usuario.getEmpresa().getPessoa().getNome());
		dto.setClienteFuncionarioId(usuario.getClienteFuncionario().getId());
		dto.setTipoClienteFuncionario(usuario.getClienteFuncionario().getTipoClienteFuncionario());
		dto.setPessoaId(usuario.getClienteFuncionario().getPessoa().getId());
		dto.setPessoa(usuario.getClienteFuncionario().getPessoa().getNome());
		return dto;
	}

	private List<UsuarioDto> converterListaParaDto(List<Usuario> usuarios) {
		return usuarios.stream().map(this::converterParaDto).toList();
	}

	/**
	 * Busca usuários por nome e retorna como DTO
	 * @param nome
	 * @param empresaId
	 * @return List<UsuarioDto>
	 */
	public List<UsuarioDto> buscaPorNomeDto(String nome, Long empresaId) {
		return converterListaParaDto(usuarioRepository.buscaPorNome(nome, empresaId));
	}

	/**
	 * Busca usuário por login e retorna como DTO
	 * @param login
	 * @param empresaId
	 * @return UsuarioDto, ou null se o login não existir na empresa logada
	 */
	public UsuarioDto buscaPorLoginDto(String login, Long empresaId) {
		Usuario usuario = usuarioRepository.buscaPorLogin(login);

		/*buscaPorLogin não filtra a empresa (é a consulta do login): aqui só devolve usuário da empresa logada*/
		if (usuario == null || !usuario.getEmpresa().getId().equals(empresaId)) {
			return null;
		}

		return converterParaDto(usuario);
	}

	/**
	 * Busca usuários por lista de IDs e retorna como DTO
	 * @param ids
	 * @param empresaId
	 * @return List<UsuarioDto>
	 */
	public List<UsuarioDto> buscarPorIdsDto(Iterable<Long> ids, Long empresaId) {
		return converterListaParaDto(usuarioRepository.buscarPorIds(ids, empresaId));
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
