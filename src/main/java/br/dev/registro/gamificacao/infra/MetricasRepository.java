package br.dev.registro.gamificacao.infra;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;

/**
 * Metricas acumuladas que alimentam as conquistas. Tudo somado no banco — sao varreduras sobre a
 * historia inteira, e trazer os registros para memoria so para somar seria desperdicio.
 *
 * <p>Toda metrica e acumulada <b>ate</b> um dia, nunca sobre a tabela inteira: uma conquista tem de
 * ser desbloqueada no dia em que o marco foi atingido. Sem o corte, recalcular o passado (ou semear
 * historico) liberaria "100 km" e "500 km" no mesmo dia, antes de os quilometros existirem.
 */
@ApplicationScoped
public class MetricasRepository {

    private final EntityManager em;

    public MetricasRepository(EntityManager em) {
        this.em = em;
    }

    private double numero(String sql, LocalDate ate) {
        Object valor = em.createNativeQuery(sql).setParameter("ate", ate).getSingleResult();
        return valor == null ? 0 : ((Number) valor).doubleValue();
    }

    /** Quilometros acumulados (qualquer modalidade que registre distancia). */
    public double kmAcumulados(LocalDate ate) {
        return numero(
                """
                select coalesce(sum((detalhes->>'distanciaKm')::numeric), 0)
                  from registro_atividade
                 where jsonb_exists(detalhes, 'distanciaKm') and data_local <= :ate
                """, ate);
    }

    /** Paginas lidas: a final menos a inicial, mais um (ler da 10 a 10 e uma pagina). */
    public double paginasLidas(LocalDate ate) {
        return numero(
                """
                select coalesce(sum((detalhes->>'paginaFinal')::int - (detalhes->>'paginaInicial')::int + 1), 0)
                  from registro_atividade
                 where jsonb_exists(detalhes, 'paginaFinal')
                   and jsonb_exists(detalhes, 'paginaInicial')
                   and data_local <= :ate
                """, ate);
    }

    public double minutosEstudo(LocalDate ate) {
        return numero(
                "select coalesce(sum(duracao_min), 0) from registro_atividade"
                        + " where categoria = 'ESTUDO' and data_local <= :ate", ate);
    }

    public double totalRegistros(LocalDate ate) {
        return numero("select count(*) from registro_atividade where data_local <= :ate", ate);
    }

    public double livrosConcluidos(LocalDate ate) {
        return numero(
                "select count(*) from livro where status = 'CONCLUIDO' and concluido_em <= :ate", ate);
    }

    /** Desafio nao guarda data de conclusao; o prazo final e a melhor aproximacao que existe. */
    public double desafiosConcluidos(LocalDate ate) {
        return numero(
                "select count(*) from desafio where status = 'CONCLUIDO'"
                        + " and (fim is null or fim <= :ate)", ate);
    }

    public double projetosConcluidos(LocalDate ate) {
        return numero(
                "select count(*) from projeto where status = 'CONCLUIDO'"
                        + " and concluido_em::date <= :ate", ate);
    }

    public double itensCapturados(LocalDate ate) {
        return numero("select count(*) from inbox_item where capturado_em::date <= :ate", ate);
    }

    public double acoesConcluidas(LocalDate ate) {
        return numero(
                "select count(*) from acao where estado = 'CONCLUIDA'"
                        + " and concluida_em::date <= :ate", ate);
    }

    public double revisoesConcluidas(LocalDate ate) {
        return numero(
                "select count(*) from revisao_semanal where concluida_em::date <= :ate", ate);
    }

    /**
     * 1 quando o inbox esta vazio tendo ja processado alguma coisa. Nao leva corte de data: "inbox
     * zerado" e uma afirmacao sobre agora, nao sobre um dia do passado.
     */
    public double inboxZerado() {
        Object valor = em.createNativeQuery(
                        """
                        select case
                                 when count(*) filter (where processado_em is not null) > 0
                                  and count(*) filter (where processado_em is null) = 0
                                 then 1 else 0
                               end
                          from inbox_item
                        """)
                .getSingleResult();
        return ((Number) valor).doubleValue();
    }

    public double livrosTecnicos(LocalDate ate) {
        return numero(
                """
                select count(*) from livro l
                  join categoria_livro c on c.id = l.categoria_id
                 where l.status = 'CONCLUIDO' and c.nome = 'Tecnico' and l.concluido_em <= :ate
                """, ate);
    }

    /** Quantas categorias distintas ja renderam pelo menos um livro concluido. */
    public double categoriasLidas(LocalDate ate) {
        return numero(
                """
                select count(distinct l.categoria_id) from livro l
                 where l.status = 'CONCLUIDO' and l.categoria_id is not null
                   and l.concluido_em <= :ate
                """, ate);
    }

    /** Minutos por categoria de um dia, para o resumo materializado. */
    @SuppressWarnings("unchecked")
    public java.util.List<Object[]> minutosPorCategoria(java.time.LocalDate dia) {
        return em.createNativeQuery(
                        """
                        select categoria, sum(duracao_min)
                          from registro_atividade
                         where data_local = :dia
                      group by categoria
                        """)
                .setParameter("dia", dia)
                .getResultList();
    }
}
