import { Suspense } from "react"

import { AgentEntry } from "@/components/agent/agent-entry"

export default function Page() {
  return (
    <Suspense fallback={null}>
      <AgentEntry />
    </Suspense>
  )
}
