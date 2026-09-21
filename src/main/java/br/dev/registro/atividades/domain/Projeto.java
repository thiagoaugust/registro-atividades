package br.dev.registro.atividades.domain;

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

/**
 * Entidade unica: e o projeto do GTD (com resultado desejado e proximas acoes) e o projeto vinculado
 * aos registros da categoria PROJETO. Nao existe um segundo conceito de projeto no sistema.
 */
@Entity
@Table(name = "projeto")
public class Projeto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String titulo;

    @Column(name = "resultado_desejado")
    public String resultadoDesejado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public StatusProjeto status = StatusProjeto.ATIVO;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    public Instant criadoEm;

    @Column(name = "concluido_em")
    public Instant concluidoEm;
}
