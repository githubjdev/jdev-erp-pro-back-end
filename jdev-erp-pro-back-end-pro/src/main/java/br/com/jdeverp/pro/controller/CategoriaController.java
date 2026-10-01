package br.com.jdeverp.pro.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.model.Categoria;
import br.com.jdeverp.pro.service.CategoriaService;
import br.com.jdeverp.pro.service.UsuarioLogadoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categoria")
public class CategoriaController {
	
	
	@Autowired
	private CategoriaService categoriaService;
	
	@Autowired
	private UsuarioLogadoService usuarioLogadoService;
	
	
	@PostMapping("/salvar")
	public ResponseEntity<Categoria> salvar(@RequestBody @Valid Categoria categoria){
		
		Categoria usuarioSalvo = categoriaService.salvar(categoria);
		
		return ResponseEntity.ok(usuarioSalvo);
	}
	
	
	@PostMapping("/atualizar")
	public ResponseEntity<Categoria> atualizar(@RequestBody @Valid Categoria categoria){
		
		Categoria usuarioSalvo = categoriaService.atualizar(categoria);
		
		return ResponseEntity.ok(usuarioSalvo);
	}
	
	@DeleteMapping("/deletar/{id}")
	public ResponseEntity<String> deletePorId(@PathVariable(required = true, value = "id") Long idCategoria){
		
		categoriaService.deleteById(idCategoria, usuarioLogadoService.getEmpresaIdLogada());
		
		return ResponseEntity.ok("Categoria deletada com sucesso.");
	}
	
	
	@GetMapping("/listar")
	public ResponseEntity<List<Categoria>> listar(){
		
		return ResponseEntity.ok(categoriaService.findAll(usuarioLogadoService.getEmpresaIdLogada()));
	}
	
	
	@GetMapping("/buscaPorNome/{nome}")
	public ResponseEntity<List<Categoria>> buscaPorNome(@PathVariable String nome){
		
		return ResponseEntity.ok(categoriaService.buscaPorNome(nome, usuarioLogadoService.getEmpresaIdLogada()));
	}
	

	@GetMapping("/buscarPorId/{id}")
	public ResponseEntity<Categoria> buscarPorId(@PathVariable Long id){

		return ResponseEntity.ok(categoriaService.buscarPorId(id, usuarioLogadoService.getEmpresaIdLogada()));
	}


	@GetMapping("/existePorNome/{nome}")
	public ResponseEntity<Boolean> existePorNome(@PathVariable String nome){

		return ResponseEntity.ok(categoriaService.existePorNome(nome, usuarioLogadoService.getEmpresaIdLogada()));
	}


	@GetMapping("/existePorNomeDiferenteId/{id}/{nome}")
	public ResponseEntity<Boolean> existePorNomeDiferenteId(@PathVariable Long id, @PathVariable String nome){

		return ResponseEntity.ok(categoriaService.existePorNomeDiferenteId(id, nome, usuarioLogadoService.getEmpresaIdLogada()));
	}


	@GetMapping("/existePorId/{id}")
	public ResponseEntity<Boolean> existsById(@PathVariable Long id){

		return ResponseEntity.ok(categoriaService.existsById(id, usuarioLogadoService.getEmpresaIdLogada()));
	}


	@GetMapping("/total")
	public ResponseEntity<Long> total(){

		return ResponseEntity.ok(categoriaService.total(usuarioLogadoService.getEmpresaIdLogada()));
	}


	@GetMapping("/paginado")
	public ResponseEntity<Page<Categoria>> listarPaginado(@RequestParam(defaultValue = "0") int page,
														 @RequestParam(defaultValue = "10") int size,
														 @RequestParam(defaultValue = "id") String sort,
														 @RequestParam(defaultValue = "asc") String direction){

		Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
		Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

		return ResponseEntity.ok(categoriaService.listarPaginado(usuarioLogadoService.getEmpresaIdLogada(), pageable));
	}

	

}
