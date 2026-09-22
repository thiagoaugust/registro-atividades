import { useEffect, useState } from "react";
import { Moon, Check } from "lucide-react";
import type { CheckinDto, DadosCheckin } from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Textarea } from "@/components/ui/input";
import { Ajuda, Campo, Secao } from "@/components/ui/campo";
import { formatarDuracao } from "@/lib/formato";
import { cn } from "@/lib/utils";

const texto = (valor: number | null | undefined) => (valor == null ? "" : String(valor));
const numero = (valor: string | undefined) =>
  valor == null || valor.trim() === "" ? null : Number(valor);

/**
 * Uma medida do relogio. Sao oito caixinhas iguais; repetir o <Campo> inteiro oito vezes so
 * alongaria o arquivo.
 */
function Medida({
  rotulo,
  campo,
  sono,
  aoMudar,
  ajuda,
  tipo = "number",
  ...resto
}: {
  rotulo: string;
  campo: string;
  sono: Record<string, string>;
  aoMudar: (sono: Record<string, string>) => void;
  ajuda: string;
  tipo?: string;
  min?: number;
  max?: number;
}) {
  return (
    <Campo rotulo={rotulo} ajuda={ajuda}>
      <Input
        type={tipo}
        value={sono[campo] ?? ""}
        onChange={(e) => aoMudar({ ...sono, [campo]: e.target.value })}
        {...resto}
      />
    </Campo>
  );
}

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
              ? "border-aferido bg-aferido text-breu"
              : "border-risco bg-placa text-giz-fraco hover:border-risco-forte",
          )}
          aria-label={rotulos ? `${n} de 5 (${rotulos[0]} a ${rotulos[1]})` : `${n} de 5`}
        >
          {n}
        </button>
      ))}
      {rotulos && (
        <span className="ml-2 text-xs text-giz-apagado">
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
  const [sono, setSono] = useState<Record<string, string>>({});
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
    setSono({
      minutosSono: texto(checkin?.minutosSono),
      dormiuEm: checkin?.dormiuEm?.slice(0, 5) ?? "",
      acordouEm: checkin?.acordouEm?.slice(0, 5) ?? "",
      minutosSonoProfundo: texto(checkin?.minutosSonoProfundo),
      minutosSonoRem: texto(checkin?.minutosSonoRem),
      despertares: texto(checkin?.despertares),
      fcRepouso: texto(checkin?.fcRepouso),
      pontuacaoSono: texto(checkin?.pontuacaoSono),
    });
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
      minutosSono: numero(sono.minutosSono),
      dormiuEm: sono.dormiuEm || null,
      acordouEm: sono.acordouEm || null,
      minutosSonoProfundo: numero(sono.minutosSonoProfundo),
      minutosSonoRem: numero(sono.minutosSonoRem),
      despertares: numero(sono.despertares),
      fcRepouso: numero(sono.fcRepouso),
      pontuacaoSono: numero(sono.pontuacaoSono),
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
    <div className="flex flex-col gap-2">
      {/* Secao com fio, como as vizinhas: uma caixa em volta so deste bloco fazia dele um corpo
          estranho no meio de uma tela que nao usa mais caixas. */}
      <Secao
        acao={
          <button
            type="button"
            className="text-[0.8125rem] text-giz-fraco transition-colors hover:text-giz"
            onClick={() => setAberto(!aberto)}
          >
            {aberto ? "fechar" : preenchido ? "editar" : "preencher"}
          </button>
        }
      >
        <span className="flex items-center gap-1.5">
          check-in do dia
          {preenchido && <Check className="size-3.5 text-aferido" />}
          {checkin?.descansoPlanejado && <Moon className="size-3.5 text-frio" />}
        </span>
      </Secao>

      {!aberto && preenchido && (
        <p className="text-xs text-giz-apagado">
          {[
            checkin.energia && `energia ${checkin.energia}`,
            checkin.minutosSono && `dormiu ${formatarDuracao(checkin.minutosSono)}`,
            checkin.pontuacaoSono && `sono ${checkin.pontuacaoSono}/100`,
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
        <form className="mt-2 flex flex-col gap-4" onSubmit={salvar}>
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
          </div>

          <div className="flex flex-col gap-3 border-t border-risco pt-4">
            <p className="flex items-center gap-1.5 text-[0.8125rem] text-giz-fraco">
              <Moon className="size-3.5" />
              Sono da noite
              <Ajuda texto="O que o relogio mediu, digitado a mao. Nada disso entra no indice do dia — serve para cruzar sono medido com o que o dia rendeu, no painel Evolucao." />
            </p>
            <div className="grid gap-3 sm:grid-cols-3">
              <Medida
                rotulo="Deitou"
                campo="dormiuEm"
                tipo="time"
                sono={sono}
                aoMudar={setSono}
                ajuda="Hora em que voce foi dormir. Junto com a hora de acordar, da a duracao quando voce nao tiver o numero do relogio."
              />
              <Medida
                rotulo="Acordou"
                campo="acordouEm"
                tipo="time"
                sono={sono}
                aoMudar={setSono}
                ajuda="Hora em que voce levantou. Noite que atravessa a meia-noite e tratada certo."
              />
              <Medida
                rotulo="Dormiu (min)"
                campo="minutosSono"
                sono={sono}
                aoMudar={setSono}
                min={0}
                max={1440}
                ajuda="Minutos de sono segundo o relogio. Se preencher, manda sobre a conta deitou-acordou: o relogio desconta os despertares, a subtracao nao."
              />
              <Medida
                rotulo="Profundo (min)"
                campo="minutosSonoProfundo"
                sono={sono}
                aoMudar={setSono}
                min={0}
                max={1440}
                ajuda="Minutos de sono profundo. E a fase ligada a recuperacao fisica — vale cruzar com os dias de treino."
              />
              <Medida
                rotulo="REM (min)"
                campo="minutosSonoRem"
                sono={sono}
                aoMudar={setSono}
                min={0}
                max={1440}
                ajuda="Minutos de sono REM. E a fase ligada a consolidacao do que se aprendeu — vale cruzar com os dias de estudo."
              />
              <Medida
                rotulo="Despertares"
                campo="despertares"
                sono={sono}
                aoMudar={setSono}
                min={0}
                max={100}
                ajuda="Quantas vezes a noite quebrou. Uma noite longa e picotada rende menos que uma curta inteira."
              />
              <Medida
                rotulo="FC repouso"
                campo="fcRepouso"
                sono={sono}
                aoMudar={setSono}
                min={20}
                max={220}
                ajuda="Frequencia cardiaca de repouso, em bpm. Sobe quando o corpo nao se recuperou — costuma avisar antes de voce sentir."
              />
              <Medida
                rotulo="Pontuacao"
                campo="pontuacaoSono"
                sono={sono}
                aoMudar={setSono}
                min={0}
                max={100}
                ajuda="A nota de 0 a 100 que o proprio relogio da para a noite. Fica ao lado da sua nota de 1 a 5 de proposito: da para ver quando o que voce sente discorda do que foi medido."
              />
            </div>
          </div>

          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={descanso}
              onChange={(e) => setDescanso(e.target.checked)}
              className="size-4 accent-aferido"
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

          <div className="flex flex-col gap-3 border-t border-risco pt-4">
            <p className="text-[0.8125rem] text-giz-fraco">
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
    </div>
  );
}
