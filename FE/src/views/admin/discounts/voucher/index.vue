<script lang="ts" setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton,
  NCard,
  NDataTable,
  NIcon,
  NInput,
  NPagination,
  NPopconfirm, // Đã thêm
  NSelect,
  NSpace,
  NSwitch, // Đã thêm
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'
import { saveAs } from 'file-saver'

// API
import {
  changeStatusVoucher,
  getVouchers,
  updateVoucherToStart,
} from '@/service/api/admin/discount/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'
import request from '@/service/request' // Import request để gọi API export blob
import { API_ADMIN_DISCOUNTS_VOUCHER } from '@/constants/url'

/* ===================== Config & Router ===================== */
const router = useRouter()
const message = useMessage()

/* ===================== Formatters ===================== */
function formatDateTime(timestamp: number | undefined) {
  if (!timestamp)
    return '-'
  return new Date(timestamp).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

/* ===================== Logic Trạng Thái (CORE) ===================== */
function getVoucherStatus(row: ADVoucherResponse) {
  const now = Date.now()
  const startDate = row.startDate || 0
  const endDate = row.endDate || Infinity
  const remainingQuantity = row.remainingQuantity

  // 1. Sắp diễn ra
  if (startDate > now)
    return { text: 'Sắp diễn ra', type: 'info', value: 'UPCOMING' }

  // 2. Đã kết thúc
  const isExpired = endDate < now
  const isOutOfStock = remainingQuantity !== null && remainingQuantity <= 0
  if (isExpired || isOutOfStock)
    return { text: 'Đã kết thúc', type: 'default', value: 'ENDED' }

  // 3. Tạm dừng (Backend trả về status = 1 là INACTIVE)
  const isPaused = row.status === 1 || row.status === 'INACTIVE'
  if (isPaused)
    return { text: 'Tạm dừng', type: 'warning', value: 'PAUSED' }

  // 4. Đang diễn ra
  return { text: 'Đang diễn ra', type: 'success', value: 'ONGOING' }
}

function getVoucherTypeText(row: ADVoucherResponse) {
  if (row.targetType === 'INDIVIDUAL')
    return { text: 'Cá nhân', type: 'info' }
  if (row.targetType === 'ALL_CUSTOMERS')
    return { text: 'Công khai', type: 'primary' }
  return { text: '—', type: 'default' }
}

/* ===================== State ===================== */
const loading = ref(false)
const exportLoading = ref(false)
const allData = ref<ADVoucherResponse[]>([]) // Giữ nguyên logic load all client side
const displayData = ref<ADVoucherResponse[]>([])
const checkedRowKeys = ref<(string | number)[]>([])

const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
})

const dynamicMaxPrice = computed(() => {
  if (allData.value.length === 0)
    return 5000000
  const maxInList = Math.max(...allData.value.map(item => item.maxValue || item.discountValue || 0))
  return Math.ceil((maxInList + 100000) / 100000) * 100000
})

/* ===================== Actions ===================== */
function openAddPage() { router.push({ name: 'discounts_voucher_add' }) }
function openEditPage(id: string) { router.push({ name: 'discounts_voucher_edit', params: { id } }) }
function openDetailPage(id: string) { router.push({ name: 'discounts_voucher_detail', params: { id } }) }

// Kích hoạt voucher Sắp diễn ra
async function handleStartVoucher(id: string) {
  loading.value = true
  try {
    await updateVoucherToStart(id)
    message.success('Đã kích hoạt phiếu giảm giá!')
    await fetchData()
  }
  catch (err: any) {
    message.error(err.response?.data?.message || 'Lỗi khi kích hoạt')
  }
  finally { loading.value = false }
}

