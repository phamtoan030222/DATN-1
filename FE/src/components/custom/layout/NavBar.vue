<script setup lang="ts">
import {
  Cart,
  Menu as MenuIcon,
  Person,
  Search,
} from '@vicons/ionicons5'
import type { MenuOption } from 'naive-ui'
import {
  NAvatar, // <-- Đã import thêm NAvatar
  NBadge,
  NButton,
  NConfigProvider,
  NDrawer,
  NDrawerContent,
  NDropdown,
  NIcon,
  NInput,
  NMenu,
  useNotification,
} from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/store'
import { useCartStore } from '@/store/app/cart'

const themeOverrides = {
  common: {
    primaryColor: '#16a34a',
    primaryColorHover: '#15803d',
    primaryColorPressed: '#166534',
    primaryColorSuppl: '#15803d',
  },
  Input: {
    borderHover: '1px solid #16a34a',
    borderFocus: '1px solid #16a34a',
    boxShadowFocus: '0 0 0 2px rgba(22, 163, 74, 0.2)',
    caretColor: '#16a34a',
  },
  Select: {
    peers: {
      InternalSelection: {
        borderHover: '1px solid #16a34a',
        borderFocus: '1px solid #16a34a',
        boxShadowFocus: '0 0 0 2px rgba(22, 163, 74, 0.2)',
      },
    },
  },
}

const router = useRouter()
const route = useRoute()
const { userInfoDatn } = storeToRefs(useAuthStore())
const { logout } = useAuthStore()

// --- LOGIC GIỎ HÀNG ---
const { cartItems } = storeToRefs(useCartStore())
const notification = useNotification()

// TÍNH TỔNG SỐ LƯỢNG SẢN PHẨM TRONG GIỎ (Thay vì đếm số dòng)
const totalCartQuantity = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.quantity || 0), 0)
})

// --- DATA MENU (Đã xóa Liên hệ) ---
const menuOptions = computed(() => [
  { label: 'Sản phẩm', key: 'products', name: 'Products' },
  { label: 'Giới thiệu', key: 'about', name: 'About' },
  { label: 'Đơn hàng', key: 'tracking', name: userInfoDatn.value?.userId ? 'Orders' : 'OrderTracking' },
])

const userOptions = computed(() => {
  if (userInfoDatn.value) {
    return [
      { label: 'Thông tin cá nhân', key: 'profile' },
      { label: 'Đăng xuất', key: 'logout' },
    ]
  }
  else {
    return [{ label: 'Đăng nhập', key: 'login' }]
  }
})

const activeKey = ref<string | null>(null)
const showDrawer = ref(false)
const keyword = ref('')

// --- XỬ LÝ TÌM KIẾM ---
function handleSearch() {
  const searchText = keyword.value.trim()
  if (searchText) {
    router.push({ path: '/san-pham', query: { q: searchText } })
  }
  else {
    router.push({ path: '/san-pham' })
  }
}

watch(
  () => route.path,
  (currentPath) => {
    if (currentPath === '/' || currentPath === '/home')
      activeKey.value = 'home'
    else if (currentPath.startsWith('/san-pham'))
      activeKey.value = 'products'
    else if (currentPath.startsWith('/gioi-thieu'))
      activeKey.value = 'about'
    else if (currentPath.startsWith('/tra-cuu'))
      activeKey.value = 'tracking'
    else activeKey.value = null
  },
  { immediate: true },
)

function handleMenuClick(key: string, item: MenuOption) {
  if (item.name)
    router.push({ name: item.name as string })
  showDrawer.value = false
}

function handlerAccountDropdown(key: string) {
  if (key === 'login') {
    router.push({ path: '/login' })
  }
  else if (key === 'profile') {
    router.push({ name: 'Profile' })
  }
  else if (key === 'logout') {
    logout()
    router.push({ name: 'Home' })
    notification.success({ content: 'Bạn đã đăng xuất', duration: 3000 })
  }
}

function handleCartClick() {
  router.push({ name: 'Cart' })
}
</script>

