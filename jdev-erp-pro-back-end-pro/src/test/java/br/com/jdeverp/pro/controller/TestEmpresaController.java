package br.com.jdeverp.pro.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import br.com.jdeverp.pro.contexto.TestContextoApi;
import br.com.jdeverp.pro.enums.TipoPessoa;
import br.com.jdeverp.pro.model.Pessoa;
import br.com.jdeverp.pro.model.Plano;
import br.com.jdeverp.pro.security.JwtAccessDeniedHandler;

public class TestEmpresaController extends TestContextoApi {

	private Pessoa pessoa;
	private Plano plano;
	private Long idEmpresaNova;

	@BeforeAll
	void criarPessoa() {
		pessoa = pessoaRepository.save(novaPessoa("Nova Empresa", TipoPessoa.JURIDICA,
				empresaRepository.findById(empresaId).orElseThrow()));
		plano = planoRepository.findAll().get(0);
	}

	private Map<String, Object> empresaJson(Long id, Long pessoaId, String logo) {
		return mapa("id", id, "plano", ref(plano.getId()), "pessoa", ref(pessoaId), "logoMarca", logo,
				"planoAtivo", true, "bloqueio", false, "totalUsuario", 3, "totalCliente", 7,
				"vigenciaPlano", LocalDate.now().plusYears(1).toString());
	}

	@Test
	@Order(1)
	public void testSalvarSemSerAdministrador() throws Exception {
		UsuarioTeste comum = criarUsuario("Comum", false);
		comToken(logar(comum.login(), SENHA), MockMvcRequestBuilders.post("/api/empresa/salvar"),
				empresaJson(null, pessoa.getId(), "logo"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value(JwtAccessDeniedHandler.MSG_SEM_PERMISSAO));
	}

	@Test
	@Order(2)
	public void testSalvar() throws Exception {
		idEmpresaNova = id(post("/api/empresa/salvar", empresaJson(null, pessoa.getId(), "logo-nova"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.logoMarca").value("logo-nova"))
				.andExpect(jsonPath("$.totalCliente").value(7))
				.andExpect(jsonPath("$.planoId").value(plano.getId()))
				.andExpect(jsonPath("$.planoNome").value(plano.getNome()))
				.andExpect(jsonPath("$.pessoaId").value(pessoa.getId()))
				.andExpect(jsonPath("$.pessoaNome").value(pessoa.getNome())));
	}

	@Test
	@Order(3)
	public void testSalvarPessoaComEmpresa() throws Exception {
		post("/api/empresa/salvar", empresaJson(null, pessoa.getId(), "logo"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Já existe uma empresa cadastrada para a pessoa: " + pessoa.getNome()));
	}

	@Test
	@Order(4)
	public void testAtualizar() throws Exception {
		post("/api/empresa/atualizar", empresaJson(idEmpresaNova, pessoa.getId(), "logo-editada"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(idEmpresaNova))
				.andExpect(jsonPath("$.logoMarca").value("logo-editada"))
				.andExpect(jsonPath("$.pessoaId").value(pessoa.getId()));
	}

	@Test
	@Order(5)
	public void testBuscarPorId() throws Exception {
		get("/api/empresa/buscarPorId/{id}", idEmpresaNova).andExpect(status().isOk())
				.andExpect(jsonPath("$.pessoaNome").value(pessoa.getNome()))
				.andExpect(jsonPath("$.vigenciaPlano").value(LocalDate.now().plusYears(1).toString()));
		get("/api/empresa/buscarPorId/0").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Empresa com id: 0 não foi encontrada."));
	}

	@Test
	@Order(6)
	public void testListarEBuscas() throws Exception {
		get("/api/empresa/listar").andExpect(status().isOk()).andExpect(jsonPath("$[*].id", hasItem(idEmpresaNova.intValue())));
		get("/api/empresa/buscaPorNome/{nome}", pessoa.getNome()).andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(idEmpresaNova));
		get("/api/empresa/existePorNome/{nome}", pessoa.getNome()).andExpect(status().isOk()).andExpect(jsonPath("$").value(true));
		get("/api/empresa/existePorNomeDiferenteId/{id}/{nome}", idEmpresaNova, pessoa.getNome()).andExpect(status().isOk())
				.andExpect(jsonPath("$").value(false));
	}

	@Test
	@Order(7)
	public void testDeletar() throws Exception {
		delete("/api/empresa/deletar/" + idEmpresaNova).andExpect(status().isOk())
				.andExpect(content().string("Empresa deletada com sucesso."));
		delete("/api/empresa/deletar/" + idEmpresaNova).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Empresa não encontrada ou já foi deletada."));
	}

	/** P25: o admin logado não apaga a própria empresa. */
	@Test
	@Order(8)
	public void testDeletarPropriaEmpresa() throws Exception {
		delete("/api/empresa/deletar/" + empresaId).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Você não pode apagar a sua própria empresa: você e os usuários dela "
						+ "perderiam o acesso ao sistema. Se ela precisar mesmo ser apagada, peça a um administrador de outra empresa."));

		get("/api/empresa/buscarPorId/{id}", empresaId).andExpect(status().isOk());
	}

	/** P25: o admin logado não bloqueia a própria empresa. */
	@Test
	@Order(9)
	public void testBloquearPropriaEmpresa() throws Exception {
		Long pessoaDaEmpresa = jdbcTemplate.queryForObject("select pessoa_id from empresa where id = ?", Long.class, empresaId);
		Map<String, Object> bloqueio = empresaJson(empresaId, pessoaDaEmpresa, "logo");
		bloqueio.put("bloqueio", true);

		post("/api/empresa/atualizar", bloqueio).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Você não pode bloquear a sua própria empresa: você e os usuários dela "
						+ "perderiam o acesso ao sistema. Se ela precisar mesmo ser bloqueada, peça a um administrador de outra empresa."));

		/* Continua desbloqueada e com acesso */
		assertEquals(Boolean.FALSE, jdbcTemplate.queryForObject("select bloqueio from empresa where id = ?", Boolean.class, empresaId));
		get("/api/empresa/listar").andExpect(status().isOk());

		/* bloqueio nulo vira false */
		bloqueio.put("bloqueio", null);
		post("/api/empresa/atualizar", bloqueio).andExpect(status().isOk()).andExpect(jsonPath("$.bloqueio").value(false));
		assertEquals(Boolean.FALSE, jdbcTemplate.queryForObject("select bloqueio from empresa where id = ?", Boolean.class, empresaId));
	}

}
