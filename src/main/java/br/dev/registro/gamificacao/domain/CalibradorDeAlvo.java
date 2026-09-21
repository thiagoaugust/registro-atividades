package br.dev.registro.gamificacao.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Define o alvo de um desafio a partir do que voce costuma fazer, e nao de um numero fixo escolhido
 * uma vez. Um desafio calibrado acompanha a evolucao: o que era dificil em marco vira o normal de
 * junho, e a meta sobe junto.
 *
 * <p>Usa a mediana, nao a media: uma unica maratona de estudo nao deve virar a expectativa de todo
 * dia, e um dia zerado nao deve derrubar a meta da semana.
 *
 * <p>Funcao pura — sem banco, sem relogio.
 */
public final class CalibradorDeAlvo {

    private CalibradorDeAlvo() {
    }

    /**
     * @param historico valores dos periodos anteriores, na ordem que vierem
     * @param fator     quanto acima da mediana o desafio pede (1.0 = o seu normal)
     * @param minimo    piso: impede que um historico fraco gere uma meta de zero
     */
    public static double calibrar(List<Double> historico, double fator, double minimo) {
        List<Double> valores = new ArrayList<>(historico.stream().filter(v -> v != null && v > 0).toList());
        if (valores.isEmpty()) {
            // Sem historico ainda: o piso e a unica referencia honesta.
            return Math.max(1, arredondar(minimo));
        }
        valores.sort(Double::compareTo);
        double alvo = mediana(valores) * fator;
        return Math.max(Math.max(1, minimo), arredondar(alvo));
    }

    private static double mediana(List<Double> ordenados) {
        int meio = ordenados.size() / 2;
        if (ordenados.size() % 2 == 1) {
            return ordenados.get(meio);
        }
        return (ordenados.get(meio - 1) + ordenados.get(meio)) / 2;
    }

    /**
     * Alvo de um desafio de recorde: superar o melhor periodo anterior. Sem historico, vale o piso —
     * assim o primeiro mes ainda tem como ser conquistado.
     */
    public static double recorde(List<Double> historico, double minimo) {
        double melhor = historico.stream().filter(v -> v != null).mapToDouble(Double::doubleValue).max().orElse(0);
        // Superar exige passar do melhor, nao empatar.
        return Math.max(Math.max(1, minimo), arredondar(melhor + 1));
    }

    /** Metas quebradas confundem: "faca 47,3 minutos" nao e um objetivo, e uma conta. */
    private static double arredondar(double valor) {
        if (valor >= 100) {
            return Math.round(valor / 10) * 10.0;
        }
        if (valor >= 20) {
            return Math.round(valor / 5) * 5.0;
        }
        return Math.round(valor);
    }
}
