import { router } from "@/router";
import { local } from "@/utils";
import { useRouteStore } from "./router";
import { useTabStore } from "./tab";

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
          query: {
            redirect: route.fullPath,
          },
        });
      }
    },

    clearAuthStorage() {
      local.remove("accessToken");
      local.remove("refreshToken");
      local.remove("userInfo");
    },

    async login(userName: string, password: string) {
      try {
        // ✅ Fake dữ liệu trả về đúng với interface Api.Login.Info
        const fakeData: Api.Login.Info = {
          id: 1,
          userName,
          role: ["admin"], // 👈 dùng role, không phải roleList
          accessToken: "fake-token-123",
          refreshToken: "fake-refresh-456",
        };

        await this.handleLoginInfo(fakeData);
      } catch (e) {
        console.warn("[Login Error]:", e);
      }
    },

    async handleLoginInfo(data: Api.Login.Info) {
      local.set("userInfo", data);
      local.set("accessToken", data.accessToken);
      local.set("refreshToken", data.refreshToken);

      this.token = data.accessToken;
      this.userInfo = data;

      const routeStore = useRouteStore();
      await routeStore.initAuthRoute();

      const route = unref(router.currentRoute);
      const query = route.query as { redirect: string };
      router.push({
        path: query.redirect || "/",
      });
    },
  },
});
