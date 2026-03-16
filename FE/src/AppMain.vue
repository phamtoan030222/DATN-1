<script setup lang="ts">
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { darkTheme } from 'naive-ui'
import type { App } from 'vue'
import { installRouter } from '@/router'
import { installPinia } from '@/store'
import { useCartStore } from '@/store/app/cart'
import { useAppStore } from './store'
import { useChatStore } from '@/store/chatStore'
import { naiveI18nOptions } from '@/utils'
import ShiftStartModal from '@/views/admin/shiftmanager/ShiftStartModal.vue'

// 1. Quy trình khởi tạo lõi (Giữ nguyên logic của bạn)
const initializationPromise = (async () => {
  const app = getCurrentInstance()?.appContext.app
  if (!app)
    return false

  await installPinia(app)
  await installRouter(app)

  const modules = import.meta.glob<{ install: (app: App) => void }>(
    './modules/*.ts',
    { eager: true },
  )
  Object.values(modules).forEach(module => app.use(module))
  return true
})()

// 2. Chờ khởi tạo xong xuôi mới chạy logic Store
await initializationPromise

const appStore = useAppStore()
const chatStore = useChatStore()
const cartStore = useCartStore()

// FIX: dùng ref để đánh dấu app đã sẵn sàng, tránh Suspense re-suspend khi navigate
const isReady = ref(false)

// 3. Logic nghiệp vụ
onMounted(() => {
  chatStore.connectSocket()
})

const naiveLocale = computed(() => {
  return (naiveI18nOptions as any)[appStore.lang]
    ? (naiveI18nOptions as any)[appStore.lang]
    : naiveI18nOptions.enUS
})

// 4. Xử lý giỏ hàng
try {
  await cartStore.fetchCartId()
  await cartStore.fetchCartItem()
}
catch (e) {
  console.error('Lỗi khởi tạo giỏ hàng:', e)
}
finally {
  // Đánh dấu sẵn sàng SAU KHI tất cả await hoàn thành
  isReady.value = true
}
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
      <!--
        FIX: v-if="isReady" đảm bảo router-view chỉ mount SAU KHI
        tất cả top-level await hoàn thành. Điều này ngăn Suspense
        re-suspend và unmount router-view khi navigate giữa các trang.
      -->
      <router-view v-if="isReady" />
      <Watermark :show-watermark="appStore.showWatermark" />
      <ShiftStartModal />
    </naive-provider>
  </n-config-provider>
</template>
