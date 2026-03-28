export type LayoutMode = 'desktop' | 'tablet' | 'mobile'
export type InterfaceMode = 'workbench' | 'stream'
export type AnalysisPanelTab = 'logs' | 'report'
export type LogFilterMode = 'all' | 'key'

export interface PanelSize {
  sideRail: number
  workPanel: number
  stage: number
  analysis: number
}
