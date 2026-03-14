<script setup lang="ts">
import { NConfigProvider, NGlobalStyle } from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'
import NavBar from '@/components/custom/layout/NavBar.vue'
import Footer from '@/components/custom/layout/BottomFooter.vue'
import ChatWidget from '../../views/client/user/chat/index.vue'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#d70018',
    primaryColorHover: '#c40016',
    fontFamily: 'Inter, system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif',
  },
  Menu: {
    itemTextColorHorizontal: '#ffffff',
    itemTextColorActiveHorizontal: '#000000',
    itemColorActiveHorizontal: '#ffffff',
    itemTextColorHoverHorizontal: '#000000',
    itemColorHoverHorizontal: '#ffffff',
    borderRadius: '6px',
    fontSize: '16px',
  },
}
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <NGlobalStyle />

    <div class="app-wrapper">
      <NavBar />

      <main class="main-content">
        <div class="container content-card">
          <RouterView />
        </div>
      </main>

      <Footer />
      <ChatWidget />
    </div>
  </NConfigProvider>
</template>

<style scoped>
/* Nền sạch, kiểu CellphoneS/Apple: trắng + xám rất nhạt */
.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 18px 14px 40px;
}

.container {
  max-width: 1300px;
  margin: 0 auto;
  width: 100%;
}

.content-card {
  border-radius: 16px;
  min-height: 600px;
}

@media (max-width: 768px) {
  .content-card {
    padding: 12px;
    border-radius: 12px;
  }
}
</style>
