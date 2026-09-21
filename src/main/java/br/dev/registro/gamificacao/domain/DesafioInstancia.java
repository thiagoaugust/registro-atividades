package br.dev.registro.gamificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * O desafio de um periodo concreto, com o alvo ja calibrado no momento em que o periodo abriu. O
 * alvo fica congelado de proposito: mudar a regua no meio da semana invalidaria o esforco ja feito.
 */
@Entity
@Table(name = "desafio_instancia")
public class DesafioInstancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desafio_id", nullable = false)
    public DesafioPeriodico desafio;

    @Column(name = "periodo_inicio", nullable = false)
    public LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    public LocalDate periodoFim;

    @Column(nullable = false)
    public BigDecimal alvo;

    @Column(nullable = false)
    public BigDecimal progresso = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public StatusDesafio status = StatusDesafio.ABERTO;

    @Column(name = "cumprido_em")
    public Instant cumpridoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm;

    public double alvoNumerico() {
        return alvo.doubleValue();
    }

    public double progressoNumerico() {
        return progresso.doubleValue();
    }

    /** Fracao de 0 a 1, limitada: 130% de uma meta nao deve estourar a barra na tela. */
    public double fracao() {
        double meta = alvoNumerico();
        return meta <= 0 ? 0 : Math.min(1, progressoNumerico() / meta);
    }
}
