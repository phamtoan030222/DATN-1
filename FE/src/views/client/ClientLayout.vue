<script setup lang="ts">
import { NConfigProvider, NGlobalStyle } from 'naive-ui'
import type { GlobalThemeOverrides } from 'naive-ui'
import NavBar from '@/components/custom/layout/NavBar.vue'
import Footer from '@/components/custom/layout/BottomFooter.vue'

const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#049d14',
    primaryColorHover: '#037a10',
    fontFamily: 'Inter, sans-serif',
  },
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
    <NGlobalStyle />

    <div class="app-wrapper">
      <NavBar />

      <main class="main-content">
        <div class="container glass-box">
          <RouterView />
        </div>
      </main>

      <Footer />
    </div>
  </NConfigProvider>
</template>

<style scoped>
/* 1. NỀN TỔNG THỂ */
.app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  /* Ảnh nền full màn hình */
  background-image: url('/src/assets/images/bg5.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  /* Giữ ảnh đứng yên khi cuộn */
}

/* 2. CĂN CHỈNH BỐ CỤC CHÍNH */
.main-content {
  flex: 1;
  /* Tạo khoảng cách trên dưới để cái hộp không dính sát vào menu/footer */
  padding: 20px;
  /* KHÔNG đặt background ở đây nữa */
  backdrop-filter: blur(10px);
}

/* 3. TẠO KHỐI HỘP NỘI DUNG (GLASSMORPHISM) */
.container {
  max-width: 1300px;
  margin: 0 auto;
  width: 100%;
}

.glass-box {
  /* Màu nền trắng đục (90%) để dễ đọc chữ nhưng vẫn thấy nền mờ */
  background-color: rgba(13, 216, 98, 0.193);

  /* Bo góc mạnh tạo cảm giác hiện đại */
  border-radius: 24px;

  /* Đổ bóng giúp hộp nổi lên khỏi nền */
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.15);

  /* Hiệu ứng kính mờ (nếu trình duyệt hỗ trợ) */
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);

  /* Viền mỏng để tách biệt rõ ràng hơn */
  border: 1px solid rgba(255, 255, 255, 0.5);

  /* Padding bên trong để nội dung không dính sát mép hộp */
  padding: 10px 25px;

  /* Đảm bảo hộp có chiều cao tối thiểu */
  min-height: 600px;
}

/* Responsive: Trên màn hình nhỏ (điện thoại) thì giảm padding và bo góc */
@media (max-width: 768px) {
  .glass-box {
    padding: 15px;
    border-radius: 12px;
  }
}
</style>
