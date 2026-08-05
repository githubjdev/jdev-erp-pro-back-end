package br.com.jdeverp.pro.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.model.Empresa;
import br.com.jdeverp.pro.repository.EmpresaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
public class EmpresaService {

	@Autowired /* Injeção de dependência */
	private EmpresaRepository empresaRepository;

	/*
	 * Posso escrever query customizadas e dinâmicas, mais complexas do que no
	 * Repository
	 */
	@PersistenceContext
	private EntityManager entityManager;

	public Empresa buscaPorId(Long id) {
		return empresaRepository.buscarPorId(id);
	}

	public List<Empresa> findAll() {

		return empresaRepository.findAll();
	}

	public List<Empresa> buscaPorNome(String nome) {
		return empresaRepository.buscaPorNome(nome);
	}

	public boolean existePorNome(String nome) {
		return empresaRepository.existePorNome(nome);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome) {
		return empresaRepository.existePorNomeDiferenteId(id, nome);
	}

	public void deleteById(Long id) {
		empresaRepository.deleteById(id);

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
