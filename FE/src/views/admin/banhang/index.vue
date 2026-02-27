<script setup lang="ts">
// ==================== IMPORTS ====================
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import type {
  GoiYVoucherResponse,
  KhachHangResponse,
  PhieuGiamGiaResponse,
  PhuongThucThanhToanResponse,
  ThongTinGiaoHangResponse,
} from '@/service/api/admin/banhang.api'
import {
  GeOneKhachHang,
  getColors,
  getCPUs,
  getCreateHoaDon,
  GetGioHang,
  getGPUs,
  getHardDrives,
  GetHoaDons,
  getImeiProductDetail,
  GetKhachHang,
  getMaGiamGia,
  getMaterials,
  getPhuongThucThanhToan,
  getProductDetails,
  getRAMs,
  huyHoaDon,
  suaGiaoHang,
  thanhToanThanhCong,
  themKhachHang,
  themMoiKhachHang,
  themSanPham,
  themSL,
  xoaKhachHang,
  xoaSL,
  xoaSP,
} from '@/service/api/admin/banhang.api'
import type {
  ADPDImeiResponse,
  ADProductDetailRequest,
  ADProductDetailResponse,
} from '@/service/api/admin/product/productDetail.api'
import type { AvailableServiceRequest, ShippingFeeRequest } from '@/service/api/ghn.api'
import { calculateFee, getAvailableServices, getGHNDistricts, getGHNProvinces, getGHNWards } from '@/service/api/ghn.api'
import { localStorageAction } from '@/utils/storage'
import { debounce } from 'lodash'
import type { DataTableColumns } from 'naive-ui'
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { toast } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'

// ==================== ICONS ====================
import {
  AddCircleOutline,
  CheckmarkOutline,
  CloseOutline,
  InformationCircleOutline,
  MoonOutline,
  QrCodeOutline,
  ReloadOutline,
  RocketOutline,
  SearchOutline,
  TicketOutline,
  TrashOutline,
  WalletOutline,
} from '@vicons/ionicons5'

// ==================== NAIVE UI COMPONENTS ====================
import {
  NAlert,
  NBadge,
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NDivider,
  NEmpty,
  NForm,
  NFormItem,
  NFormItemGi,
  NGi,
  NGrid,
  NIcon,
  NImage,
  NInput,
  NInputNumber,
  NList,
  NListItem,
  NModal,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NScrollbar,
  NSelect,
  NSpace,
  NSwitch,
  NTag,
  NText,
  NThing,
  NTooltip,
} from 'naive-ui'

// ==================== CONSTANTS & CONFIG ====================
const GHN_API_TOKEN = '72f634c6-58a2-11f0-8a1e-1e10d8df3c04'
const GHN_SHOP_ID = 5872469
const FROM_DISTRICT_ID = 3440
const FROM_WARD_CODE = '13010'

// ==================== REACTIVE STATE ====================

// Thông tin user từ localStorage
const idNV = localStorageAction.get(USER_INFO_STORAGE_KEY)

// State cho hóa đơn và tabs
const tabs = ref<
  Array<{
    id: number
    idHD: string
    code: string
    soLuong: number
    products: any[]
    loaiHoaDon: string
    isTemp?: boolean
  }>
>([])
const activeTab = ref(0)
const idHDS = ref('')
const loaiHD = ref('')
let nextTabId = 1

// State chính
const state = reactive({
  searchQuery: '',
  idSP: '',
  searchStatus: null as number | null,
  isModalOpen: false,
  isModaThanhToanlOpen: false,
  isModalChangeStatus: false,
  selectedProductId: null as string | null,
  khachHang: [] as KhachHangResponse[],
  thanhToan: [],
  discountList: [] as PhieuGiamGiaResponse[],
  phuongThuThanhToan: [] as PhuongThucThanhToanResponse[],
  tongTien: null as { tongTien: number } | null,
  detailKhachHang: null as KhachHangResponse | null,
  products: [] as ADProductDetailResponse[],
  gioHang: [] as any[],
  paginationParams: { page: 1, size: 10 },
  totalItems: 0,
  totalItemsKH: 0,
  selectedPaymentMethod: '' as string,
  currentPaymentMethod: '0',
  autoVoucherResult: null as GoiYVoucherResponse | null,
})

// State cho sản phẩm (modal)
const stateSP = reactive({
  searchQuery: '',
  searchStatus: null as number | null,
  selectedMaterial: null as string | null,
  isModalOpen: false,
  isModalChangeStatus: false,
  selectedProductId: null as string | null,
  products: [] as ADProductDetailResponse[],
  paginationParams: { page: 1, size: 10 },
  totalItems: 0,
})

// State cho bộ lọc sản phẩm
const localSearchQuery = ref('')
const localColor = ref<string | null>(null)
const localCPU = ref<string | null>(null)
const localGPU = ref<string | null>(null)
const localRAM = ref<string | null>(null)
const localHardDrive = ref<string | null>(null)
const localSelectedMaterial = ref<string | null>(null)
const priceRange = ref<[number, number]>([0, 0])

// Options cho bộ lọc
const ColorOptions = ref<{ label: string, value: string }[]>([])
const CpuOptions = ref<{ label: string, value: string }[]>([])
const GpuOptions = ref<{ label: string, value: string }[]>([])
const RamOptions = ref<{ label: string, value: string }[]>([])
const HardDriveOptions = ref<{ label: string, value: string }[]>([])
const MaterialOptions = ref<{ label: string, value: string }[]>([])

// State cho giá min/max
const stateMinMaxPrice = reactive({
  priceMin: 0,
  priceMax: 0,
})

// State cho voucher
const selectedVoucher = ref<any>(null)
const selectedDiscount = ref<PhieuGiamGiaResponse | null>(null)
const selectedDiscountCode = ref<string>('')
const applyingVoucher = ref<string | null>(null)
const autoApplying = ref(false)
const applyingBetterVoucher = ref<string | null>(null)
const betterDiscountMessage = ref('')
const isBestDiscountApplied = ref(false)
const phieuNgon = ref('')

// State cho giao hàng
const isDeliveryEnabled = ref(false)
const deliveryInfo = reactive({
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanhPho: null as string | null,
  quanHuyen: null as string | null,
  phuongXa: null as string | null,
  diaChiCuThe: '',
  ghiChu: '',
})
const provinces = ref<Array<{ value: string, label: string, code: string }>>([])
const districts = ref<Array<{ value: string, label: string, code: string }>>([])
const wards = ref<Array<{ value: string, label: string, code: string }>>([])
const shippingFee = ref(0)
const provinceCode = ref<number | null>(null)
const districtCode = ref<number | null>(null)
const wardCode = ref<string | null>(null)
const isFreeShipping = ref(false)
const currentDeliveryInfo = ref<ThongTinGiaoHangResponse | null>(null)
const deliveryInfoByInvoice = reactive<{ [key: string]: any }>({})

// State cho thanh toán
const soTien = ref(0)
const tienKhachThanhToan = ref(0)
const tienThieu = ref(0)
const tongTien = ref(0)
const tongTienTruocGiam = ref(0)
const giamGia = ref(0)
const tienHang = ref(0)

// State cho khách hàng
const customerSearchQuery = ref('')
const newCustomer = reactive({
  ten: '',
  sdt: '',
})

// State cho serial IMEI
const showSerialModal = ref(false)
const selectedProductDetail = ref<ADProductDetailResponse | null>(null)
const selectedSerials = ref<ADPDImeiResponse[]>([])
const selectedSerialIds = ref<string[]>([])
const loadingSerials = ref(false)
const imeiDaChon = ref<
  Array<{
    idHoaDonChiTiet: string
    danhSachImei: string[]
  }>
>([])

// State cho modals
const showKhachHangModal = ref(false)
const showProductModal = ref(false)
const showDiscountModal = ref(false)
const showDeliveryModal = ref(false)
const showSuggestionDetailModal = ref(false)
const selectedSuggestion = ref<any>(null)
const discountTab = ref('auto')
const addCustomerLoading = ref(false)
const isQrVNpayModalVisible = ref(false)
const isQrModalVisible = ref(false)
const qrData = ref('')
const hasCamera = ref(true)
const qrCodeUrl = ref('/images/qr.png')

// State cho thanh toán kết hợp
const isBothPaymentModalVisible = ref(false)
const amountPaid = ref(0)
const bothPaymentLoading = ref(false)
const bothPaymentMethod = ref<'CASH_FIRST' | 'BANKING_FIRST'>('CASH_FIRST')

// ==================== COMPUTED PROPERTIES ====================

/**
 * Số lượng serial khả dụng
 */
const availableSerialsCount = computed(() => {
  return selectedSerials.value.filter(s => s.imeiStatus === 'AVAILABLE').length
})

/**
 * Giá nhỏ nhất trong danh sách sản phẩm
 */
const minProductPrice = computed(() => {
  if (stateSP.products.length === 0)
    return 0
  const prices = stateSP.products.map(p => p.price).filter(price => price > 0)
  return prices.length > 0 ? Math.min(...prices) : 0
})

/**
 * Giá lớn nhất trong danh sách sản phẩm
 */
const maxProductPrice = computed(() => {
  if (stateSP.products.length === 0)
    return 0
  const prices = stateSP.products.map(p => p.price).filter(price => price > 0)
  return prices.length > 0 ? Math.max(...prices) : 0
})

/**
 * Kiểm tra có voucher gợi ý tốt hơn không
 */
const hasBetterVoucherSuggestion = computed(() => {
  return state.autoVoucherResult?.voucherTotHon?.some(
    v => v.giamThem > (selectedVoucher.value?.giamGiaThucTe || 0),
  )
})

// ==================== WATCHERS ====================

/**
 * Theo dõi thay đổi tổng tiền hàng để tự động gợi ý voucher
 */
watch(
  () => tienHang.value,
  async (newValue, oldValue) => {
    if (state.gioHang.length > 0 && newValue > 0 && idHDS.value) {
      setTimeout(async () => {
        await fetchDiscounts(idHDS.value)
      }, 500)
    }
    else if (state.gioHang.length === 0) {
      resetDiscount()
      state.autoVoucherResult = null
    }
  },
  { immediate: true },
)

/**
 * Theo dõi thay đổi khách hàng để cập nhật voucher
 */
watch(
  () => state.detailKhachHang?.id,
  async () => {
    if (state.gioHang.length > 0 && tienHang.value > 0 && idHDS.value) {
      await fetchDiscounts(idHDS.value)
    }
  },
  fetchCustomers(),
)

// ==================== API CALL FUNCTIONS ====================

/**
 * Lấy danh sách khách hàng
 */
async function fetchCustomers() {
  try {
    const params = {
      page: state.paginationParams.page,
      size: state.paginationParams.size,
      q: customerSearchQuery.value.trim(),
    }
    const response = await GetKhachHang(params)
    state.khachHang = response.data?.data || []
    state.totalItemsKH = response.totalElements || 0
  }
  catch (error) {
    console.error('Failed to fetch customers:', error)
    state.khachHang = []
    state.totalItemsKH = 0
  }
}

/**
 * Lấy danh sách hóa đơn
 */
async function fetchHoaDon() {
  try {
    await fetchProducts()
    const response = await GetHoaDons()

    if (response && Array.isArray(response)) {
      tabs.value = response.map((invoice, index) => ({
        id: index + 1,
        idHD: invoice.id,
        code: invoice.code,
        soLuong: invoice.soLuong,
        loaiHoaDon: invoice.loaiHoaDon,
        products: invoice.data?.products || [],
      }))

      if (tabs.value.length > 0) {
        activeTab.value = tabs.value[0].id
        idHDS.value = tabs.value[0].idHD
        loaiHD.value = tabs.value[0].loaiHoaDon
        await clickkActiveTab(tabs.value[0].id, tabs.value[0].idHD, tabs.value[0].loaiHoaDon)
      }
      else {
        resetState()
      }
    }
  }
  catch (error) {
    console.error('Failed to fetch invoices:', error)
    toast.error('Lấy danh sách hóa đơn thất bại!')
    resetState()
  }
}

/**
 * Lấy danh sách sản phẩm
 */
async function fetchProducts() {
  try {
    const params: ADProductDetailRequest = {
      page: stateSP.paginationParams.page,
      size: stateSP.paginationParams.size,
      q: localSearchQuery.value || undefined,
      idColor: localColor.value || undefined,
      idCPU: localCPU.value || undefined,
      idGPU: localGPU.value || undefined,
      idRAM: localRAM.value || undefined,
      idHardDrive: localHardDrive.value || undefined,
      idMaterial: localSelectedMaterial.value || undefined,
    }
    const response = await getProductDetails(params)
    stateSP.products = response.data?.data || []
    state.products = response.data?.data || []
    stateSP.totalItems = response.data?.totalElements || 0
    state.totalItems = response.data?.totalElements || 0
  }
  catch (error) {
    console.error('Failed to fetch products:', error)
    toast.error('Lấy danh sách sản phẩm thất bại!')
  }
}

