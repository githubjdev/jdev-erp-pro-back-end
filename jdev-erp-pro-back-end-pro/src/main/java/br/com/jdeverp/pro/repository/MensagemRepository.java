package br.com.jdeverp.pro.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.jdeverp.pro.model.Mensagem;
import jakarta.transaction.Transactional;

@Repository
public interface MensagemRepository extends JpaJdevRepository<Mensagem, Long> {

	/*
	 * Busca todas as mensagens da empresa passada como parametro
	 */
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa")
	List<Mensagem> findAll(@Param("idEmpresa") Long idEmpresa);

	
	/*Busca as mensagens por partes ou conteudo completo passado por parametro e da empresa passada por parametro*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa "
								+ " and unaccent(upper(trim(m.conteudo))) "
								+ " like unaccent(upper(concat('%', trim(:conteudo) ,'%')))")
	List<Mensagem> buscaPorConteudo(@Param("conteudo") String conteudo, @Param("idEmpresa") Long idEmpresa);
	
	
	/*Retorna true se já existir mensagem com o mesmo conteudo para a mesma empresa, no caso não podemos deixar salvar para não ficar repetido no banco de dados*/
	@Query("select count(m.id) > 0 from Mensagem m where m.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(m.conteudo))) "
			+ " = unaccent(upper(trim(:conteudo)))")
	boolean existePorConteudo(@Param("conteudo") String conteudo, @Param("idEmpresa") Long idEmpresa);
	
	/*Verifica se existe outra mensagem no banco de dados com o mesmo conteudo mas ID diferentes da que está tentando atualizar*/
	@Query("select count(m.id) > 0 from Mensagem m where m.empresa.id = :idEmpresa "
			+ " and unaccent(upper(trim(m.conteudo))) "
			+ " = unaccent(upper(trim(:conteudo))) and m.id <> :id")
    boolean existePorConteudoDiferenteId(@Param("id") Long id, @Param("conteudo") String conteudo, @Param("idEmpresa") Long idEmpresa);	
	
	/*Delete de uma mensagem de uma determinada empresa*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Mensagem m where m.empresa.id = :idEmpresa and m.id = :id")
	void deleteById(@Param("id") Long id, @Param("idEmpresa") Long idEmpresa);

	// ====================Métodos específicos para Chamado====================
	
	/*Busca todas as mensagens de um determinado chamado da empresa*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado")
	List<Mensagem> findAllByChamado(@Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	/*Busca mensagens de um chamado por conteúdo*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado "
									+ " and unaccent(upper(trim(m.conteudo))) "
									+ " like unaccent(upper(concat('%', trim(:conteudo) ,'%')))")
	List<Mensagem> buscaPorConteudoByChamado(@Param("conteudo") String conteudo, @Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	/*Retorna true se já existir mensagem com o mesmo conteúdo para a mesma empresa e chamado*/
	@Query("select count(m.id) > 0 from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado "
			+ " and unaccent(upper(trim(m.conteudo))) "
			+ " = unaccent(upper(trim(:conteudo)))")
	boolean existePorConteudoByChamado(@Param("conteudo") String conteudo, @Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	/*Verifica se existe outra mensagem no mesmo chamado com o mesmo conteúdo mas ID diferentes da que está tentando atualizar*/
	@Query("select count(m.id) > 0 from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado "
			+ " and unaccent(upper(trim(m.conteudo))) "
			+ " = unaccent(upper(trim(:conteudo))) and m.id <> :id")
	boolean existePorConteudoDiferenteIdByChamado(@Param("id") Long id, @Param("conteudo") String conteudo, @Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	/*Conta quantas mensagens tem um determinado chamado*/
	@Query("select count(m.id) from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado")
	long countByChamado(@Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	/*Delete de todas as mensagens de um determinado chamado da empresa*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado")
	void deleteAllByChamado(@Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	/*Delete de uma mensagem específica de um determinado chamado da empresa*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado and m.id = :id")
	void deleteByIdAndChamado(@Param("id") Long id, @Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	// ====================Métodos para Status de Leitura====================

	/*Busca todas as mensagens não lidas da empresa*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.lida = false")
	List<Mensagem> findAllNaoLidas(@Param("idEmpresa") Long idEmpresa);

	/*Busca todas as mensagens não lidas de um determinado chamado*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado and m.lida = false")
	List<Mensagem> findAllNaoLidasByChamado(@Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	/*Conta quantas mensagens não lidas tem um determinado chamado*/
	@Query("select count(m.id) from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado and m.lida = false")
	long countNaoLidasByChamado(@Param("idChamado") Long idChamado, @Param("idEmpresa") Long idEmpresa);

	/*Atualiza o status de lida de uma mensagem*/
	@Transactional
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Mensagem m set m.lida = :lida where m.id = :id and m.empresa.id = :idEmpresa")
	void updateLida(@Param("id") Long id, @Param("lida") Boolean lida, @Param("idEmpresa") Long idEmpresa);

	// ====================Métodos para Atendente====================

	/*Busca todas as mensagens de um determinado atendente da empresa*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.atendente.id = :idAtendente")
	List<Mensagem> findAllByAtendente(@Param("idAtendente") Long idAtendente, @Param("idEmpresa") Long idEmpresa);

	/*Conta quantas mensagens um determinado atendente tem*/
	@Query("select count(m.id) from Mensagem m where m.empresa.id = :idEmpresa and m.atendente.id = :idAtendente")
	long countByAtendente(@Param("idAtendente") Long idAtendente, @Param("idEmpresa") Long idEmpresa);

	// ====================Métodos para Cliente====================

	/*Busca todas as mensagens de um determinado cliente da empresa*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.cliente.id = :idCliente")
	List<Mensagem> findAllByCliente(@Param("idCliente") Long idCliente, @Param("idEmpresa") Long idEmpresa);

	/*Conta quantas mensagens um determinado cliente tem*/
	@Query("select count(m.id) from Mensagem m where m.empresa.id = :idEmpresa and m.cliente.id = :idCliente")
	long countByCliente(@Param("idCliente") Long idCliente, @Param("idEmpresa") Long idEmpresa);

	// ====================Métodos Combinados====================

	/*Busca mensagens de um chamado por um atendente específico*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.chamado.id = :idChamado and m.atendente.id = :idAtendente")
	List<Mensagem> findAllByChamadoAndAtendente(@Param("idChamado") Long idChamado, @Param("idAtendente") Long idAtendente, @Param("idEmpresa") Long idEmpresa);

	/*Busca mensagens não lidas de um atendente*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.atendente.id = :idAtendente and m.lida = false")
	List<Mensagem> findAllNaoLidasByAtendente(@Param("idAtendente") Long idAtendente, @Param("idEmpresa") Long idEmpresa);

	/*Busca mensagens não lidas de um cliente*/
	@Query("select m from Mensagem m where m.empresa.id = :idEmpresa and m.cliente.id = :idCliente and m.lida = false")
	List<Mensagem> findAllNaoLidasByCliente(@Param("idCliente") Long idCliente, @Param("idEmpresa") Long idEmpresa);

}