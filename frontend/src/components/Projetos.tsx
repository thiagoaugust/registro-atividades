import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronDown, ChevronRight, FolderKanban, Plus } from "lucide-react";
import { api, ErroApi, type ProgressoProjetoDto, type StatusProjeto } from "@/api";
import { Button } from "@/components/ui/button";
import { Input, Select, Textarea } from "@/components/ui/input";
import { Campo, Card, Secao } from "@/components/ui/campo";
import { ItemAcao } from "@/components/ListasGtd";
import { cn } from "@/lib/utils";

const GRUPOS: { status: StatusProjeto; rotulo: string }[] = [
  { status: "ATIVO", rotulo: "Ativos" },
  { status: "PAUSADO", rotulo: "Pausados" },
  { status: "CONCLUIDO", rotulo: "Concluidos" },
];

function NovoProjeto({ aoTerminar }: { aoTerminar: () => void }) {
  const [titulo, setTitulo] = useState("");
  const [resultado, setResultado] = useState("");
  const [erro, setErro] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const criar = useMutation({
    mutationFn: () =>
      api.criarProjeto({ titulo: titulo.trim(), resultadoDesejado: resultado.trim() || null }),
    onSuccess: () => {
      queryClient.invalidateQueries();
      aoTerminar();
    },
    onError: (e) => setErro(e instanceof ErroApi ? e.detalhe : "Nao deu para criar o projeto."),
  });

  return (
    <Card>
      <form
        className="grid gap-3"
        onSubmit={(evento) => {
          evento.preventDefault();
          criar.mutate();
        }}
      >
        <Campo rotulo="Projeto">
          <Input value={titulo} onChange={(e) => setTitulo(e.target.value)} autoFocus required />
        </Campo>
        <Campo
          rotulo="Resultado desejado"
          ajuda="Como fica o mundo quando o projeto acabar. E o que diz se ele foi concluido — nao a lista de tarefas."
        >
          <Textarea rows={2} value={resultado} onChange={(e) => setResultado(e.target.value)} />
        </Campo>
        <div className="flex items-center gap-2">
          <Button type="submit" disabled={criar.isPending || titulo.trim() === ""}>
            Criar projeto
          </Button>
          <Button type="button" variante="fantasma" onClick={aoTerminar}>
            Cancelar
          </Button>
        </div>
        {erro && <p className="text-sm text-giz-fraco">{erro}</p>}
      </form>
    </Card>
  );
}

