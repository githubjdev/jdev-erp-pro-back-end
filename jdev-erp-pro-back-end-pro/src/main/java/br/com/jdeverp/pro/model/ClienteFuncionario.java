package br.com.jdeverp.pro.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "cliente_funcionario")
@SequenceGenerator(name = "seq_cliente_funcionario", sequenceName = "seq_cliente_funcionario", allocationSize = 1, initialValue = 1)
public class ClienteFuncionario {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cliente_funcionario")
	private Long id;

}
