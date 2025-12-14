<script lang="ts" setup>
import { h, onMounted, ref, watch } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton,
  NCard,
  NDataTable,
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
import AddEditVoucher from './AddEditVoucher.vue'
import {
  getVouchers,
} from '@/service/api/admin/discount/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'
import formatDate from '@/utils/common.helper'

/* ===================== Utility Functions ===================== */
// Logic tính toán trạng thái (Hiển thị Tag)
function getVoucherStatus(row: ADVoucherResponse) {
  const now = Date.now()
  const startDate = row.startDate
  const endDate = row.endDate
  const remainingQuantity = row.remainingQuantity
  const targetType = row.targetType // 'ALL_CUSTOMERS' or 'INDIVIDUAL'

  // 1. Chưa bắt đầu
  if (startDate && startDate > now) {
    return { text: 'Chưa bắt đầu', type: 'info' }
  }
  // 2. Đã hết hạn hoặc Hết hàng
  const isExpiredByDate = endDate && endDate < now
  const isExpiredByQuantity = targetType === 'ALL_CUSTOMERS' && (remainingQuantity === null || remainingQuantity <= 0)

  if (isExpiredByDate || isExpiredByQuantity) {
    return { text: 'Đã hết hạn', type: 'error' }
  }
  // 3. Đang diễn ra
  const isWithinTime = (startDate || 0) <= now && (endDate || Infinity) >= now
  const hasRemainingQuantity = targetType === 'INDIVIDUAL' || (targetType === 'ALL_CUSTOMERS' && (remainingQuantity || 0) > 0)

  if (isWithinTime && hasRemainingQuantity) {
    return { text: 'Đang diễn ra', type: 'success' }
  }
  // Fallback
  return { text: 'Không xác định', type: 'default' }
}

// Logic hiển thị kiểu (Cá nhân/Công khai)
function getVoucherTypeText(row: ADVoucherResponse) {
  if (row.targetType === 'INDIVIDUAL')
    return { text: 'Cá nhân', type: 'info' }
  if (row.targetType === 'ALL_CUSTOMERS')
    return { text: 'Công khai', type: 'primary' }
  return { text: '—', type: 'default' }
}

/* ===================== State ===================== */
const message = useMessage()
const loading = ref(false)
const data = ref<ADVoucherResponse[]>([])
const pagination = ref({
  page: 1,
  pageSize: 5,
  itemCount: 0,
  showSizePicker: true,
})
const showAddEditForm = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const selectedVoucherId = ref<string | null>(null)

/* ===================== Methods ===================== */
function openAddPage() {
  formMode.value = 'add'
  selectedVoucherId.value = null
  showAddEditForm.value = true
}

function openEditPage(id: string) {
  formMode.value = 'edit'
  selectedVoucherId.value = id
  showAddEditForm.value = true
}

function closeForm() {
  showAddEditForm.value = false
  selectedVoucherId.value = null
  fetchData()
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
}

