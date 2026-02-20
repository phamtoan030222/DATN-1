<script setup lang="ts">
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

  xoaSL,
  xoaSP,
} from '@/service/api/admin/banhang.api'
import type { ADPDImeiResponse, ADProductDetailRequest, ADProductDetailResponse } from '@/service/api/admin/product/productDetail.api'
import type { AvailableServiceRequest, ShippingFeeRequest } from '@/service/api/ghn.api'
import { calculateFee, getAvailableServices, getGHNDistricts, getGHNProvinces, getGHNWards } from '@/service/api/ghn.api'
import { localStorageAction } from '@/utils/storage'
import { Html5Qrcode } from 'html5-qrcode'
import { debounce } from 'lodash'
import type { DataTableColumns } from 'naive-ui'
import { computed, h, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { toast } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'

// Naive UI Icons
import {
  AddCircleOutline,
  ReloadOutline,
  SearchOutline,
  TrashOutline,
} from '@vicons/ionicons5'

// Naive UI components
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
  NScrollbar,
  NSelect,
  NSpace,
  NSwitch,
  NTag,
  NText,
  NThing,
  NTooltip,
} from 'naive-ui'

// Local filter variables
const localSearchQuery = ref('')
const localColor = ref<string | null>(null)
const localCPU = ref<string | null>(null)
const localGPU = ref<string | null>(null)
const localRAM = ref<string | null>(null)
const localHardDrive = ref<string | null>(null)
const localSelectedMaterial = ref<string | null>(null)

const idNV = localStorageAction.get(USER_INFO_STORAGE_KEY)
const ColorOptions = ref<{ label: string, value: string }[]>([])
const CpuOptions = ref<{ label: string, value: string }[]>([])
const GpuOptions = ref<{ label: string, value: string }[]>([])
const RamOptions = ref<{ label: string, value: string }[]>([])
const HardDriveOptions = ref<{ label: string, value: string }[]>([])
const MaterialOptions = ref<{ label: string, value: string }[]>([])

const isBothPaymentModalVisible = ref(false)
const amountPaid = ref(0)
const bothPaymentLoading = ref(false)
const soTien = ref(0)
const tienKhachThanhToan = ref(0)
const tienThieu = ref(0)
const tongTien = ref(0)
const tongTienTruocGiam = ref(0)
const giamGia = ref(0)
const tienHang = ref(0)
const idSP = ref('')
const idHDS = ref('')
const tabs = ref<Array<{
  id: number
  idHD: string
  code: string
  soLuong: number
  products: any[]
  loaiHoaDon: string
  isTemp?: boolean
}>>([])
const activeTab = ref(0)
const idPGG = ref('')
let nextTabId = 1
const loaiHD = ref('')
const showDiscountModal = ref(false)
const discountList = ref<PhieuGiamGiaResponse[]>([])
const selectedDiscount = ref<PhieuGiamGiaResponse | null>(null)
const selectedDiscountCode = ref<string>('')
const isDeliveryEnabled = ref(false)
const showDeliveryModal = ref(false)
const deliveryInfo = reactive({
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanhPho: null as string | null,
  quanHuyen: null as string | null,
  phuongXa: null as string | null,
  diaChiCuThe: '',
  ghiChu: '',
})
const currentDeliveryInfo = ref<ThongTinGiaoHangResponse | null>(null)
const customerSearchQuery = ref('')
const betterDiscountMessage = ref('')
const deliveryInfoByInvoice = reactive<{ [key: string]: any }>({})
// GHN specific states
const provinces = ref<Array<{ value: string, label: string, code: string }>>([])
const districts = ref<Array<{ value: string, label: string, code: string }>>([])
const wards = ref<Array<{ value: string, label: string, code: string }>>([])
const shippingFee = ref(0)
const provinceCode = ref<number | null>(null)
const districtCode = ref<number | null>(null)
const wardCode = ref<string | null>(null)
const FROM_DISTRICT_ID = 3440
const FROM_WARD_CODE = '13010'
const isBestDiscountApplied = ref(false)
const phieuNgon = ref('')
const GHN_API_TOKEN = '72f634c6-58a2-11f0-8a1e-1e10d8df3c04'
const GHN_SHOP_ID = 5872469

// State cho modal serial
const showSerialModal = ref(false)
const selectedProductDetail = ref<ADProductDetailResponse | null>(null)
const selectedSerials = ref<ADPDImeiResponse[]>([]) // Sửa type cho đúng
const selectedSerialIds = ref<string[]>([])
const loadingSerials = ref(false) // Thêm loading state

// Hoặc tạo state riêng
const imeiDaChon = ref<Array<{
  idHoaDonChiTiet: string
  danhSachImei: string[]
}>>([])

async function fetchSerialsByProduct(productId: string) {
  try {
    console.log('Fetching serials for product:', productId)

    // Sử dụng API thực tế để lấy danh sách IMEI
    const response = await getImeiProductDetail(productId)
    console.log('API Response:', response)

    if (response.data && Array.isArray(response.data)) {
      selectedSerials.value = response.data
      console.log('Loaded serials:', selectedSerials.value.length, selectedSerials.value)
    }
    else {
      console.warn('API response structure is not as expected:', response)
      selectedSerials.value = []
    }

    selectedSerialIds.value = [] // Reset selected serials
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

// Hàm chọn sản phẩm để xem serial
async function selectProductForSerial(product: ADProductDetailResponse) {
  selectedProductDetail.value = product
  loadingSerials.value = true
  selectedSerials.value = []
  selectedSerialIds.value = []

  // Gọi API để lấy danh sách serial
  await fetchSerialsByProduct(product.id)
}

// Hàm thêm serial (IMEI) vào giỏ hàng - CHUẨN NGHIỆP VỤ
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
    // 1. Lấy danh sách IMEI đã chọn
    const imeisDaChon = selectedSerials.value
      .filter(s => selectedSerialIds.value.includes(s.id))
      .map(s => ({
        imeiCode: s.code, // Mã IMEI
        imeiId: s.id, // ID IMEI
      }))

    console.log('IMEIs đã chọn:', imeisDaChon)

    // 2. Gọi API thêm sản phẩm (hiện tại của bạn)
    const payload: ADThemSanPhamRequest = {
      invoiceId: idHDS.value,
      productDetailId: selectedProductDetail.value.id,
      imeiIds: imeisDaChon.map(i => i.imeiId), // Gửi ID IMEI
    }

    await themSanPham(payload)

    // 3. LƯU IMEI ĐÃ CHỌN VÀO STATE để dùng khi thanh toán
    // Tìm hoặc tạo idHoaDonChiTiet (giả sử lấy từ response)
    // Hoặc lưu tạm với product id

    // Cách tạm thời: lưu theo productId
    const existingIndex = imeiDaChon.value.findIndex(
      item => item.idHoaDonChiTiet === selectedProductDetail.value?.id,
    )

    if (existingIndex >= 0) {
      // Cập nhật danh sách IMEI
      imeiDaChon.value[existingIndex].danhSachImei = [
        ...imeiDaChon.value[existingIndex].danhSachImei,
        ...imeisDaChon.map(i => i.imeiCode),
      ]
    }
    else {
      // Thêm mới
      imeiDaChon.value.push({
        idHoaDonChiTiet: selectedProductDetail.value.id, // Tạm dùng productId
        danhSachImei: imeisDaChon.map(i => i.imeiCode),
      })
    }

    // 4. Thông báo thành công
    toast.success(`Đã thêm ${imeisDaChon.length} serial vào giỏ hàng!`)

    // 5. Reset và đóng modal
    showSerialModal.value = false
    selectedSerialIds.value = []
    selectedSerials.value = []

    // 6. Refresh giỏ hàng
    await refreshCart()
    await fetchDiscounts(idHDS.value)
    await fetchHoaDon()
  }
  catch (error) {
    console.error('Failed to add serials to cart:', error)
    toast.error('Thêm serial vào giỏ hàng thất bại!')
  }
}

// Hàm refresh giỏ hàng
async function refreshCart() {
  if (idHDS.value) {
    const response = await GetGioHang(idHDS.value)
    state.gioHang = response

    // Cập nhật mapping IMEI với idHoaDonChiTiet thực tế
    // (Cần backend trả về idHoaDonChiTiet trong response)

    await fetchDiscounts(idHDS.value)
  }
}
// Main reactive state object
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

const priceRange = ref<[number, number]>([0, 0])

// State cho min max price
const stateMinMaxPrice = reactive({
  priceMin: 0,
  priceMax: 0,
})

// Format tooltip cho slider
function formatTooltipRangePrice(value: number) {
  return formatCurrency(value)
}

// Tính toán min và max price từ danh sách sản phẩm
const minProductPrice = computed(() => {
  if (stateSP.products.length === 0)
    return 0
  const prices = stateSP.products.map(p => p.price).filter(price => price > 0)
  return prices.length > 0 ? Math.min(...prices) : 0
})

const maxProductPrice = computed(() => {
  if (stateSP.products.length === 0)
    return 0
  const prices = stateSP.products.map(p => p.price).filter(price => price > 0)
  return prices.length > 0 ? Math.max(...prices) : 0
})

// Tính số lượng serial khả dụng
const availableSerialsCount = computed(() => {
  return selectedSerials.value.filter(s => s.imeiStatus === 'AVAILABLE').length
})

const debouncedFetchCustomers = debounce(async () => {
  await fetchCustomers()
}, 300)

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

const selectedVoucher = ref<any>(null)
const applyingVoucher = ref<string | null>(null)
const autoApplying = ref(false)
const applyingBetterVoucher = ref<string | null>(null)
const showSuggestionDetailModal = ref(false)
const selectedSuggestion = ref<any>(null)
const discountTab = ref('auto')

// Computed properties
const hasBetterVoucherSuggestion = computed(() => {
  return state.autoVoucherResult?.voucherTotHon?.some(v =>
    v.giamThem > (selectedVoucher.value?.giamGiaThucTe || 0),
  )
})

