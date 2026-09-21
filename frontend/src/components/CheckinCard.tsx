import { useEffect, useState } from "react";
import { Moon, Check } from "lucide-react";
import type { CheckinDto, DadosCheckin } from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Textarea } from "@/components/ui/input";
import { Campo, Card } from "@/components/ui/campo";
import { cn } from "@/lib/utils";

/** Escala de 1 a 5 em botoes: um clique, sem dropdown, sem arrastar. */
function Escala({
  valor,
  aoMudar,
  rotulos,
}: {
  valor: number | null;
  aoMudar: (valor: number | null) => void;
  rotulos?: [string, string];
}) {
  return (
    <div className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          // Clicar de novo no mesmo valor limpa: todo item do check-in e opcional.
          onClick={() => aoMudar(valor === n ? null : n)}
          className={cn(
            "h-8 w-8 rounded-md border text-sm transition-colors",
            valor === n
              ? "border-emerald-500 bg-emerald-600 text-white"
              : "border-zinc-800 bg-zinc-900 text-zinc-400 hover:border-zinc-700",
          )}
          aria-label={rotulos ? `${n} de 5 (${rotulos[0]} a ${rotulos[1]})` : `${n} de 5`}
        >
          {n}
        </button>
      ))}
      {rotulos && (
        <span className="ml-2 text-xs text-zinc-600">
          {rotulos[0]} &rarr; {rotulos[1]}
        </span>
      )}
    </div>
  );
}

interface Props {
  data: string;
  checkin: CheckinDto | null;
  aoSalvar: (dados: DadosCheckin) => void;
  aoFechar: (dados: { dificuldadeFinal?: number | null; atrapalhou?: string | null }) => void;
  salvando?: boolean;
}

