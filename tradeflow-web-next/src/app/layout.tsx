import type { Metadata } from "next"

import { AppProviders } from "@/components/layout/app-providers"

import "./globals.css"

export const metadata: Metadata = {
  title: "TradeFlow Next",
  description: "TradeFlow workspace rebuilt with Next.js",
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html lang="zh-CN" className="h-full antialiased" suppressHydrationWarning>
      <body className="flex min-h-full flex-col overflow-hidden" suppressHydrationWarning>
        <AppProviders>{children}</AppProviders>
      </body>
    </html>
  )
}
