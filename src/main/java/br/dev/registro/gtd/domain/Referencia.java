package br.dev.registro.gtd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/** Material nao acionavel que vale guardar: um link, uma anotacao, um numero. */
@Entity
@Table(name = "referencia")
public class Referencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String titulo;

    public String conteudo;

    public String url;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    public String[] tags = new String[0];

    @CreationTimestamp
    @Column(name = "criada_em", nullable = false, updatable = false)
    public Instant criadaEm;
}