export function CheckinCard({ data, checkin, aoSalvar, aoFechar, salvando }: Props) {
  const [aberto, setAberto] = useState(false);
  const [energia, setEnergia] = useState<number | null>(null);
  const [horasSono, setHorasSono] = useState("");
  const [qualidadeSono, setQualidadeSono] = useState<number | null>(null);
  const [humor, setHumor] = useState<number | null>(null);
  const [estresse, setEstresse] = useState<number | null>(null);
  const [dificuldade, setDificuldade] = useState<number | null>(null);
  const [descanso, setDescanso] = useState(false);
  const [frase, setFrase] = useState("");

  const [dificuldadeFinal, setDificuldadeFinal] = useState<number | null>(null);
  const [atrapalhou, setAtrapalhou] = useState("");

  useEffect(() => {
    setEnergia(checkin?.energia ?? null);
    setHorasSono(checkin?.horasSono != null ? String(checkin.horasSono) : "");
    setQualidadeSono(checkin?.qualidadeSono ?? null);
    setHumor(checkin?.humor ?? null);
    setEstresse(checkin?.estresse ?? null);
    setDificuldade(checkin?.dificuldadePrevista ?? null);
    setDescanso(checkin?.descansoPlanejado ?? false);
    setFrase(checkin?.frase ?? "");
    setDificuldadeFinal(checkin?.dificuldadeFinal ?? null);
    setAtrapalhou(checkin?.atrapalhou ?? "");
  }, [checkin, data]);

  function salvar(evento: React.FormEvent) {
    evento.preventDefault();
    aoSalvar({
      energia,
      horasSono: horasSono.trim() === "" ? null : Number(horasSono),
      qualidadeSono,
      humor,
      estresse,
      dificuldadePrevista: dificuldade,
      descansoPlanejado: descanso,
      frase: frase.trim() === "" ? null : frase.trim(),
    });
  }

  const preenchido = checkin != null;

  return (
    <Card>
      <button
        type="button"
        className="flex w-full items-center justify-between text-left"
        onClick={() => setAberto(!aberto)}
      >
        <span className="flex items-center gap-2 text-sm font-medium">
          Check-in do dia
          {preenchido && <Check className="size-4 text-emerald-400" />}
          {checkin?.descansoPlanejado && <Moon className="size-4 text-sky-400" />}
        </span>
        <span className="text-xs text-zinc-500">
          {aberto ? "fechar" : preenchido ? "editar" : "preencher"}
        </span>
      </button>

      {!aberto && preenchido && (
        <p className="mt-2 text-xs text-zinc-500">
          {[
            checkin.energia && `energia ${checkin.energia}`,
            checkin.qualidadeSono && `sono ${checkin.qualidadeSono}`,
            checkin.humor && `humor ${checkin.humor}`,
            checkin.dificuldadeFinal
              ? `dificuldade ${checkin.dificuldadeFinal} (fechado)`
              : checkin.dificuldadePrevista && `dificuldade ${checkin.dificuldadePrevista}`,
          ]
            .filter(Boolean)
            .join(" · ") || "sem respostas"}
        </p>
      )}

      {aberto && (
        <form className="mt-4 flex flex-col gap-4" onSubmit={salvar}>
          <div className="grid gap-4 sm:grid-cols-2">
            <Campo
              rotulo="Energia ao acordar"
              ajuda="Como voce acordou, antes de o dia acontecer. Entra no calculo do indice: entregar com energia baixa vale mais do que entregar descansado."
            >
              <Escala valor={energia} aoMudar={setEnergia} rotulos={["zerado", "inteiro"]} />
            </Campo>
            <Campo
              rotulo="Qualidade do sono"
              ajuda="Se o sono restaurou, independente das horas. Dormir 8h mal vale menos que 6h bem, e o painel cruza isso com o indice do dia."
            >
              <Escala valor={qualidadeSono} aoMudar={setQualidadeSono} rotulos={["pessima", "otima"]} />
            </Campo>
            <Campo
              rotulo="Humor"
              ajuda="Como voce esta se sentindo hoje. Compoe o contexto do dia junto com energia, sono e estresse."
            >
              <Escala valor={humor} aoMudar={setHumor} rotulos={["ruim", "otimo"]} />
            </Campo>
            <Campo
              rotulo="Estresse"
              ajuda="O quanto voce esta sob pressao. Quanto maior, mais adverso o dia — e mais peso tem o que voce conseguir fazer nele."
            >
              <Escala valor={estresse} aoMudar={setEstresse} rotulos={["calmo", "no limite"]} />
            </Campo>
            <Campo
              rotulo="Dificuldade prevista"
              ajuda="O quanto voce espera que este dia seja duro. E o item de maior peso no contexto: um dia previsto como pesado em que voce entrega vira dia dificil vencido."
            >
              <Escala valor={dificuldade} aoMudar={setDificuldade} rotulos={["tranquilo", "pesado"]} />
            </Campo>
            <Campo
              rotulo="Horas de sono"
              ajuda="Quantas horas voce dormiu. Nao entra no indice do dia; serve para a correlacao entre sono e produtividade no painel Evolucao."
            >
              <Input
                type="number"
                step="0.5"
                min={0}
                max={24}
                value={horasSono}
                onChange={(e) => setHorasSono(e.target.value)}
                className="w-28"
              />
            </Campo>
          </div>

          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={descanso}
              onChange={(e) => setDescanso(e.target.checked)}
              className="size-4 accent-emerald-500"
            />
            Descanso planejado (nao quebra a streak)
          </label>

          <Campo
            rotulo="Uma frase sobre o dia"
            ajuda="Uma linha para voce reconhecer o dia quando reler daqui a meses. O numero nao lembra que voce estava doente."
          >
            <Textarea rows={2} value={frase} onChange={(e) => setFrase(e.target.value)} />
          </Campo>

          <Button type="submit" disabled={salvando}>
            Salvar check-in
          </Button>

          <div className="flex flex-col gap-3 border-t border-zinc-800 pt-4">
            <p className="text-xs font-medium uppercase tracking-wide text-zinc-400">
              Fechamento do dia (opcional)
            </p>
            <Campo
              rotulo="Como foi de verdade"
              ajuda="A dificuldade com o dia ja vivido. Quando preenchida, substitui a prevista no calculo: o julgamento do fim do dia vale mais que a expectativa da manha."
            >
              <Escala
                valor={dificuldadeFinal}
                aoMudar={setDificuldadeFinal}
                rotulos={["tranquilo", "pesado"]}
              />
            </Campo>
            <Campo
              rotulo="O que atrapalhou"
              ajuda="O que tirou o dia do rumo. Nao entra em nenhuma conta; e memoria para a revisao semanal."
            >
              <Textarea rows={2} value={atrapalhou} onChange={(e) => setAtrapalhou(e.target.value)} />
            </Campo>
            <Button
              type="button"
              variante="secundario"
              disabled={salvando}
              onClick={() =>
                aoFechar({
                  dificuldadeFinal,
                  atrapalhou: atrapalhou.trim() === "" ? null : atrapalhou.trim(),
                })
              }
            >
              Fechar o dia
            </Button>
          </div>
        </form>
      )}
    </Card>
  );
}