/**
 * Lấy danh sách voucher khả dụng
 * @param idHD - ID hóa đơn
 */
async function fetchDiscounts(idHD: string) {
  try {
    if (!idHD) {
      resetDiscount()
      return
    }

    if (state.gioHang.length === 0) {
      console.log('Giỏ hàng trống, không gọi API voucher')
      resetDiscount()
      state.discountList = []
      state.autoVoucherResult = null
      betterDiscountMessage.value = ''
      return
    }

    const params = {
      invoiceId: idHD,
      tongTien: tienHang.value,
      customerId: state.detailKhachHang?.id ?? null,
    }

    const response = await getMaGiamGia(params)

    state.autoVoucherResult = response
    state.discountList = response.voucherApDung ?? []

    if (response.voucherTotHon?.length > 0) {
      const bestSuggestion = response.voucherTotHon[0]
      betterDiscountMessage.value = `Mua thêm ${formatCurrency(
        bestSuggestion.canMuaThem,
      )} để được giảm thêm ${formatCurrency(bestSuggestion.giamThem)}`
    }
    else {
      betterDiscountMessage.value = ''
    }

    if (!selectedVoucher.value && response.voucherApDung?.length > 0) {
      const bestVoucher = response.voucherApDung.reduce((best, current) =>
        (current.giamGiaThucTe || 0) > (best.giamGiaThucTe || 0) ? current : best,
      )
      selectVoucher(bestVoucher)
    }
  }
  catch (error) {
    console.error('Fetch discounts failed', error)
    resetDiscount()
    state.discountList = []
    state.autoVoucherResult = null
  }
}

/**
 * Lấy danh sách serial theo sản phẩm
 * @param productId - ID sản phẩm
 */
async function fetchSerialsByProduct(productId: string) {
  try {
    const response = await getImeiProductDetail(productId)
    if (response.data && Array.isArray(response.data)) {
      selectedSerials.value = response.data
    }
    else {
      selectedSerials.value = []
    }
    selectedSerialIds.value = []
    showSerialModal.value = true
  }
  catch (error) {
    console.error('Failed to fetch serials:', error)
    toast.error('Lấy danh sách serial thất bại!')
    selectedSerials.value = []
  }
  finally {
    loadingSerials.value = false
  }
}

/**
 * Lấy danh sách tỉnh/thành phố (GHN)
 */
async function fetchProvinces() {
  try {
    const response = await getGHNProvinces(GHN_API_TOKEN)
    provinces.value = response.map((item: any) => ({
      value: String(item.ProvinceID),
      label: item.ProvinceName,
      code: String(item.ProvinceID),
    }))
  }
  catch (error) {
    console.error('Failed to fetch provinces:', error)
    provinces.value = []
  }
}

/**
 * Lấy danh sách quận/huyện theo tỉnh/thành phố
 * @param provinceId - ID tỉnh/thành phố
 */
async function fetchDistricts(provinceId: number) {
  try {
    const response = await getGHNDistricts(provinceId, GHN_API_TOKEN)
    districts.value = response.map((item: any) => ({
      value: String(item.DistrictID),
      label: item.DistrictName,
      code: String(item.DistrictID),
    }))
  }
  catch (error) {
    console.error('Failed to fetch districts:', error)
    districts.value = []
  }
}

/**
 * Lấy danh sách phường/xã theo quận/huyện
 * @param districtId - ID quận/huyện
 */
async function fetchWards(districtId: number) {
  try {
    const response = await getGHNWards(districtId, GHN_API_TOKEN)
    wards.value = response.map((item: any) => ({
      value: item.WardCode,
      label: item.WardName,
      code: item.WardCode,
    }))
  }
  catch (error) {
    console.error('Failed to fetch wards:', error)
    wards.value = []
  }
}

/**
 * Tính phí vận chuyển
 */
async function calculateShippingFee() {
  if (
    !isDeliveryEnabled.value
    || !idHDS.value
    || !provinceCode.value
    || !districtCode.value
    || !wardCode.value
    || tienHang.value <= 0
  ) {
    shippingFee.value = 0
    isFreeShipping.value = false
    calculateTotalAmounts()
    return
  }

  // Miễn phí vận chuyển nếu đơn hàng trên 5 triệu
  if (tienHang.value > 5000000) {
    isFreeShipping.value = true
    shippingFee.value = 0
    toast.success('Đơn hàng trên 5,000,000 VND, miễn phí vận chuyển!')
    calculateTotalAmounts()
    return
  }

  try {
    const availableServicesRequestBody: AvailableServiceRequest = {
      shop_id: GHN_SHOP_ID,
      from_district: FROM_DISTRICT_ID,
      to_district: districtCode.value,
    }
    const availableServicesResponse = await getAvailableServices(
      GHN_API_TOKEN,
      availableServicesRequestBody,
    )

    if (!availableServicesResponse.data || !availableServicesResponse.data.length) {
      shippingFee.value = 0
      isFreeShipping.value = false
      toast.warn('Không tìm thấy dịch vụ vận chuyển phù hợp.')
      calculateTotalAmounts()
      return
    }

    const selectedServiceId = availableServicesResponse.data[0].service_id
    const requestBody: ShippingFeeRequest = {
      myRequest: {
        FromDistrictID: FROM_DISTRICT_ID,
        FromWardCode: FROM_WARD_CODE,
        ServiceID: selectedServiceId,
        ToDistrictID: districtCode.value,
        ToWardCode: wardCode.value,
        Height: 15,
        Length: 15,
        Weight: 500,
        Width: 15,
        InsuranceValue: tienHang.value,
        Coupon: null,
        PickShift: null,
      },
    }

    const response = await calculateFee(requestBody, GHN_API_TOKEN, GHN_SHOP_ID)
    shippingFee.value = response.data.total || 0
    isFreeShipping.value = false
    calculateTotalAmounts()
  }
  catch (error) {
    console.error('Failed to calculate shipping fee:', error)
    shippingFee.value = 0
    isFreeShipping.value = false
    toast.error('Không thể tính phí vận chuyển.')
    calculateTotalAmounts()
  }
}

/**
 * Lấy danh sách màu sắc
 */
async function fetchColor() {
  try {
    const { data } = await getColors()
    ColorOptions.value = data.map((c: any) => ({ label: c.ten || c.label, value: c.id }))
  }
  catch (e) {
    console.error('Error fetching colors:', e)
  }
}

/**
 * Lấy danh sách CPU
 */
async function fetchCPU() {
  try {
    const { data } = await getCPUs()
    CpuOptions.value = data.map((c: any) => ({ label: c.ten || c.label, value: c.id }))
  }
  catch (e) {
    console.error('Error fetching CPU:', e)
  }
}

/**
 * Lấy danh sách GPU
 */
async function fetchGPU() {
  try {
    const { data } = await getGPUs()
    GpuOptions.value = data.map((g: any) => ({ label: g.ten || g.label, value: g.id }))
  }
  catch (e) {
    console.error('Error fetching GPU:', e)
  }
}

/**
 * Lấy danh sách RAM
 */
async function fetchRAM() {
  try {
    const { data } = await getRAMs()
    RamOptions.value = data.map((r: any) => ({ label: r.ten || r.label, value: r.id }))
  }
  catch (e) {
    console.error('Error fetching RAM:', e)
  }
}

/**
 * Lấy danh sách ổ cứng
 */
async function fetchHardDrive() {
  try {
    const { data } = await getHardDrives()
    HardDriveOptions.value = data.map((h: any) => ({ label: h.ten || h.label, value: h.id }))
  }
  catch (e) {
    console.error('Error fetching Hard Drive:', e)
  }
}

/**
 * Lấy danh sách chất liệu
 */
async function fetchMaterial() {
  try {
    const { data } = await getMaterials()
    MaterialOptions.value = data.map((m: any) => ({ label: m.ten || m.label, value: m.id }))
  }
  catch (e) {
    console.error('Error fetching Material:', e)
  }
}

// ==================== UTILITY FUNCTIONS ====================

/**
 * Format số tiền sang VND
 * @param value - Số tiền cần format
 * @returns Chuỗi đã format
 */
function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

/**
 * Format số tiền cho input
 * @param value - Số tiền cần format
 * @returns Chuỗi đã format
 */
