<script setup lang="ts">
import AppSelect from '~/components/ui/AppSelect.vue'
import type { AppLocale } from '~/stores/locale'
import { getDemoConfig } from '~/services/settings'
import type { DemoConfigResponse } from '~/types'

const { t, locale, setLocale } = useAppI18n()
const settingsStore = useSettingsStore()
const config = ref<DemoConfigResponse | null>(null)
const saved = ref(false)
const defaultMarket = computed({
  get: () => settingsStore.settings.defaultMarket,
  set: (value: string) => settingsStore.patchSettings({ defaultMarket: value }),
})
const currentLocale = computed({
  get: () => locale.value,
  set: (value: string) => updateLocale(value as AppLocale),
})
const localeOptions = [
  { value: 'zh-CN', label: '中文' },
  { value: 'en', label: 'English' },
]

onMounted(async () => {
  try {
    config.value = await getDemoConfig()
  } catch {
    config.value = null
  }
})

const saveSettings = () => {
  settingsStore.save()
  setLocale(settingsStore.settings.locale)
  saved.value = true
  setTimeout(() => {
    saved.value = false
  }, 1500)
}

const updateLocale = (value: AppLocale) => {
  settingsStore.patchSettings({ locale: value })
  setLocale(value)
}

</script>

<template>
  <NuxtLayout name="default">
    <div class="globalvibe-scrollbar h-full overflow-auto p-8">
      <h1 class="text-2xl font-semibold text-slate-800 dark:text-slate-100">{{ t('settings.title') }}</h1>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ t('settings.subtitle') }}</p>

      <div class="mt-8 max-w-2xl space-y-5">
        <div class="rounded-2xl bg-white p-5 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-800">
          <label class="text-sm font-medium text-slate-700 dark:text-slate-300">{{ t('settings.language') }}</label>
          <AppSelect v-model="currentLocale" :options="localeOptions" class="mt-3" />
        </div>

        <div class="rounded-2xl bg-white p-5 ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-800">
          <label class="text-sm font-medium text-slate-700 dark:text-slate-300">{{ t('settings.defaultMarket') }}</label>
          <AppSelect v-model="defaultMarket" :options="config?.markets ?? []" class="mt-3" />
        </div>
        <button
          type="button"
          class="rounded-lg bg-blue-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-blue-500"
          @click="saveSettings"
        >
          {{ t('settings.save') }}
        </button>

        <p v-if="saved" class="text-sm text-green-600 dark:text-green-400">{{ t('settings.savedHint') }}</p>
      </div>
    </div>
  </NuxtLayout>
</template>
