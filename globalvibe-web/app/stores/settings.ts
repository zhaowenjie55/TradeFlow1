import { defineStore } from 'pinia'
import type { AppLocale } from './locale'

const STORAGE_KEY = 'tradeflow-settings'

interface AppSettingsState {
  locale: AppLocale
  defaultMarket: string
  logSpeed: 'normal' | 'fast'
}

const normalizeMarket = (value?: string | null) => {
  if (value === 'AmazonUS') return 'AmazonUS'
  return 'AmazonUS'
}

const defaults: AppSettingsState = {
  locale: 'zh-CN',
  defaultMarket: 'AmazonUS',
  logSpeed: 'normal',
}

export const useSettingsStore = defineStore('settings', () => {
  const settings = ref<AppSettingsState>({ ...defaults })

  const save = () => {
    if (!import.meta.client) return
    localStorage.setItem(STORAGE_KEY, JSON.stringify(settings.value))
  }

  const hydrate = () => {
    if (!import.meta.client) return

    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return

    try {
      const parsed = JSON.parse(raw) as Partial<AppSettingsState>
      settings.value = {
        ...defaults,
        ...parsed,
        defaultMarket: normalizeMarket(parsed.defaultMarket),
      }
    } catch {
      settings.value = { ...defaults }
    }
  }

  const patchSettings = (next: Partial<AppSettingsState>) => {
    settings.value = {
      ...settings.value,
      ...next,
      defaultMarket: normalizeMarket(next.defaultMarket ?? settings.value.defaultMarket),
    }
  }

  return {
    settings,
    hydrate,
    save,
    patchSettings,
  }
})
