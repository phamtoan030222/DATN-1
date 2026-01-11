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
          <n-gi :span="8">
            <n-form-item label="Tìm kiếm">
              <n-input
                v-model:value="state.searchQuery"
                placeholder="Nhập mã hóa đơn, tên khách hàng, SĐT..."
                clearable
              >
                <template #prefix>
                  <n-icon><SearchIcon /></n-icon>
                </template>
              </n-input>
            </n-form-item>
          </n-gi>

          
          <n-gi :span="8">
            <n-form-item label="Khoảng thời gian">
              <n-date-picker
                v-model:value="dateRange"
                type="daterange"
                clearable
                :default-value="getTodayDateRange"
                :is-date-disabled="disablePreviousDate"
                style="width: 100%"
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
              />
            </n-form-item>
          </n-gi>
          
          <n-gi :span="4">
            <n-form-item label="Sắp xếp">
              <n-select
                v-model:value="state.sortOrder"
                :options="sortOptions"
                placeholder="Mới nhất"
              />
            </n-form-item>
          </n-gi>
          
          <n-gi :span="2">
            <n-form-item label=" ">
              <n-button
                @click="resetFilters"
                type="default"
                block
              >
                <template #icon>
                  <n-icon><RefreshIcon /></n-icon>
                </template>
                Đặt lại
              </n-button>
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
          <n-button 
            type="primary" 
            size="small" 
            @click="exportToExcel"
            class="ml-auto"
          >
            <template #icon>
              <n-icon><ExportIcon /></n-icon>
            </template>
            Xuất Excel
          </n-button>
        </div>
      </template>
      
      <div class="invoice-tabs mb-4">
        <n-tabs v-model:value="state.searchStatus" type="segment" @update:value="handleTabChange">
          <n-tab name="ALL">
            <span class="tab-content">
              Tất cả
              <n-badge :value="getTabCount('ALL')" type="info" :max="99" />
            </span>
          </n-tab>
          <n-tab v-for="tab in tabs" :key="tab.key" :name="tab.key">
            <span class="tab-content">
              {{ tab.label }}
              <n-badge :value="tab.count" type="info" :max="99" />
            </span>
          </n-tab>
        </n-tabs>
      </div>

      <n-data-table
        :columns="columns"
        :data="state.products"
        :pagination="{
          page: state.paginationParams.page,
          pageSize: state.paginationParams.size,
          pageCount: Math.ceil(state.totalItems / state.paginationParams.size),
          showSizePicker: true,
          pageSizes: [10, 20, 30, 40, 50],
          showQuickJumper: true
        }"
        :max-height="500"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
        striped
        :loading="state.loading"
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
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import BreadcrumbDefault from '@/layouts/components/header/Breadcrumb.vue'
import * as XLSX from 'xlsx'

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

// ========== ENHANCED FAKE DATA ==========

// Enum cho trạng thái hóa đơn
enum EntityTrangThaiHoaDon {
  CHO_XAC_NHAN = 0,
  DA_XAC_NHAN = 1,
  CHO_GIAO = 2,
  DANG_GIAO = 3,
  HOAN_THANH = 4,
  DA_HUY = 5,
}

enum EntityLoaiHoaDon {
  OFFLINE = "OFFLINE",
  GIAO_HANG = "GIAO_HANG",
  ONLINE = "ONLINE",
}

