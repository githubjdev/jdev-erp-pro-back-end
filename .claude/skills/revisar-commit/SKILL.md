---
name: revisar-commit
description: Checklist antes do commit no Jdev ERP Pro. Roda os testes, revisa as mudanças com o revisor-codigo, chama o auditor-seguranca quando há mudança em controller, DTO ou segurança, e propõe a mensagem de commit em Conventional Commits. Não faz commit.
---
Mudanças atuais:
!`git status --short`

Resumo do diff:
!`git diff --stat`

1. **Sem mudanças:** se a lista acima estiver vazia, pare.
2. **Arquivos proibidos:** pare e me avise se o commit proposto incluir algum destes:
   - `application-dev.properties` ou `application-prod.properties`;
   - `*.log` ou `*.log.*.gz`;
   - qualquer arquivo com segredo.
3. **Testes:** rode a suíte inteira com o comando do CLAUDE.md (cerca de 20 s, usando o banco dev). Se falhar, pare e mostre classe, método e mensagem.
4. **Revisão:** chame o subagente `revisor-codigo`.
5. **Segurança:** se as mudanças tocarem em `controller/`, `dto/`, `security/`, `JwtService`, `UsuarioService`, `UsuarioDetailsService`, `RoleService`, `RoleUsuarioService` ou `application*.properties`, chame também o subagente `auditor-seguranca`, só para esses arquivos.
6. **Resumo:** mostre só os achados críticos e de risco alto (`arquivo:linha` + correção). Se houver algum, recomende corrigir antes do commit.
7. **Mensagem:** proponha uma em Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`, `docs:`, `chore:`):
   - em português, no imperativo, com até 72 caracteres;
   - corpo curto, se precisar.
8. **Não commite:** não rode `git add`, `git commit` nem `git push`. Espere eu pedir.
