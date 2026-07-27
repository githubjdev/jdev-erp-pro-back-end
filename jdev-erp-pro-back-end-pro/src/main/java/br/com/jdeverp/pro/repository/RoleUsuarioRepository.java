package br.com.jdeverp.pro.repository;

import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.anotacoes.IgnoreEmpresaId;
import br.com.jdeverp.pro.model.RoleUsuario;

@IgnoreEmpresaId
@Repository
public interface RoleUsuarioRepository extends JpaJdevRepository<RoleUsuario, Long> {

}
