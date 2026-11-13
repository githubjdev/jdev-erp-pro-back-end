# Pendências — decisões em aberto

Esta é a lista única de dúvidas do projeto. Os outros documentos citam cada item pelo número (P1 a P25).
Quando uma pendência for respondida, registre a decisão aqui, atualize os arquivos afetados e marque o item como resolvido.

O relatório da sessão de 2026-11-11 (DTOs, open-in-view, testes, JwtFilter, área admin e regras alinhadas ao banco) está em `docs/relatorios/relatorio-sessao-2026-11-11.pdf`.

## Projeto e ambiente
| # | Pergunta | Afeta |
|---|---|---|
| P1 | A IDE usada é o Eclipse? | `analise-projeto.md` §1 |
| P2 | Qual é o papel do módulo `-pro` (deploy, produção, cópia)? O trabalho continua só no `-dev`? Em 2026-11-12, a pedido, o `-pro` recebeu de novo todas as mudanças do `-dev`, inclusive o Flyway; hoje diferem só os nomes do projeto: o `.project`, o `pom.xml` (groupId, artifactId, nome e descrição), nos properties o banco, o context-path e o arquivo de log, e o caminho com o context-path em `TestGlobalExceptionHandler` e `TestJwtAccessDeniedHandler`. | `CLAUDE.md` (Repositório) |
| P3 | Que nome de banco, context-path e arquivo de log o perfil `prod` deve ter? No `-dev`, ele é cópia do dev. No `-pro`, os dois perfis (inclusive o `dev`) apontam para o banco `jdev-erp-pro-prod`. | `application-prod.properties` |
| P5 | O serviço `postgresql-9.5` serve a outro sistema ou pode ser ignorado? | `CLAUDE.md` (Comandos) |
| P6 | O usuário de `spring.security.user.*` é usado em algum lugar? | `analise-projeto.md` §6 |
| P8 | Há motivo para manter o `jackson-datatype-jsr310` 2.13.5 fixo no `pom.xml`? | `analise-projeto.md` §2 |

## Flyway (implantado em 2026-11-12 no `-dev`)
Detalhes em `docs/Flyway.md`.

| # | Pergunta | Afeta |
|---|---|---|
| P22 | Trocar `spring.jpa.hibernate.ddl-auto=update` por `validate` nos dois perfis? O Flyway cria toda a estrutura e o teste `TestJDevERPPROApplication.testMainBancoVazio` mostra que o `update` não muda nada depois dele. Mas o `update` ainda pode criar coluna ou tabela fora das migrações quando uma entidade muda. Não mudado: a regra do `CLAUDE.md` pede decisão. | `application-*.properties`, `CLAUDE.md` |
| P23 | O código do `-pro` já tem o Flyway (copiado do `-dev` em 2026-11-12), mas o banco `jdev-erp-pro-prod` ainda não foi adotado: o `-pro` nunca subiu nele com o Flyway. Quando e como fazer essa primeira subida? O banco está sem o V2 (P13), então ela precisa de `SPRING_FLYWAY_BASELINE_VERSION=20261112.161000`, e o Flyway aplica o V2 e as migrações `220600` e `220700`. Sem essa variável, a subida é recusada e nada é gravado. Com o baseline padrão, uma cópia do prod foi recusada, como esperado. O prod também tem duas uniques que o dev não tem, sobras de versões antigas das entidades: `unique_sku` (SKU único no banco inteiro, contra o `unique_sku_empresa` por empresa) e `unique_inscricao_estadual` (duplicata de `unique_incricao_estadual`). Elas saem por migração? | `-pro`, `docs/Flyway.md` §8 |
| P24 | A carga inicial cria o admin `alex.fernando.egidio@gmail.com` com a senha de teste escrita no próprio script. Num banco novo de produção, essa senha precisa ser trocada logo depois da primeira subida. Manter assim, ou mudar a carga para um admin sem senha conhecida (com uma migração nova)? | `V20261112_161000__carga_inicial.sql` |

