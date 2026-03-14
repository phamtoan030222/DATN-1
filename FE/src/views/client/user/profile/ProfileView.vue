<template>
  <div class="bg-gray-100">
    <div class="bg-white p-10 rounded shadow w-1100px mx-auto">

      <!-- title -->
      <div class="text-22px font-semibold">Hồ Sơ Của Tôi</div>
      <div class="text-gray-500 mt-1">
      </div>

      <n-divider class="my-6" />
      <div class="flex w-1/2">

        <div class="flex-1 pr-20">

          <!-- username -->
          <div class="flex items-center mb-6">
            <div class="w-140px text-gray-500">Tên đăng nhập</div>
            <div>{{ profile.username }}</div>
          </div>

          <!-- name -->
          <div class="flex items-center mb-6">
            <div class="w-140px text-gray-500">Tên</div>
            <n-input
              v-model:value="profile.name"
              class="w-350px"
            />
          </div>

          <!-- email -->
          <div class="flex items-center mb-6">
            <div class="w-140px text-gray-500">Email</div>
              <n-input
              v-model:value="profile.email"
              class="w-350px"
            />
          </div>

          <!-- phone -->
          <div class="flex items-center mb-6">
            <div class="w-140px text-gray-500">Số điện thoại</div>
            <n-input
              v-model:value="profile.phone"
              class="w-350px"
            />
          </div>

          <!-- save -->
          <div class="ml-140px mt-8">
            <n-button type="success" @click="handleClickSave" class="w-120px">
              Lưu
            </n-button>
          </div>

        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { postInformation } from '@/service/api/client/customer/customer.api'
import { useAuthStore } from '@/store'
import { ref } from 'vue'

const { userInfoDatn } = storeToRefs(useAuthStore())
const notification = useNotification();
interface ProfileState {
  username: string
  name: string
  email: string
  phone: string
}

const profile = ref<ProfileState>({
  username: userInfoDatn.value?.username ?? '',
  name: userInfoDatn.value?.fullName ?? '',
  email: userInfoDatn.value?.email as string,
  phone: userInfoDatn.value?.phone as string,
})

const handleClickSave = async () => {
  try {
    const res = postInformation({
      fullName: profile.value.name,
      email: profile.value.email,
      phoneNumber: profile.value.phone,
    })

    notification.success({content: "Cập nhật thành công", duration: 3000})
  } catch (e) {
    notification.error({content: "Lỗi khi thay đổi. Vui lòng kiểm tra lại", duration: 3000})
  }
}
</script>

<style scoped>
</style>