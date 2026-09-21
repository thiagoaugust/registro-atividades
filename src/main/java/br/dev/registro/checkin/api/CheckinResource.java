package br.dev.registro.checkin.api;

import br.dev.registro.checkin.domain.CheckinDiario;
import br.dev.registro.checkin.domain.CheckinService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Path("/api/checkins")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CheckinResource {

    private final CheckinService service;

    public CheckinResource(CheckinService service) {
        this.service = service;
    }

    public record CheckinDto(
            LocalDate dataLocal,
            Short energia,
            BigDecimal horasSono,
            Short qualidadeSono,
            Short humor,
            Short estresse,
            Short dificuldadePrevista,
            boolean descansoPlanejado,
            String frase,
            Short dificuldadeFinal,
            String atrapalhou,
            Instant fechadoEm) {

        public static CheckinDto de(CheckinDiario c) {
            return new CheckinDto(
                    c.dataLocal, c.energia, c.horasSono, c.qualidadeSono, c.humor, c.estresse,
                    c.dificuldadePrevista, c.descansoPlanejado, c.frase, c.dificuldadeFinal,
                    c.atrapalhou, c.fechadoEm);
        }
    }

    /** 204 quando o dia ainda nao tem check-in — ausencia e uma resposta valida, nao um erro. */
    @GET
    @Path("{data}")
    public Response porData(@PathParam("data") LocalDate data) {
        CheckinDiario checkin = service.porData(data);
        return checkin == null
                ? Response.noContent().build()
                : Response.ok(CheckinDto.de(checkin)).build();
    }

    @PUT
    @Path("{data}")
    public CheckinDto salvar(@PathParam("data") LocalDate data, @Valid CheckinService.DadosCheckin dados) {
        return CheckinDto.de(service.salvar(data, dados));
    }

    @PUT
    @Path("{data}/fechamento")
    public CheckinDto fechar(
            @PathParam("data") LocalDate data, @Valid CheckinService.DadosFechamento dados) {
        return CheckinDto.de(service.fechar(data, dados));
    }
}
