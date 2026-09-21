-- Sono medido, nao so sentido.
--
-- O check-in ja tinha qualidade do sono (1 a 5, subjetivo) e horas (numero redondo). O relogio
-- entrega outra coisa: duracao ao minuto, fases, despertares, frequencia cardiaca de repouso e a
-- pontuacao dele. Guardar as duas naturezas e o que permite perguntar se o que voce sente de manha
-- bate com o que foi medido — e qual dos dois prediz melhor o dia.
--
-- A entrada e manual por enquanto; nada aqui presume integracao com o relogio.

alter table checkin_diario
    add column minutos_sono          int  check (minutos_sono between 0 and 1440),
    add column dormiu_em             time,
    add column acordou_em            time,
    add column minutos_sono_profundo int  check (minutos_sono_profundo >= 0),
    add column minutos_sono_rem      int  check (minutos_sono_rem >= 0),
    add column despertares           int  check (despertares >= 0),
    add column fc_repouso            int  check (fc_repouso between 20 and 220),
    add column pontuacao_sono        int  check (pontuacao_sono between 0 and 100);

-- O que existia em horas passa a viver em minutos: a coluna de horas volta como derivada na view,
-- para nao haver dois lugares dizendo quanto tempo voce dormiu.
update checkin_diario set minutos_sono = round(horas_sono * 60) where horas_sono is not null;

drop view vw_dia_analitico;
alter table checkin_diario drop column horas_sono;

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
       round(c.minutos_sono / 60.0, 1)                       as horas_sono,
       c.minutos_sono,
       c.minutos_sono_profundo,
       c.minutos_sono_rem,
       c.despertares,
       c.fc_repouso,
       c.pontuacao_sono,
       c.qualidade_sono,
       c.humor,
       c.estresse,
       -- a dificuldade do fechamento manda, quando existe: e o julgamento com o dia ja vivido
       coalesce(c.dificuldade_final, c.dificuldade_prevista) as dificuldade,
       extract(isodow from d.data_local)::int                as dia_semana
  from dia_resumo d
  left join checkin_diario c on c.data_local = d.data_local;
