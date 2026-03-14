<template>
  <div class="container mx-auto px-4">
    <!-- Search Card -->
    <div class="flex gap-2 flex-1 items-end">
      <n-form-item path="searchInvoiceCode" class="flex-1 mb-0">
        <n-input v-model:value="searchInvoiceCode" placeholder="Nhập mã hóa đơn để tìm kiếm" clearable />
      </n-form-item>
      <n-form-item>
        <n-button type="success" @click="handleSearch">
          <template #icon>
            <n-icon>
              <SearchOutline />
            </n-icon>
          </template>
          Tìm kiếm
        </n-button>
      </n-form-item>
    </div>

    <!-- Progress Timeline -->
    <NCard v-if="invoice" class="" :bordered="false">
      <template #header>
        <n-space justify="space-between" class="m-y-2">
          <!-- Header with invoice code -->
          <div v-if="invoice" class="mb-4 p-2 flex">
            <div class="w-1 bg-[#049d14] rounded m-r-2"></div>
            <h2 class="text-2xl font-bold">
              Tiến độ đơn hàng -
              <div class="inline-block rounded">
                {{ invoice.code }}
              </div>
            </h2>
          </div>

          <NButton type="error" block ghost @click="openCancelModal" :disabled="+(invoice.invoiceStatus) !== 0"
            :style="{ maxWidth: '150px' }">
            <template #icon>
              <n-icon>
                <CloseCircleOutline />
              </n-icon>
            </template>
            Hủy đơn hàng
          </NButton>
        </n-space>
      </template>

      <div class="relative">
        <!-- Progress bar (ẩn khi loaiHoaDon = 0) -->
        <div>
          <div class="absolute top-5 left-0 right-0 h-1.5 bg-gray-200 rounded-full z-0"></div>
          <div class="absolute top-5 left-0 h-1.5 bg-blue-500 rounded-full z-10" :style="{ width: progressWidth }">
          </div>
        </div>

        <!-- Steps -->
        <div class="relative flex justify-between z-20">
          <div v-for="(step, _) in filteredSteps" :key="step.key" class="flex flex-col items-center flex-1">
            <!-- Step circle -->
            <div
              class="w-10 h-10 rounded-full border-4 bg-white flex items-center justify-center mb-3 relative transition-all duration-300 hover:scale-110"
              :class="getStepCircleClass(step.key)">
              <n-icon size="18" :class="getStepIconClass(step.key)">
                <component :is="step.icon" />
              </n-icon>

              <!-- Completed check -->
              <div v-if="isStepCompleted(step.key)"
                class="absolute -bottom-1 -right-1 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center border-2 border-white shadow">
                <n-icon size="14" color="white">
                  <CheckmarkCircleOutline />
                </n-icon>
              </div>
            </div>

            <!-- Step label -->
            <div class="text-center">
              <p class="text-sm font-semibold mb-1" :class="getStepTextClass(step.key)">
                {{ step.title }}
              </p>
              <p v-if="historiesStatusInvoice.length > 0" class="text-xs text-gray-500">
                {{isStepCompleted(step.key) ? formatTime(historiesStatusInvoice.find(h => h.trangThai ==
                  +(step.key))?.thoiGian) : ''}}
              </p>
            </div>
          </div>
        </div>

        <!-- customer info panel -->
        <div class="flex gap-5">
          <div class="flex-1">
            <div v-if="invoice" class="mb-4 p-2 flex m-t-[50px]">
              <div class="w-1 bg-[#049d14] rounded m-r-2"></div>
              <h2 class="text-2xl font-bold">
                Thông tin khách hàng
              </h2>
            </div>
            <!-- <div class="mt-4 p-4 rounded-lg">
          <div class="flex justify-between gap-5 text-sm pb-2 mb-2">
            <div class="border-b border-gray-300/30 p-2 flex-1">
              <span class="font-medium mr-2">Khách hàng:</span>
              <span>{{ invoice.nameReceiver }}</span>
            </div>
            <div class="border-b border-gray-300/30 p-2 flex-1">
              <span class="font-medium mr-2">SĐT:</span>
              <span>{{ invoice.phoneReceiver }}</span>
            </div>
            <div class="border-b border-gray-300/30 p-2 flex-1">
              <span class="font-medium mr-2">Email:</span>
              <span>{{ invoice.email }}</span>
            </div>
            <div class="border-b border-gray-300/30 p-2 flex-1">
              <span class="font-medium mr-2">Ngày đặt:</span>
              <span>{{ convertTimeMillis(invoice.createDate) }}</span>
            </div>
          </div>
          <div v-if="invoice.description" class="text-sm pb-2 mb-2">
            <div class="border-b border-gray-300/30 p-2 flex-1">
              <span class="font-medium mr-2">Ghi chú:</span>
              <span>{{ invoice.description }}</span>
            </div>
          </div>
          <div class="text-sm">
            <div class="border-b border-gray-300/30 p-2 flex-1">
              <span class="font-medium mr-2">Địa chỉ:</span>
              <span class="break-words">{{ invoice.addressReceiver }}</span>
            </div>
          </div>
        </div> -->
            <div class="p-4 rounded-lg flex flex-col gap-3">
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Khách hàng:</span>
                <span>{{ invoice.nameReceiver }}</span>
              </div>
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">SĐT:</span>
                <span>{{ invoice.phoneReceiver }}</span>
              </div>
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Email:</span>
                <span>{{ invoice.email }}</span>
              </div>
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Ngày đặt:</span>
                <span>{{ convertTimeMillis(invoice.createDate) }}</span>
              </div>
              <div v-if="invoice.description" class="text-sm pb-2">
                <div class="border-b border-gray-300/30 p-2 flex-1">
                  <span class="font-medium mr-2">Ghi chú:</span>
                  <span>{{ invoice.description }}</span>
                </div>
              </div>
              <div class="text-sm">
                <div class="border-b border-gray-300/30 p-2 flex-1">
                  <span class="font-medium mr-2">Địa chỉ:</span>
                  <span class="break-words">{{ invoice.addressReceiver }}</span>
                </div>
              </div>
            </div>
          </div>
          <div class="flex-1">
            <div v-if="invoice" class="mb-4 p-2 flex m-t-[50px]">
              <div class="w-1 bg-[#049d14] rounded m-r-2"></div>
              <h2 class="text-2xl font-bold">
                Trạng thái thanh toán
              </h2>
            </div>
            <div class="p-4 rounded-lg flex flex-col gap-3">
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Phương thức thanh toán:</span>
                <span>{{ convertTextFromTypePayment(invoice.typePayment) }}</span>
              </div>
              <div class="border-b border-gray-300/30 p-2 flex-1">
                <span class="font-medium mr-2">Trạng thái thanh toán:</span>
                <n-tag :type="statusPayment">
                  {{ convertTextFromTrangThaiThanhToan(invoice.trangThaiThanhToan) }}
                </n-tag>
              </div>
            </div>
          </div>
        </div>

        <div v-if="invoice" class="mb-4 p-2 flex m-t-[50px]">
          <div class="w-1 bg-[#049d14] rounded m-r-2"></div>
          <h2 class="text-2xl font-bold">
            Sản phẩm đã đặt
          </h2>
        </div>

        <!-- product list -->
        <div v-if="invoiceDetails.length > 0" class="space-y-4">
          <div v-for="detail in invoiceDetails" :key="detail.id" class="flex items-center border-b border-gray-300 p-4">
            <img :src="detail.urlImage" alt="product" class="w-20 h-20 object-contain rounded" />
            <div class="flex-1 ml-4">
              <div class="text-base font-medium">
                {{ detail.product }} {{ detail.nameProductDetail }}
              </div>
              <div class="flex flex-wrap text-xs text-gray-500 mt-1 gap-2">
                <span v-if="detail.color" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.color }}</span>
                <span v-if="detail.ram" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.ram }}</span>
                <span v-if="detail.hardDrive" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.hardDrive }}</span>
                <span v-if="detail.cpu" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.cpu }}</span>
                <span v-if="detail.gpu" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.gpu }}</span>
                <span v-if="detail.material" class="border bg-gray/10 py-1 px-2.5 border-none font-semibold rounded">{{
                  detail.material }}</span>
              </div>
            </div>
            <div class="text-lg font-semibold text-red-600">
              {{ formatCurrency(detail.totalAmount) }}
            </div>
          </div>
        </div>

        <!-- totals summary -->
        <div v-if="invoice" class="mt-6 p-4 ">
          <div class="max-w-xs ml-auto text-sm space-y-3">
            <div class="flex justify-between">
              <span>Tạm tính:</span>
              <span>{{ formatCurrency(subtotal) }}</span>
            </div>
            <div class="flex justify-between text-red-600">
              <span>Giảm giá:</span>
              <span>-{{ formatCurrency(discount) }}</span>
            </div>
            <div class="flex justify-between">
              <span>Phí vận chuyển:</span>
              <span>{{ formatCurrency(shippingFee) }}</span>
            </div>
            <div class="flex justify-between font-bold text-lg border-t border-gray-300 p-2">
              <span>Tổng cộng:</span>
              <span class="text-red-600">{{ formatCurrency(total) }}</span>
            </div>
          </div>
        </div>
      </div>
    </NCard>

    <NCard v-if="!invoice" class="shadow-sm border-0 rounded-xl no-print bg-red-50" content-class="p-6">
      <p class="text-red-600 font-semibold">Không có thông tin chi tiết về đơn hàng. Vui lòng liên hệ bộ phận chăm sóc
        khách
        hàng để biết thêm chi tiết.</p>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
