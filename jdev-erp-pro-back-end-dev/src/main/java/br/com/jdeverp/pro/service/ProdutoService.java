package br.com.jdeverp.pro.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.jdeverp.pro.dto.ProdutoDTO;
import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.model.Produto;
import br.com.jdeverp.pro.repository.CategoriaRepository;
import br.com.jdeverp.pro.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/*O QUE É O SERVICE
 * Dentro do service vc pode criar infinitos métodos, gerar grafico, api de pagamento, gerar relatorio e etc*/

@Service
@Transactional(readOnly = true)
public class ProdutoService {

	@Autowired /* Injeção de dependência */
	private ProdutoRepository produtoRepository;

	/*Posso escrever query customizadas e dinâmicas, mais complexas do que no Repository*/
	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private UsuarioLogadoService usuarioLogadoService;

	@Transactional
	public ProdutoDTO salvar(Produto produto) {
		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (produtoRepository.existePorNome(produto.getNome(), empresaId)) {
			throw new MsgApiException("Já existe um produto com o mesmo nome para a empresa logada.");
		}

		produto.setCategoria(validarCategoria(produto.getCategoria(), empresaId));
		produto.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return converterParaDto(produtoRepository.saveAndFlush(produto));
	}

	@Transactional
	public ProdutoDTO atualizar(Produto produto) {
		if (produto.getId() == null) {
			throw new MsgApiException("Id do produto não informado para atualizar.");
		}

		Long empresaId = usuarioLogadoService.getEmpresaIdLogada();
		if (!produtoRepository.buscarPorId(produto.getId(), empresaId).isPresent()) {
			throw new MsgApiException("Produto não encontrado para a empresa logada.");
		}

		if (produtoRepository.existePorNomeDiferenteId(produto.getId(), produto.getNome(), empresaId)) {
			throw new MsgApiException("Já existe outro produto com o mesmo nome para a empresa logada.");
		}

		produto.setCategoria(validarCategoria(produto.getCategoria(), empresaId));
		produto.setEmpresa(usuarioLogadoService.getEmpresaLogada());
		return converterParaDto(produtoRepository.saveAndFlush(produto));
	}

	private Categoria validarCategoria(Categoria categoria, Long empresaId) {
		if (categoria == null || categoria.getId() == null) {
			throw new MsgApiException("Categoria deve ser informada para o produto.");
		}

		return categoriaRepository.buscarPorId(categoria.getId(), empresaId)
				.orElseThrow(() -> new MsgApiException("Categoria não encontrada para a empresa logada."));
	}

	public List<ProdutoDTO> findAll(Long idEmpresa) {

		return converterListaParaDto(produtoRepository.findAll(idEmpresa));
	}

	public List<ProdutoDTO> buscaPorNome(String nome, Long idEmpresa) {
		return converterListaParaDto(produtoRepository.buscaPorNome(nome, idEmpresa));
	}

	public boolean existePorNome(String nome, Long idEmpresa) {
		return produtoRepository.existePorNome(nome, idEmpresa);
	}

	public boolean existePorNomeDiferenteId(Long id, String nome, Long idEmpresa) {
		return produtoRepository.existePorNomeDiferenteId(id, nome, idEmpresa);
	}

	@Transactional
	public void deleteById(Long id, Long idEmpresa) {
		if (!produtoRepository.existsById(id, idEmpresa)) {
			throw new MsgApiException("Produto não encontrado ou já foi deletado.");
		}

		produtoRepository.deleteById(id, idEmpresa);
	}

	/*Exclusão em lote apaga tudo da empresa: só ROLE_ADMIN (P21)*/
	private void exigirAdminParaExclusaoEmLote() {
		if (!usuarioLogadoService.isAdmin()) {
			throw new MsgApiException("Apenas administradores podem excluir registros em lote.", HttpStatus.FORBIDDEN);
		}
	}

	@Transactional
	public long deleteAll(Long empresaID) {
		exigirAdminParaExclusaoEmLote();

		if (produtoRepository.total(empresaID) == 0) {
			throw new MsgApiException("Nenhum produto encontrado para deletar ou todos já foram deletados.");
		}

		return produtoRepository.deleteAll(empresaID);
	}

	@Transactional
	public void deletarAllById(Iterable<Long> ids, Long empresaId) {
		exigirAdminParaExclusaoEmLote();

		List<Long> encontrados = produtoRepository.buscarPorIds(ids, empresaId).stream().map(Produto::getId).toList();
		List<Long> naoEncontrados = new ArrayList<>();
		ids.forEach(id -> {
			if (!encontrados.contains(id)) {
				naoEncontrados.add(id);
			}
		});

		if (encontrados.isEmpty() && naoEncontrados.isEmpty()) {
			throw new MsgApiException("Nenhum registro informado para deletar.");
		}

		if (!naoEncontrados.isEmpty()) {
			throw new MsgApiException("Produtos não encontrados ou já deletados: " + naoEncontrados);
		}

		produtoRepository.deletarAllById(ids, empresaId);
	}

	public List<ProdutoDTO> buscarPorIds(Iterable<Long> ids, Long empresaId) {
		return converterListaParaDto(produtoRepository.buscarPorIds(ids, empresaId));
	}

	public boolean existsById(Long id, Long empresaId) {
		return produtoRepository.existsById(id, empresaId);
	}

	public List<ProdutoDTO> listar(Long empresaId) {
		return converterListaParaDto(produtoRepository.listar(empresaId));
	}

	public Optional<ProdutoDTO> buscarPorId(Long id, Long empresaId) {
		return produtoRepository.buscarPorId(id, empresaId).map(this::converterParaDto);
	}

	public long total(Long empresaId) {
		return produtoRepository.total(empresaId);
	}

	public Page<ProdutoDTO> listarPaginado(Long empresaId, Pageable pageable) {
		return produtoRepository.listarPaginado(empresaId, pageable).map(this::converterParaDto);
	}

	// ====================Conversão para DTO====================

	private ProdutoDTO converterParaDto(Produto produto) {
		ProdutoDTO dto = new ProdutoDTO();
		dto.setId(produto.getId());
		dto.setNome(produto.getNome());
		dto.setDescricao(produto.getDescricao());
		dto.setImagem(produto.getImagem());
		dto.setPreco(produto.getPreco());
		dto.setEstoque(produto.getEstoque());
		dto.setEstoqueMinimo(produto.getEstoqueMinimo());
		dto.setSku(produto.getSku());
		dto.setCodigoBarra(produto.getCodigoBarra());
		dto.setUnidadeMedida(produto.getUnidadeMedida());
		dto.setCategoriaId(produto.getCategoria().getId());
		dto.setCategoriaNome(produto.getCategoria().getNome());
		dto.setEmpresaId(produto.getEmpresa().getId());
		return dto;
	}

	private List<ProdutoDTO> converterListaParaDto(List<Produto> produtos) {
		return produtos.stream().map(this::converterParaDto).toList();
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
