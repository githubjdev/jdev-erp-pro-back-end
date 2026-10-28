# Análise técnica — Jdev ERP Pro (back-end)

Escopo: `jdev-erp-pro-back-end-dev/`. Atualizada em 2026-10-28.
Este documento traz só o que não está em outro lugar:
- convenções do dia a dia: `CLAUDE.md` e `.claude/rules/`;
- estrutura, módulos e segurança: `docs/arquitetura.md`;
- dúvidas em aberto: `docs/pendencias.md` (Pn).

## 1. Build
- **Maven sem wrapper.** O Maven 3.9.11 está em `C:\apache-maven-3.9.11`.
- **Versões:** Java 25 (`java.version` e `maven.compiler.source/target`) e Spring Boot 4.0.6 (parent). Packaging `jar`.
- **Repositório:** dois projetos Maven independentes, sem POM pai: `-dev` e `-pro` (P2).
- **`JAVA_HOME`:**
  - O da máquina aponta para o JDK 17; o JDK 25 está em `C:\Program Files\Java\jdk-25.0.3`.
  - Para o Claude, o JDK 25 vem do `env` de `.claude/settings.local.json`, que só vale a partir da sessão seguinte.
- **Lombok:** sem `-Dmaven.compiler.proc=full`, a compilação dá 200 erros `cannot find symbol`. A partir do JDK 23 o annotation processing implícito vem desligado, e o `pom.xml` não configura `annotationProcessorPaths`.
- **Main de instância:** `mvn package` e `spring-boot:run` falham com `Unable to find main class`, porque a main é `void main(String[] args)`, uma main de instância.
- **Como roda hoje:** a aplicação sobe pela IDE (P1).

## 2. Dependências
- **Starters:** `data-jpa`, `security`, `webmvc`, `validation`, mais os starters de teste correspondentes. Com eles vêm JUnit 5, Mockito, AssertJ e spring-security-test.
- **Outras:**
  - `postgresql`;
  - Lombok;
  - `jjwt` 0.12.7;
  - `commons-collections4` 4.5.0;
  - `jackson-datatype-jsr310` **2.13.5 fixo**: é Jackson 2, enquanto o Boot 4 usa Jackson 3. O `ObjectMapperUtil` usa `com.fasterxml.jackson` (P8);
  - `HikariCP` **4.0.3 fixo**, numa alteração ainda não commitada: sobrescreve a versão gerenciada pelo Boot e já viria pelo starter.
- **Não usa:** MapStruct, Flyway, Liquibase, springdoc, Testcontainers, H2, devtools, spring-session.
- **Funções SQL no Hibernate:** `config/PostgreSQLFunctionContributor` registra `unaccent`, `similarity`, `word_similarity` e `strict_word_similarity`.
  - `unaccent` está ativa no banco dev.
  - Nenhuma query usa as funções de `pg_trgm`.

## 3. Pacote e camadas
Base `br.com.jdeverp.pro`, organizada por camada. Ver `CLAUDE.md` (Estrutura) e `docs/arquitetura.md` §2 e §4.
- **Fora do scan:** `br.jdev.estoque.erp.multitanent.EmpresaContext` está fora do `@ComponentScan`, não vira bean e não é usada.

## 4. Módulos
Ver `docs/arquitetura.md` §2 e §3: 6 módulos, 14 controllers e 202 endpoints.

## 5. Banco e configuração
- **`application.properties`:** só `spring.profiles.active=${SPRING_PROFILES_JDEV_ERP_PRO:dev}`. A variável está documentada em `documentação/1 - Documentação`.
- **Perfis:**
  - `application-dev.properties` e `application-prod.properties` ainda não estão commitados e têm segredos em texto.
  - O prod é uma cópia do dev (P3).
  - Não existe perfil `test`.
- **`ddl-auto=update` nos dois perfis.** O schema vem do Hibernate.
  - Os scripts manuais ficam em `src/main/webapp/db_sql_versionamento/`, no padrão `V<n>_DESCRICAO.sql`. Hoje só existe `V1_INIT.sql` (P4).
  - O `V1_INIT.sql` é um seed destrutivo:
    - desliga triggers e apaga plano, empresa e pessoa;
    - recria o CHECK `plano_valor_mensal_check`;
    - insere 2 planos, a empresa 1, 30 categorias, `ROLE_USER` e `ROLE_ADMIN`.
- **Hikari e JPA:** pool de até 15 conexões, `auto-commit=false`, `open-in-view=true`.
- **Banco local:** PostgreSQL 18, serviço `postgresql-x64-18`, porta 5433. Também existe um serviço `postgresql-9.5` (P5).

