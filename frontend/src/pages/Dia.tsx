import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronLeft, ChevronRight, Pencil, Trash2 } from "lucide-react";
import { api, ErroApi, type DadosCheckin, type DadosRegistro, type RegistroDto } from "@/api";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/campo";
import { FormularioRegistro } from "@/components/FormularioRegistro";
import { CheckinCard } from "@/components/CheckinCard";
import { ResumoDoDia } from "@/components/ResumoDoDia";
import { FaixaDoDia } from "@/components/FaixaDoDia";
import { EmAndamento, type Atalho } from "@/components/EmAndamento";
import {
  CORES_CATEGORIA,
  esforcoMedio,
  formatarDetalhes,
  formatarDuracao,
  rotuloDoDia,
} from "@/lib/formato";

export function hojeLocal(): string {
  return new Date().toLocaleDateString("sv-SE", { timeZone: "America/Sao_Paulo" });
}

function somarDias(iso: string, dias: number): string {
  const [ano, mes, dia] = iso.split("-").map(Number);
  const data = new Date(Date.UTC(ano, mes - 1, dia + dias));
  return data.toISOString().slice(0, 10);
}

export function PainelDia() {
  const [data, setData] = useState(hojeLocal());
  const [editando, setEditando] = useState<RegistroDto | null>(null);
  // `linha` diz em qual item o formulario esta aberto; `chave` sobe a cada clique, para o
  // formulario reagir mesmo quando o item escolhido e o mesmo de antes.
  const [atalho, setAtalho] = useState<(Atalho & { chave: number; linha: string }) | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const dia = useQuery({ queryKey: ["dia", data], queryFn: () => api.dia(data) });
  const faixa = useQuery({ queryKey: ["faixa", data], queryFn: () => api.faixa(data) });

  // Qualquer escrita mexe em XP, nivel, streak e conquistas: o jeito honesto e invalidar tudo.
  const invalidar = () => queryClient.invalidateQueries();

  const salvar = useMutation({
    mutationFn: (dados: DadosRegistro) =>
      editando ? api.atualizarRegistro(editando.id, dados) : api.criarRegistro(dados),
    onSuccess: () => {
      setEditando(null);
      // Fecha o formulario aberto na linha: a sessao foi salva, e deixa-lo aberto com os valores
      // digitados convida a um segundo clique que criaria registro duplicado.
      setAtalho(null);
      setErro(null);
      invalidar();
    },
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para salvar."),
  });

  const excluir = useMutation({ mutationFn: api.excluirRegistro, onSuccess: invalidar });

  const salvarCheckin = useMutation({
    mutationFn: (dados: DadosCheckin) => api.salvarCheckin(data, dados),
    onSuccess: invalidar,
  });

  const fecharDia = useMutation({
    mutationFn: (dados: { dificuldadeFinal?: number | null; atrapalhou?: string | null }) =>
      api.fecharDia(data, dados),
    onSuccess: invalidar,
  });

  const registros = dia.data?.registros ?? [];
  const medio = esforcoMedio(registros);

  // Um elemento so, montado ou dentro da linha do item escolhido ou no lugar de sempre. Trocar de
  // lugar remonta e zera os campos — que e justamente o que se quer ao mudar de alvo.
  const formulario = (
    <FormularioRegistro
      data={data}
      editando={editando}
      atalho={atalho}
      aoSalvar={(dados) => salvar.mutate(dados)}
      aoCancelar={() => {
        setEditando(null);
        setAtalho(null);
        setErro(null);
      }}
      salvando={salvar.isPending}
      erro={erro}
    />
  );

  return (
    <>
      <div className="my-4 flex items-center gap-2">
        <Button
          variante="secundario"
          tamanho="icone"
          aria-label="Dia anterior"
          onClick={() => setData(somarDias(data, -1))}
        >
          <ChevronLeft className="size-4" />
        </Button>
        <div className="flex-1 text-center">
          <p className="text-sm font-medium">{rotuloDoDia(data)}</p>
          <p className="text-xs text-zinc-500">{data}</p>
        </div>
        <Button
          variante="secundario"
          tamanho="icone"
          aria-label="Proximo dia"
          onClick={() => setData(somarDias(data, 1))}
          disabled={data >= hojeLocal()}
        >
          <ChevronRight className="size-4" />
        </Button>
        {data !== hojeLocal() && (
          <Button variante="fantasma" tamanho="sm" onClick={() => setData(hojeLocal())}>
            Hoje
          </Button>
        )}
      </div>

      <div className="mb-4">
        <ResumoDoDia resumo={dia.data?.resumo ?? null} />
      </div>

      <div className="flex flex-col gap-4">
        <FaixaDoDia faixa={faixa.data} />

        <EmAndamento
          aberta={atalho?.linha ?? null}
          formulario={formulario}
          aoRegistrar={(escolha, linha) => {
            setEditando(null);
            setErro(null);
            setAtalho({ ...escolha, linha, chave: Date.now() });
          }}
          aoFechar={() => {
            setAtalho(null);
            setErro(null);
          }}
        />

        <CheckinCard
          data={data}
          checkin={dia.data?.checkin ?? null}
          aoSalvar={(dados) => salvarCheckin.mutate(dados)}
          aoFechar={(dados) => fecharDia.mutate(dados)}
          salvando={salvarCheckin.isPending || fecharDia.isPending}
        />

        {atalho === null && formulario}
      </div>

      <div className="mt-6 flex flex-wrap items-center gap-2 text-sm">
        <span className="text-zinc-400">
          {formatarDuracao(dia.data?.totalMinutos ?? 0)} no dia
          {medio !== null && ` · esforco medio ${medio}`}
        </span>
        {Object.entries(dia.data?.minutosPorCategoria ?? {}).map(([categoria, minutos]) => (
          <span
            key={categoria}
            className={`rounded-full border px-2 py-0.5 text-xs ${
              CORES_CATEGORIA[categoria as keyof typeof CORES_CATEGORIA]
            }`}
          >
            {categoria} {formatarDuracao(minutos)}
          </span>
        ))}
      </div>

      <div className="mt-3 flex flex-col gap-2">
        {dia.isLoading && <p className="text-sm text-zinc-500">Carregando...</p>}
        {!dia.isLoading && registros.length === 0 && (
          <Card className="text-sm text-zinc-500">Nenhum registro nesse dia.</Card>
        )}
        {registros.map((registro) => (
          <Card key={registro.id} className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                <span
                  className={`rounded-full border px-2 py-0.5 text-xs ${CORES_CATEGORIA[registro.categoria]}`}
                >
                  {registro.categoria}
                </span>
                <span className="text-sm font-medium">
                  {registro.titulo ?? registro.livro?.titulo ?? registro.projeto?.titulo ?? "sem titulo"}
                </span>
              </div>
              <p className="mt-1 text-xs text-zinc-400">
                {formatarDuracao(registro.duracaoMin)} · esforco {registro.esforco}
                {registro.satisfacao && ` · satisfacao ${registro.satisfacao}`}
              </p>
              {Object.keys(registro.detalhes).length > 0 && (
                <p className="mt-1 truncate text-xs text-zinc-500">
                  {formatarDetalhes(registro.detalhes)}
                </p>
              )}
              {registro.notas && <p className="mt-1 text-xs text-zinc-500">{registro.notas}</p>}
            </div>
            <div className="flex shrink-0 gap-1">
              <Button
                variante="fantasma"
                tamanho="icone"
                aria-label={`Editar ${registro.titulo ?? "registro"}`}
                onClick={() => {
                  setAtalho(null);
                  setEditando(registro);
                }}
              >
                <Pencil className="size-4" />
              </Button>
              <Button
                variante="perigo"
                tamanho="icone"
                aria-label={`Excluir ${registro.titulo ?? "registro"}`}
                onClick={() => excluir.mutate(registro.id)}
                disabled={excluir.isPending}
              >
                <Trash2 className="size-4" />
              </Button>
            </div>
          </Card>
        ))}
      </div>
    </>
  );
}
