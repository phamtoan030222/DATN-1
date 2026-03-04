<script setup lang="ts">
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'
import type { Address } from '@/service/api/admin/users/customer/address'
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
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode'
import { debounce } from 'lodash'
import type { DataTableColumns } from 'naive-ui'
import { computed, h, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { toast } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'

// Naive UI Icons
import {
  AddCircleOutline,
  BarcodeOutline,
  CloseOutline,
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

// ==================== CONSTANTS ====================
const USER_INFO = localStorageAction.get(USER_INFO_STORAGE_KEY)
const GHN_API_TOKEN = '72f634c6-58a2-11f0-8a1e-1e10d8df3c04'
const GHN_SHOP_ID = 5872469
const FROM_DISTRICT_ID = 3440
const FROM_WARD_CODE = '13010'

// ==================== FILTER STATE ====================
const localSearchQuery = ref('')
const localColor = ref<string | null>(null)
const localCPU = ref<string | null>(null)
const localGPU = ref<string | null>(null)
const localRAM = ref<string | null>(null)
const localHardDrive = ref<string | null>(null)
const localSelectedMaterial = ref<string | null>(null)

// ==================== MASTER DATA ====================
const ColorOptions = ref<{ label: string, value: string }[]>([])
const CpuOptions = ref<{ label: string, value: string }[]>([])
const GpuOptions = ref<{ label: string, value: string }[]>([])
const RamOptions = ref<{ label: string, value: string }[]>([])
const HardDriveOptions = ref<{ label: string, value: string }[]>([])
const MaterialOptions = ref<{ label: string, value: string }[]>([])
const idNV = localStorageAction.get(USER_INFO_STORAGE_KEY)
const selectedDiscount = ref<PhieuGiamGiaResponse | null>(null)

// ==================== INVOICE STATE ====================
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
const idHDS = ref('')
const loaiHD = ref('')
let nextTabId = 1
const soTien = ref(0)

// ==================== CART STATE ====================
const state = reactive({
  gioHang: [] as any[],
  detailKhachHang: null as KhachHangResponse | null,
  discountList: [] as PhieuGiamGiaResponse[],
  phuongThuThanhToan: [] as PhuongThucThanhToanResponse[],
  khachHang: [] as KhachHangResponse[],
  products: [] as ADProductDetailResponse[],
  autoVoucherResult: null as GoiYVoucherResponse | null,
  currentPaymentMethod: '0',
  paginationParams: { page: 1, size: 10 },
  totalItemsKH: 0,
})

// ==================== PRODUCT STATE ====================
const stateSP = reactive({
  products: [] as ADProductDetailResponse[],
  paginationParams: { page: 1, size: 10 },
  totalItems: 0,
})

// ==================== VOUCHER STATE ====================
const selectedVoucher = ref<any>(null)
const applyingVoucher = ref<string | null>(null)
const autoApplying = ref(false)
const giamGia = ref(0)
const betterDiscountMessage = ref('')
const showSuggestionDetailModal = ref(false)
const selectedSuggestion = ref<any>(null)

// ==================== PAYMENT STATE ====================
const tienHang = ref(0)
const tongTien = ref(0)
const tongTienTruocGiam = ref(0)
const tienKhachThanhToan = ref(0)
const tienThieu = ref(0)
const shippingFee = ref(0)
const isFreeShipping = ref(false)

// ==================== DELIVERY STATE ====================
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
const provinceCode = ref<number | null>(null)
const districtCode = ref<number | null>(null)
const wardCode = ref<string | null>(null)
const deliveryInfoByInvoice = reactive<{ [key: string]: any }>({})

// ==================== DELIVERY EDIT MODAL ====================
const showDeliveryEditModal = ref(false)
const editDeliveryForm = reactive({
  tenNguoiNhan: '',
  sdtNguoiNhan: '',
  tinhThanhPho: null as string | null,
  quanHuyen: null as string | null,
  phuongXa: null as string | null,
  diaChiCuThe: '',
  ghiChu: '',
})
const editProvinceCode = ref<number | null>(null)
const editDistrictCode = ref<number | null>(null)
const editWardCode = ref<string | null>(null)

// ==================== CUSTOMER STATE ====================
const customerSearchQuery = ref('')
const newCustomer = reactive({ ten: '', sdt: '' })
const addCustomerLoading = ref(false)

// ==================== IMEI STATE ====================
const showSerialModal = ref(false)
const selectedProductDetail = ref<ADProductDetailResponse | null>(null)
const selectedSerials = ref<ADPDImeiResponse[]>([])
const selectedSerialIds = ref<string[]>([])
const loadingSerials = ref(false)
const serialSearchQuery = ref('')
const imeiDaChon = ref<Array<{
  idHoaDonChiTiet: string
  danhSachImei: string[]
}>>([])

// ==================== MODAL STATE ====================
const showProductModal = ref(false)
const showKhachHangModal = ref(false)
const showDiscountModal = ref(false)
const showDeliveryModal = ref(false)
const isBothPaymentModalVisible = ref(false)
const isQrVNpayModalVisible = ref(false)
const isBarcodeModalVisible = ref(false)

// ==================== BARCODE SCANNER ====================
let html5QrCode: Html5Qrcode
let isScannerRunning = false
const hasCamera = ref(true)

// ==================== PRICE RANGE ====================
const priceRange = ref<[number, number]>([0, 0])
const stateMinMaxPrice = reactive({
  priceMin: 0,
  priceMax: 0,
})

// ==================== COMPUTED ====================
const hasCartItems = computed(() => state.gioHang.length > 0)

const hasDeliveryInfo = computed(() => {
  return !!(deliveryInfo.tenNguoiNhan
    && deliveryInfo.sdtNguoiNhan
    && deliveryInfo.diaChiCuThe
    && deliveryInfo.tinhThanhPho
    && deliveryInfo.quanHuyen
    && deliveryInfo.phuongXa)
})

const filteredProducts = computed(() => {
  const colorLabel = ColorOptions.value.find(o => o.value === localColor.value)?.label
  const cpuLabel = CpuOptions.value.find(o => o.value === localCPU.value)?.label
  const gpuLabel = GpuOptions.value.find(o => o.value === localGPU.value)?.label
  const ramLabel = RamOptions.value.find(o => o.value === localRAM.value)?.label
  const hardDriveLabel = HardDriveOptions.value.find(o => o.value === localHardDrive.value)?.label
  const materialLabel = MaterialOptions.value.find(o => o.value === localSelectedMaterial.value)?.label

  return stateSP.products.filter((p) => {
    const effectivePrice = p.percentage > 0
      ? p.price * (1 - p.percentage / 100)
      : p.price

    const keyword = localSearchQuery.value.trim().toLowerCase()
    const matchSearch = !keyword
      || p.name?.toLowerCase().includes(keyword)
      || p.code?.toLowerCase().includes(keyword)

    const matchColor = !colorLabel || p.color === colorLabel
    const matchCPU = !cpuLabel || p.cpu === cpuLabel
    const matchGPU = !gpuLabel || p.gpu === gpuLabel
    const matchRAM = !ramLabel || p.ram === ramLabel
    const matchHardDrive = !hardDriveLabel || p.hardDrive === hardDriveLabel
    const matchMaterial = !materialLabel || p.material === materialLabel
    const matchPrice = effectivePrice >= priceRange.value[0] && effectivePrice <= priceRange.value[1]

    return matchSearch && matchColor && matchCPU && matchGPU && matchRAM && matchHardDrive && matchMaterial && matchPrice
  })
})

const filteredSerials = computed(() => {
  if (!serialSearchQuery.value.trim())
    return selectedSerials.value
  const keyword = serialSearchQuery.value.trim().toLowerCase()
  return selectedSerials.value.filter(s => s.code?.toLowerCase().includes(keyword))
})

const availableSerialsCount = computed(() => {
  return selectedSerials.value.filter(s => s.imeiStatus === 'AVAILABLE').length
})

// ==================== UTILITY FUNCTIONS ====================
function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function calculateTotalAmounts() {
  tienHang.value = state.gioHang.reduce((sum, item) => sum + (item.price || item.giaBan), 0)

  if (selectedVoucher.value) {
    giamGia.value = selectedVoucher.value.giamGiaThucTe
  }
  else {
    giamGia.value = 0
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

// ==================== VOUCHER FUNCTIONS ====================
async function fetchDiscounts(idHD: string) {
  try {
    if (!idHD || !hasCartItems.value) {
      resetDiscountState()
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
      betterDiscountMessage.value = `Mua thêm ${formatCurrency(bestSuggestion.canMuaThem)} để được giảm thêm ${formatCurrency(bestSuggestion.giamThem)}`
    }
    else {
      betterDiscountMessage.value = ''
    }

    if (!selectedVoucher.value && response.voucherApDung?.length > 0) {
      const bestVoucher = response.voucherApDung.reduce((best, current) =>
        (current.giamGiaThucTe || 0) > (best.giamGiaThucTe || 0) ? current : best,
      )
      await selectVoucher(bestVoucher)
    }
  }
  catch (error) {
    console.error('Fetch discounts failed', error)
    resetDiscountState()
  }
}

function resetDiscountState() {
  state.autoVoucherResult = null
  state.discountList = []
  betterDiscountMessage.value = ''
}

async function selectVoucher(voucher: any) {
  applyingVoucher.value = voucher.voucherId
  try {
    selectedVoucher.value = voucher
    giamGia.value = voucher.giamGiaThucTe
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

function removeVoucher() {
  selectedVoucher.value = null
  giamGia.value = 0
  calculateTotalAmounts()
  toast.info('Đã bỏ chọn voucher')
}

async function triggerAutoApplyVoucher() {
  if (!hasCartItems.value) {
    toast.error('Vui lòng có sản phẩm trong giỏ hàng trước!')
    return
  }

  autoApplying.value = true
  try {
    await fetchDiscounts(idHDS.value)
    if (state.autoVoucherResult?.voucherApDung?.length) {
      toast.success(`Tìm thấy ${state.autoVoucherResult.voucherApDung.length} voucher phù hợp`)
    }
    else {
      toast.info('Không tìm thấy voucher phù hợp')
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

function getVoucherTagType(type: string) {
  return type === 'PERCENTAGE' ? 'success' : 'warning'
}

function getSuggestionTagType(hieuQua: number) {
  if (hieuQua >= 50)
    return 'error'
  if (hieuQua >= 30)
    return 'warning'
  if (hieuQua >= 15)
    return 'info'
  return 'default'
}

// ==================== CART FUNCTIONS ====================
async function refreshCart() {
  if (idHDS.value) {
    const response = await GetGioHang(idHDS.value)
    state.gioHang = response
    calculateTotalAmounts()
  }
}

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

    await refreshCart()
    if (hasCartItems.value) {
      await fetchDiscounts(idHDS.value)
    }
  }
  catch (error) {
    console.error('Failed to increase quantity:', error)
    toast.error('Tăng số lượng thất bại!')
  }
}

async function decreaseQuantity(idHDCT: any, idSPS: any) {
  try {
    const formData = new FormData()
    formData.append('idSP', idSPS)
    formData.append('idHDCT', idHDCT)
    await xoaSL(formData)

    await refreshCart()
    if (hasCartItems.value) {
      await fetchDiscounts(idHDS.value)
    }
    else {
      resetDiscountState()
    }
  }
  catch (error) {
    console.error('Failed to decrease quantity:', error)
    toast.error('Giảm số lượng thất bại!')
  }
}

async function deleteProduct(idSPS: any, idHDCT: string) {
  try {
    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('idSP', idSPS)
    formData.append('idHDCT', idHDCT)
    await xoaSP(formData)

    state.gioHang = state.gioHang.filter(item => item.id !== idSPS)
    calculateTotalAmounts()

    if (hasCartItems.value) {
      await fetchDiscounts(idHDS.value)
    }
    else {
      resetDiscountState()
    }

    toast.success('Xóa sản phẩm thành công!')
  }
  catch (error) {
    console.error('Failed to delete product:', error)
    toast.error('Xóa sản phẩm thất bại!')
  }
}

// ==================== IMEI FUNCTIONS ====================
async function fetchSerialsByProduct(productId: string) {
  try {
    loadingSerials.value = true
    const response = await getImeiProductDetail(productId)
    selectedSerials.value = response.data || []
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

async function selectProductForSerial(product: ADProductDetailResponse) {
  selectedProductDetail.value = product
  selectedSerialIds.value = []
  serialSearchQuery.value = ''
  await fetchSerialsByProduct(product.id)
  showSerialModal.value = true
}

async function addSerialToCart() {
  if (selectedSerialIds.value.length === 0) {
    toast.warning('Vui lòng chọn ít nhất 1 serial')
    return
  }

  try {
    const imeisDaChon = selectedSerials.value
      .filter(s => selectedSerialIds.value.includes(s.id))
      .map(s => s.id)

    const payload = {
      invoiceId: idHDS.value,
      productDetailId: selectedProductDetail.value!.id,
      imeiIds: imeisDaChon,
    }

    await themSanPham(payload)

    toast.success(`Đã thêm ${imeisDaChon.length} serial vào giỏ hàng!`)
    showSerialModal.value = false
    selectedSerialIds.value = []

    await refreshCart()
    if (hasCartItems.value) {
      await fetchDiscounts(idHDS.value)
    }
  }
  catch (error) {
    console.error('Failed to add serials to cart:', error)
    toast.error('Thêm serial vào giỏ hàng thất bại!')
  }
}

async function printSerialBarcode(serialCode: string) {
  try {
    const win = window.open('', '_blank', 'width=520,height=380')
    if (!win) {
      toast.error('Trình duyệt chặn popup, vui lòng cho phép popup!')
      return
    }

    win.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Barcode - ${serialCode}</title>
          <script src="https://cdnjs.cloudflare.com/ajax/libs/jsbarcode/3.11.6/JsBarcode.all.min.js"><\/script>
          <style>
            @page { margin: 0; size: 90mm 45mm; }
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
              width: 90mm;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              padding: 4mm 6mm;
              background: white;
              font-family: monospace;
            }
            svg {
              width: 78mm;
              height: 28mm;
              display: block;
            }
            .serial-text {
              font-size: 11pt;
              font-weight: bold;
              margin-top: 2mm;
              letter-spacing: 1px;
              text-align: center;
            }
          </style>
        </head>
        <body>
          <svg id="barcode"></svg>
          <p class="serial-text">${serialCode}</p>
          <script>
            window.onload = function() {
              try {
                JsBarcode("#barcode", "${serialCode}", {
                  format: "CODE128",
                  width: 2.2,
                  height: 80,
                  displayValue: false,
                  margin: 4,
                  background: "#ffffff",
                  lineColor: "#000000"
                });
              } catch(e) {
                document.body.innerHTML = '<p style="color:red;padding:16px">Lỗi tạo barcode: ' + e.message + '</p>';
              }
              setTimeout(function() { window.print(); }, 400);
            }
          <\/script>
        </body>
      </html>
    `)
    win.document.close()
  }
  catch (err) {
    console.error(err)
    toast.error('Tạo barcode thất bại!')
  }
}

// ==================== CUSTOMER FUNCTIONS ====================
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
  }
}

const debouncedFetchCustomers = debounce(fetchCustomers, 300)

async function selectKhachHang(customerId: string) {
  try {
    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('idKH', customerId)
    await themKhachHang(formData)

    const response = await GeOneKhachHang(idHDS.value)
    state.detailKhachHang = response

    showKhachHangModal.value = false
    toast.success('Chọn khách hàng thành công!')

    if (hasCartItems.value) {
      await fetchDiscounts(idHDS.value)
    }
  }
  catch (error) {
    console.error('Failed to select customer:', error)
    toast.error('Chọn khách hàng thất bại!')
  }
}

function clearSelectedCustomer() {
  state.detailKhachHang = null
  toast.info('Đã bỏ chọn khách hàng')
}

async function addNewCustomer() {
  try {
    if (!newCustomer.ten || !newCustomer.sdt) {
      toast.error('Vui lòng nhập đầy đủ thông tin!')
      return
    }

    if (!/^\d{10}$/.test(newCustomer.sdt)) {
      toast.error('Số điện thoại phải là 10 chữ số!')
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

    const customerResponse = await GeOneKhachHang(idHDS.value)
    state.detailKhachHang = customerResponse

    newCustomer.ten = ''
    newCustomer.sdt = ''

    toast.success('Thêm và chọn khách hàng thành công!')

    if (hasCartItems.value) {
      await fetchDiscounts(idHDS.value)
    }
  }
  catch (error) {
    console.error('Failed to add customer:', error)
    toast.error('Thêm khách hàng thất bại!')
  }
  finally {
    addCustomerLoading.value = false
  }
}

// ==================== INVOICE FUNCTIONS ====================
async function fetchHoaDon() {
  try {
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
        await switchInvoice(tabs.value[0].id, tabs.value[0].idHD, tabs.value[0].loaiHoaDon)
      }
    }
  }
  catch (error) {
    console.error('Failed to fetch invoices:', error)
    toast.error('Lấy danh sách hóa đơn thất bại!')
  }
}

async function createInvoice() {
  if (tabs.value.length >= 10) {
    toast.warning('Chỉ được tạo tối đa 10 hóa đơn!')
    return
  }

  const tempMa = `HD-TMP-${Math.random().toString(36).substring(2, 8).toUpperCase()}`
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
  resetCurrentInvoice()

  try {
    const formData = new FormData()
    formData.append('idNV', USER_INFO.userId)
    formData.append('ma', tempMa)

    const newInvoice = await getCreateHoaDon(formData)
    const tab = tabs.value.find(t => t.id === newTabId)
    if (tab) {
      tab.idHD = newInvoice.data.id
      tab.code = newInvoice.data.code
      tab.loaiHoaDon = newInvoice.data.loaiHoaDon || 'OFFLINE'
      tab.isTemp = false
    }

    await switchInvoice(newTabId, newInvoice.data.id, newInvoice.data.loaiHoaDon || 'OFFLINE')
    toast.success('Tạo hóa đơn thành công!')
  }
  catch (error) {
    console.error('Failed to create invoice:', error)
    tabs.value = tabs.value.filter(t => t.id !== newTabId)
    toast.error('Tạo hóa đơn thất bại!')
  }
}

async function cancelInvoice(invoiceId: string) {
  try {
    const formData = new FormData()
    formData.append('idNV', USER_INFO.userId)
    formData.append('idHD', invoiceId)

    await huyHoaDon(formData)
    toast.success('Hủy hóa đơn thành công!')

    const index = tabs.value.findIndex(tab => tab.idHD === invoiceId)
    if (index !== -1) {
      tabs.value.splice(index, 1)
    }

    if (tabs.value.length > 0) {
      await switchInvoice(tabs.value[0].id, tabs.value[0].idHD, tabs.value[0].loaiHoaDon)
    }
    else {
      resetInvoiceState()
    }
  }
  catch (error: any) {
    toast.error(error?.response?.data?.message || 'Hủy hóa đơn thất bại!')
  }
}

async function switchInvoice(tabId: number, invoiceId: string, type: string) {
  idHDS.value = invoiceId
  activeTab.value = tabId
  loaiHD.value = type
  isDeliveryEnabled.value = type === 'GIAO_HANG'

  try {
    resetDiscountState()
    state.gioHang = []
    state.detailKhachHang = null

    const cartResponse = await GetGioHang(invoiceId)
    state.gioHang = cartResponse
    calculateTotalAmounts()

    const customerResponse = await GeOneKhachHang(invoiceId)
    state.detailKhachHang = customerResponse.id ? customerResponse : null

    if (hasCartItems.value) {
      await fetchDiscounts(invoiceId)
    }

    if (isDeliveryEnabled.value && state.detailKhachHang) {
      await loadDeliveryInfo()
    }
  }
  catch (error) {
    console.error('Failed to switch invoice:', error)
    toast.error('Chuyển hóa đơn thất bại!')
  }
}

function resetInvoiceState() {
  idHDS.value = ''
  loaiHD.value = ''
  state.gioHang = []
  state.detailKhachHang = null
  resetDiscountState()
  selectedVoucher.value = null
  giamGia.value = 0
  isDeliveryEnabled.value = false
  calculateTotalAmounts()
}

function resetCurrentInvoice() {
  idHDS.value = ''
  state.gioHang = []
  state.detailKhachHang = null
  resetDiscountState()
  selectedVoucher.value = null
  giamGia.value = 0
  calculateTotalAmounts()
}

// ==================== DELIVERY FUNCTIONS ====================
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
  }
}

async function fetchDistrictsFromGHN(provinceId: number) {
  try {
    const response = await getGHNDistricts(GHN_API_TOKEN, provinceId)
    districts.value = response.map((item: any) => ({
      value: String(item.DistrictID),
      label: item.DistrictName,
      code: String(item.DistrictID),
    }))
  }
  catch (error) {
    console.error('Failed to fetch districts:', error)
  }
}

async function fetchWardsFromGHN(districtId: number) {
  try {
    const response = await getGHNWards(GHN_API_TOKEN, districtId)
    wards.value = response.map((item: any) => ({
      value: item.WardCode,
      label: item.WardName,
      code: item.WardCode,
    }))
  }
  catch (error) {
    console.error('Failed to fetch wards:', error)
  }
}

// Cập nhật loadDeliveryInfo
async function loadDeliveryInfo() {
  if (!state.detailKhachHang)
    return

  console.log('Loading delivery info for customer:', state.detailKhachHang)

  deliveryInfo.tenNguoiNhan = state.detailKhachHang.ten || ''
  deliveryInfo.sdtNguoiNhan = state.detailKhachHang.sdt || ''
  deliveryInfo.diaChiCuThe = state.detailKhachHang.diaChi || ''
  deliveryInfo.tinhThanhPho = state.detailKhachHang.tinh || null
  deliveryInfo.quanHuyen = state.detailKhachHang.huyen || null
  deliveryInfo.phuongXa = state.detailKhachHang.xa || null

  // Tìm và set các code value từ tên
  if (deliveryInfo.tinhThanhPho) {
    const province = provinces.value.find(p => p.label === deliveryInfo.tinhThanhPho)
    if (province) {
      provinceCode.value = Number(province.code)
      // Fetch districts cho tỉnh này
      await fetchDistrictsFromGHN(provinceCode.value)
    }
  }

  if (deliveryInfo.quanHuyen && provinceCode.value) {
    const district = districts.value.find(d => d.label === deliveryInfo.quanHuyen)
    if (district) {
      districtCode.value = Number(district.code)
      // Fetch wards cho huyện này
      await fetchWardsFromGHN(districtCode.value)
    }
  }

  if (deliveryInfo.phuongXa && districtCode.value) {
    const ward = wards.value.find(w => w.label === deliveryInfo.phuongXa)
    if (ward) {
      wardCode.value = ward.code
    }
  }

  await loadLocationNames()
  await calculateShippingFee()
}

// Cập nhật toggleDelivery
async function toggleDelivery(enabled: boolean) {
  isDeliveryEnabled.value = enabled

  if (enabled && state.detailKhachHang) {
    await loadDeliveryInfo()
  }
  else {
    // Reset delivery info
    deliveryInfo.tenNguoiNhan = state.detailKhachHang?.ten || ''
    deliveryInfo.sdtNguoiNhan = state.detailKhachHang?.sdt || ''
    deliveryInfo.diaChiCuThe = state.detailKhachHang?.diaChi || ''
    deliveryInfo.tinhThanhPho = null
    deliveryInfo.quanHuyen = null
    deliveryInfo.phuongXa = null
    provinceCode.value = null
    districtCode.value = null
    wardCode.value = null
    shippingFee.value = 0
    calculateTotalAmounts()
  }

  // Cập nhật trạng thái giao hàng lên server
  try {
    const formData = new FormData()
    formData.append('idHD', idHDS.value)
    formData.append('check', enabled ? '1' : '0')
    await suaGiaoHang(formData)
  }
  catch (error) {
    console.error('Failed to update delivery status:', error)
  }
}

// ==================== DELIVERY EDIT FUNCTIONS ====================
function openDeliveryEditModal() {
  // Copy current values to edit form
  editDeliveryForm.tenNguoiNhan = deliveryInfo.tenNguoiNhan
  editDeliveryForm.sdtNguoiNhan = deliveryInfo.sdtNguoiNhan
  editDeliveryForm.tinhThanhPho = deliveryInfo.tinhThanhPho
  editDeliveryForm.quanHuyen = deliveryInfo.quanHuyen
  editDeliveryForm.phuongXa = deliveryInfo.phuongXa
  editDeliveryForm.diaChiCuThe = deliveryInfo.diaChiCuThe
  editDeliveryForm.ghiChu = deliveryInfo.ghiChu

  editProvinceCode.value = provinceCode.value
  editDistrictCode.value = districtCode.value
  editWardCode.value = wardCode.value

  showDeliveryEditModal.value = true
}

async function onEditProvinceChange(value: string) {
  editDeliveryForm.tinhThanhPho = value
  const selectedProvince = provinces.value.find(p => p.value === value)
  editProvinceCode.value = selectedProvince ? Number.parseInt(selectedProvince.code) : null

  if (editProvinceCode.value) {
    await fetchDistrictsFromGHN(editProvinceCode.value)
    editDeliveryForm.quanHuyen = null
    editDeliveryForm.phuongXa = null
    editDistrictCode.value = null
    editWardCode.value = null
  }
  else {
    districts.value = []
    wards.value = []
  }
}

async function onEditDistrictChange(value: string) {
  editDeliveryForm.quanHuyen = value
  const selectedDistrict = districts.value.find(d => d.value === value)
  editDistrictCode.value = selectedDistrict ? Number.parseInt(selectedDistrict.code) : null

  if (editDistrictCode.value) {
    await fetchWardsFromGHN(editDistrictCode.value)
    editDeliveryForm.phuongXa = null
    editWardCode.value = null
  }
  else {
    wards.value = []
  }
}

function onEditWardChange(value: string) {
  editDeliveryForm.phuongXa = value
  const selectedWard = wards.value.find(w => w.value === value)
  editWardCode.value = selectedWard ? selectedWard.code : null
}

async function saveDeliveryInfo() {
  // Validate
  // if (!editDeliveryForm.tenNguoiNhan || !editDeliveryForm.sdtNguoiNhan || !editDeliveryForm.diaChiCuThe
  //   || !editDeliveryForm.tinhThanhPho || !editDeliveryForm.quanHuyen || !editDeliveryForm.phuongXa) {
  //   toast.error('Vui lòng nhập đầy đủ thông tin giao hàng!')
  //   return
  // }

  // Update deliveryInfo
  deliveryInfo.tenNguoiNhan = editDeliveryForm.tenNguoiNhan
  deliveryInfo.sdtNguoiNhan = editDeliveryForm.sdtNguoiNhan
  deliveryInfo.tinhThanhPho = editDeliveryForm.tinhThanhPho
  deliveryInfo.quanHuyen = editDeliveryForm.quanHuyen
  deliveryInfo.phuongXa = editDeliveryForm.phuongXa
  deliveryInfo.diaChiCuThe = editDeliveryForm.diaChiCuThe
  deliveryInfo.ghiChu = editDeliveryForm.ghiChu

  provinceCode.value = editProvinceCode.value
  districtCode.value = editDistrictCode.value
  wardCode.value = editWardCode.value

  // Recalculate shipping fee
  await calculateShippingFee()

  // Save to storage
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
    ghiChu: deliveryInfo.ghiChu,
  }

  showDeliveryEditModal.value = false
  toast.success('Cập nhật thông tin giao hàng thành công!')
}

// ==================== PROVINCE/DISTRICT/WARD HANDLERS ====================
async function onProvinceChange(value: string) {
  deliveryInfo.tinhThanhPho = value
  const selectedProvince = provinces.value.find(p => p.value === value)
  provinceCode.value = selectedProvince ? Number.parseInt(selectedProvince.code) : null

  if (provinceCode.value) {
    await fetchDistrictsFromGHN(provinceCode.value)
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

async function onDistrictChange(value: string) {
  deliveryInfo.quanHuyen = value
  const selectedDistrict = districts.value.find(d => d.value === value)
  districtCode.value = selectedDistrict ? Number.parseInt(selectedDistrict.code) : null

  if (districtCode.value) {
    await fetchWardsFromGHN(districtCode.value)
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

async function onWardChange(value: string) {
  deliveryInfo.phuongXa = value
  const selectedWard = wards.value.find(w => w.value === value)
  wardCode.value = selectedWard ? selectedWard.code : null
  await calculateShippingFee()
}

// ==================== PRODUCT FUNCTIONS ====================
async function fetchProducts() {
  try {
    const params: ADProductDetailRequest = {
      page: 1,
      size: 9999,
    }
    const response = await getProductDetails(params)
    stateSP.products = response.data?.data || []
    state.products = response.data?.data || []
    stateSP.totalItems = response.data?.totalElements || 0
    calculateMinMaxPrice(stateSP.products)
  }
  catch (error) {
    console.error('Failed to fetch products:', error)
    toast.error('Lấy danh sách sản phẩm thất bại!')
  }
}

const debouncedFetchProducts = debounce(fetchProducts, 300)

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

  stateMinMaxPrice.priceMin = Math.floor(minPrice / 100000) * 100000
  stateMinMaxPrice.priceMax = Math.ceil(maxPrice / 100000) * 100000

  if (stateMinMaxPrice.priceMax - stateMinMaxPrice.priceMin < 100000) {
    stateMinMaxPrice.priceMax = stateMinMaxPrice.priceMin + 100000
  }

  priceRange.value = [stateMinMaxPrice.priceMin, stateMinMaxPrice.priceMax]
}

function resetFilters() {
  localSearchQuery.value = ''
  localColor.value = null
  localCPU.value = null
  localGPU.value = null
  localRAM.value = null
  localHardDrive.value = null
  localSelectedMaterial.value = null
  debouncedFetchProducts()
}

function openProductSelectionModal() {
  if (!idHDS.value) {
    toast.error('Vui lòng tạo hoặc chọn hóa đơn trước!')
    return
  }
  fetchProducts()
  showProductModal.value = true
}

// ==================== PAYMENT FUNCTIONS ====================
async function updatePaymentStatus() {
  if (idHDS.value) {
    const response = await getPhuongThucThanhToan(idHDS.value)
    state.phuongThuThanhToan = response
    let totalPaid = 0
    response.forEach((item) => { totalPaid += item.tongTien })
    tienKhachThanhToan.value = totalPaid
    tienThieu.value = (tongTien.value || 0) - tienKhachThanhToan.value
  }
}

function setPaymentMethod(method: string) {
  state.currentPaymentMethod = method
  const messages: Record<string, string> = {
    0: 'Tiền mặt',
    1: 'Chuyển khoản',
    2: 'Kết hợp',
  }
  toast.success(`Đã chọn phương thức thanh toán ${messages[method] || method}`)
}

// ==================== BARCODE SCANNER ====================
function openBarcodeScanner() {
  if (!idHDS.value) {
    toast.error('Vui lòng tạo hoặc chọn hóa đơn trước!')
    return
  }
  isBarcodeModalVisible.value = true
  nextTick(() => startBarcodeScanning())
}

function startBarcodeScanning() {
  const qrRegionId = 'reader'
  const qrRegionElement = document.getElementById(qrRegionId)
  if (!qrRegionElement)
    return

  const formatsToSupport = [
    Html5QrcodeSupportedFormats.CODE_128,
    Html5QrcodeSupportedFormats.CODE_39,
    Html5QrcodeSupportedFormats.EAN_13,
    Html5QrcodeSupportedFormats.EAN_8,
  ]

  html5QrCode = new Html5Qrcode(qrRegionId, { formatsToSupport })

  Html5Qrcode.getCameras()
    .then((cameras) => {
      if (cameras?.length) {
        hasCamera.value = true
        isScannerRunning = true
        html5QrCode.start(
          cameras[0].id,
          { fps: 10, qrbox: { width: 350, height: 120 } },
          async (decodedText: string) => {
            if (!isScannerRunning)
              return
            isScannerRunning = false
            await stopBarcodeScanning()
            isBarcodeModalVisible.value = false
            await addSerialByCode(decodedText)
          },
          () => { },
        )
      }
      else {
        hasCamera.value = false
        toast.error('Không tìm thấy camera!')
      }
    })
    .catch((error) => {
      hasCamera.value = false
      toast.error(`Lỗi truy cập camera: ${error.message || error}`)
    })
}

async function stopBarcodeScanning() {
  if (!html5QrCode)
    return
  try {
    if (html5QrCode.getState() === 2) {
      await html5QrCode.stop()
    }
  }
  catch (err) {
    // Ignore
  }
  finally {
    isScannerRunning = false
  }
}

function closeBarcodeModal() {
  isBarcodeModalVisible.value = false
  stopBarcodeScanning()
}

async function addSerialByCode(serialCode: string) {
  const code = serialCode.trim()

  if (!/^\d{15,17}$/.test(code)) {
    toast.error('Mã IMEI không hợp lệ! IMEI phải là 15-17 chữ số.')
    return
  }

  toast.info(`Đang tìm IMEI: ${code}...`)

  try {
    for (const product of stateSP.products) {
      const response = await getImeiProductDetail(product.id)
      const serial = response.data?.find((s: ADPDImeiResponse) => s.code === code)

      if (serial) {
        if (serial.imeiStatus !== 'AVAILABLE') {
          toast.error(`IMEI ${code} không khả dụng`)
          return
        }

        selectedProductDetail.value = product
        selectedSerials.value = [serial]
        selectedSerialIds.value = [serial.id]
        await addSerialToCart()
        toast.success(`Đã thêm IMEI ${code}`)
        return
      }
    }

    toast.error(`Không tìm thấy IMEI: ${code}`)
  }
  catch (error) {
    console.error('addSerialByCode error:', error)
    toast.error('Thêm IMEI thất bại!')
  }
}

// ==================== CHECKOUT FUNCTION ====================
async function xacNhan(check: number) {
  console.log('=== XÁC NHẬN THANH TOÁN ===')

  if (!idHDS.value) {
    toast.error('Vui lòng chọn một hóa đơn!')
    return
  }

  if (!hasCartItems.value) {
    toast.error('Giỏ hàng trống!')
    return
  }

  // if (isDeliveryEnabled.value) {
  //   if (!hasDeliveryInfo.value) {
  //     toast.error('Vui lòng nhập đầy đủ thông tin giao hàng!')
  //     return
  //   }
  // }

  const hasImeiProduct = state.gioHang.some(item => item.sanPhamChiTiet?.quanLyImei)
  if (hasImeiProduct && imeiDaChon.value.length === 0) {
    toast.error('Vui lòng chọn IMEI cho sản phẩm laptop!')
    return
  }

  for (const item of state.gioHang) {
    if (item.sanPhamChiTiet?.quanLyImei) {
      const imeiItem = imeiDaChon.value.find(i => i.idHoaDonChiTiet === item.idHoaDonChiTiet)
      if (!imeiItem) {
        toast.error(`Chưa chọn IMEI cho sản phẩm ${item.tenSanPham || item.name}!`)
        return
      }
      if (imeiItem.danhSachImei.length !== item.soLuong) {
        toast.error(`Số lượng IMEI cho sản phẩm ${item.tenSanPham || item.name} không khớp!`)
        return
      }
    }
  }

  try {
    const selectedProvince = provinces.value.find(p => p.value === deliveryInfo.tinhThanhPho)
    const selectedDistrict = districts.value.find(d => d.value === deliveryInfo.quanHuyen)
    const selectedWard = wards.value.find(w => w.value === deliveryInfo.phuongXa)

    const requestData = {
      idHD: idHDS.value,
      idNV: USER_INFO?.userId,
      tienHang: tienHang.value,
      tongTien: tongTien.value.toString(),
      ten: deliveryInfo.tenNguoiNhan,
      sdt: deliveryInfo.sdtNguoiNhan,
      diaChi: isDeliveryEnabled.value
        ? `${deliveryInfo.diaChiCuThe}, ${selectedWard?.label}, ${selectedDistrict?.label}, ${selectedProvince?.label}`
        : '',
      tienShip: shippingFee.value,
      giamGia: giamGia.value,
      phuongThucThanhToan: state.currentPaymentMethod,
      idPGG: selectedVoucher.value?.voucherId,
      check: isDeliveryEnabled.value ? 1 : 0,
      loaiHoaDon: isDeliveryEnabled.value ? 'GIAO_HANG' : 'TAI_QUAY',
      danhSachImei: imeiDaChon.value,
      daXacNhanImei: true,
    }

    const res = await thanhToanThanhCong(requestData)

    if (res.message) {
      if (res.message.startsWith('Số') || res.message.startsWith('Phiếu')) {
        toast.error(res.message)
        return
      }
    }

    toast.success(isDeliveryEnabled.value ? 'Đã chuyển trạng thái giao hàng!' : 'Thanh toán thành công!')
    await resetAfterPayment()
  }
  catch (error: any) {
    toast.error(error?.response?.data?.message || 'Có lỗi xảy ra!')
  }
}

// ==================== RESET AFTER PAYMENT ====================
async function resetAfterPayment() {
  await fetchProducts()

  const index = tabs.value.findIndex(tab => tab.idHD === idHDS.value)
  if (index !== -1) {
    tabs.value.splice(index, 1)
  }

  if (tabs.value.length > 0) {
    await switchInvoice(tabs.value[0].id, tabs.value[0].idHD, tabs.value[0].loaiHoaDon)
  }
  else {
    resetInvoiceState()
  }

  imeiDaChon.value = []
  showDeliveryModal.value = false
}

// ==================== LOCATION FUNCTIONS ====================
const API_GEO_V2 = 'https://provinces.open-api.vn/api/v2'

const provinceNameMap = ref<Record<string, string>>({})
const districtNameMap = ref<Record<string, string>>({})
const communeNameMap = ref<Record<string, string>>({})
const provinceCodeLookup = ref<Record<string, number>>({})
const districtCodeLookup = ref<Record<string, number>>({})
const loadedProvinceDetails = new Set<string>()

async function initProvinces() {
  try {
    const res = await fetch(`${API_GEO_V2}/p/?depth=1`)
    const data = await res.json()
    data.forEach((p: any) => {
      provinceNameMap.value[String(p.code)] = p.name
      provinceNameMap.value[p.codename] = p.name
      provinceCodeLookup.value[p.codename] = p.code
      provinceCodeLookup.value[String(p.code)] = p.code
    })
  }
  catch (e) { console.error('Lỗi tải tỉnh thành') }
}

async function fetchDistricts(provinceId: number) {
  try {
    const res = await fetch(`${API_GEO_V2}/p/${provinceId}?depth=2`)
    const data = await res.json()

    if (data.districts) {
      data.districts.forEach((d: any) => {
        districtNameMap.value[String(d.code)] = d.name
        districtNameMap.value[d.codename] = d.name
        districtCodeLookup.value[d.codename] = d.code
        districtCodeLookup.value[String(d.code)] = d.code

        if (d.wards) {
          d.wards.forEach((w: any) => {
            communeNameMap.value[String(w.code)] = w.name
            communeNameMap.value[w.codename] = w.name
          })
        }
      })
    }
  }
  catch (e) {
    console.error(`Lỗi tải quận/huyện tỉnh ${provinceId}`, e)
  }
}

async function fetchWards(districtId: number) {
  try {
    const res = await fetch(`${API_GEO_V2}/d/${districtId}?depth=2`)
    const data = await res.json()

    if (data.wards) {
      data.wards.forEach((w: any) => {
        communeNameMap.value[String(w.code)] = w.name
        communeNameMap.value[w.codename] = w.name
      })
    }
  }
  catch (e) {
    console.error(`Lỗi tải xã/phường huyện ${districtId}`, e)
  }
}

function resolveFullAddress(
  provinceCode?: string | null,
  districtCode?: string | null,
  wardCode?: string | null,
  detailAddress?: string | null,
): string {
  if (!provinceCode && !districtCode && !wardCode && !detailAddress) {
    return 'Chưa có địa chỉ'
  }

  const formatSnakeCase = (str: string): string => {
    return str.split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ')
  }

  const pRaw = String(provinceCode || '')
  let pName = provinceNameMap.value[pRaw]
  if (!pName) {
    pName = formatSnakeCase(pRaw)
    if (pName && !pName.includes('Tỉnh') && !pName.includes('Thành phố')) {
      pName = `Tỉnh ${pName}`
    }
  }

  const dRaw = String(districtCode || '')
  const dName = districtNameMap.value[dRaw] || (dRaw ? formatSnakeCase(dRaw) : '')

  const wRaw = String(wardCode || '')
  const wName = communeNameMap.value[wRaw] || (wRaw ? formatSnakeCase(wRaw) : '')

  const detail = detailAddress || ''

  const parts = [detail, wName, dName, pName].filter(part => part && part !== 'null' && part !== 'undefined')

  return parts.length > 0 ? parts.join(', ') : 'Chưa có địa chỉ'
}

const formatFullAddress = computed(() => {
  return resolveFullAddress(
    deliveryInfo.tinhThanhPho,
    deliveryInfo.quanHuyen,
    deliveryInfo.phuongXa,
    deliveryInfo.diaChiCuThe,
  )
})

async function loadLocationNames() {
  if (!deliveryInfo.tinhThanhPho) {
    console.log('Không có tinhThanhPho để tải')
    return
  }

  console.log('loadLocationNames - tinhThanhPho:', deliveryInfo.tinhThanhPho)

  let provinceId = provinceCodeLookup.value[String(deliveryInfo.tinhThanhPho)]
  if (!provinceId) {
    provinceId = Number(deliveryInfo.tinhThanhPho)
  }

  if (provinceId && !isNaN(provinceId) && !loadedProvinceDetails.has(String(provinceId))) {
    await fetchDistricts(provinceId)
    loadedProvinceDetails.add(String(provinceId))
  }

  if (deliveryInfo.quanHuyen && !districtNameMap.value[deliveryInfo.quanHuyen]) {
    console.log('Chưa có tên huyện:', deliveryInfo.quanHuyen)
  }
}

function resolveAddress(addr: Address): string {
  if (!addr)
    return 'Chưa cập nhật'

  const pRaw = String(addr.provinceCity || '')
  const pName = provinceNameMap.value[pRaw] || pRaw

  const wRaw = String(addr.wardCommune || '')
  const wName = communeNameMap.value[wRaw] || wRaw

  const detail = addr.addressDetail || ''

  return [detail, wName, pName]
    .filter(part => part && part !== 'null' && part !== 'undefined')
    .join(', ')
}

// ==================== WATCHERS ====================
watch(() => state.gioHang.length, (newLength) => {
  if (newLength === 0) {
    resetDiscountState()
    selectedVoucher.value = null
    giamGia.value = 0
  }
})

watch(() => tienHang.value, (newValue) => {
  if (newValue > 0 && idHDS.value) {
    setTimeout(() => {
      if (hasCartItems.value) {
        fetchDiscounts(idHDS.value)
      }
    }, 500)
  }
})

watch(() => state.detailKhachHang?.id, () => {
  if (hasCartItems.value && idHDS.value) {
    fetchDiscounts(idHDS.value)
  }
})

watch(customerSearchQuery, () => {
  state.paginationParams.page = 1
  debouncedFetchCustomers()
})

watch([localSearchQuery, localColor, localCPU, localGPU, localRAM, localHardDrive, localSelectedMaterial], () => {
  debouncedFetchProducts()
})

// Watch delivery info changes
watch([() => deliveryInfo.tinhThanhPho, () => deliveryInfo.quanHuyen, () => deliveryInfo.phuongXa], async () => {
  if (isDeliveryEnabled.value && idHDS.value) {
    await calculateShippingFee()
  }
})

// ==================== INIT ====================
onMounted(async () => {
  await initProvinces()
  await calculateShippingFee()
  const [colors, cpus, gpus, rams, hardDrives, materials] = await Promise.all([
    getColors(),
    getCPUs(),
    getGPUs(),
    getRAMs(),
    getHardDrives(),
    getMaterials(),
  ])

  ColorOptions.value = colors.data.map((c: any) => ({ label: c.ten, value: c.ten }))
  CpuOptions.value = cpus.data.map((c: any) => ({ label: c.ten, value: c.ten }))
  GpuOptions.value = gpus.data.map((g: any) => ({ label: g.ten, value: g.ten }))
  RamOptions.value = rams.data.map((r: any) => ({ label: r.ten, value: r.ten }))
  HardDriveOptions.value = hardDrives.data.map((h: any) => ({ label: h.ten, value: h.ten }))
  MaterialOptions.value = materials.data.map((m: any) => ({ label: m.ten, value: m.ten }))

  await fetchProvinces()
  await fetchProducts()
  await fetchCustomers()
  await fetchHoaDon()
})

// ==================== CALCULATE SHIPPING FEE ====================
async function calculateShippingFee() {
  console.log('=== CALCULATE SHIPPING FEE ===', {
    isDeliveryEnabled: isDeliveryEnabled.value,
    idHDS: idHDS.value,
    provinceCode: provinceCode.value,
    districtCode: districtCode.value,
    wardCode: wardCode.value,
    tienHang: tienHang.value,
  })

  // Kiểm tra điều kiện
  if (!isDeliveryEnabled.value) {
    console.log('❌ Delivery not enabled')
    shippingFee.value = 0
    isFreeShipping.value = false
    calculateTotalAmounts()
    return
  }

  if (!idHDS.value || !provinceCode.value || !districtCode.value || !wardCode.value) {
    console.log('❌ Missing delivery info:', {
      idHDS: idHDS.value,
      provinceCode: provinceCode.value,
      districtCode: districtCode.value,
      wardCode: wardCode.value,
    })
    shippingFee.value = 0
    isFreeShipping.value = false
    calculateTotalAmounts()
    return
  }

  const amount = Number(tienHang.value)
  console.log('Tiền hàng:', amount)

  // Check free shipping
  if (amount > 5000000) {
    console.log('✅ Free shipping applied!')
    isFreeShipping.value = true
    shippingFee.value = 0
    toast.success('Đơn hàng trên 5,000,000 VND, miễn phí vận chuyển!')
    calculateTotalAmounts()
    return
  }

  // Tính phí ship bình thường
  try {
    console.log('Calculating normal shipping fee...')
    const availableServicesRequestBody: AvailableServiceRequest = {
      shop_id: GHN_SHOP_ID,
      from_district: FROM_DISTRICT_ID,
      to_district: districtCode.value,
    }

    const availableServicesResponse = await getAvailableServices(GHN_API_TOKEN, availableServicesRequestBody)
    console.log('Available services:', availableServicesResponse)

    if (!availableServicesResponse.data || !availableServicesResponse.data.length) {
      throw new Error('No available services')
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
        InsuranceValue: amount,
        Coupon: null,
        PickShift: null,
      },
    }

    const response = await calculateFee(requestBody, GHN_API_TOKEN, GHN_SHOP_ID)
    console.log('Shipping fee response:', response)

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

// ==================== TABLE COLUMNS ====================
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
      () => 'Chọn',
    ),
  },
]

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

const columnsGiohang: DataTableColumns<any> = [
  {
    title: 'Serial đã chọn',
    key: 'imel',
    width: 110,
    render: (row) => {
      if (row.imel) {
        return h(NTag, {
          type: 'success',
          size: 'small',
          onClick: () => {
            toast.info(`IMEI: ${row.imel}`)
          },
        }, () => `${row.imel}`)
      }

      const imeiItem = imeiDaChon.value.find(
        item => item.idHoaDonChiTiet === row.idHDCT,
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
    ellipsis: { tooltip: true },
    render: row => h('div', { style: { fontWeight: 500 } }, row.name || row.ten),
  },
  {
    title: 'Thông số',
    key: 'specs',
    width: 200,
    render: row => h(NSpace, { vertical: true, size: 4 }, () => [
      h('div', { style: { display: 'flex', gap: '6px', flexWrap: 'wrap' } }, [
        row.cpu && h('span', { style: { fontSize: '11px', color: '#1677ff', background: '#e6f4ff', padding: '1px 6px', borderRadius: '3px' } }, `CPU: ${row.cpu}`),
        row.gpu && h('span', { style: { fontSize: '11px', color: '#389e0d', background: '#f6ffed', padding: '1px 6px', borderRadius: '3px' } }, `GPU: ${row.gpu}`),
        row.ram && h('span', { style: { fontSize: '11px', color: '#d46b08', background: '#fff7e6', padding: '1px 6px', borderRadius: '3px' } }, `RAM: ${row.ram}`),
      ]),
    ]),
  },
  {
    title: 'Đơn giá',
    key: 'price',
    width: 110,
    align: 'right',
    render: row => h(NText, { strong: true }, () => formatCurrency(row.giaGoc)),
  },

  {
    title: 'Thành tiền',
    key: 'total',
    width: 120,
    align: 'right',
    render: row => h(NText, { type: 'primary', strong: true }, () =>
      formatCurrency(row.giaGoc * row.soLuong * (1 - (row.percentage || 0) / 100))),
  },
  {
    title: '',
    key: 'action',
    width: 50,
    align: 'center',
    render: row => h(NButton, {
      type: 'error',
      size: 'tiny',
      text: true,
      onClick: () => deleteProduct(row.id, row.idHDCT),
    }, { icon: () => h(NIcon, null, () => h(TrashOutline)) }),
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
    title: 'Ảnh',
    key: 'image',
    width: 100,
    align: 'center',
    render: (row) => {
      if (!row.urlImage)
        return h(NText, { depth: 3 }, () => 'Không có')
      return h(NImage, {
        width: 80,
        height: 60,
        src: row.urlImage,
        objectFit: 'cover',
        style: { borderRadius: '4px' },
      })
    },
  },
  {
    title: 'Mã',
    key: 'code',
    width: 100,
    render: row => h(NText, { strong: true }, () => row.code),
  },
  {
    title: 'Tên sản phẩm',
    key: 'name',
    width: 150,
    ellipsis: { tooltip: true },
  },
  {
    title: 'Giá',
    key: 'price',
    width: 120,
    align: 'center',
    render: (row) => {
      if (row.percentage > 0) {
        const discountedPrice = row.price * (1 - row.percentage / 100)
        return h(NSpace, { vertical: true, size: 0 }, () => [
          h(NText, { delete: true, depth: 3, style: { fontSize: '12px' } }, () => formatCurrency(row.price)),
          h(NText, { type: 'primary', strong: true }, () => formatCurrency(discountedPrice)),
        ])
      }
      return h(NText, { type: 'primary', strong: true }, () => formatCurrency(row.price))
    },
  },
  {
    title: 'Tồn kho',
    key: 'quantity',
    width: 80,
    align: 'center',
    render: row => h(NTag, { type: row.quantity > 0 ? 'success' : 'error', size: 'small' }, () => row.quantity),
  },
  {
    title: 'Thao tác',
    key: 'action',
    width: 100,
    align: 'center',
    render: row => h(NButton, {
      type: 'primary',
      size: 'small',
      secondary: true,
      disabled: row.status !== 'ACTIVE' || row.quantity <= 0,
      onClick: () => selectProductForSerial(row),
    }, () => 'Chọn IMEI'),
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
    title: 'In',
    key: 'print',
    width: 80,
    align: 'center',
    render: row => h(NButton, {
      size: 'tiny',
      text: true,
      onClick: () => printSerialBarcode(row.code),
    }, {
      icon: () => h(NIcon, null, () => h(BarcodeOutline)),
    }),
  },
  {
    title: 'IMEI',
    key: 'code',
    width: 180,
    render: row => h(NText, { code: true, style: { fontFamily: 'monospace' } }, () => row.code || '-'),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 100,
    align: 'center',
    render: (row) => {
      const statusMap: Record<string, { type: any, text: string }> = {
        AVAILABLE: { type: 'success', text: 'Khả dụng' },
        SOLD: { type: 'warning', text: 'Đã bán' },
        RESERVED: { type: 'info', text: 'Đã đặt' },
      }
      const config = statusMap[row.imeiStatus] || { type: 'default', text: row.imeiStatus || '-' }
      return h(NTag, { type: config.type, size: 'small' }, () => config.text)
    },
  },
]

function formatCurrencyInput(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}
</script>

<template>
  <div class="main-layout">
    <!-- LEFT COLUMN -->
    <div class="left-column">
      <!-- Pending Invoices -->
      <NCard class="card" size="small">
        <template #header>
          <NText type="primary" strong>
            Hóa đơn chờ
          </NText>
        </template>
        <template #header-extra>
          <NButton type="primary" size="small" @click="createInvoice">
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
                <div
                  v-for="tab in tabs"
                  :key="tab.id"
                  class="pending-invoice-card"
                  :class="{ active: activeTab === tab.id }"
                  @click="switchInvoice(tab.id, tab.idHD, tab.loaiHoaDon)"
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
                      @positive-click="cancelInvoice(tab.idHD)"
                    >
                      <template #trigger>
                        <NButton text type="error" size="tiny" @click.stop>
                          <NIcon><TrashOutline /></NIcon>
                        </NButton>
                      </template>
                      <div>
                        <NText strong>
                          Xác nhận hủy
                        </NText>
                        <NText depth="3">
                          Hành động này không thể hoàn tác.
                        </NText>
                      </div>
                    </NPopconfirm>
                    <NButton v-else text type="error" size="tiny" @click.stop="cancelInvoice(tab.idHD)">
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

      <!-- Cart -->
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
            <NButton type="default" size="small" secondary @click="openBarcodeScanner">
              <template #icon>
                <NIcon><BarcodeOutline /></NIcon>
              </template>
              Quét IMEI
            </NButton>
          </NSpace>
        </template>

        <NDataTable
          v-if="hasCartItems"
          :columns="columnsGiohang"
          :data="state.gioHang"
          :max-height="280"
          size="small"
          :pagination="{ pageSize: 5 }"
        />
        <NEmpty v-else description="Không có sản phẩm nào trong giỏ hàng" size="small" />
      </NCard>

      <!-- Delivery Info -->
      <NCard v-if="isDeliveryEnabled" class="card" size="small">
        <template #header>
          <NSpace align="center" justify="space-between" style="width: 100%">
            <NText type="primary" strong>
              Thông tin người nhận
            </NText>
            <NButton
              v-if="state.detailKhachHang"
              text
              type="info"
              size="small"
              @click="showDeliveryModal = true"
            >
              <template #icon />
              Chỉnh sửa
            </NButton>
          </NSpace>
        </template>

        <!-- Hiển thị thông tin người nhận -->
        <div class="delivery-info-display">
          <NSpace vertical :size="8">
            <!-- Họ tên -->
            <div>
              <NText depth="3">
                Họ tên:
              </NText>
              <NText strong>
                {{ deliveryInfo.tenNguoiNhan || 'Chưa có' }}
              </NText>
            </div>

            <!-- Số điện thoại -->
            <div>
              <NText depth="3">
                Số điện thoại:
              </NText>
              <NText strong>
                {{ deliveryInfo.sdtNguoiNhan || 'Chưa có' }}
              </NText>
            </div>

            <!-- Địa chỉ - HIỂN THỊ THÔNG MINH -->
            <div>
              <NText depth="3">
                Địa chỉ:
              </NText>
              <NText strong class="address-text">
                {{ formatFullAddress }}
              </NText>
            </div>

            <!-- Ghi chú (nếu có) -->
            <div v-if="deliveryInfo.ghiChu">
              <NText depth="3">
                Ghi chú:
              </NText>
              <NText>{{ deliveryInfo.ghiChu }}</NText>
            </div>
          </NSpace>
        </div>
      </NCard>
    </div>

    <!-- RIGHT COLUMN -->
    <div class="right-column">
      <!-- Customer Card -->
      <NCard class="card" size="small">
        <template #header>
          <NSpace align="center" justify="space-between" style="width: 100%">
            <NText type="primary" strong>
              Khách hàng
            </NText>
            <NButton v-if="state.detailKhachHang" text type="error" size="tiny" @click="clearSelectedCustomer">
              <template #icon>
                <NIcon><CloseOutline /></NIcon>
              </template>
              Bỏ chọn
            </NButton>
          </NSpace>
        </template>

        <div v-if="state.detailKhachHang">
          <NSpace vertical :size="12">
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
          <NSpace vertical :size="12">
            <div>
              <NText depth="3">
                Tên khách hàng
              </NText>
              <NInput v-model:value="newCustomer.ten" placeholder="Nhập tên khách hàng" class="mt-1" />
            </div>
            <div>
              <NText depth="3">
                Số điện thoại
              </NText>
              <NInput v-model:value="newCustomer.sdt" placeholder="Nhập số điện thoại" class="mt-1" />
            </div>
          </NSpace>
        </div>

        <template #footer>
          <NSpace>
            <NButton type="primary" secondary @click="showKhachHangModal = true">
              Chọn khách hàng
            </NButton>
            <NButton type="primary" :loading="addCustomerLoading" secondary @click="addNewCustomer">
              Thêm khách hàng
            </NButton>
          </NSpace>
        </template>
      </NCard>

      <!-- Available Vouchers -->
      <NCard v-if="hasCartItems && state.autoVoucherResult?.voucherApDung?.length" class="card" size="small">
        <template #header>
          <NSpace align="center" justify="space-between" style="width: 100%">
            <NText type="primary" strong>
              Voucher khả dụng
            </NText>
            <NTag type="success" size="small" round>
              {{ state.autoVoucherResult.voucherApDung.length }} mã
            </NTag>
          </NSpace>
        </template>

        <div v-if="selectedVoucher" class="current-voucher-section mb-3">
          <NAlert type="success" :show-icon="true" title="Đang áp dụng">
            <NSpace vertical :size="8">
              <NSpace justify="space-between" align="center">
                <NText strong>
                  {{ selectedVoucher.code }}
                </NText>
                <NTag :type="getVoucherTagType(selectedVoucher.typeVoucher)" size="small">
                  {{ selectedVoucher.typeVoucher === 'PERCENTAGE' ? `${selectedVoucher.discountValue}%` : 'Cố định' }}
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
            </NSpace>
          </NAlert>
        </div>

        <NScrollbar style="max-height: 200px;">
          <NList size="small" bordered>
            <NListItem v-for="voucher in state.autoVoucherResult.voucherApDung" :key="voucher.voucherId">
              <NThing :title="voucher.code">
                <template #avatar>
                  <NTag :type="getVoucherTagType(voucher.typeVoucher)" size="small">
                    {{ voucher.typeVoucher === 'PERCENTAGE' ? `${voucher.discountValue}%` : 'Cố định' }}
                  </NTag>
                </template>
                <template #description>
                  <NText depth="3" size="small">
                    Giảm: {{ formatCurrency(voucher.giamGiaThucTe) }}
                  </NText>
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

        <template #footer>
          <NSpace justify="space-between" align="center" style="width: 100%">
            <NButton size="tiny" text :loading="autoApplying" @click="triggerAutoApplyVoucher">
              <template #icon>
                <NIcon><ReloadOutline /></NIcon>
              </template>
              Tìm lại voucher
            </NButton>
            <NButton v-if="selectedVoucher" type="error" size="tiny" text @click="removeVoucher">
              Bỏ chọn
            </NButton>
          </NSpace>
        </template>
      </NCard>

      <!-- Purchase Suggestions -->
      <NCard v-if="hasCartItems && state.autoVoucherResult?.voucherTotHon?.length" class="card" size="small">
        <template #header>
          <NSpace align="center" justify="space-between" style="width: 100%">
            <NText type="primary" strong>
              Gợi ý mua thêm
            </NText>
            <NTag type="warning" size="small" round>
              {{ state.autoVoucherResult.voucherTotHon.length }} đề xuất
            </NTag>
          </NSpace>
        </template>

        <NScrollbar style="max-height: 250px;">
          <NList size="small" bordered>
            <NListItem v-for="suggestion in state.autoVoucherResult.voucherTotHon" :key="suggestion.voucherId">
              <NThing :title="suggestion.code">
                <template #avatar>
                  <NTag :type="getSuggestionTagType(suggestion.hieuQua)" size="small" round>
                    {{ suggestion.hieuQua }}%
                  </NTag>
                </template>
                <template #description>
                  <NSpace vertical :size="4">
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
            </NListItem>
          </NList>
        </NScrollbar>
      </NCard>

      <!-- Order Summary -->
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
              <NSwitch v-model:value="isDeliveryEnabled" size="small" @update:value="toggleDelivery" />
            </NSpace>
          </NSpace>
        </template>

        <NSpace vertical :size="16">
          <!-- Amounts -->
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

            <NDivider />

            <NSpace justify="space-between">
              <NText strong size="large">
                Tổng thanh toán:
              </NText>
              <NText strong type="primary" size="large">
                {{ formatCurrency(tongTien) }}
              </NText>
            </NSpace>
          </NSpace>

          <!-- Payment method -->
          <NSpace vertical :size="8">
            <NText depth="3">
              Phương thức thanh toán:
            </NText>
            <NSpace>
              <NButton
                :type="state.currentPaymentMethod === '0' ? 'primary' : 'default'"
                size="small"
                secondary
                @click="setPaymentMethod('0')"
              >
                Tiền mặt
              </NButton>
              <NButton
                :type="state.currentPaymentMethod === '1' ? 'primary' : 'default'"
                size="small"
                secondary
                @click="setPaymentMethod('1')"
              >
                Chuyển khoản
              </NButton>
            </NSpace>
          </NSpace>

          <!-- Checkout button -->
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

    <!-- MODALS -->
    <NModal v-model:show="showProductModal" preset="dialog" title="Chọn sản phẩm" style="width: 90%; max-width: 1400px">
      <NCard size="small" :bordered="false">
        <template #header>
          <NSpace vertical :size="12">
            <NGrid :cols="24" :x-gap="12">
              <NGi :span="12">
                <NInput v-model:value="localSearchQuery" placeholder="Tìm kiếm sản phẩm..." clearable />
              </NGi>
              <NGi :span="12">
                <NSlider
                  v-model:value="priceRange"
                  range
                  :step="100000"
                  :min="priceRange[0]"
                  :max="priceRange[1]"
                  :format-tooltip="formatCurrency"
                />
              </NGi>
            </NGrid>

            <NGrid :cols="24" :x-gap="12">
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
                <NSelect v-model:value="localSelectedMaterial" :options="MaterialOptions" placeholder="Chất liệu" clearable />
              </NGi>
            </NGrid>

            <NGrid :cols="24">
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

        <NDataTable
          :columns="columns"
          :data="filteredProducts"
          :max-height="400"
          size="small"
          :pagination="{ pageSize: 10 }"
        />
      </NCard>
    </NModal>

    <!-- Modal chỉnh sửa thông tin giao hàng -->
    <NModal v-model:show="showDeliveryModal" preset="dialog" title="Chỉnh sửa thông tin người nhận" style="width: 90%; max-width: 800px">
      <NCard size="small" :bordered="false">
        <NForm :model="deliveryInfo" label-placement="left" :label-width="140">
          <NGrid :cols="12" :x-gap="12" :y-gap="12">
            <NFormItemGi :span="6" label="Tên người nhận" required>
              <NInput v-model:value="deliveryInfo.tenNguoiNhan" placeholder="Nhập tên người nhận" />
            </NFormItemGi>
            <NFormItemGi :span="6" label="Số điện thoại" required>
              <NInput v-model:value="deliveryInfo.sdtNguoiNhan" placeholder="Nhập số điện thoại" />
            </NFormItemGi>
            <NFormItemGi :span="4" label="Tỉnh/Thành phố" required>
              <NSelect
                v-model:value="deliveryInfo.tinhThanhPho"
                :options="provinces"
                placeholder="Chọn tỉnh/thành phố"
                filterable
                clearable
                @update:value="
                  (v) => {
                    deliveryInfo.tinhThanhPho = v
                    provinceCode = v ? Number(v) : null
                    if (provinceCode) {
                      fetchDistrictsFromGHN(provinceCode)
                    }
                    deliveryInfo.quanHuyen = null
                    deliveryInfo.phuongXa = null
                  }
                "
              />
            </NFormItemGi>
            <NFormItemGi :span="4" label="Quận/Huyện" required>
              <NSelect
                v-model:value="deliveryInfo.quanHuyen"
                :options="districts"
                placeholder="Chọn quận/huyện"
                filterable
                clearable
                :disabled="!deliveryInfo.tinhThanhPho"
                @update:value="
                  (v) => {
                    deliveryInfo.quanHuyen = v
                    districtCode = v ? Number(v) : null
                    if (districtCode) {
                      fetchWardsFromGHN(districtCode)
                    }
                    deliveryInfo.phuongXa = null
                  }
                "
              />
            </NFormItemGi>
            <NFormItemGi :span="4" label="Phường/Xã" required>
              <NSelect
                v-model:value="deliveryInfo.phuongXa"
                :options="wards"
                placeholder="Chọn phường/xã"
                filterable
                clearable
                :disabled="!deliveryInfo.quanHuyen"
                @update:value="
                  (v) => {
                    deliveryInfo.phuongXa = v
                    wardCode = v
                  }
                "
              />
            </NFormItemGi>
            <NFormItemGi :span="12" label="Địa chỉ cụ thể" required>
              <NInput v-model:value="deliveryInfo.diaChiCuThe" placeholder="Nhập số nhà, tên đường..." />
            </NFormItemGi>
            <NFormItemGi :span="12" label="Ghi chú">
              <NInput v-model:value="deliveryInfo.ghiChu" type="textarea" placeholder="Nhập ghi chú (nếu có)" :rows="2" />
            </NFormItemGi>
          </NGrid>
        </NForm>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="showDeliveryModal = false">
              Hủy
            </NButton>
            <NButton type="primary" @click="saveDeliveryInfo">
              Lưu thay đổi
            </NButton>
          </NSpace>
        </template>
      </NCard>
    </NModal>

    <NModal v-model:show="showSerialModal" preset="dialog" title="Chọn IMEI" style="width: 90%; max-width: 1200px">
      <NCard size="small" :bordered="false">
        <template #header>
          <div class="serial-modal-header">
            <div>
              <NText strong>
                {{ selectedProductDetail?.name }} - {{ selectedProductDetail?.code }}
              </NText>
              <NText depth="3" size="small">
                Giá: {{ formatCurrency(selectedProductDetail?.price || 0) }}
              </NText>
            </div>
            <div>
              <NText type="primary" strong>
                Còn {{ availableSerialsCount }} IMEI
              </NText>
            </div>
          </div>
          <NInput
            v-model:value="serialSearchQuery"
            placeholder="Tìm theo IMEI..."
            clearable
            style="margin-top: 12px"
          >
            <template #prefix>
              <NIcon><SearchOutline /></NIcon>
            </template>
          </NInput>
        </template>

        <NDataTable
          :columns="serialColumns"
          :data="filteredSerials"
          :max-height="400"
          size="small"
          :loading="loadingSerials"
          :pagination="{ pageSize: 10 }"
        />

        <template #footer>
          <NSpace justify="space-between" align="center" style="width: 100%">
            <NText depth="3">
              Đã chọn {{ selectedSerialIds.length }} IMEI
            </NText>
            <NSpace>
              <NButton @click="showSerialModal = false">
                Hủy
              </NButton>
              <NButton
                type="primary"
                :disabled="selectedSerialIds.length === 0"
                @click="addSerialToCart"
              >
                Thêm {{ selectedSerialIds.length }} IMEI
              </NButton>
            </NSpace>
          </NSpace>
        </template>
      </NCard>
    </NModal>

    <NModal v-model:show="showKhachHangModal" preset="dialog" title="Chọn khách hàng" style="width: 90%; max-width: 1000px">
      <NCard size="small" :bordered="false">
        <NSpace vertical :size="16">
          <NInput v-model:value="customerSearchQuery" placeholder="Tìm kiếm theo tên hoặc số điện thoại..." clearable>
            <template #prefix>
              <NIcon><SearchOutline /></NIcon>
            </template>
          </NInput>

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
              pageSizes: [10, 20, 30],
            }"
            @update:page="(p) => { state.paginationParams.page = p; fetchCustomers() }"
            @update:page-size="(s) => { state.paginationParams.size = s; state.paginationParams.page = 1; fetchCustomers() }"
          />
        </NSpace>
      </NCard>
    </NModal>

    <!-- MODAL CHỈNH SỬA THÔNG TIN GIAO HÀNG -->
    <NModal
      v-model:show="showDeliveryEditModal"
      preset="dialog"
      title="Chỉnh sửa thông tin giao hàng"
      style="width: 700px"
      :mask-closable="false"
    >
      <NCard size="small" :bordered="false">
        <NForm :model="editDeliveryForm" label-placement="left" :label-width="120">
          <NGrid :cols="24" :x-gap="12" :y-gap="12">
            <NFormItemGi :span="12" label="Tên người nhận" required>
              <NInput
                v-model:value="editDeliveryForm.tenNguoiNhan"
                placeholder="Nhập tên người nhận"
              />
            </NFormItemGi>
            <NFormItemGi :span="12" label="Số điện thoại" required>
              <NInput
                v-model:value="editDeliveryForm.sdtNguoiNhan"
                placeholder="Nhập số điện thoại"
              />
            </NFormItemGi>

            <NFormItemGi :span="8" label="Tỉnh/Thành phố" required>
              <NSelect
                v-model:value="editDeliveryForm.tinhThanhPho"
                :options="provinces"
                placeholder="Chọn tỉnh/thành phố"
                filterable
                clearable
                @update:value="onEditProvinceChange"
              />
            </NFormItemGi>
            <NFormItemGi :span="8" label="Quận/Huyện" required>
              <NSelect
                v-model:value="editDeliveryForm.quanHuyen"
                :options="districts"
                placeholder="Chọn quận/huyện"
                filterable
                clearable
                @update:value="onEditDistrictChange"
              />
            </NFormItemGi>
            <NFormItemGi :span="8" label="Phường/Xã" required>
              <NSelect
                v-model:value="editDeliveryForm.phuongXa"
                :options="wards"
                placeholder="Chọn phường/xã"
                filterable
                clearable
                @update:value="onEditWardChange"
              />
            </NFormItemGi>

            <NFormItemGi :span="24" label="Địa chỉ cụ thể" required>
              <NInput
                v-model:value="editDeliveryForm.diaChiCuThe"
                placeholder="Nhập số nhà, tên đường..."
              />
            </NFormItemGi>

            <NFormItemGi :span="24" label="Ghi chú">
              <NInput
                v-model:value="editDeliveryForm.ghiChu"
                type="textarea"
                placeholder="Nhập ghi chú (nếu có)"
                :rows="2"
              />
            </NFormItemGi>
          </NGrid>
        </NForm>

        <template #footer>
          <NSpace justify="end">
            <NButton @click="showDeliveryEditModal = false">
              Hủy
            </NButton>
            <NButton type="primary" @click="saveDeliveryInfo">
              Lưu thay đổi
            </NButton>
          </NSpace>
        </template>
      </NCard>
    </NModal>

    <NModal
      v-model:show="isBarcodeModalVisible"
      preset="dialog"
      title="Quét IMEI"
      :mask-closable="false"
      @update:show="(val) => { if (!val) closeBarcodeModal() }"
    >
      <NCard size="small" :bordered="false">
        <NAlert type="info" size="small" show-icon style="margin-bottom: 12px;">
          Đặt mã IMEI (15-17 số) vào khung hình để quét.
        </NAlert>

        <div id="reader" style="width: 100%; max-width: 440px; margin: 0 auto;" />

        <NAlert v-if="!hasCamera" type="error" title="Lỗi camera" class="mt-2">
          Không tìm thấy camera hoặc không có quyền truy cập camera.
        </NAlert>
      </NCard>
    </NModal>
  </div>
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

.mt-1 {
  margin-top: 4px;
}

.serial-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  margin-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.address-text {
  display: block;
  line-height: 1.5;
  white-space: normal;
  word-break: break-word;
  max-width: 100%;
}

:deep(#reader) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(#reader video) {
  border-radius: 8px;
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
</style>
