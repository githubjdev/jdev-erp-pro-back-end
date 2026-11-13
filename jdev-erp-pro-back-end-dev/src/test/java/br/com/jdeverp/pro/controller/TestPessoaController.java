package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;

public class TestPessoaController extends TestContextoApi {

	private Long idPessoa;
	private Long idPessoa2;
	private String nome;
	private String nome2;
	private Map<String, Object> json;

	private long totalNoBanco() {
		return jdbcTemplate.queryForObject("select count(*) from pessoa where empresa_id = ?", Long.class, empresaId);
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		json = pessoaJson("Pessoa API");
		nome = (String) json.get("nome");
		/* A data de cadastro é do back-end: a do request é ignorada (P20) */
		json.put("dataCadastro", "2000-01-01");
		idPessoa = id(post("/api/pessoa/salvar", json)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value(nome))
				.andExpect(jsonPath("$.cpf").value(json.get("cpf")))
				.andExpect(jsonPath("$.tipoPessoa").value("FISICA"))
				.andExpect(jsonPath("$.dataCadastro").value(LocalDate.now().toString()))
				.andExpect(jsonPath("$.empresaId").value(empresaId)));

		Map<String, Object> json2 = pessoaJson("Cadastro Secundario");
		nome2 = (String) json2.get("nome");
		idPessoa2 = id(post("/api/pessoa/salvar", json2).andExpect(status().isOk()));
	}

	@Test
	@Order(2)
	public void testSalvarNomeDuplicado() throws Exception {
		Map<String, Object> duplicada = pessoaJson("Qualquer");
		duplicada.put("nome", nome);
		post("/api/pessoa/salvar", duplicada)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe uma pessoa com o mesmo nome para a empresa logada."));
	}

	@Test
	@Order(3)
	public void testAtualizar() throws Exception {
		json.put("id", idPessoa);
		json.put("cidade", "Curitiba");
		post("/api/pessoa/atualizar", json)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idPessoa))
				.andExpect(jsonPath("$.cidade").value("Curitiba"))
				/* A edição mantém a data do cadastro, mesmo com outra data no request */
				.andExpect(jsonPath("$.dataCadastro").value(LocalDate.now().toString()));

		/* Na edição vale a mesma regra: passar para jurídica exige CNPJ */
		Map<String, Object> juridicaSemCnpj = new LinkedHashMap<>(json);
		juridicaSemCnpj.put("tipoPessoa", "JURIDICA");
		juridicaSemCnpj.put("cnpj", null);
		post("/api/pessoa/atualizar", juridicaSemCnpj)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Informe o CNPJ para pessoa jurídica"));
	}

	@Test
	@Order(2)
	public void testSalvarSemDocumentoObrigatorio() throws Exception {
		/* P13: e-mail para todos, CPF para pessoa física e CNPJ para pessoa jurídica */
		Map<String, Object> fisicaSemCpf = pessoaJson("Fisica sem CPF");
		fisicaSemCpf.put("cpf", "  ");
		post("/api/pessoa/salvar", fisicaSemCpf)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Informe o CPF para pessoa física"));

		Map<String, Object> juridicaSemCnpj = pessoaJson("Juridica sem CNPJ");
		juridicaSemCnpj.put("tipoPessoa", "JURIDICA");
		juridicaSemCnpj.remove("cnpj");
		post("/api/pessoa/salvar", juridicaSemCnpj)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Informe o CNPJ para pessoa jurídica"));

		for (String tipo : new String[] { "FISICA", "JURIDICA" }) {
			Map<String, Object> semEmail = pessoaJson("Sem email " + tipo);
			semEmail.put("tipoPessoa", tipo);
			semEmail.remove("email");
			post("/api/pessoa/salvar", semEmail)
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").value("Informe o e-mail corretamente"));
		}
	}

	@Test
	@Order(2)
	public void testSalvarDocumentoPorTipo() throws Exception {
		/* Depende da migração V20261112_162000__pessoa_documento_por_tipo (antigo V2_PESSOA_DOCUMENTO_POR_TIPO.sql): antes dela, cpf e cnpj eram NOT NULL no banco */
		Map<String, Object> fisica = pessoaJson("Fisica sem CNPJ");
		fisica.put("cnpj", "");
		Long idFisica = id(post("/api/pessoa/salvar", fisica)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cpf").value(fisica.get("cpf")))
				.andExpect(jsonPath("$.cnpj").value(nullValue())));
		/* Em branco é gravado como NULL, senão a próxima pessoa física sem CNPJ bateria na unique_cnpj */
		assertNull(jdbcTemplate.queryForObject("select cnpj from pessoa where id = ?", String.class, idFisica));

		Map<String, Object> outraFisica = pessoaJson("Outra fisica sem CNPJ");
		outraFisica.put("cnpj", null);
		post("/api/pessoa/salvar", outraFisica).andExpect(status().isOk());

		Map<String, Object> juridica = pessoaJson("Juridica sem CPF");
		juridica.put("tipoPessoa", "JURIDICA");
		juridica.remove("cpf");
		post("/api/pessoa/salvar", juridica)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tipoPessoa").value("JURIDICA"))
				.andExpect(jsonPath("$.cnpj").value(juridica.get("cnpj")))
				.andExpect(jsonPath("$.cpf").value(nullValue()));
	}

	@Test
	@Order(4)
	public void testAtualizarPessoaDeOutraEmpresa() throws Exception {
		Map<String, Object> outra = pessoaJson("Outra");
		outra.put("id", admin.pessoaId() + 1_000_000);
		post("/api/pessoa/atualizar", outra)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Pessoa não encontrada para a empresa logada."));
	}

	@Test
	@Order(5)
	public void testListar() throws Exception {
		get("/api/pessoa/listar").andExpect(status().isOk()).andExpect(jsonPath("$", hasSize((int) totalNoBanco())));
		get("/api/pessoa/listarTodos").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(idPessoa.intValue())));
	}

	@Test
	@Order(6)
	public void testBuscaPorNome() throws Exception {
		get("/api/pessoa/buscaPorNome/{nome}", nome2).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(idPessoa2));
		get("/api/pessoa/existePorNome/{nome}", nome).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/pessoa/existePorNomeDiferenteId/{id}/{nome}", idPessoa, nome).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(7)
	public void testBuscarPorId() throws Exception {
		get("/api/pessoa/buscarPorId/{id}", idPessoa).andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value(nome)).andExpect(jsonPath("$.cidade").value("Curitiba"));
		get("/api/pessoa/buscarPorId/0").andExpect(status().isNotFound());
		get("/api/pessoa/existePorId/{id}", idPessoa).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
	}

	@Test
	@Order(8)
	public void testBuscarPorIdsTotalEPaginado() throws Exception {
		post("/api/pessoa/buscarPorIds", ids(idPessoa, idPessoa2)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
		get("/api/pessoa/total").andExpect(status().isOk()).andExpect(jsonPath("$").value(totalNoBanco()));
		/* A pessoa mais nova da empresa, que pode ser de outro teste da classe */
		Long maiorId = jdbcTemplate.queryForObject("select max(id) from pessoa where empresa_id = ?", Long.class, empresaId);
		get("/api/pessoa/paginado?page=0&size=1&sort=id&direction=desc").andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].id").value(maiorId));
	}

	@Test
	@Order(9)
	public void testDeletarTodosPorIds() throws Exception {
		delete("/api/pessoa/deletarTodosPorIds", ids(idPessoa2)).andExpect(status().isNoContent());
		get("/api/pessoa/existePorId/{id}", idPessoa2).andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(10)
	public void testDeletar() throws Exception {
		delete("/api/pessoa/deletar/" + idPessoa).andExpect(status().isOk())
				.andExpect(content().string("Pessoa deletada com sucesso."));
		delete("/api/pessoa/deletar/" + idPessoa).andExpect(status().isBadRequest());
	}

	@Test
	@Order(11)
	public void testDeletarTodosComPessoasEmUso() throws Exception {
		/* A pessoa da empresa e a do administrador estão em uso: o banco recusa e nada é apagado */
		long antes = totalNoBanco();
		delete("/api/pessoa/deletarTodos").andExpect(status().isBadRequest());
		get("/api/pessoa/total").andExpect(jsonPath("$").value(antes));
	}


	@Test
	@Order(100)
	public void testListarPaginado() throws Exception {
		conferirPaginado("/api/pessoa/paginado");
	}

}
