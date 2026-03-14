<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  NAlert,
  NBadge,
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NPagination,
  NSelect,
  NSpace,
  NTab,
  NTabs,
  NTag,
  NText,
  NTooltip,
  useMessage,
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { Icon } from '@iconify/vue'
import * as XLSX from 'xlsx'
import dayjs from 'dayjs'
import { toast } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'

import { GetHoaDons } from '@/service/api/admin/hoadon.api'
import type { HoaDonItem, ParamsGetHoaDon } from '@/service/api/admin/hoadon.api'

// Icons
import {
  EyeOutlined as EyeIcon,
} from '@vicons/antd'

const router = useRouter()
const message = useMessage()

// State
const state = reactive({
  searchQuery: '',
  searchStatus: null as string | null,
  loaiHoaDon: null as string | null,
  sortOrder: 'desc' as 'asc' | 'desc',
  loading: false,
  products: [] as HoaDonItem[],
  paginationParams: { page: 1, size: 10 },
  totalItems: 0,
  startDate: null as number | null,
  endDate: null as number | null,
  countByStatus: {} as Record<string, number>,
  apiError: '' as string,
})

const exportLoading = ref(false)
const dateRange = ref<[number, number] | null>(null)
const activeTab = ref('ALL')

function setTodayWithDayjs() {
  const today = dayjs()
  const startOfDay = today.startOf('day').valueOf()
  const endOfDay = today.endOf('day').valueOf()

  dateRange.value = [startOfDay, endOfDay]
  state.startDate = startOfDay
  state.endDate = endOfDay
  fetchHoaDons()
}

function disableFutureDate(timestamp: number) {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return timestamp > today.getTime()
}

function handleDateRangeChange(value: [number, number] | null) {
  if (value && value.length === 2) {
    state.startDate = dayjs(value[0]).startOf('day').valueOf()
    state.endDate = dayjs(value[1]).endOf('day').valueOf()
  }
  else {
    state.startDate = null
    state.endDate = null
    dateRange.value = null
  }
  state.paginationParams.page = 1
  fetchHoaDons()
}

// Options
const statusOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Chờ xác nhận', value: 'CHO_XAC_NHAN' },
  { label: 'Đã xác nhận', value: 'DA_XAC_NHAN' },
  { label: 'Chờ giao', value: 'CHO_GIAO' },
  { label: 'Đang giao', value: 'DANG_GIAO' },
  { label: 'Hoàn thành', value: 'HOAN_THANH' },
  { label: 'Đã hủy', value: 'DA_HUY' },
]

const loaiHoaDonOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Tại quầy', value: 'TAI_QUAY' },
  { label: 'Giao hàng', value: 'GIAO_HANG' },
  { label: 'Online', value: 'ONLINE' },
]

// Status labels & colors (Sử dụng đúng type của NTag)
type NTagType = 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error'

const statusColors: Record<string, NTagType> = {
  CHO_XAC_NHAN: 'warning',
  DA_XAC_NHAN: 'info',
  CHO_GIAO: 'default',
  DANG_GIAO: 'primary',
  HOAN_THANH: 'success',
  DA_HUY: 'error',
}

const statusLabels: Record<string, string> = {
  CHO_XAC_NHAN: 'Chờ xác nhận',
  DA_XAC_NHAN: 'Đã xác nhận',
  CHO_GIAO: 'Chờ giao',
  DANG_GIAO: 'Đang giao',
  HOAN_THANH: 'Hoàn thành',
  DA_HUY: 'Đã hủy',
}

// FIX: Cập nhật chuẩn key theo API và dùng màu hợp lệ của NTag
const loaiHoaDonColors: Record<string, NTagType> = {
  TAI_QUAY: 'success', // Xanh lá
  GIAO_HANG: 'info', // Xanh lam nhạt
  ONLINE: 'warning', // Cam/Vàng (thay cho purple không tồn tại)
}

const loaiHoaDonLabels: Record<string, string> = {
  TAI_QUAY: 'Tại quầy',
  GIAO_HANG: 'Giao hàng',
  ONLINE: 'Online',
}

