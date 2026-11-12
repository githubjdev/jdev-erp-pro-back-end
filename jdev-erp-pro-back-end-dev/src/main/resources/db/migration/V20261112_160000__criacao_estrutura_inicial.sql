-- Objetivo: criar a estrutura do banco (sequences, tabelas, PK, unique, check e FK) num banco vazio.
--
-- Por que existe: antes do Flyway, quem criava as tabelas era o Hibernate (ddl-auto=update), e os scripts
-- antigos (V1_INIT e V2_PESSOA_DOCUMENTO_POR_TIPO) só rodavam depois, à mão. O Spring Boot executa o Flyway
-- ANTES do Hibernate, então sem esta migração a carga inicial falharia num banco novo (tabela inexistente).
--
-- Origem: pg_dump --schema-only do banco jdev-erp-pro-dev em 2026-11-12, com os nomes de constraint gerados
-- pelo Hibernate a partir das entidades. Diferenças em relação a esse dump, de propósito, para que as
-- migrações seguintes rodem sem alteração:
--   * usuario.bloqueado no lugar de usuario.liberado: a carga inicial (V20261112_161000) insere em
--     "bloqueado", apaga a coluna e cria "liberado";
--   * sem plano_valor_mensal_check e sem a extensão unaccent: criados pela carga inicial;
--   * sem pessoa_cpf_pessoa_fisica_check, pessoa_cnpj_pessoa_juridica_check e pessoa_email_preenchido_check:
--     criadas pela V20261112_162000.
-- Depois das três migrações, a estrutura fica igual à do banco dev (conferido pelos testes do Flyway).
--
-- Banco existente (anterior ao Flyway) não executa esta migração: recebe baseline (ver docs/Flyway.md).

-- ==================== Sequences (allocationSize = 1 nas entidades) ====================
CREATE SEQUENCE public.seq_categoria START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_chamado START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_cliente_funcionario START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_empresa START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_item_pedido START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_mensagem START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_movimentacao_produto START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_pedido START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_pessoa START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_plano START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_produto START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_role START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_role_usuario START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE public.seq_usuario START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- ==================== Tabelas globais (sem empresa_id) ====================
CREATE TABLE public.plano (
    id bigint NOT NULL,
    ativo boolean,
    descricao character varying(255) NOT NULL,
    limite_cliente integer NOT NULL,
    limite_usuario integer NOT NULL,
    nome character varying(255) NOT NULL,
    tipo_plano character varying(255) NOT NULL,
    valor_mensal double precision NOT NULL,
    CONSTRAINT plano_pkey PRIMARY KEY (id),
    CONSTRAINT plano_limite_cliente_check CHECK (((limite_cliente <= 150) AND (limite_cliente >= 1))),
    CONSTRAINT plano_limite_usuario_check CHECK (((limite_usuario <= 150) AND (limite_usuario >= 1))),
    CONSTRAINT plano_tipo_plano_check CHECK (tipo_plano IN ('FREE', 'PRO', 'ENTERPRISE'))
);

CREATE TABLE public.role (
    id bigint NOT NULL,
    acesso character varying(255) NOT NULL,
    CONSTRAINT role_pkey PRIMARY KEY (id),
    CONSTRAINT unique_acesso UNIQUE (acesso)
);

CREATE TABLE public.pessoa (
    id bigint NOT NULL,
    ativo boolean NOT NULL,
    bairro character varying(255) NOT NULL,
    cep character varying(255) NOT NULL,
    cidade character varying(255) NOT NULL,
    cnpj character varying(255),
    complemento character varying(255),
    cpf character varying(255),
    data_cadastro date NOT NULL,
    email character varying(255) NOT NULL,
    estado character varying(255) NOT NULL,
    inscricao_estadual character varying(255),
    logradouro character varying(255) NOT NULL,
    nome character varying(255) NOT NULL,
    nome_fantasia character varying(255),
    observacao character varying(1000),
    pais character varying(255) NOT NULL,
    razao_social character varying(255),
    telefone character varying(255) NOT NULL,
    tipo_pessoa character varying(255) NOT NULL,
    empresa_id bigint NOT NULL,
    CONSTRAINT pessoa_pkey PRIMARY KEY (id),
    CONSTRAINT unique_cnpj UNIQUE (cnpj),
    CONSTRAINT unique_cpf UNIQUE (cpf),
    CONSTRAINT unique_email UNIQUE (email),
    CONSTRAINT unique_incricao_estadual UNIQUE (inscricao_estadual),
    CONSTRAINT pessoa_tipo_pessoa_check CHECK (tipo_pessoa IN ('JURIDICA', 'FISICA'))
);

