package br.dev.registro.checkin.domain;

import br.dev.registro.checkin.infra.CheckinRepository;
import br.dev.registro.comum.DiaAlterado;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.comum.Relogio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@ApplicationScoped
public class CheckinService {

    private final CheckinRepository checkins;
    private final Relogio relogio;
    private final Event<DiaAlterado> diaAlterado;

    public CheckinService(CheckinRepository checkins, Relogio relogio, Event<DiaAlterado> diaAlterado) {
        this.checkins = checkins;
        this.relogio = relogio;
        this.diaAlterado = diaAlterado;
    }

    /** Abertura do dia. Todo campo e opcional: meio check-in ainda diz mais que nenhum. */
    public record DadosCheckin(
            @Min(1) @Max(5) Short energia,
            @Min(0) @Max(1440) Integer minutosSono,
            LocalTime dormiuEm,
            LocalTime acordouEm,
            @Min(0) @Max(1440) Integer minutosSonoProfundo,
            @Min(0) @Max(1440) Integer minutosSonoRem,
            @Min(0) @Max(100) Integer despertares,
            @Min(20) @Max(220) Integer fcRepouso,
            @Min(0) @Max(100) Integer pontuacaoSono,
            @Min(1) @Max(5) Short qualidadeSono,
            @Min(1) @Max(5) Short humor,
            @Min(1) @Max(5) Short estresse,
            @Min(1) @Max(5) Short dificuldadePrevista,
            boolean descansoPlanejado,
            @Size(max = 500) String frase) {
    }

    /** Fechamento do dia: como foi de verdade, e o que atrapalhou. */
    public record DadosFechamento(
            @Min(1) @Max(5) Short dificuldadeFinal,
            @Size(max = 1000) String atrapalhou) {
    }

    public CheckinDiario porData(LocalDate dia) {
        return checkins.findById(dia);
    }

    @Transactional
    public CheckinDiario salvar(LocalDate dia, DadosCheckin dados) {
        CheckinDiario checkin = obterOuCriar(dia);
        checkin.energia = dados.energia();
        checkin.dormiuEm = dados.dormiuEm();
        checkin.acordouEm = dados.acordouEm();
        checkin.minutosSono = duracaoDoSono(dados);
        checkin.minutosSonoProfundo = dados.minutosSonoProfundo();
        checkin.minutosSonoRem = dados.minutosSonoRem();
        checkin.despertares = dados.despertares();
        checkin.fcRepouso = dados.fcRepouso();
        checkin.pontuacaoSono = dados.pontuacaoSono();
        checkin.qualidadeSono = dados.qualidadeSono();
        checkin.humor = dados.humor();
        checkin.estresse = dados.estresse();
        checkin.dificuldadePrevista = dados.dificuldadePrevista();
        checkin.descansoPlanejado = dados.descansoPlanejado();
        checkin.frase = dados.frase();

        diaAlterado.fire(new DiaAlterado(dia));
        return checkin;
    }

    @Transactional
    public CheckinDiario fechar(LocalDate dia, DadosFechamento dados) {
        CheckinDiario checkin = obterOuCriar(dia);
        checkin.dificuldadeFinal = dados.dificuldadeFinal();
        checkin.atrapalhou = dados.atrapalhou();
        checkin.fechadoEm = relogio.agora();

        // A dificuldade do fechamento entra no indice do dia, entao o resumo precisa ser refeito.
        diaAlterado.fire(new DiaAlterado(dia));
        return checkin;
    }

    /**
     * Quem digita os horarios do relogio nao precisa fazer a subtracao. A duracao informada ganha da
     * derivada: o relogio desconta os despertares, e a conta de deitar a acordar nao.
     */
    private static Integer duracaoDoSono(DadosCheckin dados) {
        if (dados.minutosSono() != null) {
            return dados.minutosSono();
        }
        if (dados.dormiuEm() == null || dados.acordouEm() == null) {
            return null;
        }
        long minutos = Duration.between(dados.dormiuEm(), dados.acordouEm()).toMinutes();
        // Deitar as 23h e acordar as 7h da negativo num relogio de 24h: a noite atravessou a meia-noite.
        return (int) (minutos <= 0 ? minutos + Duration.ofDays(1).toMinutes() : minutos);
    }

    private CheckinDiario obterOuCriar(LocalDate dia) {
        if (dia.isAfter(relogio.hoje())) {
            throw new RegraNegocioException("nao da para fazer check-in de um dia que ainda nao chegou");
        }
        CheckinDiario checkin = checkins.findById(dia);
        if (checkin == null) {
            checkin = new CheckinDiario();
            checkin.dataLocal = dia;
            checkins.persist(checkin);
        }
        return checkin;
    }
}
