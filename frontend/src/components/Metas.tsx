import { useQuery } from "@tanstack/react-query";
import { Check, Trophy, X } from "lucide-react";
import { api, type DesafioPeriodicoDto, type EscopoDesafio } from "@/api";
import { Card } from "@/components/ui/campo";
import { cn } from "@/lib/utils";

const PERIODO: Record<EscopoDesafio, string> = {
  DIARIO: "hoje",
  SEMANAL: "esta semana",
  MENSAL: "este mes",
};

/** Numeros de meta sao inteiros na prática; 7,5 km lido como "7.5" atrapalha mais que ajuda. */
function numero(valor: number) {
  return Number.isInteger(valor) ? String(valor) : valor.toFixed(1);
}

function Desafio({ desafio }: { desafio: DesafioPeriodicoDto }) {
  const cumprido = desafio.status === "CUMPRIDO";
  const perdido = desafio.status === "PERDIDO";

  return (
    <li
      className={cn(
        "rounded-md border border-zinc-800 bg-zinc-900/40 p-3",
        cumprido && "border-emerald-600/40 bg-emerald-950/20",
        perdido && "opacity-60",
      )}
    >
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="flex items-center gap-1.5 text-sm font-medium">
            {cumprido && <Check className="size-3.5 shrink-0 text-emerald-400" />}
            {perdido && <X className="size-3.5 shrink-0 text-zinc-500" />}
            {desafio.titulo}
          </p>
          {desafio.descricao && <p className="text-xs text-zinc-500">{desafio.descricao}</p>}
        </div>
        <span className="shrink-0 text-xs text-zinc-500">
          {cumprido ? `+${desafio.xp} XP` : `${desafio.xp} XP`}
        </span>
      </div>

      <div className="mt-2 h-1.5 w-full overflow-hidden rounded-full bg-zinc-800">
        <div
          className={cn(
            "h-full rounded-full transition-all",
            cumprido ? "bg-emerald-500" : "bg-zinc-500",
          )}
          style={{ width: `${Math.round(desafio.fracao * 100)}%` }}
        />
      </div>

      <p className="mt-1 text-xs text-zinc-500">
        {numero(desafio.progresso)} de {numero(desafio.alvo)} {desafio.unidade}
        {desafio.tipo === "RECORDE" && " · recorde a bater"}
      </p>
    </li>
  );
}

function Horizonte({ titulo, escopo, desafios }: {
  titulo: string;
  escopo: EscopoDesafio;
  desafios: DesafioPeriodicoDto[];
}) {
  const cumpridos = desafios.filter((d) => d.status === "CUMPRIDO").length;

  return (
    <section className="flex flex-col gap-2">
      <div className="flex items-baseline justify-between">
        <h2 className="text-sm font-medium">{titulo}</h2>
        <span className="text-xs text-zinc-500">
          {cumpridos}/{desafios.length} {PERIODO[escopo]}
        </span>
      </div>
      {desafios.length === 0 ? (
        <Card className="text-sm text-zinc-500">Nenhum desafio aberto.</Card>
      ) : (
        <ul className="flex flex-col gap-2">
          {desafios.map((desafio) => (
            <Desafio key={desafio.id} desafio={desafio} />
          ))}
        </ul>
      )}
    </section>
  );
}

/**
 * Os tres horizontes numa tela so. As metas nao sao escolhidas: cada periodo nasce com o alvo
 * calibrado sobre os seus periodos anteriores, entao o que era dificil em marco e o normal de
 * junho — e a barra sobe junto.
 */
export function Metas() {
  const painel = useQuery({ queryKey: ["desafios"], queryFn: api.desafiosPeriodicos });

  if (painel.isLoading) {
    return <p className="text-sm text-zinc-500">Carregando...</p>;
  }
  if (!painel.data) {
    return <Card className="text-sm text-zinc-500">Nao deu para carregar os desafios.</Card>;
  }

  const { diarios, semanais, mensais, historico, trofeusDoAno } = painel.data;
  const mensaisCumpridos = historico.filter(
    (d) => d.escopo === "MENSAL" && d.status === "CUMPRIDO",
  );

  return (
    <div className="flex flex-col gap-6">
      <Card className="flex items-center gap-3">
        <Trophy className="size-5 text-amber-400" />
        <div>
          <p className="text-sm font-medium">
            {trofeusDoAno} {trofeusDoAno === 1 ? "trofeu" : "trofeus"} neste ano
          </p>
          <p className="text-xs text-zinc-500">
            Cada desafio mensal cumprido vale um. O alvo sai do seu proprio historico, entao um
            trofeu de dezembro custa mais que o de janeiro.
          </p>
        </div>
      </Card>

      <Horizonte titulo="Hoje" escopo="DIARIO" desafios={diarios} />
      <Horizonte titulo="Semana" escopo="SEMANAL" desafios={semanais} />
      <Horizonte titulo="Mes" escopo="MENSAL" desafios={mensais} />

      {mensaisCumpridos.length > 0 && (
        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-medium">Estante de trofeus</h2>
          <div className="flex flex-wrap gap-2">
            {mensaisCumpridos.map((d) => (
              <span
                key={d.id}
                className="flex items-center gap-1.5 rounded-full border border-amber-500/30 bg-amber-500/10 px-2.5 py-1 text-xs text-amber-200"
                title={`${numero(d.progresso)} ${d.unidade} (alvo ${numero(d.alvo)})`}
              >
                <Trophy className="size-3" />
                {d.titulo} · {d.periodoInicio.slice(0, 7)}
              </span>
            ))}
          </div>
        </section>
      )}

      {historico.length > 0 && (
        <section className="flex flex-col gap-2">
          <h2 className="text-sm font-medium">Ja fechados</h2>
          <p className="text-xs text-zinc-500">
            O que passou fica, cumprido ou nao — e assim que da para ver se a regua esta no lugar.
          </p>
          <ul className="flex flex-col gap-1">
            {historico.map((d) => (
              <li
                key={d.id}
                className="flex items-center justify-between gap-3 border-b border-zinc-900 py-1.5 text-xs"
              >
                <span className="flex min-w-0 items-center gap-1.5">
                  {d.status === "CUMPRIDO" ? (
                    <Check className="size-3.5 shrink-0 text-emerald-400" />
                  ) : (
                    <X className="size-3.5 shrink-0 text-zinc-600" />
                  )}
                  <span className="truncate">{d.titulo}</span>
                </span>
                <span className="shrink-0 text-zinc-500">
                  {d.periodoInicio} · {numero(d.progresso)}/{numero(d.alvo)} {d.unidade}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
