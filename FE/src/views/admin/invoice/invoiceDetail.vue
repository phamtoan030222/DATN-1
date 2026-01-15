<script lang="ts" setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Icon } from '@iconify/vue'
import {
  NButton,
  NCard,
  NDivider,
  NIcon,
  NSpin,
  NStep,
  NSteps,
} from 'naive-ui'

// --- 1. DEFINITIONS ---
interface InvoiceItem {
  id: number
  productName: string
  spec: string
  quantity: number
  price: number
  total: number
}

interface InvoiceDetail {
  id: string
  code: string
  createdAt: number
  customerName: string
  customerPhone: string
  customerAddress: string
  staffName: string

  // Payment & Shipping
  paymentMethod: string
  shippingMethod: string
  shippingFee: number

  // Discount
  voucherCode: string | null
  discountEvent: string | null
  discountAmount: number

  status: 'COMPLETED' | 'INCOMPLETE'
  items: InvoiceItem[]

  subTotal: number
  tax: number
  finalAmount: number
}

const router = useRouter()
const loading = ref(false)
const invoiceData = ref<InvoiceDetail | null>(null)

// --- 2. UTILS ---
function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function formatDateTime(timestamp: number) {
  const date = new Date(timestamp)
  return `${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')} ${date.getDate()}/${date.getMonth() + 1}/${date.getFullYear()}`
}

// --- 3. MOCK DATA (LAPTOP STORE) ---
async function fetchInvoiceDetail() {
  loading.value = true
  await new Promise(resolve => setTimeout(resolve, 300))

  invoiceData.value = {
    id: '1',
    code: 'HD2026-IT01',
    createdAt: Date.now(),
    customerName: 'Trần Lập Trình',
    customerPhone: '0912.345.678',
    customerAddress: 'Tòa nhà FPT, Duy Tân, Cầu Giấy, Hà Nội',

    staffName: 'Redamancy (Sales)',
    paymentMethod: 'Chuyển khoản (VietQR)',
    shippingMethod: 'Giao hàng hỏa tốc (Ahamove)',
    shippingFee: 50000,

    // Thông tin Voucher/Giảm giá
    voucherCode: 'WELCOME_IT',
    discountEvent: 'Back to School 2026',
    discountAmount: 1500000,

    status: 'COMPLETED',
    items: [
      {
        id: 1,
        productName: 'Laptop Dell XPS 13 Plus',
        spec: 'Core i7-1360P / 32GB RAM / 1TB SSD / 4K Touch',
        quantity: 1,
        price: 45000000,
        total: 45000000,
      },
      {
        id: 2,
        productName: 'Màn hình LG UltraFine 27"',
        spec: '4K IPS / USB-C PD 90W',
        quantity: 1,
        price: 12500000,
        total: 12500000,
      },
      {
        id: 3,
        productName: 'Bàn phím cơ Keychron Q1 Pro',
        spec: 'Red Switch / Nhôm nguyên khối',
        quantity: 1,
        price: 4200000,
        total: 4200000,
      },
      {
        id: 4,
        productName: 'Chuột Logitech MX Master 3S',
        spec: 'Màu xám nhạt (Pale Grey)',
        quantity: 2,
        price: 2100000,
        total: 4200000,
      },
    ],
    subTotal: 65900000,
    tax: 0,
    finalAmount: 64450000, // (65.9tr + 50k ship - 1.5tr voucher)
  }
  loading.value = false
}

// --- 4. ACTIONS ---
function handleBack() {
  router.push({ name: 'invoice_list' })
}

function handlePrint() {
  if (!invoiceData.value)
    return
  const originalTitle = document.title
  document.title = `${invoiceData.value.code}`
  window.print()
  document.title = originalTitle
}

onMounted(() => {
  fetchInvoiceDetail()
})
</script>

