package br.com.jdeverp.pro.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "usuario", uniqueConstraints = {
		@UniqueConstraint(name="unique_cliente_funcionario", columnNames = {"cliente_funcionario_id"}),
		@UniqueConstraint(name="unique_login", columnNames = {"login"}),
		@UniqueConstraint(name="unique_senha", columnNames = {"senha"})
})
@SequenceGenerator(name = "seq_usuario", sequenceName = "seq_usuario", allocationSize = 1)
public class Usuario implements UserDetails {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_usuario")
	private Long id;
	
	@NotBlank(message = "Login deve ser informado")
	@Column(nullable = false, unique = true)
	private String login;
	
	@NotBlank(message = "Senha deve ser informado")
	@Column(nullable = false, unique = true)
	private String senha;
	
	private Boolean bloqueado = false;
	
	private String refreshToken;
	
	private String tokenSessao;
	
	@NotNull(message = "Cliente ou funcionário deve ser informado para cadastrar o usuário de acesso ao sistema")
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cliente_funcionario_id", nullable = false, 
	                     foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                           name = "cliente_funcionario_fk"))
	private ClienteFuncionario  clienteFuncionario;
	
	
	
	
	//Alex -> ROLE_ADMIN, ROLE_GERENTE
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name = "role_usuario",
	           uniqueConstraints = @UniqueConstraint(name="unique_role_user", 
	           columnNames = { "acesso_id", "usuario_id" }), /*Contraint de unicidade entre usuario e acesso*/
	           
	            joinColumns = @JoinColumn(name = "usuario_id",
	            foreignKey = @ForeignKey(name="usuario_fk")),   /*Representa a tabela de usuário e acesso*/
			
	            inverseJoinColumns = @JoinColumn(name="acesso_id",
	            foreignKey = @ForeignKey(name="acesso_fk"))
	            
	 )                  
	private List<Role> acessos = new ArrayList<Role>();
	
	
	/*Refere-se ao cadastro da empresa em multitanci*/
	@NotNull(message = "Empresa deve ser informado")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id", 
	        nullable = false, 
	      foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
	                                name = "empresa_fk"))
	private Empresa empresa;



	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return this.acessos;
	}



	@Override
	public @Nullable String getPassword() {
		return this.senha;
	}



	@Override
	public String getUsername() {
		return this.login;
	}
	

}
