<script setup lang="ts">
interface Props {
  list?: any[]
}
const { list } = defineProps<Props>()

const emit = defineEmits<{
  (e: 'read', val: number): void
}>()
</script>

<template>
  <n-scrollbar style="height: 400px">
    <div
      v-if="!list || list.length === 0"
      class="flex flex-col items-center justify-center py-10 text-gray-400"
    >
      <icon-park-outline-remind style="font-size: 36px; opacity: 0.25;" />
      <span class="mt-2 text-sm">Không có thông báo nào</span>
    </div>

    <n-list v-else hoverable clickable>
      <n-list-item
        v-for="item in list"
        :key="item.id"
        :style="{
          padding: '10px 16px',
          background: item.isRead ? 'transparent' : '#f6ffed',
        }"
        @click="emit('read', item.id)"
      >
        <n-thing content-indented :class="{ 'opacity-50': item.isRead }">
          <template #avatar>
            <div
              :style="{
                width: '8px',
                height: '8px',
                borderRadius: '50%',
                background: item.isRead ? '#d9d9d9' : '#18a058',
                marginTop: '6px',
              }"
            />
          </template>

          <template #header>
            <span
              :style="{
                fontSize: '13.5px',
                fontWeight: item.isRead ? 400 : 600,
                color: item.isRead ? '#999' : '#18a058',
              }"
            >
              {{ item.title }}
            </span>
          </template>

          <template v-if="!item.isRead" #header-extra>
            <span
              style="
                font-size: 11px;
                color: #18a058;
                background: #e6f7e6;
                border: 1px solid #b7eb8f;
                border-radius: 4px;
                padding: 1px 6px;
              "
            >
              Mới
            </span>
          </template>

          <template v-if="item.description" #description>
            <n-ellipsis :line-clamp="2" style="font-size: 12.5px; color: #666;">
              {{ item.description }}
            </n-ellipsis>
          </template>

          <template #footer>
            <span style="font-size: 11px; color: #bbb;">{{ item.date }}</span>
          </template>
        </n-thing>
      </n-list-item>
    </n-list>
  </n-scrollbar>
</template>