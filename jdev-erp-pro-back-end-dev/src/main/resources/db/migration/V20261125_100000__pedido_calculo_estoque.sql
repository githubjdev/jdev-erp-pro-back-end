-- [Flyway] Pedido: cálculo automático, baixa de estoque e perfis de venda.
-- Idempotente: só cria o que ainda não existe (o ddl-auto=update do dev pode ter criado as colunas antes).

-- Preço unitário do item (congela o preço do produto no momento da venda)
ALTER TABLE public.item_pedido ADD COLUMN IF NOT EXISTS preco_unitario numeric(38,2) NOT NULL DEFAULT 0;

UPDATE public.item_pedido i
   SET preco_unitario = round(p.preco::numeric, 2)
  FROM public.produto p
 WHERE p.id = i.produto_id
   AND i.preco_unitario = 0;

-- Trava de idempotência: o estoque do pedido só é baixado (e estornado) uma vez
ALTER TABLE public.pedido ADD COLUMN IF NOT EXISTS estoque_baixado boolean NOT NULL DEFAULT false;

-- Estoque pode chegar a zero (venda da última unidade). O CHECK antigo exigia >= 1.
ALTER TABLE public.produto DROP CONSTRAINT IF EXISTS produto_estoque_check;
ALTER TABLE public.produto ADD CONSTRAINT produto_estoque_check CHECK ((estoque >= (0)::double precision));

-- Perfis que podem mudar o status do pedido (além do ROLE_ADMIN)
INSERT INTO public.role (id, acesso)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM public.role), 'ROLE_VENDEDOR'
 WHERE NOT EXISTS (SELECT 1 FROM public.role WHERE acesso = 'ROLE_VENDEDOR');

INSERT INTO public.role (id, acesso)
SELECT (SELECT COALESCE(MAX(id), 0) + 1 FROM public.role), 'ROLE_GERENTE'
 WHERE NOT EXISTS (SELECT 1 FROM public.role WHERE acesso = 'ROLE_GERENTE');

SELECT setval('public.seq_role', (SELECT GREATEST(COALESCE(MAX(id), 1), 1) FROM public.role));
