import * as React from "react"

import { cn } from "@/lib/utils"

export const Textarea = React.forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(
  ({ className, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        "flex min-h-[120px] w-full rounded-2xl border border-[var(--tf-border)] bg-white px-4 py-3 text-sm text-[var(--tf-text)] outline-none transition placeholder:text-[var(--tf-text-subtle)] focus:border-[var(--tf-accent)] focus:ring-2 focus:ring-[var(--tf-accent-soft)]",
        className,
      )}
      {...props}
    />
  ),
)

Textarea.displayName = "Textarea"
