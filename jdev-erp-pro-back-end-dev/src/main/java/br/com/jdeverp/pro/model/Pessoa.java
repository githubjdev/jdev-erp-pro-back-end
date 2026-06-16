package br.com.jdeverp.pro.model;

import java.io.Serializable;
import java.time.LocalDate;

import br.com.jdeverp.pro.enums.TipoPessoa;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "pessoa")
@SequenceGenerator(name = "seq_pessoa", sequenceName = "seq_pessoa", allocationSize = 1, initialValue = 1)
public class Pessoa implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pessoa")
    private Long id;

    @Column(length = 200, nullable = false)
    private String nome;

    @Column(name = "razao_social", length = 200)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 200)
    private String nomeFantasia;

    @Column(name = "insc_estadual", length = 200)
    private String inscEstadual;

    @Column(length = 50)
    private String cnpj;

    @Column(length = 50, nullable = false)
    private String telefone;

    @Column(length = 30, nullable = false)
    private String cpf;

    @Column(length = 250, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", nullable = false)
    private TipoPessoa tipoPessoa;

    private Boolean ativo = true;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDate dataCadastro = LocalDate.now();

    @Column(length = 1000)
    private String observacao;

    @Column(length = 50, nullable = false)
    private String cep;

    @Column(length = 255, nullable = false)
    private String logradouro;

    @Column(length = 250 , nullable = false)
    private String bairro;

    @Column(length = 200, nullable = false)
    private String estado;

    @Column(length = 300 , nullable = false)
    private String cidade;

    @Column(length = 250 , nullable = false)
    private String pais;

    @Column(length = 400)
    private String complemento;

    /*Refere-se ao cadastro da emrpreda em multitanenti*/
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", 
            nullable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "empresa_fk"))
    private Empresa empresa;
    
    
}