export default defineNuxtPlugin(() => {
  const localeStore = useLocaleStore()
  const settingsStore = useSettingsStore()

  settingsStore.hydrate()
  localeStore.hydrateLocale()

  if (settingsStore.settings.locale !== localeStore.locale) {
    localeStore.setLocale(settingsStore.settings.locale)
  }
})
