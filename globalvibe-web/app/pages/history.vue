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
    <div class="tradeflow-scrollbar h-full overflow-auto p-8">
      <h1 class="text-2xl font-semibold text-slate-800 dark:text-slate-100">{{ t('history.title') }}</h1>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">{{ t('history.subtitle') }}</p>

      <div v-if="items.length === 0" class="mt-8 rounded-2xl bg-white p-6 text-sm text-slate-500 dark:bg-slate-900 dark:text-slate-400">
        {{ t('history.empty') }}
      </div>

      <div v-else class="mt-8 grid gap-4">
        <div
          v-for="item in items"
          :key="item.taskId"
          class="rounded-2xl bg-white p-5 shadow-sm ring-1 ring-slate-200 dark:bg-slate-900 dark:ring-slate-800"
        >
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-sm font-semibold text-slate-800 dark:text-slate-100">{{ item.keyword }}</p>
              <p class="mt-1 text-xs text-slate-500 dark:text-slate-400">{{ item.market }}</p>
            </div>
            <span class="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600 dark:bg-slate-800 dark:text-slate-300">
              {{ t(`status.${item.status}`) }}
            </span>
          </div>
          <p class="mt-3 text-xs text-slate-400 dark:text-slate-500">{{ new Date(item.createdAt).toLocaleString() }}</p>
        </div>
      </div>
    </div>
  </NuxtLayout>
</template>
