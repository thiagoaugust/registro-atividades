package br.dev.registro.comum;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RelogioTest {

    /** O dia de um registro e o dia local; 02:00 UTC ainda e ontem em Sao Paulo. */
    @Test
    void dia_local_nao_e_o_dia_utc() {
        Instant madrugadaUtc = Instant.parse("2026-09-21T02:00:00Z");

        assertThat(Relogio.fixadoEm(madrugadaUtc).hoje()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(Relogio.fixadoEm(madrugadaUtc).diaLocal(madrugadaUtc)).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void depois_das_tres_da_manha_utc_o_dia_vira() {
        Instant instante = Instant.parse("2026-09-21T03:30:00Z");
        assertThat(Relogio.fixadoEm(instante).hoje()).isEqualTo(LocalDate.of(2026, 9, 21));
    }
}
