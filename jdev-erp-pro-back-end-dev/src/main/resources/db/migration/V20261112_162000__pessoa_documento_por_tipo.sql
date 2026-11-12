-- [Flyway] Migrado em 2026-11-12 de src/main/webapp/db_sql_versionamento/V2_PESSOA_DOCUMENTO_POR_TIPO.sql.
-- [Flyway] Adaptações (só o necessário; nenhum comando SQL foi removido além destes):
-- [Flyway]   * saíram o BEGIN; e o COMMIT; do arquivo: o Flyway já executa cada migração numa transação
-- [Flyway]     e grava o histórico nela. Um COMMIT dentro do script encerraria essa transação no meio.
-- [Flyway]   * a linha "Aplicar com: psql ..." abaixo vale só para o arquivo antigo. Agora quem aplica é o
-- [Flyway]     Flyway, na subida da aplicação.
-- [Flyway] Banco que já tinha o V2 aplicado à mão recebe baseline nesta versão e não a executa de novo.

-- Objetivo: e-mail obrigatório para toda pessoa, CPF obrigatório só para pessoa física e CNPJ só para pessoa jurídica (P13).
--
-- Antes: cpf, cnpj e email eram NOT NULL para os dois tipos de pessoa.
-- Depois:
--   * cpf e cnpj aceitam NULL; as uniques unique_cpf e unique_cnpj continuam e aceitam vários NULL;
--   * pessoa_cpf_pessoa_fisica_check: pessoa FISICA precisa de CPF preenchido;
--   * pessoa_cnpj_pessoa_juridica_check: pessoa JURIDICA precisa de CNPJ preenchido;
--   * pessoa_email_preenchido_check: e-mail não pode ficar em branco (o NOT NULL do email continua).
--
-- Não apaga nem altera nenhum registro. Se algum cadastro já estiver fora da regra, o script para
-- antes de mudar qualquer coisa e informa quantos são.
-- Idempotente: pode rodar de novo sem erro.
-- Aplicar com: psql -v ON_ERROR_STOP=1 -h <host> -p <porta> -U <usuario> -d <banco> -f V2_PESSOA_DOCUMENTO_POR_TIPO.sql

-- [Flyway] BEGIN; removido (ver o cabeçalho).

DO $$
DECLARE
    fisicas_sem_cpf    integer;
    juridicas_sem_cnpj integer;
    sem_email          integer;
BEGIN
    SELECT count(*) INTO fisicas_sem_cpf
      FROM public.pessoa
     WHERE tipo_pessoa = 'FISICA' AND (cpf IS NULL OR btrim(cpf) = '');

    SELECT count(*) INTO juridicas_sem_cnpj
      FROM public.pessoa
     WHERE tipo_pessoa = 'JURIDICA' AND (cnpj IS NULL OR btrim(cnpj) = '');

    SELECT count(*) INTO sem_email
      FROM public.pessoa
     WHERE email IS NULL OR btrim(email) = '';

    IF fisicas_sem_cpf + juridicas_sem_cnpj + sem_email > 0 THEN
        RAISE EXCEPTION 'V2 não aplicado: % pessoa(s) física(s) sem CPF, % pessoa(s) jurídica(s) sem CNPJ e % pessoa(s) sem e-mail. Corrija esses cadastros e rode o script de novo.',
            fisicas_sem_cpf, juridicas_sem_cnpj, sem_email;
    END IF;
END $$;

ALTER TABLE public.pessoa ALTER COLUMN cpf DROP NOT NULL;
ALTER TABLE public.pessoa ALTER COLUMN cnpj DROP NOT NULL;
ALTER TABLE public.pessoa ALTER COLUMN email SET NOT NULL;

DO $$
BEGIN
    /* O "IS NOT NULL" é obrigatório: uma CHECK que resulta em NULL é aceita pelo PostgreSQL */
    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conrelid = 'public.pessoa'::regclass AND conname = 'pessoa_cpf_pessoa_fisica_check') THEN
        ALTER TABLE public.pessoa ADD CONSTRAINT pessoa_cpf_pessoa_fisica_check
            CHECK (tipo_pessoa <> 'FISICA' OR (cpf IS NOT NULL AND btrim(cpf) <> ''));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conrelid = 'public.pessoa'::regclass AND conname = 'pessoa_cnpj_pessoa_juridica_check') THEN
        ALTER TABLE public.pessoa ADD CONSTRAINT pessoa_cnpj_pessoa_juridica_check
            CHECK (tipo_pessoa <> 'JURIDICA' OR (cnpj IS NOT NULL AND btrim(cnpj) <> ''));
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint
                    WHERE conrelid = 'public.pessoa'::regclass AND conname = 'pessoa_email_preenchido_check') THEN
        ALTER TABLE public.pessoa ADD CONSTRAINT pessoa_email_preenchido_check
            CHECK (btrim(email) <> '');
    END IF;
END $$;

-- [Flyway] COMMIT; removido (ver o cabeçalho).
