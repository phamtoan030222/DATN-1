<script lang="ts" setup>
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { DataTableColumns } from 'naive-ui'
import {
  NAvatar,
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NIcon,
  NInput,
  NPagination,
  NSelect,
  NSpace,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
// Thêm thư viện Excel
import ExcelJS from 'exceljs'
import { saveAs } from 'file-saver'

// --- MOCK DATA TYPE ---
interface Invoice {
  id: string
  stt: number
  code: string
  customerName: string
  phoneNumber: string
  staffName: string
  staffCode: string
  totalAmount: number
  invoiceType: 'INSTORE' | 'ONLINE'
  status: 'COMPLETED' | 'INCOMPLETE'
  createdAt: number
}

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

/* ===================== Utility Functions ===================== */
function getStatusConfig(status: string) {
  if (status === 'COMPLETED')
    return { text: 'Hoàn thành', type: 'success', icon: 'carbon:checkmark-filled' }
  return { text: 'Chưa hoàn thành', type: 'warning', icon: 'carbon:time-filled' }
}

function getInvoiceTypeConfig(type: string) {
  if (type === 'INSTORE')
    return { text: 'Tại quầy', type: 'info', icon: 'carbon:store' }
  return { text: 'Online', type: 'primary', icon: 'carbon:earth-southeast-asia-filled' }
}

/* ===================== State ===================== */
const loading = ref(false)
const exportLoading = ref(false) // State cho nút xuất Excel
const displayData = ref<Invoice[]>([])
const allMockData = ref<Invoice[]>([]) // Lưu toàn bộ data để lọc khi xuất excel

const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
})

const todayStart = new Date(); todayStart.setHours(0, 0, 0, 0)
const todayEnd = new Date(); todayEnd.setHours(23, 59, 59, 999)

/* ===================== Filters ===================== */
const filters = reactive({
  keyword: '',
  dateRange: [todayStart.getTime(), todayEnd.getTime()] as [number, number] | null,
  invoiceType: null as string | null,
  status: null as string | null,
})

const invoiceTypeOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Tại quầy', value: 'INSTORE' },
  { label: 'Online', value: 'ONLINE' },
]

const statusOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Hoàn thành', value: 'COMPLETED' },
  { label: 'Chưa hoàn thành', value: 'INCOMPLETE' },
]

function resetFilters() {
  filters.keyword = ''
  const start = new Date(); start.setHours(0, 0, 0, 0)
  const end = new Date(); end.setHours(23, 59, 59, 999)
  filters.dateRange = [start.getTime(), end.getTime()]
  filters.invoiceType = null
  filters.status = null
  pagination.value.page = 1
  fetchData()
}

/* ===================== Actions ===================== */
// TRONG FILE invoiceList.vue

function openDetailPage(id: string) {
  router.push({
    name: 'invoice_list_detail',
    params: { id },
  })
}

// --- LOGIC XUẤT EXCEL ---
async function handleExportExcel() {
  exportLoading.value = true
  message.loading('Đang xử lý dữ liệu xuất Excel...')

  try {
    // 1. Lọc lại dữ liệu (Lấy toàn bộ kết quả phù hợp filter, không phân trang)
    let dataToExport = allMockData.value

    if (filters.keyword) {
      const k = filters.keyword.toLowerCase()
      dataToExport = dataToExport.filter(i =>
        i.code.toLowerCase().includes(k)
        || i.customerName.toLowerCase().includes(k)
        || i.phoneNumber.includes(k),
      )
    }
    if (filters.dateRange) {
      const [start, end] = filters.dateRange
      dataToExport = dataToExport.filter(i => i.createdAt >= start && i.createdAt <= end)
    }
    if (filters.invoiceType) {
      dataToExport = dataToExport.filter(i => i.invoiceType === filters.invoiceType)
    }
    if (filters.status) {
      dataToExport = dataToExport.filter(i => i.status === filters.status)
    }

    if (dataToExport.length === 0) {
      message.warning('Không có dữ liệu nào phù hợp để xuất!')
      exportLoading.value = false
      return
    }

    // 2. Tạo Workbook
    const workbook = new ExcelJS.Workbook()
    const worksheet = workbook.addWorksheet('Danh sách Hóa Đơn')

    // 3. Định nghĩa cột
    worksheet.columns = [
      { header: 'STT', key: 'stt', width: 8 },
      { header: 'Mã HĐ', key: 'code', width: 15 },
      { header: 'Tên Khách Hàng', key: 'customerName', width: 25 },
      { header: 'SĐT', key: 'phoneNumber', width: 15 },
      { header: 'Nhân viên', key: 'staffName', width: 20 },
      { header: 'Loại HĐ', key: 'type', width: 15 },
      { header: 'Tổng tiền', key: 'total', width: 18 },
      { header: 'Ngày tạo', key: 'date', width: 22 },
      { header: 'Trạng thái', key: 'status', width: 18 },
    ]

    // Style Header (Xanh lá đậm)
    worksheet.getRow(1).font = { bold: true, color: { argb: 'FFFFFF' } }
    worksheet.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: '15803D' } } // green-700
    worksheet.getRow(1).alignment = { vertical: 'middle', horizontal: 'center' }

    // 4. Đổ dữ liệu
    dataToExport.forEach((item, index) => {
      worksheet.addRow({
        stt: index + 1,
        code: item.code,
        customerName: item.customerName,
        phoneNumber: item.phoneNumber,
        staffName: `${item.staffName} (${item.staffCode})`,
        type: item.invoiceType === 'INSTORE' ? 'Tại quầy' : 'Online',
        total: formatCurrency(item.totalAmount),
        date: formatDateTime(item.createdAt),
        status: item.status === 'COMPLETED' ? 'Hoàn thành' : 'Chưa hoàn thành',
      })
    })

    // 5. Xuất file
    const buffer = await workbook.xlsx.writeBuffer()
    const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    saveAs(blob, `Danh_Sach_Hoa_Don_${new Date().toISOString().slice(0, 10)}.xlsx`)

    message.success(`Đã xuất ${dataToExport.length} hóa đơn!`)
  }
  catch (error) {
    console.error(error)
    message.error('Lỗi khi tạo file Excel')
  }
  finally {
    exportLoading.value = false
  }
}

