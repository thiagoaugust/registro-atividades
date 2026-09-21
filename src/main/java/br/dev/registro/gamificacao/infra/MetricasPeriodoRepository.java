package br.dev.registro.gamificacao.infra;

import br.dev.registro.gamificacao.domain.MetricaPeriodo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.time.LocalDate;

/**
 * Mede uma grandeza num intervalo de dias. E o mesmo metodo que apura o progresso de um desafio
 * aberto e que le os periodos passados para calibrar o alvo — se fossem duas contas, a meta e a
 * medicao poderiam discordar.
 */
@ApplicationScoped
public class MetricasPeriodoRepository {

    private final EntityManager em;

    public MetricasPeriodoRepository(EntityManager em) {
        this.em = em;
    }

    /** @param categoria so importa nas metricas que {@link MetricaPeriodo#exigeCategoria()} marca */
    public double medir(MetricaPeriodo metrica, LocalDate de, LocalDate ate, String categoria) {
        // A sessao precisa estar sincronizada: sao queries nativas, e o Hibernate nao faz isso sozinho.
        em.flush();

        Query query = em.createNativeQuery(sql(metrica))
                .setParameter("de", de)
                .setParameter("ate", ate);
        if (metrica.exigeCategoria()) {
            query.setParameter("categoria", categoria == null ? "" : categoria);
        }
        Object valor = query.getSingleResult();
        return valor == null ? 0 : ((Number) valor).doubleValue();
    }

    private static String sql(MetricaPeriodo metrica) {
        return switch (metrica) {
            case XP -> resumo("coalesce(sum(xp_total), 0)");
            case DIAS_COM_PRESENCA -> resumo("count(*) filter (where presenca)");
            case DIAS_DIFICEIS_VENCIDOS -> resumo("count(*) filter (where dia_dificil_vencido)");
            // Num escopo maior que o dia, "categorias distintas" e o melhor dia do periodo.
            case CATEGORIAS_DISTINTAS -> resumo("coalesce(max(categorias_distintas), 0)");

            case MINUTOS_TOTAL -> registro("coalesce(sum(duracao_min), 0)", "");
            case MINUTOS_ESTUDO -> registro("coalesce(sum(duracao_min), 0)", " and categoria = 'ESTUDO'");
            case MINUTOS_CATEGORIA -> registro("coalesce(sum(duracao_min), 0)", " and categoria = :categoria");
            case DIAS_COM_CATEGORIA -> registro("count(distinct data_local)", " and categoria = :categoria");
            case MINUTOS_APROVEITADOS -> registro(
                    "coalesce(sum(duracao_min), 0)", " and detalhes->>'local' = 'TRANSPORTE_PUBLICO'");
            case MINUTOS_PRATICA -> registro(
                    "coalesce(sum((detalhes->>'minutosPratica')::int), 0)",
                    " and jsonb_exists(detalhes, 'minutosPratica')");
            case KM -> registro(
                    "coalesce(sum((detalhes->>'distanciaKm')::numeric), 0)",
                    " and jsonb_exists(detalhes, 'distanciaKm')");
            // Ler da pagina 10 a 10 e uma pagina lida, nao zero.
            case PAGINAS -> registro(
                    "coalesce(sum((detalhes->>'paginaFinal')::int - (detalhes->>'paginaInicial')::int + 1), 0)",
                    " and jsonb_exists(detalhes, 'paginaFinal') and jsonb_exists(detalhes, 'paginaInicial')");

            case REVISOES -> """
                    select count(*) from revisao_semanal
                     where concluida_em::date between :de and :ate
                    """;
            case ACOES_CONCLUIDAS -> """
                    select count(*) from acao
                     where estado = 'CONCLUIDA' and concluida_em::date between :de and :ate
                    """;
        };
    }

    private static String resumo(String agregacao) {
        return "select " + agregacao + " from dia_resumo where data_local between :de and :ate";
    }

    private static String registro(String agregacao, String filtro) {
        return "select " + agregacao + " from registro_atividade"
                + " where data_local between :de and :ate" + filtro;
    }
}
