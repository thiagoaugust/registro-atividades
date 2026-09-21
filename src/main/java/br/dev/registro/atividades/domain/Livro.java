package br.dev.registro.atividades.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "livro")
public class Livro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String titulo;

    public String autor;

    /** Usado para fechar o livro automaticamente quando a pagina final alcanca o total. */
    @Column(name = "total_paginas")
    public Integer totalPaginas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public StatusLivro status = StatusLivro.LENDO;

    @Column(name = "concluido_em")
    public LocalDate concluidoEm;

    /** Endereco da imagem da capa. */
    @Column(name = "capa_url")
    public String capaUrl;

    /** 1 (leve) a 5 (denso), declarado por quem le. */
    public Short dificuldade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id")
    public AreaConhecimento area;

    /**
     * Dias que a leitura levou, para livros lidos antes de o sistema existir. A presenca deste campo
     * e o que marca o livro como retroativo — nao ha sessao registrada para ele.
     */
    @Column(name = "dias_leitura")
    public Integer diasLeitura;

    /** Horas totais, quando lembradas. Sem elas, o livro retroativo nao entra na conta de pag/hora. */
    @Column(name = "horas_leitura")
    public BigDecimal horasLeitura;

    public boolean retroativo() {
        return diasLeitura != null;
    }
}
