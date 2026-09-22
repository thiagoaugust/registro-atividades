import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2, Circle, AlertTriangle } from "lucide-react";
import { api, ErroApi, type PassoRevisao } from "@/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/campo";
import { formatarDuracao } from "@/lib/formato";
import { corDaVariacao, formatarVariacao } from "@/lib/analytics";

function semanaDe(iso: string): string {
  const data = new Date(`${iso}T00:00:00`);
  const diaSemana = (data.getDay() + 6) % 7; // 0 = segunda
  data.setDate(data.getDate() - diaSemana);
  return data.toLocaleDateString("sv-SE");
}

function hojeLocal(): string {
  return new Date().toLocaleDateString("sv-SE", { timeZone: "America/Sao_Paulo" });
}

function somarDias(iso: string, dias: number): string {
  const data = new Date(`${iso}T00:00:00`);
  data.setDate(data.getDate() + dias);
  return data.toLocaleDateString("sv-SE");
}

/**
 * O "refletir" do GTD. O checklist e retomavel: dificilmente a revisao inteira sai numa sentada, e
 * perder o progresso no meio e o que faz as pessoas abandonarem o habito.
 */
export function RevisaoSemanal() {
  const [erro, setErro] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const semana = semanaDe(hojeLocal());

  const checklist = useQuery({ queryKey: ["checklist"], queryFn: api.checklistRevisao });
  const revisoes = useQuery({ queryKey: ["revisoes"], queryFn: api.revisoes });
  const inbox = useQuery({ queryKey: ["inbox"], queryFn: api.inbox });
  const parados = useQuery({ queryKey: ["projetos-parados"], queryFn: api.projetosParados });

  // O resumo da semana vem do modulo de analytics, como manda o metodo: revisar sem olhar o que
  // aconteceu e so arrumar listas.
  const resumo = useQuery({
    queryKey: ["analytics-periodo", "DIA", semana, somarDias(semana, 6)],
    queryFn: () => api.periodoAnalytics("DIA", semana, somarDias(semana, 6)),
  });

  const atual = revisoes.data?.find((r) => r.semanaInicio === semana);

  const invalidar = () => queryClient.invalidateQueries();

  const iniciar = useMutation({ mutationFn: () => api.iniciarRevisao(semana), onSuccess: invalidar });
  const marcar = useMutation({
    mutationFn: ({ passo, feito }: { passo: PassoRevisao; feito: boolean }) =>
      api.marcarPasso(semana, passo, feito),
    onSuccess: invalidar,
  });
  const concluir = useMutation({
    mutationFn: () => api.concluirRevisao(semana),
    onSuccess: () => {
      setErro(null);
      invalidar();
    },
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para concluir."),
  });

  const diasDecorridos =
    Math.floor((Date.parse(`${hojeLocal()}T00:00:00`) - Date.parse(`${semana}T00:00:00`)) / 86400000) + 1;
  const pendentesInbox = inbox.data?.pendentes ?? 0;
  const projetosParados = parados.data?.length ?? 0;

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <div className="flex flex-wrap items-baseline justify-between gap-2">
          <div>
            <p className="text-sm font-medium">Revisao da semana de {semana}</p>
            <p className="text-xs text-giz-apagado">
              {atual?.concluidaEm
                ? `concluida em ${new Date(atual.concluidaEm).toLocaleString("pt-BR")} · ${atual.duracaoMin} min`
                : atual
                  ? "em andamento"
                  : "ainda nao iniciada"}
            </p>
          </div>
          {!atual && (
            <Button onClick={() => iniciar.mutate()} disabled={iniciar.isPending}>
              Comecar revisao
            </Button>
          )}
        </div>
      </Card>

      {atual && (
        <Card className="flex flex-col gap-3">
          {checklist.data?.map((item) => {
            const feito = atual.passos[item.passo] === true;
            const alerta =
              (item.passo === "ESVAZIAR_INBOX" && pendentesInbox > 0) ||
              (item.passo === "REVISAR_PROJETOS" && projetosParados > 0);

            return (
              <button
                key={item.passo}
                type="button"
                disabled={atual.concluidaEm !== null}
                onClick={() => marcar.mutate({ passo: item.passo, feito: !feito })}
                className="flex items-start gap-3 rounded-md p-2 text-left hover:bg-placa-alta disabled:hover:bg-transparent"
              >
                {feito ? (
                  <CheckCircle2 className="mt-0.5 size-5 shrink-0 text-aferido" />
                ) : (
                  <Circle className="mt-0.5 size-5 shrink-0 text-giz-apagado" />
                )}
                <span className="min-w-0">
                  <span className={`block text-sm ${feito ? "text-giz-apagado line-through" : ""}`}>
                    {item.titulo}
                  </span>
                  <span className="block text-xs text-giz-apagado">{item.descricao}</span>
                  {alerta && (
                    <span className="mt-1 flex items-center gap-1 text-xs text-latao">
                      <AlertTriangle className="size-3.5" />
                      {item.passo === "ESVAZIAR_INBOX"
                        ? `${pendentesInbox} item(ns) no inbox`
                        : `${projetosParados} projeto(s) sem proxima acao`}
                    </span>
                  )}
                </span>
              </button>
            );
          })}

          {erro && <p className="text-sm text-giz-fraco">{erro}</p>}

          {!atual.concluidaEm && (
            <Button
              onClick={() => concluir.mutate()}
              disabled={!atual.completa || concluir.isPending}
            >
              {atual.completa ? "Concluir revisao (+50 XP)" : "Marque todos os passos"}
            </Button>
          )}
        </Card>
      )}

      {resumo.data && (
        <Card>
          <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
            <p className="text-sm font-medium">A semana em numeros</p>
            {diasDecorridos < 7 && (
              <p className="text-xs text-latao/80">
                semana em andamento: {diasDecorridos} de 7 dias — a comparacao com a semana cheia
                anterior fica torta ate domingo
              </p>
            )}
          </div>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {[
              { rotulo: "XP", valor: String(resumo.data.xp.atual), variacao: resumo.data.xp },
              {
                rotulo: "Tempo",
                valor: formatarDuracao(resumo.data.minutos.atual),
                variacao: resumo.data.minutos,
              },
              {
                rotulo: "Dias ativos",
                valor: String(resumo.data.diasComPresenca.atual),
                variacao: resumo.data.diasComPresenca,
              },
              {
                rotulo: "Dias vencidos",
                valor: String(resumo.data.diasDificeisVencidos.atual),
                variacao: resumo.data.diasDificeisVencidos,
              },
            ].map((item) => (
              <div key={item.rotulo}>
                <p className="text-[0.8125rem] text-giz-apagado">{item.rotulo}</p>
                <p className="text-lg font-semibold">{item.valor}</p>
                <p className={`text-xs ${corDaVariacao(item.variacao.percentual)}`}>
                  {formatarVariacao(item.variacao.percentual)} vs semana anterior
                </p>
              </div>
            ))}
          </div>
        </Card>
      )}

      {(revisoes.data?.length ?? 0) > 0 && (
        <Card>
          <p className="mb-2 text-sm font-medium">Revisoes anteriores</p>
          <ul className="flex flex-col gap-1 text-sm">
            {revisoes.data?.slice(0, 8).map((revisao) => (
              <li key={revisao.semanaInicio} className="flex justify-between">
                <span className="text-giz">semana de {revisao.semanaInicio}</span>
                <span className="text-xs text-giz-apagado">
                  {revisao.concluidaEm ? `${revisao.duracaoMin} min` : "incompleta"}
                </span>
              </li>
            ))}
          </ul>
        </Card>
      )}
    </div>
  );
}
