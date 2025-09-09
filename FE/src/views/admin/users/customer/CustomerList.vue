<template>
  <div class="p-6">
    <!-- Header Section -->
    <n-card>
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24">
            <Icon :icon="'carbon:events'" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý Khách hàng
          </span>
        </NSpace>
        <span>Quản lý khách hàng trên toàn hệ thống cửa hàng</span>
      </NSpace>
    </n-card>

    <!-- Filter Card -->
    <NCard title="Bộ lọc" class="mb-4 mt-3">
      <NForm label-placement="top" :model="formSearch" :label-style="{ fontWeight: 'bold' }">
        <div class="space-y-3">
          <div class="flex gap-4 items-end">
            <!-- Tên khách hàng -->
            <NFormItem label="Tên khách hàng:" class="flex-1">
              <NInput v-model:value="formSearch.customerName" placeholder="Nhập tên khách hàng..."
                @input="handleAutoFilter" clearable />
            </NFormItem>

            <!-- Giới tính - Thu hẹp width -->
            <NFormItem label="Giới tính:" style="width: 200px;">
              <NSelect v-model:value="formSearch.customerGender" :options="GENDER_FILTER_OPTIONS"
                placeholder="Chọn giới tính" clearable @update:value="handleAutoFilter" />
            </NFormItem>

            <!-- Trạng thái -->
            <NFormItem label="Trạng thái:" class="flex-1">
              <NRadioGroup v-model:value="formSearch.customerStatus" @update:value="handleAutoFilter">
                <NRadio :value="STATUS_OPTIONS.ALL">Tất cả</NRadio>
                <NRadio :value="STATUS_OPTIONS.ACTIVE">Hoạt động</NRadio>
                <NRadio :value="STATUS_OPTIONS.INACTIVE">Ngưng</NRadio>
              </NRadioGroup>
            </NFormItem>

            <!-- Actions -->
            <div class="pb-6 flex gap-2">
              <NButton type="primary" secondary circle title="Làm mới" @click="refreshTable">
                <NIcon size="20">
                  <Icon :icon="'icon-park-outline:refresh'" />
                </NIcon>
              </NButton>
            </div>
          </div>
        </div>
      </NForm>
    </NCard>

    <!-- Main Table Card -->
    <NCard title="Danh sách khách hàng">
      <template #header-extra>
        <NSpace>
          <NButton @click="navigateToAdd" type="primary" circle title="Thêm khách hàng">
            <NIcon size="20">
              <Icon :icon="'icon-park-outline:add-one'" />
            </NIcon>
          </NButton>
        </NSpace>
      </template>

      <NDataTable :row-key="getRowKey" :columns="columns" :data="customers"
        :loading="loading" size="medium" :scroll-x="1200" :pagination="false" bordered />

      <!-- Pagination -->
      <div class="flex justify-center mt-4">
        <NPagination :page="currentPage" :page-size="pageSize" :page-count="pageCount"
          @update:page="handlePageChange" />
      </div>
    </NCard>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import type { DataTableColumns, SelectOption } from 'naive-ui'
import { NButton, NIcon, NPagination, NPopconfirm, NRadio, NRadioGroup, NSpace, NSwitch, useMessage } from 'naive-ui'
import type { Customer } from '@/service/api/admin/users/customer/customer'
import type { Address } from '@/service/api/admin/users/customer/address'
import { Icon } from "@iconify/vue"
import {
  deleteCustomer as deleteCustomerApi,
  getCustomers,
  updateCustomer,
} from '@/service/api/admin/users/customer/customer'
import {
  getAddressesByCustomer,
} from '@/service/api/admin/users/customer/address'
import { useRouter } from 'vue-router'
import { getDistrictName,getProvinceName,getWardName,initLocationData } from '@/service/api/admin/users/customer/location-service'

// =============================================================================
// CONSTANTS
// =============================================================================

const STATUS_OPTIONS = {
  ALL: -1,
  ACTIVE: 1,
  INACTIVE: 0,
} as const

const SEARCH_DEBOUNCE_DELAY = 500
const DEFAULT_PAGE_SIZE = 10

