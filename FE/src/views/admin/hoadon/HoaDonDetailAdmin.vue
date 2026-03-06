<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {

  NAvatar,
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NDivider,
  NForm,
  NFormItem,
  NGi,
  NGrid,
  NIcon,
  NInput,
  NModal,
  NSelect,
  NSpace,
  NSpin,
  NTag,
  NText,
  NTooltip,

  useDialog,
  useMessage,
} from 'naive-ui'
import type { DataTableColumns, FormInst, FormRules, SelectOption } from 'naive-ui'
import QRCode from 'qrcode'

import type { ADChangeStatusRequest, HoaDonChiTietItem } from '@/service/api/admin/hoadon.api'
import {
  changeOrderStatus,
  getHoaDonChiTiets,
} from '@/service/api/admin/hoadon.api'
import {
  getImeiProductDetail,
  themSanPham,
} from '@/service/api/admin/banhang.api'
import type { ADPDImeiResponse } from '@/service/api/admin/product/productDetail.api'
import { localStorageAction } from '@/utils/storage'
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'

// Icons
import {
  ArrowBackOutline,
  ArrowForwardOutline,
  CallOutline,
  CardOutline,
  CartOutline,
  CashOutline,
  CheckmarkCircleOutline,
  CheckmarkDoneOutline,
  CloseCircleOutline,
  CloseOutline,
  CreateOutline,
  CubeOutline,
  GlobeOutline,
  InformationCircleOutline,
  ListOutline,
  MailOutline,
  PersonCircleOutline,
  PersonOutline,
  PricetagOutline,
  PrintOutline,
  QrCodeOutline,
  ReceiptOutline,
  RefreshOutline,
  ScanOutline,
  TimeOutline,
} from '@vicons/ionicons5'

// ==================== Types & Interfaces ====================
interface CustomerForm {
  tenKhachHang: string
  sdtKH: string
  email: string
  diaChi: string
}

interface HoaDonData {
  tenKhachHang?: string
  sdtKH?: string
  email?: string
  diaChi?: string
  tenKhachHang2?: string
  sdtKH2?: string
  email2?: string
  diaChi2?: string
  tenNhanVien?: string
  phuongThucVanChuyen?: string
  loaiHoaDon?: string
  trangThaiHoaDon?: string
  maHoaDon?: string
  ngayTao?: number
  thoiGianCapNhatCuoi?: string
  tongTienSauGiam?: number
  phiVanChuyen?: number
  giaTriVoucher?: number
  tenVoucher?: string
  maVoucher?: string
  duNo?: number
  hoanPhi?: number
  phuongThucThanhToan?: string
  lichSuTrangThai?: string
  ghiChu?: string
}

interface DisplayProduct extends HoaDonChiTietItem {
  stt: number
  imeiId?: string
  imeiCode?: string | null
  imeiStatus?: string | null
  imeiStatusText?: string | null
  isImeiRow: boolean
  originalProductId?: number
  soLuongImei?: number
  giaBanImei?: number
  tongTienImei?: number
}

interface PrintProduct extends HoaDonChiTietItem {
  imeiCode?: string | null
  soLuongImei?: number
  giaBanImei?: number
  tongTienImei?: number
}

interface HistoryItem {
  trangThai: number
  thoiGian: string
  nhanVien: string
  ghiChu: string
}

interface IMEIItem {
  id: string
  code: string
  status: string
  statusText: string
  orderType?: string
  source?: string
  assignedAt?: number
}

// ==================== Component Setup ====================
const props = defineProps<{
  hoaDonData?: HoaDonData
}>()

const emit = defineEmits<{
  'update:customer': [customerData: CustomerForm]
}>()

// ==================== Constants ====================
const STATUS_MAP: Record<number, { label: string, type: string, icon: any }> = {
  0: { label: 'Chờ xác nhận', type: 'warning', icon: TimeOutline },
  1: { label: 'Đã xác nhận', type: 'info', icon: CheckmarkCircleOutline },
  2: { label: 'Chờ giao hàng', type: 'default', icon: CartOutline },
  3: { label: 'Đang giao hàng', type: 'info', icon: CheckmarkCircleOutline },
  4: { label: 'Hoàn thành', type: 'success', icon: CheckmarkDoneOutline },
  5: { label: 'Đã hủy', type: 'error', icon: CloseCircleOutline },
}

const INVOICE_TYPE_MAP: Record<string, { text: string, type: string }> = {
  0: { text: 'Tại quầy', type: 'success' },
  1: { text: 'Online', type: 'primary' },
  2: { text: 'Giao hàng', type: 'info' },
}

const PAYMENT_METHOD_MAP: Record<string, string> = {
  CASH: 'Tiền mặt',
  BANKING: 'Chuyển khoản',
  CREDIT_CARD: 'Thẻ tín dụng',
  MOMO: 'Ví MoMo',
  ZALOPAY: 'Ví ZaloPay',
}

const SHIPPING_METHOD_MAP: Record<string, string> = {
  STANDARD: 'Giao hàng tiêu chuẩn',
  EXPRESS: 'Giao hàng nhanh',
  ECONOMY: 'Giao hàng tiết kiệm',
}

const IMEI_STATUS_CONFIG: Record<string, { type: any, text: string }> = {
  AVAILABLE: { type: 'success', text: 'Khả dụng' },
  SOLD: { type: 'warning', text: 'Đã bán' },
  DEFECTIVE: { type: 'error', text: 'Lỗi' },
  RESERVED: { type: 'info', text: 'Đã đặt' },
}

const TIMELINE_STEPS = [
  { key: '0', title: 'Chờ xác nhận', icon: TimeOutline, color: 'yellow' },
  { key: '1', title: 'Đã xác nhận', icon: CheckmarkCircleOutline, color: 'blue' },
  { key: '2', title: 'Chờ giao hàng', icon: CartOutline, color: 'purple' },
  { key: '3', title: 'Đang giao hàng', icon: CheckmarkCircleOutline, color: 'info' },
  { key: '4', title: 'Hoàn thành', icon: CheckmarkDoneOutline, color: 'green' },
  { key: '5', title: 'Đã hủy', icon: CloseCircleOutline, color: 'red' },
]

const router = useRouter()
const route = useRoute()
const message = useMessage()
const dialog = useDialog()
const idNV = localStorageAction.get(USER_INFO_STORAGE_KEY)

// ==================== Helper Functions ====================
function parseIMEIList(imeiData: any): IMEIItem[] {
  if (!imeiData)
    return []

  try {
    // Nếu là mảng các chuỗi JSON
    if (Array.isArray(imeiData)) {
      return imeiData.map((item) => {
        if (typeof item === 'string') {
          try {
            const parsed = JSON.parse(item)
            return parsed
          }
          catch {
            return null
          }
        }
        return item
      }).filter(item => item !== null)
    }

    // Nếu là chuỗi JSON
    if (typeof imeiData === 'string') {
      const parsed = JSON.parse(imeiData)
      return Array.isArray(parsed) ? parsed : []
    }

    // Nếu đã là mảng object
    if (Array.isArray(imeiData)) {
      return imeiData
    }

    return []
  }
  catch {
    return []
  }
}

// ==================== Refs & State ====================
// Customer modal
const showCustomerModal = ref(false)
const isSavingCustomer = ref(false)
const customerFormRef = ref<FormInst | null>(null)
const customerForm = reactive<CustomerForm>({
  tenKhachHang: '',
  sdtKH: '',
  email: '',
  diaChi: '',
})

// Invoice data
const invoiceItems = ref<HoaDonChiTietItem[]>([])
const printLoading = ref(false)
const isLoading = ref(false)
const isUpdating = ref(false)
const qrCodeDataUrl = ref<string>('')

// Status modal
const showStatusModal = ref(false)
const selectedStatus = ref<number | null>(null)
const statusNote = ref('')

// Product IMEI modal
const showProductImeiModal = ref(false)
const selectedProductForModal = ref<DisplayProduct | null>(null)
const imeiInputValue = ref('')
const imeiSuggestions = ref<string[]>([])
const showImeiSuggestions = ref(false)

// Serial modal - Dành cho đơn online
const showSerialModal = ref(false)
const selectedProductItem = ref<HoaDonChiTietItem | null>(null)
const selectedSerials = ref<ADPDImeiResponse[]>([])
const selectedSerialIds = ref<string[]>([])
const loadingSerials = ref(false)
const isAddingSerials = ref(false)
const scanMode = ref(false)
const scannedSerial = ref('')
const scanInputRef = ref<HTMLInputElement | null>(null)

// Serial Info modal - Dành cho đơn tại quầy/giao hàng
const showSerialInfoModal = ref(false)
const selectedSerialInfoProduct = ref<HoaDonChiTietItem | null>(null)

// History modal
const showHistoryModal = ref(false)

// Serial Summary modal
const showSerialSummaryModal = ref(false)
const selectedSerialSummaryProduct = ref<HoaDonChiTietItem | null>(null)
const selectedSummaryImeiRow = ref<DisplayProduct | null>(null)

// ==================== Computed Properties ====================
const invoiceCode = computed(() => route.params.id as string || 'N/A')

const requiredQuantity = computed(() => selectedProductItem.value?.soLuong || 0)

const currentProductImeiList = computed(() => {
  if (!selectedProductForModal.value)
    return []
  const item = invoiceItems.value.find(i => i.id === selectedProductForModal.value!.id)
  return item ? parseIMEIList(item.danhSachImei) : []
})

const currentProductSerialInfo = computed(() => {
  if (!selectedSerialInfoProduct.value)
    return []
  return parseIMEIList(selectedSerialInfoProduct.value.danhSachImei)
})

const hoaDonData = computed(() => {
  return invoiceItems.value?.[0] || null
})

const isOnlineInvoice = computed(() => hoaDonData.value?.loaiHoaDon === '1')
const isCounterInvoice = computed(() => hoaDonData.value?.loaiHoaDon === '0')
const isDeliveryInvoice = computed(() => hoaDonData.value?.loaiHoaDon === '2')
const isCounterOrDelivery = computed(() => isCounterInvoice.value || isDeliveryInvoice.value)

const currentStatus = computed(() => Number(hoaDonData.value?.trangThaiHoaDon || '0'))
const isCompleted = computed(() => currentStatus.value === 4)
const isCancelled = computed(() => currentStatus.value === 5)

// Cho phép thay đổi IMEI: chỉ khi là đơn online VÀ đang ở trạng thái chờ xác nhận (0)
const canChangeImei = computed(() => isOnlineInvoice.value && currentStatus.value === 0)

const showCancelButton = computed(() => {
  if (isCompleted.value || isCancelled.value)
    return false
  return currentStatus.value === 0
})

const productCount = computed(() => {
  return invoiceItems.value.filter(item => item?.tenSanPham && item?.soLuong != null).length
})

const totalQuantity = computed(() => {
  return invoiceItems.value.reduce((sum, item) => {
    return sum + (item.soLuong || 0)
  }, 0)
})

const totalAmount = computed(() => {
  return invoiceItems.value.reduce((sum, item) => sum + (item.tongTien || 0), 0)
})

const imeiProductsCount = computed(() => {
  let totalImei = 0
  invoiceItems.value.forEach((item) => {
    totalImei += parseIMEIList(item.danhSachImei).length
  })
  return totalImei
})

