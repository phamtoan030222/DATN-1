<template>
  <div class="p-6">
    <!-- Header Section -->
    <n-card>
      <NSpace vertical :size="8">
        <NSpace align="center">
          <NIcon size="24">
            <Icon :icon="'carbon:events'" />
          </NIcon>
          <span style="font-weight: 600; font-size: 24px">
            Quản lý Khách hàng
          </span>
        </NSpace>
        <span>Quản lý khách hàng trên toàn hệ thống cửa hàng</span>
      </NSpace>
    </n-card>

    <!-- Filter Card -->
    <NCard title="Bộ lọc" class="mb-4 mt-3">
      <NForm label-placement="top" :model="formSearch" :label-style="{ fontWeight: 'bold' }">
        <div class="space-y-3">
          <div class="flex gap-4 items-end">
            <!-- Tên khách hàng -->
            <NFormItem label="Tên khách hàng:" class="flex-1">
              <NInput v-model:value="formSearch.customerName" placeholder="Nhập tên khách hàng..."
                @input="handleAutoFilter" clearable />
            </NFormItem>

            <!-- Giới tính -->
            <NFormItem label="Giới tính:" class="flex-1">
              <NSelect v-model:value="formSearch.customerGender" :options="GENDER_FILTER_OPTIONS"
                placeholder="Chọn giới tính" clearable @update:value="handleAutoFilter" />
            </NFormItem>

            <!-- Trạng thái -->
            <NFormItem label="Trạng thái:" class="flex-1">
              <NRadioGroup v-model:value="formSearch.customerStatus" @update:value="handleAutoFilter">
                <NRadio :value="STATUS_OPTIONS.ALL">Tất cả</NRadio>
                <NRadio :value="STATUS_OPTIONS.ACTIVE">Hoạt động</NRadio>
                <NRadio :value="STATUS_OPTIONS.INACTIVE">Ngưng</NRadio>
              </NRadioGroup>
            </NFormItem>

            <!-- Actions -->
            <div class="pb-6 flex gap-2">
              <NButton type="primary" secondary circle title="Làm mới" @click="refreshTable">
                <NIcon size="20">
                  <Icon :icon="'icon-park-outline:refresh'" />
                </NIcon>
              </NButton>
            </div>
          </div>
        </div>
      </NForm>
    </NCard>

    <!-- Main Table Card -->
    <NCard title="Danh sách khách hàng">
      <template #header-extra>
        <NSpace>
          <NButton @click="openModal('add')" type="primary" circle title="Thêm khách hàng">
            <NIcon size="20">
              <Icon :icon="'icon-park-outline:add-one'" />
            </NIcon>
          </NButton>

          <NPopconfirm @positive-click="handleBatchDelete">
            <template #trigger>
              <NButton type="error" secondary circle :disabled="checkedRowKeys.length === 0" :title="batchDeleteTitle">
                <NIcon size="20">
                  <Icon :icon="'icon-park-outline:delete'" />
                </NIcon>
              </NButton>
            </template>
            {{ batchDeleteConfirmText }}
          </NPopconfirm>
        </NSpace>
      </template>

      <NDataTable v-model:checked-row-keys="checkedRowKeys" :row-key="getRowKey" :columns="columns" :data="customers"
        :loading="loading" size="medium" :scroll-x="1000" :pagination="false" bordered />

      <!-- Pagination -->
      <div class="flex justify-center mt-4">
        <NPagination :page="currentPage" :page-size="pageSize" :page-count="pageCount"
          @update:page="handlePageChange" />
      </div>
    </NCard>

    <!-- Customer Modal -->
    <NModal v-model:show="showModal" :mask-closable="false" preset="card" :title="modalTitle" class="w-700px"
      :segmented="{ content: true, action: true }" :on-after-enter="onModalOpened" :on-after-leave="afterModalLeave">
      <NForm ref="formRef" :rules="FORM_RULES" label-placement="left" :label-width="120" :model="form"
        :show-require-mark="false">
        <div class="grid grid-cols-2 gap-4">
          <!-- Avatar Upload - Full width and centered -->
          <div class="col-span-2 flex justify-center mb-6">
            <div class="flex flex-col items-center">
              <div class="relative mb-3">
                <img :src="avatarSrc" class="w-20 h-20 rounded-lg object-cover cursor-pointer border border-gray-300"
                  alt="Avatar" @click="triggerUpload" />


                <div
                  class="absolute inset-0 bg-black bg-opacity-50 rounded-lg flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity cursor-pointer"
                  @click="triggerUpload">
                  <span class="text-white text-sm font-medium">Đổi ảnh</span>
                </div>
              </div>
              <NUpload ref="uploadRef" :max="1" accept="image/*" :default-upload="false" :on-change="handleUploadChange"
                :show-file-list="false" style="display: none;" />
              <span class="text-xs text-gray-500 text-center">Chọn ảnh đại diện<br />(Khuyến nghị: 120x120px)</span>
            </div>
          </div>

          <div class="col-span-2">
            <label class="block text-sm font-medium mb-2">Tên khách hàng *</label>
            <NInput v-model:value="form.customerName" placeholder="Nhập tên khách hàng" />
          </div>

          <div class="col-span-1">
            <label class="block text-sm font-medium mb-2">Số điện thoại *</label>
            <NInput v-model:value="form.customerPhone" placeholder="Số điện thoại" />
          </div>

          <div class="col-span-1">
            <label class="block text-sm font-medium mb-2">Giới tính</label>
            <NSelect v-model:value="form.customerGender" :options="GENDER_OPTIONS" placeholder="Chọn giới tính" />
          </div>

          <div class="col-span-2">
            <label class="block text-sm font-medium mb-2">Email *</label>
            <NInput v-model:value="form.customerEmail" placeholder="Nhập email" />
          </div>

          <div class="col-span-1">
            <label class="block text-sm font-medium mb-2">Ngày sinh</label>
            <NDatePicker v-model:formatted-value="form.customerBirthdayStr" value-format="yyyy-MM-dd" type="date"
              class="w-full" :default-value="null" clearable />
          </div>

          <div class="col-span-1">
            <label class="block text-sm font-medium mb-2">Trạng thái</label>
            <NSwitch v-model:value="form.customerStatus" :checked-value="STATUS_OPTIONS.ACTIVE"
              :unchecked-value="STATUS_OPTIONS.INACTIVE">
              <template #checked>Hoạt động</template>
              <template #unchecked>Ngưng</template>
            </NSwitch>
          </div>
        </div>
      </NForm>

      <template #action>
        <NSpace justify="center">
          <NButton @click="closeModal">Hủy bỏ</NButton>
          <NButton type="primary" :loading="submitting" @click="onSubmit">
            {{ submitButtonText }}
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, nextTick, onMounted, reactive, ref, watch } from 'vue'
import type { DataTableColumns, FormInst, FormItemRule, SelectOption,UploadFileInfo } from 'naive-ui'
import { NButton, NIcon, NPagination, NPopconfirm, NRadio, NRadioGroup, NSpace, NSwitch, NUpload, useMessage } from 'naive-ui'
import type { Customer } from '@/service/api/admin/users/customer'
import { Icon } from "@iconify/vue"
import {
  createCustomer,
  deleteCustomer as deleteCustomerApi,
  getCustomers,
  updateCustomer,
  uploadAvatar,
} from '@/service/api/admin/users/customer'

