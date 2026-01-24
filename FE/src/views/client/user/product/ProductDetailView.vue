<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NDescriptions,
  NDescriptionsItem,
  NEmpty,
  NGrid,
  NGridItem,
  NIcon,
  NInputNumber,
  NModal,
  NRate,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'
import { ArrowBack, CartOutline, CheckmarkCircle, Flash, TicketOutline } from '@vicons/ionicons5'

// Import API
import { getProductDetailById } from '@/service/api/admin/product/productDetail.api'
import { getVouchers } from '@/service/api/admin/discount/api.voucher'
import type { ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'

// --- CẤU HÌNH ---
// 🔴 Đặt là true nếu Backend chưa trả về percentage mà bạn muốn thấy UI giảm giá ngay lập tức
const IS_TEST_MODE = false

const route = useRoute()
const router = useRouter()
const message = useMessage()

// --- STATE ---
const loading = ref(false)
const product = ref<any>(null)
const quantity = ref(1)
const selectedImage = ref('')

// Voucher & Timer
const showVoucherModal = ref(false)
const rawVoucherList = ref<ADVoucherResponse[]>([])
const selectedVoucher = ref<ADVoucherResponse | null>(null)
const timeLeft = ref('')
let timerInterval: any = null

const productId = route.params.id as string

// --- FETCH DATA ---
async function fetchData() {
  loading.value = true
  try {
    const res = await getProductDetailById(productId)
    if (res.data) {
      product.value = res.data

      // LOGIC TEST UI: Nếu bật Test Mode, tự động gán giảm giá 15%
      if (IS_TEST_MODE && (!product.value.percentage || product.value.percentage <= 0)) {
        console.warn('⚠️ Đang chạy chế độ Test Mode: Giả lập giảm giá 15%')
        product.value.percentage = 15
      }

      selectedImage.value = product.value.urlImage || 'https://via.placeholder.com/500'

      // Nếu có giảm giá -> Bật đồng hồ đếm ngược Flash Sale
      if (currentPercent.value > 0) {
        startCountdown()
      }

      // Tải voucher sau khi có thông tin sản phẩm
      fetchAvailableVouchers()
    }
  }
  catch (error) {
    message.error('Không thể tải thông tin sản phẩm')
  }
  finally {
    loading.value = false
  }
}

async function fetchAvailableVouchers() {
  try {
    const now = new Date().getTime()
    const res = await getVouchers({ page: 1, size: 50, status: 'ACTIVE' })

    // Lọc voucher còn hạn & còn số lượng
    rawVoucherList.value = res.content.filter((v) => {
      const isTimeValid = (!v.startDate || v.startDate <= now) && (!v.endDate || v.endDate >= now)
      const hasQuantity = v.remainingQuantity === null || (v.remainingQuantity > 0)
      return isTimeValid && hasQuantity
    })
  }
  catch (e) { console.error(e) }
}

// --- LOGIC TÍNH GIÁ (CORE) ---

// 1. Phần trăm giảm giá (Lấy từ API)
const currentPercent = computed(() => {
  return Number(product.value?.percentage) || 0
})

// 2. Giá Niêm Yết (Giá gốc lấy từ DB)
const listPrice = computed(() => {
  return Number(product.value?.price) || 0
})

// 3. Giá Bán (Sau khi trừ % Flash Sale)
const sellingPrice = computed(() => {
  const percent = currentPercent.value
  const original = listPrice.value

  // Nếu không giảm giá -> Giá bán = Giá niêm yết
  if (percent <= 0)
    return original

  // Công thức: Giá gốc * (100 - %)/100
  return original * (100 - percent) / 100
})

// 4. Tổng tiền hàng (Giá bán * Số lượng)
const currentOrderTotal = computed(() => sellingPrice.value * quantity.value)

// 5. Logic Voucher (Lọc & Sắp xếp)
const validVouchers = computed(() => {
  const total = currentOrderTotal.value

  // Lọc voucher thỏa mãn điều kiện đơn tối thiểu
  const available = rawVoucherList.value.filter(v => !v.conditions || total >= v.conditions)

  // Sắp xếp: Giảm nhiều tiền nhất lên đầu
  return available.sort((a, b) => calcDiscount(b, total) - calcDiscount(a, total))
})

// Hàm tính tiền giảm của 1 voucher cụ thể
function calcDiscount(v: ADVoucherResponse, total: number) {
  if (v.typeVoucher === 'FIXED_AMOUNT')
    return v.discountValue || 0

  // Tính theo %
  let disc = (total * (v.discountValue || 0)) / 100
  // Kiểm tra trần giảm tối đa (maxValue)
  if (v.maxValue && disc > v.maxValue)
    disc = v.maxValue
  return disc
}

// Tiền giảm giá của Voucher ĐANG CHỌN
const voucherDiscountAmount = computed(() => {
  if (!selectedVoucher.value)
    return 0

  // Check lại điều kiện phòng khi user giảm số lượng
  const total = currentOrderTotal.value
  if (selectedVoucher.value.conditions && total < selectedVoucher.value.conditions) {
    return 0 // Không đủ điều kiện nữa
  }

  return calcDiscount(selectedVoucher.value, total)
})

// 6. TỔNG THANH TOÁN CUỐI CÙNG
const finalTotalPrice = computed(() => {
  const total = currentOrderTotal.value - voucherDiscountAmount.value
  return total > 0 ? total : 0
})

// --- UTILS ---
function startCountdown() {
  // Giả lập Flash Sale kết thúc vào 23:59:59 hôm nay
  const endOfDay = new Date()
  endOfDay.setHours(23, 59, 59, 999)
  const endTime = endOfDay.getTime()

  timerInterval = setInterval(() => {
    const now = new Date().getTime()
    const distance = endTime - now
    if (distance < 0) { timeLeft.value = '00:00:00'; clearInterval(timerInterval); return }
    const h = Math.floor((distance % (86400000)) / 3600000)
    const m = Math.floor((distance % 3600000) / 60000)
    const s = Math.floor((distance % 60000) / 1000)
    const f = (n: number) => n < 10 ? `0${n}` : n
    timeLeft.value = `${f(h)}:${f(m)}:${f(s)}`
  }, 1000)
}
onUnmounted(() => {
  if (timerInterval)
    clearInterval(timerInterval)
})

function formatCurrency(val: number | undefined) {
  if (val === undefined || val === null)
    return '0₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

// --- ACTIONS ---
function handleSelectVoucher(v: ADVoucherResponse) {
  if (selectedVoucher.value?.id === v.id) {
    selectedVoucher.value = null
  }
  else { selectedVoucher.value = v; showVoucherModal.value = false; message.success('Đã áp dụng voucher') }
}
function handleBuyNow() { message.success(`Mua ngay: ${formatCurrency(finalTotalPrice.value)}`) }
function handleAddToCart() { message.success(`Đã thêm vào giỏ hàng`) }

onMounted(() => fetchData())
</script>

<template>
  <div class="detail-container">
    <div v-if="loading" class="flex justify-center py-20">
      <NSpin size="large" />
    </div>

    <div v-else-if="product" class="main-content">
      <div class="mb-4">
        <NButton text @click="router.back()">
          <template #icon>
            <NIcon><ArrowBack /></NIcon>
          </template> Quay lại
        </NButton>
      </div>

      <NGrid x-gap="24" cols="1 m:2" responsive="screen">
        <NGridItem>
          <div class="gallery-box relative">
            <div class="main-image-wrapper border border-gray-100 rounded-lg overflow-hidden bg-white">
              <img :src="selectedImage" class="w-full h-full object-contain p-4 transition-transform hover:scale-105" @error="selectedImage = 'https://via.placeholder.com/500'">

              <div v-if="currentPercent > 0" class="absolute top-4 left-4 bg-red-600 text-white font-bold px-3 py-1 rounded shadow-md z-10 animate-bounce-slow">
                -{{ currentPercent }}%
              </div>
            </div>
          </div>
        </NGridItem>

        <NGridItem>
          <div class="info-box">
            <h1 class="text-2xl font-bold text-gray-800 mb-2 leading-tight">
              {{ product.productName || product.name }}
            </h1>

            <div class="flex items-center gap-2 mb-4 text-sm text-gray-500">
              <NRate readonly :default-value="5" size="small" />
              <span>(Mã: {{ product.code }})</span>
              <span>|</span>
              <span class="text-green-600 font-medium">Còn hàng</span>
            </div>

            <div v-if="currentPercent > 0" class="flash-sale-bar bg-gradient-to-r from-red-600 to-orange-500 text-white p-3 rounded-t-lg flex justify-between items-center shadow-md select-none">
              <div class="flex items-center gap-2">
                <NIcon size="24" class="animate-pulse">
                  <Flash />
                </NIcon>
                <span class="font-bold text-lg uppercase tracking-wide">FLASH SALE</span>
              </div>
              <div class="flex items-center gap-2 bg-black/20 px-3 py-1 rounded backdrop-blur-sm">
                <span class="text-xs opacity-90">Kết thúc trong:</span>
                <span class="font-mono font-bold text-lg text-yellow-300">{{ timeLeft }}</span>
              </div>
            </div>

            <div
              class="price-section bg-gray-50 p-5 border border-gray-200 mb-6 transition-all"
              :class="{ 'rounded-b-lg border-t-0': currentPercent > 0, 'rounded-lg': currentPercent <= 0 }"
            >
              <div v-if="currentPercent > 0" class="flex items-center gap-2 mb-1">
                <span class="text-gray-400 text-sm">Giá niêm yết:</span>
                <span class="text-gray-400 text-lg line-through font-medium">{{ formatCurrency(listPrice) }}</span>
                <span class="text-red-600 text-xs font-bold bg-red-100 px-2 py-0.5 rounded-full">-{{ currentPercent }}%</span>
              </div>

              <div class="flex items-baseline gap-2">
                <span class="text-4xl font-bold text-red-600 tracking-tight">{{ formatCurrency(sellingPrice) }}</span>
                <span v-if="currentPercent <= 0" class="text-xs text-gray-500">(Giá đã bao gồm VAT)</span>
              </div>
            </div>

            <div class="voucher-section mb-6 border border-dashed border-red-300 bg-red-50 p-4 rounded-lg hover:bg-red-50/80 transition-colors">
              <div class="flex justify-between items-center">
                <div class="flex items-center gap-2 text-red-700 font-bold">
                  <NIcon size="20">
                    <TicketOutline />
                  </NIcon> Mã giảm thêm
                </div>
                <NButton size="small" type="error" ghost @click="showVoucherModal = true">
                  {{ selectedVoucher ? 'Đổi mã khác' : 'Chọn mã giảm giá' }}
                </NButton>
              </div>

              <div v-if="selectedVoucher" class="mt-3 bg-white border border-red-200 p-3 rounded flex justify-between items-center shadow-sm">
                <div>
                  <div class="font-bold text-red-600">
                    {{ selectedVoucher.code }}
                  </div>
                  <div class="text-sm text-gray-600">
                    Đã giảm thêm: <span class="font-bold text-green-600">-{{ formatCurrency(voucherDiscountAmount) }}</span>
                  </div>
                </div>
                <NIcon color="green" size="24">
                  <CheckmarkCircle />
                </NIcon>
              </div>
              <div v-else class="mt-2 text-xs text-gray-500">
                Nhập mã để được giảm thêm trên giá đã khuyến mãi
              </div>
            </div>

            <div class="actions border-t pt-6">
              <div class="flex items-center justify-between mb-6">
                <div class="flex items-center gap-4">
                  <span class="font-medium text-gray-700">Số lượng:</span>
                  <NInputNumber v-model:value="quantity" :min="1" :max="10" button-placement="both" class="w-32" />
                </div>
                <div class="text-right">
                  <div class="text-xs text-gray-500 mb-1">
                    Tổng thanh toán:
                  </div>
                  <div class="text-2xl font-bold text-red-600">
                    {{ formatCurrency(finalTotalPrice) }}
                  </div>
                </div>
              </div>

              <div class="flex gap-4 h-12">
                <NButton type="primary" class="flex-1 h-full text-lg font-bold shadow-lg shadow-green-200 hover:-translate-y-0.5 transition-transform" color="#049d00" @click="handleBuyNow">
                  MUA NGAY
                </NButton>
                <NButton strong secondary type="info" class="flex-1 h-full text-lg font-bold hover:-translate-y-0.5 transition-transform" @click="handleAddToCart">
                  <template #icon>
                    <NIcon><CartOutline /></NIcon>
                  </template>
                  THÊM GIỎ
                </NButton>
              </div>
            </div>

            <div class="mt-10">
              <h3 class="font-bold mb-4 border-l-4 border-red-600 pl-3 text-lg text-gray-800">
                Thông số cấu hình
              </h3>
              <NDescriptions bordered label-placement="left" size="small" column="1" label-style="width: 120px; font-weight: 600; background-color: #f9fafb;">
                <NDescriptionsItem label="CPU">
                  {{ product.cpuName || product.cpu || product.idCPU || 'Đang cập nhật' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="RAM">
                  {{ product.ramName || product.ram || product.idRAM || 'Đang cập nhật' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="Ổ cứng">
                  {{ product.hardDriveName || product.hardDrive || product.idHardDrive || 'Đang cập nhật' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="Màn hình">
                  {{ product.screenName || product.screen || product.idScreen || 'Đang cập nhật' }}
                </NDescriptionsItem>
                <NDescriptionsItem label="Màu sắc">
                  {{ product.colorName || product.color || product.idColor || 'Đang cập nhật' }}
                </NDescriptionsItem>
              </NDescriptions>
            </div>
          </div>
        </NGridItem>
      </NGrid>
    </div>

    <NModal v-model:show="showVoucherModal" preset="card" title="Mã Giảm Giá Khả Dụng" style="width: 500px">
      <div v-if="validVouchers.length === 0" class="text-center py-8">
        <NEmpty description="Tiếc quá! Chưa có mã giảm giá nào phù hợp" />
        <p class="text-gray-400 text-xs mt-2">
          Thử tăng số lượng sản phẩm xem sao?
        </p>
      </div>
      <div v-else class="space-y-3 p-1 max-h-[400px] overflow-y-auto">
        <div
          v-for="(v, index) in validVouchers" :key="v.id"
          class="border rounded-lg p-3 cursor-pointer transition-all hover:shadow-md relative bg-white group"
          :class="{ 'border-red-500 bg-red-50 ring-1 ring-red-200': selectedVoucher?.id === v.id, 'border-gray-200': selectedVoucher?.id !== v.id }"
          @click="handleSelectVoucher(v)"
        >
          <div v-if="index === 0" class="absolute -top-2 -right-2 bg-orange-500 text-white text-[10px] px-2 py-0.5 rounded-full shadow-sm z-10 font-bold">
            GIẢM NHIỀU NHẤT
          </div>

          <div class="flex justify-between items-start">
            <div class="flex-1">
              <div class="flex items-center gap-2">
                <span class="font-bold text-lg text-red-600 group-hover:text-red-700">{{ v.code }}</span>
                <NTag size="tiny" type="warning" bordered>
                  {{ v.typeVoucher === 'PERCENTAGE' ? 'Giảm %' : 'Giảm tiền' }}
                </NTag>
              </div>
              <div class="text-sm font-medium text-gray-700 mt-1">
                {{ v.name }}
              </div>

              <div class="mt-2 text-xs text-gray-500 bg-gray-50 p-2 rounded inline-block">
                <div>• Giảm: {{ v.typeVoucher === 'PERCENTAGE' ? `${v.discountValue}%` : formatCurrency(v.discountValue || 0) }}</div>
                <div v-if="v.maxValue">
                  • Tối đa: {{ formatCurrency(v.maxValue) }}
                </div>
                <div>• Đơn tối thiểu: {{ formatCurrency(v.conditions || 0) }}</div>
              </div>

              <div class="mt-2 text-sm font-bold text-green-600 flex items-center gap-1">
                <NIcon><CheckmarkCircle /></NIcon>
                Áp dụng: -{{ formatCurrency(calcDiscount(v, currentOrderTotal)) }}
              </div>
            </div>

            <div class="flex items-center justify-center h-full pl-3">
              <div
                class="w-5 h-5 rounded-full border border-gray-300 flex items-center justify-center"
                :class="{ 'bg-red-500 border-red-500': selectedVoucher?.id === v.id }"
              >
                <NIcon v-if="selectedVoucher?.id === v.id" color="white" size="14">
                  <CheckmarkCircle />
                </NIcon>
              </div>
            </div>
          </div>
        </div>
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.detail-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 30px 20px;
  background-color: #fff;
  min-height: 80vh;
}
.main-image-wrapper {
  position: relative;
  padding-top: 75%; /* Aspect Ratio 4:3 */
}
.main-image-wrapper img {
  position: absolute;
  top: 0; left: 0;
}
.animate-bounce-slow {
    animation: bounce 2s infinite;
}
@keyframes bounce {
  0%, 100% { transform: translateY(-5%); }
  50% { transform: translateY(0); }
}
</style>
