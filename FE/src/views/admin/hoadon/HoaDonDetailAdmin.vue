<template>
  <div class="container mx-auto p-6 space-y-6">
    <div class="breadcrumb-section">
      <BreadcrumbDefault 
        :pageTitle="'Chi tiết hóa đơn'" 
        :routes="[
          { path: '/admin/hoa-don', name: 'Quản lý hóa đơn' },
          { path: '', name: 'Chi tiết hóa đơn' },
        ]" 
      />
    </div>
    
    <!-- Header thông tin chính -->
    <div class="flex justify-between items-center mb-6">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">Hóa đơn #{{ hoaDon?.maHoaDon }}</h1>
        <div class="flex items-center gap-3 mt-2">
          <span class="px-3 py-1 rounded-full text-sm font-medium" 
                :class="getStatusClass(hoaDon?.trangThaiHoaDon)">
            {{ getStatusText(hoaDon?.trangThaiHoaDon) }}
          </span>
          <span class="px-3 py-1 rounded-full text-sm font-medium bg-green-100 text-green-800">
            Bán tại quầy
          </span>
          <span class="px-3 py-1 rounded-full text-sm font-medium bg-blue-100 text-blue-800">
            {{ getPaymentMethodText(hoaDon?.phuongThucThanhToan) }}
          </span>
          <span class="text-gray-500 text-sm">
            {{ formatDateTime(hoaDon?.ngayTao) }}
          </span>
        </div>
      </div>
      
      <div class="flex gap-2">
        <n-button type="primary" @click="handlePrintPDF" :loading="printLoading">
          <template #icon>
            <n-icon>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M6 9V2h12v7M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2M6 14h12v8H6v-8z"
                  stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </n-icon>
          </template>
          In hóa đơn
        </n-button>
        
        <n-button type="primary" ghost @click="handleBack">
          <template #icon>
            <n-icon>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M19 12H5M12 19l-7-7 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </n-icon>
          </template>
          Quay lại
        </n-button>
      </div>
    </div>

    <!-- Grid thông tin -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Thông tin khách hàng -->
      <n-card title="THÔNG TIN KHÁCH HÀNG" bordered>
        <div class="space-y-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" class="text-blue-600">
                <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
            <div>
              <h3 class="font-semibold text-lg">{{ hoaDon?.tenKhachHang || 'Khách lẻ' }}</h3>
              <p class="text-gray-500">{{ hoaDon?.sdtKH || 'Chưa có số điện thoại' }}</p>
            </div>
          </div>
          
          <div class="space-y-2">
            <div class="flex items-center gap-2 text-gray-600">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="text-gray-400">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M22 6l-10 7L2 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span>{{ hoaDon?.email || 'Chưa có email' }}</span>
            </div>
            
            <div class="flex items-start gap-2 text-gray-600">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" class="text-gray-400 mt-1">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <circle cx="12" cy="10" r="3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <span class="flex-1">Mua tại cửa hàng</span>
            </div>
          </div>
          
          <!-- Thông tin nhân viên -->
          <div class="pt-4 border-t">
            <div class="flex items-center gap-3">
              <img :src="hoaDon?.nhanVien?.avatar || 'https://i.pravatar.cc/150?img=32'" 
                   alt="Nhân viên" 
                   class="w-10 h-10 rounded-full border">
              <div>
                <p class="text-sm text-gray-500">Nhân viên bán hàng</p>
                <p class="font-medium">{{ hoaDon?.nhanVien?.ten }} ({{ hoaDon?.nhanVien?.ma }})</p>
              </div>
            </div>
          </div>
        </div>
      </n-card>

      <!-- Thông tin thanh toán -->
      <n-card title="THÔNG TIN THANH TOÁN" bordered>
        <div class="space-y-3">
          <div class="flex justify-between">
            <span class="text-gray-600">Tổng tiền hàng:</span>
            <span class="font-medium">{{ formatCurrency(hoaDon?.tongTienHang) }}</span>
          </div>
          
          <div v-if="hoaDon?.giamGiaSanPham && hoaDon.giamGiaSanPham > 0" class="flex justify-between">
            <span class="text-gray-600">Giảm giá sản phẩm:</span>
            <span class="font-medium text-green-600">-{{ formatCurrency(hoaDon.giamGiaSanPham) }}</span>
          </div>
          
          <div v-if="hoaDon?.giamGiaHoaDon && hoaDon.giamGiaHoaDon > 0" class="flex justify-between">
            <span class="text-gray-600">Giảm giá hóa đơn:</span>
            <span class="font-medium text-green-600">-{{ formatCurrency(hoaDon.giamGiaHoaDon) }}</span>
          </div>
          
          <div class="flex justify-between border-t pt-3 mt-2">
            <span class="text-lg font-semibold">Thành tiền:</span>
            <span class="text-lg font-bold text-red-600">{{ formatCurrency(hoaDon?.thanhTien) }}</span>
          </div>
          
          <div class="flex justify-between">
            <span class="text-gray-600">Đã thanh toán:</span>
            <span class="font-medium text-green-600">{{ formatCurrency(hoaDon?.daThanhToan || 0) }}</span>
          </div>
          
          <div v-if="hoaDon?.conNo && hoaDon.conNo > 0" class="flex justify-between">
            <span class="text-gray-600">Còn nợ:</span>
            <span class="font-medium text-orange-600">{{ formatCurrency(hoaDon.conNo) }}</span>
          </div>
          
          <div class="mt-4 p-3 bg-gray-50 rounded space-y-2">
            <div class="flex items-center justify-between">
              <span class="font-medium">Phương thức thanh toán:</span>
              <span class="text-blue-600">{{ getPaymentMethodText(hoaDon?.phuongThucThanhToan) }}</span>
            </div>
            <div class="flex items-center justify-between">
              <span class="font-medium">Trạng thái thanh toán:</span>
              <span :class="getPaymentStatusClass(hoaDon?.daThanhToan, hoaDon?.thanhTien)">
                {{ getPaymentStatusText(hoaDon?.daThanhToan, hoaDon?.thanhTien) }}
              </span>
            </div>
          </div>
        </div>
      </n-card>
    </div>

    <!-- Danh sách sản phẩm LAPTOP -->
    <n-card title="DANH SÁCH LAPTOP" bordered>
      <n-data-table
        :columns="productColumns"
        :data="hoaDon?.chiTietList || []"
        :pagination="false"
        :max-height="400"
        striped
      >
        <template #empty>
          <div class="text-center py-8 text-gray-500">
            Không có sản phẩm trong hóa đơn này
          </div>
        </template>
      </n-data-table>
      
      <!-- Tổng kết -->
      <div class="border-t mt-6 pt-6">
        <div class="max-w-md ml-auto space-y-2">
          <div class="flex justify-between">
            <span class="text-gray-600">Tổng tiền hàng:</span>
            <span class="font-medium">{{ formatCurrency(hoaDon?.tongTienHang) }}</span>
          </div>
          <div v-if="hoaDon?.giamGiaSanPham && hoaDon.giamGiaSanPham > 0" class="flex justify-between">
            <span class="text-gray-600">Giảm giá sản phẩm:</span>
            <span class="font-medium text-green-600">-{{ formatCurrency(hoaDon.giamGiaSanPham) }}</span>
          </div>
          <div v-if="hoaDon?.giamGiaHoaDon && hoaDon.giamGiaHoaDon > 0" class="flex justify-between">
            <span class="text-gray-600">Giảm giá hóa đơn:</span>
            <span class="font-medium text-green-600">-{{ formatCurrency(hoaDon.giamGiaHoaDon) }}</span>
          </div>
          <div class="flex justify-between border-t pt-2">
            <span class="text-lg font-semibold">Tổng cộng:</span>
            <span class="text-lg font-bold text-red-600">{{ formatCurrency(hoaDon?.thanhTien) }}</span>
          </div>
        </div>
      </div>
    </n-card>


    <!-- Ghi chú -->
    <n-card title="GHI CHÚ" bordered>
      <div class="space-y-4">
        <div v-if="hoaDon?.ghiChuKhachHang">
          <h4 class="font-semibold text-gray-700 mb-2">Ghi chú của khách hàng:</h4>
          <p class="text-gray-600 bg-blue-50 p-3 rounded">{{ hoaDon.ghiChuKhachHang }}</p>
        </div>
        
        <div v-if="hoaDon?.ghiChuNoiBo">
          <h4 class="font-semibold text-gray-700 mb-2">Ghi chú nội bộ:</h4>
          <p class="text-gray-600 bg-gray-50 p-3 rounded">{{ hoaDon.ghiChuNoiBo }}</p>
        </div>
        
        <div v-if="!hoaDon?.ghiChuKhachHang && !hoaDon?.ghiChuNoiBo">
          <p class="text-gray-500 text-center">Không có ghi chú</p>
        </div>
      </div>
    </n-card>

    <!-- Nút hành động -->
    <div class="flex justify-end gap-3 mt-6">
      <n-button type="success" @click="handleComplete" :loading="isLoading">
        <template #icon>
          <n-icon>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M5 13l4 4L19 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </n-icon>
        </template>
        Hoàn thành đơn
      </n-button>
      
      <n-button v-if="hoaDon?.trangThaiHoaDon !== '5'" type="error" ghost @click="openCancelModal">
        <template #icon>
          <n-icon>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </n-icon>
        </template>
        Hủy đơn hàng
      </n-button>
      
      <n-button type="default" @click="handleBack">
        Đóng
      </n-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NIcon,
  NDataTable,
  NCard,
  NTag,
  NAvatar,
  useMessage,
  type DataTableColumns
} from 'naive-ui'
import BreadcrumbDefault from '@/layouts/components/header/Breadcrumb.vue'

