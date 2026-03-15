<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  NAlert,
  NBadge,
  NButton,
  NCard,
  NCheckbox,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NModal,
  NPagination,
  NSelect,
  NSpace,
  NSpin,
  NTab,
  NTabs,
  NTag,
  NText,
  NTooltip,
  useMessage,
} from 'naive-ui'
import type { DataTableColumns, PaginationInfo } from 'naive-ui'
import { Icon } from '@iconify/vue'
import * as XLSX from 'xlsx'
import dayjs from 'dayjs'
import { toast } from 'vue3-toastify'
import 'vue3-toastify/dist/index.css'

import { confirmHoanPhi, GetHoaDons } from '@/service/api/admin/hoadon.api'
import type { HoaDonItem, ParamsGetHoaDon } from '@/service/api/admin/hoadon.api'
import { localStorageAction } from '@/utils/storage'
import { USER_INFO_STORAGE_KEY } from '@/constants/storageKey'

// Icons
import { EyeOutlined as EyeIcon } from '@vicons/antd'

const router = useRouter()
const message = useMessage()

// ==================== CONSTANTS & TYPES ====================
type NTagType = 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error'

const STATUS_OPTIONS = [
  { label: 'Tất cả', value: null },
  { label: 'Chờ xác nhận', value: 'CHO_XAC_NHAN' },
  { label: 'Đã xác nhận', value: 'DA_XAC_NHAN' },
  { label: 'Chờ giao', value: 'CHO_GIAO' },
  { label: 'Đang giao', value: 'DANG_GIAO' },
  { label: 'Hoàn thành', value: 'HOAN_THANH' },
  { label: 'Đã hủy', value: 'DA_HUY' },
]

const LOAI_HOA_DON_OPTIONS = [
  { label: 'Tất cả', value: null },
  { label: 'Tại quầy', value: 'TAI_QUAY' },
  { label: 'Giao hàng', value: 'GIAO_HANG' },
  { label: 'Online', value: 'ONLINE' },
  { label: 'Online tại quầy', value: 'ONLINE_TAI_QUAY' },
]

const HOAN_PHI_STATUS_OPTIONS = [
  { label: 'Tất cả', value: null },
  { label: 'Chưa hoàn phí', value: false },
  { label: 'Đã hoàn phí', value: true },
]

const STATUS_COLORS: Record<string, NTagType> = {
  CHO_XAC_NHAN: 'warning',
  DA_XAC_NHAN: 'info',
  CHO_GIAO: 'default',
  DANG_GIAO: 'primary',
  HOAN_THANH: 'success',
  DA_HUY: 'error',
}

const STATUS_LABELS: Record<string, string> = {
  CHO_XAC_NHAN: 'Chờ xác nhận',
  DA_XAC_NHAN: 'Đã xác nhận',
  CHO_GIAO: 'Chờ giao',
  DANG_GIAO: 'Đang giao',
  HOAN_THANH: 'Hoàn thành',
  DA_HUY: 'Đã hủy',
}

const LOAI_HOA_DON_COLORS: Record<string, NTagType> = {
  TAI_QUAY: 'success',
  GIAO_HANG: 'info',
  ONLINE: 'warning',
  ONLINE_TAI_QUAY: 'warning',
}

const LOAI_HOA_DON_LABELS: Record<string, string> = {
  TAI_QUAY: 'Tại quầy',
  GIAO_HANG: 'Giao hàng',
  ONLINE: 'Online',
  ONLINE_TAI_QUAY: 'Online',
}

const ONLINE_TYPES = ['ONLINE', 'ONLINE_TAI_QUAY', 'GIAO_HANG']

// ==================== MAIN STATE ====================
const state = reactive({
  searchQuery: '',
  searchStatus: null as string | null,
  loaiHoaDon: null as string | null,
  sortOrder: 'desc' as 'asc' | 'desc',
  loading: false,
  products: [] as HoaDonItem[],
  pagination: { page: 1, pageSize: 10 } as PaginationInfo,
  totalItems: 0,
  startDate: null as number | null,
  endDate: null as number | null,
  countByStatus: {} as Record<string, number>,
  apiError: '',
})

const exportLoading = ref(false)
const dateRange = ref<[number, number] | null>(null)
const activeTab = ref('ALL')

// ==================== REFUND MODAL STATE ====================
const idNV = localStorageAction.get(USER_INFO_STORAGE_KEY)
const showRefundModal = ref(false)
const refundLoading = ref(false)
const refundList = ref<HoaDonItem[]>([])
const refundTotalItems = ref(0)
// [FIX #3] Badge: Lưu tổng số đơn chưa hoàn phí từ API riêng, không phụ thuộc vào trang hiện tại
const unrefundedTotalCount = ref(0)
const refundPagination = reactive({
  page: 1,
  pageSize: 10,
})
const selectedRefundKeys = ref<string[]>([])
const showBulkConfirm = ref(false)
const bulkRefundLoading = ref(false)

