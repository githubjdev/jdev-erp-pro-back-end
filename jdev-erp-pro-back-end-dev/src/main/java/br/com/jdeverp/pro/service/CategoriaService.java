package br.com.jdeverp.pro.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.CategoriaDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.repository.CategoriaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

	@Autowired /* Injeção de depência */
	private CategoriaRepository categoriaRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;


	@Autowired
	private UsuarioLogadoService usuarioLogadoService;


	@Transactional
	public CategoriaDTO salvar(Categoria categoria) {

		if (this.existePorNome(categoria.getNome(), usuarioLogadoService.getEmpresaIdLogada())) {
			throw new MsgApiException("Já existe uma categoria com o mesmo nome para a empresa logada.");
		}

		categoria.setEmpresa(usuarioLogadoService.getEmpresaLogada());

		return converterParaDto(categoriaRepository.saveAndFlush(categoria));
	}


	@Transactional
	public CategoriaDTO atualizar(Categoria categoria) {

		if(categoria.getId() == null) {
			throw new MsgApiException("Id da categoria não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();

		/*Só atualiza categoria da empresa logada*/
		Categoria existente = categoriaRepository.buscarPorId(categoria.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Categoria não encontrada para a empresa logada."));

		if (this.existePorNomeDiferenteId(categoria.getId(),  categoria.getNome(), empresaId)) {
			throw new MsgApiException("Já existe uma categoria com o mesmo nome desta que sendo informada para atualizar, informe outro nome.");
		}

		existente.setNome(categoria.getNome());

		return converterParaDto(categoriaRepository.saveAndFlush(existente));
	}


	/* Os métodos do service serão chamador pelo Controller */
	public List<CategoriaDTO> findAll(Long idEmpresa) {
		return converterListaParaDto(categoriaRepository.findAll(idEmpresa));
	}

	public List<CategoriaDTO> buscaPorNome(String nome, Long idEmpresa) {
		return converterListaParaDto(categoriaRepository.buscaPorNome(nome, idEmpresa));
	}

	public boolean existePorNome(String nome, Long idEmpresa) {

		return categoriaRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return categoriaRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	@Transactional
	public void deleteById(Long id, Long idEmpresa) {

		if(!categoriaRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Categoria não encontrada para a empresa logada, portanto não pode ser deletada.");
		}

		categoriaRepository.deleteById(id, idEmpresa);
	}

	public CategoriaDTO buscarPorId(Long id, Long idEmpresa) {
		Categoria categoria = categoriaRepository.buscarPorId(id, idEmpresa).orElseThrow(() ->
			new MsgApiException("Categoria não encontrada para a empresa logada."));

		return converterParaDto(categoria);
	}

	public long total(Long idEmpresa) {
		return categoriaRepository.total(idEmpresa);
	}

	public boolean existsById(Long id, Long idEmpresa) {
		return categoriaRepository.existsById(id, idEmpresa);
	}

	public Page<CategoriaDTO> listarPaginado(Long idEmpresa, Pageable pageable) {
		return categoriaRepository.listarPaginado(idEmpresa, pageable).map(this::converterParaDto);
	}

	public List<CategoriaDTO> listarPaginadoDto(Long idEmpresa, Pageable pageable) {
		return listarPaginado(idEmpresa, pageable).getContent();
	}

	private CategoriaDTO converterParaDto(Categoria categoria) {
		CategoriaDTO dto = new CategoriaDTO();
		dto.setId(categoria.getId());
		dto.setNome(categoria.getNome());
		dto.setEmpresaId(categoria.getEmpresa().getId());
		return dto;
	}

	private List<CategoriaDTO> converterListaParaDto(List<Categoria> categorias) {
		return categorias.stream().map(this::converterParaDto).toList();
	}

}
