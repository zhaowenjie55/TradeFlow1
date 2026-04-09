<script setup lang="ts">
import AppSelect from '~/components/ui/AppSelect.vue'
import { previewVoiceQuery, previewVoiceQueryText, submitMediaAnalyze } from '~/services/asr'
import { getDemoConfig } from '~/services/settings'
import type { AnalysisFormValues, DemoConfigResponse, MediaAnalysisResponse, VoiceQueryPreviewResponse } from '~/types'
import { getReadableProductName } from '~/utils/presentation'

const { t } = useAppI18n()
const { startTask, resumePhase2Task } = useTaskRunner()
const voiceRecorder = useVoiceRecorder()
const taskStore = useTaskStore()
const productsStore = useProductsStore()
const settingsStore = useSettingsStore()
const props = withDefaults(defineProps<{
  compact?: boolean
}>(), {
  compact: false,
})

const config = ref<DemoConfigResponse | null>(null)
const isSubmitting = ref(false)
const isResumingVerification = ref(false)
const isProcessingVoice = ref(false)
const isAnalyzingMedia = ref(false)
const voiceTranscript = ref('')
const translatedVoiceText = ref('')
const editableVoiceTranscript = ref('')
const editableTranslatedVoiceText = ref('')
const editableNormalizedKeyword = ref('')
const voiceError = ref('')
const voicePreview = ref<VoiceQueryPreviewResponse | null>(null)
const mediaAnalysis = ref<MediaAnalysisResponse | null>(null)
const mediaError = ref('')
const mediaInput = ref<HTMLInputElement | null>(null)
const defaultMarket = 'AmazonUS'

const form = reactive<AnalysisFormValues>({
  keyword: '',
  market: defaultMarket,
  targetProfitMargin: 30,
  topN: 9,
})

const isBusy = computed(() =>
  isSubmitting.value
  || taskStore.isPolling
  || productsStore.isAnalyzingReport
  || isProcessingVoice.value
  || isAnalyzingMedia.value
  || isResumingVerification.value
)
const marketOptions = computed(() => config.value?.markets?.length ? config.value.markets : [defaultMarket])
const syncFromSettings = () => {
  form.market = settingsStore.settings.defaultMarket || defaultMarket
}

const fillSample = () => {
  form.keyword = 'Acrylic Desktop Organizer'
  form.targetProfitMargin = 30
  form.topN = 9
  syncFromSettings()
}

const resetForm = () => {
  form.keyword = ''
  form.targetProfitMargin = 30
  form.topN = 9
  syncFromSettings()
}

const handleSubmit = async () => {
  if (!form.keyword.trim()) return

  isSubmitting.value = true

  try {
    await startTask({ ...form })
  } catch {
    // Error state is already written into the task store.
  } finally {
    isSubmitting.value = false
  }
}

const clearVoicePreview = () => {
  voiceTranscript.value = ''
  translatedVoiceText.value = ''
  editableVoiceTranscript.value = ''
  editableTranslatedVoiceText.value = ''
  editableNormalizedKeyword.value = ''
  voicePreview.value = null
}

const applyVoicePreview = (response: VoiceQueryPreviewResponse) => {
  voicePreview.value = response
  voiceTranscript.value = response.transcript.text
  translatedVoiceText.value = response.translatedText
  editableVoiceTranscript.value = response.transcript.text
  editableTranslatedVoiceText.value = response.translatedText
  editableNormalizedKeyword.value = response.normalizedKeyword || response.translatedText || response.transcript.text
  form.keyword = editableNormalizedKeyword.value
}

const submitVoiceFile = async (file: File) => {
  voiceError.value = ''
  isProcessingVoice.value = true

  try {
    clearVoicePreview()
    const response = await previewVoiceQuery(file, { language: 'zh' })
    applyVoicePreview(response)
  } catch (error) {
    voiceError.value = error instanceof Error ? error.message : '语音识别失败，请重试。'
  } finally {
    isProcessingVoice.value = false
  }
}

