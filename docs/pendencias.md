# Pendências — decisões em aberto

Esta é a lista única de dúvidas do projeto. Os outros documentos citam cada item pelo número (P1 a P20).
Quando uma pendência for respondida, registre a decisão aqui, atualize os arquivos afetados e marque o item como resolvido.

## Projeto e ambiente
| # | Pergunta | Afeta |
|---|---|---|
| P1 | A IDE usada é o Eclipse? | `analise-projeto.md` §1 |
| P2 | Qual é o papel do módulo `-pro` (deploy, produção, cópia)? O trabalho continua só no `-dev`? | `CLAUDE.md` (Repositório) |
| P3 | Que nome de banco, context-path e arquivo de log o perfil `prod` deve ter? Hoje ele é cópia do dev. | `application-prod.properties` |
| P4 | Como e quando o `V1_INIT.sql` é executado (pgAdmin, psql, só na criação do banco)? | skills `nova-migration`, `novo-crud` |
| P5 | O serviço `postgresql-9.5` serve a outro sistema ou pode ser ignorado? | `CLAUDE.md` (Comandos) |
| P6 | O usuário de `spring.security.user.*` é usado em algum lugar? | `analise-projeto.md` §6 |
| P7 | Posso subir a API e chamar um endpoint com token inválido para registrar o formato do erro do `JwtFilter`? | `arquitetura.md` §4 |
| P8 | Há motivo para manter o `jackson-datatype-jsr310` 2.13.5 fixo no `pom.xml`? | `analise-projeto.md` §2 |

## Segurança
| # | Pergunta | Afeta |
|---|---|---|
| P9 | Endpoint novo deve usar `@PreAuthorize` (regra atual do CLAUDE.md) ou só o `isAdmin()` no service? | `CLAUDE.md`, `seguranca.md`, `novo-crud` |
| P10 | Criar ou alterar `Plano` e `Role`, e atribuir roles (`RoleUsuario`), deve ser só para `ROLE_ADMIN`? | `regras-negocio.md` §1 e §2 |

## Regras de negócio
| # | Pergunta | Afeta |
|---|---|---|
| P11 | O plano FREE (valor 0) deve poder ser cadastrado pela API? Hoje o `@Min(49)` impede. | `Plano` |
| P12 | CPF, CNPJ, e-mail e inscrição estadual devem ser únicos por empresa, e não no banco inteiro (inclui `ClienteFuncionario.unique_pessoa`)? | `Pessoa`, `ClienteFuncionario` |
| P13 | O CPF deve ser obrigatório só para `FISICA` e o CNPJ só para `JURIDICA`? | `Pessoa` |
| P14 | Na edição de `ClienteFuncionario`, o usuário deve ser opcional, como já é no cadastro? | `ClienteFuncionarioService` |
| P15 | Produto pode ter estoque 0? Qual é o mínimo correto de preço e de estoque mínimo? | `Produto` |
| P16 | Os limites do plano, `planoAtivo` e `vigenciaPlano` devem bloquear cadastro ou login? | `UsuarioService`, `ClienteFuncionarioService` |
| P17 | Movimentações e itens de pedido devem atualizar `Produto.estoque`? Quais tipos somam e quais subtraem? | `MovimentacaoProdutoService`, `ItemPedidoService` |
| P18 | Os totais de pedido e de item devem ser calculados no back-end? | `PedidoService`, `ItemPedidoService` |
| P19 | Quais transições de status são permitidas em `Pedido` e em `Chamado`? | `PedidoService`, `ChamadoService` |
| P20 | O back-end deve preencher `Pessoa.dataCadastro` com a data de hoje? `Mensagem.arquivo` deve ser opcional? | `PessoaService`, `Mensagem` |

## Resolvidas
- **Token gravado no usuário:** confirmado. `existsByTokenParaUser` confere `usuario.token_sessao`.
- **`mvn package` / `spring-boot:run`:** falham com "Unable to find main class".
- **PostgreSQL local:** é o serviço `postgresql-x64-18`, na porta 5433.
- **Testes:** os 9 passam com o JDK 25 e `-Dmaven.compiler.proc=full`.
- **Extensão `unaccent`:** está ativa no banco dev. O `pg_trgm` ainda não é usado.