function formatCurrencyInput(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

/**
 * Parse chuỗi tiền tệ thành số
 * @param value - Chuỗi cần parse
 * @returns Số tiền
 */
function parseCurrency(value: string) {
  if (!value)
    return 0
  let str = String(value)
    .replace(/[^0-9nghì triệu]/g, '')
    .trim()
    .toLowerCase()
  let number = Number.parseInt(str.replace(/\D/g, '')) || 0

  if (str.includes('nghìn')) {
    number *= 1000
  }
  else if (str.includes('triệu')) {
    number *= 1000000
  }

  return number
}

/**
 * Format tooltip cho slider khoảng giá
 * @param value - Giá trị cần format
 * @returns Chuỗi đã format
 */
function formatTooltipRangePrice(value: number) {
  return formatCurrency(value)
}

/**
 * Tạo mã hóa đơn tạm thời
 * @returns Mã hóa đơn tạm
 */
function genTempMaHoaDon(): string {
  return `HD-TMP-${Math.random().toString(36).substring(2, 8).toUpperCase()}`
}

/**
 * Lấy địa chỉ đầy đủ từ thông tin giao hàng
 * @returns Chuỗi địa chỉ
 */
function getFullAddress(): string {
  const selectedProvince = provinces.value.find(p => p.code === deliveryInfo.tinhThanhPho)
  const selectedDistrict = districts.value.find(d => d.code === deliveryInfo.quanHuyen)
  const selectedWard = wards.value.find(w => w.code === deliveryInfo.phuongXa)

  return `${deliveryInfo.diaChiCuThe}, ${selectedWard?.label || ''}, ${
    selectedDistrict?.label || ''
  }, ${selectedProvince?.label || ''}`
}

/**
 * Lấy tag type cho voucher
 * @param type - Loại voucher
 * @returns Type của tag
 */
function getVoucherTagType(type: string) {
  return type === 'PERCENTAGE' ? 'success' : 'warning'
}

/**
 * Lấy tag type cho suggestion
 * @param hieuQua - Hiệu quả (%)
 * @returns Type của tag
 */
function getSuggestionTagType(hieuQua: number) {
  if (hieuQua >= 50)
    return 'error'
  if (hieuQua >= 30)
    return 'warning'
  if (hieuQua >= 15)
    return 'info'
  return 'default'
}

/**
 * Tính tổng các khoản tiền
 */
function calculateTotalAmounts() {
  tienHang.value = state.gioHang.reduce((sum, item) => sum + (item.price || item.giaBan), 0)

  if (selectedVoucher.value) {
    giamGia.value = selectedVoucher.value.giamGiaThucTe
  }
  else {
    giamGia.value = selectedDiscount.value ? selectedDiscount.value.giaTriGiamThucTe || 0 : 0
  }

  tongTienTruocGiam.value = tienHang.value
  let currentTotal = tienHang.value - giamGia.value
  if (currentTotal < 0)
    currentTotal = 0

  if (isDeliveryEnabled.value) {
    currentTotal += shippingFee.value
  }
  tongTien.value = currentTotal
  tienThieu.value = tongTien.value - tienKhachThanhToan.value
}

/**
 * Reset state discount
 */
function resetDiscount() {
  state.discountList = []
  selectedDiscount.value = null
  selectedVoucher.value = null
  selectedDiscountCode.value = ''
  giamGia.value = 0
  state.autoVoucherResult = null
  betterDiscountMessage.value = ''
  calculateTotalAmounts()
}

/**
 * Reset state tổng thể
 */
function resetState() {
  resetDiscount()
  currentDeliveryInfo.value = null
  isDeliveryEnabled.value = false
  Object.assign(deliveryInfo, {
    tenNguoiNhan: '',
    sdtNguoiNhan: '',
    diaChiCuThe: '',
    tinhThanhPho: undefined,
    quanHuyen: undefined,
    phuongXa: undefined,
  })
  provinceCode.value = null
  districtCode.value = null
  wardCode.value = null
  shippingFee.value = 0
  isFreeShipping.value = false
  state.gioHang = []
  state.detailKhachHang = null
  state.currentPaymentMethod = '0'
  tongTien.value = 0
  tongTienTruocGiam.value = 0
  giamGia.value = 0
  tienHang.value = 0
  tienKhachThanhToan.value = 0
  tienThieu.value = 0
}

/**
 * Cập nhật danh sách hóa đơn
 */
async function capNhatDanhSach() {
  const response = await GetHoaDons()
  if (response && Array.isArray(response)) {
    tabs.value = response.map((invoice, index) => ({
      id: index + 1,
      idHD: invoice.id,
      ma: invoice.ma,
      soLuong: invoice.soLuong,
      loaiHoaDon: invoice.loaiHoaDon,
      products: invoice.data?.products || [],
    }))
  }
}

/**
 * Cập nhật trạng thái thanh toán
 */
async function updatePaymentStatus() {
  if (idHDS.value) {
    const responsePTTT = await getPhuongThucThanhToan(idHDS.value)
    state.phuongThuThanhToan = responsePTTT
    let totalPaid = 0
    state.phuongThuThanhToan.forEach((item) => {
      totalPaid += item.tongTien
    })
    tienKhachThanhToan.value = totalPaid
    tienThieu.value = (tongTien.value || 0) - tienKhachThanhToan.value
  }
}

/**
 * Refresh giỏ hàng
 */
async function refreshCart() {
  if (idHDS.value) {
    const response = await GetGioHang(idHDS.value)
    state.gioHang = response
    await fetchDiscounts(idHDS.value)
  }
}

/**
 * Đặt phương thức thanh toán mặc định
 */
function setDefaultPaymentMethod() {
  state.currentPaymentMethod = '0'
}

// ==================== ACTION FUNCTIONS ====================

/**
 * Debounced fetch customers
 */
const debouncedFetchCustomers = debounce(async () => {
  await fetchCustomers()
}, 300)

/**
 * Xử lý khi click vào tab hóa đơn
 * @param id - ID tab
 * @param hd - ID hóa đơn
 * @param loaiHoaDon - Loại hóa đơn
 */
async function clickkActiveTab(id: number, hd: string, loaiHoaDon: string) {
  idHDS.value = hd
  activeTab.value = id
  loaiHD.value = loaiHoaDon
  isBestDiscountApplied.value = false

  resetDiscount()

  try {
    Object.assign(deliveryInfo, {
      tenNguoiNhan: '',
      sdtNguoiNhan: '',
      diaChiCuThe: '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    })
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    shippingFee.value = 0
    isFreeShipping.value = false

    const response = await GetGioHang(hd)
    state.gioHang = response

    const responseKH = await GeOneKhachHang(hd)
    state.detailKhachHang = responseKH.id ? responseKH : null

    if (state.gioHang.length === 0) {
      resetDiscount()
    }
    else if (tienHang.value > 0) {
      await fetchDiscounts(hd)
    }

    isDeliveryEnabled.value = loaiHoaDon === 'GIAO_HANG'

    if (isDeliveryEnabled.value && state.detailKhachHang) {
      deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || ''
      deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || ''
      deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || ''

      const tinhThanhPhoId = state.detailKhachHang.tinh
      const quanHuyenId = state.detailKhachHang.huyen
      const phuongXaId = state.detailKhachHang.xa

      if (!provinces.value.length) {
        await fetchProvinces()
      }

      if (tinhThanhPhoId) {
        const selectedProvince = provinces.value.find(p => p.code === tinhThanhPhoId.toString())
        if (selectedProvince) {
          deliveryInfo.tinhThanhPho = selectedProvince.value
          provinceCode.value = Number.parseInt(tinhThanhPhoId)
          await fetchDistricts(provinceCode.value)
        }
      }

      if (quanHuyenId && provinceCode.value) {
        const selectedDistrict = districts.value.find(d => d.code === quanHuyenId.toString())
        if (selectedDistrict) {
          deliveryInfo.quanHuyen = selectedDistrict.value
          districtCode.value = Number.parseInt(quanHuyenId)
          await fetchWards(districtCode.value)
        }
      }

      if (phuongXaId && districtCode.value) {
        const selectedWard = wards.value.find(w => w.code === phuongXaId)
        if (selectedWard) {
          deliveryInfo.phuongXa = selectedWard.value
          wardCode.value = phuongXaId
        }
      }

      await calculateShippingFee()
    }

    calculateTotalAmounts()
    await updatePaymentStatus()
  }
  catch (error) {
    console.error('Failed to switch invoice:', error)
    toast.error('Chuyển hóa đơn thất bại!')
    resetState()
  }
}

/**
 * Tạo hóa đơn mới
 */
async function createInvoice() {
  if (tabs.value.length >= 10) {
    toast.warning('Chỉ được tạo tối đa 10 hóa đơn!', { autoClose: 3000 })
    return
  }

  const tempMa = genTempMaHoaDon()
  const newTabId = nextTabId++

  tabs.value.push({
    id: newTabId,
    idHD: '',
    code: tempMa,
    soLuong: 0,
    loaiHoaDon: 'TAI_QUAY',
    products: [],
    isTemp: true,
  })

  activeTab.value = newTabId
  resetState()

  try {
    const formData = new FormData()
    formData.append('idNV', idNV.userId)
    formData.append('ma', tempMa)

    const newInvoice = await getCreateHoaDon(formData)

    const tab = tabs.value.find(t => t.id === newTabId)
    if (tab) {
      tab.idHD = newInvoice.data.id
      tab.code = newInvoice.data.code
      tab.loaiHoaDon = newInvoice.data.loaiHoaDon || 'OFFLINE'
      tab.isTemp = false
    }

    idHDS.value = newInvoice.data.id
    loaiHD.value = newInvoice.data.loaiHoaDon || 'OFFLINE'

    await clickkActiveTab(newTabId, newInvoice.data.id, newInvoice.data.loaiHoaDon || 'OFFLINE')
    toast.success('Tạo hóa đơn thành công!')
  }
  catch (error) {
    console.error('Failed to create invoice:', error)
    tabs.value = tabs.value.filter(t => t.id !== newTabId)
    toast.error('Tạo hóa đơn thất bại!')
  }
}

/**
 * Hủy hóa đơn
 * @param idHD - ID hóa đơn cần hủy
 */
async function huy(idHD: string) {
  try {
    const formData = new FormData()
    formData.append('idNV', idNV.userId)
    formData.append('idHD', idHD)

    const res = await huyHoaDon(formData)
    toast.success(res.message)

    await fetchProducts()
    await capNhatDanhSach()
    await fetchHoaDon()

    const indexToRemove = tabs.value.findIndex(tab => tab.idHD === idHDS.value)
    if (indexToRemove !== -1) {
      tabs.value.splice(indexToRemove, 1)
    }

    resetState()
  }
  catch (error: any) {
    if (error?.response?.data?.message) {
      toast.error(error.response.data.message)
    }
    else {
      toast.error('Có lỗi xảy ra khi hủy hóa đơn!')
      console.error('Lỗi khi hủy hóa đơn:', error)
    }
  }
}

/**
 * Mở modal chọn sản phẩm
 */
async function openProductSelectionModal() {
  if (!idHDS.value) {
    toast.error('Vui lòng tạo hoặc chọn hóa đơn trước khi chọn sản phẩm!')
    return
  }
  await fetchProducts()
  showProductModal.value = true
}

/**
 * Reset bộ lọc sản phẩm
 */
function resetFilters() {
  localSearchQuery.value = ''
  localColor.value = null
  localCPU.value = null
  localGPU.value = null
  localRAM.value = null
  localHardDrive.value = null
  localSelectedMaterial.value = null
  stateSP.searchQuery = ''
  stateSP.selectedMaterial = null
  fetchProducts()
}

/**
 * Xử lý thay đổi trang (sản phẩm)
 * @param page - Số trang
 */
function handlePageChange(page: number) {
  stateSP.paginationParams.page = page
  fetchProducts()
}

/**
 * Xử lý thay đổi kích thước trang (sản phẩm)
 * @param pageSize - Kích thước trang
 */
function handlePageSizeChange(pageSize: number) {
  stateSP.paginationParams.size = pageSize
  stateSP.paginationParams.page = 1
  fetchProducts()
}

/**
 * Xử lý thay đổi trang (khách hàng)
 * @param page - Số trang
 */
function handleCustomerPageChange(page: number) {
  state.paginationParams.page = page
  fetchCustomers()
}

/**
 * Xử lý thay đổi kích thước trang (khách hàng)
 * @param pageSize - Kích thước trang
 */
function handleCustomerPageSizeChange(pageSize: number) {
  state.paginationParams.size = pageSize
  state.paginationParams.page = 1
  fetchCustomers()
}

/**
 * Chọn sản phẩm để xem serial
 * @param product - Sản phẩm được chọn
 */
async function selectProductForSerial(product: ADProductDetailResponse) {
  selectedProductDetail.value = product
  loadingSerials.value = true
  selectedSerials.value = []
  selectedSerialIds.value = []
  await fetchSerialsByProduct(product.id)
}

/**
 * Thêm serial vào giỏ hàng
 */
async function addSerialToCart() {
  if (selectedSerialIds.value.length === 0) {
    toast.warning('Vui lòng chọn ít nhất 1 serial')
    return
  }

  if (!idHDS.value) {
    toast.error('Vui lòng tạo hoặc chọn hóa đơn trước!')
    return
  }

  if (!selectedProductDetail.value) {
    toast.error('Không có thông tin sản phẩm!')
    return
  }

  try {
    const imeisDaChon = selectedSerials.value
      .filter(s => selectedSerialIds.value.includes(s.id))
      .map(s => ({
        imeiCode: s.code,
        imeiId: s.id,
      }))

    const payload = {
      invoiceId: idHDS.value,
      productDetailId: selectedProductDetail.value.id,
      imeiIds: imeisDaChon.map(i => i.imeiId),
    }

    await themSanPham(payload)

    const existingIndex = imeiDaChon.value.findIndex(
      item => item.idHoaDonChiTiet === selectedProductDetail.value?.id,
    )

    if (existingIndex >= 0) {
      imeiDaChon.value[existingIndex].danhSachImei = [
        ...imeiDaChon.value[existingIndex].danhSachImei,
        ...imeisDaChon.map(i => i.imeiCode),
      ]
    }
    else {
      imeiDaChon.value.push({
        idHoaDonChiTiet: selectedProductDetail.value.id,
        danhSachImei: imeisDaChon.map(i => i.imeiCode),
      })
    }

    toast.success(`Đã thêm ${imeisDaChon.length} serial vào giỏ hàng!`)

    showSerialModal.value = false
    selectedSerialIds.value = []
    selectedSerials.value = []

    await refreshCart()
    await fetchDiscounts(idHDS.value)
    await fetchHoaDon()
  }
  catch (error) {
    console.error('Failed to add serials to cart:', error)
    toast.error('Thêm serial vào giỏ hàng thất bại!')
  }
}

/**
 * Tăng số lượng sản phẩm trong giỏ
 * @param idHDCT - ID hóa đơn chi tiết
 * @param idSPS - ID sản phẩm
 */
async function increaseQuantity(idHDCT: any, idSPS: any) {
  try {
    const formData = new FormData()
    formData.append('idSP', idSPS)
    formData.append('idHDCT', idHDCT)
    formData.append('idHD', idHDS.value)
    const res = await themSL(formData)

    if (res.message.includes('thay đổi giá từ')) {
      toast.warning(res.message)
      return
    }

    const response = await GetGioHang(idHDS.value)
    state.gioHang = response
    calculateTotalAmounts()
    await fetchDiscounts(idHDS.value)
    capNhatDanhSach()
  }
  catch (error) {
    console.error('Failed to increase quantity:', error)
    toast.error('Tăng số lượng thất bại!')
  }
}

/**
 * Giảm số lượng sản phẩm trong giỏ
 * @param idHDCT - ID hóa đơn chi tiết
 * @param idSPS - ID sản phẩm
 */
async function decreaseQuantity(idHDCT: any, idSPS: any) {
  try {
    const formData = new FormData()
    formData.append('idSP', idSPS)
    formData.append('idHDCT', idHDCT)
    await xoaSL(formData)

    const response = await GetGioHang(idHDS.value)
    state.gioHang = response
    calculateTotalAmounts()
    await fetchDiscounts(idHDS.value)
    capNhatDanhSach()
  }
  catch (error) {
    console.error('Failed to decrease quantity:', error)
    toast.error('Giảm số lượng thất bại!')
  }
}

/**
 * Xóa sản phẩm khỏi giỏ hàng
 * @param idSPS - ID sản phẩm
 * @param idHDCT - ID hóa đơn chi tiết
 */
async function deleteProduc(idSPS: any, idHDCT: string) {
  try {
    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('idSP', idSPS)
    formData.append('idHDCT', idHDCT)
    await xoaSP(formData)

    const response = await GetGioHang(idHDS.value)
    state.gioHang = response

    if (state.gioHang.length === 0) {
      resetDiscount()
      state.autoVoucherResult = null
      betterDiscountMessage.value = ''
    }
    else {
      await fetchDiscounts(idHDS.value)
    }

    toast.success('Xóa sản phẩm thành công!')
    await capNhatDanhSach()
  }
  catch (error) {
    console.error('Failed to delete product:', error)
    toast.error('Xóa sản phẩm thất bại!')
  }
}

/**
 * Chọn khách hàng
 * @param getIdKH - ID khách hàng được chọn
 */
async function selectKhachHang(getIdKH: any) {
  try {
    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('idKH', getIdKH)
    await themKhachHang(formData)
    const responseKH = await GeOneKhachHang(idHDS.value)
    state.detailKhachHang = responseKH

    Object.assign(deliveryInfo, {
      tenNguoiNhan: '',
      sdtNguoiNhan: '',
      diaChiCuThe: '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    })
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    shippingFee.value = 0

    if (state.detailKhachHang) {
      deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || ''
      deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || ''
      deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || ''
    }

    await fetchDiscounts(idHDS.value)
    showKhachHangModal.value = false
    toast.success('Chọn khách hàng thành công!')
  }
  catch (error) {
    console.error('Failed to select customer:', error)
    toast.error('Chọn khách hàng thất bại!')
  }
}

/**
 * Thêm khách hàng mới
 */
async function addCustomer() {
  try {
    if (!newCustomer.ten || !newCustomer.sdt) {
      toast.error('Vui lòng nhập đầy đủ thông tin khách hàng!')
      return
    }

    const phoneRegex = /^\d{10}$/
    if (!phoneRegex.test(newCustomer.sdt)) {
      toast.error('Số điện thoại phải là 10 chữ số!')
      return
    }

    if (!idHDS.value) {
      toast.error('Vui lòng chọn hoặc tạo hóa đơn trước khi thêm khách hàng!')
      return
    }

    addCustomerLoading.value = true

    const formData = new FormData()
    formData.append('ten', newCustomer.ten)
    formData.append('sdt', newCustomer.sdt)

    const response = await themMoiKhachHang(formData)
    const newCustomerId = response.data

    const selectFormData = new FormData()
    selectFormData.append('idHD', idHDS.value)
    selectFormData.append('idKH', newCustomerId)
    await themKhachHang(selectFormData)

    const responseKH = await GeOneKhachHang(idHDS.value)
    state.detailKhachHang = responseKH.id ? responseKH : null

    newCustomer.ten = ''
    newCustomer.sdt = ''

    toast.success('Thêm và chọn khách hàng thành công!')
  }
  catch (error) {
    console.error('Failed to add customer:', error)
    toast.error('Thêm khách hàng thất bại!')
  }
  finally {
    addCustomerLoading.value = false
  }
}

/**
 * Bỏ chọn khách hàng
 */
async function removeCustomer() {
  if (!idHDS.value) {
    toast.error('Không có hóa đơn được chọn!')
    return
  }

  try {
    await xoaKhachHang(idHDS.value)
    state.detailKhachHang = null
    toast.success('Đã bỏ chọn khách hàng!')

    // Reset thông tin giao hàng
    Object.assign(deliveryInfo, {
      tenNguoiNhan: '',
      sdtNguoiNhan: '',
      diaChiCuThe: '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    })

    // Cập nhật lại voucher (nếu có)
    if (state.gioHang.length > 0) {
      await fetchDiscounts(idHDS.value)
    }
  }
  catch (error) {
    console.error('Failed to remove customer:', error)
    toast.error('Bỏ chọn khách hàng thất bại!')
  }
}

/**
 * Xử lý bật/tắt giao hàng
 * @param isDeliveryEnableds - Trạng thái giao hàng
 */
async function giaoHang(isDeliveryEnableds: boolean) {
  isDeliveryEnabled.value = isDeliveryEnableds
  await suaGiaoHang(idHDS.value)
  await capNhatDanhSach()
}

/**
 * Xử lý thay đổi tỉnh/thành phố
 * @param value - Giá trị tỉnh/thành phố
 */
async function onProvinceChange(value: string) {
  deliveryInfo.tinhThanhPho = value
  const selectedProvince = provinces.value.find(p => p.value === value)
  provinceCode.value = selectedProvince ? Number.parseInt(selectedProvince.code) : null
  if (provinceCode.value) {
    await fetchDistricts(provinceCode.value)
    deliveryInfo.quanHuyen = null
    deliveryInfo.phuongXa = null
    districtCode.value = null
    wardCode.value = null
    await calculateShippingFee()
  }
  else {
    districts.value = []
    wards.value = []
    shippingFee.value = 0
    calculateTotalAmounts()
  }
}

/**
 * Xử lý thay đổi quận/huyện
 * @param value - Giá trị quận/huyện
 */
async function onDistrictChange(value: string) {
  deliveryInfo.quanHuyen = value
  const selectedDistrict = districts.value.find(d => d.value === value)
  districtCode.value = selectedDistrict ? Number.parseInt(selectedDistrict.code) : null
  if (districtCode.value) {
    await fetchWards(districtCode.value)
    deliveryInfo.phuongXa = null
    wardCode.value = null
    await calculateShippingFee()
  }
  else {
    wards.value = []
    shippingFee.value = 0
    calculateTotalAmounts()
  }
}

/**
 * Xử lý thay đổi phường/xã
 * @param value - Giá trị phường/xã
 */
async function onWardChange(value: string) {
  deliveryInfo.phuongXa = value
  const selectedWard = wards.value.find(w => w.value === value)
  wardCode.value = selectedWard ? selectedWard.code : null
  await calculateShippingFee()
}

/**
 * Trigger tự động áp dụng voucher
 */
async function triggerAutoApplyVoucher() {
  if (!idHDS.value || tienHang.value <= 0) {
    toast.error('Vui lòng có sản phẩm trong giỏ hàng trước!')
    return
  }

  if (state.gioHang.length === 0) {
    toast.info('Chưa có sản phẩm trong hóa đơn, không thể gợi ý voucher')
    return
  }

  autoApplying.value = true
  try {
    const params = {
      invoiceId: idHDS.value,
      tongTien: tienHang.value,
      customerId: state.detailKhachHang?.id ?? null,
    }

    const response = await getMaGiamGia(params)

    if (response) {
      state.autoVoucherResult = response

      if (!selectedVoucher.value && response.voucherApDung?.length > 0) {
        const bestVoucher = response.voucherApDung.reduce((best, current) =>
          (current.giamGiaThucTe || 0) > (best.giamGiaThucTe || 0) ? current : best,
        )
        await selectVoucher(bestVoucher)
      }

      toast.success(`Tìm thấy ${response.voucherApDung?.length || 0} voucher phù hợp`)
    }
  }
  catch (error) {
    console.error('Auto apply voucher failed:', error)
    toast.error('Lỗi khi tìm voucher phù hợp')
  }
  finally {
    autoApplying.value = false
  }
}

/**
 * Chọn voucher
 * @param voucher - Voucher được chọn
 */
async function selectVoucher(voucher: any) {
  applyingVoucher.value = voucher.voucherId

  try {
    selectedVoucher.value = voucher
    selectedDiscountCode.value = voucher.code
    giamGia.value = voucher.giamGiaThucTe

    selectedDiscount.value = {
      id: voucher.voucherId,
      ma: voucher.code,
      giaTriGiamThucTe: voucher.giamGiaThucTe,
      typeVoucher: voucher.typeVoucher,
      discountValue: voucher.discountValue,
      maxValue: voucher.maxValue,
      dieuKien: voucher.dieuKien,
    } as any

    calculateTotalAmounts()
    toast.success(`Đã áp dụng voucher ${voucher.code}`)
  }
  catch (error) {
    console.error('Select voucher failed:', error)
    toast.error('Lỗi khi áp dụng voucher')
  }
  finally {
    applyingVoucher.value = null
  }
}

/**
 * Bỏ chọn voucher
 */
function removeVoucher() {
  selectedVoucher.value = null
  selectedDiscount.value = null
  selectedDiscountCode.value = ''
  giamGia.value = 0
  calculateTotalAmounts()
  toast.info('Đã bỏ chọn voucher')
}

/**
 * Hiển thị chi tiết suggestion
 * @param suggestion - Suggestion được chọn
 */
function showSuggestionDetail(suggestion: any) {
  selectedSuggestion.value = suggestion
  showSuggestionDetailModal.value = true
}

/**
 * Xử lý phương thức thanh toán
 * @param method - Phương thức thanh toán (0: Tiền mặt, 1: Chuyển khoản, 2: Kết hợp)
 */
async function handlePaymentMethod(method: string) {
  if (!idHDS.value) {
    toast.error('Vui lòng chọn hoặc tạo hóa đơn trước khi chọn phương thức thanh toán!')
    return
  }

  if (state.gioHang.length === 0) {
    toast.error('Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi chọn phương thức thanh toán.')
    return
  }

  state.currentPaymentMethod = method

  try {
    if (method === '0') {
      toast.success('Đã chọn phương thức thanh toán Tiền mặt.')
    }
    else if (method === '1') {
      toast.success('Đã chọn phương thức thanh toán Chuyển khoản.')
      openQrModalVNPay()
    }
    else if (method === '2') {
      toast.success('Đã chọn phương thức thanh toán kết hợp (Tiền mặt + Chuyển khoản).')
      openBothPaymentModal()
    }
  }
  catch (error: any) {
    console.error('Error in handlePaymentMethod:', error)
    toast.error('Có lỗi khi chọn phương thức thanh toán!')
  }
}

/**
 * Mở modal thanh toán kết hợp
 */
function openBothPaymentModal() {
  amountPaid.value = 0
  bothPaymentMethod.value = 'CASH_FIRST'

  const remainingAmount = tongTien.value - tienKhachThanhToan.value

  if (remainingAmount <= 0) {
    toast.info('Hóa đơn đã được thanh toán đủ!')
    return
  }

  isBothPaymentModalVisible.value = true
}

/**
 * Đóng modal thanh toán kết hợp
 */
function closeBothPaymentModal() {
  isBothPaymentModalVisible.value = false
  amountPaid.value = 0
  bothPaymentLoading.value = false
}

/**
 * Xác nhận thanh toán kết hợp
 */
async function confirmBothPayment() {
  bothPaymentLoading.value = true

  try {
    if (!idHDS.value) {
      throw new Error('Không có hóa đơn được chọn!')
    }

    if (state.gioHang.length === 0) {
      throw new Error('Giỏ hàng trống!')
    }

    if (amountPaid.value <= 0) {
      throw new Error('Vui lòng nhập số tiền mặt khách đưa!')
    }

    const totalAmount = tongTien.value
    const paidAmount = tienKhachThanhToan.value
    const cashAmount = amountPaid.value
    const remainingAmount = totalAmount - paidAmount - cashAmount

    if (cashAmount > totalAmount - paidAmount) {
      throw new Error('Số tiền mặt không được lớn hơn số tiền cần thanh toán!')
    }

    if (remainingAmount > 0) {
      toast.info(
        `Đã nhận ${formatCurrency(cashAmount)} tiền mặt. `
        + `Vui lòng thanh toán ${formatCurrency(remainingAmount)} qua chuyển khoản.`,
      )

      setTimeout(() => {
        openQrModalVNPay()
      }, 1500)
    }
    else {
      toast.success('Thanh toán thành công bằng tiền mặt!')
    }

    const requestData = {
      idHD: idHDS.value,
      idNV: idNV.userId,
      tienHang: tienHang.value,
      tongTien: tongTien.value.toString(),
      tienMat: cashAmount,
      chuyenKhoan: remainingAmount > 0 ? remainingAmount : 0,
      phuongThucThanhToan: '2',
      giamGia: giamGia.value,
      bothPaymentMethod: bothPaymentMethod.value,
    }

    // await thanhToanThanhCong(requestData)

    await clickkActiveTab(activeTab.value, idHDS.value, loaiHD.value)
    closeBothPaymentModal()

    if (remainingAmount === 0) {
      toast.success('Xác nhận thanh toán kết hợp thành công!')
    }
  }
  catch (error: any) {
    console.error('Error in confirmBothPayment:', error)
    toast.error(error.message || 'Xác nhận thanh toán thất bại!')
  }
  finally {
    bothPaymentLoading.value = false
  }
}

/**
 * Mở modal QR VNPay
 */
function openQrModalVNPay() {
  isQrVNpayModalVisible.value = true
}

/**
 * Đóng modal QR VNPay
 */
function closeQrModalVnPay() {
  isQrVNpayModalVisible.value = false
}

/**
 * Xác nhận thanh toán
 * @param check - Loại xác nhận (0: thanh toán, 1: khác)
 */
async function xacNhan(check: number) {
  if (!idHDS.value) {
    toast.error('Vui lòng chọn một hóa đơn để xác nhận thanh toán!')
    return
  }

  if (state.gioHang.length === 0) {
    toast.error('Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi thanh toán.')
    return
  }

  if (isDeliveryEnabled.value) {
    if (
      !deliveryInfo.tenNguoiNhan
      || !deliveryInfo.sdtNguoiNhan
      || !deliveryInfo.diaChiCuThe
      || !deliveryInfo.tinhThanhPho
      || !deliveryInfo.quanHuyen
      || !deliveryInfo.phuongXa
    ) {
      toast.error('Vui lòng nhập đầy đủ thông tin giao hàng!')
      return
    }
  }

  try {
    const loaiHoaDon = isDeliveryEnabled.value ? 'GIAO_HANG' : 'TAI_QUAY'

    const requestData = {
      idHD: idHDS.value,
      idNV: idNV.userId,
      tienHang: tienHang.value,
      tongTien: tongTien.value.toString(),
      ten: deliveryInfo.tenNguoiNhan,
      sdt: deliveryInfo.sdtNguoiNhan,
      diaChi: isDeliveryEnabled.value ? getFullAddress() : '',
      tienShip: shippingFee.value,
      giamGia: giamGia.value,
      phuongThucThanhToan: state.currentPaymentMethod,
      idPGG: selectedDiscount.value?.id,
      check: isDeliveryEnabled.value ? 1 : 0,
      loaiHoaDon,
    }

    const res = await thanhToanThanhCong(requestData)

    if (res.message) {
      if (res.message.startsWith('Số') || res.message.startsWith('Phiếu')) {
        toast.error(res.message)
        await fetchDiscounts(idHDS.value)
        return
      }
    }

    toast.success('Thanh toán thành công!')
    await resetAfterPayment()
  }
  catch (error: any) {
    if (error?.response?.data?.message) {
      toast.error(error.response.data.message)
    }
    else {
      toast.error('Có lỗi xảy ra khi xác nhận thanh toán!')
      console.error('Lỗi khi xác nhận thanh toán:', error)
    }
  }
}

/**
 * Reset sau khi thanh toán
 */
async function resetAfterPayment() {
  await fetchProducts()
  await capNhatDanhSach()

  const indexToRemove = tabs.value.findIndex(tab => tab.idHD === idHDS.value)
  if (indexToRemove !== -1) {
    tabs.value.splice(indexToRemove, 1)
  }

  resetState()
  imeiDaChon.value = []
}

// ==================== COLUMNS DEFINITIONS ====================

/**
 * Cột cho bảng khách hàng
 */
const columnsKhachHang: DataTableColumns<KhachHangResponse> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => h(NText, { depth: 3 }, () => `${index + 1}`),
  },
  {
    title: 'Tên khách hàng',
    key: 'ten',
    width: 150,
    ellipsis: true,
  },
  {
    title: 'Số điện thoại',
    key: 'sdt',
    width: 130,
    align: 'center',
  },
  {
    title: 'Thao tác',
    key: 'operation',
    width: 90,
    align: 'center',
    render: row =>
      h(
        NButton,
        {
          type: 'primary',
          size: 'small',
          secondary: true,
          onClick: () => selectKhachHang(row.id),
        },
        { default: () => 'Chọn' },
      ),
  },
]

/**
 * Cột cho bảng giỏ hàng
 */
const columnsGiohang: DataTableColumns<any> = [
  {
    title: 'Serial đã chọn',
    key: 'imel',
    width: 110,
    render: (row) => {
      if (row.imel) {
        return h(
          NTag,
          {
            type: 'success',
            size: 'small',
            onClick: () => {
              toast.info(`IMEI: ${row.imel}`)
            },
          },
          () => `${row.imel}`,
        )
      }

      const imeiItem = imeiDaChon.value.find(item => item.idHoaDonChiTiet === row.idHDCT)

      if (imeiItem && imeiItem.danhSachImei.length > 0) {
        return h(
          NTag,
          {
            type: 'success',
            size: 'small',
            onClick: () => {
              toast.info(
                `Đã chọn ${imeiItem.danhSachImei.length} IMEI: ${imeiItem.danhSachImei.join(', ')}`,
              )
            },
          },
          () => `${imeiItem.danhSachImei.length} IMEI`,
        )
      }

      return h(
        NTag,
        {
          type: 'warning',
          size: 'small',
        },
        () => 'Chưa chọn IMEI',
      )
    },
  },
  {
    title: 'Ảnh',
    key: 'anh',
    width: 80,
    align: 'center',
    render: (row) => {
      return h(
        NBadge,
        {
          value: row.percentage ? `-${row.percentage}%` : undefined,
          type: 'error',
          offset: [-5, 0],
          style: { transform: 'scale(0.85)', transformOrigin: 'top right' },
        },
        {
          default: () =>
            h(NImage, {
              width: 100,
              height: 70,
              src: row.urlImage || row.anh,
              objectFit: 'cover',
              style: {
                'border-radius': '4px',
                'border': '1px solid #eee',
              },
            }),
        },
      )
    },
  },
  {
    title: 'Tên sản phẩm',
    key: 'name',
    width: 150,
    ellipsis: {
      tooltip: true,
    },
    render: row => h('div', { style: { fontWeight: 500 } }, row.name || row.ten),
  },
  {
    title: 'Thông số kỹ thuật',
    key: 'specifications',
    width: 200,
    ellipsis: {
      tooltip: true,
    },
    render: row =>
      h(
        NSpace,
        {
          vertical: true,
          size: 4,
        },
        () => [
          h(
            'div',
            {
              style: {
                display: 'flex',
                gap: '6px',
                flexWrap: 'wrap',
                marginBottom: '4px',
              },
            },
            [
              row.cpu
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#1677ff',
                    'background': '#e6f4ff',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #91caff',
                  },
                },
                `CPU: ${row.cpu}`,
              ),
              row.gpu
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#389e0d',
                    'background': '#f6ffed',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #b7eb8f',
                  },
                },
                `GPU: ${row.gpu}`,
              ),
              row.ram
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#d46b08',
                    'background': '#fff7e6',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #ffd591',
                  },
                },
                `RAM: ${row.ram}`,
              ),
            ],
          ),
          h(
            'div',
            {
              style: {
                display: 'flex',
                gap: '6px',
                flexWrap: 'wrap',
              },
            },
            [
              row.hardDrive
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#722ed1',
                    'background': '#f9f0ff',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #d3adf7',
                  },
                },
                `Ổ cứng: ${row.hardDrive}`,
              ),
              row.color
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#13c2c2',
                    'background': '#e6fffb',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #87e8de',
                  },
                },
                `Màu: ${row.color}`,
              ),
              row.material
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#eb2f96',
                    'background': '#fff0f6',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #ffadd2',
                  },
                },
                `Chất liệu: ${row.material}`,
              ),
            ],
          ),
        ],
      ),
  },
  {
    title: 'Đơn giá',
    key: 'price',
    width: 110,
    align: 'right',
    render: row =>
      h(
        NText,
        {
          style: { fontWeight: 500 },
        },
        () => formatCurrency(row.giaGoc),
      ),
  },
  {
    title: 'Giá bán',
    key: 'total',
    width: 120,
    align: 'right',
    render: row =>
      h(
        NText,
        {
          type: 'primary',
          strong: true,
          style: { fontSize: '14px' },
        },
        () => formatCurrency(row.giaGoc * (1 - row.percentage / 100)),
      ),
  },
  {
    title: 'Thao tác',
    key: 'operation',
    width: 80,
    align: 'center',
    render: row =>
      h(NTooltip, null, {
        trigger: () =>
          h(
            NButton,
            {
              type: 'error',
              size: 'small',
              text: true,
              circle: true,
              onClick: () => deleteProduc(row.id, row.idHDCT),
              style: { '--n-border-radius': '50%' },
            },
            {
              icon: () => h(NIcon, null, () => h(TrashOutline)),
            },
          ),
        default: () => 'Xóa sản phẩm',
      }),
  },
]

