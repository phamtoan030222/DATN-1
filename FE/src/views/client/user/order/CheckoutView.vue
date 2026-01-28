<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NDivider,
  NGi,
  NGrid,
  NIcon,
  NInput,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NTag,
  useMessage,
} from 'naive-ui'
import { CardOutline, CashOutline, CheckmarkCircle, LocationOutline, StorefrontOutline } from '@vicons/ionicons5'
import {
  GetGioHang,
  getMaGiamGia,
  thanhToanThanhCong,
} from '@/service/api/client/banhang.api'
import { CartStore } from '@/utils/cartStore'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
const processing = ref(false)

const invoiceId = CartStore.getInvoiceId()
const cartItems = ref<any[]>([])

// Form
const deliveryType = ref<'GIAO_HANG' | 'TAI_QUAY'>('GIAO_HANG')
const paymentMethod = ref('0')
const customerInfo = ref({
  ten: '',
  sdt: '',
  diaChi: '',
  ghiChu: '',
})

// Voucher
const selectedVoucher = ref<string | null>(null)
const availableVouchers = ref<any[]>([])

onMounted(async () => {
  if (!invoiceId) {
    message.error('Giỏ hàng trống')
    router.push('/')
    return
  }
  await loadCart()
})

// --- 1. SỬA LOGIC LOAD GIỎ HÀNG (QUAN TRỌNG) ---
async function loadCart() {
  loading.value = true
  try {
    const res = await GetGioHang(invoiceId!)

    // 👇 CHECK KỸ CẤU TRÚC DỮ LIỆU TRẢ VỀ 👇
    if (Array.isArray(res)) {
      cartItems.value = res
    }
    else if (res && (res as any).data && Array.isArray((res as any).data)) {
      cartItems.value = (res as any).data
    }
    else if (res && (res as any).content && Array.isArray((res as any).content)) {
      // Trường hợp trả về Page
      cartItems.value = (res as any).content
    }
    else {
      cartItems.value = []
    }

    // Nếu giỏ hàng có đồ thì mới load Voucher
    if (cartItems.value.length > 0) {
      await loadVouchers()
    }
    else {
      router.push('/cart')
    }
  }
  catch (e) {
    console.error('Lỗi load giỏ hàng:', e)
  }
  finally {
    loading.value = false
  }
}

// Tính tổng tiền hàng (Dùng giá sau giảm nếu có)
const subTotal = computed(() => {
  return cartItems.value.reduce((total, item) => {
    const price = item.giaSauGiam || item.giaBan || 0
    return total + (price * item.soLuong)
  }, 0)
})

// --- 2. SỬA LOGIC LOAD VOUCHER ---
async function loadVouchers() {
  // Tính tổng tiền thủ công để đảm bảo số liệu mới nhất
  const currentTotal = cartItems.value.reduce((t, i) => t + ((i.giaSauGiam || i.giaBan) * i.soLuong), 0)

  if (currentTotal <= 0)
    return

  try {
    const res = await getMaGiamGia({
      invoiceId: invoiceId!,
      tongTien: currentTotal,
      customerId: '',
    })

    // Check cấu trúc response từ API goi-y
    // Backend trả về: { voucherApDung: [...], voucherTotHon: [...] }
    const data = (res as any).data || res // Axios có thể bọc trong data hoặc không tùy config

    if (data && data.voucherApDung) {
      availableVouchers.value = data.voucherApDung

      // Tự động chọn voucher tốt nhất nếu chưa chọn
      if (availableVouchers.value.length > 0 && !selectedVoucher.value) {
        selectedVoucher.value = availableVouchers.value[0].voucherId
      }
    }
  }
  catch (e) {
    console.error('Lỗi load voucher', e)
  }
}

// Watch subTotal để reload voucher khi giá thay đổi (phòng hờ)
watch(subTotal, (newVal) => {
  if (newVal > 0)
    loadVouchers()
})

const discountAmount = computed(() => {
  if (!selectedVoucher.value)
    return 0
  const v = availableVouchers.value.find(x => x.voucherId === selectedVoucher.value)
  return v ? v.giamGiaThucTe : 0
})

const shippingFee = computed(() => deliveryType.value === 'GIAO_HANG' ? 30000 : 0)
const finalTotal = computed(() => Math.max(0, subTotal.value - discountAmount.value + shippingFee.value))

