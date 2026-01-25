<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NEmpty,
  NIcon,
  NInputNumber,
  NRadio,
  NRadioGroup,
  NSpace,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'
import { LocationOutline, StorefrontOutline } from '@vicons/ionicons5'

// Import API
import {
  getCreateHoaDon,
  GetGioHang,
  getMaGiamGia,
  thanhToanThanhCong,
  themSanPham,
} from '@/service/api/admin/banhang.api'
import type { ParamsThanhCong, VoucherApDungDTO } from '@/service/api/admin/banhang.api'

// Import Modal
import OrderInfoModal from './OrderInfoModal.vue'

const route = useRoute()
const router = useRouter()
const message = useMessage()

// --- STATE ---
const loading = ref(false)
const currentInvoiceId = ref<string>('') // ID Hóa đơn tạm
const cartItems = ref<any[]>([]) // Danh sách sản phẩm trong hóa đơn
const deliveryType = ref<'GIAO_HANG' | 'TAI_QUAY'>('GIAO_HANG')

// Modal State
const showInfoModal = ref(false)
const submitLoading = ref(false)

// Voucher State
const selectedVoucher = ref<VoucherApDungDTO | null>(null)
const availableVouchers = ref<VoucherApDungDTO[]>([])

// Mock ID nhân viên (Thực tế lấy từ Auth Store)
const MOCK_ID_NV = 'NV001'

// --- INIT DATA ---
onMounted(async () => {
  await initOrder()
})

async function initOrder() {
  loading.value = true
  try {
    // 1. Tạo hóa đơn tạm (Nếu chưa có ID hóa đơn trong URL/Store)
    // Giả sử user bấm Mua Ngay -> Tạo mới luôn
    const resHD = await getCreateHoaDon({ idNV: MOCK_ID_NV })
    if (resHD.data) {
      currentInvoiceId.value = resHD.data.id

      // 2. Thêm sản phẩm vào hóa đơn (Lấy từ params router khi bấm Mua Ngay)
      // Ví dụ: router.push({ name: 'Order', query: { productId: '...', imei: '...' } })
      const productId = route.query.productId as string
      const imeiId = route.query.imeiId as string

      if (productId) {
        await themSanPham({
          invoiceId: currentInvoiceId.value,
          productDetailId: productId,
          imeiIds: imeiId ? [imeiId] : [], // Nếu có IMEI
        })
      }

      // 3. Load lại giỏ hàng (Chi tiết hóa đơn) để hiển thị
      await loadCart()
    }
  }
  catch (error) {
    message.error('Có lỗi khi khởi tạo đơn hàng')
    console.error(error)
  }
  finally {
    loading.value = false
  }
}

async function loadCart() {
  if (!currentInvoiceId.value)
    return
  const res = await GetGioHang(currentInvoiceId.value)
  if (res.data) {
    // Backend trả về PaginationResponse, data nằm trong content hoặc data
    cartItems.value = (res.data as any).data || (res.data as any).content || []
    // Sau khi có tiền, load voucher
    loadVouchers()
  }
}

// --- COMPUTED ---
const subTotal = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.donGia * item.soLuong), 0)
})

const discountAmount = computed(() => {
  if (!selectedVoucher.value)
    return 0
  return selectedVoucher.value.giamGiaThucTe || 0 // Dùng field từ API
})

const finalTotal = computed(() => {
  return subTotal.value - discountAmount.value
})

// --- LOGIC ---

async function loadVouchers() {
  try {
    const res = await getMaGiamGia({
      invoiceId: currentInvoiceId.value,
      tongTien: subTotal.value,
    })
    if (res && res.voucherApDung) {
      availableVouchers.value = res.voucherApDung
      // Tự động chọn voucher tốt nhất nếu chưa chọn
      if (availableVouchers.value.length > 0 && !selectedVoucher.value) {
        selectedVoucher.value = availableVouchers.value[0]
      }
    }
  }
  catch (e) {
    console.error(e)
  }
}

function handleOpenModal() {
  if (cartItems.value.length === 0) {
    message.warning('Giỏ hàng đang trống')
    return
  }
  showInfoModal.value = true
}

