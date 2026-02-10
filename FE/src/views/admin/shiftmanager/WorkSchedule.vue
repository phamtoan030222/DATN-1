<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import {
  NAvatar,
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NIcon,
  NInput,
  NModal,
  NPopconfirm,
  NRadioButton,
  NRadioGroup,
  NSelect,
  NSpace,
  NSpin,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'

// --- API IMPORTS ---
import { shiftApi } from '@/service/api/admin/shift/shift'
import type { Shift } from '@/service/api/admin/shift/shift'
import { scheduleApi } from '@/service/api/admin/shift/schedule'
import type { WorkSchedule } from '@/service/api/admin/shift/schedule'
import { getAllStaff } from '@/service/api/admin/users/staff.api'
import type { StaffResponse } from '@/service/api/admin/users/staff.api'

const message = useMessage()
const loading = ref(false)
const shifts = ref<Shift[]>([])
const staffList = ref<StaffResponse[]>([])
const schedules = ref<WorkSchedule[]>([])
const showModal = ref(false)

// State View
const viewMode = ref<'matrix' | 'list'>('matrix')
const anchorDate = ref(Date.now())

// Filter
const filterParams = reactive({
  staffName: '',
  shiftName: '',
  dateRange: null as [number, number] | null,
})

// Pagination Configuration
const pagination = reactive({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [5, 10, 20, 50],
  onChange: (page: number) => {
    pagination.page = page
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
  },
  prefix: ({ itemCount }: any) => `Tổng số ${itemCount} lịch`,
})

// Modal State
const assignState = reactive({
  shift: null as Shift | null,
  date: '',
  dateLabel: '',
  staffId: null as string | number | null,
  scheduleId: null as number | null,
})

// 1. Logic chuẩn bị dữ liệu ngày
const weekDays = computed(() => {
  const days = []
  const currentAnchor = new Date(anchorDate.value)
  const currentDay = currentAnchor.getDay()
  const diff = currentAnchor.getDate() - currentDay + (currentDay === 0 ? -6 : 1)
  const monday = new Date(currentAnchor)
  monday.setDate(diff)

  for (let i = 0; i < 7; i++) {
    const d = new Date(monday)
    d.setDate(monday.getDate() + i)
    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const dayDate = String(d.getDate()).padStart(2, '0')
    const dateString = `${year}-${month}-${dayDate}`
    const dayOfWeek = d.getDay()
    const label = dayOfWeek === 0 ? 'CN' : `Thứ ${dayOfWeek + 1}`
    days.push({ label, date: dateString, isToday: new Date().toLocaleDateString('sv') === dateString })
  }
  return days
})

// --- LOGIC ĐIỀU HƯỚNG TUẦN ---
function changeWeek(offset: number) {
  const newDate = new Date(anchorDate.value)
  newDate.setDate(newDate.getDate() + (offset * 7))
  anchorDate.value = newDate.getTime()
}

function setToday() {
  anchorDate.value = Date.now()
}

watch(anchorDate, () => {
  loadSchedule()
})

function getErrorMessage(e: any) {
  if (e.response && e.response.data) {
    if (typeof e.response.data === 'string')
      return e.response.data
    if (e.response.data.message)
      return e.response.data.message
  }
  return 'Lỗi hệ thống, vui lòng thử lại sau!'
}

async function loadData() {
  loading.value = true
  try {
    const sRes = await shiftApi.getAll()
    shifts.value = (sRes.data || []).sort((a: any, b: any) => (a.id || 0) - (b.id || 0))
    const stRes = await getAllStaff({ page: 1, size: 1000, status: 'ACTIVE' })
    staffList.value = stRes.data?.data || stRes.data || stRes.items || []
    await loadSchedule()
  }
  catch (e) {
    message.error('Lỗi tải dữ liệu hệ thống')
  }
  finally {
    loading.value = false
  }
}

async function loadSchedule() {
  if (weekDays.value.length === 0)
    return
  const from = weekDays.value[0].date
  const to = weekDays.value[6].date
  try {
    const res = await scheduleApi.getWeekly(from, to)
    schedules.value = res.data?.data || res.data || res || []
  }
  catch (e) {
    schedules.value = []
  }
}

function getAssign(shiftId: any, date: string) {
  return schedules.value.find(s => (s.shift?.id == shiftId) && (s.workDate === date))
}

async function handleAssign() {
  if (!assignState.staffId)
    return message.warning('Chưa chọn nhân viên')
  try {
    const staff = staffList.value.find(s => s.id === assignState.staffId)
    await scheduleApi.assign({
      shiftId: assignState.shift!.id!,
      staffId: assignState.staffId,
      workDate: assignState.date,
      staffName: staff?.fullName,
    })
    message.success(`Đã xếp lịch cho: ${staff?.fullName}`)
    showModal.value = false
    loadSchedule()
  }
  catch (e: any) {
    message.error(getErrorMessage(e))
  }
}

async function removeAssign(id: any) {
  try {
    await scheduleApi.delete(id)
    message.success('Đã hủy lịch làm việc')
    loadSchedule()
  }
  catch (e: any) {
    message.error(getErrorMessage(e))
  }
}

function getAvatar(staffId: any) {
  if (!staffId)
    return null
  const staff = staffList.value.find(s => s.id == staffId)
  return staff?.avatarUrl || staff?.avatar || staff?.image || null
}

// --- LOGIC: Xử lý nút Sửa từ bảng ---
function handleEditFromTable(row: WorkSchedule) {
  if (!row.shift || !row.workDate)
    return
  assignState.shift = row.shift
  assignState.date = row.workDate
  assignState.staffId = row.staff?.id || null
  const d = new Date(row.workDate)
  const dayOfWeek = d.getDay()
  const label = dayOfWeek === 0 ? 'CN' : `Thứ ${dayOfWeek + 1}`
  assignState.dateLabel = `${label} (${row.workDate})`
  showModal.value = true
}

// --- CẤU HÌNH CỘT BẢNG (ĐÃ SỬA FONT CHỮ GIỐNG BẢNG NHÂN VIÊN) ---
const tableColumns = [
  // 1. Ngày làm việc
  {
    title: 'Ngày làm việc',
    key: 'workDate',
    width: 150,
    fixed: 'left',
    sorter: (a: any, b: any) => new Date(a.workDate).getTime() - new Date(b.workDate).getTime(),
    render(row: WorkSchedule) {
      return h('div', { class: 'flex items-center gap-2' }, [
        // Icon nhạt màu
        h(Icon, { icon: 'carbon:calendar', class: 'text-gray-400 text-base' }),
        // Chữ thường, màu xám đậm (#374151), không in đậm
        h('span', { style: 'font-size: 14px; color: #374151;' }, row.workDate),
      ])
    },
  },
  // 2. Bắt đầu (Đã bỏ font-mono và in đậm)
  {
    title: 'Bắt đầu',
    key: 'startTime',
    align: 'center',
    width: 150,
    render(row: WorkSchedule) {
      // Dùng style chữ thường giống hệt cột SĐT bên Staff
      return h('span', { style: 'font-size: 14px; color: #374151;' }, row.shift?.startTime?.slice(0, 5))
    },
  },
  // 3. Kết thúc (Đã bỏ font-mono và in đậm)
  {
    title: 'Kết thúc',
    key: 'endTime',
    align: 'center',
    width: 150,
    render(row: WorkSchedule) {
      // Dùng style chữ thường giống hệt cột SĐT bên Staff
      return h('span', { style: 'font-size: 14px; color: #374151;' }, row.shift?.endTime?.slice(0, 5))
    },
  },
  // 4. Ca làm việc
  {
    title: 'Ca làm việc',
    key: 'shiftName',
    align: 'center',
    width: 150,
    render(row: WorkSchedule) {
      // Giữ Tag nhưng làm nhạt đi cho tiệp màu
      return h(NTag, { type: 'default', bordered: true, round: true, size: 'small', class: 'px-3 text-gray-600' }, { default: () => row.shift?.name })
    },
  },
  // 5. Nhân viên (Giữ nguyên style đẹp đã làm ở bước trước)
  {
    title: 'Nhân viên',
    key: 'staffName',
    minWidth: 160,
    render(row: WorkSchedule) {
      const avatarUrl = getAvatar(row.staff?.id)
      return h(NSpace, { align: 'center', justify: 'start', size: 10 }, {
        default: () => [
          h(NAvatar, {
            size: 'small',
            round: true,
            src: avatarUrl,
            fallbackSrc: 'https://via.placeholder.com/40',
            style: 'border: 1px solid #e5e7eb;',
          }),
          h('div', [
            h('div', { style: 'font-weight: 500; color: #1f2225;' }, row.staff?.name || row.staffName || '---'),
            h('div', { style: 'font-size: 12px; color: #888' }, row.staff?.code || `NV-${row.staff?.id}`),
          ]),
        ],
      })
    },
  },
  // 6. Trạng thái
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 120,
    render(row: WorkSchedule) {
      const now = new Date()
      const workDate = new Date(row.workDate!)
      const isPast = workDate.setHours(0, 0, 0, 0) < now.setHours(0, 0, 0, 0)
      const type = isPast ? 'info' : 'success'
      const label = isPast ? 'Đã làm' : 'Dự kiến'

      return h(NTag, {
        type,
        bordered: false,
        round: true,
        size: 'small',
        style: isPast ? 'background-color: #e0f2fe; color: #0284c7; font-weight: 600;' : 'background-color: #dcfce7; color: #16a34a; font-weight: 600;',
      }, { default: () => label })
    },
  },
  // 7. Thao tác
  {
    title: 'Thao tác',
    key: 'actions',
    align: 'center',
    width: 90,
    fixed: 'right',
    render(row: WorkSchedule) {
      return h('div', { class: 'flex items-center justify-center gap-2' }, [
        h(NTooltip, { trigger: 'hover' }, {
          trigger: () => h(NButton, {
            size: 'small',
            circle: true,
            secondary: true,
            type: 'warning',
            onClick: () => handleEditFromTable(row),
          }, { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:edit' }) }) }),
          default: () => 'Sửa lịch',
        }),
        h(NPopconfirm, {
          onPositiveClick: () => removeAssign(row.id),
          positiveText: 'Xóa',
          negativeText: 'Hủy',
        }, {
          trigger: () => h(NButton, {
            circle: true,
            size: 'small',
            type: 'error',
            secondary: true,
          }, { icon: () => h(NIcon, null, { default: () => h(Icon, { icon: 'carbon:trash-can' }) }) }),
          default: () => 'Xóa lịch làm việc này?',
        }),
      ])
    },
  },
]

// Computed lọc và sắp xếp dữ liệu
const filteredSchedules = computed(() => {
  // 1. Lọc
  const result = schedules.value.filter((item) => {
    const matchName = !filterParams.staffName || (item.staff?.name || '').toLowerCase().includes(filterParams.staffName.toLowerCase())
    const matchShift = !filterParams.shiftName || (item.shift?.name || '').toLowerCase().includes(filterParams.shiftName.toLowerCase())
    let matchDate = true
    if (filterParams.dateRange) {
      const itemTime = new Date(item.workDate!).getTime()
      matchDate = itemTime >= filterParams.dateRange[0] && itemTime <= filterParams.dateRange[1]
    }
    return matchName && matchShift && matchDate
  })

  // 2. Sắp xếp: Ngày -> Giờ -> Tên
  return result.sort((a, b) => {
    const dateA = new Date(a.workDate!).getTime()
    const dateB = new Date(b.workDate!).getTime()
    if (dateA !== dateB)
      return dateA - dateB

    const timeA = a.shift?.startTime || '00:00'
    const timeB = b.shift?.startTime || '00:00'
    if (timeA !== timeB)
      return timeA.localeCompare(timeB)

    const nameA = a.staff?.name || ''
    const nameB = b.staff?.name || ''
    return nameA.localeCompare(nameB)
  })
})

function openAssignModal(shift: Shift, day: any) {
  assignState.shift = shift
  assignState.date = day.date
  assignState.dateLabel = day.label
  const existing = getAssign(shift.id!, day.date)
  assignState.staffId = existing && existing.staff ? existing.staff.id : null
  showModal.value = true
}

function clearFilters() {
  filterParams.staffName = ''
  filterParams.shiftName = ''
  filterParams.dateRange = null
  pagination.page = 1
}

onMounted(loadData)
</script>

<template>
  <div class="p-4 md:p-6 bg-slate-50 min-h-screen">
    <div class="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4">
      <div class="flex items-center gap-3">
        <div class="p-3 bg-emerald-100 text-emerald-600 rounded-xl shadow-sm border border-emerald-200">
          <NIcon size="28">
            <Icon icon="carbon:calendar-heat-map" />
          </NIcon>
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800 m-0">
            Xếp lịch nhân viên
          </h1>
          <p class="text-gray-500 text-sm">
            Quản lý và phân ca làm việc
          </p>
        </div>
      </div>

      <div class="flex gap-2">
        <NRadioGroup v-model:value="viewMode" size="medium">
          <NRadioButton value="matrix">
            <div class="flex items-center gap-1">
              <Icon icon="carbon:grid" /> Lịch biểu
            </div>
          </NRadioButton>
          <NRadioButton value="list">
            <div class="flex items-center gap-1">
              <Icon icon="carbon:list" /> Danh sách
            </div>
          </NRadioButton>
        </NRadioGroup>

        <NButton type="primary" class="rounded-full px-4 !bg-emerald-600 hover:!bg-emerald-700" :loading="loading" @click="loadData">
          <template #icon>
            <NIcon><Icon icon="carbon:rotate" /></NIcon>
          </template>
          Làm mới
        </NButton>
      </div>
    </div>

    <NCard class="mb-4 rounded-xl shadow-sm border-gray-200" size="small" :bordered="false">
      <div class="flex flex-col md:flex-row justify-between items-center gap-4">
        <div class="flex items-center gap-2">
          <div class="flex items-center bg-gray-50 p-1 rounded-lg border border-gray-200">
            <NButton quaternary circle size="small" @click="changeWeek(-1)">
              <template #icon>
                <Icon icon="carbon:chevron-left" />
              </template>
            </NButton>

            <NDatePicker
              v-model:value="anchorDate"
              type="date"
              size="small"
              class="w-[130px] text-center"
              :clearable="false"
              :actions="null"
            />

            <NButton quaternary circle size="small" @click="changeWeek(1)">
              <template #icon>
                <Icon icon="carbon:chevron-right" />
              </template>
            </NButton>
          </div>

          <NButton size="small" strong secondary type="success" @click="setToday">
            Hôm nay
          </NButton>
        </div>

        <div class="text-sm font-bold text-emerald-700 bg-emerald-50 px-3 py-1 rounded-md border border-emerald-100">
          Tuần từ: {{ weekDays[0]?.date }} đến {{ weekDays[6]?.date }}
        </div>
      </div>

      <div v-if="viewMode === 'list'" class="mt-4 pt-4 border-t border-gray-100">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-3">
          <NInput v-model:value="filterParams.staffName" placeholder="Tìm tên nhân viên..." clearable>
            <template #prefix>
              <Icon icon="carbon:search" class="text-gray-400" />
            </template>
          </NInput>

          <NInput v-model:value="filterParams.shiftName" placeholder="Tìm tên ca làm việc..." clearable>
            <template #prefix>
              <Icon icon="carbon:time" class="text-gray-400" />
            </template>
          </NInput>

          <NDatePicker
            v-model:value="filterParams.dateRange"
            type="daterange"
            clearable
            :actions="null"
            close-on-select
            start-placeholder="Ngày bắt đầu"
            end-placeholder="Ngày kết thúc"
            class="w-full"
          />

          <NButton dashed block @click="clearFilters">
            <template #icon>
              <Icon icon="carbon:filter-remove" />
            </template>
            Xóa bộ lọc
          </NButton>
        </div>
      </div>
    </NCard>

    <div class="bg-white rounded-2xl shadow-md border border-gray-200 overflow-hidden">
      <div v-if="viewMode === 'matrix'" class="overflow-x-auto">
        <div class="grid grid-cols-8 border-b-2 border-gray-200 bg-gray-50 min-w-[1000px]">
          <div class="p-4 font-bold text-gray-600 text-center uppercase text-xs tracking-wider border-r-2 border-gray-200 bg-gray-100 sticky left-0 z-10">
            Ca / Ngày
          </div>
          <div
            v-for="day in weekDays" :key="day.date"
            class="p-3 text-center border-r-2 border-gray-200 last:border-r-0 relative"
            :class="{ 'bg-emerald-50/50': day.isToday }"
          >
            <div class="text-sm font-bold" :class="day.isToday ? 'text-emerald-700' : 'text-gray-700'">
              {{ day.label }}
            </div>
            <div class="text-xs text-gray-400 mt-1 font-mono">
              {{ day.date.split('-').slice(1).join('/') }}
            </div>
            <div v-if="day.isToday" class="absolute top-1 right-1">
              <div class="w-2 h-2 bg-emerald-500 rounded-full animate-pulse" />
            </div>
          </div>
        </div>

        <div v-if="loading" class="p-20 flex justify-center">
          <NSpin size="large" description="Đang tải dữ liệu..." />
        </div>

        <div v-else class="divide-y-2 divide-gray-200 min-w-[1000px]">
          <div v-for="shift in shifts" :key="shift.id" class="grid grid-cols-8 group hover:bg-emerald-50/10 transition-colors">
            <div class="p-4 border-r-2 border-gray-200 flex flex-col justify-center items-center bg-white group-hover:bg-gray-50 sticky left-0 z-10 shadow-sm">
              <span class="font-bold text-gray-800 text-sm text-center">{{ shift.name }}</span>
              <NTag size="small" class="mt-2 text-[10px] font-mono !bg-emerald-50 !text-emerald-700 !border-emerald-100" :bordered="true">
                {{ shift.startTime?.slice(0, 5) }} - {{ shift.endTime?.slice(0, 5) }}
              </NTag>
            </div>

            <div
              v-for="day in weekDays" :key="day.date"
              class="border-r-2 border-gray-200 last:border-r-0 min-h-[120px] p-2 relative cursor-pointer hover:bg-emerald-50/40 transition-all flex flex-col justify-center items-center group/cell"
              @click="openAssignModal(shift, day)"
            >
              <div v-if="getAssign(shift.id!, day.date)" class="w-full h-full animate-fade-in relative group/item">
                <div class="bg-white border-l-4 border-l-emerald-500 border-y border-r border-gray-200 shadow-sm rounded-lg p-3 h-full flex flex-col items-center justify-center gap-2 relative hover:shadow-md transition-all">
                  <NAvatar
                    round :size="38"
                    :src="getAvatar(getAssign(shift.id!, day.date)?.staff?.id)"
                    class="border-2 border-white shadow-sm flex-shrink-0"
                    object-fit="cover"
                    :style="{ backgroundColor: '#ecfdf5', color: '#059669', fontWeight: 'bold' }"
                  >
                    <template v-if="!getAvatar(getAssign(shift.id!, day.date)?.staff?.id)">
                      {{ getAssign(shift.id!, day.date)?.staff?.name?.charAt(0).toUpperCase() }}
                    </template>
                  </NAvatar>

                  <span class="text-[11px] font-bold text-gray-700 text-center leading-tight line-clamp-2">
                    {{ getAssign(shift.id!, day.date)?.staff?.name }}
                  </span>

                  <div class="absolute -top-2 -right-2 flex gap-1 opacity-0 group-hover/item:opacity-100 transition-all duration-200 z-20" @click.stop>
                    <div class="w-7 h-7 bg-white rounded-full shadow-md border border-gray-200 flex items-center justify-center cursor-pointer hover:bg-emerald-500 hover:text-white text-emerald-600 transition-all transform hover:scale-110" @click="openAssignModal(shift, day)">
                      <Icon icon="carbon:edit" class="text-sm" />
                    </div>
                    <NPopconfirm placement="top" :show-icon="false" @positive-click="() => removeAssign(getAssign(shift.id!, day.date)?.id)">
                      <template #trigger>
                        <div class="w-7 h-7 bg-white rounded-full shadow-md border border-gray-200 flex items-center justify-center cursor-pointer hover:bg-red-500 hover:text-white text-red-600 transition-all transform hover:scale-110">
                          <Icon icon="carbon:close" class="text-sm font-bold" />
                        </div>
                      </template>
                      <span class="text-xs font-medium px-1">Gỡ lịch làm việc này?</span>
                    </NPopconfirm>
                  </div>
                </div>
              </div>

              <div v-else class="w-full h-full flex items-center justify-center opacity-0 group-hover/cell:opacity-100 transition-opacity">
                <div class="w-9 h-9 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center border border-emerald-100 shadow-sm hover:scale-110 transition-transform">
                  <Icon icon="carbon:add" class="text-lg" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="p-4">
        <NDataTable
          :columns="tableColumns"
          :data="filteredSchedules"
          :loading="loading"
          :pagination="pagination"
          :bordered="false"
          :single-line="false"
          class="shadow-sm rounded-lg"
        />

        <div v-if="filteredSchedules.length === 0 && !loading" class="text-center p-8 text-gray-400">
          Không tìm thấy dữ liệu phù hợp
        </div>
      </div>
    </div>

    <NModal v-model:show="showModal" preset="card" class="w-[450px] rounded-2xl shadow-xl" :title="`Xếp lịch: ${assignState.dateLabel}`">
      <div class="flex flex-col gap-5">
        <div class="p-4 bg-emerald-50/50 rounded-xl flex justify-between items-center border border-emerald-100">
          <div class="flex flex-col">
            <span class="text-emerald-600 text-xs uppercase tracking-wider font-semibold">Ca làm việc</span>
            <span class="font-bold text-gray-800 text-lg">{{ assignState.shift?.name }}</span>
          </div>
          <div class="flex flex-col items-end">
            <span class="text-gray-500 text-xs font-medium">Thời gian</span>
            <NTag type="success" class="font-mono font-bold" :bordered="false">
              {{ assignState.shift?.startTime?.slice(0, 5) }} - {{ assignState.shift?.endTime?.slice(0, 5) }}
            </NTag>
          </div>
        </div>

        <div class="flex flex-col gap-2">
          <span class="text-sm font-medium text-gray-700 ml-1">Chọn nhân viên trực ca:</span>
          <NSelect
            v-model:value="assignState.staffId"
            :options="staffList.map(s => ({
              label: `${s.code} - ${s.fullName}`,
              value: s.id,
            }))"
            filterable
            placeholder="Tìm kiếm theo tên hoặc mã nhân viên..."
            size="large"
          />
        </div>

        <NPopconfirm placement="top" positive-text="Xác nhận" negative-text="Quay lại" @positive-click="handleAssign">
          <template #trigger>
            <NButton type="primary" block size="large" class="mt-2 h-11 font-bold !bg-emerald-600 hover:!bg-emerald-700" :disabled="!assignState.staffId">
              <template #icon>
                <NIcon><Icon icon="carbon:save" /></NIcon>
              </template>
              Lưu lịch làm việc
            </NButton>
          </template>
          <span>Lưu thay đổi lịch trực cho ngày này?</span>
        </NPopconfirm>
      </div>
    </NModal>
  </div>
</template>

<style scoped>
.animate-fade-in { animation: fadeIn 0.3s ease-in-out; }
@keyframes fadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

::-webkit-scrollbar { height: 8px; }
::-webkit-scrollbar-track { background: #f1f1f1; }
::-webkit-scrollbar-thumb { background: #10b981; border-radius: 10px; }
::-webkit-scrollbar-thumb:hover { background: #059669; }
</style>
