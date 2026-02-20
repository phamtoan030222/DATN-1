<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NForm,
  NFormItem,
  NGrid,
  NGridItem,
  NIcon,
  NInput,
  NModal,
  NPopconfirm,
  NRadio,
  NRadioGroup,
  NSpace,
  NSwitch,
  NTag,
  NTimePicker,
  NTooltip,
  useDialog,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { shiftApi } from '@/service/api/admin/shift/shift'
import type { Shift } from '@/service/api/admin/shift/shift'

// --- STATE ---
const message = useMessage()
const dialog = useDialog()
const shifts = ref<Shift[]>([])
const loading = ref(false)
const showModal = ref(false)
const submitting = ref(false)
const formRef = ref<any>(null)

// Bộ lọc: Mặc định chọn 0 (Chỉ hiện ca Đang hoạt động)
const filter = reactive({
  keyword: '',
  status: 0 as number | null,
  startTime: null as string | null,
  endTime: null as string | null,
})

// Form Data (Thêm hasHistory)
const form = reactive({
  id: null as number | null,
  code: '',
  name: '',
  startTime: '08:00',
  endTime: '12:00',
  status: 0,
  hasHistory: false, // <-- Khai báo biến
})

const rules = {
  name: {
    required: true,
    message: 'Vui lòng nhập tên ca',
    trigger: ['blur', 'input'],
  },
  startTime: {
    required: true,
    message: 'Chọn giờ bắt đầu',
    trigger: ['blur', 'change'],
  },
  endTime: [
    {
      required: true,
      message: 'Chọn giờ kết thúc',
      trigger: ['blur', 'change'],
    },
    {
      validator: (rule: any, value: string) => {
        if (!value || !form.startTime)
          return true

        if (value <= form.startTime) {
          return new Error('Giờ kết thúc phải lớn hơn giờ bắt đầu')
        }
        return true
      },
      trigger: ['blur', 'change'],
    },
  ],
}

// --- LOGIC API ---
async function loadData() {
  loading.value = true
  try {
    const res = await shiftApi.getAll()
    shifts.value = res.data || []
  }
  catch (e) {
    message.error('Lỗi tải dữ liệu')
  }
  finally {
    loading.value = false
  }
}

// Logic Lọc & Sắp xếp (Computed)
const filteredShifts = computed(() => {
  const result = shifts.value.filter((item) => {
    const currentStatus = (item.status === 0 || item.status === 'ACTIVE') ? 0 : 1
    const term = filter.keyword?.toLowerCase().trim() || ''
    const matchName = item.name?.toLowerCase().includes(term)
    const matchCode = item.code?.toLowerCase().includes(term)
    const matchStatus = filter.status === null || currentStatus === filter.status

    let matchTime = true
    if (filter.startTime && item.startTime < filter.startTime)
      matchTime = false
    if (filter.endTime && item.endTime > filter.endTime)
      matchTime = false

    return (matchName || matchCode) && matchStatus && matchTime
  })

  return result.sort((a, b) => {
    const getScore = (s: any) => (s === 0 || s === 'ACTIVE') ? 0 : 1
    const scoreA = getScore(a.status)
    const scoreB = getScore(b.status)

    if (scoreA !== scoreB) {
      return scoreA - scoreB
    }

    const timeA = a.startTime || '99:99'
    const timeB = b.startTime || '99:99'

    return timeA.localeCompare(timeB)
  })
})

function resetFilters() {
  filter.keyword = ''
  filter.status = 0
  filter.startTime = null
  filter.endTime = null
}

function openModal(row?: Shift) {
  if (row) {
    Object.assign(form, row)
  }
  else {
    // Reset form và cờ lịch sử khi Thêm mới
    Object.assign(form, { id: null, code: '', name: '', startTime: '08:00', endTime: '12:00', status: 0, hasHistory: false })
  }
  showModal.value = true
}

// --- HÀM XỬ LÝ LỖI ---
function getErrorMessage(e: any) {
  console.log('Error Log:', e)
  if (e.response && e.response.data) {
    if (typeof e.response.data === 'string') {
      return e.response.data
    }
    if (e.response.data.message) {
      return e.response.data.message
    }
  }
  return 'Lỗi hệ thống, vui lòng thử lại sau!'
}

