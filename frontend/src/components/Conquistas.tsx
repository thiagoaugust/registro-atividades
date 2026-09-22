import { useQuery } from "@tanstack/react-query";
import { Lock, Trophy } from "lucide-react";
import { api } from "@/api";

/** Catalogo com progresso. Desbloqueadas primeiro, depois as que estao mais perto. */
export function Conquistas() {
  const conquistas = useQuery({ queryKey: ["conquistas"], queryFn: api.conquistas });

  if (conquistas.isLoading) {
    return <p className="text-sm text-giz-apagado">Carregando conquistas...</p>;
  }

  return (
    <div className="grid gap-x-8 sm:grid-cols-2">
      {conquistas.data?.map((conquista) => {
        const fracao = Math.min(conquista.progresso / conquista.alvo, 1);
        return (
          <div key={conquista.codigo} className="flex items-start gap-3 border-b border-risco/40 py-2.5">
            {conquista.desbloqueada ? (
              <Trophy className="mt-0.5 size-4 shrink-0 text-latao" />
            ) : (
              <Lock className="mt-0.5 size-4 shrink-0 text-giz-apagado" />
            )}
            <div className="min-w-0 flex-1">
              <p className={conquista.desbloqueada ? "text-sm font-medium" : "text-sm text-giz-fraco"}>
                {conquista.titulo}
              </p>
              <p className="text-xs text-giz-apagado">{conquista.descricao}</p>

              {conquista.desbloqueada ? (
                <p className="mt-1 text-xs text-latao/80">em {conquista.dataLocal}</p>
              ) : (
                <div className="mt-2 flex items-center gap-2">
                  <div className="h-1 flex-1 bg-risco">
                    <div
                      className="h-full bg-giz-apagado"
                      style={{ width: `${Math.round(fracao * 100)}%` }}
                    />
                  </div>
                  <span className="medida text-xs text-giz-apagado">
                    {Math.round(conquista.progresso)}/{Math.round(conquista.alvo)}
                  </span>
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
