package br.dev.registro.gamificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Catalogo de conquistas. E dado, nao codigo: uma conquista nova e um INSERT enquanto reusar uma
 * metrica ja conhecida pelo avaliador.
 */
@Entity
@Table(name = "conquista")
public class Conquista {

    @Id
    public String codigo;

    @Column(nullable = false)
    public String titulo;

    public String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_regra", nullable = false)
    public TipoRegra tipoRegra;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    public Map<String, Object> parametros = Map.of();

    @Column(nullable = false)
    public boolean ativa = true;

    @Column(nullable = false)
    public int ordem;

    public enum TipoRegra {
        /** Soma de uma metrica continua: km, paginas, minutos. */
        TOTAL_METRICA,
        /** Dias consecutivos, geral ou de uma categoria. */
        STREAK,
        /** Quantas vezes um evento discreto aconteceu. */
        CONTAGEM_EVENTO
    }

    public String parametroTexto(String chave) {
        Object valor = parametros.get(chave);
        return valor == null ? null : String.valueOf(valor);
    }

    public double alvo() {
        Object valor = parametros.get("alvo");
        return valor instanceof Number n ? n.doubleValue() : 1;
    }
}
