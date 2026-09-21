-- Capa, dificuldade, categoria e leitura retroativa.

create table categoria_livro (
    id    bigserial primary key,
    nome  varchar(60) not null unique,
    ativa boolean     not null default true,
    ordem int         not null default 0
);

insert into categoria_livro (nome, ordem) values
    ('Tecnico', 10),
    ('Literatura', 20),
    ('Desenvolvimento pessoal', 30),
    ('Psicologia', 40),
    ('Historia', 50),
    ('Filosofia', 60),
    ('Biografia', 70),
    ('Negocios', 80),
    ('Ciencia', 90);

alter table livro
    add column capa_url     varchar(1000),
    add column dificuldade  smallint check (dificuldade between 1 and 5),
    add column categoria_id bigint references categoria_livro (id),
    -- Leitura retroativa: livro lido antes de o sistema existir, sem sessao registrada. Guardar
    -- quantos dias levou e (quando lembrado) quantas horas e o suficiente para ele contar na
    -- estante e nas metricas por categoria, sem inventar sessoes que nunca foram registradas.
    add column dias_leitura int           check (dias_leitura > 0),
    add column horas_leitura numeric(6, 1) check (horas_leitura > 0);

create index idx_livro_categoria on livro (categoria_id) where categoria_id is not null;

-- Conquistas por genero: so agora ha o que contar.
insert into conquista (codigo, titulo, descricao, tipo_regra, parametros, ordem) values
    ('CINCO_TECNICOS', 'Estante tecnica', 'Concluir cinco livros tecnicos',
     'CONTAGEM_EVENTO', '{"evento":"LIVRO_TECNICO","alvo":5}', 190),
    ('CINCO_CATEGORIAS', 'Leitor variado', 'Concluir livros de cinco categorias diferentes',
     'CONTAGEM_EVENTO', '{"evento":"CATEGORIAS_LIDAS","alvo":5}', 200);
