package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.anotacoes.IgnoreEmpresaId;
import br.com.jdeverp.pro.model.Empresa;

@IgnoreEmpresaId
@Repository
public interface EmpresaRepository extends JpaJdevRepository<Empresa, Long> {
	
	@Query("select c from Empresa c ")
	List<Empresa> findAll();

}
