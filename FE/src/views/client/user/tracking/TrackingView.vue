<template>
  <div class="container mx-auto px-4 py-6">
    <!-- Search Card -->
    <NCard class="shadow-sm border-0 rounded-xl mb-6 no-print" content-class="p-6">
      <div class="flex gap-4 items-end">
        <n-form-item path="searchInvoiceCode" class="flex-1 mb-0">
          <n-input v-model:value="searchInvoiceCode" placeholder="Nhập mã hóa đơn để tìm kiếm" clearable />
        </n-form-item>
        <n-form-item>
          <n-button type="primary" @click="handleSearch">
            <template #icon>
              <n-icon>
                <SearchOutline />
              </n-icon>
            </template>
            Tìm kiếm
          </n-button>
        </n-form-item>
      </div>
    </NCard>

    <!-- Progress Timeline -->
    <NCard v-if="invoice" class="shadow-sm border-0 rounded-xl no-print" content-class="p-6">
      <template #header>
        <n-space justify="space-between" class="m-y-2">
          <!-- Header with invoice code -->
          <div v-if="invoice" class="mb-4">
            <h2 class="text-2xl font-bold">Tiến độ đơn hàng {{ invoice.code }}</h2>
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
          <div v-for="(step, index) in filteredSteps" :key="step.key" class="flex flex-col items-center flex-1">
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
  useMessage,
  type SelectOption
} from 'naive-ui'

// Icons
import { ClientInvoiceDetailResponse, getInvoiceById } from '@/service/api/invoice.api'
import {
  CartOutline,
  CheckmarkCircleOutline,
  CheckmarkDoneOutline,
  CloseCircleOutline,
  SearchOutline,
  TimeOutline
} from '@vicons/ionicons5'

// const router = useRouter()
// const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const notification = useNotification()

// State
const invoice = ref<ClientInvoiceDetailResponse | null>(null)
const showStatusModal = ref(false)
const selectedStatus = ref<number | null>(null)
const statusNote = ref('')
const isUpdating = ref(false)
const searchInvoiceCode = ref('')

// Lấy dữ liệu tổng hợp từ danh sách sản phẩm
const currentStatus = computed(() => {
  return parseInt(invoice.value?.invoiceStatus || '0')
})

// Computed values
const isCompleted = computed(() => currentStatus.value === 4)
const isCancelled = computed(() => currentStatus.value === 5)

const availableStatusOptions = computed<SelectOption[]>(() => {
  const current = currentStatus.value

  // Map trạng thái
  const statusMap: Record<number, { label: string; disabled: boolean }> = {
    0: { label: 'Chờ xác nhận', disabled: false },
    1: { label: 'Đã xác nhận', disabled: false },
    2: { label: 'Chờ giao hàng', disabled: false },
    3: { label: 'Đang giao hàng', disabled: false },
    4: { label: 'Hoàn thành', disabled: true },
    5: { label: 'Đã hủy', disabled: true }
  }

  // Nếu đã hoàn thành, chỉ hiển thị tất cả trạng thái đã đi qua
  if (isCompleted.value) {
    const options: SelectOption[] = []
    // Tạo array từ 0 đến 4 (hoàn thành)
    for (let i = 0; i <= 4; i++) {
      options.push({
        value: i,
        label: statusMap[i].label,
        disabled: true
      })
    }
    return options
  }

  // Nếu đã hủy, chỉ hiển thị trạng thái cuối cùng là hủy
  if (isCancelled.value) {
    return [{
      value: 5,
      label: statusMap[5].label,
      disabled: true
    }]
  }

  // Đơn đang xử lý: hiển thị các trạng thái từ hiện tại trở đi
  const options: SelectOption[] = []
  for (let i = current; i <= 5; i++) {
    const isCurrentStatus = i === current
    options.push({
      value: i,
      label: statusMap[i].label,
      disabled: isCurrentStatus // Có thể disable trạng thái hiện tại
    })
  }

  return options
})

const filteredSteps = computed(() => {
  return timelineSteps
})

