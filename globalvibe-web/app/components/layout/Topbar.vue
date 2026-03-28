<script setup lang="ts">
import type { AppLocale } from '~/stores/locale'

const route = useRoute()
const colorMode = useColorMode()
const { t, locale, setLocale } = useAppI18n()
const taskStore = useTaskStore()
const settingsStore = useSettingsStore()
const uiStore = useUIStore()
const isLocaleMenuOpen = ref(false)
const isHydrated = ref(false)
const localeButtonRef = ref<HTMLButtonElement | null>(null)
const localeOptionRefs = ref<Array<HTMLButtonElement | null>>([])
const localeActiveIndex = ref(0)

const localeOptions: Array<{ value: AppLocale, label: string }> = [
  { value: 'zh-CN', label: '中文' },
  { value: 'en', label: 'English' },
]

const navItems = [
  { to: '/', labelKey: 'nav.workspace' },
  { to: '/history', labelKey: 'nav.history' },
  { to: '/reports', labelKey: 'nav.reports' },
  { to: '/settings', labelKey: 'nav.settings' },
]

const titleMap: Record<string, string> = {
  '/': 'nav.workspace',
  '/history': 'nav.history',
  '/reports': 'nav.reports',
  '/settings': 'nav.settings',
}

const showInterfaceToggle = computed(() => route.path === '/')

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
  nextTick(() => localeButtonRef.value?.focus())
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

const openLocaleMenu = () => {
  if (!isHydrated.value) return
  isLocaleMenuOpen.value = true
  localeActiveIndex.value = Math.max(0, localeOptions.findIndex(option => option.value === locale.value))
  nextTick(() => localeOptionRefs.value[localeActiveIndex.value]?.focus())
}

const closeLocaleMenu = () => {
  isLocaleMenuOpen.value = false
}

const handleLocaleTriggerKeydown = (event: KeyboardEvent) => {
  if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    openLocaleMenu()
  }
}

const handleLocaleOptionKeydown = (event: KeyboardEvent, index: number) => {
  if (event.key === 'Escape') {
    event.preventDefault()
    closeLocaleMenu()
    nextTick(() => localeButtonRef.value?.focus())
    return
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    localeActiveIndex.value = (index + 1) % localeOptions.length
    localeOptionRefs.value[localeActiveIndex.value]?.focus()
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    localeActiveIndex.value = (index - 1 + localeOptions.length) % localeOptions.length
    localeOptionRefs.value[localeActiveIndex.value]?.focus()
  }

  if (event.key === 'Home') {
    event.preventDefault()
    localeActiveIndex.value = 0
    localeOptionRefs.value[localeActiveIndex.value]?.focus()
  }

  if (event.key === 'End') {
    event.preventDefault()
    localeActiveIndex.value = localeOptions.length - 1
    localeOptionRefs.value[localeActiveIndex.value]?.focus()
  }

  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    changeLocale(localeOptions[index].value)
  }
}
</script>

