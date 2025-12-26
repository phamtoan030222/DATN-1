<script setup lang="ts">
import type { HoaDon } from '@/service/api/admin/banhang/banhang'

defineProps<{
  list: HoaDon[]
  activeId: number | null
}>()

defineEmits(['select', 'remove'])
</script>

<template>
  <n-card title="Hóa đơn đang chờ">
    <div class="scroll-x">
      <n-card
        v-for="hd in list"
        :key="hd.id"
        size="small"
        hoverable
        :class="{ active: activeId === hd.id }"
        @click="$emit('select', hd.id)"
        style="max-width: 180px;"
      >
        <n-button
          quaternary
          circle
          size="tiny"
          class="delete"
          @click.stop="$emit('remove', hd.id)"
        >
          ✕
        </n-button>

        <b>{{ hd.maHoaDon }}</b>
        <br/>
        <n-tag type="warning" size="small">Đang chờ</n-tag>
        <div>{{ hd.cart.length }} sản phẩm</div>
      </n-card>
    </div>
  </n-card>
</template>

<style scoped>
.scroll-x {
  display: flex;
  gap: 12px;
  overflow-x: auto;
}
.active {
  border: 1px solid #18a058;
}
.delete {
  position: absolute;
  right: 6px;
  top: 6px;
}
</style>
