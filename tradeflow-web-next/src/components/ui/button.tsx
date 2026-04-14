"use client"

import * as React from "react"

import { cn } from "@/lib/utils"

type ButtonVariant = "default" | "secondary" | "outline" | "ghost" | "destructive"
type ButtonSize = "default" | "sm" | "lg" | "icon"

const variantClasses: Record<ButtonVariant, string> = {
  default: "bg-[var(--tf-accent)] text-white hover:bg-[var(--tf-accent-strong)]",
  secondary: "bg-[var(--tf-accent-soft)] text-[var(--tf-text)] hover:bg-[var(--tf-accent-soft-hover)]",
  outline: "border border-[var(--tf-border)] bg-white text-[var(--tf-text)] hover:bg-[var(--tf-bg-soft)]",
  ghost: "text-[var(--tf-text-muted)] hover:bg-[var(--tf-bg-soft)] hover:text-[var(--tf-text)]",
  destructive: "bg-red-500 text-white hover:bg-red-600",
}

const sizeClasses: Record<ButtonSize, string> = {
  default: "h-11 px-4 py-2 text-sm",
  sm: "h-9 rounded-xl px-3 text-sm",
  lg: "h-12 rounded-2xl px-5 text-sm",
  icon: "size-10 rounded-xl",
}

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "default", size = "default", type = "button", ...props }, ref) => (
    <button
      ref={ref}
      type={type}
      className={cn(
        "inline-flex shrink-0 items-center justify-center gap-2 whitespace-nowrap rounded-2xl font-medium transition disabled:pointer-events-none disabled:opacity-50",
        variantClasses[variant],
        sizeClasses[size],
        className,
      )}
      {...props}
    />
  ),
)

Button.displayName = "Button"
