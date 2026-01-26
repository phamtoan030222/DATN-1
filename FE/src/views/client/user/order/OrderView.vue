<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NDivider,
  NEmpty,
  NGi,
  NGrid,
  NIcon,
  NInput,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui' // Import thêm NInput, NSelect, Layout
import { CardOutline, CashOutline, LocationOutline, QrCodeOutline, StorefrontOutline } from '@vicons/ionicons5'

// Import API (Giữ nguyên)
import {
  getCreateHoaDon,
  GetGioHang,
  getImeiProductDetail,
  getMaGiamGia,
  thanhToanThanhCong,
  themSanPham,
} from '@/service/api/admin/banhang.api'
import type { ParamsThanhCong, VoucherApDungDTO } from '@/service/api/admin/banhang.api'

const route = useRoute()
const router = useRouter()
const message = useMessage()

// --- INTERFACES (Java DTO style) ---
interface CartItem {
  id: string
  tenSanPham: string
  anhSanPham: string
  donGia: number
  soLuong: number
  tenMauSac?: string
  tenRam?: string
}

// --- STATE ---
const loading = ref(false)
const submitLoading = ref(false)
const currentInvoiceId = ref<string>('')
const cartItems = ref<CartItem[]>([])

// Logic hiển thị form
const deliveryType = ref<'GIAO_HANG' | 'TAI_QUAY'>('GIAO_HANG')
const paymentMethod = ref<'TIEN_MAT' | 'CHUYEN_KHOAN' | 'QUET_QR'>('TIEN_MAT')

// Form data
const customerInfo = ref({
  ten: '',
  sdt: '',
  email: '',
  diaChi: '',
  storeId: null as string | null,
  ghiChu: '',
})

// Options cho Select
const storeOptions = [
  { label: 'Siêu thị 123 Nguyễn Văn Linh, Đà Nẵng', value: 'ST01' },
  { label: 'Siêu thị 456 Cầu Giấy, Hà Nội', value: 'ST02' },
  { label: 'Siêu thị 789 Quận 1, TP.HCM', value: 'ST03' },
]

// Voucher
const selectedVoucher = ref<VoucherApDungDTO | null>(null)
const availableVouchers = ref<VoucherApDungDTO[]>([])

// MOCK CONSTANT
const MOCK_ID_NV = '68178270-fde5-4598-9168-720efa06d5e7' // TODO: Lấy từ Store/Localstorage

// --- INIT DATA ---
onMounted(async () => {
  await initOrder()
})

async function initOrder() {
  loading.value = true
  try {
    // 1. Tạo hóa đơn
    const resHD = await getCreateHoaDon({ idNV: MOCK_ID_NV })
    if (!resHD.data)
      throw new Error('Không tạo được hóa đơn')
    currentInvoiceId.value = resHD.data.id

    // 2. Thêm sản phẩm (Logic xử lý IMEI)
    const productId = route.query.productId as string
    const imeiIdFromDetail = route.query.imeiId as string | undefined

    if (productId) {
      await handleAddProductToCart(productId, imeiIdFromDetail)
    }

    // 3. Load giỏ hàng
    await loadCart()
  }
  catch (error: any) {
    message.error(`Lỗi khởi tạo: ${error.message || 'Lỗi hệ thống'}`)
  }
  finally {
    loading.value = false
  }
}

async function handleAddProductToCart(productId: string, imeiIdFromDetail?: string) {
  try {
    const resImei = await getImeiProductDetail(productId)
    // Safe check data array
    const imeiArray = Array.isArray(resImei.data) ? resImei.data : (resImei?.data?.data || [])

    if (!Array.isArray(imeiArray))
      return

    // Tìm IMEI Active (Status == 0)
    let targetImei = imeiArray.find((i: any) => String(i.status) === '0' || String(i.imeiStatus) === '0')

    // Ưu tiên IMEI user chọn từ trang trước
    if (imeiIdFromDetail) {
      const specificImei = imeiArray.find((i: any) => String(i.id) === String(imeiIdFromDetail))
      if (specificImei)
        targetImei = specificImei
    }

    // Fallback: Lấy cái đầu tiên nếu không tìm thấy active (Cẩn thận logic này nhé)
    if (!targetImei && imeiArray.length > 0) {
      targetImei = imeiArray[0]
    }

    if (targetImei) {
      await themSanPham({
        invoiceId: currentInvoiceId.value,
        productDetailId: productId,
        imeiIds: [targetImei.id],
      })
    }
  }
  catch (err) {
    console.error('Add Product Error:', err)
  }
}

async function loadCart() {
  if (!currentInvoiceId.value)
    return
  try {
    const res = await GetGioHang(currentInvoiceId.value)
    // Ép kiểu dữ liệu trả về cho an toàn
    const rawData = (res.data as any)?.data || (res.data as any)?.content || []
    cartItems.value = rawData
  }
  catch (e) {
    console.error('Load Cart Error:', e)
  }
}

// --- COMPUTED & WATCHERS ---
const subTotal = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.donGia * item.soLuong), 0)
})

