package br.dev.registro.comum;

/**
 * Uma acao do GTD foi concluida ou deixou de estar concluida. Espelha {@link RegistroAlterado}: quem
 * observa reescreve o lancamento de XP da acao e recalcula o dia.
 */
public record AcaoAlterada(long acaoId, boolean concluida) {
}
