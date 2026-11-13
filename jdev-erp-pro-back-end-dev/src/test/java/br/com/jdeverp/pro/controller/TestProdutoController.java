package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;

public class TestProdutoController extends TestContextoApi {

	private Long idCategoria;
	private String nomeCategoria;
	private Long idProduto;
	private Long idProduto2;
	private String nome;

	@BeforeAll
	void criarCategoria() throws Exception {
		nomeCategoria = "Categoria Produto " + sufixo;
		idCategoria = id(post("/api/categoria/salvar", mapa("nome", nomeCategoria)).andExpect(status().isOk()));
		nome = "Produto API " + sufixo;
	}

	private Map<String, Object> produtoJson(Long id, String nome, String sku, Long categoria) {
		return mapa("id", id, "nome", nome, "descricao", "Produto de teste", "imagem", "imagem-base64",
				"preco", 25.9, "estoque", 10, "estoqueMinimo", 2, "sku", sku, "codigoBarra", "789" + sufixo,
				"unidadeMedida", "UNIDADE", "categoria", ref(categoria));
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		idProduto = id(post("/api/produto/salvar", produtoJson(null, nome, "SKU-" + sufixo, idCategoria))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value(nome))
				.andExpect(jsonPath("$.unidadeMedida").value("UNIDADE"))
				.andExpect(jsonPath("$.preco").value(25.9))
				.andExpect(jsonPath("$.categoriaId").value(idCategoria))
				.andExpect(jsonPath("$.categoriaNome").value(nomeCategoria))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));

		idProduto2 = id(post("/api/produto/salvar", produtoJson(null, "Outro " + nome, "SKU2-" + sufixo, idCategoria))
				.andExpect(status().isOk()));
	}

	@Test
	@Order(2)
	public void testSalvarPrecoEEstoqueAbaixoDoMinimo() throws Exception {
		/* No mínimo 1, como os CHECK do banco (P15) */
		Map<String, Object> semPreco = produtoJson(null, "Sem preço " + sufixo, "SKU-P-" + sufixo, idCategoria);
		semPreco.put("preco", 0);
		post("/api/produto/salvar", semPreco)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Preço deve ser de no mínimo R$ 1,00"));

		Map<String, Object> semEstoque = produtoJson(null, "Sem estoque " + sufixo, "SKU-E-" + sufixo, idCategoria);
		semEstoque.put("estoque", 0);
		post("/api/produto/salvar", semEstoque)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Estoque deve ser de no mínimo 1"));

		Map<String, Object> semEstoqueMinimo = produtoJson(null, "Sem mínimo " + sufixo, "SKU-M-" + sufixo, idCategoria);
		semEstoqueMinimo.remove("estoqueMinimo");
		post("/api/produto/salvar", semEstoqueMinimo)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Estoque mínimo deve ser informado"));
	}

	@Test
	@Order(2)
	public void testSalvarNomeDuplicado() throws Exception {
		post("/api/produto/salvar", produtoJson(null, nome, "SKU3-" + sufixo, idCategoria))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe um produto com o mesmo nome para a empresa logada."));
	}

	@Test
	@Order(3)
	public void testSalvarCategoriaDeOutraEmpresa() throws Exception {
		Long outraEmpresa = criarEmpresa();
		executarSql("insert into categoria (id, nome, empresa_id) values (nextval('seq_categoria'), ?, ?)",
				"Categoria externa " + sufixo, outraEmpresa);
		Long categoriaExterna = jdbcTemplate.queryForObject("select id from categoria where empresa_id = ?", Long.class,
				outraEmpresa);

		post("/api/produto/salvar", produtoJson(null, "Produto externo " + sufixo, "SKU4-" + sufixo, categoriaExterna))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Categoria não encontrada para a empresa logada."));
	}

	@Test
	@Order(4)
	public void testAtualizar() throws Exception {
		Map<String, Object> json = produtoJson(idProduto, nome, "SKU-" + sufixo, idCategoria);
		json.put("preco", 30.0);
		post("/api/produto/atualizar", json)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idProduto))
				.andExpect(jsonPath("$.preco").value(30.0))
				.andExpect(jsonPath("$.categoriaNome").value(nomeCategoria));
	}

	@Test
	@Order(5)
	public void testListar() throws Exception {
		get("/api/produto/listar").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		get("/api/produto/listarTodos").andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(idProduto.intValue())));
	}

	@Test
	@Order(6)
	public void testBuscaPorNome() throws Exception {
		get("/api/produto/buscaPorNome/{nome}", "Outro " + nome)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(idProduto2));
	}

	@Test
	@Order(7)
	public void testExistePorNome() throws Exception {
		get("/api/produto/existePorNome/{nome}", nome).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/produto/existePorNomeDiferenteId/{id}/{nome}", idProduto2, "Outro " + nome)
				.andExpect(status().isOk()).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(8)
	public void testBuscarPorId() throws Exception {
		get("/api/produto/buscarPorId/{id}", idProduto)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value(nome))
				.andExpect(jsonPath("$.categoriaId").value(idCategoria));

		get("/api/produto/buscarPorId/0").andExpect(status().isNotFound());
	}

	@Test
	@Order(9)
	public void testExistePorIdEBuscarPorIds() throws Exception {
		get("/api/produto/existePorId/{id}", idProduto).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		post("/api/produto/buscarPorIds", ids(idProduto, idProduto2))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));
	}

	@Test
	@Order(10)
	public void testTotalEPaginado() throws Exception {
		get("/api/produto/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(2));
		get("/api/produto/paginado?page=0&size=10&sort=nome&direction=asc")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.content[0].id").value(idProduto2));
	}

	@Test
	@Order(11)
	public void testDeletarTodosPorIds() throws Exception {
		delete("/api/produto/deletarTodosPorIds", ids(idProduto2)).andExpect(status().isNoContent());
		get("/api/produto/existePorId/{id}", idProduto2).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(12)
	public void testDeletar() throws Exception {
		delete("/api/produto/deletar/" + idProduto)
				.andExpect(status().isOk())
				.andExpect(content().string("Produto deletado com sucesso."));
		delete("/api/produto/deletar/" + idProduto).andExpect(status().isBadRequest());
	}

	@Test
	@Order(13)
	public void testDeletarTodos() throws Exception {
		post("/api/produto/salvar", produtoJson(null, "Terceiro " + nome, "SKU5-" + sufixo, idCategoria))
				.andExpect(status().isOk());

		delete("/api/produto/deletarTodos").andExpect(status().isOk()).andExpect(jsonPath("$").value(1));
		delete("/api/produto/deletarTodos").andExpect(status().isBadRequest());
	}


	@Test
	@Order(100)
	public void testListarPaginado() throws Exception {
		conferirPaginado("/api/produto/paginado");
	}

}
