<template>
  <div class="page-container">
    <div class="breadcrumb-section">
      <BreadcrumbDefault 
        :pageTitle="'Quản lý hóa đơn'" 
        :routes="[{ path: '/admin/hoa-don', name: 'Quản lý hóa đơn' }]" 
      />
    </div>
    
    <n-card class="mb-4">
      <template #header>
        <div class="section-title">
          <n-icon><FilterIcon /></n-icon>
          <span>Bộ lọc tìm kiếm</span>
        </div>
      </template>
      <div class="filter-container">
        <n-grid :cols="24" :x-gap="12" :y-gap="12">
          <n-gi :span="6">
            <n-form-item label="Tìm kiếm">
              <n-input
                v-model:value="state.searchQuery"
                placeholder="Nhập mã hóa đơn, tên khách hàng, SĐT..."
                clearable
                @keyup.enter="fetchHoaDons"
                @blur="fetchHoaDons"
              >
                <template #prefix>
                  <n-icon><SearchIcon /></n-icon>
                </template>
              </n-input>
            </n-form-item>
          </n-gi>

          <n-gi :span="6">
            <n-form-item label="Khoảng thời gian">
              <n-date-picker
                v-model:value="dateRange"
                type="daterange"
                clearable
                :is-date-disabled="disableFutureDate"
                style="width: 100%"
                @update:value="handleDateRangeChange"
                placeholder="Chọn khoảng thời gian"
              />
            </n-form-item>
          </n-gi>
          
          <n-gi :span="4">
            <n-form-item label="Loại hóa đơn">
              <n-select
                v-model:value="state.loaiHoaDon"
                :options="loaiHoaDonOptions"
                placeholder="Tất cả"
                clearable
                @update:value="fetchHoaDons"
              />
            </n-form-item>
          </n-gi>
          
          <n-gi :span="4">
            <n-form-item label="Trạng thái">
              <n-select
                v-model:value="state.searchStatus"
                :options="statusOptions"
                placeholder="Tất cả"
                clearable
                @update:value="handleStatusChange"
              />
            </n-form-item>
          </n-gi>


        </n-grid>
      </div>
    </n-card>

    <n-card>
      <template #header>
        <div class="section-title">
          <n-icon><ListIcon /></n-icon>
          <span>Danh sách hóa đơn</span>
          <div class="ml-auto" style="display: flex; gap: 8px;">
            <n-button 
              type="primary" 
              size="small" 
              @click="fetchHoaDons"
              :loading="state.loading"
            >
              <template #icon>
                <n-icon><RefreshIcon /></n-icon>
              </template>
              Làm mới
            </n-button>
            <n-button 
              type="success" 
              size="small" 
              @click="exportToExcel"
              :disabled="state.products.length === 0"
            >
              <template #icon>
                <n-icon><ExportIcon /></n-icon>
              </template>
              Xuất Excel
            </n-button>
          </div>
        </div>
      </template>
      
      <div class="invoice-tabs mb-4">
        <n-tabs v-model:value="activeTab" type="segment" @update:value="handleTabChange">
          <n-tab name="ALL">
            <span class="tab-content">
              Tất cả
              <n-badge :value="state.totalItems" type="info" :max="999" />
            </span>
          </n-tab>
          <n-tab v-for="tab in tabs" :key="tab.key" :name="tab.key">
            <span class="tab-content">
              {{ tab.label }}
              <n-badge :value="tab.count" type="info" :max="999" />
            </span>
          </n-tab>
        </n-tabs>
      </div>

      <n-alert v-if="state.apiError" type="error" class="mb-4" closable @close="state.apiError = ''">
        {{ state.apiError }}
      </n-alert>

      <n-data-table
        :columns="columns"
        :data="state.products"
        :pagination="pagination"
        :max-height="500"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
        striped
        :loading="state.loading"
        :row-key="(row) => row.id"
        :bordered="false"
      />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, watch, h, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NIcon,
  NDataTable,
  NInput,
  NSelect,
  NTooltip,
  NForm,
  NFormItem,
  NSpace,
  NGrid,
  NGi,
  NCard,
  NText,
  NTag,
  NBadge,
  NTabs,
  NTab,
  NDatePicker,
  NAlert,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import BreadcrumbDefault from '@/layouts/components/header/Breadcrumb.vue'
