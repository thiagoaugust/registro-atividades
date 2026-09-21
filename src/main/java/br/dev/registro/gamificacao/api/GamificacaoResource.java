package br.dev.registro.gamificacao.api;

import br.dev.registro.gamificacao.domain.FaixaService;
import br.dev.registro.gamificacao.domain.PerfilService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.util.List;

@Path("/api/gamificacao")
@RolesAllowed("user")
@Produces(MediaType.APPLICATION_JSON)
public class GamificacaoResource {

    private final PerfilService service;
    private final FaixaService faixas;

    public GamificacaoResource(PerfilService service, FaixaService faixas) {
        this.service = service;
        this.faixas = faixas;
    }

    @GET
    @Path("perfil")
    public PerfilService.PerfilDto perfil() {
        return service.perfil();
    }

    /** Quanto e um dia justo nesta data, dada a energia com que ela comecou. */
    @GET
    @Path("faixa/{data}")
    public FaixaService.FaixaDto faixa(@PathParam("data") LocalDate data) {
        return faixas.doDia(data);
    }

    @GET
    @Path("conquistas")
    public List<PerfilService.ConquistaDto> conquistas() {
        return service.conquistas();
    }
}
