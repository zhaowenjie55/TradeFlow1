"use client"

import { create } from "zustand"

type AppLocale = "zh-CN" | "en"

interface SettingsState {
  locale: AppLocale
  defaultMarket: string
  logSpeed: "normal" | "fast"
  hydrate: () => void
  patchSettings: (next: Partial<Omit<SettingsState, "hydrate" | "patchSettings">>) => void
}

const STORAGE_KEY = "tradeflow-next-settings"

export const useSettingsStore = create<SettingsState>((set) => ({
  locale: "zh-CN",
  defaultMarket: "AmazonUS",
  logSpeed: "normal",
  hydrate: () => {
    if (typeof window === "undefined") return
    try {
      const raw = window.localStorage.getItem(STORAGE_KEY)
      if (!raw) return
      const parsed = JSON.parse(raw) as Partial<Pick<SettingsState, "locale" | "defaultMarket" | "logSpeed">>
      set({
        locale: parsed.locale === "en" ? "en" : "zh-CN",
        defaultMarket: parsed.defaultMarket || "AmazonUS",
        logSpeed: parsed.logSpeed === "fast" ? "fast" : "normal",
      })
    } catch {
      // ignore
    }
  },
  patchSettings: (next) => {
    set((state) => {
      const updated = { ...state, ...next }
      if (typeof window !== "undefined") {
        window.localStorage.setItem(
          STORAGE_KEY,
          JSON.stringify({
            locale: updated.locale,
            defaultMarket: updated.defaultMarket,
            logSpeed: updated.logSpeed,
          }),
        )
      }
      return updated
    })
  },
}))
