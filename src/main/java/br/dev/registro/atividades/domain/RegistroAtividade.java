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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uma sessao registrada. Obrigatorios sao apenas categoria, duracao e esforco (requisito do registro
 * rapido); vinculos e detalhes sao opcionais e expansiveis.
 *
 * <p>dataLocal e o dia em America/Sao_Paulo, derivado por {@link br.dev.registro.comum.Relogio} e
 * nunca de UTC direto. inicioEm guarda o instante em UTC.
 */
@Entity
@Table(name = "registro_atividade")
public class RegistroAtividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "data_local", nullable = false)
    public LocalDate dataLocal;

    @Column(name = "inicio_em")
    public Instant inicioEm;

    @Column(name = "duracao_min", nullable = false)
    public int duracaoMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Categoria categoria;

    public String titulo;

    /** 1 a 10. */
    @Column(nullable = false)
    public short esforco;

    /** 1 a 5, opcional. */
    public Short satisfacao;

    public String notas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    public Projeto projeto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "desafio_id")
    public Desafio desafio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livro_id")
    public Livro livro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id")
    public Curso curso;

    /** Campos especificos da categoria. Validados por {@link ValidadorDetalhes}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    public Map<String, Object> detalhes = new LinkedHashMap<>();

    /** Sessao feita em tempo que ja estava perdido — o XP dela ganha bonus. */
    public boolean tempoAproveitado() {
        if (detalhes == null) {
            return false;
        }
        LocalAtividade local = LocalAtividade.de(detalhes.get("local"));
        return local != null && local.tempoAproveitado();
    }

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    public Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    public Instant atualizadoEm;
}
