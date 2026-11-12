# Flyway — versionamento do banco

Escopo: `jdev-erp-pro-back-end-dev/`. Implantado em 2026-11-12.
O `-pro` ainda não tem Flyway; ver [Pendências](#13-pendências).

## 1. O que é o Flyway no projeto
O Flyway aplica no PostgreSQL, em ordem, os scripts SQL versionados de `src/main/resources/db/migration/`. Cada script aplicado fica registrado na tabela `flyway_schema_history`, com checksum, data, duração e resultado. Um script nunca roda duas vezes no mesmo banco.

- **Quando roda:** sozinho, na subida da aplicação (IDE, testes ou `java -jar`), pelo Spring Boot (`spring-boot-starter-flyway`).
- **Ordem na subida:** conexão (Hikari) → Flyway (valida, faz baseline se for preciso, migra) → Hibernate → Tomcat.
- **Falha:** erro numa migração, na validação ou no baseline impede a aplicação de subir.
- **Versões:** Flyway 11.14.1 (`flyway-core` e `flyway-database-postgresql`), gerenciado pelo Spring Boot 4.0.6. Suporta o PostgreSQL 18.
- **Hibernate:** o `ddl-auto=update` continua ligado (dívida conhecida, P22). Ele roda depois do Flyway e, como as migrações já criam tudo o que as entidades pedem, não muda nada. O teste `TestJDevERPPROApplication.testMainBancoVazio` confere isso.

## 2. Onde ficam os scripts
| Onde | Caminho |
|---|---|
| Código-fonte | `jdev-erp-pro-back-end-dev/src/main/resources/db/migration/` |
| Dentro do JAR | `BOOT-INF/classes/db/migration/` |
| Configuração | `spring.flyway.locations=classpath:db/migration` |

O Flyway lê os scripts do classpath, sem caminho absoluto nem pasta externa, então funciona igual no Windows e no Linux. O checksum ignora a quebra de linha (CRLF ou LF).

A pasta antiga `src/main/webapp/db_sql_versionamento/` não existe mais. Os dois scripts dela foram movidos com `git mv`, e o histórico continua no git.

### Migrações atuais
| Versão | Arquivo | Origem | O que faz |
|---|---|---|---|
| `20261112.160000` | `V20261112_160000__criacao_estrutura_inicial.sql` | nova | Cria sequences, tabelas, PK, unique, check e FK. |
| `20261112.161000` | `V20261112_161000__carga_inicial.sql` | `V1_INIT.sql` | Carga inicial: planos, empresa 1, admin, 30 categorias, roles. Também ajusta o CHECK do plano, a `unaccent` e as colunas do usuário. |
| `20261112.162000` | `V20261112_162000__pessoa_documento_por_tipo.sql` | `V2_PESSOA_DOCUMENTO_POR_TIPO.sql` | CPF e CNPJ por tipo de pessoa e e-mail obrigatório (P13). |

**Por que existe a migração de estrutura (nova):**
- Antes, quem criava as tabelas era o Hibernate, e o `V1_INIT` rodava à mão depois.
- O Flyway roda antes do Hibernate. Sem essa migração, a carga inicial falharia num banco vazio (`relation "plano" does not exist`).
- Ela é o `pg_dump --schema-only` do banco dev, com os nomes de constraint gerados pelo Hibernate.
- Para o `V1_INIT` rodar sem alteração, ela traz o estado que ele espera encontrar:
  - `usuario.bloqueado` no lugar de `liberado`, porque o V1 insere em `bloqueado`, apaga a coluna e cria `liberado`;
  - sem o `plano_valor_mensal_check` e sem a `unaccent`, que o V1 cria;
  - sem os três CHECKs de pessoa, que o V2 cria.
- Depois das três migrações, a estrutura fica igual à do banco dev (conferido por `pg_dump` e pelo teste `testMigrarProduzEstruturaDoBancoDev`).

**O que mudou nos scripts antigos.** Só o necessário; os cabeçalhos `[Flyway]` de cada arquivo registram as mudanças.
- **Carga inicial:**
  - Entrou uma trava no início: se `plano`, `empresa` ou `pessoa` tiverem dados, a migração para com erro.
  - Motivo: o V1 apaga essas tabelas com as FKs desligadas. Antes ele era rodado à mão; agora roda sozinho.
  - Nenhum comando do V1 foi alterado ou removido.
- **Pessoa documento por tipo:**
  - Saíram o `BEGIN;` e o `COMMIT;`, porque o Flyway já executa cada migração numa transação.
  - O resto ficou igual.

## 3. Padrão de nomenclatura
```
VYYYYMMDD_HHMMSS__descricao_em_minusculas.sql
V20261113_090000__adicionar_indice_pedido.sql
```
- `V` maiúsculo.
- Data e hora completas: são a versão. O Flyway troca `_` por `.`, e `V20261113_090000` vira a versão `20261113.090000`.
- **Dois** underscores antes da descrição.
- Descrição em minúsculas, sem acento nem espaço, palavras separadas por `_`.
- Use a data e a hora em que criou o script. A versão não pode repetir e tem de ser maior que a última aplicada (`out-of-order=false`).
- Não use mais `V1`, `V2`, `V3`.

**Validação dos nomes:**
- `spring.flyway.validate-migration-naming=true`: um arquivo fora do padrão do Flyway impede a subida, em vez de ser ignorado em silêncio.
- O teste `TestMigracoesFlyway.testNomesDasMigracoes` exige o padrão completo acima, com data válida e versão única.

## 4. Como criar uma migração nova
1. Crie o arquivo em `src/main/resources/db/migration/` com o nome do item 3. A skill `/nova-migration` faz isso.
2. Uma alteração por arquivo, pequena e objetiva. Nada de juntar mudanças independentes num arquivo grande.
3. SQL do PostgreSQL. Prefira idempotente (`IF NOT EXISTS` / `IF EXISTS`).
   - Com `ddl-auto=update`, se a entidade mudar e a aplicação subir antes de a migração existir, o Hibernate cria a coluna no banco dev, e um `ADD COLUMN` sem `IF NOT EXISTS` quebraria ali.
   - Melhor ainda: escreva a migração antes de subir a aplicação com a entidade nova.
4. Índice em toda FK nova. Respeite a ordem das dependências: tabela antes da FK que aponta para ela.
5. **Mudança destrutiva** (DROP, mudança de tipo, NOT NULL em tabela com dados, DELETE/UPDATE em massa, renomear coluna):
   - pare e combine antes;
   - planeje em etapas: criar, copiar, trocar e só depois remover.
6. **Tabela grande:** comece com `SET lock_timeout = '5s';` para não travar a aplicação esperando lock. Evite reescrever a tabela inteira (ex.: `ALTER COLUMN TYPE`).
7. **`CREATE INDEX CONCURRENTLY`:** não roda dentro de transação.
   - Coloque-o sozinho numa migração própria. O Flyway detecta e executa essa migração fora de transação.
   - Para forçar, crie ao lado o arquivo `V..._nome.sql.conf` com `executeInTransaction=false`.
   - Não misture com outras instruções (`spring.flyway.mixed=false`).
   - Se falhar, pode sobrar um índice `INVALID`: veja a seção 10.
8. Rode os testes:
   ```bash
   mvn -Dmaven.compiler.proc=full -Dtest='TestMigracoesFlyway,TestVerificacaoBaselineFlyway,TestJDevERPPROApplication' test
   ```
   - O `testMigrarProduzEstruturaDoBancoDev` compara o resultado das migrações com o banco dev.
   - Se a entidade mudou sem migração, ele falha e mostra a diferença.
9. **Nunca edite** uma migração que já rodou em algum banco. Para corrigir, crie outra migração.

## 5. Como executar a aplicação
Não há passo manual: as migrações rodam na subida.

| Como | Comando |
|---|---|
| IDE | Rodar `br.com.jdeverp.pro.app.JDevERPPROApplication`. |
| Testes (Maven) | `mvn -Dmaven.compiler.proc=full test` |
| JAR | ver a seção 11 |

- **Perfil:** `SPRING_PROFILES_JDEV_ERP_PRO` (padrão `dev`). A configuração do Flyway é a mesma nos dois perfis.
- **Sobrescrever qualquer propriedade:**
  - por variável de ambiente, ex.: `SPRING_FLYWAY_BASELINE_VERSION`, `SPRING_DATASOURCE_URL`;
  - por argumento, ex.: `--spring.flyway.baseline-version=...`.

**Log da subida** (nível `info`; são poucas linhas por subida):
```
FlywayExecutor - Database: jdbc:postgresql://localhost:5433/jdev-erp-pro-dev?... (PostgreSQL 18.1)
DbValidate     - Successfully validated 3 migrations
DbMigrate      - Current version of schema "public": 20261112.162000
DbMigrate      - Schema "public" is up to date. No migration necessary.
```
Com migração pendente aparecem `Migrating schema "public" to version ...` e `Successfully applied N migrations ...`. Em caso de erro, a pilha mostra `FlywayMigrateException`, `FlywayValidateException` ou `Baseline do Flyway recusado`.

## 6. Como verificar o histórico
```sql
SELECT installed_rank, version, description, type, script, checksum,
       installed_by, installed_on, execution_time, success
  FROM flyway_schema_history
 ORDER BY installed_rank;
```

| Coluna | O que mostra |
|---|---|
| `type` | `SQL` (migração executada) ou `BASELINE` (banco adotado) |
| `success` | `false` = falhou (ver a seção 10) |
| `checksum` | CRC do arquivo no momento da execução |
| `execution_time` | duração, em ms |

- **Versão atual:** `SELECT max(version) FROM flyway_schema_history WHERE success;`. Também aparece no log, em `Current version of schema`.
- **Pendentes:** os arquivos de `db/migration` com versão maior que a atual. Na subida, o log diz se aplicou alguma ou se o banco já estava em dia.

Pelo `psql`:
```bash
psql -h localhost -p 5433 -U postgres -d jdev-erp-pro-dev -c "SELECT installed_rank, version, type, success, installed_on FROM flyway_schema_history ORDER BY 1"
```

## 7. Banco novo (vazio)
1. O Flyway vê o schema `public` vazio. O log diz `All configured schemas are empty; baseline operation skipped`.
2. Cria a `flyway_schema_history`.
3. Aplica as três migrações, cada uma numa transação, e registra cada uma no histórico.
4. O Hibernate sobe e não precisa criar nada.

**Requisitos:**
- **Usuário do banco:** a carga inicial usa `SET session_replication_role`, que exige superusuário (ou `GRANT SET ON PARAMETER session_replication_role TO <usuario>`, no PostgreSQL 15+). No dev, a aplicação conecta como `postgres`.
- **Admin da carga:** a carga inicial cria o admin `alex.fernando.egidio@gmail.com`, com senha de teste conhecida (está no próprio script). Num banco novo de produção, troque essa senha logo depois da primeira subida.

## 8. Banco existente (anterior ao Flyway)
É um banco com tabelas e sem a `flyway_schema_history`: o Hibernate criou as tabelas e os scripts antigos rodaram à mão. Nele, o Flyway **não pode** executar as migrações antigas: a estrutura já existe, e a carga inicial apagaria dados.

### Estratégia: baseline automático, conferido antes de gravar
```properties
spring.flyway.baseline-on-migrate=true
spring.flyway.baseline-version=20261112.162000
```
Na primeira subida num banco existente:
1. O Flyway vê o schema não vazio e sem histórico, e vai fazer o baseline.
2. **Antes de gravar**, o callback `config/VerificacaoBaselineFlyway` (evento `BEFORE_BASELINE`) confere se o banco está mesmo no estado da versão de baseline:
   - estrutura: as 14 tabelas e as 14 sequences;
   - `V1_INIT`:
     - `plano_valor_mensal_check` com a regra do FREE;
     - extensão `unaccent`;
     - `usuario.liberado` presente e `bloqueado` ausente;
     - `refresh_token` e `token_sessao` do tipo `text`;
     - `cliente_funcionario.usuario_id` aceitando nulo;
     - `ROLE_USER` e `ROLE_ADMIN` cadastradas;
   - `V2_PESSOA_DOCUMENTO_POR_TIPO`: `cpf` e `cnpj` aceitando nulo, `email` NOT NULL e os três CHECKs de pessoa.
3. **Se bate**, grava uma linha `BASELINE` na versão `20261112.162000` e não executa nenhuma migração até ela. Migrações posteriores rodam normalmente.
4. **Se não bate**, recusa:
   - não grava histórico, não executa nada, e a aplicação não sobe;
   - a mensagem lista cada divergência e o que fazer.

Banco vazio não passa por essa conferência. Nas subidas seguintes também não, porque o histórico já existe.

### Versões de baseline aceitas
| Situação do banco | Versão | Efeito |
|---|---|---|
| `V1_INIT` e `V2` aplicados à mão (ex.: o dev) | `20261112.162000` (padrão) | Nada é executado. |
| Só o `V1_INIT` aplicado (ex.: `jdev-erp-pro-prod` hoje) | `20261112.161000` | O Flyway aplica a `V20261112_162000` logo depois. |
| Qualquer outra | — | Recusada. Abaixo, recriaria tabelas e rodaria a carga destrutiva; acima, pularia migrações sem conferir. |

Para usar a `161000`, defina-a só na **primeira** subida desse banco:
```powershell
$env:SPRING_FLYWAY_BASELINE_VERSION = "20261112.161000"; java -jar target\jdev-erp-pro-back-end-dev-0.0.1-SNAPSHOT.jar
```
```bash
SPRING_FLYWAY_BASELINE_VERSION=20261112.161000 java -jar target/jdev-erp-pro-back-end-dev-0.0.1-SNAPSHOT.jar
```

### Conferência completa antes de adotar um banco importante
A conferência automática olha o que os scripts antigos mudam. Para provar a equivalência completa, compare a estrutura do banco com a de um banco criado só pelas migrações:
```bash
createdb -h localhost -p 5433 -U postgres -T template0 jdev_flyway_conferencia
# suba a aplicação apontando para ele (--spring.datasource.url=...) ou rode TestMigracoesFlyway
pg_dump -h localhost -p 5433 -U postgres --schema-only --no-owner --no-privileges -T flyway_schema_history -d jdev_flyway_conferencia > novo.sql
pg_dump -h localhost -p 5433 -U postgres --schema-only --no-owner --no-privileges -T flyway_schema_history -d <banco_existente> > existente.sql
diff novo.sql existente.sql      # ignore só as linhas \restrict / \unrestrict
dropdb -h localhost -p 5433 -U postgres jdev_flyway_conferencia
```
Se houver diferença, resolva com uma migração nova depois do baseline, ou documente a diferença antes de adotar.

### Situação em 2026-11-12
- **`jdev-erp-pro-dev`:**
  - Adotado: `BASELINE` em `20261112.162000`, gravado na primeira subida dos testes, depois da conferência.
  - Antes disso, o mesmo processo foi validado numa cópia integral do banco (estrutura e dados): nenhum dado ou objeto mudou.
- **`jdev-erp-pro-prod`:** não adotado (o `-pro` não tem Flyway). Diferenças encontradas no `pg_dump` (só leitura):
  - **sem o V2** (P13): precisa de baseline em `20261112.161000`;
  - **duas uniques a mais, sobras de versões antigas das entidades:**
    - `unique_sku` (SKU único no banco inteiro, contra o `unique_sku_empresa` da entidade);
    - `unique_inscricao_estadual` (duplicata da `unique_incricao_estadual`).
  - O baseline aceita essas sobras, porque não são o que os scripts antigos mudam, mas elas são uma divergência real: a `unique_sku` impede duas empresas de usarem o mesmo SKU. Decidir se saem por migração (P23).
  - Com o baseline padrão, uma cópia do prod foi recusada, como esperado: não subiu e nada foi gravado.

## 9. Erro de checksum
**Sintoma:** a aplicação não sobe com
```
Validate failed: Migrations have failed validation
Migration checksum mismatch for migration version 20261112.162000
-> Applied to database : 1029765891
-> Resolved locally    : -123456789
```
**Causa:** o arquivo de uma migração já aplicada foi alterado, mesmo que só num comentário.

**O que fazer, em ordem de preferência:**
1. **Desfazer a alteração no arquivo** (`git checkout -- <arquivo>` ou `git log -p <arquivo>`). Se a mudança era necessária, crie uma migração nova com ela. Isso resolve quase todos os casos.
2. **A alteração tem de ficar** (ex.: comentário já distribuído): atualize o checksum com o `repair` do Flyway, **à mão**, no banco afetado.
   - O projeto não tem o Flyway CLI nem o plugin Maven configurados; use o Flyway CLI (Community) 11.14.1, a mesma versão do projeto:
     ```bash
     flyway -url="jdbc:postgresql://localhost:5433/jdev-erp-pro-dev" -user=postgres -password=*** \
            -locations="filesystem:src/main/resources/db/migration" info     # confira antes
     flyway -url=... -user=... -password=... -locations="filesystem:src/main/resources/db/migration" repair
     ```
   - O `repair` só atualiza checksum e descrição, e remove linhas com falha. Não executa nenhum SQL das migrações.

Nunca:
- desligue o `validate-on-migrate`;
- coloque `repair` na subida;
- edite a `flyway_schema_history` sem entender o efeito.

## 10. Falha numa migração e recuperação
- **Migração comum (transacional):**
  - O PostgreSQL desfaz a migração inteira, e ela não entra no histórico. O banco fica como estava.
  - Corrija o arquivo, que nunca chegou a ser aplicado, e suba de novo.
  - Coberto por `TestMigracoesFlyway.testMigrarMigracaoInvalida` e `TestJDevERPPROApplication.testMainMigracaoInvalidaImpedeSubida`.
- **Migração não transacional (`CONCURRENTLY`):**
  - Pode deixar resto (ex.: índice `INVALID`) e uma linha `success = false` no histórico.
  - Para recuperar:
    1. Remova o resto, ex.: `DROP INDEX CONCURRENTLY IF EXISTS nome_do_indice;`.
    2. Rode `flyway repair` (seção 9) para tirar a linha com falha.
    3. Corrija o script e suba.
- **Carga inicial recusada** (`Carga inicial recusada: plano, empresa ou pessoa já têm dados`): um banco com dados chegou à carga inicial sem baseline. Não force; veja a seção 8.
- **Baseline recusado:** leia a lista de divergências na mensagem. Ou o banco precisa de outra versão de baseline (tabela da seção 8), ou de ajuste manual combinado antes.
- **Antes de mexer em produção:** faça backup (`pg_dump -Fc`).

## 11. Gerar e executar o JAR
```bash
# Bash (Git Bash): JDK 25
JAVA_HOME="/c/Program Files/Java/jdk-25.0.3" mvn -Dmaven.compiler.proc=full -DskipTests package
```
```powershell
# PowerShell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"; mvn "-Dmaven.compiler.proc=full" -DskipTests package
```
Conferir os scripts dentro do JAR:
```bash
jar tf target/jdev-erp-pro-back-end-dev-0.0.1-SNAPSHOT.jar | grep db/migration
# BOOT-INF/classes/db/migration/V20261112_160000__criacao_estrutura_inicial.sql
# BOOT-INF/classes/db/migration/V20261112_161000__carga_inicial.sql
# BOOT-INF/classes/db/migration/V20261112_162000__pessoa_documento_por_tipo.sql
```
Executar:
```bash
java -jar target/jdev-erp-pro-back-end-dev-0.0.1-SNAPSHOT.jar
java -jar target/jdev-erp-pro-back-end-dev-0.0.1-SNAPSHOT.jar --spring.datasource.url="jdbc:postgresql://host:5432/banco" --server.port=8081
SPRING_PROFILES_JDEV_ERP_PRO=prod java -jar target/jdev-erp-pro-back-end-dev-0.0.1-SNAPSHOT.jar
```
- **Main:** o JAR (e o `spring-boot:run`) só funciona porque a main passou a `public static void main`.
- **Por quê:** o `JarLauncher` do Spring Boot 4.0.6 chama a main por reflexão como método estático, então a main de instância do Java 25 (`void main`) não serve.

## 12. Cuidados com produção
- **Antes da primeira subida com Flyway:** faça backup (`pg_dump -Fc`) e defina a versão de baseline certa para aquele banco (seção 8).
- **Teste antes:** rode as migrações novas numa cópia do banco de produção.
- **Segurança:**
  - `clean-disabled=true`: o Flyway nunca apaga o schema;
  - não há `repair` automático;
  - `validate-on-migrate=true` e `out-of-order=false`.
- **Várias instâncias:** subidas simultâneas são seguras. O Flyway usa advisory lock do PostgreSQL, e só uma instância migra.
- **Migração longa ou com lock** atrasa a subida. Use `lock_timeout` e, se for preciso, `CONCURRENTLY` (seção 4).
- **Usuário do banco:** se a aplicação de produção usar um usuário sem superusuário, só a carga inicial (banco novo) precisa do privilégio extra da seção 7.
- **Logs:** `logging.level.org.flywaydb=info`, poucas linhas por subida. Não use `debug` em produção.

## 13. Pendências
Registradas em `docs/pendencias.md`:
- **P22:** trocar `ddl-auto=update` por `validate` agora que o Flyway cria a estrutura. Não mudado sem decisão.
- **P23:** adotar o Flyway no `-pro`, com baseline `20261112.161000` no `jdev-erp-pro-prod`, e decidir sobre as uniques `unique_sku` e `unique_inscricao_estadual` que só existem lá.
- **P24:** senha de teste do admin da carga inicial num banco novo de produção.

## 14. Testes automatizados
Rodam em bancos temporários (`jdev_flyway_teste_*`), criados e apagados pelos próprios testes (`contexto/BancoTemporarioTeste`) no mesmo PostgreSQL do perfil.
- Sem Docker na máquina, não há Testcontainers.
- O banco dev só é lido, para comparar a estrutura.

| Teste pedido | Onde |
|---|---|
| 1. Banco vazio | `TestMigracoesFlyway.testMigrarBancoVazio`, `TestJDevERPPROApplication.testMainBancoVazio` |
| 2. Banco existente | `TestVerificacaoBaselineFlyway` (7 casos, incluindo recusas), `TestJDevERPPROApplication.testMainBancoExistente` e `testMainBancoExistenteDivergenteNaoSobe`, `TestMigracoesFlyway.testMigrarProduzEstruturaDoBancoDev` |
| 3. Segunda inicialização | `TestJDevERPPROApplication.testMainSegundaInicializacao` |
| 4. Nova migração | `TestMigracoesFlyway.testMigrarNovaMigracao` (`src/test/resources/db/migracao-teste/nova`) |
| 5. Checksum | `TestMigracoesFlyway.testValidarChecksumAlterado` |
| 6. Migração inválida | `TestMigracoesFlyway.testMigrarMigracaoInvalida`, `TestJDevERPPROApplication.testMainMigracaoInvalidaImpedeSubida` |
| 7. JAR | `TestMigracoesFlyway.testNomesDasMigracoes` (classpath) + `jar tf` e `java -jar` (seção 11) |
| 8. Reexecução | `TestMigracoesFlyway.testMigrarDeNovoNaoRepete`, `TestJDevERPPROApplication.testMainSegundaInicializacao` |
| Trava da carga inicial | `TestMigracoesFlyway.testMigrarCargaInicialRecusaBancoComDados` |
