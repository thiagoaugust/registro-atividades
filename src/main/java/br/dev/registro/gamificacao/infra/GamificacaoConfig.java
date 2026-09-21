package br.dev.registro.gamificacao.infra;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.gamificacao.domain.ParametrosIndice;
import br.dev.registro.gamificacao.domain.ParametrosXp;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.EnumMap;
import java.util.Map;

/**
 * Parametros de gamificacao vindos da config (equivalente ao @ConfigurationProperties do Spring).
 * Os metodos default convertem para os records puros do dominio, que e o que os calculos recebem —
 * assim nenhuma funcao de calculo conhece Quarkus.
 *
 * <p>Os multiplicadores sao um mapa de config (`gamificacao.xp.multiplicadores.TREINO=1.0`), entao
 * mudar o peso de uma categoria e editar uma linha, nao recompilar.
 */
@ConfigMapping(prefix = "gamificacao")
public interface GamificacaoConfig {

    Xp xp();

    Indice indice();

    interface Xp {
        @WithDefault("0.6")
        double esforcoBase();

        @WithDefault("0.08")
        double esforcoIncremento();

        Map<String, Double> multiplicadores();

        @WithDefault("120")
        int tetoDiarioCategoria();

        @WithDefault("30")
        int tetoGtdDiario();

        @WithDefault("5")
        int pontosPorAcaoGtd();

        @WithDefault("50")
        int pontosRevisaoSemanal();

        /** XP de um desafio cumprido, por horizonte: o mes vale mais porque custa um mes. */
        @WithDefault("10")
        int pontosDesafioDiario();

        @WithDefault("40")
        int pontosDesafioSemanal();

        @WithDefault("150")
        int pontosDesafioMensal();

        @WithDefault("0.15")
        double bonusEquilibrio();

        @WithDefault("3")
        int categoriasParaEquilibrio();
    }

    interface Indice {
        @WithDefault("0.30")
        double pesoDificuldade();

        @WithDefault("0.25")
        double pesoEnergia();

        @WithDefault("0.20")
        double pesoSono();

        @WithDefault("0.15")
        double pesoHumor();

        @WithDefault("0.10")
        double pesoEstresse();

        @WithDefault("0.40")
        double k();

        @WithDefault("7")
        int diasMinimosBaseline();

        @WithDefault("28")
        int diasJanela();

        @WithDefault("15.0")
        double escalaZ();

        @WithDefault("35")
        double limiteNormal();

        @WithDefault("60")
        double limiteBom();

        @WithDefault("80")
        double limiteExcelente();

        @WithDefault("0.60")
        double adversidadeVitoria();

        @WithDefault("45")
        double objetivoVitoria();
    }

    default ParametrosXp parametrosXp() {
        Map<Categoria, Double> multiplicadores = new EnumMap<>(Categoria.class);
        for (Categoria categoria : Categoria.values()) {
            multiplicadores.put(categoria, xp().multiplicadores().getOrDefault(categoria.name(), 1.0));
        }
        return new ParametrosXp(
                xp().esforcoBase(),
                xp().esforcoIncremento(),
                multiplicadores,
                xp().tetoDiarioCategoria(),
                xp().tetoGtdDiario(),
                xp().bonusEquilibrio(),
                xp().categoriasParaEquilibrio());
    }

    default ParametrosIndice parametrosIndice() {
        return new ParametrosIndice(
                indice().pesoDificuldade(),
                indice().pesoEnergia(),
                indice().pesoSono(),
                indice().pesoHumor(),
                indice().pesoEstresse(),
                indice().k(),
                indice().diasMinimosBaseline(),
                indice().escalaZ(),
                indice().limiteNormal(),
                indice().limiteBom(),
                indice().limiteExcelente(),
                indice().adversidadeVitoria(),
                indice().objetivoVitoria());
    }
}
