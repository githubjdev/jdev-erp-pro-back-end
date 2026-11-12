---
name: novo-crud
description: Cria um CRUD completo no padrão do Jdev ERP Pro (script SQL, entidade, repository, DTO de resposta, service, controller com autorização e testes). Use quando pedirem um cadastro ou entidade nova.
argument-hint: "Entidade e campos (ex.: Fornecedor razaoSocial, cnpj, email, ativo)"
---
Crie o CRUD de: $ARGUMENTS

Migrações Flyway existentes: !`git ls-files --cached --others --exclude-standard -- ':(top,glob)jdev-erp-pro-back-end-dev/src/main/resources/db/migration/*.sql'`

1. **Referência:** escolha o CRUD mais parecido e leia o model, o repository, o service e o controller dele (isso carrega as rules). As opções:
   - `Categoria`: só nome;
   - `Produto`: FK para outra entidade da empresa;
   - `Pedido`: FK para `Usuario`, enums e valores.
   Todos recebem a entidade e devolvem DTO.
2. **Plano curto — espere meu OK antes de criar.** Mostre os arquivos; os campos (tipo Java e coluna); os campos do DTO de resposta; a tabela e a sequence; se é por empresa ou global; e as rotas com a autorização de cada uma.
   Se algum campo depender de algo em aberto em `docs/pendencias.md`, pergunte.
3. **Crie nesta ordem:**
   1. a próxima migração Flyway, `V<AAAAMMDD>_<HHMMSS>__cria_tabela_x.sql` em `src/main/resources/db/migration/`, como na skill `nova-migration` (idempotente, com tabela, sequence, FKs, unique e índice em `empresa_id`);
   2. `model/X.java` → `repository/XRepository.java` → `dto/XDTO.java` → `service/XService.java` → `controller/XController.java` com `@PreAuthorize`;
   3. os testes, pelo subagente `testador` (`TestXService` e `TestXRepository`).
4. **Testes:** rode os novos e os do Flyway com o comando do CLAUDE.md (`-Dtest=TestXService,TestXRepository,TestMigracoesFlyway`) e corrija até passar. Se a falha for de regra de negócio, pergunte antes de mudar.
5. **Revisão:** chame o subagente `revisor-codigo` e corrija os itens críticos.
6. **Fechamento:**
   - Liste os arquivos e mostre uma tabela de endpoints (método, rota, autorização).
   - Avise que a migração roda sozinha na próxima subida de cada ambiente (Flyway).
   - Não faça commit.
