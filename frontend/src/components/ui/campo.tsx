import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function Campo({
  rotulo,
  children,
  className,
  dica,
}: {
  rotulo: string;
  children: ReactNode;
  className?: string;
  dica?: string;
}) {
  return (
    <label className={cn("flex flex-col gap-1.5", className)}>
      <span className="text-xs font-medium uppercase tracking-wide text-zinc-400">{rotulo}</span>
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
