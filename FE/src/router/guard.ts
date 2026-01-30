import { ACCESS_TOKEN_STORAGE_KEY, USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import { useAppStore, useRouteStore, useTabStore } from '@/store'
import { localStorageAction } from '@/utils/storage.helper'
import type { Router } from 'vue-router'

const title = import.meta.env.VITE_APP_NAME

export function setupRouterGuard(router: Router) {
  const appStore = useAppStore()
  const routeStore = useRouteStore()
  const tabStore = useTabStore()

  router.beforeEach(async (to, from, next) => {
    if (to.meta.href) {
      window.open(to.meta.href)
      next(false)
      return
    }
    appStore.showProgress && window.$loadingBar?.start()

    const isLogin = !!(localStorageAction.get(ACCESS_TOKEN_STORAGE_KEY))
    const userInfo = localStorageAction.get(USER_INFO_STORAGE_KEY)
    if (to.name === 'root') {
      if (isLogin) {
        next({ path: import.meta.env.VITE_HOME_PATH, replace: true })
      }
      else {
        next({ path: '/login', replace: true })
      }
      return
    }

    if (to.meta.requiresAuth === true && !isLogin) {
      const redirect = to.name === 'not-found' ? undefined : to.fullPath
      next({ path: '/login-admin', query: { redirect } })
      return
    }
    else if (to.meta.roles && !to.meta.roles.includes(userInfo.rolesCodes[0])) {
      next({ name: 'forbidden' })
      return
    }

    if (!(to.name === 'login' || to.name === 'login_admin') && !routeStore.isInitAuthRoute) {
      try {
        await routeStore.initAuthRoute()
        if (to.name === 'not-found') {
          next({
            path: to.fullPath,
            replace: true,
            query: to.query,
            hash: to.hash,
          })
          return
        }
      }
      catch {
        next({ path: '/login'})
        return
      }
    }

    if ((to.name === 'login' || to.name === 'login_admin') && isLogin) {
      next({ path: to.name === 'login_admin' ? '/dashboard/sales' : '/' })
      return
    }

    next()
  })
  router.beforeResolve((to) => {
    routeStore.setActiveMenu(to.meta.activeMenu ?? to.fullPath)
    tabStore.addTab(to)
    tabStore.setCurrentTab(to.fullPath as string)
  })

  router.afterEach((to) => {
    document.title = `${to.meta.title} - ${title}`
    appStore.showProgress && window.$loadingBar?.finish()
  })
}
