# Análise do projeto — Jdev ERP Pro (back-end)

Data da análise: 2026-10-28. Escopo: módulo `jdev-erp-pro-back-end-dev` (o módulo `jdev-erp-pro-back-end-pro` não foi analisado).
Itens não confirmados no código estão marcados com `[CONFIRMAR]`.

## 1. Build

- Maven, **sem wrapper** (não há `mvnw`). Maven 3.9.11 instalado em `C:\apache-maven-3.9.11`.
- Java **25** (`java.version`, `maven.compiler.source/target` = 25).
- Spring Boot **4.0.6** (parent `spring-boot-starter-parent`), packaging `jar`, `groupId`/`artifactId` = `jdev-erp-pro-back-end-dev`.
- O repositório tem **dois projetos Maven independentes**, sem POM pai: `jdev-erp-pro-back-end-dev/` e `jdev-erp-pro-back-end-pro/`. O `src/` dos dois só difere nos arquivos com alterações em andamento.
- O `JAVA_HOME` da máquina aponta para o JDK 17, mas o JDK 25 está em `C:\Program Files\Java\jdk-25.0.3`.
- **`mvn compile` falha com o JDK 25** (200 erros `cannot find symbol` porque getters e setters do Lombok não são gerados). Desde o JDK 23, o annotation processing implícito vem desligado. Com `-Dmaven.compiler.proc=full` o build passa (testado: `BUILD SUCCESS`).
- A IDE parece ser o Eclipse (`.project`, `.classpath`, `.settings/`), que compila com o agente Lombok da IDE. `[CONFIRMAR]`

## 2. Dependências

- Starters: `data-jpa`, `security`, `webmvc`, `validation`. De teste: `data-jpa-test`, `security-test`, `webmvc-test`.
- `org.postgresql:postgresql`.
- Lombok (versão gerenciada pelo Boot, sem `annotationProcessorPaths` no `maven-compiler-plugin`).
- JWT: `io.jsonwebtoken:jjwt` 0.12.7 (artefato agregador).
- `commons-collections4` 4.5.0 (usado em `JpaJdevRepositoryImpl`).
- `jackson-datatype-jsr310` 2.13.5 (Jackson 2). O `ObjectMapperUtil` usa `com.fasterxml.jackson.databind.ObjectMapper`.
- `HikariCP` 4.0.3 com versão fixa (vem de uma alteração ainda não commitada).
- **Não usa:** MapStruct, Flyway, Liquibase, springdoc/Swagger, Testcontainers, H2, devtools, spring-session.
- Funções SQL registradas no Hibernate por `config/PostgreSQLFunctionContributor` (via `META-INF/services/org.hibernate.boot.model.FunctionContributor`): `unaccent`, `similarity`, `word_similarity` e `strict_word_similarity`. Elas dependem das extensões `unaccent` e `pg_trgm` no PostgreSQL.
  - `unaccent` está ativa no banco dev: os testes de `buscaPorNome` passaram na etapa 8.
  - As funções `similarity` estão registradas, mas nenhuma query as usa, então o `pg_trgm` ainda não é necessário.

## 3. Pacote base e camadas

- Pacote base: `br.com.jdeverp.pro`. Fontes em `src/main/java/br/com/jdeverp/pro/`.
- Main: `br.com.jdeverp.pro.app.JDevERPPROApplication`, com `@EntityScan("br.com.jdeverp.pro.model")`, `@EnableJpaRepositories("br.com.jdeverp.pro.repository", repositoryBaseClass = JpaJdevRepositoryImpl.class)`, `@ComponentScan("br.com.jdeverp.pro")`, `@EnableCaching`, `@EnableScheduling` e `@EnableAsync`.
- **Organização por camada**. Pacotes:
  - `app`: main
  - `anotacoes`: `@IgnoreEmpresaId`
  - `config`: `PostgreSQLFunctionContributor`, `ConfiguracaoProducao`
  - `controller`: 14 `@RestController`
  - `dto`
  - `enums`
  - `exception`: `GlobalExceptionHandler`, `MsgApiException`, `ResponseApi`
  - `model`: entidades JPA. Não existe pacote `entity`.
  - `repository`
  - `security`: `SecurityConfig`, `JwtFilter`, `UsuarioAutenticado`, `JwtAuthenticationEntryPoint`, `JwtAccessDeniedHandler`
  - `service`
  - `util`: `ExceptionUtil`, `ObjectMapperUtil`
  - `validator`: `RepositoryEmpresaValidator`
