package br.com.jdeverp.pro.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.anotacoes.IgnoreEmpresaId;
import br.com.jdeverp.pro.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaJdevRepository<Usuario, Long> {

	@IgnoreEmpresaId(ignorar = true, motivo = "Usado pra login então a empresa é estabelecida depois do login do usuário")
	@Query("select u from Usuario u where u.login = :login")
	Usuario buscaPorLogin(@Param("login") String login);

}
