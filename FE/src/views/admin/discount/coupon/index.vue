<!-- eslint-disable import/no-duplicates -->
<script lang="ts" setup>
import { h, onMounted, ref } from 'vue'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NIcon, NSwitch, useMessage } from 'naive-ui'
import { EditOutlined, EyeOutlined, PlusOutlined } from '@vicons/antd'
import { getVouchers, updateVoucherStatus } from '@/api/admin/product/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/api/admin/product/api.voucher'
import formatDate from '@/utils/common.helper'

import { NCard, NDatePicker, NForm, NInput, NSelect } from 'naive-ui'

import {
  NFormItem,
  NInputNumber,
  NModal,
  NRadio,
  NRadioGroup,

} from 'naive-ui'

const showAddModal = ref(false)
const showCustomerModal = ref(false)
const message = useMessage()

// Form data
const newVoucher = ref({
  name: '',
  typeVoucher: 'PERCENTAGE',
  discountValue: null as number | null,
  maxValue: null as number | null,
  startDate: null as number | null,
  endDate: null as number | null,
  note: '',
  targetType: 'ALL_CUSTOMERS', // LIMITED_BY_CONDITION | INDIVIDUAL | ALL_CUSTOMERS
  quantity: null as number | null,
  customers: [] as string[], // dùng cho INDIVIDUAL
})

const voucherTypes = [
  { label: 'Giảm %', value: 'PERCENTAGE' },
  { label: 'Giảm tiền', value: 'FIXED_AMOUNT' },
]

const targetTypes = [
  { label: 'Giới hạn điều kiện', value: 'LIMITED_BY_CONDITION' },
  { label: 'Khách hàng riêng', value: 'INDIVIDUAL' },
  { label: 'Tất cả khách hàng', value: 'ALL_CUSTOMERS' },
]

// Computed để kiểm soát hiển thị số lượng
const showQuantity = computed(() => newVoucher.value.targetType === 'LIMITED_BY_CONDITION')
const showCustomerPicker = computed(() => newVoucher.value.targetType === 'INDIVIDUAL')

function openAddModal() {
  showAddModal.value = true
}

function handleAddVoucher() {
  console.log('Voucher mới:', newVoucher.value)
  message.success('Đã thêm phiếu giảm giá mới!')
  showAddModal.value = false
}

function handleChooseCustomers() {
  // Giả sử mở modal chọn customer
  showCustomerModal.value = true
}

const filters = ref({
  name: '',
  dateRange: null as [number, number] | null,
  status: null as string | null,
})

const statusOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Hoạt động', value: 'ACTIVE' },
  { label: 'Không hoạt động', value: 'INACTIVE' },
]

function onSearch() {
  message.error('Filters:', filters.value)
}

function onAddVoucher() {
  message.error('👉 Add voucher')
}

function onViewDetail(row: ADVoucherResponse) {
  message.error('👁 Chi tiết:', row)
}

function onEditVoucher(row: ADVoucherResponse) {
  message.error('✏️ Sửa:', row)
}

const loading = ref(false)
const data = ref<ADVoucherResponse[]>([])
const pagination = ref({
  page: 1,
  pageSize: 5,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [5, 10, 20, 50],
})

const rowLoading = ref<Record<string, boolean>>({})

