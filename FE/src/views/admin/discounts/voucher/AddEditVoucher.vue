<script lang="ts" setup>
import { computed, h, onMounted, ref, watch } from 'vue'
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
  NSelect,
  NSpace,
  NSpin,
  NTag,
  useDialog,
  useMessage,
} from 'naive-ui'
import type { AxiosResponse } from 'axios'

// --- API IMPORTS ---
import type { ADVoucherResponse, ADVoucherUpsertPayload } from '@/service/api/admin/discount/api.voucher'
import { createVoucher, getVoucherById, getVoucherCustomers, updateVoucher } from '@/service/api/admin/discount/api.voucher'
import type { Customer, CustomerFilterParams } from '@/service/api/admin/users/customer/customer'

// ✅ IMPORT HÀM GỌI API MỚI (Đã định nghĩa trong file customer.ts)
import { getCustomersVoucher } from '@/service/api/admin/users/customer/customer'

/* ===================== Routing Setup ===================== */
const route = useRoute()
const router = useRouter()
const message = useMessage()
const dialog = useDialog()

const mode = computed(() => route.path.includes('/add') ? 'add' : 'edit')
const voucherId = computed(() => route.params.id as string | null)

/* ===================== State ===================== */
const addFormRef = ref<FormInst | null>(null)
const voucherUsersFormItemRef = ref<FormItemInst | null>(null)

const isLoadingData = ref(false)
const loading = ref(false)
const loadingCustomers = ref(false)

// Biến kiểm tra chế độ xem chi tiết
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
  remainingQuantity: null,
  createdDate: null,
  note: '',
  conditions: null,
  targetType: 'ALL_CUSTOMERS',
  quantity: null,
  voucherUsers: [],
  status: 'ACTIVE',
})

/* ====== Khối khách hàng & Filter ====== */
const customers = ref<Customer[]>([])
const checkedCustomerKeys = ref<(string | number)[]>([])
const pagination = ref({ page: 1, pageSize: 5, itemCount: 0 })
const customerFilters = ref({ keyword: '', customerStatus: null as number | null })
const customerMap = ref<Record<string, Customer>>({})
const initialAssignedCustomers = ref<Customer[]>([])

// Tìm đoạn khai báo sortState
const sortState = ref<{ columnKey: string | null, order: 'ascend' | 'descend' | false }>({
  columnKey: 'totalSpending', // 👉 Mặc định sort theo Tổng chi tiêu
  order: 'descend', // 👉 Mặc định Giảm dần (Người mua nhiều nhất lên đầu)
})

// State bộ lọc thời gian
const timeFilter = ref<'MONTH' | 'YEAR'>('MONTH') // Mặc định Năm nay
const timeOptions = [
  { label: 'Tháng này', value: 'MONTH' },
  { label: 'Năm nay', value: 'YEAR' },
]

