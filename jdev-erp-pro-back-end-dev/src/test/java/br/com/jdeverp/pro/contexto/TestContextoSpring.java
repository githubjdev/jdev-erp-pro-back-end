package br.com.jdeverp.pro.contexto;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.app.JDevERPPROApplication;

@TestMethodOrder(OrderAnnotation.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(locations = "classpath:application.properties")
@SpringBootTest(classes = JDevERPPROApplication.class)
@Transactional /*Se remover o @Transaction ele commita/salva no banco senão ele reverte as operações*/
public class TestContextoSpring {

	@Test
	public void testeInicial() {
		System.out.println("Teste funcionando");
	}

}