const router = useRouter()
const route = useRoute()
const message = useMessage()

// State
const hoaDon = ref<any>(null)
const printLoading = ref(false)
const isLoading = ref(false)

// Helper functions
const formatCurrency = (value: number | undefined | null): string => {
  if (value === undefined || value === null) return "0 ₫"
  return value.toLocaleString("vi-VN", { style: "currency", currency: "VND" })
}

const formatDateTime = (dateString: string | undefined): string => {
  if (!dateString) return ""
  try {
    const date = new Date(dateString)
    return date.toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    })
  } catch {
    return ""
  }
}

const formatDate = (dateString: string | undefined): string => {
  if (!dateString) return ""
  try {
    const date = new Date(dateString)
    return date.toLocaleDateString("vi-VN")
  } catch {
    return ""
  }
}

const getStatusText = (status: string | undefined): string => {
  const statusMap: Record<string, string> = {
    '0': 'Chờ xác nhận',
    '1': 'Đã xác nhận',
    '2': 'Đang xử lý',
    '3': 'Hoàn thành',
    '4': 'Đã giao',
    '5': 'Đã hủy'
  }
  return statusMap[status || '3'] || 'Hoàn thành'
}

const getStatusClass = (status: string | undefined): string => {
  const classMap: Record<string, string> = {
    '0': 'bg-yellow-100 text-yellow-800',
    '1': 'bg-blue-100 text-blue-800',
    '2': 'bg-purple-100 text-purple-800',
    '3': 'bg-green-100 text-green-800',
    '4': 'bg-green-100 text-green-800',
    '5': 'bg-red-100 text-red-800'
  }
  return classMap[status || '3'] || 'bg-green-100 text-green-800'
}

