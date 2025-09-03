<!-- modal.vue -->
<script lang="ts" setup>
import { computed, ref, watch } from 'vue'
import type { DataTableColumns, FormInst, FormItemInst, FormRules } from 'naive-ui'
import {
  NButton,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NInput,
  NInputNumber,
  NModal,
  NPagination,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpin,
  useMessage,
} from 'naive-ui'
import type { ADCustomerResponse, ADVoucherResponse } from '@/service/api/admin/product/api.voucher'

/* ===================== Props & Emits ===================== */
const props = defineProps<{
  show: boolean
  isEdit: boolean
  voucherData: Partial<ADVoucherResponse>
  isLoading: boolean
}>()

const emit = defineEmits<{
  (e: 'update:show', value: boolean): void
  (e: 'save', data: any): void
  (e: 'cancel'): void
}>()

/* ===================== State ===================== */
const message = useMessage()
const addFormRef = ref<FormInst | null>(null)
const quantityFormItemRef = ref<FormItemInst | null>(null)
const conditionsFormItemRef = ref<FormItemInst | null>(null)
const voucherUsersFormItemRef = ref<FormItemInst | null>(null)
const showCustomerModal = ref(false)
const isLoadingData = ref(false)

const newVoucher = ref({
  id: '',
  code: '',
  name: '',
  typeVoucher: 'PERCENTAGE',
  discountValue: null as number | null,
  maxValue: null as number | null,
  startDate: null as number | null,
  endDate: null as number | null,
  remainingQuantity: null as number | null,
  createdDate: null as number | null,
  note: '',
  conditions: null as number | null,
  targetType: 'ALL_CUSTOMERS',
  quantity: null as number | null,
  voucherUsers: [] as string[] | null,
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

const datavoucherUsers = ref<ADCustomerResponse[]>([
  { id: 'I9J8K7L6M5N4O3P2', code: 'KH001', name: 'Nguyễn Văn Tâm', email: 'tamngason2004@gmail.com', phone: '0123456789' },
  { id: 'A1B2C3D4E5F6G7H8', code: 'KH002', name: 'Nguyễn Anh Tuấn', email: 'tuangaymta@gmail.com', phone: '0987654321' },
  { id: 'Q1R2S3T4U5V6W7X8', code: 'KH003', name: 'Vương Đắc Tài', email: 'taideptrai11002@gmail.com', phone: '0912345678' },
  { id: 'A1B2C3D4E7GTR43B', code: 'KH004', name: 'Lương Tiểu Băng', email: 'luongtieubang1192007@gmail.com', phone: '0845678901' },
  { id: 'KH005', code: 'KH005', name: 'Hoàng Văn E', email: 'hoangvane@example.com', phone: '0765432198' },
])

const loadingvoucherUsers = ref(false)
const checkedCustomerKeys = ref<(string | number)[]>([])
const paginationvoucherUsers = ref({
  page: 1,
  pageSize: 10,
  itemCount: datavoucherUsers.value.length,
})

/* ===================== Computed ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'ALL_CUSTOMERS')

const paginatedCustomerData = computed(() => {
  const start = (paginationvoucherUsers.value.page - 1) * paginationvoucherUsers.value.pageSize
  const end = start + paginationvoucherUsers.value.pageSize
  return datavoucherUsers.value.slice(start, end)
})

/* ===================== Validation Rules ===================== */
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
  endDate: {
    type: 'number',
    required: true,
    validator: (_rule, value: number) => {
      if (value <= (newVoucher.value.startDate || 0))
        return new Error('Ngày kết thúc phải sau ngày bắt đầu')
      return true
    },
    message: 'Chọn ngày kết thúc',
    trigger: ['change'],
  },
  targetType: { required: true, message: 'Chọn đối tượng áp dụng', trigger: ['change'] },
  quantity: [
    {
      validator: (_rule, value: number) => {
        if (newVoucher.value.targetType === 'ALL_CUSTOMERS') {
          if (!value || value <= 0)
            return new Error('Vui lòng nhập số lượng hợp lệ')
        }
        return true
      },
      trigger: ['blur', 'change'],
    },
  ],
  conditions: [
    {
      validator: (_rule, value: number) => {
        if (newVoucher.value.targetType === 'ALL_CUSTOMERS' || newVoucher.value.targetType === 'INDIVIDUAL') {
          if (!value || value <= 0)
            return new Error('Vui lòng nhập điều kiện hợp lệ')
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
          if (!value || value.length === 0)
            return new Error('Vui lòng chọn ít nhất một khách hàng')
        }
        return true
      },
      trigger: ['change'],
    },
  ],
}

/* ===================== Watchers ===================== */
watch(
  () => props.show,
  (newVal) => {
    if (newVal) {
      isLoadingData.value = true
      resetNewVoucher()
      if (props.isEdit && props.voucherData) {
        newVoucher.value = {
          ...props.voucherData,
          id: props.voucherData?.id ?? '',
          code: props.voucherData?.code ?? '',
          name: props.voucherData?.name ?? '',
          typeVoucher: props.voucherData?.typeVoucher ?? 'PERCENTAGE',
          targetType: props.voucherData?.targetType ?? 'ALL_CUSTOMERS',
          discountValue: props.voucherData?.discountValue ?? 0,
          maxValue: props.voucherData?.maxValue ?? 0,
          quantity: props.voucherData?.quantity ?? 0,
          remainingQuantity: props.voucherData?.remainingQuantity ?? 0,
          startDate: props.voucherData?.startDate ?? 0,
          endDate: props.voucherData?.endDate ?? 0,
          createdDate: props.voucherData?.createdDate ?? 0,
          conditions: props.voucherData?.conditions ?? 0,
          note: props.voucherData?.note ?? '',
          status: props.voucherData?.status ?? '',
          voucherUsers: props.voucherData.voucherUsers ? props.voucherData.voucherUsers.map((detail: any) => detail.customerId || detail.id || '') : [],
        }
        checkedCustomerKeys.value = newVoucher.value.voucherUsers?.filter(id => datavoucherUsers.value.some(user => user.id === id)) || []
      }
      isLoadingData.value = false
    }
  },
  { immediate: true },
)

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
    }
    else {
      newVoucher.value.voucherUsers = []
      checkedCustomerKeys.value = []
    }
    quantityFormItemRef.value?.restoreValidation()
    conditionsFormItemRef.value?.restoreValidation()
    voucherUsersFormItemRef.value?.restoreValidation()
    addFormRef.value?.validate().catch(() => {})
  },
)

