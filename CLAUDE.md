# Jdev ERP Pro
Back-end REST de um ERP SaaS multiempresa: planos/empresas, pessoas, clientes/funcionários, categorias, produtos, movimentação de estoque, pedidos e chamados de suporte.
Java 25 · Spring Boot 4.0.6 · Spring Data JPA/Hibernate · Spring Security + JWT (jjwt 0.12.7) · PostgreSQL 18 · Maven (sem wrapper) · Lombok.

## Repositório
- Dois projetos Maven independentes, sem POM pai: `jdev-erp-pro-back-end-dev/` (onde se trabalha) e `jdev-erp-pro-back-end-pro/` (quase cópia do `-dev`).
- Só mexer no `-pro` quando eu pedir.
- Os comandos abaixo rodam dentro de `jdev-erp-pro-back-end-dev/` (PowerShell).

## Comandos
- Antes de qualquer `mvn`: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"`. O JAVA_HOME padrão da máquina é o JDK 17.
- Sempre passar `-Dmaven.compiler.proc=full`. Sem isso, o Lombok não roda no JDK 25 e a compilação falha.
- Build: `mvn -Dmaven.compiler.proc=full -DskipTests compile`
- Todos os testes: `mvn -Dmaven.compiler.proc=full test`
- Um teste: `mvn -Dmaven.compiler.proc=full -Dtest=TestCategoriaRepository test`
- Um método: `-Dtest=TestCategoriaRepository#testbuscaPorNome`
- Os testes usam o banco dev real, então o PostgreSQL precisa estar no ar.
- Rodar local: pela IDE, classe `br.com.jdeverp.pro.app.JDevERPPROApplication`.
- Perfil: `dev` é o padrão. Para trocar, use a variável `SPRING_PROFILES_JDEV_ERP_PRO` (o projeto não usa `SPRING_PROFILES_ACTIVE`).
- `mvn package` e `mvn spring-boot:run` falham com "Unable to find main class", porque a main é `void main(String[] args)` e não `public static`.
- API local: `http://localhost:8081/jdev-erp-pro-back-end-dev/api/...`
- Banco local: PostgreSQL 18, serviço do Windows `postgresql-x64-18`, porta 5433, início automático.
- Se o banco estiver parado: `Start-Service postgresql-x64-18`.
- Seed manual: `src/main/webapp/db_sql_versionamento/V1_INIT.sql`. Ele é destrutivo: apaga planos, empresas e pessoas.

## Estrutura (pacote base: `br.com.jdeverp.pro`)
Código em `src/main/java/br/com/jdeverp/pro/`, organizado por camada:
- `app/`: `JDevERPPROApplication` (scan, repositório base, cache, fuso `America/Sao_Paulo`).
- `controller/`: `@RestController` em `/api/` + entidade em camelCase (ex.: `/api/clienteFuncionario`).
- `service/`: regras de negócio, validações e checagem de admin. `UsuarioLogadoService` fornece o usuário e a empresa logados.
- `repository/`: interfaces `extends JpaJdevRepository<Entidade, Long>`. A implementação base `JpaJdevRepositoryImpl` traz os métodos filtrados por empresa.
- `model/`: entidades JPA. Não existe pacote `entity`.
- `dto/`: `LoginDTO`, `TokenDTO`, `AlterarSenhaDTO`, `UsuarioDto`, `EmpresaDTO`, `ClienteFuncionarioDTO`. Não existe pacote `mapper`.
- `enums/`: enums de domínio (`StatusPedido`, `TipoPlano`, `TipoMovimentacaoProduto`...).
- `security/`: `SecurityConfig` (SecurityFilterChain e CORS), `JwtFilter`, `UsuarioAutenticado`, handlers de 401/403.
- `exception/`: `GlobalExceptionHandler`, `MsgApiException`, `ResponseApi`.
- `validator/`: `RepositoryEmpresaValidator`, que valida as queries dos repositories na subida.
- `anotacoes/`: `@IgnoreEmpresaId`.
- `config/`: `PostgreSQLFunctionContributor`, que registra `unaccent` e `similarity` no Hibernate.
- `util/`: `ExceptionUtil`, `ObjectMapperUtil`.
- Testes: `src/test/java/br/com/jdeverp/pro/`, com classe base `contexto/TestContextoSpring`.
- Config: `src/main/resources/`.
  - `application.properties` só escolhe o perfil.
  - O resto fica em `application-dev.properties` e `application-prod.properties`.
- Migrations: o projeto não usa Flyway nem Liquibase.
  - O schema é gerado pelo Hibernate (`ddl-auto=update`).
  - Scripts SQL manuais ficam em `src/main/webapp/db_sql_versionamento/`, numerados como o `V1_INIT.sql` (o próximo é `V2_...sql`).

## Multiempresa e segurança
- Toda entidade de negócio tem `@ManyToOne(fetch = FetchType.LAZY) Empresa empresa`.
- Entidades globais, sem empresa: `Plano`, `Empresa`, `Role`, `RoleUsuario`.
- A empresa sempre vem de `usuarioLogadoService.getEmpresaIdLogada()` ou `getEmpresaLogada()`, nunca do request.
- Todo método declarado em repository precisa de `@Query` com `empresa.id` ou `empresa_id`, senão a aplicação não sobe.
  - Derived queries não passam.
  - A exceção é `@IgnoreEmpresaId(motivo = "...")`.