// =============================================================================
// CONSTANTS
// =============================================================================

const STATUS_OPTIONS = {
  ALL: -1,
  ACTIVE: 1,
  INACTIVE: 0,
} as const

const GENDER = {
  MALE: 'true',
  FEMALE: 'false',
} as const

const SEARCH_DEBOUNCE_DELAY = 500
const DEFAULT_PAGE_SIZE = 10

// Options for filters and forms
const GENDER_FILTER_OPTIONS: SelectOption[] = [
  { label: 'Tất cả', value: null },
  { label: 'Nam', value: 1 },
  { label: 'Nữ', value: 0 }
]

const GENDER_OPTIONS: SelectOption[] = [
  { label: 'Nam', value: GENDER.MALE },
  { label: 'Nữ', value: GENDER.FEMALE }
]

const FORM_RULES = {
  customerName: { required: true, message: 'Vui lòng nhập tên khách hàng', trigger: 'blur' },
  customerPhone: { required: true, message: 'Vui lòng nhập số điện thoại', trigger: 'blur' },
  customerEmail: {
    required: true,
    validator(_rule: FormItemRule, value: string) {
      if (!value) return new Error('Vui lòng nhập email')
      const emailRegex = /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/u
      if (!emailRegex.test(value)) return new Error('Email không hợp lệ')
      return true
    },
    trigger: 'blur'
  }
}

