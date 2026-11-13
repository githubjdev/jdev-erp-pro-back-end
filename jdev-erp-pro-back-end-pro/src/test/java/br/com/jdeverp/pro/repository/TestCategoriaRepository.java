package br.com.jdeverp.pro.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import br.com.jdeverp.pro.contexto.TestContextoSpring;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.model.Empresa;

public class TestCategoriaRepository extends TestContextoSpring {

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
	
	
	@Test
	public void testeListaPaginada() {
		Empresa empresa = empresaRepository.findById(1L).get();
		
		Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC,"nome"));
		
		Page<Categoria> page = categoriaRepository.listarPaginado(empresa.getId(), pageable);
		
		assertEquals("Acessórios", page.getContent().get(0).getNome());
		assertEquals("Beleza e Saúde", page.getContent().get(4).getNome());
		assertEquals(5, page.getContent().size());
		
	}
	
	@Test
	public void testFindAll() {

		assertThrows(UnsupportedOperationException.class, () ->{
			categoriaRepository.findAll();
		});
	}

	/* Isolamento por empresa: as mesmas consultas, numa empresa que não é a da categoria */
	private static final Long OUTRA_EMPRESA = -1L;

	private Categoria categoriaUnica(String nome) {
		Categoria categoria = new Categoria();
		categoria.setNome(nome + " " + System.nanoTime());
		categoria.setEmpresa(empresaRepository.findById(1L).get());
		return categoriaRepository.saveAndFlush(categoria);
	}

	@Test
	public void testbuscaPorNomeOutraEmpresa() {
		Categoria categoria = categoriaUnica("Instrumentos Músicais");

		/* Sem acento e em minúsculas, na própria empresa */
		assertEquals(categoria.getId(), categoriaRepository.buscaPorNome("instrumentos musicais", 1L).stream()
				.filter(c -> c.getId().equals(categoria.getId())).findFirst().get().getId());
		assertTrue(categoriaRepository.buscaPorNome(categoria.getNome(), OUTRA_EMPRESA).isEmpty());
		assertTrue(categoriaRepository.findAll(OUTRA_EMPRESA).isEmpty());
	}

	@Test
	public void testexistePorNomeOutraEmpresa() {
		Categoria categoria = categoriaUnica("Jardinagem");

		assertFalse(categoriaRepository.existePorNome(categoria.getNome(), OUTRA_EMPRESA));
		assertFalse(categoriaRepository.existePorNomeDiferenteId(-1L, categoria.getNome(), OUTRA_EMPRESA));
		/* A própria categoria não conta como outra com o mesmo nome */
		assertFalse(categoriaRepository.existePorNomeDiferenteId(categoria.getId(), categoria.getNome(), 1L));
	}

	@Test
	public void testdeleteByIdOutraEmpresa() {
		Categoria categoria = categoriaUnica("Pet Shop");

		categoriaRepository.deleteById(categoria.getId(), OUTRA_EMPRESA);

		assertTrue(categoriaRepository.existsById(categoria.getId(), 1L));
	}



}