## Decididas em 2026-11-11, a implementar
Decisão tomada; falta implementar. As perguntas que sobraram estão em **A confirmar**.

**P14 — Usuário opcional na edição de `ClienteFuncionario`:** sim, como já é no cadastro, porque a associação acontece depois, quando o usuário é criado.
- Ajustar o `ClienteFuncionarioService.atualizar()`. Não precisa de script.

**P16 — Plano inativo ou vigência vencida bloqueia o acesso:** sim. Bloqueia todos os usuários da empresa, no login e nas requisições, como já faz o `bloqueio` (401).
- Implementar em `UsuarioService.login()` e `UsuarioDetailsService.loadUserByUsername()`.
- **A confirmar:**
  - `vigenciaPlano` vazia significa sem vencimento ou bloqueado?
  - O acesso ainda vale no próprio dia do vencimento?
  - O `ROLE_ADMIN` também fica bloqueado? Se ficar, um admin cujo plano venceu não consegue entrar para renovar. Sugestão: o admin não é bloqueado.
  - `limiteUsuario` e `limiteCliente` devem impedir cadastro novo acima do limite? (Parte da pergunta original ainda sem resposta.)
  - `totalUsuario` e `totalCliente` da empresa devem ser calculados pelo back-end? Hoje vêm do request.

**P18 — Totais de pedido e de item calculados no back-end:** sim.
- **A confirmar as fórmulas.** O `ItemPedido` não tem preço unitário.
  - Proposta: guardar o `Produto.preco` do momento numa coluna nova `item_pedido.valor_unitario` (script V2).
  - Item: `subTotal = quantidade × valorUnitario` e `total = subTotal − desconto`.
  - Pedido: `subTotal = soma dos totais dos itens` e `total = subTotal − desconto + frete + taxas`.
  - Recalcular ao incluir, alterar ou excluir item.
  - O desconto é em valor (R$) ou em percentual?

## Em aberto: proposta conferida com os enums reais

### P17 — Estoque
A classificação proposta foi comparada com o `TipoMovimentacaoProduto`. A tabela abaixo usa os nomes que existem no código.

| Tipo (enum atual) | Efeito proposto |
|---|---|
| `ENTRADA_COMPRA`, `ENTRADA_DEVOLUCAO_CLIENTE`, `ENTRADA_TRANSFERENCIA`, `ENTRADA_PRODUCAO`, `ENTRADA_AJUSTE` | soma no estoque físico |
| `ENTRADA_BONIFICACAO` | soma (não estava na proposta) |
| `ENTRADA_INVENTARIO` / `SAIDA_INVENTARIO` | soma ou subtrai a diferença do inventário |
| `SAIDA_VENDA`, `SAIDA_DEVOLUCAO_FORNECEDOR`, `SAIDA_TRANSFERENCIA`, `SAIDA_CONSUMO_INTERNO`, `SAIDA_PERDA`, `SAIDA_AJUSTE` | subtrai do estoque físico |
| `SAIDA_AVARIA`, `SAIDA_FURTO`, `SAIDA_BONIFICACAO`, `SAIDA_BRINDE` | subtrai (não estavam na proposta) |
| `RESERVA_ESTOQUE` / `LIBERACAO_RESERVA` | não mexe no físico: soma ou tira da reserva |

**Diferenças entre a proposta e o código**
- `SAIDA_PRODUCAO` e `CANCELAMENTO` não existem no enum.
  - Criar exige atualizar o CHECK `movimentacao_produto_tipo_movimentacao_produto_check` por script.
  - Alternativa para o cancelamento: gerar o movimento inverso.
- Não há estoque reservado: o `Produto` só tem `estoque` e `estoqueMinimo`. É preciso uma coluna nova, como `estoque_reservado` (script V2), e definir se o disponível é `estoque − reservado`.

**Pedido de venda: os eventos da proposta no `StatusPedido` atual**

