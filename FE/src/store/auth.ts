import { router } from "@/router";
import { local } from "@/utils";
import { useRouteStore } from "./router";
import { useTabStore } from "./tab";
import { localStorageAction } from "@/utils/storage.helper";
import { ACCESS_TOKEN_STORAGE_KEY, REFRESH_TOKEN_STORAGE_KEY, USER_INFO_STORAGE_KEY } from "@/constants/storageKey";

interface AuthStatus {
  userInfo: Api.Login.Info | null;
  token: string;
}

export const useAuthStore = defineStore("auth-store", {
  state: (): AuthStatus => {
    return {
      userInfo: local.get("userInfo"),
      token: local.get("accessToken") || "",
    };
  },
  getters: {
    isLogin(state) {
      return Boolean(state.token);
    },
  },
  actions: {
    async logout() {
      const route = unref(router.currentRoute);
      this.clearAuthStorage();

      const routeStore = useRouteStore();
      routeStore.resetRouteStore();

      const tabStore = useTabStore();
      tabStore.clearAllTabs();

      this.$reset();

      if (route.meta.requiresAuth) {
        router.push({
          name: "login",
        });
      }
    },

    clearAuthStorage() {
      local.remove("accessToken");
      local.remove("refreshToken");
      local.remove("userInfo");

      localStorageAction.remove(ACCESS_TOKEN_STORAGE_KEY)
      localStorageAction.remove(REFRESH_TOKEN_STORAGE_KEY)
      localStorageAction.remove(USER_INFO_STORAGE_KEY)
    },

    async login(userInfo: Entity.UserInformation, accessToken: string, refreshToken: string) {
      try {
        const dataStorage: Api.Login.Info = {
          userInfo: userInfo,
          accessToken,
          refreshToken,
        };

        await this.handleLoginInfo(dataStorage);
      } catch (e) {
        console.warn("[Login Error]:", e);
      }
    },

    async handleLoginInfo(data: Api.Login.Info) {
      local.set("userInfo", data);
      local.set("accessToken", data.accessToken);
      local.set("refreshToken", data.refreshToken);

      localStorageAction.set(ACCESS_TOKEN_STORAGE_KEY, data.accessToken)
      localStorageAction.set(REFRESH_TOKEN_STORAGE_KEY, data.refreshToken)
      localStorageAction.set(USER_INFO_STORAGE_KEY, data.userInfo)

      this.token = data.accessToken;
      this.userInfo = data;

      const routeStore = useRouteStore();
      await routeStore.initAuthRoute();
    },
  },
});
