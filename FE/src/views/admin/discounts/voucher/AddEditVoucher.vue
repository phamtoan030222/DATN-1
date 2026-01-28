<script lang="ts" setup>
import { computed, h, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { DataTableColumns, FormInst, FormItemInst, FormRules } from 'naive-ui'
import {
  dateViVN,
  NButton,
  NCard,
  NConfigProvider,
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
  NSelect,
  NSpace,
  NSpin,
  NTag,
  useDialog,
  useMessage,
  viVN,
} from 'naive-ui'
import type { AxiosResponse } from 'axios'

import type { ADVoucherResponse, ADVoucherUpsertPayload } from '@/service/api/admin/discount/api.voucher'
import { createVoucher, getVoucherById, getVoucherCustomers, updateVoucher } from '@/service/api/admin/discount/api.voucher'
import type { Customer, CustomerFilterParams } from '@/service/api/admin/users/customer/customer'
import { getCustomersVoucher } from '@/service/api/admin/users/customer/customer'

/* ===================== Setup ===================== */
const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const mode = computed(() => route.path.includes('/add') ? 'add' : 'edit')
const voucherId = computed(() => route.params.id as string | null)

// State
const addFormRef = ref<FormInst | null>(null)
const isLoadingData = ref(false)
const loading = ref(false)
const loadingCustomers = ref(false)
let lastSubmissionTime = 0

const isViewOnly = ref(false)
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
  conditions: null,
  targetType: 'ALL_CUSTOMERS',
  quantity: null,
  voucherUsers: [],
  status: 'ACTIVE',
})

// Khách hàng & Filter
const customers = ref<Customer[]>([])
const checkedCustomerKeys = ref<(string | number)[]>([])
const pagination = ref({ page: 1, pageSize: 5, itemCount: 0 })
const customerFilters = ref({ keyword: '', customerStatus: null as number | null })
const timeFilter = ref<'MONTH' | 'YEAR'>('MONTH')
const timeOptions = [{ label: 'Tháng này', value: 'MONTH' }, { label: 'Năm nay', value: 'YEAR' }]
const sortState = ref<{ columnKey: string | null, order: 'ascend' | 'descend' | false }>({ columnKey: 'totalSpending', order: 'descend' })
const customerMap = ref<Record<string, Customer>>({})
const initialAssignedCustomers = ref<Customer[]>([])

/* ===================== Logic Tiền Tệ ===================== */
function displayCurrency(value: number | null | undefined): string {
  if (value === null || value === undefined)
    return ''
  return value.toLocaleString('en-US')
}

function handleCurrencyInput(value: string, field: 'discountValue' | 'maxValue' | 'conditions') {
  if (isViewOnly.value)
    return
  const rawValue = value.replace(/\D/g, '')
  let numValue = rawValue ? Number(rawValue) : null

  const LIMIT_100_MIL = 100_000_000
  const LIMIT_1_BIL = 1_000_000_000

  if (field === 'discountValue' && newVoucher.value.typeVoucher === 'FIXED_AMOUNT' && numValue && numValue >= LIMIT_100_MIL)
    numValue = LIMIT_100_MIL - 1
  if (field === 'maxValue' && numValue && numValue >= LIMIT_100_MIL)
    numValue = LIMIT_100_MIL - 1
  if (field === 'conditions' && numValue && numValue >= LIMIT_1_BIL)
    numValue = LIMIT_1_BIL - 1

  newVoucher.value[field] = numValue

  if (addFormRef.value) {
    addFormRef.value.validate(undefined, rule => rule.key === field).catch(() => { })
  }
}

/* ===================== Logic UX View Only ===================== */
function handleFormInteraction(e: MouseEvent) {
  if (isViewOnly.value) {
    const target = e.target as HTMLElement
    if (target.closest('.n-input') || target.closest('.n-radio') || target.closest('.n-date-picker') || target.closest('.n-input-number')) {
      e.stopPropagation()
      message.info('⚠️ Đang xem chi tiết (Chỉ đọc)')
    }
  }
}

