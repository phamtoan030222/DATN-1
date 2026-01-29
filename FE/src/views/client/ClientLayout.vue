<script setup lang="ts">
import { NConfigProvider } from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'
import NavBar from '@/components/custom/layout/NavBar.vue'
import Footer from '@/components/custom/layout/BottomFooter.vue'

// --- CẤU HÌNH THEME (ÁP DỤNG TOÀN APP) ---
const themeOverrides: GlobalThemeOverrides = {
  common: { primaryColor: '#049d14', primaryColorHover: '#037a10' },
  Menu: {
    itemTextColorHorizontal: '#ffffff',
    itemTextColorActiveHorizontal: '#000000',
    itemColorActiveHorizontal: '#ffffff',
    itemTextColorHoverHorizontal: '#000000',
    itemColorHoverHorizontal: '#ffffff',
    borderRadius: '4px',
    fontSize: '17px',
  },
}
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <div class="app-wrapper">
      <NavBar />

      <main class="main-content">
        <div class="container">
          <RouterView />
        </div>
      </main>

      <Footer />
    </div>
  </NConfigProvider>
</template>

<style scoped>
/* Layout Flexbox giúp Footer luôn nằm dưới đáy */
.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1; /* Chiếm toàn bộ khoảng trống còn lại */
  padding: 24px 0;
  background-color: #f9f9f9;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
  width: 100%;
}
</style>
