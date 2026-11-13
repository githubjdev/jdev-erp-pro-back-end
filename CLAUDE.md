# Jdev ERP Pro
Back-end REST de um ERP SaaS multiempresa: planos/empresas, pessoas, clientes/funcionários, categorias, produtos, movimentação de estoque, pedidos e chamados de suporte.
Java 25 · Spring Boot 4.0.6 · Spring Data JPA/Hibernate · Spring Security + JWT (jjwt 0.12.7) · PostgreSQL 18 · Maven (sem wrapper) · Lombok.

## Repositório
- Há dois projetos Maven independentes, sem POM pai:
  - `jdev-erp-pro-back-end-dev/`: onde se trabalha.
  - `jdev-erp-pro-back-end-pro/`: quase cópia do `-dev`. Só mexa nele quando eu pedir.
    - Os dois perfis do `-pro` usam o banco `jdev-erp-pro-prod`. Para rodar os testes do `-pro` no banco dev, defina `SPRING_DATASOURCE_URL` com o banco `jdev-erp-pro-dev`.
- Os caminhos abaixo são relativos a `jdev-erp-pro-back-end-dev/`, e o `mvn` roda lá.

## Comandos
- Sempre use `-Dmaven.compiler.proc=full`. Sem isso o Lombok não roda no JDK 25 e a compilação falha.
- Build: `mvn -Dmaven.compiler.proc=full -DskipTests compile`
- Testes: `mvn -Dmaven.compiler.proc=full test`
  - Um teste: `-Dtest=TestCategoriaRepository`. Um método: `-Dtest=TestCategoriaRepository#testbuscaPorNome`.
- **JDK 25:** o `JAVA_HOME` vem do `env` de `.claude/settings.local.json`. Se `mvn -v` mostrar Java 17, prefixe:
  - Bash: `JAVA_HOME="/c/Program Files/Java/jdk-25.0.3"`
  - PowerShell: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3";`
- **Banco dos testes:** o banco dev real (PostgreSQL 18, serviço `postgresql-x64-18`, porta 5433). A suíte inteira (1038 testes) leva cerca de 1 min 30 s.
  - Os testes de API (`TestContextoApi`) gravam de verdade: cada classe cria uma empresa de teste e apaga tudo dela no final.
  - Os testes do Flyway (`config/TestMigracoesFlyway`, `config/TestVerificacaoBaselineFlyway`, `app/TestJDevERPPROApplication`) usam bancos temporários `jdev_flyway_teste_*`, criados e apagados por `contexto/BancoTemporarioTeste`. Do banco dev, só leem.
- **PowerShell:** escreva `"-Dmaven.compiler.proc=full"` entre aspas, senão o PowerShell corta o argumento no ponto.
- **Rodar a API:**
  - Pela IDE, com a classe `br.com.jdeverp.pro.app.JDevERPPROApplication`.
  - O perfil padrão é `dev`. Para mudar, use `SPRING_PROFILES_JDEV_ERP_PRO`, não `SPRING_PROFILES_ACTIVE`.
  - URL: `http://localhost:8081/jdev-erp-pro-back-end-dev/api/...`
- **JAR:** `mvn -Dmaven.compiler.proc=full -DskipTests package` gera `target/jdev-erp-pro-back-end-dev-0.0.1-SNAPSHOT.jar`, que roda com `java -jar` (`docs/Flyway.md` §11).
  - A main tem de continuar `public static void main`: o launcher do Spring Boot chama a main como estática e não aceita a main de instância do Java 25.