// Transform dữ liệu cho hóa đơn tại quầy/giao hàng: mỗi IMEI thành 1 hàng
const imeiProducts = computed(() => {
  if (!invoiceItems.value || !Array.isArray(invoiceItems.value)) {
    return []
  }

  if (isOnlineInvoice.value) {
    return []
  }

  const result: any[] = []
  let stt = 1

  invoiceItems.value.forEach((invoiceItem) => {
    const imeiList = parseIMEIList(invoiceItem.danhSachImei)

    if (imeiList.length > 0) {
      imeiList.forEach((imei) => {
        result.push({
          ...invoiceItem,
          imeiId: imei.id,
          imeiCode: imei.code || imei.imeiCode,
          imeiStatus: imei.status,
          imeiStatusText: imei.statusText,
          stt: stt++,
          soLuongImei: 1,
          giaBanImei: invoiceItem.giaBan ? invoiceItem.giaBan / imeiList.length : 0,
          tongTienImei: invoiceItem.tongTien ? invoiceItem.tongTien / imeiList.length : 0,
          isImeiRow: true,
        })
      })
    }
    else {
      result.push({
        ...invoiceItem,
        stt: stt++,
        imeiCode: null,
        imeiStatus: null,
        imeiStatusText: 'Không có IMEI',
        isImeiRow: false,
        giaBanImei: invoiceItem.giaBan,
        tongTienImei: invoiceItem.tongTien,
      })
    }
  })

  return result
})

// Dữ liệu hiển thị cho bảng sản phẩm
const displayProducts = computed<DisplayProduct[]>(() => {
  if (isCounterOrDelivery.value) {
    return imeiProducts.value
  }
  else {
    const result: DisplayProduct[] = []
    let stt = 1

    invoiceItems.value.forEach((item) => {
      const imeiList = parseIMEIList(item.danhSachImei)

      if (imeiList.length > 0) {
        imeiList.forEach((imei) => {
          result.push({
            ...item,
            stt: stt++,
            imeiId: imei.id,
            imeiCode: imei.code || imei.imeiCode,
            imeiStatus: imei.status,
            imeiStatusText: imei.statusText,
            soLuong: 1,
            tongTien: item.giaBan,
            isImeiRow: true,
            originalProductId: item.id,
          })
        })
      }
      else {
        result.push({
          ...item,
          stt: stt++,
          imeiCode: null,
          imeiStatus: null,
          imeiStatusText: null,
          isImeiRow: false,
        })
      }
    })

    return result
  }
})

const printProducts = computed<PrintProduct[]>(() => {
  const result: PrintProduct[] = []

  invoiceItems.value.forEach((item) => {
    const imeiList = parseIMEIList(item.danhSachImei)

    if (imeiList.length > 0) {
      imeiList.forEach((imei) => {
        result.push({
          ...item,
          imeiCode: imei.code || imei.imeiCode,
          soLuongImei: 1,
          giaBanImei: item.giaBan,
          tongTienImei: item.giaBan,
        })
      })
    }
    else {
      result.push({
        ...item,
        imeiCode: null,
        soLuongImei: item.soLuong,
        giaBanImei: item.giaBan,
        tongTienImei: item.tongTien,
      })
    }
  })

  return result
})

const progressWidth = computed(() => `${(currentStatus.value / 4) * 100}%`)

const availableStatusOptions = computed<SelectOption[]>(() => {
  if (isCompleted.value) {
    const options: SelectOption[] = []
    for (let i = 0; i <= 4; i++) {
      options.push({
        value: i,
        label: STATUS_MAP[i].label,
        disabled: true,
      })
    }
    return options
  }

  if (isCancelled.value) {
    return [{
      value: 5,
      label: STATUS_MAP[5].label,
      disabled: true,
    }]
  }

  const options: SelectOption[] = []
  for (let i = currentStatus.value; i <= 5; i++) {
    options.push({
      value: i,
      label: STATUS_MAP[i].label,
      disabled: i === currentStatus.value,
    })
  }
  return options
})

const filteredSteps = computed(() => {
  if (isCounterInvoice.value) {
    return TIMELINE_STEPS.filter(step => step.key === '4')
  }
  return TIMELINE_STEPS
})

const historyList = computed<HistoryItem[]>(() => {
  if (!hoaDonData.value?.lichSuTrangThai)
    return []
  try {
    const arr = JSON.parse(hoaDonData.value.lichSuTrangThai)
    return [...arr].reverse()
  }
  catch {
    return []
  }
})

// Serial Summary data
const serialSummaryData = computed(() => {
  if (!selectedSerialSummaryProduct.value)
    return { product: null, counterSerials: [], deliverySerials: [], onlineSerials: [] }

  const product = selectedSerialSummaryProduct.value
  const imeiList = parseIMEIList(product.danhSachImei)

  // Phân loại serial theo loại đơn hàng
  const counterSerials = imeiList.filter(imei =>
    imei.orderType === 'COUNTER' || imei.source === 'COUNTER',
  )

  const deliverySerials = imeiList.filter(imei =>
    imei.orderType === 'DELIVERY' || imei.source === 'DELIVERY',
  )

  const onlineSerials = imeiList.filter(imei =>
    imei.orderType === 'ONLINE' || imei.source === 'ONLINE',
  )

  return {
    product,
    counterSerials,
    deliverySerials,
    onlineSerials,
    totalSerials: imeiList.length,
  }
})

// ==================== Serial Columns ====================
const serialColumns: DataTableColumns<ADPDImeiResponse> = [
  {
    title: 'Chọn',
    key: 'select',
    width: 60,
    align: 'center',
    render: row => h(NCheckbox, {
      checked: selectedSerialIds.value.includes(row.id),
      disabled: row.imeiStatus !== 'AVAILABLE',
      onUpdateChecked: (checked) => {
        if (checked) {
          if (selectedSerialIds.value.length < requiredQuantity.value) {
            selectedSerialIds.value = [...selectedSerialIds.value, row.id]
          }
          else {
            message.warning(`Chỉ được chọn tối đa ${requiredQuantity.value} serial`)
          }
        }
        else {
          selectedSerialIds.value = selectedSerialIds.value.filter(id => id !== row.id)
        }
      },
    }),
  },
  {
    title: 'Serial',
    key: 'serialNumber',
    width: 180,
    render: row => h(NText, {
      strong: true,
      code: true,
      style: { fontFamily: 'monospace', fontSize: '12px' },
    }, () => row.code || '-'),
  },
  {
    title: 'Tên',
    key: 'name',
    width: 120,
    render: row => h(NText, () => row.name || '-'),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
    align: 'center',
    render: (row) => {
      const config = IMEI_STATUS_CONFIG[row.imeiStatus] || {
        type: 'default',
        text: row.imeiStatus || 'Không xác định',
      }
      return h(NTag, { type: config.type, size: 'small', round: true }, () => config.text)
    },
  },
]

// ==================== Product Columns ====================
const productColumns = computed<DataTableColumns<DisplayProduct>>(() => {
  const baseColumns: DataTableColumns<DisplayProduct> = [
    {
      title: 'STT',
      key: 'stt',
      width: 60,
      align: 'center',
      render: row => h('span', { class: 'font-medium' }, row.stt),
    },
    {
      title: 'Sản phẩm',
      key: 'productInfo',
      width: 350,
      render: (row) => {
        if (!row.tenSanPham)
          return h('div', { class: 'hidden' })
        const isMainRow = !row.isImeiRow

        return h('div', {
          class: `flex items-center gap-3`,
        }, [
          h(NAvatar, {
            src: row.anhSanPham,
            size: row.isImeiRow ? 'small' : 'medium',
            round: false,
            class: 'border',
            fallbackSrc: 'https://via.placeholder.com/40?text=No+Image',
          }),
          h('div', { class: 'flex-1 min-w-0' }, [
            h('div', {
              class: `font-semibold text-gray-900 truncate ${row.isImeiRow ? 'text-sm' : ''}`,
            }, row.tenSanPham),
            h('div', { class: 'flex flex-wrap gap-1 mt-1' }, [
              row.thuongHieu && h(NTag, { size: 'tiny', type: 'info', bordered: false }, { default: () => row.thuongHieu }),
              row.mauSac && h(NTag, { size: 'tiny', type: 'default', bordered: false }, { default: () => row.mauSac }),
            ]),
            h('div', { class: 'text-xs text-gray-500 mt-1 truncate' }, row.size || ''),
          ]),
        ])
      },
    },
    {
      title: 'Serial',
      key: 'imeiCode',
      width: 150,
      align: 'center',
      render: (row) => {
        if (!row.imeiCode) {
          return h('div', { class: 'text-center text-gray-400 italic text-sm' }, 'Chưa có Serial')
        }

        return h('div', {
          class: 'font-mono font-bold text-center text-purple-700 text-sm p-1 bg-purple-50 rounded border border-purple-200',
        }, row.imeiCode)
      },
    },
    {
      title: 'Đơn giá',
      key: 'price',
      width: 120,
      align: 'right',
      render: (row) => {
        const price = row.giaBan
        return h('div', { class: 'font-semibold' }, formatCurrency(price))
      },
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 120,
      align: 'center',
      render: (row) => {
        // Đơn online - hiển thị nút Chọn Serial
        if (isOnlineInvoice.value) {
          if (row.isImeiRow) {
            const parentId = row.originalProductId ?? row.id
            const parentItem = invoiceItems.value.find(i => i.id === parentId)
            if (!parentItem)
              return h('span', '')

            const imeiList = parseIMEIList(parentItem.danhSachImei)
            const firstImeiCode = imeiList[0]?.code || imeiList[0]?.imeiCode
            if (row.imeiCode !== firstImeiCode)
              return h('span', '')

            return h(NButton, {
              size: 'tiny',
              type: 'info',
              secondary: true,
              onClick: () => openSerialSelectionModal(parentItem),
            }, { default: () => 'Chọn Serial' })
          }

          return h(NButton, {
            size: 'tiny',
            type: 'info',
            secondary: true,
            onClick: () => openSerialSelectionModal(row),
          }, { default: () => 'Chọn Serial' })
        }

        // Đơn tại quầy/giao hàng - hiển thị nút Xem Serial
        if (row.isImeiRow) {
          const parentId = row.originalProductId ?? row.id
          const parentItem = invoiceItems.value.find(i => i.id === parentId)
          if (!parentItem)
            return h('span', '')

          const imeiList = parseIMEIList(parentItem.danhSachImei)
          const firstImeiCode = imeiList[0]?.code || imeiList[0]?.imeiCode
          if (row.imeiCode !== firstImeiCode)
            return h('span', '')

          return h(NButton, {
            size: 'tiny',
            type: 'success',
            secondary: true,
            onClick: () => openSerialInfoModal(parentItem),
          }, { default: () => 'Xem Serial' })
        }

        return h(NButton, {
          size: 'tiny',
          type: 'success',
          secondary: true,
          onClick: () => openSerialInfoModal(row),
        }, { default: () => 'Xem Serial' })
      },
    },
  ]

  return baseColumns
})

