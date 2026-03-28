<script setup lang="ts">
import type { CandidateSummary } from '~/types'

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
</script>

<template>
  <div
    role="button"
    :aria-label="t('products.selectAriaLabel', { title: product.title })"
    :tabindex="isDisabled ? -1 : 0"
    :class="[
      'group relative flex h-full min-h-[29rem] cursor-pointer flex-col overflow-hidden rounded-2xl bg-white p-4 transition-all duration-200',
      'hover:bg-slate-50 hover:-translate-y-0.5 dark:bg-slate-900 dark:hover:bg-slate-800/80',
      isSelected ? 'ring-2 ring-blue-500 shadow-lg shadow-blue-500/10' : '',
      isDisabled ? 'cursor-not-allowed opacity-70' : '',
    ]"
    @click="handleClick"
    @keydown="handleKeyDown"
  >
    <div class="relative mb-4 aspect-square overflow-hidden rounded-xl bg-slate-100 dark:bg-slate-800">
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
        class="min-h-[2.75rem] line-clamp-2 text-sm font-medium leading-relaxed text-slate-700 dark:text-slate-300"
      >
        {{ product.title }}
      </h3>

      <div class="mt-3 rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-500 dark:bg-slate-800/60 dark:text-slate-400">
        <p>{{ t('products.market') }}</p>
        <p :title="product.market" class="mt-1 truncate font-medium text-slate-700 dark:text-slate-300">{{ product.market }}</p>
      </div>

      <div class="mt-4 flex flex-wrap gap-2.5">
        <span
          :title="`${t('products.price')} $${product.overseasPrice ?? '--'}`"
          class="max-w-full truncate rounded-full bg-blue-100 px-3 py-1.5 text-xs font-semibold text-blue-700 dark:bg-slate-800 dark:text-slate-300"
        >
          {{ t('products.price') }} ${{ product.overseasPrice ?? '--' }}
        </span>
        <span
          v-if="product.riskTag"
          class="max-w-full truncate rounded-full bg-slate-100 px-3 py-1.5 text-xs font-semibold text-slate-700 dark:bg-slate-800 dark:text-slate-300"
        >
          {{ product.riskTag }}
        </span>
      </div>

      <div class="mt-3 space-y-2 text-xs text-slate-500 dark:text-slate-400">
        <div class="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-2.5 dark:bg-slate-800/60">
          <p class="truncate">{{ t('products.margin') }}</p>
          <p class="shrink-0 font-semibold text-emerald-600 dark:text-emerald-400">
            {{ product.estimatedMargin !== null ? `+${product.estimatedMargin}%` : '--' }}
          </p>
        </div>
        <div class="flex items-center justify-between gap-3 rounded-xl bg-slate-50 px-3 py-2.5 dark:bg-slate-800/60">
          <p class="truncate">{{ t('products.secondPhase') }}</p>
          <p class="shrink-0 font-semibold text-slate-700 dark:text-slate-300">
            {{ product.suggestSecondPhase ? t('common.yes') : t('common.no') }}
          </p>
        </div>
      </div>

      <div class="mt-4 rounded-xl bg-slate-50 px-3 py-3 text-xs leading-relaxed text-slate-500 dark:bg-slate-800/60 dark:text-slate-400">
        <p class="font-medium text-slate-700 dark:text-slate-300">{{ t('products.recommendationReason') }}</p>
        <p class="mt-1 line-clamp-4">{{ product.recommendationReason ?? t('common.empty') }}</p>
      </div>

      <button
        type="button"
        :disabled="isDisabled"
        class="mt-6 flex w-full items-center justify-center gap-2 rounded-xl border border-slate-200 px-4 py-3.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50 dark:border-slate-700 dark:text-slate-200 dark:hover:bg-slate-800"
        @click.stop="handleClick"
      >
        <UIcon v-if="isAnalyzing" name="i-heroicons-arrow-path" class="h-4 w-4 animate-spin" />
        <UIcon v-else name="i-heroicons-beaker" class="h-4 w-4" />
        {{ isAnalyzing ? t('products.analyzing') : t('products.analyze') }}
      </button>
    </div>
  </div>
</template>