## Estrutura (pacote base `br.com.jdeverp.pro`, organizado por camada)
- `controller/`, `service/`, `model/` (entidades), `dto/`, `enums/`
- `repository/`: a base é `JpaJdevRepository` / `JpaJdevRepositoryImpl`.
- `security/`: `SecurityConfig`, `JwtFilter`, `UsuarioAutenticado`.
- `exception/`: `GlobalExceptionHandler`, `MsgApiException`, `ResponseApi`.
- Também: `validator/RepositoryEmpresaValidator`, `anotacoes/IgnoreEmpresaId`, `config/`, `util/`, `app/JDevERPPROApplication`.
- **Testes:** `src/test/java/br/com/jdeverp/pro/`. Bases: `contexto/TestContextoRepository` (repository, com rollback e fábricas de registros; estende `contexto/TestContextoSpring`) e `contexto/TestContextoApi` (controller e autenticação, fluxo real). Services: Mockito, sem Spring. Migrações só de teste: `src/test/resources/db/migracao-teste/`.
- **Configuração:** `src/main/resources/`. O `application.properties` só escolhe o perfil. Os perfis são `application-dev.properties` e `application-prod.properties`.
- **Banco:** versionado pelo Flyway 11.14.1 (só no `-dev`; o `-pro` ainda não tem, ver P23). Detalhes em `docs/Flyway.md`.
  - As migrações ficam em `src/main/resources/db/migration/`, no padrão `VYYYYMMDD_HHMMSS__descricao.sql`, e rodam sozinhas na subida, antes do Hibernate.
  - Hoje: `V20261112_160000__criacao_estrutura_inicial`, `V20261112_161000__carga_inicial` (antigo `V1_INIT.sql`), `V20261112_162000__pessoa_documento_por_tipo` (antigo `V2_PESSOA_DOCUMENTO_POR_TIPO.sql`), `V20261112_220600__empresa_bloqueio_obrigatorio` e `V20261112_220700__usuario_liberado_obrigatorio`.
  - Migração nova entra também na lista `VERSOES` de `config/TestMigracoesFlyway` (o teste de nomes cobra).
  - Banco anterior ao Flyway recebe baseline automático, conferido antes por `config/VerificacaoBaselineFlyway`. O banco dev está em baseline `20261112.162000` desde 2026-11-12 e hoje na versão `20261112.220700`.
  - O Hibernate continua com `ddl-auto=update` (P22).

## Multiempresa (vale para todo o código)
- **Empresa nas entidades:** toda entidade de negócio tem `Empresa empresa` (coluna `empresa_id`). São globais: `Plano`, `Empresa`, `Role`, `RoleUsuario`.
- **Origem da empresa:** sempre `usuarioLogadoService.getEmpresaIdLogada()` ou `getEmpresaLogada()`, nunca o request.
- **Queries:** todo método de repository precisa de `@Query` com `empresa.id` ou `empresa_id`, senão a aplicação não sobe.
  - Derived query não passa.
  - A exceção é `@IgnoreEmpresaId(motivo = "...")`. Ela só pula a conferência do `empresa.id`: o validador não exige o `motivo` nem lê o `ignorar` (decidido manter assim). Escreva o `motivo` mesmo assim, por convenção.
- **Métodos bloqueados:** em entidade com empresa, `findAll()`, `findById()`, `count()`, `deleteById()`, `existsById(id)` e os deletes em lote do Spring Data (`deleteAll(lista)`, `deleteAllById`, `deleteAllInBatch`, `deleteAllByIdInBatch`) lançam exceção. Use no lugar:
  - `buscarPorId(id, empresaId)`
  - `listar`
  - `total`
  - `existsById(id, empresaId)`
  - `listarPaginado`
  - `deleteAll(empresaId)` e `deletarAllById(ids, empresaId)`, que recusam entidade global e empresa nula
- **Salvar:** o `save` não filtra por empresa. Antes de atualizar ou deletar, confirme com `buscarPorId(id, empresaId)` que o registro é da empresa logada.

## Segurança
- **Sessão:** JWT stateless. O token fica gravado em `usuario.token_sessao` e é conferido a cada requisição, então um login novo derruba o anterior.
- **Acesso:**
  - A única rota pública é `POST /api/usuario/login`.
  - Roles: `ROLE_USER` e `ROLE_ADMIN`. O `ROLE_ADMIN` é admin global, de todas as empresas, e é checado no service com `usuarioLogadoService.isAdmin()`.
- **Proteção do admin (P25):**
  - Usuário com `ROLE_ADMIN` nunca é apagado pela API, e um admin não bloqueia outro.
  - `ROLE_USER` e `ROLE_ADMIN` nunca são renomeadas nem apagadas: o código as procura pelo nome (`RoleService.ACESSOS_DO_SISTEMA`).
  - O admin logado não remove o próprio acesso: não se apaga, não se bloqueia, não mexe nas próprias associações em `RoleUsuario`, não renomeia nem apaga uma role que ele tem e não apaga nem bloqueia a própria empresa.
  - Bloquear outra empresa, mesmo com admins, é permitido: é como se corta um cliente do SaaS.
  - Toda operação nova que possa tirar o acesso de quem está logado segue a mesma regra: recusa com mensagem e instrução (`usuarioLogadoService.isUsuarioLogado(id)` e `possuiAcesso(roleId)`).

