import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { CheckCircle2 } from "lucide-react";
import {
  api,
  CATEGORIAS,
  type Categoria,
  type Decisao,
  type Energia,
  type EstadoAcao,
  type InboxItemDto,
} from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Select, Textarea } from "@/components/ui/input";
import { Campo, Card } from "@/components/ui/campo";

/** As etapas seguem o fluxograma do GTD, uma pergunta por vez. */
type Etapa =
  | "acionavel"
  | "nao-acionavel"
  | "referencia"
  | "mais-de-uma-acao"
  | "projeto"
  | "dois-minutos"
  | "sou-eu"
  | "delegar"
  | "tem-data"
  | "agendar"
  | "proxima-acao";

function Pergunta({
  texto,
  dica,
  opcoes,
}: {
  texto: string;
  dica?: string;
  opcoes: { rotulo: string; aoClicar: () => void; variante?: "primario" | "secundario" }[];
}) {
  return (
    <div className="flex flex-col gap-3">
      <div>
        <p className="text-sm font-medium">{texto}</p>
        {dica && <p className="mt-1 text-xs text-giz-apagado">{dica}</p>}
      </div>
      <div className="flex flex-wrap gap-2">
        {opcoes.map((opcao) => (
          <Button
            key={opcao.rotulo}
            variante={opcao.variante ?? "secundario"}
            onClick={opcao.aoClicar}
          >
            {opcao.rotulo}
          </Button>
        ))}
      </div>
    </div>
  );
}

