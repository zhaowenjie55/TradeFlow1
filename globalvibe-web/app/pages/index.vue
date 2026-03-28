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

const isWorkbench = computed(() => uiStore.layoutMode === 'desktop' && uiStore.interfaceMode === 'workbench')
const isStream = computed(() => uiStore.interfaceMode === 'stream' || uiStore.layoutMode !== 'desktop')
</script>

<template>
  <NuxtLayout name="default">
    <div class="h-[calc(100dvh-4rem)] min-h-[calc(100dvh-4rem)] min-w-0 overflow-hidden">
      <ClientOnly>
        <div v-if="isWorkbench" class="grid h-full p-3 xl:p-4">
          <div class="tradeflow-panel min-h-0 overflow-hidden rounded-[var(--tf-radius-2xl)]">
            <Splitpanes class="default-theme h-full" @resize="handleResize">
              <Pane :size="uiStore.panelSize.workPanel" :min-size="18" :max-size="30">
                <div class="h-full border-r border-[var(--tf-border)]">
                  <AgentParameterForm />
                </div>
              </Pane>

              <Pane :size="uiStore.panelSize.stage" :min-size="34">
                <div class="h-full">
                  <ProductGrid />
                </div>
              </Pane>

              <Pane :size="uiStore.panelSize.analysis" :min-size="22" :max-size="34">
                <div class="h-full border-l border-[var(--tf-border)]">
                  <AnalysisPanel />
                </div>
              </Pane>
            </Splitpanes>
          </div>
        </div>

        <div v-else-if="isStream" class="space-y-4 p-4 pb-6 md:p-5 xl:p-6">
          <div class="tradeflow-panel overflow-hidden rounded-[var(--tf-radius-2xl)]">
            <AgentParameterForm compact />
          </div>

          <div class="tradeflow-panel overflow-hidden rounded-[var(--tf-radius-2xl)]">
            <ProductGrid compact />
          </div>

          <div class="min-h-[32rem]">
            <AnalysisPanel compact />
          </div>
        </div>

        <template #fallback>
          <div class="grid h-full p-3 xl:p-4">
            <div class="tradeflow-panel min-h-0 overflow-hidden rounded-[var(--tf-radius-2xl)]">
              <Splitpanes class="default-theme h-full" @resize="handleResize">
                <Pane :size="uiStore.panelSize.workPanel" :min-size="18" :max-size="30">
                  <div class="h-full border-r border-[var(--tf-border)]">
                    <AgentParameterForm />
                  </div>
                </Pane>

                <Pane :size="uiStore.panelSize.stage" :min-size="34">
                  <div class="h-full">
                    <ProductGrid />
                  </div>
                </Pane>

                <Pane :size="uiStore.panelSize.analysis" :min-size="22" :max-size="34">
                  <div class="h-full border-l border-[var(--tf-border)]">
                    <AnalysisPanel />
                  </div>
                </Pane>
              </Splitpanes>
            </div>
          </div>
        </template>
      </ClientOnly>
    </div>
  </NuxtLayout>
</template>
