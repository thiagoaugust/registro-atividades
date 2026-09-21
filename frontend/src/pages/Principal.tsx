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

// Recharts responde por metade do bundle e so serve a esta aba: carrega quando ela abre.
const Analytics = lazy(() =>
  import("@/components/Analytics").then((modulo) => ({ default: modulo.Analytics })),
);
import { PainelDia } from "@/pages/Dia";

const ABAS = [
  { id: "dia", rotulo: "Dia" },
  { id: "inbox", rotulo: "Esclarecer" },
  { id: "listas", rotulo: "Listas" },
  { id: "agora", rotulo: "Agora" },
  { id: "livros", rotulo: "Livros" },
  { id: "cursos", rotulo: "Estudo" },
  { id: "metas", rotulo: "Metas" },
  { id: "revisar", rotulo: "Revisar" },
  { id: "analytics", rotulo: "Evolucao" },
  { id: "conquistas", rotulo: "Conquistas" },
] as const;

type Aba = (typeof ABAS)[number]["id"];

export function Principal({ aoSair }: { aoSair: () => void }) {
  const [aba, setAba] = useState<Aba>("dia");

  const perfil = useQuery({ queryKey: ["perfil"], queryFn: api.perfil });
  const inbox = useQuery({ queryKey: ["inbox"], queryFn: api.inbox });

  return (
    <div className="mx-auto max-w-4xl p-4 sm:p-6">
      <header className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-lg font-semibold">Registro de Atividades</h1>
        <div className="flex items-center gap-2">
          <CapturaRapida pendentes={inbox.data?.pendentes ?? 0} />
          <Button variante="fantasma" tamanho="sm" onClick={aoSair}>
            Sair
          </Button>
        </div>
      </header>

      <nav className="mb-4 flex flex-wrap gap-1">
        {ABAS.map((item) => (
          <Button
            key={item.id}
            variante={aba === item.id ? "secundario" : "fantasma"}
            tamanho="sm"
            onClick={() => setAba(item.id)}
          >
            {item.rotulo}
            {item.id === "inbox" && (inbox.data?.pendentes ?? 0) > 0 && (
              <span className="ml-1 rounded-full bg-amber-500/20 px-1.5 text-xs text-amber-300">
                {inbox.data?.pendentes}
              </span>
            )}
          </Button>
        ))}
      </nav>

      <BarraPerfil perfil={perfil.data} />

      <div className="mt-4">
        {aba === "dia" && <PainelDia />}
        {aba === "inbox" && <Esclarecer />}
        {aba === "listas" && <ListasGtd />}
        {aba === "agora" && <Agora />}
        {aba === "livros" && <Livros />}
        {aba === "cursos" && <Cursos />}
        {aba === "metas" && <Metas />}
        {aba === "revisar" && <RevisaoSemanal />}
        {aba === "analytics" && (
          <Suspense fallback={<p className="text-sm text-zinc-500">Carregando graficos...</p>}>
            <Analytics />
          </Suspense>
        )}
        {aba === "conquistas" && <Conquistas />}
      </div>
    </div>
  );
}
