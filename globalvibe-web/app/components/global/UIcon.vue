<script setup lang="ts">
defineOptions({
  inheritAttrs: false,
})

const props = defineProps<{
  name: string
}>()
</script>

<template>
  <Icon :name="props.name" mode="svg" v-bind="$attrs" />
</template>
