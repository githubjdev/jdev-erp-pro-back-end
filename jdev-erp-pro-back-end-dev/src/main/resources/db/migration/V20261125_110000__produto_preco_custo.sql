-- [Flyway] Produto: preço de custo (usado como preço do item em pedido de COMPRA).
-- Idempotente: só cria o que ainda não existe (o ddl-auto=update do dev pode ter criado a coluna antes).
ALTER TABLE public.produto ADD COLUMN IF NOT EXISTS preco_custo double precision NOT NULL DEFAULT 0;

ALTER TABLE public.produto DROP CONSTRAINT IF EXISTS produto_preco_custo_check;
ALTER TABLE public.produto ADD CONSTRAINT produto_preco_custo_check CHECK ((preco_custo >= (0)::double precision));