// import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NCard,
  NFormItem,
  NIcon,
  NInput,
  NSpace,
  useDialog,
  useMessage
} from 'naive-ui'

// Icons
import { ClientInvoiceDetailResponse, ClientInvoiceDetailsResponse, getHistoryStatusInvoice, getInvoiceById, getInvoiceDetails, LichSuTrangThaiHoaDonResponse } from '@/service/api/invoice.api'
import {
  CartOutline,
  CheckmarkCircleOutline,
  CheckmarkDoneOutline,
  CloseCircleOutline,
  SearchOutline,
  TimeOutline
} from '@vicons/ionicons5'

// const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const notification = useNotification()

// State
const invoice = ref<ClientInvoiceDetailResponse | null>(null)
const searchInvoiceCode = ref('')

const historiesStatusInvoice = ref<LichSuTrangThaiHoaDonResponse[]>([])
const invoiceDetails = ref<ClientInvoiceDetailsResponse[]>([])

// Lấy dữ liệu tổng hợp từ danh sách sản phẩm
const currentStatus = computed(() => {
  return invoice.value?.invoiceStatus ?? 0
})

// pricing computations
const subtotal = computed(() => invoice.value?.totalAmount ?? 0)
const discount = computed(() => {
  if (!invoice.value) return 0
  return subtotal.value - (invoice.value.totalAmountAfterDecrease ?? subtotal.value)
})
const shippingFee = computed(() => invoice.value?.shippingFee ?? 0)
const total = computed(() => invoice.value?.totalAmountAfterDecrease ?? subtotal.value + shippingFee.value)

