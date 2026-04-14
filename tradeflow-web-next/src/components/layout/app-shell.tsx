"use client"

import { useEffect } from "react"

import { Sidebar } from "@/components/layout/sidebar"
import { Topbar } from "@/components/layout/topbar"
import { useUIStore } from "@/stores/ui-store"

export function AppShell({
  activePanel,
  children,
}: {
  activePanel: "workspace" | "history" | "reports" | "settings"
  children: React.ReactNode
}) {
  const setActivePanel = useUIStore((state) => state.setActivePanel)

  useEffect(() => {
    setActivePanel(activePanel)
  }, [activePanel, setActivePanel])

  return (
    <div className="flex h-screen overflow-hidden bg-[var(--tf-bg-page)] text-[var(--tf-text)]">
      <Sidebar />
      <div className="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
        <Topbar />
        <main className="min-h-0 flex-1 overflow-auto p-4">{children}</main>
      </div>
    </div>
  )
}
