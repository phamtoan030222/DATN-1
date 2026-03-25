<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { group } from 'radash'
import NoticeList from '../common/NoticeList.vue'
import { useNotificationStore } from '@/store/app/notification'

const notificationStore = useNotificationStore()
const currentTab = ref(0)

const massageCount = computed(() => notificationStore.unreadCount)
const groupMessage = computed(() => group(notificationStore.asMessages, (i: any) => i.type))

function handleRead(id: number) {
  const message = notificationStore.asMessages.find((m: any) => m.id === id)
  if (message)
    notificationStore.handleNotificationClick(message)
}

onMounted(async () => {
  await notificationStore.fetchNotifications()
  notificationStore.connectWebSocket()
})

onUnmounted(() => {
  notificationStore.disconnectWebSocket()
})
</script>

<template>
  <n-popover placement="bottom" trigger="click" arrow-point-to-center class="!p-0">
    <template #trigger>
      <n-tooltip placement="bottom" trigger="hover">
        <template #trigger>
          <CommonWrapper>
            <n-badge :value="massageCount" :max="99" style="color: unset">
              <icon-park-outline-remind />
            </n-badge>
          </CommonWrapper>
        </template>
        <span>{{ $t("app.notificationsTips") }}</span>
      </n-tooltip>
    </template>

    <n-tabs
      v-model:value="currentTab"
      type="line"
      animated
      justify-content="space-evenly"
      class="w-390px"
    >
      <!-- Nút đọc tất cả -->
      <template #suffix>
        <n-button
          v-if="massageCount > 0"
          text
          size="small"
          class="mr-2 text-xs text-gray-400"
          @click="notificationStore.markAllAsRead()"
        >
          Đọc tất cả
        </n-button>
      </template>

      <n-tab-pane :name="0">
        <template #tab>
          <n-space class="w-130px" justify="center">
            {{ $t("app.notifications") }}
            <n-badge
              type="info"
              :value="groupMessage[0]?.filter((i: any) => !i.isRead).length"
              :max="99"
            />
          </n-space>
        </template>
        <NoticeList :list="groupMessage[0]" @read="handleRead" />
      </n-tab-pane>

      <n-tab-pane :name="1">
        <template #tab>
          <n-space class="w-130px" justify="center">
            {{ $t("app.messages") }}
            <n-badge
              type="warning"
              :value="groupMessage[1]?.filter((i: any) => !i.isRead).length"
              :max="99"
            />
          </n-space>
        </template>
        <NoticeList :list="groupMessage[1]" @read="handleRead" />
      </n-tab-pane>
    </n-tabs>
  </n-popover>
</template>
