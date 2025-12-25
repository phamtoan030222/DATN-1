<script setup lang="ts">
import HoaDonCustomer from './HoaDonCustomer.vue'
import HoaDonCart from './HoaDonCart.vue'
import HoaDonSummary from './HoaDonSummary.vue'
import HoaDonPayment from './HoaDonPayment.vue'
import type { HoaDon } from '@/service/api/admin/banhang/banhang'

defineProps<{ hoaDon: HoaDon | null }>()
</script>

<template>
  <n-empty v-if="!hoaDon" description="Chọn hóa đơn để bán" />

  <n-card v-else title="Chi tiết hóa đơn">
    <n-grid cols="3" x-gap="16">
      <n-gi>
        <HoaDonCustomer v-model="hoaDon.customer" />
      </n-gi>
      <n-gi span="2">
        <HoaDonCart v-model="hoaDon.cart" />
      </n-gi>
    </n-grid>

    <n-divider />

    <HoaDonSummary
      :cart="hoaDon.cart"
      v-model:discount="hoaDon.discount"
    />

    <HoaDonPayment :hoa-don="hoaDon" />
  </n-card>
</template>