const toggleVoiceInput = async () => {
  voiceError.value = ''
  mediaError.value = ''

  try {
    if (voiceRecorder.isRecording.value) {
      const file = await voiceRecorder.stopRecording()
      await submitVoiceFile(file)
      return
    }
    clearVoicePreview()
    await voiceRecorder.startRecording()
  } catch (error) {
    voiceError.value = error instanceof Error ? error.message : '录音失败，请检查浏览器权限。'
  }
}

const confirmVoiceSearch = async () => {
  const finalKeyword = editableNormalizedKeyword.value.trim()
    || editableTranslatedVoiceText.value.trim()
    || editableVoiceTranscript.value.trim()

  if (!finalKeyword) {
    voiceError.value = '当前没有可确认的语音搜索词，请重新录音。'
    return
  }
  if (voicePreviewDirty.value && import.meta.client) {
    const shouldContinue = window.confirm('你已经修改了语音内容，但尚未重新生成关键词。是否直接使用当前“最终搜索关键词”发起搜索？')
    if (!shouldContinue) {
      return
    }
  }
  form.keyword = finalKeyword
  await handleSubmit()
}

const restoreVoicePreview = () => {
  if (!voicePreview.value) {
    voiceError.value = '当前没有可恢复的语音结果，请先录音。'
    return
  }
  voiceError.value = ''
  editableVoiceTranscript.value = voiceTranscript.value
  editableTranslatedVoiceText.value = translatedVoiceText.value
  editableNormalizedKeyword.value = voicePreview.value.normalizedKeyword || translatedVoiceText.value || voiceTranscript.value
  form.keyword = editableNormalizedKeyword.value
}

const syncTranslatedToKeyword = () => {
  const translated = editableTranslatedVoiceText.value.trim()
  if (!translated) {
    voiceError.value = '当前没有可同步的英文搜索词。'
    return
  }
  voiceError.value = ''
  editableNormalizedKeyword.value = translated
  form.keyword = translated
}

const regenerateVoicePreview = async () => {
  const transcript = editableVoiceTranscript.value.trim()
  const translatedText = editableTranslatedVoiceText.value.trim()
  if (!transcript && !translatedText) {
    voiceError.value = '请先填写识别文本或英文搜索词。'
    return
  }

  voiceError.value = ''
  isProcessingVoice.value = true
  try {
    const response = await previewVoiceQueryText({
      transcript: transcript || translatedText,
      translatedText,
    })
    applyVoicePreview(response)
  } catch (error) {
    voiceError.value = error instanceof Error ? error.message : '重新生成搜索词失败，请稍后再试。'
  } finally {
    isProcessingVoice.value = false
  }
}

const openMediaPicker = () => {
  mediaInput.value?.click()
}

const continueDomesticVerification = async () => {
  if (taskStore.status !== 'WAITING_1688_VERIFICATION') return

  isResumingVerification.value = true
  try {
    await resumePhase2Task()
  } finally {
    isResumingVerification.value = false
  }
}

const handleMediaPicked = async (event: Event) => {
  const input = event.target as HTMLInputElement | null
  const file = input?.files?.[0]
  if (!file) return

  mediaError.value = ''
  isAnalyzingMedia.value = true

  try {
    mediaAnalysis.value = await submitMediaAnalyze(file)
  } catch (error) {
    mediaError.value = error instanceof Error ? error.message : '媒体分析失败，请稍后再试。'
  } finally {
    isAnalyzingMedia.value = false
    if (input) {
      input.value = ''
    }
  }
}

onMounted(async () => {
  voiceRecorder.resetRecorder()
  try {
    config.value = await getDemoConfig()
  } catch {
    config.value = null
  }
  syncFromSettings()
})

onBeforeUnmount(() => {
  voiceRecorder.resetRecorder()
})

const selectedDisplayTitle = computed(() => {
  if (!productsStore.currentCandidate?.title) return ''
  return getReadableProductName(productsStore.currentCandidate.title)
})