// Fake data for invoices với đầy đủ thông tin và cấu trúc phù hợp với trang chi tiết
const fakeInvoices = Array.from({ length: 50 }, (_, index) => {
  const id = index + 1
  const maHoaDon = `HD${String(id).padStart(3, '0')}`
  const invoiceTypes = [EntityLoaiHoaDon.OFFLINE, EntityLoaiHoaDon.GIAO_HANG, EntityLoaiHoaDon.ONLINE]
  const statuses = ['0', '1', '2', '3', '4', '5'] // String values để phù hợp với API
  const paymentStatuses = ['0', '1'] // 0: Chưa thanh toán, 1: Đã thanh toán
  
  // Tạo ngày ngẫu nhiên trong 30 ngày qua
  const daysAgo = Math.floor(Math.random() * 30)
  const createdDate = Date.now() - (daysAgo * 86400000)
  const updatedDate = createdDate + Math.floor(Math.random() * 86400000)
  
  const loaiHoaDon = invoiceTypes[Math.floor(Math.random() * invoiceTypes.length)]
  const trangThaiHoaDon = statuses[Math.floor(Math.random() * statuses.length)]
  const phuongThucThanhToan = Math.random() > 0.5 ? '0' : '1' // 0: Tiền mặt, 1: Chuyển khoản
  
  const tongTienHang = Math.floor(Math.random() * (50000000 - 1000000) + 1000000)
  const phiVanChuyen = loaiHoaDon === EntityLoaiHoaDon.GIAO_HANG ? Math.floor(Math.random() * 50000) : 0
  const giaTriPGG = Math.floor(Math.random() * 1000000)
  const thanhTien = tongTienHang - giaTriPGG + phiVanChuyen
  
  // Tạo tên khách hàng ngẫu nhiên
  const firstName = ['Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Phan', 'Vũ', 'Đặng', 'Bùi', 'Đỗ']
  const middleName = ['Văn', 'Thị', 'Minh', 'Hoàng', 'Hữu', 'Công', 'Kim', 'Thanh']
  const lastName = ['An', 'Bình', 'Chi', 'Dũng', 'Giang', 'Hương', 'Khoa', 'Long', 'Mai', 'Nam']
  
  const tenKhachHang = `${firstName[Math.floor(Math.random() * firstName.length)]} ${middleName[Math.floor(Math.random() * middleName.length)]} ${lastName[Math.floor(Math.random() * lastName.length)]}`
  const sdtKhachHang = `09${Math.floor(Math.random() * 90000000 + 10000000)}`
  
  // Tạo nhân viên
  const maNhanVien = `NV${String(Math.floor(Math.random() * 10) + 1).padStart(3, '0')}`
  const nhanVienNames = ['Nguyễn Thị Mai', 'Trần Văn Nam', 'Lê Thị Hồng', 'Phạm Văn Đức', 'Hoàng Thị Lan', 'Phan Công Minh', 'Vũ Thị Ngọc', 'Đặng Văn Hải', 'Bùi Thị Thu', 'Đỗ Văn Tùng']
  const tenNhanVien = nhanVienNames[Math.floor(Math.random() * nhanVienNames.length)]
  
  // Tạo chi nhánh
  const chiNhanhNames = ['Chi nhánh Quận 1', 'Chi nhánh Quận 3', 'Chi nhánh Quận 5', 'Chi nhánh Quận 7', 'Chi nhánh Quận 10']
  const chiNhanh = chiNhanhNames[Math.floor(Math.random() * chiNhanhNames.length)]
  
  // Tạo dữ liệu sản phẩm mẫu
  const products = [
    {
      maHoaDonChiTiet: `HDCT${String(id).padStart(3, '0')}-1`,
      maHoaDon: maHoaDon,
      tenSanPham: 'iPhone 15 Pro Max 256GB',
      thuongHieu: 'Apple',
      mauSac: 'Titan tự nhiên',
      size: '256GB',
      giaBan: 32000000,
      soLuong: 1,
      thanhTien: 32000000,
      anhSanPham: 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=400&h=400&fit=crop'
    },
    {
      maHoaDonChiTiet: `HDCT${String(id).padStart(3, '0')}-2`,
      maHoaDon: maHoaDon,
      tenSanPham: 'Ốp lưng iPhone 15 Pro Max',
      thuongHieu: 'Spigen',
      mauSac: 'Đen',
      size: '15 Pro Max',
      giaBan: 500000,
      soLuong: 1,
      thanhTien: 500000,
      anhSanPham: 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=400&h=400&fit=crop'
    }
  ]
  
  return {
    id: id.toString(),
    maHoaDon: maHoaDon,
    maDonHang: `DH${String(id).padStart(4, '0')}`,
    tenHoaDon: `Hóa đơn ${maHoaDon}`,
    tenKhachHang: tenKhachHang,
    sdtKH: sdtKhachHang,
    email: `${tenKhachHang.replace(/\s+/g, '').toLowerCase()}@gmail.com`,
    diaChi: loaiHoaDon !== EntityLoaiHoaDon.OFFLINE ? 
      `Số ${Math.floor(Math.random() * 100)}, Đường ${String.fromCharCode(65 + Math.floor(Math.random() * 26))}, Quận ${Math.floor(Math.random() * 12) + 1}, TP.HCM` : 
      null,
    tenKhachHang2: tenKhachHang,
    sdtKH2: sdtKhachHang,
    email2: `${tenKhachHang.replace(/\s+/g, '').toLowerCase()}@email.com`,
    diaChi2: loaiHoaDon === EntityLoaiHoaDon.OFFLINE ? 'Mua tại quầy' : 
      `Số ${Math.floor(Math.random() * 100)}, Đường ${String.fromCharCode(65 + Math.floor(Math.random() * 26))}, Quận ${Math.floor(Math.random() * 12) + 1}, TP.HCM`,
    loaiHoaDon: loaiHoaDon,
    trangThaiHoaDon: trangThaiHoaDon,
    phuongThucThanhToan: phuongThucThanhToan,
    ngayTao: new Date(createdDate).toISOString(),
    ngayCapNhat: new Date(updatedDate).toISOString(),
    phiVanChuyen: phiVanChuyen,
    maPGG: Math.random() > 0.7 ? `VOUCHER${Math.floor(Math.random() * 1000)}` : null,
    tenPGG: Math.random() > 0.7 ? ['Giảm giá 10%', 'Giảm giá mùa hè', 'Khuyến mãi đặc biệt', 'Giảm giá thân thiết'][Math.floor(Math.random() * 4)] : null,
    giaTriPGG: giaTriPGG,
    thanhTien: thanhTien,
    tongTienHang: tongTienHang,
    giamGiaSanPham: 0,
    giamGiaHoaDon: giaTriPGG,
    daThanhToan: Math.random() > 0.5 ? thanhTien : Math.floor(thanhTien * 0.7),
    conNo: Math.random() > 0.5 ? 0 : Math.floor(thanhTien * 0.3),
    ghiChuKhachHang: Math.random() > 0.8 ? 'Giao hàng trước 17h' : null,
    ghiChuNoiBo: Math.random() > 0.8 ? 'Khách hàng VIP' : null,
    nhanVien: {
      ten: tenNhanVien,
      ma: maNhanVien,
      avatar: `https://i.pravatar.cc/150?img=${Math.floor(Math.random() * 70)}`
    },
    chiNhanh: {
      ten: chiNhanh
    },
    kenhBanHang: ['WEBSITE', 'TRUC_TIEP', 'FACEBOOK', 'ZALO'][Math.floor(Math.random() * 4)],
    hinhThucThanhToan: phuongThucThanhToan === '0' ? 'TIEN_MAT' : 'CHUYEN_KHOAN',
    chiTietList: products,
    
    // Các field cũ để tương thích với bảng hiện tại
    tenKhachHangOld: tenKhachHang,
    sdtKhachHangOld: sdtKhachHang,
    loaiHoaDonOld: loaiHoaDon,
    maNhanVienOld: maNhanVien,
    tenNhanVienOld: tenNhanVien,
    tongTienOld: tongTienHang,
    tienShipOld: phiVanChuyen,
    tienGiamOld: giaTriPGG,
    thanhTienOld: thanhTien,
    createdDateOld: createdDate,
    updatedDateOld: updatedDate,
    statusOld: trangThaiHoaDon === '0' ? 'CHO_XAC_NHAN' : 
               trangThaiHoaDon === '1' ? 'DA_XAC_NHAN' :
               trangThaiHoaDon === '2' ? 'CHO_GIAO' :
               trangThaiHoaDon === '3' ? 'DANG_GIAO' :
               trangThaiHoaDon === '4' ? 'HOAN_THANH' : 'DA_HUY',
    paymentStatusOld: phuongThucThanhToan === '0' ? 'CHUA_THANH_TOAN' : 'DA_THANH_TOAN',
    phuongThucThanhToanOld: phuongThucThanhToan === '0' ? 'TIEN_MAT' : 'CHUYEN_KHOAN',
    diaChiGiaoHangOld: loaiHoaDon !== EntityLoaiHoaDon.OFFLINE ? 
      `Số ${Math.floor(Math.random() * 100)}, Đường ${String.fromCharCode(65 + Math.floor(Math.random() * 26))}, Quận ${Math.floor(Math.random() * 12) + 1}, TP.HCM` : 
      null,
    itemsCount: Math.floor(Math.random() * 5) + 1,
    
    // Thêm dữ liệu cho timeline
    timelineStatusData: [
      {
        trangThai: trangThaiHoaDon,
        note: 'Đơn hàng được tạo',
        thoiGian: new Date(createdDate).toISOString(),
        nguoiThucHien: 'Hệ thống'
      }
    ],
    
    // Thêm lịch sử thanh toán
    lichSuThanhToan: phuongThucThanhToan === '1' ? [
      {
        id: 1,
        soTien: thanhTien,
        thoiGian: new Date(createdDate).toISOString(),
        loaiGiaoDich: phuongThucThanhToan,
        tenNhanVien: tenNhanVien,
        ghiChu: 'Thanh toán đầy đủ'
      }
    ] : []
  }
})

