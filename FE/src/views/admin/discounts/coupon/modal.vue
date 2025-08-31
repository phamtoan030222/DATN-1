<!-- modal.vue -->
<script lang="ts" setup>
/* ===================== Imports ===================== */
import { computed, h, ref, watch } from 'vue'
import type { DataTableColumns, FormInst, FormItemInst, FormRules } from 'naive-ui'
import {
  NButton,
  NDataTable,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NInputNumber,
  NModal,
  NPagination,
  NRadio,
  NRadioGroup,
  NSelect,
  NSpace,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import type { ADCustomerResponse, ADVoucherResponse } from '@/service/api/admin/product/api.voucher'

/* ===================== Props and Emits ===================== */
const props = defineProps<{
  show: boolean
  isEdit: boolean
  voucherData: Partial<ADVoucherResponse>
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
const customersFormItemRef = ref<FormItemInst | null>(null)
const showCustomerModal = ref(false)
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
  conditions: null as number | null,
  targetType: 'ALL_CUSTOMERS',
  quantity: null as number | null,
  customers: [] as string[],
})

const voucherTypes = [
  { label: 'Giảm %', value: 'PERCENTAGE' },
  { label: 'Giảm tiền', value: 'FIXED_AMOUNT' },
]
const targetTypes = [
  { label: 'Tất cả khách hàng', value: 'ALL_CUSTOMERS' },
  { label: 'Khách hàng riêng', value: 'INDIVIDUAL' },
]

// State cho bảng khách hàng
const dataCustomers = ref<ADCustomerResponse[]>([
  { id: 'A1B2C3D4E5F6G43A', code: 'KH001', name: 'Nguyễn Văn Tâm', email: 'tamngason2004@gmail.com', phone: '0123456789' },
  { id: 'CUS0000000000002', code: 'KH002', name: 'Nguyễn Anh Tuấn', email: 'tuangaymta@gmail.com.com', phone: '0987654321' },
  { id: 'CUS0000000000001', code: 'KH003', name: 'Vương Đắc Tài', email: 'taideptrai11002@gmail.com', phone: '0912345678' },
  { id: 'KH004', code: 'KH004', name: 'Phạm Thị D', email: 'phamthid@example.com', phone: '0845678901' },
  { id: 'KH005', code: 'KH005', name: 'Hoàng Văn E', email: 'hoangvane@example.com', phone: '0765432198' },
])

const loadingCustomers = ref(false)
const checkedCustomerKeys = ref<(string | number)[]>([])
const paginationCustomers = ref({
  page: 1,
  pageSize: 10,
  itemCount: 5, // Cập nhật số lượng bản ghi giả
})

/* ===================== Computed ===================== */
const showQuantity = computed(() => newVoucher.value.targetType === 'ALL_CUSTOMERS')

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
        if (newVoucher.value.targetType === 'ALL_CUSTOMERS') {
          if (!value || value <= 0) {
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
      validator: (_rule, value: number) => {
        if (newVoucher.value.targetType === 'ALL_CUSTOMERS' || newVoucher.value.targetType === 'INDIVIDUAL') {
          if (!value || value <= 0) {
            return new Error('Vui lòng nhập điều kiện hợp lệ')
          }
        }
        return true
      },
      trigger: ['blur', 'change'],
    },
  ],
  customers: [
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
      trigger: ['change'],
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

watch(() => props.show, (newVal) => {
  if (newVal) {
    resetNewVoucher()
    if (props.isEdit && props.voucherData) {
      newVoucher.value = {
        id: props.voucherData.id || '',
        code: props.voucherData.code || '',
        name: props.voucherData.name || '',
        typeVoucher: props.voucherData.typeVoucher || 'PERCENTAGE',
        discountValue: props.voucherData.discountValue ?? 0,
        maxValue: props.voucherData.maxValue ?? 0,
        startDate: props.voucherData.startDate || null,
        endDate: props.voucherData.endDate || null,
        note: props.voucherData.note || '',
        conditions: props.voucherData.conditions || null,
        targetType: props.voucherData.targetType || 'ALL_CUSTOMERS',
        quantity: props.voucherData.quantity ?? 0,
        customers: props.voucherData.customers || [],
      }
    }
  }
}, { immediate: true })

watch(showCustomerModal, (newVal) => {
  if (!newVal) {
    // Khi đóng modal, ép validate lại để hiển thị lỗi nếu chưa chọn
    addFormRef.value?.validate({ fields: ['customers'] }).catch(() => {})
  }
  else {
    // Khi mở modal, restore selection cũ
    checkedCustomerKeys.value = newVoucher.value.customers.map(id => id)
  }
})

// Watcher cho targetType để tự động mở modal và restore validation
watch(
  () => newVoucher.value.targetType,
  (val) => {
    if (val === 'INDIVIDUAL') {
      showCustomerModal.value = true
    }
    // Không reset customers để giữ selection cũ
    // Restore validation cho các trường phụ thuộc
    quantityFormItemRef.value?.restoreValidation()
    conditionsFormItemRef.value?.restoreValidation()
    customersFormItemRef.value?.restoreValidation()
  },
)

// Watcher cho customers để tự động validate
watch(
  () => newVoucher.value.customers,
  () => {
    addFormRef.value?.validate({ fields: ['customers'] }).catch(() => {})
  },
  { deep: true },
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
    conditions: null,
    targetType: 'ALL_CUSTOMERS',
    quantity: null,
    customers: [],
  }
  addFormRef.value?.restoreValidation()
}