<template>
  <div class="invoice-detail-page">
    <div class="no-print mb-4 flex justify-between items-center">
      <div class="flex items-center gap-2">
        <NButton circle secondary @click="handleBack">
          <template #icon>
            <NIcon>
              <Icon icon="carbon:arrow-left" />
            </NIcon>
          </template>
        </NButton>
        <div class="flex flex-col">
          <span class="font-bold text-lg text-gray-700">Chi tiết hóa đơn</span>
          <span v-if="invoiceData" class="text-xs text-gray-500">Mã: {{ invoiceData.code }}</span>
        </div>
      </div>
      <NButton type="primary" class="bg-indigo-600 hover:bg-indigo-700 shadow-md" @click="handlePrint">
        <template #icon>
          <NIcon>
            <Icon icon="carbon:printer" />
          </NIcon>
        </template>
        In hóa đơn / Lưu PDF
      </NButton>
    </div>

    <NSpin :show="loading">
      <div v-if="invoiceData" class="flex flex-col lg:flex-row gap-6">
        <div class="w-full lg:w-1/3 flex flex-col gap-4 no-print">
          <NCard class="bg-white border border-gray-100 rounded-xl shadow-sm">
            <template #header>
              <span class="text-sm font-bold text-gray-900 uppercase tracking-wider">
                Tiến độ đơn hàng
              </span>
            </template>

            <NSteps vertical size="small" :current="4" status="finish" class="custom-steps">
              <NStep title="Đã đặt hàng" description="Tạo đơn thành công" />
              <NStep title="Đã xác nhận" description="Nhân viên kiểm tra kho" />
              <NStep title="Thanh toán" :description="invoiceData.paymentMethod" />
              <NStep title="Hoàn thành" description="Giao hàng thành công" />
            </NSteps>
          </NCard>

          <NCard class="bg-white border border-gray-100 rounded-xl shadow-sm">
            <template #header>
              <span class="text-sm font-bold text-gray-500 uppercase">Người nhận</span>
            </template>
            <div class="flex items-center gap-3 mb-4">
              <div
                class="w-10 h-10 rounded-full bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold"
              >
                {{ invoiceData.customerName.charAt(0) }}
              </div>
              <div>
                <div class="font-bold text-gray-800">
                  {{ invoiceData.customerName }}
                </div>
                <div class="text-sm text-gray-500">
                  {{ invoiceData.customerPhone }}
                </div>
              </div>
            </div>
            <div class="text-sm text-gray-600 p-3 bg-gray-50 rounded border border-gray-100">
              <span class="font-semibold block mb-1">Địa chỉ:</span>
              {{ invoiceData.customerAddress }}
            </div>
          </NCard>

          <NCard
            v-if="invoiceData.voucherCode || invoiceData.discountEvent"
            class="bg-orange-50 border border-orange-100 rounded-xl shadow-sm"
          >
            <template #header>
              <span class="text-sm font-bold text-orange-600 uppercase">Ưu đãi đang áp dụng</span>
            </template>
            <div class="flex flex-col gap-2 text-sm">
              <div v-if="invoiceData.discountEvent" class="flex justify-between">
                <span class="text-gray-600">Chiến dịch:</span>
                <span class="font-bold">{{ invoiceData.discountEvent }}</span>
              </div>
              <div v-if="invoiceData.voucherCode" class="flex justify-between">
                <span class="text-gray-600">Mã Voucher:</span>
                <span
                  class="font-mono bg-white px-2 py-0.5 rounded border border-orange-200 text-orange-600 font-bold"
                >{{
                  invoiceData.voucherCode }}</span>
              </div>
              <NDivider class="my-2" />
              <div class="flex justify-between font-bold text-orange-700">
                <span>Tổng giảm:</span>
                <span>-{{ formatCurrency(invoiceData.discountAmount) }}</span>
              </div>
            </div>
          </NCard>
        </div>

        <div class="w-full lg:w-2/3">
          <div id="invoice-paper" class="bg-white text-black p-8 md:p-12 rounded-xl shadow-sm relative">
            <div class="flex justify-between items-start mb-10">
              <div>
                <div class="flex items-center gap-2 mb-2">
                  <!-- <Icon icon="game-icons:laptop" class="text-3xl text-indigo-700" /> -->
                  <img src="@/assets/svg-icons/logo.svg" class="w-8 h-8" alt="logo" srcset="">
                  <span class="text-2xl font-bold tracking-widest text-gray-900">My Laptop</span>
                </div>
                <div class="text-xs text-gray-500 space-y-1">
                  <p>123 Đại Cồ Việt, Hai Bà Trưng, Hà Nội</p>
                  <p>Website: mylaptop.vn | Hotline: 1900.8888</p>
                </div>
              </div>
              <div class="text-right">
                <h2 class="text-xl font-bold uppercase text-gray-800">
                  Hóa Đơn
                </h2>
                <p class="text-sm font-bold text-gray-600 mt-1">
                  Mã: {{ invoiceData.code }}
                </p>
                <p class="text-sm text-gray-500">
                  Ngày: {{ formatDateTime(invoiceData.createdAt) }}
                </p>
              </div>
            </div>

            <NDivider class="border-dashed my-6" />

            <div class="grid grid-cols-2 gap-8 mb-8 text-sm">
              <div>
                <h3 class="font-bold text-gray-400 uppercase text-[15px] mb-3 tracking-wider">
                  Khách hàng
                </h3>
                <p class="font-bold text-gray-900 text-base mb-1">
                  {{ invoiceData.customerName }}
                </p>
                <p class="text-gray-600 mb-1">
                  {{ invoiceData.customerPhone }}
                </p>
                <p class="text-gray-600 leading-relaxed">
                  {{ invoiceData.customerAddress }}
                </p>
              </div>

              <div class="text-right">
                <h3 class="font-bold text-gray-400 uppercase text-[15px] mb-3 tracking-wider">
                  Thông tin đơn hàng
                </h3>
                <div class="space-y-1.5 text-gray-600">
                  <p>Nhân viên: <span class="text-gray-900 font-medium">{{ invoiceData.staffName }}</span></p>
                  <p>Vận chuyển: <span class="text-gray-900 font-medium">{{ invoiceData.shippingMethod }}</span></p>
                  <p>Thanh toán: <span class="text-gray-900 font-medium">{{ invoiceData.paymentMethod }}</span></p>
                </div>
              </div>
            </div>

            <table class="w-full text-sm mb-6 print-table">
              <thead>
                <tr class="bg-gray-50 border-y border-gray-200 text-gray-500 text-xs uppercase">
                  <th class="py-3 text-left pl-2 font-semibold">
                    Sản phẩm
                  </th>
                  <th class="py-3 text-center font-semibold w-16">
                    SL
                  </th>
                  <th class="py-3 text-right font-semibold w-28">
                    Đơn giá
                  </th>
                  <th class="py-3 text-right font-semibold w-32 pr-2">
                    Thành tiền
                  </th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-100">
                <tr v-for="item in invoiceData.items" :key="item.id">
                  <td class="py-4 pl-2 align-top">
                    <p class="font-bold text-gray-800 text-[15px]">
                      {{ item.productName }}
                    </p>
                    <p class="text-[11px] text-gray-500 mt-1 italic">
                      {{ item.spec }}
                    </p>
                  </td>
                  <td class="py-4 text-center align-top text-gray-600 font-medium">
                    {{ item.quantity }}
                  </td>
                  <td class="py-4 text-right align-top text-gray-600">
                    {{ formatCurrency(item.price) }}
                  </td>
                  <td class="py-4 text-right align-top font-bold text-gray-900 pr-2">
                    {{ formatCurrency(item.total) }}
                  </td>
                </tr>
              </tbody>
            </table>

            <div class="flex justify-end">
              <div class="w-2/3 md:w-1/2 space-y-2 text-right">
                <div class="flex justify-between text-sm text-gray-600">
                  <span>Tổng tiền hàng:</span>
                  <span class="font-medium">{{ formatCurrency(invoiceData.subTotal) }}</span>
                </div>

                <div class="flex justify-between text-sm text-gray-600">
                  <span>Phí vận chuyển:</span>
                  <span>+ {{ formatCurrency(invoiceData.shippingFee) }}</span>
                </div>

                <div v-if="invoiceData.discountAmount > 0" class="py-2 my-1 border-y border-dashed border-gray-100">
                  <div class="flex justify-between text-sm text-green-600 mb-1">
                    <span>Ưu đãi ({{ invoiceData.discountEvent || 'Voucher' }}):</span>
                    <span>- {{ formatCurrency(invoiceData.discountAmount) }}</span>
                  </div>
                  <div v-if="invoiceData.voucherCode" class="flex justify-between text-xs text-gray-400 italic">
                    <span>Mã áp dụng:</span>
                    <span>{{ invoiceData.voucherCode }}</span>
                  </div>
                </div>

                <div class="flex justify-between text-sm text-gray-600">
                  <span>Thuế (VAT 0%):</span>
                  <span>{{ formatCurrency(invoiceData.tax) }}</span>
                </div>

                <NDivider class="my-3 bg-gray-800" />

                <div class="flex justify-between items-center">
                  <span class="font-bold text-gray-800 uppercase text-sm">Tổng thanh toán:</span>
                  <span class="text-2xl font-extrabold text-indigo-700">{{ formatCurrency(invoiceData.finalAmount)
                  }}</span>
                </div>
              </div>
            </div>

            <div class="mt-16 pt-8 text-center border-t border-gray-100">
              <p class="font-bold text-gray-800">
                Cảm ơn quý khách đã tin tưởng My Laptop Store!
              </p>
              <p class="text-[10px] text-gray-400 mt-1">
                Sản phẩm được bảo hành chính hãng. Vui lòng giữ lại hóa đơn này để bảo hành.
              </p>
            </div>
          </div>
        </div>
      </div>
    </NSpin>
  </div>