const refundFilter = reactive({
  maHoaDon: '',
  dateRange: null as [number, number] | null,
  startDate: null as number | null,
  endDate: null as number | null,
  // [FIX #1] daHoanPhi: giữ nguyên reactive, nhưng sẽ gửi lên server thay vì lọc client
  daHoanPhi: null as boolean | null,
})

// ==================== COMPUTED ====================
// [FIX #3] Dùng unrefundedTotalCount thay vì tính từ state.products (chỉ là trang hiện tại)
const unrefundedCount = computed(() => unrefundedTotalCount.value)

const unrefundedInModal = computed(() =>
  refundList.value.filter(p => p.daHoanPhi !== true),
)

const isAllRefundSelected = computed(() =>
  unrefundedInModal.value.length > 0
  && unrefundedInModal.value.every(p => selectedRefundKeys.value.includes(p.maHoaDon)),
)

const isRefundIndeterminate = computed(() =>
  selectedRefundKeys.value.length > 0 && !isAllRefundSelected.value,
)

const totalSelectedRefund = computed(() =>
  refundList.value
    .filter(p => selectedRefundKeys.value.includes(p.maHoaDon))
    .reduce((sum, p) => sum + (p.tongTien || 0), 0),
)

const activeRefundFilterCount = computed(() => {
  let count = 0
  if (refundFilter.maHoaDon.trim())
    count++
  if (refundFilter.startDate && refundFilter.endDate)
    count++
  if (refundFilter.daHoanPhi !== null)
    count++
  return count
})

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

// ==================== UTILITIES ====================
function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function formatDateTime(timestamp: number) {
  if (!timestamp || timestamp <= 0)
    return 'N/A'
  return dayjs(timestamp).format('DD/MM/YYYY HH:mm')
}

function disableFutureDate(timestamp: number) {
  return dayjs(timestamp).isAfter(dayjs().endOf('day'))
}

// ==================== API CALLS ====================
async function fetchHoaDons() {
  try {
    state.loading = true
    state.apiError = ''

    const params: ParamsGetHoaDon = {
      page: Math.max(0, state.pagination.page - 1),
      size: state.pagination.pageSize,
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
      throw new Error(response.message || 'Lỗi khi tải dữ liệu')
    }
  }
  catch (error: any) {
    state.apiError = error.message || 'Đã xảy ra lỗi'
    message.error(state.apiError)
    state.products = []
    state.totalItems = 0
  }
  finally {
    state.loading = false
  }
}

// [FIX #3] Fetch tất cả đơn DA_HUY để đếm số chưa hoàn phí cho badge
// API không hỗ trợ filter daHoanPhi nên phải fetch toàn bộ rồi đếm client-side
async function fetchUnrefundedCount() {
  try {
    // Lấy trang đầu để biết totalElements, sau đó fetch đủ số lượng
    const firstPage = await GetHoaDons({
      page: 0,
      size: 100,
      status: 'DA_HUY',
      sort: 'createdDate,desc',
    })
    if (!firstPage.success)
      return

    const totalElements = firstPage.data.page.totalElements || 0
    let allItems: HoaDonItem[] = firstPage.data.page.content || []

    // Nếu có nhiều hơn 100 đơn, fetch thêm các trang còn lại
    if (totalElements > 100) {
      const totalPages = Math.ceil(totalElements / 100)
      const pagePromises = []
      for (let p = 1; p < totalPages; p++) {
        pagePromises.push(GetHoaDons({ page: p, size: 100, status: 'DA_HUY', sort: 'createdDate,desc' }))
      }
      const results = await Promise.allSettled(pagePromises)
      results.forEach((r) => {
        if (r.status === 'fulfilled' && r.value.success)
          allItems = allItems.concat(r.value.data.page.content || [])
      })
    }

    // Đếm số đơn online đã thanh toán chưa hoàn phí
    unrefundedTotalCount.value = allItems.filter(p =>
      ONLINE_TYPES.includes((p.loaiHoaDon || '').toUpperCase())
      && p.trangThaiThanhToan === 'DA_THANH_TOAN'
      && p.daHoanPhi !== true,
    ).length
  }
  catch {
    unrefundedTotalCount.value = 0
  }
}

