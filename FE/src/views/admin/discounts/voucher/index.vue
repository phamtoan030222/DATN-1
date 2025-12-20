<script lang="ts" setup>
import { h, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router' // Import Router
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
  NRadio,
  NRadioGroup,
  NSpace,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { getVouchers } from '@/service/api/admin/discount/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'
import formatDate from '@/utils/common.helper'

/* ===================== Config & Router ===================== */
const router = useRouter()
// const route = useRoute()
const message = useMessage()

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
    return { text: 'Đã kết thúc', type: '', value: 'ENDED' }

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
  pageSize: 5,
  itemCount: 0,
  showSizePicker: true,
})

/* ===================== Methods (CHUYỂN TRANG) ===================== */
// Thay vì bật popup, ta chuyển hướng URL
function openAddPage() {
  router.push({ name: 'discounts_voucher_add' }) // Phải khớp với name trong router config
}

function openEditPage(id: string) {
  router.push({ name: 'discounts_voucher_edit', params: { id } })
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
}

/* ===================== Logic ===================== */
function handleClientSideFilter() {
  loading.value = true
  let filtered = allData.value.filter((item) => {
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
    allData.value = (res.content ?? []).map(item => ({ ...item, status: item.status ?? 'INACTIVE' }))
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
const columns: DataTableColumns<ADVoucherResponse> = [
  { title: 'STT', key: 'stt', width: 60, render: (row, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize },
  { title: 'Mã', key: 'code', width: 120 },
  { title: 'Tên', key: 'name', width: 180 },
  { title: 'Kiểu', key: 'targetType', width: 100, render(row) { const typeInfo = getVoucherTypeText(row); return h(NTag, { type: typeInfo.type, size: 'small' }, { default: () => typeInfo.text }) } },
  { title: 'Số lượng', key: 'quantity', width: 100 },
  {
    title: 'Giá trị',
    key: 'discountValue',
    render(row) {
      if (row.discountValue === null)
        return 'N/A'; if (row.typeVoucher === 'PERCENTAGE')
        return `${row.discountValue}%`; return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.discountValue)
    },
  },
  {
    title: 'Điều kiện',
    key: 'conditions',
    render(row) {
      if (row.conditions === null)
        return 'N/A'; return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.conditions)
    },
  },
  { title: 'Ngày bắt đầu', key: 'startDate', render: row => (row.startDate ? formatDate(row.startDate) : 'N/A') },
  { title: 'Ngày kết thúc', key: 'endDate', render: row => (row.endDate ? formatDate(row.endDate) : 'N/A') },
  { title: 'Trạng thái', key: 'computedStatus', width: 130, render(row) { const statusInfo = getVoucherStatus(row); return h(NTag, { type: statusInfo.type, size: 'small' }, { default: () => statusInfo.text }) } },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 80,
    render(row: ADVoucherResponse) {
      const id = row.id
      if (!id)
        return h('span', { class: 'text-gray-500 text-xs' }, 'Không có ID')
      const isUpcoming = (row.startDate || 0) > Date.now()
      return h(NSpace, { size: 8, justify: 'center' }, () => [
        h(NButton, {
          size: 'small',
          title: isUpcoming ? 'Sửa' : 'Không thể sửa phiếu đang/đã diễn ra',
          text: true,
          type: isUpcoming ? 'primary' : 'default',
          onClick: () => {
            if (!isUpcoming) { message.warning('Chỉ có thể sửa phiếu giảm giá Sắp diễn ra !!!'); return }
            openEditPage(id) // Dùng hàm chuyển trang mới
          },
        }, { icon: () => h(NIcon, { color: isUpcoming ? undefined : '#bfbfbf' }, { default: () => h(Icon, { icon: 'carbon:edit' }) }) }),
      ])
    },
  },
]

/* ===================== Watchers & Hooks ===================== */
// Khi quay trở lại trang danh sách, load lại dữ liệu
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
          <NIcon size="24">
            <Icon icon="icon-park-outline:ticket" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">Quản lý Phiếu Giảm Giá</span>
        </NSpace>
        <span>Quản lý danh sách Phiếu Giảm Giá có mặt tại cửa hàng</span>
      </NSpace>
    </NCard>

    <NCard title="Bộ lọc" class="rounded-2xl shadow-md mb-4">
      <template #header-extra>
        <div class="mr-5">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton size="large" circle secondary type="primary" title="Làm mới bộ lọc" @click="resetFilters">
                <NIcon size="35">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </NButton>
            </template>
            Làm mới bộ lọc
          </NTooltip>
        </div>
      </template>

      <NForm label-placement="top">
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
          <NFormItem label="Tên hoặc mã">
            <NInput v-model:value="filters.q" placeholder="Nhập tên hoặc mã..." />
          </NFormItem>
          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="filters.status" name="status">
              <NSpace>
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
              </NSpace>
            </NRadioGroup>
          </NFormItem>
          <NFormItem label="Điều kiện áp dụng tối thiểu">
            <NInputNumber v-model:value="filters.conditions" step="1000" placeholder="VD: 100,000..." class="w-full" />
          </NFormItem>
          <NFormItem label="Thời gian diễn ra">
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
            <NTooltip trigger="hover" placement="top">
              <template #trigger>
                <NButton type="primary" secondary circle title="Thêm mới" @click="openAddPage">
                  <NIcon size="24">
                    <Icon icon="carbon:add" />
                  </NIcon>
                </NButton>
              </template>
              Thêm mới
            </NTooltip>
            <NTooltip trigger="hover" placement="top">
              <template #trigger>
                <NButton circle secondary type="primary" title="Làm mới" @click="fetchData">
                  <NIcon size="24">
                    <Icon icon="carbon:rotate" />
                  </NIcon>
                </NButton>
              </template>
              Làm mới
            </NTooltip>
          </NSpace>
        </div>
      </template>

      <NDataTable
        v-model:checked-row-keys="checkedRowKeys" :columns="columns" :data="displayData" :loading="loading"
        :row-key="(row) => row.id" :pagination="false" bordered
      />

      <div class="flex justify-center mt-4">
        <NPagination
          :page="pagination.page" :page-size="pagination.pageSize" :item-count="pagination.itemCount"
          @update:page="(page) => { pagination.page = page }"
          @update:page-size="(size) => { pagination.pageSize = size; pagination.page = 1 }"
        />
      </div>
    </NCard>
  </div>
</template>