// Switch: Đổi trạng thái (Đang diễn ra <-> Tạm dừng)
async function handleSwitchStatus(row: ADVoucherResponse) {
  const originalStatus = row.status
  // Logic đảo status: Nếu đang 0 (Active) -> gửi request để thành 1 (Inactive)
  const isCurrentlyActive = row.status === 0 || row.status === 'ACTIVE'

  // Optimistic Update
  row.status = isCurrentlyActive ? 1 : 0

  try {
    await changeStatusVoucher(row.id!)
    message.success(`Đã ${isCurrentlyActive ? 'tạm dừng' : 'tiếp tục'} voucher!`)
  }
  catch (err: any) {
    row.status = originalStatus // Hoàn tác
    message.error(err.response?.data?.message || 'Lỗi khi thay đổi trạng thái')
  }
}

/* ===================== Filters ===================== */
const filters = reactive({
  keyword: '',
  startDate: null as number | null,
  endDate: null as number | null,
  typeVoucher: null as string | null,
  targetType: null as string | null,
  status: null as string | null,
  maxDiscountRange: [0, 5000000] as [number, number],
})

watch(dynamicMaxPrice, (newMax) => {
  if (filters.maxDiscountRange[1] === 5000000 || filters.maxDiscountRange[1] > newMax) {
    filters.maxDiscountRange = [0, newMax]
  }
})

const typeVoucherOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Giảm tiền', value: 'FIXED_AMOUNT' },
  { label: 'Giảm %', value: 'PERCENTAGE' },
]

const targetTypeOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Công khai', value: 'ALL_CUSTOMERS' },
  { label: 'Cá nhân', value: 'INDIVIDUAL' },
]

const statusOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Sắp diễn ra', value: 'UPCOMING' },
  { label: 'Đang diễn ra', value: 'ONGOING' },
  { label: 'Tạm dừng', value: 'PAUSED' },
  { label: 'Đã kết thúc', value: 'ENDED' },
]

const handleSearch = debounce(() => {
  pagination.value.page = 1
  if (allData.value.length === 0)
    fetchData()
  else handleClientSideFilter()
}, 500)

function resetFilters() {
  filters.keyword = ''
  filters.startDate = null
  filters.endDate = null
  filters.typeVoucher = null
  filters.targetType = null
  filters.status = null
  filters.maxDiscountRange = [0, dynamicMaxPrice.value]
  pagination.value.page = 1
  handleClientSideFilter()
}

// Logic lọc Client-side (GIỮ NGUYÊN ĐỂ ĐẢM BẢO DỮ LIỆU KHÔNG BỊ LỖI)
function handleClientSideFilter() {
  loading.value = true
  let filtered = allData.value

  if (filters.keyword) {
    const k = filters.keyword.toLowerCase()
    filtered = filtered.filter(item =>
      (item.code && item.code.toLowerCase().includes(k)) || (item.name && item.name.toLowerCase().includes(k)),
    )
  }

  if (filters.startDate)
    filtered = filtered.filter(item => (item.startDate || 0) >= filters.startDate!)
  if (filters.endDate)
    filtered = filtered.filter(item => (item.endDate || 0) <= filters.endDate!)
  if (filters.typeVoucher)
    filtered = filtered.filter(item => item.typeVoucher === filters.typeVoucher)
  if (filters.targetType)
    filtered = filtered.filter(item => item.targetType === filters.targetType)

  // Lọc Status (Sử dụng hàm logic chung)
  if (filters.status) {
    filtered = filtered.filter(item => getVoucherStatus(item).value === filters.status)
  }

  const [minPrice, maxPrice] = filters.maxDiscountRange
  filtered = filtered.filter((item) => {
    const val = item.maxValue || item.discountValue || 0
    return val >= minPrice && val <= maxPrice
  })

  pagination.value.itemCount = filtered.length
  const startIndex = (pagination.value.page - 1) * pagination.value.pageSize
  const endIndex = startIndex + pagination.value.pageSize
  displayData.value = filtered.slice(startIndex, endIndex)
  loading.value = false
}

