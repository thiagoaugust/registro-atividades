package br.dev.registro.gamificacao.domain;

import br.dev.registro.atividades.domain.Categoria;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Calculo de XP (docs/PLANO.md secoes 3.1 a 3.3). Funcao pura: sem CDI, sem banco. */
public final class CalculadoraXp {

    private CalculadoraXp() {
    }

    /** Um lancamento do ledger, ja desacoplado da entidade. */
    public record Lancamento(OrigemXp origem, Categoria categoria, int pontosBrutos) {
    }

    public record ResumoXp(
            int xpTotal,
            int xpBase,
            int bonusEquilibrio,
            int xpGtd,
            int categoriasDistintas,
            Map<Categoria, Integer> porCategoria) {
    }

    /** duracao x fator de esforco x multiplicador da categoria. */
    public static int pontosBrutos(int duracaoMin, int esforco, Categoria categoria, ParametrosXp p) {
        return (int) Math.round(duracaoMin * p.fatorEsforco(esforco) * p.multiplicador(categoria));
    }

    /**
     * Consolida o dia: aplica o teto por categoria, o teto do GTD e o bonus de equilibrio. O teto e
     * aplicado na consolidacao, e nao no lancamento, porque so aqui se sabe o total do dia.
     */
    public static ResumoXp consolidar(List<Lancamento> lancamentos, ParametrosXp p) {
        Map<Categoria, Integer> brutoPorCategoria = new EnumMap<>(Categoria.class);
        int brutoGtd = 0;
        // Revisao e desafio cumprido ficam fora do teto do GTD: nao sao tarefas avulsas, e limita-los
        // junto faria uma semana cheia de acoes engolir o XP da revisao.
        int xpBonus = 0;

        for (Lancamento l : lancamentos) {
            switch (l.origem()) {
                case REGISTRO -> brutoPorCategoria.merge(l.categoria(), l.pontosBrutos(), Integer::sum);
                case ACAO_GTD -> brutoGtd += l.pontosBrutos();
                case REVISAO_SEMANAL, DESAFIO_PERIODICO -> xpBonus += l.pontosBrutos();
            }
        }

        Map<Categoria, Integer> porCategoria = new EnumMap<>(Categoria.class);
        int xpBase = 0;
        for (Map.Entry<Categoria, Integer> entrada : brutoPorCategoria.entrySet()) {
            int comTeto = Math.min(entrada.getValue(), p.tetoDiarioCategoria());
            porCategoria.put(entrada.getKey(), comTeto);
            xpBase += comTeto;
        }

        int categorias = porCategoria.size();
        int bonus = categorias >= p.categoriasParaEquilibrio()
                ? (int) Math.round(xpBase * p.bonusEquilibrio())
                : 0;
        int xpGtd = Math.min(brutoGtd, p.tetoGtdDiario());

        return new ResumoXp(xpBase + bonus + xpGtd + xpBonus, xpBase, bonus, xpGtd + xpBonus,
                categorias, porCategoria);
    }

    /**
     * Curva progressiva: o nivel n exige 100 x (n-1)^1,5 de XP acumulado — nivel 1 no comeco, 2 aos
     * 100, 5 aos 800, 10 aos 2.700, 20 aos 8.285.
     */
    public static int nivel(long xpAcumulado) {
        if (xpAcumulado < 100) {
            return 1;
        }
        // O epsilon existe porque Math.pow(8, 2/3.0) devolve 3,9999999999999996: sem ele, os XP que
        // caem exatamente no limiar do nivel ficariam um nivel abaixo.
        return (int) Math.floor(Math.pow(xpAcumulado / 100.0, 2.0 / 3.0) + 1e-9) + 1;
    }

    /**
     * Menor XP acumulado que abre o nivel informado. Teto, nao arredondamento: 100 x 5^1,5 = 1.118,03,
     * e 1.118 ainda e nivel 5.
     */
    public static long xpParaNivel(int nivel) {
        return nivel <= 1 ? 0 : (long) Math.ceil(100 * Math.pow(nivel - 1, 1.5));
    }
}