// Form validation rules
const customerFormRules: FormRules = {
  tenKhachHang: [{ required: true, message: 'Vui lòng nhập họ và tên', trigger: ['blur', 'input'] }],
  sdtKH: [
    { required: true, message: 'Vui lòng nhập số điện thoại', trigger: ['blur', 'input'] },
    { pattern: /(84|0[3|5789])+(\d{8})\b/, message: 'Số điện thoại không hợp lệ', trigger: ['blur', 'input'] },
  ],
  email: [{ type: 'email', message: 'Email không hợp lệ', trigger: ['blur', 'input'] }],
}

// ==================== Helper Functions ====================
function formatCurrency(value: number | undefined | null): string {
  if (value === undefined || value === null)
    return '0 ₫'
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value)
}

function formatDateTime(timestamp: number | undefined): string {
  if (!timestamp)
    return 'N/A'
  try {
    return new Date(timestamp).toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
  }
  catch {
    return 'N/A'
  }
}

function formatTime(timestamp: number | undefined): string {
  if (!timestamp)
    return 'N/A'
  try {
    return new Date(timestamp).toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    })
  }
  catch {
    return 'N/A'
  }
}

function hideImeiSuggestions() {
  setTimeout(() => { showImeiSuggestions.value = false }, 150)
}

function formatDateTimeFromString(dateString: string | undefined): string {
  if (!dateString)
    return 'N/A'
  try {
    return new Date(dateString).toLocaleString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    })
  }
  catch {
    return dateString
  }
}

function formatHistoryTime(timeStr: string | undefined): string {
  if (!timeStr)
    return '---'
  const cleanTime = timeStr.split('.')[0]
  const parts = cleanTime.split(' ')
  if (parts.length === 2) {
    const dateParts = parts[0].split('-')
    if (dateParts.length === 3) {
      return `${parts[1]} - ${dateParts[2]}-${dateParts[1]}-${dateParts[0]}`
    }
  }
  return cleanTime
}

const formatAddress = (address: string | undefined): string => address || ''

function getStatusText(status: string | number | undefined): string {
  const statusNum = Number(status)
  return STATUS_MAP[statusNum]?.label || 'Không xác định'
}

function getStatusTagType(status: string | number | undefined): string {
  const statusNum = Number(status)
  return STATUS_MAP[statusNum]?.type || 'default'
}

function getStatusIcon(status: string | number | undefined): any {
  const statusNum = Number(status)
  return STATUS_MAP[statusNum]?.icon || TimeOutline
}

function getCurrentStatusTextClass(): string {
  const classMap: Record<number, string> = {
    0: 'text-yellow-600',
    1: 'text-blue-600',
    2: 'text-purple-600',
    3: 'text-blue-600',
    4: 'text-green-600',
    5: 'text-red-600',
  }
  return classMap[currentStatus.value] || 'text-gray-600'
}

function getCurrentStepIcon(): any {
  return getStatusIcon(currentStatus.value)
}

function getInvoiceTypeText(type: string | undefined): string {
  return INVOICE_TYPE_MAP[type || '0']?.text || 'Không xác định'
}

function getInvoiceTypeTagType(type: string | undefined): string {
  return INVOICE_TYPE_MAP[type || '0']?.type || 'default'
}

function getPaymentMethodText(method: string | undefined): string {
  if (!method)
    return 'Tiền mặt'
  return PAYMENT_METHOD_MAP[method] || method
}

function getShippingMethodText(method: string | undefined): string {
  if (isCounterInvoice.value)
    return 'Nhận tại quầy'
  if (!method)
    return 'Giao hàng tiêu chuẩn'
  return SHIPPING_METHOD_MAP[method] || method
}

function getPaymentStatusText(): string {
  return hoaDonData.value?.duNo ? 'Còn nợ' : 'Đã thanh toán'
}

function getPaymentStatusTagType(): string {
  return hoaDonData.value?.duNo ? 'warning' : 'success'
}

function getStepCircleBg(status: string | number | undefined): string {
  const statusNum = Number(status)
  const map: Record<number, string> = {
    0: 'bg-yellow-100',
    1: 'bg-blue-100',
    2: 'bg-purple-100',
    3: 'bg-blue-100',
    4: 'bg-green-100',
    5: 'bg-red-100',
  }
  return map[statusNum] || 'bg-gray-100'
}

function getStepIconClass(stepKey: string): string {
  if (isStepCurrent(stepKey))
    return 'text-blue-500'
  if (isStepCompleted(stepKey))
    return 'text-green-500'
  return 'text-gray-400'
}

function getStepTextClass(stepKey: string): string {
  if (isStepCurrent(stepKey))
    return 'text-blue-600'
  if (isStepCompleted(stepKey))
    return 'text-green-600'
  return 'text-gray-500'
}

function getStepCircleClass(stepKey: string): string {
  if (isStepCurrent(stepKey))
    return 'border-blue-500 border-4'
  if (isStepCompleted(stepKey))
    return 'border-green-500 border-2'
  return 'border-gray-300 border-2'
}

function isStepCompleted(stepKey: string): boolean {
  const stepIndex = Number(stepKey)
  return stepIndex < currentStatus.value && stepKey !== '5'
}

function isStepCurrent(stepKey: string): boolean {
  return currentStatus.value.toString() === stepKey
}

function isStepSelectable(stepKey: string): boolean {
  if (isCancelled.value || isCompleted.value)
    return false
  return Number(stepKey) === currentStatus.value + 1
}

function getStepTime(stepKey: string): string | null {
  if (!hoaDonData.value?.lichSuTrangThai)
    return null

  try {
    const history = JSON.parse(hoaDonData.value.lichSuTrangThai)
    const item = history.find((h: any) => h.trangThai?.toString() === stepKey)

    if (item?.thoiGian) {
      const [datePart, timePart] = item.thoiGian.split(' ')
      if (!datePart || !timePart)
        return item.thoiGian

      const [year, month, day] = datePart.split('-')
      return `${day}/${month}/${year} ${timePart}`
    }
    return null
  }
  catch {
    return null
  }
}

function rowClassName(row: DisplayProduct) {
  return row.isImeiRow ? 'imei-row bg-blue-50 hover:bg-blue-100' : 'product-row hover:bg-gray-50'
}

// ==================== Modal Handlers ====================
// Mở modal chọn serial (cho đơn online)
async function openSerialSelectionModal(product: HoaDonChiTietItem) {
  if (!isOnlineInvoice.value)
    return

  const existingImeis = parseIMEIList(product.danhSachImei)
  if (existingImeis.length >= product.soLuong && !canChangeImei.value) {
    dialog.info({
      title: 'Thông báo',
      content: `Sản phẩm này đã có đủ ${existingImeis.length} serial.`,
      positiveText: 'Đóng',
    })
    return
  }

  selectedProductItem.value = product
  loadingSerials.value = true
  selectedSerials.value = []
  selectedSerialIds.value = []
  scanMode.value = false
  scannedSerial.value = ''

  try {
    if (product.productDetailId) {
      const response = await getImeiProductDetail(product.productDetailId)
      if (response.success && response.data) {
        const availableSerials = response.data.filter(s =>
          s.imeiStatus === 'AVAILABLE'
          || (s.imeiStatus === 'RESERVED' && existingImeis.some(imei => imei.id === s.id)),
        )

        selectedSerials.value = availableSerials

        existingImeis.forEach((imei) => {
          if (availableSerials.some(s => s.id === imei.id)) {
            selectedSerialIds.value.push(imei.id)
          }
        })

        showSerialModal.value = true
      }
      else {
        message.error(response.message || 'Không thể tải danh sách serial')
      }
    }
    else {
      message.error('Không tìm thấy thông tin sản phẩm chi tiết')
    }
  }
  catch (error) {
    console.error('Error fetching serials:', error)
    message.error('Đã xảy ra lỗi khi tải danh sách serial')
  }
  finally {
    loadingSerials.value = false
  }
}

// Mở modal xem thông tin serial (cho đơn tại quầy/giao hàng)
function openSerialInfoModal(product: HoaDonChiTietItem) {
  selectedSerialInfoProduct.value = product
  showSerialInfoModal.value = true
}

// Mở modal tổng hợp serial
function openSerialSummaryModal() {
  if (invoiceItems.value.length === 0) {
    message.warning('Không có sản phẩm nào')
    return
  }

  // Tạo sản phẩm tổng hợp từ tất cả sản phẩm
  const allImeis: any[] = []
  invoiceItems.value.forEach((item) => {
    const imeis = parseIMEIList(item.danhSachImei)
    allImeis.push(...imeis)
  })

  const combinedProduct: HoaDonChiTietItem = {
    ...invoiceItems.value[0],
    id: 'all-products',
    tenSanPham: '📊 TẤT CẢ SẢN PHẨM',
    soLuong: totalQuantity.value,
    danhSachImei: JSON.stringify(allImeis),
  }

  selectedSerialSummaryProduct.value = combinedProduct
  selectedSummaryImeiRow.value = null
  showSerialSummaryModal.value = true
}

// Xử lý quét serial
function startScanMode() {
  scanMode.value = true
  setTimeout(() => {
    scanInputRef.value?.focus()
  }, 100)
}

function handleScanInput() {
  if (scannedSerial.value) {
    // Tìm serial trong danh sách
    const matchedSerial = selectedSerials.value.find(s =>
      s.code?.toLowerCase() === scannedSerial.value.toLowerCase()
      && s.imeiStatus === 'AVAILABLE',
    )

    if (matchedSerial) {
      if (!selectedSerialIds.value.includes(matchedSerial.id)) {
        if (selectedSerialIds.value.length < requiredQuantity.value) {
          selectedSerialIds.value = [...selectedSerialIds.value, matchedSerial.id]
          message.success(`Đã chọn serial ${matchedSerial.code}`)
        }
        else {
          message.warning(`Đã chọn đủ ${requiredQuantity.value} serial`)
        }
      }
      else {
        message.warning('Serial này đã được chọn')
      }
    }
    else {
      message.error('Không tìm thấy serial khả dụng')
    }
    scannedSerial.value = ''
  }
}

// Thêm serial vào đơn hàng
async function addSerialsToInvoice() {
  if (!selectedProductItem.value || !hoaDonData.value || selectedSerialIds.value.length === 0) {
    message.error('Vui lòng chọn ít nhất một serial')
    return
  }

  const requiredQuantity = selectedProductItem.value.soLuong || 0
  if (selectedSerialIds.value.length !== requiredQuantity) {
    message.error(`Sản phẩm cần ${requiredQuantity} serial. Vui lòng chọn đủ số lượng!`)
    return
  }

  isAddingSerials.value = true
  try {
    const response = await themSanPham({
      invoiceId: selectedProductItem.value.invoiceId,
      productDetailId: selectedProductItem.value.productDetailId,
      imeiIds: selectedSerialIds.value,
      hoaDonChiTietId: selectedProductItem.value.id,
    })

    if (response.success) {
      message.success(`Đã thêm ${selectedSerialIds.value.length} serial vào sản phẩm`)
      showSerialModal.value = false
      await fetchInvoiceDetails()
    }
    else {
      message.error(response.message || 'Thêm serial thất bại')
    }
  }
  catch (error: any) {
    console.error('Error adding serials:', error)
    message.error(error.message || 'Đã xảy ra lỗi khi thêm serial')
  }
  finally {
    isAddingSerials.value = false
  }
}

