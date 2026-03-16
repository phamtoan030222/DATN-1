import { router } from '@/router'
import { local } from '@/utils'
import { useRouteStore } from './router'
import { useTabStore } from './tab'
import { localStorageAction } from '@/utils/storage.helper'
import { ACCESS_TOKEN_STORAGE_KEY, CUSTOMER_CART_ID, CUSTOMER_CART_ITEM, REFRESH_TOKEN_STORAGE_KEY, USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import { defineStore } from 'pinia'
import { unref } from 'vue'
import { useCartStore } from '@/store/app/cart'

interface AuthStatus {
  userInfo: Api.Login.Info | null
  token: string
  userInfoDatn: Entity.UserInformation | null
}

export const useAuthStore = defineStore('auth-store', {
  state: (): AuthStatus => {
    return {
      userInfo: local.get('userInfo'),
      token: local.get('accessToken') || '',
      userInfoDatn: localStorageAction.get(USER_INFO_STORAGE_KEY) || null,
    }
  },
  getters: {
    isLogin(state) {
      return Boolean(state.token)
    },
  },
  actions: {
    async logout() {
      const route = unref(router.currentRoute)
      this.clearAuthStorage()

      const routeStore = useRouteStore()
      routeStore.resetRouteStore()

      const tabStore = useTabStore()
      tabStore.clearAllTabs()

      this.$reset()

      // Dọn dẹp giỏ hàng cũ và fetch lại thành giỏ trống (local)
      const cartStore = useCartStore()
      cartStore.clearCartState()
      await cartStore.fetchCartItem()

      if (route.meta.requiresAuth) {
        router.push({
          name: 'login',
        })
      }
    },

    clearAuthStorage() {
      local.remove('accessToken')
      local.remove('refreshToken')
      local.remove('userInfo')

      localStorageAction.remove(ACCESS_TOKEN_STORAGE_KEY)
      localStorageAction.remove(REFRESH_TOKEN_STORAGE_KEY)
      localStorageAction.remove(USER_INFO_STORAGE_KEY)
      localStorageAction.remove(CUSTOMER_CART_ID)
      localStorageAction.remove(CUSTOMER_CART_ITEM)
    },

    async login(userInfo: Entity.UserInformation, accessToken: string, refreshToken: string) {
      try {
        const dataStorage: Api.Login.Info = {
          userInfo,
          accessToken,
          refreshToken,
        }

        // 1. Phải lưu thông tin vào store trước để lấy được Token & UserInfo
        await this.handleLoginInfo(dataStorage)

        // 2. Đồng bộ giỏ hàng NGAY SAU KHI lưu thông tin login thành công
        const cartStore = useCartStore()
        await cartStore.syncLocalCartToDb()
      }
      catch (e) {
        console.warn('[Login Error]:', e)
      }
    },

    async handleLoginInfo(data: Api.Login.Info) {
      local.set('userInfo', data)
      local.set('accessToken', data.accessToken)
      local.set('refreshToken', data.refreshToken)

      localStorageAction.set(ACCESS_TOKEN_STORAGE_KEY, data.accessToken)
      localStorageAction.set(REFRESH_TOKEN_STORAGE_KEY, data.refreshToken)
      localStorageAction.set(USER_INFO_STORAGE_KEY, data.userInfo)

      this.token = data.accessToken
      this.userInfo = data
      this.userInfoDatn = data.userInfo

      const routeStore = useRouteStore()
      await routeStore.initAuthRoute()
    },
  },
})
