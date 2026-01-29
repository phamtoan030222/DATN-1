<script setup lang="ts">
import bg from '@/assets/images/banner4.jpg'
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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

// Import Store đã nâng cấp
import { CartStore } from '@/utils/cartStore'

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

const userOptions = [{ label: 'Đăng nhập', key: 'login' }]

const activeKey = ref<string | null>(null)
const showDrawer = ref(false)
const keyword = ref('')

// [QUAN TRỌNG] Gọi updateCount khi layout được load
onMounted(() => {
  CartStore.updateCount()
})

// Tự động cập nhật Menu Active
watch(
  () => route.path,
  (currentPath) => {
    if (currentPath === '/' || currentPath === '/home')
      activeKey.value = 'home'
    else if (currentPath.startsWith('/san-pham') || currentPath.startsWith('/product'))
      activeKey.value = 'products'
    else if (currentPath.startsWith('/gioi-thieu'))
      activeKey.value = 'about'
    else if (currentPath.startsWith('/lien-he'))
      activeKey.value = 'contact'
    else if (currentPath.startsWith('/tra-cuu'))
      activeKey.value = 'tracking'
    else activeKey.value = null

    // Mỗi khi chuyển trang cũng cập nhật lại số lượng cho chắc
    CartStore.updateCount()
  },
  { immediate: true },
)

function handleMenuClick(key: string, item: MenuOption) {
  if (item.href)
    router.push(item.href as string)
  showDrawer.value = false
}

function handlerAccountDropdown(key: string) {
  if (key === 'login')
    router.push({ name: 'login' })
}

function handleCartClick() {
  router.push('/cart')
}
</script>

<template>
  <header class="main-header-wrapper">
    <div class="top-banner">
      <div class="banner-overlay">
        <h2 class="banner-title">
          i'm sorry, please forgive me, thank you, i love you
        </h2>
        <!-- <p class="banner-subtitle">
          Dành riêng cho sinh viên nhập học
        </p> -->
      </div>
      <img src="/src/assets/images/banner7.jpg" class="banner-img">
    </div>

    <div class="header-inner container">
      <button class="mobile-toggle d-lg-none" @click="showDrawer = true">
        <NIcon size="32" color="#049d14">
          <MenuIcon />
        </NIcon>
      </button>

      <a href="/" class="logo-area">
        <img src="/favicon.svg" alt="Logo" class="logo-img" onerror="this.style.display='none'">
        <div class="brand-info d-none d-sm-flex"><span class="brand-text">My Laptop</span></div>
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

        <NDropdown trigger="hover" :options="userOptions" @select="handlerAccountDropdown">
          <div class="action-btn">
            <NIcon size="28" color="#333">
              <Person />
            </NIcon>
            <span class="action-label d-none d-xl-block">Tài khoản</span>
          </div>
        </NDropdown>

        <div class="action-btn cart-btn" @click="handleCartClick">
          <NBadge :value="CartStore.count.value" :max="99" color="#d03050">
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
          v-model:value="activeKey" mode="horizontal" :options="menuOptions" class="modern-menu"
          @update:value="handleMenuClick"
        />
      </div>
    </div>

    <NDrawer v-model:show="showDrawer" width="280" placement="left">
      <NDrawerContent title="MENU">
        <NMenu v-model:value="activeKey" mode="vertical" :options="menuOptions" @update:value="handleMenuClick" />
      </NDrawerContent>
    </NDrawer>
  </header>
</template>

<style scoped>
/* (Giữ nguyên style của bạn) */
.layout-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-header-wrapper {
  background: white;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 1000;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 15px;
  width: 100%;
}

.main-content {
  flex: 1;
  background-color: #f9f9f9;
  padding-bottom: 40px;
}

.d-none {
  display: none !important;
}

.d-lg-block {
  display: block !important;
}

.d-lg-none {
  display: none !important;
}

.d-lg-flex {
  display: flex !important;
}

.d-sm-flex {
  display: flex !important;
}

.d-sm-block {
  display: block !important;
}

.d-xl-block {
  display: block !important;
}

@media (max-width: 1200px) {
  .d-xl-block {
    display: none !important;
  }
}

@media (min-width: 992px) {
  .d-lg-block {
    display: block !important;
  }

  .d-lg-none {
    display: none !important;
  }

  .d-lg-flex {
    display: flex !important;
  }

  .d-none {
    display: none;
  }
}

.top-banner {
  position: relative;
  height: 60px;
  overflow: hidden;
  background: #000;
}

.banner-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.6;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  z-index: 2;
  text-align: center;
}

.banner-title {
  color: #21f15c;
  font-size: 1.8rem;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin: 0;
  text-shadow: 0 10px 5px green;
}

.banner-subtitle {
  color: #f0f0f0;
  font-size: 1rem;
  margin-top: 5px;
  text-shadow: 0 5px 5px green;
}

.header-inner {
  height: 90px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.logo-area {
  display: flex;
  align-items: center;
  text-decoration: none;
}

.logo-img {
  height: 50px;
  width: 50px;
}

.brand-text {
  font-size: 1.6rem;
  font-weight: 900;
  color: #049d14;
  margin-left: 10px;
  letter-spacing: -0.5px;
}

.search-area {
  flex: 1;
  max-width: 500px;
}

.actions-area {
  display: flex;
  align-items: center;
  gap: 25px;
}

.hotline-group {
  align-items: center;
  gap: 10px;
  margin-right: 15px;
  padding-right: 20px;
  border-right: 1px solid #eee;
}

.hotline-info {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  line-height: 1.2;
}

.hotline-label {
  font-size: 0.7rem;
  color: #666;
  font-weight: 600;
}

.hotline-number {
  font-size: 1.1rem;
  color: #049d14;
  font-weight: 800;
}

.icon-wrapper {
  width: 40px;
  height: 40px;
  background: #f0fdf4;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  color: #333;
  transition: 0.2s;
  position: relative;
}

.action-btn:hover {
  color: #049d14;
}

.action-btn:hover :deep(.n-icon) {
  color: #049d14 !important;
}

.action-label {
  font-size: 0.75rem;
  font-weight: 600;
  margin-top: 2px;
}

.nav-background {
  background-color: #049d14;
  height: 50px;
}

.nav-wrapper {
  display: flex;
  justify-content: center;
  height: 100%;
}

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

.mobile-toggle {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
}

.main-footer {
  background-color: #333;
  color: white;
  padding: 20px 0;
  text-align: center;
  margin-top: auto;
}
</style>
