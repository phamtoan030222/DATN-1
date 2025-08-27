import type { RouteRecordRaw } from "vue-router";

export const routes: RouteRecordRaw[] = [
  {
    path: "/",
    name: "root",
    children: [],
  },
  {
    path: "/login",
    name: "login",
    component: () => import("@/views/build-in/login/index.vue"),
    meta: {
      title: "Đăng nhập",
      withoutTab: true,
    },
  },
  {
    path: "/not-found",
    name: "not-found",
    component: () => import("@/views/build-in/not-found/index.vue"),
    meta: {
      title: "Không tìm thấy trang",
      icon: "icon-park-outline:ghost",
      withoutTab: true,
    },
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import("@/views/build-in/not-found/index.vue"),
    name: "not-found",
    meta: {
      title: "Không tìm thấy trang",
      icon: "icon-park-outline:ghost",
      withoutTab: true,
    },
  },
];