// =============================================================================
// STATE & REACTIVE DATA
// =============================================================================

const message = useMessage()

// Data state
const customers = ref<Customer[]>([])
const currentPage = ref<number>(1)
const pageSize = ref<number>(DEFAULT_PAGE_SIZE)
const totalElements = ref<number>(0)
const totalPages = ref<number>(1)
const loading = ref(false)
const submitting = ref(false)
const checkedRowKeys = ref<(string | number)[]>([])

// Form search state
const formSearch = reactive({
  customerName: "",
  customerGender: null as number | null,
  customerStatus: STATUS_OPTIONS.ALL as number,
})

// Modal state
const showModal = ref(false)
const modalMode = ref<'add' | 'edit'>('add')
const formRef = ref<FormInst | null>(null)
const uploadRef = ref()

// Form model
interface CustomerForm {
  id?: string | number
  customerName: string
  customerPhone: string
  customerEmail: string
  customerGender: string | null
  customerStatus: number
  customerBirthdayStr: string | null
  customerAvatar: string
}

const form = reactive<CustomerForm>({
  id: undefined,
  customerName: '',
  customerPhone: '',
  customerEmail: '',
  customerGender: GENDER.MALE,
  customerStatus: STATUS_OPTIONS.ACTIVE,
  customerBirthdayStr: null,
  customerAvatar: '',
})

const avatarState = reactive({
  file: null as File | null,
  uploading: false,
  preview: '' as string, // chỉ để hiển thị
  url: '' as string      // URL thật từ server
})

const avatarSrc = computed(() =>
  avatarState.preview || form.customerAvatar || 'https://www.svgrepo.com/show/452030/avatar-default.svg'
)


// Search timer
let searchTimer: NodeJS.Timeout | null = null

// =============================================================================
// COMPUTED PROPERTIES
// =============================================================================

const modalTitle = computed(() =>
  modalMode.value === 'add' ? 'Thêm khách hàng' : 'Sửa khách hàng'
)

const submitButtonText = computed(() =>
  modalMode.value === 'add' ? 'Thêm mới' : 'Cập nhật'
)

const pageCount = computed(() =>
  Math.ceil(totalElements.value / pageSize.value)
)

const batchDeleteTitle = computed(() =>
  `Xóa ${checkedRowKeys.value.length} khách hàng đã chọn`
)

const batchDeleteConfirmText = computed(() =>
  `Bạn có chắc chắn muốn xóa ${checkedRowKeys.value.length} khách hàng đã chọn?`
)

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

const getRowKey = (row: Customer) => row.id as string | number

const formatDate = (timestamp?: number): string => {
  if (!timestamp) return '-'
  const d = new Date(timestamp)
  return d.toLocaleDateString('vi-VN')
}
const DEFAULT_AVATAR = 'https://www.svgrepo.com/show/452030/avatar-default.svg'

const createCustomerInfoCell = (row: Customer) => {
  return h('div', { class: 'flex items-center' }, [
    h('img', {
      src: row.customerAvatar || DEFAULT_AVATAR,
      class: 'flex-shrink-0 h-10 w-10 rounded-md object-cover mr-3',
      alt: row.customerName || 'Avatar',
      onError: (e: Event) => {
        const img = e.target as HTMLImageElement
        if (!img) return
        img.onerror = null
        img.src = DEFAULT_AVATAR
      }
    }),

    h('div', [
      h('div', { class: 'font-medium text-gray-800' }, row.customerName),
      h('div', { class: 'text-sm text-gray-500' },
        `${row.customerCode || 'Chưa có mã'} • ${row.customerGender ? 'Nam' : 'Nữ'}`
      ),
      h('div', { class: 'text-xs text-gray-400' },
        `Ngày sinh: ${formatDate(row.customerBirthday)}`
      )
    ])
  ])
}



const createContactCell = (row: Customer) =>
  h('div', [
    h('div', { class: 'text-sm' }, `Email: ${row.customerEmail}`),
    h('div', { class: 'text-xs text-gray-400' }, `Số điện thoại: ${row.customerPhone}`)
  ])

