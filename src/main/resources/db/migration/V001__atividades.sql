-- Fase 1: cadastros de apoio e o registro de atividades.
-- Enums ficam como varchar + check: evoluir um check e uma migration trivial, evoluir um type do
-- Postgres nao e. Hibernate mapeia com @Enumerated(STRING).

create table projeto (
    id                 bigserial primary key,
    titulo             varchar(200) not null,
    resultado_desejado text,
    status             varchar(20)  not null default 'ATIVO'
                       check (status in ('ATIVO', 'PAUSADO', 'CONCLUIDO', 'ARQUIVADO')),
    criado_em          timestamptz  not null default now(),
    concluido_em       timestamptz
);

create table desafio (
    id         bigserial primary key,
    titulo     varchar(200)   not null,
    meta_valor numeric(12, 2) not null check (meta_valor > 0),
    unidade    varchar(30)    not null,
    inicio     date           not null,
    fim        date,
    status     varchar(20)    not null default 'ATIVO'
               check (status in ('ATIVO', 'CONCLUIDO', 'ABANDONADO')),
    constraint desafio_periodo_coerente check (fim is null or fim >= inicio)
);

create table livro (
    id            bigserial primary key,
    titulo        varchar(300) not null,
    autor         varchar(200),
    total_paginas int check (total_paginas > 0),
    status        varchar(20)  not null default 'LENDO'
                  check (status in ('LENDO', 'CONCLUIDO', 'ABANDONADO')),
    concluido_em  date
);

create table registro_atividade (
    id          bigserial primary key,
    data_local  date        not null,
    inicio_em   timestamptz,
    duracao_min int         not null check (duracao_min between 1 and 1440),
    categoria   varchar(10) not null
                check (categoria in ('TREINO', 'ESTUDO', 'LEITURA', 'DESAFIO', 'PROJETO')),
    titulo      varchar(200),
    esforco     smallint    not null check (esforco between 1 and 10),
    satisfacao  smallint    check (satisfacao between 1 and 5),
    notas       text,
    projeto_id  bigint      references projeto (id),
    desafio_id  bigint      references desafio (id),
    livro_id    bigint      references livro (id),
    detalhes    jsonb       not null default '{}'::jsonb,
    criado_em   timestamptz not null default now(),
    atualizado_em timestamptz not null default now()
);

-- A leitura dominante e sempre por faixa de dia local (dia, semana, mes, heatmap anual).
create index idx_registro_data on registro_atividade (data_local);
create index idx_registro_categoria_data on registro_atividade (categoria, data_local);
create index idx_registro_projeto on registro_atividade (projeto_id) where projeto_id is not null;
create index idx_registro_desafio on registro_atividade (desafio_id) where desafio_id is not null;
create index idx_registro_livro on registro_atividade (livro_id) where livro_id is not null;
