package br.dev.registro.gtd.domain;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.Projeto;
import br.dev.registro.atividades.domain.RegistroAtividade;
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

import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma acao do GTD. O campo {@link #estado} e o que separa "proximas acoes" de "agenda", "aguardando"
 * e "algum dia" — sao filtros sobre esta tabela, nao tabelas diferentes.
 */
@Entity
@Table(name = "acao")
public class Acao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String titulo;

    public String notas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public EstadoAcao estado = EstadoAcao.PROXIMA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contexto_id")
    public Contexto contexto;

    @Column(name = "tempo_estimado_min")
    public Integer tempoEstimadoMin;

    @Enumerated(EnumType.STRING)
    public Energia energia;

    /** Permite oferecer "registrar como atividade" quando a acao e concluida. */
    @Enumerated(EnumType.STRING)
    public Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id")
    public Projeto projeto;

    @Column(name = "agendada_para")
    public Instant agendadaPara;

    @Column(name = "delegada_para")
    public String delegadaPara;

    @Column(name = "delegada_em")
    public LocalDate delegadaEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registro_id")
    public RegistroAtividade registro;

    @CreationTimestamp
    @Column(name = "criada_em", nullable = false, updatable = false)
    public Instant criadaEm;

    @Column(name = "concluida_em")
    public Instant concluidaEm;

    public boolean aberta() {
        return estado != EstadoAcao.CONCLUIDA && estado != EstadoAcao.DESCARTADA;
    }
}
