# Regras de negócio — Jdev ERP Pro

Levantadas do código de `jdev-erp-pro-back-end-dev/` (services, entidades e `V1_INIT.sql`) em 2026-10-28. O documento descreve só o que o código faz hoje, sem regras inventadas. Pontos com intenção incerta citam a pendência correspondente (Pn) de `docs/pendencias.md`.
Formato: regra — onde está implementada. As classes ficam em `br.com.jdeverp.pro.service` e `br.com.jdeverp.pro.model`.

## 0. Regras gerais (todos os módulos com empresa)

1. **A empresa vem sempre do usuário logado.** Em salvar e atualizar, `empresa` é sobrescrita com `UsuarioLogadoService.getEmpresaLogada()`, ignorando o que vier no request. Vale para todos os services de entidades com empresa: `Pessoa`, `ClienteFuncionario`, `Categoria`, `Produto`, `MovimentacaoProduto`, `Pedido`, `ItemPedido`, `Chamado`, `Mensagem` e `UsuarioService.salvar/atualizar`.
2. **Toda consulta de repository filtra por `empresa.id`.** Sem esse filtro, a aplicação não sobe (exceção: `@IgnoreEmpresaId`) — `RepositoryEmpresaValidator.afterSingletonsInstantiated()`.
3. **Métodos sem empresa são bloqueados.** `findAll()`, `findById()`, `count()`, `deleteById(id)` e similares lançam exceção em entidades com empresa — `JpaJdevRepositoryImpl.validar()`.
4. **Excluir por id exige que o registro exista na empresa logada.** Caso contrário: "... não encontrado ou já foi deletado" — `deleteById()` de todos os services.
5. **A exclusão em lote é tudo ou nada.** Se algum id não existir na empresa, nada é apagado e a mensagem lista os ids não encontrados. Uma lista vazia também é recusada. — `deletarAllById()` em `PessoaService`, `ProdutoService`, `ClienteFuncionarioService`, `PedidoService`, `ItemPedidoService`, `MovimentacaoProdutoService`, `MensagemService` e `UsuarioService`.
6. **"Apagar todos" apaga todos os registros da empresa logada.** Exige que exista pelo menos um registro — `deleteAll(empresaId)` nos mesmos 8 services.
7. **Nomes são comparados de forma tolerante.** Comparações de nome, título e número (unicidade e busca) ignoram acento, maiúsculas e espaços nas pontas — `@Query` com `unaccent(upper(trim(...)))` nos repositories.

## 1. Acesso e usuários (`Usuario`, `Role`, `RoleUsuario`)

**Login e sessão**
1. O login localiza o usuário só pelo login, sem considerar empresa — `UsuarioService.login()`, `UsuarioRepository.buscaPorLogin()` (`@IgnoreEmpresaId`).
2. Login inexistente é recusado com 401 — `UsuarioService.login()`.
3. Usuário com `liberado=false` não entra (401) — `UsuarioService.login()`, `UsuarioDetailsService.loadUserByUsername()`.
4. Empresa com `bloqueio=true` impede o login de todos os seus usuários (401) — `UsuarioService.login()`, `UsuarioDetailsService.loadUserByUsername()`.
5. Senha errada é recusada (401). A conferência usa BCrypt — `UsuarioService.login()`.
6. O token JWT vale `jwt.expiration` (24h) e carrega `usuarioId`, `empresaId` e `login` — `JwtService.gerarToken()`.
7. **Uma sessão ativa por usuário:**
   - o login grava o token em `usuario.token_sessao` — `UsuarioService.login()` / `UsuarioRepository.updateTokenSessaoLogin()`;
   - cada requisição só é aceita se o token for o gravado — `UsuarioDetailsService.existsByTokenParaUser()`.
   - Na prática, um novo login derruba o anterior.
8. O bloqueio de usuário ou empresa também vale para quem já tem token: a cada requisição, `JwtFilter` → `UsuarioDetailsService.loadUserByUsername()` reavalia `liberado` e `bloqueio`.

