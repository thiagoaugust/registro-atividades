package br.dev.registro.gtd.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uma revisao por semana. A segunda-feira que abre a semana e a chave — nao ha "duas revisoes da
 * mesma semana", ha uma revisao que foi retomada.
 */
@Entity
@Table(name = "revisao_semanal")
public class RevisaoSemanal {

    @Id
    @Column(name = "semana_inicio")
    public LocalDate semanaInicio;

    @Column(name = "iniciada_em", nullable = false)
    public Instant iniciadaEm = Instant.now();

    @Column(name = "concluida_em")
    public Instant concluidaEm;

    @Column(name = "duracao_min")
    public Integer duracaoMin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    public Map<String, Object> passos = new LinkedHashMap<>();

    public boolean concluida() {
        return concluidaEm != null;
    }

    public boolean passoFeito(PassoRevisao passo) {
        return Boolean.TRUE.equals(passos.get(passo.name()));
    }

    public boolean todosOsPassosFeitos() {
        for (PassoRevisao passo : PassoRevisao.values()) {
            if (!passoFeito(passo)) {
                return false;
            }
        }
        return true;
    }
}
