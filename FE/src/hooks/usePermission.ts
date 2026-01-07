import { useAuthStore } from "@/store";
import { isArray, isString } from "radash";

export function usePermission() {
  const authStore = useAuthStore();

  function hasPermission(permission?: Entity.RoleType | Entity.RoleType[]) {
    if (!permission) return true;

    if (!authStore.userInfo) return false;
    const { rolesCodes } = authStore.userInfo.userInfo;

    let has = rolesCodes.includes("QUAN_LY");
    if (!has) {
      if (isArray(permission)) has = permission.some((i) => rolesCodes.includes(i));

      if (isString(permission)) has = rolesCodes.includes(permission);
    }
    return has;
  }

  return {
    hasPermission,
  };
}