// Computed Tabs
const tabs = computed(() => {
  const counts = state.countByStatus || {}
  return [
    { key: 'CHO_XAC_NHAN', label: 'Chờ xác nhận', count: counts.CHO_XAC_NHAN || 0 },
    { key: 'DA_XAC_NHAN', label: 'Đã xác nhận', count: counts.DA_XAC_NHAN || 0 },
    { key: 'CHO_GIAO', label: 'Chờ giao', count: counts.CHO_GIAO || 0 },
    { key: 'DANG_GIAO', label: 'Đang giao', count: counts.DANG_GIAO || 0 },
    { key: 'HOAN_THANH', label: 'Hoàn thành', count: counts.HOAN_THANH || 0 },
    { key: 'DA_HUY', label: 'Đã hủy', count: counts.DA_HUY || 0 },
  ]
})

function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function formatDateTime(timestamp: number) {
  if (!timestamp || timestamp <= 0)
    return 'N/A'
  try {
    return new Date(timestamp).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  }
  catch { return 'N/A' }
}

async function fetchHoaDons() {
  try {
    state.loading = true
    state.apiError = ''

    const params: ParamsGetHoaDon = {
      page: Math.max(0, state.paginationParams.page),
      size: Math.max(1, state.paginationParams.size),
      sort: state.sortOrder === 'desc' ? 'createdDate,desc' : 'createdDate,asc',
    }

    if (state.searchQuery?.trim())
      params.q = state.searchQuery.trim()
    if (state.searchStatus && state.searchStatus !== 'ALL')
      params.status = state.searchStatus
    if (state.loaiHoaDon)
      params.loaiHoaDon = state.loaiHoaDon
    if (state.startDate && state.endDate) {
      params.startDate = state.startDate
      params.endDate = state.endDate
    }

    const response = await GetHoaDons(params)

    if (response.success) {
      const hoaDonData = response.data
      state.products = hoaDonData.page.content || []
      state.totalItems = hoaDonData.page.totalElements || 0
      state.countByStatus = hoaDonData.countByStatus || {}
    }
    else {
      state.apiError = response.message || 'Lỗi khi tải dữ liệu hóa đơn'
      toast.error(state.apiError)
      resetStateData()
    }
  }
  catch (error: any) {
    state.apiError = error.message || 'Đã xảy ra lỗi khi tải dữ liệu'
    message.error(state.apiError)
    resetStateData()
  }
  finally {
    state.loading = false
  }
}

function resetStateData() {
  state.products = []
  state.totalItems = 0
  state.countByStatus = {}
  state.paginationParams.page = 1
}

function handleViewClick(invoice: HoaDonItem) {
  router.push({ name: 'orders_detail', params: { id: invoice.maHoaDon } })
}

