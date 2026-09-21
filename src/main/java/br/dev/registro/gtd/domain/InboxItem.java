package br.dev.registro.gtd.domain;

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
 * Item capturado. Nasce so com texto: decidir o que e na hora da captura e o que faz gente parar de
 * capturar.
 */
@Entity
@Table(name = "inbox_item")
public class InboxItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String texto;

    @CreationTimestamp
    @Column(name = "capturado_em", nullable = false, updatable = false)
    public Instant capturadoEm;

    @Column(name = "processado_em")
    public Instant processadoEm;

    @Enumerated(EnumType.STRING)
    public DestinoInbox destino;

    /** Id do que o item virou (acao, projeto ou referencia). Nulo para LIXO. */
    @Column(name = "destino_id")
    public Long destinoId;
}