// Progress
const progressWidth = computed(() => {
  const currentStep = currentStatus.value
  return `${( currentStep < 5 ? (((currentStep + 1) / 5) * 100) : 100)}%` // 4 là số bước tối đa (0-4)
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

const formatDateTime = (timestamp: number | undefined): string => {
  if (!timestamp) return "N/A"
  try {
    const date = new Date(timestamp)
    return date.toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    })
  } catch {
    return "N/A"
  }
}

const getStatusText = (status: string | number | undefined): string => {
  const statusMap: Record<string, string> = {
    '0': 'Chờ xác nhận',
    '1': 'Đã xác nhận',
    '2': 'Chờ giao hàng',
    '3': 'Đang giao hàng',
    '4': 'Hoàn thành',
    '5': 'Đã hủy'
  }

  const statusStr = typeof status === 'number' ? status.toString() : status
  return statusMap[statusStr || '0'] || 'Không xác định'
}

const getStatusIcon = (status: string | number | undefined): any => {
  const statusMap: Record<string, any> = {
    '0': TimeOutline,
    '1': CheckmarkCircleOutline,
    '2': CartOutline,
    '3': CheckmarkCircleOutline,
    '4': CheckmarkDoneOutline,
    '5': CloseCircleOutline
  }

  const statusStr = typeof status === 'number' ? status.toString() : status
  return statusMap[statusStr || '0'] || TimeOutline
}

// Timeline functions
const isStepCompleted = (stepKey: string): boolean => {
  const stepIndex = parseInt(stepKey)
  return stepIndex < currentStatus.value && stepKey !== '5'
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

const getStepTime = (stepKey: string): string | null => {
  if (!invoice.value?.historyStatusInvoice) {
    return null
  }

  try {
    const lichSuArray = JSON.parse(invoice.value.historyStatusInvoice)
    const targetItem = lichSuArray.find(
      (item: any) => item.trangThai?.toString() === stepKey
    )

    if (targetItem?.thoiGian) {
      const [datePart, timePart] = targetItem.thoiGian.split(' ')
      if (!datePart || !timePart) return targetItem.thoiGian

      const [year, month, day] = datePart.split('-')
      const [hours, minutes] = timePart.split(':')
      return `${day}/${month}/${year} ${hours}:${minutes}`
    }

    return null
  } catch (error) {
    console.error('Error parsing lichSuTrangThai:', error)
    return null
  }
}

const getCurrentStatusTextClass = (): string => {
  const classMap: Record<string, string> = {
    '0': 'text-yellow-600',
    '1': 'text-blue-600',
    '2': 'text-purple-600',
    '3': 'text-blue-600',
    '4': 'text-green-600',
    '5': 'text-red-600'
  }

  const statusStr = currentStatus.value.toString()
  return classMap[statusStr] || 'text-gray-600'
}

const getCurrentStepIcon = (): any => {
  return getStatusIcon(currentStatus.value)
}

// Actions

const handleSearch = (): void => {
  if (!searchInvoiceCode.value.trim()) {
    notification.warning({ content: 'Vui lòng nhập mã hóa đơn', duration: 3000 })
    return
  }

  fetchInvoice()
}

const confirmStatusUpdate = async (): Promise<void> => {
  if (selectedStatus.value === null || !invoice.value?.code) {
    notification.error({ content: 'Vui lòng chọn trạng thái mới', duration: 3000 })
    return
  }

  isUpdating.value = true
  try {
    // TODO: Gọi API changeOrderStatus tại đây
    message.success('Cập nhật trạng thái thành công')
    showStatusModal.value = false
    // TODO: Reload dữ liệu
  } catch (error: any) {
    console.error('Error updating status:', error)
    message.error(error.message || 'Đã xảy ra lỗi khi cập nhật')
  } finally {
    isUpdating.value = false
  }
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

const fetchInvoice = async (): Promise<void> => {
  try {
    const response = await getInvoiceById(searchInvoiceCode.value)

    invoice.value = response.data
  } catch (error: any) {
    invoice.value = null
  }
}

// Lifecycle
onMounted(() => {
  console.log("Tracking View loaded")
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
