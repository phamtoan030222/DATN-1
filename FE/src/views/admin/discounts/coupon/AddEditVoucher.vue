<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import type { DataTableColumns, FormInst, FormItemInst, FormRules } from 'naive-ui'
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NDivider,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSpace,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'
import type { AxiosResponse } from 'axios'

import type { ADVoucherResponse, ADVoucherUpsertPayload } from '@/service/api/admin/discount/api.voucher'
import { createVoucher, getVoucherById, getVoucherCustomers, updateVoucher } from '@/service/api/admin/discount/api.voucher'
import type { Customer, CustomerFilterParams } from '@/service/api/admin/users/customer/customer'
import { getCustomers } from '@/service/api/admin/users/customer/customer'

/* ===================== Props & Emits ===================== */
const props = defineProps<{ mode: 'add' | 'edit', voucherId: string | null }>()
const emit = defineEmits<{ close: [] }>()

/* ===================== State ===================== */
const message = useMessage()
const addFormRef = ref<FormInst | null>(null)
const quantityFormItemRef = ref<FormItemInst | null>(null)
const conditionsFormItemRef = ref<FormItemInst | null>(null)
const voucherUsersFormItemRef = ref<FormItemInst | null>(null)

const isLoadingData = ref(false)
const loading = ref(false)
const loadingCustomers = ref(false)
const showConfirm = ref(false)

const newVoucher = ref<Partial<ADVoucherResponse>>({
  id: '',
  code: '',
  name: '',
  typeVoucher: 'PERCENTAGE',
  discountValue: null,
  maxValue: null,
  startDate: null,
  endDate: null,
  remainingQuantity: null,
  createdDate: null,
  note: '',
  conditions: null,
  targetType: 'ALL_CUSTOMERS',
  quantity: null,
  voucherUsers: [],
  status: '',
})

/* ====== Khối chọn khách hàng (hiển thị bên phải) ====== */
const customers = ref<Customer[]>([])
const checkedCustomerKeys = ref<(string | number)[]>([])
const pagination = ref({ page: 1, pageSize: 5, itemCount: 0 }) // ✅ cố định 5
const customerFilters = ref({ customerName: '', customerStatus: null as number | null })
const customerMap = ref<Record<string, Customer>>({}) // để render “đã chọn”

/* ===================== Computed ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'ALL_CUSTOMERS')
const selectedCustomers = computed(() =>
  (newVoucher.value.voucherUsers ?? []).map(id => customerMap.value[id as string]).filter(Boolean),
)

/* ===================== Validation Rules ===================== */
const addVoucherRules: FormRules = {
  name: { required: true, message: 'Vui lòng nhập tên phiếu giảm giá', trigger: ['input', 'blur'] },
  typeVoucher: { required: true, message: 'Chọn loại phiếu giảm giá', trigger: ['change'] },
  discountValue: {
    required: true,
    validator: (_r, v: number | null) => {
      if (v == null)
        return new Error('Không được để trống Giá Trị!!')
      if (newVoucher.value.typeVoucher === 'PERCENTAGE') {
        if (v <= 0 || v >= 100)
          return new Error('Giá trị % phải > 0 và < 100')
      }
      else {
        if (v <= 0)
          return new Error('Giá trị giảm phải > 0 VND')
      }
      return true
    },
    trigger: ['blur', 'change'],
  },
  maxValue: {
    type: 'number',
    required: true,
    validator: (_r, v: number | null) => (v == null ? new Error('Nhập giá trị tối đa') : true),
    trigger: ['blur', 'change'],
  },
  startDate: { type: 'number', required: true, message: 'Chọn ngày bắt đầu', trigger: ['change'] },
  endDate: {
    type: 'number',
    required: true,
    validator: (_r, v: number | null) =>
      v == null || v <= (newVoucher.value.startDate || 0) ? new Error('Ngày kết thúc phải sau ngày bắt đầu') : true,
    trigger: ['change'],
  },
  targetType: { required: true, message: 'Chọn đối tượng áp dụng', trigger: ['change'] },
  quantity: [
    {
      required: true,
      validator: (_r, v: number | null) =>
        newVoucher.value.targetType === 'ALL_CUSTOMERS' && (!v || v <= 0)
          ? new Error('Vui lòng nhập số lượng hợp lệ')
          : true,
      trigger: ['blur', 'change'],
    },
  ],
  conditions: [
    {
      required: true,
      validator: (_r, v: number | null) =>
        (newVoucher.value.targetType === 'ALL_CUSTOMERS' || newVoucher.value.targetType === 'INDIVIDUAL')
        && (!v || v <= 0)
          ? new Error('Vui lòng nhập điều kiện hợp lệ')
          : true,
      trigger: ['blur', 'change'],
    },
  ],
  voucherUsers: [
    {
      required: true,
      validator: (_r, value: string[]) =>
        newVoucher.value.targetType === 'INDIVIDUAL' && (!value || value.length === 0)
          ? new Error('Vui lòng chọn ít nhất một khách hàng')
          : true,
      trigger: ['change'],
    },
  ],
}

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
    createdDate: null,
    remainingQuantity: null,
    note: '',
    conditions: null,
    targetType: 'ALL_CUSTOMERS',
    quantity: null,
    voucherUsers: [],
    status: '',
  }
  addFormRef.value?.restoreValidation()
  checkedCustomerKeys.value = []
  showConfirm.value = false
}

