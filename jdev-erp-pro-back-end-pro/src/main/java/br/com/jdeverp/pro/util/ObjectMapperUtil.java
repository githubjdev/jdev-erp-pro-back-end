package br.com.jdeverp.pro.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.tomcat.util.json.JSONParser;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Classe utilitária para manipulação de objetos JSON
 * 
 * @author jdeverp
 *
 */
@Component
public class ObjectMapperUtil extends ObjectMapper {

	private static final long serialVersionUID = 1L;

	private LinkedHashMap<String, Object> parser = new LinkedHashMap<String, Object>();

	public ObjectMapperUtil() {
		super();
		disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
		configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		registerModule(new JavaTimeModule());
		disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	public String objetoParaJson(Object objeto) throws JsonProcessingException {
		return writeValueAsString(objeto);
	}

	public Object lerJsonParaObjeto(String json, Class<?> classeCanonicalName) throws Exception {
		JavaType type = getTypeFactory().constructFromCanonical(classeCanonicalName.getCanonicalName());
		return readValue(json, type);
	}

	public LinkedHashMap<String, Object> jsonParaHashMap(String json) throws Exception {
		parser = new JSONParser(json).parseObject();
		return parser;
	}

	public String jsonHashMapAtributo(String json, String atributo) throws Exception {
		parser = new JSONParser(json).parseObject();
		return valorAtributo(atributo);
	}

	public String valorAtributo(String atributo) throws Exception {
		Object valor = parser.get(atributo);
		return valor != null ? valor.toString().trim() : null;
	}

	public Map<String, Object> jsonForMap(String json) throws Exception {
		return (Map<String, Object>) readValue(json, new TypeReference<HashMap<String, Object>>() {
		});
	}

}