| Evento | Status atual | Efeito |
|---|---|---|
| criado | `AGUARDANDO_PAGAMENTO` | reserva |
| aprovado | `PAGAMENTO_APROVADO` | — |
| separado | `SEPARADO` | — |
| faturado ou expedido | `FATURADO` | baixa o físico e libera a reserva |
| cancelado | `CANCELADO` | libera a reserva |
| devolução recebida | `DEVOLVIDO` | soma no físico |

**A confirmar**
- Cancelar depois de `FATURADO` devolve o estoque?
- Pedido de compra:
  - Não há status para "enviado ao fornecedor", "recebido" nem "recebimento parcial".
  - O `ItemPedido` não tem quantidade recebida.
  - Criar os status novos ou separar o fluxo de compra?
- O que fazem com o estoque os outros tipos de pedido? `ORCAMENTO` (reserva ou não?), `PEDIDO_PRODUCAO`, `PEDIDO_TRANSFERENCIA`, `DEVOLUCAO_CLIENTE`, `DEVOLUCAO_FORNECEDOR`, `BONIFICACAO`, `BRINDE`, `CONSIGNACAO`, `REMESSA` e `RETORNO`.
- Movimentação lançada à mão pela API:
  - altera o estoque na hora?
  - editar ou excluir a movimentação desfaz o efeito?
- **Estoque nunca chega a 0:** pela P15, o estoque fica como o CHECK do banco, `estoque >= 1`.
  - Com a baixa automática, vender a última unidade quebraria o CHECK e daria erro.
  - É preciso decidir entre trocar o CHECK para `>= 0` (script V2) ou recusar a venda que deixaria o estoque abaixo de 1.
- Idempotência: o movimento automático de cada mudança de status deve ser gerado uma vez só. Proposta: guardar o vínculo pedido + tipo + item.

### P19 — Transições de status
A matriz proposta para `Pedido` usa status que não existem: `ABERTO`, `CONFIRMADO`, `PRONTO` e `CONCLUIDO`. O `StatusPedido` tem 13 valores. Esta é a matriz refeita com eles, para confirmar:

| Status atual | Próximos permitidos (proposta) |
|---|---|
| `AGUARDANDO_PAGAMENTO` (inicial) | `PAGAMENTO_APROVADO`, `PAGAMENTO_RECUSADO`, `CANCELADO` |
| `PAGAMENTO_RECUSADO` | `AGUARDANDO_PAGAMENTO`, `CANCELADO` |
| `PAGAMENTO_APROVADO` | `EM_SEPARACAO`, `CANCELADO` |
| `EM_SEPARACAO` | `SEPARADO`, `CANCELADO` |
| `SEPARADO` | `FATURADO`, `CANCELADO` |
| `FATURADO` | `AGUARDANDO_ENVIO`, `ENTREGUE` (retirada no balcão) |
| `AGUARDANDO_ENVIO` | `ENVIADO` |
| `ENVIADO` | `EM_TRANSPORTE`, `ENTREGUE` |
| `EM_TRANSPORTE` | `ENTREGUE`, `DEVOLVIDO` |
| `ENTREGUE` | `DEVOLVIDO` |
| `DEVOLVIDO` | `REEMBOLSADO` |
| `CANCELADO` | `REEMBOLSADO`, só se o pedido já foi pago |
| `REEMBOLSADO` | nenhum (final) |

Para o `Chamado`, todos os status da proposta existem. O `StatusChamado` tem mais quatro: `EM_ANALISE`, `EM_DESENVOLVIMENTO`, `EM_TESTES` e `AGUARDANDO_HOMOLOGACAO`. Esta é a matriz com eles encaixados:

