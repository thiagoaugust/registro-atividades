package br.dev.registro.gamificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A ideia de um desafio, que se repete a cada periodo. Nao e o desafio de uma semana especifica —
 * isso e a {@link DesafioInstancia}. Separar os dois e o que permite descontinuar um desafio sem
 * apagar o que voce ja cumpriu dele.
 */
@Entity
@Table(name = "desafio_periodico")
public class DesafioPeriodico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EscopoDesafio escopo;

    @Column(nullable = false)
    public String titulo;

    public String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public MetricaPeriodo metrica;

    /** {@code categoria}, {@code fator} e {@code minimo} — o que cada metrica precisar. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    public Map<String, Object> parametros = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public TipoDesafio tipo = TipoDesafio.META;

    /** Nulo quando o alvo sai do historico; preenchido so nas contagens que nao fazem sentido calibrar. */
    public BigDecimal alvo;

    @Column(nullable = false)
    public boolean ativo = true;

    @Column(nullable = false)
    public int ordem;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    public Instant criadoEm;

    public double parametro(String nome, double padrao) {
        Object valor = parametros.get(nome);
        return valor instanceof Number numero ? numero.doubleValue() : padrao;
    }

    public String categoria() {
        Object valor = parametros.get("categoria");
        return valor == null ? null : valor.toString();
    }
}
