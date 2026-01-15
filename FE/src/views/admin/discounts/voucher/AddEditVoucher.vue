<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
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
  NRadio,
  NRadioGroup,
  NSpace,
  NSpin,
  NTag,
  useDialog,
  useMessage,
} from 'naive-ui'
import type { AxiosResponse } from 'axios'

import type { ADVoucherResponse, ADVoucherUpsertPayload } from '@/service/api/admin/discount/api.voucher'
import { createVoucher, getVoucherById, getVoucherCustomers, updateVoucher } from '@/service/api/admin/discount/api.voucher'
import type { Customer, CustomerFilterParams } from '@/service/api/admin/users/customer/customer'
import { getCustomers } from '@/service/api/admin/users/customer/customer'

/* ===================== Routing Setup ===================== */
const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const mode = computed(() => route.path.includes('/add') ? 'add' : 'edit')
const voucherId = computed(() => route.params.id as string | null)

/* ===================== State ===================== */
const addFormRef = ref<FormInst | null>(null)
const quantityFormItemRef = ref<FormItemInst | null>(null)
const conditionsFormItemRef = ref<FormItemInst | null>(null)
const voucherUsersFormItemRef = ref<FormItemInst | null>(null)

const isLoadingData = ref(false)
const loading = ref(false)
const loadingCustomers = ref(false)

// Biến kiểm tra chế độ xem chi tiết
const isViewOnly = ref(false)

// Biến lưu trạng thái ban đầu để kiểm tra logic chặn sửa
const originalTargetType = ref<string>('')

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
  status: 'ACTIVE',
})

/* ====== Khối khách hàng ====== */
const customers = ref<Customer[]>([])
const checkedCustomerKeys = ref<(string | number)[]>([])
const pagination = ref({ page: 1, pageSize: 5, itemCount: 0 })
const customerFilters = ref({ keyword: '', customerStatus: null as number | null })
const customerMap = ref<Record<string, Customer>>({})
const initialAssignedCustomers = ref<Customer[]>([])

/* ===================== Computed ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'ALL_CUSTOMERS')

// Tiêu đề trang linh hoạt
const pageTitle = computed(() => {
  if (mode.value === 'add')
    return 'Thêm Phiếu Giảm Giá'
  if (isViewOnly.value)
    return 'Chi tiết Phiếu Giảm Giá'
  return 'Sửa Phiếu Giảm Giá'
})

// Kiểm tra xem khách hàng này có phải là khách cũ (đã lưu DB) không
function isFixedCustomer(id: string | number) {
  if (mode.value !== 'edit')
    return false
  const idStr = String(id)
  return initialAssignedCustomers.value.some(c => String(c.id) === idStr)
}

const selectedCustomers = computed(() => {
  const ids = newVoucher.value.voucherUsers || []
  return ids.map((id) => {
    const idStr = String(id)
    return customerMap.value[idStr] || initialAssignedCustomers.value.find(c => String(c.id) === idStr)
  }).filter((c): c is Customer => !!c)
})

/* ===================== Rules (VALIDATION) ===================== */
const addVoucherRules: FormRules = {
  name: [
    { required: true, message: 'Vui lòng nhập tên phiếu', trigger: ['blur', 'input'] },
    {
      validator: (_rule, value) => {
        if (!value)
          return true // Để rule required xử lý

        // Kiểm tra khoảng trắng ở đầu
        if (value.startsWith(' ')) {
          return new Error('Tên không được bắt đầu bằng khoảng trắng')
        }

        // Kiểm tra chỉ toàn khoảng trắng (nếu cần)
        if (value.trim().length === 0) {
          return new Error('Tên không được để trống')
        }

        return true
      },
      trigger: ['input', 'blur'], // Kiểm tra ngay khi gõ
    },
  ],
  typeVoucher: { required: true, message: 'Chọn loại', trigger: ['change'] },
  discountValue: {
    required: true,
    validator: (_r, v: number | null) => {
      if (v == null)
        return new Error('Nhập giá trị')
      if (newVoucher.value.typeVoucher === 'PERCENTAGE' && (v <= 0 || v >= 100))
        return new Error('giá trị phải nằm trong khoảng từ 1 đến 99')
      if (newVoucher.value.typeVoucher === 'FIXED_AMOUNT' && v <= 0)
        return new Error('Phải lớn hơn 0')
      return true
    },
    trigger: ['blur', 'change'],
  },
  maxValue: {
    type: 'number',
    required: true,
    validator: (_r, v) => v == null ? new Error('Nhập tối đa') : true,
    trigger: ['blur', 'change'],
  },
  // --- VALIDATE NGÀY BẮT ĐẦU ---
  startDate: {
    type: 'number',
    required: true,
    trigger: ['blur', 'change'],
    validator: (_rule, value) => {
      if (!value)
        return new Error('Chọn ngày bắt đầu')
      // Nếu đã có ngày kết thúc, kiểm tra Start < End
      if (newVoucher.value.endDate && value >= newVoucher.value.endDate) {
        return new Error('Ngày bắt đầu phải nhỏ hơn ngày kết thúc')
      }
      return true
    },
  },
  // --- VALIDATE NGÀY KẾT THÚC ---
  endDate: {
    type: 'number',
    required: true,
    trigger: ['blur', 'change'],
    validator: (_rule, value) => {
      if (!value)
        return new Error('Chọn ngày kết thúc')
      // Nếu đã có ngày bắt đầu, kiểm tra End > Start
      if (newVoucher.value.startDate && value <= newVoucher.value.startDate) {
        return new Error('Ngày kết thúc phải lớn hơn ngày bắt đầu')
      }
      return true
    },
  },
  targetType: { required: true, message: 'Chọn đối tượng', trigger: ['change'] },
  quantity: [{
    required: true,
    validator: (_r, v) => newVoucher.value.targetType === 'ALL_CUSTOMERS' && (!v || v <= 0) ? new Error('Nhập số lượng') : true,
    trigger: ['blur', 'change'],
  }],
  conditions: [{
    required: true,
    validator: (_r, v) => (!v || v <= 0) ? new Error('Nhập điều kiện') : true,
    trigger: ['blur', 'change'],
  }],
  voucherUsers: [{
    required: true,
    validator: (_r, v: any[]) => newVoucher.value.targetType === 'INDIVIDUAL' && (!v || v.length === 0) ? new Error('Chọn khách hàng') : true,
    trigger: ['change'],
  }],
}

