<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import type { DataTableColumns, SelectOption } from 'naive-ui'
import {
  createDiscreteApi,
  NButton,
  NDropdown,
  NIcon,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSpace,
  NSwitch,
} from 'naive-ui'
import type { Customer } from '@/service/api/admin/users/customer/customer'
import type { Address } from '@/service/api/admin/users/customer/address'
import { Icon } from '@iconify/vue'
import {
  getCustomers,
  updateCustomer,
} from '@/service/api/admin/users/customer/customer'
import {
  getAddressesByCustomer,
} from '@/service/api/admin/users/customer/address'
import { useRouter } from 'vue-router'
import { getDistrictName, getProvinceName, getWardName, initLocationData } from '@/service/api/admin/users/customer/location-service'
import * as XLSX from 'xlsx'

// =============================================================================
// API SETUP
// =============================================================================
const { message, dialog, notification } = createDiscreteApi(['message', 'dialog', 'notification'])
const router = useRouter()

// =============================================================================
// CONSTANTS
// =============================================================================
const STATUS_OPTIONS = { ALL: -1, ACTIVE: 1, INACTIVE: 0 } as const
const SEARCH_DEBOUNCE_DELAY = 500
const DEFAULT_PAGE_SIZE = 10
const BATCH_SIZE = 50

const GENDER_FILTER_OPTIONS: SelectOption[] = [
  { label: 'Tất cả', value: null },
  { label: 'Nam', value: 1 },
  { label: 'Nữ', value: 0 },
]

const STATUS_FILTER_OPTIONS: SelectOption[] = [
  { label: 'Tất cả', value: null },
  { label: 'Hoạt động', value: STATUS_OPTIONS.ACTIVE },
  { label: 'Ngưng', value: STATUS_OPTIONS.INACTIVE },
]

// =============================================================================
// STATE
// =============================================================================
const customers = ref<Customer[]>([])
const customerAddresses = ref<Record<string, Address[]>>({})
const currentPage = ref<number>(1)
const pageSize = ref<number>(DEFAULT_PAGE_SIZE)
const totalElements = ref<number>(0)
const totalPages = ref<number>(1)
const loading = ref(false)

const formSearch = reactive({
  customerName: '',
  customerGender: null as number | null,
  customerStatus: null as number | null,
})

let searchTimer: NodeJS.Timeout | null = null

const pageCount = computed(() => Math.ceil(totalElements.value / pageSize.value))

// =============================================================================
// [CHỨC NĂNG 1] IN ẤN (POPUP PRINT)
// =============================================================================

function handlePrint() {
  if (customers.value.length === 0)
    return message.warning('Không có dữ liệu để in!')
  printDataToWindow(customers.value)
}

function printDataToWindow(dataList: Customer[]) {
  let rowsHtml = ''
  dataList.forEach((item, index) => {
    let addressStr = ''
    if (item.id && customerAddresses.value[item.id]) {
      const addrs = customerAddresses.value[item.id]
      const defaultAddr = addrs.find(a => a.isDefault) || addrs[0]
      addressStr = defaultAddr ? resolveAddress(defaultAddr) : ''
    }
    const genderStr = item.customerGender === null ? '-' : (item.customerGender ? 'Nam' : 'Nữ')
    const statusStr = item.customerStatus === STATUS_OPTIONS.ACTIVE ? 'Hoạt động' : 'Ngưng'

    rowsHtml += `
      <tr>
        <td style="text-align: center">${index + 1}</td>
        <td>${item.customerCode || ''}</td>
        <td>${item.customerName}</td>
        <td style="text-align: center">${genderStr}</td>
        <td>${item.customerPhone}</td>
        <td>${item.customerEmail || ''}</td>
        <td>${addressStr}</td>
        <td style="text-align: center">${statusStr}</td>
      </tr>
    `
  })

  const printWin = window.open('', '_blank', 'width=1200,height=800')
  if (!printWin)
    return message.error('Vui lòng cho phép trình duyệt mở Popup!')

  printWin.document.write(`
    <!DOCTYPE html>
    <html>
      <head>
        <title>In Danh Sách</title>
        <style>
          body { font-family: 'Times New Roman', serif; padding: 20px; font-size: 13px; }
          h2 { text-align: center; text-transform: uppercase; margin-bottom: 5px; }
          .meta { text-align: center; font-style: italic; margin-bottom: 20px; font-size: 12px; }
          table { width: 100%; border-collapse: collapse; }
          th, td { border: 1px solid #000; padding: 6px 8px; text-align: left; vertical-align: top; }
          th { background-color: #f0f0f0; text-align: center; font-weight: bold; }
          .footer { margin-top: 20px; text-align: right; font-style: italic; font-size: 11px; }
          th:first-child, td:first-child { width: 40px; text-align: center; } 
        </style>
      </head>
      <body>
        <h2>Danh Sách Khách Hàng</h2>
        <div class="meta">Trang: ${currentPage.value} | Ngày in: ${new Date().toLocaleDateString('vi-VN')}</div>
        <table>
          <thead>
            <tr>
              <th>STT</th> <th>Mã KH</th> <th>Họ tên</th> <th>Giới tính</th>
              <th>SĐT</th> <th>Email</th> <th>Địa chỉ</th> <th>Trạng thái</th>
            </tr>
          </thead>
          <tbody>${rowsHtml}</tbody>
        </table>
        <div class="footer">Người xuất: Quản trị viên</div>
      </body>
    </html>
  `)
  printWin.document.close()
  printWin.focus()
  setTimeout(() => { printWin.print(); printWin.close() }, 500)
}

