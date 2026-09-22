import { useQuery } from "@tanstack/react-query";
import { Check, Trophy, X } from "lucide-react";
import { api, type DesafioPeriodicoDto, type EscopoDesafio } from "@/api";
import { Secao } from "@/components/ui/campo";
import { cn } from "@/lib/utils";

const PERIODO: Record<EscopoDesafio, string> = {
  DIARIO: "hoje",
  SEMANAL: "esta semana",
  MENSAL: "este mes",
};

/** Numeros de meta sao inteiros na pratica; 7,5 km lido como "7.5" atrapalha mais que ajuda. */
function numero(valor: number) {
  return Number.isInteger(valor) ? String(valor) : valor.toFixed(1);
}

/**
 * Medidor de um desafio: barra curta, sempre da mesma largura. Iguais e alinhados em coluna, doze
 * deles podem ser comparados de relance — era o que catorze cards com barra de largura total nao
 * permitiam, porque cada barra media uma coisa diferente na mesma extensao de tela.
 */
function Medidor({ fracao, cumprido }: { fracao: number; cumprido: boolean }) {
  return (
    <span className="relative block h-1 w-14 bg-risco" aria-hidden>
      <span
        className={cn("absolute inset-y-0 left-0", cumprido ? "bg-aferido" : "bg-giz-apagado")}
        style={{ width: `${Math.round(Math.min(1, fracao) * 100)}%` }}
      />
    </span>
  );
}

function Desafio({ desafio }: { desafio: DesafioPeriodicoDto }) {
  const cumprido = desafio.status === "CUMPRIDO";
  const perdido = desafio.status === "PERDIDO";

  return (
    <li
      className={cn(
        "grid grid-cols-[1rem_1fr_auto_3.5rem_3.25rem] items-baseline gap-x-3 border-b border-risco/40 py-2 last:border-0",
        perdido && "opacity-50",
      )}
    >
      <span className="translate-y-0.5">
        {cumprido && <Check className="size-3.5 text-aferido" />}
        {perdido && <X className="size-3.5 text-giz-apagado" />}
      </span>

      <div className="min-w-0">
        <p className={cn("truncate text-sm", cumprido ? "text-aferido" : "text-giz")}>
          {desafio.titulo}
        </p>
        {desafio.descricao && (
          <p className="truncate text-xs text-giz-apagado">
            {desafio.descricao}
            {desafio.tipo === "RECORDE" && " · recorde a bater"}
          </p>
        )}
      </div>

      <span className="medida text-right text-xs text-giz-fraco">
        {numero(desafio.progresso)}/{numero(desafio.alvo)}{" "}
        <span className="text-giz-apagado">{desafio.unidade}</span>
      </span>

      <span className="flex justify-end pb-0.5">
        <Medidor fracao={desafio.fracao} cumprido={cumprido} />
      </span>

      <span className="medida text-right text-xs text-giz-apagado">
        {cumprido ? `+${desafio.xp}` : desafio.xp} XP
      </span>
    </li>
  );
}

function Horizonte({
  titulo,
  escopo,
  desafios,
}: {
  titulo: string;
  escopo: EscopoDesafio;
  desafios: DesafioPeriodicoDto[];
}) {
  const cumpridos = desafios.filter((d) => d.status === "CUMPRIDO").length;

  return (
    <section className="flex flex-col gap-1">
      <Secao
        acao={
          <span className="medida whitespace-nowrap text-xs text-giz-apagado">
            {cumpridos}/{desafios.length} {PERIODO[escopo]}
          </span>
        }
      >
        {titulo}
      </Secao>
      {desafios.length === 0 ? (
        <p className="py-2 text-sm text-giz-apagado">Nenhum desafio aberto.</p>
      ) : (
        <ul>
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
    return <p className="text-sm text-giz-fraco">Carregando...</p>;
  }
  if (!painel.data) {
    return <p className="text-sm text-giz-fraco">Nao deu para carregar os desafios.</p>;
  }

  const { diarios, semanais, mensais, historico, trofeusDoAno } = painel.data;
  const mensaisCumpridos = historico.filter(
    (d) => d.escopo === "MENSAL" && d.status === "CUMPRIDO",
  );

  return (
    <div className="flex flex-col gap-6">
      <p className="flex items-baseline gap-2 text-sm text-giz-fraco">
        <Trophy className="size-4 shrink-0 translate-y-0.5 text-latao" />
        <span>
          <span className="leitura text-base text-latao">{trofeusDoAno}</span>{" "}
          {trofeusDoAno === 1 ? "trofeu" : "trofeus"} neste ano. Cada desafio mensal cumprido vale
          um, e o alvo sai do seu proprio historico — entao um trofeu de dezembro custa mais que o
          de janeiro.
        </span>
      </p>

      <Horizonte titulo="hoje" escopo="DIARIO" desafios={diarios} />
      <Horizonte titulo="semana" escopo="SEMANAL" desafios={semanais} />
      <Horizonte titulo="mes" escopo="MENSAL" desafios={mensais} />

      {mensaisCumpridos.length > 0 && (
        <section className="flex flex-col gap-2">
          <Secao>estante de trofeus</Secao>
          <div className="flex flex-wrap gap-x-5 gap-y-1.5">
            {mensaisCumpridos.map((d) => (
              <span
                key={d.id}
                className="flex items-center gap-1.5 text-xs text-latao"
                title={`${numero(d.progresso)} ${d.unidade} (alvo ${numero(d.alvo)})`}
              >
                <Trophy className="size-3" />
                {d.titulo}
                <span className="medida text-latao/70">{d.periodoInicio.slice(0, 7)}</span>
              </span>
            ))}
          </div>
        </section>
      )}

      {historico.length > 0 && (
        <section className="flex flex-col gap-1">
          <Secao>ja fechados</Secao>
          <p className="pb-1 text-xs text-giz-apagado">
            O que passou fica, cumprido ou nao — e assim que da para ver se a regua esta no lugar.
          </p>
          <ul>
            {historico.map((d) => (
              <li
                key={d.id}
                className="grid grid-cols-[1rem_1fr_auto_7rem] items-baseline gap-x-3 border-b border-risco/40 py-1.5 text-xs last:border-0"
              >
                <span className="translate-y-0.5">
                  {d.status === "CUMPRIDO" ? (
                    <Check className="size-3.5 text-aferido" />
                  ) : (
                    <X className="size-3.5 text-giz-apagado" />
                  )}
                </span>
                <span className="truncate text-giz-fraco">{d.titulo}</span>
                <span className="medida text-giz-apagado">{d.periodoInicio}</span>
                <span className="medida text-right text-giz-apagado">
                  {numero(d.progresso)}/{numero(d.alvo)} {d.unidade}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