**Cadastro e edição de usuário**
9. Só `ROLE_ADMIN` cadastra ou edita usuários (400) — `UsuarioService.salvar()`, `UsuarioService.atualizar()`.
10. Login não pode repetir:
    - o service verifica dentro da empresa logada — `UsuarioService.salvar()` (`existePorLogin`);
    - no banco, a unicidade é global — `Usuario` (`unique_login`).
11. A senha precisa ter pelo menos 5 caracteres. O código recusa `< 5`, mas a mensagem diz "mais de 5" — `UsuarioService.salvar()`.
12. Cada pessoa tem no máximo um usuário:
    - `UsuarioService.salvar()` (`existePorPessoa`);
    - `UsuarioService.atualizar()` (`existeOutroUsuarioComPessoa`).
13. O usuário precisa de um `ClienteFuncionario` da mesma pessoa na empresa logada:
    - verificação em `UsuarioService.salvar()` e `atualizar()` (`findByPessoa`);
    - obrigatoriedade na entidade: `Usuario.clienteFuncionario` com `@NotNull`.
14. O usuário criado pertence à empresa do admin que o cadastrou — `UsuarioService.salvar()`.
15. Todo usuário novo recebe `ROLE_USER` — `UsuarioService.salvar()`.
16. Ao criar o usuário, o `ClienteFuncionario` recebe o vínculo de volta (`clienteFuncionario.usuario`) — `UsuarioService.salvar()`.
17. Editar o usuário não altera a senha — `UsuarioService.atualizar()`.
18. A senha nunca volta na resposta:
    - `UsuarioService.salvar()` e `atualizar()` mascaram com `***Ocultada***`;
    - os campos sensíveis de `Usuario` têm `@JsonIgnore`.
19. Ao excluir um usuário, ele é desvinculado do `ClienteFuncionario` antes — `UsuarioService.deleteById()` (`removeUserClienteFuncionarioId`).
20. Login e senha são obrigatórios, e o hash da senha é único no banco — `Usuario` (`@NotBlank`, `unique_senha`).

**Troca de senha** — `UsuarioService.alterarSenha()`
21. O usuário precisa estar liberado e a empresa, desbloqueada.
22. A nova senha precisa ser igual à confirmação.
23. A nova senha precisa ser diferente da atual.
24. A senha atual informada precisa conferir com a gravada.

**Roles (`/api/role`)**
25. `acesso` precisa começar com `ROLE_` — `RoleService.salvar()`, `RoleService.atualizar()`.
26. `acesso` é gravado em maiúsculas — `Role.setAcesso()`.
27. `acesso` é único:
    - `RoleService.salvar()` (`existePorAcesso`);
    - `RoleService.atualizar()` (`existePorAcessoDiferenteId`);
    - `Role` (`unique_acesso`).
28. É admin quem tem `ROLE_ADMIN`. O admin é global, sem vínculo a uma empresa — `Usuario.idAdmin()`, `UsuarioLogadoService.isAdmin()`.

**Atribuição de roles (`/api/roleUsuario`)**
29. Acesso e usuário são obrigatórios — `RoleUsuarioService.salvar()`, `RoleUsuarioService.atualizar()`.
30. O mesmo acesso não pode ser atribuído duas vezes ao mesmo usuário:
    - `RoleUsuarioService.salvar()` (`existePorUsuarioERole`);
    - `RoleUsuarioService.atualizar()` (`existePorUsuarioDiferenreId`);
    - `RoleUsuario` (`unique_role_user`).
31. Só é possível excluir atribuições de usuários da empresa logada — `RoleUsuarioService.deleteById()` (`buscarPorIdEEmpresa`).

## 2. SaaS: planos e empresas (`Plano`, `Empresa`)

