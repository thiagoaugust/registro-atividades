package br.dev.registro.atividades.api;

import br.dev.registro.atividades.domain.Categoria;
import br.dev.registro.atividades.domain.RegistroAtividade;
import br.dev.registro.atividades.domain.RegistroService;
import br.dev.registro.checkin.api.CheckinResource.CheckinDto;
import br.dev.registro.checkin.domain.CheckinDiario;
import br.dev.registro.checkin.domain.CheckinService;
import br.dev.registro.gamificacao.domain.Classificacao;
import br.dev.registro.gamificacao.domain.DiaResumo;
import br.dev.registro.gamificacao.infra.DiaResumoRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Tudo que a tela de um dia precisa numa chamada: registros, check-in e o resumo do dia. */
@Path("/api/dias")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
public class DiaResource {

    private final RegistroService registros;
    private final CheckinService checkins;
    private final DiaResumoRepository resumos;

    public DiaResource(RegistroService registros, CheckinService checkins, DiaResumoRepository resumos) {
        this.registros = registros;
        this.checkins = checkins;
        this.resumos = resumos;
    }

    public record ResumoDto(
            int xpTotal,
            BigDecimal indiceProdutividade,
            Classificacao classificacao,
            boolean diaDificilVencido,
            boolean descanso,
            boolean presenca,
            boolean baselineInsuficiente) {

        static ResumoDto de(DiaResumo r) {
            return new ResumoDto(
                    r.xpTotal, r.indiceProdutividade, r.classificacao, r.diaDificilVencido,
                    r.descanso, r.presenca, r.baselineInsuficiente);
        }
    }

    public record DiaDto(
            LocalDate data,
            int totalMinutos,
            Map<Categoria, Integer> minutosPorCategoria,
            List<RegistroDto> registros,
            CheckinDto checkin,
            ResumoDto resumo) {
    }

    @GET
    @Path("{data}")
    public DiaDto porData(@PathParam("data") LocalDate data) {
        List<RegistroAtividade> doDia = registros.buscar(data, data, null);

        Map<Categoria, Integer> porCategoria = new TreeMap<>();
        int total = 0;
        for (RegistroAtividade r : doDia) {
            porCategoria.merge(r.categoria, r.duracaoMin, Integer::sum);
            total += r.duracaoMin;
        }

        CheckinDiario checkin = checkins.porData(data);
        DiaResumo resumo = resumos.findById(data);

        return new DiaDto(
                data,
                total,
                porCategoria,
                RegistroDto.de(doDia),
                checkin == null ? null : CheckinDto.de(checkin),
                resumo == null ? null : ResumoDto.de(resumo));
    }
}
