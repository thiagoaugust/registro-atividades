package br.dev.registro.comum;

import java.time.LocalDate;

/**
 * Evento CDI disparado por quem muda o que um dia contem (registro salvo/editado/excluido, check-in
 * salvo). A gamificacao observa e recalcula; o modulo de atividades nao precisa conhecer XP.
 *
 * <p>Sincrono e dentro da mesma transacao de proposito: se o recalculo falhar, a escrita que o
 * originou tambem volta atras, em vez de deixar o resumo do dia mentindo ate a madrugada.
 */
public record DiaAlterado(LocalDate dia) {
}