const getPaymentMethodText = (method: string | number | undefined): string => {
  if (method === undefined || method === null) return 'Tiền mặt'
  const methods: Record<string | number, string> = {
    '0': 'Tiền mặt',
    '1': 'Chuyển khoản',
    '2': 'Thẻ tín dụng',
    '3': 'QR Code',
    'TIEN_MAT': 'Tiền mặt',
    'CHUYEN_KHOAN': 'Chuyển khoản',
    'THE_TIN_DUNG': 'Thẻ tín dụng',
    'QR_CODE': 'QR Code'
  }
  return methods[method] || 'Tiền mặt'
}

const getPaymentStatusText = (paid: number = 0, total: number = 0): string => {
  if (paid === 0) return 'Chưa thanh toán'
  if (paid < total) return 'Thanh toán một phần'
  if (paid === total) return 'Đã thanh toán'
  return 'Thanh toán thừa'
}

const getPaymentStatusClass = (paid: number = 0, total: number = 0): string => {
  if (paid === 0) return 'text-red-600 font-medium'
  if (paid < total) return 'text-orange-600 font-medium'
  if (paid === total) return 'text-green-600 font-medium'
  return 'text-blue-600 font-medium'
}

const getWarrantyEndDate = (): string => {
  if (!hoaDon.value?.ngayTao) return ''
  const date = new Date(hoaDon.value.ngayTao)
  date.setFullYear(date.getFullYear() + 1) // Thêm 1 năm bảo hành
  return date.toISOString()
}