/* ===================== Data Table ===================== */
const columns: DataTableColumns<ADVoucherResponse> = [
  // 1. Cột STT
  { title: 'STT', key: 'stt', width: 60, render: (row, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize },
  // Đã bỏ cột Selection
  { title: 'Mã', key: 'code', width: 120 },
  { title: 'Tên', key: 'name', width: 180 },
  // 2. Cột Kiểu
  {
    title: 'Kiểu',
    key: 'targetType',
    width: 100,
    render(row) {
      const typeInfo = getVoucherTypeText(row)
      return h(NTag, { type: typeInfo.type, size: 'small' }, { default: () => typeInfo.text })
    },
  },
  { title: 'Số lượng', key: 'quantity', width: 100 },
  {
    title: 'Giá trị',
    key: 'discountValue',
    render(row) {
      if (row.discountValue === null)
        return 'N/A'
      if (row.typeVoucher === 'PERCENTAGE')
        return `${row.discountValue}%`
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.discountValue)
    },
  },
  {
    title: 'Điều kiện',
    key: 'conditions',
    render(row) {
      if (row.conditions === null)
        return 'N/A'
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.conditions)
    },
  },
  {
    title: 'Ngày bắt đầu',
    key: 'startDate',
    render: row => (row.startDate ? formatDate(row.startDate) : 'N/A'),
  },
  {
    title: 'Ngày kết thúc',
    key: 'endDate',
    render: row => (row.endDate ? formatDate(row.endDate) : 'N/A'),
  },
  // 3. Cột Trạng thái (Chỉ hiển thị, không thao tác)
  {
    title: 'Trạng thái',
    key: 'computedStatus',
    width: 130,
    render(row) {
      const statusInfo = getVoucherStatus(row)
      return h(NTag, { type: statusInfo.type, size: 'small' }, { default: () => statusInfo.text })
    },
  },
  // 4. Cột Thao tác (Chỉ còn nút Sửa)
  {
    title: 'Thao tác',
    key: 'actions',
    width: 80,
    render(row: ADVoucherResponse) {
      const id = row.id
      if (!id) {
        return h('span', { class: 'text-gray-500 text-xs' }, 'Không có ID')
      }
      return h(NSpace, { size: 8, justify: 'center' }, () => [
        // Nút Sửa
        h(
          NButton,
          {
            size: 'small',
            title: 'Sửa',
            text: true,
            type: 'primary',
            onClick: () => {
              openEditPage(id)
            },
          },
          { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) },
        ),
      ])
    },
  },
]

/* ===================== Fetch API ===================== */
async function fetchData() {
  loading.value = true
  try {
    const query: ADVoucherQuery = {
      page: pagination.value.page,
      size: pagination.value.pageSize,
      q: filters.value.q || undefined,
      conditions: filters.value.conditions || undefined,
      status: filters.value.status || undefined,
      startDate: filters.value.dateRange?.[0],
      endDate: filters.value.dateRange?.[1],
    }
    const res = await getVouchers(query)
    data.value = (res.content ?? []).map(item => ({ ...item, status: item.status ?? 'INACTIVE' }))
    pagination.value.itemCount = res.totalElements ?? 0
  }
  catch (err: any) {
    message.error(`Lỗi tải dữ liệu: ${err.message || 'Unknown error'}`)
    data.value = []
  }
  finally {
    loading.value = false
  }
}

onMounted(() => {
  showAddEditForm.value = false
  fetchData()
})

watch(
  filters,
  () => {
    pagination.value.page = 1
    fetchData()
  },
  { deep: true },
)
</script>

<template>
  <div :key="showAddEditForm ? 'form' : 'list'">
    <AddEditVoucher v-if="showAddEditForm" :mode="formMode" :voucher-id="selectedVoucherId" @close="closeForm" />
    <NCard v-else class="mb-3">
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

    <NCard v-if="!showAddEditForm" title="Bộ lọc" class="rounded-2xl shadow-md mb-4">
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
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mt-4 items-end">
          <NFormItem label="Tên hoặc mã">
            <NInput v-model:value="filters.q" placeholder="Tên hoặc Mã..." class="w-full" />
          </NFormItem>
          <NFormItem label="Điều kiện áp dụng">
            <NInputNumber
              v-model:value="filters.conditions" step="1000" placeholder="Điều kiện áp dụng..."
              class="w-full"
            />
          </NFormItem>
          <NFormItem label="Trạng thái">
            <NRadioGroup v-model:value="filters.status" name="status">
              <NSpace>
                <NRadio value="">
                  Tất cả
                </NRadio>
                <NRadio value="ACTIVE">
                  Hoạt động
                </NRadio>
                <NRadio value="INACTIVE">
                  Không hoạt động
                </NRadio>
              </NSpace>
            </NRadioGroup>
          </NFormItem>
        </div>
      </NForm>
    </NCard>

    <NCard v-if="!showAddEditForm" title="Danh sách Phiếu Giảm Giá" class="border rounded-3">
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
        :columns="columns"
        :data="data"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        bordered
      />

      <div class="flex justify-center mt-4">
        <NPagination
          :page="pagination.page" :page-size="pagination.pageSize" :item-count="pagination.itemCount"
          @update:page="(page) => { pagination.page = page; fetchData(); }"
          @update:page-size="(size) => { pagination.pageSize = size; pagination.page = 1; fetchData(); }"
        />
      </div>
    </NCard>
  </div>
</template>
