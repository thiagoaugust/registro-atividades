package br.dev.registro.gtd.infra;

import br.dev.registro.gtd.domain.Referencia;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReferenciaRepository implements PanacheRepository<Referencia> {
}
