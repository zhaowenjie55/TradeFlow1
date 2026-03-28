// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: true },

  runtimeConfig: {
    public: {
      backendBaseUrl: process.env.NUXT_PUBLIC_BACKEND_BASE_URL ?? 'http://localhost:8080',
    },
  },

  // 禁用 Google 字体源，避免开发启动时对外网重试等待
  fonts: {
    providers: {
      google: false,
      googleicons: false,
    },
  },

  modules: [
    '@nuxt/ui',
    '@vueuse/nuxt',
    '@pinia/nuxt',
  ],

  colorMode: {
    classSuffix: '',
    preference: 'dark',
    fallback: 'dark',
    storageKey: 'tradeflow-theme',
  },

  css: ['~/assets/css/main.css'],

  tailwindcss: {
    configPath: '~/tailwind.config.js',
  },

  typescript: {
    strict: true,
  },
})
