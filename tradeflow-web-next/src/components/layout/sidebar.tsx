"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import { Boxes, ClipboardList, History, Settings2 } from "lucide-react"

import { useAppI18n } from "@/components/layout/locale-provider"
import { cn } from "@/lib/utils"
import { useUIStore } from "@/stores/ui-store"

const items = [
  { key: "workspace", href: "/", icon: Boxes, labelKey: "nav.workspace" },
  { key: "history", href: "/history", icon: History, labelKey: "nav.history" },
  { key: "reports", href: "/reports", icon: ClipboardList, labelKey: "nav.reports" },
  { key: "settings", href: "/settings", icon: Settings2, labelKey: "nav.settings" },
] as const

export function Sidebar() {
  const { t } = useAppI18n()
  const activePanel = useUIStore((state) => state.activePanel)
  const setActivePanel = useUIStore((state) => state.setActivePanel)
  const pathname = usePathname()

  return (
    <aside className="flex h-full w-16 shrink-0 flex-col items-center gap-2 border-r border-[var(--tf-border)] bg-[var(--tf-bg-panel)] px-2 py-4">
      <div className="flex size-10 items-center justify-center rounded-2xl bg-[var(--tf-accent-soft)] text-xs font-bold text-[var(--tf-accent-strong)]">
        TF
      </div>
      <div className="mt-2 flex flex-col gap-1.5">
        {items.map((item) => {
          const Icon = item.icon
          const active =
            pathname === item.href || (item.href !== "/" && pathname.startsWith(`${item.href}/`)) || activePanel === item.key
          return (
            <Link
              key={item.key}
              href={item.href}
              onClick={() => setActivePanel(item.key)}
              className={cn(
                "flex size-10 items-center justify-center rounded-2xl border transition",
                active
                  ? "border-transparent bg-[var(--tf-accent-soft)] text-[var(--tf-accent-strong)]"
                  : "border-transparent text-[var(--tf-text-subtle)] hover:bg-[var(--tf-bg-soft)] hover:text-[var(--tf-text)]",
              )}
              title={t(item.labelKey)}
            >
              <Icon className="size-5" />
            </Link>
          )
        })}
      </div>
    </aside>
  )
}