function CartaoProjeto({ projeto }: { projeto: ProgressoProjetoDto }) {
  // Ativo abre com as tarefas a mostra; pausado e concluido ficam so com a barra ate serem abertos.
  const [aberto, setAberto] = useState(projeto.status === "ATIVO");
  const [verFeitas, setVerFeitas] = useState(false);
  const [novaTarefa, setNovaTarefa] = useState("");
  const queryClient = useQueryClient();

  const acoes = useQuery({
    queryKey: ["acoes-projeto", projeto.projetoId],
    queryFn: () => api.acoes({ projeto: projeto.projetoId }),
    enabled: aberto,
  });

  const invalidar = () => queryClient.invalidateQueries();

  const mudarStatus = useMutation({
    mutationFn: (status: StatusProjeto) =>
      api.atualizarProjeto(projeto.projetoId, {
        titulo: projeto.titulo,
        resultadoDesejado: projeto.resultadoDesejado,
        status,
      }),
    onSuccess: invalidar,
  });

  const adicionar = useMutation({
    mutationFn: () =>
      api.criarAcao({ titulo: novaTarefa.trim(), estado: "PROXIMA", projetoId: projeto.projetoId }),
    onSuccess: () => {
      setNovaTarefa("");
      invalidar();
    },
  });

  const concluir = useMutation({ mutationFn: api.concluirAcao, onSuccess: invalidar });
  const reabrir = useMutation({ mutationFn: api.reabrirAcao, onSuccess: invalidar });
  const excluir = useMutation({ mutationFn: api.excluirAcao, onSuccess: invalidar });

  // O nome do projeto em cada tarefa repetiria o titulo do cartao.
  const tarefas = (acoes.data ?? []).map((acao) => ({ ...acao, projeto: null }));
  const faltam = tarefas.filter((a) => a.estado !== "CONCLUIDA" && a.estado !== "DESCARTADA");
  const feitas = tarefas.filter((a) => a.estado === "CONCLUIDA");
  const tudoFeito = projeto.tarefas > 0 && projeto.faltam === 0;

  return (
    <div className="flex flex-col gap-3 border-b border-risco/60 pb-4">
      <div className="flex items-start justify-between gap-3">
        <button
          type="button"
          onClick={() => setAberto(!aberto)}
          aria-expanded={aberto}
          className="flex min-w-0 items-start gap-1.5 text-left"
        >
          {aberto ? (
            <ChevronDown className="mt-0.5 size-4 shrink-0 text-giz-apagado" />
          ) : (
            <ChevronRight className="mt-0.5 size-4 shrink-0 text-giz-apagado" />
          )}
          <span className="min-w-0">
            <span className="block text-sm font-medium">{projeto.titulo}</span>
            {projeto.resultadoDesejado && (
              <span className="block text-xs text-giz-apagado">{projeto.resultadoDesejado}</span>
            )}
          </span>
        </button>
        <Select
          value={projeto.status}
          onChange={(e) => mudarStatus.mutate(e.target.value as StatusProjeto)}
          className="h-8 w-32 shrink-0 text-xs"
          aria-label={`Status de ${projeto.titulo}`}
        >
          <option value="ATIVO">ativo</option>
          <option value="PAUSADO">pausado</option>
          <option value="CONCLUIDO">concluido</option>
          <option value="ARQUIVADO">arquivar</option>
        </Select>
      </div>

      <div>
        <div className="flex items-baseline justify-between text-xs">
          <span className="text-giz-fraco">
            {projeto.tarefas === 0
              ? "sem tarefas"
              : `${projeto.concluidas} de ${projeto.tarefas} tarefas`}
            {projeto.faltam > 0 && <span className="text-latao"> · faltam {projeto.faltam}</span>}
          </span>
          <span className="medida text-giz-fraco">
            {projeto.percentual !== null ? `${projeto.percentual}%` : "—"}
          </span>
        </div>
        <div className="mt-1 h-0.5 overflow-hidden bg-risco">
          <div className="h-full bg-aferido" style={{ width: `${projeto.percentual ?? 0}%` }} />
        </div>
      </div>

      {tudoFeito && projeto.status === "ATIVO" && (
        <div className="flex flex-wrap items-center gap-2 text-xs text-giz-fraco">
          Todas as tarefas feitas. O resultado desejado foi atingido?
          <Button tamanho="sm" onClick={() => mudarStatus.mutate("CONCLUIDO")}>
            Concluir projeto
          </Button>
        </div>
      )}

      {aberto && (
        <div className="flex flex-col gap-2 pl-5">
          {acoes.isLoading && <p className="text-xs text-giz-apagado">Carregando tarefas...</p>}

          {faltam.length > 0 && (
            <div>
              <p className="text-[0.8125rem] text-giz-fraco">Falta</p>
              {faltam.map((acao) => (
                <ItemAcao
                  key={acao.id}
                  acao={acao}
                  aoConcluir={(id) => concluir.mutate(id)}
                  aoExcluir={(id) => excluir.mutate(id)}
                />
              ))}
            </div>
          )}

          <form
            className="flex gap-2"
            onSubmit={(evento) => {
              evento.preventDefault();
              if (novaTarefa.trim() !== "") adicionar.mutate();
            }}
          >
            <Input
              value={novaTarefa}
              onChange={(e) => setNovaTarefa(e.target.value)}
              placeholder="Nova tarefa para concluir o projeto"
              aria-label={`Nova tarefa em ${projeto.titulo}`}
            />
            <Button
              type="submit"
              variante="secundario"
              disabled={adicionar.isPending || novaTarefa.trim() === ""}
            >
              <Plus className="size-4" />
              Adicionar
            </Button>
          </form>

          {feitas.length > 0 && (
            <div>
              <button
                type="button"
                onClick={() => setVerFeitas(!verFeitas)}
                className="text-[0.8125rem] text-giz-apagado hover:text-giz"
              >
                {verFeitas ? "esconder" : "ver"} {feitas.length} feita(s)
              </button>
              {verFeitas &&
                feitas.map((acao) => (
                  <ItemAcao key={acao.id} acao={acao} aoReabrir={(id) => reabrir.mutate(id)} />
                ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export function Projetos() {
  const [criando, setCriando] = useState(false);
  const projetos = useQuery({ queryKey: ["progresso-projetos"], queryFn: api.progressoProjetos });

  const grupos = GRUPOS.map((g) => ({
    ...g,
    projetos: projetos.data?.filter((p) => p.status === g.status) ?? [],
  })).filter((g) => g.projetos.length > 0);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <p className="flex items-center gap-2 text-sm font-medium">
          <FolderKanban className="size-4 text-giz" />
          Projetos
        </p>
        <Button tamanho="sm" onClick={() => setCriando(!criando)}>
          <Plus className="size-4" />
          Novo projeto
        </Button>
      </div>

      {criando && <NovoProjeto aoTerminar={() => setCriando(false)} />}

      {projetos.isLoading && <p className="text-sm text-giz-apagado">Carregando...</p>}
      {!projetos.isLoading && grupos.length === 0 && (
        <p className="py-2 text-sm text-giz-apagado">
          Nenhum projeto. Crie um e liste as tarefas que faltam para concluir.
        </p>
      )}

      {grupos.map((grupo, indice) => (
        <section key={grupo.status} className={cn("flex flex-col gap-4", indice > 0 && "mt-2")}>
          <Secao>{grupo.rotulo}</Secao>
          {grupo.projetos.map((projeto) => (
            <CartaoProjeto key={projeto.projetoId} projeto={projeto} />
          ))}
        </section>
      ))}
    </div>
  );
}
