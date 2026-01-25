<script setup lang="ts">
import { ref, watch } from 'vue' // Thêm watch
import { useRoute, useRouter } from 'vue-router' // Thêm useRoute
import {
  NBadge,
  NButton,
  NDrawer,
  NDrawerContent,
  NDropdown,
  NIcon,
  NInput,
  NMenu,
} from 'naive-ui'
import type { MenuOption } from 'naive-ui'
import {
  Call,
  Cart,
  Menu as MenuIcon,
  Person,
  Search,
} from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute() 

// --- DATA ---
const menuOptions: MenuOption[] = [
  { label: 'TRANG CHỦ', key: 'home', href: '/' },
  { label: 'SẢN PHẨM', key: 'products', href: '/san-pham' },
  { label: 'GIỚI THIỆU', key: 'about', href: '/gioi-thieu' },
  { label: 'LIÊN HỆ', key: 'contact', href: '/lien-he' },
  { label: 'TRA CỨU ĐƠN HÀNG', key: 'tracking', href: '/tra-cuu' },
]

const userOptions = [{ label: 'Đăng nhập', key: 'login' }, { label: 'Đăng ký', key: 'register' }]

// Không gán cứng nữa, để null hoặc 'home' ban đầu
const activeKey = ref<string | null>(null)
const showDrawer = ref(false)
const keyword = ref('')

// --- LOGIC TỰ ĐỘNG CẬP NHẬT MENU THEO URL ---
watch(
  () => route.path,
  (currentPath) => {
    // 1. Kiểm tra Trang chủ
    if (currentPath === '/' || currentPath === '/home') {
      activeKey.value = 'home'
    }
    // 2. Kiểm tra Sản phẩm (bao gồm cả trang danh sách và chi tiết /product/...)
    else if (currentPath.startsWith('/san-pham') || currentPath.startsWith('/product')) {
      activeKey.value = 'products'
    }
    // 3. Giới thiệu
    else if (currentPath.startsWith('/gioi-thieu')) {
      activeKey.value = 'about'
    }
    // 4. Liên hệ
    else if (currentPath.startsWith('/lien-he')) {
      activeKey.value = 'contact'
    }
    // 5. Tra cứu
    else if (currentPath.startsWith('/tra-cuu')) {
      activeKey.value = 'tracking'
    }
    // Các trường hợp khác
    else {
      activeKey.value = null
    }
  },
  { immediate: true }, // Chạy ngay lập tức khi load trang
)

// --- HÀM XỬ LÝ CLICK ---
function handleMenuClick(key: string, item: MenuOption) {
  if (item.href) {
    router.push(item.href as string)
  }
  showDrawer.value = false
}
</script>

<template>
  <header class="main-header-wrapper">
    <div class="top-banner">
      <div class="banner-overlay">
        <h2 class="banner-title">
          KHUYẾN MÃI MÙA HÈ - GIẢM ĐẾN 20%
        </h2>
        <p class="banner-subtitle">
          Dành riêng cho sinh viên nhập học
        </p>
      </div>
      <img
        src="https://truonggiang.vn/wp-content/uploads/2022/09/banner-laptop-sinh-vien-2048x943-1.jpg"
        class="banner-img"
      >
    </div>

    <div class="header-inner container">
      <button class="mobile-toggle d-lg-none" @click="showDrawer = true">
        <NIcon size="32" color="#049d14">
          <MenuIcon />
        </NIcon>
      </button>

      <a href="/" class="logo-area">
        <img src="/favicon.svg" alt="Logo" class="logo-img">
        <div class="brand-info d-none d-sm-flex">
          <span class="brand-text">My Laptop</span>
        </div>
      </a>

      <div class="search-area d-none d-sm-block">
        <NInput v-model:value="keyword" placeholder="Bạn tìm laptop gì hôm nay?" round size="large">
          <template #suffix>
            <NButton circle size="medium" color="#049d14" style="margin-right: -10px;">
              <template #icon>
                <NIcon size="20" color="#fff">
                  <Search />
                </NIcon>
              </template>
            </NButton>
          </template>
        </NInput>
      </div>

      <div class="actions-area">
        <div class="hotline-group d-none d-lg-flex">
          <div class="hotline-info">
            <span class="hotline-label">TƯ VẤN MIỄN PHÍ</span>
            <span class="hotline-number">0965.237.19</span>
          </div>
          <div class="icon-wrapper">
            <NIcon size="24" color="#049d14">
              <Call />
            </NIcon>
          </div>
        </div>

        <NDropdown trigger="hover" :options="userOptions">
          <div class="action-btn">
            <NIcon size="28" color="#333">
              <Person />
            </NIcon>
            <span class="action-label d-none d-xl-block">Tài khoản</span>
          </div>
        </NDropdown>

        <div class="action-btn cart-btn">
          <NBadge :value="5" :max="99" color="#d03050">
            <NIcon size="28" color="#333">
              <Cart />
            </NIcon>
          </NBadge>
          <span class="action-label d-none d-xl-block">Giỏ hàng</span>
        </div>
      </div>
    </div>

    <div class="nav-background d-none d-lg-block">
      <div class="container nav-wrapper">
        <NMenu
          v-model:value="activeKey"
          mode="horizontal"
          :options="menuOptions"
          class="modern-menu"
          @update:value="handleMenuClick"
        />
      </div>
    </div>

    <NDrawer v-model:show="showDrawer" width="280" placement="left">
      <NDrawerContent title="MENU">
        <NMenu
          v-model:value="activeKey"
          mode="vertical"
          :options="menuOptions"
          @update:value="handleMenuClick"
        />
      </NDrawerContent>
    </NDrawer>
  </header>
