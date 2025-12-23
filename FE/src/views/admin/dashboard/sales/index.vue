<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import {
  NAvatar,
  NButton,
  NCard,
  NEmpty,
  NInput,
  NInputNumber,
  NList,
  NListItem,
  NModal,
  NSelect,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
  NText,
  NThing,
  useMessage,
} from 'naive-ui'

// --- IMPORT API (Đảm bảo đường dẫn này đúng trong dự án của bạn) ---
import { getCustomers } from '@/service/api/admin/users/customer/customer'
import { router } from '@/router'

function navigateToAdd() {
  router.push('/users/customer/add')
}
// =============================================================================
// 1. CẤU HÌNH & STATE
// =============================================================================
const message = useMessage()

// State chung
const loading = ref(false)
const processing = ref(false)

// State Sản phẩm & Giỏ hàng
const searchQuery = ref('')
const products = ref<any[]>([])
const cart = ref<any[]>([])

// State Khách hàng
const selectedCustomer = ref<string | null>(null)
const customerOptions = ref<any[]>([]) // Lưu options cho NSelect
const selectLoading = ref(false)

// State Voucher
const showVoucherModal = ref(false)
const discount = ref(0)
const appliedVoucher = ref<any | null>(null)

// State Thanh toán
const payment = reactive({
  method: 0, // 0: Tiền mặt, 1: CK, 2: Kết hợp
  cash: 0,
  transfer: 0,
  note: '',
})

// =============================================================================
// 2. MOCK DATA (Dữ liệu giả lập cho SP và Voucher)
// =============================================================================
const mockProducts = [
  { id: '1', name: 'iPhone 15 Pro Max 256GB', code: 'SP001', price: 30000000, quantity: 10 },
  { id: '2', name: 'Sạc dự phòng Anker', code: 'PK002', price: 500000, quantity: 25 },
  { id: '3', name: 'MacBook Air M2', code: 'LT003', price: 28000000, quantity: 5 },
  { id: '4', name: 'Tai nghe AirPods Pro', code: 'PK003', price: 5500000, quantity: 15 },
  { id: '5', name: 'Samsung Galaxy S24', code: 'SP002', price: 26000000, quantity: 8 },
]

const mockVouchers = [
  { code: 'OPENING', name: 'Mừng khai trương', value: 50000, type: 'fixed', scope: 'public', desc: 'Giảm 50k đơn 0đ' },
  { code: 'SALE10', name: 'Siêu sale T10', value: 10, type: 'percent', scope: 'public', desc: 'Giảm 10% tối đa 100k' },
  { code: 'VIPMEMBER', name: 'Tri ân khách VIP', value: 200000, type: 'fixed', scope: 'private', desc: 'Dành riêng cho khách cũ' },
  { code: 'BDAY', name: 'Quà sinh nhật', value: 15, type: 'percent', scope: 'private', desc: 'Giảm 15% dịp sinh nhật' },
]

// =============================================================================
// 3. LOGIC KHÁCH HÀNG (QUAN TRỌNG - ĐÃ SỬA)
// =============================================================================

// Computed: Lấy thông tin khách hàng đang chọn để hiển thị Avatar bên ngoài
const currentCustomerInfo = computed(() => {
  return customerOptions.value.find(c => c.value === selectedCustomer.value)
})

/**
 * Hàm render hiển thị custom trong DROPDOWN (vẫn giữ ảnh đẹp khi xổ xuống)
 * Hiển thị: [Avatar] [Tên] [SĐT]
 */
function renderCustomerLabel(option: any) {
  return h(
    'div',
    { class: 'flex items-center gap-3 py-1' },
    [
      h(NAvatar, {
        src: option.avatar || 'https://www.svgrepo.com/show/452030/avatar-default.svg',
        round: true,
        size: 'small',
        fallbackSrc: 'https://www.svgrepo.com/show/452030/avatar-default.svg',
      }),
      h('div', { class: 'flex flex-col text-left' }, [
        h('span', { class: 'font-medium leading-tight text-gray-800' }, option.customerName),
        h('span', { class: 'text-xs text-gray-500' }, option.customerPhone || 'Không có SĐT'),
      ]),
    ],
  )
}

/**
 * Hàm gọi API tìm kiếm khách hàng
 * @param query Từ khóa tìm kiếm (nếu rỗng sẽ load mặc định)
 */
