"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import type { Route } from "next"
import { useRouter, useSearchParams } from "next/navigation"
import { ArrowRight } from "lucide-react"

import { SoftGlowBackground } from "@/components/ui/background-components"
import { Globe, type Arc, type Marker } from "@/components/ui/cobe-globe"
import { cn } from "@/lib/utils"

const statusRows = [
  { label: "CORE SYSTEM", value: "ONLINE" },
  { label: "MARKET NETWORK", value: "ONLINE" },
  { label: "AGENT", value: "READY" },
]

const tradeMarkers: Marker[] = [
  { id: "shenzhen", location: [22.5431, 114.0579], label: "Shenzhen" },
  { id: "hangzhou", location: [30.2741, 120.1551], label: "Hangzhou" },
  { id: "yiwu", location: [29.3069, 120.0753], label: "Yiwu" },
  { id: "los-angeles", location: [34.0522, -118.2437], label: "Los Angeles" },
  { id: "new-york", location: [40.7128, -74.006], label: "New York" },
  { id: "london", location: [51.5072, -0.1276], label: "London" },
  { id: "singapore", location: [1.3521, 103.8198], label: "Singapore" },
  { id: "dubai", location: [25.2048, 55.2708], label: "Dubai" },
  { id: "tokyo", location: [35.6762, 139.6503], label: "Tokyo" },
]

const tradeArcs: Arc[] = [
  { id: "shenzhen-los-angeles", from: [22.5431, 114.0579], to: [34.0522, -118.2437] },
  { id: "hangzhou-london", from: [30.2741, 120.1551], to: [51.5072, -0.1276] },
  { id: "yiwu-dubai", from: [29.3069, 120.0753], to: [25.2048, 55.2708] },
  { id: "shenzhen-singapore", from: [22.5431, 114.0579], to: [1.3521, 103.8198] },
  { id: "hangzhou-new-york", from: [30.2741, 120.1551], to: [40.7128, -74.006] },
]

export function AgentEntry() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [launching, setLaunching] = useState(false)

  const nextPath = useMemo<Route>(() => {
    const query = searchParams.toString()
    return (query ? `/app?${query}` : "/app") as Route
  }, [searchParams])

  const launchAgent = useCallback(() => {
    if (launching) return
    setLaunching(true)
    window.setTimeout(() => {
      router.push(nextPath)
    }, 240)
  }, [launching, nextPath, router])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Enter") launchAgent()
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [launchAgent])

  return (
    <SoftGlowBackground
      className={cn(
        "text-[#202118] transition-opacity duration-300 motion-reduce:transition-none",
        launching && "opacity-0",
      )}
    >
      <main className="min-h-dvh">
        <section className="mx-auto grid min-h-dvh w-full max-w-7xl items-center gap-6 px-6 py-5 sm:gap-10 sm:px-10 sm:py-10 lg:grid-cols-[0.88fr_1.12fr] lg:gap-14 lg:px-14">
          <div className="order-2 flex w-full max-w-[36rem] flex-col items-center text-center lg:order-1 lg:items-start lg:text-left">
            <div className="inline-flex border border-[#202118]/12 bg-white/55 px-4 py-2 font-mono text-[0.68rem] font-medium uppercase tracking-[0.24em] text-[#5e5b3e] shadow-[0_12px_30px_rgba(40,36,14,0.06)] backdrop-blur">
              AGENT RUNTIME ONLINE
            </div>

            <div className="mt-7 sm:mt-12">
              <h1 className="text-balance font-mono text-[2.28rem] font-semibold tracking-[0.1em] text-[#171812] sm:text-6xl sm:tracking-[0.17em] lg:text-7xl">
                TRADEFLOW
              </h1>
              <p className="mt-6 max-w-md text-[1rem] leading-7 text-[#5c5d53] sm:text-lg">
                Global sourcing intelligence for overseas demand, domestic supply, and margin signals.
              </p>
            </div>

            <div className="mt-8 w-full max-w-md space-y-2.5 font-mono text-[0.68rem] uppercase tracking-[0.15em] text-[#4a4a3f] sm:mt-12 sm:space-y-3 sm:text-[0.72rem]">
              {statusRows.map((row) => (
                <div
                  key={row.label}
                  className="flex items-center justify-between gap-4 border border-[#202118]/10 bg-white/48 px-4 py-2.5 shadow-[0_10px_26px_rgba(45,42,20,0.045)] backdrop-blur sm:py-3"
                >
                  <span>{row.label}</span>
                  <span className="text-[#7b7325]">{row.value}</span>
                </div>
              ))}
            </div>

            <button
              type="button"
              aria-label="Launch TradeFlow Agent"
              aria-busy={launching}
              disabled={launching}
              onClick={launchAgent}
              className="mt-8 inline-flex h-12 items-center justify-center gap-3 border border-[#1f2018] bg-[#1f2018] px-7 font-mono text-xs font-semibold uppercase tracking-[0.2em] text-[#fffdf2] shadow-[0_18px_36px_rgba(31,32,24,0.18)] transition hover:-translate-y-0.5 hover:bg-[#303125] focus:outline-none focus:ring-2 focus:ring-[#c4b739] focus:ring-offset-2 focus:ring-offset-[#fffdf5] disabled:cursor-wait disabled:opacity-80 motion-reduce:transition-none sm:mt-12"
            >
              {launching ? "LAUNCHING" : "ENTER"}
              <ArrowRight aria-hidden="true" className="size-4" />
            </button>
          </div>

          <div className="order-1 flex w-full flex-col items-center lg:order-2">
            <div className="relative w-full max-w-[min(58vw,15rem)] sm:max-w-[31rem] lg:max-w-[39rem]">
              <div className="pointer-events-none absolute inset-5 rounded-full border border-[#222318]/8" />
              <div className="pointer-events-none absolute inset-14 rounded-full border border-[#c6b830]/16" />
              <div className="pointer-events-none absolute inset-0 rounded-full bg-[radial-gradient(circle_at_50%_48%,rgba(255,249,145,0.22),transparent_62%)]" />
              <Globe
                ariaLabel="TradeFlow sourcing lanes connecting China supply nodes with global demand markets"
                className="mx-auto w-full"
                markers={tradeMarkers}
                arcs={tradeArcs}
                baseColor={[0.9, 0.91, 0.84]}
                markerColor={[0.36, 0.43, 0.31]}
                arcColor={[0.73, 0.64, 0.22]}
                glowColor={[0.98, 0.94, 0.58]}
                dark={0.08}
                mapBrightness={6.8}
                markerSize={0.026}
                markerElevation={0.018}
                arcWidth={0.42}
                arcHeight={0.26}
                speed={0.0017}
                theta={0.22}
                diffuse={1.35}
                mapSamples={18000}
              />
            </div>
          </div>
        </section>
      </main>
    </SoftGlowBackground>
  )
}