import * as XLSX from 'xlsx'
import {
  GetHoaDons,
  type HoaDonItem,
  type HoaDonResponse,
  type ParamsGetHoaDon
} from "@/service/api/admin/hoadon.api";

// Icons
import {
  FilterOutlined as FilterIcon,
  UnorderedListOutlined as ListIcon,
  SearchOutlined as SearchIcon,
  ReloadOutlined as RefreshIcon,
  EyeOutlined as EyeIcon,
  PrinterOutlined as PrinterIcon,
  FileExcelOutlined as ExportIcon
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
  apiError: '' as string
})

const dateRange = ref<[number, number] | null>(null)

import dayjs from 'dayjs'

const setTodayWithDayjs = () => {
  const startOfDay = dayjs().startOf('day').valueOf()
  const endOfDay = dayjs().endOf('day').valueOf()
  
  dateRange.value = [startOfDay, endOfDay]
}
const activeTab = ref('ALL')

// Computed pagination
const pagination = computed(() => ({
  page: state.paginationParams.page,
  pageSize: state.paginationParams.size,
  pageCount: Math.ceil(state.totalItems / state.paginationParams.size),
  showSizePicker: true,
  pageSizes: [10, 20, 30, 40, 50],
  showQuickJumper: true,
  prefix: ({ itemCount }: { itemCount: number }) => `Tổng: ${itemCount} hóa đơn`
}))

// Options
const statusOptions = [
  { label: 'Chờ xác nhận', value: 'CHO_XAC_NHAN' },
  { label: 'Đã xác nhận', value: 'DA_XAC_NHAN' },
  { label: 'Chờ giao', value: 'CHO_GIAO' },
  { label: 'Đang giao', value: 'DANG_GIAO' },
  { label: 'Hoàn thành', value: 'HOAN_THANH' },
  { label: 'Đã hủy', value: 'DA_HUY' }
]

const loaiHoaDonOptions = [
  { label: 'Tại quầy', value: 'OFFLINE' },
  { label: 'Giao hàng', value: 'GIAO_HANG' },
  { label: 'Online', value: 'ONLINE' }
]

const sortOptions = [
  { label: 'Mới nhất', value: 'desc' },
  { label: 'Cũ nhất', value: 'asc' }
]

// Status colors
const statusColors: Record<string, string> = {
  CHO_XAC_NHAN: 'warning',
  DA_XAC_NHAN: 'info',
  CHO_GIAO: 'default',
  DANG_GIAO: 'primary',
  HOAN_THANH: 'success',
  DA_HUY: 'error'
}

// Status labels
const statusLabels: Record<string, string> = {
  CHO_XAC_NHAN: 'Chờ xác nhận',
  DA_XAC_NHAN: 'Đã xác nhận',
  CHO_GIAO: 'Chờ giao',
  DANG_GIAO: 'Đang giao',
  HOAN_THANH: 'Hoàn thành',
  DA_HUY: 'Đã hủy'
}

// LoaiHoaDon colors
const loaiHoaDonColors: Record<string, string> = {
  OFFLINE: 'success',
  GIAO_HANG: 'primary',
  ONLINE: 'purple'
}

// LoaiHoaDon labels
const loaiHoaDonLabels: Record<string, string> = {
  OFFLINE: 'Tại quầy',
  GIAO_HANG: 'Giao hàng',
  ONLINE: 'Online'
}

// Computed
const tabs = computed(() => {
  const counts = state.countByStatus || {}
  return [
    { key: 'CHO_XAC_NHAN', label: 'Chờ xác nhận', count: counts.CHO_XAC_NHAN || 0 },
    { key: 'DA_XAC_NHAN', label: 'Đã xác nhận', count: counts.DA_XAC_NHAN || 0 },
    { key: 'CHO_GIAO', label: 'Chờ giao', count: counts.CHO_GIAO || 0 },
    { key: 'DANG_GIAO', label: 'Đang giao', count: counts.DANG_GIAO || 0 },
    { key: 'HOAN_THANH', label: 'Hoàn thành', count: counts.HOAN_THANH || 0 },
    { key: 'DA_HUY', label: 'Đã hủy', count: counts.DA_HUY || 0 }
  ]
})

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('vi-VN', { 
    style: 'currency', 
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0
  }).format(value)
}

