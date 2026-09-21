import { useQuery } from "@tanstack/react-query";
import { Lock, Trophy } from "lucide-react";
import { api } from "@/api";
import { Card } from "@/components/ui/campo";

/** Catalogo com progresso. Desbloqueadas primeiro, depois as que estao mais perto. */
export function Conquistas() {
  const conquistas = useQuery({ queryKey: ["conquistas"], queryFn: api.conquistas });

  if (conquistas.isLoading) {
    return <p className="text-sm text-zinc-500">Carregando conquistas...</p>;
  }

  return (
    <div className="grid gap-2 sm:grid-cols-2">
      {conquistas.data?.map((conquista) => {
        const fracao = Math.min(conquista.progresso / conquista.alvo, 1);
        return (
          <Card key={conquista.codigo} className="flex items-start gap-3">
            {conquista.desbloqueada ? (
              <Trophy className="mt-0.5 size-4 shrink-0 text-amber-400" />
            ) : (
              <Lock className="mt-0.5 size-4 shrink-0 text-zinc-600" />
            )}
            <div className="min-w-0 flex-1">
              <p className={conquista.desbloqueada ? "text-sm font-medium" : "text-sm text-zinc-400"}>
                {conquista.titulo}
              </p>
              <p className="text-xs text-zinc-500">{conquista.descricao}</p>

              {conquista.desbloqueada ? (
                <p className="mt-1 text-xs text-amber-400/80">em {conquista.dataLocal}</p>
              ) : (
                <div className="mt-2 flex items-center gap-2">
                  <div className="h-1 flex-1 overflow-hidden rounded-full bg-zinc-800">
                    <div
                      className="h-full rounded-full bg-zinc-600"
                      style={{ width: `${Math.round(fracao * 100)}%` }}
                    />
                  </div>
                  <span className="text-xs text-zinc-600">
                    {Math.round(conquista.progresso)}/{Math.round(conquista.alvo)}
                  </span>
                </div>
              )}
            </div>
          </Card>
        );
      })}
    </div>
  );
}
