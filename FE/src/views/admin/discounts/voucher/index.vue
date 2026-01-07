<script lang="ts" setup>
import { h, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NInputNumber,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSpace,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import {
  getVouchers,
  updateVoucherToEnd,
  updateVoucherToStart,
} from '@/service/api/admin/discount/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'

/* ===================== Config & Router ===================== */
const router = useRouter()
const message = useMessage()

/* ===================== Format Date Time ===================== */
function formatDateTime(timestamp: number | undefined) {
  if (!timestamp)
    return '-'
  return new Date(timestamp).toLocaleString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/* ===================== Utility Functions ===================== */
function getVoucherStatus(row: ADVoucherResponse) {
  const now = Date.now()
  const startDate = row.startDate
  const endDate = row.endDate
  const remainingQuantity = row.remainingQuantity
  const targetType = row.targetType

  if (startDate && startDate > now)
    return { text: 'Sắp diễn ra', type: 'info', value: 'UPCOMING' }

  const isExpiredByDate = endDate && endDate < now
  const isExpiredByQuantity = targetType === 'ALL_CUSTOMERS' && (remainingQuantity === null || remainingQuantity <= 0)

  if (isExpiredByDate || isExpiredByQuantity)
    return { text: 'Đã kết thúc', type: 'default', value: 'ENDED' }

  const isWithinTime = (startDate || 0) <= now && (endDate || Infinity) >= now
  const hasRemainingQuantity = targetType === 'INDIVIDUAL' || (targetType === 'ALL_CUSTOMERS' && (remainingQuantity || 0) > 0)

  if (isWithinTime && hasRemainingQuantity)
    return { text: 'Đang diễn ra', type: 'success', value: 'ONGOING' }

  return { text: 'Không xác định', type: 'default', value: 'UNKNOWN' }
}

function getVoucherTypeText(row: ADVoucherResponse) {
  if (row.targetType === 'INDIVIDUAL')
    return { text: 'Cá nhân', type: 'info' }
  if (row.targetType === 'ALL_CUSTOMERS')
    return { text: 'Công khai', type: 'primary' }
  return { text: '—', type: 'default' }
}

/* ===================== State ===================== */
const loading = ref(false)
const allData = ref<ADVoucherResponse[]>([])
const displayData = ref<ADVoucherResponse[]>([])
const checkedRowKeys = ref<(string | number)[]>([])

const pagination = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
})

/* ===================== Methods (ACTIONS) ===================== */
function openAddPage() {
  router.push({ name: 'discounts_voucher_add' })
}

function openEditPage(id: string) {
  router.push({ name: 'discounts_voucher_edit', params: { id } })
}

// --- Xử lý Kích hoạt ngay ---
async function handleStartVoucher(id: string) {
  loading.value = true
  try {
    await updateVoucherToStart(id)
    message.success('Đã kích hoạt phiếu giảm giá!')
    await fetchData()
  }
  catch (err: any) {
    message.error(err.response?.data?.message || 'Lỗi khi kích hoạt voucher')
  }
  finally {
    loading.value = false
  }
}

// --- Xử lý Kết thúc ngay ---
async function handleEndVoucher(id: string) {
  loading.value = true
  try {
    await updateVoucherToEnd(id)
    message.success('Đã kết thúc phiếu giảm giá!')
    await fetchData()
  }
  catch (err: any) {
    message.error(err.response?.data?.message || 'Lỗi khi kết thúc voucher')
  }
  finally {
    loading.value = false
  }
}

/* ===================== Filters ===================== */
const filters = ref({
  q: '',
  conditions: null as number | null,
  dateRange: null as [number, number] | null,
  status: '' as string | null,
})

function resetFilters() {
  filters.value.q = ''
  filters.value.conditions = null
  filters.value.dateRange = null
  filters.value.status = ''
  pagination.value.page = 1
  handleClientSideFilter()
  fetchData()
}

function handleClientSideFilter() {
  loading.value = true
  const filtered = allData.value.filter((item) => {
    if (!filters.value.status)
      return true
    const statusInfo = getVoucherStatus(item)
    return statusInfo.value === filters.value.status
  })

  pagination.value.itemCount = filtered.length
  const startIndex = (pagination.value.page - 1) * pagination.value.pageSize
  const endIndex = startIndex + pagination.value.pageSize
  displayData.value = filtered.slice(startIndex, endIndex)
  loading.value = false
}