// --- LOGIC LƯU (THÊM/SỬA) ---
async function executeSave() {
  submitting.value = true
  try {
    await shiftApi.create(form)
    const msgAction = form.id ? 'Cập nhật' : 'Thêm mới'
    message.success(`${msgAction} thành công!`)
    showModal.value = false
    loadData()
  }
  catch (e: any) {
    message.error(getErrorMessage(e))
  }
  finally {
    submitting.value = false
  }
}

// --- LOGIC ĐỔI TRẠNG THÁI ---
async function handleStatusChange(row: Shift) {
  const originalStatus = row.status
  const isCurrentActive = row.status === 0 || row.status === 'ACTIVE'
  const nextStatus = isCurrentActive ? 1 : 0
  const actionText = !isCurrentActive ? 'Kích hoạt' : 'Ngưng hoạt động'

  try {
    row.status = nextStatus
    await shiftApi.create(row)
    message.success(`Đã ${actionText} thành công!`)
  }
  catch (e: any) {
    row.status = originalStatus
    message.error(getErrorMessage(e))
  }
}

function handleSave() {
  formRef.value?.validate((errors: any) => {
    if (errors)
      return
    const actionText = form.id ? 'cập nhật' : 'thêm mới'
    dialog.warning({
      title: form.id ? 'Xác nhận cập nhật' : 'Xác nhận thêm mới',
      content: `Bạn có chắc chắn muốn ${actionText} ca làm việc này không?`,
      positiveText: 'Đồng ý',
      negativeText: 'Hủy bỏ',
      positiveButtonProps: { style: { backgroundColor: '#16a34a', borderColor: '#16a34a', color: '#fff' } },
      onPositiveClick: async () => { await executeSave() },
    })
  })
}

function formatTimeDisplay(time: string) {
  if (!time)
    return '--:--'
  return time.length > 5 ? time.slice(0, 5) : time
}

onMounted(loadData)

// --- CẤU HÌNH CỘT ---
const columns = [
  { title: 'STT', key: 'stt', width: 60, align: 'center', render: (_, index) => index + 1 },
  {
    title: 'Thông tin Ca',
    key: 'info',
    align: 'center',
    width: 250,
    render: (row: Shift) => {
      const isActive = row.status === 0 || row.status === 'ACTIVE'
      const iconBgColor = isActive ? 'bg-green-50' : 'bg-gray-100'
      const iconTextColor = isActive ? 'text-green-600' : 'text-gray-400'
      const textColor = isActive ? 'text-gray-800' : 'text-gray-400 line-through'

      return h('div', { class: 'w-full flex items-center justify-center gap-3' }, [
        h('div', { class: `w-10 h-10 rounded-full ${iconBgColor} flex items-center justify-center ${iconTextColor} shadow-sm flex-shrink-0` }, h(NIcon, { size: 20 }, { default: () => h(Icon, { icon: 'carbon:time' }) })),
        h('div', { class: 'flex flex-col text-left' }, [
          h('span', { class: `font-bold text-[14px] ${textColor}` }, row.name),
          h('span', { class: 'text-xs text-gray-500 font-mono mt-0.5' }, row.code),
        ]),
      ])
    },
  },
  {
    title: 'Giờ bắt đầu',
    key: 'startTime',
    align: 'center',
    width: 150,
    render: (row: Shift) => {
      const isActive = row.status === 0 || row.status === 'ACTIVE'
      return h(NTag, { type: isActive ? 'success' : 'default', bordered: false, class: 'font-mono px-3 text-sm rounded-lg' }, { default: () => formatTimeDisplay(row.startTime) })
    },
  },
  {
    title: 'Giờ kết thúc',
    key: 'endTime',
    align: 'center',
    width: 150,
    render: (row: Shift) => {
      const isActive = row.status === 0 || row.status === 'ACTIVE'
      return h(NTag, { type: isActive ? 'error' : 'default', bordered: false, class: 'font-mono px-3 text-sm rounded-lg' }, { default: () => formatTimeDisplay(row.endTime) })
    },
  },
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 150,
    render: (row: Shift) => {
      const isActive = row.status === 0 || row.status === 'ACTIVE'
      return h(NPopconfirm, {
        onPositiveClick: () => handleStatusChange(row),
        positiveText: 'Đồng ý',
        negativeText: 'Hủy',
      }, {
        trigger: () => h('div', { class: 'cursor-pointer inline-block' }, [
          h(NSwitch, {
            value: isActive,
            size: 'medium',
            railStyle: ({ checked }) => checked ? { backgroundColor: '#10b981' } : {},
            style: 'pointer-events: none',
          }, { checked: () => '', unchecked: () => '' }),
        ]),
        default: () => `Bạn có chắc chắn muốn ${isActive ? 'ngưng hoạt động' : 'kích hoạt'} ca này?`,
      })
    },
  },
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    fixed: 'right',
    width: 120,
    render(row: Shift) {
      return h(NSpace, { justify: 'center' }, { default: () => [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, { size: 'small', circle: true, secondary: true, type: 'warning', class: 'bg-orange-50 text-orange-600 border-orange-200 hover:bg-orange-100', onClick: () => openModal(row) }, { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) }),
          default: () => 'Sửa thông tin',
        }),
      ] })
    },
  },
]
</script>

