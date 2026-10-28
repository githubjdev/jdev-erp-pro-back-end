---
paths:
  - "**/src/main/java/br/com/jdeverp/pro/model/**/*.java"
  - "**/src/main/java/br/com/jdeverp/pro/repository/**/*.java"
---
# Entidades JPA e repositories

- **Tabela:** snake_case no singular (`@Table(name = "item_pedido")`). As colunas já saem em snake_case pelo naming padrão; use `@Column(name = ...)` só quando precisar.
- **FK sempre explícita:** `@JoinColumn(name = "categoria_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "categoria_fk"))`.
- **Unique:** nome `unique_...` com nomes de coluna do banco (`@UniqueConstraint(name = "unique_sku_empresa", columnNames = {"sku", "empresa_id"})`). Unicidade de negócio é por empresa: inclua `empresa_id`.
- **Relacionamento novo:** `@ManyToOne`/`@OneToOne` com `fetch = FetchType.LAZY`. Não mude os `EAGER` existentes (`Usuario.acessos`, `Usuario.empresa`, `Produto.categoria`) sem eu pedir.
- **Sem `@OneToMany` nem coleções em entidade.** O projeto evita isso (veja o comentário em `Plano`). Para listar filhos, crie uma query no repository do filho, ex.: `findAllByPedido(idPedido, idEmpresa)`.
- **Lombok:** mantenha o `@Data` das entidades. Em entidade com relação bidirecional, use `@Getter @Setter`, para não gerar `equals`/`hashCode`/`toString` recursivos.
- **Enum:** sempre `@Enumerated(EnumType.STRING)`, com a classe em `br.com.jdeverp.pro.enums`.
- **Dinheiro novo:** `BigDecimal` com padrão `BigDecimal.ZERO`, como em `Pedido`. `Produto.preco` e `Plano.valorMensal` são `Double`: não converta sem pedido.
- **Quantidade:** `Double` com `@DecimalMin(value = "0.1", message = "...")`.
- **Datas:** `LocalDate`. Não há auditoria (createdAt/updatedAt). Datas de evento (`dataCadastro`, `dataAbertura`, `dataEnvio`) são preenchidas no service com `LocalDate.now()`.
- **Campo obrigatório:** Bean Validation (`@NotNull`/`@NotBlank`) **e** `@Column(nullable = false)`. Nunca deixe o NOT NULL só no banco.
- **Texto longo ou base64** (imagem, logo, arquivo): `@Column(columnDefinition = "text")`.
- **Repository:**
  - existência com `select count(x.id) > 0 ...` e parâmetros `@Param("idEmpresa")`;
  - nomes `buscaPorX`, `existePorX`, `existePorXDiferenteId`, `findAllByPai`.
- **Evitar N+1:** se o service lê relacionamentos em loop, use `join fetch` na `@Query`. Hoje `UsuarioService.listar()` faz N+1.
- **`@Query` paginada:** devolve `Page<T>`. Se houver `countQuery`, ela também precisa filtrar `empresa.id`, porque o validador só olha o `value`.
