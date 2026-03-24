import type { RouteRecordRaw } from 'vue-router'

export const routes: RouteRecordRaw[] = [

  // --- KHU VỰC CỦA USER (CLIENT) ---
  {
    path: '/',
    component: () => import('@/views/client/ClientLayout.vue'), // Layout chứa Header + Footer
    redirect: '/home', // Mặc định vào trang chủ
    children: [
      // 1. TRANG CHỦ (Khớp với href: '/')
      // Khi user vào '/', router redirect về '/home'
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/client/user/home/HomeView.vue'),
        meta: { title: 'Trang chủ' },
      },
      // 3. CHI TIẾT SẢN PHẨM (Dùng khi bấm vào 1 sản phẩm cụ thể)
      {
        path: 'product-detail/:id',
        name: 'ProductDetail',
        component: () => import('@/views/client/user/product/ProductDetailView.vue'),
        meta: { title: 'Chi tiết sản phẩm' },
      },
      // 4. GIỚI THIỆU (Khớp với href: '/gioi-thieu')
      {
        path: 'gioi-thieu',
        name: 'About',
        // Lưu ý: Bạn cần tạo file AboutView.vue
        component: () => import('@/views/client/user/about/AboutView.vue'),
        meta: { title: 'Giới thiệu' },
      },

      // 5. LIÊN HỆ (Khớp với href: '/lien-he')
      // {
      //   path: 'lien-he',
      //   name: 'Contact',
      //   // Lưu ý: Bạn cần tạo file ContactView.vue
      //   component: () => import('@/views/client/user/contact/ContactView.vue'),
      //   meta: { title: 'Liên hệ' },
      // },

      // 6. TRA CỨU ĐƠN HÀNG (Khớp với href: '/tra-cuu')
      {
        path: 'tra-cuu',
        name: 'OrderTracking',
        // Lưu ý: Bạn cần tạo file TrackingView.vue
        component: () => import('@/views/client/user/tracking/TrackingView.vue'),
        meta: { title: 'Đơn hàng' },
      },
      {
        path: 'san-pham',
        name: 'Products',
        component: () => import('@/views/client/user/product/ProductListView.vue'),
        meta: { title: 'Sản phẩm' },
      },
      // --- GIỎ HÀNG & THANH TOÁN ---
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/client/user/cart/CartView.vue'),
        meta: { title: 'Giỏ hàng' },
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/client/user/order/CheckoutView.vue'),
        meta: { title: 'Thanh toán' },
      },
      {
        path: 'order-success',
        name: 'OrderSuccess',
        component: () => import('@/views/client/user/order/OrderSuccess.vue'),
        meta: { title: 'Đặt hàng thành công' },
      },
      {
        path: 'users',
        name: 'User',
        component: () => import('@/views/client/user/UserLayout.vue'),
        children: [
          {
            path: 'orders',
            name: 'Orders',
            component: () => import('@/views/client/user/order/OrderView.vue'),
            meta: { title: 'Đơn hàng' },
          },
          {
            path: 'profile',
            name: 'Profile',
            component: () => import('@/views/client/user/profile/ProfileView.vue'),
            meta: { title: 'Thông tin cá nhân' },
          },
        ],
      },
    ],
  },

  {
    path: '/',
    name: 'root',
    children: [],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/build-in/login/components/Login/LoginCustomer.vue'),
    meta: {
      title: 'Đăng nhập',
      withoutTab: true,
    },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/build-in/login/components/Register/index.vue'),
    meta: {
      title: 'Đăng ký',
      withoutTab: true,
    },
  },
  // --- THÊM ROUTE QUÊN MẬT KHẨU Ở ĐÂY ---
  {
    path: '/reset-password',
    name: 'reset-password',
    // Trỏ đúng vào file index.vue trong thư mục ResetPwd của bạn
    component: () => import('@/views/build-in/login/components/ResetPwd/index.vue'),
    meta: {
      title: 'Quên mật khẩu',
      withoutTab: true,
    },
  },
  {
    path: '/login-admin',
    name: 'login_admin',
    component: () => import('@/views/build-in/login/components/Login/LoginAdmin.vue'),
    meta: {
      title: 'Đăng nhập',
      withoutTab: true,
    },
  },
  {
    path: '/not-found',
    name: 'not-found',
    component: () => import('@/views/build-in/not-found/index.vue'),
    meta: {
      title: 'Không tìm thấy trang',
      icon: 'icon-park-outline:ghost',
      withoutTab: true,
    },
  },
  {
    path: '/redirect',
    name: 'redirect',
    component: () => import('@/views/build-in/login/components/Redirect.vue'),
    meta: {
      title: 'Đang chuyển hướng',
      withoutTab: true,
    },
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/build-in/not-found/index.vue'),
    name: 'not-found',
    meta: {
      title: 'Không tìm thấy trang',
      icon: 'icon-park-outline:ghost',
      withoutTab: true,
    },
  },
  {
    path: '/error/403',
    name: 'forbidden',
    component: () => import('@/views/403/403.vue'),
  },
  {
    path: '/error/401',
    name: 'unauthorized',
    component: () => import('@/views/401/401.vue'),
  },
]
