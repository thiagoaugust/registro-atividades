package br.dev.registro.checkin.infra;

import br.dev.registro.checkin.domain.CheckinDiario;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;

@ApplicationScoped
public class CheckinRepository implements PanacheRepositoryBase<CheckinDiario, LocalDate> {
}
