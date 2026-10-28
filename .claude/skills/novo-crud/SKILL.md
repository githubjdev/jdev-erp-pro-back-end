---
name: novo-crud
description: Cria um CRUD completo no padrão do Jdev ERP Pro (script SQL, entidade, repository, DTO quando necessário, service, controller com autorização e testes). Use quando pedirem um cadastro ou entidade nova.
argument-hint: "Entidade e campos (ex.: Fornecedor razaoSocial, cnpj, email, ativo)"
---
Crie o CRUD de: $ARGUMENTS

Scripts SQL existentes: !`git ls-files --cached --others --exclude-standard -- ':(top,glob)jdev-erp-pro-back-end-dev/src/main/webapp/db_sql_versionamento/*.sql'`

1. **Referência:** escolha o CRUD mais parecido e leia o model, o repository, o service e o controller dele (isso carrega as rules). As opções:
   - `Categoria`: só nome;
   - `Produto`: FK para outra entidade da empresa;
   - `Pedido`: FK para `Usuario`, enums e valores;
   - `ClienteFuncionario`: resposta em DTO.
2. **Plano curto — espere meu OK antes de criar.** Mostre os arquivos; os campos (tipo Java e coluna); a tabela e a sequence; se é por empresa ou global; se usa entidade ou DTO, e por quê; e as rotas com a autorização de cada uma.
   Se algum campo depender de algo em aberto em `docs/pendencias.md`, pergunte.
3. **Crie nesta ordem:**
   1. o próximo script `V<n>_CRIA_TABELA_X.sql` (idempotente, com tabela, sequence, FKs, unique e índice em `empresa_id`);
   2. `model/X.java` → `repository/XRepository.java` → `dto/XDTO.java` (só se o plano pedir) → `service/XService.java` → `controller/XController.java` com `@PreAuthorize`;
   3. os testes, pelo subagente `testador` (`TestXService` e `TestXRepository`).
4. **Testes:** rode só os novos com o comando do CLAUDE.md (`-Dtest=TestXService,TestXRepository`) e corrija até passar. Se a falha for de regra de negócio, pergunte antes de mudar.
5. **Revisão:** chame o subagente `revisor-codigo` e corrija os itens críticos.
6. **Fechamento:**
   - Liste os arquivos e mostre uma tabela de endpoints (método, rota, autorização).
   - Avise que o script SQL precisa ser aplicado à mão.
   - Não faça commit.
