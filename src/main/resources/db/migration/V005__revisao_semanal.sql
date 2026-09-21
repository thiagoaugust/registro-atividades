-- Fase 5: a revisao semanal (o "refletir" do GTD).

create table revisao_semanal (
    -- uma por semana: a segunda-feira que abre a semana e a propria identidade
    semana_inicio date primary key,
    iniciada_em   timestamptz not null default now(),
    concluida_em  timestamptz,
    duracao_min   int         check (duracao_min >= 0),
    -- passos marcados do checklist, como {"ESVAZIAR_INBOX": true, ...}
    passos        jsonb       not null default '{}'::jsonb
);

create index idx_revisao_concluida on revisao_semanal (semana_inicio desc)
    where concluida_em is not null;

insert into conquista (codigo, titulo, descricao, tipo_regra, parametros, ordem) values
    ('PRIMEIRA_REVISAO', 'Primeira revisao', 'Concluir a primeira revisao semanal',
     'CONTAGEM_EVENTO', '{"evento":"REVISAO_CONCLUIDA","alvo":1}', 170),
    ('QUATRO_REVISOES', 'Habito de revisar', 'Quatro revisoes semanais seguidas',
     'CONTAGEM_EVENTO', '{"evento":"REVISOES_SEGUIDAS","alvo":4}', 180);