- **Não existe** pacote `mapper`: a conversão entidade↔DTO é feita à mão no service.
- Existe uma classe fora do pacote base: `br.jdev.estoque.erp.multitanent.EmpresaContext`.
- **Multi-tenant por empresa (coluna `empresa_id`):**
  - `repository/JpaJdevRepository` + `JpaJdevRepositoryImpl`: repositório base com métodos genéricos que recebem `empresaId`: `listarPaginado`, `total`, `buscarPorId`, `listar`, `existsById`, `buscarPorIds`, `deletarAllById` e `deleteAll(Long)`.
  - Em entidades que têm o campo `empresa`, o repositório base bloqueia com `UnsupportedOperationException` os métodos sem filtro por empresa (`findAll`, `findById`, `count`, `delete*`, `existsById(ID)`, `findOne`, `getReferenceById`...). O `save` **não** é bloqueado.
  - `validator/RepositoryEmpresaValidator`: na subida da aplicação, todo método declarado numa interface de repository precisa ter `@Query` contendo `empresa.id` ou `empresa_id`. Caso contrário, a aplicação não sobe. Derived queries (sem `@Query`) também são proibidas.
  - A exceção é `@IgnoreEmpresaId`, aplicado na classe em `EmpresaRepository`, `PlanoRepository`, `RoleRepository` e `RoleUsuarioRepository`, e em um método de `UsuarioRepository` (login).
  - A empresa sempre vem do usuário logado: `UsuarioLogadoService.getEmpresaIdLogada()` / `getEmpresaLogada()`.
- Fluxo: Controller → Service → Repository.

## 4. Módulos de negócio (entidades em `model/`)

- **SaaS / tenant:**
  - `Plano`: `TipoPlano` FREE/PRO/ENTERPRISE, limites de cliente e de usuário, valor mensal com CHECK no banco.
  - `Empresa`: plano, pessoa, totalUsuario, totalCliente, planoAtivo, bloqueio, logoMarca, vigenciaPlano.
- **Cadastros:**
  - `Pessoa`: `TipoPessoa` FISICA/JURIDICA.
  - `ClienteFuncionario`: `TipoClienteFuncionario` CLIENTE/FUNCIONARIO/PRESTADOR_SERVICO, `@OneToOne` com `Usuario` e com `Pessoa`.
- **Produtos / estoque:**
  - `Categoria`.
  - `Produto`: `UnidadeMedida` CENTIMETRO/METRO/UNIDADE/QUILO, categoria `EAGER`.
  - `MovimentacaoProduto`: `TipoMovimentacaoProduto` com entradas, saídas, reserva e liberação; ligada a produto e pedido.
- **Vendas:**
  - `Pedido`: `StatusPedido`, `FormaPagamento`, `TipoPedido`; vendedor e cliente são `Usuario`.
  - `ItemPedido`: produto e pedido.
- **Atendimento:**
  - `Chamado`: `TipoChamado`, `PrioridadeChamado`, `StatusChamado`; `abertoUser`, `fechadoUser`, `atendente` e `cliente` são `Usuario`.
  - `Mensagem`: chamado, atendente, cliente.
- **Acesso:**
  - `Usuario`: implementa `UserDetails`; `@ManyToMany(EAGER)` `acessos` → `Role`; empresa `EAGER`.
  - `Role`: implementa `GrantedAuthority`; campo `acesso`.
  - `RoleUsuario`: tabela `role_usuario`.
- **Financeiro:** não existe módulo.
- Entidades sem campo `empresa` (globais): `Plano`, `Empresa`, `Role`, `RoleUsuario`. Todas as outras, inclusive `Usuario`, são por empresa.

## 5. Banco de dados

- PostgreSQL. Arquivos em `src/main/resources/`:
  - `application.properties`: só tem `spring.profiles.active=${SPRING_PROFILES_JDEV_ERP_PRO:dev}`.
  - `application-dev.properties` e `application-prod.properties`: ambos ainda não commitados.
