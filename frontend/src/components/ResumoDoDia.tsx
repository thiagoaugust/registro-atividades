import { Flame, Trophy, Swords } from "lucide-react";
import type { PerfilDto, ResumoDto } from "@/api";
import { Card } from "@/components/ui/campo";
import { CORES_CLASSIFICACAO, ROTULO_CLASSIFICACAO, progressoDoNivel } from "@/lib/formato";

/** Barra de nivel, streak e conquistas — fica no topo, valendo para o perfil inteiro. */
export function BarraPerfil({ perfil }: { perfil: PerfilDto | undefined }) {
  if (!perfil) {
    return null;
  }
  const progresso = progressoDoNivel(perfil.geral);

  return (
    <Card className="flex flex-wrap items-center gap-x-6 gap-y-3">
      <div className="min-w-40 flex-1">
        <div className="flex items-baseline justify-between">
          <span className="text-sm font-medium">Nivel {perfil.geral.nivel}</span>
          <span className="text-xs text-zinc-500">
            {perfil.geral.xp} XP · faltam {perfil.geral.xpParaOProximo}
          </span>
        </div>
        <div className="mt-1.5 h-1.5 w-full overflow-hidden rounded-full bg-zinc-800">
          <div
            className="h-full rounded-full bg-emerald-500 transition-all"
            style={{ width: `${Math.round(progresso * 100)}%` }}
          />
        </div>
      </div>

      <span className="flex items-center gap-1.5 text-sm" title="Dias seguidos com atividade">
        <Flame className="size-4 text-amber-400" />
        {perfil.streakGeral}
      </span>

      <span className="flex items-center gap-1.5 text-sm" title="Conquistas desbloqueadas">
        <Trophy className="size-4 text-amber-400" />
        {perfil.conquistasDesbloqueadas}/{perfil.conquistasTotais}
      </span>
    </Card>
  );
}

export function ResumoDoDia({ resumo }: { resumo: ResumoDto | null }) {
  if (!resumo || (!resumo.presenca && resumo.xpTotal === 0)) {
    return null;
  }

  return (
    <div className="flex flex-wrap items-center gap-2 text-sm">
      <span className={`rounded-full border px-2.5 py-1 text-xs ${CORES_CLASSIFICACAO[resumo.classificacao]}`}>
        {ROTULO_CLASSIFICACAO[resumo.classificacao]} · {resumo.indiceProdutividade.toFixed(1)}
      </span>

      <span className="rounded-full border border-zinc-800 bg-zinc-900 px-2.5 py-1 text-xs text-zinc-300">
        {resumo.xpTotal} XP
      </span>

      {resumo.diaDificilVencido && (
        <span
          className="flex items-center gap-1 rounded-full border border-amber-500/30 bg-amber-500/15 px-2.5 py-1 text-xs text-amber-300"
          title="Dia adverso em que voce entregou mesmo assim"
        >
          <Swords className="size-3.5" />
          Dia dificil vencido
        </span>
      )}

      {resumo.descanso && (
        <span className="rounded-full border border-sky-500/30 bg-sky-500/15 px-2.5 py-1 text-xs text-sky-300">
          Descanso planejado
        </span>
      )}

      {resumo.baselineInsuficiente && (
        <span className="text-xs text-zinc-600" title="Menos de 7 dias de historico para comparar">
          indice provisorio
        </span>
      )}
    </div>
  );
}