export function Esclarecer() {
  const queryClient = useQueryClient();
  const inbox = useQuery({ queryKey: ["inbox"], queryFn: api.inbox });
  const contextos = useQuery({ queryKey: ["contextos"], queryFn: api.contextos });

  const item: InboxItemDto | undefined = inbox.data?.itens[0];

  const [etapa, setEtapa] = useState<Etapa>("acionavel");
  const [titulo, setTitulo] = useState("");
  const [notas, setNotas] = useState("");
  const [resultadoDesejado, setResultadoDesejado] = useState("");
  const [contextoId, setContextoId] = useState("");
  const [tempoEstimado, setTempoEstimado] = useState("");
  const [energia, setEnergia] = useState<Energia | "">("");
  const [categoria, setCategoria] = useState<Categoria | "">("");
  const [delegadaPara, setDelegadaPara] = useState("");
  const [agendadaPara, setAgendadaPara] = useState("");
  const [url, setUrl] = useState("");
  const [tags, setTags] = useState("");

  // Cada item comeca do zero, com o texto capturado servindo de titulo provisorio.
  useEffect(() => {
    setEtapa("acionavel");
    setTitulo(item?.texto ?? "");
    setNotas("");
    setResultadoDesejado("");
    setContextoId("");
    setTempoEstimado("");
    setEnergia("");
    setCategoria("");
    setDelegadaPara("");
    setAgendadaPara("");
    setUrl("");
    setTags("");
  }, [item?.id, item?.texto]);

  const processar = useMutation({
    mutationFn: (decisao: Decisao) => api.processar(item!.id, decisao),
    onSuccess: () => queryClient.invalidateQueries(),
  });

  if (inbox.isLoading) {
    return <p className="text-sm text-giz-apagado">Carregando inbox...</p>;
  }

  if (!item) {
    return (
      <Card className="flex items-center gap-3">
        <CheckCircle2 className="size-5 text-aferido" />
        <div>
          <p className="text-sm font-medium">Inbox zerado</p>
          <p className="text-xs text-giz-apagado">Nada esperando esclarecimento. Ctrl+K para capturar.</p>
        </div>
      </Card>
    );
  }

  const dadosAcao = (estado: EstadoAcao) => ({
    titulo: titulo.trim() || item.texto,
    notas: notas.trim() || null,
    estado,
    contextoId: contextoId === "" ? null : Number(contextoId),
    tempoEstimadoMin: tempoEstimado === "" ? null : Number(tempoEstimado),
    energia: energia === "" ? null : energia,
    categoria: categoria === "" ? null : categoria,
    agendadaPara: agendadaPara === "" ? null : new Date(agendadaPara).toISOString(),
    delegadaPara: delegadaPara.trim() || null,
  });

  const camposDaAcao = (
    <div className="grid gap-3 sm:grid-cols-2">
      <Campo rotulo="Qual e a proxima acao fisica?" className="sm:col-span-2">
        <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
      </Campo>
      <Campo rotulo="Contexto">
        <Select value={contextoId} onChange={(e) => setContextoId(e.target.value)}>
          <option value="">-</option>
          {contextos.data?.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nome}
            </option>
          ))}
        </Select>
      </Campo>
      <Campo rotulo="Tempo estimado (min)">
        <Input type="number" min={1} value={tempoEstimado} onChange={(e) => setTempoEstimado(e.target.value)} />
      </Campo>
      <Campo rotulo="Energia exigida">
        <Select value={energia} onChange={(e) => setEnergia(e.target.value as Energia)}>
          <option value="">-</option>
          <option value="BAIXA">baixa</option>
          <option value="MEDIA">media</option>
          <option value="ALTA">alta</option>
        </Select>
      </Campo>
      <Campo rotulo="Categoria (para virar registro depois)">
        <Select value={categoria} onChange={(e) => setCategoria(e.target.value as Categoria)}>
          <option value="">-</option>
          {CATEGORIAS.map((c) => (
            <option key={c} value={c}>
              {c}
            </option>
          ))}
        </Select>
      </Campo>
      <Campo rotulo="Notas" className="sm:col-span-2">
        <Textarea rows={2} value={notas} onChange={(e) => setNotas(e.target.value)} />
      </Campo>
    </div>
  );

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <p className="text-[0.8125rem] text-giz-apagado">
          Esclarecendo · {inbox.data?.pendentes} na fila
        </p>
        <p className="mt-2 text-base">{item.texto}</p>
        <p className="mt-1 text-xs text-giz-apagado">
          capturado em {new Date(item.capturadoEm).toLocaleString("pt-BR")}
        </p>
      </Card>

      <Card>
        {etapa === "acionavel" && (
          <Pergunta
            texto="Isso e acionavel?"
            dica="Existe alguma acao fisica que faz isso andar?"
            opcoes={[
              { rotulo: "Sim", aoClicar: () => setEtapa("mais-de-uma-acao"), variante: "primario" },
              { rotulo: "Nao", aoClicar: () => setEtapa("nao-acionavel") },
            ]}
          />
        )}

        {etapa === "nao-acionavel" && (
          <Pergunta
            texto="Entao o que fazer com isso?"
            opcoes={[
              { rotulo: "Lixo", aoClicar: () => processar.mutate({ destino: "LIXO" }) },
              {
                rotulo: "Algum dia/talvez",
                aoClicar: () =>
                  processar.mutate({ destino: "ALGUM_DIA", acao: dadosAcao("ALGUM_DIA") }),
              },
              { rotulo: "Guardar como referencia", aoClicar: () => setEtapa("referencia") },
              { rotulo: "Voltar", aoClicar: () => setEtapa("acionavel") },
            ]}
          />
        )}

        {etapa === "referencia" && (
          <div className="flex flex-col gap-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <Campo rotulo="Titulo" className="sm:col-span-2">
                <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
              </Campo>
              <Campo rotulo="URL">
                <Input value={url} onChange={(e) => setUrl(e.target.value)} placeholder="https://" />
              </Campo>
              <Campo rotulo="Tags (separadas por virgula)">
                <Input value={tags} onChange={(e) => setTags(e.target.value)} />
              </Campo>
              <Campo rotulo="Conteudo" className="sm:col-span-2">
                <Textarea rows={3} value={notas} onChange={(e) => setNotas(e.target.value)} />
              </Campo>
            </div>
            <div className="flex gap-2">
              <Button
                onClick={() =>
                  processar.mutate({
                    destino: "REFERENCIA",
                    referencia: {
                      titulo: titulo.trim() || item.texto,
                      conteudo: notas.trim() || null,
                      url: url.trim() || null,
                      tags: tags
                        .split(",")
                        .map((t) => t.trim())
                        .filter(Boolean),
                    },
                  })
                }
              >
                Arquivar
              </Button>
              <Button variante="secundario" onClick={() => setEtapa("nao-acionavel")}>
                Voltar
              </Button>
            </div>
          </div>
        )}

        {etapa === "mais-de-uma-acao" && (
          <Pergunta
            texto="Exige mais de uma acao?"
            dica="Se sim, isso e um projeto — e projeto precisa de uma proxima acao para nao parar."
            opcoes={[
              { rotulo: "Sim, e um projeto", aoClicar: () => setEtapa("projeto") },
              { rotulo: "Nao, e uma acao so", aoClicar: () => setEtapa("dois-minutos"), variante: "primario" },
              { rotulo: "Voltar", aoClicar: () => setEtapa("acionavel") },
            ]}
          />
        )}

        {etapa === "projeto" && (
          <div className="flex flex-col gap-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <Campo rotulo="Nome do projeto">
                <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
              </Campo>
              <Campo rotulo="Qual o resultado desejado?">
                <Input
                  value={resultadoDesejado}
                  onChange={(e) => setResultadoDesejado(e.target.value)}
                  placeholder="como sera quando estiver pronto"
                />
              </Campo>
            </div>
            <p className="text-[0.8125rem] text-giz-fraco">
              Primeira proxima acao
            </p>
            <Campo rotulo="Acao">
              <Input value={notas} onChange={(e) => setNotas(e.target.value)} placeholder="opcional" />
            </Campo>
            <div className="flex gap-2">
              <Button
                onClick={() =>
                  processar.mutate({
                    destino: "PROJETO",
                    projeto: {
                      titulo: titulo.trim() || item.texto,
                      resultadoDesejado: resultadoDesejado.trim() || null,
                    },
                    acao:
                      notas.trim() === ""
                        ? undefined
                        : {
                            titulo: notas.trim(),
                            estado: "PROXIMA",
                            contextoId: contextoId === "" ? null : Number(contextoId),
                          },
                  })
                }
              >
                Criar projeto
              </Button>
              <Button variante="secundario" onClick={() => setEtapa("mais-de-uma-acao")}>
                Voltar
              </Button>
            </div>
          </div>
        )}

        {etapa === "dois-minutos" && (
          <Pergunta
            texto="Leva menos de dois minutos?"
            dica="Se leva, sai mais barato fazer agora do que organizar para depois."
            opcoes={[
              {
                rotulo: "Sim, ja fiz",
                aoClicar: () => processar.mutate({ destino: "FEITO_2MIN", acao: dadosAcao("CONCLUIDA") }),
                variante: "primario",
              },
              { rotulo: "Nao", aoClicar: () => setEtapa("sou-eu") },
              { rotulo: "Voltar", aoClicar: () => setEtapa("mais-de-uma-acao") },
            ]}
          />
        )}

        {etapa === "sou-eu" && (
          <Pergunta
            texto="Voce e quem deve fazer?"
            opcoes={[
              { rotulo: "Sim", aoClicar: () => setEtapa("tem-data"), variante: "primario" },
              { rotulo: "Nao, vou delegar", aoClicar: () => setEtapa("delegar") },
              { rotulo: "Voltar", aoClicar: () => setEtapa("dois-minutos") },
            ]}
          />
        )}

        {etapa === "delegar" && (
          <div className="flex flex-col gap-3">
            <Campo rotulo="Com quem fica?">
              <Input
                value={delegadaPara}
                onChange={(e) => setDelegadaPara(e.target.value)}
                placeholder="nome ou area"
              />
            </Campo>
            <Campo rotulo="O que voce espera receber">
              <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
            </Campo>
            <div className="flex gap-2">
              <Button
                disabled={delegadaPara.trim() === ""}
                onClick={() => processar.mutate({ destino: "ACAO", acao: dadosAcao("AGUARDANDO") })}
              >
                Mandar para Aguardando
              </Button>
              <Button variante="secundario" onClick={() => setEtapa("sou-eu")}>
                Voltar
              </Button>
            </div>
          </div>
        )}

        {etapa === "tem-data" && (
          <Pergunta
            texto="Tem dia e hora marcados?"
            dica="Agenda e so para o que acontece num momento especifico. O resto e proxima acao."
            opcoes={[
              { rotulo: "Sim", aoClicar: () => setEtapa("agendar") },
              { rotulo: "Nao", aoClicar: () => setEtapa("proxima-acao"), variante: "primario" },
              { rotulo: "Voltar", aoClicar: () => setEtapa("sou-eu") },
            ]}
          />
        )}

        {etapa === "agendar" && (
          <div className="flex flex-col gap-3">
            <Campo rotulo="Quando">
              <Input
                type="datetime-local"
                value={agendadaPara}
                onChange={(e) => setAgendadaPara(e.target.value)}
              />
            </Campo>
            <Campo rotulo="O que">
              <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
            </Campo>
            <div className="flex gap-2">
              <Button
                disabled={agendadaPara === ""}
                onClick={() => processar.mutate({ destino: "ACAO", acao: dadosAcao("AGENDA") })}
              >
                Mandar para a Agenda
              </Button>
              <Button variante="secundario" onClick={() => setEtapa("tem-data")}>
                Voltar
              </Button>
            </div>
          </div>
        )}

        {etapa === "proxima-acao" && (
          <div className="flex flex-col gap-3">
            {camposDaAcao}
            <div className="flex gap-2">
              <Button onClick={() => processar.mutate({ destino: "ACAO", acao: dadosAcao("PROXIMA") })}>
                Mandar para Proximas acoes
              </Button>
              <Button variante="secundario" onClick={() => setEtapa("tem-data")}>
                Voltar
              </Button>
            </div>
          </div>
        )}

        {processar.isError && (
          <p className="mt-3 text-sm text-giz-fraco">
            {processar.error instanceof Error ? processar.error.message : "Nao deu para processar."}
          </p>
        )}
      </Card>
    </div>
  );
}
