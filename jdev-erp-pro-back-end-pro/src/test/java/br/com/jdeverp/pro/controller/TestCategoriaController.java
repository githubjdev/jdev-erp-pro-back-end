package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;

public class TestCategoriaController extends TestContextoApi {

	private Long idCategoria;
	private String nome;

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		nome = "Categoria API " + sufixo;
		idCategoria = id(post("/api/categoria/salvar", mapa("nome", nome))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value(nome))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));
	}

	@Test
	@Order(2)
	public void testSalvarNomeDuplicado() throws Exception {
		post("/api/categoria/salvar", mapa("nome", nome))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe uma categoria com o mesmo nome para a empresa logada."));
	}

	@Test
	@Order(3)
	public void testAtualizar() throws Exception {
		nome = nome + " Editada";
		post("/api/categoria/atualizar", mapa("id", idCategoria, "nome", nome))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idCategoria))
				.andExpect(jsonPath("$.nome").value(nome));
	}

	@Test
	@Order(4)
	public void testAtualizarCategoriaDeOutraEmpresa() throws Exception {
		Long outraEmpresa = criarEmpresa();
		executarSql("insert into categoria (id, nome, empresa_id) values (nextval('seq_categoria'), ?, ?)",
				"Categoria de outra empresa " + sufixo, outraEmpresa);
		Long idOutra = jdbcTemplate.queryForObject("select id from categoria where empresa_id = ?", Long.class, outraEmpresa);

		post("/api/categoria/atualizar", mapa("id", idOutra, "nome", "Invasão " + sufixo))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Categoria não encontrada para a empresa logada."));

		Map<String, Object> linha = jdbcTemplate.queryForMap("select nome, empresa_id from categoria where id = ?", idOutra);
		assertEquals("Categoria de outra empresa " + sufixo, linha.get("nome"));
		assertEquals(outraEmpresa, ((Number) linha.get("empresa_id")).longValue());
	}

	@Test
	@Order(5)
	public void testListar() throws Exception {
		get("/api/categoria/listar")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(idCategoria.intValue())));
	}

	@Test
	@Order(6)
	public void testBuscaPorNome() throws Exception {
		get("/api/categoria/buscaPorNome/{nome}", nome)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(idCategoria));
	}

	@Test
	@Order(7)
	public void testBuscarPorId() throws Exception {
		get("/api/categoria/buscarPorId/" + idCategoria)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value(nome))
				.andExpect(jsonPath("$.empresaId").value(empresaId));
	}

	@Test
	@Order(8)
	public void testBuscarPorIdInexistente() throws Exception {
		get("/api/categoria/buscarPorId/0")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Categoria não encontrada para a empresa logada."));
	}

	@Test
	@Order(9)
	public void testExistePorNome() throws Exception {
		get("/api/categoria/existePorNome/{nome}", nome).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(10)
	public void testExistePorNomeDiferenteId() throws Exception {
		get("/api/categoria/existePorNomeDiferenteId/{id}/{nome}", idCategoria, nome)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(11)
	public void testExistePorId() throws Exception {
		get("/api/categoria/existePorId/" + idCategoria).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(12)
	public void testTotal() throws Exception {
		get("/api/categoria/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
	}

	@Test
	@Order(13)
	public void testListarPaginado() throws Exception {
		get("/api/categoria/paginado?page=0&size=5&sort=id&direction=desc")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(idCategoria))
				.andExpect(jsonPath("$.content[0].nome").value(nome));
	}

	@Test
	@Order(14)
	public void testDeletar() throws Exception {
		delete("/api/categoria/deletar/" + idCategoria)
				.andExpect(status().isOk())
				.andExpect(content().string("Categoria deletada com sucesso."));

		delete("/api/categoria/deletar/" + idCategoria)
				.andExpect(status().isBadRequest());
	}

}
