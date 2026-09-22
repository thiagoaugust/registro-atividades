import { ChevronLeft, ChevronRight, Moon, Swords } from "lucide-react";
import type { FaixaDto, ResumoDto } from "@/api";
import { Ajuda } from "@/components/ui/campo";
import { Button } from "@/components/ui/button";
import { CORES_CLASSIFICACAO, ROTULO_CLASSIFICACAO, formatarDuracao, rotuloDoDia } from "@/lib/formato";
import { cn } from "@/lib/utils";

/**
 * A escala graduada da faixa do dia. As marcas sao a informacao: elas dizem onde estao o piso e o
 * teto do que costuma ser um dia como este — sem elas, a barra seria enfeite.
 */
function Escala({ faixa }: { faixa: FaixaDto }) {
  // Cabe o teto com folga e tambem um dia que passou muito dele.
  const fim = Math.max(faixa.teto * 1.25, faixa.xpDoDia * 1.08, 1);
  const em = (valor: number) => `${Math.min(100, (valor / fim) * 100)}%`;

  const cor =
    faixa.situacao === "ACIMA"
      ? "text-latao"
      : faixa.situacao === "DENTRO"
        ? "text-aferido"
        : "text-giz";

  return (
    <div className="mt-2">
      {/* Onde o dia esta: acima do eixo, com o valor colado. Separar "onde estou" de "qual e a
          escala" e o que faz a peca ser lida como leitura, e nao como barra de progresso. */}
      <div className="relative h-5">
        <span
          className={cn("absolute bottom-0 flex flex-col items-center", cor)}
          style={{ left: em(faixa.xpDoDia), transform: "translateX(-50%)" }}
        >
          <span className="leitura text-xs leading-none">{faixa.xpDoDia}</span>
          <span className="mt-0.5 h-2 w-0.5 bg-current" />
        </span>
      </div>

      {/* O eixo, com as pontas fechadas como regua. */}
      <div className="relative h-px bg-risco-forte">
        <span className="absolute -top-1 left-0 h-2 w-px bg-risco-forte" />
        <span className="absolute -top-1 right-0 h-2 w-px bg-risco-forte" />
        {/* A faixa aceitavel, engrossada sobre o proprio eixo — mas a meio tom: ela e o contexto,
            e quem tem de ganhar o olho e o marcador do dia. */}
        <span
          className="absolute -top-px h-[3px] bg-aferido/45"
          style={{ left: em(faixa.piso), width: em(faixa.teto - faixa.piso) }}
        />
      </div>

      {/* As graduacoes e seus rotulos, abaixo do eixo — a parte que diz contra o que se mede. */}
      <div className="relative h-8 text-xs">
        <span className="absolute left-0 top-0 flex flex-col items-center">
          <span className="h-1.5 w-px bg-risco-forte" />
          <span className="medida mt-0.5 leading-none text-giz-apagado">0</span>
        </span>
        {[
          { valor: faixa.piso, nome: "piso" },
          { valor: faixa.teto, nome: "teto" },
        ].map(({ valor, nome }) => (
          <span
            key={nome}
            className="absolute top-0 flex flex-col items-center"
            style={{ left: em(valor), transform: "translateX(-50%)" }}
          >
            <span className="h-1.5 w-px bg-aferido/60" />
            <span className="medida mt-0.5 leading-none text-giz-fraco">{valor}</span>
            <span className="leading-tight text-giz-apagado">{nome}</span>
          </span>
        ))}
      </div>
    </div>
  );
}

const MENSAGEM: Record<string, string> = {
  ABAIXO: "abaixo da faixa",
  DENTRO: "dentro da faixa",
  ACIMA: "acima da faixa",
};

interface Props {
  data: string;
  hoje: string;
  resumo: ResumoDto | null;
  faixa: FaixaDto | undefined;
  totalMinutos: number;
  aoNavegar: (dias: number) => void;
  aoIrParaHoje: () => void;
}

/**
 * A leitura do dia: indice, classificacao e a faixa numa peca so.
 *
 * Eram tres cards empilhados dizendo coisas da mesma medicao. Juntos e como um mostrador: o numero
 * grande, a palavra que o traduz e a escala que mostra contra o que ele esta sendo medido. Este e o
 * unico elemento ousado da tela — o resto fica quieto de proposito.
 */
