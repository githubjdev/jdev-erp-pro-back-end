package br.com.jdeverp.pro.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.Plano;
import br.com.jdeverp.pro.repository.PlanoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class PlanoService {

	@Autowired /* Injeção de dependência */
	private PlanoRepository planoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	public List<Plano> findAll() {
		
		return planoRepository.findAll();
	}

	public List<Plano> buscaPorNome(String nome) {
		return planoRepository.buscaPorNome(nome);
	}

	public boolean existePorNome(String nome) {
		return planoRepository.existePorNome(nome);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome) {
		return planoRepository.existePorNomeDiferenteId(id, nome);
	}

	public void deleteById(Long id) {
		planoRepository.deleteById(id);
	}



	// ====================dentro dos métodos do
	// service===============================

	// Verificar se está em uso
	// Realizar um consulta com integração para saber se pode deletar
	// Fazer copia e backup
	// Fazer inumeras validações de regra de negocio
	// Fazer validações
	// Lançar exeções
	// Escrever regras de negócio

}