- Perfis: `dev` (padrão) e `prod`, escolhidos pela variável de ambiente própria `SPRING_PROFILES_JDEV_ERP_PRO`, documentada em `jdev-erp-pro-back-end-dev/documentação/1 - Documentação`. **Não existe perfil `test`.**
- Hoje o `application-prod.properties` é uma cópia do dev: mesmo banco, `context-path` `/jdev-erp-pro-back-end-dev`, porta 8081 e nome do arquivo de log. `[CONFIRMAR: valores reais de produção]`
- `spring.jpa.hibernate.ddl-auto`: **dev = `update`**, **prod = `update`**.
- O schema é criado pelo Hibernate (`ddl-auto=update`). **Não há ferramenta de migration.**
- Scripts SQL manuais ficam em `src/main/webapp/db_sql_versionamento/`.
  - Padrão de nome: `V<n>_<DESCRICAO>.sql` (estilo Flyway).
  - Última versão: `V1_INIT.sql` (182 linhas, só seed). Ele:
    - desliga triggers com `session_replication_role = replica`;
    - executa `delete from` em plano, empresa e pessoa;
    - recria o CHECK `plano_valor_mensal_check`;
    - insere 2 planos, a pessoa e a empresa 1, 30 categorias e as roles `ROLE_USER`/`ROLE_ADMIN`.
- Como e quando o script é executado não está automatizado. `[CONFIRMAR]`
- Pool Hikari configurado (`auto-commit=false`, `maximum-pool-size=15`). `spring.jpa.open-in-view=true`.
- Ids: sequence por tabela (`seq_<tabela>`, `allocationSize = 1`).

## 6. Segurança

- Autenticação **JWT stateless** (jjwt), com expiração `jwt.expiration=86400000` (24h). Senhas com `BCryptPasswordEncoder`.
- `SecurityFilterChain` em `br.com.jdeverp.pro.security.SecurityConfig#securityFilterChain`:
  - CSRF desligado, CORS ligado, `SessionCreationPolicy.STATELESS`;
  - `JwtAuthenticationEntryPoint` responde 401 e `JwtAccessDeniedHandler` responde 403, ambos com JSON `ResponseApi`;
  - `JwtFilter` (`OncePerRequestFilter`) roda antes do `UsernamePasswordAuthenticationFilter`.
- `JwtFilter`:
  - lê `Authorization: Bearer <token>`, `JwtService.validarToken` e `extrairLogin`;
  - chama `UsuarioDetailsService.existsByTokenParaUser(token)`, que confere no banco se o token está gravado para aquele usuário e empresa (confirmado na etapa 2; o JWT carrega os claims de empresaId e usuarioId);
  - coloca um `UsuarioAutenticado` como principal no `SecurityContext`.
- Rotas:
  - **pública:** `POST /api/usuario/login`;
  - `/api/admin/**` exige `hasAuthority("ROLE_ADMIN")`, mas nenhum controller usa esse prefixo;
  - todas as demais exigem `authenticated()`.
  - O `context-path` (`/jdev-erp-pro-back-end-dev`) vem antes de todas.
- Roles ficam na tabela `role`, campo `acesso`, que precisa começar com `ROLE_` e é gravado em maiúsculas.
  - Seed: `ROLE_USER` (id 1) e `ROLE_ADMIN` (id 2).
  - Um comentário em `Usuario` cita `ROLE_GERENTE`, que não existe no seed.
  - Usuário novo recebe `ROLE_USER` (`UsuarioService`).
- `@EnableMethodSecurity(prePostEnabled, jsr250Enabled, securedEnabled)` está ligado, mas **não há nenhum `@PreAuthorize`, `@Secured` ou `@RolesAllowed`** no código.
- A autorização é feita no service, com `UsuarioLogadoService.isAdmin()` (tem `ROLE_ADMIN`), em `EmpresaService` e `UsuarioService`.
- CORS: `allowCredentials=true` com `allowedOriginPattern("*")` e headers e métodos `*`.
- `spring.security.user.name/password` estão definidos, mas provavelmente não têm efeito, porque já existe um `UserDetailsService` próprio. `[CONFIRMAR]`

## 7. Padrões de código

- **Idioma:** português (classes, métodos, mensagens e comentários). Os métodos herdados do Spring Data ficam em inglês (`findAll`, `deleteById`, `existsById`).
- **Injeção:** `@Autowired` em campo (73 ocorrências em controllers e services). A única injeção por construtor está em `RepositoryEmpresaValidator`.
- **Controllers:**
  - `@RestController` + `@RequestMapping("/api/<entidadeCamelCase>")`, por exemplo `/api/categoria` e `/api/clienteFuncionario`.
  - Endpoints padrão:
    - `POST /salvar`
    - `POST /atualizar` (não usa PUT)
    - `DELETE /deletar/{id}`
    - `GET /listar`
    - `GET /buscarPorId/{id}`
    - `GET /existePorNome/{nome}`
    - `GET /existePorNomeDiferenteId/{id}/{nome}`
    - `GET /existePorId/{id}`
    - `GET /total`
    - `GET /paginado`
    - operações em lote com `@RequestBody Iterable<Long> ids`
  - O retorno é sempre `ResponseEntity.ok(...)` (200), inclusive na criação.
  - O `UsuarioController` mistura caminhos em kebab-case (`/buscar-por-nome`, `/deletar-todos`).
