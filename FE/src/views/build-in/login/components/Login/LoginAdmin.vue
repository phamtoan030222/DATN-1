<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { URL_OAUTH2_GOOGLE_ADMIN, VITE_BASE_URL_CLIENT } from '@/constants/url'
import { postLogin } from '@/service/api/auth/auth.api'
import { local } from '@/utils'
import type { FormInst } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useNotification } from 'naive-ui'

const { t } = useI18n()
const notification = useNotification()

const formRef = ref<FormInst | null>(null)
const formValue = ref({
  account: '',
  pwd: '',
})

const isRemember = ref(false)
const isLoading = ref(false)
const appName = import.meta.env.VITE_APP_NAME || 'Admin Portal'

const rules = computed(() => {
  return {
    account: {
      required: true,
      trigger: ['blur', 'input'],
      message: t('login.accountRuleTip') || 'Vui lòng nhập tài khoản',
    },
    pwd: {
      required: true,
      trigger: ['blur', 'input'],
      message: t('login.passwordRuleTip') || 'Vui lòng nhập mật khẩu',
    },
  }
    return {
        account: {
            required: true,
            trigger: 'blur',
            message: 'Vui lòng nhập tài khoản',
        },
        pwd: {
            required: true,
            trigger: 'blur',
            message: 'Vui lòng nhập mật khẩu',
        },
    }
})

onMounted(() => {
  checkUserAccount()
})

function checkUserAccount() {
  const loginAccount = local.get('loginAccount')
  if (loginAccount) {
    formValue.value = loginAccount
    isRemember.value = true
  }
}

async function handleLogin() {
  try {
    // Naive UI validate sẽ throw error nếu form không hợp lệ
    await formRef.value?.validate()
  }
  catch (errors) {
    return // Dừng lại nếu form chưa nhập đủ
  }

  isLoading.value = true
  const { account, pwd } = formValue.value

  // Xử lý nhớ mật khẩu
  if (isRemember.value) {
    local.set('loginAccount', { account, pwd })
  }
  else {
    local.remove('loginAccount')
  }

  try {
    const token = await postLogin(account, pwd, 'ADMIN')
    if (token?.state) {
      window.location.href = `${VITE_BASE_URL_CLIENT}/redirect?state=${token.state}`
    }
    else {
      throw new Error('Token không hợp lệ')
    }
  }
  catch (error) {
    console.error('Lỗi đăng nhập:', error)
    notification.error({
      title: 'Đăng nhập thất bại',
      content: 'Vui lòng kiểm tra lại tài khoản hoặc mật khẩu.',
      duration: 3000,
    })
  }
  finally {
    isLoading.value = false
  }
}

function handleRedirectLoginADMIN() {
  window.location.href = URL_OAUTH2_GOOGLE_ADMIN
}
</script>

<template>
  <n-el class="wh-full flex-center" style="background-color: var(--body-color);">
    <div class="fixed top-40px right-40px text-lg">
      <DarkModeSwitch />
      <LangsSwitch />
    </div>
    <div
      class="p-4xl h-full w-full sm:w-450px sm:h-unset"
      style="background: var(--card-color); box-shadow: var(--box-shadow-1);"
    >
      <div class="w-full flex flex-col items-center">
        <SvgIconsLogo class="text-6em" />
        <n-h3>{{ appName }}</n-h3>

        <transition name="fade-slide" mode="out-in">
          <div class="w-full">
            <n-h2 depth="3" class="text-center mb-6">
              Đăng nhập Quản trị
            </n-h2>

            <n-form ref="formRef" :rules="rules" :model="formValue" :show-label="false" size="large">
              <n-form-item path="account">
                <n-input
                  v-model:value="formValue.account"
                  clearable
                  placeholder="Nhập tài khoản"
                  @keyup.enter="handleLogin"
                />
              </n-form-item>

              <n-form-item path="pwd">
                <n-input
                  v-model:value="formValue.pwd"
                  type="password"
                  placeholder="Nhập mật khẩu"
                  clearable
                  show-password-on="click"
                  @keyup.enter="handleLogin"
                >
                  <template #password-invisible-icon>
                    <icon-park-outline-preview-close-one />
                  </template>
                  <template #password-visible-icon>
                    <icon-park-outline-preview-open />
                  </template>
                </n-input>
              </n-form-item>

              <div class="flex items-center justify-between mb-4 px-1">
                <n-checkbox v-model:checked="isRemember">
                  Nhớ tài khoản
                </n-checkbox>
              </div>

              <n-space vertical :size="20">
                <n-button
                  block
                  type="primary"
                  size="large"
                  :loading="isLoading"
                  :disabled="isLoading"
                  @click="handleLogin"
                >
                  Đăng nhập
                </n-button>
              </n-space>
            </n-form>

            <n-divider>
              <span op-80>Hoặc đăng nhập bằng</span>
            </n-divider>

            <n-space justify="center">
              <n-button circle size="large" @click="handleRedirectLoginADMIN">
                <template #icon>
                  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
                    <path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448s448-200.6 448-448S759.4 64 512 64zm167 633.6C638.4 735 583 757 516.9 757c-95.7 0-178.5-54.9-218.8-134.9C281.5 589 272 551.6 272 512s9.5-77 26.1-110.1c40.3-80.1 123.1-135 218.8-135c66 0 121.4 24.3 163.9 63.8L610.6 401c-25.4-24.3-57.7-36.6-93.6-36.6c-63.8 0-117.8 43.1-137.1 101c-4.9 14.7-7.7 30.4-7.7 46.6s2.8 31.9 7.7 46.6c19.3 57.9 73.3 101 137 101c33 0 61-8.7 82.9-23.4c26-17.4 43.2-43.3 48.9-74H516.9v-94.8h230.7c2.9 16.1 4.4 32.8 4.4 50.1c0 74.7-26.7 137.4-73 180.1z" fill="currentColor" />
                  </svg>
                </template>
              </n-button>
            </n-space>
          </div>
        </transition>
      </div>
    </div>
  </n-el>
</template>
