import { Suspense } from "react"

import { Workbench } from "@/components/layout/workbench"

export default function AppPage() {
  return (
    <Suspense fallback={null}>
      <Workbench />
    </Suspense>
  )
}
