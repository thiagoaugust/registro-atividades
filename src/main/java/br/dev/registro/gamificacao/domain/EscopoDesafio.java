package br.dev.registro.gamificacao.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Os tres horizontes: o dia, a semana e o mes. Cada um sabe recortar o proprio periodo. */
public enum EscopoDesafio {
    DIARIO,
    SEMANAL,
    MENSAL;

    /** Primeiro dia do periodo que contem a data — e tambem a identidade da instancia. */
    public LocalDate inicio(LocalDate data) {
        return switch (this) {
            case DIARIO -> data;
            case SEMANAL -> data.with(DayOfWeek.MONDAY);
            case MENSAL -> data.withDayOfMonth(1);
        };
    }

    public LocalDate fim(LocalDate data) {
        return switch (this) {
            case DIARIO -> data;
            case SEMANAL -> inicio(data).plusDays(6);
            case MENSAL -> data.withDayOfMonth(data.lengthOfMonth());
        };
    }

    /** O periodo anterior, para calibrar o alvo e para saber o que ja venceu. */
    public LocalDate anterior(LocalDate inicio, int quantos) {
        return switch (this) {
            case DIARIO -> inicio.minusDays(quantos);
            case SEMANAL -> inicio.minusWeeks(quantos);
            case MENSAL -> inicio.minusMonths(quantos);
        };
    }
}
