<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router' // Import router
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

/* ===================== Routing Setup ===================== */
const route = useRoute()
const router = useRouter()
const message = useMessage()

// Tự xác định chế độ dựa trên URL
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

/* ====== Khối khách hàng ====== */
const customers = ref<Customer[]>([])
const checkedCustomerKeys = ref<(string | number)[]>([])
const pagination = ref({ page: 1, pageSize: 5, itemCount: 0 })
const customerFilters = ref({ customerName: '', customerStatus: null as number | null })
const customerMap = ref<Record<string, Customer>>({})
const initialAssignedCustomers = ref<Customer[]>([])

/* ===================== Computed ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'ALL_CUSTOMERS')

const selectedCustomers = computed(() => {
  const ids = newVoucher.value.voucherUsers || []
  return ids.map((id) => {
    const idStr = String(id)
    return customerMap.value[idStr] || initialAssignedCustomers.value.find(c => String(c.id) === idStr)
  }).filter((c): c is Customer => !!c)
})

/* ===================== Rules ===================== */
const addVoucherRules: FormRules = {
  name: { required: true, message: 'Nhập tên phiếu', trigger: ['input', 'blur'] },
  typeVoucher: { required: true, message: 'Chọn loại', trigger: ['change'] },
  discountValue: {
    required: true,
    validator: (_r, v: number | null) => {
      if (v == null)
        return new Error('Nhập giá trị')
      if (newVoucher.value.typeVoucher === 'PERCENTAGE' && (v <= 0 || v >= 100))
        return new Error('0 < % < 100')
      if (newVoucher.value.typeVoucher === 'FIXED_AMOUNT' && v <= 0)
        return new Error('> 0')
      return true
    },
    trigger: ['blur', 'change'],
  },
  maxValue: { type: 'number', required: true, validator: (_r, v) => v == null ? new Error('Nhập tối đa') : true, trigger: ['blur', 'change'] },
  startDate: { type: 'number', required: true, message: 'Chọn ngày bắt đầu', trigger: ['change'] },
  endDate: { type: 'number', required: true, validator: (_r, v) => v == null || v <= (newVoucher.value.startDate || 0) ? new Error('Ngày kết thúc sai') : true, trigger: ['change'] },
  targetType: { required: true, message: 'Chọn đối tượng', trigger: ['change'] },
  quantity: [{ required: true, validator: (_r, v) => newVoucher.value.targetType === 'ALL_CUSTOMERS' && (!v || v <= 0) ? new Error('Nhập SL') : true, trigger: ['blur', 'change'] }],
  conditions: [{ required: true, validator: (_r, v) => (!v || v <= 0) ? new Error('Nhập điều kiện') : true, trigger: ['blur', 'change'] }],
  voucherUsers: [{ required: true, validator: (_r, v: any[]) => newVoucher.value.targetType === 'INDIVIDUAL' && (!v || v.length === 0) ? new Error('Chọn khách hàng') : true, trigger: ['change'] }],
}

/* ===================== Methods ===================== */
function handleCancel() {
  // Quay về trang danh sách
  router.push('/discounts/voucher')
}

