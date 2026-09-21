package br.dev.registro.gamificacao.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * Conta dias consecutivos a partir de uma data, andando para tras. Funcao pura sobre a lista que o
 * repositorio traz (no maximo 366 linhas) — um CTE recursivo aqui seria ilegivel e mais dificil de
 * testar do que a regra merece.
 *
 * <p>Regras: um dia de descanso planejado preserva a streak sem incrementa-la; um dia sem presenca
 * nem descanso quebra; um buraco no calendario tambem quebra.
 */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * Streak corrente. O dia de hoje ainda vazio nao zera a contagem — as 8h da manha ninguem perdeu
     * a sequencia ainda; ela so quebra quando o dia fecha sem nada.
     */
    public static int corrente(List<DiaPresenca> dias, LocalDate hoje) {
        int comHoje = contar(dias, hoje);
        return comHoje > 0 ? comHoje : contar(dias, hoje.minusDays(1));
    }

    /**
     * Maior sequencia de presenca da lista, em qualquer ponto dela — o recorde do periodo, nao a
     * streak corrente. Descanso planejado preserva a sequencia sem incrementa-la.
     */
    public static int maiorSequencia(List<DiaPresenca> dias) {
        int maior = 0;
        int atual = 0;
        LocalDate anterior = null;

        // A lista vem do mais recente para o mais antigo; percorrer ao contrario da a ordem do tempo.
        for (int i = dias.size() - 1; i >= 0; i--) {
            DiaPresenca dia = dias.get(i);
            boolean emSequencia = anterior == null || dia.data().equals(anterior.plusDays(1));
            if (!emSequencia) {
                atual = 0;
            }
            if (dia.presenca()) {
                atual++;
                maior = Math.max(maior, atual);
            } else if (!dia.descanso()) {
                atual = 0;
            }
            anterior = dia.data();
        }
        return maior;
    }

    /**
     * @param dias dias do mais recente para o mais antigo, terminando em {@code ate}
     */
    public static int contar(List<DiaPresenca> dias, LocalDate ate) {
        int streak = 0;
        LocalDate esperado = ate;

        for (DiaPresenca dia : dias) {
            if (dia.data().isAfter(esperado)) {
                continue;
            }
            if (!dia.data().equals(esperado)) {
                break; // buraco no calendario
            }
            if (dia.presenca()) {
                streak++;
            } else if (!dia.descanso()) {
                break;
            }
            esperado = esperado.minusDays(1);
        }
        return streak;
    }
}