// =============================================================================
// [CHỨC NĂNG 2] EXCEL EXPORT
// =============================================================================

const exportOptions = [
  { label: `Xuất trang hiện tại`, key: 'current' },
  { label: 'Xuất theo bộ lọ', key: 'filtered' },
  { label: 'Xuất TOÀN BỘ DỮ LIỆU', key: 'all', props: { style: { color: '#d03050', fontWeight: 'bold' } } },
]

function handleSelectExport(key: string) {
  switch (key) {
    case 'current': exportCurrentPage(); break
    case 'filtered': exportFilteredData(); break
    case 'all': confirmExportAll(); break
  }
}

function exportCurrentPage() {
  if (customers.value.length === 0)
    return message.warning('Không có dữ liệu!')
  generateExcelFile(customers.value, `Page_${currentPage.value}`)
}

async function exportFilteredData() {
  if (totalElements.value === 0)
    return message.warning('Không có dữ liệu!')
  const loadingMsg = message.loading(`Đang chuẩn bị dữ liệu...`, { duration: 0 })
  try {
    const params = { ...buildSearchParams(), page: 1, size: totalElements.value }
    const response = await getCustomers(params)
    const dataList = response?.data?.data?.data || []
    await processBatchesWithProgress(dataList, loadingMsg)
    generateExcelFile(dataList, `Export_BoLoc_${new Date().getTime()}`)
  }
  catch (e) { message.error('Lỗi khi xuất dữ liệu') }
  finally { loadingMsg.destroy() }
}

function confirmExportAll() {
  dialog.warning({
    title: 'Xác nhận xuất toàn bộ',
    content: `Hệ thống sẽ tải về toàn bộ cơ sở dữ liệu khách hàng. Việc này có thể mất thời gian.`,
    positiveText: 'Bắt đầu tải',
    onPositiveClick: executeExportAll,
  })
}

// [SỬA LỖI] HÀM XUẤT TOÀN BỘ
async function executeExportAll() {
  const loadingMsg = message.loading('Đang tải danh sách tổng...', { duration: 0 })
  try {
    // 1. Tạo params sạch, ép size lớn và reset bộ lọc
    const params = {
      page: 1,
      size: 999999, // Lấy số lượng cực lớn để đảm bảo hết DB
      // Quan trọng: Gửi null/rỗng đè lên formSearch để bỏ qua bộ lọc
      customerName: null,
      customerGender: null,
      customerStatus: null,
    }

    const response = await getCustomers(params)
    const dataList = response?.data?.data?.data || []

    if (!dataList || dataList.length === 0) {
      message.warning('Database trống!')
      return
    }

    // 2. Tải địa chỉ
    await processBatchesWithProgress(dataList, loadingMsg)

    // 3. Xuất file
    generateExcelFile(dataList, `FULL_DB_${new Date().toISOString().slice(0, 10)}`)
  }
  catch (e) {
    message.error('Lỗi khi tải dữ liệu toàn bộ')
    console.error(e)
  }
  finally {
    loadingMsg.destroy()
  }
}

