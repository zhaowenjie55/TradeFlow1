import { defineStore } from 'pinia'

export type AppLocale = 'zh-CN' | 'en'

const STORAGE_KEY = 'globalvibe-locale'

export const useLocaleStore = defineStore('locale', () => {
  const locale = ref<AppLocale>('zh-CN')

  const setLocale = (value: AppLocale) => {
    locale.value = value

    if (import.meta.client) {
      localStorage.setItem(STORAGE_KEY, value)
      document.documentElement.lang = value
    }
  }

  const hydrateLocale = () => {
    if (!import.meta.client) return

    const saved = localStorage.getItem(STORAGE_KEY)

    if (saved === 'zh-CN' || saved === 'en') {
      locale.value = saved
    }

    document.documentElement.lang = locale.value
  }

  return {
    locale,
    setLocale,
    hydrateLocale,
  }
})
