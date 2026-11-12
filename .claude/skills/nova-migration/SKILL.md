---
name: nova-migration
description: Cria a próxima migração Flyway do PostgreSQL em src/main/resources/db/migration (padrão VYYYYMMDD_HHMMSS__descricao.sql), coerente com as entidades JPA. Use para mudanças de tabela, coluna, constraint, índice ou dados de seed.
argument-hint: "o que a migração deve fazer (ex.: criar índices em empresa_id nas tabelas de venda)"
---
Crie uma migração Flyway para: $ARGUMENTS

Migrações existentes: !`git ls-files --cached --others --exclude-standard -- ':(top,glob)jdev-erp-pro-back-end-dev/src/main/resources/db/migration/*.sql'`

**Como o banco funciona aqui:**
- O Flyway aplica as migrações sozinho, na subida da aplicação (IDE, testes ou `java -jar`), antes do Hibernate.
- Migração aplicada nunca é editada, nem num comentário: o checksum muda e a aplicação não sobe.
- O Hibernate ainda está com `ddl-auto=update` (P22).
- Detalhes em `docs/Flyway.md`.

1. **Nome:** `V<AAAAMMDD>_<HHMMSS>__<descricao_em_minusculas>.sql`, com a data e a hora atuais.
   - Sem acento nem espaço. Dois underscores antes da descrição.
   - A versão tem de ser maior que a da última migração da lista acima.
2. **Entidades:** leia as entidades e os repositories afetados (isso carrega a `jpa-entidades.md`). Confira os nomes reais de tabela, coluna, FK, unique e sequence.
3. **Se for destrutivo, pare:** explique o risco, o efeito nos dados e um plano em etapas, e espere meu OK. Conta como destrutivo:
   - DROP;
   - mudança de tipo;
   - NOT NULL em tabela com dados;
   - DELETE ou UPDATE em massa;
   - renomear coluna.
4. **Script:**
   - PostgreSQL, uma alteração por arquivo, pequena.
   - Idempotente (`IF NOT EXISTS` / `IF EXISTS`): o Hibernate pode já ter criado a coluna no banco dev.
   - Índice para cada FK nova.
   - Uma linha de comentário com o objetivo.
   - Tabela grande: `SET lock_timeout = '5s';` no início.
   - `CREATE INDEX CONCURRENTLY`: sozinho numa migração própria (roda fora de transação). Não use `BEGIN`/`COMMIT`: o Flyway já abre a transação.
   - Se o trabalho for grande, delegue ao subagente `especialista-banco`.
5. **Entidade:** se precisar, ajuste-a ao script (tipo, `nullable`, tamanho, constraint). O `update` do Hibernate não remove, não renomeia nem muda o tipo de colunas.
6. **Testes:**
   - Rode `-Dtest='TestMigracoesFlyway,TestVerificacaoBaselineFlyway,TestJDevERPPROApplication'`. Eles usam bancos temporários e conferem nome, execução e equivalência com o banco dev.
   - Rode também os testes das entidades afetadas (ache com Grep pelo nome), com o comando do CLAUDE.md.
   - Atenção: os testes que sobem o contexto no banco dev aplicam a migração nova nele.
7. **Fechamento:**
   - Mostre o nome do arquivo e o SQL.
   - Avise que a migração roda sozinha na próxima subida de cada ambiente.
   - Não suba a aplicação apontando para outro banco sem eu pedir.