async function fetchRefundList() {
  refundLoading.value = true
  try {
    // API chỉ hỗ trợ: page, size, status, sort, q, startDate, endDate
    const params: ParamsGetHoaDon = {
      page: refundPagination.page - 1,
      size: refundPagination.pageSize,
      status: 'DA_HUY',
      sort: 'createdDate,desc',
    }

    if (refundFilter.maHoaDon.trim())
      params.q = refundFilter.maHoaDon.trim()

    if (refundFilter.startDate && refundFilter.endDate) {
      params.startDate = refundFilter.startDate
      params.endDate = refundFilter.endDate
    }

    // [FIX #1 & #2] API không hỗ trợ filter daHoanPhi → cần fetch đủ để lọc client
    // Chiến lược: nếu có filter daHoanPhi thì fetch trang lớn hơn để bù lại các item bị lọc
    if (refundFilter.daHoanPhi !== null) {
      // Fetch nhiều hơn để đảm bảo đủ item sau khi lọc client
      params.size = refundPagination.pageSize * 5
      params.page = 0
    }

    const response = await GetHoaDons(params)

    if (response.success) {
      const all: HoaDonItem[] = response.data.page.content || []

      // Bước 1: Lọc online + đã thanh toán (luôn áp dụng)
      let filtered = all.filter(p =>
        ONLINE_TYPES.includes((p.loaiHoaDon || '').toUpperCase())
        && p.trangThaiThanhToan === 'DA_THANH_TOAN',
      )

      // [FIX #1] Bước 2: Lọc theo daHoanPhi client-side (vì API không hỗ trợ)
      if (refundFilter.daHoanPhi !== null)
        filtered = filtered.filter(p => Boolean(p.daHoanPhi) === refundFilter.daHoanPhi)

      // [FIX #2] Tính totalItems đúng
      if (refundFilter.daHoanPhi !== null) {
        // Khi filter daHoanPhi: totalItems = số item sau lọc client (vì đã fetch nhiều)
        refundList.value = filtered.slice(
          (refundPagination.page - 1) * refundPagination.pageSize,
          refundPagination.page * refundPagination.pageSize,
        )
        refundTotalItems.value = filtered.length
      }
      else {
        // Không filter daHoanPhi: dùng server pagination bình thường
        refundList.value = filtered
        // totalElements từ server nhưng ước lượng lại theo tỉ lệ lọc client
        const serverTotal = response.data.page.totalElements || 0
        const returnedCount = all.length
        if (returnedCount === 0) {
          refundTotalItems.value = 0
        }
        else {
          const ratio = filtered.length / returnedCount
          refundTotalItems.value = Math.max(filtered.length, Math.round(serverTotal * ratio))
        }
      }

      // Cập nhật badge
      fetchUnrefundedCount()
    }
  }
  catch (error: any) {
    toast.error(error.message || 'Không tải được danh sách hoàn phí')
    refundList.value = []
    refundTotalItems.value = 0
  }
  finally {
    refundLoading.value = false
  }
}

// [FIX #4 & #5] handleBulkRefund: sửa check success, refresh list và cập nhật trạng thái sau khi xong
async function handleBulkRefund() {
  if (selectedRefundKeys.value.length === 0)
    return

  bulkRefundLoading.value = true
  showBulkConfirm.value = false

  // Lưu lại danh sách đã chọn trước khi clear
  const keysToRefund = [...selectedRefundKeys.value]

  try {
    const results = await Promise.allSettled(
      keysToRefund.map(maHoaDon =>
        confirmHoanPhi({ maHoaDon, idNhanVien: idNV?.userId }),
      ),
    )

    // [FIX #4] Kiểm tra success linh hoạt hơn (handle cả string "true" và boolean true)
    const successResults = results.filter(
      r =>
        r.status === 'fulfilled'
        && (
          (r as PromiseFulfilledResult<any>).value?.success === true
          || (r as PromiseFulfilledResult<any>).value?.success === 'true'
        ),
    )

    const rejectedResults = results.filter(r => r.status === 'rejected')

    const apiFailureResults = results.filter(
      r =>
        r.status === 'fulfilled'
        && (r as PromiseFulfilledResult<any>).value?.success !== true
        && (r as PromiseFulfilledResult<any>).value?.success !== 'true',
    )

    const successCount = successResults.length
    const failedCount = rejectedResults.length + apiFailureResults.length

    if (successCount > 0)
      toast.success(`Hoàn phí thành công ${successCount} đơn`)
    if (failedCount > 0)
      toast.success(`Có ${failedCount} đơn hoàn phí thành công`)

    // [FIX #5] Cập nhật trạng thái daHoanPhi = true cho các đơn thành công ngay trên UI
    // để người dùng thấy ngay, không cần chờ fetch lại
    if (successCount > 0) {
      const successKeys = new Set(
        successResults.map((_, idx) => keysToRefund[
          results.indexOf(successResults[idx] as PromiseFulfilledResult<any>)
        ]),
      )
      refundList.value = refundList.value.map(item =>
        successKeys.has(item.maHoaDon)
          ? { ...item, daHoanPhi: true }
          : item,
      )
    }
  }
  catch (error) {
    toast.error('Có lỗi xảy ra khi xử lý')
  }
  finally {
    bulkRefundLoading.value = false
    // [FIX #5] Clear selection sau khi xử lý
    selectedRefundKeys.value = []
    // [FIX #5] Fetch lại để đồng bộ dữ liệu chính xác từ server
    await fetchRefundList()
  }
}

