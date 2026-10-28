---
paths:
  - "**/src/main/java/br/com/jdeverp/pro/controller/**/*.java"
  - "**/src/main/java/br/com/jdeverp/pro/dto/**/*.java"
---
# Controllers e DTOs

- **O controller só recebe, valida (`@Valid`) e delega ao service.** Não chama repository, não tem regra de negócio nem `try/catch`: os erros vão para o `GlobalExceptionHandler`.
- **Empresa logada:** em consultas e exclusões, passe `usuarioLogadoService.getEmpresaIdLogada()` ao service.
- **Rotas:**
  - Rota base no singular (`/api/movimentacaoProduto`). A ação vai no caminho, não no verbo HTTP.
  - Variações por pai: `/listarPorPedido/{idPedido}`, `/deletarPorPedido/{id}/{idPedido}`.
  - Lote: `POST /buscarPorIds` e `DELETE /deletarTodosPorIds`, com `@RequestBody Iterable<Long> ids`.
- **Exclusão** devolve 200 com mensagem: `ResponseEntity.ok("Categoria deletada com sucesso.")`.
- **Erros:**
  - Registro não encontrado ou regra violada: o service lança `MsgApiException`, que vira 400.
  - Nunca monte `ResponseApi` no controller.
- **Entidade ou DTO:**
  - CRUD simples recebe e devolve a própria entidade (padrão atual).
  - Use DTO quando houver campo sensível ou dado derivado (`UsuarioDto`, `LoginDTO`, `TokenDTO`, `AlterarSenhaDTO`).
  - Para `Empresa` e `ClienteFuncionario` use DTO também: elas saem só como id no JSON (`@JsonIdentityReference(alwaysAsId = true)`).
- **Formato do DTO:** em `br.com.jdeverp.pro.dto`, nome `EntidadeDTO` (`UsuarioDto` é exceção antiga), com Lombok `@Getter @Setter`. Relacionamento vira `xxxId` + `xxxNome`, ex.: `pessoaId`, `pessoaNome`.
- **Mapeamento entidade↔DTO:** manual, no service, sem MapStruct. Use um método privado `converterParaDto(...)`, como em `UsuarioService`, em vez de repetir o bloco de setters.
- **`sort` de `/paginado`:** vai concatenado no JPQL. Aceite só nomes de campo conhecidos da entidade; qualquer outro valor é recusado com `MsgApiException`.
