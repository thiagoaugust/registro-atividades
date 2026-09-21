package br.dev.registro.analytics.domain;

import br.dev.registro.analytics.infra.AnalyticsRepository;
import br.dev.registro.analytics.infra.AnalyticsRepository.CategoriaNoPeriodo;
import br.dev.registro.analytics.infra.AnalyticsRepository.Correlacao;
import br.dev.registro.analytics.infra.AnalyticsRepository.MetricasGtd;
import br.dev.registro.analytics.infra.AnalyticsRepository.MetricasLeitura;
import br.dev.registro.analytics.infra.AnalyticsRepository.MetricasTreino;
import br.dev.registro.analytics.infra.AnalyticsRepository.PontoDispersao;
import br.dev.registro.analytics.infra.AnalyticsRepository.PontoSerie;
import br.dev.registro.analytics.infra.AnalyticsRepository.ProdutividadePorDiaSemana;
import br.dev.registro.analytics.infra.AnalyticsRepository.ProgressoDesafio;
import br.dev.registro.analytics.infra.AnalyticsRepository.TemaEstudado;
import br.dev.registro.analytics.infra.AnalyticsRepository.TotaisPeriodo;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.comum.Relogio;
import br.dev.registro.gamificacao.domain.StreakCalculator;
import br.dev.registro.gamificacao.infra.DiaResumoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Orquestra as agregacoes e monta a comparacao com o periodo anterior. */
@ApplicationScoped
public class AnalyticsService {

    private final AnalyticsRepository repositorio;
    private final DiaResumoRepository resumos;
    private final Relogio relogio;

    public AnalyticsService(
            AnalyticsRepository repositorio, DiaResumoRepository resumos, Relogio relogio) {
        this.repositorio = repositorio;
        this.resumos = resumos;
        this.relogio = relogio;
    }

    public record Periodo(LocalDate de, LocalDate ate) {

        long dias() {
            return ChronoUnit.DAYS.between(de, ate) + 1;
        }

        /** O periodo imediatamente anterior, do mesmo tamanho. */
        Periodo anterior() {
            return new Periodo(de.minusDays(dias()), de.minusDays(1));
        }
    }

    public record ComparacaoPeriodo(
            Periodo periodo,
            Periodo periodoAnterior,
            Granularidade granularidade,
            Variacao xp,
            Variacao minutos,
            Variacao indiceMedio,
            Variacao diasComPresenca,
            Variacao diasDificeisVencidos,
            Variacao registros,
            List<PontoSerie> serie,
            List<CategoriaNoPeriodo> porCategoria) {
    }

    public record PorCategoria(
            MetricasTreino treino,
            MetricasLeitura leitura,
            List<TemaEstudado> temas,
            List<ProgressoDesafio> desafios) {
    }

    public record Correlacoes(
            List<Correlacao> coeficientes,
            List<ProdutividadePorDiaSemana> porDiaDaSemana,
            List<PontoDispersao> sonoVersusIndice,
            List<PontoDispersao> energiaVersusXp) {
    }

    public ComparacaoPeriodo periodo(Granularidade granularidade, LocalDate de, LocalDate ate) {
        Periodo atual = normalizar(de, ate);
        Periodo anterior = atual.anterior();

        TotaisPeriodo totaisAtual = repositorio.totais(atual.de(), atual.ate());
        TotaisPeriodo totaisAnterior = repositorio.totais(anterior.de(), anterior.ate());

        return new ComparacaoPeriodo(
                atual,
                anterior,
                granularidade,
                Variacao.de(totaisAtual.xp(), totaisAnterior.xp()),
                Variacao.de(totaisAtual.minutos(), totaisAnterior.minutos()),
                Variacao.de(totaisAtual.indiceMedio(), totaisAnterior.indiceMedio()),
                Variacao.de(totaisAtual.diasComPresenca(), totaisAnterior.diasComPresenca()),
                Variacao.de(totaisAtual.diasDificeisVencidos(), totaisAnterior.diasDificeisVencidos()),
                Variacao.de(totaisAtual.registros(), totaisAnterior.registros()),
                repositorio.serie(granularidade, atual.de(), atual.ate()),
                repositorio.porCategoria(granularidade, atual.de(), atual.ate()));
    }

    public List<AnalyticsRepository.DiaHeatmap> heatmap(Integer ano) {
        return repositorio.heatmap(ano != null ? ano : relogio.hoje().getYear());
    }