/* ===================== Fetch Data ===================== */
async function fetchData() {
  loading.value = true
  try {
    await new Promise(resolve => setTimeout(resolve, 300))

    // --- MOCK DATA GENERATION (Tạo 50 dòng để test) ---
    const mock: Invoice[] = Array.from({ length: 50 }).map((_, i) => {
      const isToday = i < 10
      const time = isToday ? Date.now() : Date.now() - (i * 86400000 * 2)
      return {
        id: `inv-${i}`,
        stt: i + 1,
        code: `HD${2024000 + i}`,
        customerName: i % 3 === 0 ? 'Nguyễn Văn Khách' : 'Trần Thị Mua',
        phoneNumber: '0987654321',
        staffName: i % 2 === 0 ? 'Phạm Toàn' : 'Nguyễn Quản Lý',
        staffCode: i % 2 === 0 ? 'NV001' : 'QL001',
        totalAmount: (Math.floor(Math.random() * 50) + 10) * 100000,
        invoiceType: i % 3 === 0 ? 'ONLINE' : 'INSTORE',
        status: i % 5 === 0 ? 'INCOMPLETE' : 'COMPLETED',
        createdAt: time,
      }
    })

    allMockData.value = mock // Lưu lại bản gốc để export

    // Logic lọc Client-side
    let filtered = mock
    if (filters.keyword) {
      const k = filters.keyword.toLowerCase()
      filtered = filtered.filter(i =>
        i.code.toLowerCase().includes(k) || i.customerName.toLowerCase().includes(k) || i.phoneNumber.includes(k),
      )
    }
    if (filters.dateRange) {
      const [start, end] = filters.dateRange
      filtered = filtered.filter(i => i.createdAt >= start && i.createdAt <= end)
    }
    if (filters.invoiceType) {
      filtered = filtered.filter(i => i.invoiceType === filters.invoiceType)
    }
    if (filters.status) {
      filtered = filtered.filter(i => i.status === filters.status)
    }

    pagination.value.itemCount = filtered.length
    const start = (pagination.value.page - 1) * pagination.value.pageSize
    displayData.value = filtered.slice(start, start + pagination.value.pageSize)
  }
  catch (error) {
    message.error('Lỗi tải dữ liệu')
  }
  finally {
    loading.value = false
  }
}

watch(() => [pagination.value.page, pagination.value.pageSize], () => fetchData())

