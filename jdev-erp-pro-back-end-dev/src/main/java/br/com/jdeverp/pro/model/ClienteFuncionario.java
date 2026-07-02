package br.com.jdeverp.pro.model;

import br.com.jdeverp.pro.enums.TipoClienteFuncionario;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "cliente_funcionario", uniqueConstraints = {
		@UniqueConstraint(name="unique_pessoa_usuario", columnNames = {"usuario_id","pessoa_id"}),
		@UniqueConstraint(name="unique_usuario", columnNames = {"usuario_id"}),
		@UniqueConstraint(name="unique_pessoa", columnNames = {"pessoa_id"})
})
@SequenceGenerator(name = "seq_cliente_funcionario", sequenceName = "seq_cliente_funcionario", allocationSize = 1, initialValue = 1)
public class ClienteFuncionario {
	
	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cliente_funcionario")
	private Long id;
	
    @NotNull(message = "Informe o tipo de realação com a pessoa")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cliente_funcionario", nullable = false)
	private TipoClienteFuncionario tipoClienteFuncionario;
	

	@NotNull(message =  "O usuário deve ser informado para criar o castrado")
    @OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", 
	       nullable = false, 
	       foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "usuario_fk"))
	private Usuario usuario;
	
	
	@NotNull(message =  "A pessoa deve ser informada para criar o castrado")
    @OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pessoa_id", 
	       nullable = false, 
	       foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "pessoa_fk"))
	private Pessoa pessoa;
	
	
    @NotNull(message = "Empresa deve ser informada corretamente")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, 
                  name = "empresa_fk"))
    private Empresa empresa;


}