async function handleSearchCustomer(query: string) {
  selectLoading.value = true
  try {
    // Gọi API với keyword (cho phép rỗng)
    const res = await getCustomers({
      page: 1,
      size: 10, // Load 10 người đầu tiên
      keyword: query,
    })

    // --- FIX QUAN TRỌNG: Đào đúng dữ liệu JSON data.data.data ---
    const content = res.data?.data?.data || []

    // Map dữ liệu
    customerOptions.value = content.map((c: any) => ({
      value: c.id,

      // Label text hiển thị trong ô INPUT sau khi chọn (chỉ chữ cho gọn)
      label: `${c.customerName} (${c.customerPhone || 'Không SĐT'})`,

      // Các trường dữ liệu phụ trợ (để dùng cho avatar và dropdown)
      customerName: c.customerName,
      customerPhone: c.customerPhone,
      avatar: c.customerAvatar,
    }))
  }
  catch (e) {
    console.error('Lỗi tìm khách hàng:', e)
  }
  finally {
    selectLoading.value = false
  }
}

// =============================================================================
// 4. LOGIC SẢN PHẨM & GIỎ HÀNG
// =============================================================================

async function fetchProducts() {
  loading.value = true
  await new Promise(resolve => setTimeout(resolve, 300))

  if (searchQuery.value) {
    const key = searchQuery.value.toLowerCase()
    products.value = mockProducts.filter(p =>
      p.name.toLowerCase().includes(key) || p.code.toLowerCase().includes(key),
    )
  }
  else {
    products.value = mockProducts
  }
  loading.value = false
}

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
    })
  }
}

function removeFromCart(index: number) {
  cart.value.splice(index, 1)
}

// =============================================================================
// 5. LOGIC VOUCHER & THANH TOÁN
// =============================================================================

const publicVouchers = computed(() => mockVouchers.filter(v => v.scope === 'public'))
const privateVouchers = computed(() => mockVouchers.filter(v => v.scope === 'private'))

const totalAmount = computed(() => cart.value.reduce((sum, item) => sum + (item.price * item.quantity), 0))
const finalAmount = computed(() => {
  const final = totalAmount.value - discount.value
  return final < 0 ? 0 : final
})

function selectVoucher(voucher: any) {
  let calc = 0
  if (voucher.type === 'fixed') {
    calc = voucher.value
  }
  else if (voucher.type === 'percent') {
    calc = totalAmount.value * (voucher.value / 100)
    if (calc > 500000)
      calc = 500000
  }

  if (calc > totalAmount.value)
    calc = totalAmount.value

  discount.value = calc
  appliedVoucher.value = voucher
  showVoucherModal.value = false
  message.success(`Áp dụng: ${voucher.name}`)
}

function removeVoucher() {
  discount.value = 0
  appliedVoucher.value = null
}

watch(finalAmount, (newVal) => {
  if (payment.method === 0) { payment.cash = newVal; payment.transfer = 0 }
  else if (payment.method === 1) { payment.cash = 0; payment.transfer = newVal }
})

watch(totalAmount, () => {
  if (appliedVoucher.value) {
    selectVoucher(appliedVoucher.value)
  }
})

function onMethodChange(val: number) {
  payment.method = val
  if (val === 0) { payment.cash = finalAmount.value; payment.transfer = 0 }
  else if (val === 1) { payment.cash = 0; payment.transfer = finalAmount.value }
}

async function handleCheckout() {
  if (cart.value.length === 0)
    return message.warning('Giỏ hàng trống!')

  const totalPaid = (payment.cash || 0) + (payment.transfer || 0)
  if (totalPaid < finalAmount.value)
    return message.error('Khách trả thiếu tiền!')

  processing.value = true
  try {
    await new Promise(r => setTimeout(r, 1000))
    message.success('Thanh toán thành công!')
    cart.value = []
    payment.cash = 0; payment.transfer = 0; payment.note = ''
    selectedCustomer.value = null
    removeVoucher()
  }
  catch (e) {
    message.error('Lỗi thanh toán')
  }
  finally {
    processing.value = false
  }
}