const discountAmount = computed(() => {
  return selectedVoucher.value?.giamGiaThucTe || 0
})

const finalTotal = computed(() => {
  const total = subTotal.value - discountAmount.value
  return total > 0 ? total : 0
})

// Tự động load voucher khi tổng tiền thay đổi (Reactive programming)
watch(subTotal, async (newVal) => {
  if (newVal > 0 && currentInvoiceId.value) {
    try {
      const res = await getMaGiamGia({
        invoiceId: currentInvoiceId.value,
        tongTien: newVal,
      })
      if (res?.voucherApDung) {
        availableVouchers.value = res.voucherApDung
        // Auto select best voucher (optional)
        if (availableVouchers.value.length > 0 && !selectedVoucher.value) {
          selectedVoucher.value = availableVouchers.value[0]
        }
      }
    }
    catch (e) { console.error(e) }
  }
})

// --- SUBMIT ---
async function handleFinalSubmit() {
  // Validation
  if (cartItems.value.length === 0)
    return message.warning('Giỏ hàng trống')
  if (!customerInfo.value.ten || !customerInfo.value.sdt)
    return message.warning('Thiếu thông tin khách hàng')
  if (deliveryType.value === 'GIAO_HANG' && !customerInfo.value.diaChi)
    return message.warning('Thiếu địa chỉ giao hàng')
  if (deliveryType.value === 'TAI_QUAY' && !customerInfo.value.storeId)
    return message.warning('Vui lòng chọn cửa hàng')

  submitLoading.value = true
  try {
    const payload: ParamsThanhCong = {
      idHD: currentInvoiceId.value,
      tongTien: finalTotal.value.toString(),
      idNV: MOCK_ID_NV,
      ten: customerInfo.value.ten,
      sdt: customerInfo.value.sdt,
      diaChi: customerInfo.value.diaChi,
      loaiHoaDon: deliveryType.value,
      idPGG: selectedVoucher.value?.voucherId,
      phuongThucThanhToan: paymentMethod.value,
      tienHang: subTotal.value,
      tienShip: 0,
      giamGia: discountAmount.value,
      check: 1,
    }

    const res = await thanhToanThanhCong(payload)
    if (res.data) {
      message.success('Đặt hàng thành công!')
      // Redirect hoặc Reset
      router.push('/success') // Nên có trang cảm ơn
    }
  }
  catch (error) {
    message.error('Đặt hàng thất bại!')
    console.error(error)
  }
  finally {
    submitLoading.value = false
  }
}