/**
 * Cột cho bảng sản phẩm
 */
const columns: DataTableColumns<ADProductDetailResponse> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => h(NText, { depth: 3 }, () => `${index + 1}`),
  },
  {
    title: 'Ảnh chi tiết',
    key: 'detailImages',
    width: 120,
    align: 'center',
    render: (row) => {
      const imageUrl = row.urlImage
      const percentage = row.percentage

      if (!imageUrl || imageUrl.trim() === '') {
        return h(NText, { depth: 3, size: 'small' }, () => 'Không có')
      }
      return h(
        NBadge,
        {
          value: percentage ? `-${percentage}%` : undefined,
          type: 'error',
          offset: [-5, 5],
        },
        {
          default: () =>
            h(NImage, {
              width: 100,
              height: 80,
              src: imageUrl,
              objectFit: 'cover',
              style: {
                borderRadius: '4px',
                border: '1px solid #f0f0f0',
                cursor: 'pointer',
              },
              fallbackSrc: '/images/no-image.png',
              previewSrc: imageUrl,
            }),
        },
      )
    },
  },
  {
    title: 'Mã',
    key: 'code',
    width: 100,
    ellipsis: true,
    render: row => h(NText, { strong: true }, () => row.code),
  },
  {
    title: 'Số lượng',
    key: 'quantity',
    width: 90,
    align: 'center',
    render: row =>
      h(
        NTag,
        {
          type: row.quantity > 0 ? 'success' : 'error',
          size: 'small',
          round: true,
        },
        () => row.quantity,
      ),
  },
  {
    title: 'Giá bán',
    key: 'price',
    width: 120,
    align: 'center',
    render: (row) => {
      const originalPrice = row.price || 0
      const percentage = row.percentage

      if (percentage && percentage > 0) {
        const discountedPrice = originalPrice * (1 - percentage / 100)

        return h(
          NSpace,
          { vertical: true, size: 0, align: 'center', justify: 'center' },
          () => [
            h(
              NText,
              { delete: true, depth: 3, style: { fontSize: '12px', lineHeight: '1.2' } },
              () => formatCurrency(originalPrice),
            ),
            h(
              NText,
              { type: 'primary', strong: true, style: { fontSize: '14px' } },
              () => formatCurrency(discountedPrice),
            ),
          ],
        )
      }
      return h(NText, { type: 'primary', strong: true }, () => formatCurrency(originalPrice))
    },
  },
  {
    title: 'Thông số kỹ thuật',
    key: 'specifications',
    width: 200,
    ellipsis: {
      tooltip: true,
    },
    render: row =>
      h(
        NSpace,
        {
          vertical: true,
          size: 4,
        },
        () => [
          h(
            'div',
            {
              style: {
                display: 'flex',
                gap: '6px',
                flexWrap: 'wrap',
                marginBottom: '4px',
              },
            },
            [
              row.cpu
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#1677ff',
                    'background': '#e6f4ff',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #91caff',
                  },
                },
                `CPU: ${row.cpu}`,
              ),
              row.gpu
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#389e0d',
                    'background': '#f6ffed',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #b7eb8f',
                  },
                },
                `GPU: ${row.gpu}`,
              ),
              row.ram
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#d46b08',
                    'background': '#fff7e6',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #ffd591',
                  },
                },
                `RAM: ${row.ram}`,
              ),
            ],
          ),
          h(
            'div',
            {
              style: {
                display: 'flex',
                gap: '6px',
                flexWrap: 'wrap',
              },
            },
            [
              row.hardDrive
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#722ed1',
                    'background': '#f9f0ff',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #d3adf7',
                  },
                },
                `Ổ cứng: ${row.hardDrive}`,
              ),
              row.color
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#13c2c2',
                    'background': '#e6fffb',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #87e8de',
                  },
                },
                `Màu: ${row.color}`,
              ),
              row.material
              && h(
                'span',
                {
                  style: {
                    'font-size': '11px',
                    'color': '#eb2f96',
                    'background': '#fff0f6',
                    'padding': '1px 6px',
                    'border-radius': '3px',
                    'border': '1px solid #ffadd2',
                  },
                },
                `Chất liệu: ${row.material}`,
              ),
            ],
          ),
        ],
      ),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
    align: 'center',
    render: row =>
      h(
        NTag,
        {
          type: row.status === 'ACTIVE' ? 'success' : 'error',
          size: 'small',
          round: true,
        },
        () => (row.status === 'ACTIVE' ? 'Hoạt động' : 'Không hoạt động'),
      ),
  },
  {
    title: 'Thao tác',
    key: 'operation',
    width: 120,
    align: 'center',
    render: row =>
      h(NSpace, { size: 8 }, () => [
        h(NTooltip, null, {
          trigger: () =>
            h(
              NButton,
              {
                type: 'primary',
                size: 'small',
                secondary: true,
                onClick: () => selectProductForSerial(row),
                disabled: row.status !== 'ACTIVE' || row.quantity <= 0,
              },
              { default: () => 'Chọn Serial' },
            ),
          default: () =>
            row.status !== 'ACTIVE'
              ? 'Sản phẩm không hoạt động'
              : row.quantity <= 0
                ? 'Hết hàng'
                : `Chọn serial từ ${row.quantity} sản phẩm có sẵn`,
        }),
      ]),
  },
]

