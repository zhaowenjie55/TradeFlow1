<script setup lang="ts">
import type { AppLocale } from '~/stores/locale'

const route = useRoute()
const colorMode = useColorMode()
const { t, locale, setLocale } = useAppI18n()
const taskStore = useTaskStore()
const settingsStore = useSettingsStore()
const isLocaleMenuOpen = ref(false)
const isHydrated = ref(false)

const localeOptions: Array<{ value: AppLocale, label: string }> = [
  { value: 'zh-CN', label: '中文' },
  { value: 'en', label: 'English' },
]

const titleMap: Record<string, string> = {
  '/': 'nav.workspace',
  '/history': 'nav.history',
  '/reports': 'nav.reports',
  '/settings': 'nav.settings',
}

const handleToggleTheme = () => {
  colorMode.preference = colorMode.value === 'dark' ? 'light' : 'dark'
}

const currentLocaleLabel = computed(() => {
  return localeOptions.find(option => option.value === locale.value)?.label ?? '中文'
})

const currentTitleKey = computed(() => {
  if (route.path.startsWith('/reports/')) {
    return 'nav.reports'
  }

  return titleMap[route.path] ?? 'nav.workspace'
})

const changeLocale = (value: AppLocale) => {
  setLocale(value)
  settingsStore.patchSettings({ locale: value })
  settingsStore.save()
  isLocaleMenuOpen.value = false
}

const handleLocaleFocusOut = (event: FocusEvent) => {
  const nextTarget = event.relatedTarget as Node | null
  if (event.currentTarget instanceof HTMLElement && nextTarget && event.currentTarget.contains(nextTarget)) {
    return
  }

  isLocaleMenuOpen.value = false
}

onMounted(() => {
  isHydrated.value = true
})
</script>

<template>
  <div class="flex h-full items-center justify-between px-5">
    <div class="flex items-center gap-2 text-sm">
      <span class="font-medium text-slate-500 dark:text-slate-400">{{ t('app.name') }}</span>
      <span class="text-slate-400 dark:text-slate-700">/</span>
      <span class="text-slate-600 dark:text-slate-300">{{ t(currentTitleKey) }}</span>
      <span class="ml-3 rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600 dark:bg-slate-900 dark:text-slate-300">
        {{ t('app.modeBadge', { mode: t(`common.${taskStore.mode}`) }) }}
      </span>
    </div>

    <div class="flex items-center gap-2.5">
      <div class="relative" @focusout="handleLocaleFocusOut">
        <button
          type="button"
          class="flex h-10 items-center gap-3 rounded-xl border border-slate-200 bg-white px-3.5 text-sm text-slate-600 shadow-sm transition-colors hover:border-slate-300 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-300 dark:hover:border-slate-700"
          :aria-expanded="isHydrated ? isLocaleMenuOpen : false"
          aria-haspopup="listbox"
          :disabled="!isHydrated"
          @click="isLocaleMenuOpen = !isLocaleMenuOpen"
        >
          <span class="shrink-0 text-xs font-medium uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500">
            {{ t('app.locale') }}
          </span>
          <span class="min-w-[5.5rem] text-left text-sm font-medium text-slate-700 dark:text-slate-200">
            {{ isHydrated ? currentLocaleLabel : '中文' }}
          </span>
          <UIcon
            name="i-heroicons-chevron-down"
            :class="[
              'h-4 w-4 text-slate-400 transition-transform dark:text-slate-500',
              isHydrated && isLocaleMenuOpen ? 'rotate-180' : '',
            ]"
          />
        </button>

        <div
          v-if="isHydrated && isLocaleMenuOpen"
          class="absolute right-0 top-[calc(100%+0.5rem)] z-30 min-w-full overflow-hidden rounded-2xl border border-slate-200 bg-white p-1.5 shadow-lg shadow-slate-200/70 dark:border-slate-800 dark:bg-slate-950 dark:shadow-black/20"
        >
          <button
            v-for="option in localeOptions"
            :key="option.value"
            type="button"
            class="flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm transition-colors"
            :class="option.value === locale
              ? 'bg-slate-100 font-medium text-slate-800 dark:bg-slate-900 dark:text-slate-100'
              : 'text-slate-600 hover:bg-slate-50 hover:text-slate-800 dark:text-slate-300 dark:hover:bg-slate-900 dark:hover:text-slate-100'"
            @mousedown.prevent
            @click="changeLocale(option.value)"
          >
            <span>{{ option.label }}</span>
            <UIcon
              v-if="option.value === locale"
              name="i-heroicons-check"
              class="h-4 w-4"
            />
          </button>
        </div>
      </div>

      <button
        type="button"
        class="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-white text-slate-600 shadow-sm transition-colors duration-200 hover:border-slate-300 hover:bg-slate-50 hover:text-slate-800 dark:border-slate-800 dark:bg-slate-950 dark:text-slate-400 dark:hover:border-slate-700 dark:hover:bg-slate-900 dark:hover:text-slate-200"
        :aria-label="isHydrated && colorMode.value === 'dark' ? t('app.light') : t('app.dark')"
        :disabled="!isHydrated"
        @click="handleToggleTheme"
      >
        <UIcon
          :name="isHydrated && colorMode.value === 'dark' ? 'i-heroicons-sun' : 'i-heroicons-moon'"
          class="h-[18px] w-[18px]"
        />
      </button>
    </div>
  </div>
</template>