async function loadVoucherData() {
  if (props.mode === 'edit' && props.voucherId) {
    isLoadingData.value = true
    try {
      const res = await getVoucherById(props.voucherId)
      if (res?.data) {
        const v = res.data
        const validType = (['PERCENTAGE', 'FIXED_AMOUNT'] as const).includes(v.typeVoucher)
          ? (v.typeVoucher as 'PERCENTAGE' | 'FIXED_AMOUNT')
          : 'PERCENTAGE'
        newVoucher.value = {
          ...v,
          id: v.id ?? '',
          code: v.code ?? '',
          name: v.name ?? '',
          typeVoucher: validType,
          targetType: v.targetType ?? 'ALL_CUSTOMERS',
          discountValue: v.discountValue ?? null,
          maxValue: v.maxValue ?? null,
          quantity: v.quantity ?? null,
          remainingQuantity: v.remainingQuantity ?? null,
          startDate: v.startDate ?? null,
          endDate: v.endDate ?? null,
          createdDate: v.createdDate ?? null,
          conditions: v.conditions ?? null,
          note: v.note ?? '',
          status: v.status ?? '',
        }

        // lấy DS khách hàng đã gán cho màn sửa
        if (newVoucher.value.targetType === 'INDIVIDUAL' && props.voucherId) {
          const assigned = await getVoucherCustomers(props.voucherId, false)
          newVoucher.value.voucherUsers = assigned.map(c => c.id)
          assigned.forEach(c => (customerMap.value[c.id] = c))
          checkedCustomerKeys.value = newVoucher.value.voucherUsers as string[]
        }
      }
    }
    catch {
      message.error('Không tải được dữ liệu voucher')
    }
    finally {
      isLoadingData.value = false
    }
  }
  else {
    resetNewVoucher()
  }
}

