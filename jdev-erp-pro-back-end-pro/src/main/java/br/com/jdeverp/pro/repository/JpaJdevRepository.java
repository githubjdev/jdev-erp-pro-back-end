package br.com.jdeverp.pro.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface JpaJdevRepository<T, ID> extends JpaRepository<T, ID> {

	/* Mais -> Nossos métodos customizados e genéricos */

	Page<T> listarPaginado(Long empresaId, Pageable pageable);

	long total(Long empresaId);

}
