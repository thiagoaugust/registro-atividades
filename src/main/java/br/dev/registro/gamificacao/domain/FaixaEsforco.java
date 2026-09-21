package br.dev.registro.gamificacao.domain;

import java.util.List;

/**
 * Quanto e um dia justo, dado como este dia comecou.
 *
 * <p>Nao e uma meta escolhida: sai dos seus proprios dias de energia parecida. O piso e o quartil
 * inferior e o teto o superior, entao metade dos dias comparaveis cai dentro da faixa — e ficar
 * dentro dela e o resultado esperado, nao a excecao. Acima do teto e um dia que rendeu de verdade;
 * abaixo do piso e um dia que ficou devendo, mesmo que o numero absoluto pareca alto.
 *
 * <p>Funcao pura — recebe o historico ja lido, nao sabe de banco nem de relogio.
 */
public record FaixaEsforco(
        BandaEnergia banda, int piso, int tipico, int teto, int diasConsiderados, boolean baseCurta) {

    /** Abaixo disso a mediana e chute: tres dias nao descrevem um habito. */
    public static final int MINIMO_DIAS = 5;

    public enum Situacao {
        /** Ainda sem historico comparavel suficiente para cobrar qualquer coisa. */
        CALIBRANDO,
        ABAIXO,
        DENTRO,
        ACIMA
    }

    public static FaixaEsforco de(BandaEnergia banda, List<Integer> xpDeDiasComparaveis) {
        List<Integer> valores = xpDeDiasComparaveis.stream().filter(v -> v != null).sorted().toList();
        if (valores.isEmpty()) {
            return new FaixaEsforco(banda, 0, 0, 0, 0, true);
        }
        return new FaixaEsforco(
                banda,
                percentil(valores, 0.25),
                percentil(valores, 0.50),
                percentil(valores, 0.75),
                valores.size(),
                valores.size() < MINIMO_DIAS);
    }

    public Situacao situacao(int xpDoDia) {
        if (baseCurta) {
            return Situacao.CALIBRANDO;
        }
        if (xpDoDia > teto) {
            return Situacao.ACIMA;
        }
        return xpDoDia >= piso ? Situacao.DENTRO : Situacao.ABAIXO;
    }

    /** Quanto falta para entrar na faixa. Zero quando ja esta dentro — nao ha divida. */
    public int faltaParaOPiso(int xpDoDia) {
        return Math.max(0, piso - xpDoDia);
    }

    /** Posto mais proximo, e nao interpolacao: o resultado e um XP que voce de fato ja fez num dia. */
    private static int percentil(List<Integer> ordenados, double fracao) {
        int indice = (int) Math.round(fracao * (ordenados.size() - 1));
        return ordenados.get(indice);
    }
}