<template>
  <div class="flex min-h-[4rem] flex-col justify-center gap-2 px-4 py-2.5 lg:px-5">
    <div class="flex items-center justify-between gap-4">
      <div class="min-w-0">
        <div class="flex items-center gap-2 text-[13px]">
          <span class="font-semibold text-slate-700 dark:text-slate-100">{{ t('app.name') }}</span>
          <span class="tradeflow-muted">/</span>
          <span class="truncate text-sm font-medium text-slate-800 dark:text-slate-100">{{ t(currentTitleKey) }}</span>
          <span class="hidden rounded-full border border-[var(--tf-border)] bg-white/70 px-2.5 py-1 text-[11px] font-medium text-slate-600 md:inline-flex dark:bg-slate-950/80 dark:text-slate-300">
            {{ t(`common.${taskStore.mode}`) }}
          </span>
        </div>
      </div>

      <div class="flex items-center gap-2.5">
        <div
          v-if="showInterfaceToggle"
          class="hidden items-center rounded-2xl border border-[var(--tf-border)] bg-white/80 p-1 shadow-sm lg:inline-flex dark:bg-slate-950/80"
        >
          <button
            type="button"
            class="rounded-xl px-3 py-2 text-sm font-medium transition"
            :class="uiStore.interfaceMode === 'workbench' ? 'bg-[var(--tf-accent-soft)] text-[var(--tf-text)]' : 'text-slate-500 dark:text-slate-400'"
            @click="uiStore.setInterfaceMode('workbench')"
          >
            {{ t('app.workbenchView') }}
          </button>
          <button
            type="button"
            class="rounded-xl px-3 py-2 text-sm font-medium transition"
            :class="uiStore.interfaceMode === 'stream' ? 'bg-[var(--tf-accent-soft)] text-[var(--tf-text)]' : 'text-slate-500 dark:text-slate-400'"
            @click="uiStore.setInterfaceMode('stream')"
          >
            {{ t('app.streamView') }}
          </button>
        </div>

        <div class="relative" @focusout="handleLocaleFocusOut">
          <button
            ref="localeButtonRef"
            type="button"
            class="flex h-10 items-center gap-3 rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3.5 text-sm text-slate-600 shadow-sm transition-colors hover:border-[var(--tf-border-strong)] hover:bg-white dark:bg-slate-950/80 dark:text-slate-300"
            :aria-expanded="isHydrated ? isLocaleMenuOpen : false"
            aria-haspopup="listbox"
            :disabled="!isHydrated"
            @click="isLocaleMenuOpen ? closeLocaleMenu() : openLocaleMenu()"
            @keydown="handleLocaleTriggerKeydown"
          >
            <span class="hidden shrink-0 text-[11px] font-medium uppercase tracking-[0.14em] text-slate-400 dark:text-slate-500 sm:inline">
              {{ t('app.locale') }}
            </span>
            <span class="min-w-[4.5rem] text-left text-sm font-medium text-slate-700 dark:text-slate-200">
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
            class="absolute right-0 top-[calc(100%+0.5rem)] z-30 min-w-full overflow-hidden rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-1.5 shadow-xl"
          >
            <button
              v-for="(option, index) in localeOptions"
              :key="option.value"
              :ref="element => { localeOptionRefs[index] = element as HTMLButtonElement | null }"
              type="button"
              role="option"
              class="flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm transition-colors"
              :class="option.value === locale
                ? 'bg-[var(--tf-accent-soft)] font-medium text-[var(--tf-text)]'
                : 'text-slate-600 hover:bg-white/70 hover:text-slate-800 dark:text-slate-300 dark:hover:bg-slate-900 dark:hover:text-slate-100'"
              @mousedown.prevent
              @keydown="handleLocaleOptionKeydown($event, index)"
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
          class="flex h-10 w-10 items-center justify-center rounded-2xl border border-[var(--tf-border)] bg-white/80 text-slate-600 shadow-sm transition-colors duration-200 hover:border-[var(--tf-border-strong)] hover:bg-white hover:text-slate-800 dark:bg-slate-950/80 dark:text-slate-400 dark:hover:text-slate-200"
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

    <div class="flex items-center gap-2 overflow-x-auto xl:hidden">
      <button
        v-if="showInterfaceToggle"
        type="button"
        class="shrink-0 rounded-full border px-3 py-1.5 text-xs font-medium transition"
        :class="uiStore.interfaceMode === 'workbench'
          ? 'border-[var(--tf-border-strong)] bg-white/80 text-slate-800 dark:bg-slate-900 dark:text-slate-100'
          : 'border-transparent bg-white/50 text-slate-500 dark:bg-slate-900/60 dark:text-slate-400'"
        @click="uiStore.setInterfaceMode(uiStore.interfaceMode === 'workbench' ? 'stream' : 'workbench')"
      >
        {{ uiStore.interfaceMode === 'workbench' ? t('app.workbenchView') : t('app.streamView') }}
      </button>
      <NuxtLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="shrink-0 rounded-full border px-3 py-1.5 text-xs font-medium transition"
        :class="route.path === item.to || (item.to === '/reports' && route.path.startsWith('/reports/'))
          ? 'border-[var(--tf-border-strong)] bg-white/80 text-slate-800 dark:bg-slate-900 dark:text-slate-100'
          : 'border-transparent bg-white/50 text-slate-500 dark:bg-slate-900/60 dark:text-slate-400'"
      >
        {{ t(item.labelKey) }}
      </NuxtLink>
    </div>
  </div>
</template>