</template>

<style scoped>
/* Reset & Base */
.main-header-wrapper { background: white; box-shadow: 0 2px 10px rgba(0,0,0,0.05); }
.container { max-width: 1200px; margin: 0 auto; padding: 0 15px; width: 100%; }

/* Utilities */
.d-none { display: none !important; }
.d-lg-block { display: block !important; }
.d-lg-none { display: none !important; }
.d-lg-flex { display: flex !important; }
.d-sm-flex { display: flex !important; }
.d-sm-block { display: block !important; }
.d-xl-block { display: block !important; }
@media (max-width: 1200px) { .d-xl-block { display: none !important; } }
@media (min-width: 992px) {
  .d-lg-block { display: block !important; }
  .d-lg-none { display: none !important; }
  .d-lg-flex { display: flex !important; }
  .d-none { display: none; }
}

/* Banner */
.top-banner { position: relative; height: 160px; overflow: hidden; background: #000; }
.banner-img { width: 100%; height: 100%; object-fit: cover; opacity: 0.6; }
.banner-overlay { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; z-index: 2; text-align: center; }
.banner-title { color: #fff; font-size: 1.8rem; font-weight: 800; text-transform: uppercase; letter-spacing: 1px; margin: 0; }
.banner-subtitle { color: #f0f0f0; font-size: 1rem; margin-top: 5px; }

/* Header Layout */
.header-inner { height: 90px; display: flex; align-items: center; justify-content: space-between; gap: 20px; }

/* Logo */
.logo-area { display: flex; align-items: center; text-decoration: none; }
.logo-img { height: 50px; width: 50px; }
.brand-text { font-size: 1.6rem; font-weight: 900; color: #049d14; margin-left: 10px; letter-spacing: -0.5px; }

/* Search Box */
.search-area { flex: 1; max-width: 500px; }

/* Actions Area */
.actions-area { display: flex; align-items: center; gap: 25px; }

/* Hotline Group */
.hotline-group { align-items: center; gap: 10px; margin-right: 15px; padding-right: 20px; border-right: 1px solid #eee; }
.hotline-info { display: flex; flex-direction: column; align-items: flex-end; line-height: 1.2; }
.hotline-label { font-size: 0.7rem; color: #666; font-weight: 600; }
.hotline-number { font-size: 1.1rem; color: #049d14; font-weight: 800; }
.icon-wrapper { width: 40px; height: 40px; background: #f0fdf4; border-radius: 50%; display: flex; align-items: center; justify-content: center; }

/* Action Buttons */
.action-btn { display: flex; flex-direction: column; align-items: center; cursor: pointer; color: #333; transition: 0.2s; position: relative; }
.action-btn:hover { color: #049d14; }
.action-btn:hover :deep(.n-icon) { color: #049d14  !important; }
.action-label { font-size: 0.75rem; font-weight: 600; margin-top: 2px; }

/* === MENU HIỆN ĐẠI === */
.nav-background { background-color: #049d14; height: 50px; }
.nav-wrapper { display: flex; justify-content: center; height: 100%; }

:deep(.modern-menu) {
  background: transparent !important;
  display: flex;
  justify-content: center;
  height: 100%;
}

:deep(.modern-menu .n-menu-item) {
  height: 100%;
  display: flex;
  align-items: center;
}

:deep(.modern-menu .n-menu-item-content) {
  padding: 0 30px !important;
  background: transparent !important;
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
}

:deep(.modern-menu .n-menu-item-content .n-menu-item-content-header) {
  color: rgba(227, 226, 226, 0.9) !important;
  font-weight: 600 !important;
  font-size: 15px !important;
  text-transform: uppercase;
  transition: color 0.3s;
}

:deep(.modern-menu .n-menu-item-content:hover .n-menu-item-content-header) {
  color: #ffffff !important;
}

:deep(.modern-menu .n-menu-item-content.n-menu-item-content--selected .n-menu-item-content-header) {
  color: #ffffff !important;
  font-weight: 700 !important;
  font-size: 17px !important;
}

:deep(.modern-menu .n-menu-item-content)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 4px;
  background-color: #ffcc00;
  transition: all 0.3s ease-out;
  transform: translateX(-50%);
}

:deep(.modern-menu .n-menu-item-content.n-menu-item-content--selected)::after {
  width: 100%;
}

.mobile-toggle { background: none; border: none; cursor: pointer; padding: 0; display: flex; align-items: center; }
</style>