// Tạo dữ liệu fake cho laptop
const createFakeLaptopData = (invoiceId: string) => {
  console.log('Creating fake laptop invoice data:', invoiceId)
  
  const laptopBrands = ['Dell', 'HP', 'Lenovo', 'Asus', 'Acer', 'MSI', 'Apple']
  const laptopModels = [
    'XPS 15', 'Spectre x360', 'ThinkPad X1', 'ZenBook Pro', 'Swift 3', 'Prestige 14', 'MacBook Pro'
  ]
  const processors = ['Intel Core i5', 'Intel Core i7', 'Intel Core i9', 'AMD Ryzen 5', 'AMD Ryzen 7', 'Apple M1', 'Apple M2']
  const rams = ['8GB', '16GB', '32GB', '64GB']
  const storages = ['256GB SSD', '512GB SSD', '1TB SSD', '2TB SSD']
  const graphics = ['Intel Iris Xe', 'NVIDIA RTX 3050', 'NVIDIA RTX 3060', 'NVIDIA RTX 4070', 'AMD Radeon', 'Apple GPU 10-core']
  
  // Chọn ngẫu nhiên
  const brand = laptopBrands[Math.floor(Math.random() * laptopBrands.length)]
  const model = laptopModels[Math.floor(Math.random() * laptopModels.length)]
  const processor = processors[Math.floor(Math.random() * processors.length)]
  const ram = rams[Math.floor(Math.random() * rams.length)]
  const storage = storages[Math.floor(Math.random() * storages.length)]
  const graphic = graphics[Math.floor(Math.random() * graphics.length)]
  
  // Giá dựa trên cấu hình
  let basePrice = 15000000
  if (processor.includes('i7') || processor.includes('Ryzen 7')) basePrice += 5000000
  if (processor.includes('i9') || processor.includes('M2')) basePrice += 10000000
  if (ram === '16GB') basePrice += 2000000
  if (ram === '32GB') basePrice += 5000000
  if (storage.includes('512GB')) basePrice += 2000000
  if (storage.includes('1TB')) basePrice += 4000000
  if (graphic.includes('RTX')) basePrice += 7000000
  
  const finalPrice = basePrice + Math.floor(Math.random() * 2000000)
  
  const fakeData = {
    id: invoiceId.replace('HD', ''),
    maHoaDon: invoiceId,
    maDonHang: `DH${invoiceId.replace('HD', '').padStart(3, '0')}`,
    tenKhachHang: ['Nguyễn Văn An', 'Trần Thị Bình', 'Lê Văn Cường', 'Phạm Thị Dung', 'Hoàng Văn Em'][Math.floor(Math.random() * 5)],
    sdtKH: `09${Math.floor(Math.random() * 90000000 + 10000000)}`,
    email: `customer${Math.floor(Math.random() * 1000)}@gmail.com`,
    diaChi: 'Mua tại cửa hàng',
    loaiHoaDon: "OFFLINE",
    trangThaiHoaDon: "3", // Hoàn thành (bán tại quầy)
    phuongThucThanhToan: Math.random() > 0.5 ? "0" : "1",
    ngayTao: new Date(Date.now() - Math.floor(Math.random() * 86400000)).toISOString(), // Ngẫu nhiên trong 24h qua
    ngayCapNhat: new Date().toISOString(),
    phiVanChuyen: 0,
    maPGG: Math.random() > 0.7 ? `KM${Math.floor(Math.random() * 1000)}` : null,
    tenPGG: Math.random() > 0.7 ? ['Giảm giá sinh viên', 'Khuyến mãi tháng', 'Giảm giá đặc biệt'][Math.floor(Math.random() * 3)] : null,
    giaTriPGG: Math.random() > 0.7 ? Math.floor(finalPrice * 0.1) : 0,
    thanhTien: finalPrice - (Math.random() > 0.7 ? Math.floor(finalPrice * 0.1) : 0),
    tongTienHang: finalPrice,
    giamGiaSanPham: 0,
    giamGiaHoaDon: Math.random() > 0.7 ? Math.floor(finalPrice * 0.1) : 0,
    daThanhToan: finalPrice - (Math.random() > 0.7 ? Math.floor(finalPrice * 0.1) : 0),
    conNo: 0,
    ghiChuKhachHang: Math.random() > 0.8 ? 'Cần cài đặt thêm phần mềm văn phòng' : null,
    ghiChuNoiBo: Math.random() > 0.8 ? 'Khách mua nhiều lần' : null,
    nhanVien: {
      ten: ['Nguyễn Thị Hồng', 'Trần Văn Nam', 'Lê Thị Lan', 'Phạm Văn Đức'][Math.floor(Math.random() * 4)],
      ma: `NV${Math.floor(Math.random() * 10) + 1}`.padStart(3, '0'),
      avatar: `https://i.pravatar.cc/150?img=${Math.floor(Math.random() * 70)}`
    },
    chiNhanh: {
      ten: 'Cửa hàng Laptop TechZone'
    },
    chiTietList: [
      {
        maHoaDonChiTiet: `${invoiceId}-001`,
        maHoaDon: invoiceId,
        tenSanPham: `Laptop ${brand} ${model}`,
        thuongHieu: brand,
        mauSac: ['Đen', 'Bạc', 'Xám', 'Trắng'][Math.floor(Math.random() * 4)],
        size: '15.6 inch',
        cauHinh: `${processor}, ${ram}, ${storage}, ${graphic}`,
        giaBan: finalPrice,
        soLuong: 1,
        thanhTien: finalPrice - (Math.random() > 0.7 ? Math.floor(finalPrice * 0.1) : 0),
        anhSanPham: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400&h=400&fit=crop'
      }
    ],
    phuKienKemTheo: [
      { ten: 'Balo laptop', gia: 0, loai: 'Tặng kèm' },
      { ten: 'Chuột không dây', gia: 0, loai: 'Tặng kèm' },
      { ten: 'Tấm lót chuột', gia: 0, loai: 'Tặng kèm' }
    ]
  }
  
  return fakeData
}