// Hàm trigger auto apply voucher
async function triggerAutoApplyVoucher() {
  if (!idHDS.value || tienHang.value <= 0) {
    toast.error('Vui lòng có sản phẩm trong giỏ hàng trước!')
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

      // Tự động chọn voucher tốt nhất nếu chưa có voucher nào được chọn
      if (!selectedVoucher.value && response.voucherApDung?.length > 0) {
        // Tìm voucher có giamGiaThucTe lớn nhất
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

// Hàm chọn voucher
async function selectVoucher(voucher: any) {
  applyingVoucher.value = voucher.voucherId

  try {
    // 1. Cập nhật state
    selectedVoucher.value = voucher
    selectedDiscountCode.value = voucher.code
    giamGia.value = voucher.giamGiaThucTe

    // 2. Cập nhật selectedDiscount (nếu cần cho backward compatibility)
    selectedDiscount.value = {
      id: voucher.voucherId,
      ma: voucher.code,
      giaTriGiamThucTe: voucher.giamGiaThucTe,
      typeVoucher: voucher.typeVoucher,
      discountValue: voucher.discountValue,
      maxValue: voucher.maxValue,
      dieuKien: voucher.dieuKien,
    } as any

    // 3. Tính toán lại tổng tiền
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

// Hàm xóa voucher
function removeVoucher() {
  selectedVoucher.value = null
  selectedDiscount.value = null
  selectedDiscountCode.value = ''
  giamGia.value = 0
  calculateTotalAmounts()
  toast.info('Đã bỏ chọn voucher')
}

// Hàm hiển thị chi tiết suggestion
function showSuggestionDetail(suggestion: any) {
  selectedSuggestion.value = suggestion
  showSuggestionDetailModal.value = true
}

// Hàm áp dụng suggestion tốt nhất
function applyBestSuggestion() {
  if (state.autoVoucherResult?.voucherTotHon?.length > 0) {
    showSuggestionDetail(state.autoVoucherResult.voucherTotHon[0])
  }
}

// Hàm áp dụng suggestion voucher
function applySuggestionVoucher() {
  if (!selectedSuggestion.value)
    return

  // Tìm voucher trong danh sách voucherApDung
  const fullVoucher = state.autoVoucherResult?.voucherApDung?.find(
    v => v.voucherId === selectedSuggestion.value.voucherId,
  )

  if (fullVoucher) {
    selectVoucher(fullVoucher)
    showSuggestionDetailModal.value = false
    toast.info(`Voucher ${fullVoucher.code} sẽ được áp dụng khi đủ điều kiện`)
  }
}

// Hàm lấy tag type cho voucher
function getVoucherTagType(type: string) {
  return type === 'PERCENTAGE' ? 'success' : 'warning'
}

// Hàm lấy tag type cho suggestion
function getSuggestionTagType(hieuQua: number) {
  if (hieuQua >= 50)
    return 'error' // Rất hiệu quả
  if (hieuQua >= 30)
    return 'warning' // Hiệu quả
  if (hieuQua >= 15)
    return 'info' // Khá hiệu quả
  return 'default' // Bình thường
}

// Cập nhật fetchDiscounts để lưu kết quả auto apply
async function fetchDiscounts(idHD: string) {
  try {
    if (!idHD) {
      resetDiscount()
      return
    }

    const params = {
      invoiceId: idHD,
      tongTien: tienHang.value,
      customerId: state.detailKhachHang?.id ?? null,
    }

    const response = await getMaGiamGia(params)

    // Lưu kết quả vào state
    state.autoVoucherResult = response
    state.discountList = response.voucherApDung ?? []

    // Cập nhật thông báo gợi ý mua thêm
    if (response.voucherTotHon?.length > 0) {
      const bestSuggestion = response.voucherTotHon[0]
      betterDiscountMessage.value
        = `Mua thêm ${formatCurrency(bestSuggestion.canMuaThem)} `
          + `để được giảm thêm ${formatCurrency(bestSuggestion.giamThem)}`
    }
    else {
      betterDiscountMessage.value = ''
    }

    // Nếu chưa có voucher được chọn, tự động chọn voucher tốt nhất
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

// Watch để trigger auto apply khi thay đổi giỏ hàng
watch(
  () => tienHang.value,
  async (newValue) => {
    if (newValue > 0 && idHDS.value) {
      // Debounce để tránh gọi API nhiều lần
      setTimeout(async () => {
        await fetchDiscounts(idHDS.value)
      }, 500)
    }
  },
  { immediate: true },
)

// Khi thay đổi khách hàng
watch(
  () => state.detailKhachHang?.id,
  async () => {
    if (tienHang.value > 0 && idHDS.value) {
      await fetchDiscounts(idHDS.value)
    }
  },
)

// Cập nhật calculateTotalAmounts để tính cả voucher
function calculateTotalAmounts() {
  tienHang.value = state.gioHang.reduce((sum, item) => sum + (item.price || item.giaBan), 0)

  // Ưu tiên sử dụng voucher được chọn
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

watch(localSearchQuery, () => {
  debouncedFetchProducts()
})

watch([localColor, localCPU, localGPU, localRAM, localHardDrive, localSelectedMaterial], () => {
  debouncedFetchProducts()
})

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

function handleColorChange() {
  debouncedFetchProducts()
}

function handleCPUChange() {
  debouncedFetchProducts()
}

function handleGPUChange() {
  debouncedFetchProducts()
}

function handleRAMChange() {
  debouncedFetchProducts()
}

function handleHardDriveChange() {
  debouncedFetchProducts()
}

function parseCurrency(value: string) {
  if (!value)
    return 0
  let str = String(value).replace(/[^0-9nghìtrieu]/g, '').trim().toLowerCase()
  let number = Number.parseInt(str.replace(/\D/g, '')) || 0

  if (str.includes('nghìn')) {
    number *= 1000
  }
  else if (str.includes('triệu')) {
    number *= 1000000
  }

  return number
}

function formatCurrencyInput(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

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
    toast.error('Không thể tải danh sách Quận/Huyện.')
    districts.value = []
  }
}

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
    toast.error('Không thể tải danh sách Phường/Xã.')
    wards.value = []
  }
}

async function fetchColor() {
  try {
    const { data } = await getColors()
    ColorOptions.value = data.map((c: any) => ({ label: c.ten || c.label, value: c.id }))
  }
  catch (e) {
    console.error('Error fetching colors:', e)
  }
}

async function fetchCPU() {
  try {
    const { data } = await getCPUs()
    CpuOptions.value = data.map((c: any) => ({ label: c.ten || c.label, value: c.id }))
  }
  catch (e) {
    console.error('Error fetching CPU:', e)
  }
}

async function fetchGPU() {
  try {
    const { data } = await getGPUs()
    GpuOptions.value = data.map((g: any) => ({ label: g.ten || g.label, value: g.id }))
  }
  catch (e) {
    console.error('Error fetching GPU:', e)
  }
}

async function fetchRAM() {
  try {
    const { data } = await getRAMs()
    RamOptions.value = data.map((r: any) => ({ label: r.ten || r.label, value: r.id }))
  }
  catch (e) {
    console.error('Error fetching RAM:', e)
  }
}

async function fetchHardDrive() {
  try {
    const { data } = await getHardDrives()
    HardDriveOptions.value = data.map((h: any) => ({ label: h.ten || h.label, value: h.id }))
  }
  catch (e) {
    console.error('Error fetching Hard Drive:', e)
  }
}

async function fetchMaterial() {
  try {
    const { data } = await getMaterials()
    MaterialOptions.value = data.map((m: any) => ({ label: m.ten || m.label, value: m.id }))
  }
  catch (e) {
    console.error('Error fetching Material:', e)
  }
}

async function checkFromDistrictAndWard() {
  try {
    const response = await getGHNDistricts(null, GHN_API_TOKEN)
    const districtExists = response.data?.some((d: any) => d.DistrictID === FROM_DISTRICT_ID) || false
    if (!districtExists) {
      console.error(`FROM_DISTRICT_ID ${FROM_DISTRICT_ID} không hợp lệ!`)
      //      toast.error('Mã quận/huyện nguồn không hợp lệ.')
      return false
    }
    const wardResponse = await getGHNWards(FROM_DISTRICT_ID, GHN_API_TOKEN)
    const wardExists = wardResponse.data?.some((w: any) => w.WardCode === FROM_WARD_CODE) || false
    if (!wardExists) {
      console.error(`FROM_WARD_CODE ${FROM_WARD_CODE} không hợp lệ!`)
      toast.error('Mã phường/xã nguồn không hợp lệ.')
      return false
    }
    return true
  }
  catch (error) {
    console.error('Lỗi khi kiểm tra mã quận/huyện hoặc phường/xã:', error)
    return false
  }
}

onMounted(async () => {
  await fetchColor()
  await fetchCPU()
  await fetchGPU()
  await fetchRAM()
  await fetchHardDrive()
  await fetchMaterial()
  await fetchHoaDon()
  await checkFromDistrictAndWard()
  await fetchProvinces()
  setDefaultPaymentMethod()
})

async function onProvinceChange(value: string) {
  deliveryInfo.tinhThanhPho = value
  const selectedProvince = provinces.value.find(p => p.value === value)
  provinceCode.value = selectedProvince ? Number.parseInt(selectedProvince.code) : null
  if (provinceCode.value) {
    await fetchDistricts(provinceCode.value)
    deliveryInfo.quanHuyen = undefined
    deliveryInfo.phuongXa = undefined
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

  deliveryInfoByInvoice[idHDS.value] = {
    tenNguoiNhan: deliveryInfo.tenNguoiNhan,
    sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
    diaChiCuThe: deliveryInfo.diaChiCuThe,
    tinhThanhPho: deliveryInfo.tinhThanhPho,
    quanHuyen: deliveryInfo.quanHuyen,
    phuongXa: deliveryInfo.phuongXa,
    provinceCode: provinceCode.value,
    districtCode: districtCode.value,
    wardCode: wardCode.value,
    shippingFee: shippingFee.value,
  }
}

async function onDistrictChange(value: string) {
  deliveryInfo.quanHuyen = value
  const selectedDistrict = districts.value.find(d => d.value === value)
  districtCode.value = selectedDistrict ? Number.parseInt(selectedDistrict.code) : null
  if (districtCode.value) {
    await fetchWards(districtCode.value)
    deliveryInfo.phuongXa = undefined
    wardCode.value = null
    await calculateShippingFee()
  }
  else {
    wards.value = []
    shippingFee.value = 0
    calculateTotalAmounts()
  }

  deliveryInfoByInvoice[idHDS.value] = {
    tenNguoiNhan: deliveryInfo.tenNguoiNhan,
    sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
    diaChiCuThe: deliveryInfo.diaChiCuThe,
    tinhThanhPho: deliveryInfo.tinhThanhPho,
    quanHuyen: deliveryInfo.quanHuyen,
    phuongXa: deliveryInfo.phuongXa,
    provinceCode: provinceCode.value,
    districtCode: districtCode.value,
    wardCode: wardCode.value,
    shippingFee: shippingFee.value,
  }
}

async function onWardChange(value: string) {
  deliveryInfo.phuongXa = value
  const selectedWard = wards.value.find(w => w.value === value)
  wardCode.value = selectedWard ? selectedWard.code : null
  await calculateShippingFee()

  deliveryInfoByInvoice[idHDS.value] = {
    tenNguoiNhan: deliveryInfo.tenNguoiNhan,
    sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
    diaChiCuThe: deliveryInfo.diaChiCuThe,
    tinhThanhPho: deliveryInfo.tinhThanhPho,
    quanHuyen: deliveryInfo.quanHuyen,
    phuongXa: deliveryInfo.phuongXa,
    provinceCode: provinceCode.value,
    districtCode: districtCode.value,
    wardCode: wardCode.value,
    shippingFee: shippingFee.value,
  }
}

const newCustomer = reactive({
  ten: '',
  sdt: '',
})

const isFreeShipping = ref(false)

watch(tienHang, () => {
  calculateTotalAmounts()
  if (isDeliveryEnabled.value && provinceCode.value && districtCode.value && wardCode.value) {
    calculateShippingFee()
  }
})

watch(tienHang, async (newTienHang) => {
  if (idHDS.value) {
    await fetchDiscounts(idHDS.value)
  }
})

async function calculateShippingFee() {
  if (!isDeliveryEnabled.value || !idHDS.value || !provinceCode.value || !districtCode.value || !wardCode.value || tienHang.value <= 0) {
    shippingFee.value = 0
    isFreeShipping.value = false
    calculateTotalAmounts()
    return
  }

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
    const availableServicesResponse = await getAvailableServices(GHN_API_TOKEN, availableServicesRequestBody)

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

async function confirmBothPayment() {
  bothPaymentLoading.value = true
  try {
    if (!idHDS.value)
      throw new Error('Không có hóa đơn được chọn!')
    if (amountPaid.value <= 0)
      throw new Error('Vui lòng nhập số tiền hợp lệ!')
    const remainingAmount = tongTien.value - tienKhachThanhToan.value - amountPaid.value
    if (remainingAmount > 0) {
      toast.info(`Còn ${formatCurrency(remainingAmount)} cần thanh toán qua QR.`)
    }
    else {
      toast.success('Thanh toán đủ!')
    }

    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('tongTien', amountPaid.value.toString())
    formData.append('phuongThuc', 'Cả hai')
    await themPTTT(formData)

    toast.success('Xác nhận thanh toán cả hai phương thức thành công!')
    await clickkActiveTab(activeTab.value, idHDS.value, loaiHD.value)
    closeBothPaymentModal()
  }
  catch (error: any) {
    console.error('Error in confirmBothPayment:', error)
    toast.error(error.message || 'Xác nhận thanh toán thất bại!')
  }
  finally {
    bothPaymentLoading.value = false
  }
}

watch([deliveryInfo.tinhThanhPho, deliveryInfo.quanHuyen, deliveryInfo.phuongXa], () => {
  localStorage.setItem('deliveryInfoByInvoice', JSON.stringify(deliveryInfoByInvoice))
})

watch(selectedDiscount, () => {
  localStorage.setItem('selectedDiscount', JSON.stringify(selectedDiscount.value))
  localStorage.setItem('isBestDiscountApplied', JSON.stringify(isBestDiscountApplied.value))
})

function applyBestDiscount() {
  if (state.discountList.length > 0) {
    const bestDiscount = state.discountList.reduce((best, current) =>
      (best.giaTriGiamThucTe || 0) > (current.giaTriGiamThucTe || 0) ? best : current,
    )
    selectedDiscount.value = bestDiscount
    selectedDiscountCode.value = bestDiscount.ma
    phieuNgon.value = bestDiscount.ma
    giamGia.value = bestDiscount.giaTriGiamThucTe || 0
    isBestDiscountApplied.value = true
  }
  else {
    selectedDiscount.value = null
    selectedDiscountCode.value = ''
    giamGia.value = 0
    isBestDiscountApplied.value = false
  }
  calculateTotalAmounts()
  showDiscountModal.value = false
}

async function giaoHang(isDeliveryEnableds: boolean) {
  isDeliveryEnabled.value = isDeliveryEnableds

  if (isDeliveryEnabled.value && state.detailKhachHang) {
    deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || ''
    deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || ''
    deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || ''

    if (!provinces.value.length) {
      await fetchProvinces()
    }

    const tinhThanhPhoId = state.detailKhachHang.tinh
    const quanHuyenId = state.detailKhachHang.huyen
    const phuongXaId = state.detailKhachHang.xa

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

    deliveryInfoByInvoice[idHDS.value] = {
      tenNguoiNhan: deliveryInfo.tenNguoiNhan,
      sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
      diaChiCuThe: deliveryInfo.diaChiCuThe,
      tinhThanhPho: deliveryInfo.tinhThanhPho,
      quanHuyen: deliveryInfo.quanHuyen,
      phuongXa: deliveryInfo.phuongXa,
      provinceCode: provinceCode.value,
      districtCode: districtCode.value,
      wardCode: wardCode.value,
      shippingFee: shippingFee.value,
    }
  }
  else {
    Object.assign(deliveryInfo, {
      tenNguoiNhan: state.detailKhachHang?.ten || '',
      sdtNguoiNhan: state.detailKhachHang?.sdt || '',
      diaChiCuThe: state.detailKhachHang?.diaChi || '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
    })
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    shippingFee.value = 0
    isFreeShipping.value = false
    calculateTotalAmounts()

    deliveryInfoByInvoice[idHDS.value] = {
      tenNguoiNhan: state.detailKhachHang?.ten || '',
      sdtNguoiNhan: state.detailKhachHang?.sdt || '',
      diaChiCuThe: state.detailKhachHang?.diaChi || '',
      tinhThanhPho: undefined,
      quanHuyen: undefined,
      phuongXa: undefined,
      provinceCode: null,
      districtCode: null,
      wardCode: null,
      shippingFee: 0,
    }
  }

  await suaGiaoHang(idHDS.value)
  await capNhatDanhSach()
}

function selectDiscount(discount: PhieuGiamGiaResponse) {
  selectedDiscount.value = discount
  selectedDiscountCode.value = discount.ma
  giamGia.value = discount.giaTriGiamThucTe || 0
  toast.success(`Đã chọn phiếu giảm giá: ${discount.ma}`)
  calculateTotalAmounts()
  if (phieuNgon.value === discount.ma) {
    isBestDiscountApplied.value = true
  }
  else {
    isBestDiscountApplied.value = false
  }
  showDiscountModal.value = false
}

function handlePageChange(page: number) {
  stateSP.paginationParams.page = page
  fetchProducts()
}

function handlePageSizeChange(pageSize: number) {
  stateSP.paginationParams.size = pageSize
  stateSP.paginationParams.page = 1
  fetchProducts()
}

function handleCustomerPageChange(page: number) {
  state.paginationParams.page = page
  fetchCustomers()
}

function handleCustomerPageSizeChange(pageSize: number) {
  state.paginationParams.size = pageSize
  state.paginationParams.page = 1
  fetchCustomers()
}

function closeBothPaymentModal() {
  isBothPaymentModalVisible.value = false
  amountPaid.value = 0
  bothPaymentLoading.value = false
}

async function themPTTT(formData: FormData) {
  try {
    await new Promise(resolve => setTimeout(resolve, 500))
  }
  catch (error) {
    console.error('Failed to add payment method:', error)
    throw error
  }
}

async function handlePaymentMethod(method: string) {
  if (!idHDS.value) {
    toast.error('Vui lòng chọn hoặc tạo hóa đơn trước khi chọn phương thức thanh toán!')
    return
  }

  state.currentPaymentMethod = method

  try {
    if (method === '0') {
      toast.success('Đã chọn phương thức thanh toán Tiền mặt.')
    }
    else if (method === '1') {
      toast.success('Đã chọn phương thức thanh toán chuyển khoản.')
      openQrModalVNPay()
    }
    else if (method === '2') {
      toast.success('Đã chọn phương thức thanh toán vừa chuyển khoản vừa tiền mặt.')
      openQrModalVNPayCaHai()
    }
  }
  catch (error: any) {
    console.error('Error in handlePaymentMethod:', error)
    toast.error('Có lỗi khi chọn phương thức thanh toán!')
  }
}

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
    }
  }
  catch (error) {
    console.error('Failed to fetch invoices:', error)
    toast.error('Lấy danh sách hóa đơn thất bại!')
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
  }
}

function resetDiscount() {
  state.discountList = []
  selectedDiscount.value = null
  selectedDiscountCode.value = ''
  giamGia.value = 0
  calculateTotalAmounts()
}

async function clickkActiveTab(id: number, hd: string, loaiHoaDon: string) {
  idHDS.value = hd
  activeTab.value = id
  loaiHD.value = loaiHoaDon
  isBestDiscountApplied.value = false

  if (tienHang.value > 0) {
    await fetchDiscounts(hd)
  }

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

      if (deliveryInfoByInvoice[hd]) {
        Object.assign(deliveryInfo, {
          tenNguoiNhan: deliveryInfoByInvoice[hd].tenNguoiNhan,
          sdtNguoiNhan: deliveryInfoByInvoice[hd].sdtNguoiNhan,
          diaChiCuThe: deliveryInfoByInvoice[hd].diaChiCuThe,
          tinhThanhPho: deliveryInfoByInvoice[hd].tinhThanhPho,
          quanHuyen: deliveryInfoByInvoice[hd].quanHuyen,
          phuongXa: deliveryInfoByInvoice[hd].phuongXa,
        })
        provinceCode.value = deliveryInfoByInvoice[hd].provinceCode
        districtCode.value = deliveryInfoByInvoice[hd].districtCode
        wardCode.value = deliveryInfoByInvoice[hd].wardCode
        shippingFee.value = deliveryInfoByInvoice[hd].shippingFee
      }

      await calculateShippingFee()
    }
    else if (state.detailKhachHang) {
      deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || ''
      deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || ''
      deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || ''
    }

    deliveryInfoByInvoice[hd] = {
      tenNguoiNhan: deliveryInfo.tenNguoiNhan,
      sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
      diaChiCuThe: deliveryInfo.diaChiCuThe,
      tinhThanhPho: deliveryInfo.tinhThanhPho,
      quanHuyen: deliveryInfo.quanHuyen,
      phuongXa: deliveryInfo.phuongXa,
      provinceCode: provinceCode.value,
      districtCode: districtCode.value,
      wardCode: wardCode.value,
      shippingFee: shippingFee.value,
    }

    calculateTotalAmounts()
    await fetchDiscounts(hd)
    await updatePaymentStatus()
  }
  catch (error) {
    console.error('Failed to switch invoice:', error)
    toast.error('Chuyển hóa đơn thất bại!')
    resetDiscount()
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
    calculateTotalAmounts()
  }
}

watch(isDeliveryEnabled, async (newValue) => {
  if (!newValue) {
    currentDeliveryInfo.value = null
    Object.assign(deliveryInfo, { tenNguoiNhan: '', sdtNguoiNhan: '', diaChiGiaoHang: '', tinhThanhPho: undefined, quanHuyen: undefined, phuongXa: undefined, diaChiCuThe: '' })
    shippingFee.value = 0
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    calculateTotalAmounts()
  }
  else {
    await fetchProvinces()
  }
}, { immediate: true })

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
    render: row => h(
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

const columnsGiohang: DataTableColumns<any> = [
  {
    title: 'Serial đã chọn',
    key: 'imel', // Đổi thành 'imei' nếu backend trả về 'imei'
    width: 110,
    render: (row) => {
      // Kiểm tra xem row có field imel/imei không
      if (row.imel) { // Hoặc row.imei tùy backend trả về
        return h(NTag, {
          type: 'success',
          size: 'small',
          onClick: () => {
            toast.info(`IMEI: ${row.imel}`)
          },
        }, () => `${row.imel}`)
      }

      // Nếu không có imel trong row, kiểm tra trong state imeiDaChon
      const imeiItem = imeiDaChon.value.find(
        item => item.idHoaDonChiTiet === row.idHDCT, // Sử dụng idHDCT thay vì id
      )

      if (imeiItem && imeiItem.danhSachImei.length > 0) {
        return h(NTag, {
          type: 'success',
          size: 'small',
          onClick: () => {
            toast.info(`Đã chọn ${imeiItem.danhSachImei.length} IMEI: ${imeiItem.danhSachImei.join(', ')}`)
          },
        }, () => `${imeiItem.danhSachImei.length} IMEI`)
      }

      return h(NTag, {
        type: 'warning',
        size: 'small',
      }, () => 'Chưa chọn IMEI')
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
          default: () => h(NImage, {
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
    render: row => h(NSpace, {
      vertical: true,
      size: 4,
    }, () => [
      // Dòng 1
      h('div', {
        style: {
          display: 'flex',
          gap: '6px',
          flexWrap: 'wrap',
          marginBottom: '4px',
        },
      }, [
        row.cpu && h('span', {
          style: {
            'font-size': '11px',
            'color': '#1677ff',
            'background': '#e6f4ff',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #91caff',
          },
        }, `CPU: ${row.cpu}`),
        row.gpu && h('span', {
          style: {
            'font-size': '11px',
            'color': '#389e0d',
            'background': '#f6ffed',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #b7eb8f',
          },
        }, `GPU: ${row.gpu}`),
        row.ram && h('span', {
          style: {
            'font-size': '11px',
            'color': '#d46b08',
            'background': '#fff7e6',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #ffd591',
          },
        }, `RAM: ${row.ram}`),
      ]),

      // Dòng 2
      h('div', {
        style: {
          display: 'flex',
          gap: '6px',
          flexWrap: 'wrap',
        },
      }, [
        row.hardDrive && h('span', {
          style: {
            'font-size': '11px',
            'color': '#722ed1',
            'background': '#f9f0ff',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #d3adf7',
          },
        }, `Ổ cứng: ${row.hardDrive}`),
        row.color && h('span', {
          style: {
            'font-size': '11px',
            'color': '#13c2c2',
            'background': '#e6fffb',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #87e8de',
          },
        }, `Màu: ${row.color}`),
        row.material && h('span', {
          style: {
            'font-size': '11px',
            'color': '#eb2f96',
            'background': '#fff0f6',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #ffadd2',
          },
        }, `Chất liệu: ${row.material}`),
      ]),
    ]),
  },

  {
    title: 'Đơn giá',
    key: 'price',
    width: 110,
    align: 'right',
    render: row => h(NText, {
      style: { fontWeight: 500 },
    }, () => formatCurrency(row.giaGoc)),
  },
  {
    title: 'Giá bán',
    key: 'total',
    width: 120,
    align: 'right',
    render: row => h(NText, {
      type: 'primary',
      strong: true,
      style: { fontSize: '14px' },
    }, () => formatCurrency((row.giaGoc) * (1 - row.percentage / 100))),
  },
  {
    title: 'Thao tác',
    key: 'operation',
    width: 80,
    align: 'center',
    render: row => h(NTooltip, null, {
      trigger: () => h(NButton, {
        type: 'error',
        size: 'small',
        text: true,
        circle: true,
        onClick: () => deleteProduc(row.id, row.idHDCT),
        style: { '--n-border-radius': '50%' },
      }, {
        icon: () => h(NIcon, null, () => h(TrashOutline)),
      }),
      default: () => 'Xóa sản phẩm',
    }),
  },
]

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
          default: () => h(NImage, {
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
            onError: (e) => {
              console.error('Không thể tải ảnh:', imageUrl, e)
            },
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
    render: row => h(NTag, {
      type: row.quantity > 0 ? 'success' : 'error',
      size: 'small',
      round: true,
    }, () => row.quantity),
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

        return h(NSpace, { vertical: true, size: 0, align: 'center', justify: 'center' }, () => [
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
        ])
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
    render: row => h(NSpace, {
      vertical: true,
      size: 4,
    }, () => [
      // Dòng 1
      h('div', {
        style: {
          display: 'flex',
          gap: '6px',
          flexWrap: 'wrap',
          marginBottom: '4px',
        },
      }, [
        row.cpu && h('span', {
          style: {
            'font-size': '11px',
            'color': '#1677ff',
            'background': '#e6f4ff',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #91caff',
          },
        }, `CPU: ${row.cpu}`),
        row.gpu && h('span', {
          style: {
            'font-size': '11px',
            'color': '#389e0d',
            'background': '#f6ffed',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #b7eb8f',
          },
        }, `GPU: ${row.gpu}`),
        row.ram && h('span', {
          style: {
            'font-size': '11px',
            'color': '#d46b08',
            'background': '#fff7e6',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #ffd591',
          },
        }, `RAM: ${row.ram}`),
      ]),

      // Dòng 2
      h('div', {
        style: {
          display: 'flex',
          gap: '6px',
          flexWrap: 'wrap',
        },
      }, [
        row.hardDrive && h('span', {
          style: {
            'font-size': '11px',
            'color': '#722ed1',
            'background': '#f9f0ff',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #d3adf7',
          },
        }, `Ổ cứng: ${row.hardDrive}`),
        row.color && h('span', {
          style: {
            'font-size': '11px',
            'color': '#13c2c2',
            'background': '#e6fffb',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #87e8de',
          },
        }, `Màu: ${row.color}`),
        row.material && h('span', {
          style: {
            'font-size': '11px',
            'color': '#eb2f96',
            'background': '#fff0f6',
            'padding': '1px 6px',
            'border-radius': '3px',
            'border': '1px solid #ffadd2',
          },
        }, `Chất liệu: ${row.material}`),
      ]),
    ]),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
    align: 'center',
    render: row => h(NTag, {
      type: row.status === 'ACTIVE' ? 'success' : 'error',
      size: 'small',
      round: true,
    }, () => row.status === 'ACTIVE' ? 'Hoạt động' : 'Không hoạt động'),
  },
  {
    title: 'Thao tác',
    key: 'operation',
    width: 120,
    align: 'center',
    render: row => h(NSpace, { size: 8 }, () => [
      h(NTooltip, null, {
        trigger: () => h(NButton, {
          type: 'primary',
          size: 'small',
          secondary: true,
          onClick: () => selectProductForSerial(row),
          disabled: row.status !== 'ACTIVE' || row.quantity <= 0,
        }, { default: () => 'Chọn Serial' }),
        default: () => row.status !== 'ACTIVE'
          ? 'Sản phẩm không hoạt động'
          : row.quantity <= 0 ? 'Hết hàng' : `Chọn serial từ ${row.quantity} sản phẩm có sẵn`,
      }),
    ]),
  },
]

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
    render: row => h(NText, {
      strong: true,
      code: true,
      style: {
        fontFamily: 'monospace',
        fontSize: '12px',
      },
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
      return h(NTag, {
        type: config.type,
        size: 'small',
        round: true,
      }, () => config.text)
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
      return h(NTag, {
        type: config.type,
        size: 'small',
        round: true,
      }, () => config.text)
    },
  },
]
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

    if (res.message === 'Số lượng sản phẩm thêm vào nhiều hơn số lượng trong kho') {
      toast.error(res.message)
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
    const updatedProduct = state.gioHang.find(item => item.id === idSPS)
    if (updatedProduct) {
      updatedProduct.soLuong = Math.max(0, updatedProduct.soLuong - 1)
      calculateTotalAmounts()
    }
  }
}

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

    idHDS.value = ''
    activeTab.value = 0
    state.gioHang = []
    state.detailKhachHang = null
    state.phuongThuThanhToan = []
    state.tongTien = null
    tongTien.value = 0
    tongTienTruocGiam.value = 0
    giamGia.value = 0
    tienHang.value = 0
    soTien.value = 0
    tienKhachThanhToan.value = 0
    tienThieu.value = 0
    state.currentPaymentMethod = '0'
    resetDiscount()
    isDeliveryEnabled.value = false
    currentDeliveryInfo.value = null
    Object.assign(deliveryInfo, { tenNguoiNhan: '', sdtNguoiNhan: '', diaChiGiaoHang: '', tinhThanhPho: undefined, quanHuyen: undefined, phuongXa: undefined, diaChiCuThe: '' })
    shippingFee.value = 0
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
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

async function xacNhan(check: number) {
  console.log('=== DEBUG XÁC NHẬN THANH TOÁN ===')
  console.log('Giỏ hàng:', state.gioHang)
  console.log('IMEI đã chọn:', imeiDaChon.value)

  // Kiểm tra từng sản phẩm
  state.gioHang.forEach((item, index) => {
    console.log(`Sản phẩm ${index + 1}:`, {
      ten: item.ten || item.name,
      id: item.id,
      idHDCT: item.idHDCT,
      soLuong: item.soLuong,
      sanPhamChiTiet: item.sanPhamChiTiet,
      quanLyImei: item.sanPhamChiTiet?.quanLyImei,
      hasImei: 'imel' in item ? item.imel : 'Không có field imel',
    })
  })

  console.log('=== END DEBUG ===')
  if (!idHDS.value) {
    toast.error('Vui lòng chọn một hóa đơn để xác nhận thanh toán!')
    console.error('Lỗi: idHDS.value là null khi xác nhận thanh toán.')
    return
  }

  // KIỂM TRA GIỎ HÀNG TRỐNG
  if (state.gioHang.length === 0) {
    toast.error('Giỏ hàng trống! Vui lòng thêm sản phẩm trước khi thanh toán.')
    return
  }

  // Kiểm tra thông tin giao hàng nếu là giao hàng
  if (isDeliveryEnabled.value) {
    if (!deliveryInfo.tenNguoiNhan || !deliveryInfo.sdtNguoiNhan || !deliveryInfo.diaChiCuThe
      || !deliveryInfo.tinhThanhPho || !deliveryInfo.quanHuyen || !deliveryInfo.phuongXa) {
      toast.error('Vui lòng nhập đầy đủ thông tin giao hàng!')
      return
    }
    if (shippingFee.value < 0 && tienHang.value > 0) {
      toast.error('Vui làm kiểm tra lại tiền giao hàng')
      return
    }
  }

  // Xác định loại hóa đơn
  const loaiHoaDon = isDeliveryEnabled.value ? 'GIAO_HANG' : 'TAI_QUAY'

  // LẤY DANH SÁCH IMEI ĐÃ CHỌN từ state
  const danhSachImeiChon = imeiDaChon.value // Sử dụng state đã lưu

  // Kiểm tra IMEI nếu có sản phẩm quản lý IMEI
  const coSanPhamImei = state.gioHang.some(item => item.sanPhamChiTiet?.quanLyImei)
  if (coSanPhamImei && danhSachImeiChon.length === 0) {
    toast.error('Vui lòng chọn IMEI cho sản phẩm laptop!')
    return
  }

  // Kiểm tra số lượng IMEI có khớp không
  let hasError = false
  for (const item of state.gioHang) {
    if (item.sanPhamChiTiet?.quanLyImei) {
      const imeiItem = danhSachImeiChon.find(i => i.idHoaDonChiTiet === item.idHoaDonChiTiet)
      if (!imeiItem) {
        toast.error(`Chưa chọn IMEI cho sản phẩm ${item.tenSanPham}!`)
        hasError = true
        break
      }
      if (imeiItem.danhSachImei.length !== item.soLuong) {
        toast.error(`Số lượng IMEI cho sản phẩm ${item.tenSanPham} không khớp! Cần ${item.soLuong}, đã chọn ${imeiItem.danhSachImei.length}`)
        hasError = true
        break
      }
    }
  }

  if (hasError)
    return

  const selectedProvince = provinces.value.find(p => p.code === deliveryInfo.tinhThanhPho)
  const selectedDistrict = districts.value.find(d => d.code === deliveryInfo.quanHuyen)
  const selectedWard = wards.value.find(w => w.code === deliveryInfo.phuongXa)

  try {
    // Tạo request object (JSON)
    const requestData: ParamsThanhCong = {
      idHD: idHDS.value,
      idNV: idNV.userId,
      tienHang: tienHang.value,
      tongTien: tongTien.value.toString(),
      ten: deliveryInfo.tenNguoiNhan,
      sdt: deliveryInfo.sdtNguoiNhan,
      diaChi: isDeliveryEnabled.value
        ? `${deliveryInfo.diaChiCuThe}, ${selectedWard?.label}, ${selectedDistrict?.label}, ${selectedProvince?.label}`
        : '',
      tienShip: shippingFee.value,
      giamGia: giamGia.value,
      phuongThucThanhToan: state.currentPaymentMethod, // '0', '1', '2'
      idPGG: selectedDiscount.value?.id,
      check: isDeliveryEnabled.value ? 1 : 0,

      // THÊM FIELD IMEI (quan trọng)
      loaiHoaDon,
      danhSachImei: danhSachImeiChon,
      daXacNhanImei: true,
    }

    console.log('Gửi request thanh toán:', requestData)

    // Gửi request với JSON
    const res = await thanhToanThanhCong(requestData)

    // Xử lý response
    if (res.message != null) {
      if (res.message.startsWith('Số')) {
        toast.error(res.message)
        return
      }

      if (res.message.startsWith('Phiếu')) {
        toast.error(res.message)
        await fetchDiscounts(idHDS.value)
        return
      }

      if (res.message.startsWith('Đã')) {
        showDeliveryModal.value = true
        await fetchDiscounts(idHDS.value)
        return
      }
    }

    // Thành công
    if (isDeliveryEnabled.value) {
      toast.success('Đã chuyển trạng thái giao hang!')
    }
    else {
      toast.success('Thanh toán thành công!')
    }

    // Reset các state (bao gồm IMEI)
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

// Hàm reset sau thanh toán
async function resetAfterPayment() {
  await fetchProducts()
  await capNhatDanhSach()

  const indexToRemove = tabs.value.findIndex(tab => tab.idHD === idHDS.value)
  if (indexToRemove !== -1) {
    tabs.value.splice(indexToRemove, 1)
  }

  idHDS.value = ''
  activeTab.value = 0
  state.gioHang = []
  state.detailKhachHang = null
  state.phuongThuThanhToan = []
  state.tongTien = null
  tongTien.value = 0
  tongTienTruocGiam.value = 0
  giamGia.value = 0
  tienHang.value = 0
  soTien.value = 0
  tienKhachThanhToan.value = 0
  tienThieu.value = 0
  state.currentPaymentMethod = '0'
  resetDiscount()
  isDeliveryEnabled.value = false
  currentDeliveryInfo.value = null
  Object.assign(deliveryInfo, {
    tenNguoiNhan: '',
    sdtNguoiNhan: '',
    diaChiGiaoHang: '',
    tinhThanhPho: undefined,
    quanHuyen: undefined,
    phuongXa: undefined,
    diaChiCuThe: '',
  })
  shippingFee.value = 0
  provinceCode.value = null
  districtCode.value = null
  wardCode.value = null

  // Reset danh sách IMEI đã chọn
  imeiDaChon.value = []
}

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
    const updatedProduct = state.gioHang.find(item => item.id === idSPS)
    if (updatedProduct) {
      updatedProduct.soLuong = Math.min(updatedProduct.soLuong + 1, 1)
      calculateTotalAmounts()
    }
  }
}

watch(
  () => tienHang.value,
  async () => {
    if (idHDS.value) {
      await fetchDiscounts(idHDS.value)
    }
  },
)

watch(
  () => [deliveryInfo.tinhThanhPho, deliveryInfo.quanHuyen, deliveryInfo.phuongXa],
  async () => {
    if (isDeliveryEnabled.value) {
      await calculateShippingFee()
    }
  },
)

async function openProductSelectionModal() {
  if (!idHDS.value) {
    toast.error('Vui lòng tạo hoặc chọn hóa đơn trước khi chọn sản phẩm!')
    return
  }
  await fetchProducts()
  showProductModal.value = true
}

async function createInvoice() {
  if (tabs.value.length >= 10) {
    toast.warning('Chỉ được tạo tối đa 10 hóa đơn!', { autoClose: 3000 })
    return
  }

  // Tạo tab tạm ngay
  const tempMa = genTempMaHoaDon()
  const newTabId = nextTabId++

  tabs.value.push({
    id: newTabId,
    idHD: '', // Chưa có DB id
    code: tempMa, // Mã tạm
    soLuong: 0,
    loaiHoaDon: 'TAI_QUAY',
    products: [],
    isTemp: true,
  })

  activeTab.value = newTabId

  // Reset state
  idHDS.value = ''
  loaiHD.value = 'TAI_QUAY'

  state.gioHang = []
  state.detailKhachHang = null
  resetDiscount()
  isDeliveryEnabled.value = false
  currentDeliveryInfo.value = null

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
  state.currentPaymentMethod = '0'
  tienHang.value = 0
  tongTien.value = 0
  tongTienTruocGiam.value = 0
  tienKhachThanhToan.value = 0
  tienThieu.value = 0

  // Gọi API tạo hóa đơn thật
  try {
    const formData = new FormData()
    formData.append('idNV', idNV.userId)
    formData.append('ma', tempMa)

    const newInvoice = await getCreateHoaDon(formData)

    // Update lại tab
    const tab = tabs.value.find(t => t.id === newTabId)
    if (tab) {
      tab.idHD = newInvoice.data.id
      tab.code = newInvoice.data.code
      tab.loaiHoaDon = newInvoice.data.loaiHoaDon || 'OFFLINE'
      tab.isTemp = false
    }

    idHDS.value = newInvoice.data.id
    loaiHD.value = newInvoice.data.loaiHoaDon || 'OFFLINE'

    await clickkActiveTab(
      newTabId,
      newInvoice.data.id,
      newInvoice.data.loaiHoaDon || 'OFFLINE',

    )

    toast.success('Tạo hóa đơn thành công!')
  }
  catch (error) {
    console.error('Failed to create invoice:', error)

    // Nếu API lỗi → xóa tab tạm
    tabs.value = tabs.value.filter(t => t.id !== newTabId)

    toast.error('Tạo hóa đơn thất bại!')
  }
}

function genTempMaHoaDon(): string {
  return `HD-TMP-${Math.random().toString(36).substring(2, 8).toUpperCase()}`
}

function closeModalP() {
  showDeliveryModal.value = false
  xacNhan(0)
}

const showKhachHangModal = ref(false)
const showProductModal = ref(false)

function setDefaultPaymentMethod() {
  state.currentPaymentMethod = '0'
}

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

      if (loaiHD.value === 'GIAO_HANG') {
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

        if (tinhThanhPhoId && quanHuyenId && phuongXaId) {
          isDeliveryEnabled.value = true
          await calculateShippingFee()
        }
        else {
          isDeliveryEnabled.value = false
          toast.info('Thông tin địa chỉ khách hàng không đầy đủ, vui lòng nhập thủ công.')
        }
      }
      else {
        isDeliveryEnabled.value = false
      }

      deliveryInfoByInvoice[idHDS.value] = {
        tenNguoiNhan: deliveryInfo.tenNguoiNhan,
        sdtNguoiNhan: deliveryInfo.sdtNguoiNhan,
        diaChiCuThe: deliveryInfo.diaChiCuThe,
        tinhThanhPho: deliveryInfo.tinhThanhPho,
        quanHuyen: deliveryInfo.quanHuyen,
        phuongXa: deliveryInfo.phuongXa,
        provinceCode: provinceCode.value,
        districtCode: districtCode.value,
        wardCode: wardCode.value,
        shippingFee: shippingFee.value,
      }
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

async function deleteProduc(idSPS: any, idHDCT: string) {
  try {
    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('idSP', idSPS)
    formData.append('idHDCT', idHDCT)
    await xoaSP(formData)
    betterDiscountMessage.value = ''
    state.gioHang = state.gioHang.filter(item => item.id !== idSPS)
    calculateTotalAmounts()

    await fetchDiscounts(idHDS.value)
    const response = await GetGioHang(idHDS.value)
    state.gioHang = response
    toast.success('Xóa sản phẩm thành công!')
    await capNhatDanhSach()
    await fetchDiscounts(idHDS.value)
  }
  catch (error) {
    console.error('Failed to delete product:', error)
    toast.error('Xóa sản phẩm thất bại!')
  }
}

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

watch(customerSearchQuery, () => {
  state.paginationParams.page = 1
  debouncedFetchCustomers()
})

async function confirmQuantityP() {
  showDeliveryModal.value = false
  applyBestDiscount()
  xacNhan(0)
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

// Tính toán min và max price từ danh sách sản phẩm
function calculateMinMaxPrice(products: ADProductDetailResponse[]) {
  if (products.length === 0) {
    stateMinMaxPrice.priceMin = 0
    stateMinMaxPrice.priceMax = 1000000
    priceRange.value = [0, 1000000]
    return
  }

  const prices = products.map(p => p.price).filter(price => price > 0)
  if (prices.length === 0) {
    stateMinMaxPrice.priceMin = 0
    stateMinMaxPrice.priceMax = 1000000
    priceRange.value = [0, 1000000]
    return
  }

  const minPrice = Math.min(...prices)
  const maxPrice = Math.max(...prices)

  // Làm tròn đến trăm nghìn
  stateMinMaxPrice.priceMin = Math.floor(minPrice / 100000) * 100000
  stateMinMaxPrice.priceMax = Math.ceil(maxPrice / 100000) * 100000

  // Đảm bảo có khoảng cách tối thiểu
  if (stateMinMaxPrice.priceMax - stateMinMaxPrice.priceMin < 100000) {
    stateMinMaxPrice.priceMax = stateMinMaxPrice.priceMin + 100000
  }

  // Set giá trị mặc định cho slider
  priceRange.value = [stateMinMaxPrice.priceMin, stateMinMaxPrice.priceMax]
}

const debouncedFetchProducts = debounce(async () => {
  stateSP.searchQuery = localSearchQuery.value
  stateSP.selectedMaterial = localSelectedMaterial.value
  await fetchProducts()
}, 300)

watch(() => state.gioHang, calculateTotalAmounts, { deep: true })
watch(giamGia, calculateTotalAmounts)
watch(shippingFee, calculateTotalAmounts)

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

const isQrVNpayModalVisible = ref(false)

function openQrModalVNPayCaHai() {
  isBothPaymentModalVisible.value = true
  amountPaid.value = 0
}

function openQrModalVNPay() {
  isQrVNpayModalVisible.value = true
}

function closeQrModalVnPay() {
  isQrVNpayModalVisible.value = false
}

const isQrModalVisible = ref(false)
const qrData = ref('')
const hasCamera = ref(true)

let html5QrCode: Html5Qrcode

function openQrModal() {
  isQrModalVisible.value = true
  nextTick(() => {
    startQrScanning()
  })
}

function startQrScanning() {
  const qrRegionId = 'reader'
  const qrRegionElement = document.getElementById(qrRegionId)
  if (!qrRegionElement) {
    console.error('Không tìm thấy phần tử với id \'reader\'')
    return
  }

  html5QrCode = new Html5Qrcode(qrRegionId)

  Html5Qrcode.getCameras()
    .then((cameras: { id: string, label: string }[]) => {
      if (cameras && cameras.length) {
        hasCamera.value = true
        const cameraId = cameras[0].id
        html5QrCode.start(
          cameraId,
          { fps: 10, qrbox: 250 },
          (qrCodeMessage: string) => {
            qrData.value = qrCodeMessage
            // Tìm sản phẩm theo serial hoặc mã
            const product = stateSP.products.find(p =>
              p.code === qrCodeMessage || p.id === qrCodeMessage,
            )
            if (product) {
              selectProductForSerial(product)
            }
            else {
              toast.error('Không tìm thấy sản phẩm với mã này!')
            }
            html5QrCode.stop()
            closeQrModal()
          },
          (errorMessage) => {
            console.warn('Lỗi đọc QR: ', errorMessage)
          },
        )
      }
      else {
        console.warn('Không tìm thấy camera nào!')
        hasCamera.value = false
        toast.error('Không tìm thấy camera hoặc không có quyền truy cập camera!')
      }
    })
    .catch((error: any) => {
      console.error('Lỗi khi lấy camera: ', error)
      hasCamera.value = false
      toast.error(`Lỗi khi truy cập camera: ${error.message}`)
    })
}

function closeQrModal() {
  isQrModalVisible.value = false
  stopQrScanning()
}

function stopQrScanning() {
  html5QrCode.stop().catch(err => console.error('Không thể dừng scanner:', err))
}

const addCustomerLoading = ref(false)

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

    console.log("New Khách hàng: " + newCustomerId);

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

onMounted(async () => {
  const storedDiscount = localStorage.getItem('selectedDiscount')
  const storedBestDiscount = localStorage.getItem('isBestDiscountApplied')
  if (storedDiscount) {
    selectedDiscount.value = JSON.parse(storedDiscount)
  }
  if (storedBestDiscount) {
    isBestDiscountApplied.value = JSON.parse(storedBestDiscount)
  }
  const storedDeliveryInfo = localStorage.getItem('deliveryInfoByInvoice')
  if (storedDeliveryInfo) {
    Object.assign(deliveryInfoByInvoice, JSON.parse(storedDeliveryInfo))
  }
  await fetchCustomers()
  await fetchHoaDon()
  if (idHDS.value) {
    await fetchDiscounts(idHDS.value)
  }
  setDefaultPaymentMethod()
  await fetchProvinces()

  if (idHDS.value && state.gioHang.length > 0) {
    await fetchDiscounts(idHDS.value)
  }
})
// code tâm thêm
// Tự động tìm voucher giảm nhiều tiền nhất
const bestSuggestion = computed(() => {
  const suggestions = state.autoVoucherResult?.voucherTotHon || []
  if (suggestions.length === 0)
    return null

  // Sắp xếp giảm dần theo số tiền được giảm thêm  và lấy cái đầu tiên
  return [...suggestions].sort((a, b) => b.giamThem - a.giamThem)[0]
})
</script>

<template>
  <div class="main-layout">
    <div class="left-column">
      <NCard class="card" size="small">
        <template #header>
          <NText type="primary" strong>
            Hóa đơn chờ
          </NText>
        </template>

        <template #header-extra>
          <NButton type="primary" size="small" class="btn-create-new-invoice" @click="createInvoice">
            <template #icon>
              <NIcon>
                <AddCircleOutline />
              </NIcon>
            </template>
            Tạo hóa đơn mới
          </NButton>
        </template>

        <div class="pending-invoices-container">
          <NScrollbar x-scrollable>
            <div class="pending-invoices-wrapper">
              <NSpace :wrap="false">
                <div
                  v-for="tab in tabs" :key="tab.id"
                  class="pending-invoice-card" :class="[{ active: activeTab === tab.id }]"
                  @click="clickkActiveTab(tab.id, tab.idHD, tab.loaiHoaDon)"
                >
                  <div class="invoice-header">
                    <NText strong>
                      {{ tab.code }}
                    </NText>

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
                        <NText strong style="display: block; margin-bottom: 8px;">
                          Xác nhận hủy
                        </NText>
                        <NText depth="3">
                          Hành động này không thể hoàn tác.
                        </NText>
                      </div>
                    </NPopconfirm>

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
            <!-- <n-button type="primary" size="small" @click="openQrModal">
              <template #icon>
                <n-icon>
                  <QrCodeOutline />
                </n-icon>
              </template>
  Quét QR
  </n-button> -->
          </NSpace>
        </template>
        <div v-if="state.gioHang.length > 0">
          <NDataTable
            :columns="columnsGiohang" :data="state.gioHang" :max-height="280" size="small"
            :pagination="{ pageSize: 5 }"
          />
        </div>
        <NEmpty v-else description="Không có sản phẩm nào trong giỏ hàng" size="small" />
      </NCard>

      <NCard v-if="isDeliveryEnabled" class="card" size="small">
        <template #header>
          <NText type="primary" strong>
            Thông tin người nhận
          </NText>
        </template>
        <NForm ref="deliveryForm" :model="deliveryInfo" label-placement="left" :label-width="140">
          <NGrid :cols="12" :x-gap="12" :y-gap="12">
            <NFormItemGi :span="6" path="tenNguoiNhan" label="Tên người nhận" required>
              <NInput v-model:value="deliveryInfo.tenNguoiNhan" placeholder="Nhập tên người nhận" clearable />
            </NFormItemGi>

            <NFormItemGi :span="6" path="sdtNguoiNhan" label="Số điện thoại" required>
              <NInput v-model:value="deliveryInfo.sdtNguoiNhan" placeholder="Nhập số điện thoại" clearable />
            </NFormItemGi>

            <NFormItemGi :span="4" path="tinhThanhPho" label="Tỉnh/Thành phố" required>
              <NSelect
                v-model:value="deliveryInfo.tinhThanhPho" placeholder="Chọn tỉnh/thành phố" :options="provinces"
                filterable clearable @update:value="onProvinceChange"
              />
            </NFormItemGi>

            <NFormItemGi :span="4" path="quanHuyen" label="Quận/Huyện" required>
              <NSelect
                v-model:value="deliveryInfo.quanHuyen" placeholder="Chọn quận/huyện" :options="districts"
                filterable clearable @update:value="onDistrictChange"
              />
            </NFormItemGi>

            <NFormItemGi :span="4" path="phuongXa" label="Phường/Xã" required>
              <NSelect
                v-model:value="deliveryInfo.phuongXa" placeholder="Chọn phường/xã" :options="wards"
                filterable clearable @update:value="onWardChange"
              />
            </NFormItemGi>

            <NFormItemGi :span="12" path="diaChiCuThe" label="Địa chỉ cụ thể" required>
              <NInput v-model:value="deliveryInfo.diaChiCuThe" placeholder="Nhập số nhà, tên đường..." clearable />
            </NFormItemGi>

            <NFormItemGi :span="12" path="ghiChu" label="Ghi chú">
              <NInput
                v-model:value="deliveryInfo.ghiChu" type="textarea" placeholder="Nhập ghi chú (nếu có)"
                :rows="3"
              />
            </NFormItemGi>
          </NGrid>
        </NForm>
      </NCard>
    </div>

    <div class="right-column">
      <NCard class="card" size="small">
        <template #header>
          <NText type="primary" strong>
            Khách hàng
          </NText>
        </template>
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
        <div v-else>
          <NSpace vertical :size="16">
            <div>
              <NText depth="3">
                Tên khách hàng
              </NText>
              <NInput v-model:value="newCustomer.ten" placeholder="Tên khách hàng" class="mt-1" />
            </div>
            <div>
              <NText depth="3">
                Số điện thoại
              </NText>
              <NInput v-model:value="newCustomer.sdt" placeholder="Số điện thoại" class="mt-1" />
            </div>
          </NSpace>
        </div>
        <template #footer>
          <NSpace>
            <NButton type="primary" secondary @click="showKhachHangModal = true">
              Chọn khách hàng
            </NButton>
            <NButton type="primary" :loading="addCustomerLoading" secondary @click="addCustomer">
              Thêm khách hàng
            </NButton>
          </NSpace>
        </template>
      </NCard>

      <NCard
        v-if="idHDS && state.autoVoucherResult?.voucherApDung?.length > 0"
        class="card" size="small"
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
                  {{ selectedVoucher.typeVoucher === 'PERCENTAGE' ? `${selectedVoucher.discountValue}%` : 'Giảm cố định'
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

        <!-- Danh sách voucher có thể áp dụng -->
        <NScrollbar style="max-height: 200px;">
          <NList size="small" bordered>
            <NListItem
              v-for="voucher in state.autoVoucherResult.voucherApDung" :key="voucher.voucherId"
              :class="{ 'active-voucher': selectedVoucher?.voucherId === voucher.voucherId }"
            >
              <NThing :title="voucher.code">
                <template #avatar>
                  <NTag :type="getVoucherTagType(voucher.typeVoucher)" size="small">
                    {{ voucher.typeVoucher === 'PERCENTAGE' ? `${voucher.discountValue}%` : 'Cố định' }}
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
                    <NSpace v-if="voucher.maxValue" justify="space-between">
                      <NText depth="3" size="small">
                        Tối đa:
                      </NText>
                      <NText depth="3" size="small">
                        {{ formatCurrency(voucher.maxValue) }}
                      </NText>
                    </NSpace>
                  </NSpace>
                </template>
              </NThing>
              <template #suffix>
                <NButton
                  type="primary" size="small" :disabled="selectedVoucher?.voucherId === voucher.voucherId"
                  :loading="applyingVoucher === voucher.voucherId"
                  @click="selectVoucher(voucher)"
                >
                  {{ selectedVoucher?.voucherId === voucher.voucherId ? 'Đã chọn' : 'Chọn' }}
                </NButton>
              </template>
            </NListItem>
          </NList>
        </NScrollbar>

        <template #footer>
          <NSpace justify="space-between" align="center" style="width: 100%">
            <NText depth="3" size="small">
              Tự động chọn voucher tốt nhất
            </NText>
            <NButton v-if="selectedVoucher" type="error" size="tiny" text @click="removeVoucher">
              Bỏ chọn
            </NButton>
          </NSpace>
        </template>
      </NCard>

      <!-- CARD GỢI Ý MUA THÊM (THÊM MỚI) -->
      <NCard
        v-if="idHDS && state.autoVoucherResult?.voucherTotHon?.length > 0"
        class="card" size="small"
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
              :key="suggestion.voucherId" :class="{ 'active-suggestion': index === 0 && suggestion.hieuQua >= 50 }"
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
                    <NSpace justify="space-between">
                      <NText depth="3" size="small">
                        Tổng giảm mới:
                      </NText>
                      <NText strong class="text-success" size="small">
                        {{ formatCurrency(suggestion.giamGiaMoi) }}
                      </NText>
                    </NSpace>
                    <NSpace justify="space-between">
                      <NText depth="3" size="small">
                        Điều kiện:
                      </NText>
                      <NText depth="3" size="small">
                        {{ formatCurrency(suggestion.dieuKien) }}
                      </NText>
                    </NSpace>
                  </NSpace>
                </template>
              </NThing>
              <template #suffix>
                <NSpace vertical :size="4" style="align-items: flex-end;">
                  <NButton
                    type="warning" size="small" :loading="applyingBetterVoucher === suggestion.voucherId"
                    @click="showSuggestionDetail(suggestion)"
                  >
                    Chi tiết
                  </NButton>
                  <NText v-if="suggestion.hieuQua >= 50" type="error" size="tiny" strong>
                    Rất hiệu quả!
                  </NText>
                </NSpace>
              </template>
            </NListItem>
          </NList>
        </NScrollbar>

        <template #footer>
          <NText depth="3" size="small">
            <NIcon :component="InformationCircleOutline" style="margin-right: 4px;" />
            Hiệu quả: % giảm thêm trên số tiền mua thêm
          </NText>
        </template>
      </NCard>

      <!-- CARD THÔNG TIN ĐƠN HÀNG (CẬP NHẬT) -->
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
              <NSwitch v-model:value="isDeliveryEnabled" size="small" @update:value="giaoHang" />
              <NButton
                type="primary" size="small" :loading="autoApplying" secondary circle
                @click="triggerAutoApplyVoucher"
              >
                <template #icon>
                  <NIcon>
                    <RocketOutline />
                  </NIcon>
                </template>
              </NButton>
            </NSpace>
          </NSpace>
        </template>

        <NSpace vertical :size="16">
          <!-- Phần chọn voucher -->
          <div v-if="idHDS">
            <NText depth="3">
              Mã giảm giá
            </NText>
            <NSpace align="center" class="mt-1">
              <NInput v-model:value="selectedDiscountCode" placeholder="Chọn mã giảm giá" readonly style="flex: 1">
                <template #prefix>
                  <NIcon :component="TicketOutline" />
                </template>
              </NInput>
              <NButton v-if="selectedVoucher" type="error" size="small" secondary @click="removeVoucher">
                Bỏ chọn
              </NButton>
            </NSpace>

            <!-- Tâm thêm -->
            <div v-if="hasBetterVoucherSuggestion" class="better-voucher-alert mt-2" />
          </div>

          <!-- Code của toàn -->
          <!-- <div v-if="hasBetterVoucherSuggestion" class="better-voucher-alert mt-2">
              <NAlert type="warning" size="small" :show-icon="true">
                <template #icon>
                  <NIcon>
                    <AlertCircleOutline />
                  </NIcon>
                </template>
                Có voucher tốt hơn!
                <NButton text type="primary" size="tiny" @click="applyBestSuggestion">
                  Xem ngay
                </NButton>
              </NAlert>
            </div>
          </div> -->

          <NDivider style="margin: 8px 0" />

          <!-- Tổng tiền chi tiết -->
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
                Đợt giảm giá:
              </NText>
              <NText type="success">
                -{{ formatCurrency(giamGia) }}
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

            <!-- Phí vận chuyển -->
            <template v-if="isDeliveryEnabled">
              <NSpace justify="space-between" align="center">
                <NText depth="3">
                  Phí vận chuyển:
                </NText>
                <NSpace align="center">
                  <NInputNumber
                    v-model:value="shippingFee" :min="0" :formatter="formatCurrencyInput"
                    :parser="parseCurrency" :disabled="isFreeShipping" size="small" style="width: 120px"
                  />
                  <img src="/images/ghn-logo.webp" alt="GHN Logo" style="height: 20px">
                </NSpace>
              </NSpace>
              <NAlert v-if="isFreeShipping" type="success" size="small" show-icon>
                Miễn phí vận chuyển (Đơn hàng trên 5,000,000 VND)
              </NAlert>
            </template>

            <NDivider style="margin: 8px 0" />

            <!-- Tổng cuối cùng -->
            <NSpace justify="space-between">
              <NText strong size="large">
                Tổng thanh toán:
              </NText>
              <NText strong type="primary" size="large">
                {{ formatCurrency(tongTien) }}
              </NText>
            </NSpace>

            <!-- Tiết kiệm được -->
            <NAlert v-if="giamGia > 0" type="success" size="small" show-icon>
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
          <NSpace vertical :size="8">
            <NText depth="3">
              Phương thức thanh toán:
            </NText>
            <NSpace>
              <NButton
                :type="state.currentPaymentMethod === '0' ? 'primary' : 'default'" size="small"
                secondary @click="handlePaymentMethod('0')"
              >
                Tiền mặt
              </NButton>
              <NButton
                :type="state.currentPaymentMethod === '1' ? 'primary' : 'default'" size="small"
                secondary @click="handlePaymentMethod('1')"
              >
                Chuyển khoản
              </NButton>
              
            </NSpace>
          </NSpace>

          <!-- Nút xác nhận thanh toán -->
          <NPopconfirm positive-text="Đồng ý" negative-text="Hủy" @positive-click="xacNhan(0)">
            <template #trigger>
              <NButton type="primary" size="large" style="width: 100%; margin-top: 8px">
                Xác nhận thanh toán
              </NButton>
            </template>
            <NText depth="3">
              Bạn có chắc chắn muốn xác nhận thanh toán hóa đơn này?
            </NText>
          </NPopconfirm>
        </NSpace>
      </NCard>
    </div>

    <!-- MODAL CHI TIẾT GỢI Ý (THÊM MỚI) -->
    <NModal
      v-model:show="showSuggestionDetailModal" preset="dialog" title="Chi tiết voucher tốt hơn"
      style="width: 500px"
    >
      <NCard v-if="selectedSuggestion" size="small">
        <NSpace vertical :size="16">
          <NSpace justify="space-between" align="center">
            <NText strong size="large">
              {{ selectedSuggestion.code }}
            </NText>
            <NTag :type="getSuggestionTagType(selectedSuggestion.hieuQua)" size="medium">
              Hiệu quả: {{ selectedSuggestion.hieuQua }}%
            </NTag>
          </NSpace>

          <NDivider />

          <NSpace vertical :size="12">
            <NSpace justify="space-between">
              <NText depth="3">
                Tổng tiền hiện tại:
              </NText>
              <NText strong>
                {{ formatCurrency(tienHang) }}
              </NText>
            </NSpace>

            <NSpace justify="space-between">
              <NText depth="3">
                Cần đạt tổng:
              </NText>
              <NText strong class="text-primary">
                {{ formatCurrency(selectedSuggestion.dieuKien) }}
              </NText>
            </NSpace>

            <NSpace justify="space-between">
              <NText depth="3">
                Cần mua thêm:
              </NText>
              <NText strong class="text-warning">
                {{ formatCurrency(selectedSuggestion.canMuaThem) }}
              </NText>
            </NSpace>

            <NDivider />

            <NSpace justify="space-between">
              <NText depth="3">
                Giảm giá hiện tại:
              </NText>
              <NText depth="3">
                {{ formatCurrency(giamGia) }}
              </NText>
            </NSpace>

            <NSpace justify="space-between">
              <NText depth="3">
                Giảm giá mới:
              </NText>
              <NText strong class="text-success">
                {{ formatCurrency(selectedSuggestion.giamGiaMoi) }}
              </NText>
            </NSpace>

            <NSpace justify="space-between">
              <NText depth="3">
                Được thêm:
              </NText>
              <NText strong class="text-success">
                +{{ formatCurrency(selectedSuggestion.giamThem) }}
              </NText>
            </NSpace>
          </NSpace>

          <NDivider />

          <NSpace justify="space-between" align="center">
            <NText depth="3" size="small">
              Mua thêm <strong>{{ formatCurrency(selectedSuggestion.canMuaThem) }}</strong>
              để được giảm thêm <strong>{{ formatCurrency(selectedSuggestion.giamThem) }}</strong>
            </NText>
            <NButton type="primary" @click="applySuggestionVoucher">
              Áp dụng khi đủ điều kiện
            </NButton>
          </NSpace>
        </NSpace>
      </NCard>
    </NModal>

    <!-- MODAL CHỌN VOUCHER (CẬP NHẬT) -->
    <NModal
      v-model:show="showDiscountModal" preset="dialog" title="Chọn mã giảm giá" style="width: 700px"
      :mask-closable="false"
    >
      <NCard size="small" :bordered="false">
        <n-tabs v-model:value="discountTab" type="segment">
          <n-tab-pane name="auto" tab="Voucher khả dụng">
            <div v-if="state.autoVoucherResult?.voucherApDung?.length > 0">
              <NScrollbar style="max-height: 400px;">
                <NList bordered class="discount-list">
                  <NListItem v-for="voucher in state.autoVoucherResult.voucherApDung" :key="voucher.voucherId">
                    <NThing :title="voucher.code">
                      <template #avatar>
                        <NTag :type="getVoucherTagType(voucher.typeVoucher)" size="small">
                          {{ voucher.typeVoucher === 'PERCENTAGE' ? `${voucher.discountValue}%` : 'Cố định' }}
                        </NTag>
                      </template>
                      <template #description>
                        <NSpace vertical :size="3" style="margin-top: 4px">
                          <NText depth="3" size="small">
                            Giảm: {{ formatCurrency(voucher.giamGiaThucTe) }}
                          </NText>
                          <NText v-if="voucher.dieuKien > 0" depth="3" size="small">
                            Điều kiện: {{ formatCurrency(voucher.dieuKien) }}
                          </NText>
                          <NText v-if="voucher.maxValue" depth="3" size="small">
                            Tối đa: {{ formatCurrency(voucher.maxValue) }}
                          </NText>
                        </NSpace>
                      </template>
                    </NThing>
                    <template #suffix>
                      <NButton
                        type="primary" size="small" :disabled="selectedVoucher?.voucherId === voucher.voucherId"
                        @click="selectVoucher(voucher)"
                      >
                        {{ selectedVoucher?.voucherId === voucher.voucherId ? 'Đã chọn' : 'Chọn' }}
                      </NButton>
                    </template>
                  </NListItem>
                </NList>
              </NScrollbar>
            </div>
            <NEmpty v-else description="Không có voucher nào phù hợp" />
          </n-tab-pane>

          <n-tab-pane v-if="state.autoVoucherResult?.voucherTotHon?.length > 0" name="suggestions" tab="Gợi ý mua thêm">
            <NScrollbar style="max-height: 400px;">
              <NList bordered class="discount-list">
                <NListItem v-for="suggestion in state.autoVoucherResult.voucherTotHon" :key="suggestion.voucherId">
                  <NThing :title="suggestion.code">
                    <template #avatar>
                      <NTag :type="getSuggestionTagType(suggestion.hieuQua)" size="small" round>
                        {{ suggestion.hieuQua }}%
                      </NTag>
                    </template>
                    <template #description>
                      <NSpace vertical :size="3" style="margin-top: 4px">
                        <NText depth="3" size="small">
                          Cần mua thêm: {{ formatCurrency(suggestion.canMuaThem)
                          }}
                        </NText>
                        <NText depth="3" size="small">
                          Giảm thêm: +{{ formatCurrency(suggestion.giamThem) }}
                        </NText>
                        <NText depth="3" size="small">
                          Điều kiện: {{ formatCurrency(suggestion.dieuKien) }}
                        </NText>
                      </NSpace>
                    </template>
                  </NThing>
                  <template #suffix>
                    <NButton type="warning" size="small" @click="showSuggestionDetail(suggestion)">
                      Chi tiết
                    </NButton>
                  </template>
                </NListItem>
              </NList>
            </NScrollbar>
          </n-tab-pane>
        </n-tabs>
      </NCard>
      <template #action>
        <NSpace justify="end">
          <NButton v-if="selectedVoucher" @click="removeVoucher">
            Bỏ chọn
          </NButton>
          <NButton type="primary" :loading="autoApplying" @click="triggerAutoApplyVoucher">
            Tìm lại voucher
          </NButton>
          <NButton @click="showDiscountModal = false">
            Đóng
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>

  <!-- Modals -->
  <NModal
    v-model:show="showProductModal" preset="dialog" title="Chọn sản phẩm" style="width: 90%; max-width: 1400px"
    :mask-closable="false"
  >
    <NCard size="small" :bordered="false">
      <template #header>
        <NSpace vertical :size="12">
          <!-- Hàng 1: Tìm kiếm và khoảng giá -->
          <NGrid :cols="24" :x-gap="12" :y-gap="12">
            <!-- Ô tìm kiếm -->
            <NGi :span="12">
              <NInput v-model:value="localSearchQuery" placeholder="Tìm kiếm sản phẩm..." clearable />
            </NGi>

            <!-- Khoảng giá với slider -->
            <NGi :span="12">
              <NFormItem label="Khoảng giá" label-placement="left">
                <NSpace vertical :size="12" style="width: 100%">
                  <n-slider
                    v-model:value="priceRange" range :step="100000" :min="stateMinMaxPrice.priceMin"
                    :max="stateMinMaxPrice.priceMax" :format-tooltip="formatTooltipRangePrice" style="width: 100%"
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

          <!-- Hàng 2: 6 combobox filter -->
          <NGrid :cols="24" :x-gap="12" :y-gap="12">
            <NGi :span="4">
              <NSelect v-model:value="localColor" :options="ColorOptions" placeholder="Màu sắc" clearable />
            </NGi>
            <NGi :span="4">
              <NSelect v-model:value="localCPU" :options="CpuOptions" placeholder="CPU" clearable />
            </NGi>
            <NGi :span="4">
              <NSelect v-model:value="localGPU" :options="GpuOptions" placeholder="GPU" clearable />
            </NGi>
            <NGi :span="4">
              <NSelect v-model:value="localRAM" :options="RamOptions" placeholder="RAM" clearable />
            </NGi>
            <NGi :span="4">
              <NSelect v-model:value="localHardDrive" :options="HardDriveOptions" placeholder="Ổ cứng" clearable />
            </NGi>
            <NGi :span="4">
              <NSelect
                v-model:value="localSelectedMaterial" :options="MaterialOptions" placeholder="Chất liệu"
                clearable
              />
            </NGi>
          </NGrid>

          <!-- Hàng 3: Nút reset -->
          <NGrid :cols="24" :x-gap="12">
            <NGi :span="24">
              <NSpace justify="end">
                <NButton type="default" size="small" secondary @click="resetFilters">
                  <template #icon>
                    <NIcon>
                      <ReloadOutline />
                    </NIcon>
                  </template>
                  Đặt lại bộ lọc
                </NButton>
              </NSpace>
            </NGi>
          </NGrid>
        </NSpace>
      </template>
      <NDataTable
        :columns="columns" :data="stateSP.products" :max-height="400" size="small" :pagination="{
          page: stateSP.paginationParams.page,
          pageSize: stateSP.paginationParams.size,
          pageCount: Math.ceil(stateSP.totalItems / stateSP.paginationParams.size),
          showSizePicker: true,
          pageSizes: [10, 20, 30, 40, 50],
        }" @update:page="handlePageChange" @update:page-size="handlePageSizeChange"
      />
    </NCard>
  </NModal>

  <!-- Modal chọn serial -->
  <NModal
    v-model:show="showSerialModal" preset="dialog" title="Chọn Serial Sản Phẩm"
    style="width: 90%; max-width: 1200px" :mask-closable="false"
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

      <!-- Thêm loading state -->
      <div v-if="loadingSerials" style="text-align: center; padding: 40px">
        <n-spin size="large">
          <template #description>
            Đang tải danh sách serial...
          </template>
        </n-spin>
      </div>

      <!-- Data table khi có dữ liệu -->
      <NDataTable
        v-else :columns="serialColumns" :data="selectedSerials" :max-height="400" size="small"
        :pagination="{ pageSize: 10 }" :loading="loadingSerials" :bordered="false"
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
              type="primary" :disabled="selectedSerialIds.length === 0 || loadingSerials"
              :loading="loadingSerials" @click="addSerialToCart"
            >
              Thêm {{ selectedSerialIds.length }} serial vào giỏ
            </NButton>
          </NSpace>
        </NSpace>
      </template>
    </NCard>
  </NModal>

  <NModal
    v-model:show="showKhachHangModal" preset="dialog" title="Chọn khách hàng"
    style="width: 90%; max-width: 1000px" :mask-closable="false"
  >
    <NCard size="small" :bordered="false">
      <NSpace vertical :size="16">
        <NInput v-model:value="customerSearchQuery" placeholder="Tìm kiếm theo tên hoặc số điện thoại..." clearable>
          <template #prefix>
            <NIcon>
              <SearchOutline />
            </NIcon>
          </template>
        </NInput>

        <NDataTable
          :columns="columnsKhachHang" :data="state.khachHang" :max-height="400" size="small" :pagination="{
            page: state.paginationParams.page,
            pageSize: state.paginationParams.size,
            pageCount: Math.ceil(state.totalItemsKH / state.paginationParams.size),
            showSizePicker: true,
            pageSizes: [10, 20, 30, 40, 50],
          }" @update:page="handleCustomerPageChange" @update:page-size="handleCustomerPageSizeChange"
        />
      </NSpace>
    </NCard>
  </NModal>

  <NModal
    v-model:show="isBothPaymentModalVisible" preset="dialog" title="Thanh toán kết hợp" positive-text="Xác nhận"
    negative-text="Hủy" :loading="bothPaymentLoading" @positive-click="confirmBothPayment"
    @negative-click="closeBothPaymentModal"
  >
    <NSpace vertical :size="16">
      <NText depth="3">
        Quét QR để thanh toán phần còn lại
      </NText>
      <NImage src="/images/qr.png" width="200" height="200" object-fit="contain" preview-disabled />
      <NForm :model="paymentForm">
        <NFormItem label="Số tiền khách đưa (VND)" path="amountPaid">
          <NInputNumber v-model:value="amountPaid" placeholder="Nhập số tiền" style="width: 100%" :min="0" />
        </NFormItem>
      </NForm>
      <NSpace vertical :size="8">
        <NSpace justify="space-between">
          <NText depth="3">
            Tổng tiền khách đã trả:
          </NText>
          <NText strong>
            {{ formatCurrency(tienKhachThanhToan + (amountPaid || 0)) }}
          </NText>
        </NSpace>
        <NSpace justify="space-between">
          <NText depth="3">
            Tiền còn thiếu:
          </NText>
          <NText type="error" strong>
            {{ formatCurrency(Math.max(0, tongTien - (tienKhachThanhToan + (amountPaid || 0)))) }}
          </NText>
        </NSpace>
      </NSpace>
    </NSpace>
  </NModal>

  <NModal
    v-model:show="isQrVNpayModalVisible" preset="dialog" title="Quét QR thanh toán" positive-text="Đã thanh toán"
    :mask-closable="false" @positive-click="closeQrModalVnPay"
  >
    <NSpace vertical align="center" :size="16">
      <NText depth="3">
        Quét mã QR để thanh toán qua VNPay
      </NText>
      <NImage src="/images/qr.png" width="200" height="200" object-fit="contain" preview-disabled />
    </NSpace>
  </NModal>

  <NModal
    v-model:show="showDeliveryModal" preset="dialog" title="Áp dụng phiếu giảm giá tốt hơn"
    positive-text="Áp dụng" negative-text="Không" :mask-closable="false" @positive-click="confirmQuantityP"
    @negative-click="closeModalP"
  >
    <NSpace vertical align="center" :size="16">
      <NText>Đang có một phiếu giảm giá tốt hơn</NText>
      <NText type="primary" strong>
        {{ phieuNgon }}
      </NText>
      <NText depth="3">
        Bạn có muốn áp dụng không?
      </NText>
    </NSpace>
  </NModal>

  <NModal
    v-model:show="isQrModalVisible" preset="dialog" title="Quét mã QR sản phẩm" :mask-closable="false"
    @after-leave="stopQrScanning"
  >
    <NCard size="small" :bordered="false">
      <div id="reader" style="width: 100%; max-width: 400px; margin: 0 auto;" />
      <NAlert v-if="!hasCamera" type="error" title="Lỗi camera" class="mt-2">
        Không tìm thấy camera hoặc không có quyền truy cập camera.
      </NAlert>
    </NCard>
  </NModal>

  <NModal
    v-model:show="showDiscountModal" preset="dialog" title="Chọn mã giảm giá" style="width: 600px"
    :mask-closable="false"
  >
    <NCard size="small" :bordered="false">
      <NList v-if="state.discountList.length > 0" class="discount-list">
        <NListItem v-for="item in state.discountList" :key="item.id">
          <NThing :title="item.ma" :description="item.ten">
            <template #avatar>
              <NTag type="success" size="small">
                {{ formatCurrency(item.giaTriGiamThucTe) }}
              </NTag>
            </template>
          </NThing>
          <template #suffix>
            <NButton
              type="primary" size="small" :disabled="!!selectedDiscount && selectedDiscount.id === item.id"
              @click="selectDiscount(item)"
            >
              {{ selectedDiscount?.id === item.id ? 'Đã chọn' : 'Chọn' }}
            </NButton>
          </template>
        </NListItem>
      </NList>
      <NEmpty v-else description="Không có mã giảm giá nào phù hợp" />
    </NCard>
    <template #action>
      <NSpace justify="end">
        <NButton v-if="selectedDiscount" @click="resetDiscount">
          Bỏ chọn
        </NButton>
        <NButton @click="showDiscountModal = false">
          Đóng
        </NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped>
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

.top-header {
  margin-bottom: 8px;
}

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

.delete-invoice-btn {
  margin-left: 4px;
}

.discount-list {
  max-height: 400px;
  overflow-y: auto;
}

.mt-1 {
  margin-top: 4px;
}

.filter-price-range {
  background: #f5f5f5;
  padding: 8px 12px;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.serial-modal-header {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 12px;
  margin-bottom: 16px;
}

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

/* Style cho checkbox trong bảng serial */
:deep(.n-checkbox) {
  margin-right: 0;
}

/* Responsive cho bảng */
@media (max-width: 768px) {
  .main-layout {
    padding: 8px;
  }

  .n-card {
    margin-bottom: 12px;
  }
}
</style>
