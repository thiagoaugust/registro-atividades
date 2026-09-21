package br.dev.registro.gamificacao.domain;

/**
 * O que um desafio mede num intervalo de dias. Sao as grandezas que o sistema ja registra — um
 * desafio novo so pode pedir aquilo que existe medicao para conferir.
 */
public enum MetricaPeriodo {
    XP("XP"),
    MINUTOS_TOTAL("minutos"),
    MINUTOS_CATEGORIA("minutos"),
    CATEGORIAS_DISTINTAS("categorias"),
    DIAS_COM_PRESENCA("dias"),
    DIAS_COM_CATEGORIA("dias"),
    DIAS_DIFICEIS_VENCIDOS("dias"),
    PAGINAS("paginas"),
    KM("km"),
    MINUTOS_ESTUDO("minutos"),
    MINUTOS_PRATICA("minutos"),
    MINUTOS_APROVEITADOS("minutos"),
    ACOES_CONCLUIDAS("acoes"),
    REVISOES("revisoes");

    private final String unidade;

    MetricaPeriodo(String unidade) {
        this.unidade = unidade;
    }

    public String unidade() {
        return unidade;
    }

    /** Metricas que dependem de uma categoria informada nos parametros. */
    public boolean exigeCategoria() {
        return this == MINUTOS_CATEGORIA || this == DIAS_COM_CATEGORIA;
    }
}
