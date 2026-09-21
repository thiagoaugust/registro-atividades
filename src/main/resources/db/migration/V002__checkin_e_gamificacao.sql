-- Fase 2: check-in diario, ledger de XP, resumo materializado do dia e conquistas.

-- Um check-in por dia. A abertura e o fechamento moram na mesma linha: sao o mesmo dia, e duas
-- tabelas so dariam um join a mais em toda leitura.
-- Todos os itens sao opcionais; a adversidade renormaliza os pesos dos que foram preenchidos.
create table checkin_diario (
    data_local           date primary key,
    energia              smallint      check (energia between 1 and 5),
    horas_sono           numeric(3, 1) check (horas_sono between 0 and 24),
    qualidade_sono       smallint      check (qualidade_sono between 1 and 5),
    humor                smallint      check (humor between 1 and 5),
    estresse             smallint      check (estresse between 1 and 5),
    dificuldade_prevista smallint      check (dificuldade_prevista between 1 and 5),
    descanso_planejado   boolean     not null default false,
    frase                text,
    -- fechamento do dia (opcional)
    dificuldade_final    smallint      check (dificuldade_final between 1 and 5),
    atrapalhou           text,
    fechado_em           timestamptz,
    criado_em            timestamptz not null default now(),
    atualizado_em        timestamptz not null default now()
);

-- Ledger: toda origem de XP escreve aqui em pontos BRUTOS. Tetos, bonus e totais sao derivados,
-- nunca guardados na origem. Editar um registro e reescrever a linha dele e recalcular o dia.
create table xp_lancamento (
    id            bigserial primary key,
    data_local    date        not null,
    origem        varchar(20) not null
                  check (origem in ('REGISTRO', 'ACAO_GTD', 'REVISAO_SEMANAL')),
    origem_id     bigint      not null,
    categoria     varchar(10) check (categoria in ('TREINO', 'ESTUDO', 'LEITURA', 'DESAFIO', 'PROJETO')),
    pontos_brutos int         not null check (pontos_brutos >= 0),
    criado_em     timestamptz not null default now()
);

create index idx_xp_data on xp_lancamento (data_local);
-- Uma origem rende um lancamento so: o upsert vira delete + insert sem risco de duplicar.
create unique index uq_xp_origem on xp_lancamento (origem, origem_id);

-- Resumo materializado. Sem ele, o heatmap anual seriam 365 recalculos com janela movel de 28 dias.
create table dia_resumo (
    data_local            date primary key,
    xp_total              int           not null default 0,
    minutos_total         int           not null default 0,
    categorias_distintas  smallint      not null default 0,
    indice_produtividade  numeric(5, 2) not null default 0,
    classificacao         varchar(10)   not null
                          check (classificacao in ('DIFICIL', 'NORMAL', 'BOM', 'EXCELENTE')),
    dia_dificil_vencido   boolean       not null default false,
    descanso              boolean       not null default false,
    presenca              boolean       not null default false,
    baseline_insuficiente boolean       not null default false,
    calculado_em          timestamptz   not null default now()
);

create index idx_dia_resumo_classificacao on dia_resumo (classificacao);

create table dia_categoria_resumo (
    data_local date        not null references dia_resumo (data_local) on delete cascade,
    categoria  varchar(10) not null
               check (categoria in ('TREINO', 'ESTUDO', 'LEITURA', 'DESAFIO', 'PROJETO')),
    xp         int         not null default 0,
    minutos    int         not null default 0,
    primary key (data_local, categoria)
);

-- Conquistas sao dados: uma conquista nova e um INSERT enquanto usar uma metrica ja conhecida.
create table conquista (
    codigo     varchar(40) primary key,
    titulo     varchar(120) not null,
    descricao  text,
    tipo_regra varchar(20)  not null check (tipo_regra in ('TOTAL_METRICA', 'STREAK', 'CONTAGEM_EVENTO')),
    parametros jsonb        not null default '{}'::jsonb,
    ativa      boolean      not null default true,
    ordem      int          not null default 0
);

create table conquista_desbloqueada (
    conquista_codigo varchar(40) primary key references conquista (codigo) on delete cascade,
    data_local       date        not null,
    desbloqueada_em  timestamptz not null default now()
);

-- Conjunto inicial. As conquistas que dependem do GTD (inbox zero, revisoes semanais seguidas)
-- entram nas migrations das fases 3 e 5, quando existir o que contar.
insert into conquista (codigo, titulo, descricao, tipo_regra, parametros, ordem) values
    ('PRIMEIRO_REGISTRO', 'Primeiro passo', 'Registrar a primeira atividade',
     'CONTAGEM_EVENTO', '{"evento":"REGISTRO","alvo":1}', 10),
    ('SEMANA_COMPLETA', 'Semana completa', 'Sete dias seguidos de presenca',
     'STREAK', '{"escopo":"GERAL","alvo":7}', 20),
    ('MES_COMPLETO', 'Mes completo', 'Trinta dias seguidos de presenca',
     'STREAK', '{"escopo":"GERAL","alvo":30}', 30),
    ('STREAK_TREINO_10', 'Constancia no treino', 'Dez dias seguidos com treino',
     'STREAK', '{"escopo":"TREINO","alvo":10}', 40),
    ('CEM_KM', '100 km', 'Cem quilometros acumulados',
     'TOTAL_METRICA', '{"metrica":"KM","alvo":100}', 50),
    ('QUINHENTOS_KM', '500 km', 'Quinhentos quilometros acumulados',
     'TOTAL_METRICA', '{"metrica":"KM","alvo":500}', 60),
    ('MIL_PAGINAS', 'Mil paginas', 'Mil paginas lidas',
     'TOTAL_METRICA', '{"metrica":"PAGINAS","alvo":1000}', 70),
    ('DEZ_LIVROS', 'Dez livros', 'Dez livros concluidos',
     'CONTAGEM_EVENTO', '{"evento":"LIVRO_CONCLUIDO","alvo":10}', 80),
    ('CEM_HORAS_ESTUDO', '100 horas de estudo', 'Cem horas acumuladas em ESTUDO',
     'TOTAL_METRICA', '{"metrica":"MINUTOS_ESTUDO","alvo":6000}', 90),
    ('DIA_EQUILIBRADO', 'Equilibrio', 'Dez dias com tres ou mais categorias',
     'CONTAGEM_EVENTO', '{"evento":"DIA_EQUILIBRADO","alvo":10}', 100),
    ('GUERREIRO', 'Guerreiro', 'Dez dias dificeis vencidos',
     'CONTAGEM_EVENTO', '{"evento":"DIA_DIFICIL_VENCIDO","alvo":10}', 110),
    ('DESAFIO_CONCLUIDO', 'Desafio batido', 'Concluir o primeiro desafio',
     'CONTAGEM_EVENTO', '{"evento":"DESAFIO_CONCLUIDO","alvo":1}', 120),
    ('PROJETO_ENTREGUE', 'Entregue', 'Concluir o primeiro projeto',
     'CONTAGEM_EVENTO', '{"evento":"PROJETO_CONCLUIDO","alvo":1}', 130);
