<script setup lang="tsx">
import { h, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  createDiscreteApi,
  NAvatar,
  NButton,
  NCard,
  NDataTable,
  NDropdown,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
  NTag,
  NTooltip,
  useDialog,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import * as XLSX from 'xlsx'
import { debounce } from 'lodash'

// API & Types
import type { Customer } from '@/service/api/admin/users/customer/customer'
import type { Address } from '@/service/api/admin/users/customer/address'
import { getCustomers, updateCustomer } from '@/service/api/admin/users/customer/customer'
import { getAddressesByCustomer } from '@/service/api/admin/users/customer/address'
import { getDistrictName, getProvinceName, getWardName, initLocationData } from '@/service/api/admin/users/location-service'

// ================= CONSTANTS & STATE =================
const message = useMessage()
const dialog = useDialog() // Init Dialog
const router = useRouter()

const customers = ref<Customer[]>([])
const customerAddresses = ref<Record<string, Address[]>>({})
const loading = ref(false)

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)
const totalElements = ref(0)

// Filter State
const filter = reactive({
  customerName: '',
  customerGender: null as number | null,
  customerStatus: null as number | null,
})

// Options cho Dropdown Excel
const exportOptions = [
  { label: 'Xuất trang hiện tại', key: 'current' },
  { label: 'Xuất theo bộ lọc (Tất cả)', key: 'filtered' },
  { label: 'Xuất TOÀN BỘ dữ liệu', key: 'all', props: { style: { color: 'red', fontWeight: 'bold' } } },
]

// ================= HELPERS =================
function resolveAddress(addr: Address): string {
  if (!addr)
    return 'Chưa cập nhật'
  const p = getProvinceName(addr.provinceCity as any)
  const d = getDistrictName(addr.district as any)
  const w = getWardName(addr.wardCommune as any)
  return [addr.addressDetail, w, d, p].filter(Boolean).join(', ')
}

// ================= API CALLS =================
async function fetchCustomers() {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: filter.customerName || undefined,
      customerGender: filter.customerGender,
      customerStatus: filter.customerStatus,
    }
    const res = await getCustomers(params)
    const data = res?.data?.data
    if (data) {
      customers.value = data.data || []
      totalElements.value = data.totalElements || 0
      await loadCustomerAddresses(customers.value)
    }
  }
  catch (e) {
    message.error('Lỗi tải danh sách khách hàng')
  }
  finally {
    loading.value = false
  }
}

async function loadCustomerAddresses(list: Customer[]) {
  const promises = list.map(async (c) => {
    if (!c.id)
      return
    try {
      if (!customerAddresses.value[c.id]) {
        const res = await getAddressesByCustomer(c.id)
        customerAddresses.value[c.id] = res.data.data || []
      }
    }
    catch { }
  })
  await Promise.all(promises)
}

// ================= ACTIONS =================
const debouncedFetch = debounce(() => {
  currentPage.value = 1
  fetchCustomers()
}, 500)

function handlePageChange(page: number) {
  currentPage.value = page
  fetchCustomers()
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchCustomers()
}

function resetFilters() {
  filter.customerName = ''
  filter.customerGender = null
  filter.customerStatus = null
  currentPage.value = 1
  fetchCustomers()
}

function navigateToAdd() {
  router.push('/users/customer/add')
}

function navigateToEdit(row: Customer) {
  router.push(`/users/customer/edit/${row.id}`)
}

async function handleStatusChange(row: Customer) {
  if (!row.id)
    return
  try {
    loading.value = true
    const newStatus = row.customerStatus === 1 ? 0 : 1
    await updateCustomer(row.id, { ...row, customerStatus: newStatus })
    message.success('Cập nhật trạng thái thành công')
    row.customerStatus = newStatus
  }
  catch {
    message.error('Cập nhật thất bại')
  }
  finally {
    loading.value = false
  }
}

// Watch filters
watch(
  () => [filter.customerGender, filter.customerStatus],
  () => {
    currentPage.value = 1
    fetchCustomers()
  },
)

// ================= EXCEL LOGIC =================
function handleSelectExport(key: string) {
  if (key === 'current') {
    if (customers.value.length === 0)
      return message.warning('Không có dữ liệu trang này!')
    exportToExcel(customers.value, `DS_KhachHang_Trang_${currentPage.value}`)
  }
  else if (key === 'filtered' || key === 'all') {
    const confirmMsg = key === 'all'
      ? 'Bạn có chắc muốn tải TOÀN BỘ dữ liệu? Việc này có thể mất thời gian.'
      : 'Tải toàn bộ kết quả theo bộ lọc hiện tại?'

    dialog.info({
      title: 'Xác nhận xuất dữ liệu',
      content: confirmMsg,
      positiveText: 'Đồng ý',
      negativeText: 'Hủy',
      onPositiveClick: () => processExportAll(key === 'all'),
    })
  }
}

