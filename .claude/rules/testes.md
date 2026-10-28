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
- **Controller:** MockMvc.
  - O principal precisa ser `UsuarioAutenticado`, porque o `UsuarioLogadoService` faz cast para essa classe.
  - A base desliga os filtros (`addFilters = false`). Teste o acesso negado pela regra usada no endpoint: `isAdmin()` no service ou `@PreAuthorize`.
- **Bibliotecas:** JUnit 5, Mockito, AssertJ, Spring Boot Test, MockMvc e spring-security-test, todas já disponíveis pelos starters. Não adicione dependências nem Testcontainers.
- **Validação na subida:** o contexto valida todas as queries (`RepositoryEmpresaValidator`). Uma `@Query` sem `empresa.id` derruba todos os testes de integração.
