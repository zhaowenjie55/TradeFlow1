"use client"

import { LocaleProvider } from "@/components/layout/locale-provider"

export function AppProviders({ children }: { children: React.ReactNode }) {
  return <LocaleProvider>{children}</LocaleProvider>
}
