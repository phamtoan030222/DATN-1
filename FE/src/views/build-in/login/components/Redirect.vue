<template>
  <div class="redirect-container">
    <p class="redirect-message">
      <n-spin size="large" />
    </p>
  </div>
</template>

<script lang="ts" setup>
import { ROLES } from '@/constants/roles'
import { useAuthStore } from '@/store'
import { getUserInformation } from '@/utils/token.helper'
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()

const router = useRouter()

const authStore = useAuthStore()

const { state } = route.query

onMounted(() => {
  if (state) {
    const decodedState = atob(state as string)

    const { accessToken, refreshToken } = JSON.parse(decodedState)

    const user = getUserInformation(accessToken)

    authStore.login(
      user,
      accessToken,
      refreshToken
    )

    if (user.rolesCodes.includes(ROLES.QUAN_LY) && user.roleScreen === "ADMIN") {
      router.push({ name: 'dashboard_sales' })
      return
    } else if (user.rolesCodes.includes(ROLES.NHAN_VIEN) && user.roleScreen === "ADMIN") {
      return
    } else if (user.rolesCodes.includes(ROLES.KHACH_HANG) && user.roleScreen === ROLES.KHACH_HANG) {
      return
    }
  }
  router.push({ name: "login " })
})
</script>

<style scoped>
.redirect-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f0f0;
}

.redirect-message {
  font-size: 1.5rem;
  color: #333;
  font-weight: bold;
}
</style>