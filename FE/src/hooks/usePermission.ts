import { useAuthStore } from "@/store";
import { isArray, isString } from "radash";

export function usePermission() {
  const authStore = useAuthStore();

  function hasPermission(permission?: Entity.RoleType | Entity.RoleType[]) {
    if (!permission) return true;

    if (!authStore.userInfo) return false;
    const { role } = authStore.userInfo;

    let has = role.includes("super");
    if (!has) {
      if (isArray(permission)) has = permission.some((i) => role.includes(i));

      if (isString(permission)) has = role.includes(permission);
    }
    return has;
  }

  return {
    hasPermission,
  };
}
