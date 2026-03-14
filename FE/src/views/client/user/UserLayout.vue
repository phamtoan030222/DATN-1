<template>
    <div class="flex gap-6">
        <div class="flex-2">
            <div class="w-260px bg-white p-16px rounded-8px shadow">

                <!-- Profile -->
                <div class="flex items-center gap-12px mb-16px">
                    <n-avatar round :size="48" :src="userInfoDatn?.pictureUrl" />

                    <div>
                        <div class="font-semibold text-18px">{{ userInfoDatn?.fullName }}</div>
                        <div class="text-gray text-12px flex items-center gap-4px">
                            Sửa hồ sơ
                        </div>
                    </div>
                </div>
                <div class="border-b border-gray-200 mb-12px"></div>
                <!-- Menu -->
                <div class="flex flex-col gap-14px text-15px">
                    <div class="flex items-center gap-10px cursor-pointer hover:text-blue">
                        <n-icon size="20" class="text-blue">
                            <PersonOutline />
                        </n-icon>
                        <RouterLink :to="{ name: 'Profile' }" :class="`${route.name == 'Profile' && 'text-green'}`">Thông tin cá nhân</RouterLink>
                    </div>

                    <div class="flex items-center gap-10px cursor-pointer hover:text-blue">
                        <n-icon size="20" class="text-blue">
                            <ReceiptOutline />
                        </n-icon>
                        <RouterLink :to="{ name: 'Orders' }" :class="`${route.name == 'Orders' && 'text-green'}`">Đơn Mua</RouterLink>
                    </div>
                </div>
            </div>
        </div>
        <div class="flex-1">
            <div class="">
                <RouterView />
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/store';
import { PersonOutline, ReceiptOutline } from '@vicons/ionicons5';

const { userInfoDatn } = storeToRefs(useAuthStore())
const route = useRoute();

</script>

<style scoped>
</style>