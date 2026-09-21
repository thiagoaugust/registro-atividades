package br.dev.registro.gtd.api;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.gtd.domain.Acao;
import br.dev.registro.gtd.domain.Contexto;
import br.dev.registro.gtd.domain.DestinoInbox;
import br.dev.registro.gtd.domain.Energia;
import br.dev.registro.gtd.domain.EstadoAcao;
import br.dev.registro.gtd.domain.InboxItem;
import br.dev.registro.gtd.domain.Referencia;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Respostas do modulo GTD. */
public final class GtdDtos {

    private GtdDtos() {
    }

    public record Vinculo(Long id, String nome) {
    }

    public record AcaoDto(
            Long id,
            String titulo,
            String notas,
            EstadoAcao estado,
            Vinculo contexto,
            Integer tempoEstimadoMin,
            Energia energia,
            Categoria categoria,
            Vinculo projeto,
            Instant agendadaPara,
            String delegadaPara,
            LocalDate delegadaEm,
            Long registroId,
            Instant criadaEm,
            Instant concluidaEm) {

        public static AcaoDto de(Acao a) {
            return new AcaoDto(
                    a.id,
                    a.titulo,
                    a.notas,
                    a.estado,
                    a.contexto == null ? null : new Vinculo(a.contexto.id, a.contexto.nome),
                    a.tempoEstimadoMin,
                    a.energia,
                    a.categoria,
                    a.projeto == null ? null : new Vinculo(a.projeto.id, a.projeto.titulo),
                    a.agendadaPara,
                    a.delegadaPara,
                    a.delegadaEm,
                    a.registro == null ? null : a.registro.id,
                    a.criadaEm,
                    a.concluidaEm);
        }

        public static List<AcaoDto> de(List<Acao> acoes) {
            return acoes.stream().map(AcaoDto::de).toList();
        }
    }

    public record InboxItemDto(
            Long id,
            String texto,
            Instant capturadoEm,
            Instant processadoEm,
            DestinoInbox destino,
            Long destinoId) {

        public static InboxItemDto de(InboxItem i) {
            return new InboxItemDto(i.id, i.texto, i.capturadoEm, i.processadoEm, i.destino, i.destinoId);
        }
    }

    public record ContextoDto(Long id, String nome, boolean ativo, int ordem) {

        public static ContextoDto de(Contexto c) {
            return new ContextoDto(c.id, c.nome, c.ativo, c.ordem);
        }
    }

    public record ReferenciaDto(
            Long id, String titulo, String conteudo, String url, List<String> tags, Instant criadaEm) {

        public static ReferenciaDto de(Referencia r) {
            return new ReferenciaDto(
                    r.id, r.titulo, r.conteudo, r.url, Arrays.asList(r.tags), r.criadaEm);
        }
    }

    /**
     * Resposta de conclusao: quando a acao tem categoria, o cliente ja recebe a sugestao pronta de
     * "registrar como atividade" e so precisa perguntar duracao e esforco.
     */
    public record ConclusaoDto(AcaoDto acao, SugestaoRegistro sugestaoRegistro) {

        public record SugestaoRegistro(Categoria categoria, String titulo, Long projetoId) {
        }

        public static ConclusaoDto de(Acao a) {
            SugestaoRegistro sugestao = a.categoria == null || a.registro != null
                    ? null
                    : new SugestaoRegistro(a.categoria, a.titulo, a.projeto == null ? null : a.projeto.id);
            return new ConclusaoDto(AcaoDto.de(a), sugestao);
        }
    }
}
