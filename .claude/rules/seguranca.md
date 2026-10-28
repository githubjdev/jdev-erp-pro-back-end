---
paths:
  - "**/src/main/java/br/com/jdeverp/pro/security/**/*.java"
  - "**/src/main/java/br/com/jdeverp/pro/controller/**/*.java"
  - "**/src/main/java/br/com/jdeverp/pro/dto/**/*.java"
  - "**/src/main/java/br/com/jdeverp/pro/service/{UsuarioService,UsuarioDetailsService,UsuarioLogadoService,JwtService,RoleService,RoleUsuarioService}.java"
---
# Segurança

- **Não remova checagens existentes:** ao adicionar `@PreAuthorize`, mantenha as checagens `usuarioLogadoService.isAdmin()` que os services já fazem.
- **Erros:** sem permissão, `MsgApiException("...", HttpStatus.FORBIDDEN)`; falha de autenticação, `HttpStatus.UNAUTHORIZED`.
- **Role nova:** se o código depender de uma role nova, ela entra por script novo em `db_sql_versionamento/`.
- **`SecurityConfig`:** não adicione `permitAll()` nem mude CORS, CSRF ou `SessionCreationPolicy` sem eu pedir.
- **Senha:** use o bean `PasswordEncoder` de `SecurityConfig#passwordEncoder()` (BCrypt), com `encode(...)` para gravar e `matches(...)` para comparar. Nunca use `new BCryptPasswordEncoder()` fora do `SecurityConfig`.
- **Token:** gere e leia só pelo `JwtService` (claims atuais: `usuarioId`, `empresaId`, `login`). Não coloque dados pessoais nem permissões no token.
- **Checagens que não podem sair:** a conferência do token gravado (`UsuarioDetailsService.existsByTokenParaUser`) e as de `liberado` e `empresa.bloqueio`.
- **Usuário logado:** sempre pelo `UsuarioLogadoService`. Nunca decida acesso com um `usuarioId` vindo do request.
- **Atribuição de role:** só para usuários da empresa logada. Valide com `usuarioRepository.buscarPorId(id, empresaId)`.
- **Log:**
  - Nunca registre token, senha, header `Authorization`, CPF, CNPJ ou e-mail.
  - Use SLF4J (`LoggerFactory.getLogger`), nunca `System.out` nem `printStackTrace()`.
- **Resposta da API:**
  - Nunca devolva `senha`, `tokenSessao` ou `refreshToken` em DTO ou JSON.
  - Em `Usuario`, os campos sensíveis levam `@JsonIgnore`. Em `UsuarioDto`, a senha volta como `***Ocultada***`.