<template>
  <div class="p-4 bg-gray-50/50 min-h-screen">
    <NCard class="mb-4 rounded-xl shadow-sm border border-gray-100" content-style="padding: 16px 24px;">
      <div class="flex items-center gap-3">
        <NIcon size="28" class="text-green-600">
          <Icon icon="carbon:time" />
        </NIcon>
        <div class="flex flex-col">
          <span class="text-xl font-bold text-gray-800">Quản lý Ca làm việc</span>
          <span class="text-gray-500 text-sm">Thiết lập khung giờ và phân ca cho nhân viên</span>
        </div>
      </div>
    </NCard>

    <NCard title="Bộ lọc tìm kiếm" class="mb-4 rounded-xl shadow-sm border border-gray-100">
      <template #header-extra>
        <div class="mr-4">
          <NTooltip trigger="hover">
            <template #trigger>
              <NButton circle secondary size="medium" class="hover:bg-gray-100" @click="resetFilters">
                <template #icon>
                  <NIcon class="text-gray-600">
                    <Icon icon="carbon:filter-reset" />
                  </NIcon>
                </template>
              </NButton>
            </template>
            Làm mới bộ lọc
          </NTooltip>
        </div>
      </template>
      <NForm label-placement="top">
        <NGrid x-gap="24" cols="1 600:2 1024:4">
          <NGridItem>
            <NFormItem label="Tìm kiếm chung">
              <NInput v-model:value="filter.keyword" placeholder="Nhập tên ca, mã ca..." clearable size="large">
                <template #prefix>
                  <Icon icon="carbon:search" class="text-gray-400" />
                </template>
              </NInput>
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NFormItem label="Thời gian bắt đầu">
              <NTimePicker v-model:formatted-value="filter.startTime" value-format="HH:mm" format="HH:mm" placeholder="HH:mm" size="large" class="w-full" clearable />
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NFormItem label="Thời gian kết thúc">
              <NTimePicker v-model:formatted-value="filter.endTime" value-format="HH:mm" format="HH:mm" placeholder="HH:mm" size="large" class="w-full" clearable />
            </NFormItem>
          </NGridItem>
          <NGridItem>
            <NFormItem label="Trạng thái">
              <NRadioGroup v-model:value="filter.status" name="statusGroup" size="large">
                <NSpace>
                  <NRadio :value="null">
                    Tất cả
                  </NRadio>
                  <NRadio :value="0">
                    Hoạt động
                  </NRadio>
                  <NRadio :value="1">
                    Ngưng
                  </NRadio>
                </NSpace>
              </NRadioGroup>
            </NFormItem>
          </NGridItem>
        </NGrid>
      </NForm>
    </NCard>

    <NCard title="Danh sách Ca làm việc" class="rounded-xl shadow-sm border border-gray-100">
      <template #header-extra>
        <div class="flex items-center gap-3 mr-2">
          <button class="group flex items-center justify-center rounded-full bg-green-600 text-white h-9 px-3 transition-all duration-300 hover:pr-4 shadow-sm hover:bg-green-700" @click="openModal()">
            <NIcon size="20">
              <Icon icon="carbon:add" />
            </NIcon>
            <span class="max-w-0 overflow-hidden opacity-0 group-hover:max-w-[100px] group-hover:opacity-100 group-hover:ml-2 transition-all duration-300 whitespace-nowrap text-sm font-medium">Thêm mới</span>
          </button>
          <NTooltip trigger="hover">
            <template #trigger>
              <NButton circle secondary size="medium" class="bg-blue-50 text-blue-600 border-blue-100 hover:bg-blue-100" @click="loadData">
                <template #icon>
                  <NIcon><Icon icon="carbon:rotate" /></NIcon>
                </template>
              </NButton>
            </template>
            Tải lại
          </NTooltip>
        </div>
      </template>
      <NDataTable :columns="columns" :data="filteredShifts" :loading="loading" :row-key="(row) => row.id" striped :pagination="{ pageSize: 10 }" class="mt-2" />
    </NCard>

    <NModal
      v-model:show="showModal"
      preset="card"
      :title="form.id ? 'Cập nhật Ca làm việc' : 'Thêm Ca Mới'"
      class="rounded-xl shadow-2xl"
      :style="{ width: '600px' }"
      :bordered="false"
      size="huge"
    >
      <NForm ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="120" require-mark-placement="right-hanging" size="large" class="mt-2 px-2">
        <NFormItem label="Tên Ca" path="name">
          <NInput v-model:value="form.name" placeholder="Ví dụ: Ca Sáng, Ca Chiều..." class="w-full" />
        </NFormItem>
        <NFormItem label="Thời gian" path="startTime">
          <div class="flex flex-col w-full">
            <div class="grid grid-cols-2 gap-4 w-full items-center">
              <div class="relative">
                <NTimePicker v-model:formatted-value="form.startTime" value-format="HH:mm" format="HH:mm" placeholder="Bắt đầu" class="w-full" :disabled="form.hasHistory" />
              </div>
              <div class="relative flex items-center gap-2">
                <span class="text-gray-400 shrink-0"><Icon icon="carbon:arrow-right" /></span>
                <NTimePicker v-model:formatted-value="form.endTime" value-format="HH:mm" format="HH:mm" placeholder="Kết thúc" class="w-full" :disabled="form.hasHistory" />
              </div>
            </div>

            <div v-if="form.hasHistory" class="text-amber-600 text-[13px] mt-2 flex items-start gap-1">
              <NIcon class="mt-[2px]">
                <Icon icon="carbon:warning-alt" />
              </NIcon>
              <span>Ca này đã có lịch sử làm việc. Bạn chỉ có thể đổi tên ca.</span>
            </div>
          </div>
        </NFormItem>
      </NForm>
      <template #footer>
        <div class="flex justify-end gap-3 pt-2 border-t border-gray-50">
          <NButton size="medium" class="px-6 rounded-lg font-medium text-gray-600 hover:bg-gray-100" @click="showModal = false">
            Hủy bỏ
          </NButton>
          <NButton type="primary" class="bg-green-600 hover:bg-green-700 border-none px-6 rounded-lg font-bold shadow-sm" :loading="submitting" size="medium" @click="handleSave">
            <template #icon>
              <NIcon><Icon :icon="form.id ? 'carbon:save' : 'carbon:add'" /></NIcon>
            </template>
            {{ form.id ? 'Cập nhật' : 'Thêm mới' }}
          </NButton>
        </div>
      </template>
    </NModal>
  </div>
</template>

<style scoped>
:deep(.n-input), :deep(.n-input .n-input-wrapper) { border-radius: 8px !important; }
:deep(.n-card-header) { padding-bottom: 20px !important; border-bottom: 1px solid #f3f4f6; }
:deep(.n-card__content) { padding-top: 30px !important; padding-bottom: 30px !important; }
</style>