    public PorCategoria porCategoria(LocalDate de, LocalDate ate) {
        Periodo periodo = normalizar(de, ate);
        return new PorCategoria(
                repositorio.metricasTreino(periodo.de(), periodo.ate()),
                repositorio.metricasLeitura(periodo.de(), periodo.ate()),
                repositorio.temasEstudados(periodo.de(), periodo.ate()),
                repositorio.progressoDesafios());
    }

    /**
     * As correlacoes que interessam: o sono e a energia explicam o dia? Escolhidas de proposito — sair
     * cruzando tudo com tudo produziria coincidencia com cara de descoberta.
     */
    public Correlacoes correlacoes(LocalDate de, LocalDate ate) {
        Periodo periodo = normalizar(de, ate);
        LocalDate inicio = periodo.de();
        LocalDate fim = periodo.ate();

        return new Correlacoes(
                List.of(
                        repositorio.correlacao(Eixo.HORAS_SONO, Eixo.INDICE, inicio, fim),
                        repositorio.correlacao(Eixo.QUALIDADE_SONO, Eixo.INDICE, inicio, fim),
                        repositorio.correlacao(Eixo.ENERGIA, Eixo.XP, inicio, fim),
                        repositorio.correlacao(Eixo.HUMOR, Eixo.INDICE, inicio, fim),
                        repositorio.correlacao(Eixo.ESTRESSE, Eixo.INDICE, inicio, fim),
                        repositorio.correlacao(Eixo.DIFICULDADE, Eixo.XP, inicio, fim)),
                repositorio.porDiaDaSemana(inicio, fim),
                repositorio.dispersao(Eixo.HORAS_SONO, Eixo.INDICE, inicio, fim),
                repositorio.dispersao(Eixo.ENERGIA, Eixo.XP, inicio, fim));
    }

    public MetricasGtd gtd(LocalDate de, LocalDate ate) {
        Periodo periodo = normalizar(de, ate);
        return repositorio.metricasGtd(periodo.de(), periodo.ate());
    }

    public record Retrospectiva(
            int ano,
            TotaisPeriodo totais,
            TotaisPeriodo anoAnterior,
            Variacao xp,
            Variacao minutos,
            Variacao diasComPresenca,
            int dificeis,
            int normais,
            int bons,
            int excelentes,
            int maiorSequencia,
            AnalyticsRepository.Destaque melhorDia,
            List<PontoSerie> porMes,
            PorCategoria categorias,
            List<AnalyticsRepository.ConquistaNoAno> conquistas,
            int revisoesConcluidas,
            AnalyticsRepository.MetricasGtd gtd) {
    }

    /** Resumo do ano inteiro: o que aconteceu, contra o ano anterior. */
    public Retrospectiva retrospectiva(int ano) {
        LocalDate inicio = LocalDate.of(ano, 1, 1);
        LocalDate fim = LocalDate.of(ano, 12, 31);
        LocalDate inicioAnterior = inicio.minusYears(1);
        LocalDate fimAnterior = fim.minusYears(1);

        TotaisPeriodo totais = repositorio.totais(inicio, fim);
        TotaisPeriodo anterior = repositorio.totais(inicioAnterior, fimAnterior);
        List<PontoSerie> porMes = repositorio.serie(Granularidade.MES, inicio, fim);

        int dificeis = 0;
        int normais = 0;
        int bons = 0;
        int excelentes = 0;
        for (PontoSerie mes : porMes) {
            dificeis += mes.dificeis();
            normais += mes.normais();
            bons += mes.bons();
            excelentes += mes.excelentes();
        }

        return new Retrospectiva(
                ano,
                totais,
                anterior,
                Variacao.de(totais.xp(), anterior.xp()),
                Variacao.de(totais.minutos(), anterior.minutos()),
                Variacao.de(totais.diasComPresenca(), anterior.diasComPresenca()),
                dificeis,
                normais,
                bons,
                excelentes,
                StreakCalculator.maiorSequencia(resumos.presencaRecente(fim, 366)),
                repositorio.melhorDia(inicio, fim),
                porMes,
                porCategoria(inicio, fim),
                repositorio.conquistasDoPeriodo(inicio, fim),
                repositorio.revisoesConcluidasNoPeriodo(inicio, fim),
                repositorio.metricasGtd(inicio, fim));
    }

    /** Sem periodo informado, os ultimos 30 dias. */
    private Periodo normalizar(LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : relogio.hoje();
        LocalDate inicio = de != null ? de : fim.minusDays(29);
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("o fim do periodo e anterior ao inicio");
        }
        return new Periodo(inicio, fim);
    }
}