const formatDateTime = (timestamp: number) => {
  if (!timestamp || timestamp <= 0) return 'N/A'
  try {
    const date = new Date(timestamp)
    const day = String(date.getDate()).padStart(2, '0')
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const year = date.getFullYear()
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${hours}:${minutes} ${day}/${month}/${year}`
  } catch {
    return 'N/A'
  }
}

const disableFutureDate = (timestamp: number) => {
  return timestamp > Date.now()
}

const handleDateRangeChange = (value: [number, number] | null) => {
  if (value && value.length === 2) {
    state.startDate = value[0]
    state.endDate = value[1]
  } else {
    state.startDate = null
    state.endDate = null
  }
  state.paginationParams.page = 1
  fetchHoaDons()
}

const handleStatusChange = (value: string | null) => {
  state.searchStatus = value
  state.paginationParams.page = 1
  fetchHoaDons()
}

// Fetch data từ API
const fetchHoaDons = async () => {
  try {
    state.loading = true
    state.apiError = ''
    
    // Chuyển đổi params để gửi lên API
    const params: ParamsGetHoaDon = {
      page: Math.max(0, state.paginationParams.page - 1), // Đảm bảo không âm
      size: Math.max(1, state.paginationParams.size),
      sort: state.sortOrder === 'desc' ? 'createdDate,desc' : 'createdDate,asc'
    }
    
    // Thêm search query
    if (state.searchQuery && state.searchQuery.trim() !== '') {
      params.q = state.searchQuery.trim()
    }
    
    // Thêm status
    if (state.searchStatus && state.searchStatus !== 'ALL') {
      params.status = state.searchStatus
    }
    
    // Thêm loại hóa đơn
    if (state.loaiHoaDon) {
      params.loaiHoaDon = state.loaiHoaDon
    }
    
    // Thêm date range nếu có
    if (state.startDate && state.endDate) {
      params.startDate = state.startDate
      params.endDate = state.endDate
    }
    
    console.log('Fetching invoices with params:', params)
    
    const response = await GetHoaDons(params)
    
    console.log('API Response:', response)
    
    if (response.success) {
      const hoaDonData = response.data
      state.products = hoaDonData.page.content || []
      state.totalItems = hoaDonData.page.totalElements || 0
      state.countByStatus = hoaDonData.countByStatus || {}
      
      // Cập nhật page number từ API (chuyển từ zero-based về one-based)
      const apiPageNumber = hoaDonData.page.number || 0
      state.paginationParams.page = Math.max(1, apiPageNumber + 1)
      
      if (state.products.length > 0) {
        message.success(`Đã tải ${state.products.length} hóa đơn`)
      }
    } else {
      state.apiError = response.message || 'Lỗi khi tải dữ liệu hóa đơn'
      message.error(state.apiError)
      
      // Reset data nếu có lỗi
      state.products = []
      state.totalItems = 0
      state.countByStatus = {}
      state.paginationParams.page = 1
    }
  } catch (error: any) {
    console.error('Lỗi khi fetch hóa đơn:', error)
    
    state.apiError = error.message || 'Đã xảy ra lỗi khi tải dữ liệu'
    message.error(state.apiError)
    
    // Reset data
    state.products = []
    state.totalItems = 0
    state.countByStatus = {}
    state.paginationParams.page = 1
  } finally {
    state.loading = false
  }
}

const handlePrintInvoice = (invoice: HoaDonItem) => {
  const printWindow = window.open('', '_blank')
  if (!printWindow) return
  
  const printContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <title>Hóa đơn ${invoice.maHoaDon || invoice.id}</title>
      <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
        .invoice-header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #333; padding-bottom: 20px; }
        .invoice-title { font-size: 24px; font-weight: bold; margin-bottom: 5px; }
        .invoice-info { margin-bottom: 20px; }
        .info-row { display: flex; margin-bottom: 8px; }
        .info-label { font-weight: bold; min-width: 150px; }
        .info-value { }
        .total-section { text-align: right; margin-top: 20px; font-size: 16px; padding-top: 20px; border-top: 1px solid #ddd; }
        .total-row { margin: 5px 0; }
        .grand-total { font-size: 18px; font-weight: bold; color: #d32f2f; }
        .thank-you { text-align: center; margin-top: 30px; font-style: italic; color: #666; }
        .status-badge { display: inline-block; padding: 4px 8px; border-radius: 4px; font-size: 12px; margin-left: 10px; }
        @media print {
          body { -webkit-print-color-adjust: exact; margin: 0; }
          .no-print { display: none !important; }
        }
      </style>
    </head>
    <body>
      <div class="invoice-header">
        <div class="invoice-title">HÓA ĐƠN BÁN HÀNG</div>
        <div style="font-size: 18px; margin-top: 10px;">Mã: ${invoice.maHoaDon || invoice.id}</div>
      </div>
      
      <div class="invoice-info">
        ${invoice.tenKhachHang ? `
        <div class="info-row">
          <div class="info-label">Khách hàng:</div>
          <div class="info-value">${invoice.tenKhachHang}${invoice.sdtKhachHang ? ' - ' + invoice.sdtKhachHang : ''}</div>
        </div>` : ''}
        <div class="info-row">
          <div class="info-label">Ngày tạo:</div>
          <div class="info-value">${formatDateTime(invoice.createdDate)}</div>
        </div>
        <div class="info-row">
          <div class="info-label">Nhân viên:</div>
          <div class="info-value">${invoice.tenNhanVien || 'N/A'} (${invoice.maNhanVien || 'N/A'})</div>
        </div>
        <div class="info-row">
          <div class="info-label">Tổng tiền:</div>
          <div class="info-value" style="font-weight: bold; color: #d32f2f;">${formatCurrency(invoice.tongTien)}</div>
        </div>
        <div class="info-row">
          <div class="info-label">Loại hóa đơn:</div>
          <div class="info-value">
            ${loaiHoaDonLabels[invoice.loaiHoaDon] || invoice.loaiHoaDon}
          </div>
        </div>
        <div class="info-row">
          <div class="info-label">Trạng thái:</div>
          <div class="info-value">
            ${statusLabels[invoice.status] || invoice.status}
          </div>
        </div>
      </div>
      
      <div class="thank-you">
        <p>Cảm ơn quý khách đã mua hàng!</p>
        <p>Hẹn gặp lại.</p>
      </div>
      
      <div class="no-print" style="margin-top: 30px; text-align: center;">
        <button onclick="window.print()" style="padding: 10px 20px; background: #1890ff; color: white; border: none; border-radius: 4px; cursor: pointer; margin-right: 10px;">
          In hóa đơn
        </button>
        <button onclick="window.close()" style="padding: 10px 20px; background: #f5222d; color: white; border: none; border-radius: 4px; cursor: pointer;">
          Đóng
        </button>
      </div>
      
      <script>
        window.onload = function() {
          // Tự động in sau 500ms
          setTimeout(() => {
            window.print();
          }, 500);
        }
      <\/script>
    </body>
    </html>
  `
  
  printWindow.document.write(printContent)
  printWindow.document.close()
  message.success('Đang mở cửa sổ in hóa đơn...')
}

const handleViewClick = (invoice: HoaDonItem) => {
  const invoiceId = invoice.maHoaDon
  
  router.push({
    name: 'orders_detail',
    params: { id: invoiceId }
  })
}

const exportToExcel = () => {
  try {
    if (state.products.length === 0) {
      message.warning('Không có dữ liệu để xuất Excel')
      return
    }
    
    const exportData = state.products.map(invoice => ({
      'Mã hóa đơn': invoice.maHoaDon || 'N/A',
      'Khách hàng': invoice.tenKhachHang || 'N/A',
      'SĐT': invoice.sdtKhachHang || 'N/A',
      'Loại': loaiHoaDonLabels[invoice.loaiHoaDon] || invoice.loaiHoaDon,
      'Nhân viên': invoice.tenNhanVien || 'N/A',
      'Mã NV': invoice.maNhanVien || 'N/A',
      'Tổng tiền': invoice.tongTien,
      'Ngày tạo': formatDateTime(invoice.createdDate),
      'Trạng thái': statusLabels[invoice.status] || invoice.status
    }))
    
    const worksheet = XLSX.utils.json_to_sheet(exportData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Hóa đơn')
    
    // Định dạng cột tiền
    const range = XLSX.utils.decode_range(worksheet['!ref'] || 'A1')
    for (let C = 6; C <= 6; ++C) { // Cột tổng tiền (G)
      for (let R = range.s.r + 1; R <= range.e.r; ++R) {
        const cell_address = { c: C, r: R }
        const cell_ref = XLSX.utils.encode_cell(cell_address)
        if (worksheet[cell_ref]) {
          worksheet[cell_ref].z = '#,##0'
        }
      }
    }
    
    // Tạo tên file với ngày hiện tại
    const today = new Date()
    const fileName = `hoa_don_${today.getDate()}_${today.getMonth() + 1}_${today.getFullYear()}.xlsx`
    
    XLSX.writeFile(workbook, fileName)
    message.success(`Đã xuất ${exportData.length} hóa đơn ra file Excel`)
  } catch (error) {
    console.error('Export error:', error)
    message.error('Xuất Excel thất bại')
  }
}

// Event handlers
const handleTabChange = (key: string) => {
  activeTab.value = key
  state.searchStatus = key === 'ALL' ? null : key
  state.paginationParams.page = 1
  fetchHoaDons()
}

const handlePageChange = (page: number) => {
  state.paginationParams.page = Math.max(1, page)
  fetchHoaDons()
}

const handlePageSizeChange = (pageSize: number) => {
  state.paginationParams.size = Math.max(1, pageSize)
  state.paginationParams.page = 1
  fetchHoaDons()
}

const resetFilters = () => {
  state.searchQuery = ''
  state.searchStatus = null
  state.loaiHoaDon = null
  state.sortOrder = 'desc'
  dateRange.value = null
  state.startDate = null
  state.endDate = null
  activeTab.value = 'ALL'
  state.paginationParams.page = 1
  state.paginationParams.size = 10
  fetchHoaDons()
  message.success('Đã đặt lại bộ lọc')
}

// Columns definition
const createColumns = (): DataTableColumns<HoaDonItem> => [
  {
    title: 'STT',
    key: 'stt',
    width: 70,
    align: 'center',
    fixed: 'left',
    render: (_, index) => h('span', 
      { style: 'font-weight: 500;' }, 
      index + 1 + (state.paginationParams.page - 1) * state.paginationParams.size
    )
  },
  {
    title: 'Mã HĐ',
    key: 'maHoaDon',
    width: 130,
    ellipsis: true,
    sorter: (a, b) => (a.maHoaDon || '').localeCompare(b.maHoaDon || ''),
    render: (row) => h(NText, { 
      type: 'primary', 
      strong: true,
      style: 'cursor: pointer; text-decoration: underline;',
      onClick: () => handleViewClick(row)
    }, { default: () => row.maHoaDon || 'N/A' })
  },
  {
    title: 'Khách hàng',
    key: 'tenKhachHang',
    width: 180,
    sorter: (a, b) => (a.tenKhachHang || '').localeCompare(b.tenKhachHang || ''),
    render: (row) => h('div', [
      h('div', { 
        style: 'font-weight: 500; cursor: pointer;',
        onClick: () => handleViewClick(row)
      }, row.tenKhachHang || 'Khách vãng lai'),
      row.sdtKhachHang && h('div', { 
        style: 'font-size: 12px; color: #666;' 
      }, row.sdtKhachHang)
    ])
  },
  {
    title: 'Nhân viên',
    key: 'nhanVien',
    width: 160,
    sorter: (a, b) => (a.tenNhanVien || '').localeCompare(b.tenNhanVien || ''),
    render: (row) => h('div', { 
      onClick: () => handleViewClick(row), 
      style: 'cursor: pointer;' 
    }, [
      h('div', { 
        style: 'font-weight: 500; font-size: 13px;',
        title: row.tenNhanVien || 'N/A'
      }, row.tenNhanVien || 'N/A'),
      h('div', { 
        style: 'font-size: 11px; color: #1890ff; background: #e6f7ff; padding: 1px 6px; border-radius: 3px; display: inline-block; margin-top: 2px;',
        title: 'Mã nhân viên'
      }, row.maNhanVien || 'N/A')
    ])
  },
  {
    title: 'Loại',
    key: 'loaiHD',
    width: 110,
    align: 'center',
    sorter: (a, b) => a.loaiHoaDon.localeCompare(b.loaiHoaDon),
    render: (row) => h(NTag, {
      type: 'primary',
      bordered: false,
      color: loaiHoaDonColors[row.loaiHoaDon] || 'default',
      size: 'small',
      style: 'min-width: 80px;'
    }, { default: () => loaiHoaDonLabels[row.loaiHoaDon] || row.loaiHoaDon })
  },
  {
    title: 'Ngày tạo',
    key: 'ngayTao',
    width: 150,
    sorter: (a, b) => a.createdDate - b.createdDate,
    render: (row) => h('div', { 
      onClick: () => handleViewClick(row), 
      style: 'cursor: pointer;' 
    }, [
      h('div', { 
        style: 'font-weight: 500; font-size: 12px;' 
      }, formatDateTime(row.createdDate))
    ])
  },
  {
    title: 'Tổng tiền',
    key: 'tongTien',
    width: 150,
    align: 'right',
    sorter: (a, b) => a.tongTien - b.tongTien,
    render: (row) => h('div', { 
      onClick: () => handleViewClick(row), 
      style: 'cursor: pointer; text-align: right;' 
    }, [
      h('div', { 
        style: 'font-weight: 600; color: #d32f2f;' 
      }, formatCurrency(row.tongTien))
    ])
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 140,
    align: 'center',
    sorter: (a, b) => a.status.localeCompare(b.status),
    render: (row) => h('div', { 
      onClick: () => handleViewClick(row), 
      style: 'cursor: pointer; text-align: center;' 
    }, [
      h(NTag, {
        type: 'primary',
        bordered: false,
        color: statusColors[row.status] || 'default',
        size: 'small',
        style: 'min-width: 100px;'
      }, { default: () => statusLabels[row.status] || row.status })
    ])
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 140,
    align: 'center',
    fixed: 'right',
    render: (row) => h(NSpace, { 
      justify: 'center', 
      size: 'small' 
    }, [
      h(NTooltip, {}, {
        trigger: () => h(NButton, {
          size: 'small',
          type: 'primary',
          onClick: () => handleViewClick(row),
          ghost: true
        }, {
          icon: () => h(NIcon, {}, () => h(EyeIcon))
        }),
        default: () => 'Xem chi tiết'
      }),
      
      h(NTooltip, {}, {
        trigger: () => h(NButton, {
          size: 'small',
          type: 'info',
          onClick: () => handlePrintInvoice(row),
          ghost: true
        }, {
          icon: () => h(NIcon, {}, () => h(PrinterIcon))
        }),
        default: () => 'In hóa đơn'
      })
    ])
  }
]

const columns = createColumns()

// Watch for filter changes
watch([
  () => state.searchQuery,
  () => state.loaiHoaDon,
  () => state.sortOrder
], () => {
  // Reset về trang 1 khi thay đổi filter
  state.paginationParams.page = 1
  fetchHoaDons()
}, { deep: true })

// Initialize
onMounted(() => {
  fetchHoaDons(),
  setTodayWithDayjs()
})
</script>

<style scoped>
.page-container {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: 100vh;
}

.breadcrumb-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
}

.section-title .ml-auto {
  margin-left: auto;
}

.filter-container {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.invoice-tabs {
  background: #fff;
  padding: 8px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.tab-content {
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.n-tabs-tab) {
  padding: 8px 16px;
  border-radius: 6px;
  transition: all 0.3s;
}

:deep(.n-tabs-tab:hover) {
  background-color: #f5f5f5;
}

:deep(.n-tabs-tab.n-tabs-tab--active) {
  background-color: #e6f7ff;
  color: #1890ff;
}

:deep(.n-tabs-bar) {
  background-color: #1890ff;
}

:deep(.n-data-table) {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.09);
}

:deep(.n-data-table-th) {
  background-color: #fafafa;
  font-weight: 600;
}

:deep(.n-data-table-tr) {
  transition: background-color 0.2s;
}

:deep(.n-data-table-tr:hover) {
  background-color: #f6f6f6;
}

:deep(.n-button) {
  transition: all 0.2s;
}

:deep(.n-button:hover) {
  transform: translateY(-1px);
}

/* Thêm style cho các cell có thể click */
:deep(.n-data-table .clickable-cell) {
  cursor: pointer;
}

:deep(.n-data-table .clickable-cell:hover) {
  background-color: #f5f5f5;
}

@media (max-width: 768px) {
  .page-container {
    padding: 12px;
  }
  
  :deep(.n-grid) {
    margin: 0 -8px;
  }
  
  :deep(.n-grid-item) {
    padding: 0 8px;
  }
  
  :deep(.n-data-table) {
    font-size: 12px;
  }
  
  :deep(.n-tabs) {
    overflow-x: auto;
  }
}
</style>