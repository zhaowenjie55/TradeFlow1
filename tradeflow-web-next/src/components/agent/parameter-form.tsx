"use client"

import { useEffect, useMemo, useRef, useState } from "react"
import { AlertTriangle, Languages, Mic, RotateCcw, Sparkles, UploadCloud } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Progress } from "@/components/ui/progress"
import { Separator } from "@/components/ui/separator"
import { Textarea } from "@/components/ui/textarea"
import { previewVoiceQuery, previewVoiceQueryText, submitMediaAnalyze } from "@/lib/api/asr"
import { getDemoConfig } from "@/lib/api/settings"
import { getReportTrustIndicator, getVerificationGuidance, humanizeQualityTier, humanizeReportSource } from "@/lib/presentation"
import { useAgentStore } from "@/stores/agent-store"
import { useTaskRunner } from "@/hooks/use-task-runner"
import { useVoiceRecorder } from "@/hooks/use-voice-recorder"
import { useAppI18n } from "@/components/layout/locale-provider"
import { useProductsStore } from "@/stores/products-store"
import { useSettingsStore } from "@/stores/settings-store"
import { useTaskStore } from "@/stores/task-store"
import type { AnalysisFormValues, DemoConfigResponse, MediaAnalysisResponse, VoiceQueryPreviewResponse } from "@/types"

function padTime(value: number) {
  return `00:${String(value).padStart(2, "0")}`
}