<template>
  <NConfigProvider :theme-overrides="themeOverrides">
    <header class="main-header-wrapper">
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

        <div class="main-menu-inline d-none d-lg-flex">
          <NMenu
            v-model:value="activeKey" mode="horizontal" :options="menuOptions" class="modern-menu"
            @update:value="handleMenuClick"
          />
        </div>

        <div class="search-area d-none d-sm-block">
          <NInput v-model:value="keyword" placeholder="Tìm laptop..." round size="large" @keyup.enter="handleSearch">
            <template #suffix>
              <NButton circle size="medium" color="#049d14" style="margin-right: -10px;" @click="handleSearch">
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
          <NDropdown trigger="hover" :options="userOptions" @select="handlerAccountDropdown">
            <div class="action-btn">
              <NAvatar
                v-if="userInfoDatn?.avatar || userInfoDatn?.pictureUrl" round size="small"
                :src="userInfoDatn?.avatar || userInfoDatn?.pictureUrl"
                style="width: 28px; height: 28px; background-color: transparent;"
              />
              <NIcon v-else size="28" color="#333">
                <Person />
              </NIcon>

              <span class="action-label d-none d-xl-block" :title="userInfoDatn?.fullName || 'Tài khoản'">
                {{ userInfoDatn?.fullName || 'Tài khoản' }}
              </span>
            </div>
          </NDropdown>

          <div class="action-btn cart-btn" @click="handleCartClick">
            <NBadge :value="totalCartQuantity" :max="99" :show-zero="false" color="#d03050">
              <NIcon size="28" color="#333">
                <Cart />
              </NIcon>
            </NBadge>
            <span class="action-label d-none d-xl-block">Giỏ hàng</span>
          </div>
        </div>
      </div>

      <NDrawer v-model:show="showDrawer" width="280" placement="left">
        <NDrawerContent title="MENU">
          <NMenu v-model:value="activeKey" mode="vertical" :options="menuOptions" @update:value="handleMenuClick" />
        </NDrawerContent>
      </NDrawer>
    </header>
  </NConfigProvider>
</template>

<style scoped>
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
  color: #0abe1c;
  margin-left: 10px;
  letter-spacing: -0.5px;
}

.main-menu-inline {
  flex: 1;
  justify-content: flex-start;
  margin-left: 10px;
}

:deep(.modern-menu) {
  background: transparent !important;
  display: flex;
  height: 100%;
}

:deep(.modern-menu .n-menu-item) {
  display: flex;
  align-items: center;
}

:deep(.modern-menu .n-menu-item-content) {
  padding: 0 8px !important;
  background: transparent !important;
  position: relative;
  height: 100%;
  display: flex;
  align-items: center;
}

:deep(.modern-menu .n-menu-item-content .n-menu-item-content-header) {
  color: #333 !important;
  font-weight: 600 !important;
  font-size: 15px !important;
  transition: color 0.3s;
  margin: 0 8px;
}

:deep(.modern-menu .n-menu-item-content:hover .n-menu-item-content-header) {
  color: #049d14 !important;
}

:deep(.modern-menu .n-menu-item-content.n-menu-item-content--selected .n-menu-item-content-header) {
  color: #049d14 !important;
  font-weight: 700 !important;
  font-size: 14px !important;
}

:deep(.modern-menu .n-menu-item-content)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  width: 0;
  height: 3px;
  background-color: #049d14;
  transition: all 0.3s ease-out;
  transform: translateX(-50%);
}

:deep(.modern-menu .n-menu-item-content.n-menu-item-content--selected)::after {
  width: 100%;
}

.search-area {
  width: 100%;
  max-width: 280px;
  margin-left: auto;
}

.actions-area {
  display: flex;
  align-items: center;
  gap: 25px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  color: #333;
  transition: 0.2s;
  position: relative;
  max-width: 90px;
  /* Giới hạn độ rộng vùng nút để cắt chữ */
}

.action-btn:hover {
  color: #049d14;
}

.action-btn:hover :deep(.n-icon) {
  color: #049d14 !important;
}

/* ĐÃ CẬP NHẬT: CSS để không rớt dòng và thêm dấu ... */
.action-label {
  font-size: 0.75rem;
  font-weight: 600;
  margin-top: 2px;
  width: 100%;
  text-align: center;
  white-space: nowrap;
  /* Không cho phép rớt dòng */
  overflow: hidden;
  /* Cắt phần chữ thừa */
  text-overflow: ellipsis;
  /* Hiển thị dấu ... */
}

.mobile-toggle {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  display: flex;
  align-items: center;
}
</style>
