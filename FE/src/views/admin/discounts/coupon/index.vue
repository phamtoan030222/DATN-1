<!-- index.vue -->
<script lang="ts" setup>
/* ===================== Imports ===================== */
import { computed, h, onMounted, ref, watch } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton,
  NCard,
  NDatePicker,
  NIcon,
  NInput,
  NPagination,
  NPopconfirm,
  NSpace,
  NSwitch,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { createVoucher, deleteVoucher, deleteVouchers, getVoucherById, getVouchers, updateVoucher, updateVoucherStatus } from '@/service/api/admin/product/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/service/api/admin/product/api.voucher'
import formatDate from '@/utils/common.helper'
import ModalVoucher from './modal.vue' // Import modal component

/* ===================== State ===================== */
const message = useMessage()
const showAddModal = ref(false)
const checkedRowKeys = ref<(string | number)[]>([])
const loading = ref(false)
const data = ref<ADVoucherResponse[]>([])
const rowLoading = ref<Record<string, boolean>>({})
const pagination = ref({
  page: 1,
  pageSize: 5,
  itemCount: 0,
  showSizePicker: true,
})
const isEdit = ref(false)
const editingVoucher = ref<Partial<ADVoucherResponse>>({}) // Dữ liệu voucher để edit

/* ===================== Methods ===================== */
function openAddModal() {
  isEdit.value = false
  editingVoucher.value = {}
  showAddModal.value = true
}

async function openEditModal(id: string) {
  try {
    isEdit.value = true
    const res = await getVoucherById(id)
    if (res?.data) {
      editingVoucher.value = res.data
    }
    showAddModal.value = true
  }
  catch (err) {
    message.error('Không tải được dữ liệu voucher')
  }
}

async function handleSaveVoucher(voucherData: any) {
  try {
    let res
    if (isEdit.value && voucherData.id) {
      res = await updateVoucher(voucherData.id, voucherData)
      if (!res.data.success) {
        throw new Error(res.data.message || 'Cập nhật thất bại')
      }
      message.success('Cập nhật voucher thành công')
    }
    else {
      res = await createVoucher(voucherData)
      if (!res.data.success) {
        throw new Error(res.data.message || 'Thêm thất bại')
      }
      message.success('Thêm voucher thành công')
    }
    showAddModal.value = false
    isEdit.value = false
    fetchData()
  }
  catch (err) {
    const errorMessage = err.response?.data?.message || err.message || 'Thao tác thất bại'
    message.error(errorMessage)
  }
}

/* ===================== Delete ===================== */
async function handleDeleteOne(id: string, name: string) {
  try {
    await deleteVoucher(id)
    message.success(`Xoá thành công voucher: ${name}`)
    await fetchData()
  }
  catch (err) {
    message.error('Lỗi xóa voucher')
  }
}

async function handleDeleteMany(ids: string[]) {
  try {
    await deleteVouchers(ids)
    message.success('Xoá thành công')
    checkedRowKeys.value = [] // reset
    await fetchData()
  }
  catch (err) {
    message.error('Lỗi xóa voucher')
  }
}

/* ===================== Filters ===================== */
const filters = ref({
  q: '',
  conditions: null as number | null,
  dateRange: null as [number, number] | null,
  status: null as string | null,
})

function resetFilters() {
  filters.value.q = ''
  filters.value.conditions = null
  filters.value.dateRange = null
  filters.value.status = null
}

const statusOptions = [
  { label: 'Tất cả', value: '' },
  { label: 'Hoạt động', value: 'ACTIVE' },
  { label: 'Không hoạt động', value: 'INACTIVE' },
]