/* ===================== Methods ===================== */
function handleCancel() {
  router.push('/discounts/voucher')
}

async function loadVoucherData() {
  if (mode.value === 'edit' && voucherId.value) {
    isLoadingData.value = true
    try {
      const res = await getVoucherById(voucherId.value)
      if (res?.data) {
        const v = res.data

        const now = Date.now()
        if (v.startDate && v.startDate <= now) {
          isViewOnly.value = true
        }
        else {
          isViewOnly.value = false
        }

        const validType = (['PERCENTAGE', 'FIXED_AMOUNT'] as const).includes(v.typeVoucher) ? v.typeVoucher : 'PERCENTAGE'
        originalTargetType.value = v.targetType ?? 'ALL_CUSTOMERS'

        newVoucher.value = {
          ...v,
          typeVoucher: validType as 'PERCENTAGE' | 'FIXED_AMOUNT',
          targetType: v.targetType ?? 'ALL_CUSTOMERS',
          voucherUsers: [],
        }

        if (newVoucher.value.targetType === 'INDIVIDUAL') {
          await fetchCustomers()
          try {
            const customers: Customer[] = await getVoucherCustomers(voucherId.value, false)
            const extractedIds: string[] = []
            const loadedObjects: Customer[] = []

            customers.forEach((customer) => {
              if (customer.id) {
                extractedIds.push(customer.id)
                loadedObjects.push(customer)
              }
            })

            initialAssignedCustomers.value = loadedObjects
            newVoucher.value.voucherUsers = extractedIds
            checkedCustomerKeys.value = extractedIds
          }
          catch (subErr) {
            console.error('Lỗi lấy danh sách khách hàng:', subErr)
          }
        }
      }
    }
    catch (err) {
      message.error('Lỗi tải dữ liệu voucher')
    }
    finally {
      isLoadingData.value = false
    }
  }
}