async function processBatchesWithProgress(allData: Customer[], loadingMsg: any) {
  const total = allData.length
  let processed = 0
  for (let i = 0; i < total; i += BATCH_SIZE) {
    const chunk = allData.slice(i, i + BATCH_SIZE)
    await fetchAddressesForBatch(chunk)
    processed += chunk.length
    if (loadingMsg)
      loadingMsg.content = `Đang tải địa chỉ: ${processed}/${total} khách hàng...`
    await new Promise(r => setTimeout(r, 10))
  }
}

async function fetchAddressesForBatch(list: Customer[]) {
  const promises = list.map(async (c) => {
    if (!c.id)
      return
    try {
      if (!customerAddresses.value[c.id]) {
        const res = await getAddressesByCustomer(c.id)
        customerAddresses.value[c.id] = res.data.data || []
      }
    }
    catch (e) { /* ignore */ }
  })
  await Promise.all(promises)
}

function generateExcelFile(dataList: Customer[], fileName: string) {
  const excelData = dataList.map((item, index) => {
    let addressStr = ''
    if (item.id && customerAddresses.value[item.id]) {
      const addrs = customerAddresses.value[item.id]
      const defaultAddr = addrs.find(a => a.isDefault) || addrs[0]
      addressStr = defaultAddr ? resolveAddress(defaultAddr) : ''
    }
    else { addressStr = '(Chưa có)' }
    return {
      'STT': index + 1,
      'Mã KH': item.customerCode,
      'Họ tên': item.customerName,
      'Giới tính': item.customerGender ? 'Nam' : 'Nữ',
      'Ngày sinh': formatDate(item.customerBirthday),
      'SĐT': item.customerPhone,
      'Email': item.customerEmail,
      'Địa chỉ': addressStr,
      'Trạng thái': item.customerStatus === STATUS_OPTIONS.ACTIVE ? 'Hoạt động' : 'Ngưng',
      'Ngày tạo': formatDate(item.customerCreatedDate),
    }
  })
  const ws = XLSX.utils.json_to_sheet(excelData)
  ws['!cols'] = [{ wch: 5 }, { wch: 12 }, { wch: 20 }, { wch: 8 }, { wch: 12 }, { wch: 15 }, { wch: 25 }, { wch: 40 }, { wch: 12 }, { wch: 12 }]
  const wb = XLSX.utils.book_new(); XLSX.utils.book_append_sheet(wb, ws, 'Data'); XLSX.writeFile(wb, `${fileName}.xlsx`)
  message.success(`Đã xuất ${dataList.length} dòng!`)
}

// =============================================================================
// TABLE RENDERS
// =============================================================================

const getRowKey = (row: Customer) => row.id as string | number
const formatDate = (t?: number) => !t ? '-' : new Date(t).toLocaleDateString('vi-VN')
const DEFAULT_AVATAR = 'https://www.svgrepo.com/show/452030/avatar-default.svg'

function createCustomerInfoCell(row: Customer) {
  return h('div', { class: 'flex items-center' }, [
    h('img', {
      src: row.customerAvatar || DEFAULT_AVATAR,
      class: 'flex-shrink-0 h-10 w-10 rounded-md object-cover mr-3',
      onError: (e: any) => { e.target.src = DEFAULT_AVATAR },
    }),
    h('div', [
      h('div', { class: 'font-medium text-gray-800' }, row.customerName),
      h('div', { class: 'text-sm text-gray-500' }, `${row.customerCode || ''} • ${row.customerGender ? 'Nam' : 'Nữ'}`),
      h('div', { class: 'text-xs text-gray-400' }, `NS: ${formatDate(row.customerBirthday)}`),
    ]),
  ])
}
const createEmailCell = (row: Customer) => h('div', { class: 'text-sm' }, row.customerEmail || '-')
const createPhoneCell = (row: Customer) => h('div', { class: 'text-sm' }, row.customerPhone || '-')
function createAddressCell(row: Customer) {
  const addrs = customerAddresses.value[row.id as string] || []
  const def = addrs.find(a => a.isDefault) || addrs[0]
  if (!def)
    return h('div', { class: 'text-sm text-gray-500' }, 'Chưa có địa chỉ')
  const extra = addrs.length - 1
  return h('div', [h('div', { class: 'text-xs text-sm' }, resolveAddress(def)), extra > 0 ? h('div', { class: 'text-xs text-gray-400 italic' }, `+${extra} địa chỉ khác`) : null])
}
const createDateCell = (row: Customer) => h('div', { class: 'text-xs' }, formatDate(row.customerCreatedDate))

