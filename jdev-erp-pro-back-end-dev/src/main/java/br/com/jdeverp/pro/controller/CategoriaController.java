package br.com.jdeverp.pro.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
	
	

}
