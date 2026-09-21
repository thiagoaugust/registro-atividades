package br.dev.registro.gamificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "conquista_desbloqueada")
public class ConquistaDesbloqueada {

    @Id
    @Column(name = "conquista_codigo")
    public String conquistaCodigo;

    @Column(name = "data_local", nullable = false)
    public LocalDate dataLocal;

    @Column(name = "desbloqueada_em", nullable = false)
    public Instant desbloqueadaEm = Instant.now();
}
