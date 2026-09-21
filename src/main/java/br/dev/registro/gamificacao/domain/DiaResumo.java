package br.dev.registro.gamificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Resumo materializado do dia. Recalculado por evento e reconciliado pelo job noturno; nenhuma tela
 * recalcula janela movel de 28 dias na hora de renderizar.
 */
@Entity
@Table(name = "dia_resumo")
public class DiaResumo {

    @Id
    @Column(name = "data_local")
    public LocalDate dataLocal;

    @Column(name = "xp_total", nullable = false)
    public int xpTotal;

    @Column(name = "minutos_total", nullable = false)
    public int minutosTotal;

    @Column(name = "categorias_distintas", nullable = false)
    public short categoriasDistintas;

    @Column(name = "indice_produtividade", nullable = false)
    public BigDecimal indiceProdutividade = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Classificacao classificacao = Classificacao.DIFICIL;

    @Column(name = "dia_dificil_vencido", nullable = false)
    public boolean diaDificilVencido;

    @Column(nullable = false)
    public boolean descanso;

    /** Houve registro ou check-in: e o que segura a streak. */
    @Column(nullable = false)
    public boolean presenca;

    @Column(name = "baseline_insuficiente", nullable = false)
    public boolean baselineInsuficiente;

    @Column(name = "calculado_em", nullable = false)
    public Instant calculadoEm = Instant.now();
}