// Options for filters
const GENDER_FILTER_OPTIONS: SelectOption[] = [
  { label: 'Tất cả', value: null },
  { label: 'Nam', value: 1 },
  { label: 'Nữ', value: 0 }
]

// =============================================================================
// COMPOSABLES
// =============================================================================

const message = useMessage()
const router = useRouter()

// =============================================================================
// STATE & REACTIVE DATA
// =============================================================================

// Data state
const customers = ref<Customer[]>([])
const customerAddresses = ref<Record<string, Address[]>>({})
const currentPage = ref<number>(1)
const pageSize = ref<number>(DEFAULT_PAGE_SIZE)
const totalElements = ref<number>(0)
const totalPages = ref<number>(1)
const loading = ref(false)

// Form search state
const formSearch = reactive({
  customerName: "",
  customerGender: null as number | null,
  customerStatus: STATUS_OPTIONS.ALL as number,
})

// Search timer
let searchTimer: NodeJS.Timeout | null = null

// =============================================================================
// COMPUTED PROPERTIES
// =============================================================================

const pageCount = computed(() =>
  Math.ceil(totalElements.value / pageSize.value)
)

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

const getRowKey = (row: Customer) => row.id as string | number

const formatDate = (timestamp?: number): string => {
  if (!timestamp) return '-'
  const d = new Date(timestamp)
  return d.toLocaleDateString('vi-VN')
}

const DEFAULT_AVATAR = 'https://www.svgrepo.com/show/452030/avatar-default.svg'

const createCustomerInfoCell = (row: Customer) => {
  return h('div', { class: 'flex items-center' }, [
    h('img', {
      src: row.customerAvatar || DEFAULT_AVATAR,
      class: 'flex-shrink-0 h-10 w-10 rounded-md object-cover mr-3',
      alt: row.customerName || 'Avatar',
      onError: (e: Event) => {
        const img = e.target as HTMLImageElement
        if (!img) return
        img.onerror = null
        img.src = DEFAULT_AVATAR
      }
    }),

    h('div', [
      h('div', { class: 'font-medium text-gray-800' }, row.customerName),
      h('div', { class: 'text-sm text-gray-500' },
        `${row.customerCode || 'Chưa có mã'} • ${row.customerGender ? 'Nam' : 'Nữ'}`
      ),
      h('div', { class: 'text-xs text-gray-400' },
        `Ngày sinh: ${formatDate(row.customerBirthday)}`
      )
    ])
  ])
}

const createEmailCell = (row: Customer) =>
  h('div', { class: 'text-sm' }, row.customerEmail || '-')

const createPhoneCell = (row: Customer) =>
  h('div', { class: 'text-sm' }, row.customerPhone || '-')

const createAddressCell = (row: Customer) => {
  const addresses = customerAddresses.value[row.id as string] || []
  const defaultAddress = addresses.find(addr => addr.isDefault)
  const extraCount = addresses.length - 1 // số địa chỉ khác

  if (!defaultAddress) {
    return h('div', { class: 'text-sm text-gray-500' }, 'Chưa có địa chỉ')
  }

  const children = [
    h(
      'div',
      { class: 'text-xs text-sm' },
      resolveAddress(defaultAddress) // địa chỉ mặc định
    )
  ]

  if (extraCount > 0) {
    children.push(
      h(
        'div',
        { class: 'text-xs text-gray-400 italic' },
        `+ ${extraCount} địa chỉ khác`
      )
    )
  }

  return h('div', children)
}

const createDateCell = (row: Customer) =>
  h('div', [
    h('div', { class: 'font-sm' }, formatDate(row.customerCreatedDate)),
    h('div', { class: 'text-xs text-gray-400' },
      `Cập nhật: ${formatDate(row.customerModifiedDate)}`
    )
  ])

