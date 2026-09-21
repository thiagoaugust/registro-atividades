-- Desafios diarios, semanais e trofeus mensais.
--
-- Duas tabelas: o catalogo e a ideia que se repete; a instancia e o desafio daquele periodo, com o
-- alvo ja calibrado, o progresso e o desfecho. E o que da repeticao, descontinuacao (o catalogo sai
-- do ar sem apagar o passado) e a taxa de cumprimento de graca.
--
-- Nenhum alvo e escolhido a mao. Alvo nulo no catalogo significa "calibre pelo meu historico quando
-- a instancia nascer"; os poucos alvos fixos sao contagens que nao fazem sentido calibrar (concluir
-- a revisao da semana e 1, nao "a sua mediana de revisoes").

create table desafio_periodico (
    id          bigserial primary key,
    escopo      varchar(10)  not null check (escopo in ('DIARIO', 'SEMANAL', 'MENSAL')),
    titulo      varchar(160) not null,
    descricao   text,
    metrica     varchar(30)  not null,
    -- categoria alvo, fator de calibracao, piso: o que cada metrica precisar
    parametros  jsonb        not null default '{}'::jsonb,
    tipo        varchar(10)  not null default 'META' check (tipo in ('META', 'RECORDE')),
    alvo        numeric(10, 2) check (alvo > 0),
    -- Descontinuar e desligar aqui: as instancias ja cumpridas continuam no historico.
    ativo       boolean      not null default true,
    ordem       int          not null default 0,
    criado_em   timestamptz  not null default now()
);

create index idx_desafio_periodico_ativo on desafio_periodico (escopo, ordem) where ativo;

create table desafio_instancia (
    id             bigserial primary key,
    desafio_id     bigint         not null references desafio_periodico (id) on delete cascade,
    periodo_inicio date           not null,
    periodo_fim    date           not null,
    alvo           numeric(10, 2) not null,
    progresso      numeric(10, 2) not null default 0,
    status         varchar(10)    not null default 'ABERTO'
                   check (status in ('ABERTO', 'CUMPRIDO', 'PERDIDO')),
    cumprido_em    timestamptz,
    atualizado_em  timestamptz    not null default now(),
    -- Um desafio rende uma instancia por periodo; e o que torna a geracao idempotente.
    constraint uq_desafio_periodo unique (desafio_id, periodo_inicio)
);

create index idx_instancia_periodo on desafio_instancia (periodo_inicio desc);
create index idx_instancia_aberta on desafio_instancia (status) where status = 'ABERTO';

-- XP de desafio cumprido entra no mesmo ledger das outras origens.
alter table xp_lancamento drop constraint xp_lancamento_origem_check;
alter table xp_lancamento add constraint xp_lancamento_origem_check
    check (origem in ('REGISTRO', 'ACAO_GTD', 'REVISAO_SEMANAL', 'DESAFIO_PERIODICO'));

-- O catalogo. "fator" diz o quanto acima da sua mediana o desafio pede; "minimo" impede que um
-- historico fraco gere uma meta de zero. O XP do dia nao aparece aqui: quem cuida do curto prazo
-- em XP e a faixa de esforco, que muda com a energia da manha.
insert into desafio_periodico (escopo, titulo, descricao, metrica, parametros, tipo, alvo, ordem) values
    ('DIARIO', 'Registre o dia', 'Pelo menos uma atividade ou o check-in',
     'DIAS_COM_PRESENCA', '{}', 'META', 1, 10),
    ('DIARIO', 'Dia equilibrado', 'Tres categorias diferentes no mesmo dia',
     'CATEGORIAS_DISTINTAS', '{}', 'META', 3, 20),
    ('DIARIO', 'Pratique de verdade', 'Minutos de pratica deliberada no dia',
     'MINUTOS_PRATICA', '{"fator":1.0,"minimo":15}', 'META', null, 30),

    ('SEMANAL', 'Semana de treino', 'Dias com treino nesta semana',
     'DIAS_COM_CATEGORIA', '{"categoria":"TREINO","fator":1.0,"minimo":2}', 'META', null, 10),
    ('SEMANAL', 'Horas de estudo', 'Minutos de estudo na semana',
     'MINUTOS_ESTUDO', '{"fator":1.1,"minimo":60}', 'META', null, 20),
    ('SEMANAL', 'Revisao feita', 'Concluir a revisao semanal',
     'REVISOES', '{}', 'META', 1, 30),
    ('SEMANAL', 'Semana presente', 'Dias ativos na semana',
     'DIAS_COM_PRESENCA', '{"fator":1.0,"minimo":3}', 'META', null, 40),

    ('MENSAL', 'Mes presente', 'Dias ativos no mes',
     'DIAS_COM_PRESENCA', '{"fator":1.0,"minimo":12}', 'META', null, 10),
    ('MENSAL', 'Mes sem furar revisao', 'Quatro revisoes semanais concluidas',
     'REVISOES', '{}', 'META', 4, 20),
    ('MENSAL', 'Recorde de quilometragem', 'Mais quilometros que em qualquer mes anterior',
     'KM', '{"minimo":10}', 'RECORDE', null, 30),
    ('MENSAL', 'Recorde de leitura', 'Mais paginas que em qualquer mes anterior',
     'PAGINAS', '{"minimo":50}', 'RECORDE', null, 40),
    ('MENSAL', 'Mes de superacao', 'Dias dificeis vencidos no mes',
     'DIAS_DIFICEIS_VENCIDOS', '{"fator":1.0,"minimo":3}', 'META', null, 50),
    ('MENSAL', 'Recorde de pratica', 'Mais pratica deliberada que em qualquer mes anterior',
     'MINUTOS_PRATICA', '{"minimo":60}', 'RECORDE', null, 60);
