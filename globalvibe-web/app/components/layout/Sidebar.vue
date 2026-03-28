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
  <div class="flex h-full flex-col items-center py-4">
    <div class="mb-6 flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-200 dark:bg-slate-900">
      <span class="text-sm font-bold tracking-tight text-slate-600 dark:text-slate-300">GV</span>
    </div>

    <nav class="flex flex-1 flex-col items-center gap-1">
      <NuxtLink
        v-for="item in menuItems"
        :key="item.to"
        :to="item.to"
        :aria-label="t(item.labelKey)"
        :class="[
          'group flex h-10 w-10 items-center justify-center rounded-xl transition-all duration-200',
          route.path === item.to
            ? 'bg-slate-200 text-slate-800 dark:bg-slate-800 dark:text-slate-200'
            : 'text-slate-400 hover:bg-slate-100 hover:text-slate-600 dark:text-slate-500 dark:hover:bg-slate-800/50 dark:hover:text-slate-300',
        ]"
      >
        <UIcon :name="item.icon" class="h-[18px] w-[18px]" />
      </NuxtLink>
    </nav>
  </div>
</template>