function createStatusCell(row: Customer) {
  return h(NPopconfirm, {
    onPositiveClick: () => toggleStatus(row),
    positiveText: 'Đồng ý',
    negativeText: 'Quay lại',
  }, {
    trigger: () => h(NSwitch, { value: row.customerStatus === STATUS_OPTIONS.ACTIVE, disabled: loading.value }, { checked: () => 'Hoạt động', unchecked: () => 'Ngưng' }),
    default: () => 'Đổi trạng thái?',
  })
}

const createActionsCell = (row: Customer) => h(NSpace, { justify: 'center', size: 12 }, { default: () => [h(NIcon, { size: 18, class: 'cursor-pointer text-blue-600', onClick: () => navigateToEdit(row.id as string) }, { default: () => h(Icon, { icon: 'mdi:pencil' }) })] })

const columns: DataTableColumns<Customer> = [
  { title: 'STT', key: 'stt', width: 50, align: 'center', render: (_, i) => (currentPage.value - 1) * pageSize.value + i + 1 },
  { title: 'Thông tin', key: 'info', width: 150, render: createCustomerInfoCell },
  { title: 'Email', key: 'email', width: 150, render: createEmailCell },
  { title: 'SĐT', key: 'phone', width: 90, align: 'center', render: createPhoneCell },
  { title: 'Địa chỉ', key: 'address', width: 200, render: createAddressCell },
  { title: 'Ngày tạo', key: 'date', width: 120, align: 'center', render: createDateCell },
  { title: 'Trạng thái', key: 'status', width: 100, align: 'center', render: createStatusCell },
  { title: 'Sửa', key: 'action', width: 60, align: 'center', fixed: 'right', render: createActionsCell },
]

watch(() => formSearch, () => handleAutoFilter(), { deep: true })

function buildSearchParams() {
  const page = Math.max(1, Number(currentPage.value) || 1)
  const size = Math.max(1, Number(pageSize.value) || DEFAULT_PAGE_SIZE)
  return {
    page,
    size,
    ...(formSearch.customerName && { customerName: formSearch.customerName }),
    ...(formSearch.customerGender !== null && { customerGender: formSearch.customerGender }),
    ...(formSearch.customerStatus !== null && { customerStatus: formSearch.customerStatus }),
  }
}

async function loadCustomers() {
  try {
    loading.value = true
    const res = await getCustomers(buildSearchParams())
    const data = res?.data?.data
    if (!data)
      return
    customers.value = data.data || []
    totalElements.value = data.totalElements || 0
    pageSize.value = Number(data.size || pageSize.value)
    totalPages.value = Number(data.totalPages || 1)
    await loadCustomerAddresses()
  }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}
async function loadCustomerAddresses() {
  const promises = customers.value.map(async (c) => {
    if (!c.id)
      return
    try { const res = await getAddressesByCustomer(c.id); customerAddresses.value[c.id] = res.data.data || [] }
    catch {}
  })
  await Promise.all(promises)
}
function navigateToAdd() { router.push('/users/customer/add') }
function navigateToEdit(id: string) { router.push(`/users/customer/edit/${id}`) }
function handleAutoFilter() {
  if (searchTimer)
    clearTimeout(searchTimer); searchTimer = setTimeout(() => { currentPage.value = 1; loadCustomers() }, SEARCH_DEBOUNCE_DELAY)
}
function refreshTable() {
  if (searchTimer)
    clearTimeout(searchTimer)
  Object.assign(formSearch, { customerName: '', customerGender: null, customerStatus: null })
  currentPage.value = 1
  loadCustomers()
}
function handlePageChange(p: number) { currentPage.value = p; loadCustomers() }
async function toggleStatus(c: Customer) {
  try { await updateCustomer(c.id!, { ...c, customerStatus: c.customerStatus === 1 ? 0 : 1 }); message.success('Đã đổi trạng thái'); loadCustomers() }
  catch { message.error('Lỗi đổi trạng thái') }
}
function resolveAddress(addr: Address): string {
  if (!addr)
    return ''
  const p = getProvinceName(addr.provinceCity as any); const d = getDistrictName(addr.district as any); const w = getWardName(addr.wardCommune as any)
  return [addr.addressDetail, w, d, p].filter(Boolean).join(', ')
}
onMounted(async () => { await initLocationData(); await loadCustomers() })
</script>

