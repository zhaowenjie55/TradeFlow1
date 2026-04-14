import * as React from "react"

import { cn } from "@/lib/utils"

export function Badge({
  className,
  variant = "default",
  ...props
}: React.HTMLAttributes<HTMLSpanElement> & { variant?: "default" | "secondary" | "outline" }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium",
        variant === "default" && "bg-[var(--tf-accent-soft)] text-[var(--tf-accent-strong)]",
        variant === "secondary" && "bg-[var(--tf-bg-soft)] text-[var(--tf-text-muted)]",
        variant === "outline" && "border border-[var(--tf-border)] text-[var(--tf-text-muted)]",
        className,
      )}
      {...props}
    />
  )
}
