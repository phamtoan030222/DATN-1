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
    path: "/login-admin",
    name: "login_admin",
    component: () => import("@/views/build-in/login/components/Login/LoginAdmin.vue"),
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
    path: "/redirect",
    name: "redirect",
    component: () => import("@/views/build-in/login/components/Redirect.vue"),
    meta: {
      title: "Đang chuyển hướng",
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
  {
    path: "/error/403",
    name: "forbidden",
    component: () => import("@/views/403/403.vue"),
  },
  {
    path: "/error/401",
    name: "unauthorized",
    component: () => import("@/views/401/401.vue"),
  },
];