const createDateCell = (row: Customer) =>
  h('div', [
    h('div', { class: 'font-sm' }, formatDate(row.customerCreatedDate)),
    h('div', { class: 'text-xs text-gray-400' },
      `Cập nhật: ${formatDate(row.customerModifiedDate)}`
    )
  ])

const createStatusCell = (row: Customer) =>
  h('div', { class: 'flex flex-col items-center space' }, [
    h(NSwitch, {
      value: row.customerStatus === STATUS_OPTIONS.ACTIVE,
      'onUpdate:value': (val: boolean) => toggleStatus(row, val)
    }),
  ])

const createActionsCell = (row: Customer) =>
  h(NSpace, { justify: 'center', size: 12 }, {
    default: () => [
      // Nút edit
      h(NIcon, {
        size: 18,
        class: 'cursor-pointer text-blue-500 hover:text-blue-700',
        onClick: () => openModal('edit', row)
      }, { default: () => h(Icon, { icon: 'mdi:pencil' }) }),

      // Nút delete + confirm
      h(NPopconfirm, {
        onPositiveClick: () => handleDeleteCustomer(row.id!)
      }, {
        default: () => 'Bạn có chắc chắn muốn xóa khách hàng này?',
        trigger: () => h(NIcon, {
          size: 18,
          class: 'cursor-pointer text-red-500 hover:text-red-700'
        }, { default: () => h(Icon, { icon: 'icon-park-outline:delete' }) })
      })
    ]
  })

// =============================================================================
// TABLE COLUMNS
// =============================================================================

const columns: DataTableColumns<Customer> = [
  { type: 'selection', width: 20 },
  {
    title: 'STT',
    key: 'stt',
    width: 20,
    render: (_row, index) => (currentPage.value - 1) * pageSize.value + index + 1
  },
  {
    title: 'Thông tin',
    key: 'info',
    width: 80,
    render: createCustomerInfoCell
  },
  {
    title: 'Liên hệ',
    key: 'contact',
    width: 100,
    render: createContactCell
  },
  {
    title: 'Ngày tạo',
    key: 'dates',
    width: 60,
    render: createDateCell
  },
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 10,
    render: createStatusCell
  },
  {
    title: 'Hành động',
    key: 'actions',
    align: 'center',
    width: 50,
    render: createActionsCell
  }
]

// =============================================================================
// WATCHERS
// =============================================================================

watch(
  () => formSearch,
  () => handleAutoFilter(),
  { deep: true }
)

// =============================================================================
// UPLOAD FUNCTIONS
// =============================================================================

function triggerUpload() {
  uploadRef.value?.$el?.querySelector('input[type="file"]')?.click()
}

async function handleUploadChange({ file }: { file: UploadFileInfo }) {
  // Nếu không có raw file hoặc file đã bị remove thì bỏ qua
  const rawFile = file?.file as File | undefined
  if (!rawFile || file.status === 'removed') return

  avatarState.preview = URL.createObjectURL(rawFile)
  avatarState.uploading = true
  const stop = message.loading('Đang tải ảnh lên...', 0)

  try {
    const { url } = await uploadAvatar(rawFile)
    if (!url) throw new Error('Server không trả về URL ảnh')

    form.customerAvatar = url
    avatarState.url = url
  } catch (e: any) {
    console.error('Upload error:', e)
    message.error(e?.message || 'Upload thất bại')
    form.customerAvatar = ''
    avatarState.url = ''
  } finally {
    stop()
    avatarState.uploading = false
  }
}



// =============================================================================
// API FUNCTIONS
// =============================================================================

const buildSearchParams = () => {
  const page = Math.max(1, Number(currentPage.value) || 1)
  const size = Math.max(1, Number(pageSize.value) || DEFAULT_PAGE_SIZE)

  return {
    page,
    size,
    ...(formSearch.customerName && { customerName: formSearch.customerName }),
    ...(formSearch.customerGender !== null && { customerGender: formSearch.customerGender }),
    ...(formSearch.customerStatus !== STATUS_OPTIONS.ALL && { customerStatus: formSearch.customerStatus }),
  }
}

