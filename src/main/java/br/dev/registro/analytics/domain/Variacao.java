package br.dev.registro.analytics.domain;

/**
 * Comparacao de um numero com o mesmo numero do periodo anterior.
 *
 * @param percentual variacao relativa; nulo quando nao ha base de comparacao — sem periodo anterior
 *                   nao existe "+100%", existe "nao da para comparar"
 */
public record Variacao(double atual, double anterior, Double percentual) {

    public static Variacao de(double atual, double anterior) {
        Double percentual = anterior == 0 ? null : (atual - anterior) / Math.abs(anterior) * 100;
        return new Variacao(atual, anterior, percentual == null ? null : arredondar(percentual));
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10) / 10.0;
    }
}