// --- LOGIC XUẤT EXCEL TỪ BACKEND (MỚI) ---
async function handleExportExcel() {
  exportLoading.value = true
  message.loading('Đang yêu cầu server xuất file...')
  try {
    // Mapping params bộ lọc hiện tại để gửi lên BE
    const params = {
      q: filters.keyword || undefined,
      typeVoucher: filters.typeVoucher || undefined,
      targetType: filters.targetType || undefined,
      // Map 'status' filter của frontend sang 'period' của backend (UPCOMING, PAUSED,...)
      period: filters.status || undefined,
      startDate: filters.startDate || undefined,
      endDate: filters.endDate || undefined,
    }

    // Gọi API với responseType là blob để nhận file
    const response = await request.get(`${API_ADMIN_DISCOUNTS_VOUCHER}/export`, {
      params,
      responseType: 'blob',
    })

    const blob = new Blob([response.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    saveAs(blob, `Danh_Sach_Voucher_${new Date().toISOString().slice(0, 10)}.xlsx`)
    message.success(`Xuất ${pagination.value.itemCount} file thành công!`)
  }
  catch (error) {
    console.error(error)
    message.error('Lỗi khi xuất file Excel từ server')
  }
  finally {
    exportLoading.value = false
  }
}

watch(() => [filters.startDate, filters.endDate, filters.typeVoucher, filters.targetType, filters.status, filters.maxDiscountRange], () => {
  pagination.value.page = 1
  handleClientSideFilter()
}, { deep: true })

watch(() => [pagination.value.page, pagination.value.pageSize], () => {
  handleClientSideFilter()
})

async function fetchData() {
  loading.value = true
  try {
    // Load size lớn để lọc client-side (như code cũ của bạn)
    const query: ADVoucherQuery = { page: 1, size: 2000 }
    const res = await getVouchers(query)
    allData.value = (res.content ?? []).map(item => ({ ...item }))
    handleClientSideFilter()
    checkedRowKeys.value = []
  }
  catch (err: any) {
    message.error(`Lỗi tải dữ liệu: ${err.message || 'Error'}`)
    allData.value = []
    displayData.value = []
  }
  finally {
    loading.value = false
  }
}

/* ===================== Data Table Config ===================== */
const columns: DataTableColumns<ADVoucherResponse> = [
  {
    title: 'STT',
    key: 'stt',
    fixed: 'left',
    align: 'center',
    width: 60,
    render: (_, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize,
  },
  {
    title: 'Mã',
    key: 'code',
    fixed: 'left',
    width: 100,
    render: row => h('strong', { class: 'text-primary cursor-pointer', onClick: () => openDetailPage(row.id!) }, row.code),
  },
  {
    title: 'Tên',
    key: 'name',
    fixed: 'left',
    width: 150,
    ellipsis: { tooltip: true },
  },
  {
    title: 'Kiểu',
    align: 'center',
    key: 'targetType',
    width: 90,
    render: row => h(NTag, { type: getVoucherTypeText(row).type, size: 'small', bordered: false }, { default: () => getVoucherTypeText(row).text }),
  },
  {
    title: 'Số lượng',
    align: 'center',
    width: 90,
    key: 'remainingQuantity',
  },
  {
    title: 'Chi tiết ưu đãi',
    key: 'discountValue',
    width: 180,
    align: 'left',
    render(row) {
      const isPercent = row.typeVoucher === 'PERCENTAGE'
      const nodes = []
      nodes.push(h(NTag, { type: isPercent ? 'error' : 'primary', size: 'small', class: 'font-bold' }, { default: () => isPercent ? `Giảm ${row.discountValue}%` : `Giảm ${formatCurrency(row.discountValue || 0)}` },
      ))
      if (isPercent) {
        nodes.push(h('div', { class: 'text-[11px] text-gray-500 mt-1' }, row.maxValue ? `(Tối đa: ${formatCurrency(row.maxValue)})` : '(Không giới hạn)'))
      }
      if (row.conditions) {
        nodes.push(h('div', { class: 'text-[11px] text-gray-500 mt-0.5' }, `Đơn tối thiểu: ${formatCurrency(row.conditions)}`))
      }
      else {
        nodes.push(h('div', { class: 'text-[11px] text-gray-400 mt-0.5 italic' }, 'Không yêu cầu đơn tối thiểu'))
      }
      return h('div', { class: 'flex flex-col items-start' }, nodes)
    },
  },
  {
    title: 'Thời gian áp dụng',
    key: 'time',
    width: 150,
    render: (row) => {
      return h('div', { class: 'flex flex-col text-xs' }, [
        h('div', { class: 'text-gray-500' }, [h('span', { class: 'font-semibold' }, 'Từ: '), formatDateTime(row.startDate)]),
        h('div', { class: 'text-gray-500 mt-1' }, [h('span', { class: 'font-semibold' }, 'Đến: '), formatDateTime(row.endDate)]),
      ])
    },
  },
  {
    title: 'Trạng thái',
    key: 'computedStatus',
    width: 100,
    align: 'center',
    render(row) {
      const statusInfo = getVoucherStatus(row)
      return h(NTag, {
        type: statusInfo.type,
        size: 'small',
        style: { cursor: 'pointer' },
        onClick: () => { filters.status = statusInfo.value },
      }, { default: () => statusInfo.text })
    },
  },
  // --- CỘT THAO TÁC (CHỈNH SỬA) ---
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 80,
    fixed: 'right',
    render(row: ADVoucherResponse) {
      const id = row.id
      if (!id)
        return '-'

      const statusInfo = getVoucherStatus(row)
      const actions = []
      const isUpcoming = statusInfo.value === 'UPCOMING'
      const isEnded = statusInfo.value === 'ENDED'
      const cantEdit = statusInfo.value === 'ONGOING' || statusInfo.value === 'PAUSED'
      const canEdit = !isEnded && !cantEdit

      // 1. Nút Xem/Sửa
      const btnConfig = canEdit
        ? { icon: 'carbon:edit', type: 'warning', tooltip: 'Sửa thông tin' }
        : { icon: 'carbon:view', type: 'info', tooltip: 'Xem chi tiết' }

      actions.push(h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h(NButton, {
          size: 'small',
          type: btnConfig.type as any,
          secondary: true,
          circle: true,
          class: 'mr-2 transition-all duration-200 hover:scale-[1.3] hover:shadow-lg',
          onClick: () => canEdit ? openEditPage(id) : openDetailPage(id),
        }, { icon: () => h(Icon, { icon: btnConfig.icon }) }),
        default: () => btnConfig.tooltip,
      }))

      // 2. Nút BẮT ĐẦU (Play) - Có

      if (isUpcoming) {
        actions.push(h(NPopconfirm, { positiveText: 'Xác nhận', negativeText: 'Hủy', onPositiveClick: () => handleStartVoucher(id) }, {
          trigger: () => h(NTooltip, { trigger: 'hover' }, {
            trigger: () => h(NButton, {
              size: 'small',
              type: 'success',
              secondary: true,
              circle: true,
              class: 'hover:scale-[1.3]',
            }, { icon: () => h(Icon, { icon: 'carbon:play-filled-alt' }) }),
            default: () => 'Kích hoạt ngay',
          }),
          default: () => 'Kích hoạt Voucher này ngay bây giờ?',
        }))
      }

      // 3. SWITCH (Dừng/Tiếp tục) - Có Confirm
      else if (!isEnded) {
        const isChecked = row.status === 0 || row.status === 'ACTIVE'

        actions.push(h(NPopconfirm, { positiveText: 'Xác nhận', negativeText: 'Hủy', onPositiveClick: () => handleSwitchStatus(row) }, {
          trigger: () => h(NTooltip, { trigger: 'hover' }, {
            // Dùng div bọc switch và pointerEvents: none để bắt sự kiện click vào div -> kích hoạt Popconfirm
            trigger: () => h('div', { style: 'display: inline-block; cursor: pointer' }, [
              h(NSwitch, {
                value: isChecked,
                size: 'small',
                style: { pointerEvents: 'none' }, // Vô hiệu hóa click trực tiếp lên switch
              }),
            ]),
            default: () => isChecked ? 'Tạm dừng' : 'Tiếp tục',
          }),
          default: () => isChecked
            ? 'Bạn có chắc chắn muốn TẠM DỪNG voucher này?'
            : 'Bạn có chắc chắn muốn TIẾP TỤC voucher này?',
        }))
      }

      return h('div', { class: 'flex justify-center items-center' }, actions)
    },
  },
]