async function handleFinalSubmit(customerInfo: any) {
  submitLoading.value = true
  try {
    const payload: ParamsThanhCong = {
      idHD: currentInvoiceId.value,
      tongTien: finalTotal.value.toString(),
      idNV: MOCK_ID_NV, // Có thể null nếu khách tự mua
      ten: customerInfo.ten,
      sdt: customerInfo.sdt,
      diaChi: customerInfo.diaChi, // Địa chỉ hoặc tên Siêu thị
      loaiHoaDon: deliveryType.value,
      idPGG: selectedVoucher.value?.voucherId,
      phuongThucThanhToan: 'TIEN_MAT', // Hoặc xử lý chọn PTTT trước đó
      tienHang: subTotal.value,
      tienShip: 0, // Logic tính ship nếu cần
      giamGia: discountAmount.value,
      check: 1, // Flag xác nhận
      // Các field khác tuỳ backend yêu cầu
    }

    const res = await thanhToanThanhCong(payload)
    if (res.data) {
      message.success('Đặt hàng thành công!')
      showInfoModal.value = false
      router.push('/dat-hang-thanh-cong') // Chuyển trang cảm ơn
    }
  }
  catch (error) {
    message.error('Đặt hàng thất bại. Vui lòng thử lại.')
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
  <div class="checkout-container bg-gray-50 min-h-screen pb-20">
    <div class="bg-white shadow-sm py-4 mb-4">
      <div class="container mx-auto px-4">
        <h1 class="text-xl font-bold text-gray-800">
          Xác nhận đơn hàng
        </h1>
      </div>
    </div>

    <div v-if="loading" class="flex justify-center pt-20">
      <NSpin size="large" />
    </div>

    <div v-else class="container mx-auto px-4 max-w-4xl">
      <NCard class="mb-4" content-style="padding: 0;">
        <div class="flex">
          <label
            class="flex-1 p-4 cursor-pointer text-center border-b-2 transition-colors flex items-center justify-center gap-2"
            :class="deliveryType === 'GIAO_HANG' ? 'border-orange-500 bg-orange-50 text-orange-600 font-bold' : 'border-transparent text-gray-500'"
          >
            <input v-model="deliveryType" type="radio" value="GIAO_HANG" class="hidden">
            <NIcon size="20">
              <LocationOutline />
            </NIcon>
            Giao tận nơi
          </label>
          <label
            class="flex-1 p-4 cursor-pointer text-center border-b-2 transition-colors flex items-center justify-center gap-2"
            :class="deliveryType === 'TAI_QUAY' ? 'border-blue-500 bg-blue-50 text-blue-600 font-bold' : 'border-transparent text-gray-500'"
          >
            <input v-model="deliveryType" type="radio" value="TAI_QUAY" class="hidden">
            <NIcon size="20">
              <StorefrontOutline />
            </NIcon>
            Nhận tại siêu thị
          </label>
        </div>
      </NCard>

      <NCard title="Danh sách sản phẩm" class="mb-4" size="small">
        <div v-if="cartItems.length === 0">
          <NEmpty description="Giỏ hàng trống" />
        </div>
        <div v-else class="space-y-4">
          <div v-for="item in cartItems" :key="item.id" class="flex gap-4 py-2 border-b last:border-0">
            <img
              :src="item.anhSanPham || 'https://via.placeholder.com/80'"
              class="w-20 h-20 object-cover rounded border"
            >
            <div class="flex-1">
              <div class="font-medium text-gray-800">
                {{ item.tenSanPham }}
              </div>
              <div class="text-sm text-gray-500 mt-1">
                Màu: {{ item.tenMauSac }} | RAM: {{ item.tenRam }}
              </div>
              <div class="flex justify-between items-center mt-2">
                <div class="font-bold text-red-600">
                  {{ formatCurrency(item.donGia) }}
                </div>
                <div>x{{ item.soLuong }}</div>
              </div>
            </div>
          </div>
        </div>
      </NCard>

      <NCard title="Mã giảm giá" class="mb-4" size="small">
        <div v-if="availableVouchers.length === 0" class="text-gray-400 text-sm">
          Không có mã giảm giá phù hợp
        </div>
        <NRadioGroup v-else v-model:value="selectedVoucher" name="voucherGroup">
          <NSpace vertical>
            <NRadio v-for="v in availableVouchers" :key="v.voucherId" :value="v">
              <div class="flex items-center gap-2">
                <span class="font-bold text-orange-600">{{ v.code }}</span>
                <span class="text-sm text-gray-600">
                  - Giảm {{ formatCurrency(v.giamGiaThucTe) }}
                </span>
              </div>
            </NRadio>
          </NSpace>
        </NRadioGroup>
      </NCard>

      <div class="bg-white p-4 rounded shadow-sm border sticky bottom-0">
        <div class="flex justify-between mb-2 text-sm">
          <span class="text-gray-500">Tạm tính:</span>
          <span>{{ formatCurrency(subTotal) }}</span>
        </div>
        <div v-if="selectedVoucher" class="flex justify-between mb-2 text-sm">
          <span class="text-gray-500">Giảm giá:</span>
          <span class="text-green-600">-{{ formatCurrency(discountAmount) }}</span>
        </div>
        <div class="flex justify-between items-end mt-4 pt-4 border-t">
          <div>
            <div class="font-bold text-gray-800">
              Tổng tiền
            </div>
            <div class="text-xs text-gray-500">
              (Đã bao gồm VAT)
            </div>
          </div>
          <div class="text-2xl font-bold text-red-600">
            {{ formatCurrency(finalTotal) }}
          </div>
        </div>

        <NButton
          block type="primary" color="#f97316" class="mt-4 h-12 text-lg font-bold"
          :disabled="cartItems.length === 0" @click="handleOpenModal"
        >
          ĐẶT HÀNG
        </NButton>
      </div>
    </div>

    <OrderInfoModal
      v-model:show="showInfoModal" :delivery-type="deliveryType" :loading="submitLoading"
      @submit="handleFinalSubmit"
    />
  </div>
</template>

<style scoped>
.checkout-container {
  font-family: 'Inter', sans-serif;
}
</style>
