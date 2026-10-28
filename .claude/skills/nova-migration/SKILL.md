---
name: nova-migration
description: Cria o próximo script SQL versionado do PostgreSQL em db_sql_versionamento, coerente com as entidades JPA (o projeto não usa Flyway nem Liquibase). Use para mudanças de tabela, coluna, constraint, índice ou dados de seed.
argument-hint: "o que o script deve fazer (ex.: criar índices em empresa_id nas tabelas de venda)"
---
Crie um script SQL para: $ARGUMENTS

Contexto:
- Não há ferramenta de migration: o schema vem do Hibernate (`ddl-auto=update`).
- Os scripts ficam em `jdev-erp-pro-back-end-dev/src/main/webapp/db_sql_versionamento/` e são aplicados à mão.
- Siga a regra de scripts do `CLAUDE.md` e `.claude/rules/jpa-entidades.md`.

1. **Versão:** liste os scripts (`V*_*.sql`) e defina a próxima. Depois de `V1_INIT.sql` vem `V2_`. Nunca edite um script existente.
2. **Leitura:** leia as entidades afetadas em `model/` e seus repositories. Confira os nomes reais de tabela, coluna, FK (`xxx_fk`), unique (`unique_...`) e sequence (`seq_` + tabela).
3. **Destrutivo? Pare.** DROP, mudança de tipo, NOT NULL em tabela com dados, DELETE ou UPDATE em massa e renomear coluna contam como destrutivos. Explique o risco, o efeito nos dados e um plano em etapas, e espere meu OK.
4. **Script:** escreva `V<n>_DESCRICAO_EM_MAIUSCULAS.sql`.
   - PostgreSQL idempotente (`IF NOT EXISTS` / `IF EXISTS`).
   - Índice para cada FK nova e uma linha de comentário com o objetivo.
   - Trabalho grande: delegue ao subagente `especialista-banco`.
5. **Entidade:** se precisar, ajuste-a para ficar igual ao script (tipo, `nullable`, tamanho, constraint). O `ddl-auto=update` não remove, não renomeia nem muda tipo de coluna.
6. **Testes:** rode os que usam essas entidades, dentro de `jdev-erp-pro-back-end-dev/`:
   `JAVA_HOME="/c/Program Files/Java/jdk-25.0.3" mvn -Dmaven.compiler.proc=full -Dtest=TestX test`
   Ache os testes com Grep pelo nome da entidade. Se não houver nenhum, diga.
7. **Fechamento:** mostre o nome do script, o SQL e como aplicá-lo (ele não roda sozinho: precisa ser executado no banco de cada ambiente). Não execute nada no banco sem eu pedir.
