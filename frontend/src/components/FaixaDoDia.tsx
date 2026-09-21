import { BatteryLow, BatteryMedium, BatteryFull, Gauge } from "lucide-react";
import type { FaixaDto } from "@/api";
import { Card } from "@/components/ui/campo";
import { Ajuda } from "@/components/ui/campo";

const ICONE = {
  BAIXA: BatteryLow,
  NORMAL: BatteryMedium,
  ALTA: BatteryFull,
};

/**
 * A regua do dia. Nao e uma meta escolhida: e o intervalo em que caem metade dos seus proprios dias
 * de energia parecida, entao ficar dentro dele e o esperado — e um dia de energia 1 cobra menos que
 * um dia de energia 5.
 */
export function FaixaDoDia({ faixa }: { faixa: FaixaDto | undefined }) {
  if (!faixa) {
    return null;
  }

  const Icone = faixa.banda ? ICONE[faixa.banda] : Gauge;

  if (faixa.situacao === "CALIBRANDO") {
    return (
      <Card className="flex items-center gap-2 text-xs text-zinc-500">
        <Icone className="size-4 shrink-0" />
        <span>
          Calibrando a faixa deste tipo de dia — {faixa.diasComparaveis}{" "}
          {faixa.diasComparaveis === 1 ? "dia comparavel" : "dias comparaveis"} ate agora.
        </span>
        <Ajuda texto="A faixa sai dos seus proprios dias de energia parecida. Com menos de cinco, o numero seria chute, entao o sistema nao cobra nada ainda." />
      </Card>
    );
  }

  // A escala precisa caber o teto e tambem um dia que passou muito dele.
  const escala = Math.max(faixa.teto * 1.25, faixa.xpDoDia * 1.1, 1);
  const porcento = (valor: number) => `${Math.min(100, (valor / escala) * 100)}%`;

  const mensagem = {
    ABAIXO: `Faltam ${faixa.faltaParaOPiso} XP para a faixa de hoje.`,
    DENTRO: "Dentro da faixa de hoje.",
    ACIMA: "Acima da faixa — dia que rendeu.",
  }[faixa.situacao];

  const cor = {
    ABAIXO: "text-zinc-400",
    DENTRO: "text-emerald-400",
    ACIMA: "text-amber-300",
  }[faixa.situacao];

  return (
    <Card className="flex flex-col gap-2">
      <div className="flex items-center justify-between gap-2 text-xs">
        <span className="flex items-center gap-1.5 text-zinc-400">
          <Icone className="size-4" />
          {faixa.rotulo}
          <Ajuda
            texto={`Faixa calculada sobre ${faixa.diasComparaveis} dias seus de energia parecida: metade deles ficou entre ${faixa.piso} e ${faixa.teto} XP. Nao e meta escolhida, e o seu normal.`}
          />
        </span>
        <span className={cor}>{mensagem}</span>
      </div>

      <div className="relative h-2.5 w-full rounded-full bg-zinc-800">
        <div
          className="absolute inset-y-0 rounded-full bg-emerald-500/25"
          style={{ left: porcento(faixa.piso), width: porcento(faixa.teto - faixa.piso) }}
        />
        {/* O tipico e uma marca, nao uma borda: passar dele nao muda o desfecho do dia. */}
        <div
          className="absolute inset-y-0 w-px bg-emerald-500/60"
          style={{ left: porcento(faixa.tipico) }}
        />
        <div
          className="absolute -top-0.5 h-3.5 w-1 rounded-full bg-zinc-100"
          style={{ left: porcento(faixa.xpDoDia) }}
          aria-hidden
        />
      </div>

      <div className="flex justify-between text-xs text-zinc-500">
        <span>{faixa.xpDoDia} XP hoje</span>
        <span>
          faixa {faixa.piso}–{faixa.teto} XP
        </span>
      </div>
    </Card>
  );
}
