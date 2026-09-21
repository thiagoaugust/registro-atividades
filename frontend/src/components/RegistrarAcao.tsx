import { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { api, ErroApi } from "@/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Campo } from "@/components/ui/campo";

/**
 * A ponte GTD -> registro. A acao concluida ja trouxe categoria e titulo, entao a pergunta que sobra
 * e "quanto tempo levou e o quanto custou" — dois campos, nao um formulario inteiro.
 */
export function RegistrarAcao({
  acao,
  aoFechar,
  aoRegistrar,
}: {
  acao: { id: number; categoria: string; titulo: string };
  aoFechar: () => void;
  aoRegistrar: () => void;
}) {
  const [duracao, setDuracao] = useState("30");
  const [esforco, setEsforco] = useState(5);
  const [erro, setErro] = useState<string | null>(null);

  const registrar = useMutation({
    mutationFn: () =>
      api.registrarAcao(acao.id, { duracaoMin: Number(duracao), esforco }),
    onSuccess: () => {
      aoRegistrar();
      aoFechar();
    },
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para registrar."),
  });

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onClick={aoFechar}
    >
      <div
        className="w-full max-w-sm rounded-lg border border-zinc-800 bg-zinc-900 p-5"
        onClick={(evento) => evento.stopPropagation()}
      >
        <p className="text-sm font-medium">Registrar como atividade</p>
        <p className="mt-1 text-xs text-zinc-500">
          {acao.categoria} · {acao.titulo}
        </p>

        <div className="mt-4 flex flex-col gap-4">
          <Campo rotulo="Duracao (min)">
            <Input
              type="number"
              min={1}
              value={duracao}
              onChange={(e) => setDuracao(e.target.value)}
              autoFocus
            />
          </Campo>
          <Campo rotulo={`Esforco ${esforco}`}>
            <input
              type="range"
              min={1}
              max={10}
              value={esforco}
              onChange={(e) => setEsforco(Number(e.target.value))}
              className="h-9 w-full accent-emerald-500"
            />
          </Campo>

          {erro && <p className="text-sm text-rose-400">{erro}</p>}

          <div className="flex gap-2">
            <Button onClick={() => registrar.mutate()} disabled={registrar.isPending}>
              Registrar
            </Button>
            <Button variante="secundario" onClick={aoFechar}>
              Agora nao
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
