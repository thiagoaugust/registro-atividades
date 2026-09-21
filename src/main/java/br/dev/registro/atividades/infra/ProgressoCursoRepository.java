package br.dev.registro.atividades.infra;

import br.dev.registro.atividades.domain.ProgressoCurso;
import br.dev.registro.atividades.domain.ProgressoCurso.Agregado;
import br.dev.registro.atividades.domain.StatusCurso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Agregados de estudo por curso e por area de conhecimento. Somas no banco; o Java so monta os DTOs.
 */
@ApplicationScoped
public class ProgressoCursoRepository {

    /** Minutos de pratica deliberada de uma sessao — parte da duracao, nao tempo a mais. */
    private static final String MINUTOS_PRATICA =
            "coalesce((r.detalhes->>'minutosPratica')::int, 0)";

    private final EntityManager em;

    public ProgressoCursoRepository(EntityManager em) {
        this.em = em;
    }

    @SuppressWarnings("unchecked")
    public List<Agregado> agregados(LocalDate hoje) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        with sessoes as (
                            select r.curso_id,
                                   r.data_local,
                                   r.duracao_min,
                                   r.esforco,
                                   %s as minutos_pratica
                              from registro_atividade r
                             where r.curso_id is not null
                        )
                        select c.id, c.titulo, c.instituicao, c.url, c.carga_horaria,
                               c.area_id, a.nome, c.status, c.concluido_em,
                               c.horas_retroativas, c.dias_retroativos,
                               coalesce((select sum(s.duracao_min) from sessoes s
                                          where s.curso_id = c.id), 0),
                               coalesce((select sum(s.minutos_pratica) from sessoes s
                                          where s.curso_id = c.id), 0),
                               (select count(*) from sessoes s where s.curso_id = c.id),
                               (select avg(s.esforco) from sessoes s where s.curso_id = c.id),
                               (select min(s.data_local) from sessoes s where s.curso_id = c.id),
                               (select max(s.data_local) from sessoes s where s.curso_id = c.id),
                               coalesce((select sum(s.duracao_min) from sessoes s
                                          where s.curso_id = c.id and s.data_local > :inicioJanela), 0)
                          from curso c
                          left join area_conhecimento a on a.id = c.area_id
                      order by case c.status when 'CURSANDO' then 0 when 'CONCLUIDO' then 1 else 2 end,
                               c.concluido_em desc nulls last,
                               c.titulo
                        """
                                .formatted(MINUTOS_PRATICA))
                .setParameter("inicioJanela", hoje.minusDays(ProgressoCurso.JANELA_RITMO_DIAS))
                .getResultList();

        return linhas.stream()
                .map(l -> new Agregado(
                        ((Number) l[0]).longValue(),
                        (String) l[1],
                        (String) l[2],
                        (String) l[3],
                        l[4] == null ? null : ((Number) l[4]).doubleValue(),
                        l[5] == null ? null : ((Number) l[5]).longValue(),
                        (String) l[6],
                        StatusCurso.valueOf((String) l[7]),
                        (LocalDate) l[8],
                        l[9] == null ? null : ((Number) l[9]).doubleValue(),
                        l[10] == null ? null : ((Number) l[10]).intValue(),
                        ((Number) l[11]).intValue(),
                        ((Number) l[12]).intValue(),
                        ((Number) l[13]).intValue(),
                        l[14] == null ? null : ((Number) l[14]).doubleValue(),
                        (LocalDate) l[15],
                        (LocalDate) l[16],
                        ((Number) l[17]).intValue()))
                .toList();
    }

    public record TempoPorArea(
            Long areaId,
            String area,
            int minutosCurso,
            int minutosLivro,
            int minutosPratica,
            int cursos,
            int livros) {

        public int minutosTotal() {
            return minutosCurso + minutosLivro;
        }
    }

    /**
     * Onde o tempo de estudo foi parar, por area de conhecimento — somando curso e livro, que e a
     * pergunta que a area compartilhada existe para responder. Retroativos entram com o tempo
     * informado no cadastro.
     */
    @SuppressWarnings("unchecked")
    public List<TempoPorArea> tempoPorArea() {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        with curso_tempo as (
                            select c.area_id,
                                   coalesce(c.horas_retroativas * 60,
                                            (select sum(r.duracao_min) from registro_atividade r
                                              where r.curso_id = c.id), 0) as minutos,
                                   coalesce((select sum(%s) from registro_atividade r
                                              where r.curso_id = c.id), 0) as minutos_pratica,
                                   1 as curso
                              from curso c
                        ),
                        livro_tempo as (
                            select l.area_id,
                                   coalesce(l.horas_leitura * 60,
                                            (select sum(r.duracao_min) from registro_atividade r
                                              where r.livro_id = l.id), 0) as minutos,
                                   1 as livro
                              from livro l
                        ),
                        areas as (
                            select area_id from curso_tempo
                            union
                            select area_id from livro_tempo
                        )
                        select ar.area_id,
                               coalesce(a.nome, 'Sem area'),
                               coalesce((select sum(ct.minutos) from curso_tempo ct
                                          where ct.area_id is not distinct from ar.area_id), 0),
                               coalesce((select sum(lt.minutos) from livro_tempo lt
                                          where lt.area_id is not distinct from ar.area_id), 0),
                               coalesce((select sum(ct.minutos_pratica) from curso_tempo ct
                                          where ct.area_id is not distinct from ar.area_id), 0),
                               coalesce((select sum(ct.curso) from curso_tempo ct
                                          where ct.area_id is not distinct from ar.area_id), 0),
                               coalesce((select sum(lt.livro) from livro_tempo lt
                                          where lt.area_id is not distinct from ar.area_id), 0)
                          from areas ar
                          left join area_conhecimento a on a.id = ar.area_id
                      order by 3 + 4 desc, a.ordem nulls last
                        """
                                .formatted(MINUTOS_PRATICA))
                .getResultList();

        return linhas.stream()
                .map(l -> new TempoPorArea(
                        l[0] == null ? null : ((Number) l[0]).longValue(),
                        (String) l[1],
                        ((Number) l[2]).intValue(),
                        ((Number) l[3]).intValue(),
                        ((Number) l[4]).intValue(),
                        ((Number) l[5]).intValue(),
                        ((Number) l[6]).intValue()))
                .toList();
    }

    public record TempoPorTema(String tema, int minutos, int minutosPratica, int sessoes) {
    }

    /** O detalhe fino: o tema livre que voce escreve em cada sessao de estudo. */
    @SuppressWarnings("unchecked")
    public List<TempoPorTema> tempoPorTema(LocalDate de, LocalDate ate) {
        List<Object[]> linhas = em.createNativeQuery(
                        """
                        select coalesce(r.detalhes->>'tema', 'sem tema'),
                               sum(r.duracao_min),
                               sum(%s),
                               count(*)
                          from registro_atividade r
                         where r.categoria = 'ESTUDO' and r.data_local between :de and :ate
                      group by 1
                      order by 2 desc
                        """
                                .formatted(MINUTOS_PRATICA))
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getResultList();

        return linhas.stream()
                .map(l -> new TempoPorTema(
                        (String) l[0],
                        ((Number) l[1]).intValue(),
                        ((Number) l[2]).intValue(),
                        ((Number) l[3]).intValue()))
                .toList();
    }

    public record ResumoPratica(int minutosEstudo, int minutosPratica, Double percentual) {
    }

    /** Quanto do estudo do periodo foi pratica deliberada, e nao consumo. */
    public ResumoPratica resumoPratica(LocalDate de, LocalDate ate) {
        Object[] l = (Object[]) em.createNativeQuery(
                        """
                        select coalesce(sum(r.duracao_min), 0), coalesce(sum(%s), 0)
                          from registro_atividade r
                         where r.categoria = 'ESTUDO' and r.data_local between :de and :ate
                        """
                                .formatted(MINUTOS_PRATICA))
                .setParameter("de", de)
                .setParameter("ate", ate)
                .getSingleResult();

        int estudo = ((Number) l[0]).intValue();
        int pratica = ((Number) l[1]).intValue();
        Double percentual = estudo == 0 ? null : Math.round(pratica * 1000.0 / estudo) / 10.0;
        return new ResumoPratica(estudo, pratica, percentual);
    }
}