// Actions
const handlePrintPDF = () => {
  printLoading.value = true
  setTimeout(() => {
    message.success("Đã tạo file PDF hóa đơn")
    printLoading.value = false
    
    // Tạo nội dung HTML để in
    const printContent = `
      <!DOCTYPE html>
      <html>
      <head>
        <title>Hóa đơn ${hoaDon.value.maHoaDon}</title>
        <style>
          body { font-family: Arial, sans-serif; margin: 0; padding: 20px; }
          .invoice-header { text-align: center; margin-bottom: 30px; }
          .invoice-title { font-size: 24px; font-weight: bold; margin-bottom: 5px; }
          .invoice-info { margin-bottom: 20px; }
          .info-row { display: flex; margin-bottom: 5px; }
          .info-label { font-weight: bold; min-width: 150px; }
          .items-table { width: 100%; border-collapse: collapse; margin: 20px 0; }
          .items-table th { background: #f5f5f5; padding: 10px; text-align: left; }
          .items-table td { padding: 10px; border-bottom: 1px solid #ddd; }
          .total-section { text-align: right; margin-top: 20px; font-size: 16px; }
          .total-row { margin: 5px 0; }
          .grand-total { font-size: 18px; font-weight: bold; color: #d32f2f; }
          .thank-you { text-align: center; margin-top: 30px; font-style: italic; }
        </style>
      </head>
      <body>
        <div class="invoice-header">
          <div class="invoice-title">HÓA ĐƠN BÁN LAPTOP</div>
          <div>Mã: ${hoaDon.value.maHoaDon}</div>
        </div>
        
        <div class="invoice-info">
          <div class="info-row">
            <div class="info-label">Khách hàng:</div>
            <div class="info-value">${hoaDon.value.tenKhachHang}</div>
          </div>
          <div class="info-row">
            <div class="info-label">Số điện thoại:</div>
            <div class="info-value">${hoaDon.value.sdtKH}</div>
          </div>
          <div class="info-row">
            <div class="info-label">Ngày mua:</div>
            <div class="info-value">${formatDateTime(hoaDon.value.ngayTao)}</div>
          </div>
          <div class="info-row">
            <div class="info-label">Nhân viên:</div>
            <div class="info-value">${hoaDon.value.nhanVien.ten}</div>
          </div>
        </div>
        
        <table class="items-table">
          <thead>
            <tr>
              <th>STT</th>
              <th>Sản phẩm</th>
              <th>Thông số</th>
              <th>Số lượng</th>
              <th>Đơn giá</th>
              <th>Thành tiền</th>
            </tr>
          </thead>
          <tbody>
            ${hoaDon.value.chiTietList.map((item, index) => `
              <tr>
                <td>${index + 1}</td>
                <td>${item.tenSanPham}</td>
                <td>${item.cauHinh || '15.6 inch'}</td>
                <td>${item.soLuong}</td>
                <td>${formatCurrency(item.giaBan)}</td>
                <td>${formatCurrency(item.thanhTien)}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
        
        <div class="total-section">
          <div class="total-row">Tổng tiền hàng: ${formatCurrency(hoaDon.value.tongTienHang)}</div>
          ${hoaDon.value.giamGiaHoaDon ? `<div class="total-row">Giảm giá: -${formatCurrency(hoaDon.value.giamGiaHoaDon)}</div>` : ''}
          <div class="total-row grand-total">Thành tiền: ${formatCurrency(hoaDon.value.thanhTien)}</div>
          <div class="total-row">Phương thức thanh toán: ${getPaymentMethodText(hoaDon.value.phuongThucThanhToan)}</div>
        </div>
        
        <div class="thank-you">
          <p>Cảm ơn quý khách đã mua hàng tại TechZone!</p>
          <p>Hẹn gặp lại.</p>
        </div>
      </body>
      </html>
    `
    
    const printWindow = window.open('', '_blank')
    if (printWindow) {
      printWindow.document.write(printContent)
      printWindow.document.close()
      printWindow.print()
    }
  }, 1000)
}

const handleBack = () => {
  router.push('/orders/list')
}

const handleComplete = () => {
  if (hoaDon.value.trangThaiHoaDon === '3') {
    message.success('Đơn hàng đã hoàn thành')
    return
  }
  
  isLoading.value = true
  setTimeout(() => {
    hoaDon.value.trangThaiHoaDon = '3'
    hoaDon.value.ngayCapNhat = new Date().toISOString()
    message.success('Đã cập nhật trạng thái đơn hàng thành hoàn thành')
    isLoading.value = false
  }, 1000)
}

const openCancelModal = () => {
  if (hoaDon.value.trangThaiHoaDon === '5') {
    message.warning('Đơn hàng đã bị hủy')
    return
  }
  
  window.$dialog?.warning({
    title: 'Xác nhận hủy đơn hàng',
    content: 'Bạn có chắc chắn muốn hủy đơn hàng này?',
    positiveText: 'Xác nhận hủy',
    negativeText: 'Hủy bỏ',
    onPositiveClick: () => {
      isLoading.value = true
      setTimeout(() => {
        hoaDon.value.trangThaiHoaDon = '5'
        hoaDon.value.ngayCapNhat = new Date().toISOString()
        message.error('Đơn hàng đã được hủy')
        isLoading.value = false
      }, 1000)
    }
  })
}

// Table configuration
const productColumns: DataTableColumns = [
  { 
    title: "STT", 
    key: "stt", 
    width: 60, 
    align: "center",
    render: (_, index) => h('span', { class: 'font-medium' }, index + 1)
  },
  { 
    title: "Ảnh", 
    key: "image", 
    width: 80,
    render: (row) => h(NAvatar, {
      src: row.anhSanPham,
      size: 'medium',
      class: 'border'
    })
  },
  { 
    title: "Sản phẩm", 
    key: "productInfo", 
    width: 300,
    render: (row) => h('div', { class: 'space-y-1' }, [
      h('div', { class: 'font-semibold text-gray-800' }, row.tenSanPham),
      h('div', { class: 'text-sm text-gray-600' }, [
        h(NTag, { 
          size: 'small', 
          type: 'info',
          class: 'mr-1'
        }, { default: () => row.thuongHieu }),
        h(NTag, { 
          size: 'small', 
          type: 'default',
          class: 'mr-1'
        }, { default: () => row.mauSac })
      ]),
      h('div', { class: 'text-xs text-gray-500 mt-1' }, row.cauHinh || '15.6 inch')
    ])
  },
  { 
    title: "Đơn giá", 
    key: "price", 
    width: 130, 
    align: "right",
    render: (row) => h('div', { class: 'font-semibold' }, formatCurrency(row.giaBan))
  },
  { 
    title: "SL", 
    key: "quantity", 
    width: 80, 
    align: "center",
    render: (row) => h('div', { class: 'text-center font-medium' }, row.soLuong)
  },
  { 
    title: "Thành tiền", 
    key: "total", 
    width: 150, 
    align: "right",
    render: (row) => h('div', { class: 'font-bold text-red-600' }, formatCurrency(row.thanhTien))
  }
]

// Lifecycle
onMounted(() => {
  console.log("Invoice detail page loaded")
  
  // Lấy ID từ URL
  const invoiceId = route.params.id as string
  console.log("Invoice ID from URL:", invoiceId)
  
  // Tạo dữ liệu fake
  hoaDon.value = createFakeLaptopData(invoiceId || 'HD001')
  
  console.log("Fake invoice data loaded:", hoaDon.value)
})
</script>

<style scoped>
.breadcrumb-section {
  margin-bottom: 20px;
}

.timeline-vertical {
  padding: 20px 0;
}

.timeline-steps {
  position: relative;
  max-width: 800px;
  margin: 0 auto;
}

.timeline-step {
  display: flex;
  margin-bottom: 30px;
  position: relative;
}

.step-icon {
  margin-right: 15px;
}

.icon-circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid;
  background: white;
  z-index: 2;
}

.icon-circle.completed {
  border-color: #10b981;
  background: #10b981;
  color: white;
}

.icon-circle.current {
  border-color: #3b82f6;
  background: #3b82f6;
  color: white;
}

.icon-circle.pending {
  border-color: #e5e7eb;
  background: white;
  color: #9ca3af;
}

.icon-circle.cancelled {
  border-color: #ef4444;
  background: #ef4444;
  color: white;
}

.step-line {
  position: absolute;
  top: 50px;
  left: 24px;
  width: 2px;
  height: calc(100% + 30px);
  background: #e5e7eb;
  z-index: 1;
}

.timeline-step:last-child .step-line {
  display: none;
}

.timeline-step.completed .step-line {
  background: #10b981;
}

.step-content {
  flex: 1;
  padding-top: 8px;
}

.step-title {
  font-weight: 600;
  font-size: 16px;
  margin-bottom: 4px;
}

.step-note {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 2px;
}

.step-time {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 2px;
}

.step-person {
  font-size: 12px;
  color: #6b7280;
  font-style: italic;
}

@media (max-width: 768px) {
  .timeline-step {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .step-icon {
    margin-right: 0;
    margin-bottom: 10px;
  }
  
  .step-line {
    top: 50px;
    left: 24px;
    width: 2px;
    height: calc(100% + 30px);
  }
}
</style>