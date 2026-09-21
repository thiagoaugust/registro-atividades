package br.dev.registro.analytics.infra;

import br.dev.registro.analytics.domain.Eixo;
import br.dev.registro.analytics.domain.Granularidade;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Todas as agregacoes acontecem aqui, em SQL: somas por periodo, media movel por window function e
 * ate a correlacao (corr() e funcao nativa do Postgres). O Java so monta os DTOs.
 */
@ApplicationScoped
public class AnalyticsRepository {

    private final EntityManager em;

    public AnalyticsRepository(EntityManager em) {
        this.em = em;
    }

    // ---------- heatmap ----------

    public record DiaHeatmap(
            LocalDate data,
            int xp,
            int minutos,
            BigDecimal indice,
            String classificacao,
            boolean diaDificilVencido,
            boolean descanso) {
    }

    @SuppressWarnings("unchecked")
    public List<DiaHeatmap> heatmap(int ano) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        select data_local, xp_total, minutos_total, indice_produtividade,
                               classificacao, dia_dificil_vencido, descanso
                          from dia_resumo
                         where data_local >= :inicio and data_local <= :fim
                      order by data_local
                        """)
                .setParameter("inicio", LocalDate.of(ano, 1, 1))
                .setParameter("fim", LocalDate.of(ano, 12, 31))
                .getResultList();

        return linhas.stream()
                .map(l -> new DiaHeatmap(
                        (LocalDate) l[0],
                        ((Number) l[1]).intValue(),
                        ((Number) l[2]).intValue(),
                        (BigDecimal) l[3],
                        (String) l[4],
                        (Boolean) l[5],
                        (Boolean) l[6]))
                .toList();
    }

    // ---------- serie por periodo ----------

    public record PontoSerie(
            LocalDate periodo,
            int xp,
            int minutos,
            BigDecimal indiceMedio,
            BigDecimal xpMediaMovel,
            int diasComPresenca,
            int diasDificeisVencidos,
            int dificeis,
            int normais,
            int bons,
            int excelentes) {
    }

    /**
     * Serie agregada com media movel de 7 buckets calculada por window function — trazer as linhas
     * para o Java so para somar de novo seria dobrar o trabalho.
     */
    @SuppressWarnings("unchecked")
    public List<PontoSerie> serie(Granularidade granularidade, LocalDate de, LocalDate ate) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        with por_periodo as (
                            select date_trunc(:unidade, data_local::timestamp)::date as periodo,
                                   sum(xp_total)                                     as xp,
                                   sum(minutos_total)                                as minutos,
                                   round(avg(indice_produtividade), 2)               as indice_medio,
                                   count(*) filter (where presenca)                  as com_presenca,
                                   count(*) filter (where dia_dificil_vencido)       as vencidos,
                                   count(*) filter (where classificacao = 'DIFICIL')   as dificeis,
                                   count(*) filter (where classificacao = 'NORMAL')    as normais,
                                   count(*) filter (where classificacao = 'BOM')       as bons,
                                   count(*) filter (where classificacao = 'EXCELENTE') as excelentes
                              from dia_resumo
                             where data_local between :de and :ate
                          group by 1
                        )
                        select periodo, xp, minutos, indice_medio,
                               round(avg(xp) over (order by periodo rows between 6 preceding and current row), 2),
                               com_presenca, vencidos, dificeis, normais, bons, excelentes
                          from por_periodo
                      order by periodo
                        """)
                .setParameter("unidade", granularidade.unidade())
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();

        return linhas.stream()
                .map(l -> new PontoSerie(
                        (LocalDate) l[0],
                        ((Number) l[1]).intValue(),
                        ((Number) l[2]).intValue(),
                        (BigDecimal) l[3],
                        (BigDecimal) l[4],
                        ((Number) l[5]).intValue(),
                        ((Number) l[6]).intValue(),
                        ((Number) l[7]).intValue(),
                        ((Number) l[8]).intValue(),
                        ((Number) l[9]).intValue(),
                        ((Number) l[10]).intValue()))
                .toList();
    }

    public record TotaisPeriodo(
            int xp, int minutos, double indiceMedio, int diasComPresenca,
            int diasDificeisVencidos, int registros) {
    }

    public TotaisPeriodo totais(LocalDate de, LocalDate ate) {
        Object[] l = (Object[]) em.createNativeQuery(
                        """
                        select coalesce(sum(d.xp_total), 0),
                               coalesce(sum(d.minutos_total), 0),
                               coalesce(round(avg(d.indice_produtividade) filter (where d.presenca), 2), 0),
                               count(*) filter (where d.presenca),
                               count(*) filter (where d.dia_dificil_vencido),
                               (select count(*) from registro_atividade r
                                 where r.data_local between :de and :ate)
                          from dia_resumo d
                         where d.data_local between :de and :ate
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getSingleResult();

        return new TotaisPeriodo(
                ((Number) l[0]).intValue(),
                ((Number) l[1]).intValue(),
                ((Number) l[2]).doubleValue(),
                ((Number) l[3]).intValue(),
                ((Number) l[4]).intValue(),
                ((Number) l[5]).intValue());
    }

    // ---------- por categoria ----------

    public record CategoriaNoPeriodo(LocalDate periodo, String categoria, int xp, int minutos) {
    }

    @SuppressWarnings("unchecked")
    public List<CategoriaNoPeriodo> porCategoria(Granularidade granularidade, LocalDate de, LocalDate ate) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        select date_trunc(:unidade, data_local::timestamp)::date, categoria,
                               sum(xp), sum(minutos)
                          from dia_categoria_resumo
                         where data_local between :de and :ate
                      group by 1, 2
                      order by 1, 2
                        """)
                .setParameter("unidade", granularidade.unidade())
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();

        return linhas.stream()
                .map(l -> new CategoriaNoPeriodo(
                        (LocalDate) l[0],
                        (String) l[1],
                        ((Number) l[2]).intValue(),
                        ((Number) l[3]).intValue()))
                .toList();
    }

    // ---------- metricas especificas ----------

    public record MetricasTreino(double km, Integer paceMedioSegPorKm, int sessoes, int minutos) {
    }

    /**
     * Pace medio e ponderado pela distancia (tempo total / distancia total), nao a media dos paces —
     * senao um tiro de 400 m pesaria igual a uma corrida de 20 km.
     */
    public MetricasTreino metricasTreino(LocalDate de, LocalDate ate) {
        Object[] l = (Object[]) em.createNativeQuery(
                        """
                        select coalesce(sum((detalhes->>'distanciaKm')::numeric), 0),
                               coalesce(sum(duracao_min) filter (where jsonb_exists(detalhes, 'distanciaKm')), 0),
                               count(*),
                               coalesce(sum(duracao_min), 0)
                          from registro_atividade
                         where categoria = 'TREINO' and data_local between :de and :ate
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getSingleResult();

        double km = ((Number) l[0]).doubleValue();
        int minutosComDistancia = ((Number) l[1]).intValue();
        Integer pace = km > 0 ? (int) Math.round(minutosComDistancia * 60 / km) : null;
        return new MetricasTreino(km, pace, ((Number) l[2]).intValue(), ((Number) l[3]).intValue());
    }

    public record MetricasLeitura(int paginas, int livrosConcluidos, int sessoes, int minutos) {
    }

    public MetricasLeitura metricasLeitura(LocalDate de, LocalDate ate) {
        Object[] l = (Object[]) em.createNativeQuery(
                        """
                        select coalesce(sum((detalhes->>'paginaFinal')::int
                                          - (detalhes->>'paginaInicial')::int + 1)
                                        filter (where jsonb_exists(detalhes, 'paginaFinal')), 0),
                               count(*),
                               coalesce(sum(duracao_min), 0),
                               (select count(*) from livro
                                 where status = 'CONCLUIDO' and concluido_em between :de and :ate)
                          from registro_atividade
                         where categoria = 'LEITURA' and data_local between :de and :ate
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getSingleResult();

        return new MetricasLeitura(
                ((Number) l[0]).intValue(),
                ((Number) l[3]).intValue(),
                ((Number) l[1]).intValue(),
                ((Number) l[2]).intValue());
    }

    public record TemaEstudado(String tema, int minutos, int sessoes) {
    }

    @SuppressWarnings("unchecked")
    public List<TemaEstudado> temasEstudados(LocalDate de, LocalDate ate) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        select coalesce(detalhes->>'tema', 'sem tema'), sum(duracao_min), count(*)
                          from registro_atividade
                         where categoria = 'ESTUDO' and data_local between :de and :ate
                      group by 1
                      order by 2 desc
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();

        return linhas.stream()
                .map(l -> new TemaEstudado((String) l[0], ((Number) l[1]).intValue(), ((Number) l[2]).intValue()))
                .toList();
    }

    public record ProgressoDesafio(
            long id, String titulo, String unidade, BigDecimal meta, BigDecimal progresso,
            LocalDate inicio, LocalDate fim, String status) {
    }

    @SuppressWarnings("unchecked")
    public List<ProgressoDesafio> progressoDesafios() {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        select d.id, d.titulo, d.unidade, d.meta_valor, d.inicio, d.fim, d.status,
                               coalesce((select sum((r.detalhes->>'valorProgresso')::numeric)
                                           from registro_atividade r
                                          where r.desafio_id = d.id
                                            and jsonb_exists(r.detalhes, 'valorProgresso')), 0)
                          from desafio d
                      order by d.inicio desc
                        """)
                .getResultList();

        return linhas.stream()
                .map(l -> new ProgressoDesafio(
                        ((Number) l[0]).longValue(),
                        (String) l[1],
                        (String) l[2],
                        (BigDecimal) l[3],
                        (BigDecimal) l[7],
                        (LocalDate) l[4],
                        (LocalDate) l[5],
                        (String) l[6]))
                .toList();
    }

    // ---------- correlacoes ----------

    public record Correlacao(String nome, Double coeficiente, int pares) {
    }

    /** corr() e agregacao nativa do Postgres: nada de trazer os pares para calcular Pearson no Java. */
    public Correlacao correlacao(Eixo x, Eixo y, LocalDate de, LocalDate ate) {
        String colunaX = x.coluna();
        String colunaY = y.coluna();
        Object[] l = (Object[]) em.createNativeQuery(
                        """
                        select corr(%s, %s), count(*) filter (where %s is not null and %s is not null)
                          from vw_dia_analitico
                         where data_local between :de and :ate and presenca
                        """.formatted(colunaX, colunaY, colunaX, colunaY))
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getSingleResult();

        Double coeficiente = l[0] == null ? null : arredondar(((Number) l[0]).doubleValue());
        return new Correlacao("%s x %s".formatted(x.rotulo(), y.rotulo()), coeficiente,
                ((Number) l[1]).intValue());
    }

    public record ProdutividadePorDiaSemana(int diaSemana, BigDecimal indiceMedio, int xpMedio, int dias) {
    }

    @SuppressWarnings("unchecked")
    public List<ProdutividadePorDiaSemana> porDiaDaSemana(LocalDate de, LocalDate ate) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        select dia_semana, round(avg(indice_produtividade), 2), round(avg(xp_total)), count(*)
                          from vw_dia_analitico
                         where data_local between :de and :ate and presenca
                      group by 1
                      order by 1
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();

        return linhas.stream()
                .map(l -> new ProdutividadePorDiaSemana(
                        ((Number) l[0]).intValue(),
                        (BigDecimal) l[1],
                        ((Number) l[2]).intValue(),
                        ((Number) l[3]).intValue()))
                .toList();
    }

    public record PontoDispersao(BigDecimal x, BigDecimal y) {
    }

    /** Os pares crus, para o grafico de dispersao ao lado do coeficiente. */
    @SuppressWarnings("unchecked")
    public List<PontoDispersao> dispersao(Eixo x, Eixo y, LocalDate de, LocalDate ate) {
        String colunaX = x.coluna();
        String colunaY = y.coluna();
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        select %s::numeric, %s::numeric
                          from vw_dia_analitico
                         where data_local between :de and :ate and presenca
                           and %s is not null and %s is not null
                        """.formatted(colunaX, colunaY, colunaX, colunaY))
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();

        return linhas.stream()
                .map(l -> new PontoDispersao((BigDecimal) l[0], (BigDecimal) l[1]))
                .toList();
    }

    // ---------- GTD ----------

    public record MetricasGtd(
            int capturados,
            int processados,
            int pendentes,
            Double idadeMediaPendentesDias,
            int acoesConcluidas,
            int acoesAbertas,
            int projetosParados) {
    }

    public MetricasGtd metricasGtd(LocalDate de, LocalDate ate) {
        Object[] l = (Object[]) em.createNativeQuery(
                        """
                        select (select count(*) from inbox_item
                                 where capturado_em::date between :de and :ate),
                               (select count(*) from inbox_item
                                 where processado_em::date between :de and :ate),
                               (select count(*) from inbox_item where processado_em is null),
                               (select round(avg(extract(epoch from (now() - capturado_em)) / 86400)::numeric, 1)
                                  from inbox_item where processado_em is null),
                               (select count(*) from acao
                                 where estado = 'CONCLUIDA' and concluida_em::date between :de and :ate),
                               (select count(*) from acao
                                 where estado in ('PROXIMA', 'AGENDA', 'AGUARDANDO')),
                               (select count(*) from projeto p
                                 where p.status = 'ATIVO'
                                   and not exists (select 1 from acao a
                                                    where a.projeto_id = p.id
                                                      and a.estado in ('PROXIMA', 'AGENDA', 'AGUARDANDO')))
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getSingleResult();

        return new MetricasGtd(
                ((Number) l[0]).intValue(),
                ((Number) l[1]).intValue(),
                ((Number) l[2]).intValue(),
                l[3] == null ? null : ((Number) l[3]).doubleValue(),
                ((Number) l[4]).intValue(),
                ((Number) l[5]).intValue(),
                ((Number) l[6]).intValue());
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 1000) / 1000.0;
    }

    public record Destaque(LocalDate data, int xp, BigDecimal indice, String classificacao) {
    }

    /** O melhor dia do periodo por XP — o destaque da retrospectiva. */
    public Destaque melhorDia(LocalDate de, LocalDate ate) {
        List<?> linhas = em.createNativeQuery(
                        """
                        select data_local, xp_total, indice_produtividade, classificacao
                          from dia_resumo
                         where data_local between :de and :ate and presenca
                      order by xp_total desc, indice_produtividade desc
                         limit 1
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();

        if (linhas.isEmpty()) {
            return null;
        }
        Object[] l = (Object[]) linhas.get(0);
        return new Destaque((LocalDate) l[0], ((Number) l[1]).intValue(), (BigDecimal) l[2], (String) l[3]);
    }

    public record ConquistaNoAno(String codigo, String titulo, LocalDate dataLocal) {
    }

    @SuppressWarnings("unchecked")
    public List<ConquistaNoAno> conquistasDoPeriodo(LocalDate de, LocalDate ate) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        select d.conquista_codigo, c.titulo, d.data_local
                          from conquista_desbloqueada d
                          join conquista c on c.codigo = d.conquista_codigo
                         where d.data_local between :de and :ate
                      order by d.data_local
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();

        return linhas.stream()
                .map(l -> new ConquistaNoAno((String) l[0], (String) l[1], (LocalDate) l[2]))
                .toList();
    }

    public int revisoesConcluidasNoPeriodo(LocalDate de, LocalDate ate) {
        Object total = em.createNativeQuery(
                        """
                        select count(*) from revisao_semanal
                         where concluida_em::date between :de and :ate
                        """)
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getSingleResult();
        return ((Number) total).intValue();
    }

    /** Menor e maior dia com dados, para a tela saber que anos oferecer. */
    public LocalDate[] intervaloComDados() {
        Object[] l = (Object[]) em.createNativeQuery(
                        "select min(data_local), max(data_local) from dia_resumo where presenca")
                .getSingleResult();
        return new LocalDate[] {(LocalDate) l[0], (LocalDate) l[1]};
    }
}
