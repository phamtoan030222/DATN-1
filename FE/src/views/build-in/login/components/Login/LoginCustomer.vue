<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import type { FormInst } from 'naive-ui'
import { useNotification } from 'naive-ui'
import { local } from '@/utils'
import { postLogin } from '@/service/api/auth/auth.api'
import { URL_OAUTH2_GOOGLE_CUSTOMER, VITE_BASE_URL_CLIENT } from '@/constants/url'

const router = useRouter()
const { t } = useI18n()
const notification = useNotification()

const rules = computed(() => {
  return {
    account: { required: true, trigger: 'blur', message: t('login.accountRuleTip') || 'Vui lòng nhập tài khoản' },
    pwd: { required: true, trigger: 'blur', message: t('login.passwordRuleTip') || 'Vui lòng nhập mật khẩu' },
  }
})

const formValue = ref({ account: '', pwd: '' })
const isLoading = ref(false)
const formRef = ref<FormInst | null>(null)

function handleLogin() {
  formRef.value?.validate(async (errors) => {
    if (errors)
      return
    isLoading.value = true
    try {
      const token = await postLogin(formValue.value.account, formValue.value.pwd, 'CUSTOMER')
      if (token)
        window.location.href = `${VITE_BASE_URL_CLIENT}/redirect?state=${token.state}`
      notification.error({ content: 'Đăng nhập thành công.', duration: 3000,
      })
    }
    catch (error) {
      notification.error({ content: 'Đăng nhập thất bại. Vui lòng kiểm tra lại tài khoản.', duration: 3000,
      })
    }
    finally {
      isLoading.value = false
    }
  })
}

onMounted(() => {
  const loginAccount = local.get('loginAccount')
  if (loginAccount)
    formValue.value = loginAccount
})

function handleRedirectLoginCUSTOMER() {
  window.location.href = URL_OAUTH2_GOOGLE_CUSTOMER
}
</script>

<template>
  <n-el class="wh-full min-h-screen flex items-center justify-center" style="background-color: var(--body-color);">
    <div class="fixed top-40px right-40px text-lg z-50">
      <DarkModeSwitch />
      <LangsSwitch />
    </div>

    <div class="w-full max-w-[420px] rounded-2xl p-8 sm:p-10 mx-4" style="background: var(--card-color); box-shadow: var(--box-shadow-1);">
      <div class="w-full flex flex-col items-center">
        <SvgIconsLogo class="text-[70px] text-[#4caf50]" />
        <n-h3 class="font-bold text-xl mt-3 mb-6">
          My Laptop
        </n-h3>

        <n-h2 depth="3" class="text-center font-bold mb-6 text-gray-800">
          Đăng nhập
        </n-h2>

        <n-form ref="formRef" :rules="rules" :model="formValue" :show-label="false" size="large" class="w-full">
          <n-form-item path="account">
            <n-input v-model:value="formValue.account" clearable placeholder="Nhập tài khoản" class="rounded-md" @keyup.enter="handleLogin" />
          </n-form-item>

          <n-form-item path="pwd">
            <n-input v-model:value="formValue.pwd" type="password" placeholder="Nhập mật khẩu" clearable show-password-on="click" class="rounded-md" @keyup.enter="handleLogin">
              <template #password-invisible-icon>
                <icon-park-outline-preview-close-one />
              </template>
              <template #password-visible-icon>
                <icon-park-outline-preview-open />
              </template>
            </n-input>
          </n-form-item>

          <n-space vertical :size="20" class="w-full">
            <div class="flex items-center justify-start">
              <n-button text class="!text-[#4caf50] hover:!text-[#3e8e41] font-medium" @click="router.push('/reset-password')">
                Quên mật khẩu?
              </n-button>
            </div>

            <n-button block type="primary" size="large" :loading="isLoading" class="!bg-[#4caf50] hover:!bg-[#3e8e41] !border-none font-bold rounded-md h-[42px]" @click="handleLogin">
              Đăng nhập
            </n-button>

            <n-flex justify="center" class="mt-2 text-[14px]">
              <n-text>Không có tài khoản?</n-text>
              <n-button type="primary" text class="!text-[#4caf50] hover:!text-[#3e8e41] font-medium" @click="router.push('/register')">
                Đăng ký ngay
              </n-button>
            </n-flex>
          </n-space>
        </n-form>

        <n-divider><span class="opacity-80">Hoặc</span></n-divider>

        <n-space justify="center" class="w-full mt-2">
          <n-button circle size="large" class="text-gray-800 hover:!text-[#4caf50] hover:!border-[#4caf50] transition-colors border-gray-300 shadow-sm" @click="handleRedirectLoginCUSTOMER">
            <template #icon>
              <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024" class="w-5 h-5">
                <path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448s448-200.6 448-448S759.4 64 512 64zm167 633.6C638.4 735 583 757 516.9 757c-95.7 0-178.5-54.9-218.8-134.9C281.5 589 272 551.6 272 512s9.5-77 26.1-110.1c40.3-80.1 123.1-135 218.8-135c66 0 121.4 24.3 163.9 63.8L610.6 401c-25.4-24.3-57.7-36.6-93.6-36.6c-63.8 0-117.8 43.1-137.1 101c-4.9 14.7-7.7 30.4-7.7 46.6s2.8 31.9 7.7 46.6c19.3 57.9 73.3 101 137 101c33 0 61-8.7 82.9-23.4c26-17.4 43.2-43.3 48.9-74H516.9v-94.8h230.7c2.9 16.1 4.4 32.8 4.4 50.1c0 74.7-26.7 137.4-73 180.1z" fill="currentColor" />
              </svg>
            </template>
          </n-button>
        </n-space>
      </div>
    </div>
  </n-el>
</template>

<style scoped></style>
