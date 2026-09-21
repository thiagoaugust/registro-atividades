package br.dev.registro.gamificacao.domain;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.gamificacao.infra.ConquistaRepository;
import br.dev.registro.gamificacao.infra.DiaResumoRepository;
import br.dev.registro.gamificacao.infra.GamificacaoConfig;
import br.dev.registro.gamificacao.infra.MetricasRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Avalia o catalogo de conquistas depois de cada recalculo. As metricas sao calculadas sob demanda e
 * memorizadas por execucao: se todas as conquistas de quilometragem ja foram desbloqueadas, a soma
 * de km nem chega a ser consultada.
 */
@ApplicationScoped
public class AvaliadorConquistas {

    private static final Logger LOG = Logger.getLogger(AvaliadorConquistas.class);
    private static final int JANELA_STREAK = 400;

    private final ConquistaRepository conquistas;
    private final MetricasRepository metricas;
    private final DiaResumoRepository resumos;
    private final GamificacaoConfig config;
    private final br.dev.registro.gtd.domain.RevisaoSemanalService revisoes;

    public AvaliadorConquistas(
            ConquistaRepository conquistas,
            MetricasRepository metricas,
            DiaResumoRepository resumos,
            GamificacaoConfig config,
            br.dev.registro.gtd.domain.RevisaoSemanalService revisoes) {
        this.conquistas = conquistas;
        this.metricas = metricas;
        this.resumos = resumos;
        this.config = config;
        this.revisoes = revisoes;
    }

    /** @return codigos desbloqueados agora (lista vazia no caso comum) */
    @Transactional
    public List<String> avaliar(LocalDate dia) {
        Set<String> jaDesbloqueadas = conquistas.codigosDesbloqueados();
        Map<String, Double> cache = new HashMap<>();
        List<String> novas = new ArrayList<>();

        for (Conquista conquista : conquistas.ativas()) {
            if (jaDesbloqueadas.contains(conquista.codigo)) {
                continue;
            }
            if (progresso(conquista, dia, cache) >= conquista.alvo()) {
                ConquistaDesbloqueada desbloqueada = new ConquistaDesbloqueada();
                desbloqueada.conquistaCodigo = conquista.codigo;
                desbloqueada.dataLocal = dia;
                conquistas.desbloquear(desbloqueada);
                novas.add(conquista.codigo);
            }
        }
        return novas;
    }

    /**
     * Valor da metrica de uma conquista acumulado ate o dia informado — tambem usado para mostrar
     * "faltam X" no painel, onde o dia e hoje.
     */
    public double progresso(Conquista conquista, LocalDate ate, Map<String, Double> cache) {
        return switch (conquista.tipoRegra) {
            case TOTAL_METRICA -> metrica(conquista.parametroTexto("metrica"), ate, cache);
            case CONTAGEM_EVENTO -> metrica(conquista.parametroTexto("evento"), ate, cache);
            case STREAK -> streak(conquista.parametroTexto("escopo"), ate, cache);
        };
    }

    private double metrica(String nome, LocalDate ate, Map<String, Double> cache) {
        if (nome == null) {
            return 0;
        }
        Supplier<Double> calculo = switch (nome) {
            case "KM" -> () -> metricas.kmAcumulados(ate);
            case "PAGINAS" -> () -> metricas.paginasLidas(ate);
            case "MINUTOS_ESTUDO" -> () -> metricas.minutosEstudo(ate);
            case "REGISTRO" -> () -> metricas.totalRegistros(ate);
            case "LIVRO_CONCLUIDO" -> () -> metricas.livrosConcluidos(ate);
            case "LIVRO_TECNICO" -> () -> metricas.livrosTecnicos(ate);
            case "CATEGORIAS_LIDAS" -> () -> metricas.categoriasLidas(ate);
            case "CURSO_CONCLUIDO" -> () -> metricas.cursosConcluidos(ate);
            case "MINUTOS_PRATICA" -> () -> metricas.minutosPratica(ate);
            case "MINUTOS_APROVEITADOS" -> () -> metricas.minutosAproveitados(ate);
            case "DESAFIO_CONCLUIDO" -> () -> metricas.desafiosConcluidos(ate);
            case "PROJETO_CONCLUIDO" -> () -> metricas.projetosConcluidos(ate);
            case "ITEM_CAPTURADO" -> () -> metricas.itensCapturados(ate);
            case "ACAO_CONCLUIDA" -> () -> metricas.acoesConcluidas(ate);
            case "INBOX_ZERADO" -> metricas::inboxZerado;
            case "REVISAO_CONCLUIDA" -> () -> metricas.revisoesConcluidas(ate);
            case "REVISOES_SEGUIDAS" -> () -> (double) revisoes.semanasSeguidas(ate);
            case "DIA_EQUILIBRADO" -> () ->
                    (double) resumos.contarDiasEquilibrados(config.xp().categoriasParaEquilibrio(), ate);
            case "DIA_DIFICIL_VENCIDO" -> () -> (double) resumos.contarDiasDificeisVencidos(ate);
            default -> null;
        };

        if (calculo == null) {
            // Conquista configurada com uma metrica que este avaliador nao conhece (por exemplo, uma
            // do GTD antes da fase 3). Fica em zero em vez de derrubar o salvamento que a disparou.
            LOG.warnf("Metrica desconhecida em conquista: %s", nome);
            return 0;
        }
        return cache.computeIfAbsent(nome + "@" + ate, chave -> calculo.get());
    }

    private double streak(String escopo, LocalDate ate, Map<String, Double> cache) {
        if (escopo == null) {
            return 0;
        }
        Categoria categoria = categoriaOuNula(escopo);
        if (categoria == null && !"GERAL".equals(escopo)) {
            LOG.warnf("Escopo de streak desconhecido em conquista: %s", escopo);
            return 0;
        }
        // A streak e contada ate o dia avaliado, nao ate hoje: a conquista pertence ao dia em que a
        // sequencia fechou.
        return cache.computeIfAbsent("STREAK_" + escopo + "@" + ate, chave -> {
            List<DiaPresenca> dias = categoria == null
                    ? resumos.presencaRecente(ate, JANELA_STREAK)
                    : resumos.presencaRecente(categoria, ate, JANELA_STREAK);
            return (double) StreakCalculator.corrente(dias, ate);
        });
    }

    private static Categoria categoriaOuNula(String nome) {
        for (Categoria categoria : Categoria.values()) {
            if (categoria.name().equals(nome)) {
                return categoria;
            }
        }
        return null;
    }
}
