package br.dev.registro.atividades.infra;

import br.dev.registro.atividades.domain.ProgressoLeitura;
import br.dev.registro.atividades.domain.ProgressoLeitura.Agregado;
import br.dev.registro.atividades.domain.StatusLivro;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Agregados de leitura por livro, somados no banco. O Java recebe os numeros crus e
 * {@link ProgressoLeitura} faz as contas derivadas.
 *
 * <p>Nas somas por categoria e dificuldade, livro retroativo entra com o que foi informado no
 * cadastro (total de paginas, horas) e os demais com o que foi de fato registrado em sessoes.
 */
@ApplicationScoped
public class ProgressoLeituraRepository {

    /** Quantas sessoes recentes definem o "ritmo atual" de um livro. */
    private static final int SESSOES_RECENTES = 5;

    /** Paginas e minutos por livro, somados das sessoes registradas. */
    private static final String CTE_LEITURA =
            """
            with leitura as (
                select r.livro_id,
                       sum(r.duracao_min) filter (
                           where jsonb_exists(r.detalhes, 'paginaFinal')) as minutos,
                       sum((r.detalhes->>'paginaFinal')::int
                         - (r.detalhes->>'paginaInicial')::int + 1) filter (
                           where jsonb_exists(r.detalhes, 'paginaFinal')
                             and jsonb_exists(r.detalhes, 'paginaInicial')) as paginas
                  from registro_atividade r
                 where r.categoria = 'LEITURA' and r.livro_id is not null
              group by r.livro_id
            )
            """;

    /** Retroativo usa o cadastro; o resto usa o que foi registrado. */
    private static final String PAGINAS_DO_LIVRO =
            """
            coalesce(sum(coalesce(
                case when l.dias_leitura is not null then l.total_paginas end,
                le.paginas, 0)), 0)
            """;

    private static final String MINUTOS_DO_LIVRO =
            """
            coalesce(sum(coalesce(
                case when l.dias_leitura is not null then l.horas_leitura * 60 end,
                le.minutos, 0)), 0)
            """;

    private final EntityManager em;

    public ProgressoLeituraRepository(EntityManager em) {
        this.em = em;
    }