function openProductImeiModal(row: DisplayProduct) {
  let targetItem: HoaDonChiTietItem | undefined

  if (row.isImeiRow) {
    const searchId = row.originalProductId ?? row.id
    targetItem = invoiceItems.value.find(i => i.id === searchId)
  }
  else {
    targetItem = invoiceItems.value.find(i => i.id === row.id)
  }

  if (!targetItem) {
    message.error('Không tìm thấy thông tin sản phẩm')
    return
  }

  selectedProductForModal.value = { ...targetItem, stt: row.stt, isImeiRow: false }
  imeiInputValue.value = ''
  imeiSuggestions.value = []
  showImeiSuggestions.value = false
  showProductImeiModal.value = true
}

function handleImeiInput(val: string) {
  imeiInputValue.value = val
  if (!val.trim()) {
    showImeiSuggestions.value = false
    imeiSuggestions.value = []
    return
  }
  const existing = currentProductImeiList.value.map(i => i.code || i.imeiCode).filter(Boolean)
  imeiSuggestions.value = existing.filter(code =>
    code?.toLowerCase().includes(val.toLowerCase()),
  )
  showImeiSuggestions.value = imeiSuggestions.value.length > 0
}

function selectImeiSuggestion(code: string) {
  imeiInputValue.value = code
  showImeiSuggestions.value = false
}

// ==================== API Functions ====================
async function fetchInvoiceDetails(): Promise<void> {
  try {
    isLoading.value = true
    const response = await getHoaDonChiTiets({
      maHoaDon: invoiceCode.value,
      page: 0,
      size: 100,
    })

    if (response.success && response.data?.content) {
      invoiceItems.value = response.data.content
      console.log('Invoice items loaded:', invoiceItems.value.length)
      console.log('Invoice type:', hoaDonData.value?.loaiHoaDon)

      invoiceItems.value.forEach((item, index) => {
        const imeiList = parseIMEIList(item.danhSachImei)
        console.log(`Product ${index + 1}: ${item.tenSanPham}, Quantity: ${item.soLuong}, IMEIs: ${imeiList.length}`)
        imeiList.forEach((imei) => {
          console.log(`  - IMEI: ${imei.code}, Status: ${imei.status}`)
        })
      })
    }
    else {
      message.error(response.message || 'Không thể tải chi tiết hóa đơn')
    }
  }
  catch (error: any) {
    console.error('Lỗi khi fetch chi tiết hóa đơn:', error)
    message.error(error.message || 'Đã xảy ra lỗi khi tải dữ liệu')
  }
  finally {
    isLoading.value = false
  }
}

async function generateQRCode(): Promise<void> {
  if (!hoaDonData.value)
    return

  try {
    const invoiceInfo = `
MÃ HĐ: ${hoaDonData.value.maHoaDon}
NGÀY: ${formatDateTime(hoaDonData.value.ngayTao)}
KH: ${hoaDonData.value.tenKhachHang2 || hoaDonData.value.tenKhachHang || 'Khách lẻ'}
SĐT: ${hoaDonData.value.sdtKH2 || hoaDonData.value.sdtKH || 'N/A'}
TỔNG: ${formatCurrency(hoaDonData.value.tongTienSauGiam)}
URL: ${window.location.origin}/admin/hoa-don/${hoaDonData.value.maHoaDon}
    `.trim()

    const canvas = document.createElement('canvas')
    await QRCode.toCanvas(canvas, invoiceInfo, {
      width: 250,
      margin: 2,
      errorCorrectionLevel: 'H',
    })

    qrCodeDataUrl.value = canvas.toDataURL('image/png')
  }
  catch (error) {
    console.error('Error generating QR code:', error)
  }
}

// ==================== Event Handlers ====================
// Customer handlers
function openCustomerEditModal() {
  if (hoaDonData.value) {
    Object.keys(customerForm).forEach((key) => {
      const formKey = key as keyof CustomerForm
      const dataKey = key as keyof HoaDonData
      customerForm[formKey] = hoaDonData.value?.[dataKey] || ''
    })
  }
  showCustomerModal.value = true
}

function saveCustomerInfo() {
  customerFormRef.value?.validate((errors) => {
    if (!errors) {
      isSavingCustomer.value = true
      setTimeout(() => {
        emit('update:customer', { ...customerForm })
        isSavingCustomer.value = false
        showCustomerModal.value = false
        message.success('Cập nhật thông tin khách hàng thành công')
      }, 500)
    }
  })
}

// Status handlers
function handleStepClick(stepKey: string): void {
  if (!isStepSelectable(stepKey))
    return
  selectedStatus.value = Number(stepKey)
  showStatusModal.value = true
}

async function confirmStatusUpdate(): Promise<void> {
  if (selectedStatus.value === null || !hoaDonData.value?.maHoaDon) {
    message.error('Vui lòng chọn trạng thái mới')
    return
  }

  isUpdating.value = true
  try {
    const response = await changeOrderStatus({
      maHoaDon: hoaDonData.value.maHoaDon,
      statusTrangThaiHoaDon: selectedStatus.value,
      note: statusNote.value || '',
      idNhanVien: idNV.userId,
    })

    if (response.success) {
      message.success('Cập nhật trạng thái thành công')
      await fetchInvoiceDetails()
      selectedStatus.value = null
      statusNote.value = ''
      showStatusModal.value = false
    }
    else {
      message.error(response.message || 'Cập nhật thất bại')
    }
  }
  catch (error: any) {
    console.error('Error updating status:', error)
    message.error(error.message || 'Đã xảy ra lỗi khi cập nhật')
  }
  finally {
    isUpdating.value = false
  }
}

function handleEditInvoice(): void {
  message.info('Tính năng chỉnh sửa hóa đơn đang được phát triển')
}

function openCancelModal(): void {
  if (isCancelled.value) {
    message.warning('Đơn hàng đã bị hủy')
    return
  }

  dialog.error({
    title: 'Xác nhận hủy đơn hàng',
    content: 'Bạn có chắc chắn muốn hủy đơn hàng này? Hành động này không thể hoàn tác.',
    positiveText: 'Xác nhận hủy',
    negativeText: 'Hủy bỏ',
    positiveButtonProps: { type: 'error' },
    onPositiveClick: () => {
      selectedStatus.value = 5
      showStatusModal.value = true
    },
  })
}

