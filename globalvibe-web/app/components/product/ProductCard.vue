<script setup lang="ts">
import type { CandidateSummary } from '~/types'
import { getReadableOriginalTitle, getReadableProductName } from '~/utils/presentation'

const { t } = useAppI18n()

const props = defineProps<{
  product: CandidateSummary
  isSelected?: boolean
  isAnalyzing?: boolean
  isDisabled?: boolean
}>()

const emit = defineEmits<{
  select: [productId: string]
}>()

const handleClick = () => {
  if (props.isDisabled) return
  emit('select', props.product.productId)
}

const handleKeyDown = (event: KeyboardEvent) => {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    handleClick()
  }
}

const displayTitle = computed(() => getReadableProductName(props.product.title))
const originalTitle = computed(() => getReadableOriginalTitle(props.product.title, displayTitle.value))

const targetSiteUrl = computed(() => {
  if (!props.product.productId || !/amazon/i.test(props.product.market)) return null
  return `https://www.amazon.com/dp/${props.product.productId}`
})

const handleOpenTargetSite = () => {
  if (!targetSiteUrl.value || !import.meta.client) return
  window.open(targetSiteUrl.value, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div
    role="button"
    :aria-label="t('products.selectAriaLabel', { title: displayTitle })"
    :tabindex="isDisabled ? -1 : 0"
    :class="[
      'group relative flex h-full cursor-pointer flex-col overflow-hidden rounded-[var(--tf-radius-xl)] border p-3 transition-all duration-200',
      'border-[var(--tf-border)] bg-white hover:-translate-y-0.5 hover:border-[var(--tf-border-strong)] hover:bg-slate-50 dark:bg-slate-900 dark:hover:bg-slate-800/80',
      isSelected ? 'border-blue-400 shadow-lg shadow-blue-500/10 ring-1 ring-blue-400/30' : '',
      isDisabled ? 'cursor-not-allowed opacity-70' : '',
    ]"
    @click="handleClick"
    @keydown="handleKeyDown"
  >
    <div class="relative mb-2.5 aspect-[16/10] overflow-hidden rounded-xl bg-slate-100 dark:bg-slate-800">
      <img
        v-if="product.imageUrl"
        :src="product.imageUrl"
        :alt="product.title"
        class="h-full w-full object-cover transition-transform duration-200 group-hover:scale-105"
      />
      <div v-else class="flex h-full items-center justify-center text-slate-400 dark:text-slate-600">
        <UIcon name="i-heroicons-photo" class="h-12 w-12" />
      </div>

      <div v-if="isSelected" class="absolute right-2 top-2 rounded-full bg-blue-500 px-2.5 py-1 text-[10px] font-semibold text-white">
        {{ isAnalyzing ? t('products.analyzing') : t('products.selected') }}
      </div>
    </div>

    <div class="flex min-h-0 flex-1 flex-col">
      <h3
        :title="product.title"
        class="min-h-[2.35rem] line-clamp-2 text-[15px] font-semibold leading-[1.2] text-slate-800 dark:text-slate-100"
      >
        {{ displayTitle }}
      </h3>
      <p v-if="originalTitle" class="mt-1 line-clamp-1 text-[12px] leading-5 text-slate-500 dark:text-slate-400">
        {{ originalTitle }}
      </p>

      <button
        type="button"
        class="mt-2 rounded-xl bg-slate-50 px-3 py-2 text-left text-[12px] text-slate-500 transition hover:bg-slate-100 dark:bg-slate-800/60 dark:text-slate-400 dark:hover:bg-slate-800"
        @click.stop="handleOpenTargetSite"
      >
        <p>{{ t('products.market') }}</p>
        <div class="mt-1 flex items-center justify-between gap-2">
          <p :title="product.market" class="truncate font-medium text-slate-700 dark:text-slate-300">{{ product.market }}</p>
          <UIcon v-if="targetSiteUrl" name="i-heroicons-arrow-top-right-on-square" class="h-3.5 w-3.5 shrink-0 text-slate-400 dark:text-slate-500" />
        </div>
      </button>

      <div class="mt-2.5 flex flex-wrap gap-2">
        <span
          :title="`${t('products.price')} $${product.overseasPrice ?? '--'}`"
          class="max-w-full truncate rounded-full bg-blue-100 px-3 py-1.5 text-xs font-semibold text-blue-700 dark:bg-slate-800 dark:text-slate-300"
        >
          {{ t('products.price') }} ${{ product.overseasPrice ?? '--' }}
        </span>
      </div>

      <button
        type="button"
        :disabled="isDisabled"
        class="mt-2.5 flex w-full items-center justify-center gap-2 rounded-xl border border-slate-200 px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
        @click.stop="handleClick"
      >
        <UIcon v-if="isAnalyzing" name="i-heroicons-arrow-path" class="h-4 w-4 animate-spin" />
        <UIcon v-else name="i-heroicons-beaker" class="h-4 w-4" />
        {{ isAnalyzing ? t('products.analyzing') : t('products.analyze') }}
      </button>
    </div>
  </div>
</template>
