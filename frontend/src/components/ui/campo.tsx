import { useId, type ReactNode } from "react";
import { HelpCircle } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Explicacao do campo, no hover e tambem no foco por teclado — um tooltip so de :hover deixa de
 * fora quem navega por tab. O texto responde "para que serve", nao repete o rotulo.
 */
export function Ajuda({ texto }: { texto: string }) {
  const id = useId();

  return (
    <span className="group relative inline-flex align-middle">
      <button
        type="button"
        aria-label={`O que e este campo: ${texto}`}
        aria-describedby={id}
        // O botao vive dentro do <label>: sem o preventDefault, clicar no "?" abriria o select
        // ou focaria o input ao lado.
        onClick={(evento) => evento.preventDefault()}
        className="inline-flex cursor-help text-zinc-600 transition-colors hover:text-zinc-300 focus:text-zinc-300 focus:outline-none"
      >
        <HelpCircle className="size-3.5" />
      </button>

      <span
        id={id}
        role="tooltip"
        className="pointer-events-none absolute left-0 top-5 z-50 w-60 rounded-md border border-zinc-700 bg-zinc-950 p-2 text-xs font-normal normal-case leading-snug tracking-normal text-zinc-300 opacity-0 shadow-lg transition-opacity group-focus-within:opacity-100 group-hover:opacity-100"
      >
        {texto}
      </span>
    </span>
  );
}

export function Campo({
  rotulo,
  children,
  className,
  dica,
  ajuda,
}: {
  rotulo: string;
  children: ReactNode;
  className?: string;
  dica?: string;
  ajuda?: string;
}) {
  return (
    <label className={cn("flex flex-col gap-1.5", className)}>
      <span className="flex items-center gap-1.5 text-xs font-medium uppercase tracking-wide text-zinc-400">
        {rotulo}
        {ajuda && <Ajuda texto={ajuda} />}
      </span>
      {children}
      {dica && <span className="text-xs text-zinc-500">{dica}</span>}
    </label>
  );
}

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cn("rounded-lg border border-zinc-800 bg-zinc-900/50 p-4", className)}>
      {children}
    </div>
  );
}
