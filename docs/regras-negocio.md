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
6a. **A exclusão em lote nunca sai da empresa logada, nem para o admin.** O delete sempre leva `empresa.id = :empresaId`, com a empresa do token. Entidade global ou empresa nula é recusada, e os deletes em lote do Spring Data sem empresa são bloqueados — `JpaJdevRepositoryImpl.validarExclusaoEmLote()` e `validar()`.
7. **Exclusão em lote (`deletarTodos`, `deletarTodosPorIds`) é só de `ROLE_ADMIN`** (403 "Apenas administradores podem excluir registros em lote.") — `@PreAuthorize` nos controllers e `exigirAdminParaExclusaoEmLote()` nos services (P21). A exclusão dos filhos de um registro (`deletarPorPedido`, `deletarPorChamado`, `deletarTodosPorChamado`) continua aberta.
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
9. Só `ROLE_ADMIN` cadastra, edita ou exclui usuários (403) — `@PreAuthorize` em `UsuarioController` e `UsuarioService.salvar()`, `atualizar()`, `deleteById()`, `deleteAll()`. As consultas são de qualquer usuário logado, dentro da empresa dele.
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
21. O usuário comum só troca a própria senha (403 "Você só pode alterar a sua própria senha."); o admin troca a de qualquer usuário da empresa. O usuário precisa estar liberado e a empresa, desbloqueada.
22. A nova senha precisa ser igual à confirmação.
23. A nova senha precisa ser diferente da atual.
24. A senha atual informada precisa conferir com a gravada.

**Roles (`/api/role`)** — todo o endpoint é só de `ROLE_ADMIN` (403): `@PreAuthorize` em `RoleController`; `RoleService.salvar()`, `atualizar()` e `deleteById()` também conferem.
25. `acesso` precisa começar com `ROLE_` — `RoleService.salvar()`, `RoleService.atualizar()`.
26. `acesso` é gravado em maiúsculas — `Role.setAcesso()`.
27. `acesso` é único:
    - `RoleService.salvar()` (`existePorAcesso`);
    - `RoleService.atualizar()` (`existePorAcessoDiferenteId`);
    - `Role` (`unique_acesso`).
28. É admin quem tem `ROLE_ADMIN`. O admin é global, sem vínculo a uma empresa — `Usuario.idAdmin()`, `UsuarioLogadoService.isAdmin()`.

**Atribuição de roles (`/api/roleUsuario`)** — todo o endpoint é só de `ROLE_ADMIN` (403): `@PreAuthorize` em `RoleUsuarioController`; `RoleUsuarioService.salvar()`, `atualizar()`, `deleteById()` e `deleteByUsuarioAndRole()` também conferem.
29. Acesso e usuário são obrigatórios — `RoleUsuarioService.salvar()`, `RoleUsuarioService.atualizar()`.
30. O mesmo acesso não pode ser atribuído duas vezes ao mesmo usuário:
    - `RoleUsuarioService.salvar()` (`existePorUsuarioERole`);
    - `RoleUsuarioService.atualizar()` (`existePorUsuarioDiferenreId`);
    - `RoleUsuario` (`unique_role_user`).
31. Só é possível excluir atribuições de usuários da empresa logada — `RoleUsuarioService.deleteById()` (`buscarPorIdEEmpresa`).

## 2. SaaS: planos e empresas (`Plano`, `Empresa`)

**Plano (global)** — todo o endpoint é só de `ROLE_ADMIN` (403): `@PreAuthorize` em `PlanoController`; `PlanoService.salvar()`, `atualizar()` e `deleteById()` também conferem.
32. O nome do plano é único em todo o sistema — `PlanoService.salvar()`, `PlanoService.atualizar()`.
33. Nome, descrição, valor mensal, limites e tipo são obrigatórios — `Plano` (`@NotBlank`/`@NotNull`).
34. O valor mensal segue o CHECK do banco: o plano `FREE` vale 0; os demais, de R$ 49 a R$ 200 — `PlanoService.validarValorMensal()` e CHECK `plano_valor_mensal_check` em `V1_INIT.sql`.
35. O plano `FREE` é inserido direto no banco: pela API não se cadastra outro nem se transforma outro plano em FREE ("Já existe o plano FREE: ele é cadastrado direto no banco e não pode haver outro.") — `PlanoService.salvar()`, `atualizar()` (P11).
36. O limite de usuários e o limite de clientes vão de 1 a 150 — `Plano.limiteUsuario`, `Plano.limiteCliente`.
37. Tipos de plano: `FREE`, `PRO`, `ENTERPRISE` — `TipoPlano`.
38. Planos do seed — `V1_INIT.sql`:
    - "Plano Free": FREE, R$ 0, 4 usuários, 2 clientes;
    - "Plano Pro": PRO, R$ 50, 30 usuários, 15 clientes.

