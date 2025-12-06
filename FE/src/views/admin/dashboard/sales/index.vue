<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
// Import các component từ Naive UI
import {
  NAvatar,
  NButton,
  NCard,
  NEmpty,
  NInput,
  NInputNumber,
  NSelect,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'

import { useAuthStore } from '@/store/auth'

const message = useMessage()
const authStore = useAuthStore()

// --- STATE ---
const loading = ref(false)
const processing = ref(false)
const searchQuery = ref('')
const products = ref<any[]>([])
const cart = ref<any[]>([])
const selectedCustomer = ref<string | null>(null)

// Dữ liệu thanh toán
const payment = reactive({
  method: 0, // 0: Tiền mặt, 1: Chuyển khoản
  cash: 0,
  transfer: 0,
  note: '',
})

// --- MOCK DATA (Dữ liệu giả để test giao diện) ---
const mockData = [
  { id: '1', name: 'iPhone 15 Pro Max 256GB', code: 'SP001', price: 30000000, quantity: 10 },
  { id: '2', name: 'Sạc dự phòng Anker', code: 'PK002', price: 500000, quantity: 25 },
  { id: '3', name: 'MacBook Air M2', code: 'LT003', price: 28000000, quantity: 5 },
  { id: '4', name: 'Tai nghe AirPods Pro', code: 'PK003', price: 5500000, quantity: 15 },
  { id: '5', name: 'Samsung Galaxy S24', code: 'SP002', price: 26000000, quantity: 8 },
]

// --- ACTIONS ---

// 1. Lấy danh sách sản phẩm
async function fetchProducts() {
  loading.value = true
  try {
    // Giả lập độ trễ mạng 0.5s
    await new Promise(resolve => setTimeout(resolve, 500))

    if (searchQuery.value) {
      const key = searchQuery.value.toLowerCase()
      products.value = mockData.filter(p =>
        p.name.toLowerCase().includes(key) || p.code.toLowerCase().includes(key),
      )
    }
    else {
      products.value = mockData
    }
  }
  catch (e) {
    message.error('Lỗi tải dữ liệu')
  }
  finally {
    loading.value = false
  }
}

// 2. Thêm vào giỏ hàng
function addToCart(product: any) {
  const index = cart.value.findIndex(item => item.productDetailId === product.id)

  if (index !== -1) {
    if (cart.value[index].quantity >= product.quantity) {
      message.warning('Đã đạt giới hạn tồn kho!')
      return
    }
    cart.value[index].quantity++
  }
  else {
    cart.value.push({
      productDetailId: product.id,
      name: product.name,
      code: product.code,
      price: product.price,
      quantity: 1,
      maxQuantity: product.quantity,
    })
  }

  // Nếu đang chọn tiền mặt thì tự động điền số tiền
  if (payment.method === 0) {
    payment.cash = totalAmount.value
  }
}

// 3. Xóa khỏi giỏ
function removeFromCart(index: number) {
  cart.value.splice(index, 1)
}

// --- COMPUTED & WATCHERS ---

const totalAmount = computed(() => {
  return cart.value.reduce((sum, item) => sum + (item.price * item.quantity), 0)
})

// Khi đổi phương thức thanh toán
function onMethodChange(val: number) {
  payment.method = val
  if (val === 0) { // Tiền mặt -> Tự điền tiền mặt
    payment.cash = totalAmount.value
    payment.transfer = 0
  }
  else if (val === 1) { // Chuyển khoản -> Tự điền CK
    payment.cash = 0
    payment.transfer = totalAmount.value
  }
  // Kết hợp -> Để 0 cho tự nhập
}

// 4. Thanh toán (Giả lập)
async function handleCheckout() {
  if (cart.value.length === 0) {
    message.warning('Giỏ hàng đang trống!')
    return
  }

  const totalPaid = (payment.cash || 0) + (payment.transfer || 0)
  if (totalPaid < totalAmount.value) {
    message.error(`Khách trả thiếu ${(totalAmount.value - totalPaid).toLocaleString()} đ`)
    return
  }

  try {
    processing.value = true
    // Giả lập gọi API mất 1s
    await new Promise(resolve => setTimeout(resolve, 1000))

    message.success('Thanh toán thành công (Demo)!')

    // Reset sau khi bán
    cart.value = []
    payment.cash = 0
    payment.transfer = 0
    payment.note = ''
    selectedCustomer.value = null
  }
  catch (error) {
    message.error('Có lỗi xảy ra')
  }
  finally {
    processing.value = false
  }
}

// Chạy khi component được load
onMounted(() => {
  fetchProducts()
})
</script>

<template>
  <div class="flex h-[calc(100vh-110px)] gap-3 p-2 bg-slate-100">
    <div class="w-[65%] flex flex-col h-full">
      <NCard
        size="small" class="h-full flex flex-col shadow-sm rounded-lg"
        content-style="padding: 10px; display: flex; flex-direction: column; height: 100%;"
      >
        <div class="flex gap-2 mb-3">
          <NInput
            v-model:value="searchQuery" size="large" placeholder="🔍 Tìm kiếm sản phẩm (F2)..."
            @keyup.enter="fetchProducts"
          />
          <NButton type="primary" size="large" @click="fetchProducts">
            Tìm
          </NButton>
        </div>

        <div class="flex-1 overflow-y-auto pr-1">
          <NSpin :show="loading">
            <div v-if="products.length > 0" class="grid grid-cols-3 gap-3">
              <div
                v-for="prod in products" :key="prod.id"
                class="bg-white border hover:border-blue-500 cursor-pointer p-3 rounded-lg shadow-sm transition-all hover:shadow-md flex flex-col h-[140px] justify-between relative group"
                @click="addToCart(prod)"
              >
                <div class="absolute top-2 right-2 bg-gray-100 text-xs px-2 py-1 rounded-full text-gray-600">
                  SL: {{ prod.quantity }}
                </div>

                <div>
                  <div class="font-bold text-gray-800 line-clamp-2 text-sm mb-1">
                    {{ prod.name }}
                  </div>
                  <div class="text-xs text-gray-400 bg-gray-50 inline-block px-1 rounded">
                    {{ prod.code }}
                  </div>
                </div>

                <div class="flex justify-between items-end">
                  <div class="text-red-600 font-bold text-lg">
                    {{ prod.price.toLocaleString() }} đ
                  </div>
                  <NButton
                    size="tiny" type="primary" ghost
                    class="opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    + Thêm
                  </NButton>
                </div>
              </div>
            </div>
            <NEmpty v-else description="Không tìm thấy sản phẩm nào" class="mt-20" />
          </NSpin>
        </div>
      </NCard>
    </div>

    <div class="w-[35%] flex flex-col h-full gap-3">
      <NCard size="small" class="shadow-sm rounded-lg">
        <div class="flex gap-2 items-center">
          <NAvatar round size="small" src="https://ui-avatars.com/api/?name=Guest&background=random" />
          <NSelect
            v-model:value="selectedCustomer" filterable placeholder="Khách lẻ (hoặc tìm sđt...)" class="flex-1"
            clearable :options="[]"
          />
          <NButton circle size="small" type="primary" ghost>
            <template #icon>
              +
            </template>
          </NButton>
        </div>
      </NCard>

      <NCard
        size="small" title="Giỏ hàng" class="flex-1 shadow-sm rounded-lg flex flex-col"
        content-style="flex: 1; overflow: hidden; display: flex; flex-direction: column; padding: 0;"
      >
        <template #header-extra>
          <NTag type="success" size="small">
            {{ cart.length }} SP
          </NTag>
        </template>

        <div class="flex-1 overflow-y-auto p-2 bg-gray-50">
          <div v-if="cart.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400">
            <div class="text-4xl mb-2">
              🛒
            </div>
            <div>Chưa có sản phẩm</div>
          </div>

          <div v-else class="flex flex-col gap-2">
            <div
              v-for="(item, idx) in cart" :key="idx"
              class="bg-white p-2 rounded border border-gray-100 flex justify-between items-center group hover:shadow-sm"
            >
              <div class="flex-1">
                <div class="font-medium text-sm text-gray-800 line-clamp-1">
                  {{ item.name }}
                </div>
                <div class="flex items-center text-xs text-gray-500 mt-1">
                  {{ item.price.toLocaleString() }} x
                  <input
                    v-model="item.quantity" type="number"
                    class="w-10 mx-1 text-center border rounded bg-gray-50 focus:outline-blue-500" min="1"
                  >
                </div>
              </div>
              <div class="text-right">
                <div class="font-bold text-red-600 text-sm">
                  {{ (item.price * item.quantity).toLocaleString() }}
                </div>
                <div
                  class="text-xs text-gray-300 hover:text-red-500 cursor-pointer mt-1 underline"
                  @click="removeFromCart(idx)"
                >
                  Xóa
                </div>
              </div>
            </div>
          </div>
        </div>
      </NCard>

      <NCard size="small" class="shadow-sm rounded-lg mt-auto">
        <div class="space-y-3">
          <div class="flex justify-between items-center pb-2 border-b border-dashed">
            <span class="text-gray-500 font-medium">Tổng tiền hàng:</span>
            <span class="text-xl font-bold text-red-600">{{ totalAmount.toLocaleString() }} đ</span>
          </div>

          <div class="grid grid-cols-3 bg-gray-100 p-1 rounded">
            <div
              v-for="(label, val) in ['Tiền mặt', 'Chuyển khoản', 'Kết hợp']" :key="val" class="text-center text-xs py-1.5 cursor-pointer rounded transition-all"
              :class="payment.method === val ? 'bg-white shadow text-blue-600 font-bold' : 'text-gray-500 hover:bg-gray-200'"
              @click="onMethodChange(val)"
            >
              {{ label }}
            </div>
          </div>

          <div class="space-y-2">
            <div v-if="payment.method !== 1" class="flex items-center justify-between">
              <span class="text-xs text-gray-500 w-20">Khách đưa:</span>
              <NInputNumber
                v-model:value="payment.cash" class="flex-1" size="small" :show-button="false"
                placeholder="0" :min="0"
              >
                <template #suffix>
                  ₫
                </template>
              </NInputNumber>
            </div>
            <div v-if="payment.method !== 0" class="flex items-center justify-between">
              <span class="text-xs text-gray-500 w-20">CK:</span>
              <NInputNumber
                v-model:value="payment.transfer" class="flex-1" size="small" :show-button="false"
                placeholder="0" :min="0"
              >
                <template #suffix>
                  ₫
                </template>
              </NInputNumber>
            </div>
          </div>

          <NInput
            v-model:value="payment.note" type="textarea" placeholder="Ghi chú đơn hàng..." :rows="1"
            size="small"
          />

          <NButton
            type="primary" class="w-full h-10 text-base font-bold shadow-md shadow-blue-200"
            :loading="processing" @click="handleCheckout"
          >
            THANH TOÁN (F9)
          </NButton>
        </div>
      </NCard>
    </div>
  </div>
</template>
