---
name: nova-migration
description: Cria o próximo script SQL versionado do PostgreSQL em db_sql_versionamento, coerente com as entidades JPA (o projeto não usa Flyway nem Liquibase). Use para mudanças de tabela, coluna, constraint, índice ou dados de seed.
argument-hint: "o que o script deve fazer (ex.: criar índices em empresa_id nas tabelas de venda)"
---
Crie um script SQL para: $ARGUMENTS

Scripts existentes: !`git ls-files --cached --others --exclude-standard -- ':(top,glob)jdev-erp-pro-back-end-dev/src/main/webapp/db_sql_versionamento/*.sql'`

**Como o banco funciona aqui:** o schema vem do Hibernate (`ddl-auto=update`). Os scripts são aplicados à mão e nunca devem ser editados depois de criados.

1. **Versão:** defina a próxima a partir da lista acima. Depois de `V1_INIT.sql` vem `V2_`.
2. **Entidades:** leia as entidades e os repositories afetados (isso carrega a `jpa-entidades.md`) e confira os nomes reais de tabela, coluna, FK, unique e sequence.
3. **Se for destrutivo, pare:** explique o risco, o efeito nos dados e um plano em etapas, e espere meu OK. Conta como destrutivo:
   - DROP;
   - mudança de tipo;
   - NOT NULL em tabela com dados;
   - DELETE ou UPDATE em massa;
   - renomear coluna.
4. **Script:** escreva `V<n>_DESCRICAO_EM_MAIUSCULAS.sql`.
   - PostgreSQL, idempotente (`IF NOT EXISTS` / `IF EXISTS`).
   - Índice para cada FK nova.
   - Uma linha de comentário com o objetivo.
   - Se o trabalho for grande, delegue ao subagente `especialista-banco`.
5. **Entidade:** se precisar, ajuste-a ao script (tipo, `nullable`, tamanho, constraint). O `update` do Hibernate não remove, não renomeia nem muda o tipo de colunas.
6. **Testes:** rode os que usam essas entidades (ache com Grep pelo nome) com o comando do CLAUDE.md. Se não houver nenhum, diga.
7. **Fechamento:** mostre o nome do script, o SQL e como aplicá-lo no banco de cada ambiente. Não execute nada no banco sem eu pedir.