export function ParameterForm() {
  const { t } = useAppI18n()
  const { startTask, resumePhase2Task } = useTaskRunner()
  const voiceRecorder = useVoiceRecorder()
  const taskStore = useTaskStore()
  const taskLogs = useAgentStore((state) => state.taskLogs)
  const productsStore = useProductsStore()
  const settingsStore = useSettingsStore()
  const selectedProductId = useProductsStore((state) => state.selectedProductId)
  const currentReport = useProductsStore((state) =>
    state.selectedProductId ? state.reportsByProductId[state.selectedProductId] ?? null : null,
  )

  const [config, setConfig] = useState<DemoConfigResponse | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [isResumingVerification, setIsResumingVerification] = useState(false)
  const [isProcessingVoice, setIsProcessingVoice] = useState(false)
  const [isAnalyzingMedia, setIsAnalyzingMedia] = useState(false)
  const [voiceTranscript, setVoiceTranscript] = useState("")
  const [translatedVoiceText, setTranslatedVoiceText] = useState("")
  const [editableVoiceTranscript, setEditableVoiceTranscript] = useState("")
  const [editableTranslatedVoiceText, setEditableTranslatedVoiceText] = useState("")
  const [editableNormalizedKeyword, setEditableNormalizedKeyword] = useState("")
  const [voiceError, setVoiceError] = useState("")
  const [voicePreview, setVoicePreview] = useState<VoiceQueryPreviewResponse | null>(null)
  const [voicePreviewExpanded, setVoicePreviewExpanded] = useState(false)
  const [mediaAnalysis, setMediaAnalysis] = useState<MediaAnalysisResponse | null>(null)
  const [mediaError, setMediaError] = useState("")
  const mediaInputRef = useRef<HTMLInputElement | null>(null)

  const [form, setForm] = useState<AnalysisFormValues>({
    keyword: "",
    market: "AmazonUS",
    targetProfitMargin: 30,
    topN: 9,
  })

  const isBusy =
    isSubmitting ||
    taskStore.isPolling ||
    productsStore.isAnalyzingReport ||
    isProcessingVoice ||
    isAnalyzingMedia ||
    isResumingVerification

  const activeProvenanceBadges = useMemo(() => {
    const provenance = currentReport?.provenance
    if (!provenance) return []
    return [
      provenance.qualityTier
        ? { key: "quality", label: t("report.qualityTier"), value: humanizeQualityTier(provenance.qualityTier, t) }
        : null,
      { key: "fallback", label: t("report.fallback"), value: t(provenance.fallbackUsed ? "common.yes" : "common.no") },
      provenance.retrievalSource
        ? { key: "retrieval", label: t("report.matchSource"), value: humanizeReportSource(provenance.retrievalSource, t) }
        : null,
      provenance.detailSource
        ? { key: "detail", label: t("report.detailSource"), value: humanizeReportSource(provenance.detailSource, t) }
        : null,
    ].filter((item): item is { key: string; label: string; value: string } => Boolean(item))
  }, [currentReport?.provenance, t])

  const verificationGuidance = useMemo(() => getVerificationGuidance(taskLogs, t), [taskLogs, t])
  const currentTrustIndicator = useMemo(() => {
    if (!currentReport?.provenance) return null
    return getReportTrustIndicator(
      currentReport.provenance.qualityTier,
      currentReport.provenance.fallbackUsed,
      t,
    )
  }, [currentReport?.provenance, t])

  const marketOptions = config?.markets?.length ? config.markets : ["AmazonUS"]

  const voicePreviewDirty = useMemo(() => {
    if (!voicePreview) return false
    return (
      editableVoiceTranscript.trim() !== voiceTranscript.trim() ||
      editableTranslatedVoiceText.trim() !== translatedVoiceText.trim() ||
      editableNormalizedKeyword.trim() !== (voicePreview.normalizedKeyword || translatedVoiceText || voiceTranscript).trim()
    )
  }, [editableNormalizedKeyword, editableTranslatedVoiceText, editableVoiceTranscript, translatedVoiceText, voicePreview, voiceTranscript])

  useEffect(() => {
    settingsStore.hydrate()
    void getDemoConfig().then(setConfig).catch(() => setConfig(null))
  }, [settingsStore])

  useEffect(() => {
    setForm((current) => ({
      ...current,
      market: settingsStore.defaultMarket || "AmazonUS",
    }))
  }, [settingsStore.defaultMarket])

  const fillSample = () => {
    setForm((current) => ({
      ...current,
      keyword: "Acrylic Desktop Organizer",
      targetProfitMargin: 30,
      topN: 9,
      market: settingsStore.defaultMarket || "AmazonUS",
    }))
  }

  const resetForm = () => {
    setForm((current) => ({
      ...current,
      keyword: "",
      targetProfitMargin: 30,
      topN: 9,
      market: settingsStore.defaultMarket || "AmazonUS",
    }))
  }

  const clearVoicePreview = () => {
    setVoiceTranscript("")
    setTranslatedVoiceText("")
    setEditableVoiceTranscript("")
    setEditableTranslatedVoiceText("")
    setEditableNormalizedKeyword("")
    setVoicePreview(null)
    setVoicePreviewExpanded(false)
  }

  const applyVoicePreview = (response: VoiceQueryPreviewResponse) => {
    setVoicePreview(response)
    setVoicePreviewExpanded(false)
    setVoiceTranscript(response.transcript.text)
    setTranslatedVoiceText(response.translatedText)
    setEditableVoiceTranscript(response.transcript.text)
    setEditableTranslatedVoiceText(response.translatedText)
    const nextKeyword = response.normalizedKeyword || response.translatedText || response.transcript.text
    setEditableNormalizedKeyword(nextKeyword)
    setForm((current) => ({ ...current, keyword: nextKeyword }))
  }

  const submitVoiceFile = async (file: File) => {
    setVoiceError("")
    setIsProcessingVoice(true)
    try {
      clearVoicePreview()
      const response = await previewVoiceQuery(file, { language: "zh" })
      applyVoicePreview(response)
    } catch (error) {
      setVoiceError(error instanceof Error ? error.message : t("errors.voicePreview"))
    } finally {
      setIsProcessingVoice(false)
    }
  }

  const toggleVoiceInput = async () => {
    setVoiceError("")
    setMediaError("")
    try {
      if (voiceRecorder.isRecording) {
        const file = await voiceRecorder.stopRecording()
        await submitVoiceFile(file)
        return
      }
      clearVoicePreview()
      await voiceRecorder.startRecording()
    } catch (error) {
      setVoiceError(error instanceof Error ? error.message : t("errors.voicePermission"))
    }
  }

  const confirmVoiceSearch = async () => {
    const finalKeyword =
      editableNormalizedKeyword.trim() ||
      editableTranslatedVoiceText.trim() ||
      editableVoiceTranscript.trim()

    if (!finalKeyword) {
      setVoiceError(t("errors.voiceNoKeyword"))
      return
    }

    if (voicePreviewDirty && typeof window !== "undefined") {
      const shouldContinue = window.confirm(t("prompts.confirmKeywordOverride"))
      if (!shouldContinue) return
    }

    setForm((current) => ({ ...current, keyword: finalKeyword }))
    setIsSubmitting(true)
    try {
      await startTask({ ...form, keyword: finalKeyword })
    } finally {
      setIsSubmitting(false)
    }
  }

  const regenerateVoicePreview = async () => {
    const transcript = editableVoiceTranscript.trim()
    const translatedText = editableTranslatedVoiceText.trim()
    if (!transcript && !translatedText) {
      setVoiceError(t("errors.voicePreviewMissing"))
      return
    }

    setVoiceError("")
    setIsProcessingVoice(true)
    try {
      const response = await previewVoiceQueryText({
        transcript: transcript || translatedText,
        translatedText,
      })
      applyVoicePreview(response)
    } catch (error) {
      setVoiceError(error instanceof Error ? error.message : t("errors.voiceRegenerate"))
    } finally {
      setIsProcessingVoice(false)
    }
  }

  const restoreVoicePreview = () => {
    if (!voicePreview) {
      setVoiceError(t("errors.voiceNoPreview"))
      return
    }
    setVoiceError("")
    setEditableVoiceTranscript(voiceTranscript)
    setEditableTranslatedVoiceText(translatedVoiceText)
    const nextKeyword = voicePreview.normalizedKeyword || translatedVoiceText || voiceTranscript
    setEditableNormalizedKeyword(nextKeyword)
    setForm((current) => ({ ...current, keyword: nextKeyword }))
  }

  const syncTranslatedToKeyword = () => {
    const translated = editableTranslatedVoiceText.trim()
    if (!translated) {
      setVoiceError(t("errors.voiceNoTranslatedText"))
      return
    }
    setVoiceError("")
    setEditableNormalizedKeyword(translated)
    setForm((current) => ({ ...current, keyword: translated }))
  }

  const continueDomesticVerification = async () => {
    if (taskStore.status !== "WAITING_1688_VERIFICATION") return
    setIsResumingVerification(true)
    try {
      await resumePhase2Task()
    } finally {
      setIsResumingVerification(false)
    }
  }

  const handleMediaPicked = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    if (!file) return

    setMediaError("")
    setIsAnalyzingMedia(true)
    try {
      setMediaAnalysis(await submitMediaAnalyze(file))
    } catch (error) {
      setMediaError(error instanceof Error ? error.message : t("errors.mediaAnalyze"))
    } finally {
      setIsAnalyzingMedia(false)
      event.target.value = ""
    }
  }

  const handleSubmit = async () => {
    if (!form.keyword.trim()) return
    setIsSubmitting(true)
    try {
      await startTask(form)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="tradeflow-scrollbar h-full overflow-auto px-2.5 py-2">
      <div className="space-y-2">
        <Card>
          <CardContent className="space-y-2 p-2.5">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 flex-1">
                <p className="text-[11px] uppercase tracking-[0.18em] text-[var(--tf-text-subtle)]">{t("workspace.eyebrow")}</p>
                <h2 className="mt-0.5 text-[15px] font-semibold leading-tight text-[var(--tf-text)]">{t("task.title")}</h2>
                <p className="mt-0.5 line-clamp-2 text-[10px] leading-[1.45] text-[var(--tf-text-muted)]">{t("task.subtitle")}</p>
              </div>
              <span className="shrink-0 rounded-full border border-[var(--tf-border)] px-2.5 py-1 text-[10px] text-[var(--tf-text-muted)]">
                {taskStore.status === "IDLE" ? t("workspace.idle") : t(`status.${taskStore.status}`)}
              </span>
            </div>

            <div className="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-2.5">
              <p className="text-[11px] uppercase tracking-[0.18em] text-[var(--tf-text-subtle)]">{t("workspace.currentTask")}</p>
              <h3 className="mt-1 line-clamp-2 text-[14px] font-semibold leading-snug text-[var(--tf-text)]">{taskStore.params.keyword || "--"}</h3>
              <p className="mt-0.5 text-[11px] text-[var(--tf-text-muted)]">{taskStore.status === "IDLE" ? t("workspace.idle") : t(`status.${taskStore.status}`)}</p>
              <div className="mt-1.5 space-y-1">
                <div className="flex items-center justify-between text-xs text-[var(--tf-text-muted)]">
                  <span>{t("workspace.taskProgress")}</span>
                  <span>{taskStore.progress}%</span>
                </div>
                <Progress value={taskStore.progress} />
              </div>
              {taskStore.currentTaskPhase === "PHASE2" && selectedProductId && activeProvenanceBadges.length > 0 && (
                <>
                  {currentTrustIndicator && (
                    <div className={`mt-2.5 rounded-2xl border px-3 py-2.5 text-xs leading-5 ${getToneClasses(currentTrustIndicator.tone)}`}>
                      <p className="font-semibold">{currentTrustIndicator.title}</p>
                      <p className="mt-1">{currentTrustIndicator.description}</p>
                    </div>
                  )}
                  <div className="mt-2.5 flex flex-wrap gap-1.5">
                    {activeProvenanceBadges.map((badge) => (
                      <span
                        key={badge.key}
                        className="rounded-full border border-[var(--tf-border)] bg-white px-2 py-1 text-[10px] leading-none text-[var(--tf-text-muted)]"
                      >
                        {badge.label} · {badge.value}
                      </span>
                    ))}
                  </div>
                </>
              )}
            </div>

            {taskStore.errorMessage && (
              <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-600">
                {taskStore.errorMessage}
              </div>
            )}

            {taskStore.status === "WAITING_1688_VERIFICATION" && (
              <div className="rounded-2xl border border-sky-200 bg-sky-50 px-3 py-3">
                <div className="flex flex-col gap-2">
                  <div className="flex items-start gap-2">
                    <AlertTriangle className="mt-0.5 size-4 shrink-0 text-sky-600" />
                    <div className="min-w-0">
                      <p className="text-xs font-semibold leading-5 text-sky-900">{verificationGuidance.title}</p>
                      <p className="mt-1 text-xs leading-[1.5] text-sky-800">{verificationGuidance.detail}</p>
                      <p className="mt-2 text-[11px] leading-5 text-sky-700">{verificationGuidance.hint}</p>
                    </div>
                  </div>
                  <Button size="sm" variant="outline" className="h-8 shrink-0 self-start rounded-xl bg-white text-xs" onClick={continueDomesticVerification} disabled={isResumingVerification}>
                    {isResumingVerification ? t("workspace.resumingVerification") : t("workspace.resumeVerification")}
                  </Button>
                </div>
              </div>
            )}

            <div className="rounded-2xl border border-[var(--tf-border)] p-2.5">
              <div className="flex items-center gap-2 text-sm font-medium text-[var(--tf-text)]">
                <Mic className="size-4 text-emerald-500" />
                {t("workspace.voiceLabel")}
              </div>
              <p className="mt-1 text-[11px] leading-4.5 text-[var(--tf-text-muted)]">{t("workspace.voiceSubtitle")}</p>

              <div className="mt-2.5 space-y-2">
                <Input
                  value={form.keyword}
                  onChange={(event) => setForm((current) => ({ ...current, keyword: event.target.value }))}
                  placeholder={t("workspace.keywordPlaceholder")}
                  className="h-8.5 w-full text-sm"
                />
                <Button size="sm" className={voiceRecorder.isRecording ? "h-8.5 w-full rounded-xl bg-red-500 text-sm hover:bg-red-600" : "h-8.5 w-full rounded-xl text-sm"} onClick={toggleVoiceInput}>
                  <Mic className="size-4" />
                  {voiceRecorder.isRecording ? t("workspace.stopVoice") : t("workspace.startVoice")}
                </Button>
              </div>

              {voiceRecorder.isRecording && (
                <div className="mt-2.5 rounded-2xl border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-600">
                  {t("workspace.recordingHint")}
                  <span className="ml-2 font-mono">
                    {padTime(voiceRecorder.elapsedSeconds)} / {padTime(voiceRecorder.maxDurationSeconds)}
                  </span>
                </div>
              )}

              {isProcessingVoice && (
                <div className="mt-2.5 rounded-2xl border border-blue-200 bg-blue-50 px-3 py-2.5 text-sm text-blue-600">
                  {t("workspace.processingVoice")}
                </div>
              )}

              {voiceError && (
                <div className="mt-2.5 rounded-2xl border border-red-200 bg-red-50 px-3 py-2.5 text-sm text-red-600">
                  {voiceError}
                </div>
              )}

              {voicePreview && (
                <div className="mt-2.5 space-y-2.5">
                  <StepPills
                    steps={[t("workspace.stepRecord"), t("workspace.stepDescribe"), t("workspace.stepConfirm")]}
                  />
                  <div className="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-2.5">
                    <div className="flex items-center justify-between gap-2">
                      <div className="min-w-0">
                        <p className="text-[11px] uppercase tracking-[0.18em] text-[var(--tf-text-subtle)]">{t("workspace.previewSummary")}</p>
                        <p className="mt-0.5 line-clamp-1 text-xs font-medium text-[var(--tf-text)]">
                          {editableNormalizedKeyword || editableTranslatedVoiceText || editableVoiceTranscript}
                        </p>
                      </div>
                      <Button
                        size="sm"
                        variant="outline"
                        className="h-7.5 shrink-0 rounded-xl px-2.5 text-[11px]"
                        onClick={() => setVoicePreviewExpanded((current) => !current)}
                      >
                        {voicePreviewExpanded ? t("workspace.collapsePreview") : t("workspace.expandPreview")}
                      </Button>
                    </div>
                    <div className="mt-2 grid gap-1.5">
                      <CompactPreviewRow label={t("workspace.recognitionTitle")} value={editableVoiceTranscript} />
                      <CompactPreviewRow label={t("workspace.translatedTitle")} value={editableTranslatedVoiceText} />
                      <CompactPreviewRow label={t("workspace.normalizedTitle")} value={editableNormalizedKeyword} strong />
                    </div>
                  </div>
                  {voicePreviewDirty && (
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 px-3 py-2.5 text-sm text-slate-700">
                      {t("workspace.voiceDirtyHint")}
                    </div>
                  )}
                  {voicePreviewExpanded && (
                    <div className="space-y-2.5">
                      <EditableField
                        title={t("workspace.recognitionTitle")}
                        value={editableVoiceTranscript}
                        onChange={setEditableVoiceTranscript}
                        placeholder={t("workspace.recognitionPlaceholder")}
                        editableHint={t("workspace.editableHint")}
                      />
                      <EditableField
                        title={t("workspace.translatedTitle")}
                        value={editableTranslatedVoiceText}
                        onChange={setEditableTranslatedVoiceText}
                        placeholder={t("workspace.translatedPlaceholder")}
                        editableHint={t("workspace.editableHint")}
                      />
                      <EditableField
                        title={t("workspace.normalizedTitle")}
                        value={editableNormalizedKeyword}
                        onChange={setEditableNormalizedKeyword}
                        placeholder={t("workspace.normalizedPlaceholder")}
                        editableHint={t("workspace.editableHint")}
                      />
                    </div>
                  )}
                  <div className="grid gap-2">
                    <Button size="sm" variant="outline" className="justify-start text-xs" onClick={restoreVoicePreview}>
                      <RotateCcw className="size-4" />
                      {t("workspace.restoreVoice")}
                    </Button>
                    <Button size="sm" variant="outline" className="justify-start text-xs" onClick={syncTranslatedToKeyword}>
                      <Languages className="size-4" />
                      {t("workspace.syncToKeyword")}
                    </Button>
                    <Button size="sm" variant="outline" className="justify-start text-xs" onClick={regenerateVoicePreview} disabled={isProcessingVoice}>
                      <Sparkles className="size-4" />
                      {t("workspace.regenerateVoice")}
                    </Button>
                    <Button size="sm" className="text-xs" onClick={confirmVoiceSearch} disabled={isBusy}>
                      {t("workspace.confirmSearch")}
                    </Button>
                  </div>
                </div>
              )}
            </div>

            <div className="rounded-2xl border border-[var(--tf-border)] p-2.5">
              <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-[var(--tf-text)]">{t("workspace.mediaTitle")}</p>
                  <p className="mt-0.5 text-xs leading-4 text-[var(--tf-text-muted)]">{t("workspace.mediaSubtitle")}</p>
                </div>
                <Button size="sm" variant="outline" className="h-8 rounded-xl text-xs sm:self-auto self-start" onClick={() => mediaInputRef.current?.click()}>
                  <UploadCloud className="size-4" />
                  {t("workspace.uploadMedia")}
                </Button>
              </div>
              <input ref={mediaInputRef} type="file" className="hidden" accept="audio/*,video/*" onChange={handleMediaPicked} />
              {isAnalyzingMedia && <p className="mt-3 text-sm text-[var(--tf-text-muted)]">{t("workspace.analyzingMedia")}</p>}
              {mediaError && <p className="mt-3 text-sm text-red-600">{mediaError}</p>}
              {mediaAnalysis && (
              <div className="mt-2.5 rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] p-2.5">
                  <p className="text-sm font-medium text-[var(--tf-text)]">{t("workspace.transcriptTitle")}</p>
                  <p className="mt-1.5 text-xs leading-5 text-[var(--tf-text-muted)]">{mediaAnalysis.transcript.text}</p>
                </div>
              )}
            </div>

            <Separator />

            <div className="grid gap-2.5">
              <label className="space-y-2">
                <span className="text-sm font-medium text-[var(--tf-text)]">{t("workspace.targetMarket")}</span>
                <select
                  className="flex h-10 w-full rounded-2xl border border-[var(--tf-border)] bg-white px-3 text-sm text-[var(--tf-text)] outline-none"
                  value={form.market}
                  onChange={(event) => setForm((current) => ({ ...current, market: event.target.value }))}
                >
                  {marketOptions.map((market) => (
                    <option key={market} value={market}>
                      {market}
                    </option>
                  ))}
                </select>
              </label>

              <label className="space-y-2">
                <span className="text-sm font-medium text-[var(--tf-text)]">{t("workspace.topN")}</span>
                <div className="flex items-center gap-3">
                  <input
                    type="range"
                    min={3}
                    max={12}
                    value={form.topN}
                    onChange={(event) => setForm((current) => ({ ...current, topN: Number(event.target.value) }))}
                    className="w-full"
                  />
                  <span className="w-8 text-sm text-[var(--tf-text-muted)]">{form.topN}</span>
                </div>
              </label>
            </div>

            <div className="grid gap-2 sm:grid-cols-2">
              <Button size="sm" variant="outline" className="text-xs" onClick={fillSample}>
                {t("workspace.fillSample")}
              </Button>
              <Button size="sm" variant="outline" className="text-xs" onClick={resetForm}>
                {t("workspace.reset")}
              </Button>
            </div>

            <Button className="h-10 w-full rounded-xl text-sm" size="sm" onClick={handleSubmit} disabled={isBusy || !form.keyword.trim()}>
              {isSubmitting ? t("common.processing") : t("workspace.startAnalysis")}
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

function StepPills({ steps }: { steps: string[] }) {
  return (
    <div className="flex flex-wrap gap-2">
      {steps.map((item) => (
        <span key={item} className="rounded-full border border-[var(--tf-border)] bg-white px-3 py-1 text-[11px] text-[var(--tf-text-muted)]">
          {item}
        </span>
      ))}
    </div>
  )
}

function getToneClasses(tone: "success" | "info" | "warning" | "danger") {
  switch (tone) {
    case "success":
      return "border-emerald-200 bg-emerald-50 text-emerald-800"
    case "warning":
      return "border-sky-200 bg-sky-50 text-sky-800"
    case "danger":
      return "border-red-200 bg-red-50 text-red-800"
    default:
      return "border-sky-200 bg-sky-50 text-sky-800"
  }
}

function EditableField({
  title,
  value,
  onChange,
  placeholder,
  editableHint,
}: {
  title: string
  value: string
  onChange: (value: string) => void
  placeholder: string
  editableHint: string
}) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-2">
        <p className="min-w-0 truncate text-sm font-medium text-[var(--tf-text)]">{title}</p>
        <span className="shrink-0 text-xs text-[var(--tf-text-subtle)]">{editableHint}</span>
      </div>
      <Textarea className="min-h-[84px]" value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
    </div>
  )
}

function CompactPreviewRow({
  label,
  value,
  strong = false,
}: {
  label: string
  value: string
  strong?: boolean
}) {
  return (
    <div className="flex items-center justify-between gap-3 rounded-xl border border-[var(--tf-border)] bg-white px-2.5 py-2">
      <span className="shrink-0 text-[10px] uppercase tracking-[0.14em] text-[var(--tf-text-subtle)]">{label}</span>
      <span
        className={
          strong
            ? "min-w-0 truncate text-[11px] font-semibold text-[var(--tf-text)]"
            : "min-w-0 truncate text-[11px] text-[var(--tf-text-muted)]"
        }
      >
        {value || "--"}
      </span>
    </div>
  )
}
