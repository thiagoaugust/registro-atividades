-- Cursos, e a categoria de livro promovida a area de conhecimento.
--
-- A mesma lista (Tecnico, Psicologia, Historia...) passa a classificar livro e curso, para que
-- "quanto investi em Psicologia" seja uma soma, e nao dois numeros em telas diferentes.

alter table categoria_livro rename to area_conhecimento;
alter table livro rename column categoria_id to area_id;

create table curso (
    id            bigserial primary key,
    titulo        varchar(300)  not null,
    instituicao   varchar(200),
    url           varchar(1000),
    -- Carga horaria prevista; o progresso e o tempo dedicado dividido por ela.
    carga_horaria numeric(6, 1) check (carga_horaria > 0),
    area_id       bigint        references area_conhecimento (id),
    status        varchar(20)   not null default 'CURSANDO'
                  check (status in ('CURSANDO', 'CONCLUIDO', 'ABANDONADO')),
    concluido_em  date,
    -- Curso feito antes de o sistema existir, mesmo criterio dos livros retroativos:
    -- conta na estante e nas areas, mas nao gera registro nem XP.
    horas_retroativas numeric(6, 1) check (horas_retroativas > 0),
    dias_retroativos  int           check (dias_retroativos > 0),
    criado_em     timestamptz   not null default now()
);

create index idx_curso_area on curso (area_id) where area_id is not null;

alter table registro_atividade
    add column curso_id bigint references curso (id);

create index idx_registro_curso on registro_atividade (curso_id) where curso_id is not null;

insert into conquista (codigo, titulo, descricao, tipo_regra, parametros, ordem) values
    ('PRIMEIRO_CURSO', 'Primeiro curso', 'Concluir o primeiro curso',
     'CONTAGEM_EVENTO', '{"evento":"CURSO_CONCLUIDO","alvo":1}', 210),
    ('CEM_HORAS_PRATICA', 'Pratica deliberada', 'Cem horas de pratica deliberada',
     'TOTAL_METRICA', '{"metrica":"MINUTOS_PRATICA","alvo":6000}', 220);
