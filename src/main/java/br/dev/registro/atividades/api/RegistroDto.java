package br.dev.registro.atividades.api;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.RegistroAtividade;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Resposta de um registro. Os vinculos viram {id, titulo} — o cliente nao precisa do objeto inteiro. */
public record RegistroDto(
        Long id,
        LocalDate dataLocal,
        Instant inicioEm,
        int duracaoMin,
        Categoria categoria,
        String titulo,
        short esforco,
        Short satisfacao,
        String notas,
        Vinculo projeto,
        Vinculo desafio,
        Vinculo livro,
        Map<String, Object> detalhes,
        Instant criadoEm,
        Instant atualizadoEm) {

    public record Vinculo(Long id, String titulo) {
    }

    public static RegistroDto de(RegistroAtividade r) {
        return new RegistroDto(
                r.id,
                r.dataLocal,
                r.inicioEm,
                r.duracaoMin,
                r.categoria,
                r.titulo,
                r.esforco,
                r.satisfacao,
                r.notas,
                r.projeto == null ? null : new Vinculo(r.projeto.id, r.projeto.titulo),
                r.desafio == null ? null : new Vinculo(r.desafio.id, r.desafio.titulo),
                r.livro == null ? null : new Vinculo(r.livro.id, r.livro.titulo),
                r.detalhes,
                r.criadoEm,
                r.atualizadoEm);
    }

    public static List<RegistroDto> de(List<RegistroAtividade> registros) {
        return registros.stream().map(RegistroDto::de).toList();
    }
}