function exportToExcel() {
  if (state.products.length === 0) {
    message.warning('Không có dữ liệu để xuất Excel')
    return
  }
  exportLoading.value = true
  try {
    const exportData = state.products.map((invoice) => {
      const loaiKey = (invoice.loaiHoaDon || '').toUpperCase()
      const statusKey = (invoice.status || '').toUpperCase()

      return {
        'Mã hóa đơn': invoice.maHoaDon || 'N/A',
        'Khách hàng': invoice.tenKhachHang || 'N/A',
        'SĐT': invoice.sdtKhachHang || 'N/A',
        'Loại': loaiHoaDonLabels[loaiKey] || invoice.loaiHoaDon,
        'Nhân viên': invoice.tenNhanVien || 'N/A',
        'Mã NV': invoice.maNhanVien || 'N/A',
        'Tổng tiền': invoice.tongTien,
        'Ngày tạo': formatDateTime(invoice.createdDate),
        'Trạng thái': statusLabels[statusKey] || invoice.status,
      }
    })

    const worksheet = XLSX.utils.json_to_sheet(exportData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Hóa đơn')

    const today = new Date()
    const fileName = `Danh_Sach_Hoa_Don_${today.toISOString().slice(0, 10)}.xlsx`

    XLSX.writeFile(workbook, fileName)
    message.success(`Đã xuất ${exportData.length} hóa đơn ra file Excel`)
  }
  catch (error) {
    message.error('Xuất Excel thất bại')
  }
  finally {
    exportLoading.value = false
  }
}

function resetFilters() {
  state.searchQuery = ''
  state.searchStatus = null
  state.loaiHoaDon = null
  state.sortOrder = 'desc'
  dateRange.value = null
  state.startDate = null
  state.endDate = null
  activeTab.value = 'ALL'
  state.paginationParams.page = 1
  fetchHoaDons()
  message.success('Đã đặt lại bộ lọc')
}

function handleTabChange(key: string) {
  activeTab.value = key
  state.searchStatus = key === 'ALL' ? null : key
  state.paginationParams.page = 1
  fetchHoaDons()
}

// FIX: Xử lý phân trang tách biệt
function handlePageChange(page: number) {
  state.paginationParams.page = page
  fetchHoaDons()
}

function handlePageSizeChange(pageSize: number) {
  state.paginationParams.size = pageSize
  state.paginationParams.page = 1 // Reset về trang 1
  fetchHoaDons()
}

watch([() => state.searchQuery, () => state.loaiHoaDon, () => state.searchStatus], () => {
  state.paginationParams.page = 1
  fetchHoaDons()
})

const columns: DataTableColumns<HoaDonItem> = [
  { title: 'STT', key: 'stt', width: 60, align: 'center', fixed: 'left', render: (_, index) => index + 1 + (state.paginationParams.page - 1) * state.paginationParams.size },
  {
    title: 'Mã HĐ',
    key: 'maHoaDon',
    width: 130,
    fixed: 'left',
    render: row => h(NText, { type: 'primary', strong: true, class: 'cursor-pointer hover:underline', onClick: () => handleViewClick(row) }, { default: () => row.maHoaDon || 'N/A' }),
  },
  {
    title: 'Khách hàng',
    key: 'tenKhachHang',
    width: 180,
    render: row => h('div', { class: 'flex flex-col' }, [
      h('div', { class: 'font-medium cursor-pointer hover:text-primary', onClick: () => handleViewClick(row) }, row.tenKhachHang || 'Khách vãng lai'),
      row.sdtKhachHang && h('div', { class: 'text-xs text-gray-500 mt-1' }, row.sdtKhachHang),
    ]),
  },
  {
    title: 'Nhân viên',
    key: 'nhanVien',
    width: 160,
    render: row => h('div', { class: 'flex flex-col' }, [
      h('div', { class: 'font-medium text-[13px]' }, row.tenNhanVien || 'N/A'),
      h('div', { class: 'text-[11px] text-blue-500 bg-blue-50 px-1.5 py-0.5 rounded w-fit mt-1' }, row.maNhanVien || 'N/A'),
    ]),
  },
  {
    title: 'Loại',
    key: 'loaiHD',
    width: 110,
    align: 'center',
    render: (row) => {
      // FIX: Dùng toUpperCase() để bắt trường hợp API trả về "Online" thay vì "ONLINE"
      const rawLoai = row.loaiHoaDon || ''
      const key = rawLoai.toUpperCase()
      const type = loaiHoaDonColors[key] || 'default'
      const label = loaiHoaDonLabels[key] || rawLoai

      return h(NTag, { type, size: 'small', bordered: false, style: 'font-weight: 500;' }, { default: () => label })
    },
  },
  {
    title: 'Ngày tạo',
    key: 'ngayTao',
    width: 150,
    render: row => h('div', { class: 'font-medium text-xs text-gray-600' }, formatDateTime(row.createdDate)),
  },
  {
    title: 'Tổng tiền',
    key: 'tongTien',
    width: 150,
    align: 'right',
    render: row => h('div', { class: 'font-semibold text-red-600' }, formatCurrency(row.tongTien)),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 140,
    align: 'center',
    render: (row) => {
      // Đề phòng API trả về status thường
      const key = (row.status || '').toUpperCase()
      const type = statusColors[key] || 'default'
      const label = statusLabels[key] || row.status

      return h(NTag, { type, size: 'small', bordered: false, style: 'font-weight: 500;' }, { default: () => label })
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 80,
    align: 'center',
    fixed: 'right',
    render: row => h(NTooltip, { trigger: 'hover' }, {
      trigger: () => h(NButton, {
        size: 'small',
        type: 'info',
        secondary: true,
        circle: true,
        class: 'transition-all duration-200 hover:scale-[1.3] hover:shadow-lg',
        onClick: () => handleViewClick(row),
      }, { icon: () => h(NIcon, null, { default: () => h(EyeIcon) }) }),
      default: () => 'Xem chi tiết',
    }),
  },
]

onMounted(() => {
  setTodayWithDayjs()
})
</script>

<template>
  <div class="flex flex-col gap-4">
    <NCard class="shadow-sm border-none">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-green-600">
            <Icon icon="carbon:document" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px; color: #1f2937">Quản lý Hóa Đơn</span>
        </NSpace>
        <span class="text-gray-500">Quản lý và theo dõi danh sách Hóa Đơn của cửa hàng</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="shadow-md rounded-2xl border border-gray-100">
      <template #header-extra>
        <div class="mr-5">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton
                size="large"
                circle
                secondary
                type="primary"
                class="transition-all duration-200 hover:scale-110 hover:shadow-md"
                title="Làm mới bộ lọc"
                @click="resetFilters"
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

      <NForm label-placement="top">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-6 gap-6">
          <NFormItem label="Tìm kiếm chung" class="lg:col-span-2">
            <NInput
              v-model:value="state.searchQuery"
              placeholder="Mã hóa đơn, tên KH, SĐT..."
              clearable
              @keyup.enter="fetchHoaDons"
              @blur="fetchHoaDons"
            >
              <template #prefix>
                <NIcon><Icon icon="carbon:search" class="text-gray-600" /></NIcon>
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="Khoảng thời gian" class="lg:col-span-2">
            <NDatePicker
              v-model:value="dateRange"
              type="daterange"
              clearable
              :is-date-disabled="disableFutureDate"
              style="width: 100%"
              placeholder="Chọn khoảng thời gian"
              @update:value="handleDateRangeChange"
            />
          </NFormItem>

          <NFormItem label="Loại hóa đơn" class="lg:col-span-1">
            <NSelect v-model:value="state.loaiHoaDon" :options="loaiHoaDonOptions" placeholder="Tất cả" />
          </NFormItem>

          <NFormItem label="Trạng thái" class="lg:col-span-1">
            <NSelect v-model:value="state.searchStatus" :options="statusOptions" placeholder="Tất cả" />
          </NFormItem>
        </div>
      </NForm>
    </NCard>

    <NCard title="Danh sách Hóa Đơn" class="shadow-sm rounded-xl border border-gray-100">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton type="success" secondary class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg" :loading="exportLoading" :disabled="state.products.length === 0" @click="exportToExcel">
              <template #icon>
                <NIcon size="20">
                  <Icon icon="file-icons:microsoft-excel" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">Xuất Excel</span>
            </NButton>
            <NButton type="info" secondary class="group rounded-full px-4 transition-all duration-300 ease-in-out hover:shadow-lg" :loading="state.loading" @click="fetchHoaDons">
              <template #icon>
                <NIcon size="20">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">Tải lại</span>
            </NButton>
          </NSpace>
        </div>
      </template>

      <div class="mb-4">
        <NTabs v-model:value="activeTab" type="segment" @update:value="handleTabChange">
          <NTab name="ALL">
            <span class="flex items-center gap-2">
              Tất cả
              <NBadge :value="state.totalItems" type="info" :max="999" />
            </span>
          </NTab>
          <NTab v-for="tab in tabs" :key="tab.key" :name="tab.key">
            <span class="flex items-center gap-2">
              {{ tab.label }}
              <NBadge :value="tab.count" type="info" :max="999" />
            </span>
          </NTab>
        </NTabs>
      </div>

      <NAlert v-if="state.apiError" type="error" class="mb-4" closable @close="state.apiError = ''">
        {{ state.apiError }}
      </NAlert>

      <NDataTable
        :columns="columns"
        :data="state.products"
        :loading="state.loading"
        :row-key="(row) => row.maHoaDon"
        :pagination="false"
        striped
        :scroll-x="1200"
        class="rounded-lg overflow-hidden"
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="state.paginationParams.page"
          v-model:page-size="state.paginationParams.size"
          :item-count="state.totalItems"
          :page-sizes="[5, 10, 20, 30, 50, 100]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </NCard>
  </div>
</template>

<style scoped>
:deep(.n-input .n-input__input-el) { font-size: 14px; }
:deep(.n-tabs-tab) {
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.3s;
}
:deep(.n-tabs-tab.n-tabs-tab--active) {
  background-color: #e6f7ff;
  color: #1890ff;
}
</style>