async function fetchData() {
  loading.value = true
  try {
    const query: ADVoucherQuery = {
      page: 1,
      size: 1000,
      q: filters.value.q || undefined,
      conditions: filters.value.conditions || undefined,
      startDate: filters.value.dateRange?.[0],
      endDate: filters.value.dateRange?.[1],
      status: undefined,
    }
    const res = await getVouchers(query)
    allData.value = (res.content ?? []).map(item => ({ ...item }))
    handleClientSideFilter()
    checkedRowKeys.value = []
  }
  catch (err: any) {
    message.error(`Lỗi tải dữ liệu: ${err.message || 'Unknown error'}`)
    allData.value = []
    displayData.value = []
  }
  finally {
    loading.value = false
  }
}

/* ===================== Data Table ===================== */
/* ===================== Data Table ===================== */
const columns: DataTableColumns<ADVoucherResponse> = [
  { title: 'STT', key: 'stt', fixed: 'left', align: 'center', width: 60, render: (row, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize },
  { title: 'Mã', key: 'code', fixed: 'left', width: 120, render: row => h('strong', { class: 'text-primary' }, row.code) },
  { title: 'Tên', key: 'name', fixed: 'left', width: 180, ellipsis: { tooltip: true } },
  {
    title: 'Kiểu',
    align: 'center',
    key: 'targetType',
    width: 90,
    render(row) {
      const typeInfo = getVoucherTypeText(row)
      return h(NTag, { type: typeInfo.type, size: 'small', bordered: false }, { default: () => typeInfo.text })
    },
  },
  { title: 'Số lượng', align: 'center', width: 90, key: 'quantity' },
  { title: 'Còn lại', align: 'center', key: 'remainingQuantity', width: 90 },
  {
    title: 'Giá trị',
    width: 100,
    align: 'center',
    key: 'discountValue',
    render(row) {
      if (row.discountValue === null)
        return 'N/A'

      const isPercent = row.typeVoucher === 'PERCENTAGE'
      // Yêu cầu: % là error, Tiền là primary
      const tagType = isPercent ? 'error' : 'primary'

      return h(
        NTag,
        { type: tagType, size: 'small' },
        { default: () => isPercent ? `${row.discountValue}%` : new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.discountValue) },
      )
    },
  },
  {
    title: 'Giảm tối đa',
    key: 'maxValue',
    width: 100,
    render(row) {
      if (row.maxValue === null)
        return '-'
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.maxValue)
    },
  },
  {
    title: 'Đơn tối thiểu',
    key: 'conditions',
    width: 120,
    render(row) {
      if (row.conditions === null)
        return '-'
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.conditions)
    },
  },
  { title: 'Bắt đầu', key: 'startDate', width: 110, render: row => (row.startDate ? formatDateTime(row.startDate) : '-') },
  { title: 'Kết thúc', key: 'endDate', width: 110, render: row => (row.endDate ? formatDateTime(row.endDate) : '-') },
  {
    title: 'Trạng thái',
    key: 'computedStatus',
    width: 110,
    align: 'center',
    render(row) {
      const statusInfo = getVoucherStatus(row)
      return h(
        NTag,
        {
          type: statusInfo.type,
          size: 'small',
          style: { cursor: 'pointer' },
          onClick: () => {
            filters.value.status = statusInfo.value
            handleClientSideFilter()
          },
        },
        { default: () => statusInfo.text },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 120, // Tăng nhẹ width để đủ chỗ cho các nút scale
    fixed: 'right',
    render(row: ADVoucherResponse) {
      const id = row.id
      if (!id)
        return h('span', null, '-')

      const statusInfo = getVoucherStatus(row)
      const actions = []

      // --- LOGIC NÚT SỬA ---
      let editTooltip = 'Sửa thông tin'
      let isEditDisabled = false
      let editBtnClass = 'transition-all duration-200 hover:scale-[1.3] hover:shadow-lg' // Class active

      // Xử lý trạng thái disable
      if (statusInfo.value === 'ONGOING') {
        editTooltip = 'Không thể sửa vì đang diễn ra'
        isEditDisabled = true
        editBtnClass = 'opacity-50 cursor-not-allowed' // Class disabled
      }
      else if (statusInfo.value === 'ENDED') {
        editTooltip = 'Không được sửa vì đã kết thúc'
        isEditDisabled = true
        editBtnClass = 'opacity-50 cursor-not-allowed' // Class disabled
      }

      // Render Nút Sửa
      actions.push(
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            type: isEditDisabled ? 'default' : 'warning', // Nếu disable thì màu xám, ngược lại màu xanh
            secondary: true,
            circle: true,
            disabled: isEditDisabled,
            class: editBtnClass,
            onClick: isEditDisabled ? undefined : () => openEditPage(id),
          }, { icon: () => h(Icon, { icon: 'carbon:edit' }) }),
          default: () => editTooltip,
        }),
      )

      // --- LOGIC NÚT KÍCH HOẠT (Chỉ hiện khi Sắp diễn ra) ---
      if (statusInfo.value === 'UPCOMING') {
        actions.push(
          h(NPopconfirm, { onPositiveClick: () => handleStartVoucher(id) }, {
            trigger: () => h(NTooltip, { trigger: 'hover' }, {
              trigger: () => h(NButton, {
                size: 'small',
                type: 'success',
                secondary: true,
                circle: true,
                class: 'ml-2 transition-all duration-200 hover:scale-[1.3] hover:shadow-lg', // Scale 130%
              }, { icon: () => h(Icon, { icon: 'carbon:play-filled-alt' }) }),
              default: () => 'Kích hoạt ngay',
            }),
            default: () => 'Bạn có chắc chắn muốn kích hoạt Voucher này ngay bây giờ?',
          }),
        )
      }

      // --- LOGIC NÚT KẾT THÚC (Chỉ hiện khi Đang diễn ra) ---
      if (statusInfo.value === 'ONGOING') {
        actions.push(
          h(NPopconfirm, { onPositiveClick: () => handleEndVoucher(id) }, {
            trigger: () => h(NTooltip, { trigger: 'hover' }, {
              trigger: () => h(NButton, {
                size: 'small',
                type: 'error',
                secondary: true,
                circle: true,
                class: 'ml-2 transition-all duration-200 hover:scale-[1.3] hover:shadow-lg', // Scale 130%
              }, { icon: () => h(Icon, { icon: 'carbon:stop-filled-alt' }) }),
              default: () => 'Kết thúc ngay',
            }),
            default: () => 'Xác nhận kết thúc Voucher này? Khách hàng sẽ không thể sử dụng nữa.',
          }),
        )
      }

      return h('div', { class: 'flex justify-center items-center' }, actions)
    },
  },
]
/* ===================== Watchers & Hooks ===================== */
onMounted(() => {
  fetchData()
})

