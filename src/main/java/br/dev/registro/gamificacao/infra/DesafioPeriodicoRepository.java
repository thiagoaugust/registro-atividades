package br.dev.registro.gamificacao.infra;

import br.dev.registro.gamificacao.domain.DesafioPeriodico;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class DesafioPeriodicoRepository implements PanacheRepository<DesafioPeriodico> {

    /** So os ativos geram periodo novo; os descontinuados ficam pelo historico que ja produziram. */
    public List<DesafioPeriodico> ativos() {
        return list("ativo = true order by escopo, ordem");
    }
}
