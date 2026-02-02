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
  useMessage,
} from 'naive-ui'
import {
  CardOutline,
  CashOutline,
  CheckmarkCircle,
  LocationOutline,
  StorefrontOutline,
  TicketOutline,
} from '@vicons/ionicons5'

// Import Store & API
import { CartStore } from '@/utils/cartStore'
import type { CartItem } from '@/utils/cartStore'
import { createOrder, getMaGiamGia } from '@/service/api/client/banhang.api'
import { localStorageAction } from '@/utils'
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'

const router = useRouter()
const message = useMessage()
const processing = ref(false)

// Data
const cartItems = ref<CartItem[]>([])
const userInfo = localStorageAction.get(USER_INFO_STORAGE_KEY)

// Form Info
const deliveryType = ref<'GIAO_HANG' | 'TAI_QUAY'>('GIAO_HANG')
const paymentMethod = ref('0')
const customerInfo = ref({
  ten: userInfo?.ten || '',
  sdt: userInfo?.sdt || '',
  diaChi: '',
  ghiChu: '',
})

// Voucher
const selectedVoucher = ref<string | null>(null)
const availableVouchers = ref<any[]>([])
const discountAmount = ref(0) // Số tiền được giảm

onMounted(() => {
  loadCart()
})

function loadCart() {
  cartItems.value = CartStore.getCartItems()
  if (cartItems.value.length === 0) {
    message.warning('Giỏ hàng trống')
    router.push('/cart')
  }
  else {
    // Load voucher ngay khi có giỏ hàng
    loadVouchers()
  }
}

// Tính tổng tiền hàng
const subTotal = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.price * item.quantity), 0)
})

const shippingFee = computed(() => deliveryType.value === 'GIAO_HANG' ? 30000 : 0)

// Tổng thanh toán cuối cùng
const finalTotal = computed(() => {
  const total = subTotal.value + shippingFee.value - discountAmount.value
  return total > 0 ? total : 0
})

const formatCurrency = (val: number) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)

// --- [MỚI] LOGIC LOAD VOUCHER (CLIENT-SIDE) ---
async function loadVouchers() {
  if (subTotal.value <= 0)
    return

  try {
    // Gọi API gợi ý voucher (Không cần invoiceId vì chưa tạo hóa đơn)
    const res: any = await getMaGiamGia({
      invoiceId: '', // Gửi rỗng vì chưa có hóa đơn
      tongTien: subTotal.value,
      customerId: userInfo?.id || null,
    })

    // Xử lý dữ liệu trả về (Tùy backend trả về cấu trúc nào)
    const data = res.data || res
    if (data && data.voucherApDung) {
      availableVouchers.value = data.voucherApDung

      // Tự động chọn voucher tốt nhất
      if (availableVouchers.value.length > 0 && !selectedVoucher.value) {
        handleSelectVoucher(availableVouchers.value[0].voucherId)
      }
    }
  }
  catch (e) {
    console.error('Lỗi load voucher:', e)
  }
}

// Khi chọn voucher
function handleSelectVoucher(voucherId: string | null) {
  selectedVoucher.value = voucherId
  if (!voucherId) {
    discountAmount.value = 0
    return
  }

  const voucher = availableVouchers.value.find(v => v.voucherId === voucherId)
  if (voucher) {
    discountAmount.value = voucher.giamGiaThucTe || 0
    message.success(`Đã áp dụng mã: ${voucher.code}`)
  }
}

// Watch tổng tiền thay đổi -> Load lại voucher (để cập nhật mức giảm)
watch(subTotal, () => {
  loadVouchers()
})

// --- XỬ LÝ ĐẶT HÀNG ---
async function handleCheckout() {
  if (!customerInfo.value.ten || !customerInfo.value.sdt) {
    return message.warning('Vui lòng nhập Họ tên và Số điện thoại')
  }
  if (deliveryType.value === 'GIAO_HANG' && !customerInfo.value.diaChi) {
    return message.warning('Vui lòng nhập địa chỉ giao hàng')
  }

  processing.value = true
  try {
    const productPayload = cartItems.value.map(item => ({
      productDetailId: item.productDetailId,
      quantity: item.quantity,
      price: item.price,
    }))

    const payload = {
      ten: customerInfo.value.ten,
      sdt: customerInfo.value.sdt,
      diaChi: customerInfo.value.diaChi,
      ghiChu: customerInfo.value.ghiChu,

      tongTien: finalTotal.value,
      tienHang: subTotal.value,
      tienShip: shippingFee.value,
      giamGia: discountAmount.value,

      phuongThucThanhToan: paymentMethod.value,
      loaiHoaDon: deliveryType.value,
      idPGG: selectedVoucher.value || null, // Gửi ID Voucher lên server

      products: productPayload,
    }

    const res: any = await createOrder(payload)

    if (res.status === 200 || res.status === 'OK' || res.data) {
      message.success('Đặt hàng thành công!')
      CartStore.clearCart()
      router.push('/order-success')
    }
    else {
      message.error(res.message || 'Đặt hàng thất bại')
    }
  }
  catch (error: any) {
    console.error(error)
    const msg = error.response?.data?.message || 'Có lỗi xảy ra'
    message.error(msg)
  }
  finally {
    processing.value = false
  }
}
</script>

<template>
  <div class="py-8 font-sans">
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
              <div v-for="item in cartItems" :key="item.productDetailId"
                class="flex justify-between text-sm py-2 border-b border-dashed">
                <div class="flex-1 pr-2">
                  <div class="font-medium line-clamp-2">
                    {{ item.name }} {{ item.cpu }} {{ item.ram }} {{ item.hardDrive }}
                  </div>
                  <div class="text-gray-500 text-xs mt-1">
                    SL: <strong>x{{ item.quantity }}</strong>
                  </div>
                </div>
                <div class="text-right">
                  <div class="font-bold text-gray-800">
                    {{ formatCurrency(item.price * item.quantity) }}
                  </div>
                </div>
              </div>
            </div>

            <NDivider />

            <div class="mb-4">
              <div class="text-sm font-medium mb-1 flex items-center gap-1 text-gray-700">
                <NIcon color="#d03050">
                  <TicketOutline />
                </NIcon> Mã ưu đãi
              </div>
              <NSelect v-model:value="selectedVoucher" :options="availableVouchers" label-field="code"
                value-field="voucherId" placeholder="Chọn mã giảm giá" clearable
                :render-label="(option: any) => `${option.code} - Giảm ${formatCurrency(option.giamGiaThucTe)}`"
                @update:value="handleSelectVoucher" />
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
