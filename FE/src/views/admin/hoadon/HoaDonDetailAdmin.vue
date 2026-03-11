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
  NRadioButton,
  NRadioGroup,
  NSelect,
  NSpace,
  NSpin,
  NTag,
  NText,
  useDialog,
  useMessage,
} from 'naive-ui'
import type { DataTableColumns, FormInst, FormRules, SelectOption } from 'naive-ui'
import QRCode from 'qrcode'

import type { ADChangeStatusRequest, HoaDonChiTietItem } from '@/service/api/admin/hoadon.api'

import {
  changeOrderStatus,
  getHoaDonChiTiets,
  updateCustomerInvoice,
} from '@/service/api/admin/hoadon.api'
import {
  doiImei,
  ganImei,
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
  EyeOutline,
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
  TIEN_MAT: 'Tiền mặt',
  VNPAY: 'VNPay',
  CHUYEN_KHOAN: 'Chuyển khoản',
  THE_TIN_DUNG: 'Thẻ tín dụng/Thẻ ghi nợ',
  VI_DIEN_TU: 'Ví điện tử',
  TIEN_MAT_CHUYEN_KHOAN: 'Tiền mặt + Chuyển khoản',
  CASH: 'Tiền mặt',
  BANKING: 'Chuyển khoản',
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
async function refreshSerialList() {
  if (!selectedProductItem.value || loadingSerials.value)
    return

  loadingSerials.value = true
  try {
    const response = await getImeiProductDetail(selectedProductItem.value.productDetailId!)
    if (response.success && response.data) {
      const existingImeis = parseIMEIList(selectedProductItem.value.danhSachImei)

      if (changingImei.value) {
        // Luồng thay đổi: loại bỏ các serial đã gán NGOẠI TRỪ serial đang thay thế
        const otherAssignedImeis = existingImeis.filter(imei => imei.id !== changingImei.value!.id)
        selectedSerials.value = response.data.filter(s =>
          s.imeiStatus === 'AVAILABLE'
          && !otherAssignedImeis.some(imei => imei.id === s.id),
        )
      }
      else {
        // Luồng bổ sung: loại bỏ tất cả serial đã gán
        selectedSerials.value = response.data.filter(s =>
          s.imeiStatus === 'AVAILABLE'
          && !existingImeis.some(imei => imei.id === s.id),
        )
      }
      message.success('Đã làm mới danh sách serial')
    }
    else {
      message.error(response.message || 'Không thể tải danh sách serial')
    }
  }
  catch {
    message.error('Đã xảy ra lỗi khi làm mới')
  }
  finally {
    loadingSerials.value = false
  }
}

function getPaymentMethodIcon(method: string | undefined) {
  switch (method) {
    case 'VNPAY':
    case 'THE_TIN_DUNG':
      return CardOutline // icon thẻ
    case 'CHUYEN_KHOAN':
    case 'TIEN_MAT_CHUYEN_KHOAN':
      return GlobeOutline // icon chuyển khoản
    case 'TIEN_MAT':
    default:
      return CashOutline // icon tiền mặt
  }
}

function parseIMEIList(imeiData: any): IMEIItem[] {
  if (!imeiData)
    return []
  try {
    if (Array.isArray(imeiData)) {
      return imeiData.map((item) => {
        if (typeof item === 'string') {
          try { return JSON.parse(item) }
          catch { return null }
        }
        return item
      }).filter(item => item !== null)
    }
    if (typeof imeiData === 'string') {
      const parsed = JSON.parse(imeiData)
      return Array.isArray(parsed) ? parsed : []
    }
    if (Array.isArray(imeiData))
      return imeiData
    return []
  }
  catch { return [] }
}

function getModalStatusColorClass(status: number | null) {
  if (status === null)
    return 'bg-gray-50 text-gray-500 border-gray-200'
  const map: Record<number, string> = {
    0: 'bg-yellow-50 text-yellow-600 border-yellow-500',
    1: 'bg-blue-50 text-blue-600 border-blue-500',
    2: 'bg-purple-50 text-purple-600 border-purple-500',
    3: 'bg-blue-50 text-blue-600 border-blue-500',
    4: 'bg-green-50 text-green-600 border-green-500',
    5: 'bg-red-50 text-red-600 border-red-500',
  }
  return map[status] || 'bg-gray-50 text-gray-600 border-gray-300'
}

// ==================== Refs & State ====================
const showCustomerModal = ref(false)
const isSavingCustomer = ref(false)
const customerFormRef = ref<FormInst | null>(null)
const customerForm = reactive<CustomerForm>({
  tenKhachHang: '',
  sdtKH: '',
  email: '',
  diaChi: '',
})

const invoiceItems = ref<HoaDonChiTietItem[]>([])
const printLoading = ref(false)
const isLoading = ref(false)
const isUpdating = ref(false)
const qrCodeDataUrl = ref<string>('')

const showStatusModal = ref(false)
const selectedStatus = ref<number | null>(null)
const statusNote = ref('')

const showSerialModal = ref(false)
const selectedProductItem = ref<HoaDonChiTietItem | null>(null)
const selectedSerials = ref<ADPDImeiResponse[]>([])
const selectedSerialIds = ref<string[]>([])
const loadingSerials = ref(false)
const isAddingSerials = ref(false)
const scanMode = ref(false)
const scannedSerial = ref('')
const scanInputRef = ref<HTMLInputElement | null>(null)

const requiredQuantityToAssign = ref(0)
const alreadyAssignedCount = ref(0)

// ✅ Ref lưu serial đang được thay thế (null = đang ở luồng bổ sung)
const changingImei = ref<IMEIItem | null>(null)

const showSerialInfoModal = ref(false)
const selectedSerialInfoProduct = ref<HoaDonChiTietItem | null>(null)
const showHistoryModal = ref(false)
const productViewMode = ref('products')

// ==================== Computed Properties ====================
const invoiceCode = computed(() => route.params.id as string || 'N/A')

const currentProductSerialInfo = computed(() => {
  if (!selectedSerialInfoProduct.value)
    return []
  return parseIMEIList(selectedSerialInfoProduct.value.danhSachImei)
})

const hoaDonData = computed(() => invoiceItems.value?.[0] || null)
const isOnlineInvoice = computed(() => hoaDonData.value?.loaiHoaDon === '1')
const isCounterInvoice = computed(() => hoaDonData.value?.loaiHoaDon === '0')
const isDeliveryInvoice = computed(() => hoaDonData.value?.loaiHoaDon === '2')
const isCounterOrDelivery = computed(() => isCounterInvoice.value || isDeliveryInvoice.value)

const currentStatus = computed(() => Number(hoaDonData.value?.trangThaiHoaDon || '0'))
const isCompleted = computed(() => currentStatus.value === 4)
const isCancelled = computed(() => currentStatus.value === 5)
const canChangeImei = computed(() => isOnlineInvoice.value && currentStatus.value === 0)

const showCancelButton = computed(() => {
  if (isCompleted.value || isCancelled.value)
    return false
  return currentStatus.value === 0
})

const productCount = computed(() => invoiceItems.value.filter(item => item?.tenSanPham && item?.soLuong != null).length)
const totalQuantity = computed(() => invoiceItems.value.reduce((sum, item) => sum + (item.soLuong || 0), 0))
const totalAmount = computed(() => invoiceItems.value.reduce((sum, item) => sum + (item.tongTien || 0), 0))
const imeiProductsCount = computed(() => {
  let totalImei = 0
  invoiceItems.value.forEach((item) => { totalImei += parseIMEIList(item.danhSachImei).length })
  return totalImei
})

// === TÁCH MỖI SERIAL THÀNH 1 DÒNG ===
const flattenedInvoiceItems = computed(() => {
  const result: any[] = []
  let stt = 1
  invoiceItems.value.forEach((item) => {
    const imeiList = parseIMEIList(item.danhSachImei)
    const totalQty = item.soLuong || 1

    for (let i = 0; i < totalQty; i++) {
      const assignedImei = imeiList[i] || null
      result.push({
        ...item,
        stt: stt++,
        uniqueKey: `${item.id}_${assignedImei ? assignedImei.id : `unassigned_${i}`}`,
        soLuongOriginal: item.soLuong,
        soLuong: 1,
        tongTien: item.giaBan,
        imeiAssigned: assignedImei,
        isAssigned: !!assignedImei,
      })
    }
  })
  return result
})

const printProducts = computed<PrintProduct[]>(() => {
  const result: PrintProduct[] = []
  invoiceItems.value.forEach((item) => {
    const imeiList = parseIMEIList(item.danhSachImei)
    if (imeiList.length > 0) {
      imeiList.forEach((imei) => {
        result.push({ ...item, imeiCode: imei.code || imei.imeiCode, soLuongImei: 1, giaBanImei: item.giaBan, tongTienImei: item.giaBan })
      })
    }
    else {
      result.push({ ...item, imeiCode: null, soLuongImei: item.soLuong, giaBanImei: item.giaBan, tongTienImei: item.tongTien })
    }
  })
  return result
})

// ==================== LOGIC TIẾN TRÌNH ====================
const filteredSteps = computed(() => {
  if (isCancelled.value)
    return TIMELINE_STEPS.filter(step => step.key === '0' || step.key === '5')
  if (isCounterInvoice.value)
    return TIMELINE_STEPS.filter(step => ['0', '4'].includes(step.key))
  if (isDeliveryInvoice.value)
    return TIMELINE_STEPS.filter(step => ['0', '1', '2', '3', '4'].includes(step.key))
  return TIMELINE_STEPS.filter(step => step.key !== '5')
})

const nextStatusToUpdate = computed<number | null>(() => {
  if (isCompleted.value || isCancelled.value)
    return null
  if (isCounterInvoice.value) {
    if (currentStatus.value === 0)
      return 4
    return null
  }
  else if (isDeliveryInvoice.value) {
    if (currentStatus.value === 0)
      return 1
    if (currentStatus.value === 1)
      return 2
    if (currentStatus.value === 2)
      return 3
    if (currentStatus.value === 3)
      return 4
    return null
  }
  else {
    if (currentStatus.value < 4)
      return currentStatus.value + 1
    return null
  }
})

const currentVisibleSteps = computed(() => filteredSteps.value.filter(step => Number(step.key) <= currentStatus.value || (isCancelled.value && step.key === '5')))

function getDynamicIconSizeClass(stepKey: string) {
  if (currentStatus.value.toString() === stepKey)
    return 'w-24 h-24 border-[6px] border-blue-500 bg-blue-50 text-blue-600 shadow-lg'
  return 'w-20 h-20 border-[5px] border-blue-500 bg-white text-blue-500 shadow-md'
}

function getDynamicIconSize(stepKey: string) { return currentStatus.value.toString() === stepKey ? 48 : 40 }
function getTimelineContainerWidth(totalVisible: number) {
  if (totalVisible === 1)
    return 'max-w-sm'
  if (totalVisible === 2)
    return 'max-w-2xl'
  if (totalVisible === 3)
    return 'max-w-4xl'
  if (totalVisible === 4)
    return 'max-w-6xl'
  return 'w-full'
}

const historyList = computed<HistoryItem[]>(() => {
  if (!hoaDonData.value?.lichSuTrangThai)
    return []
  try { return [...JSON.parse(hoaDonData.value.lichSuTrangThai)].reverse() }
  catch { return [] }
})

// ==================== BẢNG CHỌN SERIAL (TRONG MODAL) ====================
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
          if (selectedSerialIds.value.length < requiredQuantityToAssign.value) {
            selectedSerialIds.value = [...selectedSerialIds.value, row.id]
          }
          else {
            message.warning(`Chỉ được chọn tối đa ${requiredQuantityToAssign.value} serial`)
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
    flex: 1,
    render: row => h(NText, { strong: true, code: true, style: { fontFamily: 'monospace', fontSize: '14px', letterSpacing: '1px' } }, () => row.code || '-'),
  },
  { title: 'Tên / Kho', key: 'name', width: 150, render: row => h(NText, () => row.name || '-') },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 120,
    align: 'center',
    render: (row) => {
      const config = IMEI_STATUS_CONFIG[row.imeiStatus] || { type: 'default', text: row.imeiStatus || 'Không xác định' }
      return h(NTag, { type: config.type, size: 'small', round: true }, () => config.text)
    },
  },
]

