---
paths:
  - "**/src/main/java/br/com/jdeverp/pro/service/**/*.java"
---
# Services

- **Antes de mexer:** leia a seção do módulo em `docs/regras-negocio.md`. Não implemente nada que esteja em aberto em `docs/pendencias.md`.
- **Injeção:** repositories e `UsuarioLogadoService` com `@Autowired` em campo.
- **salvar:**
  1. unicidade na empresa (`existePorNome(nome, empresaId)`);
  2. valida as FKs;
  3. `setEmpresa(usuarioLogadoService.getEmpresaLogada())`;
  4. `saveAndFlush`.
- **atualizar:**
  1. id não nulo;
  2. `existente = buscarPorId(id, empresaId)`, senão `MsgApiException`;
  3. unicidade com `existePorXDiferenteId`;
  4. copia os campos editáveis para `existente`, como em `PedidoService` e `ChamadoService`;
  5. `saveAndFlush(existente)`.
- **FK vinda do request:** valide num método privado `validarX(x, empresaId)` que exige o `id` e busca com `buscarPorId(id, empresaId)`. Exemplo: `ProdutoService.validarCategoria()`.
- **Exclusão:** confira com `existsById(id, empresaId)` antes. Em lote, é tudo ou nada, como em `PessoaService.deletarAllById()`.
- **Mensagens:** em português e específicas, ex.: "Produto não encontrado para a empresa logada."
- **Transação:** método com mais de uma escrita usa `@Transactional` (`org.springframework.transaction.annotation`).
- **DTO:** conversão num método privado `converterParaDto(...)`. Nunca copie a `senha`.
