"use client"

import { CpuArchitecture } from "@/components/ui/cpu-architecture"
import {
  getSearchPhaseIndex,
  SEARCH_PHASE_CONFIG,
  SEARCH_PHASE_SEQUENCE,
  type SearchPhase,
} from "@/components/agent/search-processing-workflow"

interface SearchProcessingPanelProps {
  phase: SearchPhase
  candidateCount?: number
}

export function SearchProcessingPanel({ phase, candidateCount = 0 }: SearchProcessingPanelProps) {
  const phaseConfig = SEARCH_PHASE_CONFIG[phase]
  const currentIndex = getSearchPhaseIndex(phase)

  return (
    <div className="flex h-full min-h-[24rem] items-center justify-center px-2 py-6">
      <div className="w-full max-w-2xl rounded-[28px] border border-slate-200 bg-white px-5 py-6 shadow-[0_18px_60px_rgba(15,23,42,0.07)] sm:px-8 sm:py-8">
        <div className="mx-auto w-full max-w-md">
          <div className="relative mx-auto aspect-[2/1] w-full">
            <div className="absolute inset-x-8 top-1/2 h-16 -translate-y-1/2 rounded-full bg-blue-100/70 blur-3xl" />
            <CpuArchitecture className="relative z-10 h-full w-full text-slate-500" text="AI" />
          </div>
        </div>

        <div className="mt-7 text-center">
          <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-blue-600">
            TradeFlow engine active
          </p>
          <h3 className="mt-3 text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">
            {phaseConfig.panelTitle}
          </h3>
          <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-500">{phaseConfig.panelSubtitle}</p>
        </div>

        <div className="mt-7 grid gap-2 sm:grid-cols-2">
          {SEARCH_PHASE_SEQUENCE.slice(0, -1).map((step, index) => {
            const state = index < currentIndex ? "done" : index === currentIndex ? "active" : "pending"
            return (
              <div
                key={step}
                className={[
                  "flex items-center justify-between rounded-2xl border px-3 py-2.5 text-xs transition-colors",
                  state === "done"
                    ? "border-emerald-200 bg-emerald-50 text-emerald-800"
                    : state === "active"
                      ? "border-blue-200 bg-blue-50 text-blue-800"
                      : "border-slate-200 bg-slate-50 text-slate-500",
                ].join(" ")}
              >
                <span className="font-medium">{SEARCH_PHASE_CONFIG[step].label}</span>
                <span className="font-mono text-[10px] uppercase tracking-[0.12em]">
                  {state === "done" ? "Done" : state === "active" ? "Running" : "Queued"}
                </span>
              </div>
            )
          })}
        </div>

        {phase === "results_streaming" && candidateCount > 0 && (
          <div className="mt-5 rounded-2xl border border-blue-200 bg-blue-50 px-4 py-3 text-center text-xs font-medium text-blue-800">
            Streaming {candidateCount} ranked candidates into the workbench.
          </div>
        )}
      </div>
    </div>
  )
}