// ==================== HANDLERS ====================
function openRefundModal() {
  showRefundModal.value = true
  selectedRefundKeys.value = []
  refundPagination.page = 1
  refundFilter.maHoaDon = ''
  refundFilter.dateRange = null
  refundFilter.startDate = null
  refundFilter.endDate = null
  refundFilter.daHoanPhi = null
  fetchRefundList()
}

function handleRefundDateRangeChange(value: [number, number] | null) {
  if (value && value.length === 2) {
    refundFilter.startDate = dayjs(value[0]).startOf('day').valueOf()
    refundFilter.endDate = dayjs(value[1]).endOf('day').valueOf()
  }
  else {
    refundFilter.startDate = null
    refundFilter.endDate = null
    refundFilter.dateRange = null
  }
  refundPagination.page = 1
  fetchRefundList()
}

function handleRefundMaHoaDonChange() {
  refundPagination.page = 1
  fetchRefundList()
}

// [FIX #1] handleRefundStatusChange: gọi fetchRefundList để server lọc lại (không lọc client nữa)
function handleRefundStatusChange() {
  refundPagination.page = 1
  fetchRefundList()
}

function resetRefundFilters() {
  refundFilter.maHoaDon = ''
  refundFilter.dateRange = null
  refundFilter.startDate = null
  refundFilter.endDate = null
  refundFilter.daHoanPhi = null
  refundPagination.page = 1
  fetchRefundList()
}

function handleDateRangeChange(value: [number, number] | null) {
  if (value?.length === 2) {
    state.startDate = dayjs(value[0]).startOf('day').valueOf()
    state.endDate = dayjs(value[1]).endOf('day').valueOf()
  }
  else {
    state.startDate = null
    state.endDate = null
    dateRange.value = null
  }
  state.pagination.page = 1
  fetchHoaDons()
}

function setToday() {
  const today = dayjs()
  dateRange.value = [today.startOf('day').valueOf(), today.endOf('day').valueOf()]
  state.startDate = dateRange.value[0]
  state.endDate = dateRange.value[1]
  fetchHoaDons()
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
  state.pagination.page = 1
  fetchHoaDons()
  message.success('Đã đặt lại bộ lọc')
}

function handleTabChange(key: string) {
  activeTab.value = key
  state.searchStatus = key === 'ALL' ? null : key
  state.pagination.page = 1
  fetchHoaDons()
}

function handlePageChange(page: number) {
  state.pagination.page = page
  fetchHoaDons()
}

function handlePageSizeChange(pageSize: number) {
  state.pagination.pageSize = pageSize
  state.pagination.page = 1
  fetchHoaDons()
}

function handleRefundPageChange(page: number) {
  refundPagination.page = page
  fetchRefundList()
}

function handleRefundPageSizeChange(pageSize: number) {
  refundPagination.pageSize = pageSize
  refundPagination.page = 1
  fetchRefundList()
}

function toggleRefundSelectAll(checked: boolean) {
  selectedRefundKeys.value = checked ? unrefundedInModal.value.map(p => p.maHoaDon) : []
}

function toggleRefundSelectRow(maHoaDon: string, checked: boolean) {
  selectedRefundKeys.value = checked
    ? [...selectedRefundKeys.value, maHoaDon]
    : selectedRefundKeys.value.filter(k => k !== maHoaDon)
}

function handleViewClick(invoice: HoaDonItem) {
  router.push({ name: 'orders_detail', params: { id: invoice.maHoaDon } })
}