async function fetchCustomers() {
  loadingCustomers.value = true
  try {
    const query: CustomerFilterParams = {
      page: pagination.value.page,
      size: pagination.value.pageSize, // ✅ cố định 5
      customerName: customerFilters.value.customerName.trim() || undefined,
      customerStatus: customerFilters.value.customerStatus ?? undefined,
    }
    const res: AxiosResponse<any, any> = await getCustomers(query)

    let data: Customer[] = []
    if (res.data && typeof res.data === 'object' && res.data.data && Array.isArray(res.data.data.data)) {
      data = res.data.data.data
      pagination.value.itemCount = res.data.data.totalElements || data.length
    }
    else if (Array.isArray(res.data)) {
      data = res.data
      pagination.value.itemCount = data.length
    }
    else {
      throw new TypeError('Dữ liệu phản hồi từ API không đúng định dạng')
    }

    customers.value = data.map((it: Customer) => ({
      ...it,
      id: it.id || it.customerCode || `temp-${Math.random()}`,
    }))
    customers.value.forEach(c => (customerMap.value[c.id] = c))
  }
  catch (err: any) {
    message.error(err.message || 'Lỗi tải danh sách khách hàng')
    customers.value = []
    pagination.value.itemCount = 0
  }
  finally {
    loadingCustomers.value = false
  }
}

function syncCheckedFromModel() {
  checkedCustomerKeys.value = (newVoucher.value.voucherUsers ?? []) as (string | number)[]
}

function onSelectionChange(keys: (string | number)[]) {
  checkedCustomerKeys.value = keys
  newVoucher.value.voucherUsers = keys as string[]
  voucherUsersFormItemRef.value?.restoreValidation()
}

/* ====== Lưu / Hủy ====== */
async function handleValidateAndConfirm() {
  try {
    await addFormRef.value?.validate()
    showConfirm.value = true
  }
  catch {
    message.error('Vui lòng nhập đầy đủ thông tin')
    showConfirm.value = false
  }
}

async function handleSaveVoucher() {
  loading.value = true
  try {
    // Build đúng payload gửi BE
    const base: ADVoucherUpsertPayload = {
      name: newVoucher.value.name!,
      typeVoucher: newVoucher.value.typeVoucher as 'PERCENTAGE' | 'FIXED_AMOUNT',
      targetType: newVoucher.value.targetType as 'INDIVIDUAL' | 'ALL_CUSTOMERS',
      discountValue: Number(newVoucher.value.discountValue),
      maxValue: newVoucher.value.maxValue ?? null,
      conditions: newVoucher.value.conditions ?? null,
      startDate: Number(newVoucher.value.startDate),
      endDate: Number(newVoucher.value.endDate),
      note: newVoucher.value.note ?? null,
    }

    if (base.targetType === 'ALL_CUSTOMERS') {
      base.quantity = Number(newVoucher.value.quantity)
    }
    else {
      base.voucherUsers = (newVoucher.value.voucherUsers ?? []).map(id => ({ customer: { id } }))
    }

    const res
      = props.mode === 'edit' && props.voucherId
        ? await updateVoucher(props.voucherId, base)
        : await createVoucher(base)

    if (!res.data?.success)
      throw new Error(res.data?.message || 'Thao tác thất bại')
    message.success(props.mode === 'edit' ? 'Cập nhật voucher thành công' : 'Thêm voucher thành công')
    emit('close')
  }
  catch (err: any) {
    message.error(err.response?.data?.message || err.message || 'Thao tác thất bại')
  }
  finally {
    loading.value = false
    showConfirm.value = false
  }
}

function handleCancel() {
  showConfirm.value = false
  emit('close')
}

/* ===================== Watchers & Mount ===================== */
onMounted(async () => {
  await loadVoucherData()
  if (newVoucher.value.targetType === 'INDIVIDUAL') {
    await fetchCustomers()
    syncCheckedFromModel()
  }
})

watch(() => newVoucher.value.typeVoucher, (val) => {
  if (val === 'FIXED_AMOUNT')
    newVoucher.value.maxValue = newVoucher.value.discountValue
})
watch(() => newVoucher.value.discountValue, (val) => {
  if (newVoucher.value.typeVoucher === 'FIXED_AMOUNT')
    newVoucher.value.maxValue = val
})