// ==================== BẢNG SẢN PHẨM - TAB DANH SÁCH (có cột thao tác) ====================
const productColumns = computed<DataTableColumns<any>>(() => {
  return [
    {
      title: 'STT',
      key: 'stt',
      width: 60,
      align: 'center',
      render: row => h('span', { class: 'font-medium text-gray-500' }, row.stt),
    },
    {
      title: 'Sản phẩm',
      key: 'productInfo',
      minWidth: 300,
      align: 'center',
      titleAlign: 'center',
      render: (row) => {
        if (!row.tenSanPham)
          return h('div', { class: 'hidden' })
        return h('div', { class: 'flex items-center justify-center gap-4 py-1' }, [
          h(NAvatar, {
            src: row.anhSanPham,
            size: 'large',
            round: false,
            class: 'border border-gray-200 rounded-md shadow-sm bg-white',
            fallbackSrc: 'https://via.placeholder.com/40?text=No+Image',
          }),
          h('div', { class: 'min-w-0 flex flex-col items-start' }, [
            h('div', { class: 'font-bold text-gray-900 text-sm truncate' }, row.tenSanPham),
            h('div', { class: 'flex flex-wrap items-center gap-2 mt-1.5' }, [
              row.thuongHieu && h(NTag, { size: 'tiny', type: 'info', bordered: false }, { default: () => row.thuongHieu }),
              row.mauSac && h(NTag, { size: 'tiny', type: 'default', bordered: false }, { default: () => row.mauSac }),
              row.size && h('span', { class: 'text-xs text-gray-500' }, `| Size: ${row.size}`),
            ]),
          ]),
        ])
      },
    },
    {
      title: 'Mã Serial',
      key: 'serialCode',
      width: 180,
      align: 'center',
      render: (row) => {
        if (row.isAssigned) {
          return h('span', { class: 'font-mono font-bold text-indigo-600 tracking-wide' }, row.imeiAssigned.code || row.imeiAssigned.imeiCode)
        }
        return h(NTag, { type: 'warning', size: 'small', round: true }, { default: () => 'Chưa có mã' })
      },
    },
    {
      title: 'Đơn giá',
      key: 'price',
      width: 140,
      align: 'right',
      render: row => h('div', { class: 'font-medium text-gray-600' }, formatCurrency(row.giaBan)),
    },
    {
      title: 'Thành tiền',
      key: 'total',
      width: 140,
      align: 'right',
      render: row => h('div', { class: 'font-bold text-red-600 text-base' }, formatCurrency(row.giaBan)),
    },
    // ✅ Cột Thao tác: Bổ sung Serial (chưa có) hoặc Thay đổi Serial (đã có)
    {
      title: 'Thao tác',
      key: 'action',
      width: 170,
      align: 'center',
      render: (row) => {
        // Không phải online invoice hoặc không được phép thao tác → hiện dấu gạch
        if (!isOnlineInvoice.value || !canChangeImei.value) {
          return h('span', { class: 'text-gray-300 text-sm' }, '—')
        }

        const originalProduct = invoiceItems.value.find(p => p.id === row.id)
        if (!originalProduct)
          return h('span', { class: 'text-gray-300' }, '—')

        if (row.isAssigned) {
          // ✅ Đã có serial → nút "Thay đổi Serial" (màu cam)
          return h(NButton, {
            size: 'small',
            type: 'warning',
            round: true,
            onClick: () => openSerialChangeModal(originalProduct, row.imeiAssigned),
          }, {
            icon: () => h(NIcon, { size: 15 }, { default: () => h(RefreshOutline) }),
            default: () => 'Thay đổi Serial',
          })
        }

        // ✅ Chưa có serial → nút "Bổ sung Serial" (màu xanh)
        return h(NButton, {
          size: 'small',
          type: 'primary',
          round: true,
          onClick: () => openSerialSelectionModal(originalProduct),
        }, {
          icon: () => h(NIcon, { size: 15 }, { default: () => h(ScanOutline) }),
          default: () => 'Bổ sung Serial',
        })
      },
    },
  ]
})

