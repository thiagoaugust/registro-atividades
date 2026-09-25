package br.dev.registro.gtd.domain;

import br.dev.registro.atividades.domain.StatusProjeto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressoProjetoTest {

    private static ProgressoProjeto de(int tarefas, int concluidas) {
        return ProgressoProjeto.de(1L, "Projeto", "resultado", StatusProjeto.ATIVO, tarefas, concluidas);
    }

    @Test
    void percentual_e_o_que_falta() {
        ProgressoProjeto p = de(8, 3);

        assertThat(p.percentual()).isEqualTo(37.5);
        assertThat(p.faltam()).isEqualTo(5);
    }

    @Test
    void projeto_sem_tarefa_nao_tem_percentual() {
        ProgressoProjeto p = de(0, 0);

        assertThat(p.percentual()).isNull();
        assertThat(p.faltam()).isZero();
    }

    @Test
    void tudo_feito_da_cem_por_cento() {
        ProgressoProjeto p = de(4, 4);

        assertThat(p.percentual()).isEqualTo(100.0);
        assertThat(p.faltam()).isZero();
    }

    @Test
    void arredonda_em_uma_casa() {
        assertThat(de(3, 1).percentual()).isEqualTo(33.3);
    }
}
