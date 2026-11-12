---
paths:
  - "**/src/main/java/br/com/jdeverp/pro/model/**/*.java"
  - "**/src/main/java/br/com/jdeverp/pro/repository/**/*.java"
---
# Entidades JPA e repositories

- **Tabela:** snake_case no singular (`@Table(name = "item_pedido")`). As colunas já saem em snake_case pelo naming padrão.
- **Id:** `@SequenceGenerator(name = "seq_categoria", sequenceName = "seq_categoria", allocationSize = 1, initialValue = 1)` + `@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_categoria")`.
- **FK explícita:** `@JoinColumn(name = "categoria_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "categoria_fk"))`.
- **Unique:** `@UniqueConstraint(name = "unique_sku_empresa", columnNames = {"sku", "empresa_id"})`. Use nomes de coluna do banco e inclua `empresa_id`.
- **Relacionamento novo:** `@ManyToOne`/`@OneToOne` LAZY.
  - Não mude os EAGER existentes (`Usuario.acessos`, `Usuario.empresa`, `Produto.categoria`).
  - Não crie `@OneToMany` nem coleções (veja `Plano`); liste os filhos pelo repository do filho.
- **Lombok e `EntidadeBase`:** `@Getter @Setter @ToString(onlyExplicitlyIncluded = true)` e `extends EntidadeBase`. Nunca use `@Data` nem `@EqualsAndHashCode`.
  - `@ToString` gerado pelo Lombok lê os relacionamentos LAZY e dá `LazyInitializationException` fora da sessão; `@EqualsAndHashCode` também.
  - **`@ToString.Include`:** só em `id` e campos simples.
    - Nunca em relacionamento, coleção, senha, token, CPF, CNPJ, e-mail, telefone, endereço, nem em texto longo ou base64 (`logoMarca`, `imagem`, `arquivo`).
  - **`equals`/`hashCode`:** vêm do `EntidadeBase` e comparam pelo id, sem carregar o proxy. Não sobrescreva.
- **Campo obrigatório:** Bean Validation com mensagem em português **e** `@Column(nullable = false)`.
- **Tipos:** enum com `@Enumerated(EnumType.STRING)`, em `enums/`; dinheiro novo em `BigDecimal` (padrão `BigDecimal.ZERO`; `Produto.preco` e `Plano.valorMensal` continuam `Double`); quantidade em `Double` com `@DecimalMin("0.1")`.
- **Datas e textos:** datas em `LocalDate`, preenchidas no service (não há auditoria); texto longo ou base64 em `columnDefinition = "text"`.
- **Repository:** toda `@Query` com `empresa.id` e `@Param("idEmpresa")`; existência com `select count(x.id) > 0`; métodos `buscaPorX`, `existePorX`, `existePorXDiferenteId`, `findAllByPai`.
- **Busca por nome:** `unaccent(upper(trim(x.nome))) like unaccent(upper(concat('%', trim(:nome), '%')))`.
- **Delete customizado:** `@Transactional` + `@Modifying(flushAutomatically = true, clearAutomatically = true)`.
- **N+1:** se o service percorre relacionamentos em loop, use `join fetch` na `@Query` (hoje `UsuarioService.listar()` faz N+1).
- **Paginação:** retorne `Page<T>`. O `countQuery` também precisa de `empresa.id`, porque o validador só olha o `value`.
