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

import br.com.jdeverp.pro.dto.LoginDTO;
import br.com.jdeverp.pro.dto.TokenDTO;
import br.com.jdeverp.pro.dto.UsuarioDto;
import br.com.jdeverp.pro.service.UsuarioLogadoService;
import br.com.jdeverp.pro.service.UsuarioService;
import jakarta.validation.Valid;

/*Cada controller tem um contexto, recebe dados do front-end, api, integração e etc*/
@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {
	
	@Autowired
	private UsuarioService usuarioService;
	
	@Autowired
	private UsuarioLogadoService usuarioLogadoService;
	
	
	@PostMapping("/salvar")
	public ResponseEntity<UsuarioDto> salvar(@RequestBody @Valid UsuarioDto usuarioDto){
		
		UsuarioDto usuarioSalvo = usuarioService.salvar(usuarioDto);
		
		return ResponseEntity.ok(usuarioSalvo);
	}
	
	@PostMapping("/atualizar")
	public ResponseEntity<UsuarioDto> atualizar(@RequestBody @Valid UsuarioDto usuarioDto){
		
		UsuarioDto usuarioSalvo = usuarioService.atualizar(usuarioDto);
		
		return ResponseEntity.ok(usuarioSalvo);
	}
	
	
	/*Ponto de acesso (end-point): /api/usuario/login */
	@PostMapping("/login")
	public ResponseEntity<TokenDTO> login(@RequestBody  @Valid LoginDTO login){
		
		TokenDTO tokebDto = usuarioService.login(login);
		
		return ResponseEntity.ok(tokebDto);
	}
	
	
	@GetMapping("/listar")
	public ResponseEntity<List<UsuarioDto>> listarUsuarios(){
		
		return ResponseEntity.ok(usuarioService.listar(usuarioLogadoService.getEmpresaIdLogada()));
	}
	
	
	@GetMapping("/buscarPorId/{id}")
	public ResponseEntity<UsuarioDto> buscarPorId(@PathVariable(required = true, value = "id") Long idUser){
		return ResponseEntity.ok(usuarioService.buscarPorIdDto(idUser, usuarioLogadoService.getEmpresaIdLogada()));
	}
	
	
	@DeleteMapping("/deletar/{id}")
	public ResponseEntity<String> deletePorId(@PathVariable(required = true, value = "id") Long idUser){
		
		usuarioService.deleteById(idUser, usuarioLogadoService.getEmpresaIdLogada());
		
		return ResponseEntity.ok("Usuário deletado com sucesso!");
	}
	
}
