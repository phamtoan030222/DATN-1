<script lang="ts" setup>
import { h, onMounted, ref, watch } from 'vue'
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
import AddEditVoucher from './AddEditVoucher.vue'
import {
  getVouchers,
} from '@/service/api/admin/discount/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'
import formatDate from '@/utils/common.helper'

/* ===================== Utility Functions ===================== */
function getVoucherStatus(row: ADVoucherResponse) {
  const now = Date.now()
  const startDate = row.startDate
  const endDate = row.endDate
  const remainingQuantity = row.remainingQuantity
  const targetType = row.targetType

  if (startDate && startDate > now) {
    return { text: 'Sắp diễn ra', type: 'info', value: 'UPCOMING' }
  }

  const isExpiredByDate = endDate && endDate < now
  const isExpiredByQuantity = targetType === 'ALL_CUSTOMERS' && (remainingQuantity === null || remainingQuantity <= 0)

  if (isExpiredByDate || isExpiredByQuantity) {
    return { text: 'Đã kết thúc', type: '', value: 'ENDED' }
  }

  const isWithinTime = (startDate || 0) <= now && (endDate || Infinity) >= now
  const hasRemainingQuantity = targetType === 'INDIVIDUAL' || (targetType === 'ALL_CUSTOMERS' && (remainingQuantity || 0) > 0)

  if (isWithinTime && hasRemainingQuantity) {
    return { text: 'Đang diễn ra', type: 'success', value: 'ONGOING' }
  }

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
const message = useMessage()
const loading = ref(false)
// Biến lưu trữ toàn bộ dữ liệu gốc từ API
const allData = ref<ADVoucherResponse[]>([])
// Biến lưu trữ dữ liệu đang hiển thị (đã phân trang)
const displayData = ref<ADVoucherResponse[]>([])
const checkedRowKeys = ref<(string | number)[]>([])

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
  fetchData() // Tải lại dữ liệu khi đóng form
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
  handleClientSideFilter() // Lọc lại
}

/* ===================== Client Side Logic ===================== */
// Hàm xử lý lọc và phân trang tại Frontend
function handleClientSideFilter() {
  loading.value = true

  // 1. Lọc dữ liệu từ allData
  let filtered = allData.value.filter((item) => {
    // Lọc theo Tên/Mã (Backend đã làm tốt, nhưng nếu fetch all thì lọc lại cũng được)
    // Ở đây ta giả định fetch all vẫn dùng tham số 'q' của BE để giảm tải,
    // hoặc lọc thủ công nếu muốn chính xác tuyệt đối.
    // Để đơn giản, ta tin tưởng BE đã lọc theo q, conditions, dateRange rồi.
    // Ta CHỈ LỌC THÊM STATUS ở đây.

    if (!filters.value.status)
      return true // Không chọn status -> lấy hết

    // Tính toán trạng thái thời gian thực
    const statusInfo = getVoucherStatus(item)
    return statusInfo.value === filters.value.status
  })

  // 2. Cập nhật tổng số lượng
  pagination.value.itemCount = filtered.length

  // 3. Cắt trang (Phân trang)
  const startIndex = (pagination.value.page - 1) * pagination.value.pageSize
  const endIndex = startIndex + pagination.value.pageSize

  displayData.value = filtered.slice(startIndex, endIndex)

  loading.value = false
}

/* ===================== Fetch API ===================== */
async function fetchData() {
  loading.value = true
  try {
    // Mẹo: Request size cực lớn để lấy hết dữ liệu về (Client-side handling)
    const query: ADVoucherQuery = {
      page: 1, // Luôn lấy từ trang 1 của BE
      size: 1000, // Lấy 1000 bản ghi (hoặc số lớn hơn tùy database)
      q: filters.value.q || undefined,
      conditions: filters.value.conditions || undefined,
      startDate: filters.value.dateRange?.[0],
      endDate: filters.value.dateRange?.[1],
      // KHÔNG gửi status UPCOMING/ONGOING... lên BE vì BE không hiểu
      // Chỉ gửi status nếu nó là ACTIVE/INACTIVE (nếu bạn muốn kết hợp)
      status: undefined,
    }

    const res = await getVouchers(query)

    // Lưu toàn bộ dữ liệu thô
    allData.value = (res.content ?? []).map(item => ({ ...item, status: item.status ?? 'INACTIVE' }))

    // Gọi hàm lọc client
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
  { type: 'selection' },
  { title: 'STT', key: 'stt', width: 60, render: (row, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize },
  { title: 'Mã', key: 'code', width: 120 },
  { title: 'Tên', key: 'name', width: 180 },
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
  {
    title: 'Trạng thái',
    key: 'computedStatus',
    width: 130,
    render(row) {
      const statusInfo = getVoucherStatus(row)
      return h(NTag, { type: statusInfo.type, size: 'small' }, { default: () => statusInfo.text })
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 80,
    render(row: ADVoucherResponse) {
      const id = row.id
      if (!id)
        return h('span', { class: 'text-gray-500 text-xs' }, 'Không có ID')
      return h(NSpace, { size: 8, justify: 'center' }, () => [
        h(
          NButton,
          {
            size: 'small',
            title: 'Sửa',
            text: true,
            type: 'primary',
            onClick: () => openEditPage(id),
          },
          { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) },
        ),
      ])
    },
  },
]

/* ===================== Watchers & Hooks ===================== */
onMounted(() => {
  showAddEditForm.value = false
  fetchData()
})

// Khi bộ lọc thay đổi, gọi lại API để lấy dữ liệu mới nhất (với các tiêu chí tìm kiếm cơ bản)
watch(
  [() => filters.value.q, () => filters.value.conditions, () => filters.value.dateRange],
  () => {
    pagination.value.page = 1
    fetchData()
  },
)

// Khi thay đổi Status Filter hoặc Pagination -> CHỈ cần lọc lại trên Client, không cần gọi API
watch(
  [() => filters.value.status, () => pagination.value.page, () => pagination.value.pageSize],
  () => {
    handleClientSideFilter()
  },
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
            <NInputNumber
              v-model:value="filters.conditions"
              step="1000"
              placeholder="VD: 100,000..."
              class="w-full"
            />
          </NFormItem>

          <NFormItem label="Thời gian diễn ra">
            <NDatePicker
              v-model:value="filters.dateRange"
              type="daterange"
              clearable
              class="w-full"
              start-placeholder="Bắt đầu"
              end-placeholder="Kết thúc"
            />
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
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="displayData"
        :loading="loading"
        :row-key="(row) => row.id"
        :pagination="false"
        bordered
      />

      <div class="flex justify-center mt-4">
        <NPagination
          :page="pagination.page"
          :page-size="pagination.pageSize"
          :item-count="pagination.itemCount"
          @update:page="(page) => { pagination.page = page }"
          @update:page-size="(size) => { pagination.pageSize = size; pagination.page = 1 }"
        />
      </div>
    </NCard>
  </div>
</template>
