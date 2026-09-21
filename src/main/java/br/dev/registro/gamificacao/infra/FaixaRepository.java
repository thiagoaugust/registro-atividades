package br.dev.registro.gamificacao.infra;

import br.dev.registro.gamificacao.domain.BandaEnergia;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.time.LocalDate;
import java.util.List;

/** O historico que a faixa de esforco compara: XP dos dias parecidos com o de hoje. */
@ApplicationScoped
public class FaixaRepository {

    /** Tres meses: recente o bastante para refletir a fase atual, longo o bastante para ter dias. */
    public static final int DIAS_JANELA = 90;

    private final EntityManager em;

    public FaixaRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * XP dos dias comparaveis anteriores a {@code ate}.
     *
     * <p>Exclui o proprio dia — o resultado de hoje nao pode entrar no calculo da barra de hoje. Exclui
     * descanso planejado e dias sem presenca: a faixa e "quanto rende um dia em que voce aparece", e
     * folga nao deve derrubar a regua de quinta-feira.
     *
     * <p>Banda nula (dia sem energia informada) le todos os dias, sem recorte.
     */
    @SuppressWarnings("unchecked")
    public List<Integer> xpDeDiasComparaveis(LocalDate ate, BandaEnergia banda) {
        String recorte = banda == null ? "" : " and c.energia between :minimo and :maximo";
        Query query = em.createNativeQuery(
                        """
                        select d.xp_total
                          from dia_resumo d
                          left join checkin_diario c on c.data_local = d.data_local
                         where d.data_local < :ate
                           and d.data_local >= :de
                           and d.presenca
                           and not d.descanso
                        """ + recorte)
                .setParameter("ate", ate)
                .setParameter("de", ate.minusDays(DIAS_JANELA));
        if (banda != null) {
            query.setParameter("minimo", banda.minimo()).setParameter("maximo", banda.maximo());
        }
        return ((List<Number>) query.getResultList()).stream().map(Number::intValue).toList();
    }
}
