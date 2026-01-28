<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NEmpty,
  NIcon,
  NPopconfirm,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'
import {
  AddOutline,
  ArrowForward,
  BagCheckOutline,
  RemoveOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import {
  GetGioHang,
  giamSoLuong,
  tangSoLuong,
  xoaSP,
} from '@/service/api/client/banhang.api'
import { CartStore } from '@/utils/cartStore'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const updateLoading = ref(false)
const cartItems = ref<any[]>([])
const invoiceId = CartStore.getInvoiceId()

async function fetchCart() {
  if (!invoiceId)
    return
  loading.value = true
  try {
    const res = await GetGioHang(invoiceId)
    // Xử lý response
    if (Array.isArray(res)) {
      cartItems.value = res
    }
    else if ((res as any).data) {
      cartItems.value = Array.isArray((res as any).data) ? (res as any).data : []
    }
    else {
      cartItems.value = []
    }
  }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}

async function handleRemove(idHDCT: string) {
  try {
    await xoaSP({ idSP: idHDCT })
    message.success('Đã xóa sản phẩm')
    await fetchCart()
  }
  catch (e: any) {
    message.error(e.response?.data?.message || 'Lỗi xóa')
  }
}

async function handleIncrease(idHDCT: string) {
  if (updateLoading.value)
    return
  updateLoading.value = true
  try {
    await tangSoLuong(idHDCT)
    await fetchCart()
  }
  catch (e: any) {
    message.error(e.response?.data?.message || 'Hết hàng')
  }
  finally { updateLoading.value = false }
}

async function handleDecrease(idHDCT: string) {
  if (updateLoading.value)
    return
  updateLoading.value = true
  try {
    await giamSoLuong(idHDCT)
    await fetchCart()
  }
  catch (e: any) {
    message.error('Lỗi giảm số lượng')
  }
  finally { updateLoading.value = false }
}

const subTotal = computed(() => {
  return cartItems.value.reduce((total, item) => {
    const price = item.giaSauGiam || item.giaBan || 0
    return total + (price * item.soLuong)
  }, 0)
})

function formatCurrency(val: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

onMounted(() => { fetchCart() })
</script>

<template>
  <div class="container mx-auto px-4 py-8 max-w-4xl min-h-[60vh]">
    <h1 class="text-2xl font-bold mb-6 flex items-center gap-2 text-gray-800">
      <NIcon color="#d70018">
        <BagCheckOutline />
      </NIcon> Giỏ hàng của bạn
    </h1>

    <div v-if="loading" class="flex justify-center py-20">
      <NSpin size="large" />
    </div>

    <div v-else-if="!invoiceId || cartItems.length === 0" class="text-center py-16 bg-white rounded-lg shadow-sm border border-gray-100">
      <NEmpty description="Giỏ hàng đang trống" size="large">
        <template #extra>
          <NButton type="primary" color="#d70018" @click="router.push('/home')">
            TIẾP TỤC MUA SẮM
          </NButton>
        </template>
      </NEmpty>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2 space-y-4">
        <div v-for="item in cartItems" :key="item.id" class="flex gap-4 p-4 bg-white rounded-xl border border-gray-100 shadow-sm relative group hover:shadow-md transition-all">
          <img :src="item.anh || 'https://via.placeholder.com/100'" class="w-28 h-28 object-contain border rounded-lg p-2 bg-gray-50">

          <div class="flex-1 flex flex-col justify-between py-1">
            <div>
              <h3 class="font-bold text-gray-800 text-lg">
                {{ item.ten }}
              </h3>
              <div class="text-xs text-gray-500 mt-2 flex flex-wrap gap-2">
                <NTag size="small" :bordered="false" type="info">
                  {{ item.ram }}
                </NTag>
                <NTag size="small" :bordered="false" type="info">
                  {{ item.hardDrive }}
                </NTag>
                <NTag size="small" :bordered="false" type="warning">
                  {{ item.color }}
                </NTag>
              </div>
            </div>

            <div class="flex justify-between items-end mt-4">
              <div>
                <template v-if="item.giaSauGiam && item.giaSauGiam < item.giaBan">
                  <div class="text-gray-400 text-xs line-through">
                    {{ formatCurrency(item.giaBan) }}
                  </div>
                  <div class="text-red-600 font-bold text-xl">
                    {{ formatCurrency(item.giaSauGiam) }}
                  </div>
                </template>
                <template v-else>
                  <div class="text-red-600 font-bold text-xl">
                    {{ formatCurrency(item.giaBan) }}
                  </div>
                </template>
              </div>

              <div class="flex items-center gap-3">
                <NButton strong secondary circle size="small" :disabled="updateLoading" @click="handleDecrease(item.idHDCT)">
                  <template #icon>
                    <NIcon><RemoveOutline /></NIcon>
                  </template>
                </NButton>
                <span class="text-base font-bold min-w-[20px] text-center">{{ item.soLuong }}</span>
                <NButton strong secondary circle size="small" :disabled="updateLoading" @click="handleIncrease(item.idHDCT)">
                  <template #icon>
                    <NIcon><AddOutline /></NIcon>
                  </template>
                </NButton>
              </div>
            </div>
          </div>

          <NPopconfirm positive-text="Xóa" negative-text="Hủy" @positive-click="handleRemove(item.idHDCT)">
            <template #trigger>
              <button class="absolute top-3 right-3 text-black-300 hover:text-red-500 p-1 transition-colors rounded-full hover:bg-red-50">
                <NIcon size="20">
                  <TrashOutline />
                </NIcon>
              </button>
            </template>
            Xóa sản phẩm này khỏi giỏ?
          </NPopconfirm>
        </div>
      </div>

      <div class="lg:col-span-1">
        <div class="bg-white p-6 rounded-xl border shadow-sm sticky top-4">
          <div class="flex justify-between mb-4 text-gray-600">
            <span>Tạm tính:</span><span class="font-bold text-gray-800">{{ formatCurrency(subTotal) }}</span>
          </div>
          <div class="border-t pt-4">
            <div class="flex justify-between items-center mb-6">
              <span class="font-bold text-lg text-gray-800">Tổng tiền:</span>
              <span class="font-bold text-xl text-red-600">{{ formatCurrency(subTotal) }}</span>
            </div>
            <NButton block type="primary" color="#d70018" size="large" class="font-bold h-12 shadow-lg shadow-red-100" @click="router.push('/checkout')">
              THANH TOÁN <NIcon class="ml-2">
                <ArrowForward />
              </NIcon>
            </NButton>
            <NButton block class="mt-3" @click="router.push('/san-pham')">
              Chọn thêm sản phẩm khác
            </NButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