// ==================== BẢNG SẢN PHẨM - TAB CHI TIẾT SERIAL ====================
const productColumnsSerialTong = computed<DataTableColumns<HoaDonChiTietItem>>(() => {
  return [
    {
      title: 'STT',
      key: 'stt',
      width: 60,
      align: 'center',
      render: (_, index) => h('span', { class: 'font-medium text-gray-500' }, index + 1),
    },
    {
      title: 'Sản phẩm',
      key: 'productInfo',
      minWidth: 300,
      align: 'center',
      titleAlign: 'center',
      render: (row) => {
        if (!row.tenSanPham)
          return h('div', { class: 'hidden' })
        return h('div', { class: 'flex items-center justify-center gap-4 py-1' }, [
          h(NAvatar, {
            src: row.anhSanPham,
            size: 'large',
            round: false,
            class: 'border border-gray-200 rounded-md shadow-sm bg-white',
            fallbackSrc: 'https://via.placeholder.com/40?text=No+Image',
          }),
          h('div', { class: 'min-w-0' }, [
            h('div', { class: 'font-bold text-gray-900 text-sm truncate' }, row.tenSanPham),
            h('div', { class: 'flex flex-wrap items-center gap-2 mt-1.5' }, [
              row.thuongHieu && h(NTag, { size: 'tiny', type: 'info', bordered: false }, { default: () => row.thuongHieu }),
              row.mauSac && h(NTag, { size: 'tiny', type: 'default', bordered: false }, { default: () => row.mauSac }),
              row.size && h('span', { class: 'text-xs text-gray-500' }, `| Size: ${row.size}`),
            ]),
          ]),
        ])
      },
    },
    {
      title: 'Số lượng',
      key: 'soLuong',
      width: 140,
      align: 'center',
      render: row => h('span', { class: 'font-bold text-gray-800 text-base' }, row.soLuong),
    },
    {
      title: 'Đơn giá',
      key: 'price',
      width: 180,
      align: 'right',
      render: row => h('div', { class: 'font-medium text-gray-600' }, formatCurrency(row.giaBan)),
    },
    {
      title: 'Thành tiền',
      key: 'total',
      width: 180,
      align: 'right',
      render: row => h('div', { class: 'font-bold text-red-600 text-base' }, formatCurrency(row.tongTien)),
    },
    {
      title: 'Thao tác',
      key: 'action',
      width: 120,
      align: 'center',
      render: (row) => {
        return h(NButton, {
          size: 'small',
          quaternary: true,
          type: 'info',
          class: 'hover:bg-blue-50',
          onClick: () => openSerialInfoModal(row),
        }, {
          icon: () => h(NIcon, { size: 20 }, { default: () => h(EyeOutline) }),
        })
      },
    },
  ]
})

const customerFormRules: FormRules = {
  tenKhachHang: [{ required: true, message: 'Vui lòng nhập họ và tên', trigger: ['blur', 'input'] }],
  sdtKH: [
    { required: true, message: 'Vui lòng nhập số điện thoại', trigger: ['blur', 'input'] },
    { pattern: /(84|0[3|5789])+(\d{8})\b/, message: 'Số điện thoại không hợp lệ', trigger: ['blur', 'input'] },
  ],
  email: [{ type: 'email', message: 'Email không hợp lệ', trigger: ['blur', 'input'] }],
}

// ==================== Helper Formatting Functions ====================
function formatCurrency(value: number | undefined | null): string {
  if (value === undefined || value === null)
    return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(value)
}

function formatDateTime(timestamp: number | undefined): string {
  if (!timestamp)
    return 'N/A'
  try {
    return new Date(timestamp).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
  }
  catch { return 'N/A' }
}

function formatHistoryTime(timeStr: string | undefined): string {
  if (!timeStr)
    return '---'
  const cleanTime = timeStr.split('.')[0]
  const parts = cleanTime.split(' ')
  if (parts.length === 2) {
    const dateParts = parts[0].split('-')
    if (dateParts.length === 3)
      return `${parts[1]} - ${dateParts[2]}-${dateParts[1]}-${dateParts[0]}`
  }
  return cleanTime
}

const formatAddress = (address: string | undefined): string => address || ''
function getStatusText(status: string | number | undefined): string { return STATUS_MAP[Number(status)]?.label || 'Không xác định' }
function getStatusTagType(status: string | number | undefined): string { return STATUS_MAP[Number(status)]?.type || 'default' }
function getStatusIcon(status: string | number | undefined): any { return STATUS_MAP[Number(status)]?.icon || TimeOutline }
function getCurrentStatusTextClass(): string {
  const classMap: Record<number, string> = { 0: 'text-yellow-600', 1: 'text-blue-600', 2: 'text-purple-600', 3: 'text-blue-600', 4: 'text-green-600', 5: 'text-red-600' }
  return classMap[currentStatus.value] || 'text-gray-600'
}
function getCurrentStepIcon(): any { return getStatusIcon(currentStatus.value) }
function getInvoiceTypeText(type: string | undefined): string { return INVOICE_TYPE_MAP[type || '0']?.text || 'Không xác định' }
function getInvoiceTypeTagType(type: string | undefined): string { return INVOICE_TYPE_MAP[type || '0']?.type || 'default' }
function getPaymentMethodText(method: string | undefined): string {
  if (!method)
    return 'Chưa xác định' // ← đổi fallback, không ra "Tiền mặt" nữa
  return PAYMENT_METHOD_MAP[method] || method
}
function getPaymentStatusText(): string { return hoaDonData.value?.duNo ? 'Còn nợ' : 'Đã thanh toán' }
function getPaymentStatusTagType(): string { return hoaDonData.value?.duNo ? 'warning' : 'success' }
function getStepCircleBg(status: string | number | undefined): string {
  const map: Record<number, string> = { 0: 'bg-yellow-100', 1: 'bg-blue-100', 2: 'bg-purple-100', 3: 'bg-blue-100', 4: 'bg-green-100', 5: 'bg-red-100' }
  return map[Number(status)] || 'bg-gray-100'
}
function getStepIconClass(stepKey: string): string {
  if (currentStatus.value.toString() === stepKey)
    return 'text-blue-500'
  if (Number(stepKey) < currentStatus.value && stepKey !== '5')
    return 'text-green-500'
  return 'text-gray-400'
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
  catch { return null }
}

// ==================== Modal Handlers ====================
function openStatusModalNext(): void {
  if (nextStatusToUpdate.value !== null) {
    if (currentStatus.value === 0 && nextStatusToUpdate.value === 1) {
      const isMissingSerial = invoiceItems.value.some((product) => {
        const assignedCount = parseIMEIList(product.danhSachImei).length
        const requiredCount = product.soLuong || 0
        return assignedCount < requiredCount
      })
      if (isMissingSerial) {
        message.error('Vui lòng gán đủ Serial cho tất cả sản phẩm trước khi xác nhận đơn hàng!')
        return
      }
    }
    selectedStatus.value = nextStatusToUpdate.value
    showStatusModal.value = true
  }
}

// ✅ Mở modal BỔ SUNG serial (chưa có serial)
async function openSerialSelectionModal(product: HoaDonChiTietItem) {
  if (!isOnlineInvoice.value)
    return

  // Reset changingImei để đảm bảo là luồng bổ sung
  changingImei.value = null

  const existingImeis = parseIMEIList(product.danhSachImei)
  alreadyAssignedCount.value = existingImeis.length
  requiredQuantityToAssign.value = (product.soLuong || 0) - alreadyAssignedCount.value

  if (requiredQuantityToAssign.value <= 0) {
    dialog.info({ title: 'Thông báo', content: `Sản phẩm này đã gán đủ ${existingImeis.length} serial.`, positiveText: 'Đóng' })
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
          s.imeiStatus === 'AVAILABLE' && !existingImeis.some(imei => imei.id === s.id),
        )
        selectedSerials.value = availableSerials
        showSerialModal.value = true
      }
      else { message.error(response.message || 'Không thể tải danh sách serial') }
    }
    else { message.error('Không tìm thấy thông tin chi tiết của sản phẩm') }
  }
  catch (error) { message.error('Đã xảy ra lỗi khi tải danh sách serial') }
  finally { loadingSerials.value = false }
}

async function openSerialChangeModal(product: HoaDonChiTietItem, currentImei: IMEIItem) {
  changingImei.value = currentImei
  requiredQuantityToAssign.value = 1
  // ✅ Không dùng alreadyAssignedCount cho luồng thay đổi nữa
  alreadyAssignedCount.value = parseIMEIList(product.danhSachImei).length

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
        const existingImeis = parseIMEIList(product.danhSachImei)

        // ✅ Lọc bỏ serial đã gán NGOẠI TRỪ serial đang thay thế
        // (loại bỏ tất cả serial trong existingImeis TRỪ currentImei.id)
        const otherAssignedImeis = existingImeis.filter(imei => imei.id !== currentImei.id)

        const availableSerials = response.data.filter(s =>
          s.imeiStatus === 'AVAILABLE'
          && !otherAssignedImeis.some(imei => imei.id === s.id),
        )
        selectedSerials.value = availableSerials
        showSerialModal.value = true
      }
      else { message.error(response.message || 'Không thể tải danh sách serial') }
    }
    else { message.error('Không tìm thấy thông tin chi tiết của sản phẩm') }
  }
  catch (error) { message.error('Đã xảy ra lỗi khi tải danh sách serial') }
  finally { loadingSerials.value = false }
}

