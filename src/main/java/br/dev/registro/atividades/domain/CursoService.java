package br.dev.registro.atividades.domain;

import br.dev.registro.atividades.infra.AreaConhecimentoRepository;
import br.dev.registro.atividades.infra.CursoRepository;
import br.dev.registro.atividades.infra.ProgressoCursoRepository;
import br.dev.registro.atividades.infra.RegistroRepository;
import br.dev.registro.comum.RecursoNaoEncontradoException;
import br.dev.registro.comum.RegraNegocioException;
import br.dev.registro.comum.Relogio;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cadastro de cursos e o que se sabe sobre a dedicacao a cada um. */
@ApplicationScoped
public class CursoService {

    private final CursoRepository cursos;
    private final AreaConhecimentoRepository areas;
    private final RegistroRepository registros;
    private final ProgressoCursoRepository progressos;
    private final Relogio relogio;

    public CursoService(
            CursoRepository cursos,
            AreaConhecimentoRepository areas,
            RegistroRepository registros,
            ProgressoCursoRepository progressos,
            Relogio relogio) {
        this.cursos = cursos;
        this.areas = areas;
        this.registros = registros;
        this.progressos = progressos;
        this.relogio = relogio;
    }

    public record DadosCurso(
            @NotBlank @Size(max = 300) String titulo,
            @Size(max = 200) String instituicao,
            @Size(max = 1000) String url,
            @Positive BigDecimal cargaHoraria,
            Long areaId,
            StatusCurso status,
            /** Preenchidos so para curso feito antes de o sistema existir. */
            @Positive BigDecimal horasRetroativas,
            @Positive Integer diasRetroativos,
            LocalDate concluidoEm) {
    }

    public record EstudoPorArea(
            List<ProgressoCursoRepository.TempoPorArea> porArea,
            List<ProgressoCursoRepository.TempoPorTema> porTema,
            ProgressoCursoRepository.ResumoPratica pratica) {
    }

    public List<Curso> listar() {
        return cursos.listar();
    }

    public Curso porId(long id) {
        return cursos.porIdComArea(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Curso", id));
    }

    public List<ProgressoCurso> progresso() {
        LocalDate hoje = relogio.hoje();
        return progressos.agregados(hoje).stream()
                .map(agregado -> ProgressoCurso.de(agregado, hoje))
                .toList();
    }

    /** Onde o tempo de estudo foi parar: por area, por tema, e quanto disso foi pratica. */
    public EstudoPorArea onde(LocalDate de, LocalDate ate) {
        LocalDate fim = ate != null ? ate : relogio.hoje();
        LocalDate inicio = de != null ? de : fim.minusDays(29);
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("o fim do periodo e anterior ao inicio");
        }
        return new EstudoPorArea(
                progressos.tempoPorArea(),
                progressos.tempoPorTema(inicio, fim),
                progressos.resumoPratica(inicio, fim));
    }

    @Transactional
    public Curso criar(DadosCurso dados) {
        Curso curso = new Curso();
        aplicar(dados, curso);
        cursos.persist(curso);
        return curso;
    }

    @Transactional
    public Curso atualizar(long id, DadosCurso dados) {
        Curso curso = porId(id);
        aplicar(dados, curso);
        return curso;
    }

    private void aplicar(DadosCurso dados, Curso curso) {
        curso.titulo = dados.titulo();
        curso.instituicao = dados.instituicao();
        curso.url = dados.url();
        curso.cargaHoraria = dados.cargaHoraria();
        curso.horasRetroativas = dados.horasRetroativas();
        curso.diasRetroativos = dados.diasRetroativos();

        curso.area = dados.areaId() == null
                ? null
                : areas.findByIdOptional(dados.areaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Area", dados.areaId()));

        // Curso retroativo ja nasce concluido: quem informa as horas esta cadastrando historico.
        StatusCurso novo = dados.status() != null
                ? dados.status()
                : (curso.horasRetroativas != null ? StatusCurso.CONCLUIDO : curso.status);

        if (novo == StatusCurso.CONCLUIDO) {
            if (dados.concluidoEm() != null) {
                curso.concluidoEm = dados.concluidoEm();
            } else if (curso.concluidoEm == null) {
                curso.concluidoEm = relogio.hoje();
            }
            if (curso.concluidoEm.isAfter(relogio.hoje())) {
                throw new RegraNegocioException("a data de conclusao nao pode estar no futuro");
            }
        } else {
            curso.concluidoEm = null;
        }
        curso.status = novo;
    }

    @Transactional
    public void excluir(long id) {
        Curso curso = porId(id);
        long vinculados = registros.count("curso.id = ?1", id);
        if (vinculados > 0) {
            throw new RegraNegocioException(
                    "o curso tem %d registro(s) vinculado(s); exclua ou desvincule antes"
                            .formatted(vinculados));
        }
        cursos.delete(curso);
    }
}
