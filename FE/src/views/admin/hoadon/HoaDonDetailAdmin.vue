<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <!-- Breadcrumb -->
    <div class="mb-6">
      <BreadcrumbDefault 
        :pageTitle="'Chi tiết hóa đơn'" 
        :routes="[
          { path: '/admin/hoa-don', name: 'Quản lý hóa đơn' },
          { path: '', name: `Hóa đơn #${invoiceCode}` },
        ]" 
      />
    </div>

    <!-- Header -->
    <div class="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 mb-6">
      <div>
        <h1 class="text-2xl lg:text-3xl font-bold text-gray-900">Hóa đơn #{{ invoiceCode }}</h1>
        <div class="flex flex-wrap items-center gap-2 mt-2">
          <NTag :type="getStatusTagType(hoaDonData?.trangThaiHoaDon)" size="medium" round>
            <template #icon>
              <n-icon :component="getStatusIcon(hoaDonData?.trangThaiHoaDon)" />
            </template>
            {{ getStatusText(hoaDonData?.trangThaiHoaDon) }}
          </NTag>
          
          <NTag type="info" size="small" round>
            {{ getInvoiceTypeText(hoaDonData?.loaiHoaDon) }}
          </NTag>
          
          <NTag type="default" size="small">
            {{ formatDateTime(hoaDonData?.ngayTao) }}
          </NTag>
        </div>
      </div>
      
      <div class="flex flex-wrap gap-2">
        <NButton type="primary" @click="handlePrintPDF" :loading="printLoading">
          <template #icon>
            <n-icon><PrintOutline /></n-icon>
          </template>
          In hóa đơn
        </NButton>
        
        <NButton type="default" @click="handleBack">
          <template #icon>
            <n-icon><ArrowBackOutline /></n-icon>
          </template>
          Quay lại
        </NButton>
      </div>
    </div>

    <!-- Progress Timeline -->
    <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-900">Tiến trình đơn hàng</h3>
          <span class="text-sm text-gray-500">Cập nhật: {{ formatDateTime(hoaDonData?.ngayTao) }}</span>
        </div>
      </template>

      <div class="relative">
        <!-- Progress bar -->
        <div class="absolute top-5 left-0 right-0 h-1.5 bg-gray-200 rounded-full z-0"></div>
        <div class="absolute top-5 left-0 h-1.5 bg-blue-500 rounded-full z-10" 
             :style="{ width: progressWidth }"></div>
        
        <!-- Steps -->
        <div class="relative flex justify-between z-20">
          <div v-for="(step, index) in timelineSteps" 
               :key="step.key"
               class="flex flex-col items-center w-1/5"
               :class="{ 'cursor-pointer': isStepSelectable(step.key) }"
               @click="handleStepClick(step.key)">
            
            <!-- Step circle -->
            <div class="w-10 h-10 rounded-full border-4 bg-white flex items-center justify-center mb-3 relative transition-all duration-300 hover:scale-110"
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
              <p class="text-xs text-gray-500">
                {{ getStepTime(step.key) || 'Đang chờ' }}
              </p>
            </div>
          </div>
        </div>
      </div>
      
      <!-- Current status info -->
      <div class="mt-8 pt-6 border-t border-gray-100">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-blue-50 flex items-center justify-center">
              <n-icon size="24" color="#3b82f6">
                <component :is="getCurrentStepIcon()" />
              </n-icon>
            </div>
            <div>
              <p class="text-sm text-gray-500">Trạng thái hiện tại</p>
              <p class="font-semibold" :class="getCurrentStatusTextClass()">
                {{ getStatusText(hoaDonData?.trangThaiHoaDon) }}
              </p>
            </div>
          </div>
          
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-green-50 flex items-center justify-center">
              <n-icon size="24" color="#10b981">
                <CashOutline />
              </n-icon>
            </div>
            <div>
              <p class="text-sm text-gray-500">Tổng tiền</p>
              <p class="font-semibold text-red-600">
                {{ formatCurrency(hoaDonData?.thanhTien) }}
              </p>
            </div>
          </div>
          
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-purple-50 flex items-center justify-center">
              <n-icon size="24" color="#8b5cf6">
                <CubeOutline />
              </n-icon>
            </div>
            <div>
              <p class="text-sm text-gray-500">Số lượng sản phẩm</p>
              <p class="font-semibold text-gray-900">
                {{ invoiceItems?.length || 0 }} sản phẩm
              </p>
            </div>
          </div>
        </div>
      </div>
    </NCard>

    <!-- Main Information Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Customer Information -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563"><PersonOutline /></n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Thông tin khách hàng</h3>
          </div>
        </template>
        
        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-14 h-14 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <n-icon size="24" color="#3b82f6"><PersonCircleOutline /></n-icon>
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-semibold text-gray-900 truncate">
                {{ hoaDonData?.tenKhachHang2 || 'Khách lẻ' }}
              </h4>
              <div class="flex flex-wrap gap-2 mt-2">
                <span class="inline-flex items-center gap-1 text-sm text-gray-600">
                  <n-icon size="14"><CallOutline /></n-icon>
                  {{ hoaDonData?.sdtKH2 || 'Chưa cập nhật' }}
                </span>
                <span class="inline-flex items-center gap-1 text-sm text-gray-600">
                  <n-icon size="14"><MailOutline /></n-icon>
                  {{ hoaDonData?.email2 || 'Chưa có email' }}
                </span>
              </div>
            </div>
          </div>
          
          <div class="space-y-3 pt-4 border-t border-gray-100">
            <div>
              <p class="text-sm text-gray-600 mb-1">Địa chỉ:</p>
              <p class="text-sm font-medium text-gray-900 bg-gray-50 p-3 rounded">
                {{ formatAddress(hoaDonData?.diaChi2) || 'Không có địa chỉ' }}
              </p>
            </div>
          </div>
        </div>
      </NCard>

      <!-- Order Summary -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563"><ReceiptOutline /></n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Tóm tắt đơn hàng</h3>
          </div>
        </template>
        
        <div class="space-y-3">
          <div class="grid grid-cols-2 gap-3">
            <div class="bg-gray-50 p-3 rounded-lg">
              <p class="text-xs text-gray-500">Tổng sản phẩm</p>
              <p class="text-lg font-bold text-gray-900">{{ invoiceItems?.length || 0 }}</p>
            </div>
            <div class="bg-gray-50 p-3 rounded-lg">
              <p class="text-xs text-gray-500">Tổng số lượng</p>
              <p class="text-lg font-bold text-gray-900">{{ totalQuantity }}</p>
            </div>
          </div>
          
          <div class="space-y-2 pt-4">
            <div class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Tổng tiền hàng:</span>
              <span class="font-semibold">{{ formatCurrency(totalAmount) }}</span>
            </div>
            
            <div v-if="hoaDonData?.phiVanChuyen && hoaDonData.phiVanChuyen > 0" 
                 class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Phí vận chuyển:</span>
              <span class="font-semibold">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>
            
            <div v-if="hoaDonData?.giaTriVoucher && hoaDonData.giaTriVoucher > 0" 
                 class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Giảm giá voucher:</span>
              <span class="font-semibold text-green-600">-{{ formatCurrency(hoaDonData.giaTriVoucher) }}</span>
            </div>
            
            <div class="flex justify-between items-center pt-4">
              <span class="text-lg font-bold text-gray-900">TỔNG CỘNG:</span>
              <span class="text-xl font-bold text-red-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>
          </div>
          
          <!-- Voucher Information -->
          <div v-if="hoaDonData?.maVoucher" class="mt-4 p-3 bg-green-50 rounded-lg border border-green-100">
            <div class="flex items-center gap-2 mb-1">
              <n-icon size="16" color="#10b981"><PricetagOutline /></n-icon>
              <span class="text-sm font-medium text-green-800">Voucher áp dụng</span>
            </div>
            <p class="text-green-700 font-medium">{{ hoaDonData.maVoucher }} - {{ hoaDonData.tenVoucher }}</p>
            <p class="text-xs text-green-600 mt-1">Giảm: {{ formatCurrency(hoaDonData.giaTriVoucher) }}</p>
          </div>
        </div>
      </NCard>

      <!-- Payment Information -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563"><CardOutline /></n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Thông tin thanh toán</h3>
          </div>
        </template>
        
        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
                <n-icon size="20" color="#3b82f6">
                  <CashOutline />
                </n-icon>
              </div>
              <div>
                <p class="font-medium">Tiền mặt</p>
                <p class="text-xs text-gray-500">Phương thức</p>
              </div>
            </div>
            <NTag type="success" size="small">
              Đã thanh toán
            </NTag>
          </div>
          
          <div class="space-y-3 pt-4 border-t border-gray-100">
            <div class="flex justify-between items-center">
              <span class="text-gray-600">Tổng tiền thanh toán:</span>
              <span class="font-bold text-green-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>
            
            <div v-if="hoaDonData?.duNo && hoaDonData.duNo > 0" class="flex justify-between items-center">
              <span class="text-gray-600">Còn nợ:</span>
              <span class="font-bold text-orange-600">{{ formatCurrency(hoaDonData.duNo) }}</span>
            </div>
            
            <div v-if="hoaDonData?.hoanPhi && hoaDonData.hoanPhi > 0" class="flex justify-between items-center">
              <span class="text-gray-600">Hoàn phí:</span>
              <span class="font-bold text-blue-600">{{ formatCurrency(hoaDonData.hoanPhi) }}</span>
            </div>
            
            <div class="pt-4">
              <p class="text-sm text-gray-600 mb-2">Thời gian thanh toán:</p>
              <div class="flex items-center gap-2 text-sm">
                <n-icon size="16" color="#6b7280"><TimeOutline /></n-icon>
                <span>{{ formatDateTime(hoaDonData?.ngayTao) }}</span>
              </div>
            </div>
          </div>
        </div>
      </NCard>
    </div>

    <!-- Products Table -->
    <NCard class="shadow-sm border-0 rounded-xl overflow-hidden">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563"><CubeOutline /></n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Danh sách sản phẩm</h3>
          </div>
          <span class="text-sm text-gray-500">{{ invoiceItems?.length || 0 }} sản phẩm</span>
        </div>
      </template>
      
      <div class="overflow-x-auto">
        <n-data-table
          :columns="productColumns"
          :data="invoiceItems"
          :pagination="false"
          striped
          class="min-w-full"
        />
      </div>
      
      <!-- Summary -->
      <div class="border-t border-gray-200">
        <div class="p-6">
          <div class="max-w-md ml-auto space-y-3">
            <div class="flex justify-between items-center">
              <span class="text-gray-600">Tạm tính:</span>
              <span class="font-medium">{{ formatCurrency(totalAmount) }}</span>
            </div>
            
            <div v-if="hoaDonData?.phiVanChuyen && hoaDonData.phiVanChuyen > 0" 
                 class="flex justify-between items-center">
              <span class="text-gray-600">Phí vận chuyển:</span>
              <span class="font-medium">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>
            
            <div v-if="hoaDonData?.giaTriVoucher && hoaDonData.giaTriVoucher > 0" 
                 class="flex justify-between items-center">
              <span class="text-gray-600">Giảm giá voucher:</span>
              <span class="font-medium text-green-600">-{{ formatCurrency(hoaDonData.giaTriVoucher) }}</span>
            </div>
            
            <div class="flex justify-between items-center pt-3 border-t border-gray-200">
              <span class="text-lg font-bold text-gray-900">Tổng cộng:</span>
              <span class="text-xl font-bold text-red-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>
          </div>
        </div>
      </div>
    </NCard>

    <!-- Notes & Actions -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- System Information -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563"><InformationCircleOutline /></n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Thông tin hệ thống</h3>
          </div>
        </template>
        
        <div class="space-y-3 text-sm">
          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Mã hóa đơn:</span>
            <span class="font-medium font-mono">{{ hoaDonData?.maHoaDon }}</span>
          </div>
          
          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Loại hóa đơn:</span>
            <NTag size="small" :type="getInvoiceTypeTagType(hoaDonData?.loaiHoaDon)">
              {{ getInvoiceTypeText(hoaDonData?.loaiHoaDon) }}
            </NTag>
          </div>
          
          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Ngày tạo:</span>
            <span class="font-medium">{{ formatDateTime(hoaDonData?.ngayTao) }}</span>
          </div>
          
          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Trạng thái:</span>
            <NTag :type="getStatusTagType(hoaDonData?.trangThaiHoaDon)" size="small">
              {{ getStatusText(hoaDonData?.trangThaiHoaDon) }}
            </NTag>
          </div>
          
          <div class="flex justify-between items-center py-1">
            <span class="text-gray-600">Phí vận chuyển:</span>
            <span class="font-medium">{{ formatCurrency(hoaDonData?.phiVanChuyen || 0) }}</span>
          </div>
          
          <div v-if="hoaDonData?.thoiGian" class="flex justify-between items-center py-1">
            <span class="text-gray-600">Thời gian xử lý:</span>
            <span class="font-medium">{{ hoaDonData.thoiGian }}</span>
          </div>
        </div>
      </NCard>

      <!-- Actions -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <n-icon size="20" color="#4b5563"><SettingsOutline /></n-icon>
            <h3 class="text-lg font-semibold text-gray-900">Hành động</h3>
          </div>
        </template>
        
        <div class="space-y-3">
          <NButton type="success" block @click="handleComplete" :disabled="isCompleted" :loading="isLoading">
            <template #icon>
              <n-icon><CheckmarkDoneOutline /></n-icon>
            </template>
            Hoàn thành đơn hàng
          </NButton>
          
          <NButton type="warning" block ghost @click="handleUpdateStatus">
            <template #icon>
              <n-icon><RefreshOutline /></n-icon>
            </template>
            Cập nhật trạng thái
          </NButton>
          
          <NButton type="error" block ghost @click="openCancelModal" :disabled="isCancelled">
            <template #icon>
              <n-icon><CloseCircleOutline /></n-icon>
            </template>
            Hủy đơn hàng
          </NButton>
          
          <NButton type="default" block @click="handleEditInvoice">
            <template #icon>
              <n-icon><CreateOutline /></n-icon>
            </template>
            Chỉnh sửa hóa đơn
          </NButton>
        </div>
      </NCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, h } from 'vue'
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
import { getHoaDonChiTiets } from '@/service/api/admin/hoadon.api'
import type { HoaDonResponse, ParamsGetHoaDonCT } from '@/service/api/admin/hoadon.api'