- **DTO vs entidade:** a maioria dos endpoints recebe e devolve a **entidade** (`@RequestBody @Valid Categoria categoria`).
  - DTOs de entrada: `LoginDTO`, `AlterarSenhaDTO`, `UsuarioDto`.
  - DTOs de saída: `TokenDTO`, `EmpresaDTO` e `ClienteFuncionarioDTO` (os dois últimos em andamento).
  - Os nomes misturam `UsuarioDto` e `*DTO`.
- **Services:**
  - `@Service`; erros de negócio lançam `MsgApiException(msg)` (400) ou `MsgApiException(msg, HttpStatus)`.
  - Padrão do `atualizar`: valida que o id não é nulo → `buscarPorId(id, empresaId)` → força a empresa logada → `saveAndFlush`.
- **Repositories:**
  - `interface XRepository extends JpaJdevRepository<X, Long>` com `@Repository`.
  - Todas as consultas usam `@Query` JPQL com `empresa.id`.
  - Busca por nome usa `unaccent(upper(trim(...))) like ...`.
  - Deletes usam `@Transactional` (`jakarta.transaction`) + `@Modifying(flushAutomatically = true, clearAutomatically = true)`.
- **Transações:** nenhum `@Transactional` nos services. Só há `jakarta.transaction.Transactional` em 14 repositories.
- **Entidades:**
  - Lombok `@Data` em quase todas (`Plano` é a exceção);
  - Bean Validation com mensagem em português (`@NotBlank(message = "...")`);
  - `@ManyToOne(fetch = LAZY)` para `empresa`;
  - `@JsonIgnore` nos campos sensíveis de `Usuario`.
- **Erros:** `br.com.jdeverp.pro.exception.GlobalExceptionHandler` (`@RestControllerAdvice`). Formato `ResponseApi`:
  ```json
  { "localDateTime": "...", "status": 400, "error": "...", "message": "...", "path": "/..." }
  ```
  - `MsgApiException` → status da própria exceção;
  - `MethodArgumentNotValidException` → 400, com as mensagens unidas por `", "`;
  - `DataIntegrityViolationException` → 400;
  - lock otimista → 400;
  - `ConstraintViolationException` → 500;
  - `Exception` → 500.
- **Paginação:**
  - `GET /paginado?page=0&size=10&sort=id&direction=asc`;
  - o controller monta um `PageRequest` → `repository.listarPaginado(empresaId, pageable)`;
  - o retorno é `Page<Entidade>` (`PageImpl` serializado direto).
- **Outros:**
  - fuso `America/Sao_Paulo` e locale `pt_BR` definidos no `@PostConstruct` da main;
  - cache `ConcurrentMapCacheManager("cacheJDevERPPRO")`.

## 8. Testes

- JUnit 5 + Spring Boot Test. O MockMvc está habilitado (`@AutoConfigureMockMvc(addFilters = false)`), mas nenhum teste o usa.
- Existem só testes de integração com `@SpringBootTest` + `@Transactional` (rollback no fim). Não há `@WebMvcTest`, `@DataJpaTest` nem teste unitário.
- Classe base: `src/test/java/br/com/jdeverp/pro/contexto/TestContextoSpring.java`, com:
  - `@SpringBootTest(classes = JDevERPPROApplication.class)`;
  - `@TestPropertySource(locations = "classpath:application.properties")`;
  - `@TestMethodOrder(OrderAnnotation.class)`.
  - Os testes estendem essa classe.
- Testes existentes:
  - `TestContextoSpring`: 1 teste de smoke;
  - `repository/TestCategoriaRepository`: 7 testes.
- Nomenclatura: classe `Test<Classe>` (coberta pelo padrão `**/Test*.java` do Surefire), métodos `test<metodo>`.
- **Os testes usam o banco dev real** (não há perfil de teste nem `src/test/resources`) e dependem do seed V1 (`empresaRepository.findById(1L)`).
- Rodar um teste só: `mvn -Dmaven.compiler.proc=full -Dtest=TestCategoriaRepository test`. Um método só: `-Dtest=TestCategoriaRepository#testbuscaPorNome`.
- Na etapa 8, `mvn -Dmaven.compiler.proc=full test` (com JDK 25) rodou 9 testes em 17 s: todos passaram (`BUILD SUCCESS`).