    @SuppressWarnings("unchecked")
    public List<Agregado> agregados(LocalDate hoje) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        with sessoes as (
                            select r.livro_id,
                                   r.data_local,
                                   r.duracao_min,
                                   r.esforco,
                                   case when jsonb_exists(r.detalhes, 'paginaFinal')
                                        then (r.detalhes->>'paginaFinal')::int end as pagina_final,
                                   case when jsonb_exists(r.detalhes, 'paginaFinal')
                                         and jsonb_exists(r.detalhes, 'paginaInicial')
                                        then (r.detalhes->>'paginaFinal')::int
                                           - (r.detalhes->>'paginaInicial')::int + 1 end as paginas
                              from registro_atividade r
                             where r.categoria = 'LEITURA' and r.livro_id is not null
                        ),
                        com_paginas as (
                            select s.*,
                                   row_number() over (
                                       partition by s.livro_id
                                       order by s.data_local desc) as recencia
                              from sessoes s
                             where s.paginas is not null
                        )
                        select l.id, l.titulo, l.autor, l.total_paginas, l.status, l.concluido_em,
                               l.capa_url, l.dificuldade, l.categoria_id, c.nome,
                               l.dias_leitura, l.horas_leitura,
                               coalesce((select max(p.pagina_final) from com_paginas p
                                          where p.livro_id = l.id), 0),
                               coalesce((select sum(p.paginas) from com_paginas p
                                          where p.livro_id = l.id), 0),
                               (select count(*) from sessoes s where s.livro_id = l.id),
                               coalesce((select sum(s.duracao_min) from sessoes s
                                          where s.livro_id = l.id), 0),
                               (select avg(s.esforco) from sessoes s where s.livro_id = l.id),
                               (select min(s.data_local) from sessoes s where s.livro_id = l.id),
                               (select max(s.data_local) from sessoes s where s.livro_id = l.id),
                               coalesce((select sum(p.duracao_min) from com_paginas p
                                          where p.livro_id = l.id), 0),
                               coalesce((select sum(p.paginas) from com_paginas p
                                          where p.livro_id = l.id and p.recencia <= :recentes), 0),
                               coalesce((select sum(p.duracao_min) from com_paginas p
                                          where p.livro_id = l.id and p.recencia <= :recentes), 0),
                               coalesce((select sum(p.paginas) from com_paginas p
                                          where p.livro_id = l.id and p.data_local > :inicioJanela), 0)
                          from livro l
                          left join categoria_livro c on c.id = l.categoria_id
                          -- o que esta sendo lido vem primeiro; concluidos, do mais recente;
                          -- ordenar por status cru daria ordem alfabetica (ABANDONADO, CONCLUIDO...)
                      order by case l.status when 'LENDO' then 0 when 'CONCLUIDO' then 1 else 2 end,
                               l.concluido_em desc nulls last,
                               l.titulo
                        """)
                .setParameter("recentes", SESSOES_RECENTES)
                .setParameter("inicioJanela", hoje.minusDays(ProgressoLeitura.JANELA_RITMO_DIAS))
                .getResultList();

        return linhas.stream().map(ProgressoLeituraRepository::paraAgregado).toList();
    }

    private static Agregado paraAgregado(Object[] l) {
        return new Agregado(
                ((Number) l[0]).longValue(),
                (String) l[1],
                (String) l[2],
                l[3] == null ? null : ((Number) l[3]).intValue(),
                StatusLivro.valueOf((String) l[4]),
                (LocalDate) l[5],
                (String) l[6],
                l[7] == null ? null : ((Number) l[7]).shortValue(),
                l[8] == null ? null : ((Number) l[8]).longValue(),
                (String) l[9],
                l[10] == null ? null : ((Number) l[10]).intValue(),
                l[11] == null ? null : ((Number) l[11]).doubleValue(),
                ((Number) l[12]).intValue(),
                ((Number) l[13]).intValue(),
                ((Number) l[14]).intValue(),
                ((Number) l[15]).intValue(),
                l[16] == null ? null : ((Number) l[16]).doubleValue(),
                (LocalDate) l[17],
                (LocalDate) l[18],
                ((Number) l[19]).intValue(),
                ((Number) l[20]).intValue(),
                ((Number) l[21]).intValue(),
                ((Number) l[22]).intValue());
    }

    /**
     * Pagina em que a leitura parou. E o que permite pedir so "parei na pagina X" no registro: o
     * inicio do trecho sai daqui.
     */
    public int ultimaPaginaLida(long livroId, Long ignorarRegistroId) {
        Object valor = em.createNativeQuery(
                        """
                        select coalesce(max((detalhes->>'paginaFinal')::int), 0)
                          from registro_atividade
                         where livro_id = :id
                           and categoria = 'LEITURA'
                           and jsonb_exists(detalhes, 'paginaFinal')
                           -- cast explicito: o Postgres nao infere o tipo de um parametro em "? is null"
                           and (cast(:ignorar as bigint) is null or id <> cast(:ignorar as bigint))
                        """)
                .setParameter("id", livroId)
                .setParameter("ignorar", ignorarRegistroId)
                .getSingleResult();
        return ((Number) valor).intValue();
    }

    public record PorCategoria(
            Long categoriaId,
            String categoria,
            int concluidos,
            int lendo,
            int paginas,
            Double paginasPorHora) {
    }

    /** Livro sem categoria aparece como "Sem categoria" em vez de sumir da conta. */
    @SuppressWarnings("unchecked")
    public List<PorCategoria> porCategoria() {
        List<Object[]> linhas = em.createNativeQuery(
                        CTE_LEITURA
                                + """
                                select l.categoria_id,
                                       coalesce(c.nome, 'Sem categoria'),
                                       count(*) filter (where l.status = 'CONCLUIDO'),
                                       count(*) filter (where l.status = 'LENDO'),
                                """
                                + PAGINAS_DO_LIVRO
                                + ", "
                                + MINUTOS_DO_LIVRO
                                + """
                                  from livro l
                                  left join categoria_livro c on c.id = l.categoria_id
                                  left join leitura le on le.livro_id = l.id
                              group by l.categoria_id, c.nome, c.ordem
                              order by c.ordem nulls last, c.nome
                                """)
                .getResultList();

        return linhas.stream()
                .map(l -> new PorCategoria(
                        l[0] == null ? null : ((Number) l[0]).longValue(),
                        (String) l[1],
                        ((Number) l[2]).intValue(),
                        ((Number) l[3]).intValue(),
                        ((Number) l[4]).intValue(),
                        velocidade(((Number) l[4]).doubleValue(), ((Number) l[5]).doubleValue())))
                .toList();
    }

    public record PorDificuldade(short dificuldade, int livros, int paginas, Double paginasPorHora) {
    }

    /**
     * Velocidade por grau de dificuldade declarado — o cruzamento que mostra quanto um livro denso
     * custa a mais de tempo que um leve.
     */
    @SuppressWarnings("unchecked")
    public List<PorDificuldade> porDificuldade() {
        List<Object[]> linhas = em.createNativeQuery(
                        CTE_LEITURA
                                + """
                                select l.dificuldade, count(*),
                                """
                                + PAGINAS_DO_LIVRO
                                + ", "
                                + MINUTOS_DO_LIVRO
                                + """
                                  from livro l
                                  left join leitura le on le.livro_id = l.id
                                 where l.dificuldade is not null
                              group by l.dificuldade
                              order by l.dificuldade
                                """)
                .getResultList();

        return linhas.stream()
                .map(l -> new PorDificuldade(
                        ((Number) l[0]).shortValue(),
                        ((Number) l[1]).intValue(),
                        ((Number) l[2]).intValue(),
                        velocidade(((Number) l[2]).doubleValue(), ((Number) l[3]).doubleValue())))
                .toList();
    }

    private static Double velocidade(double paginas, double minutos) {
        if (paginas <= 0 || minutos <= 0) {
            return null;
        }
        return Math.round(paginas * 60.0 / minutos * 10) / 10.0;
    }
}
