import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api, type Energia } from "@/api";
import { Select } from "@/components/ui/input";
import { Campo, Card } from "@/components/ui/campo";
import { ItemAcao } from "@/components/ListasGtd";
import { RegistrarAcao } from "@/components/RegistrarAcao";

const TEMPOS = [15, 30, 60, 120, 240];

/** "O que fazer agora?": contexto onde estou, tempo que tenho, energia que sobrou. */
export function Agora() {
  const [contextoId, setContextoId] = useState("");
  const [tempo, setTempo] = useState("");
  const [energia, setEnergia] = useState<Energia | "">("");
  const [paraRegistrar, setParaRegistrar] = useState<{ id: number; categoria: string; titulo: string } | null>(
    null,
  );
  const queryClient = useQueryClient();

  const contextos = useQuery({ queryKey: ["contextos"], queryFn: api.contextos });
  const sugestoes = useQuery({
    queryKey: ["engajar", contextoId, tempo, energia],
    queryFn: () =>
      api.engajar({
        contexto: contextoId === "" ? undefined : Number(contextoId),
        tempoDisponivel: tempo === "" ? undefined : Number(tempo),
        energia: energia === "" ? undefined : energia,
      }),
  });

  const concluir = useMutation({
    mutationFn: api.concluirAcao,
    onSuccess: (resultado) => {
      queryClient.invalidateQueries();
      if (resultado.sugestaoRegistro) {
        setParaRegistrar({
          id: resultado.acao.id,
          categoria: resultado.sugestaoRegistro.categoria,
          titulo: resultado.sugestaoRegistro.titulo,
        });
      }
    },
  });

  // Sem escolha explicita, o backend usa a energia do check-in de hoje como palpite.
  const energiaEmUso = energia === "" ? sugestoes.data?.energiaSugerida : energia;

  return (
    <div className="flex flex-col gap-4">
      <Card className="grid gap-3 sm:grid-cols-3">
        <Campo rotulo="Onde voce esta">
          <Select value={contextoId} onChange={(e) => setContextoId(e.target.value)}>
            <option value="">qualquer contexto</option>
            {contextos.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nome}
              </option>
            ))}
          </Select>
        </Campo>
        <Campo rotulo="Tempo disponivel">
          <Select value={tempo} onChange={(e) => setTempo(e.target.value)}>
            <option value="">qualquer</option>
            {TEMPOS.map((minutos) => (
              <option key={minutos} value={minutos}>
                ate {minutos} min
              </option>
            ))}
          </Select>
        </Campo>
        <Campo
          rotulo="Energia agora"
          dica={energia === "" && energiaEmUso ? `do check-in: ${energiaEmUso.toLowerCase()}` : undefined}
        >
          <Select value={energia} onChange={(e) => setEnergia(e.target.value as Energia)}>
            <option value="">usar a do check-in</option>
            <option value="BAIXA">baixa</option>
            <option value="MEDIA">media</option>
            <option value="ALTA">alta</option>
          </Select>
        </Campo>
      </Card>

      <div className="flex flex-col gap-2">
        {sugestoes.isLoading && <p className="text-sm text-giz-apagado">Procurando...</p>}
        {sugestoes.data?.acoes.length === 0 && (
          <Card className="text-sm text-giz-apagado">
            Nada cabe nesses filtros. Afrouxe o tempo ou a energia — ou va descansar.
          </Card>
        )}
        {sugestoes.data?.acoes.map((acao) => (
          <ItemAcao key={acao.id} acao={acao} aoConcluir={(id) => concluir.mutate(id)} />
        ))}
      </div>

      {paraRegistrar && (
        <RegistrarAcao
          acao={paraRegistrar}
          aoFechar={() => setParaRegistrar(null)}
          aoRegistrar={() => queryClient.invalidateQueries()}
        />
      )}
    </div>
  );
}