const selectedCandidateMeta = computed(() => {
  if (!productsStore.currentCandidate) return []

  return [
    {
      key: 'margin',
      label: t('products.margin'),
      value: productsStore.currentCandidate.estimatedMargin !== null ? `+${productsStore.currentCandidate.estimatedMargin}%` : '--',
    },
    {
      key: 'risk',
      label: t('products.riskTag'),
      value: productsStore.currentCandidate.riskTag ?? '--',
    },
  ]
})

const panelClass = computed(() => props.compact ? 'h-auto p-5' : 'h-full px-4 py-4 xl:px-5 xl:py-4')
const scrollClass = computed(() => props.compact ? 'gap-5 pr-0' : 'gap-3.5 pr-1')
const isRecording = computed(() => voiceRecorder.isRecording.value)
const recorderError = computed(() => voiceError.value || voiceRecorder.errorMessage.value)
const hasVoicePreview = computed(() => Boolean(voicePreview.value))
const voicePreviewDirty = computed(() => {
  if (!voicePreview.value) return false
  return editableVoiceTranscript.value !== voiceTranscript.value
    || editableTranslatedVoiceText.value !== translatedVoiceText.value
    || editableNormalizedKeyword.value !== (voicePreview.value.normalizedKeyword || translatedVoiceText.value || voiceTranscript.value)
})
const voiceTimerLabel = computed(() => {
  const elapsed = voiceRecorder.elapsedSeconds.value
  const total = voiceRecorder.maxDurationSeconds
  const format = (value: number) => `00:${String(value).padStart(2, '0')}`
  return `${format(elapsed)} / ${format(total)}`
})
const voiceActionLabel = computed(() => {
  if (isProcessingVoice.value) return '正在转写...'
  return isRecording.value ? '结束录音并发起搜索' : '点击开始录音'
})
const voiceStatusLabel = computed(() => {
  if (isProcessingVoice.value) return '语音转写中'
  if (isRecording.value) return '正在录音'
  return '语音输入待命'
})
const voiceHintText = computed(() => {
  if (isProcessingVoice.value) {
    return '系统正在识别语音并翻译成英文关键词，请稍候。'
  }
  if (isRecording.value) {
    return '请自然说出你的选品需求，讲完后点击右侧按钮结束录音。'
  }
  if (hasVoicePreview.value) {
    return '请先检查并编辑下面的识别文本、英文搜索词与最终关键词，确认无误后再发起搜索。'
  }
  return '点击开始录音后再讲话。支持中文语音识别，并会自动转换成英文搜索词供你确认。'
})
const showVerificationResume = computed(() =>
  taskStore.currentTaskPhase === 'PHASE2' && taskStore.status === 'WAITING_1688_VERIFICATION'
)
</script>

