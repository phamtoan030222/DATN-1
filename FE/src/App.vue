<script setup lang="ts">
import AppMain from "./AppMain.vue";
import AppLoading from "./components/common/AppLoading.vue";
import { getCPUs } from "./service/api/admin/product/cpu.api";
</script>

<template>
  <Suspense>
    <AppMain />

    <template #fallback>
      <AppLoading />
    </template>
  </Suspense>
</template>