## 9. Comandos

Rodar dentro de `jdev-erp-pro-back-end-dev/` (PowerShell):

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"   # o JAVA_HOME padrão da máquina é o 17

mvn -Dmaven.compiler.proc=full -DskipTests compile     # build (testado: OK)
mvn -Dmaven.compiler.proc=full test                    # todos os testes (usa o banco dev)
mvn -Dmaven.compiler.proc=full -Dtest=TestCategoriaRepository test
```

- Perfil: `$env:SPRING_PROFILES_JDEV_ERP_PRO = "prod"`. Sem a variável, o perfil é `dev`.
- URL local: `http://localhost:8081/jdev-erp-pro-back-end-dev/api/...`
- **`mvn package` falha** com `Unable to find main class` (testado na etapa 2), e `spring-boot:run` usa a mesma detecção. Motivo: a main é `void main(String[] args)` (main de instância do Java 25), não `public static`. A aplicação subiu com o perfil `dev` em 27/10, segundo o log, provavelmente pela IDE.
- Banco local: não há `docker-compose`. O PostgreSQL 18 está instalado como serviço do Windows `postgresql-x64-18` (porta 5433, início automático, que é a porta do `application-dev.properties`). Também existe um serviço `postgresql-9.5` rodando. (Confirmado na etapa 2.)
- Seed: rodar `src/main/webapp/db_sql_versionamento/V1_INIT.sql` à mão, depois que o Hibernate criar as tabelas. `[CONFIRMAR]`

## 10. Pontos de atenção (só registrados, nada foi corrigido)

**Segurança e multi-tenant**
- `CategoriaService.atualizar` não verifica se o id pertence à empresa logada antes do `save`. Um usuário pode sobrescrever a categoria de outra empresa e transferi-la para a sua. Os outros services fazem essa verificação.
- `JpaJdevRepositoryImpl.listarPaginado` concatena o `sort` vindo da query string direto no `order by` do JPQL: possível injeção de JPQL.
- `RoleService`, `RoleUsuarioService` e `PlanoService` não exigem admin. Qualquer usuário autenticado pode:
  - criar ou alterar roles e planos, que são globais;
  - associar `ROLE_ADMIN` a usuários da própria empresa, inclusive a si mesmo. `ROLE_ADMIN` dá acesso à gestão de todas as empresas (`EmpresaService`).
  - `[CONFIRMAR se é intencional]`
- A regra de `/api/admin/**` não protege nenhuma rota, e `@EnableMethodSecurity` está ligado sem uso.
- CORS aceita qualquer origem com `allowCredentials=true`.
- Senha do banco, senha do `spring.security.user` e `jwt.secret` estão em texto plano nos `.properties`. Já estiveram commitadas no histórico do git (`application.properties`, até o commit `aed3214`).
- Há uma senha em texto claro num comentário em `JDevERPPROApplication.configTimeZone()`.
- Quando ocorre uma exceção no `JwtFilter`, ele relança `MsgApiException`. Como isso acontece fora do `DispatcherServlet`, o `GlobalExceptionHandler` não trata o erro. `[CONFIRMAR o formato da resposta]`

**Banco e configuração**
- `ddl-auto=update` em `dev` e em `prod`, sem nenhuma ferramenta de migration.
- `application-prod.properties` é igual ao dev (banco, context-path e log).
- `V1_INIT.sql` é destrutivo (`delete from` com triggers desligados) e não pode rodar em produção.
- Propriedades obsoletas ou sem efeito:
  - `spring.http.encoding.*` e `spring.resources.*`;
  - `server.tomcat.max-threads`;
  - `logging.file.max-history`, `logging.file.total-size-cap` e `logging.file.clean-history-on-start`;
  - `spring.datasource.platform` e `spring.datasource.dbcp2.*` (o pool é Hikari);
  - `spring.devtools.*` e `spring.session.jdbc.*` (sem as dependências);
  - `hibernate.cache.redisson.*` e `JCacheRegionFactory` (sem as dependências);
  - `spring.jpa.database-platform=PostgreSQL9Dialect` (removido no Hibernate 6+ e redundante com `hibernate.dialect`).
- `spring.main.allow-bean-definition-overriding=true` esconde conflitos de bean.
- `spring.jpa.open-in-view=true`.

