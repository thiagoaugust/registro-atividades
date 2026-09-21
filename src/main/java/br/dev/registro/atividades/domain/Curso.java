package br.dev.registro.atividades.domain;

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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Curso com carga horaria. O progresso e o tempo dedicado dividido pela carga — mesma ideia do livro,
 * trocando paginas por horas.
 */
@Entity
@Table(name = "curso")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String titulo;

    public String instituicao;

    public String url;

    /** Carga horaria prevista. Sem ela nao ha percentual nem previsao, so o tempo acumulado. */
    @Column(name = "carga_horaria")
    public BigDecimal cargaHoraria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    public AreaConhecimento area;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public StatusCurso status = StatusCurso.CURSANDO;

    @Column(name = "concluido_em")
    public LocalDate concluidoEm;

    /** Horas de um curso feito antes de o sistema existir. */
    @Column(name = "horas_retroativas")
    public BigDecimal horasRetroativas;

    @Column(name = "dias_retroativos")
    public Integer diasRetroativos;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    public Instant criadoEm;

    public boolean retroativo() {
        return horasRetroativas != null;
    }
}