CREATE TABLE public.empresa (
    id bigint NOT NULL,
    bloqueio boolean,
    logo_marca text NOT NULL,
    plano_ativo boolean,
    total_cliente integer,
    total_usuario integer,
    vigencia_plano date,
    pessoa_id bigint NOT NULL,
    plano_id bigint NOT NULL,
    CONSTRAINT empresa_pkey PRIMARY KEY (id),
    CONSTRAINT unique_pessoa_empresa UNIQUE (pessoa_id)
);

-- ==================== Pessoas, usuários e acessos ====================
CREATE TABLE public.cliente_funcionario (
    id bigint NOT NULL,
    tipo_cliente_funcionario character varying(255) NOT NULL,
    empresa_id bigint NOT NULL,
    pessoa_id bigint NOT NULL,
    usuario_id bigint,
    CONSTRAINT cliente_funcionario_pkey PRIMARY KEY (id),
    CONSTRAINT unique_pessoa UNIQUE (pessoa_id),
    CONSTRAINT unique_pessoa_usuario UNIQUE (usuario_id, pessoa_id),
    CONSTRAINT unique_usuario UNIQUE (usuario_id),
    CONSTRAINT cliente_funcionario_tipo_cliente_funcionario_check CHECK (tipo_cliente_funcionario IN ('CLIENTE', 'FUNCIONARIO', 'PRESTADOR_SERVICO'))
);

CREATE TABLE public.usuario (
    id bigint NOT NULL,
    bloqueado boolean,
    login character varying(255) NOT NULL,
    refresh_token text,
    senha character varying(255) NOT NULL,
    token_sessao text,
    cliente_funcionario_id bigint NOT NULL,
    empresa_id bigint NOT NULL,
    CONSTRAINT usuario_pkey PRIMARY KEY (id),
    CONSTRAINT unique_cliente_funcionario UNIQUE (cliente_funcionario_id),
    CONSTRAINT unique_login UNIQUE (login),
    CONSTRAINT unique_senha UNIQUE (senha)
);

CREATE TABLE public.role_usuario (
    id bigint NOT NULL,
    acesso_id bigint NOT NULL,
    usuario_id bigint NOT NULL,
    CONSTRAINT role_usuario_pkey PRIMARY KEY (id),
    CONSTRAINT unique_role_user UNIQUE (acesso_id, usuario_id)
);

-- ==================== Produtos e estoque ====================
CREATE TABLE public.categoria (
    id bigint NOT NULL,
    nome character varying(255) NOT NULL,
    empresa_id bigint NOT NULL,
    CONSTRAINT categoria_pkey PRIMARY KEY (id)
);

CREATE TABLE public.produto (
    id bigint NOT NULL,
    codigo_barra character varying(255),
    descricao character varying(255),
    estoque double precision NOT NULL,
    estoque_minimo double precision NOT NULL,
    imagem text NOT NULL,
    nome character varying(255) NOT NULL,
    preco double precision NOT NULL,
    sku character varying(255) NOT NULL,
    unidade_medida character varying(255) NOT NULL,
    categoria_id bigint NOT NULL,
    empresa_id bigint NOT NULL,
    CONSTRAINT produto_pkey PRIMARY KEY (id),
    CONSTRAINT unique_nome_produto_empresa UNIQUE (nome, empresa_id),
    CONSTRAINT unique_sku_empresa UNIQUE (sku, empresa_id),
    CONSTRAINT produto_estoque_check CHECK ((estoque >= (1)::double precision)),
    CONSTRAINT produto_estoque_minimo_check CHECK ((estoque_minimo >= (1)::double precision)),
    CONSTRAINT produto_preco_check CHECK ((preco >= (1)::double precision)),
    CONSTRAINT produto_unidade_medida_check CHECK (unidade_medida IN ('CENTIMETRO', 'METRO', 'UNIDADE', 'QUILO'))
);

