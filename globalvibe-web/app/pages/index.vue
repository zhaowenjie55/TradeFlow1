<script setup lang="ts">
import { Pane, Splitpanes } from 'splitpanes'
import AgentParameterForm from '~/components/agent/ParameterForm.vue'
import AnalysisPanel from '~/components/layout/AnalysisPanel.vue'
import ProductGrid from '~/components/product/ProductGrid.vue'
import { useUIStore } from '~/stores/ui'

const uiStore = useUIStore()

const handleResize = (sizes: number[]) => {
  uiStore.updatePanelSizes(sizes)
}
</script>

<template>
  <NuxtLayout name="default">
    <div class="h-full min-h-0 min-w-0">
      <ClientOnly>
        <Splitpanes class="default-theme h-full" @resize="handleResize">
          <Pane :size="18" :min-size="15" :max-size="30">
            <div class="h-full border-r border-slate-200 bg-white/50 dark:border-slate-800 dark:bg-slate-900/50">
              <AgentParameterForm />
            </div>
          </Pane>

          <Pane :size="50" :min-size="30">
            <div class="h-full bg-slate-50 dark:bg-slate-950">
              <ProductGrid />
            </div>
          </Pane>

          <Pane :size="22" :min-size="15" :max-size="35">
            <div class="h-full border-l border-slate-200 bg-white/50 dark:border-slate-800 dark:bg-slate-900/50">
              <AnalysisPanel />
            </div>
          </Pane>
        </Splitpanes>

        <template #fallback>
          <div class="flex h-full min-h-0">
            <div class="h-full w-[18%] min-w-0 border-r border-slate-200 bg-white/50 dark:border-slate-800 dark:bg-slate-900/50">
              <AgentParameterForm />
            </div>

            <div class="h-full min-w-0 flex-1 bg-slate-50 dark:bg-slate-950">
              <ProductGrid />
            </div>

            <div class="h-full w-[22%] min-w-0 border-l border-slate-200 bg-white/50 dark:border-slate-800 dark:bg-slate-900/50">
              <AnalysisPanel />
            </div>
          </div>
        </template>
      </ClientOnly>
    </div>
  </NuxtLayout>
</template>
