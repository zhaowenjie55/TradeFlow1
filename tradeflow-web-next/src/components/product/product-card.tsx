"use client"

import { useMemo } from "react"
import { ExternalLink, FlaskConical } from "lucide-react"

import { Button } from "@/components/ui/button"
import { ImageWithFallback } from "@/components/ui/image-with-fallback"
import { Card, CardContent } from "@/components/ui/card"
import { getReadableOriginalTitle, getReadableProductName } from "@/lib/presentation"
import { useAppI18n } from "@/components/layout/locale-provider"
import type { CandidateSummary } from "@/types"

export function ProductCard({
  candidate,
  selected,
  analyzing,
  disabled,
  onAnalyze,
}: {
  candidate: CandidateSummary
  selected: boolean
  analyzing: boolean
  disabled: boolean
  onAnalyze: (productId: string) => void
}) {
  const { t } = useAppI18n()

  const displayTitle = useMemo(() => getReadableProductName(candidate.title), [candidate.title])
  const originalTitle = useMemo(
    () => getReadableOriginalTitle(candidate.title, displayTitle),
    [candidate.title, displayTitle],
  )
  const targetSiteUrl = useMemo(() => {
    if (candidate.link) return candidate.link
    if (!candidate.productId || !/amazon/i.test(candidate.market)) return null
    return `https://www.amazon.com/dp/${candidate.productId}`
  }, [candidate.link, candidate.market, candidate.productId])

  return (
    <Card
      className={[
        "group overflow-hidden rounded-[20px] border-[var(--tf-border)] bg-white transition-all duration-200 hover:-translate-y-0.5 hover:border-[var(--tf-border-strong)] hover:shadow-sm",
        selected ? "border-[var(--tf-accent)] shadow-[0_0_0_1px_var(--tf-accent)]" : "",
        disabled && !selected ? "opacity-70" : "",
      ].join(" ")}
    >
      <CardContent className="flex h-full flex-col gap-2 p-2.5">
        <div className="relative overflow-hidden rounded-2xl bg-[var(--tf-bg-soft)]">
          <ImageWithFallback
            src={candidate.imageUrl}
            alt={candidate.title}
            wrapperClassName="aspect-[3/2] w-full rounded-2xl"
            className="aspect-[3/2] w-full rounded-2xl transition-transform duration-200 group-hover:scale-[1.02]"
          />

          {selected && (
            <div className="absolute right-2 top-2 rounded-full bg-[var(--tf-accent)] px-2.5 py-1 text-[10px] font-semibold text-white">
              {analyzing ? t("products.analyzing") : t("products.selected")}
            </div>
          )}
        </div>

        <div className="min-h-0 flex-1">
          <h3 className="line-clamp-2 min-h-[2.2rem] text-[13px] font-semibold leading-[1.22] text-[var(--tf-text)]">
            {displayTitle}
          </h3>
          {originalTitle && (
            <p className="mt-1 line-clamp-1 text-[10px] leading-4.5 text-[var(--tf-text-muted)]">
              {originalTitle}
            </p>
          )}

          <div className="mt-2 rounded-xl bg-[var(--tf-bg-soft)] px-2.5 py-1.5">
            <p className="text-[10px] text-[var(--tf-text-subtle)]">{t("products.market")}</p>
            <div className="mt-1 flex items-center justify-between gap-2">
              <p className="truncate text-[10px] font-medium text-[var(--tf-text-muted)]">{candidate.market}</p>
              {targetSiteUrl && <ExternalLink className="size-3.5 shrink-0 text-[var(--tf-text-subtle)]" />}
            </div>
          </div>

          <div className="mt-2 flex flex-wrap gap-1.5">
            {candidate.overseasPrice !== null && (
              <span className="rounded-full bg-[var(--tf-accent-soft)] px-2 py-1 text-[10px] font-semibold text-[var(--tf-accent-strong)]">
                {t("products.price")} ${candidate.overseasPrice}
              </span>
            )}
            {candidate.riskTag && (
              <span className="rounded-full border border-[var(--tf-border)] px-2 py-1 text-[10px] text-[var(--tf-text-muted)]">
                {candidate.riskTag}
              </span>
            )}
          </div>
        </div>

        <div className="mt-auto flex items-center gap-2">
          <Button
            size="sm"
            className="h-7.5 flex-1 rounded-xl px-2.5 text-[11px]"
            variant={selected ? "secondary" : "outline"}
            onClick={() => onAnalyze(candidate.productId)}
            disabled={disabled}
          >
            <FlaskConical className="size-4" />
            {analyzing && selected ? t("products.analyzing") : t("products.analyze")}
          </Button>
          {targetSiteUrl && (
            <Button
              variant="ghost"
              size="icon"
              className="size-7.5 rounded-xl"
              aria-label="open"
              onClick={() => window.open(targetSiteUrl, "_blank", "noopener,noreferrer")}
              disabled={disabled && selected}
            >
              <ExternalLink className="size-3.5" />
            </Button>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