async function loadCustomers() {
  try {
    loading.value = true
    const params = buildSearchParams()
    const response = await getCustomers(params)

    const payload = response?.data?.data
    if (!payload) throw new Error('Không có dữ liệu trả về')
    // ✅ Gán avatar mặc định nếu null/undefined/rỗng
    customers.value = (payload.data || []).map((c: Customer) => ({
      ...c,
      customerAvatar: c.customerAvatar || 'https://www.svgrepo.com/show/452030/avatar-default.svg'
    }))

    customers.value = payload.data || []
    pageSize.value = Number(payload.size ?? params.size)
    totalElements.value = Number(payload.totalElements ?? 0)
    totalPages.value = Number(payload.totalPages ?? 1)
  } catch (error) {
    console.error('Lỗi khi tải danh sách khách hàng:', error)
    customers.value = []
    totalElements.value = 0
    totalPages.value = 1
    message.error('Có lỗi xảy ra khi tải dữ liệu khách hàng')
  } finally {
    loading.value = false
  }
}

// =============================================================================
// EVENT HANDLERS
// =============================================================================

function handleAutoFilter() {
  if (searchTimer) {
    clearTimeout(searchTimer)
  }

  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadCustomers()
  }, SEARCH_DEBOUNCE_DELAY)
}

function refreshTable() {
  if (searchTimer) {
    clearTimeout(searchTimer)
    searchTimer = null
  }

  Object.assign(formSearch, {
    customerName: "",
    customerGender: null,
    customerStatus: STATUS_OPTIONS.ALL,
  })
  currentPage.value = 1
  loadCustomers()
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadCustomers()
}

// =============================================================================
// MODAL FUNCTIONS
// =============================================================================

function resetForm() {
  form.id = undefined
  form.customerName = ''
  form.customerPhone = ''
  form.customerEmail = ''
  form.customerGender = GENDER.MALE
  form.customerStatus = STATUS_OPTIONS.ACTIVE
  form.customerBirthdayStr = null
  avatarState.file = null
  avatarState.uploading = false
  if (avatarState.preview) URL.revokeObjectURL(avatarState.preview)
  avatarState.preview = ''
  avatarState.url = ''
}

// ✅ CẬP NHẬT HÀM populateFormWithCustomer - không cần convert URL
function populateFormWithCustomer(customer: Customer) {
  form.id = customer.id
  form.customerName = customer.customerName || ''
  form.customerPhone = customer.customerPhone || ''
  form.customerEmail = customer.customerEmail || ''
  form.customerGender = customer.customerGender ? GENDER.MALE : GENDER.FEMALE
  form.customerStatus = customer.customerStatus ?? STATUS_OPTIONS.ACTIVE
  form.customerBirthdayStr = customer.customerBirthday
    ? new Date(customer.customerBirthday).toISOString().slice(0, 10)
    : null

  // ✅ Backend đã trả full URL, dùng trực tiếp
  form.customerAvatar = customer.customerAvatar || ''
}

function openModal(mode: 'add' | 'edit', customer?: Customer) {
  modalMode.value = mode

  if (mode === 'edit' && customer) {
    populateFormWithCustomer(customer)
  } else {
    resetForm()
  }

  nextTick(() => {
    uploadRef.value?.clear?.()
    // Bảo hiểm: reset cờ
    avatarState.uploading = false
    showModal.value = true
  })
}

function resetAvatar() {
  if (avatarState.preview) {
    URL.revokeObjectURL(avatarState.preview)
  }
  avatarState.file = null
  avatarState.preview = ''
  avatarState.url = ''
  avatarState.uploading = false
  form.customerAvatar = ''
  uploadRef.value?.clear?.()
}

function closeModal() {
  showModal.value = false
}

function afterModalLeave() {
  resetForm()
  formRef.value?.restoreValidation()
  uploadRef.value?.clear?.()
  if (avatarState.preview) URL.revokeObjectURL(avatarState.preview)
  avatarState.preview = ''
  avatarState.url = ''
  avatarState.uploading = false
  submitting.value = false
  resetAvatar()
}

function onModalOpened() {
  nextTick(() => {
    const firstInput = document.querySelector('.n-modal input')
    if (firstInput instanceof HTMLInputElement) {
      firstInput.focus()
    }
  })
}

// =============================================================================
// FORM SUBMISSION
// =============================================================================

