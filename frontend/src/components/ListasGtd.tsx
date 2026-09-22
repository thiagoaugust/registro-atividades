import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, Check, RotateCcw, Trash2 } from "lucide-react";
import { api, type AcaoDto, type EstadoAcao } from "@/api";
import { Button } from "@/components/ui/button";
import { Select } from "@/components/ui/input";
import { Card } from "@/components/ui/campo";
import { CORES_CATEGORIA, formatarDuracao } from "@/lib/formato";
import { RegistrarAcao } from "@/components/RegistrarAcao";

const LISTAS: { estado: EstadoAcao; rotulo: string }[] = [
  { estado: "PROXIMA", rotulo: "Proximas acoes" },
  { estado: "AGENDA", rotulo: "Agenda" },
  { estado: "AGUARDANDO", rotulo: "Aguardando" },
  { estado: "ALGUM_DIA", rotulo: "Algum dia/talvez" },
  { estado: "CONCLUIDA", rotulo: "Concluidas" },
];

export function ItemAcao({
  acao,
  aoConcluir,
  aoReabrir,
  aoExcluir,
}: {
  acao: AcaoDto;
  aoConcluir?: (id: number) => void;
  aoReabrir?: (id: number) => void;
  aoExcluir?: (id: number) => void;
}) {
  return (
    <Card className="flex items-start justify-between gap-3">
      <div className="min-w-0">
        <p className={acao.estado === "CONCLUIDA" ? "text-sm text-giz-apagado line-through" : "text-sm"}>
          {acao.titulo}
        </p>
        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-giz-apagado">
          {acao.contexto && <span className="text-frio">{acao.contexto.nome}</span>}
          {acao.tempoEstimadoMin && <span>{formatarDuracao(acao.tempoEstimadoMin)}</span>}
          {acao.energia && <span>energia {acao.energia.toLowerCase()}</span>}
          {acao.projeto && <span className="text-giz-fraco">{acao.projeto.nome}</span>}
          {acao.categoria && (
            <span className={`rounded-full border px-1.5 ${CORES_CATEGORIA[acao.categoria]}`}>
              {acao.categoria}
            </span>
          )}
          {acao.agendadaPara && <span>{new Date(acao.agendadaPara).toLocaleString("pt-BR")}</span>}
          {acao.delegadaPara && (
            <span>
              com {acao.delegadaPara} desde {acao.delegadaEm}
            </span>
          )}
          {acao.registroId && <span className="text-aferido">registrada</span>}
        </div>
        {acao.notas && <p className="mt-1 text-xs text-giz-apagado">{acao.notas}</p>}
      </div>

      <div className="flex shrink-0 gap-1">
        {aoConcluir && acao.estado !== "CONCLUIDA" && (
          <Button
            variante="fantasma"
            tamanho="icone"
            aria-label={`Concluir ${acao.titulo}`}
            title="Concluir"
            onClick={() => aoConcluir(acao.id)}
          >
            <Check className="size-4" />
          </Button>
        )}
        {aoReabrir && acao.estado === "CONCLUIDA" && (
          <Button
            variante="fantasma"
            tamanho="icone"
            aria-label={`Reabrir ${acao.titulo}`}
            title="Reabrir"
            onClick={() => aoReabrir(acao.id)}
          >
            <RotateCcw className="size-4" />
          </Button>
        )}
        {aoExcluir && (
          <Button
            variante="perigo"
            tamanho="icone"
            aria-label={`Excluir ${acao.titulo}`}
            onClick={() => aoExcluir(acao.id)}
          >
            <Trash2 className="size-4" />
          </Button>
        )}
      </div>
    </Card>
  );
}

