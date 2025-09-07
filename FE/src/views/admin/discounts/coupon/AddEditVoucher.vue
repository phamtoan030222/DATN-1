<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue'
import type { DataTableColumns, FormInst, FormItemInst, FormRules } from 'naive-ui'
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NPagination,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  NSpin,
  useMessage,
} from 'naive-ui'
import type { AxiosResponse } from 'axios'
import type { ADVoucherResponse } from '@/service/api/admin/discount/api.voucher'
import { createVoucher, getVoucherById, updateVoucher } from '@/service/api/admin/discount/api.voucher'
import type { Customer, CustomerFilterParams } from '@/service/api/admin/users/customer'
import { getCustomers } from '@/service/api/admin/users/customer'

/* ===================== Props & Emits ===================== */
const props = defineProps<{
  mode: 'add' | 'edit'
  voucherId: string | null
}>()
const emit = defineEmits<{
  close: []
}>()

/* ===================== State ===================== */
const message = useMessage()
const addFormRef = ref<FormInst | null>(null)
const quantityFormItemRef = ref<FormItemInst | null>(null)
const conditionsFormItemRef = ref<FormItemInst | null>(null)
const voucherUsersFormItemRef = ref<FormItemInst | null>(null)
const showCustomerModal = ref(false)
const isLoadingData = ref(false)
const loading = ref(false)
const loadingvoucherUsers = ref(false)
const showConfirm = ref(false) // New state to control confirmation dialog

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

const voucherTypes = [
  { label: 'Giảm %', value: 'PERCENTAGE' },
  { label: 'Giảm tiền', value: 'FIXED_AMOUNT' },
]
const targetTypes = [
  { label: 'Tất cả khách hàng', value: 'ALL_CUSTOMERS' },
  { label: 'Khách hàng riêng', value: 'INDIVIDUAL' },
]

const datavoucherUsers = ref<Customer[]>([])
const checkedCustomerKeys = ref<(string | number)[]>([])
const paginationvoucherUsers = ref({
  page: 1,
  pageSize: 10,
  itemCount: 0,
})
const customerFilters = ref({
  customerName: '',
  customerStatus: null as number | null,
})

const customerStatusOptions = [
  { label: 'Tất cả', value: null },
  { label: 'Hoạt động', value: 1 },
  { label: 'Không hoạt động', value: 0 },
]

/* ===================== Computed ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'ALL_CUSTOMERS')

/* ===================== Validation Rules ===================== */
const addVoucherRules: FormRules = {
  name: { required: true, message: 'Vui lòng nhập tên phiếu giảm giá', trigger: ['input', 'blur'] },
  typeVoucher: { required: true, message: 'Chọn loại phiếu giảm giá', trigger: ['change'] },
  discountValue: {
    required: true,
    validator: (_rule, value: number | null) => {
      if (value === null) {
        return new Error('Không được để trống Giá Trị!!')
      }
      if (newVoucher.value.typeVoucher === 'PERCENTAGE') {
        if (value <= 0 || value >= 100) {
          return new Error('Giá trị % phải > 0 và < 100')
        }
      }
      else if (newVoucher.value.typeVoucher === 'FIXED_AMOUNT') {
        if (value <= 0) {
          return new Error('Giá trị giảm phải >0 VND')
        }
      }
      return true
    },
    trigger: ['blur', 'change'],
  },
  maxValue: {
    type: 'number',
    required: true,
    validator: (_rule, value: number | null) => {
      if (value === null) {
        return new Error('Nhập giá trị tối đa')
      }
      return true
    },
    trigger: ['blur', 'change'],
  },
  startDate: { type: 'number', required: true, message: 'Chọn ngày bắt đầu', trigger: ['change'] },
  endDate: {
    type: 'number',
    required: true,
    validator: (_rule, value: number | null) => {
      if (value === null || value <= (newVoucher.value.startDate || 0)) {
        return new Error('Ngày kết thúc phải sau ngày bắt đầu')
      }
      return true
    },
    trigger: ['change'],
  },
  targetType: { required: true, message: 'Chọn đối tượng áp dụng', trigger: ['change'] },
  quantity: [
    {
      required: true,
      validator: (_rule, value: number | null) => {
        if (newVoucher.value.targetType === 'ALL_CUSTOMERS') {
          if (value === null || value <= 0) {
            return new Error('Vui lòng nhập số lượng hợp lệ')
          }
        }
        return true
      },
      trigger: ['blur', 'change'],
    },
  ],
  conditions: [
    {
      required: true,
      validator: (_rule, value: number | null) => {
        if (newVoucher.value.targetType === 'ALL_CUSTOMERS' || newVoucher.value.targetType === 'INDIVIDUAL') {
          if (value === null || value <= 0) {
            return new Error('Vui lòng nhập điều kiện hợp lệ')
          }
        }
        return true
      },
      trigger: ['blur', 'change'],
    },
  ],
  voucherUsers: [
    {
      required: true,
      validator: (_rule, value: string[]) => {
        if (newVoucher.value.targetType === 'INDIVIDUAL') {
          if (!value || value.length === 0) {
            return new Error('Vui lòng chọn ít nhất một khách hàng')
          }
        }
        return true
      },
      trigger: ['input', 'change'],
    },
  ],
}