/* ===================== Columns Config ===================== */
const columns: DataTableColumns<Invoice> = [
  {
    title: 'STT',
    key: 'stt',
    align: 'center',
    width: 60,
    render: (_, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize,
  },
  {
    title: 'Mã HĐ',
    key: 'code',
    width: 120,
    render: row => h('strong', {
      class: 'text-primary cursor-pointer hover:underline',
      onClick: () => openDetailPage(row.id),
    }, row.code),
  },
  {
    title: 'Khách hàng',
    key: 'customerName',
    width: 180,
    render: row => h('div', { class: 'flex flex-col' }, [
      h('span', { class: 'font-medium' }, row.customerName),
      h('span', { class: 'text-xs text-gray-400' }, row.phoneNumber),
    ]),
  },
  {
    title: 'Nhân viên',
    key: 'staffName',
    width: 160,
    render: row => h('div', { class: 'flex items-center gap-2' }, [
      h(NAvatar, { size: 'small', round: true, style: { backgroundColor: '#fde047', color: '#854d0e', fontSize: '12px' } }, { default: () => row.staffName.charAt(0) }),
      h('div', { class: 'flex flex-col' }, [
        h('span', { class: 'text-xs font-semibold' }, row.staffName),
        h('span', { class: 'text-[10px] text-gray-400' }, row.staffCode),
      ]),
    ]),
  },
  {
    title: 'Loại HĐ',
    key: 'invoiceType',
    align: 'center',
    width: 120,
    render: (row) => {
      const conf = getInvoiceTypeConfig(row.invoiceType)
      return h(NTag, { type: conf.type as any, bordered: false, size: 'small' }, {
        default: () => h('div', { class: 'flex items-center gap-1' }, [
          h(Icon, { icon: conf.icon }),
          h('span', conf.text),
        ]),
      })
    },
  },
  {
    title: 'Tổng tiền',
    key: 'totalAmount',
    width: 140,
    render: row => h('span', { class: 'font-bold text-red-600' }, formatCurrency(row.totalAmount)),
  },
  {
    title: 'Ngày tạo',
    key: 'createdAt',
    width: 150,
    render: row => formatDateTime(row.createdAt),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 160,
    align: 'center',
    render: (row) => {
      const conf = getStatusConfig(row.status)
      return h(NTag, { type: conf.type as any, bordered: false, round: true }, {
        default: () => h('div', { class: 'flex items-center gap-1' }, [
          h(Icon, { icon: conf.icon }),
          h('span', conf.text),
        ]),
      })
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 80,
    fixed: 'right',
    render: (row) => {
      return h(NTooltip, { trigger: 'hover' }, {
        trigger: () => h(NButton, {
          size: 'small',
          type: 'info',
          secondary: true,
          circle: true,
          onClick: () => openDetailPage(row.id),
        }, { icon: () => h(Icon, { icon: 'carbon:view' }) }),
        default: () => 'Xem chi tiết',
      })
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
          <NIcon size="24" class="text-indigo-600">
            <Icon icon="carbon:receipt" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">Quản lý Hóa Đơn</span>
        </NSpace>
        <span class="text-gray-500">Quản lý danh sách hóa đơn bán hàng</span>
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
            Làm mới bộ lọc (Về hôm nay)
          </NTooltip>
        </div>
      </template>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-6">
        <div class="lg:col-span-2">
          <div class="text-xs font-bold text-gray-600 mb-1 ml-1 uppercase">
            Tìm kiếm chung
          </div>
          <NInput
            v-model:value="filters.keyword"
            placeholder="Mã HĐ, Tên KH, SĐT..."
            clearable
            @input="pagination.page = 1; fetchData()"
          >
            <template #prefix>
              <NIcon>
                <Icon icon="carbon:search" class="text-gray-600" />
              </NIcon>
            </template>
          </NInput>
        </div>

        <div class="lg:col-span-2">
          <div class="text-xs font-bold text-gray-600 mb-1 ml-1 uppercase">
            Ngày tạo
          </div>
          <NDatePicker
            v-model:value="filters.dateRange"
            type="daterange"
            clearable
            class="w-full"
            @update:value="fetchData"
          />
        </div>

        <div class="lg:col-span-1">
          <div class="text-xs font-bold text-gray-600 mb-1 ml-1 uppercase">
            Loại hóa đơn
          </div>
          <NSelect v-model:value="filters.invoiceType" :options="invoiceTypeOptions" placeholder="Tất cả" @update:value="fetchData" />
        </div>

        <div class="lg:col-span-1">
          <div class="text-xs font-bold text-gray-600 mb-1 ml-1 uppercase">
            Trạng thái
          </div>
          <NSelect v-model:value="filters.status" :options="statusOptions" placeholder="Tất cả" @update:value="fetchData" />
        </div>
      </div>
    </NCard>

    <NCard title="Danh sách Hóa Đơn" class="border rounded-2xl shadow-sm border-gray-100">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary" secondary
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
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Tải lại
              </span>
            </NButton>
          </NSpace>
        </div>
      </template>

      <NDataTable
        :columns="columns" :data="displayData" :loading="loading"
        :pagination="false" striped :scroll-x="1200"
        class="rounded-lg overflow-hidden"
      />
      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="pagination.pageSizes"
          show-size-picker
        />
      </div>
    </NCard>
  </div>
</template>
