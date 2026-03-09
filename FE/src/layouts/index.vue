<script setup lang="ts">
import { useAppStore, useRouteStore } from '@/store'
import {
  BackTop,
  Breadcrumb,
  CollapaseButton,
  FullScreen,
  Logo,
  MobileDrawer,
  Notices,
  Setting,
  SettingDrawer,
  TabBar,
  UserCenter,
} from './components'
import { NBadge } from "naive-ui";
import { computed, h, ref, watch } from "vue";
import Content from './Content.vue'
import { ProLayout, useLayoutMenu } from 'pro-naive-ui'
import { localStorageAction } from '@/utils'
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import { useChatStore } from '@/store/chatStore'
import { storeToRefs } from 'pinia'
import { useRoute } from 'vue-router'

const route = useRoute()
const appStore = useAppStore()
const routeStore = useRouteStore()
const chatStore = useChatStore()

const { layoutMode } = storeToRefs(useAppStore())

// TRUYỀN MENU VÀO LAYOUT (Nguyên bản)
const { layout, activeKey } = useLayoutMenu({
  mode: layoutMode,
  accordion: true,
  menus: routeStore.menus,
})

function renderMenuLabel(option: any) {
  // 1. Lấy chữ gốc
  let originalLabel = option.label;
  if (typeof originalLabel === 'function') {
    originalLabel = originalLabel();
  } else if (!originalLabel) {
    originalLabel = option.title || option.name;
  }

  // 2. Định danh Menu
  const menuId = option.key || option.name;
  const targetKeys = ['support', 'support_chat', '/support', '/support/chat'];

  // 3. Nếu đúng là Menu Chat thì vẽ thêm Badge
  if (targetKeys.includes(menuId)) {
    return h(
      'div',
      { style: 'display: flex; align-items: center; justify-content: space-between; width: 100%; padding-right: 12px;' },
      [
        h('span', [originalLabel]), 
        // Chỉ vẽ chấm đỏ khi có khách chưa đọc/đang chờ (>0)
        chatStore.totalUnread > 0 
          ? h(NBadge, { value: chatStore.totalUnread, type: 'error' }) 
          : null
      ]
    );
  }

  return originalLabel;
}

const userInfo = localStorageAction.get(USER_INFO_STORAGE_KEY)

watch(
  () => route.path,
  () => {
    activeKey.value = routeStore.activeMenu
  },
  { immediate: true },
)

const showMobileDrawer = ref(false)
const sidebarWidth = ref(240)
const sidebarCollapsedWidth = ref(64)

const hasHorizontalMenu = computed(() =>
  ['horizontal', 'mixed-two-column', 'mixed-sidebar'].includes(layoutMode.value),
)

const hidenCollapaseButton = computed(
  () => ['horizontal'].includes(layoutMode.value) || appStore.isMobile,
)
</script>

<template>
  <SettingDrawer />
  
  <ProLayout
    v-model:collapsed="appStore.collapsed"
    :mode="layoutMode"
    :is-mobile="appStore.isMobile"
    :show-logo="appStore.showLogo && !appStore.isMobile"
    :show-footer="appStore.showFooter"
    :show-tabbar="false"
    nav-fixed
    show-nav
    show-sidebar
    :nav-height="60"
    :tabbar-height="45"
    :footer-height="40"
    :sidebar-width="sidebarWidth"
    :sidebar-collapsed-width="sidebarCollapsedWidth"
  >
    <template #logo>
      <Logo />
    </template>

    <template #nav-left>
      <template v-if="appStore.isMobile">
        <Logo />
      </template>

      <template v-else>
        <div
          v-if="!hasHorizontalMenu || !hidenCollapaseButton"
          class="h-full flex-y-center gap-1 p-x-sm"
        >
          <CollapaseButton v-if="!hidenCollapaseButton" />
          <Breadcrumb v-if="!hasHorizontalMenu" />
        </div>
      </template>
    </template>

    <template #nav-center>
      <div class="h-full flex-y-center gap-1">
        <n-menu v-if="hasHorizontalMenu" v-bind="layout.horizontalMenuProps" />
      </div>
    </template>

    <template #nav-right>
      <div class="h-full flex-y-center gap-1 p-x-xl">
        <template v-if="appStore.isMobile">
          <n-button quaternary @click="showMobileDrawer = true">
            <template #icon>
              <n-icon size="18">
                <icon-park-outline-hamburger-button />
              </n-icon>
            </template>
          </n-button>
        </template>

        <template v-else>
          <span class="font-500">
            {{ userInfo?.fullName }}
          </span>
          <Notices />
          <Setting />
          <UserCenter />
        </template>
      </div>
    </template>

    <template #sidebar>
      <n-menu
        v-bind="layout.verticalMenuProps"
        :collapsed-width="sidebarCollapsedWidth"
        :render-label="renderMenuLabel" 
        :data-force-update="chatStore.totalUnread"
      />
    </template> 

    <template #sidebar-extra>
      <n-scrollbar class="flex-[1_0_0]">
        <n-menu
          v-bind="layout.verticalExtraMenuProps"
          :collapsed-width="sidebarCollapsedWidth"
          :render-label="renderMenuLabel"
          :data-force-update="chatStore.totalUnread"
        />
      </n-scrollbar>
    </template>

    <template #tabbar>
      <TabBar />
    </template>

    <template #footer>
      <div class="flex-center h-full">
        {{ appStore.footerText }}
      </div>
    </template>
    <Content />
    <BackTop />
    <SettingDrawer />

    <MobileDrawer v-model:show="showMobileDrawer">
      <n-menu 
        v-bind="layout.verticalMenuProps" 
        :render-label="renderMenuLabel" 
        :data-force-update="chatStore.totalUnread"
      />
    </MobileDrawer>
  </ProLayout>
</template>   