import { defineStore } from 'pinia'
import type { AnalysisPanelTab, InterfaceMode, LayoutMode, PanelSize } from '~/types'

const STORAGE_KEY = 'tradeflow-ui'

const defaultPanelSize: PanelSize = {
  sideRail: 5,
  workPanel: 22,
  stage: 50,
  analysis: 28,
}

const normalizePanelSize = (value?: Partial<PanelSize> | null): PanelSize => {
  if (!value) return { ...defaultPanelSize }

  const workPanel = Number(value.workPanel ?? defaultPanelSize.workPanel)
  const stage = Number(value.stage ?? defaultPanelSize.stage)
  const analysis = Number(value.analysis ?? defaultPanelSize.analysis)

  return {
    sideRail: Number(value.sideRail ?? defaultPanelSize.sideRail),
    workPanel: Math.min(30, Math.max(18, workPanel)),
    stage: Math.min(58, Math.max(34, stage)),
    analysis: Math.min(34, Math.max(22, analysis)),
  }
}

const resolveLayoutMode = (width: number): LayoutMode => {
  if (width >= 1280) return 'desktop'
  if (width >= 1024) return 'desktop'
  return 'mobile'
}

export const useUIStore = defineStore('ui', () => {
  const panelSize = ref<PanelSize>({ ...defaultPanelSize })
  const isDarkMode = ref(true)
  const isSidebarCollapsed = ref(false)
  const activePanel = ref<'workspace' | 'history' | 'reports' | 'settings'>('workspace')
  const layoutMode = ref<LayoutMode>('desktop')
  const interfaceMode = ref<InterfaceMode>('workbench')
  const analysisPanelTab = ref<AnalysisPanelTab>('report')

  const updatePanelSizes = (sizes: number[]) => {
    if (sizes.length >= 4) {
      panelSize.value = normalizePanelSize({
        sideRail: sizes[0],
        workPanel: sizes[1],
        stage: sizes[2],
        analysis: sizes[3],
      })
    }
    else if (sizes.length >= 3) {
      panelSize.value = normalizePanelSize({
        sideRail: 5,
        workPanel: sizes[0],
        stage: sizes[1],
        analysis: sizes[2],
      })
    }

    save()
  }

  const setLayoutMode = (width: number) => {
    layoutMode.value = resolveLayoutMode(width)
  }

  const hydrate = () => {
    if (!import.meta.client) return

    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      try {
        const parsed = JSON.parse(raw) as Partial<{
          panelSize: PanelSize
          isSidebarCollapsed: boolean
          analysisPanelTab: AnalysisPanelTab
          interfaceMode: InterfaceMode
        }>

        panelSize.value = normalizePanelSize(parsed.panelSize)
        isSidebarCollapsed.value = Boolean(parsed.isSidebarCollapsed)
        analysisPanelTab.value = parsed.analysisPanelTab === 'logs' ? 'logs' : 'report'
        interfaceMode.value = parsed.interfaceMode === 'stream' ? 'stream' : 'workbench'
      } catch {
        panelSize.value = { ...defaultPanelSize }
      }
    }

    setLayoutMode(window.innerWidth)
  }

  const save = () => {
    if (!import.meta.client) return

    localStorage.setItem(STORAGE_KEY, JSON.stringify({
      panelSize: panelSize.value,
      isSidebarCollapsed: isSidebarCollapsed.value,
      analysisPanelTab: analysisPanelTab.value,
      interfaceMode: interfaceMode.value,
    }))
  }

  const toggleSidebar = () => {
    isSidebarCollapsed.value = !isSidebarCollapsed.value
    save()
  }

  const toggleTheme = () => {
    isDarkMode.value = !isDarkMode.value
  }

  const setAnalysisPanelTab = (value: AnalysisPanelTab) => {
    analysisPanelTab.value = value
    save()
  }

  const setInterfaceMode = (value: InterfaceMode) => {
    interfaceMode.value = value
    save()
  }

  return {
    panelSize,
    isDarkMode,
    isSidebarCollapsed,
    activePanel,
    layoutMode,
    interfaceMode,
    analysisPanelTab,
    hydrate,
    save,
    setLayoutMode,
    updatePanelSizes,
    toggleSidebar,
    toggleTheme,
    setAnalysisPanelTab,
    setInterfaceMode,
  }
})
