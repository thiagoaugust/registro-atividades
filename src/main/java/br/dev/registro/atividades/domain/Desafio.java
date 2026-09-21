package br.dev.registro.atividades.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Meta com prazo. O progresso NAO e um campo: e a soma dos registros vinculados (ver
 * RegistroRepository.progressoDoDesafio), para nao dessincronizar quando um registro e editado.
 */
@Entity
@Table(name = "desafio")
public class Desafio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String titulo;

    @Column(name = "meta_valor", nullable = false)
    public BigDecimal metaValor;

    /** Unidade da meta e do progresso dos registros: km, paginas, horas, repeticoes... */
    @Column(nullable = false)
    public String unidade;

    @Column(nullable = false)
    public LocalDate inicio;

    public LocalDate fim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public StatusDesafio status = StatusDesafio.ATIVO;
}
