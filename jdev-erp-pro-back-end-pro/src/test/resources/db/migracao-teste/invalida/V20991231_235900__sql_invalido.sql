-- Só para testes (src/test/resources, não vai para o JAR): migração com erro.
-- A primeira instrução é válida; a segunda falha. O Flyway tem de desfazer as duas (transação) e impedir a subida.
CREATE TABLE public.flyway_teste_migracao_invalida (id bigint NOT NULL);

INSERT INTO public.tabela_que_nao_existe (id) VALUES (1);
