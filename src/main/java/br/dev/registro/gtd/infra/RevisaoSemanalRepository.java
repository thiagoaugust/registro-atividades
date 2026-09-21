package br.dev.registro.gtd.infra;

import br.dev.registro.gtd.domain.RevisaoSemanal;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class RevisaoSemanalRepository implements PanacheRepositoryBase<RevisaoSemanal, LocalDate> {

    public List<RevisaoSemanal> concluidas() {
        return list("concluidaEm is not null", Sort.by("semanaInicio").descending());
    }

    public List<RevisaoSemanal> recentes(int limite) {
        return find("order by semanaInicio desc").page(0, limite).list();
    }
}
