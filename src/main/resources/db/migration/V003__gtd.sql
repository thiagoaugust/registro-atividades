-- Fase 3: o fluxo GTD (capturar, esclarecer, organizar, engajar).

create table contexto (
    id    bigserial primary key,
    nome  varchar(40) not null unique,
    ativo boolean     not null default true,
    ordem int         not null default 0
);

insert into contexto (nome, ordem) values
    ('@casa', 10), ('@computador', 20), ('@rua', 30), ('@telefone', 40);

-- "Proximas acoes", "Agenda", "Aguardando" e "Algum dia/talvez" sao a MESMA tabela vista por
-- estados diferentes: sao listas de uma coisa so, e quatro tabelas seriam quatro copias do CRUD.
create table acao (
    id                 bigserial primary key,
    titulo             varchar(300) not null,
    notas              text,
    estado             varchar(20)  not null
                       check (estado in ('PROXIMA', 'AGENDA', 'AGUARDANDO', 'ALGUM_DIA',
                                         'CONCLUIDA', 'DESCARTADA')),
    contexto_id        bigint       references contexto (id),
    tempo_estimado_min int          check (tempo_estimado_min > 0),
    energia            varchar(10)  check (energia in ('BAIXA', 'MEDIA', 'ALTA')),
    categoria          varchar(10)  check (categoria in ('TREINO', 'ESTUDO', 'LEITURA', 'DESAFIO', 'PROJETO')),
    projeto_id         bigint       references projeto (id),
    agendada_para      timestamptz,
    delegada_para      varchar(120),
    delegada_em        date,
    -- Preenchido quando a acao concluida virou um registro de atividade.
    registro_id        bigint       references registro_atividade (id) on delete set null,
    criada_em          timestamptz  not null default now(),
    concluida_em       timestamptz,
    constraint acao_agenda_tem_data check (estado <> 'AGENDA' or agendada_para is not null),
    constraint acao_aguardando_tem_dono check (estado <> 'AGUARDANDO' or delegada_para is not null)
);

create index idx_acao_estado on acao (estado);
create index idx_acao_contexto on acao (contexto_id) where estado = 'PROXIMA';
create index idx_acao_projeto on acao (projeto_id) where projeto_id is not null;

-- Captura: um campo de texto e Enter. Tudo o mais e decidido depois, no esclarecimento.
create table inbox_item (
    id            bigserial primary key,
    texto         text        not null,
    capturado_em  timestamptz not null default now(),
    processado_em timestamptz,
    -- A decisao tomada no esclarecimento. Fica guardada porque o estado da acao muda depois
    -- (ALGUM_DIA vira PROXIMA), e a decisao original e o que as metricas do GTD querem medir.
    destino       varchar(20) check (destino in ('LIXO', 'ALGUM_DIA', 'REFERENCIA', 'ACAO',
                                                 'PROJETO', 'FEITO_2MIN')),
    destino_id    bigint
);

create index idx_inbox_pendente on inbox_item (capturado_em) where processado_em is null;

create table referencia (
    id        bigserial primary key,
    titulo    varchar(300) not null,
    conteudo  text,
    url       text,
    tags      text[]       not null default '{}',
    criada_em timestamptz  not null default now()
);

-- Conquistas que so agora tem o que contar.
insert into conquista (codigo, titulo, descricao, tipo_regra, parametros, ordem) values
    ('PRIMEIRA_CAPTURA', 'Cabeca vazia', 'Capturar o primeiro item no inbox',
     'CONTAGEM_EVENTO', '{"evento":"ITEM_CAPTURADO","alvo":1}', 140),
    ('INBOX_ZERO', 'Inbox zero', 'Deixar o inbox sem nenhum item pendente',
     'CONTAGEM_EVENTO', '{"evento":"INBOX_ZERADO","alvo":1}', 150),
    ('CEM_ACOES', 'Cem acoes', 'Concluir cem proximas acoes',
     'CONTAGEM_EVENTO', '{"evento":"ACAO_CONCLUIDA","alvo":100}', 160);
