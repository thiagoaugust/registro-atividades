package br.dev.registro.gamificacao.domain;

/**
 * Classificacao do dia (docs/PLANO.md secao 3.4). Cruza o objetivo (XP contra a janela movel de 28
 * dias) com o contexto subjetivo do check-in, de modo que um dia mediano sob condicoes ruins suba de
 * classificacao em vez de descer: esforco e o que conta, nao volume bruto.
 *
 * <p>Funcao pura — sem CDI, sem banco, sem relogio.
 */
public final class ClassificadorDia {

    private ClassificadorDia() {
    }

    /** Janela movel dos dias anteriores, ja sem os dias de descanso. */
    public record Baseline(double media, double desvioPadrao, int dias) {

        public static final Baseline VAZIA = new Baseline(0, 0, 0);
    }

    /** O check-in do dia. Cada item e opcional; os ausentes saem do calculo. */
    public record ContextoDia(
            Integer dificuldade, Integer energia, Integer qualidadeSono, Integer humor, Integer estresse) {

        public static final ContextoDia AUSENTE = new ContextoDia(null, null, null, null, null);

        public boolean vazio() {
            return dificuldade == null && energia == null && qualidadeSono == null
                    && humor == null && estresse == null;
        }
    }

    public record ResultadoDia(
            double indice,
            Classificacao classificacao,
            boolean diaDificilVencido,
            boolean baselineInsuficiente,
            double objetivo,
            double adversidade) {
    }

    public static ResultadoDia classificar(
            int xpDia, Baseline baseline, ContextoDia contexto, boolean descanso, ParametrosIndice p) {

        double adversidade = adversidade(contexto, p);
        boolean semBaseline = baseline.dias() < p.diasMinimosBaseline();

        // Dia de descanso planejado nao e um dia fraco: e um dia que nao entra na conta. Indice
        // neutro e classificacao fixa evitam que ferias virem uma sequencia de dias DIFICIL.
        if (descanso) {
            return new ResultadoDia(50, Classificacao.NORMAL, false, semBaseline, 50, adversidade);
        }

        double objetivo = objetivo(xpDia, baseline, p, semBaseline);
        double indice = limitar(objetivo * (1 + p.k() * adversidade));

        boolean vencido = !contexto.vazio()
                && adversidade >= p.adversidadeVitoria()
                && objetivo >= p.objetivoVitoria();

        return new ResultadoDia(
                arredondar(indice), p.classificar(indice), vencido, semBaseline,
                arredondar(objetivo), arredondar(adversidade * 100) / 100.0);
    }

    /**
     * Sem historico suficiente nao ha z-score honesto: o dia fica no meio da escala em vez de ganhar
     * um numero inventado a partir de dois ou tres dias.
     */
    private static double objetivo(int xpDia, Baseline baseline, ParametrosIndice p, boolean semBaseline) {
        if (semBaseline || baseline.desvioPadrao() < 1) {
            return 50;
        }
        double z = (xpDia - baseline.media()) / baseline.desvioPadrao();
        return limitar(50 + p.escalaZ() * z);
    }

    /**
     * Media ponderada dos itens preenchidos, renormalizada: um check-in parcial nao e tratado como se
     * os campos em branco fossem otimos.
     */
    private static double adversidade(ContextoDia c, ParametrosIndice p) {
        double soma = 0;
        double pesos = 0;

        if (c.dificuldade() != null) {
            soma += p.pesoDificuldade() * crescente(c.dificuldade());
            pesos += p.pesoDificuldade();
        }
        if (c.energia() != null) {
            soma += p.pesoEnergia() * decrescente(c.energia());
            pesos += p.pesoEnergia();
        }
        if (c.qualidadeSono() != null) {
            soma += p.pesoSono() * decrescente(c.qualidadeSono());
            pesos += p.pesoSono();
        }
        if (c.humor() != null) {
            soma += p.pesoHumor() * decrescente(c.humor());
            pesos += p.pesoHumor();
        }
        if (c.estresse() != null) {
            soma += p.pesoEstresse() * crescente(c.estresse());
            pesos += p.pesoEstresse();
        }
        return pesos == 0 ? 0 : soma / pesos;
    }

    /** 1..5 onde 5 e o pior (dificuldade, estresse) -> 0..1. */
    private static double crescente(int valor) {
        return (valor - 1) / 4.0;
    }

    /** 1..5 onde 1 e o pior (energia, sono, humor) -> 0..1. */
    private static double decrescente(int valor) {
        return (5 - valor) / 4.0;
    }

    private static double limitar(double valor) {
        return Math.max(0, Math.min(100, valor));
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100) / 100.0;
    }
}
