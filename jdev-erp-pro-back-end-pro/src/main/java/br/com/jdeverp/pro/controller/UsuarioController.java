package br.com.jdeverp.pro.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.jdeverp.pro.dto.LoginDTO;
import br.com.jdeverp.pro.dto.TokenDTO;
import br.com.jdeverp.pro.service.UsuarioService;
import jakarta.validation.Valid;

/*Cada controller tem um contexto, recebe dados do front-end, api, integração e etc*/
@RestController
@RequestMapping("/api/usuario")
public class UsuarioController {
	
	@Autowired
	private UsuarioService usuarioService;
	
	
	/*Ponto de acesso (end-point): /api/usuario/login */
	@PostMapping("/login")
	public ResponseEntity<TokenDTO> login(@RequestBody  @Valid LoginDTO login){
		
		TokenDTO tokebDto = usuarioService.login(login);
		
		return ResponseEntity.ok(tokebDto);
	}
}
