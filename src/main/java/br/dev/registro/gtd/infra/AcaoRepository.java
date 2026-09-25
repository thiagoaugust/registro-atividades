package br.dev.registro.gtd.infra;

import br.dev.registro.atividades.domain.Projeto;
import br.dev.registro.atividades.domain.StatusProjeto;
import br.dev.registro.gtd.domain.Acao;
import br.dev.registro.gtd.domain.Energia;
import br.dev.registro.gtd.domain.EstadoAcao;
import br.dev.registro.gtd.domain.ProgressoProjeto;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AcaoRepository implements PanacheRepository<Acao> {

    /** Os vinculos vem carregados: os DTOs sao montados depois do commit. */
    private static final String SELECT =
            """
            select a from Acao a
            left join fetch a.contexto
            left join fetch a.projeto
            """;

    public Optional<Acao> porIdComVinculos(long id) {
        return getEntityManager()
                .createQuery(SELECT + " where a.id = :id", Acao.class)
                .setParameter("id", id)
                .getResultList()
                .stream()
                .findFirst();
    }

    public List<Acao> listar(EstadoAcao estado, Long contextoId, Long projetoId) {
        StringBuilder jpql = new StringBuilder(SELECT + " where 1 = 1");
        if (estado != null) {
            jpql.append(" and a.estado = :estado");
        }
        if (contextoId != null) {
            jpql.append(" and a.contexto.id = :contextoId");
        }
        if (projetoId != null) {
            jpql.append(" and a.projeto.id = :projetoId");
        }
        jpql.append(" order by a.agendadaPara asc nulls last, a.criadaEm asc");

        TypedQuery<Acao> query = getEntityManager().createQuery(jpql.toString(), Acao.class);
        if (estado != null) {
            query.setParameter("estado", estado);
        }
        if (contextoId != null) {
            query.setParameter("contextoId", contextoId);
        }
        if (projetoId != null) {
            query.setParameter("projetoId", projetoId);
        }
        return query.getResultList();
    }

    /**
     * "O que fazer agora?": proximas acoes que cabem no contexto, no tempo livre e na energia do
     * momento. Acao sem tempo ou energia declarados entra sempre — o filtro serve para escolher, nao
     * para esconder o que nao foi detalhado.
     */
    public List<Acao> engajar(Long contextoId, Integer tempoDisponivelMin, Energia energiaAtual) {
        StringBuilder jpql = new StringBuilder(SELECT + " where a.estado = :estado");
        if (contextoId != null) {
            jpql.append(" and (a.contexto is null or a.contexto.id = :contextoId)");
        }
        if (tempoDisponivelMin != null) {
            jpql.append(" and (a.tempoEstimadoMin is null or a.tempoEstimadoMin <= :tempo)");
        }
        if (energiaAtual != null) {
            jpql.append(" and (a.energia is null or a.energia in :energias)");
        }
        jpql.append(" order by a.criadaEm asc");

        TypedQuery<Acao> query = getEntityManager()
                .createQuery(jpql.toString(), Acao.class)
                .setParameter("estado", EstadoAcao.PROXIMA);
        if (contextoId != null) {
            query.setParameter("contextoId", contextoId);
        }
        if (tempoDisponivelMin != null) {
            query.setParameter("tempo", tempoDisponivelMin);
        }
        if (energiaAtual != null) {
            query.setParameter("energias", energiasAteA(energiaAtual));
        }
        return query.getResultList();
    }

    private static List<Energia> energiasAteA(Energia disponivel) {
        List<Energia> cabem = new ArrayList<>();
        for (Energia energia : Energia.values()) {
            if (energia.cabeEm(disponivel)) {
                cabem.add(energia);
            }
        }
        return cabem;
    }

    /**
     * Projeto ativo sem nenhuma acao aberta e o erro classico do GTD: parece vivo na lista, mas nada
     * o move. A revisao semanal usa isso.
     */
    /**
     * Tarefas e concluidas por projeto, arquivados fora. Soma no banco com um group by; a conta de
     * percentual fica em {@link ProgressoProjeto}.
     */
    public List<ProgressoProjeto> progressoPorProjeto() {
        // Sem flush: so o GET da tela chama isto, fora de transacao e sem escrita pendente na sessao.
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = getEntityManager()
                .createNativeQuery(
                        """
                        select p.id, p.titulo, p.resultado_desejado, p.status,
                               count(a.id) filter (where a.estado <> 'DESCARTADA'),
                               count(a.id) filter (where a.estado = 'CONCLUIDA')
                          from projeto p
                          left join acao a on a.projeto_id = p.id
                         where p.status <> 'ARQUIVADO'
                         group by p.id
                         order by case p.status when 'ATIVO' then 0 when 'PAUSADO' then 1 else 2 end,
                                  p.titulo
                        """)
                .getResultList();
        return linhas.stream()
                .map(l -> ProgressoProjeto.de(
                        ((Number) l[0]).longValue(),
                        (String) l[1],
                        (String) l[2],
                        StatusProjeto.valueOf((String) l[3]),
                        ((Number) l[4]).intValue(),
                        ((Number) l[5]).intValue()))
                .toList();
    }

    public List<Projeto> projetosAtivosSemProximaAcao() {
        return getEntityManager()
                .createQuery(
                        """
                        select p from Projeto p
                         where p.status = br.dev.registro.atividades.domain.StatusProjeto.ATIVO
                           and not exists (
                               select 1 from Acao a
                                where a.projeto = p
                                  and a.estado in (br.dev.registro.gtd.domain.EstadoAcao.PROXIMA,
                                                   br.dev.registro.gtd.domain.EstadoAcao.AGENDA,
                                                   br.dev.registro.gtd.domain.EstadoAcao.AGUARDANDO))
                         order by p.criadoEm
                        """,
                        Projeto.class)
                .getResultList();
    }

    public long contarConcluidas() {
        return count("estado", EstadoAcao.CONCLUIDA);
    }
}