async function loadVoucherData() {
  if (mode.value === 'edit' && voucherId.value) {
    isLoadingData.value = true
    try {
      // 1. Lấy thông tin Voucher
      const res = await getVoucherById(voucherId.value)
      if (res?.data) {
        const v = res.data
        const validType = (['PERCENTAGE', 'FIXED_AMOUNT'] as const).includes(v.typeVoucher) ? v.typeVoucher : 'PERCENTAGE'

        newVoucher.value = {
          ...v,
          typeVoucher: validType as 'PERCENTAGE' | 'FIXED_AMOUNT',
          targetType: v.targetType ?? 'ALL_CUSTOMERS',
          voucherUsers: [],
        }

        // 2. Nếu là INDIVIDUAL -> Lấy danh sách khách
        if (newVoucher.value.targetType === 'INDIVIDUAL') {
          // Load danh sách khách hàng để hiển thị bảng chọn
          await fetchCustomers()

          try {
            // API đã được chuẩn hoá → trả về Customer[]
            const customers: Customer[] = await getVoucherCustomers(voucherId.value, false)

            const extractedIds: string[] = []
            const loadedObjects: Customer[] = []

            customers.forEach((customer) => {
              if (customer.id) {
                extractedIds.push(customer.id)
                loadedObjects.push(customer)
              }
            })

            // Gán state cho form sửa
            initialAssignedCustomers.value = loadedObjects
            newVoucher.value.voucherUsers = extractedIds
            checkedCustomerKeys.value = extractedIds
          }
          catch (subErr) {
            console.error('Lỗi lấy danh sách khách hàng:', subErr)
            // Không chặn form, cho phép người dùng tiếp tục sửa voucher
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
      customerName: customerFilters.value.customerName.trim() || undefined,
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
  checkedCustomerKeys.value = keys
  newVoucher.value.voucherUsers = keys.map(String)
  voucherUsersFormItemRef.value?.restoreValidation()
}

function unselectCustomer(id: string) {
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

watch(customerFilters, () => { pagination.value.page = 1; fetchCustomers() }, { deep: true })
watch(() => pagination.value.page, fetchCustomers)

onMounted(() => { loadVoucherData() })

/* ====== Save ====== */
async function handleValidateAndConfirm() {
  try {
    await addFormRef.value?.validate()
    showConfirm.value = true
  }
  catch {
    message.error('Vui lòng nhập đủ thông tin')
  }
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

    if (!res.data?.success)
      throw new Error(res.data?.message || 'Thất bại')

    message.success('Thành công')
    handleCancel() // Quay về trang danh sách
  }
  catch (err: any) {
    message.error(err.response?.data?.message || err.message || 'Lỗi hệ thống')
  }
  finally {
    loading.value = false
    showConfirm.value = false
  }
}

/* ====== Table ====== */
const customerColumns: DataTableColumns<Customer> = [
  { type: 'selection', disabled: row => !row.id },
  { title: 'Mã', key: 'customerCode', width: 100 },
  { title: 'Tên', key: 'customerName', width: 180 },
  { title: 'SĐT', key: 'customerPhone', width: 120 },
  { title: 'Trạng thái', key: 'customerStatus', width: 100, render: row => row.customerStatus === 1 ? 'Hoạt Động' : 'Khóa' },
]
</script>

<template>
  <NCard :title="mode === 'edit' ? 'Sửa Phiếu Giảm Giá' : 'Thêm Phiếu Giảm giá'" class="mt-6">
    <NSpin :show="loading || isLoadingData">
      <div class="grid grid-cols-12 gap-6">
        <div class="col-span-12 lg:col-span-7">
          <NForm ref="addFormRef" :model="newVoucher" :rules="addVoucherRules" label-placement="top">
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
                    <NRadio value="ALL_CUSTOMERS">
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
                <NInputNumber v-model:value="newVoucher.discountValue" :min="0" :step="1000" placeholder="Nhập giá trị ...">
                  <template #suffix>
                    {{ newVoucher.typeVoucher === 'PERCENTAGE' ? '%' : 'VND' }}
                  </template>
                </NInputNumber>
              </NFormItem>
              <NFormItem label="Tối đa" path="maxValue">
                <NInputNumber v-model:value="newVoucher.maxValue" :min="0" :step="1000" :disabled="newVoucher.typeVoucher === 'FIXED_AMOUNT'" placeholder="giá trị ...">
                  <template #suffix>
                    VND
                  </template>
                </NInputNumber>
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Ngày bắt đầu" path="startDate">
                <NDatePicker v-model:value="newVoucher.startDate" type="datetime" style="width: 100%" placeholder="Ngày bắt đầu ..." />
              </NFormItem>
              <NFormItem label="Ngày kết thúc" path="endDate">
                <NDatePicker v-model:value="newVoucher.endDate" type="datetime" style="width: 100%" placeholder="Ngày kết thúc ..." />
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem ref="conditionsFormItemRef" label="Đơn tối thiểu" path="conditions">
                <NInputNumber v-model:value="newVoucher.conditions" :min="1" :step="10000" placeholder="Điều kiện áp dụng ..." />
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
              <NPopconfirm :show="showConfirm" :on-positive-click="handleSaveVoucher" @update:show="(v) => showConfirm = v">
                <template #trigger>
                  <NButton type="primary" :loading="loading" @click="handleValidateAndConfirm">
                    Lưu
                  </NButton>
                </template>
                Xác nhận lưu?
              </NPopconfirm>
            </div>
          </NForm>
        </div>

        <div v-if="newVoucher.targetType === 'INDIVIDUAL'" class="col-span-12 lg:col-span-5">
          <NCard title="Chọn khách hàng" size="small" class="mb-3">
            <NSpin :show="loadingCustomers">
              <NInput v-model:value="customerFilters.customerName" placeholder="Tìm tên..." class="mb-2" />

              <NDataTable
                v-model:checked-row-keys="checkedCustomerKeys"
                :columns="customerColumns"
                :data="customers"
                :row-key="(row: Customer) => row.id"
                :pagination="false"
                size="small"
                @update:checked-row-keys="onSelectionChange"
              />
              <div class="flex justify-center mt-2">
                <NPagination
                  v-model:page="pagination.page"
                  :page-size="pagination.pageSize"
                  :item-count="pagination.itemCount"
                  @update:page-size="(s) => { pagination.pageSize = s; pagination.page = 1 }"
                />
              </div>
            </NSpin>
          </NCard>

          <NCard title="Đã chọn" size="small">
            <div v-if="(newVoucher.voucherUsers?.length || 0) > 0" class="max-h-48 overflow-y-auto">
              <NSpace wrap>
                <NTag v-for="c in selectedCustomers" :key="c.id" type="success" closable @close="unselectCustomer(c.id)">
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
