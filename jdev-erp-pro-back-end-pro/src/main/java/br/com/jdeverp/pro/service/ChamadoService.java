package br.com.jdeverp.pro.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.ChamadoDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;
import br.com.jdeverp.pro.util.DtoUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional(readOnly = true)
public class ChamadoService {

	@Autowired /* Injeção de depência */
	private ChamadoRepository chamadoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	@Transactional
	public ChamadoDTO salvar(Chamado chamado) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (chamadoRepository.existePorTitulo(chamado.getTitulo(), empresaId)) {
			throw new MsgApiException("Já existe um chamado com o mesmo título para a empresa logada.");
		}

		chamado.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		/*Busca o usuário logado de novo: o do UsuarioLogadoService foi carregado fora desta sessão e o DTO precisa do nome dele*/
		chamado.setAbertoUser(validarUsuario(usuarioLogadoService.getUsuarioLogado(), empresaId));
		chamado.setDataAbertura(LocalDate.now());
		chamado.setAtendente(validarUsuario(chamado.getAtendente(), empresaId));
		chamado.setCliente(validarUsuario(chamado.getCliente(), empresaId));
		chamado.setFechadoUser(null);
		chamado.setDataFechamento(null);
		return converterParaDto(chamadoRepository.saveAndFlush(chamado));
	}

	@Transactional
	public ChamadoDTO atualizar(Chamado chamado) {
		if (chamado.getId() == null) {
			throw new MsgApiException("Id do chamado não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		Chamado existente = chamadoRepository.buscarPorId(chamado.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Chamado não encontrado para a empresa logada."));

		if (chamadoRepository.existePorTituloDiferenteId(chamado.getId(), chamado.getTitulo(), empresaId)) {
			throw new MsgApiException("Já existe outro chamado com o mesmo título para a empresa logada.");
		}

		existente.setTitulo(chamado.getTitulo());
		existente.setDescricao(chamado.getDescricao());
		existente.setTipoChamado(chamado.getTipoChamado());
		existente.setPrioridadeChamado(chamado.getPrioridadeChamado());
		existente.setStatusChamado(chamado.getStatusChamado());
		existente.setDataFechamento(chamado.getDataFechamento());
		existente.setFechadoUser(chamado.getFechadoUser() == null ? null
				: validarUsuario(chamado.getFechadoUser(), empresaId));
		existente.setAtendente(validarUsuario(chamado.getAtendente(), empresaId));
		existente.setCliente(validarUsuario(chamado.getCliente(), empresaId));
		return converterParaDto(chamadoRepository.saveAndFlush(existente));
	}

	private Usuario validarUsuario(Usuario usuario, Long empresaId) {
		if (usuario == null || usuario.getId() == null) {
			throw new MsgApiException("Cliente e atendente devem ser informados para o chamado.");
		}

		return usuarioRepository.buscarPorId(usuario.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Usuário relacionado não encontrado para a empresa logada."));
	}

	public Optional<ChamadoDTO> buscarPorId(Long id, Long idEmpresa) {
		return chamadoRepository.buscarPorId(id, idEmpresa).map(this::converterParaDto);
	}

	/* Os métodos do service serão chamador pelo Controller */
	public List<ChamadoDTO> findAll(Long idEmpresa) {
		return converterListaParaDto(chamadoRepository.findAll(idEmpresa));
	}

	public List<ChamadoDTO> buscaPorTitulo(String titulo, Long idEmpresa) {
		return converterListaParaDto(chamadoRepository.buscaPorTitulo(titulo, idEmpresa));
	}

	public boolean existePorTitulo(String titulo, Long idEmpresa) {
		return chamadoRepository.existePorTitulo(titulo, idEmpresa);
	}

	public boolean existePorTituloDiferenteId(Long id, String titulo, Long idEmpresa) {
		return chamadoRepository.existePorTituloDiferenteId(id, titulo, idEmpresa);
	}

	@Transactional
	public void deleteById(Long id, Long idEmpresa) {
		if (!chamadoRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Chamado não encontrado ou já foi deletado.");
		}

		chamadoRepository.deleteById(id, idEmpresa);
	}

	private ChamadoDTO converterParaDto(Chamado chamado) {
		ChamadoDTO dto = new ChamadoDTO();
		dto.setId(chamado.getId());
		dto.setTitulo(chamado.getTitulo());
		dto.setDescricao(chamado.getDescricao());
		dto.setTipoChamado(chamado.getTipoChamado());
		dto.setPrioridadeChamado(chamado.getPrioridadeChamado());
		dto.setStatusChamado(chamado.getStatusChamado());
		dto.setDataAbertura(chamado.getDataAbertura());
		dto.setDataFechamento(chamado.getDataFechamento());
		dto.setAbertoUserId(chamado.getAbertoUser().getId());
		dto.setAbertoUserNome(DtoUtil.nomeUsuario(chamado.getAbertoUser()));
		dto.setFechadoUserId(chamado.getFechadoUser() != null ? chamado.getFechadoUser().getId() : null);
		dto.setFechadoUserNome(DtoUtil.nomeUsuario(chamado.getFechadoUser()));
		dto.setAtendenteId(chamado.getAtendente().getId());
		dto.setAtendenteNome(DtoUtil.nomeUsuario(chamado.getAtendente()));
		dto.setClienteId(chamado.getCliente().getId());
		dto.setClienteNome(DtoUtil.nomeUsuario(chamado.getCliente()));
		dto.setEmpresaId(chamado.getEmpresa().getId());
		return dto;
	}

	private List<ChamadoDTO> converterListaParaDto(List<Chamado> chamados) {
		return chamados.stream().map(this::converterParaDto).toList();
	}

}