// Icons từ Naive UI/Ionicons
import {
  TimeOutline,
  CheckmarkCircleOutline,
  CartOutline,
  CheckmarkDoneOutline,
  CloseCircleOutline,
  CashOutline,
  CardOutline,
  PersonOutline,
  PersonCircleOutline,
  CallOutline,
  MailOutline,
  ReceiptOutline,
  CubeOutline,
  PricetagOutline,
  SettingsOutline,
  PrintOutline,
  ArrowBackOutline,
  CreateOutline,
  RefreshOutline,
  InformationCircleOutline
} from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()
const message = useMessage()

// State
const invoiceItems = ref<HoaDonResponse[]>([])
const printLoading = ref(false)
const isLoading = ref(false)

// Computed values
const invoiceCode = computed(() => route.params.id as string || 'N/A')

// Lấy dữ liệu tổng hợp từ danh sách sản phẩm
const hoaDonData = computed(() => {
  if (!invoiceItems.value || invoiceItems.value.length === 0) return null
  // Lấy thông tin từ item đầu tiên (tất cả đều giống nhau về thông tin hóa đơn)
  return invoiceItems.value[0]
})

const totalQuantity = computed(() => {
  return invoiceItems.value?.reduce((sum, item) => sum + (item.soLuong || 1), 0) || 0
})

