package br.com.jdeverp.pro.repository;

import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.Empresa;

@Repository
public interface EmpresaRepository extends JpaJdevRepository<Empresa, Long> {

}