<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-4">
      <NSpace align="center">
        <NIcon size="24" class="text-green-600">
          <Icon icon="carbon:events" />
        </NIcon>
        <span class="font-bold text-xl text-gray-800">Quản lý Khách hàng</span>
      </NSpace>
    </div>

    <div class="bg-white p-4 rounded-lg shadow-sm mb-4 border border-gray-100">
      <div class="flex flex-col md:flex-row justify-between items-center gap-4">
        <div class="flex items-center gap-2">
          <NButton text type="primary" class="font-medium hover:underline" @click="refreshTable">
            <template #icon>
              <NIcon><Icon icon="mdi:filter-off" /></NIcon>
            </template>
            Xóa bộ lọc
          </NButton>
          <div class="h-4 w-px bg-gray-300 mx-2 hidden md:block" />
        </div>

        <div class="flex flex-wrap items-center gap-3 w-full md:w-auto">
          <div class="w-full md:w-40">
            <NSelect v-model:value="formSearch.customerGender" :options="GENDER_FILTER_OPTIONS" placeholder="Giới tính" size="medium" clearable @update:value="handleAutoFilter" />
          </div>
          <div class="w-full md:w-48">
            <NSelect v-model:value="formSearch.customerStatus" :options="STATUS_FILTER_OPTIONS" placeholder="Trạng thái" size="medium" clearable @update:value="handleAutoFilter" />
          </div>
          <div class="w-full md:w-64">
            <NInput v-model:value="formSearch.customerName" placeholder="Tìm kiếm người dùng..." size="medium" clearable @input="handleAutoFilter">
              <template #prefix>
                <NIcon class="text-gray-400">
                  <Icon icon="mdi:magnify" />
                </NIcon>
              </template>
            </NInput>
          </div>
        </div>
      </div>
    </div>

    <NCard content-style="padding: 0;">
      <div class="p-4 border-b flex justify-between items-center bg-gray-50 rounded-t-lg">
        <div class="font-bold text-base text-gray-700">
          Danh sách khách hàng
        </div>
        <NSpace>
          <NDropdown trigger="click" :options="exportOptions" @select="handleSelectExport">
            <NButton class="no-print" size="medium">
              <template #icon>
                <NIcon><Icon icon="mdi:file-excel" /></NIcon>
              </template>
              Xuất Excel
              <NIcon class="ml-1">
                <Icon icon="mdi:chevron-down" />
              </NIcon>
            </NButton>
          </NDropdown>
          <NButton class="no-print" size="medium" @click="handlePrint">
            <template #icon>
              <NIcon><Icon icon="mdi:printer" /></NIcon>
            </template>
            In danh sách
          </NButton>
          <NButton type="primary" size="medium" @click="navigateToAdd">
            <template #icon>
              <NIcon><Icon icon="icon-park-outline:add-one" /></NIcon>
            </template>
            Thêm khách hàng
          </NButton>
        </NSpace>
      </div>

      <NDataTable
        :row-key="getRowKey" :columns="columns" :data="customers" :loading="loading"
        size="small" :scroll-x="1000" :pagination="false" :bordered="false" class="custom-table"
      />

      <div class="p-4 flex justify-center border-t no-print">
        <NPagination :page="currentPage" :page-size="pageSize" :page-count="pageCount" @update:page="handlePageChange" />
      </div>
    </NCard>
  </div>
</template>

<style scoped>
:deep(.n-data-table-th__title-wrapper) { justify-content: center !important; }
:deep(.n-data-table-th) { text-align: center !important; background-color: #fafafc !important; font-weight: 600; }
:deep(.n-data-table .n-data-table-td) { padding-top: 4px !important; padding-bottom: 4px !important; height: 40px !important; }
:deep(.n-data-table .n-button) { height: 24px !important; font-size: 12px !important; padding: 0 6px !important; }
:deep(.n-data-table img) { height: 32px !important; width: 32px !important; }
:deep(.n-data-table) { font-size: 13px !important; }
</style>