**Plano (global)**
32. O nome do plano é único em todo o sistema — `PlanoService.salvar()`, `PlanoService.atualizar()`.
33. Nome, descrição, valor mensal, limites e tipo são obrigatórios — `Plano` (`@NotBlank`/`@NotNull`).
34. Pela API, o valor mensal vai de R$ 49 a R$ 200 — `Plano.valorMensal` (`@Min(49)`, `@Max(200)`).
35. No banco, o plano `FREE` deve ter valor 0, e os demais, entre 49 e 200 — CHECK `plano_valor_mensal_check` em `V1_INIT.sql`.
36. O limite de usuários e o limite de clientes vão de 1 a 150 — `Plano.limiteUsuario`, `Plano.limiteCliente`.
37. Tipos de plano: `FREE`, `PRO`, `ENTERPRISE` — `TipoPlano`.
38. Planos do seed — `V1_INIT.sql`:
    - "Plano Free": FREE, R$ 0, 4 usuários, 2 clientes;
    - "Plano Pro": PRO, R$ 50, 30 usuários, 15 clientes.

**Empresa (global)**
39. Só `ROLE_ADMIN` cadastra, edita ou exclui empresas (403) — `EmpresaService.salvar()`, `atualizar()`, `deleteById()`.
40. Cada pessoa pode ter no máximo uma empresa:
    - `EmpresaService.salvar()` (`existePorPessoa`);
    - `Empresa` (`unique_pessoa_empresa`).
41. Plano e pessoa são obrigatórios, e o plano precisa existir — `EmpresaService.validarPlano()`, `EmpresaService.salvar()`, `atualizar()`.
42. A logomarca é obrigatória — `Empresa.logoMarca` (`@NotNull`, `@NotEmpty`).
43. Valores padrão de uma empresa nova — `Empresa`:
    - `totalUsuario=0`;
    - `totalCliente=0`;
    - `planoAtivo=false`;
    - `bloqueio=false`.
44. `bloqueio=true` bloqueia login e uso de todos os usuários da empresa (ver regras 4 e 8).

## 3. Cadastros (`Pessoa`, `ClienteFuncionario`)

**Pessoa**
45. O nome da pessoa é único dentro da empresa — `PessoaService.salvar()` (`existePorNome`), `PessoaService.atualizar()` (`existePorNomeDiferenteId`).
46. Só é possível editar pessoas da empresa logada — `PessoaService.atualizar()` (`existsById(id, empresaId)`).
47. CPF é obrigatório e validado — `Pessoa.cpf` (`@NotNull`, `@CPF`).
48. CNPJ é validado e aceita o formato alfanumérico — `Pessoa.cnpj` (`@CNPJ(format = ALPHANUMERIC)`). No banco a coluna é `NOT NULL`.
49. E-mail é validado e, no banco, `NOT NULL` — `Pessoa.email` (`@Email`).
50. CPF, CNPJ, e-mail e inscrição estadual são únicos **em todo o banco**, não só na empresa — `Pessoa` (`unique_cpf`, `unique_cnpj`, `unique_email`, `unique_incricao_estadual`).
51. Campos obrigatórios — `Pessoa` (`@NotBlank`/`@NotNull`):
    - nome, telefone, tipo de pessoa;
    - CEP, logradouro, bairro, estado, cidade, país.
52. Tipos de pessoa: `FISICA`, `JURIDICA` — `TipoPessoa`.
53. Uma pessoa nova é criada com `ativo=true`. A observação aceita até 1000 caracteres — `Pessoa`.

**Cliente / funcionário**
54. Uma pessoa tem no máximo um cadastro de cliente/funcionário:
    - o service verifica dentro da empresa — `ClienteFuncionarioService.salvar()` (`existeParaPessoa`);
    - no banco, a unicidade é global — `ClienteFuncionario` (`unique_pessoa`).
55. Um usuário só pode estar vinculado a um cliente/funcionário — `ClienteFuncionario` (`unique_usuario`).
56. Tipo e pessoa são obrigatórios — `ClienteFuncionario` (`@NotNull`).
57. Tipos possíveis: `CLIENTE`, `FUNCIONARIO`, `PRESTADOR_SERVICO` — `TipoClienteFuncionario`.
58. No cadastro, o usuário é opcional: ele é vinculado depois, por `UsuarioService.salvar()` — `ClienteFuncionarioService.salvar()`.
59. Na edição, pessoa e usuário são obrigatórios e precisam ser da empresa logada — `ClienteFuncionarioService.atualizar()`.