-- ==================== Pedidos ====================
CREATE TABLE public.pedido (
    id bigint NOT NULL,
    data_cancelamento date,
    data_pagamento date,
    data_pedido date NOT NULL,
    desconto numeric(38,2),
    forma_pagamento character varying(255) NOT NULL,
    frete numeric(38,2),
    numero_pedido character varying(255) NOT NULL,
    observacao character varying(255),
    status_pedido character varying(255) NOT NULL,
    sub_total numeric(38,2),
    taxas numeric(38,2),
    tipo_pedido character varying(255) NOT NULL,
    total numeric(38,2),
    cliente_id bigint NOT NULL,
    empresa_id bigint NOT NULL,
    vendedor_id bigint NOT NULL,
    CONSTRAINT pedido_pkey PRIMARY KEY (id),
    CONSTRAINT pedido_forma_pagamento_check CHECK (forma_pagamento IN ('DINHEIRO', 'PIX', 'CARTAO_CREDITO', 'CARTAO_DEBITO', 'BOLETO', 'TRANSFERENCIA_BANCARIA', 'DEPOSITO_BANCARIO', 'CHEQUE', 'CREDIARIO', 'VALE_ALIMENTACAO', 'VALE_REFEICAO', 'VALE_PRESENTE', 'CARTEIRA_DIGITAL', 'FATURADO', 'CREDITO_LOJA', 'OUTROS')),
    CONSTRAINT pedido_status_pedido_check CHECK (status_pedido IN ('AGUARDANDO_PAGAMENTO', 'PAGAMENTO_APROVADO', 'PAGAMENTO_RECUSADO', 'EM_SEPARACAO', 'SEPARADO', 'FATURADO', 'AGUARDANDO_ENVIO', 'ENVIADO', 'EM_TRANSPORTE', 'ENTREGUE', 'CANCELADO', 'DEVOLVIDO', 'REEMBOLSADO')),
    CONSTRAINT pedido_tipo_pedido_check CHECK (tipo_pedido IN ('VENDA', 'COMPRA', 'ORCAMENTO', 'PEDIDO_PRODUCAO', 'PEDIDO_TRANSFERENCIA', 'DEVOLUCAO_CLIENTE', 'DEVOLUCAO_FORNECEDOR', 'BONIFICACAO', 'BRINDE', 'CONSIGNACAO', 'REMESSA', 'RETORNO'))
);

CREATE TABLE public.item_pedido (
    id bigint NOT NULL,
    desconto numeric(38,2),
    quantidade double precision NOT NULL,
    sub_total numeric(38,2),
    total numeric(38,2),
    empresa_id bigint NOT NULL,
    pedido_id bigint NOT NULL,
    produto_id bigint NOT NULL,
    CONSTRAINT item_pedido_pkey PRIMARY KEY (id)
);

CREATE TABLE public.movimentacao_produto (
    id bigint NOT NULL,
    data_movimento date NOT NULL,
    quantidade double precision NOT NULL,
    tipo_movimentacao_produto character varying(255) NOT NULL,
    valor numeric(38,2),
    empresa_id bigint NOT NULL,
    pedido_id bigint,
    produto_id bigint NOT NULL,
    CONSTRAINT movimentacao_produto_pkey PRIMARY KEY (id),
    CONSTRAINT movimentacao_produto_tipo_movimentacao_produto_check CHECK (tipo_movimentacao_produto IN ('ENTRADA_COMPRA', 'ENTRADA_DEVOLUCAO_CLIENTE', 'ENTRADA_TRANSFERENCIA', 'ENTRADA_PRODUCAO', 'ENTRADA_AJUSTE', 'ENTRADA_BONIFICACAO', 'ENTRADA_INVENTARIO', 'SAIDA_VENDA', 'SAIDA_DEVOLUCAO_FORNECEDOR', 'SAIDA_TRANSFERENCIA', 'SAIDA_CONSUMO_INTERNO', 'SAIDA_PERDA', 'SAIDA_AVARIA', 'SAIDA_FURTO', 'SAIDA_BONIFICACAO', 'SAIDA_BRINDE', 'SAIDA_AJUSTE', 'SAIDA_INVENTARIO', 'RESERVA_ESTOQUE', 'LIBERACAO_RESERVA'))
);

-- ==================== Suporte (chamados) ====================
CREATE TABLE public.chamado (
    id bigint NOT NULL,
    data_abertura date NOT NULL,
    data_fechamento date,
    descricao character varying(255) NOT NULL,
    prioridade_chamado character varying(255) NOT NULL,
    status_chamado character varying(255) NOT NULL,
    tipo_chamado character varying(255) NOT NULL,
    titulo character varying(255) NOT NULL,
    aberto_user_id bigint NOT NULL,
    atendente_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    empresa_id bigint NOT NULL,
    fechado_user_id bigint,
    CONSTRAINT chamado_pkey PRIMARY KEY (id),
    CONSTRAINT chamado_prioridade_chamado_check CHECK (prioridade_chamado IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT chamado_status_chamado_check CHECK (status_chamado IN ('ABERTO', 'EM_ANALISE', 'AGUARDANDO_CLIENTE', 'EM_ATENDIMENTO', 'EM_DESENVOLVIMENTO', 'EM_TESTES', 'AGUARDANDO_HOMOLOGACAO', 'RESOLVIDO', 'FECHADO', 'CANCELADO')),
    CONSTRAINT chamado_tipo_chamado_check CHECK (tipo_chamado IN ('DUVIDA', 'SUPORTE', 'INCIDENTE', 'BUG', 'MELHORIA', 'NOVA_FUNCIONALIDADE', 'INTEGRACAO', 'CADASTRO', 'PERMISSAO_ACESSO', 'INFRAESTRUTURA', 'FINANCEIRO', 'TREINAMENTO', 'CONSULTORIA'))
);

CREATE TABLE public.mensagem (
    id bigint NOT NULL,
    arquivo text NOT NULL,
    conteudo text NOT NULL,
    data_envio date NOT NULL,
    lida boolean,
    atendente_id bigint NOT NULL,
    chamado_id bigint NOT NULL,
    cliente_id bigint NOT NULL,
    empresa_id bigint NOT NULL,
    CONSTRAINT mensagem_pkey PRIMARY KEY (id)
);

-- ==================== Chaves estrangeiras (no fim por causa das referências circulares) ====================
ALTER TABLE public.empresa ADD CONSTRAINT pessoa_fk FOREIGN KEY (pessoa_id) REFERENCES public.pessoa(id);
ALTER TABLE public.empresa ADD CONSTRAINT plano_fk FOREIGN KEY (plano_id) REFERENCES public.plano(id);

ALTER TABLE public.pessoa ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);

