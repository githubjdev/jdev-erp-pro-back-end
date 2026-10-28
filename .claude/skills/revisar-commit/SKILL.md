---
name: revisar-commit
description: Checklist antes do commit no Jdev ERP Pro. Roda os testes, revisa o diff com o revisor-codigo, chama o auditor-seguranca quando há mudança em controller, DTO ou segurança, e propõe a mensagem de commit em Conventional Commits. Não faz commit.
---
1. **Mudanças:** veja o que mudou com `git status`, `git diff --stat` e `git diff --staged --stat`. Se não houver mudanças, pare.
2. **Arquivos proibidos:** se estes estiverem staged ou entrarem no commit proposto, avise e não continue sem meu OK:
   - `application-dev.properties`;
   - `application-prod.properties`;
   - `*.log` e `*.log.*.gz`;
   - qualquer arquivo com segredo.
3. **Testes:** rode todos, dentro de `jdev-erp-pro-back-end-dev/`:
   `JAVA_HOME="/c/Program Files/Java/jdk-25.0.3" mvn -Dmaven.compiler.proc=full test`
   - Eles usam o banco dev. Se o serviço `postgresql-x64-18` estiver parado, avise.
   - Se algum teste falhar, pare e mostre o erro principal: classe, método e mensagem.
4. **Revisão:** chame o subagente `revisor-codigo` nas mudanças atuais.
5. **Segurança:** chame também o subagente `auditor-seguranca`, só para os arquivos alterados, se o diff tocar em:
   - `controller/`, `dto/` ou `security/`;
   - `JwtService`, `UsuarioService`, `UsuarioDetailsService`, `RoleService` ou `RoleUsuarioService`;
   - `application*.properties`.
6. **Resumo:** mostre só os achados críticos e de risco alto, com `arquivo:linha` e a correção. Se houver algum, recomende corrigir antes do commit.
7. **Mensagem de commit:** em Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`).
   - Descrição em português, no imperativo, com até 72 caracteres.
   - Corpo curto, se precisar.
8. **Sem commit:** não rode `git add`, `git commit` nem `git push`. Espere eu pedir.