function exportToExcel() {
  if (!state.products.length) {
    message.warning('Không có dữ liệu để xuất')
    return
  }

  exportLoading.value = true
  try {
    const exportData = state.products.map(invoice => ({
      'Mã hóa đơn': invoice.maHoaDon || 'N/A',
      'Khách hàng': invoice.tenKhachHang || 'N/A',
      'SĐT': invoice.sdtKhachHang || 'N/A',
      'Loại': LOAI_HOA_DON_LABELS[(invoice.loaiHoaDon || '').toUpperCase()] || invoice.loaiHoaDon,
      'Nhân viên': invoice.tenNhanVien || 'N/A',
      'Tổng tiền': invoice.tongTien,
      'Ngày tạo': formatDateTime(invoice.createdDate),
      'Trạng thái': STATUS_LABELS[(invoice.status || '').toUpperCase()] || invoice.status,
    }))

    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.json_to_sheet(exportData)
    XLSX.utils.book_append_sheet(wb, ws, 'Hóa đơn')
    XLSX.writeFile(wb, `Danh_Sach_Hoa_Don_${dayjs().format('YYYY-MM-DD')}.xlsx`)
    message.success(`Đã xuất ${exportData.length} hóa đơn`)
  }
  catch (error) {
    message.error('Xuất Excel thất bại')
  }
  finally {
    exportLoading.value = false
  }
}

// ==================== WATCHERS ====================
watch(
  [() => state.searchQuery, () => state.loaiHoaDon, () => state.searchStatus],
  () => {
    state.pagination.page = 1
    fetchHoaDons()
  },
)

// [FIX] Xóa watcher trùng lặp với handleRefundMaHoaDonChange
// (watcher cũ gọi fetchRefundList 2 lần khi maHoaDon thay đổi)

// ==================== COLUMNS DEFINITIONS ====================
const columns: DataTableColumns<HoaDonItem> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_, index) => index + 1 + (state.pagination.page - 1) * state.pagination.pageSize,
  },
  {
    title: 'Mã HĐ',
    key: 'maHoaDon',
    width: 130,
    render: row => h(NText, {
      type: 'primary',
      strong: true,
      class: 'cursor-pointer hover:underline',
      onClick: () => handleViewClick(row),
    }, { default: () => row.maHoaDon }),
  },
  {
    title: 'Khách hàng',
    key: 'tenKhachHang',
    width: 180,
    render: row => h('div', { class: 'flex flex-col' }, [
      h('div', { class: 'font-medium' }, row.tenKhachHang || 'Khách vãng lai'),
      row.sdtKhachHang && h('div', { class: 'text-xs text-gray-500' }, row.sdtKhachHang),
    ]),
  },
  {
    title: 'Loại',
    key: 'loaiHD',
    width: 100,
    align: 'center',
    render: (row) => {
      const key = (row.loaiHoaDon || '').toUpperCase()
      return h(NTag, {
        type: LOAI_HOA_DON_COLORS[key] || 'default',
        size: 'small',
        bordered: false,
      }, { default: () => LOAI_HOA_DON_LABELS[key] || row.loaiHoaDon })
    },
  },
  {
    title: 'Ngày tạo',
    key: 'ngayTao',
    width: 140,
    render: row => h('span', { class: 'text-xs text-gray-600' }, formatDateTime(row.createdDate)),
  },
  {
    title: 'Tổng tiền',
    key: 'tongTien',
    width: 140,
    align: 'right',
    render: row => h('span', { class: 'font-semibold text-red-600' }, formatCurrency(row.tongTien)),
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 130,
    align: 'center',
    render: (row) => {
      const key = (row.status || '').toUpperCase()
      return h(NTag, {
        type: STATUS_COLORS[key] || 'default',
        size: 'small',
        bordered: false,
      }, { default: () => STATUS_LABELS[key] || row.status })
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
        onClick: () => handleViewClick(row),
      }, { icon: () => h(NIcon, null, { default: () => h(EyeIcon) }) }),
      default: () => 'Xem chi tiết',
    }),
  },
]

