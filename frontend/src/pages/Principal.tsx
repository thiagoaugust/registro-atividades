import { Suspense, lazy, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/api";
import { Button } from "@/components/ui/button";
import { BarraPerfil } from "@/components/ResumoDoDia";
import { CapturaRapida } from "@/components/CapturaRapida";
import { Conquistas } from "@/components/Conquistas";
import { Metas } from "@/components/Metas";
import { Esclarecer } from "@/components/Esclarecer";
import { ListasGtd } from "@/components/ListasGtd";
import { Agora } from "@/components/Agora";
import { RevisaoSemanal } from "@/components/RevisaoSemanal";
import { Livros } from "@/components/Livros";
import { Cursos } from "@/components/Cursos";
import { cn } from "@/lib/utils";

// Recharts responde por metade do bundle e so serve a esta secao: carrega quando ela abre.
const Analytics = lazy(() =>
  import("@/components/Analytics").then((modulo) => ({ default: modulo.Analytics })),
);
import { PainelDia } from "@/pages/Dia";

/**
 * Cinco secoes, nao nove abas chapadas. A fileira antiga nao dizia onde se registra o dia — tudo
 * tinha o mesmo peso. Agora o primeiro item e o ato diario, e o que era aba virou subsecao do lugar
 * a que pertence.
 */
const SECOES = [
  { id: "hoje", rotulo: "Hoje" },
  {
    id: "metas",
    rotulo: "Metas",
    partes: [
      { id: "desafios", rotulo: "Desafios e trofeus" },
      { id: "conquistas", rotulo: "Conquistas" },
    ],
  },
  {
    id: "estante",
    rotulo: "Estante",
    partes: [
      { id: "livros", rotulo: "Livros" },
      { id: "cursos", rotulo: "Cursos" },
    ],
  },
  {
    id: "organizar",
    rotulo: "Organizar",
    partes: [
      { id: "esclarecer", rotulo: "Esclarecer" },
      { id: "agora", rotulo: "Agora" },
      { id: "listas", rotulo: "Listas" },
      { id: "revisar", rotulo: "Revisar" },
    ],
  },
  { id: "evolucao", rotulo: "Evolucao" },
] as const;

type Secao = (typeof SECOES)[number]["id"];

const TELAS: Record<string, React.ReactNode> = {
  hoje: <PainelDia />,
  desafios: <Metas />,
  conquistas: <Conquistas />,
  livros: <Livros />,
  cursos: <Cursos />,
  esclarecer: <Esclarecer />,
  agora: <Agora />,
  listas: <ListasGtd />,
  revisar: <RevisaoSemanal />,
};

export function Principal({ aoSair }: { aoSair: () => void }) {
  const [secao, setSecao] = useState<Secao>("hoje");
  // Uma parte lembrada por secao: voltar para Organizar devolve a subsecao onde voce estava.
  const [partes, setPartes] = useState<Record<string, string>>({});

  const perfil = useQuery({ queryKey: ["perfil"], queryFn: api.perfil });
  const inbox = useQuery({ queryKey: ["inbox"], queryFn: api.inbox });
  const pendentes = inbox.data?.pendentes ?? 0;

  const atual = SECOES.find((s) => s.id === secao)!;
  const subsecoes = "partes" in atual ? atual.partes : null;
  const parte = subsecoes ? (partes[secao] ?? subsecoes[0].id) : secao;

  return (
    <div className="mx-auto max-w-4xl px-4 pb-16 sm:px-6">
      <header className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-2 pt-6">
        <h1 className="text-base text-giz-fraco">Registro de atividades</h1>
        <div className="flex items-center gap-2">
          <CapturaRapida pendentes={pendentes} />
          <Button variante="fantasma" tamanho="sm" onClick={aoSair}>
            Sair
          </Button>
        </div>
      </header>

      {/* Navegacao como guias de arquivo: a ativa se liga ao conteudo por um fio que se interrompe. */}
      <nav className="mt-4 flex gap-1 border-b border-risco" aria-label="Secoes">
        {SECOES.map((item) => {
          const ativa = secao === item.id;
          const alerta =
            item.id === "organizar" && pendentes > 0 && !ativa ? pendentes : null;
          return (
            <button
              key={item.id}
              onClick={() => setSecao(item.id)}
              aria-current={ativa ? "page" : undefined}
              className={cn(
                "-mb-px flex items-center gap-1.5 border-b-2 px-3 pb-2 pt-1 text-sm transition-colors",
                ativa
                  ? "border-aferido text-giz"
                  : "border-transparent text-giz-fraco hover:text-giz",
              )}
            >
              {item.rotulo}
              {alerta && <span className="medida text-xs text-latao">{alerta}</span>}
            </button>
          );
        })}
      </nav>

      {subsecoes && (
        <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1">
          {subsecoes.map((item) => (
            <button
              key={item.id}
              onClick={() => setPartes({ ...partes, [secao]: item.id })}
              className={cn(
                "text-[0.8125rem] transition-colors",
                parte === item.id
                  ? "text-giz underline decoration-aferido decoration-2 underline-offset-4"
                  : "text-giz-fraco hover:text-giz",
              )}
            >
              {item.rotulo}
              {item.id === "esclarecer" && pendentes > 0 && (
                <span className="medida ml-1 text-latao">{pendentes}</span>
              )}
            </button>
          ))}
        </div>
      )}

      <div className="mt-5">
        <BarraPerfil perfil={perfil.data} />
      </div>

      <main className="mt-5">
        {secao === "evolucao" ? (
          <Suspense fallback={<p className="text-sm text-giz-fraco">Carregando graficos...</p>}>
            <Analytics />
          </Suspense>
        ) : (
          TELAS[parte]
        )}
      </main>
    </div>
  );
}