function handleAddVoucher() {
  addFormRef.value?.validate((errors) => {
    if (!errors) {
      emit('save', { ...newVoucher.value })
    }
    else {
      message.error('Vui lòng nhập đầy đủ thông tin')
    }
  })
}

function fetchCustomers() { // Sửa để sử dụng dữ liệu giả
  // Không cần loading vì dữ liệu tĩnh
  // dataCustomers.value đã được khởi tạo sẵn
  paginationCustomers.value.itemCount = dataCustomers.value.length
}

function handleConfirmCustomers() {
  newVoucher.value.customers = checkedCustomerKeys.value as string[]
  showCustomerModal.value = false
}

/* ===================== Data Table Columns for Customers ===================== */
const customerColumns: DataTableColumns<ADCustomerResponse> = [
  { type: 'selection' },
  { title: 'Mã', key: 'code', width: 100 },
  { title: 'Tên', key: 'name', width: 200 },
  { title: 'Email', key: 'email', width: 250 },
  { title: 'SĐT', key: 'phone', width: 150 },
]
</script>

<template>
  <!-- Modal thêm voucher -->
  <NModal
    :show="props.show" preset="card" :title="props.isEdit ? 'Sửa Phiếu Giảm Giá' : 'Thêm Phiếu Giảm Giá'"
    style="width: 500px;" :bordered="false" @update:show="emit('update:show', $event)"
  >
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
          <NDatePicker v-model:value="newVoucher.startDate" type="date" placeholder="Nhập ngày bắt đầu" />
        </NFormItem>
        <NFormItem label="Ngày kết thúc" path="endDate">
          <NDatePicker v-model:value="newVoucher.endDate" type="date" placeholder="Nhập ngày kết thúc" />
        </NFormItem>
      </div>

      <div class="grid grid-cols-2 gap-4">
        <NFormItem v-if="showQuantity" ref="quantityFormItemRef" label="Số lượng" path="quantity">
          <NInputNumber v-model:value="newVoucher.quantity" :min="1" placeholder="Nhập số lượng..." />
        </NFormItem>
        <NFormItem ref="conditionsFormItemRef" label="Điều kiện áp dụng" path="conditions">
          <NInputNumber v-model:value="newVoucher.conditions" :min="1" placeholder="Nhập điều kiện áp dụng..." />
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

      <!-- Tích hợp path="customers" vào NFormItem hiển thị -->
      <NFormItem
        v-if="newVoucher.targetType === 'INDIVIDUAL'"
        ref="customersFormItemRef"
        :key="`customers-${newVoucher.targetType}`"
        label="Khách hàng đã chọn"
        path="customers"
      >
        <NButton @click="showCustomerModal = true">
          Chọn khách hàng (Đã chọn: {{ newVoucher.customers.length }})
        </NButton>
      </NFormItem>

      <div class="flex justify-end gap-2 mt-4">
        <NButton @click="() => { resetNewVoucher(); emit('cancel') }">
          Hủy
        </NButton>
        <NButton type="primary" @click="handleAddVoucher">
          {{ props.isEdit ? 'Lưu' : 'Thêm' }}
        </NButton>
      </div>
    </NForm>
  </NModal>
  <!-- Modal chọn khách hàng -->
  <NModal v-model:show="showCustomerModal" preset="card" style="width: 800px;" title="Chọn khách hàng">
    <NDataTable
      v-model:checked-row-keys="checkedCustomerKeys" :columns="customerColumns" :data="dataCustomers"
      :loading="loadingCustomers" :row-key="row => row.id" :pagination="false" bordered
    />
    <div class="flex justify-center mt-4">
      <NPagination
        :page="paginationCustomers.page" :page-size="paginationCustomers.pageSize"
        :item-count="paginationCustomers.itemCount"
        @update:page="page => { paginationCustomers.page = page; fetchCustomers() }"
        @update:page-size="size => { paginationCustomers.pageSize = size; fetchCustomers() }"
      />
    </div>
    <div class="flex justify-end gap-2 mt-4">
      <NButton @click="showCustomerModal = false">
        Đóng
      </NButton>
      <NButton type="primary" @click="handleConfirmCustomers">
        Xác nhận
      </NButton>
    </div>
  </NModal>
</template>
