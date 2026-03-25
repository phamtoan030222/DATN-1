<script setup lang="ts">
import { ref } from 'vue'

// IMPORT ĐÚNG CẤU TRÚC THƯ MỤC CỦA BẠN
import LoginCustomer from './components/Login/LoginCustomer.vue'
import Register from './components/Register/index.vue'
import ResetPwd from './components/ResetPwd/index.vue'

// Khai báo các loại form
type IformType = 'login' | 'resetPwd' | 'register'
const formType = ref<IformType>('login')

// Map component tương ứng
const formComponets = {
  login: LoginCustomer,
  resetPwd: ResetPwd,
  register: Register,
}
</script>

<template>
  <n-el class="wh-full flex items-center justify-center bg-gray-50/50">
    <div class="fixed top-40px right-40px text-lg z-50">
      <DarkModeSwitch />
      <LangsSwitch />
    </div>

    <div class="w-full max-w-[420px] bg-white rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.08)] p-10 mx-4 relative">
      <div class="w-full flex flex-col items-center">
        <div class="flex flex-col items-center mb-6">
          <SvgIconsLogo class="text-[70px] text-[#4caf50]" />
          <h2 class="text-xl font-bold text-gray-800 mt-3 tracking-wide">
            My Laptop
          </h2>
        </div>

        <transition name="fade-slide" mode="out-in">
          <component
            :is="formComponets[formType]"
            v-model="formType"
            class="w-full"
          />
        </transition>
      </div>
    </div>
  </n-el>
</template>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}
.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(-20px);
}
.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
