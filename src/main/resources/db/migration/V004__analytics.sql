-- Fase 4: analytics.
--
-- Uma view so, para o que toda consulta analitica precisa: o resumo do dia com o contexto do
-- check-in ao lado. O resto sao queries parametrizadas por periodo e granularidade — views nao
-- recebem parametro, e virariam ou uma por combinacao ou uma varredura maior que a necessaria.

create view vw_dia_analitico as
select d.data_local,
       d.xp_total,
       d.minutos_total,
       d.categorias_distintas,
       d.indice_produtividade,
       d.classificacao,
       d.dia_dificil_vencido,
       d.descanso,
       d.presenca,
       c.energia,
       c.horas_sono,
       c.qualidade_sono,
       c.humor,
       c.estresse,
       -- a dificuldade do fechamento manda, quando existe: e o julgamento com o dia ja vivido
       coalesce(c.dificuldade_final, c.dificuldade_prevista) as dificuldade,
       extract(isodow from d.data_local)::int                as dia_semana
  from dia_resumo d
  left join checkin_diario c on c.data_local = d.data_local;

-- Agregacao por tema de estudo e por modalidade de treino sai do JSONB; sem indice, cada consulta
-- de periodo faria varredura completa da tabela de registros.
create index idx_registro_detalhes on registro_atividade using gin (detalhes);
