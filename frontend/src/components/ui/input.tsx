import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

/*
 * Campos sem caixa: fundo do proprio painel e um fio embaixo, como linha de formulario de folha de
 * medicao. Caixa arredondada em volta de cada input era o que fazia o formulario parecer os outros.
 */
const base =
  "w-full border-0 border-b border-risco bg-transparent text-sm text-giz placeholder:text-giz-apagado " +
  "focus:border-aferido focus:outline-none focus:ring-0";

export function Input({ className, ...props }: ComponentProps<"input">) {
  return <input className={cn(base, "medida h-8 px-0.5", className)} {...props} />;
}

export function Textarea({ className, ...props }: ComponentProps<"textarea">) {
  return <textarea className={cn(base, "resize-y px-0.5 py-1.5", className)} {...props} />;
}

/* Select nativo: um dropdown do proprio browser resolve o caso todo, sem biblioteca de popover. */
export function Select({ className, ...props }: ComponentProps<"select">) {
  return <select className={cn(base, "h-8 px-0.5", className)} {...props} />;
}
