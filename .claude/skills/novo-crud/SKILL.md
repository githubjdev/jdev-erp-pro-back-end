---
name: novo-crud
description: Cria um CRUD completo no padrão do Jdev ERP Pro (script SQL, entidade, repository, DTO quando necessário, service com conversão manual, controller com autorização e testes). Use quando pedirem um cadastro ou entidade nova.
argument-hint: "Entidade e campos (ex.: Fornecedor razaoSocial, cnpj, email, ativo)"
---
Crie o CRUD de: $ARGUMENTS
O código fica em `jdev-erp-pro-back-end-dev/src/main/java/br/com/jdeverp/pro/`. Siga o `CLAUDE.md` e as regras de `.claude/rules/`.

1. **Referência:** escolha o CRUD existente mais parecido e diga qual foi: `Categoria` (só nome), `Produto` (FK para entidade da empresa), `Pedido` (FK para `Usuario`, enums e valores) ou `ClienteFuncionario` (resposta em DTO).
2. **Plano curto — espere meu OK antes de seguir.** Ele traz:
   - arquivos; campos com tipo Java e coluna; tabela e sequence (`seq_` + tabela);
   - se é por empresa (padrão) ou global; se a API usa a entidade ou DTO, e por quê;
   - as rotas `/api/entidadeCamelCase/...`, cada uma com a autorização (`isAuthenticated()` ou `hasAuthority('ROLE_ADMIN')`).
3. **Implementação, nesta ordem:**
   1. Script `src/main/webapp/db_sql_versionamento/V<próximo>_CRIA_TABELA_X.sql`, idempotente, com tabela, sequence, FKs, unique e índice em `empresa_id`.
   2. `model/X.java`.
   3. `repository/XRepository.java`, `extends JpaJdevRepository<X, Long>`, com `empresa.id` em toda `@Query`.
   4. `dto/XDTO.java`, só se o plano pedir. A conversão é manual (sem mapper).
   5. `service/XService.java`: regras com `MsgApiException` e conversão em um `converterParaDto` privado.
   6. `controller/XController.java`, com `@PreAuthorize` em cada endpoint.
   7. Testes, pelo subagente `testador`: `TestXService` com Mockito e `TestXRepository` estendendo `TestContextoSpring`.
4. **Testes:** rode só os novos, dentro de `jdev-erp-pro-back-end-dev/`:
   `JAVA_HOME="/c/Program Files/Java/jdk-25.0.3" mvn -Dmaven.compiler.proc=full -Dtest=TestXService,TestXRepository test`
   - A subida do contexto também valida as queries (`RepositoryEmpresaValidator`).
   - Corrija até passar. Se a falha for de regra de negócio, pergunte antes de mudar.
5. **Revisão:** chame o subagente `revisor-codigo` nas mudanças e corrija os itens críticos.
6. **Fechamento:** liste os arquivos e uma tabela de endpoints (método, rota, autorização). Avise que o script SQL não roda sozinho e não faça commit.