const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)

// --- 3. XỬ LÝ ĐẶT HÀNG ---
async function handleCheckout() {
  // Validate
  if (!customerInfo.value.ten || !customerInfo.value.sdt) {
    return message.warning('Vui lòng nhập Họ tên và Số điện thoại')
  }
  if (deliveryType.value === 'GIAO_HANG' && !customerInfo.value.diaChi) {
    return message.warning('Vui lòng nhập địa chỉ giao hàng')
  }

  processing.value = true
  try {
    const payload = {
      idHD: invoiceId!,
      idNV: null,
      ten: customerInfo.value.ten,
      sdt: customerInfo.value.sdt,
      diaChi: customerInfo.value.diaChi,
      tongTien: finalTotal.value.toString(),
      tienHang: subTotal.value,
      tienShip: shippingFee.value,
      giamGia: discountAmount.value,
      phuongThucThanhToan: paymentMethod.value,
      idPGG: selectedVoucher.value || null,
      loaiHoaDon: deliveryType.value,
      check: 0, // Gửi 0 để thanh toán luôn
    }

    const res = await thanhToanThanhCong(payload)

    // Kiểm tra lỏng lẻo hơn để bắt các trường hợp thành công
    // Backend trả về ResponseObject, axios trả về object
    const status = res?.status || (res as any)?.code

    if (status === 200 || status === 201 || status === 'OK' || status === 'CREATED') {
      message.success('Đặt hàng thành công!')
      CartStore.clearCart()
      router.push('/order-success')
    }
    else {
      message.error((res as any)?.message || 'Có lỗi xảy ra, vui lòng thử lại')
    }
  }
  catch (error: any) {
    console.error('Lỗi checkout:', error)
    const msg = error.response?.data?.message || 'Đặt hàng thất bại'
    message.error(msg)
  }
  finally {
    processing.value = false
  }
}
</script>