const columns: DataTableColumns<ADVoucherResponse> = [
  { type: 'selection' },
  { title: 'Mã', key: 'code', width: 120 },
  { title: 'Tên', key: 'name', width: 180 },
  {
    title: 'Loại Phiếu',
    key: 'typeVoucher',
    render(row) {
      return row.typeVoucher ? (row.typeVoucher === 'PERCENTAGE' ? 'Giảm %' : 'Giảm tiền') : 'N/A'
    },
  },
  { title: 'Giá trị', key: 'discountValue' },
  { title: 'Tối đa', key: 'maxValue' },
  { title: 'Ngày bắt đầu', key: 'startDate', render: row => row.startDate ? formatDate(row.startDate) : 'N/A' },
  { title: 'Ngày kết thúc', key: 'endDate', render: row => row.endDate ? formatDate(row.endDate) : 'N/A' },
  {
    title: 'Trạng thái',
    key: 'status',
    render(row) {
      const isActive = (row.status ?? 'INACTIVE') === 'ACTIVE'
      return h(NSwitch, {
        'value': isActive,
        'loading': rowLoading.value[row.id ?? ''] ?? false,
        'onUpdate:value': async (newValue: boolean) => {
          const newStatus = newValue ? 'ACTIVE' : 'INACTIVE'
          rowLoading.value = { ...rowLoading.value, [row.id ?? '']: true }
          try {
            await updateVoucherStatus(row.id!, newStatus)
            row.status = newStatus
            message.success('Cập nhật trạng thái thành công')
          }
          catch (error) {
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
    title: 'Chức Năng',
    key: 'actions',
    render(row) {
      return [
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => onViewDetail(row),
          },
          {
            icon: () => h(NIcon, null, { default: () => h(EyeOutlined) }),
          },
        ),
        h(
          NButton,
          {
            size: 'small',
            quaternary: true,
            onClick: () => onEditVoucher(row),
          },
          {
            icon: () => h(NIcon, null, { default: () => h(EditOutlined) }),
          },
        ),
      ]
    },
  },
]

async function fetchData() {
  loading.value = true
  try {
    const query: ADVoucherQuery = {
      page: pagination.value.page,
      size: pagination.value.pageSize,
    }
    const res = await getVouchers(query)
    data.value = (res.content ?? []).map(item => ({
      ...item,
      status: item.status ?? 'INACTIVE',
    }))
    pagination.value.itemCount = res.totalElements ?? 0
  }
  catch (error) {
    message.error('Lỗi tải dữ liệu')
    data.value = []
  }
  finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <!-- Modal thêm voucher -->
  <NModal v-model:show="showAddModal" preset="card" title="Thêm Phiếu Giảm Giá" size="small" :bordered="false">
    <NForm label-placement="top">
      <NFormItem label="Tên">
        <NInput v-model:value="newVoucher.name" placeholder="Nhập tên phiếu giảm giá" />
      </NFormItem>

      <NFormItem label="Loại Phiếu">
        <NSelect v-model:value="newVoucher.typeVoucher" :options="voucherTypes" />
      </NFormItem>

      <div class="grid grid-cols-2 gap-4">
        <NFormItem label="Giá trị">
          <NInputNumber v-model:value="newVoucher.discountValue" placeholder="Nhập giá trị" :min="0" />
        </NFormItem>

        <NFormItem label="Tối đa">
          <NInputNumber v-model:value="newVoucher.maxValue" placeholder="Nhập giá trị tối đa" :min="0" />
        </NFormItem>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <NFormItem label="Ngày bắt đầu">
          <NDatePicker v-model:value="newVoucher.startDate" type="date" />
        </NFormItem>
        <NFormItem label="Ngày kết thúc">
          <NDatePicker v-model:value="newVoucher.endDate" type="date" />
        </NFormItem>
      </div>

      <NFormItem label="Ghi chú">
        <NInput v-model:value="newVoucher.note" type="textarea" placeholder="Ghi chú..." />
      </NFormItem>

      <NFormItem label="Đối tượng áp dụng">
        <NRadioGroup v-model:value="newVoucher.targetType">
          <NRadio value="LIMITED_BY_CONDITION">
            Giới hạn điều kiện
          </NRadio>
          <NRadio value="INDIVIDUAL">
            Khách hàng riêng
          </NRadio>
          <NRadio value="ALL_CUSTOMERS">
            Tất cả khách hàng
          </NRadio>
        </NRadioGroup>
      </NFormItem>

      <!-- Số lượng -->
      <NFormItem v-if="showQuantity" label="Số lượng">
        <NInputNumber v-model:value="newVoucher.quantity" :min="1" />
      </NFormItem>

      <!-- Button chọn customer -->
      <NFormItem v-if="showCustomerPicker">
        <NButton @click="handleChooseCustomers">
          Chọn khách hàng
        </NButton>
      </NFormItem>

      <div class="flex justify-end gap-2 mt-4">
        <NButton @click="showAddModal = false">
          Hủy
        </NButton>
        <NButton type="primary" @click="handleAddVoucher">
          Thêm
        </NButton>
      </div>
    </NForm>
  </NModal>

  <!-- Modal chọn khách hàng -->
  <NModal v-model:show="showCustomerModal" preset="card" title="Chọn khách hàng" size="large">
    <!-- Giả sử sau này load danh sách khách hàng ở đây -->
    <div class="p-4">
      <p>Danh sách khách hàng sẽ hiển thị ở đây...</p>
    </div>
    <div class="flex justify-end mt-4">
      <NButton @click="showCustomerModal = false">
        Đóng
      </NButton>
    </div>
  </NModal>

  <NForm>
    <NCard class="rounded-2xl shadow-md">
      <template #header>
        <div class="flex justify-between items-center">
          <span class="font-semibold">Bộ lọc tìm kiếm</span>
          <NButton type="primary" @click="openAddModal">
            <template #icon>
              <NIcon>
                <PlusOutlined />
              </NIcon>
            </template>
            Thêm Phiếu Giảm Giá
          </NButton>
        </div>
      </template>

      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
        <NInput v-model:value="filters.name" placeholder="Tên..." />
        <NDatePicker v-model:value="filters.dateRange" type="daterange" clearable placeholder="Khoảng ngày" />
        <NSelect v-model:value="filters.status" :options="statusOptions" placeholder="Trạng thái" clearable />
        <NButton type="primary" block @click="onSearch">
          Tìm kiếm
        </NButton>
      </div>
    </NCard>
  </NForm>

  <n-data-table
    :columns="columns" :data="data" :loading="loading" :pagination="pagination" remote
    @update:page="(page) => { pagination.page = page; fetchData() }"
    @update:page-size="(size) => { pagination.pageSize = size; fetchData() }"
  />
</template>