watch(() => newVoucher.value.targetType, async (val) => {
  if (val === 'INDIVIDUAL') {
    await fetchCustomers()
    syncCheckedFromModel()
  }
  else {
    newVoucher.value.voucherUsers = []
    checkedCustomerKeys.value = []
  }
  quantityFormItemRef.value?.restoreValidation()
  conditionsFormItemRef.value?.restoreValidation()
  voucherUsersFormItemRef.value?.restoreValidation()
  addFormRef.value?.restoreValidation()
})

watch(customerFilters, () => {
  pagination.value.page = 1
  fetchCustomers()
}, { deep: true })

watch(() => pagination.value.page, fetchCustomers)

/* ===================== Data Table Columns ===================== */
const customerColumns: DataTableColumns<Customer> = [
  { type: 'selection', disabled: (row: Customer) => !row.id },
  { title: 'Mã', key: 'customerCode', width: 120 },
  { title: 'Tên', key: 'customerName', width: 220 },
  { title: 'Email', key: 'customerEmail', width: 240 },
  { title: 'SĐT', key: 'customerPhone', width: 150 },
  {
    title: 'Trạng thái',
    key: 'customerStatus',
    width: 120,
    render: (row: Customer) => row.customerStatus === 1 ? 'Hoạt động' : 'Không hoạt động',
  },
]

function unselectCustomer(id: string) {
  const next = new Set(checkedCustomerKeys.value as string[])
  next.delete(id)
  onSelectionChange([...next])
}
</script>

