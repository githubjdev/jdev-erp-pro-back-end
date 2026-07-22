package br.com.jdeverp.pro.app;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableAsync
@EnableTransactionManagement
@EntityScan(basePackages = "br.com.jdeverp.pro.model")
@EnableJpaRepositories(basePackages = "br.com.jdeverp.pro.repository")
@ComponentScan(basePackages = "br.com.jdeverp.pro")
public class JDevERPPROApplication {

	void main(String[] args) {
		SpringApplication app = new SpringApplication(JDevERPPROApplication.class);
		app.run(args);

	}

	@Bean(name = "cacheManager")
	public CacheManager cacheManager() {
		ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("cacheJDevERPPRO");
		return cacheManager;
	}

	@PostConstruct
	private void configTimeZone() {
		Locale.setDefault(Locale.forLanguageTag("pt_BR"));
		TimeZone sp = TimeZone.getTimeZone("America/Sao_Paulo");
		TimeZone.setDefault(sp);
		Calendar.getInstance().setTimeZone(sp);
	}

}