/* ===================== Utilities ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'ALL_CUSTOMERS')
const pageTitle = computed(() => mode.value === 'add' ? 'Thêm Phiếu Giảm Giá' : (isViewOnly.value ? 'Chi tiết Phiếu Giảm Giá' : 'Sửa Phiếu Giảm Giá'))
function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined)
    return '0 ₫'; return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}
function isFixedCustomer(id: string | number) {
  if (mode.value !== 'edit')
    return false; const idStr = String(id); return initialAssignedCustomers.value.some(c => String(c.id) === idStr)
}
const selectedCustomers = computed(() => { const ids = newVoucher.value.voucherUsers || []; return ids.map((id) => { const idStr = String(id); return customerMap.value[idStr] || initialAssignedCustomers.value.find(c => String(c.id) === idStr) }).filter((c): c is Customer => !!c) })

/* ===================== RULES (ĐÃ SỬA LỖI LẶP) ===================== */
const addVoucherRules: FormRules = {
  // Gộp tất cả validate tên vào 1 object duy nhất
  name: [
    {
      required: true,
      trigger: ['blur', 'input'],
      validator: (_rule, value) => {
        if (!value)
          return new Error('Vui lòng nhập tên phiếu')
        if (value.trim().length === 0)
          return new Error('Không được để trống tên')
        if (value.length > 50)
          return new Error('Tên tối đa 50 ký tự')
        return true
      },
    },
  ],
  // Gộp validate giá trị giảm
  discountValue: [
    {
      required: true,
      trigger: ['blur', 'change'],
      validator: (_r, v: number | null) => {
        if (v == null)
          return new Error('Nhập giá trị giảm')
        if (v === 0)
          return new Error('Phải lớn hơn 0')
        if (newVoucher.value.typeVoucher === 'PERCENTAGE' && (v < 0 || v > 100))
          return new Error('Từ 1 - 100%')
        return true
      },
    },
  ],
  // Gộp validate tối đa
  maxValue: [
    {
      required: true,
      trigger: ['blur', 'change'],
      validator: (_r, v) => (v == null) ? new Error('Nhập giá trị giảm tối đa') : true,
    },
  ],
  // Gộp validate ngày bắt đầu (bao gồm check > hiện tại)
  startDate: [
    {
      type: 'number',
      required: true,
      trigger: ['blur', 'change'],
      validator: (_rule, value) => {
        if (!value)
          return new Error('Chọn ngày bắt đầu')
        // Chỉ check > hiện tại nếu là thêm mới
        if (mode.value === 'add' && value < Date.now())
          return new Error('Ngày bắt đầu không được trong quá khứ')
        return true
      },
    },
  ],
  // Gộp validate ngày kết thúc
  endDate: [
    {
      type: 'number',
      required: true,
      trigger: ['blur', 'change'],
      validator: (_rule, value) => {
        if (!value)
          return new Error('Chọn ngày kết thúc')
        if (newVoucher.value.startDate && value <= newVoucher.value.startDate)
          return new Error('Ngày kết thúc phải sau ngày bắt đầu')
        if (mode.value === 'add' && value < Date.now())
          return new Error('Ngày kết thúc không được trong quá khứ')
        return true
      },
    },
  ],
  // Gộp validate điều kiện
  conditions: [
    {
      required: true,
      trigger: ['blur', 'change'],
      validator: (_r, v) => (!v || v <= 0) ? new Error('Nhập điều kiện áp dụng') : true,
    },
  ],
  targetType: { required: true, message: 'Chọn đối tượng', trigger: ['change'] },
  quantity: [{ required: true, validator: (_r, v) => newVoucher.value.targetType === 'ALL_CUSTOMERS' && (!v || v <= 0) ? new Error('Nhập số lượng') : true, trigger: ['blur', 'change'] }],
  voucherUsers: [{ required: true, validator: (_r, v: any[]) => newVoucher.value.targetType === 'INDIVIDUAL' && (!v || v.length === 0) ? new Error('Chọn ít nhất 1 khách hàng') : true, trigger: ['change'] }],
}

/* ===================== Methods (CRUD) ===================== */
function handleCancel() { router.push('/discounts/voucher') }

