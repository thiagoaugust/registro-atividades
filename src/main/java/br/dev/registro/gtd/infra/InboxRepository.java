package br.dev.registro.gtd.infra;

import br.dev.registro.gtd.domain.InboxItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class InboxRepository implements PanacheRepository<InboxItem> {

    /** Mais antigos primeiro: e a ordem em que o esclarecimento deve atacar a fila. */
    public List<InboxItem> pendentes() {
        return list("processadoEm is null", Sort.by("capturadoEm"));
    }

    public long contarPendentes() {
        return count("processadoEm is null");
    }

    public InboxItem proximoPendente() {
        return find("processadoEm is null", Sort.by("capturadoEm")).firstResult();
    }
}