ALTER TABLE public.cliente_funcionario ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);
ALTER TABLE public.cliente_funcionario ADD CONSTRAINT pessoa_fk FOREIGN KEY (pessoa_id) REFERENCES public.pessoa(id);
ALTER TABLE public.cliente_funcionario ADD CONSTRAINT usuario_fk FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);

ALTER TABLE public.usuario ADD CONSTRAINT cliente_funcionario_fk FOREIGN KEY (cliente_funcionario_id) REFERENCES public.cliente_funcionario(id);
ALTER TABLE public.usuario ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);

ALTER TABLE public.role_usuario ADD CONSTRAINT acesso_fk FOREIGN KEY (acesso_id) REFERENCES public.role(id);
ALTER TABLE public.role_usuario ADD CONSTRAINT usuario_fk FOREIGN KEY (usuario_id) REFERENCES public.usuario(id);

ALTER TABLE public.categoria ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);

ALTER TABLE public.produto ADD CONSTRAINT categoria_fk FOREIGN KEY (categoria_id) REFERENCES public.categoria(id);
ALTER TABLE public.produto ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);

ALTER TABLE public.pedido ADD CONSTRAINT cliente_fk FOREIGN KEY (cliente_id) REFERENCES public.usuario(id);
ALTER TABLE public.pedido ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);
ALTER TABLE public.pedido ADD CONSTRAINT vendedor_fk FOREIGN KEY (vendedor_id) REFERENCES public.usuario(id);

ALTER TABLE public.item_pedido ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);
ALTER TABLE public.item_pedido ADD CONSTRAINT pedido_fk FOREIGN KEY (pedido_id) REFERENCES public.pedido(id);
ALTER TABLE public.item_pedido ADD CONSTRAINT produto_fk FOREIGN KEY (produto_id) REFERENCES public.produto(id);

ALTER TABLE public.movimentacao_produto ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);
ALTER TABLE public.movimentacao_produto ADD CONSTRAINT pedido_fk FOREIGN KEY (pedido_id) REFERENCES public.pedido(id);
ALTER TABLE public.movimentacao_produto ADD CONSTRAINT produto_fk FOREIGN KEY (produto_id) REFERENCES public.produto(id);

ALTER TABLE public.chamado ADD CONSTRAINT aberto_user_fk FOREIGN KEY (aberto_user_id) REFERENCES public.usuario(id);
ALTER TABLE public.chamado ADD CONSTRAINT atendente_fk FOREIGN KEY (atendente_id) REFERENCES public.usuario(id);
ALTER TABLE public.chamado ADD CONSTRAINT cliente_fk FOREIGN KEY (cliente_id) REFERENCES public.usuario(id);
ALTER TABLE public.chamado ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);
ALTER TABLE public.chamado ADD CONSTRAINT fechado_user_fk FOREIGN KEY (fechado_user_id) REFERENCES public.usuario(id);

ALTER TABLE public.mensagem ADD CONSTRAINT atendente_fk FOREIGN KEY (atendente_id) REFERENCES public.usuario(id);
ALTER TABLE public.mensagem ADD CONSTRAINT chamado_fk FOREIGN KEY (chamado_id) REFERENCES public.chamado(id);
ALTER TABLE public.mensagem ADD CONSTRAINT cliente_fk FOREIGN KEY (cliente_id) REFERENCES public.usuario(id);
ALTER TABLE public.mensagem ADD CONSTRAINT empresa_fk FOREIGN KEY (empresa_id) REFERENCES public.empresa(id);