/* ===================== Utilities ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'ALL_CUSTOMERS')

const pageTitle = computed(() => {
  if (mode.value === 'add')
    return 'Thêm Phiếu Giảm Giá'
  if (isViewOnly.value)
    return 'Chi tiết Phiếu Giảm Giá'
  return 'Sửa Phiếu Giảm Giá'
})

function formatCurrency(value: number | null | undefined) {
  if (value === null || value === undefined)
    return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

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

/* ===================== Rules ===================== */
const addVoucherRules: FormRules = {
  name: [
    { required: true, message: 'Vui lòng nhập tên phiếu', trigger: ['blur', 'input'] },
    {
      validator: (_rule, value) => {
        if (!value)
          return true
        if (value.startsWith(' '))
          return new Error('Tên không được bắt đầu bằng khoảng trắng')
        if (value.trim().length === 0)
          return new Error('Tên không được để trống')
        return true
      },
      trigger: ['input', 'blur'],
    },
  ],
  typeVoucher: { required: true, message: 'Chọn loại', trigger: ['change'] },
  discountValue: {
    required: true,
    validator: (_r, v: number | null) => {
      if (v == null)
        return new Error('Nhập giá trị')
      if (newVoucher.value.typeVoucher === 'PERCENTAGE' && (v <= 0 || v >= 100))
        return new Error('Giá trị từ 1 đến 99%')
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
  startDate: {
    type: 'number',
    required: true,
    validator: (_rule, value) => {
      if (!value)
        return new Error('Chọn ngày bắt đầu')
      if (newVoucher.value.endDate && value >= newVoucher.value.endDate)
        return new Error('Ngày bắt đầu < ngày kết thúc')
      return true
    },
    trigger: ['blur', 'change'],
  },
  endDate: {
    type: 'number',
    required: true,
    validator: (_rule, value) => {
      if (!value)
        return new Error('Chọn ngày kết thúc')
      if (newVoucher.value.startDate && value <= newVoucher.value.startDate)
        return new Error('Ngày kết thúc > ngày bắt đầu')
      return true
    },
    trigger: ['blur', 'change'],
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

// Xử lý sự kiện khi click header bảng để sort
function handleSorterChange(sorter: { columnKey: string, order: 'ascend' | 'descend' | false } | null) {
  if (!sorter) {
    sortState.value = { columnKey: null, order: false }
  }
  else {
    sortState.value = {
      columnKey: sorter.columnKey,
      order: sorter.order,
    }
  }
  pagination.value.page = 1
  fetchCustomers()
}

async function loadVoucherData() {
  if (mode.value === 'edit' && voucherId.value) {
    isLoadingData.value = true
    try {
      const res = await getVoucherById(voucherId.value)
      if (res?.data) {
        const v = res.data

        // eslint-disable-next-line no-console
        console.log('Dữ liệu API trả về:', v)

        isViewOnly.value = !!(v.startDate && v.startDate <= Date.now())

        const validType = (['PERCENTAGE', 'FIXED_AMOUNT'] as const).includes(v.typeVoucher) ? v.typeVoucher : 'PERCENTAGE'
        originalTargetType.value = v.targetType ?? 'ALL_CUSTOMERS'

        newVoucher.value = {
          ...v,
          typeVoucher: validType as 'PERCENTAGE' | 'FIXED_AMOUNT',
          targetType: v.targetType ?? 'ALL_CUSTOMERS',
          note: v.note || '',
          voucherUsers: [],
        }

        if (newVoucher.value.targetType === 'INDIVIDUAL') {
          // Chỉ load danh sách nếu cần thiết, nhưng nên gọi fetchCustomers để hiển thị lại đúng state
          await fetchCustomers()

          try {
            const customersRes: Customer[] = await getVoucherCustomers(voucherId.value, false)
            const extractedIds: string[] = []
            const loadedObjects: Customer[] = []
            customersRes.forEach((customer) => {
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
            console.error(subErr)
          }
        }
      }
    }
    catch (err) {
      message.error('Lỗi tải dữ liệu')
    }
    finally {
      isLoadingData.value = false
    }
  }
}

// ✅ HÀM FETCH CUSTOMERS ĐÃ SỬA LẠI LOGIC MAP DỮ LIỆU
async function fetchCustomers() {
  loadingCustomers.value = true
  try {
    const params: CustomerFilterParams = {
      page: pagination.value.page,
      size: pagination.value.pageSize,
      keyword: customerFilters.value.keyword.trim() || undefined,
      timeRange: timeFilter.value,
      sortField: sortState.value.columnKey || undefined,
      sortDirection: sortState.value.order ? (sortState.value.order === 'ascend' ? 'asc' : 'desc') : undefined,
    }

    const res: AxiosResponse<any, any> = await getCustomersVoucher(params)

    let data: Customer[] = []

    // 🛠️ XỬ LÝ RESPONSE: Kiểm tra đúng cấu trúc trả về từ Spring Boot (ResponseObject -> Page -> content)
    // res.data là ResponseObject
    // res.data.data là Page object
    const pageData = res.data?.data

    if (pageData && Array.isArray(pageData.content)) {
      // Trường hợp trả về Page (có content, totalElements)
      data = pageData.content
      pagination.value.itemCount = pageData.totalElements || 0
    }
    else if (Array.isArray(pageData)) {
      // Trường hợp trả về List trực tiếp
      data = pageData
      pagination.value.itemCount = data.length
    }
    else if (res.data && Array.isArray(res.data)) {
      // Trường hợp API trả về mảng ngay ở root (hiếm gặp ở project này nhưng cứ đề phòng)
      data = res.data
      pagination.value.itemCount = data.length
    }

    // Map ID thành string để NaiveUI hoạt động trơn tru
    customers.value = data.map(it => ({
      ...it,
      id: String(it.id || it.customerCode || `tmp-${Math.random()}`),
    }))

    // Cập nhật map để hiển thị ở phần "Đã chọn"
    customers.value.forEach((c) => {
      if (c.id)
        customerMap.value[String(c.id)] = c
    })
  }
  catch (err) {
    console.error('Lỗi tải danh sách khách hàng:', err)
    customers.value = []
    message.error('Không thể tải danh sách khách hàng')
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
      message.warning('Không thể bỏ chọn khách hàng cũ!')
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
    addFormRef.value?.validate(undefined, rule => rule.key === 'endDate').catch(() => { })
})
watch(() => newVoucher.value.endDate, () => {
  if (newVoucher.value.startDate)
    addFormRef.value?.validate(undefined, rule => rule.key === 'startDate').catch(() => { })
})

// Khi đổi keyword, time filter hoặc status -> reset page về 1
watch([() => customerFilters.value.keyword, timeFilter, () => customerFilters.value.customerStatus], () => {
  pagination.value.page = 1
  fetchCustomers()
})

// Khi đổi trang
watch(() => pagination.value.page, fetchCustomers)

onMounted(() => { loadVoucherData() })

/* ====== Save Logic ====== */
function handleValidateAndConfirm() {
  if (isViewOnly.value)
    return
  addFormRef.value?.validate((errors) => {
    if (!errors) {
      dialog.success({
        title: 'Xác nhận',
        content: `Bạn có chắc chắn muốn ${mode.value === 'add' ? 'thêm' : 'cập nhật'} phiếu giảm giá này?`,
        positiveText: 'Đồng ý',
        negativeText: 'Hủy',
        onPositiveClick: handleSaveVoucher,
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

    if (!res.data || !(res.data.success || res.data.isSuccess)) {
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

/* ====== Table Columns Config ====== */
const customerColumns: DataTableColumns<Customer> = [
  {
    type: 'selection',
    disabled: row => !row.id || isFixedCustomer(row.id) || isViewOnly.value,
  },
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    render: (row, index) => index + 1 + (pagination.value.page - 1) * pagination.value.pageSize,
  },
  { title: 'Mã KH', key: 'customerCode', width: 90 },

  // --- CỘT MERGE: TÊN + EMAIL + SĐT ---
  {
    title: 'Thông tin khách hàng',
    key: 'customerName',
    width: 220,
    render(row: any) {
      return h('div', { class: 'flex flex-col' }, [
        h('span', { class: 'font-semibold text-gray-800' }, row.customerName),
        h('div', { class: 'text-xs text-gray-500 mt-1 flex flex-col' }, [
          row.customerEmail ? h('span', {}, `${row.customerEmail}`) : null,
          h('span', {}, row.customerPhone || '---'),
        ]),
      ])
    },
  },

  // --- CỘT SỐ ĐƠN (Dynamic Title & Sort) ---
  {
    title: () => timeFilter.value === 'MONTH' ? 'Đơn (Tháng)' : 'Đơn (Năm)',
    key: 'totalOrders',
    width: 110,
    align: 'center',
    sorter: true,
  },

  // --- CỘT CHI TIÊU (Dynamic Title & Sort) ---
  {
    title: () => timeFilter.value === 'MONTH' ? 'Chi tiêu (Tháng)' : 'Chi tiêu (Năm)',
    key: 'totalSpending',
    width: 140,
    align: 'right',
    sorter: true,
    render: (row: any) => formatCurrency(row.totalSpending),
  },
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
            ref="addFormRef" :model="newVoucher" :rules="addVoucherRules" label-placement="top"
            :disabled="isViewOnly" :class="{ 'view-only-form': isViewOnly }"
          >
            <NFormItem label="Tên phiếu" path="name">
              <NInput v-model:value="newVoucher.name" placeholder="Nhập tên phiếu ..." />
            </NFormItem>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Loại ưu đãi" path="typeVoucher">
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
                    <NRadio
                      value="ALL_CUSTOMERS"
                      :disabled="isViewOnly || (mode === 'edit' && originalTargetType === 'INDIVIDUAL')"
                    >
                      Tất cả
                    </NRadio>
                    <NRadio value="INDIVIDUAL">
                      Cá nhân
                    </NRadio>
                  </NSpace>
                </NRadioGroup>
              </NFormItem>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <NFormItem label="Giá trị giảm" path="discountValue">
                <NInputNumber
                  v-model:value="newVoucher.discountValue" :min="0"
                  :step="newVoucher.typeVoucher === 'PERCENTAGE' ? 5 : 50000" placeholder="Nhập giá trị ..."
                >
                  <template #suffix>
                    {{ newVoucher.typeVoucher === 'PERCENTAGE' ? '%' : '₫' }}
                  </template>
                </NInputNumber>
              </NFormItem>
              <NFormItem label="Giảm tối đa" path="maxValue">
                <NInputNumber
                  v-model:value="newVoucher.maxValue" :min="0" :step="1000"
                  :disabled="isViewOnly || newVoucher.typeVoucher === 'FIXED_AMOUNT'" placeholder="Nhập tối đa ..."
                >
                  <template #suffix>
                    ₫
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
              <NFormItem ref="conditionsFormItemRef" label="Đơn hàng tối thiểu" path="conditions">
                <NInputNumber
                  v-model:value="newVoucher.conditions" :min="1" :step="10000"
                  placeholder="Điều kiện đơn hàng ..."
                />
              </NFormItem>
              <NFormItem v-if="showQuantity" ref="quantityFormItemRef" label="Số lượng phiếu" path="quantity">
                <NInputNumber v-model:value="newVoucher.quantity" :min="1" placeholder="Số lượng phát hành ..." />
              </NFormItem>
            </div>

            <NFormItem label="Ghi chú">
              <NInput v-model:value="newVoucher.note" type="textarea" placeholder="Nhập ghi chú ..." />
            </NFormItem>

            <div class="flex justify-end gap-2 mt-4">
              <NButton @click="handleCancel">
                Quay lại
              </NButton>
              <NButton v-if="!isViewOnly" type="primary" :loading="loading" @click="handleValidateAndConfirm">
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
                  v-model:value="customerFilters.keyword" placeholder="Tìm tên, mã, email..." class="flex-1"
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
</template>

<style scoped>
/* Styles cho chế độ View Only */
:deep(.view-only-form .n-input--disabled),
:deep(.view-only-form .n-input-number--disabled),
:deep(.view-only-form .n-date-picker--disabled),
:deep(.view-only-form .n-radio--disabled),
:deep(.view-only-form .n-checkbox--disabled) {
  opacity: 1 !important;
  cursor: default;
  background-color: #f9f9fa;
}

:deep(.view-only-form .n-input__input-el),
:deep(.view-only-form .n-input__textarea-el),
:deep(.view-only-form .n-date-picker-input__value) {
  color: #2c3e50 !important;
  -webkit-text-fill-color: #2c3e50 !important;
  font-weight: 500;
  cursor: default;
}

:deep(.view-only-form .n-radio--disabled .n-radio__label) {
  color: #2c3e50 !important;
  cursor: default;
}

:deep(.view-only-form .n-radio--disabled.n-radio--checked .n-radio__dot) {
  background-color: #18a058 !important;
  border-color: #18a058 !important;
  opacity: 1 !important;
}

:deep(.view-only-form .n-input--disabled .n-input__border),
:deep(.view-only-form .n-input--disabled .n-input__state-border) {
  border-color: #e0e0e0 !important;
}
</style>
