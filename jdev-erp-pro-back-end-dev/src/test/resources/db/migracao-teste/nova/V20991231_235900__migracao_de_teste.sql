-- Só para testes (src/test/resources, não vai para o JAR): simula uma migração nova depois das existentes.
CREATE TABLE public.flyway_teste_nova_migracao (
    id bigint NOT NULL,
    descricao character varying(100) NOT NULL,
    CONSTRAINT flyway_teste_nova_migracao_pkey PRIMARY KEY (id)
);

INSERT INTO public.flyway_teste_nova_migracao (id, descricao) VALUES (1, 'aplicada pelo Flyway');