| Status atual | Próximos permitidos (proposta) |
|---|---|
| `ABERTO` (inicial) | `EM_ANALISE`, `EM_ATENDIMENTO`, `CANCELADO` |
| `EM_ANALISE` | `EM_ATENDIMENTO`, `EM_DESENVOLVIMENTO`, `AGUARDANDO_CLIENTE`, `CANCELADO` |
| `EM_ATENDIMENTO` | `AGUARDANDO_CLIENTE`, `EM_DESENVOLVIMENTO`, `RESOLVIDO`, `CANCELADO` |
| `AGUARDANDO_CLIENTE` | `EM_ATENDIMENTO`, `CANCELADO` |
| `EM_DESENVOLVIMENTO` | `EM_TESTES`, `CANCELADO` |
| `EM_TESTES` | `AGUARDANDO_HOMOLOGACAO`, `EM_DESENVOLVIMENTO` |
| `AGUARDANDO_HOMOLOGACAO` | `RESOLVIDO`, `EM_DESENVOLVIMENTO` |
| `RESOLVIDO` | `FECHADO`, `EM_ATENDIMENTO` (reabertura) |
| `FECHADO`, `CANCELADO` | nenhum (finais) |

**A confirmar**
- As duas matrizes acima.
- O status inicial é sempre o da tabela, ou o cadastro aceita outro?
- O pedido de compra e o orçamento seguem o mesmo fluxo de venda? O orçamento não tem pagamento.
- Datas automáticas (proposta):
  - `PAGAMENTO_APROVADO` preenche `dataPagamento`;
  - `CANCELADO` preenche `dataCancelamento`;
  - `FECHADO` preenche `fechadoUser` (o usuário logado) e `dataFechamento`.
- Histórico de status (anterior, novo, usuário, data, motivo):
  - Proposta: tabelas novas `pedido_status_historico` e `chamado_status_historico` (script V2).
  - O motivo é obrigatório no cancelamento e na reabertura?
- Quem pode mudar o status: qualquer usuário da empresa ou só alguns?

## Resolvidas
- **Token gravado no usuário:** confirmado. `existsByTokenParaUser` confere `usuario.token_sessao`.
- **`mvn package` / `spring-boot:run` (2026-11-12):** falhavam com "Unable to find main class" por causa da main de instância (`void main`).
  - A main passou a `public static void main`, porque o `JarLauncher` do Spring Boot 4.0.6 também só chama main estática.
  - O JAR é gerado e roda com `java -jar`.
- **P25 — Proteção do administrador e `deletar-todos` de usuários (2026-11-12):** decidido e implementado.
  - Usuário com `ROLE_ADMIN` nunca é apagado pela API, por ninguém. Para apagar, outro admin remove antes a `ROLE_ADMIN` dele.
  - O admin logado não remove o próprio acesso: não se apaga, não se bloqueia (`liberado=false`), não remove nem altera as próprias associações em `RoleUsuario` e não renomeia nem apaga uma role que ele tem (ex.: `ROLE_ADMIN`). As recusas dizem o que fazer.
  - `deletar-todos` apaga só os usuários que não são admin e solta antes o vínculo com `cliente_funcionario` (a falha por FK acabou).
  - Complemento (2026-11-12):
    - um admin não bloqueia outro (`liberado=false` ou nulo);
    - o admin logado não apaga nem bloqueia a própria empresa;
    - `ROLE_USER` e `ROLE_ADMIN` não podem ser renomeadas nem apagadas, por ninguém;
    - `liberado` nulo vira `false` no cadastro e na edição de usuário, e `Usuario.isEnabled()` trata nulo como bloqueado;
    - `bloqueio` nulo vira `false` no cadastro e na edição de empresa.
  - Mantido de propósito: bloquear outra empresa, mesmo com admins, continua permitido (é como se corta um cliente do SaaS).
  - Regras 17a, 27b, 31a a 31g e 44a de `docs/regras-negocio.md`. Testes: `TestUsuarioService`, `TestRoleService`, `TestRoleUsuarioService`, `TestEmpresaService`, `TestUsuario`, `TestUsuarioController`, `TestRoleController`, `TestRoleUsuarioController`, `TestEmpresaController`.
