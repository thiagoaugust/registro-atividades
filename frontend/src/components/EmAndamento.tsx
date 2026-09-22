import { type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { BookOpen, GraduationCap, Hammer, Plus, X } from "lucide-react";
import { api, type Categoria } from "@/api";
import { Button } from "@/components/ui/button";
import { Secao } from "@/components/ui/campo";

/** O que o botao manda para o formulario: a categoria certa e o vinculo ja escolhido. */
export interface Atalho {
  categoria: Categoria;
  vinculoId?: string;
  cursoId?: string;
}

interface Linha {
  chave: string;
  titulo: string;
  detalhe: string;
  atalho: Atalho;
}

function Grupo({
  titulo,
  Icone,
  linhas,
  aberta,
  formulario,
  aoRegistrar,
  aoFechar,
}: {
  titulo: string;
  Icone: typeof BookOpen;
  linhas: Linha[];
  aberta: string | null;
  formulario: ReactNode;
  aoRegistrar: (atalho: Atalho, chave: string) => void;
  aoFechar: () => void;
}) {
  if (linhas.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-col gap-1.5">
      <Secao>
        <span className="flex items-center gap-1.5">
          <Icone className="size-3.5" />
          {titulo}
        </span>
      </Secao>
      {linhas.map((linha) => (
        <div key={linha.chave} className="flex flex-col gap-2 border-b border-risco/40 pb-1.5 last:border-0">
          <div className="flex items-center gap-3">
            <div className="flex min-w-0 flex-1 items-baseline justify-between gap-3">
              <span className="truncate text-sm text-giz">{linha.titulo}</span>
              <span className="medida shrink-0 text-xs text-giz-apagado">{linha.detalhe}</span>
            </div>
            {aberta === linha.chave ? (
              <Button
                variante="fantasma"
                tamanho="sm"
                aria-label={`Fechar o registro de ${linha.titulo}`}
                onClick={aoFechar}
              >
                <X className="size-3.5" />
                Fechar
              </Button>
            ) : (
              <Button
                variante="fantasma"
                tamanho="sm"
                aria-label={`Registrar sessao de ${linha.titulo}`}
                onClick={() => aoRegistrar(linha.atalho, linha.chave)}
              >
                <Plus className="size-3.5" />
                Registrar
              </Button>
            )}
          </div>

          {/* O formulario nasce aqui, nao no fim da pagina: registrar e a acao do item que voce
              acabou de clicar, e rolar para procurar o formulario e o que tornava isso chato. */}
          {aberta === linha.chave && <div className="border-l-2 border-risco-forte pl-3">{formulario}</div>}
        </div>
      ))}
    </div>
  );
}

/**
 * O que esta aberto agora — livro, projeto, curso — cada um com o botao que abre o formulario ja
 * apontado para ele. Sem isto, registrar exige lembrar a categoria, achar o vinculo no select e
 * conferir em que pagina a leitura parou; o atalho resolve os tres.
 */
export function EmAndamento({
  aberta,
  formulario,
  aoRegistrar,
  aoFechar,
}: {
  /** Chave da linha com o formulario aberto, ou null. */
  aberta: string | null;
  formulario: ReactNode;
  aoRegistrar: (atalho: Atalho, chave: string) => void;
  aoFechar: () => void;
}) {
  const livros = useQuery({ queryKey: ["progresso-livros"], queryFn: api.progressoLivros });
  const cursos = useQuery({ queryKey: ["progresso-cursos"], queryFn: api.progressoCursos });
  const projetos = useQuery({ queryKey: ["projetos"], queryFn: api.projetos });

  const lendo: Linha[] = (livros.data ?? [])
    .filter((l) => l.status === "LENDO")
    .map((l) => ({
      chave: `livro-${l.livroId}`,
      titulo: l.titulo,
      detalhe:
        l.ultimaPagina > 0
          ? `parou na pagina ${l.ultimaPagina}${l.totalPaginas ? ` de ${l.totalPaginas}` : ""}`
          : "nao comecou",
      atalho: { categoria: "LEITURA", vinculoId: String(l.livroId) },
    }));

  const cursando: Linha[] = (cursos.data ?? [])
    .filter((c) => c.status === "CURSANDO" && !c.retroativo)
    .map((c) => ({
      chave: `curso-${c.cursoId}`,
      titulo: c.titulo,
      detalhe:
        c.horasRestantes !== null
          ? `faltam ${c.horasRestantes.toFixed(1)}h`
          : `${Math.round(c.minutos / 60)}h feitas`,
      atalho: { categoria: "ESTUDO", cursoId: String(c.cursoId) },
    }));

  const ativos: Linha[] = (projetos.data ?? [])
    .filter((p) => p.status === "ATIVO")
    .map((p) => ({
      chave: `projeto-${p.id}`,
      titulo: p.titulo,
      detalhe: p.resultadoDesejado ?? "em andamento",
      atalho: { categoria: "PROJETO", vinculoId: String(p.id) },
    }));

  if (lendo.length === 0 && cursando.length === 0 && ativos.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-col gap-5">
      {[
        { titulo: "Lendo", Icone: BookOpen, linhas: lendo },
        { titulo: "Cursando", Icone: GraduationCap, linhas: cursando },
        { titulo: "Projetos", Icone: Hammer, linhas: ativos },
      ].map((grupo) => (
        <Grupo
          key={grupo.titulo}
          {...grupo}
          aberta={aberta}
          formulario={formulario}
          aoRegistrar={aoRegistrar}
          aoFechar={aoFechar}
        />
      ))}
    </div>
  );
}
