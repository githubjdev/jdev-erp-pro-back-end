package br.com.jdeverp.pro.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.repository.CategoriaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class CategoriaService {

	@Autowired /* Injeção de depência */
	private CategoriaRepository categoriaRepository;
	
	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;
	
	
	@Autowired
	private UsuarioLogadoService usuarioLogadoService;
	
	
	public Categoria salvar(Categoria categoria) {
		
		if (this.existePorNome(categoria.getNome(), usuarioLogadoService.getEmpresaIdLogada())) {
			throw new MsgApiException("Já existe uma categoria com o mesmo nome para a empresa logada.");
		}
		
		categoria.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		
		return categoriaRepository.save(categoria);
	}
	
	
	public Categoria atualizar(Categoria categoria) {
		
		if(categoria.getId() == null) {
			throw new MsgApiException("Id da categoria não informado para atualizar.");
		}
		
		if (this.existePorNomeDiferenteId(categoria.getId(),  categoria.getNome(), usuarioLogadoService.getEmpresaIdLogada())) {
			throw new MsgApiException("Já existe uma categoria com o mesmo nome desta que sendo informada para atualizar, informe outro nome.");
		}
		
		categoria.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		
		return categoriaRepository.save(categoria);
	}
	

	/* Os métodos do service serão chamador pelo Controller */
	public List<Categoria> findAll(Long idEmpresa) {
		return categoriaRepository.findAll(idEmpresa);
	}

	public List<Categoria> buscaPorNome(String nome, Long idEmpresa) {
		return categoriaRepository.buscaPorNome(nome, idEmpresa);
	}

	boolean existePorNome(String nome, Long idEmpresa) {

		return categoriaRepository.existePorNome(nome, idEmpresa);
	}

	boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return categoriaRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	public void deleteById(Long id, Long idEmpresa) {
		
		if(!categoriaRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Categoria não encontrada para a empresa logada, portanto não pode ser deletada.");
		}
		
		categoriaRepository.deleteById(id, idEmpresa);
	}

	public Categoria buscarPorId(Long id, Long idEmpresa) {
		return categoriaRepository.buscarPorId(id, idEmpresa).orElseThrow(() -> 
			new MsgApiException("Categoria não encontrada para a empresa logada."));
	}

	public long total(Long idEmpresa) {
		return categoriaRepository.total(idEmpresa);
	}

	public boolean existsById(Long id, Long idEmpresa) {
		return categoriaRepository.existsById(id, idEmpresa);
	}

	public Page<Categoria> listarPaginado(Long idEmpresa, Pageable pageable) {
		return categoriaRepository.listarPaginado(idEmpresa, pageable);
	}

	public List<Categoria> listarPaginadoDto(Long idEmpresa, Pageable pageable) {
		
		Page<Categoria> categorias = categoriaRepository.listarPaginado(idEmpresa, pageable);
		List<Categoria> listaRetorno = new java.util.ArrayList<>();
		
		for (Categoria categoria : categorias) {
			listaRetorno.add(categoria);
		}
		
		return listaRetorno;		
	}

}
