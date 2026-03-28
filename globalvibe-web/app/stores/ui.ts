import { defineStore } from 'pinia'
import type { PanelSize } from '~/types'

export const useUIStore = defineStore('ui', () => {
  // Panel sizes for resizable layout
  const panelSize = ref<PanelSize>({
    sideRail: 5,
    workPanel: 18,
    stage: 50,
    analysis: 22,
  })

  // Theme
  const isDarkMode = ref(true)

  // Sidebar state
  const isSidebarCollapsed = ref(false)

  // Active panel
  const activePanel = ref<'workspace' | 'history' | 'reports' | 'settings'>('workspace')

  // Update panel sizes from splitter resize (side rail is fixed layout, not a Pane — sizes are [work, stage, analysis])
  const updatePanelSizes = (sizes: number[]) => {
    if (sizes.length >= 4) {
      panelSize.value = {
        sideRail: sizes[0],
        workPanel: sizes[1],
        stage: sizes[2],
        analysis: sizes[3],
      }
    }
    else if (sizes.length >= 3) {
      panelSize.value = {
        sideRail: 5,
        workPanel: sizes[0],
        stage: sizes[1],
        analysis: sizes[2],
      }
    }
  }

  // Toggle sidebar
  const toggleSidebar = () => {
    isSidebarCollapsed.value = !isSidebarCollapsed.value
  }

  // Toggle theme
  const toggleTheme = () => {
    isDarkMode.value = !isDarkMode.value
  }

  return {
    panelSize,
    isDarkMode,
    isSidebarCollapsed,
    activePanel,
    updatePanelSizes,
    toggleSidebar,
    toggleTheme,
  }
})