</template>

<style scoped>
/* ==================================================================
   FIX LỖI: DÍNH MENU SIDEBAR & CẮT NỘI DUNG KHI IN
   ================================================================== */

@media print {

  /* 1. RESET KHỔ GIẤY */
  @page {
    margin: 0;
    /* Xóa lề mặc định của trình duyệt */
    size: auto;
  }

  /* 2. ẨN TẤT CẢ MỌI THỨ CỦA TRANG WEB */
  body,
  html,
  .n-layout,
  .n-layout-sider,
  .n-layout-header {
    visibility: hidden !important;
    height: 0 !important;
    width: 0 !important;
    overflow: hidden !important;
    margin: 0 !important;
    padding: 0 !important;
  }

  /* Ẩn các thành phần con trong body để chắc chắn */
  body * {
    visibility: hidden !important;
  }

  /* 3. HIỆN LẠI DUY NHẤT TỜ HÓA ĐƠN */
  #invoice-paper,
  #invoice-paper * {
    visibility: visible !important;
  }

  /* 4. ĐỊNH VỊ TỜ HÓA ĐƠN CHIẾM TOÀN BỘ TRANG GIẤY */
  #invoice-paper {
    position: fixed !important;
    /* Dùng fixed để đè lên tất cả */
    left: 0 !important;
    top: 0 !important;
    width: 100vw !important;
    /* Full chiều rộng giấy */
    min-height: 100vh !important;

    margin: 0 !important;
    padding: 30px 40px !important;
    /* Căn lề đẹp cho văn bản */

    background-color: white !important;
    z-index: 999999 !important;
    /* Lớp cao nhất */

    /* Reset các style ảnh hưởng từ layout cha */
    border: none !important;
    box-shadow: none !important;
    border-radius: 0 !important;
  }

  /* 5. ẨN CÁC NÚT BẤM THỪA (Nếu lỡ lọt vào) */
  .no-print {
    display: none !important;
  }

  /* 6. Ép màu sắc hiển thị đúng (Background, Text) */
  * {
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
    color-adjust: exact !important;
  }
}

/* 1. Chỉnh tiêu đề bước (Ví dụ: Đã đặt hàng, Thanh toán...) */
:deep(.n-step-content-header__title) {
  font-weight: 700 !important; /* In đậm (Bold) */
  color: #2a2a2a !important;   /* Màu xám đen (Gray-900) - Bất chấp trạng thái */
  font-size: 14px;
}

/* 2. Chỉnh mô tả bước (Ví dụ: Tạo đơn thành công...) */
:deep(.n-step-content__description) {
  font-weight: 500 !important; /* Hơi đậm một chút (Medium) */
  color: #6b7280 !important;   /* Màu xám vừa (Gray-500) */
  font-size: 12px;
  margin-top: 2px !important;
}

/* 3. (Tùy chọn) Chỉnh màu dấu tick xanh cho đậm hơn nếu thích */
:deep(.n-step-indicator) {
  --n-indicator-icon-color: #059669 !important; /* Màu xanh lá đậm hơn */
  --n-indicator-border-color: #059669 !important;
}

/* Style cho màn hình thường (Web view) */
.invoice-detail-page {
  max-width: 1300px;
  margin: 0 auto;
}
</style>