// Print handler
async function handlePrint() {
  if (!hoaDonData.value)
    return

  printLoading.value = true
  await generateQRCode()

  const invoiceContent = document.getElementById('invoice-content')
  if (!invoiceContent) {
    message.error('Không tìm thấy nội dung hóa đơn')
    printLoading.value = false
    return
  }

  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    message.error('Trình duyệt đã chặn cửa sổ popup. Vui lòng cho phép popup và thử lại.')
    printLoading.value = false
    return
  }

  const printStyles = `
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: Arial, sans-serif; background: white; color: #1a1a1a; padding: 24px; }
    table { width: 100%; border-collapse: collapse; }
    th { background-color: #16a34a !important; color: #fff !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    tr:nth-child(even) td { background-color: #f9fafb !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    @media print {
      body { padding: 0; }
      th { background-color: #16a34a !important; color: #fff !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
    }
  </style>
`

  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>HoaDon_${hoaDonData.value.maHoaDon || invoiceCode.value}</title>
      ${printStyles}
    </head>
    <body>
      ${invoiceContent.innerHTML}
      <script>
        window.onload = function() {
          window.print();
        }
      <\/script>
    </body>
    </html>
  `)

  printWindow.document.close()
  printLoading.value = false
}

// Navigation handlers
const handleBack = (): void => router.push('/orders/list')

// ==================== Watchers ====================
watch(() => hoaDonData.value, async () => {
  if (hoaDonData.value)
    await generateQRCode()
}, { deep: true })

watch(() => props.hoaDonData, (newData) => {
  if (newData) {
    Object.keys(customerForm).forEach((key) => {
      const formKey = key as keyof CustomerForm
      const dataKey = key as keyof HoaDonData
      customerForm[formKey] = newData[dataKey] || ''
    })
  }
}, { immediate: true })

// ==================== Lifecycle ====================
onMounted(async () => {
  console.log('Invoice detail page loaded')
  console.log('Invoice ID from URL:', invoiceCode.value)

  if (invoiceCode.value) {
    await fetchInvoiceDetails()
    await generateQRCode()
  }
  else {
    message.error('Không tìm thấy mã hóa đơn')
    router.push('/admin/hoa-don')
  }
})
</script>

<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <!-- Header -->
    <div class="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4 mb-6 no-print">
      <div>
        <h1 class="text-2xl lg:text-3xl font-bold text-gray-900">
          Hóa đơn #{{ invoiceCode }}
        </h1>
        <div class="flex flex-wrap items-center gap-2 mt-2">
          <NTag :type="getStatusTagType(currentStatus)" size="medium" round>
            <template #icon>
              <NIcon :component="getStatusIcon(currentStatus)" />
            </template>
            {{ getStatusText(currentStatus) }}
          </NTag>
          <NTag :type="getInvoiceTypeTagType(hoaDonData?.loaiHoaDon)" size="small" round>
            {{ getInvoiceTypeText(hoaDonData?.loaiHoaDon) }}
          </NTag>
          <NTag type="default" size="small">
            {{ formatDateTime(hoaDonData?.ngayTao) }}
          </NTag>
        </div>
      </div>

      <div class="flex flex-wrap gap-2">
        <NButton type="info" secondary @click="showHistoryModal = true">
          <template #icon>
            <NIcon><ListOutline /></NIcon>
          </template>
          Chi tiết
        </NButton>
        <NButton type="primary" :loading="printLoading" @click="handlePrint">
          <template #icon>
            <NIcon><PrintOutline /></NIcon>
          </template>
          In hóa đơn
        </NButton>
        <NButton type="default" @click="handleBack">
          <template #icon>
            <NIcon><ArrowBackOutline /></NIcon>
          </template>
          Quay lại
        </NButton>
      </div>
    </div>

    <!-- Invoice Content (Hidden) -->
 <div id="invoice-content" class="hidden">
      <div class="invoice-paper bg-white text-black p-8 md:p-12">
        <div class="flex justify-between items-start mb-10">
          <div>
            <div class="flex items-center gap-2 mb-2">
              <img src="@/assets/svg-icons/logo.svg" class="w-8 h-8" alt="logo">
              <span class="text-2xl font-bold tracking-widest text-gray-900">My Laptop</span>
            </div>
            <div class="text-xs text-gray-500 space-y-1">
              <p>123 Đại Cồ Việt, Hai Bà Trưng, Hà Nội</p>
              <p>Website: mylaptop.vn | Hotline: 1900.8888</p>
            </div>
          </div>
          <div class="text-right">
            <h2 class="text-xl font-bold uppercase text-gray-800">
              Hóa Đơn
            </h2>
            <p class="text-sm font-bold text-gray-600 mt-1">
              Mã: {{ hoaDonData?.maHoaDon || invoiceCode }}
            </p>
            <p class="text-sm text-gray-500">
              Ngày: {{ formatDateTime(hoaDonData?.ngayTao) }}
            </p>
          </div>
        </div>

        <NDivider class="border-dashed my-6" />

        <!-- Thông tin khách hàng và đơn hàng -->
        <div class="grid grid-cols-2 gap-8 mb-8 text-sm">
          <div>
            <h3 class="font-bold text-gray-400 uppercase text-[15px] mb-3 tracking-wider">
              Khách hàng
            </h3>
            <p class="font-bold text-gray-900 text-base mb-1">
              {{ hoaDonData?.tenKhachHang2 || hoaDonData?.tenKhachHang || 'Khách lẻ' }}
            </p>
            <p class="text-gray-600 mb-1">
              {{ hoaDonData?.sdtKH2 || hoaDonData?.sdtKH || 'Chưa cập nhật' }}
            </p>
            <p class="text-gray-600 leading-relaxed">
              {{ formatAddressCustomer(hoaDonData?.diaChi) || formatAddressCustomer(hoaDonData?.diaChi2) || 'Không có địa chỉ' }}
            </p>
          </div>

          <div class="text-right">
            <h3 class="font-bold text-gray-400 uppercase text-[15px] mb-3 tracking-wider">
              Thông tin đơn hàng
            </h3>
            <div class="space-y-1.5 text-gray-600">
              <p>Nhân viên: <span class="text-gray-900 font-medium">{{ hoaDonData?.tenNhanVien || 'Không xác định' }}</span></p>
              <p>Vận chuyển: <span class="text-gray-900 font-medium">{{ getShippingMethodText(hoaDonData?.phuongThucVanChuyen) }}</span></p>
              <p>Thanh toán: <span class="text-gray-900 font-medium">{{ getPaymentMethodText(hoaDonData?.phuongThucThanhToan) }}</span></p>
            </div>
          </div>
        </div>

        <!-- Bảng sản phẩm -->
        <table class="w-full text-sm mb-6">
          <thead>
            <tr class="bg-gray-50 border-y border-gray-200 text-gray-500 text-xs uppercase">
              <th class="py-3 text-left pl-2 font-semibold">
                Sản phẩm
              </th>
              <th class="py-3 text-center font-semibold w-16">
                SL
              </th>
              <th class="py-3 text-right font-semibold w-28">
                Đơn giá
              </th>
              <th class="py-3 text-right font-semibold w-32 pr-2">
                Thành tiền
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100">
            <tr v-for="item in printProducts" :key="item.id + (item.imeiCode || '')">
              <td class="py-4 pl-2 align-top">
                <p class="font-bold text-gray-800 text-[15px]">
                  {{ item.tenSanPham }}
                </p>
                <p class="text-[11px] text-gray-500 mt-1 italic">
                  <span v-if="item.thuongHieu">{{ item.thuongHieu }} | </span>
                  <span v-if="item.mauSac">{{ item.mauSac }} | </span>
                  <span v-if="item.imeiCode">Serial: {{ item.imeiCode }}</span>
                  <span v-else-if="!item.imeiCode && item.danhSachImei && parseIMEIList(item.danhSachImei).length === 0">Không có Serial</span>
                </p>
              </td>
              <td class="py-4 text-center align-top text-gray-600 font-medium">
                {{ item.soLuongImei || item.soLuong }}
              </td>
              <td class="py-4 text-right align-top text-gray-600">
                {{ formatCurrency(item.giaBanImei || item.giaBan) }}
              </td>
              <td class="py-4 text-right align-top font-bold text-gray-900 pr-2">
                {{ formatCurrency(item.tongTienImei || item.tongTien) }}
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Tổng hợp thanh toán -->
        <div class="flex justify-end">
          <div class="w-2/3 md:w-1/2 space-y-2 text-right">
            <div class="flex justify-between text-sm text-gray-600">
              <span>Tổng tiền hàng:</span>
              <span class="font-medium">{{ formatCurrency(totalAmount) }}</span>
            </div>

            <div v-if="hoaDonData?.phiVanChuyen && hoaDonData.phiVanChuyen > 0" class="flex justify-between text-sm text-gray-600">
              <span>Phí vận chuyển:</span>
              <span>+ {{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>

            <div v-if="hoaDonData?.giaTriVoucher && hoaDonData.giaTriVoucher > 0" class="py-2 my-1 border-y border-dashed border-gray-100">
              <div class="flex justify-between text-sm text-green-600 mb-1">
                <span>Ưu đãi ({{ hoaDonData?.tenVoucher || 'Voucher' }}):</span>
                <span>- {{ formatCurrency(hoaDonData.giaTriVoucher) }}</span>
              </div>
              <div v-if="hoaDonData?.maVoucher" class="flex justify-between text-xs text-gray-400 italic">
                <span>Mã áp dụng:</span>
                <span>{{ hoaDonData.maVoucher }}</span>
              </div>
            </div>

            <div class="flex justify-between text-sm text-gray-600">
              <span>Thuế (VAT 0%):</span>
              <span>{{ formatCurrency(0) }}</span>
            </div>

            <NDivider class="my-3 bg-gray-800" />

            <div class="flex justify-between items-center">
              <span class="font-bold text-gray-800 uppercase text-sm">Tổng thanh toán:</span>
              <span class="text-2xl font-extrabold text-indigo-700">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>

            <div v-if="hoaDonData?.duNo && hoaDonData.duNo > 0" class="flex justify-between text-sm text-orange-600 mt-2">
              <span>Còn nợ:</span>
              <span class="font-bold">{{ formatCurrency(hoaDonData.duNo) }}</span>
            </div>
          </div>
        </div>

        <!-- Ghi chú -->
        <div v-if="hoaDonData?.ghiChu" class="mt-6 p-4 bg-gray-50 rounded-lg">
          <p class="text-sm text-gray-600">
            <span class="font-bold">Ghi chú:</span> {{ hoaDonData.ghiChu }}
          </p>
        </div>

        <!-- Footer -->
        <div class="mt-16 pt-8 text-center border-t border-gray-100">
          <p class="font-bold text-gray-800">
            Cảm ơn quý khách đã tin tưởng My Laptop Store!
          </p>
          <p class="text-[10px] text-gray-400 mt-1">
            Sản phẩm được bảo hành chính hãng. Vui lòng giữ lại hóa đơn này để bảo hành.
          </p>
          <p class="text-[10px] text-gray-400 mt-1">
            Hóa đơn được tạo lúc {{ formatTime(hoaDonData?.ngayTao) }}
          </p>
        </div>
      </div>
    </div>


    <!-- Progress Timeline -->
    <NCard class="shadow-sm border-0 rounded-xl no-print" content-class="p-6">
      <template #header>
        <div class="flex items-center justify-between">
          <h3 class="text-lg font-semibold text-gray-900">
            Tiến trình đơn hàng
          </h3>
          <span class="text-sm text-gray-500">Cập nhật: {{ formatDateTime(hoaDonData?.thoiGianCapNhatCuoi || hoaDonData?.ngayTao) }}</span>
        </div>

        <NButton
          v-if="showCancelButton"
          type="error"
          block
          ghost
          :disabled="isCancelled"
          :style="{ maxWidth: '150px', marginTop: '10px' }"
          @click="openCancelModal"
        >
          <template #icon>
            <NIcon>
              <CloseCircleOutline />
            </NIcon>
          </template>
          Hủy đơn hàng
        </NButton>
      </template>

      <div class="relative">
        <!-- Progress bar (ẩn khi loaiHoaDon = 0) -->
        <template v-if="!isCounterInvoice">
          <div class="absolute top-5 left-0 right-0 h-1.5 bg-gray-200 rounded-full z-0" />
          <div class="absolute top-5 left-0 h-1.5 bg-blue-500 rounded-full z-10" :style="{ width: progressWidth }" />
        </template>

        <!-- Steps -->
        <div class="relative flex justify-between z-20">
          <div
            v-for="step in filteredSteps" :key="step.key" class="flex flex-col items-center flex-1"
            :class="{ 'cursor-pointer': isStepSelectable(step.key) }" @click="handleStepClick(step.key)"
          >
            <div
              class="w-10 h-10 rounded-full border-4 bg-white flex items-center justify-center mb-3 relative transition-all duration-300 hover:scale-110"
              :class="getStepCircleClass(step.key)"
            >
              <NIcon size="18" :class="getStepIconClass(step.key)">
                <component :is="step.icon" />
              </NIcon>
              <div v-if="isStepCompleted(step.key)" class="absolute -bottom-1 -right-1 w-6 h-6 bg-green-500 rounded-full flex items-center justify-center border-2 border-white shadow">
                <NIcon size="14" color="white">
                  <CheckmarkCircleOutline />
                </NIcon>
              </div>
            </div>
            <div class="text-center">
              <p class="text-sm font-semibold mb-1" :class="getStepTextClass(step.key)">
                {{ step.title }}
              </p>
              <p class="text-xs text-gray-500 min-h-[20px]">
                {{ getStepTime(step.key) || 'Không có dữ liệu' }}
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
              <NIcon size="24" color="#3b82f6">
                <component :is="getCurrentStepIcon()" />
              </NIcon>
            </div>
            <div>
              <p class="text-sm text-gray-500">
                Trạng thái hiện tại
              </p>
              <p class="font-semibold" :class="getCurrentStatusTextClass()">
                {{ getStatusText(currentStatus) }}
              </p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-green-50 flex items-center justify-center">
              <NIcon size="24" color="#10b981">
                <CashOutline />
              </NIcon>
            </div>
            <div>
              <p class="text-sm text-gray-500">
                Tổng tiền
              </p>
              <p class="font-semibold text-red-600">
                {{ formatCurrency(hoaDonData?.tongTienSauGiam) }}
              </p>
            </div>
          </div>
          <div class="flex items-center gap-3">
            <div class="w-12 h-12 rounded-full bg-purple-50 flex items-center justify-center">
              <NIcon size="24" color="#8b5cf6">
                <CubeOutline />
              </NIcon>
            </div>
            <div>
              <p class="text-sm text-gray-500">
                Số lượng sản phẩm
              </p>
              <p class="font-semibold text-gray-900">
                {{ totalQuantity }} sản phẩm
              </p>
            </div>
          </div>
        </div>
      </div>
    </NCard>

    <!-- Main Information Grid -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 no-print">
      <!-- Customer Information -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <NIcon size="20" color="#4b5563">
                <PersonOutline />
              </NIcon>
              <h3 class="text-lg font-semibold text-gray-900">
                Thông tin khách hàng
              </h3>
            </div>
            <NButton v-if="currentStatus === 0" type="primary" tertiary size="small" class="!text-sm" @click="openCustomerEditModal">
              <template #icon>
                <NIcon><CreateOutline /></NIcon>
              </template>
              Chỉnh sửa
            </NButton>
          </div>
        </template>

        <div class="space-y-4">
          <div class="flex items-start gap-3">
            <div class="w-14 h-14 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
              <NIcon size="24" color="#3b82f6">
                <PersonCircleOutline />
              </NIcon>
            </div>
            <div class="flex-1 min-w-0">
              <h4 class="font-semibold text-gray-900 truncate">
                {{ hoaDonData?.tenKhachHang2 || hoaDonData?.tenKhachHang || 'Khách lẻ' }}
              </h4>
              <div class="flex flex-wrap gap-2 mt-2">
                <span class="inline-flex items-center gap-1 text-sm text-gray-600">
                  <NIcon size="14"><CallOutline /></NIcon>
                  {{ hoaDonData?.sdtKH2 || hoaDonData?.sdtKH || 'Chưa cập nhật' }}
                </span>
                <span class="inline-flex items-center gap-1 text-sm text-gray-600">
                  <NIcon size="14"><MailOutline /></NIcon>
                  {{ hoaDonData?.email2 || hoaDonData?.email || 'Chưa có email' }}
                </span>
              </div>
            </div>
          </div>

          <div class="space-y-3 pt-4 border-t border-gray-100">
            <div>
              <p class="text-sm text-gray-600 mb-1">
                Địa chỉ giao hàng:
              </p>
              <p class="text-sm font-medium text-gray-900 bg-gray-50 p-3 rounded">
                {{ formatAddress(hoaDonData?.diaChi) || formatAddress(hoaDonData?.diaChi2) || 'Không có địa chỉ' }}
              </p>
            </div>
          </div>
        </div>
      </NCard>

      <!-- Customer Edit Modal -->
      <NModal
        v-model:show="showCustomerModal" preset="card" title="Chỉnh sửa thông tin khách hàng"
        class="!w-full !max-w-2xl" :segmented="{ content: 'soft', footer: 'soft' }"
      >
        <div class="space-y-4">
          <NForm
            ref="customerFormRef" :model="customerForm" :rules="customerFormRules"
            label-placement="left" label-width="140" require-mark-placement="right-hanging" size="medium"
          >
            <NGrid :cols="2" :x-gap="24">
              <NGi>
                <NFormItem label="Họ và tên" path="tenKhachHang">
                  <NInput v-model:value="customerForm.tenKhachHang" placeholder="Nhập họ và tên" clearable />
                </NFormItem>
              </NGi>
              <NGi>
                <NFormItem label="Số điện thoại" path="sdtKH">
                  <NInput v-model:value="customerForm.sdtKH" placeholder="Nhập số điện thoại" clearable />
                </NFormItem>
              </NGi>
              <NGi :span="2">
                <NFormItem label="Email" path="email">
                  <NInput v-model:value="customerForm.email" placeholder="Nhập email" clearable />
                </NFormItem>
              </NGi>
              <NGi :span="2">
                <NFormItem label="Địa chỉ" path="diaChi">
                  <NInput
                    v-model:value="customerForm.diaChi" type="textarea" placeholder="Nhập địa chỉ đầy đủ"
                    :autosize="{ minRows: 2, maxRows: 4 }"
                  />
                </NFormItem>
              </NGi>
            </NGrid>
          </NForm>
        </div>

        <template #footer>
          <div class="flex justify-end gap-3">
            <NButton @click="showCustomerModal = false">
              Hủy
            </NButton>
            <NButton type="primary" :loading="isSavingCustomer" @click="saveCustomerInfo">
              Lưu thông tin
            </NButton>
          </div>
        </template>
      </NModal>

      <!-- Order Summary -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <NIcon size="20" color="#4b5563">
              <ReceiptOutline />
            </NIcon>
            <h3 class="text-lg font-semibold text-gray-900">
              Tóm tắt đơn hàng
            </h3>
          </div>
        </template>

        <div class="space-y-3">
          <div class="grid grid-cols-2 gap-3">
            <div class="bg-gray-50 p-3 rounded-lg">
              <p class="text-xs text-gray-500">
                Tổng sản phẩm
              </p>
              <div v-if="productCount > 0">
                <p class="text-lg font-bold text-gray-900">
                  {{ productCount }} sản phẩm
                </p>
                <p class="text-xs text-gray-500 mt-1">
                  {{ imeiProductsCount }} SERIAL
                </p>
              </div>
              <div v-else>
                <p class="text-lg font-bold text-gray-900 text-red-500">
                  Không có sản phẩm
                </p>
              </div>
            </div>
            <div class="bg-gray-50 p-3 rounded-lg">
              <p class="text-xs text-gray-500">
                Tổng số lượng
              </p>
              <p class="text-lg font-bold text-gray-900">
                {{ totalQuantity }}
              </p>
            </div>
          </div>

          <div class="space-y-2 pt-4">
            <div class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Tổng tiền hàng:</span>
              <span class="font-semibold">{{ formatCurrency(totalAmount) }}</span>
            </div>

            <div v-if="hoaDonData?.phiVanChuyen" class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Phí vận chuyển:</span>
              <span class="font-semibold">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>

            <div v-if="hoaDonData?.giaTriVoucher" class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Giảm giá voucher:</span>
              <span class="font-semibold text-green-600">-{{ formatCurrency(hoaDonData.giaTriVoucher) }}</span>
            </div>

            <div class="flex justify-between items-center pt-4">
              <span class="text-lg font-bold text-gray-900">TỔNG CỘNG:</span>
              <span class="text-xl font-bold text-red-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>
          </div>

          <div v-if="hoaDonData?.maVoucher" class="mt-4 p-3 bg-green-50 rounded-lg border border-green-100">
            <div class="flex items-center gap-2 mb-1">
              <NIcon size="16" color="#10b981">
                <PricetagOutline />
              </NIcon>
              <span class="text-sm font-medium text-green-800">Voucher áp dụng</span>
            </div>
            <p class="text-green-700 font-medium">
              {{ hoaDonData.maVoucher }} - {{ hoaDonData.tenVoucher }}
            </p>
            <p class="text-xs text-green-600 mt-1">
              Giảm: {{ formatCurrency(hoaDonData.giaTriVoucher) }}
            </p>
          </div>
        </div>
      </NCard>

      <!-- Payment Information -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <NIcon size="20" color="#4b5563">
              <CardOutline />
            </NIcon>
            <h3 class="text-lg font-semibold text-gray-900">
              Thông tin thanh toán
            </h3>
          </div>
        </template>

        <div class="space-y-4">
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <div class="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center">
                <NIcon size="20" color="#3b82f6">
                  <CashOutline />
                </NIcon>
              </div>
              <div>
                <p class="font-medium">
                  {{ getPaymentMethodText(hoaDonData?.phuongThucThanhToan) }}
                </p>
                <p class="text-xs text-gray-500">
                  Phương thức
                </p>
              </div>
            </div>
            <NTag :type="getPaymentStatusTagType()" size="small">
              {{ getPaymentStatusText() }}
            </NTag>
          </div>

          <div class="space-y-3 pt-4 border-t border-gray-100">
            <div class="flex justify-between items-center">
              <span class="text-gray-600">Tổng tiền thanh toán:</span>
              <span class="font-bold text-green-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>

            <div v-if="hoaDonData?.duNo" class="flex justify-between items-center">
              <span class="text-gray-600">Còn nợ:</span>
              <span class="font-bold text-orange-600">{{ formatCurrency(hoaDonData.duNo) }}</span>
            </div>

            <div v-if="hoaDonData?.hoanPhi" class="flex justify-between items-center">
              <span class="text-gray-600">Hoàn phí:</span>
              <span class="font-bold text-blue-600">{{ formatCurrency(hoaDonData.hoanPhi) }}</span>
            </div>

            <div class="pt-4">
              <p class="text-sm text-gray-600 mb-2">
                Thời gian thanh toán:
              </p>
              <div class="flex items-center gap-2 text-sm">
                <NIcon size="16" color="#6b7280">
                  <TimeOutline />
                </NIcon>
                <span>{{ formatDateTime(hoaDonData?.ngayTao) }}</span>
              </div>
            </div>
          </div>
        </div>
      </NCard>
    </div>

    <!-- Products Table -->
    <NCard class="shadow-sm border-0 rounded-xl overflow-hidden no-print">
      <template #header>
        <div class="flex items-center justify-between">
          <div class="flex items-center gap-2">
            <NIcon size="20" color="#4b5563">
              <CubeOutline />
            </NIcon>
            <h3 class="text-lg font-semibold text-gray-900">
              Danh sách sản phẩm
            </h3>
          </div>
          <div class="flex items-center gap-4">
            <span class="text-sm text-gray-500">{{ productCount }} sản phẩm</span>
            <span class="text-sm text-gray-500">{{ totalQuantity }} số lượng</span>
            <NTag v-if="imeiProductsCount > 0" type="info" size="small">
              {{ imeiProductsCount }} Serial
            </NTag>

            <!-- Nút xem tổng hợp serial -->
            <NButton
              type="info"
              secondary
              size="small"
              @click="openSerialSummaryModal"
            >
              <template #icon>
                <NIcon><GlobeOutline /></NIcon>
              </template>
              Xem tổng hợp SERIAL
            </NButton>
          </div>
        </div>
      </template>

      <div class="overflow-x-auto">
        <NDataTable
          :columns="productColumns" :data="displayProducts" :pagination="false" striped
          class="min-w-full" :row-class-name="rowClassName"
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

            <div v-if="hoaDonData?.phiVanChuyen" class="flex justify-between items-center">
              <span class="text-gray-600">Phí vận chuyển:</span>
              <span class="font-medium">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>

            <div v-if="hoaDonData?.giaTriVoucher" class="flex justify-between items-center">
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

    <!-- Status Update Modal -->
    <NModal v-model:show="showStatusModal" preset="dialog" title="Cập nhật trạng thái hóa đơn" class="no-print">
      <template #header>
        <div class="flex items-center gap-2">
          <NIcon size="20" color="#3b82f6">
            <RefreshOutline />
          </NIcon>
          <span class="font-semibold">Cập nhật trạng thái</span>
        </div>
      </template>

      <div class="space-y-4 py-4">
        <div class="p-3 bg-blue-50 rounded-lg">
          <p class="text-sm text-gray-600">
            Mã hóa đơn: <span class="font-bold">{{ hoaDonData?.maHoaDon }}</span>
          </p>
          <div class="flex items-center gap-2 mt-2">
            <NTag :type="getStatusTagType(currentStatus)" size="small">
              {{ getStatusText(currentStatus) }}
            </NTag>
            <NIcon size="16" class="text-gray-400">
              <ArrowForwardOutline />
            </NIcon>
            <NTag :type="getStatusTagType(selectedStatus)" size="small">
              {{ getStatusText(selectedStatus) }}
            </NTag>
          </div>
        </div>

        <NFormItem label="Chọn trạng thái mới:" required>
          <NSelect
            v-model:value="selectedStatus" :options="availableStatusOptions" placeholder="Chọn trạng thái"
            clearable :disabled="isCompleted || isCancelled"
          />
        </NFormItem>

        <NFormItem label="Ghi chú (tùy chọn):">
          <NInput v-model:value="statusNote" type="textarea" placeholder="Nhập ghi chú cho trạng thái mới..." :rows="3" />
        </NFormItem>

        <div v-if="selectedStatus != null" class="p-3 bg-gray-50 rounded">
          <p class="text-sm font-medium text-gray-700 mb-1">
            Xác nhận thay đổi:
          </p>
          <p class="text-sm text-gray-600">
            Chuyển từ <span class="font-bold text-yellow-600">{{ getStatusText(currentStatus) }}</span>
            sang <span class="font-bold text-green-600">{{ getStatusText(selectedStatus) }}</span>
          </p>
        </div>
      </div>

      <template #action>
        <div class="flex gap-2 justify-end">
          <NButton @click="showStatusModal = false">
            Hủy
          </NButton>
          <NButton type="primary" :disabled="!selectedStatus" :loading="isUpdating" @click="confirmStatusUpdate">
            Xác nhận cập nhật
          </NButton>
        </div>
      </template>
    </NModal>

    <!-- Serial Selection Modal - Dành cho đơn online -->
    <NModal
      v-model:show="showSerialModal"
      preset="card"
      :title="`Chọn Serial - ${selectedProductItem?.tenSanPham || ''}`"
      style="width: 90%; max-width: 1000px"
      :bordered="false"
      class="no-print"
    >
      <div class="space-y-4">
        <!-- Thông tin sản phẩm -->
        <div v-if="selectedProductItem" class="flex items-center gap-3 p-3 bg-blue-50 rounded-lg">
          <NAvatar
            :src="selectedProductItem.anhSanPham"
            size="medium"
            round
            fallback-src="https://via.placeholder.com/40"
          />
          <div class="flex-1">
            <div class="font-semibold">
              {{ selectedProductItem.tenSanPham }}
            </div>
            <div class="text-sm text-gray-600">
              Cần chọn: {{ selectedProductItem.soLuong }} serial |
              Đã chọn: {{ selectedSerialIds.length }}/{{ selectedProductItem.soLuong }}
            </div>
          </div>

          <!-- Nút quét serial -->
          <NButton type="primary" secondary @click="startScanMode">
            <template #icon>
              <NIcon><QrCodeOutline /></NIcon>
            </template>
            Quét Serial
          </NButton>
        </div>

        <!-- Chế độ quét -->
        <div v-if="scanMode" class="p-4 bg-yellow-50 rounded-lg border border-yellow-200">
          <div class="flex items-center gap-3">
            <NInput
              ref="scanInputRef"
              v-model:value="scannedSerial"
              placeholder="Quét hoặc nhập serial..."
              @keyup.enter="handleScanInput"
            >
              <template #prefix>
                <NIcon><QrCodeOutline /></NIcon>
              </template>
            </NInput>
            <NButton type="primary" @click="handleScanInput">
              Thêm
            </NButton>
            <NButton @click="scanMode = false">
              Hủy
            </NButton>
          </div>
          <div class="text-xs text-gray-500 mt-2">
            Quét mã QR hoặc nhập serial và nhấn Enter để thêm nhanh
          </div>
        </div>

        <!-- Danh sách serial -->
        <div v-if="loadingSerials" class="text-center py-8">
          <NSpin size="large" />
        </div>

        <NDataTable
          v-else
          :columns="serialColumns"
          :data="selectedSerials"
          :max-height="400"
          :pagination="{ pageSize: 10 }"
          :bordered="false"
        />

        <!-- Footer -->
        <div class="flex justify-end gap-3 pt-4 border-t">
          <NButton @click="showSerialModal = false">
            Hủy
          </NButton>
          <NButton
            type="primary"
            :disabled="selectedSerialIds.length !== requiredQuantity"
            :loading="isAddingSerials"
            @click="addSerialsToInvoice"
          >
            Xác nhận ({{ selectedSerialIds.length }}/{{ requiredQuantity }})
          </NButton>
        </div>
      </div>
    </NModal>

    <!-- Serial Info Modal - Dành cho đơn tại quầy/giao hàng -->
    <NModal
      v-model:show="showSerialInfoModal"
      preset="card"
      :title="`Thông tin Serial - ${selectedSerialInfoProduct?.tenSanPham || ''}`"
      style="width: 500px"
      :bordered="false"
      class="no-print"
    >
      <div v-if="selectedSerialInfoProduct" class="space-y-4">
        <!-- Thông tin sản phẩm -->
        <div class="flex items-center gap-3 p-3 bg-green-50 rounded-lg">
          <NAvatar
            :src="selectedSerialInfoProduct.anhSanPham"
            size="medium"
            round
            fallback-src="https://via.placeholder.com/40"
          />
          <div>
            <div class="font-semibold">
              {{ selectedSerialInfoProduct.tenSanPham }}
            </div>
            <div class="text-sm text-gray-600">
              Số lượng: {{ selectedSerialInfoProduct.soLuong }} |
              Giá: {{ formatCurrency(selectedSerialInfoProduct.giaBan) }}
            </div>
          </div>
        </div>

        <NDivider />

        <!-- Danh sách serial -->
        <div v-if="currentProductSerialInfo.length === 0" class="text-center py-8 text-gray-400">
          <NIcon size="48" class="mb-2">
            <CubeOutline />
          </NIcon>
          <p>Sản phẩm chưa có serial nào được gán</p>
        </div>

        <div v-else class="space-y-2 max-h-96 overflow-y-auto">
          <div
            v-for="(imei, index) in currentProductSerialInfo"
            :key="imei.id || index"
            class="flex items-center justify-between p-3 bg-gray-50 rounded-lg border"
          >
            <div class="flex items-center gap-2">
              <span class="text-xs text-gray-400">{{ index + 1 }}.</span>
              <span class="font-mono font-semibold">{{ imei.code || imei.imeiCode }}</span>
            </div>
            <NTag :type="IMEI_STATUS_CONFIG[imei.status]?.type || 'default'" size="small" round>
              {{ IMEI_STATUS_CONFIG[imei.status]?.text || imei.status }}
            </NTag>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end">
          <NButton type="primary" @click="showSerialInfoModal = false">
            Đóng
          </NButton>
        </div>
      </template>
    </NModal>

    <!-- History Modal -->
    <NModal
      v-model:show="showHistoryModal" preset="card" title="Lịch sử đơn hàng"
      style="width: 1000px; max-width: 95vw; border-radius: 12px;" class="no-print shadow-2xl"
      :bordered="false" size="huge"
    >
      <div class="w-full mt-2">
        <div class="grid grid-cols-12 gap-4 pb-4 border-b-2 border-gray-100 text-[13px] font-bold text-gray-500 uppercase tracking-wider">
          <div class="col-span-3 pl-4">
            Trạng thái
          </div>
          <div class="col-span-3">
            Thời gian
          </div>
          <div class="col-span-3">
            Người xác nhận
          </div>
          <div class="col-span-3 pr-4">
            Ghi chú
          </div>
        </div>

        <div class="divide-y divide-gray-100 max-h-[60vh] overflow-y-auto pr-2 mt-2">
          <div
            v-for="(item, index) in historyList" :key="index"
            class="grid grid-cols-12 gap-4 py-4 items-center hover:bg-gray-50/70 transition-colors rounded-lg"
          >
            <div class="col-span-3 pl-4 flex items-center gap-3">
              <div class="w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0" :class="getStepCircleBg(item.trangThai)">
                <NIcon size="18" :class="getStepIconClass(item.trangThai.toString())">
                  <component :is="getStatusIcon(item.trangThai)" />
                </NIcon>
              </div>
              <span class="font-bold text-gray-800 text-[15px]">{{ getStatusText(item.trangThai) }}</span>
            </div>

            <div class="col-span-3 text-[14px] text-gray-600 font-medium tracking-wide">
              {{ formatHistoryTime(item.thoiGian) }}
            </div>

            <div class="col-span-3 text-[14.5px] text-gray-800 font-semibold">
              {{ item.nhanVien || 'Hệ thống tự động' }}
            </div>

            <div class="col-span-3 pr-4 text-[14px] text-gray-600 leading-relaxed">
              {{ item.ghiChu || '---' }}
            </div>
          </div>

          <div v-if="historyList.length === 0" class="py-16 text-center text-gray-400 flex flex-col items-center">
            <NIcon size="48" class="mb-3 opacity-30">
              <TimeOutline />
            </NIcon>
            <p class="text-lg">
              Chưa có dữ liệu lịch sử cập nhật.
            </p>
          </div>
        </div>
      </div>
    </NModal>

    <!-- Product IMEI Info Modal -->
    <NModal
      v-model:show="showProductImeiModal"
      preset="card"
      title="Thông tin sản phẩm và SERIAL"
      style="width: 480px; max-width: 95vw; border-radius: 16px;"
      :bordered="false"
      class="no-print"
    >
      <div v-if="selectedProductForModal" class="space-y-4">
        <!-- Thông tin sản phẩm -->
        <div class="flex items-center gap-4 p-4 bg-purple-50 rounded-xl border border-purple-100">
          <NAvatar
            :src="selectedProductForModal.anhSanPham"
            :size="64"
            :round="false"
            class="border-2 border-purple-200 rounded-lg flex-shrink-0"
            fallback-src="https://via.placeholder.com/64?text=No+Image"
          />
          <div class="flex-1 min-w-0">
            <p class="font-bold text-gray-900 text-base truncate">
              {{ selectedProductForModal.tenSanPham }}
            </p>
            <div class="flex flex-wrap gap-1 mt-1">
              <NTag v-if="selectedProductForModal.size" size="tiny" type="default" :bordered="false">
                {{ selectedProductForModal.size }}
              </NTag>
              <NTag v-if="selectedProductForModal.mauSac" size="tiny" type="default" :bordered="false">
                {{ selectedProductForModal.mauSac }}
              </NTag>
              <NTag v-if="selectedProductForModal.thuongHieu" size="tiny" type="info" :bordered="false">
                {{ selectedProductForModal.thuongHieu }}
              </NTag>
            </div>
            <p class="text-orange-500 font-bold text-lg mt-2">
              {{ formatCurrency(selectedProductForModal.giaBan) }}
            </p>
          </div>
        </div>

        <NDivider class="my-2" />

        <!-- CÓ IMEI RỒI: Hiển thị danh sách -->
        <template v-if="currentProductImeiList.length > 0">
          <div>
            <div class="flex items-center justify-between mb-3">
              <p class="text-sm font-semibold text-gray-700">
                Mã SERIAL đã gán
                <NTag size="tiny" type="success" class="ml-2">
                  {{ currentProductImeiList.length }} SERIAL
                </NTag>
              </p>
              <!-- Chỉ online + chờ xác nhận mới cho thay đổi -->
              <NTag v-if="canChangeImei" size="tiny" type="warning">
                Có thể thay đổi
              </NTag>
            </div>

            <div class="space-y-2">
              <div
                v-for="(imei, idx) in currentProductImeiList"
                :key="imei.id || idx"
                class="flex items-center justify-between px-4 py-2 bg-gray-50 rounded-lg border border-gray-200"
              >
                <div class="flex items-center gap-2">
                  <span class="text-xs text-gray-400 w-5">{{ idx + 1 }}.</span>
                  <span class="font-mono font-semibold text-gray-800 text-sm tracking-wider">
                    {{ imei.code || imei.imeiCode || '---' }}
                  </span>
                </div>
                <NTag
                  :type="IMEI_STATUS_CONFIG[imei.status]?.type || 'default'"
                  size="small"
                  round
                >
                  {{ IMEI_STATUS_CONFIG[imei.status]?.text || imei.status }}
                </NTag>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div>
            <p class="text-sm font-semibold text-gray-700 mb-3">
              Mã SERIAL
              <NTag size="tiny" type="info" class="ml-2">
                Cần {{ selectedProductForModal.soLuong }} SERIAL
              </NTag>
            </p>

            <!-- Input IMEI -->
            <div class="relative">
              <div class="flex gap-2">
                <div class="flex-1 relative">
                  <NInput
                    :value="imeiInputValue"
                    placeholder="Nhập SERIAL..."
                    clearable
                    @input="handleImeiInput"
                    @focus="showImeiSuggestions = imeiSuggestions.length > 0"
                    @blur="hideImeiSuggestions"
                  >
                    <template #prefix>
                      <NIcon size="16" color="#9333ea">
                        <CardOutline />
                      </NIcon>
                    </template>
                  </NInput>

                  <div
                    v-if="showImeiSuggestions && imeiSuggestions.length > 0"
                    class="absolute z-50 left-0 right-0 top-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-48 overflow-y-auto"
                  >
                    <div
                      v-for="suggestion in imeiSuggestions"
                      :key="suggestion"
                      class="px-4 py-2 hover:bg-purple-50 cursor-pointer font-mono text-sm text-gray-700 border-b border-gray-50 last:border-0"
                      @mousedown="selectImeiSuggestion(suggestion)"
                    >
                      {{ suggestion }}
                    </div>
                  </div>
                </div>

                <!-- Nút scan -->
                <NButton type="primary" secondary style="background: #ede9fe; border-color: #9333ea;" @click="startScanMode">
                  <template #icon>
                    <NIcon color="#9333ea">
                      <QrCodeOutline />
                    </NIcon>
                  </template>
                </NButton>

                <NButton
                  type="primary"
                  style="background: #9333ea; border-color: #9333ea;"
                  @click="() => { showProductImeiModal = false; openSerialSelectionModal(selectedProductForModal!) }"
                >
                  <template #icon>
                    <NIcon color="white">
                      <ListOutline />
                    </NIcon>
                  </template>
                </NButton>
              </div>
            </div>

            <div class="mt-3 p-3 bg-yellow-50 rounded-lg border border-yellow-100 text-sm text-yellow-700">
              ⚠️ Chưa có SERIAL nào được gán. Nhập hoặc chọn từ danh sách.
            </div>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="flex justify-end gap-2">
          <NButton
            v-if="canChangeImei && selectedProductForModal && currentProductImeiList.length > 0"
            type="warning"
            @click="() => { showProductImeiModal = false; openSerialSelectionModal(selectedProductForModal!) }"
          >
            <template #icon>
              <NIcon><RefreshOutline /></NIcon>
            </template>
            Thay đổi SERIAL
          </NButton>
          <NButton
            v-else-if="selectedProductForModal && currentProductImeiList.length === 0"
            type="primary"
            style="background: #9333ea; border-color: #9333ea;"
            @click="() => { showProductImeiModal = false; openSerialSelectionModal(selectedProductForModal!) }"
          >
            <template #icon>
              <NIcon><ListOutline /></NIcon>
            </template>
            Chọn SERIAL từ danh sách
          </NButton>
          <NButton @click="showProductImeiModal = false">
            Đóng
          </NButton>
        </div>
      </template>
    </NModal>

    <!-- Serial Summary Modal - Hiển thị tổng hợp SERIAL theo loại đơn hàng -->
    <NModal
      v-model:show="showSerialSummaryModal"
      preset="card"
      title="Thông tin SERIAL tổng hợp"
      style="width: 800px; max-width: 95vw; border-radius: 16px;"
      :bordered="false"
      class="no-print"
    >
      <div v-if="serialSummaryData.product" class="space-y-6">
        <!-- Thông tin sản phẩm -->
        <div class="flex items-center gap-4 p-4 bg-gradient-to-r from-blue-50 to-purple-50 rounded-xl border border-blue-100">
          <NAvatar
            :src="serialSummaryData.product.anhSanPham"
            :size="80"
            :round="false"
            class="border-2 border-blue-200 rounded-lg flex-shrink-0"
            fallback-src="https://via.placeholder.com/80?text=No+Image"
          />
          <div class="flex-1">
            <p class="font-bold text-gray-900 text-lg">
              {{ serialSummaryData.product.tenSanPham }}
            </p>
            <div class="flex flex-wrap gap-2 mt-2">
              <NTag v-if="serialSummaryData.product.maSanPham" size="small" type="info" :bordered="false">
                Mã: {{ serialSummaryData.product.maSanPham }}
              </NTag>
              <NTag v-if="serialSummaryData.product.size" size="small" type="default" :bordered="false">
                Size: {{ serialSummaryData.product.size }}
              </NTag>
              <NTag v-if="serialSummaryData.product.mauSac" size="small" type="default" :bordered="false">
                Màu: {{ serialSummaryData.product.mauSac }}
              </NTag>
            </div>
            <div class="flex items-center gap-4 mt-3">
              <p class="text-sm text-gray-600">
                Số lượng: <span class="font-bold text-gray-900">{{ serialSummaryData.product.soLuong }}</span>
              </p>
              <p class="text-sm text-gray-600">
                SERIAL: <span class="font-bold text-blue-600">{{ serialSummaryData.totalSerials }}</span>
              </p>
              <p class="text-sm text-gray-600">
                Đã gán: <span class="font-bold" :class="serialSummaryData.totalSerials >= serialSummaryData.product.soLuong ? 'text-green-600' : 'text-orange-600'">
                  {{ Math.min(serialSummaryData.totalSerials, serialSummaryData.product.soLuong) }}/{{ serialSummaryData.product.soLuong }}
                </span>
              </p>
            </div>
          </div>
        </div>

        <!-- Tabs phân loại SERIAL -->
        <div class="space-y-6">
          <!-- SERIAL tại quầy -->
          <div v-if="serialSummaryData.counterSerials.length > 0" class="border border-green-200 rounded-xl overflow-hidden">
            <div class="bg-green-50 px-4 py-3 border-b border-green-200 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <NIcon size="20" color="#16a34a">
                  <CartOutline />
                </NIcon>
                <span class="font-bold text-green-700">SERIAL bán tại quầy</span>
              </div>
              <NTag type="success" size="small" round>
                {{ serialSummaryData.counterSerials.length }} SERIAL
              </NTag>
            </div>
            <div class="p-4 bg-white">
              <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
                <div
                  v-for="(imei, idx) in serialSummaryData.counterSerials"
                  :key="imei.id || idx"
                  class="flex items-center justify-between p-3 bg-green-50/50 rounded-lg border border-green-100"
                >
                  <div>
                    <span class="font-mono font-bold text-gray-800 text-sm">
                      {{ imei.code || imei.imeiCode }}
                    </span>
                  </div>
                  <NTag size="tiny" type="success" round>
                    {{ imei.statusText || 'Đã bán' }}
                  </NTag>
                </div>
              </div>
            </div>
          </div>

          <!-- SERIAL giao hàng -->
          <div v-if="serialSummaryData.deliverySerials.length > 0" class="border border-blue-200 rounded-xl overflow-hidden">
            <div class="bg-blue-50 px-4 py-3 border-b border-blue-200 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <NIcon size="20" color="#2563eb">
                  <CubeOutline />
                </NIcon>
                <span class="font-bold text-blue-700">SERIAL giao hàng</span>
              </div>
              <NTag type="info" size="small" round>
                {{ serialSummaryData.deliverySerials.length }} SERIAL
              </NTag>
            </div>
            <div class="p-4 bg-white">
              <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
                <div
                  v-for="(imei, idx) in serialSummaryData.deliverySerials"
                  :key="imei.id || idx"
                  class="flex items-center justify-between p-3 bg-blue-50/50 rounded-lg border border-blue-100"
                >
                  <div>
                    <span class="font-mono font-bold text-gray-800 text-sm">
                      {{ imei.code || imei.imeiCode }}
                    </span>
                  </div>
                  <NTag size="tiny" type="info" round>
                    {{ imei.statusText || 'Đã gán' }}
                  </NTag>
                </div>
              </div>
            </div>
          </div>

          <!-- SERIAL online -->
          <div v-if="serialSummaryData.onlineSerials.length > 0" class="border border-purple-200 rounded-xl overflow-hidden">
            <div class="bg-purple-50 px-4 py-3 border-b border-purple-200 flex items-center justify-between">
              <div class="flex items-center gap-2">
                <NIcon size="20" color="#9333ea">
                  <GlobeOutline />
                </NIcon>
                <span class="font-bold text-purple-700">SERIAL đơn online</span>
              </div>
              <NTag type="primary" size="small" round>
                {{ serialSummaryData.onlineSerials.length }} SERIAL
              </NTag>
            </div>
            <div class="p-4 bg-white">
              <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
                <div
                  v-for="(imei, idx) in serialSummaryData.onlineSerials"
                  :key="imei.id || idx"
                  class="flex items-center justify-between p-3 bg-purple-50/50 rounded-lg border border-purple-100"
                >
                  <div>
                    <span class="font-mono font-bold text-gray-800 text-sm">
                      {{ imei.code || imei.imeiCode }}
                    </span>
                  </div>
                  <NTag size="tiny" type="primary" round>
                    {{ imei.statusText || 'Đã đặt' }}
                  </NTag>
                </div>
              </div>
            </div>
          </div>

          <!-- Không có SERIAL -->
          <div v-if="serialSummaryData.totalSerials === 0" class="text-center py-12 bg-gray-50 rounded-xl">
            <NIcon size="48" class="text-gray-300 mb-3">
              <CubeOutline />
            </NIcon>
            <p class="text-gray-500 font-medium">
              Không có SERIAL nào được gán
            </p>
          </div>
        </div>

        <!-- Thông tin bổ sung -->
        <div v-if="selectedSummaryImeiRow" class="mt-4 p-3 bg-yellow-50 rounded-lg border border-yellow-200">
          <div class="flex items-center gap-2 text-sm text-yellow-700">
            <NIcon size="18">
              <InformationCircleOutline />
            </NIcon>
            <span>
              Đang xem SERIAL của <strong>{{ selectedSummaryImeiRow.imeiCode }}</strong>
              trong tổng số {{ serialSummaryData.totalSerials }} SERIAL
            </span>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="flex justify-end gap-3">
          <NButton type="primary" @click="showSerialSummaryModal = false">
            Đóng
          </NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
/* Print styles */
@media print {
  .no-print {
    display: none !important;
  }
  body {
    background: white !important;
    color: black !important;
    padding: 0 !important;
    margin: 0 !important;
    font-size: 12px;
  }
  .container {
    max-width: 100% !important;
    padding: 0 !important;
    margin: 0 !important;
  }
  #invoice-paper {
    box-shadow: none !important;
    padding: 20px !important;
    max-width: 100%;
  }
  .print-table {
    width: 100%;
    border-collapse: collapse;
  }
  .print-table th,
  .print-table td {
    padding: 8px 5px;
    border: 1px solid #ddd;
  }
  .print-table th {
    background-color: #f8f9fa;
    font-weight: bold;
  }
  tr {
    page-break-inside: avoid;
  }
  thead {
    display: table-header-group;
  }
  tfoot {
    display: table-footer-group;
  }
}

/* Hide invoice content */
.hidden {
  display: none;
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

/* Row styles */
:deep(.imei-row) {
  border-left: 4px solid #3b82f6 !important;
}
:deep(.product-row) {
  border-left: 4px solid #d1d5db !important;
}

/* QR Code styles */
.qr-code-container,
.qr-code-small {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.qr-code-container img,
.qr-code-small img {
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
  padding: 0.25rem;
  transition: transform 0.2s;
  print-color-adjust: exact;
  -webkit-print-color-adjust: exact;
}
.qr-code-container img:hover,
.qr-code-small img:hover {
  transform: scale(1.1);
}
</style>