async function loadVoucherData() {
  if (mode.value === 'edit' && voucherId.value) {
    isLoadingData.value = true
    try {
      const res = await getVoucherById(voucherId.value)
      if (res?.data) {
        const v = res.data
        isViewOnly.value = !!(v.startDate && v.startDate <= Date.now())
        const validType = (['PERCENTAGE', 'FIXED_AMOUNT'] as const).includes(v.typeVoucher) ? v.typeVoucher : 'PERCENTAGE'
        originalTargetType.value = v.targetType ?? 'ALL_CUSTOMERS'
        newVoucher.value = { ...v, typeVoucher: validType as 'PERCENTAGE' | 'FIXED_AMOUNT', targetType: v.targetType ?? 'ALL_CUSTOMERS', note: v.note || '', voucherUsers: [] }
        if (newVoucher.value.targetType === 'INDIVIDUAL') {
          await fetchCustomers()
          const customersRes: Customer[] = await getVoucherCustomers(voucherId.value, false)
          const extractedIds: string[] = []
          const loadedObjects: Customer[] = []
          customersRes.forEach((customer) => { if (customer.id) { extractedIds.push(customer.id); loadedObjects.push(customer) } })
          initialAssignedCustomers.value = loadedObjects; newVoucher.value.voucherUsers = extractedIds; checkedCustomerKeys.value = extractedIds
        }
      }
    }
    catch (err) { message.error('Lỗi tải dữ liệu') }
    finally { isLoadingData.value = false }
  }
}

async function fetchCustomers() {
  loadingCustomers.value = true; try {
    const params: CustomerFilterParams = { page: pagination.value.page, size: pagination.value.pageSize, keyword: customerFilters.value.keyword.trim() || undefined, timeRange: timeFilter.value, sortField: sortState.value.columnKey || undefined, sortDirection: sortState.value.order ? (sortState.value.order === 'ascend' ? 'asc' : 'desc') : undefined }; const res: AxiosResponse<any, any> = await getCustomersVoucher(params); let data: Customer[] = []; const pageData = res.data?.data; if (pageData && Array.isArray(pageData.content)) { data = pageData.content; pagination.value.itemCount = pageData.totalElements || 0 }
    else if (Array.isArray(pageData)) { data = pageData; pagination.value.itemCount = data.length }
    else if (res.data && Array.isArray(res.data)) { data = res.data; pagination.value.itemCount = data.length }; customers.value = data.map(it => ({ ...it, id: String(it.id || it.customerCode || `tmp-${Math.random()}`) })); customers.value.forEach((c) => {
      if (c.id)
        customerMap.value[String(c.id)] = c
    })
  }
  catch (err) { console.error(err); customers.value = []; message.error('Không thể tải danh sách khách hàng') }
  finally { loadingCustomers.value = false }
}
function handleSorterChange(sorter: { columnKey: string, order: 'ascend' | 'descend' | false } | null) {
  if (!sorter) { sortState.value = { columnKey: null, order: false } }
  else { sortState.value = { columnKey: sorter.columnKey, order: sorter.order } }; pagination.value.page = 1; fetchCustomers()
}
function onSelectionChange(keys: (string | number)[]) {
  if (isViewOnly.value)
    return; if (mode.value === 'edit' && initialAssignedCustomers.value.length > 0) { const fixedIds = initialAssignedCustomers.value.map(c => String(c.id)); const keysStr = keys.map(String); const isMissingFixed = fixedIds.some(fixedId => !keysStr.includes(fixedId)); if (isMissingFixed) { message.warning('Không thể bỏ chọn khách hàng cũ!'); const mergedKeys = Array.from(new Set([...fixedIds, ...keysStr])); checkedCustomerKeys.value = mergedKeys; newVoucher.value.voucherUsers = mergedKeys; return } }; checkedCustomerKeys.value = keys; newVoucher.value.voucherUsers = keys.map(String); voucherUsersFormItemRef.value?.restoreValidation()
}
function unselectCustomer(id: string) {
  if (isViewOnly.value)
    return; if (isFixedCustomer(id)) { message.warning('Không thể huỷ khách hàng cũ'); return }; const currentKeys = checkedCustomerKeys.value.map(String); const nextKeys = currentKeys.filter(k => k !== String(id)); onSelectionChange(nextKeys)
}

watch(() => newVoucher.value.targetType, async (val) => {
  if (isLoadingData.value)
    return; if (val === 'INDIVIDUAL') { await fetchCustomers(); checkedCustomerKeys.value = (newVoucher.value.voucherUsers ?? []) as string[] }
  else { newVoucher.value.voucherUsers = []; checkedCustomerKeys.value = [] }; addFormRef.value?.restoreValidation()
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
    addFormRef.value?.validate(undefined, rule => rule.key === 'endDate').catch(() => { })
})
watch(() => newVoucher.value.endDate, () => {
  if (newVoucher.value.startDate)
    addFormRef.value?.validate(undefined, rule => rule.key === 'startDate').catch(() => { })
})
watch([() => customerFilters.value.keyword, timeFilter, () => customerFilters.value.customerStatus], () => { pagination.value.page = 1; fetchCustomers() })
watch(() => pagination.value.page, fetchCustomers)
onMounted(() => { loadVoucherData() })

