import type { ComponentProps } from "react";
import { cn } from "@/lib/utils";

export function Input({ className, ...props }: ComponentProps<"input">) {
  return (
    <input
      className={cn(
        "h-9 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm placeholder:text-zinc-500 focus:border-emerald-600 focus:outline-none",
        className,
      )}
      {...props}
    />
  );
}

export function Textarea({ className, ...props }: ComponentProps<"textarea">) {
  return (
    <textarea
      className={cn(
        "w-full rounded-md border border-zinc-800 bg-zinc-900 p-3 text-sm placeholder:text-zinc-500 focus:border-emerald-600 focus:outline-none",
        className,
      )}
      {...props}
    />
  );
}

/* Select nativo: um dropdown do proprio browser resolve o caso todo, sem biblioteca de popover. */
export function Select({ className, ...props }: ComponentProps<"select">) {
  return (
    <select
      className={cn(
        "h-9 w-full rounded-md border border-zinc-800 bg-zinc-900 px-2 text-sm focus:border-emerald-600 focus:outline-none",
        className,
      )}
      {...props}
    />
  );
}
