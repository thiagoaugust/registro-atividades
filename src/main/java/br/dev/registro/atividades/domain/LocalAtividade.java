package br.dev.registro.atividades.domain;

/**
 * Onde a sessao aconteceu. Existe para uma pergunta so: foi tempo que ja estava perdido?
 *
 * <p>Estudar 40 minutos em casa e estudar 40 minutos em pe num onibus nao custam o mesmo. O segundo
 * nao tirou tempo de mais nada — era tempo morto — e custa mais atencao para acontecer. Por isso o
 * transporte publico bonifica o XP: o sistema mede esforco, e aqui ha esforco a mais.
 */
public enum LocalAtividade {
    CASA,
    TRABALHO,
    TRANSPORTE_PUBLICO,
    RUA,
    OUTRO;

    /** Tempo que so existe porque voce o resgatou de um deslocamento. */
    public boolean tempoAproveitado() {
        return this == TRANSPORTE_PUBLICO;
    }

    /** Null para valor ausente ou desconhecido — a validacao e que recusa o desconhecido. */
    public static LocalAtividade de(Object valor) {
        if (valor == null) {
            return null;
        }
        try {
            return valueOf(String.valueOf(valor));
        } catch (IllegalArgumentException naoEhUmLocal) {
            return null;
        }
    }
}
