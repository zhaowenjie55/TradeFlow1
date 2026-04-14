"use client"

import type { AnalysisPanelTab, InterfaceMode, LayoutMode, PanelSize } from "@/types"
import { create } from "zustand"

const STORAGE_KEY = "tradeflow-next-ui"
const STORAGE_VERSION = 7

const defaultPanelSize: PanelSize = {
  sideRail: 5,
  workPanel: 21,
  stage: 53,
  analysis: 26,
}

const MIN_PANEL_SIZE = { workPanel: 21, stage: 50, analysis: 25 } as const

function clampPanelSize(stored: Partial<PanelSize>): PanelSize {
  const workPanel = Math.max(stored.workPanel ?? defaultPanelSize.workPanel, MIN_PANEL_SIZE.workPanel)
  const analysis = Math.max(stored.analysis ?? defaultPanelSize.analysis, MIN_PANEL_SIZE.analysis)
  if (workPanel + analysis + MIN_PANEL_SIZE.stage > 100) return { ...defaultPanelSize }
  const stage = Math.max(100 - workPanel - analysis, MIN_PANEL_SIZE.stage)
  return { sideRail: stored.sideRail ?? defaultPanelSize.sideRail, workPanel, stage, analysis }
}

function resolveLayoutMode(width: number): LayoutMode {
  if (width >= 1280) return "desktop"
  if (width >= 768) return "tablet"
  return "mobile"
}

interface UIState {
  panelSize: PanelSize
  activePanel: "workspace" | "history" | "reports" | "settings"
  layoutMode: LayoutMode
  interfaceMode: InterfaceMode
  analysisPanelTab: AnalysisPanelTab
  hydrate: () => void
  setLayoutMode: (width: number) => void
  setPanelSize: (value: Partial<PanelSize>) => void
  setInterfaceMode: (value: InterfaceMode) => void
  setAnalysisPanelTab: (value: AnalysisPanelTab) => void
  setActivePanel: (value: UIState["activePanel"]) => void
}

export const useUIStore = create<UIState>((set) => ({
  panelSize: { ...defaultPanelSize },
  activePanel: "workspace",
  layoutMode: "desktop",
  interfaceMode: "workbench",
  analysisPanelTab: "report",
  hydrate: () => {
    if (typeof window === "undefined") return
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY)
      if (raw) {
        const parsed = JSON.parse(raw) as Partial<Pick<UIState, "analysisPanelTab" | "interfaceMode" | "panelSize">> & {
          version?: number
        }
        if (parsed.version !== STORAGE_VERSION) {
          window.localStorage.setItem(
            STORAGE_KEY,
            JSON.stringify({
              version: STORAGE_VERSION,
              analysisPanelTab: parsed.analysisPanelTab === "logs" ? "logs" : "report",
              interfaceMode: parsed.interfaceMode === "stream" ? "stream" : "workbench",
              panelSize: { ...defaultPanelSize },
            }),
          )
          set({
            analysisPanelTab: parsed.analysisPanelTab === "logs" ? "logs" : "report",
            interfaceMode: parsed.interfaceMode === "stream" ? "stream" : "workbench",
            panelSize: { ...defaultPanelSize },
            layoutMode: resolveLayoutMode(window.innerWidth),
          })
          return
        }
        const clampedPanelSize = clampPanelSize(parsed.panelSize ?? {})
        window.localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({ ...parsed, version: STORAGE_VERSION, panelSize: clampedPanelSize }),
        )
        set({
          analysisPanelTab: parsed.analysisPanelTab === "logs" ? "logs" : "report",
          interfaceMode: parsed.interfaceMode === "stream" ? "stream" : "workbench",
          panelSize: clampedPanelSize,
          layoutMode: resolveLayoutMode(window.innerWidth),
        })
        return
      }
    } catch {
      // ignore corrupted local state
    }
    set({ layoutMode: resolveLayoutMode(window.innerWidth) })
  },
  setLayoutMode: (width) => set({ layoutMode: resolveLayoutMode(width) }),
  setPanelSize: (value) => {
    set((state) => {
      const panelSize = { ...state.panelSize, ...value }
      if (typeof window !== "undefined") {
        const raw = window.localStorage.getItem(STORAGE_KEY)
        const parsed = raw ? JSON.parse(raw) : {}
        window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...parsed, version: STORAGE_VERSION, panelSize }))
      }
      return { panelSize }
    })
  },
  setInterfaceMode: (value) => {
    if (typeof window !== "undefined") {
      const raw = window.localStorage.getItem(STORAGE_KEY)
      const parsed = raw ? JSON.parse(raw) : {}
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...parsed, version: STORAGE_VERSION, interfaceMode: value }))
    }
    set({ interfaceMode: value })
  },
  setAnalysisPanelTab: (value) => {
    if (typeof window !== "undefined") {
      const raw = window.localStorage.getItem(STORAGE_KEY)
      const parsed = raw ? JSON.parse(raw) : {}
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...parsed, version: STORAGE_VERSION, analysisPanelTab: value }))
    }
    set({ analysisPanelTab: value })
  },
  setActivePanel: (value) => set({ activePanel: value }),
}))