**Empresa (global)**
39. Só `ROLE_ADMIN` cadastra, edita, exclui, lista e pesquisa empresas (403) — `@PreAuthorize` em `EmpresaController` e `EmpresaService`. O usuário comum consulta só a própria empresa (`buscarPorId`); outra empresa dá 403 "Apenas administradores podem consultar outras empresas.". No `salvar()`, a permissão é conferida antes de tudo.
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
46. Só é possível editar pessoas da empresa logada — `PessoaService.atualizar()` (`buscarPorId(id, empresaId)`).
47. CPF é obrigatório só para pessoa física ("Informe o CPF para pessoa física"); quando informado, é validado — `Pessoa.isCpfInformadoParaPessoaFisica()` (`@AssertTrue`) e `Pessoa.cpf` (`@CPF`); no banco, `pessoa_cpf_pessoa_fisica_check` (P13, V2).
48. CNPJ é obrigatório só para pessoa jurídica ("Informe o CNPJ para pessoa jurídica"); quando informado, é validado e aceita o formato alfanumérico — `Pessoa.isCnpjInformadoParaPessoaJuridica()` (`@AssertTrue`) e `Pessoa.cnpj` (`@CNPJ(format = ALPHANUMERIC)`); no banco, `pessoa_cnpj_pessoa_juridica_check` (P13, V2).
48a. CPF ou CNPJ em branco conta como não informado e é gravado como `NULL`, para duas pessoas sem o documento não baterem na unique — `Pessoa.setCpf()`, `setCnpj()`.
49. E-mail é obrigatório para qualquer tipo de pessoa e validado — `Pessoa.email` (`@NotBlank`, `@Email`), como a coluna `NOT NULL` e `pessoa_email_preenchido_check` (V2).
49a. A data de cadastro é a de hoje, preenchida pelo back-end; a do request é ignorada e a edição mantém a original — `PessoaService.salvar()`, `atualizar()` (P20).
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
66. Preço, estoque e estoque mínimo são obrigatórios e no mínimo 1, como os CHECK do banco — `Produto` (`@NotNull`, `@Min(1)`) (P15).
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
92. Conteúdo e arquivo são obrigatórios (o arquivo, como a coluna `NOT NULL`, P20). Uma mensagem nova nasce com `lida=false` — `Mensagem`.
93. Para excluir uma mensagem por chamado, ela precisa pertencer ao chamado informado — `MensagemService.deleteByIdAndChamado()`.
94. Para apagar as mensagens de um chamado, o chamado precisa ter pelo menos uma — `MensagemService.deleteAllByChamado()`.

## Pontos de atenção (duplicidades, contradições e lacunas)

**Contradições entre camadas**
- **Unicidade de login.** O service verifica por empresa (`existePorLogin(login, empresaId)`), mas o banco e o login são globais (`unique_login`, `buscaPorLogin` sem empresa). Um login repetido em outra empresa passa pelo service e quebra no banco.
- **Unicidade de pessoa.** O nome é único por empresa, enquanto CPF, CNPJ, e-mail e inscrição estadual são únicos no banco inteiro. Na prática, a mesma pessoa não pode estar em duas empresas. O mesmo vale para `ClienteFuncionario.unique_pessoa` (global) frente a `existeParaPessoa` (por empresa). Decidido manter assim (P12).
- **`ClienteFuncionario`: usuário opcional no cadastro, obrigatório na edição.** Um cadastro sem usuário não pode ser editado (regras 58 e 59). Decidido que o usuário fica opcional também na edição; falta implementar (P14).
- **Senha mínima.** O código aceita 5 caracteres, mas a mensagem diz "mais de 5" (regra 11).
- **Estoque mínimo 1 x baixa de estoque.** Pelo CHECK do banco, o estoque nunca fica abaixo de 1 (regra 66). Quando a baixa automática existir (P17), vender a última unidade daria erro.

**Validação alinhada às restrições do banco** — corrigida em 2026-11-11 (P11, P13, P15, P20):
- Plano FREE: a API recusava o valor 0 que o CHECK exige. Agora segue o CHECK, e a API não cadastra outro FREE (regras 34 e 35).
- `Pessoa`: o CNPJ e o e-mail não tinham `@NotBlank`, e a falta só aparecia como erro do banco. Agora o e-mail é obrigatório para todos, o CPF só para pessoa física e o CNPJ só para jurídica, no back-end e no banco (script `V2_PESSOA_DOCUMENTO_POR_TIPO.sql`). A data de cadastro é preenchida pelo back-end (regras 47 a 49a).
- `Produto`: as mensagens diziam "maior que 1" para um mínimo de 1, e faltava `@NotNull` (regra 66).
- `Mensagem.arquivo` era `NOT NULL` sem validação na entidade (regra 92).

**Autorização da área de administração** — corrigida em 2026-11-11 (P9 e P10), com testes em `security/TestPermissoesAdmin` e nos `Test*Service`:
- `Plano`, `Role` e `RoleUsuario` não exigiam nada: qualquer usuário criava planos, renomeava roles e se dava `ROLE_ADMIN`. Agora são só de `ROLE_ADMIN`.
- A negação por falta de `ROLE_ADMIN` é sempre 403 (o `UsuarioService` devolvia 400).
- `UsuarioService.deleteById()` e `deleteAll()` não exigiam admin. Agora exigem.
- Qualquer usuário listava e pesquisava as empresas de todos os tenants. Agora só o admin; o usuário comum vê só a própria.
- `alterarSenha` aceitava o id de qualquer usuário da empresa. Agora o usuário comum só troca a própria senha.
- O `sort` da paginação era concatenado no JPQL: dava para injetar uma subconsulta no `order by` e ler dados de qualquer tabela, inclusive o token de sessão do admin. Agora só aceita o nome de um campo simples da entidade (400 "Campo de ordenação inválido.") — `JpaJdevRepositoryImpl.validarCampoOrdenacao()`.

