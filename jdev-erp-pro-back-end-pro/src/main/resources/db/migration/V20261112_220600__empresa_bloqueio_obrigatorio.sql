-- Objetivo: empresa.bloqueio nunca nulo. Nulo vira false (desbloqueada), com DEFAULT false e NOT NULL (regra 44a).
-- Idempotente: o UPDATE só pega nulos, e SET DEFAULT / SET NOT NULL podem rodar de novo sem erro.
UPDATE public.empresa SET bloqueio = false WHERE bloqueio IS NULL;
ALTER TABLE IF EXISTS public.empresa ALTER COLUMN bloqueio SET DEFAULT false;
ALTER TABLE IF EXISTS public.empresa ALTER COLUMN bloqueio SET NOT NULL;