function openSerialInfoModal(product: HoaDonChiTietItem) {
  selectedSerialInfoProduct.value = product
  showSerialInfoModal.value = true
}

function startScanMode() {
  scanMode.value = true
  setTimeout(() => { scanInputRef.value?.focus() }, 100)
}

function handleScanInput() {
  if (scannedSerial.value) {
    const matchedSerial = selectedSerials.value.find(s => s.code?.toLowerCase() === scannedSerial.value.toLowerCase() && s.imeiStatus === 'AVAILABLE')
    if (matchedSerial) {
      if (!selectedSerialIds.value.includes(matchedSerial.id)) {
        if (selectedSerialIds.value.length < requiredQuantityToAssign.value) {
          selectedSerialIds.value = [...selectedSerialIds.value, matchedSerial.id]
          message.success(`Đã chọn serial ${matchedSerial.code}`)
        }
        else { message.warning(`Bạn chỉ cần chọn ${requiredQuantityToAssign.value} serial!`) }
      }
      else { message.warning('Serial này đã nằm trong danh sách đang chọn') }
    }
    else { message.error('Không tìm thấy serial khả dụng (Hoặc serial đã được gán)') }
    scannedSerial.value = ''
  }
}

// ✅ Hàm lưu serial - xử lý cả 2 luồng: bổ sung và thay đổi
async function addSerialsToInvoice() {
  if (!selectedProductItem.value || selectedSerialIds.value.length === 0) {
    message.error('Vui lòng chọn ít nhất một serial')
    return
  }

  isAddingSerials.value = true
  try {
    let response

    if (changingImei.value) {
      // ✅ Gọi API ĐỔI IMEI riêng — truyền 1 newImeiId (không phải array)
      response = await doiImei({
        hoaDonChiTietId: selectedProductItem.value.id,
        oldImeiId: changingImei.value.id,
        newImeiId: selectedSerialIds.value[0], // Chỉ lấy phần tử đầu tiên
      })
    }
    else {
      // Luồng BỔ SUNG bình thường
      response = await ganImei({
        hoaDonChiTietId: selectedProductItem.value.id,
        imeiIds: selectedSerialIds.value,
      })
    }

    if (response?.status === 'OK') {
      const actionText = changingImei.value ? 'Thay đổi' : 'Gán'
      message.success(`${actionText} serial thành công`)
      showSerialModal.value = false
      changingImei.value = null
      await fetchInvoiceDetails()
    }
    else {
      message.error(response?.message || 'Thao tác thất bại')
    }
  }
  catch (error: any) {
    message.error(error.response?.data?.message || 'Đã xảy ra lỗi hệ thống')
  }
  finally {
    isAddingSerials.value = false
  }
}

// ✅ Reset changingImei khi đóng modal
function handleSerialModalClose(val: boolean) {
  if (!val) {
    changingImei.value = null
    selectedSerialIds.value = []
    scanMode.value = false
    scannedSerial.value = ''
  }
}

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

async function saveCustomerInfo() {
  customerFormRef.value?.validate(async (errors) => {
    if (!errors) {
      isSavingCustomer.value = true
      try {
        const response = await updateCustomerInvoice({
          maHoaDon: invoiceCode.value,
          tenKhachHang: customerForm.tenKhachHang,
          sdtKH: customerForm.sdtKH,
          email: customerForm.email,
          diaChi: customerForm.diaChi,
        })

        if (response.success || response.status === 'OK') {
          emit('update:customer', { ...customerForm })
          showCustomerModal.value = false
          message.success('Cập nhật thông tin khách hàng thành công')
          await fetchInvoiceDetails() // ← Reload lại UI
        }
        else {
          message.error(response.message || 'Cập nhật thất bại')
        }
      }
      catch (error: any) {
        message.error(error?.response?.data?.message || 'Đã xảy ra lỗi')
      }
      finally {
        isSavingCustomer.value = false
      }
    }
  })
}

async function confirmStatusUpdate(): Promise<void> {
  if (selectedStatus.value === null || !hoaDonData.value?.maHoaDon) {
    message.error('Vui lòng chọn trạng thái mới')
    return
  }

  // ✅ Bắt buộc nhập lý do khi hủy đơn
  if (selectedStatus.value === 5 && !statusNote.value.trim()) {
    message.error('Vui lòng nhập lý do hủy đơn hàng')
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
      message.success(selectedStatus.value === 5 ? 'Đã hủy đơn hàng thành công' : 'Cập nhật trạng thái thành công')
      await fetchInvoiceDetails()
      selectedStatus.value = null
      statusNote.value = ''
      showStatusModal.value = false
    }
    else { message.error(response.message || 'Cập nhật thất bại') }
  }
  catch (error: any) { message.error(error.message || 'Đã xảy ra lỗi khi cập nhật') }
  finally { isUpdating.value = false }
}

function openCancelModal(): void {
  if (isCancelled.value) { message.warning('Đơn hàng đã bị hủy'); return }
  // Tái sử dụng modal chuyển trạng thái, set sẵn status = 5 (Đã hủy)
  selectedStatus.value = 5
  statusNote.value = ''
  showStatusModal.value = true
}

// ==================== API Functions ====================
async function fetchInvoiceDetails(): Promise<void> {
  try {
    isLoading.value = true
    const response = await getHoaDonChiTiets({ maHoaDon: invoiceCode.value, page: 0, size: 100 })
    if (response.success && response.data?.content) {
      invoiceItems.value = response.data.content
    }
    else { message.error(response.message || 'Không thể tải chi tiết hóa đơn') }
  }
  catch (error: any) { message.error(error.message || 'Đã xảy ra lỗi khi tải dữ liệu') }
  finally { isLoading.value = false }
}

async function generateQRCode(): Promise<void> {
  if (!hoaDonData.value)
    return
  try {
    const invoiceInfo = `MÃ HĐ: ${hoaDonData.value.maHoaDon}\nNGÀY: ${formatDateTime(hoaDonData.value.ngayTao)}\nKH: ${hoaDonData.value.tenKhachHang2 || hoaDonData.value.tenKhachHang || 'Khách lẻ'}\nTỔNG: ${formatCurrency(hoaDonData.value.tongTienSauGiam)}\nURL: ${window.location.origin}/admin/hoa-don/${hoaDonData.value.maHoaDon}`.trim()
    const canvas = document.createElement('canvas')
    await QRCode.toCanvas(canvas, invoiceInfo, { width: 250, margin: 2, errorCorrectionLevel: 'H' })
    qrCodeDataUrl.value = canvas.toDataURL('image/png')
  }
  catch (error) { console.error('Error generating QR code:', error) }
}

async function handlePrint() {
  if (!hoaDonData.value)
    return
  printLoading.value = true
  await generateQRCode()
  const invoiceContent = document.getElementById('invoice-content')
  if (!invoiceContent) { message.error('Không tìm thấy nội dung hóa đơn'); printLoading.value = false; return }
  const printWindow = window.open('', '_blank')
  if (!printWindow) { message.error('Trình duyệt đã chặn popup. Vui lòng cho phép popup.'); printLoading.value = false; return }
  const printStyles = `<style>* { box-sizing: border-box; margin: 0; padding: 0; } body { font-family: Arial, sans-serif; background: white; color: #1a1a1a; padding: 24px; } table { width: 100%; border-collapse: collapse; } th { background-color: #16a34a !important; color: #fff !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; } tr:nth-child(even) td { background-color: #f9fafb !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; } @media print { body { padding: 0; } th { background-color: #16a34a !important; color: #fff !important; -webkit-print-color-adjust: exact; print-color-adjust: exact; } }</style>`
  printWindow.document.write(`<!DOCTYPE html><html><head><title>HoaDon_${hoaDonData.value.maHoaDon || invoiceCode.value}</title>${printStyles}</head><body>${invoiceContent.innerHTML}<script>window.onload = function() { window.print(); }<\/script></body></html>`)
  printWindow.document.close()
  printLoading.value = false
}

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

