// https://nuxt.com/docs/api/configuration/nuxt-config
import { env } from 'node:process'

export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },
  pages: false,

  runtimeConfig: {
    public: {
      backendBaseUrl: env.NUXT_PUBLIC_BACKEND_BASE_URL ?? 'http://localhost:8081',
      reactFrontendUrl: env.NUXT_PUBLIC_REACT_FRONTEND_URL ?? 'http://localhost:3000',
    },
  },

  modules: ['@nuxt/ui'],

  css: ['~/assets/css/main.css'],

  typescript: {
    strict: true,
  },
})