/* ===================== Methods ===================== */
async function loadVoucherData() {
  if (props.mode === 'edit' && props.voucherId) {
    isLoadingData.value = true
    try {
      const res = await getVoucherById(props.voucherId)
      if (res?.data) {
        const validTypeVoucher = (['PERCENTAGE', 'FIXED_AMOUNT'] as const).includes(res.data.typeVoucher)
          ? res.data.typeVoucher as 'PERCENTAGE' | 'FIXED_AMOUNT'
          : 'PERCENTAGE'

        newVoucher.value = {
          ...res.data,
          id: res.data?.id ?? '',
          code: res.data?.code ?? '',
          name: res.data?.name ?? '',
          typeVoucher: validTypeVoucher,
          targetType: res.data?.targetType ?? 'ALL_CUSTOMERS',
          discountValue: res.data?.discountValue ?? null,
          maxValue: res.data?.maxValue ?? null,
          quantity: res.data?.quantity ?? null,
          remainingQuantity: res.data?.remainingQuantity ?? null,
          startDate: res.data?.startDate ?? null,
          endDate: res.data?.endDate ?? null,
          createdDate: res.data?.createdDate ?? null,
          conditions: res.data?.conditions ?? null,
          note: res.data?.note ?? '',
          status: res.data?.status ?? '',
          voucherUsers: res.data.voucherUsers
            ? res.data.voucherUsers.map((detail: any) => detail.customerId || detail.id || '')
            : [],
        }
        checkedCustomerKeys.value = newVoucher.value.voucherUsers?.filter(id =>
          datavoucherUsers.value.some(user => user.id === id),
        ) || []
      }
    }
    catch (err: any) {
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
  showConfirm.value = false // Reset confirmation state
}

async function handleValidateAndConfirm() {
  try {
    await addFormRef.value?.validate()
    showConfirm.value = true // Show confirmation dialog after successful validation
  }
  catch (errors) {
    message.error('Vui lòng nhập đầy đủ thông tin')
    showConfirm.value = false // Ensure confirmation dialog is not shown on validation failure
  }
}

async function handleSaveVoucher() {
  loading.value = true
  try {
    const payload: Partial<ADVoucherResponse> = {
      ...newVoucher.value,
      voucherUsers: newVoucher.value.voucherUsers?.map(id => ({
        customer: { id },
      })) || [],
    }
    let res
    if (props.mode === 'edit' && props.voucherId) {
      res = await updateVoucher(props.voucherId, payload)
      if (!res.data.success) {
        throw new Error(res.data.message || 'Cập nhật thất bại')
      }
      message.success('Cập nhật voucher thành công')
    }
    else {
      res = await createVoucher(payload)
      if (!res.data.success) {
        throw new Error(res.data.message || 'Thêm thất bại')
      }
      message.success('Thêm voucher thành công')
    }
    emit('close')
  }
  catch (err: any) {
    const errorMessage = err.response?.data?.message || err.message || 'Thao tác thất bại'
    message.error(errorMessage)
  }
  finally {
    loading.value = false
    showConfirm.value = false // Reset confirmation state
  }
}

function handleCancel() {
  showConfirm.value = false // Reset confirmation state
  emit('close')
}

async function fetchvoucherUsers() {
  loadingvoucherUsers.value = true
  try {
    const query: CustomerFilterParams = {
      page: paginationvoucherUsers.value.page,
      size: paginationvoucherUsers.value.pageSize,
      customerName: customerFilters.value.customerName.trim() || undefined,
      customerStatus: customerFilters.value.customerStatus ?? undefined,
    }
    const res: AxiosResponse<any, any> = await getCustomers(query)
    let customerData: Customer[] = []
    if (res.data && typeof res.data === 'object' && res.data.data && Array.isArray(res.data.data.data)) {
      customerData = res.data.data.data
      paginationvoucherUsers.value.itemCount = res.data.data.totalElements || customerData.length
    }
    else if (Array.isArray(res.data)) {
      customerData = res.data
      paginationvoucherUsers.value.itemCount = customerData.length
    }
    else {
      throw new TypeError(`Dữ liệu phản hồi từ API không phải là mảng: ${JSON.stringify(res.data)}`)
    }

    datavoucherUsers.value = customerData.map((item: Customer) => ({
      ...item,
      id: item.id || item.customerCode || `temp-${Math.random()}`,
      customerName: item.customerName || 'N/A',
      customerEmail: item.customerEmail || 'N/A',
      customerPhone: item.customerPhone || 'N/A',
      customerCode: item.customerCode || 'N/A',
      customerStatus: item.customerStatus !== null && item.customerStatus !== undefined ? item.customerStatus : 0,
    }))
    console.log('Fetched customers:', datavoucherUsers.value)
  }
  catch (err: any) {
    message.error(err.message || 'Lỗi tải danh sách khách hàng')
    console.error('Error fetching customers:', err, 'Response:', err.response?.data)
    datavoucherUsers.value = []
    paginationvoucherUsers.value.itemCount = 0
  }
  finally {
    loadingvoucherUsers.value = false
  }
}

function handleConfirmvoucherUsers() {
  newVoucher.value.voucherUsers = checkedCustomerKeys.value as string[]
  showCustomerModal.value = false
  voucherUsersFormItemRef.value?.restoreValidation()
  addFormRef.value?.validate({ fields: ['voucherUsers'] }).catch((err) => {
    console.error('Validation error:', err)
  })
}

function handleCancelCustomerModal() {
  showCustomerModal.value = false
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

watch(
  () => newVoucher.value.targetType,
  (val) => {
    if (val === 'INDIVIDUAL' && !isLoadingData.value) {
      showCustomerModal.value = true
      fetchvoucherUsers()
    }
    else {
      newVoucher.value.voucherUsers = []
      checkedCustomerKeys.value = []
    }
    quantityFormItemRef.value?.restoreValidation()
    conditionsFormItemRef.value?.restoreValidation()
    voucherUsersFormItemRef.value?.restoreValidation()
    addFormRef.value?.restoreValidation()
  },
)

watch(
  () => newVoucher.value.voucherUsers,
  () => {
    voucherUsersFormItemRef.value?.restoreValidation()
  },
  { deep: true },
)

watch(
  () => showCustomerModal.value,
  (newVal) => {
    if (newVal) {
      checkedCustomerKeys.value = newVoucher.value.voucherUsers?.map(id => id) || []
      fetchvoucherUsers()
    }
  },
)

watch(
  customerFilters,
  () => {
    paginationvoucherUsers.value.page = 1
    fetchvoucherUsers()
  },
  { deep: true },
)

onMounted(() => {
  loadVoucherData()
})

/* ===================== Data Table Columns ===================== */
const customerColumns: DataTableColumns<Customer> = [
  {
    type: 'selection',
    disabled: (row: Customer) => !row.id,
  },
  { title: 'Mã', key: 'customerCode', width: 100 },
  { title: 'Tên', key: 'customerName', width: 200 },
  { title: 'Email', key: 'customerEmail', width: 250 },
  { title: 'SĐT', key: 'customerPhone', width: 150 },
  {
    title: 'Trạng thái',
    key: 'customerStatus',
    width: 120,
    render: (row: Customer) => row.customerStatus === 1 ? 'Hoạt động' : 'Không hoạt động',
  },
]
</script>

<template>
  <NCard :title="props.mode === 'edit' ? 'Sửa Phiếu Giảm Giá' : 'Thêm Phiếu Giảm Giá'" class="max-w-2xl mx-auto mt-8">
    <NSpin :show="loading || isLoadingData">
      <NForm ref="addFormRef" :model="newVoucher" :rules="addVoucherRules" label-placement="top">
        <NFormItem label="Tên" path="name">
          <NInput v-model:value="newVoucher.name" placeholder="Nhập tên phiếu giảm giá" />
        </NFormItem>
        <NFormItem label="Loại Phiếu" path="typeVoucher">
          <NSelect v-model:value="newVoucher.typeVoucher" :options="voucherTypes" />
        </NFormItem>
        <div class="grid grid-cols-2 gap-4">
          <NFormItem label="Giá trị" path="discountValue">
            <NInputNumber
              v-model:value="newVoucher.discountValue"
              :min="0"
              :step="1000"
              placeholder="Nhập giá trị phiếu"
            >
              <template #suffix>
                {{ newVoucher.typeVoucher === 'PERCENTAGE' ? '%' : 'VND' }}
              </template>
            </NInputNumber>
          </NFormItem>
          <NFormItem label="Tối đa" path="maxValue">
            <NInputNumber
              v-model:value="newVoucher.maxValue"
              :min="0"
              :step="1000"
              :disabled="newVoucher.typeVoucher === 'FIXED_AMOUNT'"
              placeholder="Nhập giá trị tối đa"
            >
              <template #suffix>
                VND
              </template>
            </NInputNumber>
          </NFormItem>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <NFormItem label="Ngày bắt đầu" path="startDate">
            <NDatePicker v-model:value="newVoucher.startDate" type="datetime" placeholder="Nhập ngày bắt đầu" />
          </NFormItem>
          <NFormItem label="Ngày kết thúc" path="endDate">
            <NDatePicker v-model:value="newVoucher.endDate" type="datetime" placeholder="Nhập ngày kết thúc" />
          </NFormItem>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <NFormItem ref="conditionsFormItemRef" label="Điều kiện áp dụng" path="conditions">
            <NInputNumber v-model:value="newVoucher.conditions" :min="1" placeholder="Nhập điều kiện áp dụng..." />
          </NFormItem>
          <NFormItem v-if="showQuantity" ref="quantityFormItemRef" label="Số lượng" path="quantity">
            <NInputNumber v-model:value="newVoucher.quantity" :min="1" placeholder="Nhập số lượng..." />
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
        <NFormItem
          v-if="newVoucher.targetType === 'INDIVIDUAL'"
          ref="voucherUsersFormItemRef"
          :key="`voucherUsers-${newVoucher.targetType}`"
          label="Khách hàng đã chọn"
          path="voucherUsers"
        >
          <NButton @click="showCustomerModal = true">
            Chọn khách hàng (Đã chọn: {{ newVoucher.voucherUsers?.length || 0 }})
          </NButton>
        </NFormItem>
        <div class="flex justify-end gap-2 mt-4">
          <NButton @click="handleCancel">
            Quay lại
          </NButton>
          <NPopconfirm
            :on-positive-click="handleSaveVoucher"
            positive-text="Xác nhận"
            negative-text="Hủy"
            :show="showConfirm"
            @update:show="(value) => showConfirm = value"
          >
            <template #trigger>
              <NButton
                type="primary"
                :loading="loading"
                :disabled="loading"
                @click="handleValidateAndConfirm"
              >
                {{ props.mode === 'edit' ? 'Lưu' : 'Thêm' }}
              </NButton>
            </template>
            Bạn có chắc muốn {{ props.mode === 'edit' ? 'lưu' : 'thêm' }} phiếu giảm giá này?
          </NPopconfirm>
        </div>
      </NForm>
    </NSpin>

    <!-- Customer Selection Modal -->
    <NModal v-model:show="showCustomerModal" preset="dialog" title="Chọn Khách Hàng" :show-icon="false" :style="{ width: '60vw' }">
      <NSpin :show="loadingvoucherUsers">
        <NForm :model="customerFilters" label-placement="top">
          <div class="grid grid-cols-2 gap-4">
            <NFormItem label="Tìm kiếm theo tên">
              <NInput
                v-model:value="customerFilters.customerName"
                placeholder="Nhập tên khách hàng..."
                clearable
              />
            </NFormItem>
            <NFormItem label="Trạng thái">
              <NRadioGroup v-model:value="customerFilters.customerStatus" name="status">
                <NSpace>
                  <NRadio :value="null" label="Tất cả" />
                  <NRadio :value="1" label="Hoạt động" />
                  <NRadio :value="0" label="Không hoạt động" />
                </NSpace>
              </NRadioGroup>
            </NFormItem>
          </div>
        </NForm>
        <NDataTable
          v-model:checked-row-keys="checkedCustomerKeys"
          :columns="customerColumns"
          :data="datavoucherUsers"
          :row-key="(row: Customer) => row.id"
          :pagination="false"
          :scroll-x="800"
        />
        <NPagination
          v-model:page="paginationvoucherUsers.page"
          :page-size="paginationvoucherUsers.pageSize"
          :item-count="paginationvoucherUsers.itemCount"
          class="mt-4"
        />
        <div class="flex justify-end gap-2 mt-4">
          <NButton @click="handleCancelCustomerModal">
            Hủy
          </NButton>
          <NButton type="primary" @click="handleConfirmvoucherUsers">
            Xác nhận
          </NButton>
        </div>
      </NSpin>
    </NModal>
  </NCard>
</template>