/**
 * Cột cho bảng serial
 */
const serialColumns: DataTableColumns<ADPDImeiResponse> = [
  {
    title: 'Chọn',
    key: 'select',
    width: 60,
    align: 'center',
    render: row =>
      h(NCheckbox, {
        checked: selectedSerialIds.value.includes(row.id),
        disabled: row.imeiStatus !== 'AVAILABLE',
        onUpdateChecked: (checked) => {
          if (checked) {
            selectedSerialIds.value = [...selectedSerialIds.value, row.id]
          }
          else {
            selectedSerialIds.value = selectedSerialIds.value.filter(id => id !== row.id)
          }
        },
      }),
  },
  {
    title: 'Serial/IMEI',
    key: 'serialNumber',
    width: 180,
    render: row =>
      h(
        NText,
        {
          strong: true,
          code: true,
          style: {
            fontFamily: 'monospace',
            fontSize: '12px',
          },
        },
        () => row.code || '-',
      ),
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
      const statusConfig: Record<string, { type: any, text: string }> = {
        AVAILABLE: { type: 'success', text: 'Khả dụng' },
        SOLD: { type: 'warning', text: 'Đã bán' },
        DEFECTIVE: { type: 'error', text: 'Lỗi' },
        RESERVED: { type: 'info', text: 'Đã đặt' },
      }
      const config = statusConfig[row.imeiStatus] || {
        type: 'default',
        text: row.imeiStatus || 'Không xác định',
      }
      return h(
        NTag,
        {
          type: config.type,
          size: 'small',
          round: true,
        },
        () => config.text,
      )
    },
  },
  {
    title: 'Trạng thái SP',
    key: 'productStatus',
    width: 100,
    align: 'center',
    render: (row) => {
      const config = {
        ACTIVE: { type: 'success', text: 'Hoạt động' },
        INACTIVE: { type: 'default', text: 'Không HĐ' },
      }[row.status] || { type: 'default', text: row.status || '-' }
      return h(
        NTag,
        {
          type: config.type,
          size: 'small',
          round: true,
        },
        () => config.text,
      )
    },
  },
]

