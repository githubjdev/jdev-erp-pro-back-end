---
paths:
  - "**/src/test/**/*.java"
---
# Testes

- **Nomes:** a classe é `Test` + a classe testada (ex.: `TestCategoriaRepository`), os métodos são `test` + o método testado, e o pacote é o mesmo da classe testada.
- **Service:** teste unitário, sem Spring.
  - Use `@ExtendWith(MockitoExtension.class)`, `@Mock` nos repositories e no `UsuarioLogadoService`, e `@InjectMocks` no service.
  - Cubra o caminho feliz e cada `MsgApiException` (mensagem e status), inclusive a regra de empresa.
- **Repository com `@Query`:** estenda `contexto/TestContextoSpring` (`@SpringBootTest` + `@Transactional`, com rollback).
  - Esses testes usam o banco dev real e o seed: empresa 1, via `empresaRepository.findById(1L)`.
- **Controller e autenticação:** estenda `contexto/TestContextoApi`, como em `controller/TestCategoriaController` e `security/TestJwtFilter`.
  - Fluxo real: filtros ligados, login de verdade (`POST /api/usuario/login`) e `spring.jpa.open-in-view=false`.
  - Sem `@Transactional` no teste, de propósito: uma transação do teste deixaria a sessão aberta e esconderia `LazyInitializationException`. As requisições gravam no banco dev.
  - Cada classe cria a sua empresa de teste, com um admin já logado (`admin`, `token`), e apaga tudo dela no final. Nunca use a empresa 1: os `deletarTodos` apagam a empresa inteira.
  - Para isolamento, crie uma "outra empresa" com `criarEmpresa()` e `criarUsuario(nome, admin, empresaId)`. Para escrever SQL de preparação, use `executarSql(...)`: o pool está com `auto-commit=false`.
  - Métodos ordenados com `@Order`, um por endpoint. Todo endpoint novo precisa de teste.
  - Teste o acesso negado pela regra usada no endpoint (`isAdmin()` no service ou `@PreAuthorize`), com um usuário sem `ROLE_ADMIN` e `comToken(...)`.
- **Sessão do Hibernate:** `service/TestSessaoHibernate` chama os services direto, com o banco dev e sem transação no teste, autenticado como no `JwtFilter`.
  - Confere que nenhum DTO lê relacionamento LAZY fora da sessão do service, e que `toString`, `equals` e `hashCode` das entidades funcionam fora da sessão.
  - Service novo, ou DTO com relacionamento novo, ganha um teste ali.
- **`JwtFilter`:** o fluxo real fica em `security/TestJwtFilter`; os caminhos que não dá para provocar de ponta a ponta (ex.: banco fora do ar) ficam em `security/TestJwtFilterUnitario`, com Mockito.
- **Bibliotecas:** JUnit 5, Mockito, AssertJ, Spring Boot Test, MockMvc e spring-security-test, todas já disponíveis pelos starters. Não adicione dependências nem Testcontainers.
- **Validação na subida:** o contexto valida todas as queries (`RepositoryEmpresaValidator`). Uma `@Query` sem `empresa.id` derruba todos os testes de integração.
