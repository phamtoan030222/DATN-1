<script setup lang="ts">
import { computed, getCurrentInstance, onMounted } from 'vue'
import { darkTheme } from 'naive-ui'
import type { App } from 'vue'
import { installRouter } from '@/router'
import { installPinia } from '@/store'
import { useCartStore } from '@/store/app/card'
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
const cartStore = useCartStore() // Đã sửa: dùng biến cartStore để gọi hàm bên dưới

// 3. Logic nghiệp vụ
onMounted(() => {
  chatStore.connectSocket()
})

const naiveLocale = computed(() => {
  return (naiveI18nOptions as any)[appStore.lang]
    ? (naiveI18nOptions as any)[appStore.lang]
    : naiveI18nOptions.enUS
})

// 4. Xử lý giỏ hàng (Dùng await trực tiếp vì trong script setup có Top-level await)
try {
  await cartStore.fetchCartId()
  await cartStore.fetchCartItem()
}
catch (e) {
  console.error('Lỗi khởi tạo giỏ hàng:', e)
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
      <router-view />
      <Watermark :show-watermark="appStore.showWatermark" />
      <ShiftStartModal />
    </naive-provider>
  </n-config-provider>
</template>