function handleValidateAndConfirm() {
  if (isViewOnly.value)
    return
  if (loading.value)
    return
  addFormRef.value?.validate((errors) => {
    if (errors)
      return
    dialog.success({ title: 'Xác nhận', content: `Bạn có chắc chắn muốn ${mode.value === 'add' ? 'thêm' : 'cập nhật'} phiếu giảm giá này?`, positiveText: 'Đồng ý', negativeText: 'Hủy', onPositiveClick: () => { handleSaveVoucher(); return true } })
  })
}

async function handleSaveVoucher() {
  const now = Date.now(); if (now - lastSubmissionTime < 1000)
    return; lastSubmissionTime = now; if (loading.value)
    return; loading.value = true
  try {
    const base: ADVoucherUpsertPayload = { name: newVoucher.value.name!, typeVoucher: newVoucher.value.typeVoucher as 'PERCENTAGE' | 'FIXED_AMOUNT', targetType: newVoucher.value.targetType as 'INDIVIDUAL' | 'ALL_CUSTOMERS', discountValue: Number(newVoucher.value.discountValue), maxValue: newVoucher.value.maxValue ?? null, conditions: newVoucher.value.conditions ?? null, startDate: Number(newVoucher.value.startDate), endDate: Number(newVoucher.value.endDate), note: newVoucher.value.note ?? null, status: newVoucher.value.status }
    if (base.targetType === 'ALL_CUSTOMERS')
      base.quantity = Number(newVoucher.value.quantity); else base.voucherUsers = (newVoucher.value.voucherUsers ?? []).map(id => ({ customer: { id } }))
    const res = mode.value === 'edit' && voucherId.value ? await updateVoucher(voucherId.value, base) : await createVoucher(base)
    if (!res.data || !(res.data.success || res.data.isSuccess))
      throw new Error(res.data?.message || 'Thất bại')
    message.success('Thành công'); handleCancel()
  }
  catch (err: any) { message.error(err.response?.data?.message || err.message || 'Lỗi hệ thống') }
  finally { loading.value = false }
}