// Computed values
const isCancelled = computed(() => currentStatus.value === 5)

const filteredSteps = computed(() => {
  return timelineSteps
})

// Progress
const progressWidth = computed(() => {
  const currentStep = currentStatus.value
  return `${(currentStep < 5 ? (((currentStep + 1) / 5) * 100) : 100)}%` // 4 là số bước tối đa (0-4)
})

// Timeline steps
const timelineSteps = [
  { key: '0', title: 'Chờ xác nhận', icon: TimeOutline, color: 'yellow' },
  { key: '1', title: 'Đã xác nhận', icon: CheckmarkCircleOutline, color: 'blue' },
  { key: '2', title: 'Chờ giao hàng', icon: CartOutline, color: 'purple' },
  { key: '3', title: 'Đang giao hàng', icon: CheckmarkCircleOutline, color: 'info' },
  { key: '4', title: 'Hoàn thành', icon: CheckmarkDoneOutline, color: 'green' },
]

// Helper functions
const formatCurrency = (value: number | undefined | null): string => {
  if (value === undefined || value === null) return "0 ₫"
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value)
}

// convert Java LocalDateTime string to "HH:mm:ss dd/mm/yyyy"
function formatTime(datetime: string | undefined | null): string {
  if (!datetime) return ''
  const d = new Date(datetime)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${pad(d.getDate())}/${pad(
    d.getMonth() + 1
  )}/${d.getFullYear()}`
}

// convert milliseconds since epoch to "HH:mm:ss dd/mm/yyyy"
function convertTimeMillis(ms: number | undefined | null): string {
  if (ms == null) return ''
  const d = new Date(ms)
  if (isNaN(d.getTime())) return ''
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())} ${pad(d.getDate())}/${pad(
    d.getMonth() + 1
  )}/${d.getFullYear()}`
}

// Timeline functions
const isStepCompleted = (stepKey: string): boolean => {
  const stepIndex = parseInt(stepKey)
  return stepIndex <= currentStatus.value && stepKey !== '5'
}

const isStepCurrent = (stepKey: string): boolean => {
  return currentStatus.value.toString() === stepKey
}

