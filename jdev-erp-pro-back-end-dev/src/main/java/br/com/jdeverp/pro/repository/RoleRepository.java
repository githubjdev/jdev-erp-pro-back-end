package br.com.jdeverp.pro.repository;

import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.anotacoes.IgnoreEmpresaId;
import br.com.jdeverp.pro.model.Role;

@IgnoreEmpresaId
@Repository
public interface RoleRepository  extends JpaJdevRepository<Role, Long>{

}
