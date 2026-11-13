package br.com.jdeverp.pro.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.com.jdeverp.pro.dto.LoginDTO;
import br.com.jdeverp.pro.dto.PessoaDTO;

/** ObjectMapper do projeto (Jackson 2): datas ISO, propriedade desconhecida ignorada e leitura de JSON para mapa. */
public class TestObjectMapperUtil {

	private final ObjectMapperUtil objectMapperUtil = new ObjectMapperUtil();

	@Test
	public void testObjetoParaJson() throws Exception {
		PessoaDTO pessoa = new PessoaDTO();
		pessoa.setNome("Maria");
		pessoa.setDataCadastro(LocalDate.of(2026, 11, 12));

		String json = objectMapperUtil.objetoParaJson(pessoa);

		/* Data como texto ISO, não como timestamp */
		assertTrue(json.contains("\"dataCadastro\":\"2026-11-12\""), json);
		assertTrue(json.contains("\"nome\":\"Maria\""), json);
	}

	@Test
	public void testLerJsonParaObjeto() throws Exception {
		Object objeto = objectMapperUtil.lerJsonParaObjeto("{\"login\":\"maria\",\"senha\":\"123\",\"campoDesconhecido\":1}",
				LoginDTO.class);

		/* Propriedade que não existe na classe é ignorada */
		LoginDTO login = assertInstanceOf(LoginDTO.class, objeto);
		assertEquals("maria", login.getLogin());
		assertEquals("123", login.getSenha());
	}

	@Test
	public void testJsonParaHashMap() throws Exception {
		LinkedHashMap<String, Object> mapa = objectMapperUtil.jsonParaHashMap("{\"b\":2,\"a\":\"texto\"}");

		/* Mantém a ordem do JSON */
		assertEquals("[b, a]", mapa.keySet().toString());
		assertEquals("texto", mapa.get("a"));
	}

	@Test
	public void testJsonHashMapAtributo() throws Exception {
		assertEquals("valor com espaço", objectMapperUtil.jsonHashMapAtributo("{\"nome\":\"  valor com espaço  \"}", "nome"));
		assertNull(objectMapperUtil.jsonHashMapAtributo("{\"nome\":\"x\"}", "outro"));
	}

	@Test
	public void testValorAtributo() throws Exception {
		objectMapperUtil.jsonParaHashMap("{\"id\":10,\"ativo\":true}");

		/* Lê do último JSON lido pela instância */
		assertEquals("10", objectMapperUtil.valorAtributo("id"));
		assertEquals("true", objectMapperUtil.valorAtributo("ativo"));
		assertNull(objectMapperUtil.valorAtributo("nada"));
	}

	@Test
	public void testJsonForMap() throws Exception {
		Map<String, Object> mapa = objectMapperUtil.jsonForMap("{\"id\":10,\"itens\":[1,2]}");

		assertEquals(10, mapa.get("id"));
		assertEquals(2, ((java.util.List<?>) mapa.get("itens")).size());
	}

}
