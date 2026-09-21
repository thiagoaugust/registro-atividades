package br.dev.registro.gamificacao.domain;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.comum.Relogio;
import br.dev.registro.gamificacao.infra.ConquistaRepository;
import br.dev.registro.gamificacao.infra.DiaResumoRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Leitura do perfil: nivel, XP, streaks e conquistas. Tudo derivado do resumo materializado. */
@ApplicationScoped
public class PerfilService {

    private static final int JANELA_STREAK = 400;

    private final DiaResumoRepository resumos;
    private final ConquistaRepository conquistas;
    private final AvaliadorConquistas avaliador;
    private final Relogio relogio;

    public PerfilService(
            DiaResumoRepository resumos,
            ConquistaRepository conquistas,
            AvaliadorConquistas avaliador,
            Relogio relogio) {
        this.resumos = resumos;
        this.conquistas = conquistas;
        this.avaliador = avaliador;
        this.relogio = relogio;
    }

    public record NivelDto(int nivel, long xp, long xpNesteNivel, long xpParaOProximo) {

        static NivelDto de(long xpAcumulado) {
            int nivel = CalculadoraXp.nivel(xpAcumulado);
            long base = CalculadoraXp.xpParaNivel(nivel);
            long proximo = CalculadoraXp.xpParaNivel(nivel + 1);
            return new NivelDto(nivel, xpAcumulado, xpAcumulado - base, proximo - xpAcumulado);
        }
    }

    public record PerfilDto(
            NivelDto geral,
            Map<Categoria, NivelDto> porCategoria,
            int streakGeral,
            Map<Categoria, Integer> streakPorCategoria,
            int conquistasDesbloqueadas,
            int conquistasTotais) {
    }

    public record ConquistaDto(
            String codigo,
            String titulo,
            String descricao,
            double alvo,
            double progresso,
            boolean desbloqueada,
            LocalDate dataLocal,
            Instant desbloqueadaEm) {
    }

    public PerfilDto perfil() {
        LocalDate hoje = relogio.hoje();

        Map<Categoria, NivelDto> niveis = new EnumMap<>(Categoria.class);
        Map<Categoria, Long> xpPorCategoria = resumos.xpAcumuladoPorCategoria();
        Map<Categoria, Integer> streaks = new EnumMap<>(Categoria.class);

        for (Categoria categoria : Categoria.values()) {
            niveis.put(categoria, NivelDto.de(xpPorCategoria.getOrDefault(categoria, 0L)));
            streaks.put(
                    categoria,
                    StreakCalculator.corrente(resumos.presencaRecente(categoria, hoje, JANELA_STREAK), hoje));
        }

        return new PerfilDto(
                NivelDto.de(resumos.xpAcumulado()),
                niveis,
                StreakCalculator.corrente(resumos.presencaRecente(hoje, JANELA_STREAK), hoje),
                streaks,
                conquistas.codigosDesbloqueados().size(),
                (int) conquistas.count("ativa = true"));
    }

    /** Catalogo com o progresso de cada conquista — desbloqueadas primeiro, depois as mais perto. */
    public List<ConquistaDto> conquistas() {
        Map<String, ConquistaDesbloqueada> desbloqueadas = new HashMap<>();
        for (ConquistaDesbloqueada d : conquistas.desbloqueadas()) {
            desbloqueadas.put(d.conquistaCodigo, d);
        }

        Map<String, Double> cache = new HashMap<>();
        List<ConquistaDto> resultado = new ArrayList<>();
        LocalDate hoje = relogio.hoje();

        for (Conquista conquista : conquistas.ativas()) {
            ConquistaDesbloqueada desbloqueada = desbloqueadas.get(conquista.codigo);
            double progresso = desbloqueada != null
                    ? conquista.alvo()
                    : Math.min(avaliador.progresso(conquista, hoje, cache), conquista.alvo());

            resultado.add(new ConquistaDto(
                    conquista.codigo,
                    conquista.titulo,
                    conquista.descricao,
                    conquista.alvo(),
                    progresso,
                    desbloqueada != null,
                    desbloqueada == null ? null : desbloqueada.dataLocal,
                    desbloqueada == null ? null : desbloqueada.desbloqueadaEm));
        }

        resultado.sort((a, b) -> {
            if (a.desbloqueada() != b.desbloqueada()) {
                return a.desbloqueada() ? -1 : 1;
            }
            return Double.compare(b.progresso() / b.alvo(), a.progresso() / a.alvo());
        });
        return resultado;
    }
}