## 4. Produtos e estoque (`Categoria`, `Produto`, `MovimentacaoProduto`)

**Categoria**
60. O nome da categoria é obrigatório e único dentro da empresa — `Categoria.nome`, `CategoriaService.salvar()`, `CategoriaService.atualizar()`.
61. A categoria do seed pertence à empresa 1 (30 categorias padrão) — `V1_INIT.sql`.

**Produto**
62. O nome do produto é único dentro da empresa:
    - `ProdutoService.salvar()` e `atualizar()`;
    - `Produto` (`unique_nome_produto_empresa`).
63. O SKU é obrigatório e único dentro da empresa. A unicidade só é garantida pelo banco, sem checagem no service — `Produto` (`unique_sku_empresa`).
64. A categoria é obrigatória e precisa ser da empresa logada — `ProdutoService.validarCategoria()`.
65. Nome, imagem e unidade de medida são obrigatórios — `Produto`.
66. Preço, estoque e estoque mínimo precisam ser ≥ 1. As mensagens dizem "maior que 1" — `Produto` (`@Min(1)`).
67. Unidades de medida: `CENTIMETRO`, `METRO`, `UNIDADE`, `QUILO` — `UnidadeMedida`.

**Movimentação de produto**
68. O produto é obrigatório e precisa ser da empresa logada — `MovimentacaoProdutoService.validarProduto()`.
69. O pedido é opcional, mas, se informado, precisa ser da empresa logada — `MovimentacaoProdutoService.validarPedido()`.
70. Sem data informada, `dataMovimento` vira a data de hoje. Na edição, a data só muda se vier no request — `MovimentacaoProdutoService.salvar()`, `atualizar()`.
71. A quantidade mínima é 0,1 (padrão 1) e o tipo é obrigatório — `MovimentacaoProduto` (`@DecimalMin("0.1")`, `@NotNull`).
72. Tipos — `TipoMovimentacaoProduto`:
    - entradas: compra, devolução, transferência, produção, ajuste, bonificação, inventário;
    - saídas: venda, devolução ao fornecedor, transferência, consumo interno, perda, avaria, furto, bonificação, brinde, ajuste, inventário;
    - `RESERVA_ESTOQUE`;
    - `LIBERACAO_RESERVA`.

## 5. Vendas (`Pedido`, `ItemPedido`)

**Pedido**
73. O número do pedido é obrigatório e único dentro da empresa:
    - `PedidoService.salvar()` (`existePorNumeroPedido`);
    - `PedidoService.atualizar()` (`existePorNumeroPedidoDiferenteId`).
74. Vendedor e cliente são obrigatórios e precisam ser **usuários** da empresa logada — `PedidoService.validarUsuario()`.
75. Status, forma de pagamento, tipo e data do pedido são obrigatórios — `Pedido` (`@NotNull`).
76. Os valores (`subTotal`, `desconto`, `frete`, `taxas`, `total`) vêm do request, com padrão 0, sem nenhum cálculo no back-end — `Pedido`, `PedidoService.salvar()`, `atualizar()`.
77. Valores possíveis:
    - status: `AGUARDANDO_PAGAMENTO` … `REEMBOLSADO` (13 valores) — `StatusPedido`;
    - formas de pagamento: 16 valores — `FormaPagamento`;
    - tipos de pedido: 12 valores — `TipoPedido`.

**Item do pedido**
78. Pedido e produto são obrigatórios e precisam ser da empresa logada — `ItemPedidoService.validarPedido()`, `ItemPedidoService.validarProduto()`.
79. O mesmo produto não pode aparecer duas vezes no mesmo pedido. A comparação é pelo **nome** do produto — `ItemPedidoService.salvar()` (`existePorNomePorPedido`), `atualizar()` (`existePorNomeDiferenteIdPorPedido`).
80. A quantidade mínima é 0,1 (padrão 1) — `ItemPedido.quantidade` (`@DecimalMin("0.1")`).
81. Para excluir um item, ele precisa pertencer ao pedido informado — `ItemPedidoService.deleteById(id, idPedido, idEmpresa)`.
82. Os valores do item (`subTotal`, `desconto`, `total`) vêm do request, sem cálculo — `ItemPedidoService.salvar()`, `atualizar()`.

