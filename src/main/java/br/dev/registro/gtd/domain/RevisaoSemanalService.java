package br.dev.registro.gtd.domain;

import br.dev.registro.comum.RecursoNaoEncontradoException;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.comum.Relogio;
import br.dev.registro.comum.RevisaoConcluida;
import br.dev.registro.gtd.infra.RevisaoSemanalRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/** O "refletir" do GTD: um checklist guiado, retomavel, uma vez por semana. */
@ApplicationScoped
public class RevisaoSemanalService {

    private final RevisaoSemanalRepository revisoes;
    private final Relogio relogio;
    private final Event<RevisaoConcluida> revisaoConcluida;

    public RevisaoSemanalService(
            RevisaoSemanalRepository revisoes, Relogio relogio, Event<RevisaoConcluida> revisaoConcluida) {
        this.revisoes = revisoes;
        this.relogio = relogio;
        this.revisaoConcluida = revisaoConcluida;
    }

    /** A semana e identificada pela segunda-feira que a abre. */
    public LocalDate semanaDe(LocalDate data) {
        return data.with(DayOfWeek.MONDAY);
    }

    public RevisaoSemanal porSemana(LocalDate semanaInicio) {
        RevisaoSemanal revisao = revisoes.findById(semanaInicio);
        if (revisao == null) {
            throw new RecursoNaoEncontradoException("Revisao da semana", semanaInicio);
        }
        return revisao;
    }

    public RevisaoSemanal atual() {
        return revisoes.findById(semanaDe(relogio.hoje()));
    }

    public List<RevisaoSemanal> recentes() {
        return revisoes.recentes(12);
    }

    /** Abre a revisao da semana ou devolve a que ja estava em andamento. */
    @Transactional
    public RevisaoSemanal iniciar(LocalDate data) {
        LocalDate semana = semanaDe(data != null ? data : relogio.hoje());
        if (semana.isAfter(relogio.hoje())) {
            throw new RegraNegocioException("nao da para revisar uma semana que ainda nao comecou");
        }
        RevisaoSemanal revisao = revisoes.findById(semana);
        if (revisao == null) {
            revisao = new RevisaoSemanal();
            revisao.semanaInicio = semana;
            revisao.iniciadaEm = relogio.agora();
            revisoes.persist(revisao);
        }
        return revisao;
    }

    @Transactional
    public RevisaoSemanal marcarPasso(LocalDate semanaInicio, PassoRevisao passo, boolean feito) {
        RevisaoSemanal revisao = porSemana(semanaInicio);
        revisao.passos.put(passo.name(), feito);
        // O Hibernate so detecta mudanca no JSONB se a referencia do mapa mudar.
        revisao.passos = new java.util.LinkedHashMap<>(revisao.passos);
        return revisao;
    }

    /**
     * Conclui a revisao. A duracao, quando nao informada, sai do tempo entre abrir e concluir — o
     * ponto de registrar isso e saber se a revisao esta virando um ritual de dez minutos.
     */
    @Transactional
    public RevisaoSemanal concluir(LocalDate semanaInicio, Integer duracaoMin) {
        RevisaoSemanal revisao = porSemana(semanaInicio);
        if (revisao.concluida()) {
            throw new RegraNegocioException("essa revisao ja foi concluida");
        }
        if (!revisao.todosOsPassosFeitos()) {
            throw new RegraNegocioException("ainda faltam passos do checklist");
        }

        revisao.concluidaEm = relogio.agora();
        revisao.duracaoMin = duracaoMin != null
                ? duracaoMin
                : (int) ChronoUnit.MINUTES.between(revisao.iniciadaEm, revisao.concluidaEm);

        revisaoConcluida.fire(new RevisaoConcluida(revisao.semanaInicio, relogio.diaLocal(revisao.concluidaEm)));
        return revisao;
    }

    /** Descrição dos passos, para a tela montar o checklist sem repetir os textos. */
    public List<Map<String, String>> checklist() {
        return java.util.Arrays.stream(PassoRevisao.values())
                .map(passo -> Map.of(
                        "passo", passo.name(),
                        "titulo", passo.titulo(),
                        "descricao", passo.descricao()))
                .toList();
    }

    /**
     * Semanas consecutivas com revisao concluida ate a data informada, terminando na semana dela ou
     * na anterior — a semana corrente ainda nao acabou, entao nao ter revisado nela nao quebra o
     * habito. A data importa: avaliar uma conquista no passado nao pode enxergar revisoes futuras.
     */
    public int semanasSeguidas(LocalDate ate) {
        List<RevisaoSemanal> concluidas = revisoes.concluidas().stream()
                .filter(r -> !r.semanaInicio.isAfter(semanaDe(ate)))
                .toList();
        if (concluidas.isEmpty()) {
            return 0;
        }
        LocalDate semanaAtual = semanaDe(ate);
        LocalDate esperada = concluidas.get(0).semanaInicio.equals(semanaAtual)
                ? semanaAtual
                : semanaAtual.minusWeeks(1);

        int seguidas = 0;
        for (RevisaoSemanal revisao : concluidas) {
            if (revisao.semanaInicio.isAfter(esperada)) {
                continue;
            }
            if (!revisao.semanaInicio.equals(esperada)) {
                break;
            }
            seguidas++;
            esperada = esperada.minusWeeks(1);
        }
        return seguidas;
    }
}
