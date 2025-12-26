<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMessage } from 'naive-ui'
import HoaDonChoList from './components/HoaDonChoList.vue'
import HoaDonWorkspace from './components/HoaDonWorkspace.vue'
import type { HoaDon } from './models'

const message = useMessage()

/* ================= STATE ================= */
const hoaDonList = ref<HoaDon[]>([])
const activeHoaDonId = ref<number | null>(null)

/* ================= COMPUTED ================= */
const activeHoaDon = computed(() =>
  hoaDonList.value.find(h => h.id === activeHoaDonId.value) || null
)

/* ================= ACTION ================= */
function createHoaDon() {
  if (hoaDonList.value.length >= 10) {
    message.warning('Tối đa 10 hóa đơn đang chờ')
    return
  }

  const newHd: HoaDon = {
    id: Date.now(),
    maHoaDon: `HD${hoaDonList.value.length + 1}`,
    status: 'CHO',
    customer: {
      name: '',
      phone: '',
      address: ''
    },
    cart: [],
    discount: 0
  }

  hoaDonList.value.push(newHd)
  activeHoaDonId.value = newHd.id
}

function removeHoaDon(id: number) {
  hoaDonList.value = hoaDonList.value.filter(h => h.id !== id)

  if (activeHoaDonId.value === id) {
    activeHoaDonId.value = hoaDonList.value[0]?.id || null
  }
}
</script>

<template>
  <n-space vertical size="large" class="p-4">

    <!-- ACTION: TẠO HÓA ĐƠN MỚI -->
    <n-card>
      <n-button type="primary" @click="createHoaDon">
        Tạo hóa đơn mới
      </n-button>
    </n-card>

    <!-- DANH SÁCH HÓA ĐƠN CHỜ -->
    <HoaDonChoList
      :list="hoaDonList"
      :active-id="activeHoaDonId"
      @select="activeHoaDonId = $event"
      @remove="removeHoaDon"
    />

    <!-- WORKSPACE HÓA ĐƠN -->
    <HoaDonWorkspace
      v-if="activeHoaDon"
      :key="activeHoaDon.id"
      :hoa-don="activeHoaDon"
    />
    
    <!-- EMPTY STATE -->
    <n-empty v-else description="Chọn hóa đơn để tiếp tục bán hàng" />
    
  </n-space>
</template>
