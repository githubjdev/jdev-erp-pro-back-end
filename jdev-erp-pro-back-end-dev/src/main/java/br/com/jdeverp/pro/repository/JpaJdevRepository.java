package br.com.jdeverp.pro.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/*BaseRepository*/
/*INterface customizada de repository*/
@NoRepositoryBean
public interface JpaJdevRepository<T, ID> extends JpaRepository<T, ID> {

	
	
	
}