export function LeituraDoDia({
  data,
  hoje,
  resumo,
  faixa,
  totalMinutos,
  aoNavegar,
  aoIrParaHoje,
}: Props) {
  const indice = resumo?.indiceProdutividade ?? 0;
  const descanso = resumo?.descanso || faixa?.situacao === "DESCANSO";

  return (
    <section className="rounded border border-risco bg-placa px-4 pb-4 pt-3">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-baseline gap-2">
          <h2 className="text-sm text-giz">{rotuloDoDia(data)}</h2>
          <span className="medida text-xs text-giz-apagado">{data}</span>
        </div>
        <div className="flex items-center gap-0.5">
          <Button
            variante="fantasma"
            tamanho="icone"
            aria-label="Dia anterior"
            onClick={() => aoNavegar(-1)}
          >
            <ChevronLeft className="size-4" />
          </Button>
          <Button
            variante="fantasma"
            tamanho="icone"
            aria-label="Proximo dia"
            disabled={data >= hoje}
            onClick={() => aoNavegar(1)}
          >
            <ChevronRight className="size-4" />
          </Button>
          {data !== hoje && (
            <Button variante="fantasma" tamanho="sm" onClick={aoIrParaHoje}>
              Hoje
            </Button>
          )}
        </div>
      </div>

      <div className="mt-3 flex flex-wrap items-end justify-between gap-x-8 gap-y-3">
        <div className="flex items-end gap-3">
          <span
            className={cn(
              "leitura text-5xl leading-none",
              resumo ? CORES_CLASSIFICACAO[resumo.classificacao] : "text-giz-apagado",
            )}
          >
            {indice.toFixed(1)}
          </span>
          <div className="pb-0.5">
            <p className="flex items-center gap-1.5 text-sm text-giz">
              {resumo ? ROTULO_CLASSIFICACAO[resumo.classificacao].toLowerCase() : "dia sem dados"}
              <Ajuda texto="O indice cruza o XP do dia com a sua janela de 28 dias e com o contexto do check-in: um dia mediano sob condicoes ruins sobe de classificacao, em vez de descer." />
            </p>
            <p className="medida text-xs text-giz-fraco">
              {resumo?.xpTotal ?? 0} XP
              {totalMinutos > 0 && <> · {formatarDuracao(totalMinutos)}</>}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs">
          {resumo?.diaDificilVencido && (
            <span
              className="flex items-center gap-1.5 text-latao"
              title="Dia adverso em que voce entregou mesmo assim"
            >
              <Swords className="size-3.5" />
              dia dificil vencido
            </span>
          )}
          {descanso && (
            <span className="flex items-center gap-1.5 text-frio">
              <Moon className="size-3.5" />
              descanso planejado
            </span>
          )}
          {resumo?.baselineInsuficiente && (
            <span className="text-giz-apagado" title="Menos de 7 dias de historico para comparar">
              indice provisorio
            </span>
          )}
        </div>
      </div>

      {faixa && !descanso && (
        <div className="mt-4">
          <div className="flex flex-wrap items-baseline justify-between gap-x-4 text-xs">
            <span className="flex items-center gap-1.5 text-giz-fraco">
              faixa do dia · {faixa.rotulo}
              <Ajuda
                texto={`Metade dos seus dias de energia parecida ficou entre ${faixa.piso} e ${faixa.teto} XP. Nao e meta escolhida: sai do seu proprio historico, entao a regua sobe junto com voce.`}
              />
            </span>
            {faixa.situacao === "CALIBRANDO" ? (
              <span className="text-giz-apagado">
                calibrando · {faixa.diasComparaveis}{" "}
                {faixa.diasComparaveis === 1 ? "dia comparavel" : "dias comparaveis"}
              </span>
            ) : (
              <span
                className={
                  faixa.situacao === "ACIMA"
                    ? "text-latao"
                    : faixa.situacao === "DENTRO"
                      ? "text-aferido"
                      : "text-giz-fraco"
                }
              >
                {MENSAGEM[faixa.situacao]}
                {faixa.situacao === "ABAIXO" && (
                  <span className="medida text-giz-apagado"> · faltam {faixa.faltaParaOPiso}</span>
                )}
              </span>
            )}
          </div>
          {faixa.situacao !== "CALIBRANDO" && <Escala faixa={faixa} />}
        </div>
      )}
    </section>
  );
}
