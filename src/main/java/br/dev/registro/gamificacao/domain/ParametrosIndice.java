package br.dev.registro.gamificacao.domain;

/**
 * Parametros da classificacao do dia (docs/PLANO.md secao 3.4).
 *
 * @param diasMinimosBaseline abaixo disso nao ha historico para um z-score honesto
 * @param escalaZ             quanto cada desvio-padrao vale no componente objetivo
 * @param k                   quanto a adversidade do check-in levanta o indice
 * @param adversidadeVitoria  adversidade a partir da qual o dia conta como dificil
 * @param objetivoVitoria     desempenho minimo para o dia dificil ser considerado vencido
 */
public record ParametrosIndice(
        double pesoDificuldade,
        double pesoEnergia,
        double pesoSono,
        double pesoHumor,
        double pesoEstresse,
        double k,
        int diasMinimosBaseline,
        double escalaZ,
        double limiteNormal,
        double limiteBom,
        double limiteExcelente,
        double adversidadeVitoria,
        double objetivoVitoria) {

    public static ParametrosIndice padrao() {
        return new ParametrosIndice(0.30, 0.25, 0.20, 0.15, 0.10, 0.40, 7, 15.0, 35, 60, 80, 0.60, 45);
    }

    public Classificacao classificar(double indice) {
        if (indice < limiteNormal()) {
            return Classificacao.DIFICIL;
        }
        if (indice < limiteBom()) {
            return Classificacao.NORMAL;
        }
        return indice < limiteExcelente() ? Classificacao.BOM : Classificacao.EXCELENTE;
    }
}