## 6. Segurança
Ver `docs/arquitetura.md` §5 e o `CLAUDE.md`. O ponto específico desta análise:
- `spring.security.user.name/password` estão definidos, mas provavelmente sem efeito, porque o projeto já tem um `UserDetailsService` próprio (P6).

## 7. Padrões de código
Ver o `CLAUDE.md` (Convenções) e `.claude/rules/`.

## 8. Testes
- **Existentes:**
  - `contexto/TestContextoSpring`: classe base, com `@SpringBootTest`, `@AutoConfigureMockMvc(addFilters = false)`, `@Transactional` e `@TestPropertySource("classpath:application.properties")`;
  - `repository/TestCategoriaRepository`: 7 testes próprios mais o herdado.
- **Resultado:** `mvn -Dmaven.compiler.proc=full test` roda 9 testes em cerca de 17 s, com `BUILD SUCCESS` (2026-10-28).
- **Banco:** os testes usam o banco dev real e o seed (empresa 1). Não há `src/test/resources`.

## 9. Comandos
Ver `CLAUDE.md` (Comandos).

## 10. Pontos de atenção (dívidas registradas, não corrigidas)

**Segurança e multiempresa**
- **`CategoriaService.atualizar` sem checagem de empresa:** salva sem confirmar o id na empresa logada, então dá para sobrescrever a categoria de outra empresa.
- **Injeção de JPQL:** `JpaJdevRepositoryImpl.listarPaginado` concatena o `sort` do request no `order by`.
- **Escalada de privilégio:** `PlanoService`, `RoleService` e `RoleUsuarioService` não exigem admin, então qualquer usuário pode se dar `ROLE_ADMIN` (P10). O `UsuarioService.deleteAll` também não exige admin.
- **`/api/admin/**` sem uso:** protegido no `SecurityConfig`, mas nenhuma rota usa esse prefixo. O `@EnableMethodSecurity` está ligado e não é usado.
- **CORS aberto:** `allowedOriginPattern("*")` com `allowCredentials=true`.
- **Segredos expostos:**
  - em texto nos `.properties` e no histórico do git (`application.properties` até o commit `aed3214`);
  - há uma senha em comentário em `JDevERPPROApplication.configTimeZone()`.
- **Erro no `JwtFilter`:** token inválido lança `MsgApiException` (400), e o filtro relança a exceção fora do `GlobalExceptionHandler` (P7).

**Banco e configuração**
- **`ddl-auto=update` em prod:** sem ferramenta de migration.
- **Prod igual ao dev:** o `application-prod.properties` é cópia do dev.
- **Propriedades obsoletas ou sem efeito:**
  - `spring.http.encoding.*`, `spring.resources.*`, `server.tomcat.max-threads`;
  - `logging.file.max-history`, `logging.file.total-size-cap`, `logging.file.clean-history-on-start`;
  - `spring.datasource.platform`, `spring.datasource.dbcp2.*`;
  - `spring.devtools.*`, `spring.session.jdbc.*`;
  - `hibernate.cache.redisson.*` e `JCacheRegionFactory`;
  - `spring.jpa.database-platform=PostgreSQL9Dialect`.
- **Overriding de beans:** `spring.main.allow-bean-definition-overriding=true` esconde conflitos.
- **Mensagens de erro:** os `server.error.include-*` estão comentados.

**Código**
- **Entidades expostas:** a maioria dos controllers recebe e devolve entidades JPA, o que abre espaço para mass assignment.
- **Transações:**
  - os services não usam `@Transactional`;
  - os repositories usam `jakarta.transaction.Transactional`, não o do Spring.
- **Código morto e nomes enganosos:**
  - `EmpresaContext` é código morto;
  - `ConfiguracaoProducao` está anotada com `@Profile("dev")` e usa `System.out`.
- **Status HTTP inadequados:**
  - `ConstraintViolationException` devolve 500, quando deveria ser 400;
  - criar devolve 200, não 201.
- **Concorrência e log:**
  - não há `@Version` nas entidades;
  - o `JwtFilter` e o `JwtService` usam `printStackTrace()`.
- **Bug no `EmpresaDTO`:** `EmpresaDTO.pessoaId` recebe o id do plano (em `EmpresaService`).

**Repositório**
- **Módulos duplicados:** `-dev` e `-pro` são quase iguais e não têm POM pai (P2).
- **Logs soltos:** `*.log` e `*.log.*.gz` são gerados na raiz do módulo. Desde a otimização, estão no `.gitignore` da raiz.
