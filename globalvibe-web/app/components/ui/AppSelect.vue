<script setup lang="ts">
interface SelectOption {
  label: string
  value: string
}

const props = withDefaults(defineProps<{
  id?: string
  modelValue: string
  options: Array<string | SelectOption>
  placeholder?: string
  disabled?: boolean
}>(), {
  id: undefined,
  placeholder: '',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const isOpen = ref(false)
const rootRef = ref<HTMLElement | null>(null)

const normalizedOptions = computed<SelectOption[]>(() =>
  props.options.map(option => typeof option === 'string'
    ? { label: option, value: option }
    : option),
)

const selectedOption = computed(() =>
  normalizedOptions.value.find(option => option.value === props.modelValue),
)

const buttonLabel = computed(() => selectedOption.value?.label ?? props.modelValue ?? props.placeholder)

const toggleOpen = () => {
  if (props.disabled) return
  isOpen.value = !isOpen.value
}

const handleSelect = (value: string) => {
  emit('update:modelValue', value)
  isOpen.value = false
}

const handleFocusOut = (event: FocusEvent) => {
  const nextTarget = event.relatedTarget as Node | null
  if (rootRef.value && nextTarget && rootRef.value.contains(nextTarget)) return
  isOpen.value = false
}
</script>

<template>
  <div ref="rootRef" class="relative" @focusout="handleFocusOut">
    <button
      :id="id"
      type="button"
      :disabled="disabled"
      :aria-expanded="isOpen"
      aria-haspopup="listbox"
      :class="[
        'flex w-full items-center justify-between gap-3 rounded-xl border px-4 py-3 text-left text-sm shadow-sm transition-all',
        disabled
          ? 'cursor-not-allowed border-slate-200 bg-slate-100 text-slate-400 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-500'
          : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50 dark:border-slate-800 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-slate-700 dark:hover:bg-slate-950',
      ]"
      @click="toggleOpen"
    >
      <span class="truncate">{{ buttonLabel }}</span>
      <UIcon
        name="i-heroicons-chevron-down"
        :class="[
          'h-4 w-4 shrink-0 text-slate-400 transition-transform dark:text-slate-500',
          isOpen ? 'rotate-180' : '',
        ]"
      />
    </button>

    <div
      v-if="isOpen"
      class="absolute left-0 right-0 top-[calc(100%+0.5rem)] z-30 overflow-hidden rounded-2xl border border-slate-200 bg-white p-1.5 shadow-lg shadow-slate-200/70 dark:border-slate-800 dark:bg-slate-950 dark:shadow-black/20"
    >
      <button
        v-for="option in normalizedOptions"
        :key="option.value"
        type="button"
        class="flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm transition-colors"
        :class="option.value === modelValue
          ? 'bg-slate-100 font-medium text-slate-800 dark:bg-slate-900 dark:text-slate-100'
          : 'text-slate-600 hover:bg-slate-50 hover:text-slate-800 dark:text-slate-300 dark:hover:bg-slate-900 dark:hover:text-slate-100'"
        @mousedown.prevent
        @click="handleSelect(option.value)"
      >
        <span class="truncate">{{ option.label }}</span>
        <UIcon
          v-if="option.value === modelValue"
          name="i-heroicons-check"
          class="h-4 w-4 shrink-0"
        />
      </button>
    </div>
  </div>
</template>