<template>
  <NCard :title="props.mode === 'edit' ? 'Sửa Phiếu Giảm Giá' : 'Thêm Phiếu Giảm Giá'" class="mt-6">
    <NSpin :show="loading || isLoadingData">
      <div class="grid grid-cols-12 gap-6">
        <!-- Form -->
        <div class="col-span-12 lg:col-span-7">
          <NForm ref="addFormRef" :model="newVoucher" :rules="addVoucherRules" label-placement="top">
            <NFormItem label="Tên" path="name">
              <NInput v-model:value="newVoucher.name" placeholder="Nhập tên phiếu giảm giá" />
            </NFormItem>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Loại Phiếu" path="typeVoucher">
                <NRadioGroup v-model:value="newVoucher.typeVoucher">
                  <NSpace>
                    <NRadio value="PERCENTAGE">
                      Giảm %
                    </NRadio>
                    <NRadio value="FIXED_AMOUNT">
                      Giảm tiền
                    </NRadio>
                  </NSpace>
                </NRadioGroup>
              </NFormItem>
              <NFormItem label="Đối tượng áp dụng" path="targetType">
                <NRadioGroup v-model:value="newVoucher.targetType">
                  <NSpace>
                    <NRadio value="ALL_CUSTOMERS">
                      Tất cả KH
                    </NRadio>
                    <NRadio value="INDIVIDUAL">
                      Khách hàng riêng
                    </NRadio>
                  </NSpace>
                </NRadioGroup>
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Giá trị" path="discountValue">
                <NInputNumber v-model:value="newVoucher.discountValue" :min="0" :step="1000" placeholder="Nhập giá trị">
                  <template #suffix>
                    {{ newVoucher.typeVoucher === 'PERCENTAGE' ? '%' : 'VND' }}
                  </template>
                </NInputNumber>
              </NFormItem>
              <NFormItem label="Tối đa" path="maxValue">
                <NInputNumber v-model:value="newVoucher.maxValue" :min="0" :step="1000" :disabled="newVoucher.typeVoucher === 'FIXED_AMOUNT'" placeholder="Nhập tối đa">
                  <template #suffix>
                    VND
                  </template>
                </NInputNumber>
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Ngày bắt đầu" path="startDate">
                <NDatePicker v-model:value="newVoucher.startDate" type="datetime" placeholder="Chọn ngày bắt đầu" />
              </NFormItem>
              <NFormItem label="Ngày kết thúc" path="endDate">
                <NDatePicker v-model:value="newVoucher.endDate" type="datetime" placeholder="Chọn ngày kết thúc" />
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem ref="conditionsFormItemRef" label="Điều kiện áp dụng" path="conditions">
                <NInputNumber v-model:value="newVoucher.conditions" :min="1" placeholder="Nhập điều kiện..." />
              </NFormItem>
              <NFormItem v-if="showQuantity" ref="quantityFormItemRef" label="Số lượng" path="quantity">
                <NInputNumber v-model:value="newVoucher.quantity" :min="1" placeholder="Nhập số lượng..." />
              </NFormItem>
            </div>

            <NFormItem label="Ghi chú">
              <NInput v-model:value="newVoucher.note" type="textarea" placeholder="Ghi chú..." />
            </NFormItem>

            <div class="flex justify-end gap-2 mt-2">
              <NButton @click="handleCancel">
                Quay lại
              </NButton>
              <NPopconfirm
                :on-positive-click="handleSaveVoucher"
                positive-text="Xác nhận"
                negative-text="Hủy"
                :show="showConfirm"
                @update:show="(v) => showConfirm = v"
              >
                <template #trigger>
                  <NButton type="primary" :loading="loading" :disabled="loading" @click="handleValidateAndConfirm">
                    {{ props.mode === 'edit' ? 'Lưu' : 'Thêm' }}
                  </NButton>
                </template>
                Bạn có chắc muốn {{ props.mode === 'edit' ? 'lưu' : 'thêm' }} phiếu giảm giá này?
              </NPopconfirm>
            </div>
          </NForm>
        </div>

        <!-- Bảng chọn khách hàng (chỉ khi INDIVIDUAL) -->
        <div v-if="newVoucher.targetType === 'INDIVIDUAL'" class="col-span-12 lg:col-span-5">
          <NCard title="Chọn khách hàng" size="small" class="mb-3">
            <NSpin :show="loadingCustomers">
              <NForm :model="customerFilters" label-placement="top">
                <div class="grid grid-cols-2 gap-3">
                  <NFormItem label="Tìm theo tên">
                    <NInput v-model:value="customerFilters.customerName" placeholder="Nhập tên..." clearable />
                  </NFormItem>
                  <NFormItem label="Trạng thái">
                    <NRadioGroup v-model:value="customerFilters.customerStatus">
                      <NSpace>
                        <NRadio :value="null">
                          Tất cả
                        </NRadio>
                        <NRadio :value="1">
                          Hoạt động
                        </NRadio>
                        <NRadio :value="0">
                          Không hoạt động
                        </NRadio>
                      </NSpace>
                    </NRadioGroup>
                  </NFormItem>
                </div>
              </NForm>

              <NDataTable
                v-model:checked-row-keys="checkedCustomerKeys"
                :columns="customerColumns"
                :data="customers"
                :row-key="(row: Customer) => row.id"
                :pagination="false"
                @update:checked-row-keys="onSelectionChange"
              />
              <div class="flex justify-center mt-3">
                <NPagination
                  v-model:page="pagination.page"
                  :page-size="pagination.pageSize"
                  :item-count="pagination.itemCount"
                />
              </div>
            </NSpin>
          </NCard>

          <NCard title="Khách hàng đã chọn" size="small">
            <template v-if="(newVoucher.voucherUsers?.length || 0) > 0">
              <NSpace wrap>
                <NTag
                  v-for="c in selectedCustomers"
                  :key="c.id"
                  type="success"
                  closable
                  @close="unselectCustomer(c.id)"
                >
                  {{ c.customerName || c.id }}
                </NTag>
              </NSpace>
              <NDivider />
              <div class="text-xs text-gray-500">
                Tổng đã chọn: {{ newVoucher.voucherUsers?.length || 0 }}
              </div>
            </template>
            <template v-else>
              <div class="text-gray-500">
                Chưa chọn khách hàng nào.
              </div>
            </template>
          </NCard>
        </div>
      </div>
    </NSpin>
  </NCard>
</template>