/* ===================== Data Table ===================== */
const columns: DataTableColumns<ADVoucherResponse> = [
  { type: 'selection' },
  { title: 'Mã', key: 'code', width: 120 },
  { title: 'Tên', key: 'name', width: 180 },
  {
    title: 'Số lượng',
    key: 'quantity',
    width: '100',
  },
  {
    title: 'Giá trị',
    key: 'discountValue',
    render(row) {
      if (row.typeVoucher === 'PERCENTAGE') {
        return `${row.discountValue}%`
      }
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.discountValue)
    },
  },

  { title: 'Tối đa', key: 'maxValue', render(row) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.maxValue)
  } },
  { title: 'Điều kiện', key: 'conditions', render(row) {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.conditions)
  } },
  {
    title: 'Ngày bắt đầu',
    key: 'startDate',
    render: row => row.startDate ? formatDate(row.startDate) : 'N/A',
  },
  {
    title: 'Ngày kết thúc',
    key: 'endDate',
    render: row => row.endDate ? formatDate(row.endDate) : 'N/A',
  },
  {
    title: 'Trạng thái',
    key: 'status',
    render(row) {
      const isActive = (row.status ?? 'INACTIVE') === 'ACTIVE'
      return h(NSwitch, {
        value: isActive,
        loading: rowLoading.value[row.id ?? ''] ?? false,
        async onUpdateValue(newValue: boolean) {
          const newStatus = newValue ? 'ACTIVE' : 'INACTIVE'
          rowLoading.value = { ...rowLoading.value, [row.id ?? '']: true }
          try {
            await updateVoucherStatus(row.id!, newStatus)
            row.status = newStatus
            message.success('Cập nhật trạng thái thành công')
          }
          catch {
            message.error('Lỗi cập nhật trạng thái')
          }
          finally {
            rowLoading.value = { ...rowLoading.value, [row.id ?? '']: false }
          }
        },
      })
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    render(row) {
      return h(NSpace, { size: 8 }, () => [
        h(
          NButton,
          {
            class: 'mr-3',
            size: 'small',
            title: 'Sửa',
            text: true,
            onClick: () => openEditModal(row.id),
          },
          { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) },
        ),
        h(
          NPopconfirm,
          { onPositiveClick: () => handleDeleteOne(row.id, row.name) },
          {
            trigger: () => h(
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
  catch {
    message.error('Lỗi tải dữ liệu')
    data.value = []
  }
  finally {
    loading.value = false
  }
}
onMounted(fetchData)

// Tự động gọi fetchData khi filters thay đổi
watch(filters, () => {
  pagination.value.page = 1
  fetchData()
}, { deep: true })
</script>

<template>
  <!-- Modal thêm/sửa voucher từ component riêng -->
  <ModalVoucher
    v-model:show="showAddModal"
    :is-edit="isEdit"
    :voucher-data="editingVoucher"
    :is-loading="loading"
    @save="handleSaveVoucher"
    @cancel="showAddModal = false"
  />

  <!-- Header -->
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

  <!-- Bộ lọc -->
  <NCard title="Bộ lọc" class="rounded-2xl shadow-md mb-4">
    <NForm label-placement="top">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-4 items-end">
        <NFormItem label="Tên hoặc mã">
          <NInput v-model:value="filters.q" placeholder="Tên hoặc Mã..." class="w-full" />
        </NFormItem>
        <NFormItem label="Điều kiện áp dụng">
          <NInputNumber v-model:value="filters.conditions" placeholder="Điều kiện áp dụng..." class="w-full" />
        </NFormItem>
        <NFormItem label="Ngày bắt đầu - ngày kết thúc">
          <NDatePicker v-model:value="filters.dateRange" type="daterange" placeholder="Khoảng ngày" class="w-full" />
        </NFormItem>
        <NFormItem label="Trạng thái">
          <NSelect v-model:value="filters.status" :options="statusOptions" placeholder="Trạng thái" class="w-full" />
        </NFormItem>

        <!-- Button đưa lên chung hàng -->
        <div class="flex justify-end items-end col-span-full mr-10">
          <NTooltip trigger="hover" placement="top">
            <template #trigger>
              <NButton circle secondary type="primary" title="Làm mới" @click="resetFilters">
                <NIcon size="30">
                  <Icon icon="carbon:rotate" />
                </NIcon>
              </NButton>
            </template>
            Làm mới bộ lọc
          </NTooltip>
        </div>
      </div>
    </NForm>
  </NCard>

  <!-- Table -->
  <NCard title="Danh sách Phiếu Giảm Giá" class="border rounded-3">
    <template #header-extra>
      <NSpace>
        <!-- Thêm -->
        <NTooltip trigger="hover" placement="top">
          <template #trigger>
            <NButton type="primary" secondary circle title="Thêm mới" @click="openAddModal">
              <NIcon size="24">
                <Icon icon="carbon:add" />
              </NIcon>
            </NButton>
          </template>
          Thêm mới
        </NTooltip>
        <!-- Refresh -->
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
        <!-- Xoá -->
        <NTooltip :disabled="checkedRowKeys.length > 0">
          <template #trigger>
            <NPopconfirm @positive-click="handleDeleteMany(checkedRowKeys as string[])">
              <template #trigger>
                <NTooltip trigger="hover" placement="top">
                  <template #trigger>
                    <NButton circle secondary type="error" :disabled="checkedRowKeys.length === 0" title="Xóa đã chọn">
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
      </NSpace>
    </template>
    <NDataTable
      v-model:checked-row-keys="checkedRowKeys" :columns="columns" :data="data" :loading="loading"
      :row-key="row => row.id" :pagination="false" bordered
    />
    <div class="flex justify-center mt-4">
      <NPagination
        :page="pagination.page" :page-size="pagination.pageSize" :item-count="pagination.itemCount"
        @update:page="page => { pagination.page = page; fetchData() }"
        @update:page-size="size => { pagination.pageSize = size; fetchData() }"
      />
    </div>
  </NCard>
</template>
