package br.dev.registro.gamificacao.domain;

import java.time.LocalDate;

/** Um dia visto pela otica da streak: teve atividade? foi descanso planejado? */
public record DiaPresenca(LocalDate data, boolean presenca, boolean descanso) {
}
