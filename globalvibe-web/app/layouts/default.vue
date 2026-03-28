<template>
  <div class="h-screen w-screen overflow-hidden bg-slate-100 text-slate-700 dark:bg-slate-950 dark:text-slate-300">
    <header class="h-14 shrink-0 border-b border-slate-200 bg-white/50 dark:border-slate-800 dark:bg-slate-900/50">
      <LayoutTopbar />
    </header>

    <main class="flex h-[calc(100vh-3.5rem)] min-h-0">
      <aside
        class="h-full w-[5%] min-w-12 shrink-0 border-r border-slate-200 bg-slate-50/80 dark:border-slate-800 dark:bg-slate-900/80"
      >
        <LayoutSidebar />
      </aside>

      <div class="min-h-0 min-w-0 flex-1 overflow-hidden">
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
  background-color: rgba(0, 0, 0, 0.05);
  border: none;
  min-width: 4px;
  cursor: col-resize;
}

.splitpanes.default-theme .splitpanes__splitter:hover {
  background-color: rgba(0, 0, 0, 0.15);
}

.dark .splitpanes.default-theme .splitpanes__splitter {
  background-color: rgba(255, 255, 255, 0.03);
}

.dark .splitpanes.default-theme .splitpanes__splitter:hover {
  background-color: rgba(148, 163, 184, 0.2);
}
</style>
