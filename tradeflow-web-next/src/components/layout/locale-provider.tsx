"use client"

import en from "@/messages/en.json"
import zhCN from "@/messages/zh-CN.json"
import { createContext, useContext, useEffect, useMemo, useState } from "react"

export type AppLocale = "zh-CN" | "en"

const STORAGE_KEY = "tradeflow-next-locale"

const messages = {
  "zh-CN": zhCN,
  en,
} as const

type MessageTree = Record<string, unknown>

interface LocaleContextValue {
  locale: AppLocale
  setLocale: (value: AppLocale) => void
  t: (key: string, params?: Record<string, string | number | null | undefined>) => string
}

const LocaleContext = createContext<LocaleContextValue | null>(null)

function resolveMessage(tree: MessageTree, key: string): unknown {
  return key.split(".").reduce<unknown>((current, segment) => {
    if (!current || typeof current !== "object") return undefined
    return (current as MessageTree)[segment]
  }, tree)
}

function interpolate(template: string, params?: Record<string, string | number | null | undefined>) {
  if (!params) return template
  return template.replace(/\{(\w+)\}/g, (_, token) => {
    const value = params[token]
    return value === null || value === undefined ? "" : String(value)
  })
}

export function LocaleProvider({ children }: { children: React.ReactNode }) {
  const [locale, setLocaleState] = useState<AppLocale>(() => {
    if (typeof window === "undefined") return "zh-CN"
    const saved = window.localStorage.getItem(STORAGE_KEY)
    return saved === "zh-CN" || saved === "en" ? saved : "zh-CN"
  })

  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])

  const setLocale = (value: AppLocale) => {
    setLocaleState(value)
    window.localStorage.setItem(STORAGE_KEY, value)
    document.documentElement.lang = value
  }

  const value = useMemo<LocaleContextValue>(() => ({
    locale,
    setLocale,
    t: (key, params) => {
      const resolved = resolveMessage(messages[locale] as unknown as MessageTree, key)
      if (typeof resolved !== "string") return key
      return interpolate(resolved, params)
    },
  }), [locale])

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>
}

export function useAppI18n() {
  const context = useContext(LocaleContext)
  if (!context) throw new Error("useAppI18n must be used inside LocaleProvider")
  return context
}
