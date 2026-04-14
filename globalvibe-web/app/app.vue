<script setup lang="ts">
const runtimeConfig = useRuntimeConfig()
const reactFrontendUrl = runtimeConfig.public.reactFrontendUrl || 'http://localhost:3000'
</script>

<template>
  <UApp>
    <NuxtRouteAnnouncer />
    <div class="tradeflow-legacy-shell">
      <div class="tradeflow-legacy-card">
        <div class="tradeflow-legacy-badge">
          Legacy Nuxt Client
        </div>
        <div class="space-y-4">
          <div class="space-y-2">
            <h1 class="tradeflow-legacy-title">
              This frontend is no longer active.
            </h1>
            <p class="tradeflow-legacy-copy">
              All live task creation, polling, candidate selection, 1688 verification handling, and report rendering now belong to the React frontend.
            </p>
          </div>

          <div class="tradeflow-legacy-list">
            <p>React app: <a :href="reactFrontendUrl" class="tradeflow-legacy-link">{{ reactFrontendUrl }}</a></p>
            <p>Nuxt app behavior has been intentionally disabled to avoid UI drift and duplicated task flows.</p>
          </div>

          <div class="tradeflow-legacy-actions">
            <a :href="reactFrontendUrl" class="tradeflow-legacy-primary">
              Open React Frontend
            </a>
          </div>
        </div>
      </div>
    </div>
  </UApp>
</template>
