package br.dev.registro.comum;

import java.time.LocalDate;

/**
 * A revisao semanal foi concluida. A gamificacao observa para lancar o XP no dia em que ela
 * aconteceu — o merito e do dia em que voce sentou para revisar.
 */
public record RevisaoConcluida(LocalDate semanaInicio, LocalDate dia) {
}
