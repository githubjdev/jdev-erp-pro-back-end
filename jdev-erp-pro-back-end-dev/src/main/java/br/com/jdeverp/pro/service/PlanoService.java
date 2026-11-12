package br.com.jdeverp.pro.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.PlanoDTO;
import br.com.jdeverp.pro.enums.TipoPlano;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Plano;
import br.com.jdeverp.pro.repository.PlanoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
@Transactional(readOnly = true)
public class PlanoService {

	@Autowired /* Injeção de dependência */
	private PlanoRepository planoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	/*Plano é dado global (vale para todas as empresas): só ROLE_ADMIN cadastra, altera ou apaga*/
	private void exigirAdmin(String operacao) {
		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem " + operacao + " planos.", HttpStatus.FORBIDDEN);
		}
	}

	/*Igual ao CHECK plano_valor_mensal_check do banco: FREE vale 0; os demais, de R$ 49 a R$ 200*/
	private void validarValorMensal(Plano plano) {
		if (plano.getValorMensal() == null) {
			throw new MsgApiException("Valor mensal não pode ser nulo");
		}

		double valor = plano.getValorMensal();

		if (plano.getTipoPlano() == TipoPlano.FREE && valor != 0) {
			throw new MsgApiException("O plano FREE deve ter valor mensal 0.");
		}

		if (plano.getTipoPlano() != TipoPlano.FREE && (valor < 49 || valor > 200)) {
			throw new MsgApiException("O valor mensal do plano deve ficar entre R$ 49 e R$ 200.");
		}
	}

	@Transactional
	public PlanoDTO salvar(Plano plano) {
		exigirAdmin("cadastrar");

		/*O plano FREE é inserido direto no banco e não se cadastra outro pela API (P11)*/
		if (plano.getTipoPlano() == TipoPlano.FREE && planoRepository.existePorTipo(TipoPlano.FREE)) {
			throw new MsgApiException("Já existe o plano FREE: ele é cadastrado direto no banco e não pode haver outro.");
		}

		validarValorMensal(plano);

		if (planoRepository.existePorNome(plano.getNome())) {
			throw new MsgApiException("Já existe um plano com o mesmo nome: " + plano.getNome());
		}

		return converterParaDto(planoRepository.saveAndFlush(plano));
	}

	@Transactional
	public PlanoDTO atualizar(Plano plano) {
		exigirAdmin("atualizar");

		if (plano.getId() == null) {
			throw new MsgApiException("Id do plano deve ser informado para edição.");
		}

		if (!planoRepository.findById(plano.getId()).isPresent()) {
			throw new MsgApiException("Plano com id: " + plano.getId() + " não foi encontrado.");
		}

		/*Transformar outro plano em FREE criaria um segundo plano FREE (P11)*/
		if (plano.getTipoPlano() == TipoPlano.FREE && planoRepository.existePorTipoDiferenteId(plano.getId(), TipoPlano.FREE)) {
			throw new MsgApiException("Já existe o plano FREE: ele é cadastrado direto no banco e não pode haver outro.");
		}

		validarValorMensal(plano);

		if (planoRepository.existePorNomeDiferenteId(plano.getId(), plano.getNome())) {
			throw new MsgApiException("Já existe outro plano com o mesmo nome: " + plano.getNome());
		}

		return converterParaDto(planoRepository.saveAndFlush(plano));
	}

	public Optional<PlanoDTO> buscarPorId(Long id) {
		return planoRepository.findById(id).map(this::converterParaDto);
	}

	public List<PlanoDTO> findAll() {

		return converterListaParaDto(planoRepository.findAll());
	}

	public List<PlanoDTO> buscaPorNome(String nome) {
		return converterListaParaDto(planoRepository.buscaPorNome(nome));
	}

	public boolean existePorNome(String nome) {
		return planoRepository.existePorNome(nome);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome) {
		return planoRepository.existePorNomeDiferenteId(id, nome);
	}

	@Transactional
	public void deleteById(Long id) {
		exigirAdmin("deletar");

		if (!planoRepository.findById(id).isPresent()) {
			throw new MsgApiException("Plano não encontrado ou já foi deletado.");
		}

		planoRepository.deleteById(id);
	}

	// ====================Conversão para DTO====================

	private PlanoDTO converterParaDto(Plano plano) {
		PlanoDTO dto = new PlanoDTO();
		dto.setId(plano.getId());
		dto.setNome(plano.getNome());
		dto.setDescricao(plano.getDescricao());
		dto.setAtivo(plano.getAtivo());
		dto.setValorMensal(plano.getValorMensal());
		dto.setLimiteUsuario(plano.getLimiteUsuario());
		dto.setLimiteCliente(plano.getLimiteCliente());
		dto.setTipoPlano(plano.getTipoPlano());
		return dto;
	}

	private List<PlanoDTO> converterListaParaDto(List<Plano> planos) {
		return planos.stream().map(this::converterParaDto).toList();
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
