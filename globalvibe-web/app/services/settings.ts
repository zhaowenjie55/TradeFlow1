import type { DemoConfigResponse } from '~/types'

export const getDemoConfig = () => {
  return $fetch<DemoConfigResponse>('/api/settings/demo-config')
}
