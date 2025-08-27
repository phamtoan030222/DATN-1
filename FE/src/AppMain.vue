<script setup lang="ts">
import type { App } from "vue";
import { installRouter } from "@/router";
import { installPinia } from "@/store";
import { naiveI18nOptions } from "@/utils";
import { darkTheme } from "naive-ui";
import { useAppStore } from "./store";

const initializationPromise = (async () => {
  const app = getCurrentInstance()?.appContext.app;
  if (!app) {
    throw new Error("Failed to get app instance");
  }

  await installPinia(app);

  await installRouter(app);

  const modules = import.meta.glob<{ install: (app: App) => void }>(
    "./modules/*.ts",
    {
      eager: true,
    }
  );

  Object.values(modules).forEach((module) => app.use(module));

  return true;
})();

await initializationPromise;

const appStore = useAppStore();

const naiveLocale = computed(() => {
  return naiveI18nOptions[appStore.lang]
    ? naiveI18nOptions[appStore.lang]
    : naiveI18nOptions.enUS;
});
</script>

<template>
  <n-config-provider
    class="wh-full"
    inline-theme-disabled
    :theme="appStore.colorMode === 'dark' ? darkTheme : null"
    :locale="naiveLocale.locale"
    :date-locale="naiveLocale.dateLocale"
    :theme-overrides="appStore.theme"
  >
    <naive-provider>
      <router-view />
      <Watermark :show-watermark="appStore.showWatermark" />
    </naive-provider>
  </n-config-provider>
</template>
