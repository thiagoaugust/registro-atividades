-- Onde a atividade aconteceu, e o que isso vale.
--
-- Estudar no transporte publico e tempo resgatado de um deslocamento: nao tirou tempo de mais nada
-- e custa mais atencao para acontecer. O bonus de XP e a bonificacao dessa diferenca; a conquista e
-- o trofeu mensal sao o reconhecimento de longo prazo.

-- Contexto do GTD: "onde estou" agora inclui o caminho.
insert into contexto (nome, ordem) values ('@transporte publico', 50);

insert into conquista (codigo, titulo, descricao, tipo_regra, parametros, ordem) values
    ('TEMPO_APROVEITADO', 'Tempo que ninguem aproveita',
     'Cinco horas de estudo ou leitura no transporte publico',
     'TOTAL_METRICA', '{"metrica":"MINUTOS_APROVEITADOS","alvo":300}', 140),
    ('TEMPO_APROVEITADO_MESTRE', 'Vinte horas no caminho',
     'Vinte horas de estudo ou leitura no transporte publico',
     'TOTAL_METRICA', '{"metrica":"MINUTOS_APROVEITADOS","alvo":1200}', 150);

-- Trofeu mensal: repete todo mes, com o alvo calibrado pelos meses anteriores.
insert into desafio_periodico (escopo, titulo, descricao, metrica, parametros, tipo, alvo, ordem) values
    ('MENSAL', 'Trofeu do tempo aproveitado',
     'Minutos de estudo ou leitura no transporte publico',
     'MINUTOS_APROVEITADOS', '{"fator":1.1,"minimo":60}', 'META', null, 70);