// State
const state = reactive({
  searchQuery: '',
  searchStatus: null as string | null,
  loaiHoaDon: null as string | null,
  sortOrder: 'desc' as 'asc' | 'desc',
  loading: false,
  selectedProductId: null as string | null,
  products: [] as any[],
  paginationParams: { page: 1, size: 10 },
  totalItems: 0,
  startDate: null as number | null,
  endDate: null as number | null
})

const dateRange = ref<[number, number] | null>(null)

// Computed: Date range mặc định là hôm nay
const getTodayDateRange = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const tomorrow = new Date(today)
  tomorrow.setDate(tomorrow.getDate() + 1)
  return [today.getTime(), tomorrow.getTime() - 1]
})

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

const paymentStatusOptions = [
  { label: 'Chưa thanh toán', value: 'CHUA_THANH_TOAN' },
  { label: 'Đã thanh toán', value: 'DA_THANH_TOAN' },
  { label: 'Thanh toán một phần', value: 'THANH_TOAN_MOT_PHAN' }
]

// Status colors
const statusColors: Record<string, string> = {
  CHO_XAC_NHAN: 'orange',
  DA_XAC_NHAN: 'gold',
  CHO_GIAO: 'blue',
  DANG_GIAO: 'cyan',
  HOAN_THANH: 'green',
  DA_HUY: 'red'
}

