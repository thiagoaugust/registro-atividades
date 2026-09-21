package br.dev.registro.gamificacao.domain;

import br.dev.registro.comum.Relogio;
import br.dev.registro.gamificacao.infra.DesafioInstanciaRepository;
import br.dev.registro.gamificacao.infra.DesafioPeriodicoRepository;
import br.dev.registro.gamificacao.infra.GamificacaoConfig;
import br.dev.registro.gamificacao.infra.MetricasPeriodoRepository;
import br.dev.registro.gamificacao.infra.XpLancamentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Os tres horizontes: o dia, a semana e o mes.
 *
 * <p>Nenhum alvo e digitado. Quando um periodo abre, o alvo e calibrado sobre os periodos anteriores
 * do mesmo desafio — e congela ali. Mudar a regua no meio da semana invalidaria o esforco ja feito;
 * deixar a regua fixa para sempre transformaria em rotina o que era desafio.
 */
@ApplicationScoped
public class DesafioPeriodicoService {

    /** Quantos periodos anteriores entram na calibragem, por horizonte. */
    private static final Map<EscopoDesafio, Integer> HISTORICO = new EnumMap<>(Map.of(
            EscopoDesafio.DIARIO, 21,
            EscopoDesafio.SEMANAL, 8,
            EscopoDesafio.MENSAL, 6));

    /** Recorde olha bem mais para tras: "mais que qualquer mes anterior" nao sao seis meses. */
    private static final int PERIODOS_RECORDE = 24;

    private static final int LIMITE_HISTORICO = 60;

    private final DesafioPeriodicoRepository desafios;
    private final DesafioInstanciaRepository instancias;
    private final MetricasPeriodoRepository metricas;
    private final XpLancamentoRepository lancamentos;
    private final RecalculoDiaService recalculo;
    private final GamificacaoConfig config;
    private final Relogio relogio;

    public DesafioPeriodicoService(
            DesafioPeriodicoRepository desafios,
            DesafioInstanciaRepository instancias,
            MetricasPeriodoRepository metricas,
            XpLancamentoRepository lancamentos,
            RecalculoDiaService recalculo,
            GamificacaoConfig config,
            Relogio relogio) {
        this.desafios = desafios;
        this.instancias = instancias;
        this.metricas = metricas;
        this.lancamentos = lancamentos;
        this.recalculo = recalculo;
        this.config = config;
        this.relogio = relogio;
    }

    public record DesafioDto(
            long id,
            EscopoDesafio escopo,
            String titulo,
            String descricao,
            String unidade,
            TipoDesafio tipo,
            LocalDate periodoInicio,
            LocalDate periodoFim,
            double alvo,
            double progresso,
            double fracao,
            StatusDesafio status,
            int xp) {
    }

    /** Os tres horizontes lado a lado, mais o que ja fechou. */
    public record PainelDto(
            List<DesafioDto> diarios,
            List<DesafioDto> semanais,
            List<DesafioDto> mensais,
            List<DesafioDto> historico,
            int trofeusDoAno) {
    }

    /**
     * Ponto unico de sincronizacao: abre os periodos que faltam, fecha os que venceram e reapura os
     * abertos. Escrever numa leitura nao e elegante, mas a alternativa — reapurar treze metricas a
     * cada registro salvo — custaria muito mais num app de uma pessoa so.
     */
    @Transactional
    public PainelDto painel() {
        LocalDate hoje = relogio.hoje();
        sincronizar(hoje);

        Map<EscopoDesafio, List<DesafioDto>> porEscopo = new EnumMap<>(EscopoDesafio.class);
        for (EscopoDesafio escopo : EscopoDesafio.values()) {
            porEscopo.put(escopo, new ArrayList<>());
        }
        for (DesafioInstancia instancia : instancias.abertasEm(hoje)) {
            porEscopo.get(instancia.desafio.escopo).add(paraDto(instancia));
        }

        List<DesafioDto> historico =
                instancias.concluidas(hoje, LIMITE_HISTORICO).stream().map(this::paraDto).toList();
        long trofeus = historico.stream()
                .filter(d -> d.escopo() == EscopoDesafio.MENSAL
                        && d.status() == StatusDesafio.CUMPRIDO
                        && d.periodoInicio().getYear() == hoje.getYear())
                .count();

        return new PainelDto(
                porEscopo.get(EscopoDesafio.DIARIO),
                porEscopo.get(EscopoDesafio.SEMANAL),
                porEscopo.get(EscopoDesafio.MENSAL),
                historico,
                (int) trofeus);
    }

