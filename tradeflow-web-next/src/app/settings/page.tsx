"use client"

import { useEffect, useState } from "react"

import { AppShell } from "@/components/layout/app-shell"
import { useAppI18n } from "@/components/layout/locale-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { getDemoConfig } from "@/lib/api/settings"
import { useSettingsStore } from "@/stores/settings-store"
import type { DemoConfigResponse } from "@/types"

export default function SettingsPage() {
  const { locale, setLocale, t } = useAppI18n()
  const settingsStore = useSettingsStore()
  const [config, setConfig] = useState<DemoConfigResponse | null>(null)
  const [saved, setSaved] = useState(false)

  useEffect(() => {
    settingsStore.hydrate()
    void getDemoConfig().then(setConfig).catch(() => setConfig(null))
  }, [settingsStore])

  const saveSettings = () => {
    setLocale(settingsStore.locale)
    setSaved(true)
    window.setTimeout(() => setSaved(false), 1500)
  }

  return (
    <AppShell activePanel="settings">
      <div className="mx-auto max-w-3xl">
        <h1 className="text-2xl font-semibold text-[var(--tf-text)]">{t("settings.title")}</h1>
        <p className="mt-2 text-sm text-[var(--tf-text-muted)]">{t("settings.subtitle")}</p>

        <div className="mt-8 space-y-5">
          <Card>
            <CardContent className="p-5">
              <label htmlFor="settings-language" className="text-sm font-medium text-[var(--tf-text)]">
                {t("settings.language")}
              </label>
              <select
                id="settings-language"
                className="mt-3 flex h-12 w-full rounded-2xl border border-[var(--tf-border)] bg-white px-4 text-sm text-[var(--tf-text)] outline-none"
                value={locale}
                onChange={(event) => {
                  const nextLocale = event.target.value === "en" ? "en" : "zh-CN"
                  settingsStore.patchSettings({ locale: nextLocale })
                  setLocale(nextLocale)
                }}
              >
                <option value="zh-CN">中文</option>
                <option value="en">English</option>
              </select>
            </CardContent>
          </Card>

          <Card>
            <CardContent className="p-5">
              <label htmlFor="settings-market" className="text-sm font-medium text-[var(--tf-text)]">
                {t("settings.defaultMarket")}
              </label>
              <select
                id="settings-market"
                className="mt-3 flex h-12 w-full rounded-2xl border border-[var(--tf-border)] bg-white px-4 text-sm text-[var(--tf-text)] outline-none"
                value={settingsStore.defaultMarket}
                onChange={(event) => settingsStore.patchSettings({ defaultMarket: event.target.value })}
              >
                {(config?.markets ?? ["AmazonUS"]).map((market) => (
                  <option key={market} value={market}>
                    {market}
                  </option>
                ))}
              </select>
            </CardContent>
          </Card>

          <Button onClick={saveSettings}>{t("settings.save")}</Button>
          {saved && <p className="text-sm text-green-600">{t("settings.savedHint")}</p>}
        </div>
      </div>
    </AppShell>
  )
}
