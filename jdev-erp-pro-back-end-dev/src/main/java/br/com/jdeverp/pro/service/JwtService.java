package br.com.jdeverp.pro.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import br.com.jdeverp.pro.exception.MsgApiException;
import br.com.jdeverp.pro.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration}")
	private String expiration;

	/**
	 * Retorna a chave secreta para assinatura do token JWT.
	 *
	 * @return A chave secreta.
	 */
	private SecretKey getKey() {
		return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.ISO_8859_1));
	}

	/**
	 * Extrai as claims (informações) do token JWT.
	 *
	 * @param token O token JWT.
	 * @return As claims extraídas do token.
	 */
	public Claims extrairClaims(String token) {
		return Jwts.parser()
				   .verifyWith(getKey())
				   .build()
				   .parseSignedClaims(token)
				   .getPayload();
	}
	
	/**
	 * Gera um token JWT para o usuário fornecido.
	 *
	 * @param usuario O usuário para o qual o token será gerado.
	 * @return O token JWT gerado.
	 */
	public String gerarToken(Usuario usuario) {
		return Jwts.builder()
				   .subject(usuario.getLogin())
				   .claim("usuarioId", usuario.getId())
				   .claim("empresaId", usuario.getEmpresa().getId())
				   .claim("login", usuario.getLogin())
				   .issuedAt(new Date())
				   .expiration(new Date(System.currentTimeMillis() + expiration))
				   .signWith(getKey())
				   .compact();
	}
	
	/**
	 * Extrai o ID do empresaId do token JWT.
	 *
	 * @return O ID do empresaId.
	 */
	public Long extrairEmpresaId(){
		
		Claims claims = extrairClaims(getToken());
		
		return claims.get("empresaId", Long.class);
		
	}
	
	
	/**
	 * Extrai o ID do empresaId do token JWT.
	 *
	 * @return O ID do empresaId.
	 */
	public Long extrairEmpresaId(String token){
		
		Claims claims = extrairClaims(token);
		
		return claims.get("empresaId", Long.class);
		
	}
	
	/**
	 * Extrai o ID do usuário do token JWT.
	 *
	 * @return O ID do usuário.
	 */
	public Long extrairUsuarioId(){
		
		Claims claims = extrairClaims(getToken());
		
		return claims.get("usuarioId", Long.class);
		
	}
	
	
	public String extrairLogin(String token) {
		return extrairClaims(token).getSubject();
	}
	
	
	
	public boolean validarToken(String token) {
		
		try {
		
		if (token == null || token.isEmpty()) {
			return false;
		}
		
		Jwts.parser().verifyWith(getKey())
		    .build()
		    .parseSignedClaims(token);
		
		return true;
		
		}catch (Exception e) {
			e.printStackTrace();
			throw new MsgApiException("Token de acesso do usuário é inválido.");
		}
		
	}
	
	
	
	private String getToken() {
		return (String) SecurityContextHolder.getContext().getAuthentication().getCredentials();
	}
	

}