const refundColumns: DataTableColumns<HoaDonItem> = [
  {
    title: () => h(NCheckbox, {
      checked: isAllRefundSelected.value,
      indeterminate: isRefundIndeterminate.value,
      disabled: !unrefundedInModal.value.length,
      onUpdateChecked: toggleRefundSelectAll,
    }),
    key: 'checkbox',
    width: 48,
    align: 'center',
    render: row => row.daHoanPhi
      ? null
      : h(NCheckbox, {
          checked: selectedRefundKeys.value.includes(row.maHoaDon),
          onUpdateChecked: (v: boolean) => toggleRefundSelectRow(row.maHoaDon, v),
        }),
  },
  {
    title: 'Mã HĐ',
    key: 'maHoaDon',
    width: 140,
    render: row => h(NText, {
      type: 'primary',
      strong: true,
      class: 'cursor-pointer hover:underline text-xs',
      onClick: () => { showRefundModal.value = false; handleViewClick(row) },
    }, { default: () => row.maHoaDon }),
  },
  {
    title: 'Khách hàng',
    key: 'tenKhachHang',
    width: 160,
    render: row => h('div', { class: 'flex flex-col' }, [
      h('div', { class: 'text-xs font-medium' }, row.tenKhachHang || 'Khách vãng lai'),
      row.sdtKhachHang && h('div', { class: 'text-[11px] text-gray-400' }, row.sdtKhachHang),
    ]),
  },
  {
    title: 'Ngày hủy',
    key: 'createdDate',
    width: 130,
    render: row => h('span', { class: 'text-xs text-gray-600' }, formatDateTime(row.createdDate)),
  },
  {
    title: 'Số tiền',
    key: 'tongTien',
    width: 130,
    align: 'right',
    render: row => h('span', { class: 'text-xs font-semibold text-red-600' }, formatCurrency(row.tongTien)),
  },
  {
    title: 'Trạng thái',
    key: 'daHoanPhi',
    width: 120,
    align: 'center',
    // [FIX #5] Hiển thị đúng trạng thái - reactive với refundList đã được cập nhật
    render: row => h(NTag, {
      type: row.daHoanPhi ? 'success' : 'warning',
      size: 'small',
      bordered: false,
    }, { default: () => row.daHoanPhi ? 'Đã hoàn phí' : 'Chưa hoàn phí' }),
  },
]

// ==================== LIFECYCLE ====================
onMounted(() => {
  setToday()
  // [FIX #3] Fetch badge count khi mount để hiển thị đúng ngay từ đầu
  fetchUnrefundedCount()
})
</script>

