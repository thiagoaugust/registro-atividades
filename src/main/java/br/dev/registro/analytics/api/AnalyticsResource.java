package br.dev.registro.analytics.api;

import br.dev.registro.analytics.domain.AnalyticsService;
import br.dev.registro.analytics.domain.Granularidade;
import br.dev.registro.analytics.infra.AnalyticsRepository;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;

/**
 * Leituras agregadas. Tudo aqui e somado no banco: os metodos so escolhem o periodo e devolvem o
 * resultado.
 */
@Path("/api/analytics")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
public class AnalyticsResource {

    private final AnalyticsService service;

    public AnalyticsResource(AnalyticsService service) {
        this.service = service;
    }

    /** Um ponto por dia do ano, para o heatmap estilo GitHub. */
    @GET
    @Path("heatmap")
    public List<AnalyticsRepository.DiaHeatmap> heatmap(@QueryParam("ano") Integer ano) {
        return service.heatmap(ano);
    }

    /** Serie do periodo com media movel e comparacao com o periodo anterior de mesmo tamanho. */
    @GET
    @Path("periodo")
    public AnalyticsService.ComparacaoPeriodo periodo(
            @QueryParam("granularidade") Granularidade granularidade,
            @QueryParam("de") LocalDate de,
            @QueryParam("ate") LocalDate ate) {
        return service.periodo(granularidade != null ? granularidade : Granularidade.DIA, de, ate);
    }

    @GET
    @Path("categorias")
    public AnalyticsService.PorCategoria categorias(
            @QueryParam("de") LocalDate de, @QueryParam("ate") LocalDate ate) {
        return service.porCategoria(de, ate);
    }

    @GET
    @Path("correlacoes")
    public AnalyticsService.Correlacoes correlacoes(
            @QueryParam("de") LocalDate de, @QueryParam("ate") LocalDate ate) {
        return service.correlacoes(de, ate);
    }

    /** Pagina de fim de ano: totais, destaques, conquistas e comparacao com o ano anterior. */
    @GET
    @Path("retrospectiva/{ano}")
    public AnalyticsService.Retrospectiva retrospectiva(@PathParam("ano") int ano) {
        return service.retrospectiva(ano);
    }

    @GET
    @Path("gtd")
    public AnalyticsRepository.MetricasGtd gtd(
            @QueryParam("de") LocalDate de, @QueryParam("ate") LocalDate ate) {
        return service.gtd(de, ate);
    }
}
