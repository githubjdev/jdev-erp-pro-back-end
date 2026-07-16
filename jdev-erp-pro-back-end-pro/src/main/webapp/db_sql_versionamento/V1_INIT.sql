--Desabilita restrições, trigger e contrains
SET session_replication_role = replica;

alter table plano drop constraint if exists plano_valor_mensal_check;

delete from plano;
delete from empresa;
delete from pessoa;

ALTER TABLE IF EXISTS public.plano
    ADD CONSTRAINT plano_valor_mensal_check 
    CHECK (
   		 (tipo_plano = 'FREE' and valor_mensal = 0)
   			 OR
    	 (tipo_plano <> 'FREE'
    		and valor_mensal >= 49::double precision 
    		AND valor_mensal <= 200::double precision)
    );
    
    
INSERT INTO public.plano(
	id, ativo, descricao, 
	limite_cliente, limite_usuario, nome,
	tipo_plano, valor_mensal)
	VALUES (1, true, 'Plano free para teste', 
	2, 4, 'Plano Free', 
	'FREE', 0);  

select nextval('seq_plano');	
	
INSERT INTO public.plano(
	id, ativo, descricao, 
	limite_cliente, limite_usuario, nome,
	tipo_plano, valor_mensal)
	VALUES (2, true, 'Plano Pro nível médio', 
	15, 30, 'Plano Pro', 
	'PRO', 50); 	
	
select nextval('seq_plano');	




INSERT INTO public.pessoa(
	id, ativo, bairro,
	cep, cidade, cnpj,
	complemento, cpf, data_cadastro,
	email, estado,
	logradouro, nome, nome_fantasia,
	observacao, pais, razao_social,
	telefone, tipo_pessoa, empresa_id, 
	inscricao_estadual)
	VALUES (
	     1, true, 'Jd Dias 1',
		'87025-758', 'Maringá', '26.934.453/0001-89',
		'perto do mercado Katayma', '059.486.784-85', '2026-07-15',
		'contato@jdevtreinamento.com.br', 'PR', 
		'Rua Pioneiro Antonio', 'Alex Fernando Egidio', 'Jdev Treinamento LTDA',
		'Nenhuma', 'Brasil', 'JDev Treinamento', 
		'44 9 8821-2355', 'JURIDICA', 1, 
		'878787-787');


select nextval('seq_pessoa');


	INSERT INTO public.empresa(
	id, bloqueio, logo_marca, 
	plano_ativo, total_cliente, 
	total_usuario,vigencia_plano, 
	pessoa_id, plano_id)
	VALUES (1, false, 'não existe', 
	        true, 10, 20, '2030-10-10', 1, 1);
			
select nextval('seq_empresa');		



--Habilita restrições, trigger e contrains
SET session_replication_role = origin;