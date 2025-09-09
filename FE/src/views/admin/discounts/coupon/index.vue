<script lang="ts" setup>
/* ===================== Imports ===================== */
import { h, onMounted, ref, watch } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import {
  NButton,
  NCard,
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
import AddEditVoucher from './AddEditVoucher.vue' // Import component
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
  pageSize: 5,
  itemCount: 0,
  showSizePicker: true,
})
const showAddEditForm = ref(false) // Trạng thái hiển thị form
const formMode = ref<'add' | 'edit'>('add') // Chế độ form: add hoặc edit
const selectedVoucherId = ref<string | null>(null) // ID voucher đang chỉnh sửa
const selectedStatus = ref<string | null>(null) // Trạng thái mới được chọn cho các voucher

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
  fetchData() // Làm mới danh sách sau khi đóng form
}

/* ===================== Delete ===================== */
async function handleDeleteOne(id: string, name: string) {
  try {
    await deleteVoucher(id)
    message.success(`Xoá thành công voucher: ${name}`)
    await fetchData()
  }
  catch (err) {
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
  catch (err) {
    message.error(`Lỗi xóa nhiều voucher: ${err.message || 'Unknown error'}`)
  }
}

/* ===================== Toggle Status for Selected Vouchers ===================== */
async function handleConfirmStatusChange() {
  if (checkedRowKeys.value.length === 0) {
    message.warning('Vui lòng chọn ít nhất một phiếu giảm giá')
    return
  }
  if (!selectedStatus.value) {
    message.warning('Vui lòng chọn trạng thái mới')
    return
  }

  try {
    // Set loading state for all selected rows
    const newRowLoading = { ...rowLoading.value }
    checkedRowKeys.value.forEach((key) => {
      newRowLoading[key as string] = true
    })
    rowLoading.value = newRowLoading

    // Update status for each selected voucher
    const updatePromises = checkedRowKeys.value.map(async (key) => {
      const voucher = data.value.find(v => v.id === key)
      if (voucher) {
        await updateVoucherStatus(voucher.id!, selectedStatus.value!)
        voucher.status = selectedStatus.value
      }
    })

    await Promise.all(updatePromises)
    message.success('Cập nhật trạng thái thành công cho các phiếu giảm giá đã chọn')
    selectedStatus.value = null // Reset selected status after success
    checkedRowKeys.value = [] // Clear selection
  }
  catch (err) {
    message.error(`Lỗi cập nhật trạng thái: ${err.message || 'Unknown error'}`)
  }
  finally {
    // Reset loading state
    const newRowLoading = { ...rowLoading.value }
    checkedRowKeys.value.forEach((key) => {
      newRowLoading[key as string] = false
    })
    rowLoading.value = newRowLoading
    await fetchData() // Refresh data
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
  { title: 'Số lượng', key: 'quantity', width: '100' },
  {
    title: 'Giá trị',
    key: 'discountValue',
    render(row) {
      if (row.discountValue === null) {
        return 'N/A'
      }
      if (row.typeVoucher === 'PERCENTAGE') {
        return `${row.discountValue}%`
      }
      return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(row.discountValue)
    },
  },
  {
    title: 'Điều kiện',
    key: 'conditions',
    render(row) {
      if (row.conditions === null) {
        return 'N/A'
      }
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
    render(row) {
      const isActive = (row.status ?? 'INACTIVE') === 'ACTIVE'
      return h(
        NPopconfirm,
        {
          onPositiveClick: async () => {
            const newStatus = isActive ? 'INACTIVE' : 'ACTIVE'
            rowLoading.value = { ...rowLoading.value, [row.id ?? '']: true }
            try {
              await updateVoucherStatus(row.id!, newStatus)
              row.status = newStatus
              message.success('Cập nhật trạng thái thành công')
            }
            catch (err) {
              message.error(`Lỗi cập nhật trạng thái: ${err.message || 'Unknown error'}`)
            }
            finally {
              rowLoading.value = { ...rowLoading.value, [row.id ?? '']: false }
            }
          },
          positiveText: 'Xác nhận',
          negativeText: 'Hủy',
        },
        {
          trigger: () =>
            h(NSwitch, {
              value: isActive,
              loading: rowLoading.value[row.id ?? ''] ?? false,
            }),
          default: () => `Bạn có chắc muốn thay đổi trạng thái thành ${isActive ? 'Không hoạt động' : 'Hoạt động'}?`,
        },
      )
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
            onClick: () => openEditPage(row.id),
          },
          { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) },
        ),
        h(
          NPopconfirm,
          { onPositiveClick: () => handleDeleteOne(row.id, row.name) },
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
  catch (err) {
    message.error(`Lỗi tải dữ liệu: ${err.message || 'Unknown error'}`)
    data.value = []
  }
  finally {
    loading.value = false
  }
}

onMounted(() => {
  showAddEditForm.value = false // Đảm bảo form không hiển thị khi tải lại trang
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
  <!-- Form Thêm/Sửa -->
  <div :key="showAddEditForm ? 'form' : 'list'">
    <AddEditVoucher v-if="showAddEditForm" :mode="formMode" :voucher-id="selectedVoucherId" @close="closeForm" />

    <!-- Header -->
    <NCard v-if="!showAddEditForm" class="mb-3">
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
                <NRadio value="" label="Tất cả" />
                <NRadio value="ACTIVE" label="Hoạt động" />
                <NRadio value="INACTIVE" label="Không hoạt động" />
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
                          circle secondary type="error" :disabled="checkedRowKeys.length === 0"
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
            <!-- Thanh trạng thái và nút Xác nhận -->
            <div v-if="checkedRowKeys.length > 0" class="flex items-center">
              <NRadioGroup v-model:value="selectedStatus" size="small" style="margin-right: 8px;">
                <NSpace>
                  <NRadio value="ACTIVE" label="Hoạt động" />
                  <NRadio value="INACTIVE" label="Không hoạt động" />
                </NSpace>
              </NRadioGroup>
              <NPopconfirm @positive-click="handleConfirmStatusChange">
                <template #trigger>
                  <NButton type="primary" size="small" :disabled="!selectedStatus">
                    Xác nhận
                  </NButton>
                </template>
                Bạn có chắc muốn thay đổi trạng thái thành {{ selectedStatus }}?
              </NPopconfirm>
            </div>
          </NSpace>
        </div>
      </template>
      <NDataTable
        v-model:checked-row-keys="checkedRowKeys" :columns="columns" :data="data" :loading="loading"
        :row-key="(row) => row.id" :pagination="false" bordered
      />
      <div class="flex justify-center mt-4">
        <NPagination
          :page="pagination.page" :page-size="pagination.pageSize" :item-count="pagination.itemCount"
          @update:page="(page) => { pagination.page = page; fetchData(); }"
          @update:page-size="(size) => { pagination.pageSize = size; fetchData(); }"
        />
      </div>
    </NCard>
  </div>
</template>
