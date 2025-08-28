<script setup lang="ts">
import AppMain from "./AppMain.vue";
import AppLoading from "./components/common/AppLoading.vue";
</script>

<template>
  <Suspense>
    <AppMain />

    <template #fallback>
      <AppLoading />
    </template>
  </Suspense>
</template>
