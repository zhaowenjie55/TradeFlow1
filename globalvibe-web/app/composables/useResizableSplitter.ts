import { ref, onMounted, onUnmounted } from 'vue'

interface SplitterOptions {
  minSize?: number
  maxSize?: number
  onResize?: (size: number) => void
}

export const useResizableSplitter = (options: SplitterOptions = {}) => {
  const { minSize = 10, maxSize = 40, onResize } = options

  const isDragging = ref(false)
  const containerRef = ref<HTMLElement | null>(null)

  const startDrag = (event: MouseEvent) => {
    event.preventDefault()
    isDragging.value = true

    const handleMouseMove = (e: MouseEvent) => {
      if (!isDragging.value || !containerRef.value?.parentElement) return

      const parent = containerRef.value.parentElement
      const parentRect = parent.getBoundingClientRect()
      const newSize = ((e.clientX - parentRect.left) / parentRect.width) * 100

      // Clamp between min and max
      const clampedSize = Math.min(maxSize, Math.max(minSize, newSize))
      onResize?.(clampedSize)
    }

    const handleMouseUp = () => {
      isDragging.value = false
      document.removeEventListener('mousemove', handleMouseMove)
      document.removeEventListener('mouseup', handleMouseUp)
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
    }

    document.addEventListener('mousemove', handleMouseMove)
    document.addEventListener('mouseup', handleMouseUp)
    document.body.style.cursor = 'col-resize'
    document.body.style.userSelect = 'none'
  }

  return {
    isDragging,
    containerRef,
    startDrag,
  }
}
