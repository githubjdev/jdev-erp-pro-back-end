---
paths:
  - "**/src/main/java/br/com/jdeverp/pro/controller/**/*.java"
  - "**/src/main/java/br/com/jdeverp/pro/dto/**/*.java"
---
# Controllers e DTOs

- **Papel:** o controller recebe, valida (`@RequestBody @Valid`) e delega ao service. Não acessa repository, não tem regra de negócio nem `try/catch` (os erros vão para o `GlobalExceptionHandler`).
- **Classe:** `@RestController` + `@RequestMapping("/api/entidadeCamelCase")` no singular. Service e `UsuarioLogadoService` com `@Autowired`. Passe `usuarioLogadoService.getEmpresaIdLogada()` ao service.
- **Endpoints padrão:**
  - escrita: `POST /salvar`, `POST /atualizar`, `DELETE /deletar/{id}`;
  - consulta: `GET /listar`, `GET /buscarPorId/{id}`, `GET /existePorNome/{nome}`, `GET /existePorNomeDiferenteId/{id}/{nome}`, `GET /existePorId/{id}`, `GET /total`, `GET /paginado`.
- **Variações:** por pai, `/listarPorPedido/{idPedido}` e `/deletarPorPedido/{id}/{idPedido}`. Em lote, `POST /buscarPorIds` e `DELETE /deletarTodosPorIds`, com `@RequestBody Iterable<Long> ids`.
- **Resposta:**
  - Sempre `ResponseEntity.ok(...)`. A exclusão devolve uma mensagem (`"Categoria deletada com sucesso."`).
  - Registro não encontrado ou regra violada: o service lança `MsgApiException` (400). Nunca monte `ResponseApi` no controller.
- **Paginação:** `page=0`, `size=10`, `sort=id`, `direction=asc` → `PageRequest.of(...)` → `service.listarPaginado(empresaId, pageable)`, como em `CategoriaController.listarPaginado()`.
  - O `sort` é concatenado no JPQL: aceite só campos conhecidos da entidade.
- **Entidade ou DTO:**
  - CRUD simples usa a própria entidade.
  - Use DTO para campo sensível ou derivado (`UsuarioDto`, `LoginDTO`, `TokenDTO`, `AlterarSenhaDTO`).
  - Use DTO também para `Empresa` e `ClienteFuncionario`: no JSON elas saem só como id (`@JsonIdentityReference(alwaysAsId = true)`).
- **DTO:** classe `dto/EntidadeDTO` (`UsuarioDto` é exceção antiga), com Lombok `@Getter @Setter`. Relacionamentos viram `xxxId` + `xxxNome`. A conversão é manual, no service.
