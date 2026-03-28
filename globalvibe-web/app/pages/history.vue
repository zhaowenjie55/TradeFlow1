<script setup lang="ts">
import { getTaskHistory } from '~/services/task'
import type { TaskHistoryItem } from '~/types'

const { t } = useAppI18n()
const items = ref<TaskHistoryItem[]>([])

onMounted(async () => {
  try {
    const response = await getTaskHistory()
    items.value = response.items
  } catch {
    items.value = []
  }
})
</script>

<template>
  <NuxtLayout name="default">
    <div class="tradeflow-scrollbar min-h-[calc(100dvh-4.5rem)] overflow-auto p-4 md:p-6 xl:p-8">
      <div class="mx-auto max-w-5xl">
        <h1 class="text-2xl font-semibold text-slate-800 dark:text-slate-100">{{ t('history.title') }}</h1>
        <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ t('history.subtitle') }}</p>

        <div v-if="items.length === 0" class="tradeflow-card mt-8 rounded-[var(--tf-radius-xl)] p-6 text-sm text-slate-500 dark:text-slate-400">
        {{ t('history.empty') }}
        </div>

        <div v-else class="mt-8 grid gap-4">
          <div
            v-for="item in items"
            :key="item.taskId"
            class="tradeflow-card rounded-[var(--tf-radius-xl)] p-5"
          >
            <div class="flex items-start justify-between gap-4">
              <div>
                <p class="text-sm font-semibold text-slate-800 dark:text-slate-100">{{ item.keyword }}</p>
                <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ item.market }}</p>
              </div>
              <span class="rounded-full border border-[var(--tf-border)] bg-white/70 px-3 py-1 text-xs text-slate-600 dark:bg-slate-950/70 dark:text-slate-300">
                {{ t(`status.${item.status}`) }}
              </span>
            </div>
            <p class="mt-3 text-xs text-slate-400 dark:text-slate-500">{{ new Date(item.createdAt).toLocaleString() }}</p>
          </div>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>
