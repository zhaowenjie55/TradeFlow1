export default defineNuxtPlugin(() => {
  const uiStore = useUIStore()

  uiStore.hydrate()

  const handleResize = () => {
    uiStore.setLayoutMode(window.innerWidth)
  }

  window.addEventListener('resize', handleResize, { passive: true })
})
