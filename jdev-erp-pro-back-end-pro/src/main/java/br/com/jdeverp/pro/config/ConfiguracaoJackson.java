package br.com.jdeverp.pro.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.cfg.EnumFeature;

/**
 * Enum no JSON da API sempre pelo nome (ex.: CARTAO_CREDITO), o mesmo valor
 * gravado no banco (EnumType.STRING).
 *
 * O Jackson 3 (Spring Boot 4) lê e escreve enum pelo toString() por padrão.
 * Vários enums do projeto sobrescrevem o toString() com a descrição
 * ("Cartão de Crédito"), então a API passava a exigir a descrição ao salvar e
 * a devolver a descrição nas consultas.
 */
@Configuration
public class ConfiguracaoJackson {

	@Bean
	public JsonMapperBuilderCustomizer enumPeloNome() {
		return builder -> builder.disable(EnumFeature.READ_ENUMS_USING_TO_STRING,
				EnumFeature.WRITE_ENUMS_USING_TO_STRING);
	}

}
