<script setup lang="ts">
import DefaultLayout from '~/layouts/default.vue'
</script>

<template>
  <UApp>
    <NuxtRouteAnnouncer />
    <DefaultLayout>
      <NuxtPage />
    </DefaultLayout>
  </UApp>
</template>
