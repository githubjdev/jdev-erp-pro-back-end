package br.com.jdeverp.pro.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.jdeverp.pro.contexto.TestContextoSpring;

public class TestCategoriaRepository extends TestContextoSpring {

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Test
	public void testfindAll() {

		categoriaRepository.findAll(1L);

	}

}