const createStatusCell = (row: Customer) =>
  h('div', { class: 'flex flex-col items-center space' }, [
    h(NPopconfirm, {
      onPositiveClick: () => toggleStatus(row)
    }, {
      default: () => `Bạn có chắc chắn muốn ${row.customerStatus === STATUS_OPTIONS.ACTIVE ? 'tắt' : 'bật'} trạng thái khách hàng này?`,
      trigger: () => h(NSwitch, {
        value: row.customerStatus === STATUS_OPTIONS.ACTIVE,
        disabled: loading.value
      })
    })
  ])

const createActionsCell = (row: Customer) =>
  h(NSpace, { justify: 'center', size: 12 }, {
    default: () => [
      // Nút edit - navigate to edit page
      h(NIcon, {
        size: 18,
        class: 'cursor-pointer text-blue-500 hover:text-blue-700',
        onClick: () => navigateToEdit(row.id as string),
        title: 'Sửa khách hàng'
      }, { default: () => h(Icon, { icon: 'mdi:pencil' }) })
    ]
  })

// =============================================================================
// TABLE COLUMNS
// =============================================================================

const columns: DataTableColumns<Customer> = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    render: (_row, index) => (currentPage.value - 1) * pageSize.value + index + 1
  },
  {
    title: 'Thông tin',
    key: 'info',
    width: 250,
    render: createCustomerInfoCell
  },
  {
    title: 'Email',
    key: 'email',
    width: 200,
    render: createEmailCell
  },
  {
    title: 'Số điện thoại',
    key: 'phone',
    width: 150,
    render: createPhoneCell
  },
  {
    title: 'Địa chỉ',
    key: 'address',
    width: 250,
    render: createAddressCell
  },
  {
    title: 'Ngày tạo',
    key: 'dates',
    width: 150,
    render: createDateCell
  },
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 100,
    render: createStatusCell
  },
  {
    title: 'Hành động',
    key: 'actions',
    align: 'center',
    width: 100,
    render: createActionsCell
  }
]

// =============================================================================
// WATCHERS
// =============================================================================

watch(
  () => formSearch,
  () => handleAutoFilter(),
  { deep: true }
)

// =============================================================================
// API FUNCTIONS
// =============================================================================

const buildSearchParams = () => {
  const page = Math.max(1, Number(currentPage.value) || 1)
  const size = Math.max(1, Number(pageSize.value) || DEFAULT_PAGE_SIZE)

  return {
    page,
    size,
    ...(formSearch.customerName && { customerName: formSearch.customerName }),
    ...(formSearch.customerGender !== null && { customerGender: formSearch.customerGender }),
    ...(formSearch.customerStatus !== STATUS_OPTIONS.ALL && { customerStatus: formSearch.customerStatus }),
  }
}

async function loadCustomers() {
  try {
    loading.value = true
    const params = buildSearchParams()
    const response = await getCustomers(params)

    const payload = response?.data?.data
    if (!payload) throw new Error('Không có dữ liệu trả về')

    customers.value = (payload.data || []).map((c: Customer) => ({
      ...c,
      customerAvatar: c.customerAvatar || DEFAULT_AVATAR
    }))

    pageSize.value = Number(payload.size ?? params.size)
    totalElements.value = Number(payload.totalElements ?? 0)
    totalPages.value = Number(payload.totalPages ?? 1)

    // Load addresses for all customers
    await loadCustomerAddresses()
  } catch (error) {
    console.error('Lỗi khi tải danh sách khách hàng:', error)
    customers.value = []
    totalElements.value = 0
    totalPages.value = 1
    message.error('Có lỗi xảy ra khi tải dữ liệu khách hàng')
  } finally {
    loading.value = false
  }
}

async function loadCustomerAddresses() {
  try {
    const addressPromises = customers.value.map(async (customer) => {
      if (!customer.id) return
      try {
        const response = await getAddressesByCustomer(customer.id as string)
        const rawAddrs: Address[] = response.data.data || []
        // map and add displayAddress
        customerAddresses.value[customer.id as string] = rawAddrs.map(a => ({
          ...a,
          displayAddress: resolveAddress(a) // add computed field
        }))
      } catch (error) {
        console.error(`Lỗi khi tải địa chỉ cho khách hàng ${customer.id}:`, error)
        customerAddresses.value[customer.id as string] = []
      }
    })

    await Promise.all(addressPromises)
  } catch (error) {
    console.error('Lỗi khi tải địa chỉ khách hàng:', error)
  }
}