async function fetchCustomers() {
  loadingCustomers.value = true
  try {
    const query: CustomerFilterParams = {
      page: pagination.value.page,
      size: pagination.value.pageSize,
      keyword: customerFilters.value.keyword.trim() || undefined,
      customerStatus: customerFilters.value.customerStatus ?? undefined,
    }
    const res: AxiosResponse<any, any> = await getCustomers(query)

    let data: Customer[] = []
    if (res.data?.data?.data && Array.isArray(res.data.data.data)) {
      data = res.data.data.data
      pagination.value.itemCount = res.data.data.totalElements || 0
    }
    else if (res.data?.data && Array.isArray(res.data.data)) {
      data = res.data.data
      pagination.value.itemCount = res.data.totalElements || 0
    }
    else if (Array.isArray(res.data)) {
      data = res.data
      pagination.value.itemCount = data.length
    }

    customers.value = data.map(it => ({ ...it, id: String(it.id || it.customerCode || `tmp-${Math.random()}`) }))
    customers.value.forEach((c) => {
      if (c.id)
        customerMap.value[String(c.id)] = c
    })
  }
  catch (err) {
    customers.value = []
  }
  finally {
    loadingCustomers.value = false
  }
}

function onSelectionChange(keys: (string | number)[]) {
  if (isViewOnly.value)
    return

  if (mode.value === 'edit' && initialAssignedCustomers.value.length > 0) {
    const fixedIds = initialAssignedCustomers.value.map(c => String(c.id))
    const keysStr = keys.map(String)
    const isMissingFixed = fixedIds.some(fixedId => !keysStr.includes(fixedId))

    if (isMissingFixed) {
      message.warning('Không thể bỏ chọn khách hàng đã được gán voucher!')
      const mergedKeys = Array.from(new Set([...fixedIds, ...keysStr]))
      checkedCustomerKeys.value = mergedKeys
      newVoucher.value.voucherUsers = mergedKeys
      return
    }
  }

  checkedCustomerKeys.value = keys
  newVoucher.value.voucherUsers = keys.map(String)
  voucherUsersFormItemRef.value?.restoreValidation()
}

function unselectCustomer(id: string) {
  if (isViewOnly.value)
    return

  if (isFixedCustomer(id)) {
    message.warning('Không thể huỷ khách hàng cũ')
    return
  }
  const currentKeys = checkedCustomerKeys.value.map(String)
  const nextKeys = currentKeys.filter(k => k !== String(id))
  onSelectionChange(nextKeys)
}

/* ====== Watchers ====== */
watch(() => newVoucher.value.targetType, async (val) => {
  if (isLoadingData.value)
    return

  if (val === 'INDIVIDUAL') {
    await fetchCustomers()
    checkedCustomerKeys.value = (newVoucher.value.voucherUsers ?? []) as string[]
  }
  else {
    newVoucher.value.voucherUsers = []
    checkedCustomerKeys.value = []
  }
  addFormRef.value?.restoreValidation()
})

watch(() => newVoucher.value.typeVoucher, (val) => {
  if (val === 'FIXED_AMOUNT')
    newVoucher.value.maxValue = newVoucher.value.discountValue
})
watch(() => newVoucher.value.discountValue, (val) => {
  if (newVoucher.value.typeVoucher === 'FIXED_AMOUNT')
    newVoucher.value.maxValue = val
})

watch(() => newVoucher.value.startDate, () => {
  if (newVoucher.value.endDate)
    addFormRef.value?.validate(undefined, rule => rule.key === 'endDate').catch(() => {})
})
watch(() => newVoucher.value.endDate, () => {
  if (newVoucher.value.startDate)
    addFormRef.value?.validate(undefined, rule => rule.key === 'startDate').catch(() => {})
})

watch(customerFilters, () => { pagination.value.page = 1; fetchCustomers() }, { deep: true })
watch(() => pagination.value.page, fetchCustomers)

onMounted(() => { loadVoucherData() })

/* ====== Save with Dialog ====== */
function handleValidateAndConfirm() {
  if (isViewOnly.value)
    return

  addFormRef.value?.validate((errors) => {
    if (!errors) {
      dialog.warning({
        title: 'Xác nhận',
        content: `Bạn có chắc chắn muốn ${mode.value === 'add' ? 'thêm mới' : 'cập nhật'} phiếu giảm giá này?`,
        positiveText: 'Đồng ý',
        negativeText: 'Hủy',
        onPositiveClick: () => {
          handleSaveVoucher()
        },
      })
    }
  })
}

