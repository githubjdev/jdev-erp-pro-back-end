package br.com.jdeverp.pro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface JpaJdevRepository<T, ID> extends JpaRepository<T, ID> {

	
	/*Mais -> Nossos métodos customizados*/
	
}