// ==================== LIFECYCLE HOOKS ====================

onMounted(async () => {
  // Lấy dữ liệu cho bộ lọc
  await fetchColor()
  await fetchCPU()
  await fetchGPU()
  await fetchRAM()
  await fetchHardDrive()
  await fetchMaterial()

  // Lấy danh sách hóa đơn và tỉnh/thành phố
  await fetchHoaDon()
  await fetchProvinces()

  // Thiết lập phương thức thanh toán mặc định
  setDefaultPaymentMethod()

  // Lấy voucher nếu có
  if (idHDS.value && state.gioHang.length > 0) {
    await fetchDiscounts(idHDS.value)
  }
})
</script>

<template>
  <div class="main-layout">
    <!-- ==================== CỘT TRÁI ==================== -->
    <div class="left-column">
      <!-- Card: Danh sách hóa đơn chờ -->
      <NCard class="card" size="small">
        <template #header>
          <NText type="primary" strong>
            Hóa đơn chờ
          </NText>
        </template>

        <template #header-extra>
          <NButton
            type="primary"
            size="small"
            class="btn-create-new-invoice"
            @click="createInvoice"
          >
            <template #icon>
              <NIcon><AddCircleOutline /></NIcon>
            </template>
            Tạo hóa đơn mới
          </NButton>
        </template>

        <div class="pending-invoices-container">
          <NScrollbar x-scrollable>
            <div class="pending-invoices-wrapper">
              <NSpace :wrap="false">
                <!-- Danh sách tab hóa đơn -->
                <div
                  v-for="tab in tabs"
                  :key="tab.id"
                  class="pending-invoice-card"
                  :class="{ active: activeTab === tab.id }"
                  @click="clickkActiveTab(tab.id, tab.idHD, tab.loaiHoaDon)"
                >
                  <div class="invoice-header">
                    <NText strong>
                      {{ tab.code }}
                    </NText>

                    <!-- Nút hủy hóa đơn (có popconfirm nếu có sản phẩm) -->
                    <NPopconfirm
                      v-if="tab.soLuong > 0"
                      :show-icon="false"
                      positive-text="Xác nhận hủy"
                      negative-text="Hủy bỏ"
                      @positive-click="() => huy(tab.idHD)"
                    >
                      <template #trigger>
                        <NButton text type="error" size="tiny" @click.stop>
                          <NIcon><TrashOutline /></NIcon>
                        </NButton>
                      </template>
                      <div class="popconfirm-content">
                        <NText strong style="display: block; margin-bottom: 8px">
                          Xác nhận hủy
                        </NText>
                        <NText depth="3">
                          Hành động này không thể hoàn tác.
                        </NText>
                      </div>
                    </NPopconfirm>

                    <!-- Nút hủy hóa đơn (không popconfirm) -->
                    <NButton
                      v-else
                      text
                      type="error"
                      size="tiny"
                      @click.stop="huy(tab.idHD)"
                    >
                      <NIcon><TrashOutline /></NIcon>
                    </NButton>
                  </div>

                  <NSpace vertical :size="4">
                    <NTag type="warning" size="small" round>
                      Chờ xử lý
                    </NTag>
                    <NText depth="3">
                      {{ tab.soLuong || 0 }} sản phẩm
                    </NText>
                  </NSpace>
                </div>
              </NSpace>
            </div>
          </NScrollbar>
        </div>
      </NCard>

      <!-- Card: Giỏ hàng -->
      <NCard class="card" size="small">
        <template #header>
          <NText type="primary" strong>
            Giỏ hàng
          </NText>
        </template>

        <template #header-extra>
          <NSpace>
            <NButton type="primary" size="small" @click="openProductSelectionModal">
              Chọn sản phẩm
            </NButton>
          </NSpace>
        </template>

        <!-- Hiển thị giỏ hàng -->
        <div v-if="state.gioHang.length > 0">
          <NDataTable
            :columns="columnsGiohang"
            :data="state.gioHang"
            :max-height="280"
            size="small"
            :pagination="{ pageSize: 5 }"
          />
        </div>
        <NEmpty v-else description="Không có sản phẩm nào trong giỏ hàng" size="small" />
      </NCard>

      <!-- Card: Thông tin giao hàng (chỉ hiển thị khi bật giao hàng) -->
      <NCard v-if="isDeliveryEnabled" class="card" size="small">
        <template #header>
          <NText type="primary" strong>
            Thông tin người nhận
          </NText>
        </template>

        <NForm ref="deliveryForm" :model="deliveryInfo" label-placement="left" :label-width="140">
          <NGrid :cols="12" :x-gap="12" :y-gap="12">
            <!-- Tên người nhận -->
            <NFormItemGi :span="6" path="tenNguoiNhan" label="Tên người nhận" required>
              <NInput
                v-model:value="deliveryInfo.tenNguoiNhan"
                placeholder="Nhập tên người nhận"
                clearable
              />
            </NFormItemGi>

            <!-- Số điện thoại -->
            <NFormItemGi :span="6" path="sdtNguoiNhan" label="Số điện thoại" required>
              <NInput
                v-model:value="deliveryInfo.sdtNguoiNhan"
                placeholder="Nhập số điện thoại"
                clearable
              />
            </NFormItemGi>

            <!-- Tỉnh/Thành phố -->
            <NFormItemGi :span="4" path="tinhThanhPho" label="Tỉnh/Thành phố" required>
              <NSelect
                v-model:value="deliveryInfo.tinhThanhPho"
                placeholder="Chọn tỉnh/thành phố"
                :options="provinces"
                filterable
                clearable
                @update:value="onProvinceChange"
              />
            </NFormItemGi>

            <!-- Quận/Huyện -->
            <NFormItemGi :span="4" path="quanHuyen" label="Quận/Huyện" required>
              <NSelect
                v-model:value="deliveryInfo.quanHuyen"
                placeholder="Chọn quận/huyện"
                :options="districts"
                filterable
                clearable
                @update:value="onDistrictChange"
              />
            </NFormItemGi>

            <!-- Phường/Xã -->
            <NFormItemGi :span="4" path="phuongXa" label="Phường/Xã" required>
              <NSelect
                v-model:value="deliveryInfo.phuongXa"
                placeholder="Chọn phường/xã"
                :options="wards"
                filterable
                clearable
                @update:value="onWardChange"
              />
            </NFormItemGi>

            <!-- Địa chỉ cụ thể -->
            <NFormItemGi :span="12" path="diaChiCuThe" label="Địa chỉ cụ thể" required>
              <NInput
                v-model:value="deliveryInfo.diaChiCuThe"
                placeholder="Nhập số nhà, tên đường..."
                clearable
              />
            </NFormItemGi>

            <!-- Ghi chú -->
            <NFormItemGi :span="12" path="ghiChu" label="Ghi chú">
              <NInput
                v-model:value="deliveryInfo.ghiChu"
                type="textarea"
                placeholder="Nhập ghi chú (nếu có)"
                :rows="3"
              />
            </NFormItemGi>
          </NGrid>
        </NForm>
      </NCard>
    </div>

    <!-- ==================== CỘT PHẢI ==================== -->
    <div class="right-column">
      <!-- Card: Thông tin khách hàng -->
      <NCard class="card" size="small">
        <template #header>
          <NSpace justify="space-between" align="center">
            <NText type="primary" strong>
              Khách hàng
            </NText>
            <!-- Nút bỏ chọn khách hàng -->
            <NButton
              v-if="state.detailKhachHang"
              type="error"
              size="tiny"
              text
              @click="removeCustomer"
            >
              <template #icon>
                <NIcon><CloseOutline /></NIcon>
              </template>
              Bỏ chọn
            </NButton>
          </NSpace>
        </template>

        <!-- Hiển thị thông tin khách hàng đã chọn -->
        <div v-if="state.detailKhachHang">
          <NSpace vertical :size="16">
            <div>
              <NText depth="3">
                Tên khách hàng
              </NText>
              <NInput :value="state.detailKhachHang.ten" readonly class="mt-1" />
            </div>
            <div>
              <NText depth="3">
                Số điện thoại
              </NText>
              <NInput :value="state.detailKhachHang.sdt" readonly class="mt-1" />
            </div>
          </NSpace>
        </div>

        <!-- Form thêm khách hàng mới -->
        <div v-else>
          <NSpace vertical :size="16">
            <div>
              <NText depth="3">
                Tên khách hàng
              </NText>
              <NInput
                v-model:value="newCustomer.ten"
                placeholder="Tên khách hàng"
                class="mt-1"
              />
            </div>
            <div>
              <NText depth="3">
                Số điện thoại
              </NText>
              <NInput
                v-model:value="newCustomer.sdt"
                placeholder="Số điện thoại"
                class="mt-1"
              />
            </div>
          </NSpace>
        </div>

        <template #footer>
          <NSpace>
            <NButton type="primary" secondary @click="showKhachHangModal = true">
              Chọn khách hàng
            </NButton>
            <NButton
              type="primary"
              :loading="addCustomerLoading"
              secondary
              @click="addCustomer"
            >
              Thêm khách hàng
            </NButton>
          </NSpace>
        </template>
      </NCard>

      <!-- Card: Danh sách voucher khả dụng -->
      <NCard
        v-if="idHDS && state.gioHang.length > 0 && state.autoVoucherResult?.voucherApDung?.length > 0"
        class="card"
        size="small"
        :segmented="{ content: true }"
      >
        <template #header>
          <NSpace align="center" justify="space-between" style="width: 100%">
            <NText type="primary" strong>
              <NIcon :component="TicketOutline" style="margin-right: 6px;" />
              Voucher khả dụng
            </NText>
            <NTag type="success" size="small" round>
              {{ state.autoVoucherResult.voucherApDung.length }} mã
            </NTag>
          </NSpace>
        </template>

        <!-- Voucher đang được áp dụng -->
        <div v-if="selectedVoucher" class="current-voucher-section mb-3">
          <NAlert type="success" :show-icon="true" title="Đang áp dụng">
            <NSpace vertical :size="8">
              <NSpace justify="space-between" align="center">
                <NText strong>
                  {{ selectedVoucher.code }}
                </NText>
                <NTag type="success" size="small">
                  {{
                    selectedVoucher.typeVoucher === 'PERCENTAGE'
                      ? `${selectedVoucher.discountValue}%`
                      : 'Giảm cố định'
                  }}
                </NTag>
              </NSpace>
              <NSpace justify="space-between">
                <NText depth="3">
                  Giá trị giảm:
                </NText>
                <NText type="success" strong>
                  -{{ formatCurrency(selectedVoucher.giamGiaThucTe) }}
                </NText>
              </NSpace>
              <NSpace v-if="selectedVoucher.dieuKien > 0" justify="space-between">
                <NText depth="3">
                  Điều kiện:
                </NText>
                <NText depth="3">
                  {{ formatCurrency(selectedVoucher.dieuKien) }}
                </NText>
              </NSpace>
            </NSpace>
          </NAlert>
        </div>

        <!-- Danh sách voucher -->
        <NScrollbar style="max-height: 200px;">
          <NList size="small" bordered>
            <NListItem
              v-for="voucher in state.autoVoucherResult.voucherApDung"
              :key="voucher.voucherId"
              :class="{ 'active-voucher': selectedVoucher?.voucherId === voucher.voucherId }"
            >
              <NThing :title="voucher.code">
                <template #avatar>
                  <NTag :type="getVoucherTagType(voucher.typeVoucher)" size="small">
                    {{
                      voucher.typeVoucher === 'PERCENTAGE'
                        ? `${voucher.discountValue}%`
                        : 'Cố định'
                    }}
                  </NTag>
                </template>
                <template #description>
                  <NSpace vertical :size="3" style="margin-top: 4px">
                    <NSpace justify="space-between">
                      <NText depth="3" size="small">
                        Giảm:
                      </NText>
                      <NText strong class="text-success" size="small">
                        {{ formatCurrency(voucher.giamGiaThucTe) }}
                      </NText>
                    </NSpace>
                    <NSpace v-if="voucher.dieuKien > 0" justify="space-between">
                      <NText depth="3" size="small">
                        Điều kiện:
                      </NText>
                      <NText depth="3" size="small">
                        {{ formatCurrency(voucher.dieuKien) }}
                      </NText>
                    </NSpace>
                  </NSpace>
                </template>
              </NThing>
              <template #suffix>
                <NButton
                  type="primary"
                  size="small"
                  :disabled="selectedVoucher?.voucherId === voucher.voucherId"
                  :loading="applyingVoucher === voucher.voucherId"
                  @click="selectVoucher(voucher)"
                >
                  {{ selectedVoucher?.voucherId === voucher.voucherId ? 'Đã chọn' : 'Chọn' }}
                </NButton>
              </template>
            </NListItem>
          </NList>
        </NScrollbar>
      </NCard>

      <!-- Card: Gợi ý mua thêm để được voucher tốt hơn -->
      <NCard
        v-if="idHDS && state.gioHang.length > 0 && state.autoVoucherResult?.voucherTotHon?.length > 0"
        class="card"
        size="small"
        :segmented="{ content: true }"
      >
        <template #header>
          <NSpace align="center" justify="space-between" style="width: 100%">
            <NText type="primary" strong>
              <NIcon :component="RocketOutline" style="margin-right: 6px;" />
              Gợi ý mua thêm
            </NText>
            <NTag type="warning" size="small" round>
              {{ state.autoVoucherResult.voucherTotHon.length }} đề xuất
            </NTag>
          </NSpace>
        </template>

        <NScrollbar style="max-height: 250px;">
          <NList size="small" bordered>
            <NListItem
              v-for="(suggestion, index) in state.autoVoucherResult.voucherTotHon"
              :key="suggestion.voucherId"
              :class="{ 'active-suggestion': index === 0 && suggestion.hieuQua >= 50 }"
            >
              <NThing :title="suggestion.code">
                <template #avatar>
                  <NTag :type="getSuggestionTagType(suggestion.hieuQua)" size="small" round>
                    {{ suggestion.hieuQua }}%
                  </NTag>
                </template>
                <template #description>
                  <NSpace vertical :size="4" style="margin-top: 4px">
                    <NSpace justify="space-between">
                      <NText depth="3" size="small">
                        Cần mua thêm:
                      </NText>
                      <NText strong class="text-warning" size="small">
                        {{ formatCurrency(suggestion.canMuaThem) }}
                      </NText>
                    </NSpace>
                    <NSpace justify="space-between">
                      <NText depth="3" size="small">
                        Giảm thêm:
                      </NText>
                      <NText strong class="text-success" size="small">
                        +{{ formatCurrency(suggestion.giamThem) }}
                      </NText>
                    </NSpace>
                  </NSpace>
                </template>
              </NThing>
              <template #suffix>
                <NButton
                  type="warning"
                  size="small"
                  :loading="applyingBetterVoucher === suggestion.voucherId"
                  @click="showSuggestionDetail(suggestion)"
                >
                  Chi tiết
                </NButton>
              </template>
            </NListItem>
          </NList>
        </NScrollbar>
      </NCard>

      <!-- Card: Thông tin đơn hàng và thanh toán -->
      <NCard class="card" size="small">
        <template #header>
          <NSpace justify="space-between" align="center" style="width: 100%">
            <NText type="primary" strong>
              Thông tin đơn hàng
            </NText>
            <NSpace align="center">
              <NText depth="3">
                Giao hàng
              </NText>
              <NSwitch
                v-model:value="isDeliveryEnabled"
                size="small"
                @update:value="giaoHang"
              />
              <NButton
                v-if="state.gioHang.length > 0"
                type="primary"
                size="small"
                :loading="autoApplying"
                secondary
                circle
                @click="triggerAutoApplyVoucher"
              >
                <template #icon>
                  <NIcon><RocketOutline /></NIcon>
                </template>
              </NButton>
            </NSpace>
          </NSpace>
        </template>

        <NSpace vertical :size="16">
          <!-- Mã giảm giá đã chọn -->
          <div v-if="idHDS && state.gioHang.length > 0">
            <NText depth="3">
              Mã giảm giá
            </NText>
            <NSpace align="center" class="mt-1">
              <NInput
                v-model:value="selectedDiscountCode"
                placeholder="Chọn mã giảm giá"
                readonly
                style="flex: 1"
              >
                <template #prefix>
                  <NIcon :component="TicketOutline" />
                </template>
              </NInput>
              <NButton
                v-if="selectedVoucher"
                type="error"
                size="small"
                secondary
                @click="removeVoucher"
              >
                Bỏ chọn
              </NButton>
            </NSpace>
          </div>

          <NDivider style="margin: 8px 0" />

          <!-- Chi tiết tổng tiền -->
          <NSpace vertical :size="12">
            <!-- Tổng tiền hàng -->
            <NSpace justify="space-between">
              <NText depth="3">
                Tổng tiền hàng:
              </NText>
              <NText strong>
                {{ formatCurrency(tienHang) }}
              </NText>
            </NSpace>

            <!-- Giảm giá -->
            <NSpace v-if="giamGia > 0 && tienHang > 0" justify="space-between">
              <NText depth="3">
                Giảm giá:
              </NText>
              <NText type="success">
                -{{ formatCurrency(giamGia) }}
              </NText>
            </NSpace>

            <!-- Phí vận chuyển -->
            <template v-if="isDeliveryEnabled">
              <NSpace justify="space-between" align="center">
                <NText depth="3">
                  Phí vận chuyển:
                </NText>
                <NSpace align="center">
                  <NInputNumber
                    v-model:value="shippingFee"
                    :min="0"
                    :formatter="formatCurrencyInput"
                    :parser="parseCurrency"
                    :disabled="isFreeShipping"
                    size="small"
                    style="width: 120px"
                  />
                  <img src="/images/ghn-logo.webp" alt="GHN Logo" style="height: 20px">
                </NSpace>
              </NSpace>
              <NAlert v-if="isFreeShipping" type="success" size="small" show-icon>
                Miễn phí vận chuyển (Đơn hàng trên 5,000,000 VND)
              </NAlert>
            </template>

            <NDivider style="margin: 8px 0" />

            <!-- Tổng thanh toán -->
            <NSpace justify="space-between">
              <NText strong size="large">
                Tổng thanh toán:
              </NText>
              <NText strong type="primary" size="large">
                {{ formatCurrency(tongTien) }}
              </NText>
            </NSpace>

            <!-- Thông báo tiết kiệm -->
            <NAlert v-if="giamGia > 0 && tienHang > 0" type="success" size="small" show-icon>
              <NSpace justify="space-between" align="center">
                <span>Bạn tiết kiệm được:</span>
                <NText strong type="success">
                  {{ formatCurrency(giamGia) }}
                </NText>
              </NSpace>
            </NAlert>
          </NSpace>

          <NDivider style="margin: 12px 0" />

          <!-- Phương thức thanh toán -->
          <NSpace v-if="state.gioHang.length > 0" vertical :size="8">
            <NText depth="3">
              Phương thức thanh toán:
            </NText>
            <NSpace>
              <NButton
                :type="state.currentPaymentMethod === '0' ? 'primary' : 'default'"
                size="small"
                secondary
                @click="handlePaymentMethod('0')"
              >
                <template #icon>
                  <NIcon><MoonOutline /></NIcon>
                </template>
                Tiền mặt
              </NButton>
              <NButton
                :type="state.currentPaymentMethod === '1' ? 'primary' : 'default'"
                size="small"
                secondary
                @click="handlePaymentMethod('1')"
              >
                <template #icon>
                  <NIcon><QrCodeOutline /></NIcon>
                </template>
                Chuyển khoản
              </NButton>
              <NButton
                :type="state.currentPaymentMethod === '2' ? 'primary' : 'default'"
                size="small"
                secondary
                @click="handlePaymentMethod('2')"
              >
                <template #icon>
                  <NIcon><WalletOutline /></NIcon>
                </template>
                Tiền mặt + CK
              </NButton>
            </NSpace>
          </NSpace>

          <!-- Nút xác nhận thanh toán -->
          <NPopconfirm
            v-if="state.gioHang.length > 0"
            positive-text="Đồng ý"
            negative-text="Hủy"
            @positive-click="xacNhan(0)"
          >
            <template #trigger>
              <NButton type="primary" size="large" style="width: 100%; margin-top: 8px">
                Xác nhận thanh toán
              </NButton>
            </template>
            <NText depth="3">
              Bạn có chắc chắn muốn xác nhận thanh toán hóa đơn này?
            </NText>
          </NPopconfirm>
          <NButton
            v-else
            type="primary"
            size="large"
            style="width: 100%; margin-top: 8px"
            disabled
          >
            Xác nhận thanh toán
          </NButton>
        </NSpace>
      </NCard>
    </div>

    <!-- ==================== MODAL ==================== -->

    <!-- Modal thanh toán kết hợp (Tiền mặt + Chuyển khoản) -->
    <NModal
      v-model:show="isBothPaymentModalVisible"
      preset="dialog"
      title="Thanh toán kết hợp"
      style="width: 500px"
      :mask-closable="false"
    >
      <NCard size="small" :bordered="false">
        <NSpace vertical :size="20">
          <!-- Thông tin đơn hàng -->
          <NCard size="small" :bordered="true" embedded>
            <NSpace vertical :size="12">
              <NSpace justify="space-between">
                <NText depth="3">
                  Tổng tiền hàng:
                </NText>
                <NText strong>
                  {{ formatCurrency(tienHang) }}
                </NText>
              </NSpace>

              <NSpace v-if="giamGia > 0" justify="space-between">
                <NText depth="3">
                  Giảm giá:
                </NText>
                <NText type="success">
                  -{{ formatCurrency(giamGia) }}
                </NText>
              </NSpace>

              <NSpace v-if="shippingFee > 0" justify="space-between">
                <NText depth="3">
                  Phí vận chuyển:
                </NText>
                <NText>{{ formatCurrency(shippingFee) }}</NText>
              </NSpace>

              <NDivider style="margin: 4px 0" />

              <NSpace justify="space-between">
                <NText strong>
                  Tổng thanh toán:
                </NText>
                <NText strong type="primary" style="font-size: 18px">
                  {{ formatCurrency(tongTien) }}
                </NText>
              </NSpace>

              <NSpace v-if="tienKhachThanhToan > 0" justify="space-between">
                <NText depth="3">
                  Đã thanh toán:
                </NText>
                <NText type="success">
                  {{ formatCurrency(tienKhachThanhToan) }}
                </NText>
              </NSpace>

              <NSpace justify="space-between">
                <NText depth="3">
                  Còn lại:
                </NText>
                <NText type="error" strong>
                  {{ formatCurrency(tongTien - tienKhachThanhToan) }}
                </NText>
              </NSpace>
            </NSpace>
          </NCard>

          <!-- Form nhập tiền mặt -->
          <NForm>
            <NFormItem label="Số tiền mặt khách đưa" required>
              <NInputNumber
                v-model:value="amountPaid"
                placeholder="Nhập số tiền mặt"
                style="width: 100%"
                :min="0"
                :max="tongTien - tienKhachThanhToan"
                :formatter="formatCurrencyInput"
                :parser="parseCurrency"
              >
                <template #prefix>
                  <NIcon><MoonOutline /></NIcon>
                </template>
              </NInputNumber>
            </NFormItem>
          </NForm>

          <!-- Thông tin thanh toán -->
          <NAlert type="info" :show-icon="true">
            <template #icon>
              <NIcon><InformationCircleOutline /></NIcon>
            </template>
            <NSpace vertical :size="8">
              <div>
                <NText>
                  Tiền mặt: <strong>{{ formatCurrency(amountPaid || 0) }}</strong>
                </NText>
              </div>
              <div>
                <NText>
                  Chuyển khoản:
                  <strong class="text-primary">
                    {{
                      formatCurrency(
                        Math.max(0, tongTien - tienKhachThanhToan - (amountPaid || 0)),
                      )
                    }}
                  </strong>
                </NText>
              </div>
            </NSpace>
          </NAlert>

          <!-- Chọn thứ tự thanh toán -->
          <NCard size="small" :bordered="true">
            <template #header>
              <NText strong>
                Thứ tự thanh toán
              </NText>
            </template>
            <NSpace vertical :size="12">
              <NRadioGroup v-model:value="bothPaymentMethod">
                <NSpace vertical>
                  <NRadio value="CASH_FIRST">
                    <NSpace align="center">
                      <NIcon><MoonOutline /></NIcon>
                      <span>Tiền mặt trước, chuyển khoản sau</span>
                    </NSpace>
                  </NRadio>
                  <NRadio value="BANKING_FIRST">
                    <NSpace align="center">
                      <NIcon><QrCodeOutline /></NIcon>
                      <span>Chuyển khoản trước, tiền mặt sau</span>
                    </NSpace>
                  </NRadio>
                </NSpace>
              </NRadioGroup>
            </NSpace>
          </NCard>

          <!-- QR Code nếu chọn chuyển khoản trước -->
          <div v-if="bothPaymentMethod === 'BANKING_FIRST'" class="text-center">
            <NDivider>Quét mã QR để chuyển khoản</NDivider>
            <NImage
              :src="qrCodeUrl"
              width="200"
              height="200"
              object-fit="contain"
              preview-disabled
              class="mx-auto"
            />
            <NText depth="3" size="small" style="display: block; margin-top: 8px">
              Số tiền cần chuyển:
              {{
                formatCurrency(Math.max(0, tongTien - tienKhachThanhToan - (amountPaid || 0)))
              }}
            </NText>
          </div>
        </NSpace>
      </NCard>

      <template #action>
        <NSpace justify="end">
          <NButton @click="closeBothPaymentModal">
            Hủy
          </NButton>
          <NButton
            type="primary"
            :loading="bothPaymentLoading"
            :disabled="amountPaid <= 0"
            @click="confirmBothPayment"
          >
            <template #icon>
              <NIcon><CheckmarkOutline /></NIcon>
            </template>
            Xác nhận thanh toán
          </NButton>
        </NSpace>
      </template>
    </NModal>

    <!-- Modal chọn khách hàng -->
    <NModal
      v-model:show="showKhachHangModal"
      preset="dialog"
      title="Chọn khách hàng"
      style="width: 90%; max-width: 1000px"
      :mask-closable="false"
    >
      <NCard size="small" :bordered="false">
        <NSpace vertical :size="16">
          <!-- Ô tìm kiếm -->
          <NInput
            v-model:value="customerSearchQuery"
            placeholder="Tìm kiếm theo tên hoặc số điện thoại..."
            clearable
          >
            <template #prefix>
              <NIcon><SearchOutline /></NIcon>
            </template>
          </NInput>

          <!-- Bảng danh sách khách hàng -->
          <NDataTable
            :columns="columnsKhachHang"
            :data="state.khachHang"
            :max-height="400"
            size="small"
            :pagination="{
              page: state.paginationParams.page,
              pageSize: state.paginationParams.size,
              pageCount: Math.ceil(state.totalItemsKH / state.paginationParams.size),
              showSizePicker: true,
              pageSizes: [10, 20, 30, 40, 50],
            }"
            @update:page="handleCustomerPageChange"
            @update:page-size="handleCustomerPageSizeChange"
          />
        </NSpace>
      </NCard>
    </NModal>

    <!-- Modal chọn sản phẩm -->
    <NModal
      v-model:show="showProductModal"
      preset="dialog"
      title="Chọn sản phẩm"
      style="width: 90%; max-width: 1400px"
      :mask-closable="false"
    >
      <NCard size="small" :bordered="false">
        <template #header>
          <NSpace vertical :size="12">
            <!-- Hàng 1: Tìm kiếm và khoảng giá -->
            <NGrid :cols="24" :x-gap="12" :y-gap="12">
              <NGi :span="12">
                <NInput
                  v-model:value="localSearchQuery"
                  placeholder="Tìm kiếm sản phẩm..."
                  clearable
                />
              </NGi>
              <NGi :span="12">
                <NFormItem label="Khoảng giá" label-placement="left">
                  <NSpace vertical :size="12" style="width: 100%">
                    <n-slider
                      v-model:value="priceRange"
                      range
                      :step="100000"
                      :min="stateMinMaxPrice.priceMin"
                      :max="stateMinMaxPrice.priceMax"
                      :format-tooltip="formatTooltipRangePrice"
                      style="width: 100%"
                    />
                    <NSpace justify="space-between" style="width: 100%">
                      <NText depth="3">
                        {{ formatCurrency(priceRange[0]) }}
                      </NText>
                      <NText depth="3">
                        {{ formatCurrency(priceRange[1]) }}
                      </NText>
                    </NSpace>
                  </NSpace>
                </NFormItem>
              </NGi>
            </NGrid>

            <!-- Hàng 2: Bộ lọc thông số -->
            <NGrid :cols="24" :x-gap="12" :y-gap="12">
              <NGi :span="4">
                <NSelect
                  v-model:value="localColor"
                  :options="ColorOptions"
                  placeholder="Màu sắc"
                  clearable
                />
              </NGi>
              <NGi :span="4">
                <NSelect
                  v-model:value="localCPU"
                  :options="CpuOptions"
                  placeholder="CPU"
                  clearable
                />
              </NGi>
              <NGi :span="4">
                <NSelect
                  v-model:value="localGPU"
                  :options="GpuOptions"
                  placeholder="GPU"
                  clearable
                />
              </NGi>
              <NGi :span="4">
                <NSelect
                  v-model:value="localRAM"
                  :options="RamOptions"
                  placeholder="RAM"
                  clearable
                />
              </NGi>
              <NGi :span="4">
                <NSelect
                  v-model:value="localHardDrive"
                  :options="HardDriveOptions"
                  placeholder="Ổ cứng"
                  clearable
                />
              </NGi>
              <NGi :span="4">
                <NSelect
                  v-model:value="localSelectedMaterial"
                  :options="MaterialOptions"
                  placeholder="Chất liệu"
                  clearable
                />
              </NGi>
            </NGrid>

            <!-- Hàng 3: Nút đặt lại bộ lọc -->
            <NGrid :cols="24" :x-gap="12">
              <NGi :span="24">
                <NSpace justify="end">
                  <NButton type="default" size="small" secondary @click="resetFilters">
                    <template #icon>
                      <NIcon><ReloadOutline /></NIcon>
                    </template>
                    Đặt lại bộ lọc
                  </NButton>
                </NSpace>
              </NGi>
            </NGrid>
          </NSpace>
        </template>

        <!-- Bảng danh sách sản phẩm -->
        <NDataTable
          :columns="columns"
          :data="stateSP.products"
          :max-height="400"
          size="small"
          :pagination="{
            page: stateSP.paginationParams.page,
            pageSize: stateSP.paginationParams.size,
            pageCount: Math.ceil(stateSP.totalItems / stateSP.paginationParams.size),
            showSizePicker: true,
            pageSizes: [10, 20, 30, 40, 50],
          }"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </NCard>
    </NModal>

    <!-- Modal chọn serial IMEI -->
    <NModal
      v-model:show="showSerialModal"
      preset="dialog"
      title="Chọn Serial Sản Phẩm"
      style="width: 90%; max-width: 1200px"
      :mask-closable="false"
    >
      <NCard size="small" :bordered="false">
        <template #header>
          <NSpace align="center" justify="space-between" class="serial-modal-header">
            <div>
              <NText strong>
                {{ selectedProductDetail?.name }} - {{ selectedProductDetail?.code }}
              </NText>
              <NText depth="3" size="small" style="display: block; margin-top: 4px">
                Giá: {{ formatCurrency(selectedProductDetail?.price || 0) }}
              </NText>
            </div>
            <div style="text-align: right">
              <NText type="primary" strong>
                Còn {{ availableSerialsCount }} serial khả dụng
              </NText>
              <NText depth="3" size="small" style="display: block; margin-top: 4px">
                Tổng: {{ selectedSerials.length }} serial
              </NText>
            </div>
          </NSpace>
        </template>

        <!-- Loading -->
        <div v-if="loadingSerials" style="text-align: center; padding: 40px">
          <n-spin size="large">
            <template #description>
              Đang tải danh sách serial...
            </template>
          </n-spin>
        </div>

        <!-- Bảng danh sách serial -->
        <NDataTable
          v-else
          :columns="serialColumns"
          :data="selectedSerials"
          :max-height="400"
          size="small"
          :pagination="{ pageSize: 10 }"
          :loading="loadingSerials"
          :bordered="false"
        />

        <template #footer>
          <NSpace justify="space-between" align="center" style="width: 100%">
            <NText depth="3">
              Đã chọn {{ selectedSerialIds.length }} serial
            </NText>
            <NSpace>
              <NButton @click="showSerialModal = false">
                Hủy
              </NButton>
              <NButton
                type="primary"
                :disabled="selectedSerialIds.length === 0 || loadingSerials"
                :loading="loadingSerials"
                @click="addSerialToCart"
              >
                Thêm {{ selectedSerialIds.length }} serial vào giỏ
              </NButton>
            </NSpace>
          </NSpace>
        </template>
      </NCard>
    </NModal>

    <!-- Modal QR VNPay -->
    <NModal
      v-model:show="isQrVNpayModalVisible"
      preset="dialog"
      title="Quét QR thanh toán"
      positive-text="Đã thanh toán"
      :mask-closable="false"
      @positive-click="closeQrModalVnPay"
    >
      <NSpace vertical align="center" :size="16">
        <NText depth="3">
          Quét mã QR để thanh toán qua VNPay
        </NText>
        <NImage
          src="/images/qr.png"
          width="200"
          height="200"
          object-fit="contain"
          preview-disabled
        />
      </NSpace>
    </NModal>
  </div>
