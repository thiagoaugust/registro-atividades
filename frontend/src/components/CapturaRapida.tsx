import { useEffect, useRef, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Inbox } from "lucide-react";
import { api } from "@/api";
import { Input } from "@/components/ui/input";

/**
 * Captura de qualquer tela: Ctrl+K abre, Enter salva, Esc fecha. Nenhuma pergunta na hora da
 * captura — decidir o que a coisa e na hora de anotar e o que faz as pessoas pararem de anotar.
 */
export function CapturaRapida({ pendentes }: { pendentes: number }) {
  const [aberto, setAberto] = useState(false);
  const [texto, setTexto] = useState("");
  const [confirmado, setConfirmado] = useState(false);
  const campo = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  const capturar = useMutation({
    mutationFn: api.capturar,
    onSuccess: () => {
      setTexto("");
      setConfirmado(true);
      setTimeout(() => setConfirmado(false), 1200);
      queryClient.invalidateQueries({ queryKey: ["inbox"] });
      queryClient.invalidateQueries({ queryKey: ["conquistas"] });
      queryClient.invalidateQueries({ queryKey: ["perfil"] });
    },
  });

  useEffect(() => {
    function atalho(evento: KeyboardEvent) {
      if ((evento.ctrlKey || evento.metaKey) && evento.key.toLowerCase() === "k") {
        evento.preventDefault();
        setAberto(true);
      }
      if (evento.key === "Escape") {
        setAberto(false);
      }
    }
    window.addEventListener("keydown", atalho);
    return () => window.removeEventListener("keydown", atalho);
  }, []);

  useEffect(() => {
    if (aberto) {
      campo.current?.focus();
    }
  }, [aberto]);

  if (!aberto) {
    return (
      <button
        type="button"
        onClick={() => setAberto(true)}
        className="flex items-center gap-2 rounded-md border border-risco bg-placa px-3 py-1.5 text-xs text-giz-fraco hover:border-risco-forte"
        title="Capturar (Ctrl+K)"
      >
        <Inbox className="size-4" />
        <span>Capturar</span>
        {pendentes > 0 && (
          <span className="rounded-full bg-latao/20 px-1.5 text-latao">{pendentes}</span>
        )}
        <kbd className="hidden rounded border border-risco-forte px-1 text-[10px] text-giz-apagado sm:inline">
          Ctrl+K
        </kbd>
      </button>
    );
  }

  return (
    <form
      className="flex items-center gap-2"
      onSubmit={(evento) => {
        evento.preventDefault();
        if (texto.trim() !== "") {
          capturar.mutate(texto.trim());
        }
      }}
    >
      <Input
        ref={campo}
        value={texto}
        onChange={(e) => setTexto(e.target.value)}
        onBlur={() => texto.trim() === "" && setAberto(false)}
        placeholder={confirmado ? "capturado!" : "o que esta na sua cabeca?"}
        className="w-64"
      />
      {pendentes > 0 && <span className="text-xs text-latao">{pendentes} no inbox</span>}
    </form>
  );
}