const totalAmount = computed(() => {
  return invoiceItems.value?.reduce((sum, item) => sum + (item.tongTien || 0), 0) || 0
})

const progressWidth = computed(() => {
  const currentStep = parseInt(hoaDonData.value?.trangThaiHoaDon || '0')
  return `${(currentStep / 4) * 100}%` // 4 là số bước tối đa
})

const isCompleted = computed(() => hoaDonData.value?.trangThaiHoaDon === '4')
const isCancelled = computed(() => hoaDonData.value?.trangThaiHoaDon === '5')

// Timeline steps
const timelineSteps = [
  { key: '0', title: 'Chờ xác nhận', icon: TimeOutline, color: 'yellow' },
  { key: '1', title: 'Đã xác nhận', icon: CheckmarkCircleOutline, color: 'blue' },
  { key: '2', title: 'Đang xử lý', icon: CartOutline, color: 'purple' },
  { key: '3', title: 'Đang giao', icon: CheckmarkCircleOutline, color: 'info' },
  { key: '4', title: 'Hoàn thành', icon: CheckmarkDoneOutline, color: 'green' },
  { key: '5', title: 'Đã hủy', icon: CloseCircleOutline, color: 'red' }
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

const formatAddress = (address: string | undefined): string => {
  if (!address) return ''
  // Format lại địa chỉ từ format API
  return address.replace(/,/g, ', ').replace(/_/g, ' ')
}

const getStatusText = (status: string | undefined): string => {
  const statusMap: Record<string, string> = {
    '0': 'Chờ xác nhận',
    '1': 'Đã xác nhận',
    '2': 'Đang xử lý',
    '3': 'Đang giao',
    '4': 'Hoàn thành',
    '5': 'Đã hủy'
  }
  return statusMap[status || '0'] || 'Không xác định'
}

const getStatusIcon = (status: string | undefined): any => {
  const iconMap: Record<string, any> = {
    '0': TimeOutline,
    '1': CheckmarkCircleOutline,
    '2': CartOutline,
    '3': CheckmarkCircleOutline,
    '4': CheckmarkDoneOutline,
    '5': CloseCircleOutline
  }
  return iconMap[status || '0'] || TimeOutline
}

const getStatusTagType = (status: string | undefined): string => {
  const typeMap: Record<string, string> = {
    '0': 'warning',
    '1': 'info',
    '2': 'default',
    '3': 'info',
    '4': 'success',
    '5': 'error'
  }
  return typeMap[status || '0'] || 'default'
}

const getInvoiceTypeText = (type: string | undefined): string => {
  const typeMap: Record<string, string> = {
    '0': 'Tại quầy',
    '1': 'Giao hàng',
    '2': 'Online'
  }
  return typeMap[type || '0'] || 'Không xác định'
}

const getInvoiceTypeTagType = (type: string | undefined): string => {
  const typeMap: Record<string, string> = {
    '0': 'success',
    '1': 'primary',
    '2': 'info'
  }
  return typeMap[type || '0'] || 'default'
}

const getCurrentStatusTextClass = (): string => {
  const status = hoaDonData.value?.trangThaiHoaDon || '0'
  const classMap: Record<string, string> = {
    '0': 'text-yellow-600',
    '1': 'text-blue-600',
    '2': 'text-purple-600',
    '3': 'text-blue-600',
    '4': 'text-green-600',
    '5': 'text-red-600'
  }
  return classMap[status] || 'text-gray-600'
}

const getCurrentStepIcon = (): any => {
  return getStatusIcon(hoaDonData.value?.trangThaiHoaDon)
}

// Timeline functions
const isStepCompleted = (stepKey: string): boolean => {
  const currentStep = parseInt(hoaDonData.value?.trangThaiHoaDon || '0')
  const stepIndex = parseInt(stepKey)
  return stepIndex < currentStep && stepKey !== '5'
}

const isStepCurrent = (stepKey: string): boolean => {
  return hoaDonData.value?.trangThaiHoaDon === stepKey
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
  if (stepKey === '0') return formatDateTime(hoaDonData.value?.ngayTao)
  return null
}

const isStepSelectable = (stepKey: string): boolean => {
  if (isCancelled.value) return false
  if (isCompleted.value) return false
  return false
}

// Actions
const handleStepClick = (stepKey: string): void => {
  if (!isStepSelectable(stepKey)) {
    return
  }
  message.info('Chức năng cập nhật trạng thái đang phát triển')
}

const handlePrintPDF = (): void => {
  printLoading.value = true
  setTimeout(() => {
    message.success("Đang tạo file PDF...")
    printLoading.value = false
    window.print()
  }, 1500)
}

const handleBack = (): void => {
  router.push('/admin/hoa-don')
}

const handleComplete = (): void => {
  if (isCompleted.value) {
    message.warning('Đơn hàng đã hoàn thành')
    return
  }
  message.info('Chức năng cập nhật trạng thái đang phát triển')
}

const handleUpdateStatus = (): void => {
  message.info('Chức năng cập nhật trạng thái đang phát triển')
}

const handleEditInvoice = (): void => {
  message.info("Tính năng chỉnh sửa hóa đơn đang được phát triển")
}

const openCancelModal = (): void => {
  if (isCancelled.value) {
    message.warning('Đơn hàng đã bị hủy')
    return
  }
  
  window.$dialog?.error({
    title: 'Xác nhận hủy đơn hàng',
    content: 'Bạn có chắc chắn muốn hủy đơn hàng này? Hành động này không thể hoàn tác.',
    positiveText: 'Xác nhận hủy',
    negativeText: 'Hủy bỏ',
    positiveButtonProps: {
      type: 'error',
      ghost: false
    },
    onPositiveClick: () => {
      message.info('Chức năng hủy đơn hàng đang phát triển')
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
    title: "Sản phẩm", 
    key: "productInfo", 
    width: 300,
    render: (row) => h('div', { class: 'flex items-center gap-3' }, [
      h(NAvatar, {
        src: row.anhSanPham,
        size: 'medium',
        round: false,
        class: 'border',
        fallbackSrc: 'https://via.placeholder.com/60?text=No+Image'
      }),
      h('div', { class: 'flex-1 min-w-0' }, [
        h('div', { class: 'font-semibold text-gray-900 truncate' }, row.tenSanPham || 'Không có tên'),
        h('div', { class: 'flex flex-wrap gap-1 mt-1' }, [
          h(NTag, { 
            size: 'tiny', 
            type: 'info',
            bordered: false 
          }, { default: () => row.thuongHieu || 'No brand' }),
          h(NTag, { 
            size: 'tiny', 
            type: 'default',
            bordered: false 
          }, { default: () => row.mauSac || 'No color' }),
        ]),
        h('div', { class: 'text-xs text-gray-500 mt-1 truncate' }, row.size || 'No spec')
      ])
    ])
  },
  { 
    title: "Đơn giá", 
    key: "price", 
    width: 120, 
    align: "right",
    render: (row) => h('div', { class: 'font-semibold' }, formatCurrency(row.giaBan))
  },
  { 
    title: "SL", 
    key: "quantity", 
    width: 80, 
    align: "center",
    render: (row) => h('div', { 
      class: 'inline-flex items-center justify-center w-8 h-8 bg-gray-100 rounded-full font-medium'
    }, row.soLuong || 1)
  },
  { 
    title: "Thành tiền", 
    key: "total", 
    width: 140, 
    align: "right",
    render: (row) => h('div', { class: 'font-bold text-red-600' }, formatCurrency(row.tongTien || row.giaBan))
  }
]

// Fetch invoice details
const fetchInvoiceDetails = async (): Promise<void> => {
  try {
    isLoading.value = true
    
    const params: ParamsGetHoaDonCT = {
      maHoaDon: invoiceCode.value,
      page: 0,
      size: 100 // Lấy tất cả sản phẩm
    }
    
    const response = await getHoaDonChiTiets(params)
    
    if (response.success && response.data?.content) {
      invoiceItems.value = response.data.content
      message.success(`Đã tải ${invoiceItems.value.length} sản phẩm trong hóa đơn`)
    } else {
      message.error(response.message || 'Không thể tải chi tiết hóa đơn')
    }
  } catch (error: any) {
    console.error('Lỗi khi fetch chi tiết hóa đơn:', error)
    message.error(error.message || 'Đã xảy ra lỗi khi tải dữ liệu')
  } finally {
    isLoading.value = false
  }
}

// Lifecycle
onMounted(() => {
  console.log("Invoice detail page loaded")
  console.log("Invoice ID from URL:", invoiceCode.value)
  
  if (invoiceCode.value) {
    fetchInvoiceDetails()
  } else {
    message.error('Không tìm thấy mã hóa đơn')
    router.push('/admin/hoa-don')
  }
})
</script>

<style scoped>
/* Print styles */
@media print {
  .no-print {
    display: none !important;
  }
  
  body {
    background: white !important;
    color: black !important;
  }
  
  .container {
    max-width: 100% !important;
    padding: 0 !important;
    margin: 0 !important;
  }
  
  .shadow-sm, .rounded-xl, .border {
    box-shadow: none !important;
    border-radius: 0 !important;
    border: 1px solid #ddd !important;
  }
}

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

/* Animation for progress bar */
@keyframes progress {
  from {
    width: 0%;
  }
  to {
    width: v-bind(progressWidth);
  }
}

.bg-blue-500 {
  animation: progress 1s ease-out;
}
</style>