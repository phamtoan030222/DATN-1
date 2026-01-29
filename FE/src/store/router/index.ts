import type { MenuOption } from "naive-ui";
import { router } from "@/router";
import { staticRoutes } from "@/router/routes.static";
import { fetchUserRoutes } from "@/service";
import { $t, localStorageAction } from "@/utils";
import { createMenus, createRoutes, generateCacheRoutes } from "./helper";
import { USER_INFO_STORAGE_KEY } from "@/constants/storageKey";

interface RoutesStatus {
  isInitAuthRoute: boolean;
  menus: MenuOption[];
  rowRoutes: AppRoute.RowRoute[];
  activeMenu: string | null;
  cacheRoutes: string[];
}
export const useRouteStore = defineStore("route-store", {
  state: (): RoutesStatus => {
    return {
      isInitAuthRoute: false,
      activeMenu: null,
      menus: [],
      rowRoutes: [],
      cacheRoutes: [],
    };
  },
  actions: {
    resetRouteStore() {
      this.resetRoutes();
      this.$reset();
    },
    resetRoutes() {
      if (router.hasRoute("appRoot")) router.removeRoute("appRoot");
    },
    setActiveMenu(key: string) {
      this.activeMenu = key;
    },

    async initRouteInfo() {
      if (import.meta.env.VITE_ROUTE_LOAD_MODE === "dynamic") {
        try {
          const result = await fetchUserRoutes({
            id: 1,
          });

          if (!result.isSuccess || !result.data) {
            throw new Error("Failed to fetch user routes");
          }

          return result.data;
        } catch (error) {
          console.error("Failed to initialize route info:", error);
          throw error;
        }
      } else {
        this.rowRoutes = staticRoutes;
        const userInfo = localStorageAction.get(USER_INFO_STORAGE_KEY);
        return userInfo && userInfo.rolesCodes ? staticRoutes.filter((route) => route.roles?.includes(userInfo.rolesCodes[0])) : staticRoutes;
      }
    },
    async initAuthRoute() {
      this.isInitAuthRoute = false;

      try {
        // Initialize route information
        const rowRoutes = await this.initRouteInfo();
        if (!rowRoutes) {
          const error = new Error("Failed to get route information");
          window.$message.error($t(`app.getRouteError`));
          throw error;
        }
        this.rowRoutes = rowRoutes;

        // Generate actual route and insert
        const routes = createRoutes(rowRoutes);
        router.addRoute(routes);

        // Generate side menu
        this.menus = createMenus(rowRoutes);

        // Generate the route cache
        this.cacheRoutes = generateCacheRoutes(rowRoutes);

        this.isInitAuthRoute = true;
      } catch (error) {
        this.isInitAuthRoute = false;
        throw error;
      }
    },
  },
});