// Khởi tạo
onMounted(() => {
  fetchProducts()
  // Load sẵn danh sách khách hàng ban đầu (để bấm vào là có luôn)
  handleSearchCustomer('')
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
            <NEmpty v-else description="Không tìm thấy sản phẩm" class="mt-20" />
          </NSpin>
        </div>
      </NCard>
    </div>

    <div class="w-[35%] flex flex-col h-full gap-3">
      <NCard size="small" class="shadow-sm rounded-lg">
        <div class="flex gap-3 items-center ">
          <NSelect
            v-model:value="selectedCustomer" filterable remote placeholder="Tìm khách hàng (F4)..."
            class="flex-1" clearable :options="customerOptions" :loading="selectLoading"
            :render-label="renderCustomerLabel" @search="handleSearchCustomer"
            @focus="() => handleSearchCustomer('')"
          />
          <NButton circle size="small" type="primary" ghost title="Thêm mới nhanh" @click="navigateToAdd">
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
                  {{ item.price.toLocaleString() }} x <input
                    v-model="item.quantity" type="number"
                    class="w-10 mx-1 text-center border rounded bg-gray-50 outline-none" min="1"
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
          <div class="flex justify-between items-center text-sm">
            <span class="text-gray-500">Tổng tiền hàng:</span>
            <span class="font-bold">{{ totalAmount.toLocaleString() }} đ</span>
          </div>

          <div
            class="flex justify-between items-center p-2 rounded cursor-pointer border border-transparent hover:bg-blue-50 hover:border-blue-100 group transition-all"
            @click="showVoucherModal = true"
          >
            <div class="flex items-center gap-2 text-blue-600 font-medium">
              <span>🎟 Sử dụng mã giảm giá</span>
              <NTag v-if="appliedVoucher" type="success" size="small" class="ml-2">
                {{ appliedVoucher.code }} (-{{ discount.toLocaleString() }})
              </NTag>
            </div>
            <div class="text-gray-400 group-hover:text-blue-500">
              >
            </div>
          </div>

          <div class="flex justify-between items-center pb-2 border-b border-dashed">
            <span class="text-gray-800 font-bold">Khách cần trả:</span>
            <span class="text-xl font-bold text-red-600">{{ finalAmount.toLocaleString() }} đ</span>
          </div>

          <div class="grid grid-cols-3 bg-gray-100 p-1 rounded">
            <div
              v-for="(label, val) in ['Tiền mặt', 'Chuyển khoản', 'Kết hợp']" :key="val"
              class="text-center text-xs py-1.5 cursor-pointer rounded transition-all"
              :class="payment.method === val ? 'bg-white shadow text-blue-600 font-bold' : 'text-gray-500 hover:bg-gray-200'"
              @click="onMethodChange(val)"
            >
              {{ label }}
            </div>
          </div>

          <div class="space-y-2">
            <div v-if="payment.method !== 1" class="flex items-center justify-between">
              <span class="text-xs text-gray-500 w-20">Tiền mặt:</span>
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
              <span class="text-xs text-gray-500 w-20">Chuyển khoản:</span>
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

    <NModal v-model:show="showVoucherModal" preset="card" title="Chọn Mã Giảm Giá" class="w-[500px]" :bordered="false">
      <NTabs type="segment" animated>
        <NTabPane name="public" tab="Công khai">
          <NList hoverable clickable>
            <NListItem v-for="v in publicVouchers" :key="v.code" @click="selectVoucher(v)">
              <NThing :title="v.name" content-style="margin-top: 4px;">
                <template #description>
                  <div class="flex gap-2">
                    <NTag type="info" size="small">
                      {{ v.code }}
                    </NTag><span class="text-xs text-gray-500 pt-1">{{ v.desc }}</span>
                  </div>
                </template>
                <template #header-extra>
                  <NButton size="small" type="primary" ghost>
                    Dùng
                  </NButton>
                </template>
              </NThing>
            </NListItem>
          </NList>
        </NTabPane>
        <NTabPane name="private" tab="Riêng tư (Của khách)">
          <div v-if="!selectedCustomer" class="text-center py-4 text-gray-400 italic">
            Vui lòng chọn khách hàng để xem voucher riêng
          </div>
          <NList v-else hoverable clickable>
            <div v-if="privateVouchers.length === 0" class="text-center py-4 text-gray-400">
              Khách này không có voucher riêng
            </div>
            <NListItem v-for="v in privateVouchers" v-else :key="v.code" @click="selectVoucher(v)">
              <NThing :title="v.name">
                <template #description>
                  <div class="flex gap-2">
                    <NTag type="warning" size="small">
                      {{ v.code }}
                    </NTag><span class="text-xs text-gray-500 pt-1">{{ v.desc }}</span>
                  </div>
                </template>
                <template #header-extra>
                  <NButton size="small" type="primary" ghost>
                    Dùng
                  </NButton>
                </template>
              </NThing>
            </NListItem>
          </NList>
        </NTabPane>
      </NTabs>
      <template #footer>
        <div class="flex justify-end">
          <NButton
            v-if="appliedVoucher" type="error" ghost size="small"
            @click="removeVoucher(), showVoucherModal = false"
          >
            Gỡ bỏ mã đang dùng
          </NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>
