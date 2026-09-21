package br.dev.registro.comum;

/**
 * Um registro de atividade foi criado, editado ou excluido. Quem observa e responsavel por
 * reescrever o que deriva dele (o lancamento de XP) e por recalcular os dias afetados.
 */
public record RegistroAlterado(long registroId, boolean excluido) {
}
