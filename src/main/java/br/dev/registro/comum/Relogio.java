package br.dev.registro.comum;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Fonte unica de "que dia e hoje". Nunca chame LocalDate.now() direto: o dia de um registro e o dia
 * local em America/Sao_Paulo, e testes precisam conseguir fixar o relogio.
 */
@ApplicationScoped
public class Relogio {

    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final Clock clock;

    public Relogio() {
        this(Clock.system(ZONA));
    }

    public Relogio(Clock clock) {
        this.clock = clock;
    }

    /** Relogio fixo, para testes. */
    public static Relogio fixadoEm(Instant instante) {
        return new Relogio(Clock.fixed(instante, ZONA));
    }

    public Instant agora() {
        return clock.instant();
    }

    public LocalDate hoje() {
        return LocalDate.now(clock);
    }

    /** O dia local de um instante UTC. */
    public LocalDate diaLocal(Instant instante) {
        return instante.atZone(ZONA).toLocalDate();
    }
}