// =============================================================================
// NAVIGATION FUNCTIONS
// =============================================================================

function navigateToAdd() {
  router.push('/users/customer/add')
}

function navigateToEdit(customerId: string) {
  router.push(`/users/customer/edit/${customerId}`)
}

// =============================================================================
// EVENT HANDLERS
// =============================================================================

function handleAutoFilter() {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadCustomers()
  }, SEARCH_DEBOUNCE_DELAY)
}

function refreshTable() {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }

  Object.assign(formSearch, {
    customerName: "",
    customerGender: null,
    customerStatus: STATUS_OPTIONS.ALL,
  })
  currentPage.value = 1
  loadCustomers()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadCustomers()
}

// =============================================================================
// CUSTOMER OPERATIONS
// =============================================================================

async function toggleStatus(customer: Customer) {
  const originalStatus = customer.customerStatus
  const newStatus = originalStatus === STATUS_OPTIONS.ACTIVE ? STATUS_OPTIONS.INACTIVE : STATUS_OPTIONS.ACTIVE

  try {
    // Optimistic UI update
    const idx = customers.value.findIndex(c => c.id === customer.id)
    if (idx !== -1) customers.value[idx].customerStatus = newStatus

    const updatedCustomer: Customer = {
      ...customer,
      customerStatus: newStatus,
    }

    await updateCustomer(customer.id!, updatedCustomer)
    message.success('Cập nhật trạng thái thành công!')
  } catch (error) {
    // Rollback on error
    const idx = customers.value.findIndex(c => c.id === customer.id)
    if (idx !== -1) customers.value[idx].customerStatus = originalStatus

    console.error('Lỗi khi cập nhật trạng thái:', error)
    message.error('Có lỗi khi cập nhật trạng thái!')
  }
}

async function handleDeleteCustomer(id: string | number) {
  try {
    await deleteCustomerApi(String(id))
    customers.value = customers.value.filter(c => c.id !== id)
    delete customerAddresses.value[id as string]
    totalElements.value = Math.max(0, totalElements.value - 1)
    message.success(`Đã xóa khách hàng ID: ${id}`)

    if (customers.value.length === 0 && currentPage.value > 1) {
      currentPage.value = currentPage.value - 1
      await loadCustomers()
    }
  } catch (error) {
    console.error('Lỗi khi xóa khách hàng:', error)
    message.error('Có lỗi xảy ra khi xóa khách hàng!')
  }
}

function resolveAddress(address: Address): string {
  if (!address) return ''

  const addrDetail = address.addressDetail ? String(address.addressDetail).trim() : ''

  // BE might store either codename (string), code (number) or name (string)
  const wardLabel = getWardName(address.wardCommune as any) || ''
  const districtLabel = getDistrictName(address.district as any) || ''
  const provinceLabel = getProvinceName(address.provinceCity as any) || ''

  // build pieces, filter empty ones
  const parts = []
  if (addrDetail) parts.push(addrDetail)
  if (wardLabel) parts.push(wardLabel)
  if (districtLabel) parts.push(districtLabel)
  if (provinceLabel) parts.push(provinceLabel)

  // if none of ward/district/province resolved (all empty),
  // try fallback: if values exist but not resolvable, show them tidied
  if (parts.length === 0) {
    // try to fallback to raw values (tidy codename)
    const rawParts = []
    if (addrDetail) rawParts.push(addrDetail)
    if (address.wardCommune) rawParts.push(String(address.wardCommune))
    if (address.district) rawParts.push(String(address.district))
    if (address.provinceCity) rawParts.push(String(address.provinceCity))
    return rawParts.filter(Boolean).join(', ')
  }

  return parts.join(', ')
}

// =============================================================================
// LIFECYCLE
// =============================================================================

onMounted(async () => {
  await initLocationData()   // đợi load dữ liệu địa giới xong
  await loadCustomers()      // sau đó mới load danh sách khách hàng
})

</script>