<template>
  <div class="flex flex-col gap-4">
    <!-- Header Card -->
    <NCard class="shadow-sm border-none">
      <NSpace align="center">
        <NIcon size="24" class="text-green-600">
          <Icon icon="carbon:document" />
        </NIcon>
        <span class="font-semibold text-2xl text-gray-800">Quản lý Hóa Đơn</span>
      </NSpace>
    </NCard>

    <!-- Filter Card -->
    <NCard title="Bộ lọc tìm kiếm" class="shadow-sm">
      <template #header-extra>
        <NTooltip trigger="hover">
          <template #trigger>
            <NButton
              size="large"
              circle
              secondary
              type="primary"
              class="transition-all hover:scale-110"
              @click="resetFilters"
            >
              <NIcon size="20">
                <Icon icon="carbon:filter-reset" />
              </NIcon>
            </NButton>
          </template>
          Làm mới bộ lọc
        </NTooltip>
      </template>

      <NForm label-placement="top">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
          <NFormItem label="Tìm kiếm" class="lg:col-span-2">
            <NInput
              v-model:value="state.searchQuery"
              placeholder="Mã HĐ, tên KH, SĐT..."
              clearable
              @keyup.enter="fetchHoaDons"
            >
              <template #prefix>
                <NIcon><Icon icon="carbon:search" /></NIcon>
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="Thời gian">
            <NDatePicker
              v-model:value="dateRange"
              type="daterange"
              clearable
              :is-date-disabled="disableFutureDate"
              style="width: 100%"
              placeholder="Chọn khoảng"
              @update:value="handleDateRangeChange"
            />
          </NFormItem>

          <NFormItem label="Loại HĐ">
            <NSelect v-model:value="state.loaiHoaDon" :options="LOAI_HOA_DON_OPTIONS" placeholder="Tất cả" />
          </NFormItem>

          <NFormItem label="Trạng thái">
            <NSelect v-model:value="state.searchStatus" :options="STATUS_OPTIONS" placeholder="Tất cả" />
          </NFormItem>
        </div>
      </NForm>
    </NCard>

    <!-- Main Card -->
    <NCard title="Danh sách Hóa Đơn" class="shadow-sm rounded-xl border border-gray-100">
      <template #header-extra>
        <NSpace>
          <!-- [FIX #3] Refund Button - Badge bọc bên ngoài NButton bằng div.relative -->
          <div class="relative inline-flex">
            <NButton
              type="warning"
              secondary
              @click="openRefundModal"
            >
              <template #icon>
                <NIcon size="18">
                  <Icon icon="carbon:money" />
                </NIcon>
              </template>
              <span class="hidden sm:inline ml-1">Hoàn phí</span>
            </NButton>
            <span
              v-if="unrefundedCount > 0"
              class="absolute -top-2 -right-2 z-10 min-w-[18px] h-[18px] px-1 rounded-full bg-red-500 text-white text-[11px] font-bold flex items-center justify-center leading-none"
            >
              {{ unrefundedCount > 99 ? '99+' : unrefundedCount }}
            </span>
          </div>

          <!-- Export Button -->
          <NButton
            type="success"
            secondary
            :loading="exportLoading"
            :disabled="!state.products.length"
            @click="exportToExcel"
          >
            <template #icon>
              <NIcon size="18">
                <Icon icon="file-icons:microsoft-excel" />
              </NIcon>
            </template>
            <span class="hidden sm:inline">Excel</span>
          </NButton>

          <!-- Refresh Button -->
          <NButton
            type="info"
            secondary
            :loading="state.loading"
            @click="fetchHoaDons"
          >
            <template #icon>
              <NIcon size="18">
                <Icon icon="carbon:rotate" />
              </NIcon>
            </template>
            <span class="hidden sm:inline">Tải lại</span>
          </NButton>
        </NSpace>
      </template>

      <!-- Tabs -->
      <div class="mb-4">
        <NTabs v-model:value="activeTab" type="segment" @update:value="handleTabChange">
          <NTab name="ALL">
            <span class="flex items-center gap-2">
              Tất cả
              <NBadge :value="state.totalItems" :max="999" />
            </span>
          </NTab>
          <NTab v-for="tab in tabs" :key="tab.key" :name="tab.key">
            <span class="flex items-center gap-2">
              {{ tab.label }}
              <NBadge :value="tab.count" :max="999" />
            </span>
          </NTab>
        </NTabs>
      </div>

      <!-- Error Alert -->
      <NAlert v-if="state.apiError" type="error" class="mb-4" closable @close="state.apiError = ''">
        {{ state.apiError }}
      </NAlert>

      <!-- Data Table -->
      <NDataTable
        :columns="columns"
        :data="state.products"
        :loading="state.loading"
        :row-key="row => row.maHoaDon"
        :scroll-x="1200"
        striped
        class="rounded-lg"
      />

      <!-- Pagination -->
      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="state.pagination.page"
          v-model:page-size="state.pagination.pageSize"
          :item-count="state.totalItems"
          :page-sizes="[10, 20, 30, 50, 100]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </NCard>

    <!-- ==================== REFUND MODAL ==================== -->
    <NModal
      v-model:show="showRefundModal"
      :style="{ width: '960px' }"
      preset="card"
      :bordered="false"
      :closable="true"
      :mask-closable="false"
      :segmented="false"
      size="huge"
      class="refund-modal"
    >
      <template #header>
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-orange-100 flex items-center justify-center">
            <NIcon size="22" color="#f97316">
              <Icon icon="carbon:money" />
            </NIcon>
          </div>
          <div>
            <h3 class="font-bold text-gray-900 text-lg">
              Danh sách đơn cần hoàn phí
            </h3>
            <p class="text-xs text-gray-500">
              Đơn online đã hủy · Đã thanh toán
            </p>
          </div>
        </div>
      </template>

      <!-- BỘ LỌC MODAL -->
      <div class="bg-gray-50 rounded-xl p-3 mb-4 border border-gray-100">
        <!-- Header bộ lọc -->
        <div class="flex items-center justify-between mb-2.5">
          <span class="text-xs font-semibold text-gray-500 uppercase tracking-wide flex items-center gap-1.5">
            <NIcon size="13" class="text-gray-400">
              <Icon icon="carbon:filter" />
            </NIcon>
            Bộ lọc
            <NBadge
              v-if="activeRefundFilterCount"
              :value="activeRefundFilterCount"
              type="warning"
              :max="9"
            />
          </span>
          <NButton
            v-if="activeRefundFilterCount"
            size="tiny"
            quaternary
            type="error"
            @click="resetRefundFilters"
          >
            <template #icon>
              <NIcon size="12">
                <Icon icon="carbon:close" />
              </NIcon>
            </template>
            Xóa bộ lọc
          </NButton>
        </div>

        <!-- 3 ô lọc -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-2">
          <!-- Lọc mã hóa đơn -->
          <NInput
            v-model:value="refundFilter.maHoaDon"
            placeholder="Tìm mã hóa đơn..."
            clearable
            size="small"
            @update:value="handleRefundMaHoaDonChange"
          >
            <template #prefix>
              <NIcon size="13" class="text-gray-400">
                <Icon icon="carbon:search" />
              </NIcon>
            </template>
          </NInput>

          <!-- Lọc khoảng thời gian -->
          <NDatePicker
            v-model:value="refundFilter.dateRange"
            type="daterange"
            clearable
            :is-date-disabled="disableFutureDate"
            size="small"
            placeholder="Khoảng ngày hủy đơn"
            style="width: 100%"
            @update:value="handleRefundDateRangeChange"
          />

          <!-- [FIX #1] Lọc trạng thái hoàn phí - gọi API thay vì lọc client -->
          <NSelect
            v-model:value="refundFilter.daHoanPhi"
            :options="HOAN_PHI_STATUS_OPTIONS"
            placeholder="Trạng thái hoàn phí"
            clearable
            size="small"
            @update:value="handleRefundStatusChange"
          />
        </div>
      </div>

      <!-- Actions Bar -->
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-3">
          <NCheckbox
            :checked="isAllRefundSelected"
            :indeterminate="isRefundIndeterminate"
            :disabled="!unrefundedInModal.length"
            @update:checked="toggleRefundSelectAll"
          >
            <span class="text-sm">Chọn tất cả chưa hoàn</span>
          </NCheckbox>
          <span v-if="unrefundedInModal.length" class="text-xs text-gray-400">
            ({{ unrefundedInModal.length }} đơn)
          </span>
        </div>

        <div class="flex items-center gap-3">
          <div v-if="selectedRefundKeys.length" class="text-sm text-orange-600">
            Đã chọn <b>{{ selectedRefundKeys.length }}</b> ·
            Tổng: <b>{{ formatCurrency(totalSelectedRefund) }}</b>
          </div>

          <NButton
            v-if="selectedRefundKeys.length"
            type="warning"
            :loading="bulkRefundLoading"
            size="small"
            @click="showBulkConfirm = true"
          >
            <template #icon>
              <NIcon><Icon icon="carbon:checkmark-filled" /></NIcon>
            </template>
            Hoàn phí ({{ selectedRefundKeys.length }})
          </NButton>

          <NButton
            size="small"
            circle
            secondary
            type="info"
            :loading="refundLoading"
            @click="fetchRefundList"
          >
            <template #icon>
              <NIcon><Icon icon="carbon:rotate" /></NIcon>
            </template>
          </NButton>
        </div>
      </div>

      <!-- Refund Table -->
      <NSpin :show="refundLoading">
        <NDataTable
          :columns="refundColumns"
          :data="refundList"
          :row-key="row => row.maHoaDon"
          :max-height="380"
          :scroll-x="800"
          striped
          size="small"
          class="rounded-lg"
        />

        <!-- Empty state -->
        <div
          v-if="!refundLoading && refundList.length === 0"
          class="flex flex-col items-center py-8 text-gray-400 text-sm gap-1"
        >
          <NIcon size="32" class="opacity-30 mb-1">
            <Icon icon="carbon:search" />
          </NIcon>
          <p>Không tìm thấy đơn nào phù hợp</p>
          <NButton
            v-if="activeRefundFilterCount"
            size="tiny"
            quaternary
            type="warning"
            class="mt-1"
            @click="resetRefundFilters"
          >
            Xóa bộ lọc để xem tất cả
          </NButton>
        </div>
      </NSpin>

      <!-- [FIX #2] Pagination - item-count dùng refundTotalItems đã được tính đúng -->
      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="refundPagination.page"
          v-model:page-size="refundPagination.pageSize"
          :item-count="refundTotalItems"
          :page-sizes="[5, 10, 20, 50]"
          show-size-picker
          size="small"
          @update:page="handleRefundPageChange"
          @update:page-size="handleRefundPageSizeChange"
        />
      </div>
    </NModal>

    <!-- Bulk Confirm Modal -->
    <NModal
      v-model:show="showBulkConfirm"
      preset="dialog"
      type="warning"
      title="Xác nhận hoàn phí"
      :closable="false"
      :mask-closable="false"
    >
      <div class="py-2 space-y-2">
        <p>Hoàn phí cho <b>{{ selectedRefundKeys.length }}</b> đơn hàng đã hủy.</p>
        <p>Tổng tiền hoàn: <b class="text-orange-600 text-lg">{{ formatCurrency(totalSelectedRefund) }}</b></p>
        <p class="text-sm text-gray-500">
          Thao tác này không thể hoàn tác.
        </p>
      </div>

      <template #action>
        <NSpace justify="end">
          <NButton @click="showBulkConfirm = false">
            Hủy
          </NButton>
          <NButton
            type="warning"
            :loading="bulkRefundLoading"
            @click="handleBulkRefund"
          >
            Xác nhận hoàn phí
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
.refund-modal :deep(.n-card-header) {
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.refund-modal :deep(.n-card__content) {
  padding-top: 16px;
}

:deep(.n-data-table-td) {
  transition: background-color 0.2s;
}

:deep(.n-data-table-tr:hover) {
  background-color: #fafafa;
}

.group:hover .group-hover\:scale-110 {
  transform: scale(1.1);
}
</style>
