package br.dev.registro.atividades.infra;

import br.dev.registro.atividades.domain.AreaConhecimento;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class AreaConhecimentoRepository implements PanacheRepository<AreaConhecimento> {

    public List<AreaConhecimento> ativas() {
        return list("ativa = true", Sort.by("ordem").and("nome"));
    }
}