<template>
  <div :class="['flex min-h-0 flex-col', panelClass]">
    <div class="mb-3">
      <div class="flex items-center justify-between gap-3">
        <div>
          <h2 class="text-lg font-semibold tracking-tight text-slate-900 dark:text-slate-100">{{ t('task.title') }}</h2>
          <p class="mt-0.5 text-[13px] leading-5 text-slate-500 dark:text-slate-400">{{ t('task.subtitle') }}</p>
        </div>
        <span class="rounded-full border border-[var(--tf-border)] bg-white/80 px-2.5 py-1 text-[11px] font-medium text-slate-600 dark:bg-slate-950/80 dark:text-slate-300">
          {{ t(`status.${taskStore.status}`) }}
        </span>
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col">
      <div :class="['tradeflow-scrollbar flex min-h-0 flex-1 flex-col overflow-y-auto', scrollClass]">
        <div class="tradeflow-panel rounded-[var(--tf-radius-xl)] px-4 py-3.5">
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="tradeflow-section-title">{{ t('task.currentTask') }}</p>
              <p class="mt-1.5 line-clamp-2 text-[15px] font-semibold leading-6 text-slate-900 dark:text-slate-100">{{ form.keyword || '--' }}</p>
              <p class="mt-0.5 text-[13px] text-slate-500 dark:text-slate-400">{{ t(`common.${taskStore.mode}`) }}</p>
            </div>
          </div>

          <div class="mt-3">
            <div class="mb-1.5 flex items-center justify-between text-[13px] text-slate-500 dark:text-slate-400">
              <span>{{ t('task.progress') }}</span>
              <span class="font-medium text-slate-700 dark:text-slate-200">{{ taskStore.progress }}%</span>
            </div>
            <div class="h-2 rounded-full bg-slate-200/80 dark:bg-slate-800">
              <div class="h-2 rounded-full bg-blue-500 transition-all duration-500" :style="{ width: `${taskStore.progress}%` }" />
            </div>
            <p class="mt-1.5 text-[13px] text-slate-500 dark:text-slate-400">{{ t(`stage.${taskStore.stage}`) }}</p>
          </div>
        </div>

        <div class="rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-soft)] px-4 py-3 text-sm leading-6 text-slate-600 dark:text-slate-300">
          <p class="line-clamp-2 font-medium leading-5 text-slate-800 dark:text-slate-100">{{ t('task.backendNotice') }}</p>
        </div>

        <div
          v-if="showVerificationResume"
          class="rounded-2xl border border-amber-200 bg-amber-50/90 px-4 py-3.5 text-sm text-amber-900 shadow-[0_16px_32px_-28px_rgba(245,158,11,0.45)] dark:border-amber-900/40 dark:bg-amber-950/20 dark:text-amber-100"
        >
          <div class="flex items-start justify-between gap-3">
            <div class="min-w-0">
              <p class="text-sm font-semibold">需要完成 1688 验证</p>
              <p class="mt-1 text-[13px] leading-6 text-amber-800/90 dark:text-amber-100/90">
                系统检测到 1688 登录或滑块验证。请在弹出的浏览器窗口完成验证，然后点击“完成验证后继续抓取”恢复国内货源检索。
              </p>
            </div>
            <button
              type="button"
              class="shrink-0 rounded-xl bg-amber-500 px-3 py-2 text-xs font-semibold text-white transition hover:bg-amber-400 disabled:cursor-not-allowed disabled:bg-amber-300 dark:disabled:bg-amber-800"
              :disabled="isBusy"
              @click="continueDomesticVerification"
            >
              {{ isResumingVerification ? '正在继续抓取...' : '完成验证后继续抓取' }}
            </button>
          </div>
        </div>

        <div class="space-y-2">
          <label for="keyword" class="tradeflow-section-title">{{ t('task.keyword') }}</label>
          <div
            :class="[
              'rounded-[26px] border px-4 py-4 transition-all duration-200',
              isRecording
                ? 'border-red-200 bg-[linear-gradient(135deg,rgba(254,242,242,0.96),rgba(255,255,255,0.98))] shadow-[0_18px_42px_-28px_rgba(239,68,68,0.45)] dark:border-red-900/60 dark:bg-[linear-gradient(135deg,rgba(69,10,10,0.32),rgba(2,6,23,0.88))]'
                : isProcessingVoice
                  ? 'border-blue-200 bg-[linear-gradient(135deg,rgba(239,246,255,0.96),rgba(255,255,255,0.98))] shadow-[0_18px_42px_-30px_rgba(59,130,246,0.35)] dark:border-blue-900/60 dark:bg-[linear-gradient(135deg,rgba(30,64,175,0.18),rgba(2,6,23,0.88))]'
                  : 'border-[var(--tf-border)] bg-[linear-gradient(135deg,rgba(248,250,252,0.98),rgba(255,255,255,0.98))] shadow-[0_18px_42px_-34px_rgba(15,23,42,0.25)] dark:bg-[linear-gradient(135deg,rgba(15,23,42,0.92),rgba(2,6,23,0.92))]'
            ]"
          >
            <div class="flex items-start justify-between gap-3">
              <div class="min-w-0">
                <div class="flex items-center gap-2">
                  <span
                    :class="[
                      'inline-flex h-2.5 w-2.5 rounded-full',
                      isRecording ? 'bg-red-500 shadow-[0_0_0_6px_rgba(239,68,68,0.12)]' : isProcessingVoice ? 'bg-blue-500 shadow-[0_0_0_6px_rgba(59,130,246,0.10)]' : 'bg-emerald-500 shadow-[0_0_0_6px_rgba(16,185,129,0.10)]'
                    ]"
                  />
                  <p class="text-[12px] font-semibold tracking-[0.08em] text-slate-500 uppercase dark:text-slate-400">Voice Search</p>
                </div>
                <p class="mt-2 text-sm font-semibold text-slate-900 dark:text-slate-100">{{ voiceStatusLabel }}</p>
                <p class="mt-1 text-[13px] leading-5 text-slate-500 dark:text-slate-400">{{ voiceHintText }}</p>
              </div>
              <div
                v-if="isRecording || isProcessingVoice"
                :class="[
                  'shrink-0 rounded-full px-3 py-1 text-[12px] font-semibold',
                  isRecording ? 'bg-white/85 text-red-700 dark:bg-red-950/60 dark:text-red-200' : 'bg-white/85 text-blue-700 dark:bg-blue-950/60 dark:text-blue-200'
                ]"
              >
                {{ isRecording ? voiceTimerLabel : '正在转写...' }}
              </div>
            </div>

            <div class="mt-4 flex gap-2">
              <input
                id="keyword"
                v-model="form.keyword"
                type="text"
                :placeholder="t('task.keywordPlaceholder')"
                class="block min-w-0 flex-1 rounded-2xl border border-[var(--tf-border)] bg-white/90 px-4 py-3 text-[15px] text-slate-800 shadow-sm transition-all duration-200 placeholder:text-slate-400 focus:border-[var(--tf-border-strong)] focus:outline-none dark:bg-slate-900/90 dark:text-slate-200 dark:placeholder:text-slate-600"
                :disabled="isBusy"
              />
              <button
                type="button"
                class="inline-flex min-w-[158px] items-center justify-center gap-2 rounded-2xl border px-4 py-3 text-sm font-semibold transition"
                :disabled="isBusy"
                :class="isRecording
                  ? 'border-red-300 bg-red-600 text-white hover:bg-red-500 dark:border-red-800 dark:bg-red-600'
                  : isProcessingVoice
                    ? 'border-blue-300 bg-blue-600 text-white dark:border-blue-800 dark:bg-blue-600'
                    : 'border-slate-200 bg-slate-950 text-white hover:bg-slate-800 dark:border-slate-700 dark:bg-white dark:text-slate-900 dark:hover:bg-slate-100'"
                :title="voiceActionLabel"
                @click="toggleVoiceInput"
              >
                <UIcon
                  :name="isRecording ? 'i-heroicons-stop-circle' : isProcessingVoice ? 'i-heroicons-arrow-path' : 'i-heroicons-microphone'"
                  :class="['h-5 w-5', isProcessingVoice ? 'animate-spin' : '']"
                />
                <span>{{ voiceActionLabel }}</span>
              </button>
            </div>

            <div class="mt-3 flex flex-wrap gap-2">
              <span class="rounded-full border border-[var(--tf-border)] bg-white/85 px-3 py-1 text-[12px] font-medium text-slate-600 dark:bg-slate-950/70 dark:text-slate-300">
                1. 点击开始录音
              </span>
              <span class="rounded-full border border-[var(--tf-border)] bg-white/85 px-3 py-1 text-[12px] font-medium text-slate-600 dark:bg-slate-950/70 dark:text-slate-300">
                2. 自然描述选品需求
              </span>
              <span class="rounded-full border border-[var(--tf-border)] bg-white/85 px-3 py-1 text-[12px] font-medium text-slate-600 dark:bg-slate-950/70 dark:text-slate-300">
                3. 确认英文搜索词后再搜索
              </span>
            </div>

            <div v-if="voiceTranscript || translatedVoiceText || recorderError" class="mt-4 space-y-3">
              <div
                v-if="voiceTranscript"
                class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-3 dark:bg-slate-950/70"
              >
                <div class="flex items-center justify-between gap-3">
                  <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">识别文本</p>
                  <span class="text-[11px] text-slate-400 dark:text-slate-500">可手动修正</span>
                </div>
                <textarea
                  v-model="editableVoiceTranscript"
                  rows="3"
                  class="mt-2 block w-full resize-y rounded-2xl border border-[var(--tf-border)] bg-white px-3 py-2.5 text-sm leading-6 text-slate-800 transition placeholder:text-slate-400 focus:border-[var(--tf-border-strong)] focus:outline-none dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-600"
                  placeholder="在这里检查并修改识别文本"
                />
              </div>
              <div
                v-if="translatedVoiceText"
                class="rounded-2xl border border-blue-200 bg-blue-50/70 px-3 py-3 dark:border-blue-900/50 dark:bg-blue-950/20"
              >
                <div
                  v-if="voicePreviewDirty"
                  class="mb-3 rounded-2xl border border-amber-200 bg-amber-50/90 px-3 py-2.5 text-[12px] leading-5 text-amber-800 dark:border-amber-900/50 dark:bg-amber-950/30 dark:text-amber-200"
                >
                  你已经修改了语音识别结果。建议先点击“重新生成关键词”，再确认搜索。
                </div>
                <div class="flex items-center justify-between gap-3">
                  <p class="text-[12px] font-medium text-blue-700 dark:text-blue-300">英文搜索词预览</p>
                  <span class="text-[11px] text-blue-500/80 dark:text-blue-300/80">可手动修正</span>
                </div>
                <textarea
                  v-model="editableTranslatedVoiceText"
                  rows="2"
                  class="mt-2 block w-full resize-y rounded-2xl border border-blue-200 bg-white px-3 py-2.5 text-sm leading-6 text-slate-900 transition placeholder:text-slate-400 focus:border-blue-400 focus:outline-none dark:border-blue-900/50 dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-600"
                  placeholder="在这里检查并修改英文搜索词"
                />
                <div class="mt-3 rounded-2xl border border-[var(--tf-border)] bg-white/85 px-3 py-3 dark:bg-slate-950/70">
                  <div class="flex items-center justify-between gap-3">
                    <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">最终搜索关键词</p>
                    <span class="text-[11px] text-slate-400 dark:text-slate-500">确认后将用于 Amazon 搜索</span>
                  </div>
                  <input
                    v-model="editableNormalizedKeyword"
                    type="text"
                    class="mt-2 block w-full rounded-2xl border border-[var(--tf-border)] bg-white px-3 py-2.5 text-sm text-slate-900 transition placeholder:text-slate-400 focus:border-[var(--tf-border-strong)] focus:outline-none dark:bg-slate-900 dark:text-slate-100 dark:placeholder:text-slate-600"
                    placeholder="例如：cat water fountain"
                  />
                </div>
                <div class="mt-3 flex flex-wrap gap-2">
                  <button
                    type="button"
                    class="rounded-xl border border-[var(--tf-border)] bg-white px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800 dark:disabled:border-slate-800 dark:disabled:bg-slate-900 dark:disabled:text-slate-600"
                    :disabled="isBusy || isProcessingVoice || !hasVoicePreview"
                    @click="restoreVoicePreview"
                  >
                    恢复语音原结果
                  </button>
                  <button
                    type="button"
                    class="rounded-xl border border-[var(--tf-border)] bg-white px-3 py-2 text-xs font-semibold text-slate-700 transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400 dark:bg-slate-900 dark:text-slate-200 dark:hover:bg-slate-800 dark:disabled:border-slate-800 dark:disabled:bg-slate-900 dark:disabled:text-slate-600"
                    :disabled="isBusy || isProcessingVoice || !editableTranslatedVoiceText.trim()"
                    @click="syncTranslatedToKeyword"
                  >
                    一键同步到最终关键词
                  </button>
                  <button
                    type="button"
                    class="rounded-xl border border-blue-200 bg-white px-3 py-2 text-xs font-semibold text-blue-700 transition hover:bg-blue-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400 dark:border-blue-900/50 dark:bg-slate-900 dark:text-blue-200 dark:hover:bg-slate-800 dark:disabled:border-slate-800 dark:disabled:bg-slate-900 dark:disabled:text-slate-600"
                    :disabled="isBusy || isProcessingVoice"
                    @click="regenerateVoicePreview"
                  >
                    重新生成关键词
                  </button>
                  <button
                    type="button"
                    class="rounded-xl bg-blue-600 px-3 py-2 text-xs font-semibold text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:bg-slate-300 dark:disabled:bg-slate-700"
                    :disabled="isBusy || isProcessingVoice"
                    @click="confirmVoiceSearch"
                  >
                    确认并搜索
                  </button>
                </div>
              </div>
              <div
                v-if="recorderError"
                class="rounded-2xl border border-red-200 bg-red-50 px-3 py-3 text-[13px] leading-5 text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-200"
              >
                {{ recorderError }}
              </div>
            </div>
          </div>
        </div>

        <div class="grid gap-3 xl:grid-cols-[minmax(0,1fr)_9rem]">
          <div class="space-y-2">
            <label for="market" class="tradeflow-section-title">{{ t('task.market') }}</label>
          <AppSelect
            id="market"
            v-model="form.market"
            :options="marketOptions"
            :disabled="isBusy"
          />
        </div>

          <div class="space-y-3">
            <div class="flex items-center justify-between">
              <label for="topN" class="tradeflow-section-title">{{ t('task.topN') }}</label>
              <span class="rounded-full border border-[var(--tf-border)] bg-white/70 px-2.5 py-1 text-[12px] font-semibold text-slate-700 dark:bg-slate-950/80 dark:text-slate-200">{{ form.topN }}</span>
            </div>
            <input
              id="topN"
              v-model="form.topN"
              type="range"
              min="5"
              max="20"
              step="1"
              class="h-2 w-full cursor-pointer rounded-full bg-slate-200 accent-blue-500 dark:bg-slate-800 dark:accent-slate-500"
              :disabled="isBusy"
            />
          </div>
        </div>

        <div
          v-if="taskStore.errorMessage"
          class="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm leading-6 text-red-700 dark:border-red-900/60 dark:bg-red-950/30 dark:text-red-200"
        >
          {{ taskStore.errorMessage }}
        </div>

        <div
          v-if="productsStore.currentCandidate"
          class="tradeflow-panel rounded-[var(--tf-radius-xl)] px-4 py-3.5"
        >
          <p class="tradeflow-section-title">{{ t('products.selectedTitle') }}</p>
          <p class="mt-1.5 line-clamp-2 text-[15px] font-semibold leading-6 text-slate-900 dark:text-slate-100">{{ selectedDisplayTitle }}</p>
          <p class="mt-1.5 line-clamp-2 text-[13px] leading-5 text-slate-500 dark:text-slate-400">
            {{ t('products.selectedTaskHint', { title: selectedDisplayTitle }) }}
          </p>
          <div class="mt-3 grid grid-cols-2 gap-2.5">
            <div
              v-for="item in selectedCandidateMeta"
              :key="item.key"
              class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-2.5 dark:bg-slate-950/70"
            >
              <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">{{ item.label }}</p>
              <p class="mt-1 text-sm font-semibold text-slate-800 dark:text-slate-100">{{ item.value }}</p>
            </div>
          </div>
        </div>

        <div class="tradeflow-panel rounded-[var(--tf-radius-xl)] px-4 py-3.5">
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="tradeflow-section-title">视频 / 音频分析</p>
              <p class="mt-1 text-[13px] leading-5 text-slate-500 dark:text-slate-400">上传视频或音频，提取 transcript 并生成卖点、痛点、场景与关键词。</p>
            </div>
            <button
              type="button"
              class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-2 text-sm font-medium text-slate-700 transition hover:border-[var(--tf-border-strong)] hover:bg-white dark:bg-slate-950/70 dark:text-slate-200 dark:hover:bg-slate-900"
              :disabled="isBusy"
              @click="openMediaPicker"
            >
              {{ isAnalyzingMedia ? '分析中…' : '上传媒体' }}
            </button>
          </div>
          <input
            ref="mediaInput"
            type="file"
            accept="audio/*,video/*"
            class="hidden"
            @change="handleMediaPicked"
          />
          <p v-if="mediaError" class="mt-2 text-[12px] leading-5 text-red-600 dark:text-red-400">{{ mediaError }}</p>
          <div v-if="mediaAnalysis" class="mt-3 space-y-3">
            <div class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-3 dark:bg-slate-950/70">
              <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">Transcript</p>
              <p class="mt-1 text-sm leading-6 text-slate-800 dark:text-slate-100">{{ mediaAnalysis.transcript.text || '--' }}</p>
            </div>
            <div class="grid gap-2 md:grid-cols-2">
              <div class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-3 dark:bg-slate-950/70">
                <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">关键词</p>
                <p class="mt-1 text-sm leading-6 text-slate-800 dark:text-slate-100">{{ mediaAnalysis.intent.keywords.join(' / ') || '--' }}</p>
              </div>
              <div class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-3 dark:bg-slate-950/70">
                <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">目标人群</p>
                <p class="mt-1 text-sm leading-6 text-slate-800 dark:text-slate-100">{{ mediaAnalysis.intent.targetAudience.join(' / ') || '--' }}</p>
              </div>
              <div class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-3 dark:bg-slate-950/70">
                <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">卖点</p>
                <p class="mt-1 text-sm leading-6 text-slate-800 dark:text-slate-100">{{ mediaAnalysis.intent.sellingPoints.join(' / ') || '--' }}</p>
              </div>
              <div class="rounded-2xl border border-[var(--tf-border)] bg-white/80 px-3 py-3 dark:bg-slate-950/70">
                <p class="text-[12px] font-medium text-slate-500 dark:text-slate-400">痛点 / 场景</p>
                <p class="mt-1 text-sm leading-6 text-slate-800 dark:text-slate-100">
                  {{ [...mediaAnalysis.intent.painPoints, ...mediaAnalysis.intent.useCases].join(' / ') || '--' }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div :class="['mt-3 space-y-2.5 border-t border-[var(--tf-border)] pt-3', props.compact ? 'sticky bottom-0 bg-transparent' : '']">
        <div class="grid grid-cols-2 gap-3">
          <button
            type="button"
            class="rounded-2xl border border-[var(--tf-border)] bg-white/70 px-4 py-2.5 text-sm font-medium text-slate-600 transition hover:border-[var(--tf-border-strong)] hover:bg-white dark:bg-slate-950/70 dark:text-slate-300 dark:hover:bg-slate-900"
            :disabled="isBusy"
            @click="fillSample"
          >
            {{ t('task.sampleFill') }}
          </button>
          <button
            type="button"
            class="rounded-2xl border border-[var(--tf-border)] bg-white/70 px-4 py-3 text-sm font-medium text-slate-600 transition hover:border-[var(--tf-border-strong)] hover:bg-white dark:bg-slate-950/70 dark:text-slate-300 dark:hover:bg-slate-900"
            :disabled="isBusy"
            @click="resetForm"
          >
            {{ t('task.reset') }}
          </button>
        </div>

        <button
          type="button"
          :disabled="isBusy || !form.keyword.trim()"
          :class="[
            'flex w-full items-center justify-center gap-2 rounded-2xl bg-blue-600 px-6 py-3 text-sm font-semibold tracking-wide transition-all duration-200',
            isBusy
              ? 'cursor-not-allowed bg-slate-100 text-slate-400 dark:bg-slate-800 dark:text-slate-500'
              : 'text-white hover:bg-blue-500',
          ]"
          @click="handleSubmit"
        >
          <UIcon v-if="isBusy" name="i-heroicons-arrow-path" class="h-4 w-4 animate-spin" />
          <UIcon v-else name="i-heroicons-play" class="h-4 w-4" />
          {{ isBusy ? t('task.submitting') : t('task.submit') }}
        </button>
      </div>
    </div>
  </div>
</template>
