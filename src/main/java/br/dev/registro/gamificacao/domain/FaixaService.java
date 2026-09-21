package br.dev.registro.gamificacao.domain;

import br.dev.registro.checkin.domain.CheckinDiario;
import br.dev.registro.checkin.infra.CheckinRepository;
import br.dev.registro.gamificacao.domain.FaixaEsforco.Situacao;
import br.dev.registro.gamificacao.infra.DiaResumoRepository;
import br.dev.registro.gamificacao.infra.FaixaRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;

/**
 * Responde "quanto e um dia justo hoje" — e so hoje, porque a resposta depende de como o dia
 * comecou. Sem energia no check-in, a faixa e a geral; preenchida a energia, a regua muda na hora.
 */
@ApplicationScoped
public class FaixaService {

    private final FaixaRepository faixas;
    private final CheckinRepository checkins;
    private final DiaResumoRepository resumos;

    public FaixaService(
            FaixaRepository faixas, CheckinRepository checkins, DiaResumoRepository resumos) {
        this.faixas = faixas;
        this.checkins = checkins;
        this.resumos = resumos;
    }

    public record FaixaDto(
            LocalDate data,
            BandaEnergia banda,
            String rotulo,
            int piso,
            int tipico,
            int teto,
            int xpDoDia,
            int faltaParaOPiso,
            Situacao situacao,
            int diasComparaveis) {
    }

    public FaixaDto doDia(LocalDate dia) {
        CheckinDiario checkin = checkins.findById(dia);
        Integer energia = checkin == null || checkin.energia == null ? null : checkin.energia.intValue();
        BandaEnergia banda = BandaEnergia.de(energia);

        FaixaEsforco faixa = FaixaEsforco.de(banda, faixas.xpDeDiasComparaveis(dia, banda));
        DiaResumo resumo = resumos.findById(dia);
        int xp = resumo == null ? 0 : resumo.xpTotal;

        // Folga marcada nao e divida. O classificador ja trata descanso como dia neutro; cobrar a
        // faixa aqui diria o contrario na mesma tela.
        boolean descanso = checkin != null && checkin.descansoPlanejado;
        Situacao situacao = descanso ? Situacao.DESCANSO : faixa.situacao(xp);

        return new FaixaDto(
                dia,
                banda,
                descanso ? "descanso planejado" : banda == null ? "dia sem energia informada" : banda.rotulo(),
                faixa.piso(),
                faixa.tipico(),
                faixa.teto(),
                xp,
                descanso ? 0 : faixa.faltaParaOPiso(xp),
                situacao,
                faixa.diasConsiderados());
    }
}