**Lacunas de isolamento por empresa** — corrigidas em 2026-11-11, com testes em `TestCategoriaService`, `TestClienteFuncionarioService`, `TestRoleUsuarioService`, `TestMensagemService` e nos testes de controller:
- `CategoriaService.atualizar()` busca a categoria com `buscarPorId(id, empresaId)` e grava o registro existente ("Categoria não encontrada para a empresa logada.").
- `ClienteFuncionarioService.salvar()` busca a pessoa na empresa logada e, se vier, o usuário também.
- `RoleUsuarioService.salvar()` e `atualizar()` buscam o usuário na empresa logada e o acesso no banco. O `atualizar()` também confere se o registro é da empresa logada.
- `MensagemService.updateLida()` confere se a mensagem existe na empresa logada.
- `/api/usuario/buscar-por-login/{login}` devolvia usuário de qualquer empresa (o `buscaPorLogin` não filtra empresa, por ser a consulta do login). Agora devolve 404 para login de outra empresa.

**Regras que o modelo sugere, mas não existem no código** (P16–P19)
- **Limites do plano não aplicados.** `limiteUsuario` e `limiteCliente` não são verificados ao cadastrar usuário ou cliente. `totalUsuario` e `totalCliente` da empresa vêm do request, não são calculados. (P16)
- **Plano ativo e vigência não verificados.** `planoAtivo` e `vigenciaPlano` não são checados no login nem em outro ponto. Só `bloqueio` bloqueia. (P16)
- **Estoque não atualizado.** `MovimentacaoProduto` e `ItemPedido` não alteram `Produto.estoque`, e nada impede vender sem estoque. (P17)
- **Totais não calculados.** Os totais de `Pedido` e `ItemPedido` não são calculados nem conferidos, e não há relação entre os itens e o total do pedido. (P18)
- **Sem transição de status.** Não há regra de mudança de status em `Pedido` nem em `Chamado`. `dataCancelamento`, `dataPagamento`, `dataFechamento` e `fechadoUser` não dependem do status (ex.: `FECHADO` sem `fechadoUser`). (P19)

**Defeitos pontuais encontrados ao levantar as regras**
- **Ordem das verificações em `EmpresaService.salvar()`:** corrigida em 2026-11-11. Agora confere admin, depois pessoa nula, depois duplicidade; a mensagem só traz o nome se a pessoa for da empresa logada.
- **Duplicidade de item comparada pelo nome.** `ItemPedido` compara o nome do produto, não o id (regra 79). Hoje isso funciona porque o nome é único por empresa (regra 62). Se essa regra mudar, a checagem de item duplicado deixa de valer.
- **Mensagem copiada de outro contexto:** `Pedido.dataPedido` tem a mensagem "Informe a forma de pagamento".

**Corrigidos em 2026-11-11** (cobertos pelos testes de controller)
- `EmpresaDTO.pessoaId` recebia o id do plano; o `planoId` era `String` e nunca era preenchido. Agora vêm `pessoaId`, `planoId` e `planoNome` corretos.
- `UsuarioService.alterarSenha()` e `atualizar()` chamavam `.get()` no `Optional` sem testar: usuário inexistente dava 500. Agora dão "Usuário não encontrado." e "Usuário não encontrado para a empresa logada.". A mensagem do `atualizar()` diz "atualizar usuários".
- `EmpresaService.buscaPorId()` dava 500 para id inexistente. Agora: "Empresa com id: X não foi encontrada.".
- Os `deletarTodos` e `deletarTodosPorIds` davam 500 ("read-only transaction"): o delete em lote do `JpaJdevRepositoryImpl` herdava a transação somente leitura do `SimpleJpaRepository`. Os services agora abrem transação de escrita.
- Enum no JSON: o Jackson 3 lia e escrevia pelo `toString()` (a descrição), então salvar com `"DINHEIRO"` dava 400. Agora é sempre o nome do enum (`config/ConfiguracaoJackson`).
- `empresa` com `@NotNull` em `Chamado`, `ItemPedido`, `Mensagem`, `MovimentacaoProduto` e `Pedido` (e `Chamado.dataAbertura`) obrigava o front a mandar um valor que o service sobrescreve. O `@NotNull` saiu; as colunas continuam `NOT NULL`.
- O buscarPorId de usuário não trazia `login` nem `pessoaId`, então o retorno não servia para editar.

## Pendências
As decisões e dúvidas deste documento estão em `docs/pendencias.md`. P14, P16 e P18 já foram decididas e esperam implementação; P17 e P19 esperam a confirmação das matrizes.
