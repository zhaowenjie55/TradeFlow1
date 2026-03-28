<script setup lang="ts">
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
    </div>
  </NuxtLayout>
</template>
