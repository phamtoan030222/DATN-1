<script lang="ts" setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NIcon,
  NInput,
  NPagination,
  NPopconfirm,
  NSelect,
  NSlider,
  NSpace,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { debounce } from 'lodash'
import ExcelJS from 'exceljs'
import { saveAs } from 'file-saver'

import {
  getVouchers,
  updateVoucherToEnd,
  updateVoucherToStart,
} from '@/service/api/admin/discount/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'

/* ===================== Config & Router ===================== */
const router = useRouter()
const message = useMessage()

const STEP_PRICE = 10000

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

/* ===================== Utility Functions ===================== */
function getVoucherStatus(row: ADVoucherResponse) {
  const now = Date.now()
  const startDate = row.startDate
  const endDate = row.endDate
  const remainingQuantity = row.remainingQuantity
  const targetType = row.targetType

  if (startDate && startDate > now)
    return { text: 'Sắp diễn ra', type: 'info', value: 'UPCOMING' }

  const isExpiredByDate = endDate && endDate < now
  const isExpiredByQuantity = targetType === 'ALL_CUSTOMERS' && (remainingQuantity === null || remainingQuantity <= 0)

  if (isExpiredByDate || isExpiredByQuantity)
    return { text: 'Đã kết thúc', type: 'default', value: 'ENDED' }

  const isWithinTime = (startDate || 0) <= now && (endDate || Infinity) >= now
  const hasRemainingQuantity = targetType === 'INDIVIDUAL' || (targetType === 'ALL_CUSTOMERS' && (remainingQuantity || 0) > 0)

  if (isWithinTime && hasRemainingQuantity)
    return { text: 'Đang diễn ra', type: 'success', value: 'ONGOING' }

  return { text: 'Không xác định', type: 'default', value: 'UNKNOWN' }
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
const exportLoading = ref(false) // Loading riêng cho nút xuất Excel
const allData = ref<ADVoucherResponse[]>([])
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

async function handleEndVoucher(id: string) {
  loading.value = true
  try {
    await updateVoucherToEnd(id)
    message.success('Đã kết thúc phiếu giảm giá!')
    await fetchData()
  }
  catch (err: any) {
    message.error(err.response?.data?.message || 'Lỗi khi kết thúc')
  }
  finally { loading.value = false }
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

const typeVoucherOptions = [{ label: 'Tất cả', value: null }, { label: 'Giảm tiền', value: 'FIXED_AMOUNT' }, { label: 'Giảm %', value: 'PERCENTAGE' }]
const targetTypeOptions = [{ label: 'Tất cả', value: null }, { label: 'Công khai', value: 'ALL_CUSTOMERS' }, { label: 'Cá nhân', value: 'INDIVIDUAL' }]
const statusOptions = [{ label: 'Tất cả', value: null }, { label: 'Sắp diễn ra', value: 'UPCOMING' }, { label: 'Đang diễn ra', value: 'ONGOING' }, { label: 'Đã kết thúc', value: 'ENDED' }]

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

// Logic lọc dữ liệu phía Client
function handleClientSideFilter() {
  loading.value = true

  // 1. Lọc từ dữ liệu gốc (allData)
  let filtered = allData.value

  // Lọc Keyword
  if (filters.keyword) {
    const k = filters.keyword.toLowerCase()
    filtered = filtered.filter(item =>
      (item.code && item.code.toLowerCase().includes(k))
      || (item.name && item.name.toLowerCase().includes(k)),
    )
  }

  // Lọc Ngày
  if (filters.startDate)
    filtered = filtered.filter(item => (item.startDate || 0) >= filters.startDate!)
  if (filters.endDate)
    filtered = filtered.filter(item => (item.endDate || 0) <= filters.endDate!)

  // Lọc Dropdown
  if (filters.typeVoucher)
    filtered = filtered.filter(item => item.typeVoucher === filters.typeVoucher)
  if (filters.targetType)
    filtered = filtered.filter(item => item.targetType === filters.targetType)
  if (filters.status)
    filtered = filtered.filter(item => getVoucherStatus(item).value === filters.status)

  // Lọc Slider (Giá)
  const [minPrice, maxPrice] = filters.maxDiscountRange
  filtered = filtered.filter((item) => {
    const val = item.maxValue || item.discountValue || 0
    return val >= minPrice && val <= maxPrice
  })

  // 2. Cập nhật phân trang
  pagination.value.itemCount = filtered.length
  const startIndex = (pagination.value.page - 1) * pagination.value.pageSize
  const endIndex = startIndex + pagination.value.pageSize

  // 3. Cắt dữ liệu để hiển thị
  displayData.value = filtered.slice(startIndex, endIndex)
  loading.value = false
}

// --- LOGIC XUẤT EXCEL THEO BỘ LỌC ---
async function handleExportExcel() {
  exportLoading.value = true
  message.loading('Đang xử lý dữ liệu xuất Excel...')

  try {
    // 1. Gọi API lấy dữ liệu (Size lớn để lấy hết)
    // Lưu ý: Ta gửi các params lọc lên Server để Server lọc bớt phần lớn dữ liệu
    const query: ADVoucherQuery = {
      page: 1,
      size: 100000,
      keyword: filters.keyword,
      startDate: filters.startDate,
      endDate: filters.endDate,
      typeVoucher: filters.typeVoucher,
      targetType: filters.targetType,
      status: filters.status,
    }

    const res = await getVouchers(query)
    let dataToExport = res.content || []

    // 2. Lọc lại bằng Slider (Client-side logic)
    // Vì slider thường lọc trên RAM chứ API ít hỗ trợ range custom này
    const [minPrice, maxPrice] = filters.maxDiscountRange
    // Chỉ lọc nếu người dùng có kéo slider khác mặc định
    if (minPrice > 0 || maxPrice < dynamicMaxPrice.value) {
      dataToExport = dataToExport.filter((item) => {
        const val = item.maxValue || item.discountValue || 0
        return val >= minPrice && val <= maxPrice
      })
    }

    if (dataToExport.length === 0) {
      message.warning('Không có dữ liệu nào phù hợp bộ lọc để xuất!')
      exportLoading.value = false
      return
    }

    // 3. Tạo file Excel
    const workbook = new ExcelJS.Workbook()
    const worksheet = workbook.addWorksheet('Danh sách Voucher')

    // 4. Định nghĩa cột
    worksheet.columns = [
      { header: 'STT', key: 'stt', width: 8 },
      { header: 'Mã Voucher', key: 'code', width: 15 },
      { header: 'Tên Voucher', key: 'name', width: 25 },
      { header: 'Loại', key: 'type', width: 15 },
      { header: 'Đối tượng', key: 'target', width: 15 },
      { header: 'Giá trị giảm', key: 'value', width: 15 },
      { header: 'Giảm tối đa', key: 'maxValue', width: 15 },
      { header: 'Đơn tối thiểu', key: 'condition', width: 18 },
      { header: 'Số lượng', key: 'quantity', width: 12 },
      { header: 'Bắt đầu', key: 'startDate', width: 20 },
      { header: 'Kết thúc', key: 'endDate', width: 20 },
      { header: 'Trạng thái', key: 'status', width: 15 },
    ]

    // Style Header (Màu xanh lá)
    worksheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFF' } }
    worksheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '16A34A' } }
    worksheet.getRow(1).alignment = { vertical: 'middle', horizontal: 'center' }

    // 5. Đổ dữ liệu vào dòng
    dataToExport.forEach((item, index) => {
      const isPercent = item.typeVoucher === 'PERCENTAGE'
      const statusInfo = getVoucherStatus(item)

      worksheet.addRow({
        stt: index + 1,
        code: item.code,
        name: item.name,
        type: isPercent ? 'Phần trăm (%)' : 'Tiền mặt',
        target: item.targetType === 'INDIVIDUAL' ? 'Cá nhân' : 'Công khai',
        value: isPercent ? `${item.discountValue}%` : formatCurrency(item.discountValue || 0),
        maxValue: isPercent && item.maxValue ? formatCurrency(item.maxValue) : '-',
        condition: item.conditions ? formatCurrency(item.conditions) : 'Không có',
        quantity: item.targetType === 'INDIVIDUAL' ? 'Riêng' : (item.remainingQuantity ?? '∞'),
        // Sử dụng helper formatDateTime để giống giao diện
        startDate: formatDateTime(item.startDate),
        endDate: formatDateTime(item.endDate),
        status: statusInfo.text,
      })
    })

    // 6. Tải file về
    const buffer = await workbook.xlsx.writeBuffer()
    const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    saveAs(blob, `Danh_Sach_Voucher_${new Date().toISOString().slice(0, 10)}.xlsx`)

    message.success(`Đã xuất ${dataToExport.length} bản ghi thành công!`)
  }
  catch (error) {
    console.error(error)
    message.error('Lỗi khi tạo file Excel')
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
    const query: ADVoucherQuery = { page: 1, size: 1000 }
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
    width: 120,
    render: row => h('strong', { class: 'text-primary cursor-pointer', onClick: () => openEditPage(row.id!) }, row.code),
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
    render: (row) => {
      if (row.targetType === 'INDIVIDUAL')
        return 'Riêng'
      return row.quantity !== null ? `${row.remainingQuantity ?? 0}/${row.quantity}` : '∞'
    },
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
        nodes.push(h('div', { class: 'text-[11px] text-gray-500 mt-1' }, row.maxValue ? `(Tối đa: ${formatCurrency(row.maxValue)})` : '(Không giới hạn)',
        ))
      }

      if (row.conditions) {
        nodes.push(h('div', { class: 'text-[11px] text-gray-500 mt-0.5' }, `Đơn tối thiểu: ${formatCurrency(row.conditions)}`,
        ))
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
        h('div', { class: 'text-gray-500' }, [
          h('span', { class: 'font-semibold' }, 'Từ: '),
          formatDateTime(row.startDate),
        ]),
        h('div', { class: 'text-gray-500 mt-1' }, [
          h('span', { class: 'font-semibold' }, 'Đến: '),
          formatDateTime(row.endDate),
        ]),
      ])
    },
  },
  {
    title: 'Trạng thái',
    key: 'computedStatus',
    width: 110,
    align: 'center',
    render(row) {
      const statusInfo = getVoucherStatus(row)
      return h(NTag, { type: statusInfo.type, size: 'small', style: { cursor: 'pointer' }, onClick: () => { filters.status = statusInfo.value } }, { default: () => statusInfo.text })
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 100,
    fixed: 'right',
    render(row: ADVoucherResponse) {
      const id = row.id
      if (!id)
        return '-'
      const statusInfo = getVoucherStatus(row)
      const actions = []

      const isUpcoming = statusInfo.value === 'UPCOMING'

      const btnConfig = isUpcoming
        ? { icon: 'carbon:edit', type: 'warning', tooltip: 'Sửa thông tin' }
        : { icon: 'carbon:view', type: 'info', tooltip: 'Xem chi tiết' }

      actions.push(h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h(NButton, {
          size: 'small',
          type: btnConfig.type as any,
          secondary: true,
          circle: true,
          class: 'transition-all duration-200 hover:scale-[1.3] hover:shadow-lg',
          onClick: () => openEditPage(id),
        }, { icon: () => h(Icon, { icon: btnConfig.icon }) }),
        default: () => btnConfig.tooltip,
      }))

      if (statusInfo.value === 'UPCOMING') {
        actions.push(h(NPopconfirm, { onPositiveClick: () => handleStartVoucher(id) }, {
          trigger: () => h(NTooltip, { trigger: 'hover' }, { trigger: () => h(NButton, { size: 'small', type: 'success', secondary: true, circle: true, class: 'ml-2 hover:scale-[1.3]' }, { icon: () => h(Icon, { icon: 'carbon:play-filled-alt' }) }), default: () => 'Kích hoạt ngay' }),
          default: () => 'Kích hoạt Voucher này ngay bây giờ?',
        }))
      }

      if (statusInfo.value === 'ONGOING') {
        actions.push(h(NPopconfirm, { onPositiveClick: () => handleEndVoucher(id) }, {
          trigger: () => h(NTooltip, { trigger: 'hover' }, { trigger: () => h(NButton, { size: 'small', type: 'error', secondary: true, circle: true, class: 'ml-2 hover:scale-[1.3]' }, { icon: () => h(Icon, { icon: 'carbon:stop-filled-alt' }) }), default: () => 'Kết thúc ngay' }),
          default: () => 'Xác nhận kết thúc Voucher này?',
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
          <NIcon size="24" class="text-green-600">
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
          <div class="text-xs font-bold text-green-600 mb-1 ml-1 uppercase">
            Tìm kiếm chung
          </div>
          <NInput
            v-model:value="filters.keyword" placeholder="Tìm theo mã hoặc tên phiếu..." clearable
            @input="handleSearch"
          >
            <template #prefix>
              <NIcon icon="carbon:search" class="text-green-600" />
            </template>
          </NInput>
        </div>

        <div class="lg:col-span-3">
          <div class="text-xs font-bold text-green-600 mb-1 ml-1 uppercase">
            Thời gian áp dụng
          </div>
          <div class="flex items-center gap-2">
            <NDatePicker v-model:value="filters.startDate" type="date" placeholder="Từ ngày" class="w-full" clearable />
            <span class="text-gray-400">-</span>
            <NDatePicker v-model:value="filters.endDate" type="date" placeholder="Đến ngày" class="w-full" clearable />
          </div>
        </div>

        <div class="lg:col-span-1">
          <div class="text-xs font-bold text-green-600 mb-1 ml-1 uppercase">
            Kiểu voucher
          </div>
          <NSelect v-model:value="filters.typeVoucher" :options="typeVoucherOptions" placeholder="Tất cả" />
        </div>

        <div class="lg:col-span-1">
          <div class="text-xs font-bold text-green-600 mb-1 ml-1 uppercase">
            Đối tượng
          </div>
          <NSelect v-model:value="filters.targetType" :options="targetTypeOptions" placeholder="Tất cả" />
        </div>

        <div class="lg:col-span-1">
          <div class="text-xs font-bold text-green-600 mb-1 ml-1 uppercase">
            Trạng thái
          </div>
          <NSelect v-model:value="filters.status" :options="statusOptions" placeholder="Tất cả" />
        </div>

        <div class="lg:col-span-3">
          <div class="flex justify-between items-center mb-1 ml-1">
            <div class="text-xs font-bold text-green-600 uppercase">
              Giảm tối đa
            </div>
            <div class="text-[10px] text-green-700 font-mono bg-green-50 px-1 rounded">
              {{ formatCurrency(filters.maxDiscountRange[0]) }} - {{ formatCurrency(filters.maxDiscountRange[1]) }}
            </div>
          </div>
          <div class="px-2 pt-1">
            <NSlider
              v-model:value="filters.maxDiscountRange" range :min="0" :max="dynamicMaxPrice" :step="STEP_PRICE"
              :tooltip="false"
            />
          </div>
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
              type="warning" secondary
              class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg"
              :loading="exportLoading"
              :disabled="loading"
              @click="handleExportExcel"
            >
              <template #icon>
                <NIcon size="20">
                  <Icon icon="file-icons:microsoft-excel" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
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
        :row-key="(row) => row.id" :pagination="false" striped :scroll-x="1200"
        class="rounded-lg overflow-hidden"
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