- **P4 — execução do `V1_INIT.sql` (2026-11-12):** resolvida pela adoção do Flyway.
  - O script virou a migração `V20261112_161000__carga_inicial.sql` e roda sozinho, só em banco novo.
  - Uma trava recusa a execução se `plano`, `empresa` ou `pessoa` tiverem dados.
  - Banco existente recebe baseline e não a executa (`docs/Flyway.md`).
- **PostgreSQL local:** é o serviço `postgresql-x64-18`, na porta 5433.
- **Testes:** os 9 passam com o JDK 25 e `-Dmaven.compiler.proc=full`.
- **Extensão `unaccent`:** está ativa no banco dev. O `pg_trgm` ainda não é usado.
- **P7 — erro do `JwtFilter` (2026-11-11):** decidido responder conforme o erro.
  - Token recusado responde 401 pelo `JwtAuthenticationEntryPoint`, com o motivo na mensagem: inválido, expirado, sessão substituída, usuário ou empresa bloqueados.
  - Erro inesperado responde 500 pelo `GlobalExceptionHandler`.
  - O login continua aberto com um token recusado no header. Antes da correção, esse caso falhava.
  - O `JwtService.validarToken` saiu: a validação fica no `extrairLogin`.
  - Testes: `TestJwtFilter` e `TestJwtFilterUnitario`.
- **P9 — `@PreAuthorize` (2026-11-11):** decidido usar nos dois lugares, `@PreAuthorize` no controller e `isAdmin()` no service.
  - Todo controller tem `@PreAuthorize`: `isAuthenticated()` para dados da empresa e `hasAuthority('ROLE_ADMIN')` para a área de administração.
  - O `isAdmin()` continua no service.
  - A negação do `@PreAuthorize` responde 403 pelo `GlobalExceptionHandler`. Antes, cairia no handler genérico como 500.
- **P10 — área de administração só para `ROLE_ADMIN` (2026-11-11):**
  - Só o admin acessa `Plano`, `Role`, `RoleUsuario`, `Empresa` (o usuário comum vê só a própria empresa) e o cadastro, a edição e a exclusão de usuários.
  - Corrigidos também:
    - o usuário comum só troca a própria senha;
    - a injeção de JPQL pelo `sort` da paginação, que permitia ler o token de sessão do admin;
    - a ordem das verificações em `EmpresaService.salvar()`.
  - Testes: `security/TestPermissoesAdmin` (os endpoints da área admin com um usuário comum) e os `Test*Service`.
- **P11 — Plano FREE (2026-11-11):** o FREE é inserido direto no banco e não se cadastra outro pela API.
  - `PlanoService.salvar()` recusa FREE quando já existe um.
  - `atualizar()` recusa transformar outro plano em FREE.
  - O valor segue o CHECK `plano_valor_mensal_check`: FREE com 0 (só o próprio FREE pode ser editado com 0); os demais, de R$ 49 a R$ 200.
  - Testes: `TestPlanoService` e `TestPlanoController`.