async function handleSaveVoucher() {
  loading.value = true
  try {
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
      status: newVoucher.value.status,
    }

    if (base.targetType === 'ALL_CUSTOMERS') {
      base.quantity = Number(newVoucher.value.quantity)
    }
    else {
      base.voucherUsers = (newVoucher.value.voucherUsers ?? []).map(id => ({ customer: { id } }))
    }

    const res = mode.value === 'edit' && voucherId.value
      ? await updateVoucher(voucherId.value, base)
      : await createVoucher(base)

    const isSuccess = res.data?.success || res.data?.isSuccess
    if (!res.data || !isSuccess) {
      throw new Error(res.data?.message || 'Thất bại')
    }

    message.success('Thành công')
    handleCancel()
  }
  catch (err: any) {
    message.error(err.response?.data?.message || err.message || 'Lỗi hệ thống')
  }
  finally {
    loading.value = false
  }
}

/* ====== Table ====== */
const customerColumns: DataTableColumns<Customer> = [
  {
    type: 'selection',
    disabled: row => !row.id || isFixedCustomer(row.id) || isViewOnly.value,
  },
  { title: 'STT', key: 'stt', width: 60, render: (row, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize },
  { title: 'Mã', key: 'customerCode', width: 100 },
  { title: 'Tên', key: 'customerName', width: 180 },
  { title: 'SĐT', key: 'customerPhone', width: 120 },
  { title: 'Trạng thái', key: 'customerStatus', width: 100, render: row => row.customerStatus === 1 ? 'Hoạt Động' : 'Khóa' },
]
</script>

<template>
  <NCard :title="pageTitle" class="mt-6">
    <NSpin :show="loading || isLoadingData">
      <div class="grid grid-cols-12 gap-6">
        <div
          class="col-span-12 transition-all duration-300"
          :class="newVoucher.targetType === 'INDIVIDUAL' ? 'lg:col-span-7' : 'lg:col-start-3 lg:col-span-8'"
        >
          <NForm
            ref="addFormRef"
            :model="newVoucher"
            :rules="addVoucherRules"
            label-placement="top"
            :disabled="isViewOnly"
            :class="{ 'view-only-form': isViewOnly }"
          >
            <NFormItem label="Tên" path="name">
              <NInput v-model:value="newVoucher.name" placeholder="Nhập tên ..." />
            </NFormItem>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Loại" path="typeVoucher">
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
              <NFormItem label="Đối tượng" path="targetType">
                <NRadioGroup v-model:value="newVoucher.targetType">
                  <NSpace>
                    <NRadio
                      value="ALL_CUSTOMERS"
                      :disabled="isViewOnly || (mode === 'edit' && originalTargetType === 'INDIVIDUAL')"
                    >
                      Tất cả
                    </NRadio>
                    <NRadio value="INDIVIDUAL">
                      Riêng
                    </NRadio>
                  </NSpace>
                </NRadioGroup>
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Giá trị" path="discountValue">
                <NInputNumber
                  v-model:value="newVoucher.discountValue" :min="0"
                  :step="newVoucher.typeVoucher === 'PERCENTAGE' ? 5 : 50000" placeholder="Nhập giá trị ..."
                >
                  <template #suffix>
                    {{ newVoucher.typeVoucher === 'PERCENTAGE' ? '%' : 'VND' }}
                  </template>
                </NInputNumber>
              </NFormItem>
              <NFormItem label="Giảm tối đa" path="maxValue">
                <NInputNumber
                  v-model:value="newVoucher.maxValue" :min="0" :step="1000"
                  :disabled="isViewOnly || newVoucher.typeVoucher === 'FIXED_AMOUNT'"
                  placeholder="giá trị ..."
                >
                  <template #suffix>
                    VND
                  </template>
                </NInputNumber>
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Ngày bắt đầu" path="startDate">
                <NDatePicker
                  v-model:value="newVoucher.startDate" type="datetime" style="width: 100%"
                  placeholder="Ngày bắt đầu ..."
                />
              </NFormItem>
              <NFormItem label="Ngày kết thúc" path="endDate">
                <NDatePicker
                  v-model:value="newVoucher.endDate" type="datetime" style="width: 100%"
                  placeholder="Ngày kết thúc ..."
                />
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem ref="conditionsFormItemRef" label="Đơn tối thiểu" path="conditions">
                <NInputNumber
                  v-model:value="newVoucher.conditions" :min="1" :step="10000"
                  placeholder="Điều kiện áp dụng ..."
                />
              </NFormItem>
              <NFormItem v-if="showQuantity" ref="quantityFormItemRef" label="Số lượng" path="quantity">
                <NInputNumber v-model:value="newVoucher.quantity" :min="1" placeholder="Số lượng ..." />
              </NFormItem>
            </div>

            <NFormItem label="Ghi chú">
              <NInput v-model:value="newVoucher.note" type="textarea" placeholder="Ghi chú ..." />
            </NFormItem>

            <div class="flex justify-end gap-2 mt-2">
              <NButton @click="handleCancel">
                Quay lại
              </NButton>

              <NButton v-if="!isViewOnly" type="primary" :loading="loading" @click="handleValidateAndConfirm">
                Lưu
              </NButton>
            </div>
          </NForm>
        </div>

        <div v-if="newVoucher.targetType === 'INDIVIDUAL'" class="col-span-12 lg:col-span-5">
          <NCard title="Chọn khách hàng" size="small" class="mb-3">
            <NSpin :show="loadingCustomers">
              <NInput
                v-model:value="customerFilters.keyword"
                placeholder="Tìm tên, mã, số điện thoại"
                class="mb-2"
                :disabled="isViewOnly"
              />

              <NDataTable
                v-model:checked-row-keys="checkedCustomerKeys" :columns="customerColumns" :data="customers"
                :row-key="(row: Customer) => row.id" :pagination="false" size="small" striped
                @update:checked-row-keys="onSelectionChange"
              />
              <div class="flex justify-end mt-2">
                <NPagination
                  v-model:page="pagination.page" :page-size="pagination.pageSize"
                  :item-count="pagination.itemCount"
                  @update:page-size="(s) => { pagination.pageSize = s; pagination.page = 1 }"
                />
              </div>
            </NSpin>
          </NCard>

          <NCard title="Đã chọn" size="small">
            <div v-if="(newVoucher.voucherUsers?.length || 0) > 0" class="max-h-48 overflow-y-auto">
              <NSpace wrap>
                <NTag
                  v-for="c in selectedCustomers" :key="c.id"
                  type="success"
                  :closable="!isFixedCustomer(c.id) && !isViewOnly"
                  @close="unselectCustomer(c.id)"
                >
                  {{ c.customerName || c.id }}
                </NTag>
              </NSpace>
              <NDivider class="my-2" />
              <div class="text-xs text-gray-500 font-bold">
                SL: {{ newVoucher.voucherUsers?.length }}
              </div>
            </div>
            <div v-else class="text-gray-400 text-center py-4">
              Chưa chọn khách nào
            </div>
          </NCard>
        </div>
      </div>
    </NSpin>
  </NCard>
</template>

<style scoped>
/* --- Style cho chế độ Xem Chi Tiết (View Only) --- */

/* 1. Làm rõ text và input, loại bỏ hiệu ứng mờ */
:deep(.view-only-form .n-input--disabled),
:deep(.view-only-form .n-input-number--disabled),
:deep(.view-only-form .n-date-picker--disabled),
:deep(.view-only-form .n-radio--disabled),
:deep(.view-only-form .n-checkbox--disabled) {
  opacity: 1 !important; /* Hiển thị rõ 100% */
  cursor: default;       /* SỬA: Đổi cursor thành default (mũi tên bình thường) */
  background-color: #fafafa; /* Nền xám cực nhạt */
}

/* 2. Đổi màu chữ thành màu tối (đen/xám đậm) thay vì xám nhạt */
:deep(.view-only-form .n-input__input-el),
:deep(.view-only-form .n-input__textarea-el),
:deep(.view-only-form .n-date-picker-input__value) {
  color: #333 !important;
  -webkit-text-fill-color: #333 !important;
  font-weight: 500;
  cursor: default; /* Đảm bảo cả text cũng không hiện not-allowed */
}

/* 3. Chỉnh màu cho Radio button khi disable */
:deep(.view-only-form .n-radio--disabled .n-radio__label) {
  color: #333 !important;
  cursor: default;
}
/* Làm đậm chấm tròn đã chọn của radio */
:deep(.view-only-form .n-radio--disabled.n-radio--checked .n-radio__dot) {
  background-color: #18a058 !important; /* Giữ màu xanh */
  border-color: #18a058 !important;
  opacity: 1 !important;
}

/* 4. Viền */
:deep(.view-only-form .n-input--disabled .n-input__border),
:deep(.view-only-form .n-input--disabled .n-input__state-border) {
  border-color: #d9d9d9 !important;
}
</style>