## Convenções gerais
- Nomes em português: classes, métodos, mensagens e comentários.
- Injeção com `@Autowired` em campo.
- **Entidade ou DTO:** o controller recebe a entidade (`@RequestBody @Valid`) e sempre devolve DTO, nunca a entidade. O service converte. Método usado por outro service continua devolvendo a entidade (ver `.claude/rules/api-controllers.md`).
- **Transação:** `spring.jpa.open-in-view=false`. Service com `@Transactional(readOnly = true)` na classe e `@Transactional` em todo método que grava; relacionamento LAZY só é lido dentro do service (ver `.claude/rules/services.md`).
- **Enum no JSON:** sempre pelo nome do enum, nunca pela descrição (`config/ConfiguracaoJackson`).
- **ObjectMapper:** onde o código precisar de um, injete o bean `util/ObjectMapperUtil` (`@Autowired`), nunca `new ObjectMapper()`. Ele já ignora propriedade desconhecida e grava datas em ISO.
  - Cuidado: `jsonParaHashMap` e `valorAtributo` guardam o último JSON lido no próprio bean, que é compartilhado entre requisições. Em código concorrente, prefira `jsonForMap`.
- **Erro de negócio:** `throw new MsgApiException("mensagem")` devolve 400. Para outro status, `new MsgApiException("mensagem", HttpStatus.FORBIDDEN)`.
  - O handler responde `ResponseApi {localDateTime, status, error, message, path}`.
- **Detalhes por camada:** ficam em `.claude/rules/` (controllers, services, entidades, segurança, testes). Cada regra carrega sozinha quando o arquivo correspondente é aberto.

## Regras que nunca podem ser quebradas
- **Migrações:** nunca altere uma migração existente em `src/main/resources/db/migration/`, nem num comentário: o checksum muda e a aplicação não sobe. Crie a próxima com `/nova-migration`.
  - Toda mudança de tabela, constraint ou seed vem acompanhada de uma migração nova.
  - Nunca use `clean`, `repair` automático, `out-of-order` nem desligue a validação do Flyway.
- **`ddl-auto`:** nunca use `update` nem `create` em perfil novo, nem mude o `ddl-auto` existente sem eu pedir. Hoje dev e prod usam `update`: é dívida conhecida.
- **Isolamento por empresa:** nunca quebre. `@IgnoreEmpresaId` só com `motivo` e com o meu aval.
- **Endpoint novo:** sempre com `@PreAuthorize` (P9), além do `isAdmin()` no service.
  - `isAuthenticated()` para dados da empresa logada.
  - `hasAuthority('ROLE_ADMIN')` para a área de administração: dados globais (`Plano`, `Role`, `Empresa`), atribuição de acesso (`RoleUsuario`) e cadastro, edição ou exclusão de usuários (P10).
- **Credenciais:** nunca commite.
  - Segredo novo vem de variável de ambiente, ex.: `jwt.secret=${JWT_SECRET}`.
  - `application-dev.properties` e `application-prod.properties` contêm segredos.
- **Pendências:** não implemente regra que esteja em aberto em `docs/pendencias.md` sem decisão minha.
- **Testes:** rode os testes afetados antes de dizer que terminou. Se não der, avise.
- **Git:** sem `git commit` nem `git push` sem eu pedir.

## Mais contexto (ler só quando a tarefa pedir)
- `docs/regras-negocio.md`: regras por módulo, com a classe e o método de cada uma. Leia antes de mudar um service.
- `docs/arquitetura.md`: módulos, relacionamentos, fluxo da requisição e login.
- `docs/analise-projeto.md`: build, dependências, configuração e dívidas técnicas.
- `docs/pendencias.md`: decisões em aberto, numeradas de P1 a P25.
- `docs/Flyway.md`: migrações, nomenclatura, baseline de banco existente, checksum, recuperação de falhas e JAR.
