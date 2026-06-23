package br.com.jdeverp.pro.model;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "role", uniqueConstraints = {
		@UniqueConstraint(name = "unique_acesso", columnNames = "acesso"),
})
@SequenceGenerator(name = "seq_role", sequenceName = "seq_role", allocationSize = 1, initialValue = 1)
public class Role implements GrantedAuthority {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_role")
	private Long id;

	@NotBlank(message = "Acesso deve ser informado")
	@NotNull(message = "Acesso não pode ser nulo")
	@Column(nullable = false, unique = true)
	private String acesso;

	@Override
	public @Nullable String getAuthority() {

		return this.acesso;
	}

}
