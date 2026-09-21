package br.dev.registro.gamificacao.domain;

import br.dev.registro.atividades.domain.Categoria;

import java.util.Map;

/**
 * Parametros do calculo de XP. Record puro: a configuracao (@ConfigMapping) monta um destes e as
 * funcoes de calculo nao conhecem Quarkus.
 *
 * @param esforcoBase        termo fixo do fator de esforco
 * @param esforcoIncremento  quanto cada ponto de esforco soma ao fator
 * @param multiplicadores    peso por categoria
 * @param tetoDiarioCategoria teto de XP por categoria por dia — e o que impede 4h da mesma coisa
 *                            valerem mais que um dia equilibrado
 * @param tetoGtdDiario      teto de XP vindo de acoes GTD no dia
 * @param bonusEquilibrio    fracao do XP base somada quando o dia tem categorias suficientes
 */
public record ParametrosXp(
        double esforcoBase,
        double esforcoIncremento,
        Map<Categoria, Double> multiplicadores,
        int tetoDiarioCategoria,
        int tetoGtdDiario,
        double bonusEquilibrio,
        int categoriasParaEquilibrio) {

    public static ParametrosXp padrao() {
        return new ParametrosXp(
                0.6,
                0.08,
                Map.of(
                        Categoria.TREINO, 1.0,
                        Categoria.ESTUDO, 1.0,
                        Categoria.LEITURA, 0.8,
                        Categoria.DESAFIO, 1.2,
                        Categoria.PROJETO, 1.1),
                120,
                30,
                0.15,
                3);
    }

    public double multiplicador(Categoria categoria) {
        return multiplicadores().getOrDefault(categoria, 1.0);
    }

    /** esforco 1 -> 0,68 | 5 -> 1,00 | 10 -> 1,40. */
    public double fatorEsforco(int esforco) {
        return esforcoBase() + esforcoIncremento() * esforco;
    }
}