## 6. Atendimento (`Chamado`, `Mensagem`)

**Chamado**
83. O título do chamado é único dentro da empresa — `ChamadoService.salvar()` (`existePorTitulo`), `atualizar()` (`existePorTituloDiferenteId`).
84. Título, descrição, tipo, prioridade e status são obrigatórios — `Chamado` (`@NotNull`).
85. Ao abrir um chamado — `ChamadoService.salvar()`:
    - `abertoUser` = usuário logado;
    - `dataAbertura` = hoje;
    - `fechadoUser` e `dataFechamento` ficam vazios.
86. Atendente e cliente são obrigatórios e precisam ser usuários da empresa logada — `ChamadoService.validarUsuario()`.
87. Na edição — `ChamadoService.atualizar()`:
    - `abertoUser` e `dataAbertura` não mudam;
    - `fechadoUser` é opcional, mas, se informado, precisa ser da empresa;
    - `dataFechamento` é livre.
88. Valores possíveis:
    - tipos: 13 valores — `TipoChamado`;
    - prioridades: `BAIXA`, `MEDIA`, `ALTA`, `CRITICA` — `PrioridadeChamado`;
    - status: `ABERTO` … `CANCELADO` (10 valores) — `StatusChamado`.

**Mensagem**
89. O chamado é obrigatório e precisa ser da empresa logada — `MensagemService.validarChamado()`.
90. Atendente e cliente são obrigatórios e precisam ser usuários da empresa logada — `MensagemService.validarUsuario()`.
91. A data de envio é sempre a data de hoje, e a edição não a altera — `MensagemService.salvar()`.
92. O conteúdo é obrigatório. Uma mensagem nova nasce com `lida=false` — `Mensagem`.
93. Para excluir uma mensagem por chamado, ela precisa pertencer ao chamado informado — `MensagemService.deleteByIdAndChamado()`.
94. Para apagar as mensagens de um chamado, o chamado precisa ter pelo menos uma — `MensagemService.deleteAllByChamado()`.

## Pontos de atenção (duplicidades, contradições e lacunas)

**Contradições entre camadas**
- **Plano FREE x validação da API.** O CHECK do banco exige valor 0 para o FREE, mas `@Min(49)` em `Plano.valorMensal` recusa 0. Pela API, não dá para cadastrar nem editar um plano FREE (regras 34 e 35). (P11)
- **Unicidade de login.** O service verifica por empresa (`existePorLogin(login, empresaId)`), mas o banco e o login são globais (`unique_login`, `buscaPorLogin` sem empresa). Um login repetido em outra empresa passa pelo service e quebra no banco.
- **Unicidade de pessoa.** O nome é único por empresa, enquanto CPF, CNPJ, e-mail e inscrição estadual são únicos no banco inteiro. Na prática, a mesma pessoa não pode estar em duas empresas. O mesmo vale para `ClienteFuncionario.unique_pessoa` (global) frente a `existeParaPessoa` (por empresa). (P12)
- **CPF e CNPJ obrigatórios ao mesmo tempo.** Isso vale para qualquer `TipoPessoa`: `cpf` com `@NotNull` e `cnpj` com `nullable=false`. A intenção provável é exigir um ou outro conforme FISICA/JURIDICA. (P13)
- **`ClienteFuncionario`: usuário opcional no cadastro, obrigatório na edição.** Um cadastro sem usuário não pode ser editado (regras 58 e 59). (P14)
- **Senha mínima.** O código aceita 5 caracteres, mas a mensagem diz "mais de 5" (regra 11).
- **Estoque e preço mínimos.** `@Min(1)` aceita 1, mas a mensagem diz "maior que 1". Além disso, nenhum produto pode ter estoque 0 (regra 66). (P15)

