package br.com.jdeverp.pro.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * equals, hashCode e toString das entidades, sem banco. O comportamento com
 * proxy LAZY fora da sessão é testado em {@code service/TestSessaoHibernate}.
 */
public class TestEntidadeBase {

	private static Categoria categoria(Long id, String nome) {
		Categoria categoria = new Categoria();
		categoria.setId(id);
		categoria.setNome(nome);
		return categoria;
	}

	@Test
	public void testEqualsMesmoId() {
		assertEquals(categoria(1L, "Games"), categoria(1L, "Outro nome"));
		assertNotEquals(categoria(1L, "Games"), categoria(2L, "Games"));
	}

	@Test
	public void testEqualsEntidadeNova() {
		Categoria nova = categoria(null, "Games");

		assertEquals(nova, nova);
		assertNotEquals(nova, categoria(null, "Games"));
		assertNotEquals(nova, null);
	}

	@Test
	public void testEqualsClassesDiferentes() {
		Produto produto = new Produto();
		produto.setId(1L);

		assertNotEquals(categoria(1L, "Games"), produto);
		assertNotEquals(categoria(1L, "Games"), "Games");
	}

	@Test
	public void testHashCodeNaoMudaAoGerarId() {
		Set<Categoria> categorias = new HashSet<>();
		Categoria categoria = categoria(null, "Games");
		categorias.add(categoria);

		/* O save gera o id depois que a entidade já está no Set */
		categoria.setId(10L);

		assertTrue(categorias.contains(categoria));
		assertEquals(categoria(10L, "Games").hashCode(), categoria.hashCode());
	}

	@Test
	public void testToStringSemRelacionamentos() {
		Empresa empresa = new Empresa();
		empresa.setId(1L);
		empresa.setLogoMarca("base64-da-logo");
		Categoria categoria = categoria(5L, "Games");
		categoria.setEmpresa(empresa);

		Produto produto = new Produto();
		produto.setId(7L);
		produto.setNome("Teclado");
		produto.setImagem("base64-da-imagem");
		produto.setCategoria(categoria);
		produto.setEmpresa(empresa);

		assertEquals("Categoria(id=5, nome=Games)", categoria.toString());

		String texto = produto.toString();
		assertTrue(texto.startsWith("Produto(id=7, nome=Teclado"), texto);
		assertFalse(texto.contains("categoria"), texto);
		assertFalse(texto.contains("empresa"), texto);
		assertFalse(texto.contains("base64"), "toString não deve trazer imagem: " + texto);
	}

	@Test
	public void testToStringSemDadosSensiveis() {
		Usuario usuario = new Usuario();
		usuario.setId(3L);
		usuario.setLogin("joao");
		usuario.setSenha("$2a$10$hash-da-senha");
		usuario.setTokenSessao("eyJ.token.sessao");
		usuario.setRefreshToken("eyJ.refresh");
		assertEquals("Usuario(id=3, login=joao, liberado=true)", usuario.toString());

		Pessoa pessoa = new Pessoa();
		pessoa.setId(4L);
		pessoa.setNome("João Souza");
		pessoa.setCpf("123.456.789-09");
		pessoa.setCnpj("12.345.678/0001-95");
		pessoa.setEmail("joao@teste.com.br");
		pessoa.setTelefone("44 99999-0000");
		String texto = pessoa.toString();
		assertTrue(texto.contains("João Souza"), texto);
		assertFalse(texto.contains("123.456.789-09"), "toString não pode trazer o CPF");
		assertFalse(texto.contains("12.345.678/0001-95"), "toString não pode trazer o CNPJ");
		assertFalse(texto.contains("joao@teste.com.br"), "toString não pode trazer o e-mail");
		assertFalse(texto.contains("99999"), "toString não pode trazer o telefone");
	}

}