onMounted(() => fetchData())
</script>

<template>
  <div>
    <NCard class="mb-3 shadow-sm border-none">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-black-600">
            <Icon icon="icon-park-outline:ticket" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">Quản lý Phiếu Giảm Giá</span>
        </NSpace>
        <span class="text-gray-500">Quản lý danh sách Phiếu Giảm Giá có mặt tại cửa hàng</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="shadow-sm rounded-xl border border-gray-100 mb-4">
      <template #header-extra>
        <div class="mr-5">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton
                size="large" circle secondary type="success"
                class="transition-all duration-200 hover:scale-110 hover:shadow-md" @click="resetFilters"
              >
                <NIcon size="24">
                  <Icon icon="carbon:filter-reset" />
                </NIcon>
              </NButton>
            </template>
            Làm mới bộ lọc
          </NTooltip>
        </div>
      </template>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-6">
        <div class="lg:col-span-3">
          <div class="text-xs font-bold text-black-600 mb-1 ml-1">
            Tìm kiếm chung
          </div>
          <NInput
            v-model:value="filters.keyword" placeholder="Tìm theo mã hoặc tên phiếu..." clearable
            @input="handleSearch"
          >
            <template #prefix>
              <NIcon>
                <Icon icon="carbon:search" class="text-gray-600" />
              </NIcon>
            </template>
          </NInput>
        </div>

        <div class="lg:col-span-1">
          <div class="text-xs font-bold text-black-600 mb-1 ml-1">
            Kiểu voucher
          </div>
          <NSelect v-model:value="filters.typeVoucher" :options="typeVoucherOptions" placeholder="Tất cả" />
        </div>

        <div class="lg:col-span-1">
          <div class="text-xs font-bold text-black-600 mb-1 ml-1">
            Đối tượng
          </div>
          <NSelect v-model:value="filters.targetType" :options="targetTypeOptions" placeholder="Tất cả" />
        </div>

        <div class="lg:col-span-1">
          <div class="text-xs font-bold text-black-600 mb-1 ml-1">
            Trạng thái
          </div>
          <NSelect v-model:value="filters.status" :options="statusOptions" placeholder="Tất cả" />
        </div>
      </div>
    </NCard>

    <NCard title="Danh sách Phiếu Giảm Giá" class="border rounded-2xl shadow-sm border-gray-100">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary" secondary
              class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg"
              @click="openAddPage"
            >
              <template #icon>
                <NIcon size="20">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2"
              >
                Tạo mới
              </span>
            </NButton>

            <NButton
              type="success" secondary
              class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg"
              :loading="exportLoading" :disabled="loading" @click="handleExportExcel"
            >
              <template #icon>
                <NIcon size="20">
                  <Icon icon="file-icons:microsoft-excel" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2"
              >
                Xuất Excel
              </span>
            </NButton>

            <NButton
              type="info" secondary
              class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg"
              @click="fetchData"
            >
              <template #icon>
                <NIcon size="20">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              <span
                class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2"
              >
                Tải lại
              </span>
            </NButton>
          </NSpace>
        </div>
      </template>

      <NDataTable
        v-model:checked-row-keys="checkedRowKeys" :columns="columns" :data="displayData" :loading="loading"
        :row-key="(row) => row.id" :pagination="false" striped :scroll-x="1200" class="rounded-lg overflow-hidden"
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="pagination.page" v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount" :page-sizes="[5, 10, 20, 50]" :show-size-picker="true"
        />
      </div>
    </NCard>
  </div>
</template>

<style scoped>
:deep(.n-input .n-input__input-el) {
  font-size: 14px;
}

/* Slider màu xanh lá */
:deep(.n-slider .n-slider-rail .n-slider-rail__fill) {
  background-color: #16a34a !important;
}

:deep(.n-slider:hover .n-slider-rail .n-slider-rail__fill) {
  background-color: #15803d !important;
}
</style>
