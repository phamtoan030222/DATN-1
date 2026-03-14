<script setup lang="ts">
import { useAppStore } from '@/store'

const router = useRouter()
const appStore = useAppStore()

const name = import.meta.env.VITE_APP_NAME

const hidenLogoText = computed(() => {
  if (['sidebar', 'mixed-sidebar', 'horizontal'].includes(appStore.layoutMode)) {
    return false
  }
  if (['two-column', 'mixed-two-column'].includes(appStore.layoutMode)) {
    return true
  }
  return appStore.collapsed
})
</script>

<template>
  <div
    class="h-60px text-xl flex-center cursor-pointer gap-2 p-x-2 brand-text"
    @click="router.push('/dashboard/sales')"
  >
    <svg-icons-logo class="text-1.5em" />
    <span
      v-show="!hidenLogoText"
      class="text-ellipsis overflow-hidden whitespace-nowrap"
    >{{ name }}</span>
  </div>
</template>

<style scoped>
.brand-text {
  font-size: 1.6rem;
  font-weight: 900;
  color: #049d14;
  margin-left: 10px;
  letter-spacing: -0.5px;
}
</style>