watch([() => filters.value.q, () => filters.value.conditions, () => filters.value.dateRange], () => { pagination.value.page = 1; fetchData() })
watch([() => filters.value.status, () => pagination.value.page, () => pagination.value.pageSize], () => { handleClientSideFilter() })
</script>

<template>
  <div>
    <NCard class="mb-3">
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24" class="text-red-500">
            <Icon icon="icon-park-outline:ticket" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">Quản lý Phiếu Giảm Giá</span>
        </NSpace>
        <span>Quản lý danh sách Phiếu Giảm Giá có mặt tại cửa hàng</span>
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
                title="Làm mới bộ lọc"
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

      <NForm label-placement="top">
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-2">
          <NFormItem label="Tên hoặc mã">
            <NInput v-model:value="filters.q" placeholder="Nhập tên hoặc mã..." clearable>
              <template #prefix>
                <Icon icon="carbon:search" />
              </template>
            </NInput>
          </NFormItem>

          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="filters.status" name="status">
              <div class="flex flex-wrap gap-2">
                <NRadio value="">
                  Tất cả
                </NRadio>
                <NRadio value="UPCOMING">
                  Sắp diễn ra
                </NRadio>
                <NRadio value="ONGOING">
                  Đang diễn ra
                </NRadio>
                <NRadio value="ENDED">
                  Đã kết thúc
                </NRadio>
              </div>
            </NRadioGroup>
          </NFormItem>

          <NFormItem label="Đơn tối thiểu">
            <NInputNumber v-model:value="filters.conditions" step="1000" placeholder="VD: 100,000" class="w-full" />
          </NFormItem>

          <NFormItem label="Thời gian">
            <NDatePicker
              v-model:value="filters.dateRange" type="daterange" clearable class="w-full"
              start-placeholder="Bắt đầu" end-placeholder="Kết thúc"
            />
          </NFormItem>
        </div>
      </NForm>
    </NCard>

    <NCard title="Danh sách Phiếu Giảm Giá" class="border rounded-3">
      <template #header-extra>
        <div class="mr-5">
          <NSpace>
            <NButton
              type="primary"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="openAddPage"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:add" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Tạo phiếu giảm giá
              </span>
            </NButton>

            <NButton
              type="info"
              secondary
              class="group rounded-full px-3 transition-all duration-300 ease-in-out"
              @click="fetchData"
            >
              <template #icon>
                <NIcon size="24">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </template>
              <span class="max-w-0 overflow-hidden whitespace-nowrap opacity-0 transition-all duration-300 ease-in-out group-hover:max-w-[150px] group-hover:opacity-100 group-hover:ml-2">
                Tải lại dữ liệu
              </span>
            </NButton>
          </NSpace>
        </div>
      </template>

      <NDataTable
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="displayData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        striped
        :scroll-x="1200"
      />

      <div class="flex justify-end mt-4">
        <NPagination
          v-model:page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          :page-sizes="[5, 10, 20, 50]"
          :show-size-picker="true"
        />
      </div>
    </NCard>
  </div>
</template>
