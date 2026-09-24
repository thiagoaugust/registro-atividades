-- Fila de leitura: o mesmo livro, antes de a primeira sessao tira-lo dela (docs/PLANO.md 5.8).
alter table livro drop constraint livro_status_check;
alter table livro add constraint livro_status_check
    check (status in ('LENDO', 'CONCLUIDO', 'QUERO_LER', 'ABANDONADO'));
