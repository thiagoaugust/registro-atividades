package br.dev.registro.gamificacao.domain;

import br.dev.registro.atividades.domain.Categoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma linha do ledger de XP, sempre em pontos BRUTOS: tetos e bonus sao aplicados na consolidacao do
 * dia, onde se conhece o total. Ha no maximo um lancamento por (origem, origem_id).
 */
@Entity
@Table(name = "xp_lancamento")
public class XpLancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "data_local", nullable = false)
    public LocalDate dataLocal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrigemXp origem;

    @Column(name = "origem_id", nullable = false)
    public long origemId;

    /** Nulo nas origens do GTD: acao concluida e revisao semanal nao pertencem a uma categoria. */
    @Enumerated(EnumType.STRING)
    public Categoria categoria;

    @Column(name = "pontos_brutos", nullable = false)
    public int pontosBrutos;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    public Instant criadoEm;

    public static XpLancamento de(
            LocalDate dataLocal, OrigemXp origem, long origemId, Categoria categoria, int pontos) {
        XpLancamento l = new XpLancamento();
        l.dataLocal = dataLocal;
        l.origem = origem;
        l.origemId = origemId;
        l.categoria = categoria;
        l.pontosBrutos = pontos;
        return l;
    }
}
