package br.com.jdeverp.pro.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/*Classe de teste de variével de ambiente de Profile*/
@Component
@Profile("dev")
public class ConfiguracaoProducao {

	public ConfiguracaoProducao() {
		System.out.println("Executando no ambiente dev");
	}

}
