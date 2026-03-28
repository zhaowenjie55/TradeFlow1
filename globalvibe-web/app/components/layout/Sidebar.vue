<script setup lang="ts">
const { t } = useAppI18n()
const route = useRoute()

const menuItems = [
  { icon: 'i-heroicons-sparkles', to: '/', labelKey: 'nav.workspace' },
  { icon: 'i-heroicons-clock', to: '/history', labelKey: 'nav.history' },
  { icon: 'i-heroicons-document-chart-bar', to: '/reports', labelKey: 'nav.reports' },
  { icon: 'i-heroicons-cog-6-tooth', to: '/settings', labelKey: 'nav.settings' },
]
</script>

<template>
  <div class="flex h-full flex-col items-center gap-5 py-5">
    <div class="tradeflow-card flex h-11 w-11 items-center justify-center rounded-2xl">
      <span class="text-sm font-bold tracking-tight text-slate-700 dark:text-slate-200">TF</span>
    </div>

    <nav class="flex flex-1 flex-col items-center gap-1">
      <NuxtLink
        v-for="item in menuItems"
        :key="item.to"
        :to="item.to"
        :aria-label="t(item.labelKey)"
        :class="[
          'group relative flex h-11 w-11 items-center justify-center rounded-2xl transition-all duration-200',
          route.path === item.to
            ? 'tradeflow-surface-accent text-[var(--tf-accent)] shadow-sm'
            : 'text-slate-400 hover:bg-white/70 hover:text-slate-700 dark:hover:bg-slate-900 dark:hover:text-slate-200',
        ]"
      >
        <UIcon :name="item.icon" class="h-[18px] w-[18px]" />
        <span class="pointer-events-none absolute left-[calc(100%+0.75rem)] top-1/2 hidden -translate-y-1/2 whitespace-nowrap rounded-xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] px-3 py-1.5 text-xs font-medium text-[var(--tf-text)] shadow-lg xl:group-hover:block">
          {{ t(item.labelKey) }}
        </span>
      </NuxtLink>
    </nav>
  </div>
</template>
