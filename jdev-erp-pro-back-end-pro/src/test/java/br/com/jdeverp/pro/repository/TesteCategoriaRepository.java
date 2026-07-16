package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.jdeverp.pro.contexto.TestContextoSpring;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.model.Empresa;

public class TesteCategoriaRepository extends TestContextoSpring {

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Test
	public void testbuscaPorNome() {

		Empresa empresa = empresaRepository.findById(1L).get();

		Categoria categoria = new Categoria();
		categoria.setNome("Eletrodoméstico");
		categoria.setEmpresa(empresa);

		/* Salva no banco e retorno os dados salvos */
		categoria = categoriaRepository.saveAndFlush(categoria);

		/* Verifica os dados salvos */
		assertTrue(categoria.getId() > 0);
		assertEquals("Eletrodoméstico", categoria.getNome());

		/* Busca e esta se o método buscaPorNome está trasendo a categoria */
		categoria = categoriaRepository.buscaPorNome("Eletrodoméstico", empresa.getId()).get(0);
		assertTrue(categoria.getId() > 0);
		assertEquals("Eletrodoméstico", categoria.getNome());

	}

	@Test
	public void testfindAll() {

		Empresa empresa = empresaRepository.findById(1L).get();

		Categoria categoria = new Categoria();
		categoria.setNome("Eletrodoméstico");
		categoria.setEmpresa(empresa);

		/* Salva no banco e retorno os dados salvos */
		categoria = categoriaRepository.saveAndFlush(categoria);

		/* Verifica os dados salvos */
		assertTrue(categoria.getId() > 0);
		assertEquals("Eletrodoméstico", categoria.getNome());

		/* Busca e esta se o método buscaPorNome está trasendo a categoria */
		categoria = categoriaRepository.findAll(empresa.getId()).get(0);
		assertTrue(categoria.getId() > 0);
		assertEquals("Eletrodoméstico", categoria.getNome());

	}

	@Test
	public void testexistePorNome() {

		Empresa empresa = empresaRepository.findById(1L).get();

		Categoria categoria = new Categoria();
		categoria.setNome("Eletrodoméstico");
		categoria.setEmpresa(empresa);

		/* Salva no banco e retorno os dados salvos */
		categoria = categoriaRepository.saveAndFlush(categoria);

		/* Verifica os dados salvos */
		assertTrue(categoria.getId() > 0);
		assertEquals("Eletrodoméstico", categoria.getNome());

		/* Busca e esta se o método buscaPorNome está trasendo a categoria */
		boolean existe = categoriaRepository.existePorNome("Eletrodoméstico", empresa.getId());
		assertTrue(existe);

	}
	
	
	@Test
	public void testexistePorNomeDiferenteId() {

		Empresa empresa = empresaRepository.findById(1L).get();

		Categoria categoria = new Categoria();
		categoria.setNome("Eletrodoméstico");
		categoria.setEmpresa(empresa);

		/* Salva no banco e retorno os dados salvos */
		categoria = categoriaRepository.saveAndFlush(categoria);

		/* Verifica os dados salvos */
		assertTrue(categoria.getId() > 0);
		assertEquals("Eletrodoméstico", categoria.getNome());

		/* Busca e esta se o método buscaPorNome está trasendo a categoria */
		boolean existe = categoriaRepository.
				            existePorNomeDiferenteId(categoria.getId(),
				    		   "Eletrodoméstico", empresa.getId());
		assertTrue(existe);

	}
	
	
	@Test
	public void testdeleteById() {

		Empresa empresa = empresaRepository.findById(1L).get();

		Categoria categoria = new Categoria();
		categoria.setNome("Som Automotivo");
		categoria.setEmpresa(empresa);

		/* Salva no banco e retorno os dados salvos */
		categoria = categoriaRepository.saveAndFlush(categoria);

		/* Verifica os dados salvos */
		assertTrue(categoria.getId() > 0);
		assertEquals("Som Automotivo", categoria.getNome());

		/* Busca e esta se o método buscaPorNome está trasendo a categoria */
		categoriaRepository.deleteById(categoria.getId(), empresa.getId());
		
		boolean existe = categoriaRepository.existePorNome("Som Automotivo", empresa.getId());
		assertFalse(existe);

	}

}
