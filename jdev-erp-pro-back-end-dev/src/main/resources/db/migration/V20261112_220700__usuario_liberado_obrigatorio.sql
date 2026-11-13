-- Objetivo: usuario.liberado nunca nulo. Nulo vira false (bloqueado: liberar é decisão do admin), com DEFAULT false e NOT NULL (regra 17a).
-- Idempotente: o UPDATE só pega nulos, e SET DEFAULT / SET NOT NULL podem rodar de novo sem erro.
UPDATE public.usuario SET liberado = false WHERE liberado IS NULL;
ALTER TABLE IF EXISTS public.usuario ALTER COLUMN liberado SET DEFAULT false;
ALTER TABLE IF EXISTS public.usuario ALTER COLUMN liberado SET NOT NULL;
