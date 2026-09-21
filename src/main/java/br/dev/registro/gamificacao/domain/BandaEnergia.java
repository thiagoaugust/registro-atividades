package br.dev.registro.gamificacao.domain;

/**
 * Como o dia comecou. Existe para nao comparar um dia de energia 1 com um dia de energia 5: a mesma
 * quantidade de XP significa coisas diferentes nos dois, e uma meta unica faria o dia ruim parecer
 * fracasso e o dia bom parecer suficiente.
 *
 * <p>Tres faixas, nao cinco: com cinco, cada grupo fica com poucos dias de historico e a mediana
 * vira ruido.
 */
public enum BandaEnergia {
    BAIXA(1, 2, "dia de energia baixa"),
    NORMAL(3, 3, "dia de energia normal"),
    ALTA(4, 5, "dia de energia alta");

    private final int minimo;
    private final int maximo;
    private final String rotulo;

    BandaEnergia(int minimo, int maximo, String rotulo) {
        this.minimo = minimo;
        this.maximo = maximo;
        this.rotulo = rotulo;
    }

    /** Null quando a energia nao foi informada — ai a faixa e a geral, sem recorte. */
    public static BandaEnergia de(Integer energia) {
        if (energia == null) {
            return null;
        }
        for (BandaEnergia banda : values()) {
            if (energia >= banda.minimo && energia <= banda.maximo) {
                return banda;
            }
        }
        return null;
    }

    public int minimo() {
        return minimo;
    }

    public int maximo() {
        return maximo;
    }

    public String rotulo() {
        return rotulo;
    }
}