const buildCustomerPayload = (): Customer => ({
  customerName: form.customerName,
  customerPhone: form.customerPhone,
  customerEmail: form.customerEmail,
  customerGender: form.customerGender === GENDER.MALE,
  customerStatus: form.customerStatus ?? STATUS_OPTIONS.ACTIVE,
  customerBirthday: form.customerBirthdayStr
    ? new Date(form.customerBirthdayStr).getTime()
    : undefined,
  customerAvatar: form.customerAvatar || undefined,
})

async function onSubmit() {

  if (avatarState.uploading && !avatarState.url) {
    return message.warning('Ảnh đang tải lên, vui lòng đợi xíu nhé!')
  }

  if (form.customerAvatar && /^(blob:|data:)/.test(form.customerAvatar)) {
    return message.error('Ảnh chưa tải xong. Vui lòng thử lại!')
  }

  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    const payload = buildCustomerPayload()

    if (form.id) {
      await updateCustomer(String(form.id), payload)
      message.success('Cập nhật khách hàng thành công!')
    } else {
      await createCustomer(payload)
      message.success('Thêm khách hàng thành công!')
    }

    await loadCustomers()
    closeModal()
  } catch (error) {
    console.error('Lỗi khi lưu khách hàng:', error)
    message.error('Có lỗi xảy ra khi lưu khách hàng!')
  } finally {
    submitting.value = false
  }
}

// =============================================================================
// CUSTOMER OPERATIONS
// =============================================================================

async function toggleStatus(customer: Customer, uiValue?: boolean) {
  const originalStatus = customer.customerStatus
  const newStatus = typeof uiValue === 'boolean'
    ? (uiValue ? STATUS_OPTIONS.ACTIVE : STATUS_OPTIONS.INACTIVE)
    : (originalStatus === STATUS_OPTIONS.ACTIVE ? STATUS_OPTIONS.INACTIVE : STATUS_OPTIONS.ACTIVE)

  try {
    // Optimistic UI update
    const idx = customers.value.findIndex(c => c.id === customer.id)
    if (idx !== -1) customers.value[idx].customerStatus = newStatus

    const updatedCustomer: Customer = {
      ...customer,
      customerStatus: newStatus,
    }

    await updateCustomer(customer.id!, updatedCustomer)
    message.success('Cập nhật trạng thái thành công!')
  } catch (error) {
    // Rollback on error
    const idx = customers.value.findIndex(c => c.id === customer.id)
    if (idx !== -1) customers.value[idx].customerStatus = originalStatus

    console.error('Lỗi khi cập nhật trạng thái:', error)
    message.error('Có lỗi khi cập nhật trạng thái!')
  }
}

async function handleDeleteCustomer(id: string | number) {
  try {
    await deleteCustomerApi(String(id))
    customers.value = customers.value.filter(c => c.id !== id)
    totalElements.value = Math.max(0, totalElements.value - 1)
    message.success(`Đã xóa khách hàng ID: ${id}`)

    if (customers.value.length === 0 && currentPage.value > 1) {
      currentPage.value = currentPage.value - 1
      await loadCustomers()
    }
  } catch (error) {
    console.error('Lỗi khi xóa khách hàng:', error)
    message.error('Có lỗi xảy ra khi xóa khách hàng!')
  }
}

async function handleBatchDelete() {
  try {
    const ids = [...checkedRowKeys.value]
    if (ids.length === 0) return

    const results = await Promise.allSettled(
      ids.map(id => deleteCustomerApi(String(id)))
    )
    const successCount = results.filter(r => r.status === 'fulfilled').length

    customers.value = customers.value.filter(c => !ids.includes(c.id as never))
    checkedRowKeys.value = []
    totalElements.value = Math.max(0, totalElements.value - successCount)

    message.success(`Đã xóa ${successCount} khách hàng`)

    if (customers.value.length === 0 && currentPage.value > 1) {
      currentPage.value = 1
      await loadCustomers()
    }
  } catch (error) {
    console.error('Lỗi khi xóa hàng loạt:', error)
    message.error('Có lỗi xảy ra khi xóa khách hàng!')
  }
}

// =============================================================================
// LIFECYCLE
// =============================================================================

onMounted(() => {
  loadCustomers()
})
</script>
