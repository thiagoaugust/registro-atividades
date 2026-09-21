package br.dev.registro.gtd.infra;

import br.dev.registro.gtd.domain.Contexto;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ContextoRepository implements PanacheRepository<Contexto> {

    public List<Contexto> ativos() {
        return list("ativo = true", Sort.by("ordem").and("nome"));
    }
}
