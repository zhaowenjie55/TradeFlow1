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
const triggerRef = ref<HTMLButtonElement | null>(null)
const optionRefs = ref<Array<HTMLButtonElement | null>>([])
const activeIndex = ref(0)
const openDirection = ref<'down' | 'up'>('down')

const normalizedOptions = computed<SelectOption[]>(() =>
  props.options.map(option => typeof option === 'string'
    ? { label: option, value: option }
    : option),
)

const selectedOption = computed(() =>
  normalizedOptions.value.find(option => option.value === props.modelValue),
)

const buttonLabel = computed(() => selectedOption.value?.label ?? props.modelValue ?? props.placeholder)

const updateDirection = () => {
  if (!triggerRef.value) return

  const rect = triggerRef.value.getBoundingClientRect()
  const viewportHeight = window.innerHeight
  const estimatedMenuHeight = Math.min(320, normalizedOptions.value.length * 44 + 16)
  openDirection.value = viewportHeight - rect.bottom < estimatedMenuHeight && rect.top > estimatedMenuHeight
    ? 'up'
    : 'down'
}

const focusActiveOption = () => {
  nextTick(() => {
    optionRefs.value[activeIndex.value]?.focus()
  })
}

const openMenu = () => {
  if (props.disabled) return
  activeIndex.value = Math.max(0, normalizedOptions.value.findIndex(option => option.value === props.modelValue))
  updateDirection()
  isOpen.value = true
  focusActiveOption()
}

const closeMenu = () => {
  isOpen.value = false
}

const toggleOpen = () => {
  if (props.disabled) return
  if (isOpen.value) {
    closeMenu()
    return
  }
  openMenu()
}

const handleSelect = (value: string) => {
  emit('update:modelValue', value)
  closeMenu()
  nextTick(() => triggerRef.value?.focus())
}

const handleFocusOut = (event: FocusEvent) => {
  const nextTarget = event.relatedTarget as Node | null
  if (rootRef.value && nextTarget && rootRef.value.contains(nextTarget)) return
  closeMenu()
}

const handleTriggerKeydown = (event: KeyboardEvent) => {
  if (event.key === 'ArrowDown' || event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    openMenu()
  }
}

const handleOptionKeydown = (event: KeyboardEvent, index: number) => {
  if (event.key === 'Escape') {
    event.preventDefault()
    closeMenu()
    nextTick(() => triggerRef.value?.focus())
    return
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = (index + 1) % normalizedOptions.value.length
    focusActiveOption()
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = (index - 1 + normalizedOptions.value.length) % normalizedOptions.value.length
    focusActiveOption()
  }

  if (event.key === 'Home') {
    event.preventDefault()
    activeIndex.value = 0
    focusActiveOption()
  }

  if (event.key === 'End') {
    event.preventDefault()
    activeIndex.value = normalizedOptions.value.length - 1
    focusActiveOption()
  }

  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    handleSelect(normalizedOptions.value[index].value)
  }
}
</script>

<template>
  <div ref="rootRef" class="relative" @focusout="handleFocusOut">
    <button
      ref="triggerRef"
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
      @keydown="handleTriggerKeydown"
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
      class="absolute left-0 right-0 z-30 overflow-hidden rounded-2xl border border-[var(--tf-border)] bg-[var(--tf-bg-panel)] p-1.5 shadow-xl"
      :class="openDirection === 'down' ? 'top-[calc(100%+0.5rem)]' : 'bottom-[calc(100%+0.5rem)]'"
    >
      <button
        v-for="(option, index) in normalizedOptions"
        :key="option.value"
        :ref="element => { optionRefs[index] = element as HTMLButtonElement | null }"
        type="button"
        role="option"
        class="flex w-full items-center justify-between rounded-xl px-3 py-2.5 text-sm transition-colors"
        :class="option.value === modelValue
          ? 'bg-[var(--tf-accent-soft)] font-medium text-[var(--tf-text)]'
          : 'text-slate-600 hover:bg-white/70 hover:text-slate-800 dark:text-slate-300 dark:hover:bg-slate-900 dark:hover:text-slate-100'"
        @mousedown.prevent
        @keydown="handleOptionKeydown($event, index)"
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