    @Transactional
    public void sincronizar(LocalDate hoje) {
        abrirPeriodos(hoje);
        // Fecha antes de reapurar: uma instancia vencida nao deve ganhar progresso de hoje.
        fecharVencidas(hoje);
        reapurarAbertas(hoje);
    }

    private void abrirPeriodos(LocalDate hoje) {
        for (DesafioPeriodico desafio : desafios.ativos()) {
            LocalDate inicio = desafio.escopo.inicio(hoje);
            if (instancias.existe(desafio.id, inicio)) {
                continue;
            }
            DesafioInstancia instancia = new DesafioInstancia();
            instancia.desafio = desafio;
            instancia.periodoInicio = inicio;
            instancia.periodoFim = desafio.escopo.fim(hoje);
            instancia.alvo = BigDecimal.valueOf(calibrar(desafio, inicio));
            instancias.persist(instancia);
        }
    }

    /** O alvo do periodo, tirado dos periodos anteriores do mesmo desafio. */
    private double calibrar(DesafioPeriodico desafio, LocalDate inicio) {
        if (desafio.alvo != null) {
            return desafio.alvo.doubleValue();
        }
        double minimo = desafio.parametro("minimo", 1);
        boolean recorde = desafio.tipo == TipoDesafio.RECORDE;
        int quantos = recorde ? PERIODOS_RECORDE : HISTORICO.get(desafio.escopo);

        List<Double> historico = new ArrayList<>();
        for (int i = 1; i <= quantos; i++) {
            LocalDate anterior = desafio.escopo.anterior(inicio, i);
            historico.add(metricas.medir(
                    desafio.metrica, anterior, desafio.escopo.fim(anterior), desafio.categoria()));
        }

        return recorde
                ? CalibradorDeAlvo.recorde(historico, minimo)
                : CalibradorDeAlvo.calibrar(historico, desafio.parametro("fator", 1), minimo);
    }

    private void fecharVencidas(LocalDate hoje) {
        for (DesafioInstancia instancia : instancias.vencidasSemFechar(hoje)) {
            // Uma ultima apuracao: o registro de domingo a noite pode ter sido lancado na segunda.
            apurar(instancia, hoje);
            if (instancia.status == StatusDesafio.ABERTO) {
                instancia.status = StatusDesafio.PERDIDO;
            }
        }
    }

    private void reapurarAbertas(LocalDate hoje) {
        for (DesafioInstancia instancia : instancias.abertasEm(hoje)) {
            apurar(instancia, hoje);
        }
    }

    /**
     * Mede o periodo e, se o alvo foi batido, marca cumprido e lanca o XP. Cumprido nao volta atras:
     * a semana em que voce correu 30 km nao deixa de ter acontecido porque um registro foi corrigido.
     */
    private void apurar(DesafioInstancia instancia, LocalDate hoje) {
        DesafioPeriodico desafio = instancia.desafio;
        double medido = metricas.medir(
                desafio.metrica, instancia.periodoInicio, instancia.periodoFim, desafio.categoria());
        instancia.progresso = BigDecimal.valueOf(medido);

        if (instancia.status != StatusDesafio.ABERTO || medido < instancia.alvoNumerico()) {
            return;
        }
        instancia.status = StatusDesafio.CUMPRIDO;
        instancia.cumpridoEm = relogio.agora();

        // O XP cai no dia em que o desafio foi batido; fechando atrasado, no ultimo dia do periodo.
        LocalDate dia = hoje.isAfter(instancia.periodoFim) ? instancia.periodoFim : hoje;
        lancamentos.removerDaOrigem(OrigemXp.DESAFIO_PERIODICO, instancia.id);
        lancamentos.persist(XpLancamento.de(
                dia, OrigemXp.DESAFIO_PERIODICO, instancia.id, null, xpDe(desafio.escopo)));
        recalculo.recalcular(dia);
    }

    private int xpDe(EscopoDesafio escopo) {
        return switch (escopo) {
            case DIARIO -> config.xp().pontosDesafioDiario();
            case SEMANAL -> config.xp().pontosDesafioSemanal();
            case MENSAL -> config.xp().pontosDesafioMensal();
        };
    }

    private DesafioDto paraDto(DesafioInstancia i) {
        return new DesafioDto(
                i.id,
                i.desafio.escopo,
                i.desafio.titulo,
                i.desafio.descricao,
                i.desafio.metrica.unidade(),
                i.desafio.tipo,
                i.periodoInicio,
                i.periodoFim,
                i.alvoNumerico(),
                i.progressoNumerico(),
                i.fracao(),
                i.status,
                xpDe(i.desafio.escopo));
    }
}
