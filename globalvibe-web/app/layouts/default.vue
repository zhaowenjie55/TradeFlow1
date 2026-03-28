<template>
  <div class="tradeflow-shell">
    <header class="tradeflow-glass sticky top-0 z-40 shrink-0 border-b border-[var(--tf-border)]">
      <LayoutTopbar />
    </header>

    <main class="flex h-[calc(100dvh-4rem)] min-h-[calc(100dvh-4rem)]">
      <aside
        class="hidden w-[72px] shrink-0 border-r border-[var(--tf-border)] bg-[var(--tf-bg-soft)]/90 xl:block"
      >
        <LayoutSidebar />
      </aside>

      <div class="min-h-0 min-w-0 flex-1">
        <slot />
      </div>
    </main>
  </div>
</template>

<style>
.splitpanes.default-theme .splitpanes__pane {
  background-color: transparent;
}

.splitpanes.default-theme .splitpanes__splitter {
  background-color: transparent;
  border: none;
  min-width: 10px;
  cursor: col-resize;
  position: relative;
}

.splitpanes.default-theme .splitpanes__splitter::before {
  content: '';
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 50%;
  width: 4px;
  transform: translateX(-50%);
  border-radius: 9999px;
  background-color: rgba(148, 163, 184, 0.22);
  transition: background-color 220ms ease;
}

.splitpanes.default-theme .splitpanes__splitter:hover::before,
.splitpanes.default-theme .splitpanes__splitter:active::before {
  background-color: rgba(59, 130, 246, 0.45);
}

.dark .splitpanes.default-theme .splitpanes__splitter {
  background-color: transparent;
}

.dark .splitpanes.default-theme .splitpanes__splitter::before {
  background-color: rgba(71, 85, 105, 0.42);
}

.dark .splitpanes.default-theme .splitpanes__splitter:hover::before,
.dark .splitpanes.default-theme .splitpanes__splitter:active::before {
  background-color: rgba(96, 165, 250, 0.5);
}
</style>