**Build e dependências**
- O build pelo Maven no terminal só funciona com `-Dmaven.compiler.proc=full` (Lombok) e com `JAVA_HOME` apontando para o JDK 25.
- `HikariCP` 4.0.3 fixo (alteração não commitada): sobrescreve a versão gerenciada pelo Boot 4, e o pool já vem pelo `starter-data-jpa`.
- `jackson-datatype-jsr310` 2.13.5 fixo (Jackson 2), enquanto o Boot 4 usa Jackson 3 por padrão. `[CONFIRMAR: conflito de versões do jackson-databind 2]`
- Não há Maven Wrapper.

**Código**
- Os controllers expõem e recebem entidades JPA. Com isso, a API fica acoplada ao modelo e há risco de mass assignment.
- Não há `@Transactional` nos services: operações com várias escritas não são atômicas. Os repositories usam `jakarta.transaction.Transactional` em vez do `org.springframework.transaction.annotation.Transactional`.
- `EmpresaContext` está fora do `@ComponentScan` (`br.jdev.estoque.erp.multitanent`): não vira bean e não é usado.
- `ConfiguracaoProducao` está anotada com `@Profile("dev")` (o nome sugere prod) e usa `System.out`.
- `ConstraintViolationException` devolve 500, quando validação deveria devolver 400.
- A criação devolve 200 em vez de 201, e a atualização é feita por `POST /atualizar`.
- Não há `@Version` nas entidades, embora o handler trate `ObjectOptimisticLockingFailureException`.
- `JwtFilter` usa `e.printStackTrace()`.

**Repositório**
- `-dev` e `-pro` são cópias quase idênticas no mesmo repositório, sem POM pai, então mudanças precisam ser replicadas à mão. `[CONFIRMAR o papel do -pro]`
- Os arquivos `jdev-erp-pro-back-end-dev.log*` gerados na raiz do módulo não estão no `.gitignore` do módulo (que só tem `/target/`).

## Itens `[CONFIRMAR]`

1. Se o Eclipse é a IDE usada (compila com o agente Lombok).
2. ~~Se as extensões `unaccent` e `pg_trgm` estão criadas~~: `unaccent` está ativa no dev; `pg_trgm` ainda não é usada (etapa 8).
3. Os valores reais do perfil `prod` (hoje é uma cópia do dev).
4. Como e quando o `V1_INIT.sql` é executado.
5. ~~Se o token JWT fica gravado no usuário~~: confirmado (etapa 2).
6. Se `spring.security.user.*` tem algum efeito.
7. ~~Se os testes passam~~: 9 testes passaram (etapa 8).
8. ~~Se `mvn spring-boot:run` funciona~~: `mvn package` falha com `Unable to find main class` (etapa 2).
9. ~~Como o PostgreSQL local é instalado~~: serviço `postgresql-x64-18`, porta 5433 (etapa 2).
10. Se a ausência de checagem de admin em Role, RoleUsuario e Plano é intencional.
11. O formato da resposta quando o `JwtFilter` lança exceção.
12. Se há conflito de versões do Jackson 2 com o Boot 4.
13. Qual é o papel do módulo `-pro` (deploy? cópia?).

## Dúvidas em aberto da etapa 2 (CLAUDE.md)

Registradas em 2026-10-28, ainda sem decisão. O `CLAUDE.md` segue como está até que sejam respondidas.

1. **Autorização em endpoint novo.** O `CLAUDE.md` exige `@PreAuthorize`, mas o código atual checa admin no service (`usuarioLogadoService.isAdmin()`). Fica a dúvida se o padrão novo deve mesmo ser `@PreAuthorize`.
2. **Quem pode atribuir roles (`RoleUsuario`).** Hoje qualquer usuário autenticado atribui roles, inclusive `ROLE_ADMIN`. Não está decidido se deve ser só `ROLE_ADMIN`. Por isso o `RoleUsuario` ficou fora da regra "dado global só para `ROLE_ADMIN`".
3. **Papel do módulo `-pro`.** O `CLAUDE.md` diz "só mexer quando eu pedir". Falta confirmar se ele é cópia, deploy ou outra coisa (mesmo assunto do item 13 acima).
4. ~~**Documentos citados que não existem.**~~ Resolvido: `docs/arquitetura.md` e `docs/regras-negocio.md` foram criados na etapa 3.
5. **Serviço `postgresql-9.5`.** Há um segundo PostgreSQL rodando na máquina, além do 18 (porta 5433), que é o usado pelo projeto. Falta saber se o 9.5 serve a outro sistema.
