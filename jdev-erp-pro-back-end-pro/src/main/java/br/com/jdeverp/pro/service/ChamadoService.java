package br.com.jdeverp.pro.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.Chamado;
import br.com.jdeverp.pro.repository.ChamadoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ChamadoService {

	@Autowired /* Injeção de depência */
	private ChamadoRepository chamadoRepository;
	
	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;
	

	/* Os métodos do service serão chamador pelo Controller */
	public List<Chamado> findAll(Long idEmpresa) {
		return chamadoRepository.findAll(idEmpresa);
	}

	List<Chamado> buscaPorTitulo(String titulo, Long idEmpresa) {
		return chamadoRepository.buscaPorTitulo(titulo, idEmpresa);
	}

	boolean existePorTitulo(String titulo, Long idEmpresa) {
		return chamadoRepository.existePorTitulo(titulo, idEmpresa);
	}

	boolean existePorTituloDiferenteId(Long id, String titulo, Long idEmpresa) {
		return chamadoRepository.existePorTituloDiferenteId(id, titulo, idEmpresa);
	}

	void deleteById(Long id, Long idEmpresa) {
		chamadoRepository.deleteById(id, idEmpresa);
	}

}