const paymentStatusColors: Record<string, string> = {
  CHUA_THANH_TOAN: 'red',
  DA_THANH_TOAN: 'green',
  THANH_TOAN_MOT_PHAN: 'orange'
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

const paymentStatusLabels: Record<string, string> = {
  CHUA_THANH_TOAN: 'Chưa thanh toán',
  DA_THANH_TOAN: 'Đã thanh toán',
  THANH_TOAN_MOT_PHAN: 'Thanh toán 1 phần'
}

// LoaiHoaDon colors
const loaiHoaDonColors: Record<string, string> = {
  OFFLINE: 'green',
  GIAO_HANG: 'blue',
  ONLINE: 'purple'
}

// LoaiHoaDon labels
const loaiHoaDonLabels: Record<string, string> = {
  OFFLINE: 'Tại quầy',
  GIAO_HANG: 'Giao hàng',
  ONLINE: 'Online'
}

const phuongThucTTLabels: Record<string, string> = {
  TIEN_MAT: 'Tiền mặt',
  CHUYEN_KHOAN: 'Chuyển khoản',
  VI_DIEN_TU: 'Ví điện tử'
}

// Computed
const filteredProducts = computed(() => {
  let result = [...fakeInvoices]
  
  // Sắp xếp theo ngày tạo
  result.sort((a, b) => {
    return state.sortOrder === 'desc' ? 
      b.createdDateOld - a.createdDateOld : 
      a.createdDateOld - b.createdDateOld
  })
  
  // Filter by search query
  if (state.searchQuery) {
    const search = state.searchQuery.toLowerCase()
    result = result.filter(p => 
      p.maHoaDon.toLowerCase().includes(search) ||
      p.tenKhachHangOld.toLowerCase().includes(search) ||
      p.sdtKhachHangOld.includes(search) ||
      p.maNhanVienOld.toLowerCase().includes(search)
    )
  }
  
  // Filter by status
  if (state.searchStatus && state.searchStatus !== 'ALL') {
    result = result.filter(p => p.statusOld === state.searchStatus)
  }
  
  // Filter by loại hóa đơn
  if (state.loaiHoaDon) {
    result = result.filter(p => p.loaiHoaDonOld === state.loaiHoaDon)
  }
  
  // Filter by date range
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    result = result.filter(p => {
      const date = p.createdDateOld
      return date >= start && date <= end
    })
  } else {
    // Mặc định hiển thị hôm nay
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const tomorrow = new Date(today)
    tomorrow.setDate(tomorrow.getDate() + 1)
    
    result = result.filter(p => {
      return p.createdDateOld >= today.getTime() && p.createdDateOld < tomorrow.getTime()
    })
  }
  
  return result
})

