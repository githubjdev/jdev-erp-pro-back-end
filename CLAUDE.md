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
- **Banco dos testes:** o banco dev real (PostgreSQL 18, serviço `postgresql-x64-18`, porta 5433). A suíte inteira leva cerca de 40 s.
  - Os testes de API (`TestContextoApi`) gravam de verdade: cada classe cria uma empresa de teste e apaga tudo dela no final.
- **PowerShell:** escreva `"-Dmaven.compiler.proc=full"` entre aspas, senão o PowerShell corta o argumento no ponto.
- **Rodar a API:**
  - Pela IDE, com a classe `br.com.jdeverp.pro.app.JDevERPPROApplication`.
  - O perfil padrão é `dev`. Para mudar, use `SPRING_PROFILES_JDEV_ERP_PRO`, não `SPRING_PROFILES_ACTIVE`.
  - URL: `http://localhost:8081/jdev-erp-pro-back-end-dev/api/...`
- Não tente `mvn package` nem `spring-boot:run`: falham com "Unable to find main class", porque a main é `void main`, não `public static`.

## Estrutura (pacote base `br.com.jdeverp.pro`, organizado por camada)
- `controller/`, `service/`, `model/` (entidades), `dto/`, `enums/`
- `repository/`: a base é `JpaJdevRepository` / `JpaJdevRepositoryImpl`.
- `security/`: `SecurityConfig`, `JwtFilter`, `UsuarioAutenticado`.
- `exception/`: `GlobalExceptionHandler`, `MsgApiException`, `ResponseApi`.
- Também: `validator/RepositoryEmpresaValidator`, `anotacoes/IgnoreEmpresaId`, `config/`, `util/`, `app/JDevERPPROApplication`.
- **Testes:** `src/test/java/br/com/jdeverp/pro/`. Bases: `contexto/TestContextoSpring` (repository, com rollback) e `contexto/TestContextoApi` (controller e autenticação, fluxo real). Services: Mockito, sem Spring.
- **Configuração:** `src/main/resources/`. O `application.properties` só escolhe o perfil. Os perfis são `application-dev.properties` e `application-prod.properties`.
- **Banco:** não há Flyway nem Liquibase.
  - O schema é gerado pelo Hibernate (`ddl-auto=update`).
  - Os scripts manuais ficam em `src/main/webapp/db_sql_versionamento/`. Hoje: `V1_INIT.sql` (seed destrutivo) e `V2_PESSOA_DOCUMENTO_POR_TIPO.sql` (aplicado no dev; prod pendente, ver P13).

## Multiempresa (vale para todo o código)
- **Empresa nas entidades:** toda entidade de negócio tem `Empresa empresa` (coluna `empresa_id`). São globais: `Plano`, `Empresa`, `Role`, `RoleUsuario`.
- **Origem da empresa:** sempre `usuarioLogadoService.getEmpresaIdLogada()` ou `getEmpresaLogada()`, nunca o request.
- **Queries:** todo método de repository precisa de `@Query` com `empresa.id` ou `empresa_id`, senão a aplicação não sobe.
  - Derived query não passa.
  - A exceção é `@IgnoreEmpresaId(motivo = "...")`.
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

## Convenções gerais
- Nomes em português: classes, métodos, mensagens e comentários.
- Injeção com `@Autowired` em campo.
- **Entidade ou DTO:** o controller recebe a entidade (`@RequestBody @Valid`) e sempre devolve DTO, nunca a entidade. O service converte. Método usado por outro service continua devolvendo a entidade (ver `.claude/rules/api-controllers.md`).
- **Transação:** `spring.jpa.open-in-view=false`. Service com `@Transactional(readOnly = true)` na classe e `@Transactional` em todo método que grava; relacionamento LAZY só é lido dentro do service (ver `.claude/rules/services.md`).
- **Enum no JSON:** sempre pelo nome do enum, nunca pela descrição (`config/ConfiguracaoJackson`).
- **Erro de negócio:** `throw new MsgApiException("mensagem")` devolve 400. Para outro status, `new MsgApiException("mensagem", HttpStatus.FORBIDDEN)`.
  - O handler responde `ResponseApi {localDateTime, status, error, message, path}`.
- **Detalhes por camada:** ficam em `.claude/rules/` (controllers, services, entidades, segurança, testes). Cada regra carrega sozinha quando o arquivo correspondente é aberto.

## Regras que nunca podem ser quebradas
- **Scripts SQL:** nunca altere um script existente em `db_sql_versionamento/`; crie o próximo, `V2_...sql`, com `/nova-migration`.
  - Toda mudança de tabela, constraint ou seed vem acompanhada de um script novo.
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
- `docs/pendencias.md`: decisões em aberto, numeradas de P1 a P21.