</template>

<style scoped>
/* ==================== LAYOUT STYLES ==================== */
.main-layout {
  display: flex;
  gap: 16px;
  padding: 16px;
  height: calc(100vh - 64px);
  background: #f5f5f5;
}

.left-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.right-column {
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ==================== PENDING INVOICE STYLES ==================== */
.pending-invoices-container {
  width: 100%;
}

.pending-invoices-wrapper {
  width: 100%;
}

.pending-invoice-card {
  min-width: 200px;
  padding: 12px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.pending-invoice-card:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.pending-invoice-card.active {
  border-color: #1890ff;
  background: #f0f9ff;
}

.invoice-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

/* ==================== UTILITY STYLES ==================== */
.mt-1 {
  margin-top: 4px;
}

.mt-2 {
  margin-top: 8px;
}

.mb-3 {
  margin-bottom: 12px;
}

.text-primary {
  color: #18a058;
}

.text-warning {
  color: #f0a020;
}

.text-success {
  color: #18a058;
}

.text-center {
  text-align: center;
}

.mx-auto {
  margin-left: auto;
  margin-right: auto;
}

/* ==================== MODAL STYLES ==================== */
.serial-modal-header {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 12px;
  margin-bottom: 16px;
}

/* ==================== RESPONSIVE STYLES ==================== */
@media (max-width: 1200px) {
  .main-layout {
    flex-direction: column;
    height: auto;
  }

  .right-column {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .main-layout {
    padding: 12px;
    gap: 12px;
  }

  .pending-invoice-card {
    min-width: 180px;
  }
}

/* ==================== NAIVE UI OVERRIDES ==================== */
:deep(.n-checkbox) {
  margin-right: 0;
}
</style>