export function ListasGtd() {
  const [estado, setEstado] = useState<EstadoAcao | "REFERENCIAS">("PROXIMA");
  const [contextoId, setContextoId] = useState("");
  const [paraRegistrar, setParaRegistrar] = useState<{ id: number; categoria: string; titulo: string } | null>(
    null,
  );
  const queryClient = useQueryClient();

  const contextos = useQuery({ queryKey: ["contextos"], queryFn: api.contextos });
  const parados = useQuery({ queryKey: ["projetos-parados"], queryFn: api.projetosParados });
  const acoes = useQuery({
    queryKey: ["acoes", estado, contextoId],
    enabled: estado !== "REFERENCIAS",
    queryFn: () =>
      api.acoes({
        estado: estado as EstadoAcao,
        contexto: contextoId === "" ? undefined : Number(contextoId),
      }),
  });
  const referencias = useQuery({
    queryKey: ["referencias"],
    enabled: estado === "REFERENCIAS",
    queryFn: () => api.referencias(),
  });

  const invalidar = () => queryClient.invalidateQueries();

  const concluir = useMutation({
    mutationFn: api.concluirAcao,
    onSuccess: (resultado) => {
      invalidar();
      // A acao ja sabe categoria e titulo: so faltam duracao e esforco para virar registro.
      if (resultado.sugestaoRegistro) {
        setParaRegistrar({
          id: resultado.acao.id,
          categoria: resultado.sugestaoRegistro.categoria,
          titulo: resultado.sugestaoRegistro.titulo,
        });
      }
    },
  });

  const reabrir = useMutation({ mutationFn: api.reabrirAcao, onSuccess: invalidar });
  const excluir = useMutation({ mutationFn: api.excluirAcao, onSuccess: invalidar });

  return (
    <div className="flex flex-col gap-4">
      {(parados.data?.length ?? 0) > 0 && (
        <Card className="flex items-start gap-3 border-latao/30 bg-latao/10">
          <AlertTriangle className="mt-0.5 size-4 shrink-0 text-latao" />
          <div>
            <p className="text-sm font-medium text-latao">
              {parados.data?.length} projeto(s) sem proxima acao
            </p>
            <p className="text-xs text-latao/70">
              {parados.data?.map((p) => p.nome).join(", ")}
            </p>
          </div>
        </Card>
      )}

      <div className="flex flex-wrap items-center gap-2">
        {LISTAS.map((lista) => (
          <Button
            key={lista.estado}
            variante={estado === lista.estado ? "secundario" : "fantasma"}
            tamanho="sm"
            onClick={() => setEstado(lista.estado)}
          >
            {lista.rotulo}
          </Button>
        ))}

        <Button
          variante={estado === "REFERENCIAS" ? "secundario" : "fantasma"}
          tamanho="sm"
          onClick={() => setEstado("REFERENCIAS")}
        >
          Referencias
        </Button>

        {estado === "PROXIMA" && (
          <Select
            value={contextoId}
            onChange={(e) => setContextoId(e.target.value)}
            className="ml-auto w-44"
          >
            <option value="">todos os contextos</option>
            {contextos.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
              </option>
            ))}
          </Select>
        )}
      </div>

      {estado === "REFERENCIAS" ? (
        <div className="flex flex-col gap-2">
          {referencias.data?.length === 0 && (
            <Card className="text-sm text-giz-apagado">Nenhuma referencia arquivada.</Card>
          )}
          {referencias.data?.map((referencia) => (
            <Card key={referencia.id}>
              <p className="text-sm font-medium">{referencia.titulo}</p>
              {referencia.url && (
                <a
                  href={referencia.url}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-1 block truncate text-xs text-frio hover:underline"
                >
                  {referencia.url}
                </a>
              )}
              {referencia.conteudo && <p className="mt-1 text-xs text-giz-apagado">{referencia.conteudo}</p>}
              {referencia.tags.length > 0 && (
                <p className="mt-1 text-xs text-giz-apagado">{referencia.tags.map((t) => `#${t}`).join(" ")}</p>
              )}
            </Card>
          ))}
        </div>
      ) : (
      <div className="flex flex-col gap-2">
        {acoes.isLoading && <p className="text-sm text-giz-apagado">Carregando...</p>}
        {acoes.data?.length === 0 && <Card className="text-sm text-giz-apagado">Lista vazia.</Card>}
        {acoes.data?.map((acao) => (
          <ItemAcao
            key={acao.id}
            acao={acao}
            aoConcluir={(id) => concluir.mutate(id)}
            aoReabrir={(id) => reabrir.mutate(id)}
            aoExcluir={(id) => excluir.mutate(id)}
          />
        ))}
      </div>
      )}

      {paraRegistrar && (
        <RegistrarAcao
          acao={paraRegistrar}
          aoFechar={() => setParaRegistrar(null)}
          aoRegistrar={invalidar}
        />
      )}
    </div>
  );
}
