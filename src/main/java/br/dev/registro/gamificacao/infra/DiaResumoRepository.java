package br.dev.registro.gamificacao.infra;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.gamificacao.domain.ClassificadorDia.Baseline;
import br.dev.registro.gamificacao.domain.DiaPresenca;
import br.dev.registro.gamificacao.domain.DiaResumo;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class DiaResumoRepository implements PanacheRepositoryBase<DiaResumo, LocalDate> {

    /**
     * Janela movel dos dias anteriores. Dias de descanso ficam de fora: uma semana de ferias
     * rebaixaria a media e faria os dias seguintes parecerem excelentes sem motivo.
     */
    public Baseline baseline(LocalDate dia, int diasJanela) {
        Object[] linha = (Object[]) getEntityManager()
                .createNativeQuery(
                        """
                        select coalesce(avg(xp_total), 0),
                               coalesce(stddev_samp(xp_total), 0),
                               count(*)
                          from dia_resumo
                         where data_local < :dia
                           and data_local >= :inicio
                           and not descanso
                        """)
                .setParameter("dia", dia)
                .setParameter("inicio", dia.minusDays(diasJanela))
                .getSingleResult();

        return new Baseline(
                ((Number) linha[0]).doubleValue(),
                ((Number) linha[1]).doubleValue(),
                ((Number) linha[2]).intValue());
    }

    /** Reescreve a quebra por categoria do dia (o recalculo sempre substitui, nunca acumula). */
    public void substituirCategorias(LocalDate dia, Map<Categoria, Integer> xp, Map<Categoria, Integer> minutos) {
        getEntityManager()
                .createNativeQuery("delete from dia_categoria_resumo where data_local = :dia")
                .setParameter("dia", dia)
                .executeUpdate();

        for (Map.Entry<Categoria, Integer> entrada : minutos.entrySet()) {
            getEntityManager()
                    .createNativeQuery(
                            """
                            insert into dia_categoria_resumo (data_local, categoria, xp, minutos)
                            values (:dia, :categoria, :xp, :minutos)
                            """)
                    .setParameter("dia", dia)
                    .setParameter("categoria", entrada.getKey().name())
                    .setParameter("xp", xp.getOrDefault(entrada.getKey(), 0))
                    .setParameter("minutos", entrada.getValue())
                    .executeUpdate();
        }
    }

    public long xpAcumulado() {
        Object soma = getEntityManager()
                .createNativeQuery("select coalesce(sum(xp_total), 0) from dia_resumo")
                .getSingleResult();
        return ((Number) soma).longValue();
    }

    public Map<Categoria, Long> xpAcumuladoPorCategoria() {
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = getEntityManager()
                .createNativeQuery(
                        "select categoria, coalesce(sum(xp), 0) from dia_categoria_resumo group by categoria")
                .getResultList();

        Map<Categoria, Long> total = new EnumMap<>(Categoria.class);
        for (Object[] linha : linhas) {
            total.put(Categoria.valueOf((String) linha[0]), ((Number) linha[1]).longValue());
        }
        return total;
    }

    /**
     * Dias com presenca (ou descanso planejado) ate a data, do mais recente para tras. A streak e
     * contada em Java sobre essa lista: sao no maximo 366 linhas, e a regra de "descanso nao quebra"
     * fica legivel e testavel em vez de virar um CTE recursivo.
     */
    public List<DiaPresenca> presencaRecente(LocalDate ate, int limiteDias) {
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = getEntityManager()
                .createNativeQuery(
                        """
                        select data_local, presenca, descanso
                          from dia_resumo
                         where data_local <= :ate
                      order by data_local desc
                         limit :limite
                        """)
                .setParameter("ate", ate)
                .setParameter("limite", limiteDias)
                .getResultList();

        List<DiaPresenca> dias = new ArrayList<>(linhas.size());
        for (Object[] linha : linhas) {
            dias.add(new DiaPresenca(paraLocalDate(linha[0]), (Boolean) linha[1], (Boolean) linha[2]));
        }
        return dias;
    }

    /** Dias em que houve atividade de uma categoria, do mais recente para tras. */
    public List<DiaPresenca> presencaRecente(Categoria categoria, LocalDate ate, int limiteDias) {
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = getEntityManager()
                .createNativeQuery(
                        """
                        select d.data_local,
                               (c.categoria is not null) as presenca,
                               d.descanso
                          from dia_resumo d
                          left join dia_categoria_resumo c
                                 on c.data_local = d.data_local and c.categoria = :categoria
                         where d.data_local <= :ate
                      order by d.data_local desc
                         limit :limite
                        """)
                .setParameter("categoria", categoria.name())
                .setParameter("ate", ate)
                .setParameter("limite", limiteDias)
                .getResultList();

        List<DiaPresenca> dias = new ArrayList<>(linhas.size());
        for (Object[] linha : linhas) {
            dias.add(new DiaPresenca(paraLocalDate(linha[0]), (Boolean) linha[1], (Boolean) linha[2]));
        }
        return dias;
    }

    /** O driver devolve LocalDate para colunas date, mas java.sql.Date ainda aparece em alguns tipos. */
    private static LocalDate paraLocalDate(Object valor) {
        return valor instanceof java.sql.Date data ? data.toLocalDate() : (LocalDate) valor;
    }

    public long contarDiasEquilibrados(int minimoCategorias, LocalDate ate) {
        return count("categoriasDistintas >= ?1 and dataLocal <= ?2", (short) minimoCategorias, ate);
    }

    public long contarDiasDificeisVencidos(LocalDate ate) {
        return count("diaDificilVencido = true and dataLocal <= ?1", ate);
    }
}
