import en from '~~/locales/en.json'
import zhCN from '~~/locales/zh-CN.json'
import type { AppLocale } from '~/stores/locale'

const messages = {
  'zh-CN': zhCN,
  en,
} as const

const readByPath = (source: Record<string, unknown>, path: string): string | null => {
  const value = path.split('.').reduce<unknown>((current, segment) => {
    if (!current || typeof current !== 'object') return null
    return (current as Record<string, unknown>)[segment]
  }, source)

  return typeof value === 'string' ? value : null
}

const interpolate = (template: string, params?: Record<string, string | number>) => {
  if (!params) return template

  return Object.entries(params).reduce((result, [key, value]) => {
    return result.replaceAll(`{${key}}`, String(value))
  }, template)
}

export const useAppI18n = () => {
  const localeStore = useLocaleStore()

  const locale = computed(() => localeStore.locale)

  const setLocale = (value: AppLocale) => {
    localeStore.setLocale(value)
  }

  const t = (key: string, params?: Record<string, string | number>) => {
    const activeMessages = messages[locale.value]
    const fallback = messages['zh-CN']
    const resolved = readByPath(activeMessages as Record<string, unknown>, key) ?? readByPath(fallback as Record<string, unknown>, key) ?? key
    return interpolate(resolved, params)
  }

  return {
    locale,
    setLocale,
    t,
  }
}
