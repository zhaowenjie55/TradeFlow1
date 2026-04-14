import * as React from "react"

import { cn } from "@/lib/utils"

export const Input = React.forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  ({ className, ...props }, ref) => (
    <input
      ref={ref}
      className={cn(
        "flex h-12 w-full rounded-2xl border border-[var(--tf-border)] bg-white px-4 text-sm text-[var(--tf-text)] outline-none transition placeholder:text-[var(--tf-text-subtle)] focus:border-[var(--tf-accent)] focus:ring-2 focus:ring-[var(--tf-accent-soft)]",
        className,
      )}
      {...props}
    />
  ),
)

Input.displayName = "Input"