async function processExportAll(isAll: boolean) {
  const loadingMsg = message.loading('Đang tải dữ liệu...', { duration: 0 })
  try {
    const params: any = {
      page: 1,
      size: 10000,
    }

    if (!isAll) {
      params.customerName = filter.customerName || undefined
      params.customerGender = filter.customerGender
      params.customerStatus = filter.customerStatus
    }

    const res = await getCustomers(params)
    const dataList = res?.data?.data?.data || []

    if (dataList.length === 0) {
      message.warning('Không tìm thấy dữ liệu nào!')
      return
    }

    loadingMsg.content = `Đang xử lý ${dataList.length} dòng dữ liệu...`
    await loadCustomerAddresses(dataList)

    exportToExcel(dataList, isAll ? 'DS_ToanBo_KhachHang' : 'DS_KhachHang_TheoLoc')
  }
  catch (e) {
    message.error('Lỗi khi xuất dữ liệu')
  }
  finally {
    loadingMsg.destroy()
  }
}

function exportToExcel(dataList: Customer[], fileName: string) {
  const excelData = dataList.map((item, index) => {
    let addressStr = ''
    if (item.id && customerAddresses.value[item.id]) {
      const addrs = customerAddresses.value[item.id]
      const defaultAddr = addrs.find(a => a.isDefault) || addrs[0]
      addressStr = defaultAddr ? resolveAddress(defaultAddr) : ''
    }
    return {
      'STT': index + 1,
      'Mã KH': item.customerCode,
      'Họ tên': item.customerName,
      'Giới tính': item.customerGender ? 'Nam' : 'Nữ',
      'SĐT': item.customerPhone,
      'Email': item.customerEmail,
      'Địa chỉ': addressStr,
      'Trạng thái': item.customerStatus === 1 ? 'Hoạt động' : 'Ngưng',
    }
  })

  const ws = XLSX.utils.json_to_sheet(excelData)
  ws['!cols'] = [{ wch: 5 }, { wch: 15 }, { wch: 25 }, { wch: 10 }, { wch: 15 }, { wch: 25 }, { wch: 40 }, { wch: 15 }]

  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Data')
  XLSX.writeFile(wb, `${fileName}_${new Date().getTime()}.xlsx`)
  message.success('Xuất file thành công!')
}

function handlePrint() {
  if (customers.value.length === 0)
    return message.warning('Không có dữ liệu để in!')
  window.print()
}

onMounted(async () => {
  await initLocationData()
  fetchCustomers()
})

