package br.dev.registro.analytics.domain;

/**
 * Colunas que podem entrar numa correlacao. Existe como enum para que nenhum nome de coluna venha do
 * cliente: a query de correlacao interpola o nome (corr() nao aceita isso como parametro), entao a
 * lista precisa ser fechada aqui.
 */
public enum Eixo {
    HORAS_SONO("horas_sono", "horas de sono"),
    QUALIDADE_SONO("qualidade_sono", "qualidade do sono"),
    ENERGIA("energia", "energia"),
    HUMOR("humor", "humor"),
    ESTRESSE("estresse", "estresse"),
    DIFICULDADE("dificuldade", "dificuldade percebida"),
    INDICE("indice_produtividade", "indice do dia"),
    XP("xp_total", "XP"),
    MINUTOS("minutos_total", "minutos");

    private final String coluna;
    private final String rotulo;

    Eixo(String coluna, String rotulo) {
        this.coluna = coluna;
        this.rotulo = rotulo;
    }

    public String coluna() {
        return coluna;
    }

    public String rotulo() {
        return rotulo;
    }
}
