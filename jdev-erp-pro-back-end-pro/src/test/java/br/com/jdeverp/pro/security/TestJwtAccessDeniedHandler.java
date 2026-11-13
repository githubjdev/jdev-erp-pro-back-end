package br.com.jdeverp.pro.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.jdeverp.pro.util.ObjectMapperUtil;

/** 403 do filtro de segurança: JSON no formato do ResponseApi, com a mesma mensagem do @PreAuthorize. */
public class TestJwtAccessDeniedHandler {

	@Test
	public void testHandle() throws Exception {
		JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler();
		ObjectMapperUtil objectMapperUtil = new ObjectMapperUtil();
		ReflectionTestUtils.setField(handler, "objectMapperUtil", objectMapperUtil);
		MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/jdev-erp-pro-back-end-pro/api/plano/deletar/1");
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.handle(request, response, new AccessDeniedException("Access Denied"));

		assertEquals(403, response.getStatus());
		assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON_VALUE), response.getContentType());
		assertEquals("UTF-8", response.getCharacterEncoding());
		var corpo = objectMapperUtil.jsonForMap(response.getContentAsString());
		assertEquals(403, corpo.get("status"));
		assertEquals("Forbidden", corpo.get("error"));
		assertEquals(JwtAccessDeniedHandler.MSG_SEM_PERMISSAO, corpo.get("message"));
		assertEquals("/jdev-erp-pro-back-end-pro/api/plano/deletar/1", corpo.get("path"));
	}

}