const tabs = computed(() => {
  const counts = getStatusCounts()
  return [
    { key: 'CHO_XAC_NHAN', label: 'Chờ xác nhận', count: counts.CHO_XAC_NHAN || 0 },
    { key: 'DA_XAC_NHAN', label: 'Đã xác nhận', count: counts.DA_XAC_NHAN || 0 },
    { key: 'CHO_GIAO', label: 'Chờ giao', count: counts.CHO_GIAO || 0 },
    { key: 'DANG_GIAO', label: 'Đang giao', count: counts.DANG_GIAO || 0 },
    { key: 'HOAN_THANH', label: 'Hoàn thành', count: counts.HOAN_THANH || 0 },
    { key: 'DA_HUY', label: 'Đã hủy', count: counts.DA_HUY || 0 }
  ]
})

// Functions
const getStatusCounts = () => {
  const counts: Record<string, number> = {}
  fakeInvoices.forEach(invoice => {
    counts[invoice.statusOld] = (counts[invoice.statusOld] || 0) + 1
  })
  return counts
}

const getTabCount = (tabKey: string) => {
  if (tabKey === 'ALL') return fakeInvoices.length
  const counts = getStatusCounts()
  return counts[tabKey] || 0
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

const formatDateTime = (timestamp: number) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const year = date.getFullYear()
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${hours}:${minutes} ${day}/${month}/${year}`
}

const disablePreviousDate = (timestamp: number) => {
  return timestamp > Date.now()
}

const handlePrintInvoice = (invoice: any) => {
  // Mở cửa sổ mới để in
  const printWindow = window.open('', '_blank')
  if (!printWindow) return
  
  const printContent = `
    <!DOCTYPE html>
    <html>
    <head>
      <title>Hóa đơn ${invoice.maHoaDon}</title>
      <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
        .invoice-header { text-align: center; margin-bottom: 30px; }
        .invoice-title { font-size: 24px; font-weight: bold; margin-bottom: 5px; }
        .invoice-info { margin-bottom: 20px; }
        .info-row { display: flex; margin-bottom: 5px; }
        .info-label { font-weight: bold; min-width: 150px; }
        .info-value { }
        .items-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
        .items-table th { background: #f5f5f5; padding: 10px; text-align: left; }
        .items-table td { padding: 10px; border-bottom: 1px solid #ddd; }
        .total-section { text-align: right; margin-top: 20px; font-size: 16px; }
        .total-row { margin: 5px 0; }
        .grand-total { font-size: 18px; font-weight: bold; color: #d32f2f; }
        .thank-you { text-align: center; margin-top: 30px; font-style: italic; }
        @media print {
          body { -webkit-print-color-adjust: exact; }
          .no-print { display: none !important; }
        }
      </style>
    </head>
    <body>
      <div class="invoice-header">
        <div class="invoice-title">HÓA ĐƠN BÁN HÀNG</div>
        <div>Mã: ${invoice.maHoaDon}</div>
      </div>
      
      <div class="invoice-info">
        <div class="info-row">
          <div class="info-label">Khách hàng:</div>
          <div class="info-value">${invoice.tenKhachHangOld} - ${invoice.sdtKhachHangOld}</div>
        </div>
        <div class="info-row">
          <div class="info-label">Ngày tạo:</div>
          <div class="info-value">${formatDateTime(invoice.createdDateOld)}</div>
        </div>
        <div class="info-row">
          <div class="info-label">Nhân viên:</div>
          <div class="info-value">${invoice.tenNhanVienOld} (${invoice.maNhanVienOld})</div>
        </div>
        ${invoice.diaChiGiaoHangOld ? `
        <div class="info-row">
          <div class="info-label">Địa chỉ giao:</div>
          <div class="info-value">${invoice.diaChiGiaoHangOld}</div>
        </div>` : ''}
      </div>
      
      <table class="items-table">
        <thead>
          <tr>
            <th>STT</th>
            <th>Sản phẩm</th>
            <th>Số lượng</th>
            <th>Đơn giá</th>
            <th>Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          ${Array.from({ length: invoice.itemsCount }, (_, i) => `
            <tr>
              <td>${i + 1}</td>
              <td>Laptop ${String.fromCharCode(65 + i)}</td>
              <td>${Math.floor(Math.random() * 3) + 1}</td>
              <td>${formatCurrency(Math.floor(Math.random() * 10000000) + 5000000)}</td>
              <td>${formatCurrency(Math.floor(Math.random() * 20000000) + 10000000)}</td>
            </tr>
          `).join('')}
        </tbody>
      </table>
      
      <div class="total-section">
        <div class="total-row">Tổng tiền hàng: ${formatCurrency(invoice.tongTienOld)}</div>
        <div class="total-row">Phí vận chuyển: ${formatCurrency(invoice.tienShipOld)}</div>
        <div class="total-row">Giảm giá: -${formatCurrency(invoice.tienGiamOld)}</div>
        <div class="total-row grand-total">Thành tiền: ${formatCurrency(invoice.thanhTienOld)}</div>
        <div class="total-row">Phương thức: ${phuongThucTTLabels[invoice.phuongThucThanhToanOld]}</div>
        <div class="total-row">Trạng thái: ${paymentStatusLabels[invoice.paymentStatusOld]}</div>
      </div>
      
      <div class="thank-you">
        <p>Cảm ơn quý khách đã mua hàng!</p>
        <p>Hẹn gặp lại.</p>
      </div>
      
      <div class="no-print" style="margin-top: 20px; text-align: center;">
        <button onclick="window.print()" style="padding: 10px 20px; background: #1890ff; color: white; border: none; border-radius: 4px; cursor: pointer;">
          In hóa đơn
        </button>
        <button onclick="window.close()" style="padding: 10px 20px; margin-left: 10px; background: #f5222d; color: white; border: none; border-radius: 4px; cursor: pointer;">
          Đóng
        </button>
      </div>
      
      <script>
        window.onload = function() {
          window.print();
        }
      <\/script>
    </body>
    </html>
  `
  
  printWindow.document.write(printContent)
  printWindow.document.close()
  message.success('Đang mở cửa sổ in hóa đơn...')
}


const handleViewClick = (invoice: any) => {
  const invoiceId = invoice.maHoaDon || invoice.id || 'HD001'
  
  // Sử dụng router.push để điều hướng trong cùng ứng dụng
  router.push({
    name: 'orders_detail', // Hoặc path: `/orders/detail/${invoiceId}`
    params: { id: invoiceId }
  })
}

const updateDisplayedProducts = () => {
  const startIndex = (state.paginationParams.page - 1) * state.paginationParams.size
  const endIndex = startIndex + state.paginationParams.size
  state.products = filteredProducts.value.slice(startIndex, endIndex)
  state.totalItems = filteredProducts.value.length
}

const exportToExcel = () => {
  try {
    const exportData = filteredProducts.value.map(invoice => ({
      'Mã hóa đơn': invoice.maHoaDon,
      'Khách hàng': invoice.tenKhachHangOld,
      'SĐT': invoice.sdtKhachHangOld,
      'Loại': loaiHoaDonLabels[invoice.loaiHoaDonOld],
      'Nhân viên': invoice.tenNhanVienOld,
      'Mã NV': invoice.maNhanVienOld,
      'Tổng tiền': invoice.tongTienOld,
      'Phí vận chuyển': invoice.tienShipOld,
      'Giảm giá': invoice.tienGiamOld,
      'Thành tiền': invoice.thanhTienOld,
      'Ngày tạo': formatDateTime(invoice.createdDateOld),
      'Trạng thái': statusLabels[invoice.statusOld],
      'TT thanh toán': paymentStatusLabels[invoice.paymentStatusOld],
      'Phương thức TT': phuongThucTTLabels[invoice.phuongThucThanhToanOld]
    }))
    
    const worksheet = XLSX.utils.json_to_sheet(exportData)
    const workbook = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(workbook, worksheet, 'Hóa đơn')
    
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
  state.searchStatus = key === 'ALL' ? null : key
  state.paginationParams.page = 1
  updateDisplayedProducts()
}

const handlePageChange = (page: number) => {
  state.paginationParams.page = page
  updateDisplayedProducts()
}

const handlePageSizeChange = (pageSize: number) => {
  state.paginationParams.size = pageSize
  state.paginationParams.page = 1
  updateDisplayedProducts()
}

const resetFilters = () => {
  state.searchQuery = ''
  state.searchStatus = null
  state.loaiHoaDon = null
  state.sortOrder = 'desc'
  dateRange.value = getTodayDateRange.value as [number, number]
  state.paginationParams.page = 1
  updateDisplayedProducts()
  message.success('Đã đặt lại bộ lọc')
}

// Columns definition
const createColumns = (): DataTableColumns => [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => h('span', 
      { style: 'font-weight: 500;' }, 
      index + 1 + (state.paginationParams.page - 1) * state.paginationParams.size
    )
  },
  {
    title: 'Mã hóa đơn',
    key: 'maHoaDon',
    width: 120,
    ellipsis: true,
    sorter: (a, b) => a.maHoaDon.localeCompare(b.maHoaDon),
    render: (row) => h(NText, { 
      type: 'primary', 
      strong: true,
      style: 'cursor: pointer; text-decoration: underline;',
      onClick: () => handleViewClick(row)
    }, { default: () => row.maHoaDon })
  },
  {
    title: 'Khách hàng',
    key: 'tenKhachHang',
    width: 180,
    sorter: (a, b) => a.tenKhachHangOld.localeCompare(b.tenKhachHangOld),
    render: (row) => h('div', [
      h('div', { 
        style: 'font-weight: 500; cursor: pointer;',
        onClick: () => handleViewClick(row)
      }, row.tenKhachHangOld),
      h('div', { style: 'font-size: 12px; color: #666;' }, row.sdtKhachHangOld)
    ])
  },
  {
    title: 'Nhân viên',
    key: 'nhanVien',
    width: 150,
    sorter: (a, b) => a.tenNhanVienOld.localeCompare(b.tenNhanVienOld),
    render: (row) => h('div', { onClick: () => handleViewClick(row), style: 'cursor: pointer;' }, [
      h('div', { 
        style: 'font-weight: 500; font-size: 13px;',
        title: row.tenNhanVienOld
      }, row.tenNhanVienOld),
      h('div', { 
        style: 'font-size: 11px; color: #1890ff; background: #e6f7ff; padding: 1px 4px; border-radius: 3px; display: inline-block; margin-top: 2px;',
        title: 'Mã nhân viên'
      }, row.maNhanVienOld)
    ])
  },
  {
    title: 'Loại',
    key: 'loaiHD',
    width: 100,
    align: 'center',
    sorter: (a, b) => a.loaiHoaDonOld.localeCompare(b.loaiHoaDonOld),
    render: (row) => h(NTag, {
      type: 'primary',
      bordered: false,
      color: loaiHoaDonColors[row.loaiHoaDonOld],
      size: 'small'
    }, { default: () => loaiHoaDonLabels[row.loaiHoaDonOld] })
  },
  {
    title: 'Ngày tạo',
    key: 'ngayTao',
    width: 140,
    sorter: (a, b) => a.createdDateOld - b.createdDateOld,
    render: (row) => h('div', { onClick: () => handleViewClick(row), style: 'cursor: pointer;' }, [
      h('div', { style: 'font-weight: 500; font-size: 12px;' }, formatDateTime(row.createdDateOld)),
      h('div', { style: 'font-size: 11px; color: #999;' }, 'Cập nhật: ' + formatDateTime(row.updatedDateOld))
    ])
  },
  {
    title: 'Tổng tiền',
    key: 'tongTien',
    width: 130,
    align: 'right',
    sorter: (a, b) => a.tongTienOld - b.tongTienOld,
    render: (row) => h('div', { onClick: () => handleViewClick(row), style: 'cursor: pointer;' }, [
      h('div', { style: 'font-weight: 600; color: #d32f2f;' }, formatCurrency(row.thanhTienOld)),
      h('div', { 
        style: 'font-size: 11px; color: #999; text-align: right;',
        title: `Tiền hàng: ${formatCurrency(row.tongTienOld)} | Ship: ${formatCurrency(row.tienShipOld)} | Giảm: ${formatCurrency(row.tienGiamOld)}`
      }, `${formatCurrency(row.tongTienOld)}`)
    ])
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 120,
    align: 'center',
    sorter: (a, b) => a.statusOld.localeCompare(b.statusOld),
    render: (row) => h('div', { onClick: () => handleViewClick(row), style: 'cursor: pointer;' }, [
      h(NTag, {
        type: 'primary',
        bordered: false,
        color: statusColors[row.statusOld],
        size: 'small',
        style: 'margin-bottom: 3px;'
      }, { default: () => statusLabels[row.statusOld] }),
      h(NTag, {
        type: 'primary',
        bordered: false,
        color: paymentStatusColors[row.paymentStatusOld],
        size: 'small'
      }, { default: () => paymentStatusLabels[row.paymentStatusOld] })
    ])
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 120,
    align: 'center',
    render: (row) => h(NSpace, { justify: 'center', size: 'small' }, [
      h(NTooltip, {}, {
        trigger: () => h(NButton, {
          size: 'small',
          type: 'primary',
          onClick: () => handleViewClick(row)
        }, {
          icon: () => h(NIcon, {}, () => h(EyeIcon))
        }),
        default: () => 'Xem chi tiết'
      }),
      
      row.paymentStatusOld === 'DA_THANH_TOAN' || row.paymentStatusOld === 'THANH_TOAN_MOT_PHAN' ? 
        h(NTooltip, {}, {
          trigger: () => h(NButton, {
            size: 'small',
            type: 'info',
            onClick: () => handlePrintInvoice(row)
          }, {
            icon: () => h(NIcon, {}, () => h(PrinterIcon))
          }),
          default: () => 'In hóa đơn'
        }) : null
    ])
  }
]

const columns = createColumns()

// Watch for changes
watch([() => state.searchQuery, () => state.searchStatus, () => state.loaiHoaDon, () => state.sortOrder, dateRange], () => {
  state.paginationParams.page = 1
  updateDisplayedProducts()
})

// Initialize
onMounted(() => {
  // Set default date range to today
  dateRange.value = getTodayDateRange.value as [number, number]
  updateDisplayedProducts()
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