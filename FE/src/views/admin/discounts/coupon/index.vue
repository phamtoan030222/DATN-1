<!-- eslint-disable import/no-duplicates -->
<script lang="ts" setup>
/* ===================== Imports ===================== */
import { computed, h, onMounted, ref, watch } from 'vue'
import type { DataTableColumns, FormInst, FormRules } from 'naive-ui'
import {
  NButton,
  NCard,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NInputNumber,
  NModal,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { createVoucher, deleteVoucher, deleteVouchers, getVouchers, updateVoucher, updateVoucherStatus } from '@/api/admin/product/api.voucher'
import type { ADVoucherQuery, ADVoucherResponse } from '@/api/admin/product/api.voucher'
import formatDate from '@/utils/common.helper'
import { getVoucherById } from '@/api/admin/product/api.voucher'

const props = defineProps<{
  isEdit?: boolean
  editId?: string
}>()

const emit = defineEmits(['success', 'cancel'])
/* ===================== State ===================== */
const message = useMessage()
const addFormRef = ref<FormInst | null>(null)

const showAddModal = ref(false)
const showCustomerModal = ref(false)
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
const editingId = ref<string | null>(null)

/* ===================== Voucher form ===================== */
const newVoucher = ref({
  id: '',
  code: '',
  name: '',
  typeVoucher: 'PERCENTAGE',
  discountValue: null as number | null,
  maxValue: null as number | null,
  startDate: null as number | null,
  endDate: null as number | null,
  note: '',
  targetType: 'ALL_CUSTOMERS',
  quantity: null as number | null,
  customers: [] as string[],
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

/* ===================== Computed ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'LIMITED_BY_CONDITION')
const showCustomerPicker = computed(() => newVoucher.value.targetType === 'INDIVIDUAL')

/* ===================== Validate Rules ===================== */
const addVoucherRules: FormRules = {
  name: { required: true, message: 'Vui lòng nhập tên phiếu giảm giá', trigger: ['input', 'blur'] },
  typeVoucher: { required: true, message: 'Chọn loại phiếu giảm giá', trigger: ['change'] },
  discountValue: {
    validator: (_rule, value: number) => {
      if (newVoucher.value.typeVoucher === 'PERCENTAGE') {
        if (value <= 0 || value >= 100)
          return new Error('Giá trị % phải >0 và <100')
      }
      else if (newVoucher.value.typeVoucher === 'FIXED_AMOUNT') {
        if (value <= 0)
          return new Error('Giá trị giảm phải >0 VND')
      }
      return true
    },
    trigger: ['blur', 'change'],
  },
  maxValue: {
    type: 'number',
    required: true,
    message: 'Nhập giá trị tối đa',
    trigger: ['blur', 'change'],
  },
  startDate: { type: 'number', required: true, message: 'Chọn ngày bắt đầu', trigger: ['change'] },
  endDate: { type: 'number', required: true, message: 'Chọn ngày kết thúc', trigger: ['change'] },
  targetType: { required: true, message: 'Chọn đối tượng áp dụng', trigger: ['change'] },
  quantity: [
    {
      validator: (_rule, value: number) => {
        if (newVoucher.value.targetType === 'LIMITED_BY_CONDITION') {
          if (!value || value <= 0) {
            return new Error('Vui lòng nhập số lượng hợp lệ')
          }
        }
        return true
      },
      trigger: ['blur', 'change'],
    },
  ],

}

/* ===================== Watchers ===================== */
watch(
  () => newVoucher.value.typeVoucher,
  (val) => {
    if (val === 'FIXED_AMOUNT') {
      newVoucher.value.maxValue = newVoucher.value.discountValue
    }
    else {
      newVoucher.value.maxValue = null
    }
  },
)

watch(
  () => newVoucher.value.discountValue,
  (val) => {
    if (newVoucher.value.typeVoucher === 'FIXED_AMOUNT') {
      newVoucher.value.maxValue = val
    }
  },
)

/* ===================== Methods ===================== */
function resetNewVoucher() {
  newVoucher.value = {
    id: '',
    code: '',
    name: '',
    typeVoucher: 'PERCENTAGE',
    discountValue: null,
    maxValue: null,
    startDate: null,
    endDate: null,
    note: '',
    targetType: 'ALL_CUSTOMERS',
    quantity: null,
    customers: [],
  }
  addFormRef.value?.restoreValidation()
}

function openAddModal() {
  isEdit.value = false
  editingId.value = null
  resetNewVoucher() // reset form trống
  showAddModal.value = true
}

async function openEditModal(id: string) {
  try {
    isEdit.value = true
    editingId.value = id
    const res = await getVoucherById(id)

    if (res?.data) {
      const v = res.data
      newVoucher.value = {
        id: v.id || '',
        code: v.code || '',
        name: v.name || '',
        typeVoucher: v.typeVoucher || 'PERCENTAGE',
        discountValue: v.discountValue ?? 0,
        maxValue: v.maxValue ?? 0,
        startDate: v.startDate || null,
        endDate: v.endDate || null,
        note: v.note || '',
        targetType: v.targetType || 'ALL_CUSTOMERS',
        quantity: v.quantity ?? 0,
        customers: v.customers || [],
      }
    }

    showAddModal.value = true
  }
  catch (err) {
    message.error('Không tải được dữ liệu voucher')
  }
}

function handleAddVoucher() {
  addFormRef.value?.validate(async (errors) => {
    if (!errors) {
      if (isEdit.value && editingId.value) {
        // gọi API update ở đây
        console.log('Cập nhật voucher:', editingId.value, newVoucher.value)
        message.success('Đã cập nhật phiếu giảm giá!')
      }
      else {
        // gọi API create ở đây
        console.log('Thêm voucher mới:', newVoucher.value)
        message.success('Đã thêm phiếu giảm giá mới!')
      }

      resetNewVoucher()
      showAddModal.value = false
      isEdit.value = false
      editingId.value = null
      fetchData()
    }
    else {
      message.error('Vui lòng nhập đầy đủ thông tin')
    }
  })
}

function handleChooseCustomers() {
  showCustomerModal.value = true
}

/* ===================== Delete ===================== */
const vouchers = ref<ADVoucherResponse[]>([])
async function handleDeleteOne(id: string, name: string) {
  const voucher = vouchers.value.find(v => v.id === id)
  await deleteVoucher(id)
  message.success(`Xoá thành công voucher: ${name}`)
  await fetchData()
}

async function handleDeleteMany(ids: string[]) {
  await deleteVouchers(ids)
  message.success(`Xoá thành công`)
  await fetchData()
}

/* ===================== Filters ===================== */
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
  fetchData()
}

/* ===================== Data Table ===================== */
const columns: DataTableColumns<ADVoucherResponse> = [
  { type: 'selection' },
  { title: 'Mã', key: 'code', width: 120 },
  { title: 'Tên', key: 'name', width: 180 },
  {
    title: 'Loại Phiếu',
    key: 'typeVoucher',
    render(row) {
      return row.typeVoucher === 'PERCENTAGE' ? 'Giảm %' : 'Giảm tiền'
    },
  },
  { title: 'Giá trị', key: 'discountValue' },
  { title: 'Tối đa', key: 'maxValue' },
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

/* ================= thêm + sửa =============== */
const form = ref({
  code: '',
  name: '',
  typeVoucher: 'PERCENTAGE',
  voucherScope: 'PUBLIC',
  customerEmail: '',
  decreaseUnit: 0,
  maximumReduction: 0,
  startTime: '',
  endTime: '',
  conditionOfUse: '',
})
// Nếu là sửa thì load data
onMounted(async () => {
  if (props.isEdit && props.editId) {
    const res = await getVoucherById(props.editId)
    form.value = res.data
  }
})

// Submit form
async function handleSubmit() {
  try {
    if (props.isEdit && props.editId) {
      await updateVoucher(props.editId, form.value)
      message.success('Cập nhật voucher thành công')
    }
    else {
      await createVoucher(form.value)
      message.success('Thêm voucher thành công')
    }
    emit('success') // callback về cha reload data
  }
  catch (err) {
    console.error(err)
    message.error('Thao tác thất bại')
  }
}

/* ===================== Fetch API ===================== */
async function fetchData() {
  loading.value = true
  try {
    const query: ADVoucherQuery = {
      page: pagination.value.page,
      size: pagination.value.pageSize,
      name: filters.value.name || undefined,
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
</script>

<template>
  <!-- Modal thêm voucher -->
  <NModal v-model:show="showAddModal" preset="card" title="Thêm Phiếu Giảm Giá" style="width: 500px;" :bordered="false">
    <NForm ref="addFormRef" :model="newVoucher" :rules="addVoucherRules" label-placement="top">
      <NFormItem label="Tên" path="name">
        <NInput v-model:value="newVoucher.name" placeholder="Nhập tên phiếu giảm giá" />
      </NFormItem>

      <NFormItem label="Loại Phiếu" path="typeVoucher">
        <NSelect v-model:value="newVoucher.typeVoucher" :options="voucherTypes" />
      </NFormItem>

      <div class="grid grid-cols-2 gap-4">
        <NFormItem label="Giá trị" path="discountValue">
          <NInputNumber v-model:value="newVoucher.discountValue" :min="0" :step="1000">
            <template #suffix>
              {{ newVoucher.typeVoucher === 'PERCENTAGE' ? '%' : 'VND' }}
            </template>
          </NInputNumber>
        </NFormItem>

        <NFormItem label="Tối đa" path="maxValue">
          <NInputNumber
            v-model:value="newVoucher.maxValue" :min="0" :step="1000"
            :disabled="newVoucher.typeVoucher === 'FIXED_AMOUNT'"
          >
            <template #suffix>
              VND
            </template>
          </NInputNumber>
        </NFormItem>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <NFormItem label="Ngày bắt đầu" path="startDate">
          <NDatePicker v-model:value="newVoucher.startDate" type="date" />
        </NFormItem>
        <NFormItem label="Ngày kết thúc" path="endDate">
          <NDatePicker v-model:value="newVoucher.endDate" type="date" />
        </NFormItem>
      </div>

      <NFormItem label="Ghi chú">
        <NInput v-model:value="newVoucher.note" type="textarea" placeholder="Ghi chú..." />
      </NFormItem>

      <NFormItem label="Đối tượng áp dụng" path="targetType">
        <NRadioGroup v-model:value="newVoucher.targetType">
          <NRadio v-for="opt in targetTypes" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </NRadio>
        </NRadioGroup>
      </NFormItem>

      <NFormItem v-if="showQuantity" label="Số lượng" path="quantity">
        <NInputNumber v-model:value="newVoucher.quantity" :min="1" />
      </NFormItem>

      <NFormItem v-if="showCustomerPicker">
        <NButton @click="handleChooseCustomers">
          Chọn khách hàng
        </NButton>
      </NFormItem>

      <div class="flex justify-end gap-2 mt-4">
        <NButton @click="() => { resetNewVoucher(); showAddModal = false }">
          Hủy
        </NButton>
        <NButton type="primary" @click="handleAddVoucher">
          {{ isEdit ? 'Lưu' : 'Thêm' }}
        </NButton>
      </div>
    </NForm>
  </NModal>

  <!-- Modal chọn khách hàng -->
  <NModal v-model:show="showCustomerModal" preset="card" style="width: 500px;" title="Chọn khách hàng">
    <div class="p-4">
      <p>Danh sách khách hàng sẽ hiển thị ở đây...</p>
    </div>
    <div class="flex justify-end mt-4">
      <NButton @click="showCustomerModal = false">
        Đóng
      </NButton>
    </div>
  </NModal>

  <!-- Header -->
  <NCard class="mb-3">
    <NSpace vertical :size="8">
      <NSpace :align="'center'">
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
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mt-4">
      <NInput v-model:value="filters.name" placeholder="Tên..." />
      <NDatePicker v-model:value="filters.dateRange" type="daterange" clearable placeholder="Khoảng ngày" />
      <NSelect v-model:value="filters.status" :options="statusOptions" placeholder="Trạng thái" clearable />

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
    </div>
  </NCard>

  <!-- Table -->
  <NCard title="Danh sách Phiếu Giảm Giá" class="border rounded-3">
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