function formatCurrency(val: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
</script>

<template>
  <div class="checkout-page bg-gray-50 min-h-screen pb-10">
    <div class="bg-white shadow-sm py-4 mb-6 sticky top-0 z-20">
      <div class="container mx-auto px-4">
        <h1 class="text-xl font-bold text-gray-800 flex items-center gap-2">
          <NIcon color="#f97316">
            <StorefrontOutline />
          </NIcon>
          Xác nhận đơn hàng
        </h1>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center pt-20 h-screen">
      <NSpin size="large" description="Đang khởi tạo đơn hàng..." />
    </div>

    <div v-else class="container mx-auto px-4 max-w-6xl">
      <NGrid x-gap="24" y-gap="24" cols="1 1000:3" responsive="screen">
        <NGi span="2">
          <NCard title="1. Hình thức nhận hàng" class="mb-4 shadow-sm" size="small">
            <div class="flex gap-4 mb-4">
              <div
                class="flex-1 p-3 border rounded-lg cursor-pointer transition-all flex items-center justify-center gap-2"
                :class="deliveryType === 'GIAO_HANG' ? 'border-orange-500 bg-orange-50 text-orange-700 font-bold ring-1 ring-orange-500' : 'border-gray-200 hover:bg-gray-50'"
                @click="deliveryType = 'GIAO_HANG'"
              >
                <NIcon size="20">
                  <LocationOutline />
                </NIcon> Giao tận nơi
              </div>
              <div
                class="flex-1 p-3 border rounded-lg cursor-pointer transition-all flex items-center justify-center gap-2"
                :class="deliveryType === 'TAI_QUAY' ? 'border-blue-500 bg-blue-50 text-blue-700 font-bold ring-1 ring-blue-500' : 'border-gray-200 hover:bg-gray-50'"
                @click="deliveryType = 'TAI_QUAY'"
              >
                <NIcon size="20">
                  <StorefrontOutline />
                </NIcon> Nhận tại cửa hàng
              </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <NInput v-model:value="customerInfo.ten" placeholder="Họ và tên người nhận" />
              <NInput v-model:value="customerInfo.sdt" placeholder="Số điện thoại" />
              <NInput v-model:value="customerInfo.email" placeholder="Email (Không bắt buộc)" class="md:col-span-2" />

              <template v-if="deliveryType === 'GIAO_HANG'">
                <NInput
                  v-model:value="customerInfo.diaChi"
                  placeholder="Địa chỉ chi tiết (Số nhà, đường, phường/xã...)"
                  class="md:col-span-2"
                />
              </template>
              <template v-else>
                <NSelect
                  v-model:value="customerInfo.storeId"
                  :options="storeOptions"
                  placeholder="Chọn siêu thị nhận hàng"
                  class="md:col-span-2"
                />
              </template>

              <NInput
                v-model:value="customerInfo.ghiChu"
                type="textarea"
                placeholder="Ghi chú đơn hàng (VD: Giao giờ hành chính)"
                class="md:col-span-2"
              />
            </div>
          </NCard>

          <NCard title="2. Phương thức thanh toán" class="mb-4 shadow-sm" size="small">
            <NRadioGroup v-model:value="paymentMethod" name="paymentGroup" class="w-full">
              <div class="space-y-3">
                <NRadio value="TIEN_MAT" class="w-full p-3 border rounded-lg" :class="{ 'bg-orange-50 border-orange-200': paymentMethod === 'TIEN_MAT' }">
                  <div class="flex items-center gap-2">
                    <NIcon size="20">
                      <CashOutline />
                    </NIcon> Thanh toán tiền mặt khi nhận hàng (COD)
                  </div>
                </NRadio>
                <NRadio value="CHUYEN_KHOAN" class="w-full p-3 border rounded-lg" :class="{ 'bg-orange-50 border-orange-200': paymentMethod === 'CHUYEN_KHOAN' }">
                  <div class="flex items-center gap-2">
                    <NIcon size="20">
                      <CardOutline />
                    </NIcon> Chuyển khoản ngân hàng
                  </div>
                </NRadio>
                <NRadio value="QUET_QR" class="w-full p-3 border rounded-lg" :class="{ 'bg-orange-50 border-orange-200': paymentMethod === 'QUET_QR' }">
                  <div class="flex items-center gap-2">
                    <NIcon size="20">
                      <QrCodeOutline />
                    </NIcon> Quét mã QR
                  </div>
                </NRadio>
              </div>
            </NRadioGroup>
          </NCard>
        </NGi>

        <NGi>
          <div class="sticky top-20">
            <NCard title="Đơn hàng của bạn" size="small" class="shadow-lg border-t-4 border-orange-500">
              <div v-if="cartItems.length === 0">
                <NEmpty description="Giỏ hàng trống" size="small" />
              </div>
              <div v-else class="max-h-[300px] overflow-y-auto pr-2 custom-scrollbar">
                <div v-for="item in cartItems" :key="item.id" class="flex gap-3 mb-4 last:mb-0">
                  <div class="relative">
                    <img :src="item.anhSanPham || 'https://via.placeholder.com/80'" class="w-16 h-16 object-cover rounded border bg-gray-100">
                    <span class="absolute -top-2 -right-2 bg-gray-500 text-white text-xs w-5 h-5 flex items-center justify-center rounded-full">
                      {{ item.soLuong }}
                    </span>
                  </div>
                  <div class="flex-1 text-sm">
                    <div class="font-bold text-gray-800 line-clamp-2">
                      {{ item.tenSanPham }}
                    </div>
                    <div class="text-xs text-gray-500 mt-1">
                      {{ item.tenMauSac }} / {{ item.tenRam }}
                    </div>
                    <div class="text-orange-600 font-semibold mt-1">
                      {{ formatCurrency(item.donGia) }}
                    </div>
                  </div>
                </div>
              </div>

              <NDivider style="margin: 12px 0" />

              <div class="mb-4">
                <div class="text-sm font-bold text-gray-700 mb-2">
                  Mã giảm giá
                </div>
                <NSelect
                  v-model:value="selectedVoucher"
                  :options="availableVouchers"
                  label-field="code"
                  value-field="voucherId"
                  placeholder="Chọn mã giảm giá"
                  :render-label="(option) => `${option.code} - Giảm ${formatCurrency(option.giamGiaThucTe)}`"
                  clearable
                />
              </div>

              <NDivider style="margin: 12px 0" />

              <div class="space-y-2 text-sm">
                <div class="flex justify-between text-gray-600">
                  <span>Tạm tính:</span>
                  <span>{{ formatCurrency(subTotal) }}</span>
                </div>
                <div v-if="selectedVoucher" class="flex justify-between text-green-600">
                  <span>Giảm giá:</span>
                  <span>-{{ formatCurrency(discountAmount) }}</span>
                </div>
                <div class="flex justify-between text-gray-600">
                  <span>Phí vận chuyển:</span>
                  <span>Miễn phí</span>
                </div>
              </div>

              <NDivider style="margin: 12px 0" />

              <div class="flex justify-between items-center mb-4">
                <span class="font-bold text-gray-800 text-lg">Tổng cộng:</span>
                <span class="font-bold text-red-600 text-xl">{{ formatCurrency(finalTotal) }}</span>
              </div>

              <NButton
                block type="primary" color="#f97316" size="large"
                :loading="submitLoading"
                :disabled="cartItems.length === 0"
                class="font-bold shadow-orange-200 shadow-lg"
                @click="handleFinalSubmit"
              >
                ĐẶT HÀNG NGAY
              </NButton>
            </NCard>
          </div>
        </NGi>
      </NGrid>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 4px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }
</style>