<template>
  <div class="bg-gray-50 min-h-screen py-8 font-sans">
    <div class="container mx-auto px-4 max-w-6xl">
      <h1 class="text-2xl font-bold mb-6 text-gray-800 border-l-4 border-red-600 pl-3">
        Thanh toán đơn hàng
      </h1>

      <NGrid x-gap="24" cols="1 l:3" responsive="screen">
        <NGi span="2">
          <div class="space-y-6">
            <NCard title="1. Thông tin nhận hàng" size="small" class="shadow-sm">
              <div class="flex gap-4 mb-4">
                <div
                  class="flex-1 p-3 border rounded cursor-pointer flex items-center justify-center gap-2 transition-all"
                  :class="deliveryType === 'GIAO_HANG' ? 'border-red-500 bg-red-50 text-red-700 font-bold ring-1 ring-red-500' : 'hover:bg-gray-50'"
                  @click="deliveryType = 'GIAO_HANG'">
                  <NIcon>
                    <LocationOutline />
                  </NIcon> Giao tận nơi
                </div>
                <div
                  class="flex-1 p-3 border rounded cursor-pointer flex items-center justify-center gap-2 transition-all"
                  :class="deliveryType === 'TAI_QUAY' ? 'border-blue-500 bg-blue-50 text-blue-700 font-bold ring-1 ring-blue-500' : 'hover:bg-gray-50'"
                  @click="deliveryType = 'TAI_QUAY'">
                  <NIcon>
                    <StorefrontOutline />
                  </NIcon> Nhận tại cửa hàng
                </div>
              </div>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <NInput v-model:value="customerInfo.ten" placeholder="Họ và tên người nhận (*)" />
                <NInput v-model:value="customerInfo.sdt" placeholder="Số điện thoại (*)" />
                <NInput v-if="deliveryType === 'GIAO_HANG'" v-model:value="customerInfo.diaChi"
                  placeholder="Địa chỉ chi tiết (Số nhà, đường...)" class="md:col-span-2" />
                <NInput v-model:value="customerInfo.ghiChu" type="textarea" placeholder="Ghi chú đơn hàng"
                  class="md:col-span-2" />
              </div>
            </NCard>

            <NCard title="2. Phương thức thanh toán" size="small" class="shadow-sm">
              <NRadioGroup v-model:value="paymentMethod" name="payment" class="w-full">
                <div class="space-y-3">
                  <div class="border rounded p-3 cursor-pointer hover:bg-gray-50" @click="paymentMethod = '0'">
                    <NRadio value="0" class="w-full">
                      <div class="flex items-center gap-2">
                        <NIcon color="#16a34a">
                          <CashOutline />
                        </NIcon> Thanh toán khi nhận hàng (COD)
                      </div>
                    </NRadio>
                  </div>
                  <div class="border rounded p-3 cursor-pointer hover:bg-gray-50" @click="paymentMethod = '1'">
                    <NRadio value="1" class="w-full">
                      <div class="flex items-center gap-2">
                        <NIcon color="#2563eb">
                          <CardOutline />
                        </NIcon> Chuyển khoản ngân hàng
                      </div>
                    </NRadio>
                  </div>
                </div>
              </NRadioGroup>
            </NCard>
          </div>
        </NGi>

        <NGi>
          <div class="bg-white p-5 rounded-lg shadow-md border sticky top-4">
            <h3 class="font-bold text-lg mb-4 pb-2 border-b">
              Đơn hàng ({{ cartItems.length }} sản phẩm)
            </h3>

            <div class="space-y-3 mb-4 max-h-[300px] overflow-y-auto pr-1">
              <div v-for="item in cartItems" :key="item.id"
                class="flex justify-between text-sm py-2 border-b border-dashed">
                <div class="flex-1 pr-2">
                  <div class="font-medium line-clamp-2">
                    {{ item.ten }}
                  </div>
                  <div class="text-gray-500 text-xs mt-1">
                    {{ item.color }} / {{ item.ram }} - x{{ item.soLuong }}
                  </div>
                </div>

                <div class="text-right">
                  <template v-if="item.giaSauGiam && item.giaSauGiam < item.giaBan">
                    <div class="font-bold text-gray-800">
                      {{ formatCurrency(item.giaSauGiam) }}
                    </div>
                    <div class="text-xs text-gray-400 line-through">
                      {{ formatCurrency(item.giaBan) }}
                    </div>
                  </template>
                  <template v-else>
                    <div class="font-bold text-gray-800">
                      {{ formatCurrency(item.giaBan) }}
                    </div>
                  </template>
                </div>
              </div>
            </div>

            <NDivider />

            <div class="mb-4">
              <div class="text-sm font-medium mb-1 flex items-center gap-1 text-gray-700">
                <NIcon>
                  <CheckmarkCircle />
                </NIcon> Mã ưu đãi
              </div>
              <NSelect v-model:value="selectedVoucher" :options="availableVouchers" label-field="code"
                value-field="voucherId" placeholder="Chọn mã giảm giá" clearable
                :render-label="(option) => `${option.code} - Giảm ${formatCurrency(option.giamGiaThucTe)}`" />
            </div>

            <div class="space-y-2 text-sm text-gray-600 bg-gray-50 p-3 rounded">
              <div class="flex justify-between">
                <span>Tạm tính:</span><span class="font-medium">{{ formatCurrency(subTotal) }}</span>
              </div>
              <div class="flex justify-between">
                <span>Phí vận chuyển:</span><span class="font-medium">{{ formatCurrency(shippingFee) }}</span>
              </div>
              <div v-if="discountAmount > 0" class="flex justify-between text-green-600 font-bold">
                <span>Voucher giảm:</span><span>-{{ formatCurrency(discountAmount) }}</span>
              </div>
            </div>

            <div class="flex justify-between items-center mt-4 pt-4 border-t">
              <div>
                <div class="font-bold text-lg text-gray-800">
                  Tổng cộng
                </div>
                <div class="text-xs text-gray-500 font-normal">
                  (Đã bao gồm VAT)
                </div>
              </div>
              <span class="font-bold text-2xl text-red-600">{{ formatCurrency(finalTotal) }}</span>
            </div>

            <NButton block type="primary" color="#d70018" size="large"
              class="font-bold h-12 text-lg mt-4 shadow-lg shadow-red-200" :loading="processing"
              @click="handleCheckout">
              ĐẶT HÀNG NGAY
            </NButton>
          </div>
        </NGi>
      </NGrid>
    </div>
  </div>
</template>
