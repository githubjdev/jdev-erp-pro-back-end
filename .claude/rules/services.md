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
- **Transação** (`org.springframework.transaction.annotation.Transactional`):
  - O `spring.jpa.open-in-view` é `false`: a sessão do Hibernate só existe dentro da transação do service.
  - A classe leva `@Transactional(readOnly = true)` e todo método que grava ou apaga leva `@Transactional`. Esquecer o `@Transactional` num método de escrita dá erro "read-only transaction" no PostgreSQL.
  - Tudo que lê relacionamento LAZY (inclusive a conversão para DTO) fica dentro do método do service. Fora dele, dá `LazyInitializationException`.
- **DTO:**
  - Todo método chamado pelo controller devolve DTO. A conversão fica nos métodos privados `converterParaDto(X)` e `converterListaParaDto(List<X>)`; paginado usa `page.map(this::converterParaDto)`. Nunca copie a `senha`.
  - Método usado por outro service continua devolvendo a entidade. Para o controller, crie a variante com sufixo `Dto`: `PessoaService.buscarPorId` / `buscarPorIdDto`, `RoleService.buscaPorAcesso` / `buscaPorAcessoDto`, `RoleUsuarioService.salvar` / `salvarDto`, `ClienteFuncionarioService.findByPessoa` / `findByPessoaDto`.
  - Relacionamento vindo do request chega só com o `id`: troque pelo registro buscado no banco (`validarX`) antes de salvar, senão o DTO sai com o nome `null`.
  - O usuário do `UsuarioLogadoService` é carregado no `JwtFilter`, fora da transação. Para pôr o nome dele no DTO, busque-o de novo (`validarUsuario(usuarioLogadoService.getUsuarioLogado(), empresaId)`, como em `ChamadoService.salvar`). Sem isso, dá `LazyInitializationException`.
