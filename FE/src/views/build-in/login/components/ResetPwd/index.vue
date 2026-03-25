<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInst, FormRules } from 'naive-ui'
import { useNotification } from 'naive-ui'
import { forgotCustomerPassword } from '@/service/api/client/customer/customer.api'

const router = useRouter()
const notification = useNotification()

const formRef = ref<FormInst | null>(null)
const isLoading = ref(false)
const formValue = ref({ email: '' })

const rules: FormRules = {
  email: [
    { required: true, message: 'Vui lòng nhập email của bạn', trigger: ['blur', 'input'] },
    { type: 'email', message: 'Định dạng email không hợp lệ', trigger: ['blur', 'input'] },
  ],
}

function handleResetPassword() {
  formRef.value?.validate(async (errors) => {
    if (errors)
      return
    isLoading.value = true
    try {
      await forgotCustomerPassword({ email: formValue.value.email })
      notification.success({ content: 'Mật khẩu mới đã được gửi vào Email của bạn!', duration: 3000 })
      router.push('/login')
    }
    catch (error: any) {
      notification.error({ content: error?.response?.data?.message || 'Lỗi khi gửi yêu cầu. Vui lòng thử lại!', duration: 3000 })
    }
    finally {
      isLoading.value = false
    }
  })
}
</script>

<template>
  <n-el class="wh-full flex items-center justify-center bg-gray-50/50">
    <div class="fixed top-40px right-40px text-lg z-50">
      <DarkModeSwitch />
      <LangsSwitch />
    </div>

    <div class="w-full max-w-[420px] bg-white rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.08)] p-10 mx-4">
      <div class="flex flex-col items-center mb-6">
        <SvgIconsLogo class="text-[70px] text-[#4caf50]" />
        <h2 class="text-xl font-bold text-gray-800 mt-3 tracking-wide">
          My Laptop
        </h2>
      </div>

      <h3 class="text-xl font-bold text-center mb-2 text-gray-800">
        Quên mật khẩu
      </h3>
      <p class="text-center text-gray-500 mb-6 text-sm">
        Vui lòng nhập email đã đăng ký. Chúng tôi sẽ gửi mật khẩu mới vào email của bạn.
      </p>

      <n-form ref="formRef" :rules="rules" :model="formValue" :show-label="false" size="large">
        <n-form-item path="email">
          <n-input v-model:value="formValue.email" clearable placeholder="Nhập địa chỉ Email" class="rounded-md" @keyup.enter="handleResetPassword">
            <template #prefix>
              <icon-park-outline-mail class="text-gray-400" />
            </template>
          </n-input>
        </n-form-item>

        <n-space vertical :size="20" class="mt-2">
          <n-button block type="primary" size="large" :loading="isLoading" class="!bg-[#4caf50] hover:!bg-[#3e8e41] !border-none font-bold rounded-md h-[42px]" @click="handleResetPassword">
            Gửi mật khẩu mới
          </n-button>
          <n-button block size="large" class="rounded-md h-[42px] font-medium text-gray-700" @click="router.push('/login')">
            Quay lại Đăng nhập
          </n-button>
        </n-space>
      </n-form>
    </div>
  </n-el>
</template>
