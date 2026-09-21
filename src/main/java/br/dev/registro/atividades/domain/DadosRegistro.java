package br.dev.registro.atividades.domain;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Entrada de criacao/edicao de um registro. Obrigatorios: categoria, duracao e esforco — o resto e
 * opcional para que o registro rapido caiba em tres campos.
 *
 * <p>Record de dominio usado direto como corpo da requisicao. Quem nao pode cruzar a fronteira REST
 * e a entidade JPA (lazy refs, estado gerenciado), nao um record imutavel.
 */
public record DadosRegistro(
        LocalDate dataLocal,
        Instant inicioEm,
        @Min(1) @Max(1440) int duracaoMin,
        @NotNull Categoria categoria,
        @Size(max = 200) String titulo,
        @Min(1) @Max(10) short esforco,
        @Min(1) @Max(5) Short satisfacao,
        @Size(max = 4000) String notas,
        Long projetoId,
        Long desafioId,
        Long livroId,
        Map<String, Object> detalhes) {
}