const customerColumns: DataTableColumns<Customer> = [
  { type: 'selection', disabled: row => !row.id || isFixedCustomer(row.id) || isViewOnly.value },
  { title: 'STT', key: 'stt', width: 60, render: (row, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize },
  { title: 'Mã KH', key: 'customerCode', width: 90 },
  { title: 'Thông tin khách hàng', key: 'customerName', width: 220, render(row: any) { return h('div', { class: 'flex flex-col' }, [h('span', { class: 'font-semibold text-gray-800' }, row.customerName), h('div', { class: 'text-xs text-gray-500 mt-1 flex flex-col' }, [row.customerEmail ? h('span', {}, `${row.customerEmail}`) : null, h('span', {}, row.customerPhone || '---')])]) } },
  { title: () => timeFilter.value === 'MONTH' ? 'Đơn (Tháng)' : 'Đơn (Năm)', key: 'totalOrders', width: 110, align: 'center', sorter: true },
  { title: () => timeFilter.value === 'MONTH' ? 'Chi tiêu (Tháng)' : 'Chi tiêu (Năm)', key: 'totalSpending', width: 140, align: 'right', sorter: true, render: (row: any) => formatCurrency(row.totalSpending) },
]
</script>

<template>
  <NConfigProvider :locale="viVN" :date-locale="dateViVN">
    <NCard :title="pageTitle" class="mt-6">
      <NSpin :show="loading || isLoadingData">
        <div class="grid grid-cols-12 gap-6" @click.capture="handleFormInteraction">
          <div
            class="col-span-12 transition-all duration-300"
            :class="newVoucher.targetType === 'INDIVIDUAL' ? 'lg:col-span-7' : 'lg:col-start-3 lg:col-span-8'"
          >
            <NForm
              ref="addFormRef" :model="newVoucher" :rules="addVoucherRules" label-placement="top"
              :disabled="isViewOnly" :class="{ 'view-only-override': isViewOnly }"
            >
              <NFormItem label="Tên phiếu" path="name" class="bold-label">
                <NInput
                  v-model:value="newVoucher.name" placeholder="Nhập tên phiếu (tối đa 50 ký tự)..." maxlength="50"
                  show-count class="bold-input"
                />
              </NFormItem>

              <div class="grid grid-cols-2 gap-4">
                <NFormItem label="Loại ưu đãi" path="typeVoucher" class="bold-label">
                  <NRadioGroup v-model:value="newVoucher.typeVoucher">
                    <NSpace>
                      <NRadio value="PERCENTAGE" class="bold-input">
                        Giảm %
                      </NRadio>
                      <NRadio value="FIXED_AMOUNT" class="bold-input">
                        Giảm tiền
                      </NRadio>
                    </NSpace>
                  </NRadioGroup>
                </NFormItem>
                <NFormItem label="Đối tượng áp dụng" path="targetType" class="bold-label">
                  <NRadioGroup v-model:value="newVoucher.targetType">
                    <NSpace>
                      <NRadio
                        value="ALL_CUSTOMERS" class="bold-input"
                        :disabled="isViewOnly || (mode === 'edit' && originalTargetType === 'INDIVIDUAL')"
                      >
                        Tất cả
                      </NRadio>
                      <NRadio value="INDIVIDUAL" class="bold-input">
                        Cá nhân
                      </NRadio>
                    </NSpace>
                  </NRadioGroup>
                </NFormItem>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <NFormItem label="Giá trị giảm" path="discountValue" class="bold-label">
                  <template v-if="newVoucher.typeVoucher === 'PERCENTAGE'">
                    <NInputNumber
                      v-model:value="newVoucher.discountValue" :min="0" :max="100" placeholder="1-100%"
                      class="bold-input"
                    >
                      <template #suffix>
                        %
                      </template>
                    </NInputNumber>
                  </template>
                  <template v-else>
                    <NInput
                      :value="displayCurrency(newVoucher.discountValue)" placeholder="Nhỏ hơn 100,000,000đ"
                      class="bold-input" @input="(v) => handleCurrencyInput(v, 'discountValue')"
                    >
                      <template #suffix>
                        ₫
                      </template>
                    </NInput>
                  </template>
                </NFormItem>

                <NFormItem label="Giảm tối đa" path="maxValue" class="bold-label">
                  <NInput
                    :value="displayCurrency(newVoucher.maxValue)"
                    :disabled="isViewOnly || newVoucher.typeVoucher === 'FIXED_AMOUNT'" placeholder="Nhỏ hơn 100,000,000đ"
                    class="bold-input" @input="(v) => handleCurrencyInput(v, 'maxValue')"
                  >
                    <template #suffix>
                      ₫
                    </template>
                  </NInput>
                </NFormItem>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <NFormItem label="Ngày bắt đầu" path="startDate" class="bold-label">
                  <NDatePicker
                    v-model:value="newVoucher.startDate" type="datetime" style="width: 100%"
                    format="dd/MM/yyyy HH:mm" :actions="null" update-value-on-close placeholder="Chọn ngày bắt đầu..."
                    class="bold-input"
                  />
                </NFormItem>
                <NFormItem label="Ngày kết thúc" path="endDate" class="bold-label">
                  <NDatePicker
                    v-model:value="newVoucher.endDate" type="datetime" style="width: 100%"
                    format="dd/MM/yyyy HH:mm" :actions="null" update-value-on-close placeholder="Chọn ngày kết thúc..."
                    class="bold-input"
                  />
                </NFormItem>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <NFormItem ref="conditionsFormItemRef" label="Đơn hàng tối thiểu" path="conditions" class="bold-label">
                  <NInput
                    :value="displayCurrency(newVoucher.conditions)" placeholder="Nhỏ hơn 1,000,000,000đ" class="bold-input"
                    @input="(v) => handleCurrencyInput(v, 'conditions')"
                  >
                    <template #suffix>
                      ₫
                    </template>
                  </NInput>
                </NFormItem>
                <NFormItem
                  v-if="showQuantity" ref="quantityFormItemRef" label="Số lượng phiếu" path="quantity"
                  class="bold-label"
                >
                  <NInputNumber
                    v-model:value="newVoucher.quantity" :min="1" placeholder="Số lượng..."
                    class="bold-input"
                  />
                </NFormItem>
              </div>

              <NFormItem label="Ghi chú" class="bold-label">
                <NInput
                  v-model:value="newVoucher.note" type="textarea" placeholder="Nhập ghi chú ..."
                  class="bold-input"
                />
              </NFormItem>

              <div class="flex justify-end gap-2 mt-4">
                <NButton @click="handleCancel">
                  Quay lại
                </NButton>
                <NButton
                  v-if="!isViewOnly" type="primary" :loading="loading" :disabled="loading"
                  @click="handleValidateAndConfirm"
                >
                  Lưu dữ liệu
                </NButton>
              </div>
            </NForm>
          </div>

          <div v-if="newVoucher.targetType === 'INDIVIDUAL'" class="col-span-12 lg:col-span-5">
            <NCard title="Chọn khách hàng" size="small" class="mb-4">
              <NSpin :show="loadingCustomers">
                <div class="flex gap-2 mb-3">
                  <NInput
                    v-model:value="customerFilters.keyword" placeholder="Tìm tên, mã..." class="flex-1"
                    :disabled="isViewOnly"
                  />
                  <NSelect v-model:value="timeFilter" :options="timeOptions" class="w-32" :disabled="isViewOnly" />
                </div>
                <NDataTable
                  v-model:checked-row-keys="checkedCustomerKeys" :columns="customerColumns" :data="customers"
                  :row-key="(row: Customer) => row.id" :pagination="false" size="small" striped remote
                  @update:sorter="handleSorterChange" @update:checked-row-keys="onSelectionChange"
                />
                <div class="flex justify-end mt-3">
                  <NPagination
                    v-model:page="pagination.page" :page-size="pagination.pageSize"
                    :item-count="pagination.itemCount"
                    @update:page-size="(s) => { pagination.pageSize = s; pagination.page = 1 }"
                  />
                </div>
              </NSpin>
            </NCard>
            <NCard title="Danh sách đã chọn" size="small">
              <div v-if="(newVoucher.voucherUsers?.length || 0) > 0" class="max-h-48 overflow-y-auto">
                <NSpace wrap>
                  <NTag
                    v-for="c in selectedCustomers" :key="c.id" type="success"
                    :closable="!isFixedCustomer(c.id) && !isViewOnly" @close="unselectCustomer(c.id)"
                  >
                    {{ c.customerName || c.id }}
                  </NTag>
                </NSpace>
                <NDivider class="my-2" />
                <div class="text-xs text-gray-500 font-bold">
                  Tổng cộng: {{ newVoucher.voucherUsers?.length }} khách hàng
                </div>
              </div>
              <div v-else class="text-gray-400 text-center py-6 italic">
                Chưa chọn khách hàng nào
              </div>
            </NCard>
          </div>
        </div>
      </NSpin>
    </NCard>
  </NConfigProvider>
</template>

<style scoped>
/* 1. CSS Cho Chế độ Sửa/Thêm (BOLD HẾT) */
:deep(.bold-label .n-form-item-label) {
  font-weight: 600 !important;
  font-size: 14px !important;
  color: #000000;
}

:deep(.bold-input .n-input__input-el),
:deep(.bold-input .n-input__textarea-el),
:deep(.bold-input .n-input-number-input__input-el),
:deep(.bold-input .n-date-picker-input__value),
:deep(.bold-input .n-radio__label) {
  font-weight: 400 !important;
  color: #000000 !important;
}

/* 2. CSS OVERRIDE CHO VIEW ONLY (CHỮ THƯỜNG, NỀN TRẮNG, BỎ VIỀN) */
:deep(.view-only-override .n-input--disabled),
:deep(.view-only-override .n-input-number--disabled),
:deep(.view-only-override .n-date-picker--disabled),
:deep(.view-only-override .n-radio--disabled),
:deep(.view-only-override .n-checkbox--disabled) {
  background-color: transparent !important;
  /* Trong suốt/trắng */
  border: none !important;
  /* Bỏ viền */
  box-shadow: none !important;
  /* Bỏ bóng */
  cursor: text !important;
}

/* Chữ về dạng thường, màu đen cho dễ đọc */
:deep(.view-only-override .n-input__input-el),
:deep(.view-only-override .n-input__textarea-el),
:deep(.view-only-override .n-date-picker-input__value),
:deep(.view-only-override .n-radio__label) {
  color: #000 !important;
  -webkit-text-fill-color: #000 !important;
  font-weight: 400 !important;
  /* Reset về chữ thường */
  padding: 0 !important;
}

/* Ẩn bớt icon suffix khi view-only nếu muốn gọn */
:deep(.view-only-override .n-input__suffix) {
  color: #666 !important;
}
</style>
