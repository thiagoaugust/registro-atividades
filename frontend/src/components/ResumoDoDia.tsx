import { Flame, Trophy } from "lucide-react";
import type { PerfilDto } from "@/api";
import { progressoDoNivel } from "@/lib/formato";

/**
 * Nivel, streak e conquistas. E cromo global — vale para o perfil inteiro, nao para a tela aberta —
 * entao fica fino e quieto: uma linha, sem caixa. O elemento ousado da tela e a leitura do dia.
 */
export function BarraPerfil({ perfil }: { perfil: PerfilDto | undefined }) {
  if (!perfil) {
    return null;
  }
  const progresso = progressoDoNivel(perfil.geral);

  return (
    <div className="flex items-center gap-4 text-xs text-giz-fraco">
      <span className="whitespace-nowrap">
        nivel <span className="medida text-giz">{perfil.geral.nivel}</span>
      </span>

      <span className="relative h-px min-w-16 flex-1 bg-risco" title="Progresso no nivel">
        <span
          className="absolute inset-y-0 left-0 bg-aferido"
          style={{ width: `${Math.round(progresso * 100)}%` }}
        />
      </span>

      <span className="medida whitespace-nowrap">
        {perfil.geral.xp} XP · faltam {perfil.geral.xpParaOProximo}
      </span>

      <span className="flex items-center gap-1" title="Dias seguidos com atividade">
        <Flame className="size-3.5 text-latao" />
        <span className="medida">{perfil.streakGeral}</span>
      </span>

      <span className="flex items-center gap-1" title="Conquistas desbloqueadas">
        <Trophy className="size-3.5 text-latao" />
        <span className="medida">
          {perfil.conquistasDesbloqueadas}/{perfil.conquistasTotais}
        </span>
      </span>
    </div>
  );
}
