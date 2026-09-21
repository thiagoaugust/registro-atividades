package br.dev.registro.checkin.domain;

import br.dev.registro.gamificacao.domain.ClassificadorDia.ContextoDia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Um por dia, editavel. Abertura e fechamento na mesma linha — e o mesmo dia, e duas tabelas so
 * dariam um join a mais em toda leitura.
 *
 * <p>Todos os itens sao opcionais: um check-in respondido pela metade vale mais que nenhum, e a
 * adversidade renormaliza os pesos do que foi preenchido.
 */
@Entity
@Table(name = "checkin_diario")
public class CheckinDiario {

    @Id
    @Column(name = "data_local")
    public LocalDate dataLocal;

    public Short energia;

    @Column(name = "horas_sono")
    public BigDecimal horasSono;

    @Column(name = "qualidade_sono")
    public Short qualidadeSono;

    public Short humor;

    public Short estresse;

    @Column(name = "dificuldade_prevista")
    public Short dificuldadePrevista;

    @Column(name = "descanso_planejado", nullable = false)
    public boolean descansoPlanejado;

    public String frase;

    @Column(name = "dificuldade_final")
    public Short dificuldadeFinal;

    public String atrapalhou;

    @Column(name = "fechado_em")
    public Instant fechadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    public Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm;

    /**
     * O que o classificador enxerga. A dificuldade do fechamento, quando existe, substitui a prevista:
     * o julgamento do fim do dia vale mais que a expectativa da manha.
     */
    public ContextoDia contexto() {
        Short dificuldade = dificuldadeFinal != null ? dificuldadeFinal : dificuldadePrevista;
        return new ContextoDia(
                inteiro(dificuldade), inteiro(energia), inteiro(qualidadeSono), inteiro(humor), inteiro(estresse));
    }

    private static Integer inteiro(Short valor) {
        return valor == null ? null : valor.intValue();
    }
}
