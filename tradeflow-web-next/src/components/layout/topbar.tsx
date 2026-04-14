"use client"

import { Monitor, Rows3 } from "lucide-react"

import { Button } from "@/components/ui/button"
import { useAppI18n } from "@/components/layout/locale-provider"
import { useUIStore } from "@/stores/ui-store"

export function Topbar() {
  const { locale, setLocale } = useAppI18n()
  const interfaceMode = useUIStore((state) => state.interfaceMode)
  const setInterfaceMode = useUIStore((state) => state.setInterfaceMode)

  return (
    <header className="flex h-11 items-center justify-between border-b border-[var(--tf-border)] bg-[var(--tf-bg-panel)] px-3.5">
      <div className="flex items-center gap-3">
        <span className="text-[15px] font-semibold text-[var(--tf-text)]">TradeFlow</span>
        <span className="rounded-full bg-[var(--tf-bg-soft)] px-2.5 py-0.5 text-[11px] text-[var(--tf-text-muted)]">
          {locale === "zh-CN" ? "自动降级" : "Auto Fallback"}
        </span>
      </div>
      <div className="flex items-center gap-1">
        <Button
          variant={interfaceMode === "workbench" ? "secondary" : "ghost"}
          size="sm"
          className="h-8 rounded-xl px-2.5 text-xs"
          onClick={() => setInterfaceMode("workbench")}
        >
          <Monitor data-icon="inline-start" className="size-4" />
          {locale === "zh-CN" ? "桌面工作台" : "Workspace"}
        </Button>
        <Button
          variant={interfaceMode === "stream" ? "secondary" : "ghost"}
          size="sm"
          className="h-8 rounded-xl px-2.5 text-xs"
          onClick={() => setInterfaceMode("stream")}
        >
          <Rows3 className="size-4" />
          {locale === "zh-CN" ? "流式浏览" : "Stream View"}
        </Button>
        <Button
          variant={locale === "zh-CN" ? "secondary" : "ghost"}
          size="sm"
          className="h-8 rounded-xl px-2.5 text-xs"
          onClick={() => setLocale("zh-CN")}
        >
          {locale === "zh-CN" ? "语言 中文" : "ZH"}
        </Button>
        <Button
          variant={locale === "en" ? "secondary" : "ghost"}
          size="sm"
          className="h-8 rounded-xl px-2.5 text-xs"
          onClick={() => setLocale("en")}
        >
          EN
        </Button>
      </div>
    </header>
  )
}
