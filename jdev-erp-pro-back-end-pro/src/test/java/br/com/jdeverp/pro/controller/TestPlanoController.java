package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.contexto.TestContextoApi;

/** Plano é global: o teste cria um plano próprio e apaga no final. */
public class TestPlanoController extends TestContextoApi {

	private static final String MSG_FREE = "Já existe o plano FREE: ele é cadastrado direto no banco e não pode haver outro.";

	private Long idPlano;
	private String nome;

	@AfterAll
	void apagarPlano() {
		if (idPlano != null) {
			executarSql("delete from plano where id = ?", idPlano);
		}
	}

	private Map<String, Object> planoJson(Long id, String nome, double valor) {
		return mapa("id", id, "nome", nome, "descricao", "Plano de teste", "ativo", true, "valorMensal", valor,
				"limiteUsuario", 10, "limiteCliente", 20, "tipoPlano", "PRO");
	}

	@Test
	@Order(1)
	public void testSalvar() throws Exception {
		nome = "Plano Teste API " + sufixo;
		idPlano = id(post("/api/plano/salvar", planoJson(null, nome, 99.9))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value(nome))
				.andExpect(jsonPath("$.valorMensal").value(99.9))
				.andExpect(jsonPath("$.tipoPlano").value("PRO")));
	}

	@Test
	@Order(2)
	public void testSalvarNomeDuplicadoEValorInvalido() throws Exception {
		post("/api/plano/salvar", planoJson(null, nome, 99.9))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe um plano com o mesmo nome: " + nome));

		/* Plano pago: de R$ 49 a R$ 200, como o CHECK do banco */
		post("/api/plano/salvar", planoJson(null, "Outro " + nome, 10))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("O valor mensal do plano deve ficar entre R$ 49 e R$ 200."));
	}

	@Test
	@Order(3)
	public void testPlanoFree() throws Exception {
		/* O FREE é inserido direto no banco: a API não cadastra outro nem transforma outro plano em FREE (P11) */
		Map<String, Object> free = planoJson(null, "Outro Free " + sufixo, 0);
		free.put("tipoPlano", "FREE");
		post("/api/plano/salvar", free)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(MSG_FREE));

		Map<String, Object> virarFree = planoJson(idPlano, nome, 0);
		virarFree.put("tipoPlano", "FREE");
		post("/api/plano/atualizar", virarFree)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(MSG_FREE));
	}

	@Test
	@Order(4)
	public void testAtualizar() throws Exception {
		post("/api/plano/atualizar", planoJson(idPlano, nome, 120))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idPlano))
				.andExpect(jsonPath("$.valorMensal").value(120.0));
	}

	@Test
	@Order(5)
	public void testListarEBuscas() throws Exception {
		get("/api/plano/listar").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(idPlano.intValue())));
		get("/api/plano/buscarPorId/{id}", idPlano).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value(nome));
		get("/api/plano/buscarPorId/0").andExpect(status().isNotFound());
		get("/api/plano/buscaPorNome/{nome}", nome).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(idPlano));
		get("/api/plano/existePorNome/{nome}", nome).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/plano/existePorNomeDiferenteId/{id}/{nome}", idPlano, nome).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(6)
	public void testDeletar() throws Exception {
		delete("/api/plano/deletar/" + idPlano).andExpect(status().isOk())
				.andExpect(content().string("Plano deletado com sucesso."));
		delete("/api/plano/deletar/" + idPlano).andExpect(status().isBadRequest());
		idPlano = null;
	}

}