const getStepCircleClass = (stepKey: string): string => {
  if (isStepCurrent(stepKey)) {
    return 'border-blue-500 border-4'
  } else if (isStepCompleted(stepKey)) {
    return 'border-green-500 border-2'
  } else {
    return 'border-gray-300 border-2'
  }
}

const getStepIconClass = (stepKey: string): string => {
  if (isStepCurrent(stepKey)) {
    return 'text-blue-500'
  } else if (isStepCompleted(stepKey)) {
    return 'text-green-500'
  } else {
    return 'text-gray-400'
  }
}

const getStepTextClass = (stepKey: string): string => {
  if (isStepCurrent(stepKey)) {
    return 'text-blue-600'
  } else if (isStepCompleted(stepKey)) {
    return 'text-green-600'
  } else {
    return 'text-gray-500'
  }
}


// Actions

const handleSearch = (): void => {
  if (!searchInvoiceCode.value.trim()) {
    notification.warning({ content: 'Vui lòng nhập mã hóa đơn', duration: 3000 })
    return
  }

  fetchInvoice()
}

const openCancelModal = (): void => {
  if (isCancelled.value) {
    message.warning('Đơn hàng đã bị hủy')
    return
  }

  dialog.error({
    title: 'Xác nhận hủy đơn hàng',
    content: 'Bạn có chắc chắn muốn hủy đơn hàng này? Hành động này không thể hoàn tác.',
    positiveText: 'Xác nhận hủy',
    negativeText: 'Hủy bỏ',
    positiveButtonProps: {
      type: 'error',
      ghost: false
    },
    onPositiveClick: () => {
      // TODO: Gọi API hủy đơn hàng tại đây
    }
  })
}

const fetchInvoice = async (query?: string): Promise<void> => {
  try {
    const response = await getInvoiceById(query ?? searchInvoiceCode.value)

    invoice.value = response.data
    // load history statuses and details concurrently
    if (invoice.value?.id) {
      try {
        const [histRes, detailRes] = await Promise.all([
          getHistoryStatusInvoice(invoice.value.id),
          getInvoiceDetails([invoice.value.id])
        ])
        historiesStatusInvoice.value = histRes.data || []
        invoiceDetails.value = detailRes.data.filter(it => it.idInvoice === invoice.value?.id) || []
      } catch (e) {
        console.error('Error fetching invoice meta:', e)
        historiesStatusInvoice.value = []
        invoiceDetails.value = []
      }
    }
  } catch (error: any) {
    invoice.value = null
    historiesStatusInvoice.value = []
  }
}

function convertTextFromTypePayment(payment: string): string {
  switch (payment) {
    case 'TIEN_MAT':
      return 'Tiền mặt'
    case 'VNPAY':
      return 'VNPay'
    case 'CHUYEN_KHOAN':
      return 'Chuyển khoản'
    case 'THE_TIN_DUNG':
      return 'Thẻ tín dụng/Thẻ ghi nợ'
    case 'VI_DIEN_TU':
      return 'Ví điện tử'
    case 'TIEN_MAT_CHUYEN_KHOAN':
      return 'Tiền mặt + Chuyển khoản'
    default:
      return ''
  }
}

function convertTextFromTrangThaiThanhToan(status: string): string {
  switch (status) {
    case 'CHUA_THANH_TOAN':
      return 'Chưa thanh toán'
    case 'CHO_THANH_TOAN_VNPAY':
      return 'Chờ thanh toán VNPay'
    case 'DA_THANH_TOAN':
      return 'Đã thanh toán'
    case 'THANH_TOAN_MOT_PHAN':
      return 'Thanh toán một phần'
    case 'THANH_TOAN_THAT_BAI':
      return 'Thanh toán thất bại'
    default:
      return ''
  }
}

const statusPayment = computed(() => {
  switch (invoice.value?.trangThaiThanhToan) {
    case 'CHUA_THANH_TOAN':
      return 'error'
    case 'CHO_THANH_TOAN_VNPAY':
      return 'warning'
    case 'DA_THANH_TOAN':
      return 'success'
    case 'THANH_TOAN_MOT_PHAN':
      return 'success'
    case 'THANH_TOAN_THAT_BAI':
      return 'error'
    default:
      return 'success'
  }
})

// Lifecycle
onMounted(() => {
  fetchInvoice(route.query.q as string)
})
</script>

<style scoped>
/* Custom scrollbar */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>