- Em entidade com empresa, `findAll()`, `findById()`, `count()`, `deleteById()` e `existsById(id)` lançam exceção. Use no lugar `buscarPorId(id, empresaId)`, `listar(empresaId)`, `total(empresaId)`, `existsById(id, empresaId)` e `listarPaginado(empresaId, pageable)`.
- O `save` não filtra por empresa. Em atualizar ou deletar, confirme antes com `buscarPorId(id, empresaId)` que o registro é da empresa logada.
- JWT stateless. O token também fica gravado no usuário e é conferido a cada requisição (`UsuarioDetailsService.existsByTokenParaUser`).
- A única rota pública é `POST /api/usuario/login`. Todas as outras exigem autenticação.
- Roles ficam na tabela `role`, campo `acesso`, sempre com o prefixo `ROLE_`. Existentes: `ROLE_USER` e `ROLE_ADMIN`.
  - `ROLE_ADMIN` é o admin global: gerencia todas as empresas.
- `@EnableMethodSecurity` está ligado. O código atual checa admin no service, com `usuarioLogadoService.isAdmin()`.

## Convenções
- Nomes em português: classes, métodos, mensagens e comentários.
- Injeção de dependência com `@Autowired` em campo, que é o padrão em controllers e services.
- Endpoints de CRUD: `POST /salvar`, `POST /atualizar`, `DELETE /deletar/{id}`, `GET /listar`, `GET /buscarPorId/{id}`, `GET /existePorNome/{nome}`, `GET /total`, `GET /paginado`.
  - Caminhos em camelCase. Resposta com `ResponseEntity.ok(...)`.
- A maioria dos CRUDs recebe e devolve a própria entidade.
  - Há DTO em login, senha, usuário, `Empresa` e `ClienteFuncionario`.
  - A conversão entidade↔DTO é manual, no service.
- Validação:
  - Bean Validation na entidade, com mensagem em português (`@NotBlank(message = "...")`), e `@Valid` no controller.
  - Regras de negócio ficam no service.
- Erro de negócio: `throw new MsgApiException("mensagem")` devolve 400. Para outro status, use `new MsgApiException("mensagem", HttpStatus.FORBIDDEN)`.
- O `GlobalExceptionHandler` responde com `ResponseApi`: `{localDateTime, status, error, message, path}`.
- Atualizar segue o padrão: id não nulo → `buscarPorId(id, empresaId)` ou `MsgApiException` → força a empresa logada → `saveAndFlush`.
- Paginação:
  - Requisição: `GET /paginado?page=0&size=10&sort=id&direction=asc`.
  - O controller monta o `PageRequest` e chama `repository.listarPaginado(empresaId, pageable)`, que devolve `Page<T>`.
- Queries:
  - Busca por nome com `unaccent(upper(trim(...))) like`.
  - Delete customizado com `@Transactional` + `@Modifying(flushAutomatically = true, clearAutomatically = true)`.
- Ids por sequence com `allocationSize = 1`, ex.: `@SequenceGenerator(name = "seq_categoria", sequenceName = "seq_categoria", allocationSize = 1)`.
- Entidades com Lombok `@Data`.
- Testes:
  - Classe `Test` + nome da classe testada (ex.: `TestCategoriaRepository`) e métodos `test` + nome do método.
  - Testes de integração (repository) estendem `TestContextoSpring`, que é `@SpringBootTest` + `@Transactional` com rollback.
  - Teste de service é unitário, com Mockito e sem Spring.

## Regras que nunca podem ser quebradas
- Nunca alterar um script existente em `db_sql_versionamento/`: crie o próximo `V2_...sql`.
  - Toda mudança de entidade que altera tabela, constraint ou seed vem acompanhada de um script novo.
- Nunca pôr `ddl-auto=update` ou `create` em perfil novo, nem mudar o `ddl-auto` dos perfis existentes sem eu pedir.
  - Hoje dev e prod usam `update`: é uma dívida conhecida.
- Nunca quebrar o isolamento por empresa (seção acima). Usar `@IgnoreEmpresaId` só com `motivo` e com o meu aval.
- Todo endpoint novo tem autorização explícita com `@PreAuthorize`:
  - `isAuthenticated()` para dados da empresa logada;
  - `hasAuthority('ROLE_ADMIN')` para dados globais (`Plano`, `Role`, `Empresa`).
- Nunca commitar credenciais.
  - Segredo novo entra como variável de ambiente no properties (ex.: `jwt.secret=${JWT_SECRET}`).
  - `application-dev.properties` e `application-prod.properties` têm segredos: não commitar.
- Rodar os testes afetados antes de dizer que uma tarefa terminou. Se não der para rodar (ex.: banco fora do ar), avisar.
- Não fazer `git commit` nem `git push` sem eu pedir.

## Mais contexto (ler só quando a tarefa pedir)
- Arquitetura e módulos: `docs/arquitetura.md`
- Regras de negócio: `docs/regras-negocio.md`
- Análise técnica completa: `docs/analise-projeto.md`
