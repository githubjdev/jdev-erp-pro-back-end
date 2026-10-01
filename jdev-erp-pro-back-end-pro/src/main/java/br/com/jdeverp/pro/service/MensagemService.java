package br.com.jdeverp.pro.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.model.Mensagem;
import br.com.jdeverp.pro.model.Usuario;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import br.com.jdeverp.pro.repository.MensagemRepository;
import br.com.jdeverp.pro.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class MensagemService {

	@Autowired /* Injeção de dependência */
	private MensagemRepository mensagemRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private ChamadoRepository chamadoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	public Mensagem salvar(Mensagem mensagem) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();

		mensagem.setChamado(validarChamado(mensagem.getChamado(), empresaId));
		mensagem.setAtendente(validarUsuario(mensagem.getAtendente(), empresaId));
		mensagem.setCliente(validarUsuario(mensagem.getCliente(), empresaId));
		mensagem.setDataEnvio(LocalDate.now());
		mensagem.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return mensagemRepository.saveAndFlush(mensagem);
	}

	public Mensagem atualizar(Mensagem mensagem) {
		if (mensagem.getId() == null) {
			throw new MsgApiException("Id da mensagem não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		Mensagem existente = mensagemRepository.buscarPorId(mensagem.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Mensagem não encontrada para a empresa logada."));

		existente.setConteudo(mensagem.getConteudo());
		existente.setArquivo(mensagem.getArquivo());
		existente.setLida(mensagem.getLida());
		existente.setChamado(validarChamado(mensagem.getChamado(), empresaId));
		existente.setAtendente(validarUsuario(mensagem.getAtendente(), empresaId));
		existente.setCliente(validarUsuario(mensagem.getCliente(), empresaId));
		return mensagemRepository.saveAndFlush(existente);
	}

	private Chamado validarChamado(Chamado chamado, Long empresaId) {
		if (chamado == null || chamado.getId() == null) {
			throw new MsgApiException("Chamado deve ser informado para a mensagem.");
		}

		return chamadoRepository.buscarPorId(chamado.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Chamado não encontrado para a empresa logada."));
	}

	private Usuario validarUsuario(Usuario usuario, Long empresaId) {
		if (usuario == null || usuario.getId() == null) {
			throw new MsgApiException("Cliente e atendente devem ser informados para a mensagem.");
		}

		return usuarioRepository.buscarPorId(usuario.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Usuário relacionado não encontrado para a empresa logada."));
	}

	public List<Mensagem> findAll(Long idEmpresa) {
		
		return mensagemRepository.findAll(idEmpresa);
	}

	public List<Mensagem> buscaPorConteudo(String conteudo, Long idEmpresa) {
		return mensagemRepository.buscaPorConteudo(conteudo, idEmpresa);
	}

	public boolean existePorConteudo(String conteudo, Long idEmpresa) {
		return mensagemRepository.existePorConteudo(conteudo, idEmpresa);
	}

	public boolean existePorConteudoDiferenteId(Long id, String conteudo, Long idEmpresa) {
		return mensagemRepository.existePorConteudoDiferenteId(id, conteudo, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {
		if (!mensagemRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Mensagem não encontrada ou já foi deletada.");
		}

		mensagemRepository.deleteById(id, idEmpresa);
	}

	public long deleteAll(Long empresaID) {
		if (mensagemRepository.total(empresaID) == 0) {
			throw new MsgApiException("Nenhuma mensagem encontrada para deletar ou todas já foram deletadas.");
		}

		return mensagemRepository.deleteAll(empresaID);
	}

	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		List<Long> encontrados = mensagemRepository.buscarPorIds(ids, empresaId).stream().map(Mensagem::getId).toList();
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
			throw new MsgApiException("Mensagens não encontradas ou já deletadas: " + naoEncontrados);
		}

		mensagemRepository.deletarAllById(ids, empresaId);
	}

	public List<Mensagem> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return mensagemRepository.buscarPorIds(ids, empresaId);
	}

	public boolean existsById(Long id, Long empresaId) {
		return mensagemRepository.existsById(id, empresaId);
	}

	public List<Mensagem> listar(Long empresaId) {
		return mensagemRepository.listar(empresaId);
	}

	public Optional<Mensagem> buscarPorId(Long id, Long empresaId) {
		return mensagemRepository.buscarPorId(id, empresaId);
	}

	public long total(Long empresaId) {
		return mensagemRepository.total(empresaId);
	}

	public Page<Mensagem> listarPaginado(Long empresaId, Pageable pageable) {
		return mensagemRepository.listarPaginado(empresaId, pageable);
	}

	// ====================Métodos específicos para Chamado====================

	public List<Mensagem> findAllByChamado(Long idChamado, Long idEmpresa) {
		return mensagemRepository.findAllByChamado(idChamado, idEmpresa);
	}

	public List<Mensagem> buscaPorConteudoByChamado(String conteudo, Long idChamado, Long idEmpresa) {
		return mensagemRepository.buscaPorConteudoByChamado(conteudo, idChamado, idEmpresa);
	}

	public boolean existePorConteudoByChamado(String conteudo, Long idChamado, Long idEmpresa) {
		return mensagemRepository.existePorConteudoByChamado(conteudo, idChamado, idEmpresa);
	}

	public boolean existePorConteudoDiferenteIdByChamado(Long id, String conteudo, Long idChamado, Long idEmpresa) {
		return mensagemRepository.existePorConteudoDiferenteIdByChamado(id, conteudo, idChamado, idEmpresa);
	}

	public long countByChamado(Long idChamado, Long idEmpresa) {
		return mensagemRepository.countByChamado(idChamado, idEmpresa);
	}

	public void deleteAllByChamado(Long idChamado, Long idEmpresa) {
		if (mensagemRepository.countByChamado(idChamado, idEmpresa) == 0) {
			throw new MsgApiException("Nenhuma mensagem encontrada para o chamado ou todas já foram deletadas.");
		}

		mensagemRepository.deleteAllByChamado(idChamado, idEmpresa);
	}

	public void deleteByIdAndChamado(Long id, Long idChamado, Long idEmpresa) {
		if (!mensagemRepository.buscarPorId(id, idEmpresa).filter(mensagem -> mensagem.getChamado() != null && mensagem.getChamado().getId().equals(idChamado)).isPresent()) {
			throw new MsgApiException("Mensagem não encontrada ou já foi deletada.");
		}

		mensagemRepository.deleteByIdAndChamado(id, idChamado, idEmpresa);
	}

	// ====================Métodos para Status de Leitura====================

	public List<Mensagem> findAllNaoLidas(Long idEmpresa) {
		return mensagemRepository.findAllNaoLidas(idEmpresa);
	}

	public List<Mensagem> findAllNaoLidasByChamado(Long idChamado, Long idEmpresa) {
		return mensagemRepository.findAllNaoLidasByChamado(idChamado, idEmpresa);
	}

	public long countNaoLidasByChamado(Long idChamado, Long idEmpresa) {
		return mensagemRepository.countNaoLidasByChamado(idChamado, idEmpresa);
	}

	public void updateLida(Long id, Boolean lida, Long idEmpresa) {
		mensagemRepository.updateLida(id, lida, idEmpresa);
	}

	// ====================Métodos para Atendente====================

	public List<Mensagem> findAllByAtendente(Long idAtendente, Long idEmpresa) {
		return mensagemRepository.findAllByAtendente(idAtendente, idEmpresa);
	}

	public long countByAtendente(Long idAtendente, Long idEmpresa) {
		return mensagemRepository.countByAtendente(idAtendente, idEmpresa);
	}

	// ====================Métodos para Cliente====================

	public List<Mensagem> findAllByCliente(Long idCliente, Long idEmpresa) {
		return mensagemRepository.findAllByCliente(idCliente, idEmpresa);
	}

	public long countByCliente(Long idCliente, Long idEmpresa) {
		return mensagemRepository.countByCliente(idCliente, idEmpresa);
	}

	// ====================Métodos Combinados====================

	public List<Mensagem> findAllByChamadoAndAtendente(Long idChamado, Long idAtendente, Long idEmpresa) {
		return mensagemRepository.findAllByChamadoAndAtendente(idChamado, idAtendente, idEmpresa);
	}

	public List<Mensagem> findAllNaoLidasByAtendente(Long idAtendente, Long idEmpresa) {
		return mensagemRepository.findAllNaoLidasByAtendente(idAtendente, idEmpresa);
	}

	public List<Mensagem> findAllNaoLidasByCliente(Long idCliente, Long idEmpresa) {
		return mensagemRepository.findAllNaoLidasByCliente(idCliente, idEmpresa);
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
