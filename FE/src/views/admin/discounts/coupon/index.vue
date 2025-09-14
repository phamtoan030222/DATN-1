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
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSpace,
  NSwitch,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import AddEditVoucher from './AddEditVoucher.vue'

import {
  deleteVoucher,
  deleteVouchers,
  getVouchers,
  updateVoucherStatus,
} from '@/service/api/admin/discount/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'
import formatDate from '@/utils/common.helper'

/* ===================== State ===================== */
const message = useMessage()
const checkedRowKeys = ref<(string | number)[]>([])
const loading = ref(false)
const data = ref<ADVoucherResponse[]>([])
const rowLoading = ref<Record<string, boolean>>({})

const pagination = ref({
  page: 1,
  pageSize: 5, // ✅ TRẢ LẠI 5
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

/* ===================== Delete ===================== */
async function handleDeleteOne(id: string, name: string) {
  try {
    await deleteVoucher(id)
    message.success(`Xoá thành công voucher: ${name}`)
    await fetchData()
  }
  catch (err: any) {
    message.error(`Lỗi xóa voucher: ${err.message || 'Unknown error'}`)
  }
}

async function handleDeleteMany(ids: string[]) {
  try {
    await deleteVouchers(ids)
    message.success('Xoá thành công')
    checkedRowKeys.value = []
    await fetchData()
  }
  catch (err: any) {
    message.error(`Lỗi xóa nhiều voucher: ${err.message || 'Unknown error'}`)
  }
}

/* ===================== Toggle Status (per row) ===================== */
async function toggleOne(row: ADVoucherResponse) {
  const id = String(row.id ?? '')
  if (!id)
    return
  rowLoading.value = { ...rowLoading.value, [id]: true }
  try {
    const next = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    await updateVoucherStatus(id, next)
    row.status = next
    message.success('Cập nhật trạng thái thành công')
  }
  catch (err: any) {
    message.error(`Lỗi cập nhật trạng thái: ${err.message || 'Unknown error'}`)
  }
  finally {
    rowLoading.value = { ...rowLoading.value, [id]: false }
  }
}

/* ===================== Toggle Status for Selected (FIXED) ===================== */
async function handleConfirmStatusChange() {
  // ✅ CHỤP ID TRƯỚC
  const ids = checkedRowKeys.value.map(k => String(k))
  if (ids.length === 0) {
    message.warning('Vui lòng chọn ít nhất một phiếu giảm giá')
    return
  }

  // ✅ Bật loading theo danh sách ids
  const rlStart = { ...rowLoading.value }
  ids.forEach((id) => { rlStart[id] = true })
  rowLoading.value = rlStart

  try {
    await Promise.all(ids.map(async (id) => {
      const row = data.value.find(v => v.id === id)
      if (!row)
        return
      const next = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
      await updateVoucherStatus(id, next)
      row.status = next
    }))
    message.success('Cập nhật trạng thái thành công cho các phiếu đã chọn')
  }
  catch (err: any) {
    message.error(`Lỗi cập nhật trạng thái: ${err.message || 'Unknown error'}`)
  }
  finally {
    // ✅ TẮT loading theo đúng ids
    const rlEnd = { ...rowLoading.value }
    ids.forEach((id) => { rlEnd[id] = false })
    rowLoading.value = rlEnd

    // ✅ Rồi mới clear selection
    checkedRowKeys.value = []

    // Optional: đồng bộ server
    await fetchData()
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
}

/* ===================== Data Table ===================== */
const columns: DataTableColumns<ADVoucherResponse> = [
  { type: 'selection' },
  { title: 'Mã', key: 'code', width: 120 },
  { title: 'Tên', key: 'name', width: 180 },
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
    key: 'status',
    width: 130,
    render(row) {
      const isActive = (row.status ?? 'INACTIVE') === 'ACTIVE'
      const id = String(row.id ?? '')
      return h(
        NPopconfirm,
        {
          onPositiveClick: () => toggleOne(row),
          positiveText: 'Xác nhận',
          negativeText: 'Hủy',
        },
        {
          trigger: () =>
            h(NSwitch, {
              value: isActive,
              loading: !!rowLoading.value[id],
            }),
          default: () => `Chuyển sang ${isActive ? 'Không hoạt động' : 'Hoạt động'}?`,
        },
      )
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    width: 120,
    render(row: ADVoucherResponse) {
      const id = row.id
      return h(NSpace, { size: 8 }, () => [
        h(
          NButton,
          {
            size: 'small',
            title: 'Sửa',
            text: true,
            onClick: () => {
              if (id)
                openEditPage(id)
              else message.error('ID không tồn tại')
            },
          },
          { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) },
        ),
        h(
          NPopconfirm,
          { onPositiveClick: () => (id ? handleDeleteOne(id, row.name) : message.error('ID không tồn tại')) },
          {
            trigger: () =>
              h(
                NButton,
                { size: 'small', title: 'Xoá', text: true, type: 'error' },
                { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:trash-can' }) }) },
              ),
            default: () => 'Bạn có chắc muốn xóa?',
          },
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
      size: pagination.value.pageSize, // ✅ vẫn dùng pageSize ở trên (5)
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
    <!-- Form Thêm/Sửa -->
    <AddEditVoucher
      v-if="showAddEditForm"
      :mode="formMode"
      :voucher-id="selectedVoucherId"
      @close="closeForm"
    />

    <!-- Header -->
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

    <!-- Bộ lọc -->
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
            <NInputNumber v-model:value="filters.conditions" placeholder="Điều kiện áp dụng..." class="w-full" />
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

    <!-- Table -->
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

            <NTooltip :disabled="checkedRowKeys.length === 0">
              <template #trigger>
                <NPopconfirm @positive-click="handleDeleteMany(checkedRowKeys as string[])">
                  <template #trigger>
                    <NTooltip trigger="hover" placement="top">
                      <template #trigger>
                        <NButton
                          circle
                          secondary
                          type="error"
                          :disabled="checkedRowKeys.length === 0"
                          title="Xóa đã chọn"
                        >
                          <NIcon size="24">
                            <Icon icon="carbon:trash-can" />
                          </NIcon>
                        </NButton>
                      </template>
                      Xóa mục đã chọn
                    </NTooltip>
                  </template>
                  Bạn có chắc muốn xóa không?
                </NPopconfirm>
              </template>
              Chưa chọn phiếu giảm giá nào
            </NTooltip>

            <!-- Nút chuyển trạng thái hàng loạt -->
            <div v-if="checkedRowKeys.length > 0" class="flex items-center">
              <NPopconfirm @positive-click="handleConfirmStatusChange">
                <template #trigger>
                  <NButton type="primary" size="small">
                    Chuyển đổi trạng thái
                  </NButton>
                </template>
                Bạn có chắc muốn chuyển đổi trạng thái cho các phiếu đã chọn?
              </NPopconfirm>
            </div>
          </NSpace>
        </div>
      </template>

      <NDataTable
        v-model:checked-row-keys="checkedRowKeys"
        :columns="columns"
        :data="data"
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
          @update:page="(page) => { pagination.page = page; fetchData(); }"
          @update:page-size="(size) => { pagination.pageSize = size; fetchData(); }"
        />
      </div>
    </NCard>
  </div>
</template>
