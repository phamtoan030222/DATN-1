<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useNotification } from 'naive-ui'
import type { FormInst } from 'naive-ui'
import { VITE_BASE_URL_CLIENT } from '@/constants/url'
import { postLogin, postRegister, RegisterRequest } from '@/service/api/auth/auth.api'
import dayjs from 'dayjs'

const router = useRouter()
const notification = useNotification()
const formRef = ref<FormInst>()
const loading = ref(false)

const formValue = ref<RegisterRequest & { repassword?: string }>({
  fullname: '',
  birthday:  dayjs().valueOf(),
  email: '',
  phoneNumber: '',
  username: '',
  password: '',
  repassword: '',
})

const rules: any = {
  fullname: { required: true, trigger: 'blur', message: 'Vui lòng nhập họ tên' },
  birthday: { type: 'number', required: true, trigger: ['blur', 'change'], message: 'Chọn ngày sinh' },
  email: { required: true, trigger: 'blur', type: 'email', message: 'Email hợp lệ' },
  phoneNumber: { required: true, trigger: 'blur', pattern: /^0\d{9}$/, message: 'SĐT hợp lệ' },
  username: { required: true, trigger: 'blur', min: 4, message: 'Ít nhất 4 ký tự' },
  password: { required: true, trigger: 'blur', min: 6, message: 'Ít nhất 6 ký tự' },
  repassword: {
    required: true,
    trigger: 'blur',
    validator: (rule: any, value: string) => value === formValue.value.password ? true : new Error('Không khớp'),
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

async function handleRegister() {
  try {
    await formRef.value?.validate()
    loading.value = true
    await postRegister(formValue.value)
    notification.success({ content: 'Đăng ký thành công', duration: 3000 })
    const token = await postLogin(formValue.value.username, formValue.value.password, 'CUSTOMER')
    if (token)
      window.location.href = `${VITE_BASE_URL_CLIENT}/redirect?state=${token.state}`
  }
  catch (error: any) {
    notification.error({ content: error?.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.' })
  }
  finally {
    loading.value = false
  }
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

      <h3 class="text-xl font-bold text-center mb-6 text-gray-800">
        Đăng ký
      </h3>

      <n-form ref="formRef" :model="formValue" :rules="rules" label-placement="top" size="medium" :show-label="false">
        <n-grid :span="24" :x-gap="12" :y-gap="12">
          <n-form-item-gi :span="24" path="fullname">
            <n-input v-model:value="formValue.fullname" placeholder="Họ và tên" clearable :disabled="loading" class="rounded-md" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="phoneNumber">
            <n-input v-model:value="formValue.phoneNumber" placeholder="Số điện thoại" clearable :disabled="loading" class="rounded-md" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="birthday">
            <n-date-picker v-model:value="formValue.birthday" type="date" placeholder="Ngày sinh" class="w-full rounded-md" />
          </n-form-item-gi>
          <n-form-item-gi :span="24" path="email">
            <n-input v-model:value="formValue.email" placeholder="Email" clearable :disabled="loading" class="rounded-md" />
          </n-form-item-gi>
          <n-form-item-gi :span="24" path="username">
            <n-input v-model:value="formValue.username" placeholder="Tên đăng nhập" clearable :disabled="loading" class="rounded-md" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="password">
            <n-input v-model:value="formValue.password" type="password" placeholder="Mật khẩu" show-password-on="click" :disabled="loading" class="rounded-md" />
          </n-form-item-gi>
          <n-form-item-gi :span="12" path="repassword">
            <n-input v-model:value="formValue.repassword" type="password" placeholder="Xác nhận" show-password-on="click" :disabled="loading" class="rounded-md" />
          </n-form-item-gi>
          <n-form-item-gi :span="24">
            <n-button block type="primary" size="large" :loading="loading" class="!bg-[#4caf50] hover:!bg-[#3e8e41] !border-none font-bold rounded-md h-[42px] mt-2" @click="handleRegister">
              Đăng ký
            </n-button>
          </n-form-item-gi>
        </n-grid>
      </n-form>

      <div class="flex justify-center items-center gap-2 mt-4 text-[14px]">
        <span class="text-gray-700">Đã có tài khoản?</span>
        <n-button text class="!text-[#4caf50] hover:!text-[#3e8e41] font-medium" @click="router.push('/login')">
          Đăng nhập ngay
        </n-button>
      </div>
    </div>
  </n-el>
</template>