watch(
  () => newVoucher.value.voucherUsers,
  () => {
    voucherUsersFormItemRef.value?.restoreValidation()
    addFormRef.value?.validate({ fields: ['voucherUsers'] }).catch(() => {})
  },
  { deep: true },
)

watch(
  () => showCustomerModal.value,
  (newVal) => {
    if (newVal) {
      checkedCustomerKeys.value = newVoucher.value.voucherUsers?.map(id => id) || []
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
}

function handleAddVoucher() {
  addFormRef.value?.validate((errors) => {
    if (!errors) {
      const payload = {
        ...newVoucher.value,
        voucherUsers: newVoucher.value.voucherUsers?.map(id => ({
          customer: { id },
        })) || [],
      }
      emit('save', payload)
    }
    else {
      message.error('Vui lòng nhập đầy đủ thông tin')
    }
  })
}

function fetchvoucherUsers() {
  paginationvoucherUsers.value.itemCount = datavoucherUsers.value.length
}

function handleConfirmvoucherUsers() {
  newVoucher.value.voucherUsers = checkedCustomerKeys.value as string[]
  showCustomerModal.value = false
  voucherUsersFormItemRef.value?.restoreValidation()
  addFormRef.value?.validate({ fields: ['voucherUsers'] }).catch(() => {})
}

function handleCancelCustomerModal() {
  showCustomerModal.value = false
}

/* ===================== Data Table Columns ===================== */
const customerColumns: DataTableColumns<ADCustomerResponse> = [
  { type: 'selection' },
  { title: 'Mã', key: 'code', width: 100 },
  { title: 'Tên', key: 'name', width: 200 },
  { title: 'Email', key: 'email', width: 250 },
  { title: 'SĐT', key: 'phone', width: 150 },
]
</script>

<template>
  <NModal
    :show="props.show" preset="card" :title="props.isEdit ? 'Sửa Phiếu Giảm Giá' : 'Thêm Phiếu Giảm Giá'"
    style="width: 500px;" :bordered="false" @update:show="emit('update:show', $event)"
  >
    <NSpin :show="props.isLoading">
      <NForm ref="addFormRef" :model="newVoucher" :rules="addVoucherRules" label-placement="top">
        <NFormItem label="Tên" path="name">
          <NInput v-model:value="newVoucher.name" placeholder="Nhập tên phiếu giảm giá" />
        </NFormItem>
        <NFormItem label="Loại Phiếu" path="typeVoucher">
          <NSelect v-model:value="newVoucher.typeVoucher" :options="voucherTypes" />
        </NFormItem>
        <div class="grid grid-cols-2 gap-4">
          <NFormItem label="Giá trị" path="discountValue">
            <NInputNumber v-model:value="newVoucher.discountValue" :min="0" :step="1000" placeholder="Nhập giá trị phiếu">
              <template #suffix>
                {{ newVoucher.typeVoucher === 'PERCENTAGE' ? '%' : 'VND' }}
              </template>
            </NInputNumber>
          </NFormItem>
          <NFormItem label="Tối đa" path="maxValue">
            <NInputNumber
              v-model:value="newVoucher.maxValue" :min="0" :step="1000"
              :disabled="newVoucher.typeVoucher === 'FIXED_AMOUNT'" placeholder="Nhập giá trị tối đa"
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
          <NButton @click="() => { resetNewVoucher(); emit('cancel') }">
            Hủy
          </NButton>
          <NButton type="primary" :loading="props.isLoading" :disabled="props.isLoading" @click="handleAddVoucher">
            {{ props.isEdit ? 'Lưu' : 'Thêm' }}
          </NButton>
        </div>
      </NForm>
    </NSpin>
  </NModal>

  <!-- Modal chọn khách hàng -->
  <NModal v-model:show="showCustomerModal" preset="card" style="width: 800px;" title="Chọn khách hàng">
    <NDataTable
      v-model:checked-row-keys="checkedCustomerKeys"
      :columns="customerColumns"
      :data="paginatedCustomerData"
      :loading="loadingvoucherUsers"
      :row-key="row => row.id"
      :pagination="false"
      bordered
    />
    <div class="flex justify-center mt-4">
      <NPagination
        :page="paginationvoucherUsers.page"
        :page-size="paginationvoucherUsers.pageSize"
        :item-count="paginationvoucherUsers.itemCount"
        @update:page="page => { paginationvoucherUsers.page = page; fetchvoucherUsers() }"
        @update:page-size="size => { paginationvoucherUsers.pageSize = size; fetchvoucherUsers() }"
      />
    </div>
    <div class="flex justify-end gap-2 mt-4">
      <NButton @click="handleCancelCustomerModal">
        Đóng
      </NButton>
      <NButton type="primary" @click="handleConfirmvoucherUsers">
        Xác nhận
      </NButton>
    </div>
  </NModal>
</template>