**Autorização inconsistente entre módulos**
- **Admin exigido de forma desigual.** `Empresa` e `Usuario` (salvar/atualizar) exigem `ROLE_ADMIN`. `Plano`, `Role` e `RoleUsuario` não exigem nada: qualquer usuário autenticado pode criar planos, criar roles e atribuir `ROLE_ADMIN` a si mesmo. (P10)
- **Status HTTP diferente para a mesma negação.** `EmpresaService` devolve 403 quando falta `ROLE_ADMIN`, e `UsuarioService` devolve 400.
- **Exclusão de usuários sem admin.** `UsuarioService.deleteById()` e `deleteAll()` não exigem admin, embora cadastrar exija. O `deleteAll` apaga inclusive o usuário logado.
- **Listagem de empresas sem admin.** Qualquer usuário autenticado lista e busca todas as empresas de todos os tenants — `EmpresaService.findAll()`, `buscaPorNome()`.

**Lacunas de isolamento por empresa**
- `CategoriaService.atualizar()` não verifica se a categoria é da empresa logada, ao contrário dos outros services.
- `ClienteFuncionarioService.salvar()` não verifica se a pessoa informada é da empresa logada. O `atualizar()` verifica.
- `RoleUsuarioService.salvar()` e `atualizar()` não verificam se o usuário informado é da empresa logada.
- `MensagemService.updateLida()` não verifica se a mensagem existe.

**Regras que o modelo sugere, mas não existem no código** (P16–P20)
- **Limites do plano não aplicados.** `limiteUsuario` e `limiteCliente` não são verificados ao cadastrar usuário ou cliente. `totalUsuario` e `totalCliente` da empresa vêm do request, não são calculados. (P16)
- **Plano ativo e vigência não verificados.** `planoAtivo` e `vigenciaPlano` não são checados no login nem em outro ponto. Só `bloqueio` bloqueia. (P16)
- **Estoque não atualizado.** `MovimentacaoProduto` e `ItemPedido` não alteram `Produto.estoque`, e nada impede vender sem estoque. (P17)
- **Totais não calculados.** Os totais de `Pedido` e `ItemPedido` não são calculados nem conferidos, e não há relação entre os itens e o total do pedido. (P18)
- **Sem transição de status.** Não há regra de mudança de status em `Pedido` nem em `Chamado`. `dataCancelamento`, `dataPagamento`, `dataFechamento` e `fechadoUser` não dependem do status (ex.: `FECHADO` sem `fechadoUser`). (P19)
- **Datas obrigatórias sem preenchimento.** `Pessoa.dataCadastro` é `NOT NULL` no banco, mas o service não preenche nem valida. O mesmo vale para `Mensagem.arquivo`, que é `NOT NULL` sem `@NotNull`. (P20)

**Defeitos pontuais encontrados ao levantar as regras**
- **`EmpresaDTO.pessoaId` recebe o id do plano.** Acontece em todos os métodos de `EmpresaService` (`dto.setPessoaId(empresa.getPlano().getId())`).
- **Ordem das verificações em `EmpresaService.salvar()`.** Ele chama `existePorPessoa` antes de checar se a pessoa é nula e antes de checar se o usuário é admin. A mensagem de duplicidade busca a pessoa só na empresa logada (`.get()` sem tratamento).
- **`null` verificado tarde demais em `UsuarioService.alterarSenha()`.** Ele chama `.get()` no `Optional` antes de testar `null`: um usuário inexistente gera 500 em vez da mensagem prevista.
- **Duplicidade de item comparada pelo nome.** `ItemPedido` compara o nome do produto, não o id (regra 79). Hoje isso funciona porque o nome é único por empresa (regra 62). Se essa regra mudar, a checagem de item duplicado deixa de valer.
- **Mensagens copiadas de outro contexto.**
  - `UsuarioService.atualizar()` diz "Apenas administradores podem cadastrar novos usuários".
  - `Pedido.dataPedido` tem a mensagem "Informe a forma de pagamento".

## Pendências
As dúvidas deste documento estão em `docs/pendencias.md` (P10 a P20).
