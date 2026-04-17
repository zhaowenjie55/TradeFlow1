"use client"

import { useEffect, useMemo, useRef, useState } from "react"

import { Button } from "@/components/ui/button"
import { useAppI18n } from "@/components/layout/locale-provider"
import { useAgentStore } from "@/stores/agent-store"
import { useTaskStore } from "@/stores/task-store"
import { SEARCH_PHASE_CONFIG } from "@/components/agent/search-processing-workflow"
import { getLiveStageMessage, humanizeTaskLog, isKeyTaskLog, resolveTaskLogCategory } from "@/lib/presentation"

function formatTaskTime(timestamp: string) {
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return "--:--:--"
  const hh = String(date.getHours()).padStart(2, "0")
  const mm = String(date.getMinutes()).padStart(2, "0")
  const ss = String(date.getSeconds()).padStart(2, "0")
  return `${hh}:${mm}:${ss}`
}

export function ThinkingLog() {
  const { t } = useAppI18n()
  const logs = useAgentStore((state) => state.taskLogs)
  const stagedReasoningLogs = useAgentStore((state) => state.stagedReasoningLogs)
  const searchExperienceActive = useAgentStore((state) => state.searchExperienceActive)
  const isPolling = useTaskStore((state) => state.isPolling)
  const stage = useTaskStore((state) => state.stage)
  const [filter, setFilter] = useState<"all" | "key">("all")
  const [typedLengths, setTypedLengths] = useState<Record<string, number>>({})
  const [autoFollowEnabled, setAutoFollowEnabled] = useState(true)

  const visibleLogs = useMemo(() => (filter === "all" ? logs : logs.filter(isKeyTaskLog)), [filter, logs])
  const showOnlyStagedReasoning = searchExperienceActive && stagedReasoningLogs.length > 0

  const containerRef = useRef<HTMLDivElement>(null)
  const isUserScrolledRef = useRef(false)
  const previousKeysRef = useRef<string[]>([])

  const keyedLogs = useMemo(
    () =>
      visibleLogs.map((entry, index) => {
        const key = `${entry.timestamp}-${entry.stage}-${index}`
        return { key, entry, view: humanizeTaskLog(entry), category: resolveTaskLogCategory(entry) }
      }),
    [visibleLogs],
  )

  useEffect(() => {
    const el = containerRef.current
    if (!el) return
    const onScroll = () => {
      const nextUserScrolled = el.scrollTop + el.clientHeight < el.scrollHeight - 40
      isUserScrolledRef.current = nextUserScrolled
      setAutoFollowEnabled(!nextUserScrolled)
    }
    onScroll()
    el.addEventListener("scroll", onScroll, { passive: true })
    return () => el.removeEventListener("scroll", onScroll)
  }, [])

  useEffect(() => {
    if (!isUserScrolledRef.current && containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight
    }
  }, [keyedLogs, typedLengths, isPolling, stagedReasoningLogs])

  useEffect(() => {
    const nextKeys = keyedLogs.map((item) => item.key)
    const previousKeys = previousKeysRef.current

    if (previousKeys.length === 0) {
      previousKeysRef.current = nextKeys
      return
    }

    const appended = keyedLogs.filter((item) => !previousKeys.includes(item.key))
    if (!appended.length) {
      previousKeysRef.current = nextKeys
      return
    }

    const latest = appended[appended.length - 1]
    setTypedLengths((current) => {
      const next = { ...current }
      appended.slice(0, -1).forEach((item) => {
        next[item.key] = item.view.message.length
      })
      next[latest.key] = 0
      return next
    })

    let revealed = 0
    const fullLength = latest.view.message.length
    const timer = window.setInterval(() => {
      revealed = Math.min(fullLength, revealed + 2)
      setTypedLengths((current) => ({ ...current, [latest.key]: revealed }))
      if (revealed >= fullLength) {
        window.clearInterval(timer)
      }
    }, 18)

    previousKeysRef.current = nextKeys
    return () => window.clearInterval(timer)
  }, [keyedLogs])

  if (!visibleLogs.length && !stagedReasoningLogs.length) {
    return (
      <div className="flex h-full min-h-[14rem] items-center justify-center px-6 text-sm text-[var(--tf-text-subtle)]">
        {t("analysisPanel.emptyLogs")}
      </div>
    )
  }

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex items-center justify-between border-b border-[var(--tf-border)] px-3 py-2">
        <div className="inline-flex rounded-xl border border-[var(--tf-border)] bg-white p-0.5">
          <Button
            size="sm"
            variant={filter === "all" ? "secondary" : "ghost"}
            className="h-8 rounded-lg px-2.5 text-xs"
            onClick={() => setFilter("all")}
          >
            {t("logs.filterAll")}
          </Button>
          <Button
            size="sm"
            variant={filter === "key" ? "secondary" : "ghost"}
            className="h-8 rounded-lg px-2.5 text-xs"
            onClick={() => setFilter("key")}
          >
            {t("logs.filterKey")}
          </Button>
        </div>
        <span className="text-[11px] text-[var(--tf-text-subtle)]">
          {autoFollowEnabled ? t("logs.autoFollowOn") : t("logs.autoFollowOff")}
        </span>
      </div>
      <div ref={containerRef} className="tradeflow-scrollbar h-full overflow-auto px-3 py-3">
        <div className="space-y-2">
          {stagedReasoningLogs.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center justify-between px-1 text-[10px] font-semibold uppercase tracking-[0.18em] text-blue-600">
                <span>Agent reasoning</span>
                {searchExperienceActive && <span className="animate-pulse">Active</span>}
              </div>
              {stagedReasoningLogs.map((entry, index) => {
                const isLatest = index === stagedReasoningLogs.length - 1 && searchExperienceActive
                return (
                  <div
                    key={entry.id}
                    className={[
                      "tf-log-entry rounded-2xl border p-3",
                      isLatest
                        ? "border-blue-200 bg-blue-50 text-blue-900"
                        : "border-[var(--tf-border)] bg-white text-[var(--tf-text)]",
                    ].join(" ")}
                  >
                    <div className="mb-1 flex items-center justify-between gap-2 text-[11px] text-[var(--tf-text-subtle)]">
                      <span>{SEARCH_PHASE_CONFIG[entry.phase].label}</span>
                      <span>{formatTaskTime(entry.timestamp)}</span>
                    </div>
                    <p className="text-xs leading-5">
                      {entry.message}
                      {isLatest && <span className="ml-0.5 inline-block animate-pulse">▋</span>}
                    </p>
                  </div>
                )
              })}
            </div>
          )}

          {!showOnlyStagedReasoning && stagedReasoningLogs.length > 0 && keyedLogs.length > 0 && (
            <div className="px-1 pt-2 text-[10px] font-semibold uppercase tracking-[0.18em] text-[var(--tf-text-subtle)]">
              System events
            </div>
          )}

          {!showOnlyStagedReasoning && keyedLogs.map(({ key, entry, view, category }) => {
            const typed = typedLengths[key]
            const message = typeof typed === "number" ? view.message.slice(0, typed) : view.message
            const isStillTyping = typeof typed === "number" && typed < view.message.length
            return (
              <div
                key={key}
                className={[
                  "tf-log-entry rounded-2xl border p-3",
                  category === "alert"
                    ? "border-sky-200 bg-sky-50"
                    : "border-[var(--tf-border)] bg-[var(--tf-bg-soft)]",
                ].join(" ")}
              >
                <div className="mb-1 flex items-center justify-between gap-2 text-[11px] text-[var(--tf-text-subtle)]">
                  <span>{view.stageLabel}</span>
                  <span>{formatTaskTime(entry.timestamp)}</span>
                </div>
                <p className="text-xs leading-5 text-[var(--tf-text)]">
                  {message}
                  {isStillTyping && <span className="ml-0.5 inline-block animate-pulse">▋</span>}
                </p>
              </div>
            )
          })}
          {!showOnlyStagedReasoning && isPolling && (
            <div className="rounded-2xl border border-blue-200 bg-blue-50 p-3">
              <p className="text-xs text-blue-700">
                {getLiveStageMessage(stage)}
                <span className="ml-1 inline-block animate-pulse">▋</span>
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