// ================= TABLE COLUMNS =================
const columns = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    fixed: 'left',
    render: (_, index) => (currentPage.value - 1) * pageSize.value + index + 1,
  },
  {
    title: 'Khách hàng',
    key: 'info',
    width: 200,
    fixed: 'left',
    render: (row: Customer) => h(
      NSpace,
      { align: 'center', justify: 'start' },
      {
        default: () => [
          h(NAvatar, {
            size: 'medium',
            round: true,
            src: row.customerAvatar || 'https://via.placeholder.com/150',
            fallbackSrc: 'https://via.placeholder.com/150',
          }),
          h('div', [
            h('div', { style: 'font-weight: 600' }, row.customerName),
            h('div', { style: 'font-size: 12px; color: #888' }, row.customerCode),
          ]),
        ],
      },
    ),
  },
  {
    title: 'Giới tính',
    key: 'gender',
    width: 90,
    align: 'center',
    render: (row: Customer) => h(NTag, {
      type: row.customerGender ? 'success' : 'error',
      size: 'small',
      bordered: false,
    }, { default: () => (row.customerGender ? 'Nam' : 'Nữ') }),
  },
  {
    title: 'Ngày sinh',
    key: 'customerBirthday',
    width: 120,
    align: 'center',
    render: (row: Customer) => {
      if (!row.customerBirthday)
        return h('span', { class: 'text-gray-400 italic' }, 'Chưa cập nhật')
      const date = new Date(row.customerBirthday)
      if (isNaN(date.getTime()))
        return h('span', { class: 'text-gray-400 italic' }, 'Lỗi ngày tháng')

      const formatted = new Intl.DateTimeFormat('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      }).format(date)

      return h('span', { class: 'text-sm' }, formatted)
    },
  },
  {
    title: 'Liên hệ',
    key: 'contact',
    mminWidth: 200,
    render: (row: Customer) => h('div', [
      h('div', { class: 'flex items-center gap-1 mb-1' }, [
        h(Icon, { icon: 'carbon:phone', class: 'text-gray-400' }),
        h('span', { class: 'text-sm' }, row.customerPhone || '---'),
      ]),
      h('div', { class: 'flex items-center gap-1' }, [
        h(Icon, { icon: 'carbon:email', class: 'text-gray-400' }),
        h('span', { class: 'text-sm' }, row.customerEmail || '---'),
      ]),
    ]),
  },
  {
    title: 'Địa chỉ nhận hàng',
    key: 'address',
    minWidth: 250,
    ellipsis: { tooltip: true },
    render: (row: Customer) => {
      const addrs = customerAddresses.value[row.id as string] || []
      const def = addrs.find(a => a.isDefault) || addrs[0]

      if (!def)
        return h('span', { class: 'text-gray-400 italic text-xs' }, 'Chưa có địa chỉ')

      const fullAddress = resolveAddress(def)

      return h('div', { class: 'flex flex-col gap-1 py-1' }, [
        h(
          NTooltip,
          { trigger: 'hover', style: { maxWidth: '400px' } },
          {
            trigger: () => h('span', { class: 'text-xs text-gray-700 cursor-help text-left' }, fullAddress),
            default: () => fullAddress,
          },
        ),
        h(NTag, { size: 'tiny', bordered: false, type: 'info', class: 'w-fit text-[10px]' }, { default: () => `Tổng: ${addrs.length} địa chỉ` },
        ),
      ])
    },
  },
  {
    title: 'Trạng thái',
    key: 'status',
    width: 120,
    align: 'center',
    render: (row: Customer) => h(
      NPopconfirm,
      {
        onPositiveClick: () => handleStatusChange(row),
        positiveText: 'Đồng ý',
        negativeText: 'Hủy',
      },
      {
        trigger: () => h(NSwitch, {
          value: row.customerStatus === 1,
          size: 'small',
          loading: loading.value,
          // Không bind update trực tiếp
        }),
        default: () => `Bạn có chắc muốn ${row.customerStatus === 1 ? 'ngưng hoạt động' : 'kích hoạt'} khách hàng này?`,
      },
    ),
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 80,
    align: 'center',
    fixed: 'right',
    render: (row: Customer) => h(
      NTooltip,
      { trigger: 'hover' },
      {
        trigger: () => h(NButton, {
          size: 'small',
          secondary: true,
          type: 'warning',
          circle: true,
          class: 'scale-100 transition-all hover:scale-125 hover:shadow-lg',
          onClick: () => navigateToEdit(row),
        }, { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) }),
        default: () => 'Sửa thông tin',
      },
    ),
  },
]
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-blue-600">
            <Icon icon="carbon:user-multiple" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý Khách hàng
          </span>
        </NSpace>
        <span>Quản lý thông tin khách hàng, lịch sử mua hàng và địa chỉ nhận hàng</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="rounded-2xl shadow-md mb-4">
      <template #header-extra>
        <div class="mr-5">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton
                size="large"
                circle
                secondary
                type="primary"
                class="transition-all duration-200 hover:scale-110 hover:shadow-md"
                @click="resetFilters"
              >
                <NIcon size="24">
                  <Icon icon="carbon:filter-reset" />
                </NIcon>
              </NButton>
            </template>
            Làm mới bộ lọc
          </NTooltip>
        </div>
      </template>

      <NForm label-placement="top" :model="filter">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mt-2">
          <NFormItem label="Tìm kiếm chung">
            <NInput
              v-model:value="filter.customerName"
              placeholder="Tên, mã khách hàng, SĐT..."
              clearable
              @input="debouncedFetch"
              @keydown.enter="fetchCustomers"
            >
              <template #prefix>
                <Icon icon="carbon:search" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="Giới tính">
            <NSelect
              v-model:value="filter.customerGender"
              placeholder="Chọn giới tính"
              clearable
              :options="[
                { label: 'Nam', value: 1 },
                { label: 'Nữ', value: 0 },
              ]"
            />
          </NFormItem>

          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="filter.customerStatus" name="radiogroup">
              <NSpace>
                <NRadio :value="null">
                  Tất cả
                </NRadio>
                <NRadio :value="1">
                  Hoạt động
                </NRadio>
                <NRadio :value="0">
                  Ngưng
                </NRadio>
              </NSpace>
            </NRadioGroup>
          </NFormItem>
        </div>
      </NForm>
    </NCard>

    <NCard title="Danh sách khách hàng" class="border rounded-3">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="navigateToAdd"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Thêm mới
              </span>
            </NButton>

            <NDropdown trigger="hover" :options="exportOptions" @select="handleSelectExport">
              <NButton
                type="success"
                secondary
                class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              >
                <template #icon>
                  <NIcon size="24">
                    <Icon icon="file-icons:microsoft-excel" />
                  </NIcon>
                </template>
                <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                  Xuất Excel
                </span>
              </NButton>
            </NDropdown>

            <NButton
              type="info"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="handlePrint"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:printer" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                In danh sách
              </span>
            </NButton>
            <NButton
              type="info"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="fetchCustomers"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Tải lại
              </span>
            </NButton>
          </NSpace>
        </div>
      </template>

      <NDataTable
        :columns="columns"
        :data="customers"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        striped
        :scroll-x="1200"
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="totalElements"
          :page-sizes="[5, 10, 20, 50]"
          show-size-picker
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </NCard>
  </div>
</template>
