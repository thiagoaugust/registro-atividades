package br.dev.registro.analytics.domain;

import java.time.LocalDate;

/**
 * Janela de agregacao. A unidade vai para o date_trunc do Postgres — por isso ela vem de um enum, e
 * nunca de texto do cliente.
 */
public enum Granularidade {
    DIA("day"),
    SEMANA("week"),
    MES("month"),
    ANO("year");

    private final String unidade;

    Granularidade(String unidade) {
        this.unidade = unidade;
    }

    public String unidade() {
        return unidade;
    }

    /** Inicio do periodo anterior de mesmo tamanho, para a comparacao. */
    public LocalDate recuar(LocalDate data, long periodos) {
        return switch (this) {
            case DIA -> data.minusDays(periodos);
            case SEMANA -> data.minusWeeks(periodos);
            case MES -> data.minusMonths(periodos);
            case ANO -> data.minusYears(periodos);
        };
    }
}