- **P12 — Unicidade de CPF, CNPJ, e-mail e inscrição estadual (2026-11-11):** únicos no banco inteiro, como já é hoje; vale também para `ClienteFuncionario.unique_pessoa`. Nada a mudar.
- **P13 — CPF e CNPJ (2026-11-11, revisada no mesmo dia):** e-mail obrigatório para qualquer pessoa; CPF obrigatório só para pessoa física; CNPJ obrigatório só para pessoa jurídica. Os três continuam únicos no banco inteiro (P12).
  - Substitui a primeira decisão do dia (CPF, CNPJ e e-mail obrigatórios para todos).
  - Banco: `V2_PESSOA_DOCUMENTO_POR_TIPO.sql` tira o `NOT NULL` de `cpf` e `cnpj` e cria `pessoa_cpf_pessoa_fisica_check`, `pessoa_cnpj_pessoa_juridica_check` e `pessoa_email_preenchido_check`. O script não altera dados e para sem mudar nada se algum cadastro estiver fora da regra.
  - **Aplicado em:** dev em 2026-11-11 (sem erro; os 3 cadastros já cumpriam a regra). Prod pendente: o `-pro` já tem o código novo (2026-11-12), então o V2 precisa ir para o banco `jdev-erp-pro-prod` antes de subir o `-pro`. Sem ele, cadastrar pessoa física sem CNPJ ou jurídica sem CPF esbarra no `NOT NULL` do banco.
  - **Desde 2026-11-12** é a migração Flyway `V20261112_162000__pessoa_documento_por_tipo.sql`. Se o `-pro` adotar o Flyway (P23), o prod recebe baseline em `20261112.161000` e o Flyway aplica o V2 sozinho.
  - Back-end: `Pessoa.isCpfInformadoParaPessoaFisica()` e `isCnpjInformadoParaPessoaJuridica()` (`@AssertTrue`); `setCpf`/`setCnpj` gravam em branco como `NULL`, para não bater na unique. `@NotNull` em `ativo` continua.
  - Testes: `model/TestPessoa` e `TestPessoaController`.
- **P15 — Preço e estoque (2026-11-11):** decidido seguir os CHECK do banco: preço, estoque e estoque mínimo obrigatórios e no mínimo 1.
  - No `Produto`, entrou `@NotNull` nos três, e as mensagens agora dizem "no mínimo" (antes diziam "maior que 1").
  - Sem script.
  - Ver o efeito no estoque em P17.
- **P20 — Datas e anexos (2026-11-11):** decidido seguir as restrições do banco.
  - `Pessoa.dataCadastro`: o `PessoaService` preenche com a data de hoje no cadastro e mantém a data na edição; a data do request é ignorada.
  - `Mensagem.arquivo` continua obrigatório (`NOT NULL`), agora com `@NotBlank` e mensagem.
  - Testes: `TestPessoaService`, `TestPessoaController` e `TestMensagemController`.
- **P21 — Exclusão em lote (2026-11-11):** só `ROLE_ADMIN`.
  - Vale para `deletarTodos` e `deletarTodosPorIds` de `ClienteFuncionario`, `ItemPedido`, `Mensagem`, `MovimentacaoProduto`, `Pedido`, `Pessoa` e `Produto`.
  - A checagem fica no controller (`@PreAuthorize`) e no service (403 "Apenas administradores podem excluir registros em lote.").
  - Os `deletarPorPedido`, `deletarPorChamado` e `deletarTodosPorChamado` continuam abertos: apagam só os filhos de um registro.
  - **Multiempresa:** mesmo para o admin, a exclusão em lote só alcança a empresa logada (o `empresaId` vem do token, nunca do request). Reforçado no `JpaJdevRepositoryImpl`:
    - a empresa da entidade é detectada pelo metamodelo do JPA (antes, `getDeclaredField`, que não via campo herdado e desligaria o filtro em silêncio);
    - `deleteAll(empresaId)` e `deletarAllById(ids, empresaId)` recusam entidade global (`Plano`, `Role`, `Empresa`, `RoleUsuario`) e empresa nula;
    - os métodos do Spring Data que apagavam sem filtro (`deleteAllByIdInBatch`, `deleteAllInBatch(lista)`, `deleteAllById`, `deleteAll(lista)`) e os `getOne`/`getById` passaram a ser bloqueados em entidade com empresa.
  - Nenhuma FK do banco tem `ON DELETE CASCADE` (conferido em 2026-11-11), então apagar na empresa logada não arrasta registro de outra; se outra empresa referenciar o registro, o banco recusa e nada é apagado.
  - Testes: `TestPermissoesAdmin`, os `Test*Service`, `repository/TestJpaJdevRepositoryImpl` e `security/TestIsolamentoExclusaoEmLote` (o admin de uma empresa tenta apagar em lote os registros de outra, sozinhos e misturados com os seus, e roda todos os `deletarTodos`; os registros das outras empresas não mudam).
