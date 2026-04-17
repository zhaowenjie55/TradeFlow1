import type { ReactNode } from "react"

import { cn } from "@/lib/utils"

interface SoftGlowBackgroundProps {
  children: ReactNode
  className?: string
  contentClassName?: string
}

export function SoftGlowBackground({ children, className, contentClassName }: SoftGlowBackgroundProps) {
  return (
    <div className={cn("relative min-h-dvh w-full overflow-hidden bg-[#fffdf5]", className)}>
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 z-0"
        style={{
          backgroundImage: "radial-gradient(circle at center, #fff991 0%, rgba(255, 249, 145, 0.38) 34%, transparent 72%)",
          opacity: 0.58,
          mixBlendMode: "multiply",
        }}
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 z-0 bg-[radial-gradient(circle_at_68%_38%,rgba(255,255,255,0.92),transparent_42%),linear-gradient(135deg,rgba(255,255,255,0.76),transparent_46%)]"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 z-0 bg-[linear-gradient(rgba(45,51,44,0.035)_1px,transparent_1px),linear-gradient(90deg,rgba(45,51,44,0.035)_1px,transparent_1px)] bg-[size:56px_56px]"
      />
      <div className={cn("relative z-10", contentClassName)}>{children}</div>
    </div>
  )
}
