"use client"

import Link from "next/link"
import { useEffect, useState } from "react"
import { ArrowRight } from "lucide-react"

import { AppShell } from "@/components/layout/app-shell"
import { useAppI18n } from "@/components/layout/locale-provider"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { getTaskHistory } from "@/lib/api/task"
import type { TaskHistoryItem } from "@/types"

export default function HistoryPage() {
  const { t } = useAppI18n()
  const [items, setItems] = useState<TaskHistoryItem[]>([])

  useEffect(() => {
    void getTaskHistory()
      .then((response) => setItems(response.items))
      .catch(() => setItems([]))
  }, [])

  return (
    <AppShell activePanel="history">
      <div className="mx-auto max-w-5xl">
        <h1 className="text-2xl font-semibold text-[var(--tf-text)]">{t("history.title")}</h1>
        <p className="mt-2 text-sm text-[var(--tf-text-muted)]">{t("history.subtitle")}</p>

        {items.length === 0 ? (
          <Card className="mt-8">
            <CardContent className="p-6 text-sm text-[var(--tf-text-muted)]">{t("history.empty")}</CardContent>
          </Card>
        ) : (
          <div className="mt-8 grid gap-4">
            {items.map((item) => (
              <Card key={item.taskId}>
                <CardContent className="p-5">
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <p className="text-sm font-semibold text-[var(--tf-text)]">{item.keyword}</p>
                      <p className="mt-1 text-xs text-[var(--tf-text-muted)]">{item.market}</p>
                    </div>
                    <span className="rounded-full border border-[var(--tf-border)] bg-white/70 px-3 py-1 text-xs text-[var(--tf-text-muted)]">
                      {item.status}
                    </span>
                  </div>
                  <p className="mt-3 text-xs text-[var(--tf-text-subtle)]">{new Date(item.createdAt).toLocaleString()}</p>
                  <div className="mt-4">
                    <Link href={`/?taskId=${item.taskId}`}>
                      <Button variant="outline" size="sm">
                        {t("productGrid.openWorkbench")}
                        <ArrowRight className="size-4" />
                      </Button>
                    </Link>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}
      </div>
    </AppShell>
  )
}
