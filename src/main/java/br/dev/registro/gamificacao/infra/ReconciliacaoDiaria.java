package br.dev.registro.gamificacao.infra;

import br.dev.registro.comum.Relogio;
import br.dev.registro.gamificacao.domain.DesafioPeriodicoService;
import br.dev.registro.gamificacao.domain.RecalculoDiaService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.LocalDate;

/**
 * O recalculo por evento so toca os dias alterados, mas a baseline de um dia depende dos 28
 * anteriores — e um dia sem nenhum registro nem chega a gerar evento, apesar de precisar existir
 * como zero para nao inflar a media movel. Este job fecha as duas lacunas de madrugada.
 */
@ApplicationScoped
public class ReconciliacaoDiaria {

    private static final Logger LOG = Logger.getLogger(ReconciliacaoDiaria.class);
    private static final int DIAS = 35;

    private final RecalculoDiaService recalculo;
    private final DesafioPeriodicoService desafios;
    private final Relogio relogio;

    public ReconciliacaoDiaria(
            RecalculoDiaService recalculo, DesafioPeriodicoService desafios, Relogio relogio) {
        this.recalculo = recalculo;
        this.desafios = desafios;
        this.relogio = relogio;
    }

    @Scheduled(cron = "0 10 3 * * ?", timeZone = "America/Sao_Paulo", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void reconciliar() {
        LocalDate hoje = relogio.hoje();
        int dias = recalculo.recalcularIntervalo(hoje.minusDays(DIAS), hoje);
        // Depois do recalculo: o desafio de ontem precisa ser apurado contra o dia ja fechado.
        desafios.sincronizar(hoje);
        LOG.infof("Reconciliacao diaria: %d dias recalculados ate %s", dias, hoje);
    }
}