onMounted(async () => {
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

fun<template>
  <div class="container mx-auto px-4 py-6 space-y-6">
    <!-- ==================== HEADER ==================== -->
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
          </template> Chi tiết
        </NButton>
        <NButton type="primary" :loading="printLoading" @click="handlePrint">
          <template #icon>
            <NIcon><PrintOutline /></NIcon>
          </template> In hóa đơn
        </NButton>
        <NButton type="default" @click="handleBack">
          <template #icon>
            <NIcon><ArrowBackOutline /></NIcon>
          </template> Quay lại
        </NButton>
      </div>
    </div>

    <!-- ==================== NỘI DUNG IN ==================== -->
    <div id="invoice-content" class="hidden">
      <div style="font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; background: #fff; padding: 32px; color: #1a1a1a;">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px;">
          <div style="display: flex; align-items: center; gap: 12px;">
            <img src="@/assets/svg-icons/logo.svg" style="width: 64px; height: 64px;" alt="logo">
            <div>
              <div style="font-size: 22px; font-weight: 800; color: #16a34a; letter-spacing: 1px;">
                My Laptop
              </div>
              <div style="font-size: 11px; color: #6b7280; margin-top: 2px;">
                123 Đại Cồ Việt, Hai Bà Trưng, Hà Nội
              </div>
              <div style="font-size: 11px; color: #6b7280;">
                SĐT: 1900.8888 | mylaptop.vn
              </div>
            </div>
          </div>
          <div style="text-align: right; display: flex; align-items: flex-start; gap: 16px;">
            <div>
              <div style="font-size: 20px; font-weight: 900; color: #16a34a; text-transform: uppercase; letter-spacing: 2px;">
                Hóa Đơn Bán Hàng
              </div>
              <div style="font-size: 12px; color: #374151; margin-top: 6px;">
                <span style="font-weight: 600;">Mã đơn hàng:</span> {{ hoaDonData?.maHoaDon || invoiceCode }}
              </div>
              <div style="font-size: 12px; color: #374151; margin-top: 2px;">
                <span style="font-weight: 600;">Ngày đặt:</span> {{ formatDateTime(hoaDonData?.ngayTao) }}
              </div>
            </div>
            <div v-if="qrCodeDataUrl" style="text-align: center;">
              <img :src="qrCodeDataUrl" alt="QR" style="width: 80px; height: 80px; border: 1px solid #e5e7eb; border-radius: 4px; padding: 2px;">
              <div style="font-size: 8px; color: #9ca3af; margin-top: 2px;">
                Mã hóa đơn
              </div>
            </div>
          </div>
        </div>
        <div style="border-top: 3px solid #16a34a; margin-bottom: 20px;" />
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px;">
          <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px;">
            <div style="font-size: 12px; font-weight: 700; color: #16a34a; text-transform: uppercase; margin-bottom: 10px; border-bottom: 1px solid #d1fae5; padding-bottom: 6px;">
              Thông tin khách hàng
            </div>
            <div style="font-size: 12px; line-height: 1.9; color: #374151;">
              <div><span style="font-weight: 600;">Tên khách hàng:</span> {{ hoaDonData?.tenKhachHang2 || hoaDonData?.tenKhachHang || 'Khách lẻ' }}</div>
              <div><span style="font-weight: 600;">Số điện thoại:</span> {{ hoaDonData?.sdtKH2 || hoaDonData?.sdtKH || 'Chưa cập nhật' }}</div>
              <div><span style="font-weight: 600;">Địa chỉ:</span> {{ formatAddress(hoaDonData?.diaChi) || formatAddress(hoaDonData?.diaChi2) || 'Không có' }}</div>
              <div><span style="font-weight: 600;">Email:</span> {{ hoaDonData?.email2 || hoaDonData?.email || 'Không có' }}</div>
            </div>
          </div>
          <div style="border: 1px solid #e5e7eb; border-radius: 8px; padding: 14px;">
            <div style="font-size: 12px; font-weight: 700; color: #16a34a; text-transform: uppercase; margin-bottom: 10px; border-bottom: 1px solid #d1fae5; padding-bottom: 6px;">
              Thông tin đơn hàng
            </div>
            <div style="font-size: 12px; line-height: 1.9; color: #374151;">
              <div><span style="font-weight: 600;">Loại đơn:</span> {{ getInvoiceTypeText(hoaDonData?.loaiHoaDon) }}</div>
              <div><span style="font-weight: 600;">Thanh toán:</span> {{ getPaymentMethodText(hoaDonData?.phuongThucThanhToan) }}</div>
              <div v-if="hoaDonData?.maVoucher">
                <span style="font-weight: 600;">Voucher:</span> {{ hoaDonData.maVoucher }}
              </div>
              <div><span style="font-weight: 600;">Nhân viên:</span> {{ hoaDonData?.tenNhanVien || 'Không xác định' }}</div>
            </div>
          </div>
        </div>
        <table style="width: 100%; border-collapse: collapse; font-size: 12px; margin-bottom: 20px;">
          <thead>
            <tr style="background-color: #16a34a; color: #fff;">
              <th style="padding: 10px 8px; text-align: center; width: 40px;">
                STT
              </th>
              <th style="padding: 10px 8px; text-align: left;">
                Tên sản phẩm
              </th>
              <th style="padding: 10px 8px; text-align: center; width: 140px;">
                Serial
              </th>
              <th style="padding: 10px 8px; text-align: right; width: 110px;">
                Đơn giá (đ)
              </th>
              <th style="padding: 10px 8px; text-align: right; width: 120px;">
                Thành tiền (đ)
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, idx) in printProducts" :key="item.id + (item.imeiCode || '')" :style="{ backgroundColor: idx % 2 === 0 ? '#f9fafb' : '#ffffff' }">
              <td style="padding: 10px 8px; text-align: center; border-bottom: 1px solid #e5e7eb; color: #6b7280;">
                {{ idx + 1 }}
              </td>
              <td style="padding: 10px 8px; border-bottom: 1px solid #e5e7eb;">
                <div style="font-weight: 700; color: #111827;">
                  {{ item.tenSanPham }}
                </div>
                <div style="font-size: 11px; color: #6b7280; margin-top: 2px;">
                  <span v-if="item.thuongHieu">{{ item.thuongHieu }}</span><span v-if="item.mauSac"> | {{ item.mauSac }}</span><span v-if="item.size"> | {{ item.size }}</span>
                </div>
              </td>
              <td style="padding: 10px 8px; text-align: center; border-bottom: 1px solid #e5e7eb; font-family: monospace; font-weight: 600; color: #374151;">
                {{ item.imeiCode || '—' }}
              </td>
              <td style="padding: 10px 8px; text-align: right; border-bottom: 1px solid #e5e7eb; color: #374151;">
                {{ formatCurrency(item.giaBanImei || item.giaBan) }}
              </td>
              <td style="padding: 10px 8px; text-align: right; border-bottom: 1px solid #e5e7eb; font-weight: 700;">
                {{ formatCurrency(item.tongTienImei || item.tongTien) }}
              </td>
            </tr>
          </tbody>
        </table>
        <div style="display: flex; justify-content: flex-end; margin-bottom: 20px;">
          <div style="width: 280px; font-size: 13px;">
            <div style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #e5e7eb; color: #6b7280;">
              <span>Tổng tiền hàng:</span><span style="font-weight: 600; color: #111827;">{{ formatCurrency(totalAmount) }}</span>
            </div>
            <div v-if="hoaDonData?.phiVanChuyen" style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #e5e7eb; color: #6b7280;">
              <span>Phí vận chuyển:</span><span style="font-weight: 600; color: #111827;">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>
            <div v-if="hoaDonData?.giaTriVoucher" style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #e5e7eb; color: #16a34a;">
              <span>Giảm giá:</span><span style="font-weight: 700;">-{{ formatCurrency(Math.abs(hoaDonData.giaTriVoucher)) }}</span>
            </div>
            <div v-if="hoaDonData?.duNo" style="display: flex; justify-content: space-between; padding: 6px 0; border-bottom: 1px solid #e5e7eb; color: #d97706;">
              <span>Còn nợ:</span><span style="font-weight: 700;">{{ formatCurrency(hoaDonData.duNo) }}</span>
            </div>
            <div style="display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; margin-top: 6px; background: #f0fdf4; border: 2px solid #16a34a; border-radius: 8px;">
              <span style="font-weight: 800; font-size: 14px;">Thành tiền:</span><span style="font-weight: 900; font-size: 18px; color: #16a34a;">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>
          </div>
        </div>
        <div style="border-top: 1px solid #e5e7eb; padding-top: 14px; text-align: center;">
          <div style="font-size: 13px; font-weight: 700; color: #16a34a; margin-bottom: 4px;">
            Cảm ơn quý khách đã tin tưởng My Laptop!
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== TIẾN TRÌNH ĐƠN HÀNG ==================== -->
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
            <NIcon><CloseCircleOutline /></NIcon>
          </template> Hủy đơn hàng
        </NButton>
      </template>

      <div class="pt-2 pb-0 w-full">
        <div
          class="mx-auto flex justify-between items-start transition-all duration-500"
          :class="getTimelineContainerWidth(currentVisibleSteps.length)"
        >
          <template v-for="(step, index) in currentVisibleSteps" :key="step.key">
            <div class="relative flex flex-col items-center flex-1">
              <div
                v-if="index < currentVisibleSteps.length - 1"
                class="absolute h-[6px] bg-blue-500 z-0 left-[50%] w-full"
                style="top: 45px;"
              />
              <div class="h-24 flex items-center justify-center relative z-10 w-full">
                <div
                  class="rounded-full flex items-center justify-center transition-all duration-500 relative"
                  :class="getDynamicIconSizeClass(step.key)"
                >
                  <NIcon :size="getDynamicIconSize(step.key)">
                    <component :is="step.icon" />
                  </NIcon>
                  <div
                    v-if="index < currentVisibleSteps.length - 1 && !isCancelled"
                    class="absolute bottom-0 right-0 bg-green-500 rounded-full flex items-center justify-center border-2 border-white shadow-sm w-7 h-7"
                  >
                    <NIcon size="16" color="white">
                      <CheckmarkCircleOutline />
                    </NIcon>
                  </div>
                </div>
              </div>
              <div class="text-center mt-3 px-2">
                <p class="font-bold text-blue-600 text-sm md:text-base leading-tight">
                  {{ step.title }}
                </p>
                <p class="text-xs text-gray-500 mt-1">
                  {{ getStepTime(step.key) || 'Vừa xong' }}
                </p>
              </div>
              <div
                v-if="index === currentVisibleSteps.length - 1 && nextStatusToUpdate !== null"
                class="mt-3 w-full flex justify-center pb-2"
              >
                <Transition name="fade">
                  <NButton
                    type="primary"
                    size="large"
                    round
                    class="shadow-md hover:-translate-y-1 transition-transform font-bold whitespace-nowrap"
                    @click="openStatusModalNext"
                  >
                    Chuyển: {{ getStatusText(nextStatusToUpdate) }}
                  </NButton>
                </Transition>
              </div>
            </div>
          </template>
        </div>
      </div>

      <div class="mt-4 pt-6 border-t border-gray-100">
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

    <!-- ==================== 3 CARD THÔNG TIN ==================== -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 no-print">
      <!-- Thông tin khách hàng -->
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
            <NButton v-if="currentStatus === 0" type="primary" tertiary size="small" @click="openCustomerEditModal">
              <template #icon>
                <NIcon><CreateOutline /></NIcon>
              </template> Chỉnh sửa
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
                  <NIcon size="14"><CallOutline /></NIcon> {{ hoaDonData?.sdtKH2 || hoaDonData?.sdtKH || 'Chưa cập nhật' }}
                </span>
                <span class="inline-flex items-center gap-1 text-sm text-gray-600">
                  <NIcon size="14"><MailOutline /></NIcon> {{ hoaDonData?.email2 || hoaDonData?.email || 'Chưa có email' }}
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

      <!-- Tóm tắt đơn hàng -->
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
              <p class="text-lg font-bold text-gray-900">
                {{ productCount }} sản phẩm
              </p>
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
              <span class="text-gray-600">Tổng tiền hàng:</span><span class="font-semibold">{{ formatCurrency(totalAmount) }}</span>
            </div>
            <div v-if="hoaDonData?.phiVanChuyen" class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Phí vận chuyển:</span><span class="font-semibold">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>
            <div v-if="hoaDonData?.giaTriVoucher && hoaDonData?.maVoucher" class="flex justify-between items-center py-2 border-b border-gray-100">
              <span class="text-gray-600">Giảm giá voucher:</span><span class="font-semibold text-green-600">-{{ formatCurrency(Math.abs(hoaDonData.giaTriVoucher)) }}</span>
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
              Giảm: {{ formatCurrency(Math.abs(hoaDonData.giaTriVoucher)) }}
            </p>
          </div>
        </div>
      </NCard>

      <!-- Thông tin thanh toán -->
      <NCard class="shadow-sm border-0 rounded-xl" content-class="p-6">
        <template #header>
          <div class="flex items-center gap-2">
            <NIcon size="20" color="#3b82f6">
              <component :is="getPaymentMethodIcon(hoaDonData?.phuongThucThanhToan)" />
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
                  <component :is="getPaymentMethodIcon(hoaDonData?.phuongThucThanhToan)" />
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

    <!-- ==================== DANH SÁCH SẢN PHẨM ==================== -->
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
          <NRadioGroup v-model:value="productViewMode" size="small">
            <NRadioButton value="products">
              <div class="flex items-center gap-1 px-2">
                <NIcon><ListOutline /></NIcon> Danh sách
              </div>
            </NRadioButton>
            <NRadioButton value="serials">
              <div class="flex items-center gap-1 px-2">
                <NIcon><QrCodeOutline /></NIcon> Chi tiết Serial
              </div>
            </NRadioButton>
          </NRadioGroup>
        </div>
      </template>

      <!-- Tab: Danh sách (có cột thao tác Bổ sung / Thay đổi Serial) -->
      <div v-if="productViewMode === 'products'">
        <div class="overflow-x-auto">
          <NDataTable
            :columns="productColumns"
            :data="flattenedInvoiceItems"
            :pagination="false"
            striped
            class="min-w-full border-b border-gray-200"
            :row-class-name="() => 'hover:bg-gray-50/80'"
          />
        </div>
        <div class="p-6 bg-gray-50/30">
          <div class="max-w-md ml-auto space-y-3">
            <div class="flex justify-between items-center">
              <span class="text-gray-600">Tạm tính:</span>
              <span class="font-medium">{{ formatCurrency(totalAmount) }}</span>
            </div>
            <div v-if="hoaDonData?.phiVanChuyen" class="flex justify-between items-center">
              <span class="text-gray-600">Phí vận chuyển:</span>
              <span class="font-medium">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
            </div>
            <div v-if="hoaDonData?.giaTriVoucher && hoaDonData?.maVoucher" class="flex justify-between items-center">
              <span class="text-gray-600">Giảm giá voucher:</span>
              <span class="font-medium text-green-600">-{{ formatCurrency(Math.abs(hoaDonData.giaTriVoucher)) }}</span>
            </div>
            <div class="flex justify-between items-center pt-3 border-t border-gray-200">
              <span class="text-lg font-bold text-gray-900">Tổng cộng:</span>
              <span class="text-xl font-bold text-red-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Tab: Chi tiết Serial (không có nút bổ sung, chỉ xem) -->
      <div v-else class="p-6 bg-gray-50/70">
        <div v-if="invoiceItems.length === 0" class="text-center py-16 bg-white rounded-2xl border border-gray-200 shadow-sm">
          <NIcon size="56" class="text-gray-300 mb-4">
            <CubeOutline />
          </NIcon>
          <p class="text-gray-500 text-lg font-medium">
            Không có sản phẩm nào trong hóa đơn
          </p>
        </div>

        <div v-else class="rounded-xl bg-white shadow-sm overflow-hidden">
          <div class="overflow-x-auto">
            <NDataTable
              :columns="productColumnsSerialTong"
              :data="invoiceItems"
              :pagination="false"
              striped
              class="min-w-full border-b border-gray-200"
              :row-class-name="() => 'hover:bg-gray-50/80'"
            />
          </div>
          <div class="p-6 bg-gray-50/30">
            <!-- Chỉ hiển thị tổng số serial, không có nút bổ sung -->
            <div class="flex items-center mb-4">
              <NTag size="small" type="success" round class="font-medium px-3">
                Tổng {{ imeiProductsCount }} SERIAL đã gán
              </NTag>
            </div>
            <div class="max-w-md ml-auto space-y-3">
              <div class="flex justify-between items-center">
                <span class="text-gray-600">Tạm tính:</span>
                <span class="font-medium">{{ formatCurrency(totalAmount) }}</span>
              </div>
              <div v-if="hoaDonData?.phiVanChuyen" class="flex justify-between items-center">
                <span class="text-gray-600">Phí vận chuyển:</span>
                <span class="font-medium">{{ formatCurrency(hoaDonData.phiVanChuyen) }}</span>
              </div>
              <div v-if="hoaDonData?.giaTriVoucher && hoaDonData?.maVoucher" class="flex justify-between items-center">
                <span class="text-gray-600">Giảm giá voucher:</span>
                <span class="font-medium text-green-600">-{{ formatCurrency(Math.abs(hoaDonData.giaTriVoucher)) }}</span>
              </div>
              <div class="flex justify-between items-center pt-3 border-t border-gray-200">
                <span class="text-lg font-bold text-gray-900">Tổng cộng:</span>
                <span class="text-xl font-bold text-red-600">{{ formatCurrency(hoaDonData?.tongTienSauGiam) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </NCard>

    <!-- ==================== MODAL: CHỌN / THAY ĐỔI SERIAL ==================== -->
    <NModal
      v-model:show="showSerialModal"
      preset="card"
      :title="changingImei
        ? `Thay đổi Serial - ${selectedProductItem?.tenSanPham || ''}`
        : `Bổ sung Serial - ${selectedProductItem?.tenSanPham || ''}`"
      style="width: 90%; max-width: 1000px; border-radius: 12px"
      :bordered="false"
      class="no-print shadow-2xl"
      @update:show="handleSerialModalClose"
    >
      <div class="space-y-4">
        <!-- Thông tin sản phẩm -->
        <div v-if="selectedProductItem" class="flex items-center justify-between p-4 bg-blue-50/50 border border-blue-100 rounded-xl">
          <div class="flex items-center gap-4">
            <NAvatar
              :src="selectedProductItem.anhSanPham"
              size="large"
              round
              fallback-src="https://via.placeholder.com/40"
              class="border border-blue-200 bg-white p-1"
            />
            <div>
              <div class="font-bold text-gray-900 text-lg mb-1">
                {{ selectedProductItem.tenSanPham }}
              </div>
              <!-- ✅ Hiển thị thông tin khác nhau tuỳ luồng -->
              <div class="flex flex-wrap items-center gap-3 text-sm">
                <template v-if="changingImei">
                  <!-- ✅ Chỉ hiện số đang chọn, không hiện tổng đã gán (tránh nhầm lẫn) -->
                  <span class="text-blue-700 font-medium">
                    Đang chọn serial thay thế:
                    <span class="font-bold">{{ selectedSerialIds.length }}</span>/1
                  </span>
                </template>
                <template v-else>
                  <span class="text-gray-500">
                    Đã gán: <span class="font-bold text-green-600">{{ alreadyAssignedCount }}</span>/{{ selectedProductItem.soLuong }}
                  </span>
                  <span class="text-gray-300">|</span>
                  <span class="text-blue-700 font-medium">
                    Cần bổ sung: <span class="font-bold">{{ requiredQuantityToAssign }}</span> Serial
                  </span>
                  <span class="text-gray-300">|</span>
                  <span class="text-orange-600 font-medium">
                    Đang chọn: <span class="font-bold">{{ selectedSerialIds.length }}</span>/{{ requiredQuantityToAssign }}
                  </span>
                </template>
              </div>
            </div>
          </div>

          <div class="flex items-center gap-2 flex-shrink-0">
            <NButton
              circle
              size="large"
              secondary
              :loading="loadingSerials"
              title="Làm mới danh sách serial"
              style="border: 1px solid #e0e7ff;"
              @click="refreshSerialList"
            >
              <template #icon>
                <NIcon color="#6366f1">
                  <RefreshOutline />
                </NIcon>
              </template>
            </NButton>

            <NButton type="primary" size="large" round class="shadow-sm" @click="startScanMode">
              <template #icon>
                <NIcon><QrCodeOutline /></NIcon>
              </template>
              Quét mã vạch
            </NButton>
          </div>
        </div>

        <!-- ✅ Banner hiển thị serial cũ đang bị thay thế -->
        <div v-if="changingImei" class="flex items-center gap-3 p-3 bg-orange-50 border border-orange-200 rounded-xl">
          <NIcon size="20" color="#f97316">
            <RefreshOutline />
          </NIcon>
          <div class="text-sm">
            <span class="text-gray-500">Serial cũ sẽ bị thay thế: </span>
            <span class="font-mono font-bold text-orange-600 tracking-wider">{{ changingImei.code }}</span>
            <span class="text-gray-400 ml-2 text-xs">(Serial này sẽ trở về trạng thái khả dụng)</span>
          </div>
        </div>

        <!-- Khu vực quét mã vạch -->
        <div v-if="scanMode" class="p-4 bg-yellow-50 rounded-xl border border-yellow-200 shadow-inner">
          <div class="flex items-center gap-3">
            <NInput
              ref="scanInputRef"
              v-model:value="scannedSerial"
              placeholder="Nhấp chuột vào đây và Quét mã vạch Serial..."
              size="large"
              class="font-mono"
              @keyup.enter="handleScanInput"
            >
              <template #prefix>
                <NIcon><QrCodeOutline /></NIcon>
              </template>
            </NInput>
            <NButton type="primary" size="large" @click="handleScanInput">
              Thêm
            </NButton>
            <NButton size="large" @click="scanMode = false">
              Đóng máy quét
            </NButton>
          </div>
        </div>

        <!-- Bảng danh sách serial -->
        <div v-if="loadingSerials" class="text-center py-12">
          <NSpin size="large" />
          <p class="text-gray-400 mt-4">
            Đang tải dữ liệu Serial trong kho...
          </p>
        </div>
        <div v-else class="border border-gray-200 rounded-xl overflow-hidden">
          <NDataTable
            :columns="serialColumns"
            :data="selectedSerials"
            :max-height="400"
            :pagination="{ pageSize: 10 }"
            :bordered="false"
            striped
          />
        </div>

        <!-- Nút hành động -->
        <div class="flex justify-end gap-3 pt-5">
          <NButton size="large" round @click="showSerialModal = false">
            Hủy bỏ
          </NButton>
          <NButton
            type="primary"
            size="large"
            round
            :disabled="selectedSerialIds.length !== requiredQuantityToAssign"
            :loading="isAddingSerials"
            class="font-bold shadow-md"
            @click="addSerialsToInvoice"
          >
            {{ changingImei
              ? `Lưu thay đổi (${selectedSerialIds.length}/1)`
              : `Lưu thay đổi (${selectedSerialIds.length}/${requiredQuantityToAssign})` }}
          </NButton>
        </div>
      </div>
    </NModal>

    <!-- ==================== MODAL: XEM SERIAL ĐÃ GÁN ==================== -->
    <NModal
      v-model:show="showSerialInfoModal"
      preset="card"
      title="Thông tin Serial đã gán"
      style="width: 500px; border-radius: 12px"
      :bordered="false"
      class="no-print shadow-xl"
    >
      <div v-if="selectedSerialInfoProduct" class="space-y-4">
        <div class="flex items-center gap-4 p-4 bg-green-50/50 border border-green-100 rounded-xl">
          <NAvatar
            :src="selectedSerialInfoProduct.anhSanPham"
            size="large"
            round
            fallback-src="https://via.placeholder.com/40"
            class="border border-green-200 bg-white"
          />
          <div>
            <div class="font-bold text-gray-900 text-base mb-1">
              {{ selectedSerialInfoProduct.tenSanPham }}
            </div>
            <div class="text-sm text-gray-500">
              Đã gán <span class="font-bold text-green-600">{{ currentProductSerialInfo.length }}/{{ selectedSerialInfoProduct.soLuongOriginal || selectedSerialInfoProduct.soLuong }}</span> máy
            </div>
          </div>
        </div>
        <div v-if="currentProductSerialInfo.length === 0" class="text-center py-8 text-gray-400 bg-gray-50 rounded-xl border border-dashed border-gray-200">
          <NIcon size="48" class="mb-2 opacity-50">
            <CubeOutline />
          </NIcon>
          <p>Sản phẩm chưa có serial nào</p>
        </div>
        <div v-else class="space-y-2 max-h-96 overflow-y-auto pr-2">
          <div
            v-for="(imei, index) in currentProductSerialInfo"
            :key="imei.id || index"
            class="flex items-center justify-between p-3 bg-white rounded-lg border border-gray-200 shadow-sm"
          >
            <div class="flex items-center gap-3">
              <span class="text-xs text-gray-400 font-bold w-4 text-center">{{ index + 1 }}</span>
              <span class="font-mono font-bold text-indigo-600 text-base tracking-wider">{{ imei.code || imei.imeiCode }}</span>
            </div>
            <NIcon size="18" class="text-green-500">
              <CheckmarkCircleOutline />
            </NIcon>
          </div>
        </div>
      </div>
      <template #footer>
        <div class="flex justify-end pt-2">
          <NButton type="default" round @click="showSerialInfoModal = false">
            Đóng lại
          </NButton>
        </div>
      </template>
    </NModal>

    <!-- ==================== MODAL: LỊCH SỬ ĐƠN HÀNG ==================== -->
    <NModal
      v-model:show="showHistoryModal"
      preset="card"
      title="Lịch sử đơn hàng"
      style="width: 1000px; max-width: 95vw; border-radius: 12px;"
      class="no-print shadow-2xl"
      :bordered="false"
      size="huge"
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
            v-for="(item, index) in historyList"
            :key="index"
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

    <!-- ==================== MODAL: CHUYỂN TRẠNG THÁI ==================== -->
    <NModal
      v-model:show="showStatusModal"
      preset="card"
      class="no-print !w-[420px] !max-w-[90vw] !rounded-2xl shadow-2xl"
      :bordered="false"
      size="huge"
      content-style="padding-top: 0;"
    >
      <template #header>
        <div class="flex items-center gap-3 pt-2">
          <div class="w-10 h-10 rounded-full bg-blue-50 flex items-center justify-center">
            <NIcon size="22" color="#3b82f6">
              <RefreshOutline />
            </NIcon>
          </div>
          <div>
            <h3 class="text-lg font-bold text-gray-900 leading-tight">
              Chuyển trạng thái đơn hàng
            </h3>
            <p class="text-xs text-gray-500 font-normal mt-0.5">
              Mã đơn: <span class="font-mono text-gray-700 font-semibold">{{ hoaDonData?.maHoaDon }}</span>
            </p>
          </div>
        </div>
      </template>

      <div class="py-4">
        <div class="bg-gradient-to-br from-gray-50 to-gray-100/50 rounded-xl p-4 border border-gray-200 shadow-inner flex items-center justify-between relative overflow-hidden mb-4">
          <div class="flex flex-col items-center gap-2 relative z-10 w-1/3">
            <div class="w-10 h-10 rounded-full bg-white border-2 border-gray-200 flex items-center justify-center text-gray-400 shadow-sm opacity-80">
              <NIcon size="20">
                <component :is="getStatusIcon(currentStatus)" />
              </NIcon>
            </div>
            <span class="text-sm font-semibold text-gray-500 text-center">{{ getStatusText(currentStatus) }}</span>
          </div>
          <div class="flex flex-col items-center justify-center w-1/3 relative z-10">
            <div class="w-full h-[2px] bg-gray-200 absolute top-[50%] -translate-y-1/2 -z-10" />
            <div class="w-8 h-8 rounded-full bg-white border border-gray-200 flex items-center justify-center shadow-sm">
              <NIcon size="16" class="text-gray-400">
                <ArrowForwardOutline />
              </NIcon>
            </div>
          </div>
          <div class="flex flex-col items-center gap-2 relative z-10 w-1/3">
            <div
              class="w-12 h-12 rounded-full border-[3px] flex items-center justify-center shadow-md scale-110 transition-colors"
              :class="getModalStatusColorClass(selectedStatus)"
            >
              <NIcon size="24">
                <component :is="getStatusIcon(selectedStatus)" />
              </NIcon>
            </div>
            <span class="text-sm font-bold text-center mt-1" :class="getModalStatusColorClass(selectedStatus).split(' ')[1]">
              {{ getStatusText(selectedStatus) }}
            </span>
          </div>
        </div>
        <div class="space-y-2">
          <label class="text-sm font-semibold text-gray-700 flex items-center gap-1">
            {{ selectedStatus === 5 ? 'Lý do hủy đơn' : 'Ghi chú thêm' }}
            <span v-if="selectedStatus === 5" class="text-red-500 font-bold">*</span>
            <span v-else class="text-gray-400 font-normal text-xs">(Không bắt buộc)</span>
          </label>
          <NInput
            v-model:value="statusNote"
            type="textarea"
            :placeholder="selectedStatus === 5
              ? 'Bắt buộc nhập lý do hủy đơn hàng...'
              : 'Nhập ghi chú cho nhân viên hoặc khách hàng...'"
            :rows="3"
            :status="selectedStatus === 5 && statusNote.trim() === '' ? 'error' : undefined"
            class="!rounded-lg"
          />
          <p v-if="selectedStatus === 5 && statusNote.trim() === ''" class="text-xs text-red-500 mt-1">
            Vui lòng nhập lí do
          </p>
        </div>
      </div>

      <template #footer>
        <div class="flex gap-3 justify-end pt-2">
          <NButton size="large" class="!rounded-lg font-medium" @click="showStatusModal = false">
            Hủy bỏ
          </NButton>
          <NButton
            type="primary"
            size="large"
            class="!rounded-lg font-bold shadow-md hover:-translate-y-0.5 transition-transform"
            :loading="isUpdating"
            @click="confirmStatusUpdate"
          >
            Xác nhận chuyển
          </NButton>
        </div>
      </template>
    </NModal>
    <!-- ==================== MODAL: CHỈNH SỬA KHÁCH HÀNG ==================== -->
    <NModal
      v-model:show="showCustomerModal"
      preset="card"
      title="Chỉnh sửa thông tin khách hàng"
      style="width: 500px; max-width: 95vw; border-radius: 12px"
      :bordered="false"
      class="no-print shadow-xl"
    >
      <NForm
        ref="customerFormRef"
        :model="customerForm"
        :rules="customerFormRules"
        label-placement="top"
      >
        <NFormItem label="Họ và tên" path="tenKhachHang">
          <NInput
            v-model:value="customerForm.tenKhachHang"
            placeholder="Nhập họ và tên khách hàng"
            size="large"
          />
        </NFormItem>

        <NFormItem label="Số điện thoại" path="sdtKH">
          <NInput
            v-model:value="customerForm.sdtKH"
            placeholder="Nhập số điện thoại"
            size="large"
          />
        </NFormItem>

        <NFormItem label="Email" path="email">
          <NInput
            v-model:value="customerForm.email"
            placeholder="Nhập email (không bắt buộc)"
            size="large"
          />
        </NFormItem>

        <NFormItem label="Địa chỉ" path="diaChi">
          <NInput
            v-model:value="customerForm.diaChi"
            placeholder="Nhập địa chỉ giao hàng"
            size="large"
            type="textarea"
            :rows="3"
          />
        </NFormItem>
      </NForm>

      <template #footer>
        <div class="flex justify-end gap-3">
          <NButton size="large" @click="showCustomerModal = false">
            Hủy bỏ
          </NButton>
          <NPopconfirm
            positive-text="Xác nhận"
            negative-text="Hủy bỏ"
            @positive-click="saveCustomerInfo"
          >
            <template #trigger>
              <NButton
                type="primary"
                size="large"
                :loading="isSavingCustomer"
              >
                Lưu thông tin
              </NButton>
            </template>

            <template #icon>
              <NIcon color="#18a058">
                <CheckmarkCircleOutline />
              </NIcon>
            </template>

            Bạn có chắc muốn cập nhật thông tin khách hàng?
          </NPopconfirm>
        </div>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
@media print {
  .no-print { display: none !important; }
  body { background: white !important; color: black !important; padding: 0 !important; margin: 0 !important; font-size: 12px; }
  .container { max-width: 100% !important; padding: 0 !important; margin: 0 !important; }
  #invoice-paper { box-shadow: none !important; padding: 20px !important; max-width: 100%; }
  .print-table { width: 100%; border-collapse: collapse; }
  .print-table th, .print-table td { padding: 8px 5px; border: 1px solid #ddd; }
  .print-table th { background-color: #f8f9fa; font-weight: bold; }
  tr { page-break-inside: avoid; }
  thead { display: table-header-group; }
  tfoot { display: table-footer-group; }
}

.hidden { display: none; }

::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: #f1f1f1; border-radius: 3px; }
::-webkit-scrollbar-thumb { background: #c1c1c1; border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: #a8a8a8; }

:deep(.n-popconfirm__action .n-button--primary-type) {
  background-color: #18a058 !important;
  border-color: #18a058 !important;
}

:deep(.n-popconfirm__action .n-button--primary-type:hover) {
  background-color: #0f9249 !important;
  border-color: #0f9249 !important;
}
</style>
