<script setup lang="ts">
import { VITE_BASE_URL_CLIENT } from '@/constants/url'
import { postLogin, postRegister, RegisterRequest } from '@/service/api/auth/auth.api'
import type { FormInst } from 'naive-ui'

const emit = defineEmits(['update:modelValue'])
const formRef = ref<FormInst>()
const loading = ref(false)
const notification = useNotification()
const rules: any = {
  fullname: {
    required: true,
    trigger: 'blur',
    message: 'Vui lòng nhập họ và tên',
  },
  birthday: {
    type: 'number',
    required: true,
    trigger: ['blur', 'change'],
    message: 'Vui lòng chọn ngày sinh',
  },
  email: {
    required: true,
    trigger: 'blur',
    type: 'email',
    message: 'Vui lòng nhập email hợp lệ',
  },
  phone: {
    required: true,
    trigger: 'blur',
    pattern: /^0[0-9]{9}$/,
    message: 'Vui lòng nhập số điện thoại hợp lệ',
  },
  username: {
    required: true,
    trigger: 'blur',
    min: 4,
    message: 'Tài khoản phải ít nhất 4 ký tự',
  },
  password: {
    required: true,
    trigger: 'blur',
    min: 6,
    message: 'Mật khẩu phải ít nhất 6 ký tự',
  },
  repassword: {
    required: true,
    trigger: 'blur',
    validator: (rule: any, value: string) => {
      if (value !== formValue.value.password) {
        return new Error('Mật khẩu không trùng khớp')
      }
      return true
    },
  },
}

// const formValue = ref<RegisterRequest>({
//   fullname: '',
//   birthday: undefined,
//   email: '',
//   phone: '',
//   account: '',
//   password: '',
//   repassword: '',
// })

const formValue = ref<RegisterRequest>({
  fullname: 'adjshjasdf',
  birthday: 1767799666000,
  email: 'asjdfjs@gmail.com',
  phone: '0385423039',
  username: 'user1',
  password: '123456',
  repassword: '123456',
})

async function handleRegister() {
  try {
    await formRef.value?.validate()
    loading.value = true

    await postRegister(formValue.value)
    notification.success({ content: 'Đăng ký thành công' })

    const token = await postLogin(formValue.value.username, formValue.value.password, 'CUSTOMER')
    if (token) {
      window.location.href = `${VITE_BASE_URL_CLIENT}/redirect?state=${token.state}`
    }
  }
  catch (error) {
    notification.error({ content: 'Đăng ký thất bại. Vui lòng thử lại.' })
  }
  finally {
    loading.value = false
  }
}

const handRedirectToLogin = () => {
  window.location.href = VITE_BASE_URL_CLIENT + '/login'
}
</script>

<template>
  <div class="w-full h-full min-h-screen flex items-center justify-center from-[#f5f7fa] to-[#c3cfe2]">
    <div
      class="max-w-50% bg-white rounded-12px shadow-lg p-24px animate-in fade-in slide-in-from-bottom-8 duration-500">

      <!-- Form -->
      <n-form ref="formRef" :model="formValue" :rules="rules" label-width="auto" @submit.prevent="handleRegister">
        <n-grid :span="24" :x-gap="24">
          <!-- Fullname -->
          <n-form-item-gi :span="12" path="fullname" label="Họ và tên">
            <n-input v-model:value="formValue.fullname" type="text" placeholder="Nhập họ và tên" clearable
              :disabled="loading" />
          </n-form-item-gi>

          <n-form-item-gi :span="12" path="birthday" label="Ngày sinh">
            <n-date-picker v-model:value="formValue.birthday" type="datetime" placeholder="Chọn ngày sinh" />
          </n-form-item-gi>

          <!-- Email -->
          <n-form-item-gi :span="12" path="email" label="Email">
            <n-input v-model:value="formValue.email" placeholder="Nhập email" clearable :disabled="loading" />
          </n-form-item-gi>

          <!-- Phone -->
          <n-form-item-gi :span="12" path="phone" label="Số điện thoại">
            <n-input v-model:value="formValue.phone" placeholder="Nhập số điện thoại" clearable :disabled="loading" />
          </n-form-item-gi>

          <!-- Username -->
          <n-form-item-gi :span="24" path="username" label="Tên người dùng">
            <n-input v-model:value="formValue.username" type="text" placeholder="Nhập tên người dùng" clearable
              :disabled="loading" />
          </n-form-item-gi>

          <!-- Password -->
          <n-form-item-gi :span="24" path="password" label="Mật khẩu">
            <n-input v-model:value="formValue.password" type="password" placeholder="Nhập mật khẩu"
              show-password-on="click" :disabled="loading" />
          </n-form-item-gi>

          <!-- Confirm Password -->
          <n-form-item-gi :span="24" path="repassword" label="Xác nhận mật khẩu">
            <n-input v-model:value="formValue.repassword" type="password" placeholder="Xác nhận mật khẩu"
              show-password-on="click" :disabled="loading" />
          </n-form-item-gi>

          <!-- Submit Button -->
          <n-form-item-gi :span="24">
            <n-button type="primary" block :loading="loading" :disabled="loading" @click="handleRegister"
              class="h-38px text-14px font-600 rounded-8px border-none bg-[#36ad6a]! color-white! mt-8px transition-all duration-300 hover:bg-[#2d9156]! hover:shadow-lg hover:shadow-[rgba(54,173,106,0.4)] active:translate-y-1px">
              Đăng ký
            </n-button>
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <!-- Login Link -->
      <div class="flex justify-center gap-6px mt-16px text-13px color-[#666]">
        <span>Đã có tài khoản?</span>
        <n-button text tag="a" type="primary" @click="handRedirectToLogin" class="color-[#36ad6a]! hover:color-[#2d9156]!">
          Đăng nhập ngay
        </n-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
:deep(.n-form) {
  gap: 0;
}

:deep(.n-form-item-gi) {
  margin-bottom: 4px;
}

:deep(.n-form-item-gi__label) {
  color: #333;
  font-weight: 500;
  font-size: 14px;
}

:deep(.n-input) {
  border-radius: 8px;
  font-size: 14px;
}

:deep(.n-input__input) {
  font-size: 14px;
  height: 32px;
}

:deep(.n-input:not(.n-input--disabled):hover) {
  border-color: #36ad6a !important;
}

:deep(.n-input.n-input--focus) {
  border-color: #36ad6a !important;
  box-shadow: 0 0 0 2px rgba(54, 173, 106, 0.15) !important;
}

:deep(.n-date-picker) {
  width: 100%;
}
</style>
