<script setup lang="ts">
import { computed, h, onMounted, reactive, ref, watch } from 'vue'
import {
  NAvatar,
  NCard,
  NDataTable,
  NDatePicker,
  NIcon,
  NInput,
  NRadioButton,
  NRadioGroup,
  NSpace,
  NSpin,
  NTag,
} from 'naive-ui'
import { Icon } from '@iconify/vue'

// --- API IMPORTS ---
import { shiftApi } from '@/service/api/admin/shift/shift'
import type { Shift } from '@/service/api/admin/shift/shift'
import { scheduleApi } from '@/service/api/admin/shift/schedule'
import type { WorkSchedule } from '@/service/api/admin/shift/schedule'
import { getAllStaff } from '@/service/api/admin/users/staff.api'
import type { StaffResponse } from '@/service/api/admin/users/staff.api'

const loading = ref(false)
const shifts = ref<Shift[]>([])
const staffList = ref<StaffResponse[]>([])
const schedules = ref<WorkSchedule[]>([])

// State View
const viewMode = ref<'matrix' | 'list'>('matrix')
const anchorDate = ref(Date.now())

// Filter
const filterParams = reactive({
  staffName: '',
  shiftName: '',
  dateRange: null as [number, number] | null,
})

// Pagination cho bảng danh sách
const pagination = reactive({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [5, 10, 20, 50],
  onChange: (page: number) => { pagination.page = page },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
  },
  prefix: ({ itemCount }: any) => `Tổng số ${itemCount} lịch trực`,
})

// Logic xử lý ngày
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

function changeWeek(offset: number) {
  const newDate = new Date(anchorDate.value)
  newDate.setDate(newDate.getDate() + (offset * 7))
  anchorDate.value = newDate.getTime()
}

function setToday() { anchorDate.value = Date.now() }

watch(anchorDate, () => { loadSchedule() })

async function loadData() {
  loading.value = true
  try {
    const sRes = await shiftApi.getAll()
    let rawShifts = sRes.data || []
    rawShifts = rawShifts.filter((s: any) => s.status === 1 || s.status === true || s.status === 'ACTIVE')
    shifts.value = rawShifts.sort((a: any, b: any) => (a.startTime || '').localeCompare(b.startTime || ''))

    const stRes = await getAllStaff({ page: 1, size: 1000, status: 'ACTIVE' })
    staffList.value = stRes.data?.data || stRes.data || []
    await loadSchedule()
  }
  catch (e) { console.error('Lỗi tải dữ liệu') }
  finally { loading.value = false }
}

async function loadSchedule() {
  if (weekDays.value.length === 0)
    return
  const from = weekDays.value[0].date
  const to = weekDays.value[6].date
  try {
    const res = await scheduleApi.getWeekly(from, to)
    schedules.value = res.data?.data || res.data || []
  }
  catch (e) { schedules.value = [] }
}

function getAssign(shiftId: any, date: string) {
  return schedules.value.find(s => (s.shift?.id == shiftId) && (s.workDate === date))
}

const tableColumns = [
  {
    title: 'Ngày làm việc',
    key: 'workDate',
    render(row: WorkSchedule) {
      return h('div', { class: 'flex items-center gap-2' }, [
        h(Icon, { icon: 'carbon:calendar', class: 'text-gray-400' }),
        h('span', { style: 'font-size: 14px; color: #374151;' }, row.workDate),
      ])
    },
  },
  {
    title: 'Thời gian',
    key: 'time',
    align: 'center',
    render(row: WorkSchedule) {
      return h('span', { class: 'font-mono' }, `${row.shift?.startTime?.slice(0, 5)} - ${row.shift?.endTime?.slice(0, 5)}`)
    },
  },
  {
    title: 'Ca trực',
    key: 'shiftName',
    render(row: WorkSchedule) {
      return h(NTag, { type: 'success', bordered: false, round: true, size: 'small' }, { default: () => row.shift?.name })
    },
  },
  {
    title: 'Nhân viên',
    key: 'staffName',
    render(row: WorkSchedule) {
      return h('div', [
        h('div', { class: 'font-bold text-gray-700' }, row.staff?.name || row.staffName),
        h('div', { class: 'text-[10px] text-gray-400' }, row.staff?.code),
      ])
    },
  },
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    render(row: WorkSchedule) {
      const isPast = new Date(row.workDate!).setHours(0, 0, 0, 0) < new Date().setHours(0, 0, 0, 0)
      return h(NTag, {
        type: isPast ? 'default' : 'success',
        round: true,
        size: 'small',
        style: isPast ? '' : 'font-weight: bold',
      }, { default: () => isPast ? 'Đã diễn ra' : 'Sắp tới' })
    },
  },
]

const filteredSchedules = computed(() => {
  return schedules.value.filter((item) => {
    const matchName = !filterParams.staffName || (item.staff?.name || '').toLowerCase().includes(filterParams.staffName.toLowerCase())
    const matchShift = !filterParams.shiftName || (item.shift?.name || '').toLowerCase().includes(filterParams.shiftName.toLowerCase())
    if (filterParams.dateRange) {
      const itemTime = new Date(item.workDate!).getTime()
      return matchName && matchShift && itemTime >= filterParams.dateRange[0] && itemTime <= filterParams.dateRange[1]
    }
    return matchName && matchShift
  })
})

function clearFilters() {
  filterParams.staffName = ''; filterParams.shiftName = ''; filterParams.dateRange = null
}

onMounted(loadData)
</script>

<template>
  <div class="p-4 md:p-6 bg-[#f8fafc] min-h-screen font-sans">
    <div class="flex justify-between items-center mb-6">
      <div class="flex items-center gap-4">
        <div class="p-3 bg-emerald-600 text-white rounded-2xl shadow-lg shadow-emerald-200/50">
          <Icon icon="carbon:calendar-heat-map" class="text-2xl" />
        </div>
        <div>
          <h1 class="text-2xl font-bold text-gray-800 m-0 leading-none">
            Lịch Trực Nhân Viên
          </h1>
          <p class="text-xs text-gray-400 mt-2 font-medium uppercase tracking-widest">
            Phân ca làm việc hàng tuần
          </p>
        </div>
      </div>

      <div class="flex gap-2">
        <NRadioGroup v-model:value="viewMode" size="medium" class="bg-white p-1 rounded-xl shadow-sm border border-gray-100">
          <NRadioButton value="matrix">
            <Icon icon="carbon:grid" class="inline mr-1" /> Lịch biểu
          </NRadioButton>
          <NRadioButton value="list">
            <Icon icon="carbon:list" class="inline mr-1" /> Danh sách
          </NRadioButton>
        </NRadioGroup>
        <NButton type="primary" secondary class="rounded-xl border border-gray-100 font-bold" @click="loadData">
          <template #icon>
            <Icon icon="carbon:rotate" />
          </template> Làm mới
        </NButton>
      </div>
    </div>

    <NCard class="mb-6 rounded-2xl shadow-sm border border-gray-100" size="small" :bordered="false">
      <div class="flex justify-between items-center gap-4">
        <div class="flex items-center gap-2">
          <div class="flex items-center bg-gray-50 p-1 rounded-xl border border-gray-200">
            <NButton quaternary circle size="small" @click="changeWeek(-1)">
              <Icon icon="carbon:chevron-left" />
            </NButton>
            <NDatePicker v-model:value="anchorDate" type="date" size="small" class="w-[130px] font-bold" :actions="null" :clearable="false" />
            <NButton quaternary circle size="small" @click="changeWeek(1)">
              <Icon icon="carbon:chevron-right" />
            </NButton>
          </div>
          <NButton size="small" strong secondary type="success" class="rounded-lg h-[34px]" @click="setToday">
            Hôm nay
          </NButton>
        </div>
        <div class="text-[13px] font-bold text-emerald-700 bg-emerald-50 px-4 py-1.5 rounded-xl border border-emerald-100 uppercase tracking-tight">
          Tuần: {{ weekDays[0]?.date }} ➔ {{ weekDays[6]?.date }}
        </div>
      </div>

      <div v-if="viewMode === 'list'" class="mt-4 pt-4 border-t border-gray-50 grid grid-cols-4 gap-3">
        <NInput v-model:value="filterParams.staffName" placeholder="Tìm tên nhân viên..." clearable />
        <NInput v-model:value="filterParams.shiftName" placeholder="Tìm tên ca trực..." clearable />
        <NDatePicker v-model:value="filterParams.dateRange" type="daterange" clearable class="w-full" />
        <NButton dashed @click="clearFilters">
          Xóa bộ lọc
        </NButton>
      </div>
    </NCard>

    <div class="bg-white rounded-2xl shadow-xl border-t border-l border-gray-200 overflow-hidden">
      <div v-if="viewMode === 'matrix'" class="overflow-x-auto">
        <div class="grid grid-cols-8 bg-[#fdfdfd] min-w-[1000px]">
          <div class="p-4 font-bold text-gray-400 text-center text-[11px] uppercase sticky left-0 z-10 bg-[#f9fafb] border-r border-b border-gray-200 flex items-center justify-center">
            CA / NGÀY
          </div>
          <div
            v-for="day in weekDays"
            :key="day.date"
            class="p-3 text-center border-r border-b border-gray-200"
            :class="{ 'bg-emerald-50/40': day.isToday }"
          >
            <div class="text-[14px] font-bold" :class="day.isToday ? 'text-emerald-700' : 'text-gray-700'">
              {{ day.label }}
            </div>
            <div class="text-[10px] text-gray-400 mt-1 font-mono font-medium">
              {{ day.date.split('-').slice(1).join('/') }}
            </div>
          </div>
        </div>

        <div v-if="loading" class="p-20 flex justify-center">
          <NSpin size="large" />
        </div>

        <div v-else class="min-w-[1000px]">
          <div v-for="shift in shifts" :key="shift.id" class="grid grid-cols-8">
            <div class="p-4 border-r border-b border-gray-200 flex flex-col justify-center items-center bg-white sticky left-0 z-10 shadow-[2px_0_0_rgba(0,0,0,0.02)]">
              <span class="font-bold text-gray-800 text-[13px] text-center leading-tight">{{ shift.name }}</span>
              <span class="text-[10px] text-emerald-600 font-bold mt-2 bg-emerald-50 px-2 py-0.5 rounded-md border border-emerald-100">
                {{ shift.startTime?.slice(0, 5) }} - {{ shift.endTime?.slice(0, 5) }}
              </span>
            </div>

            <div
              v-for="day in weekDays"
              :key="day.date"
              class="border-r border-b border-gray-200 min-h-[90px] p-2 flex flex-col justify-center items-center transition-all hover:bg-slate-50/50 relative"
            >
              <div v-if="getAssign(shift.id!, day.date)" class="w-full h-full animate-fade-in flex flex-col">
                <div class="bg-white border border-gray-100 shadow-sm rounded-xl p-3 h-full flex flex-col items-center justify-center border-l-4 border-l-emerald-500">
                  <p class="text-[12px] font-bold text-gray-800 leading-tight text-center">
                    {{ getAssign(shift.id!, day.date)?.staff?.name }}
                  </p>
                  <p class="text-[9px] text-emerald-600 font-bold mt-2 uppercase tracking-tighter bg-emerald-50 px-1.5 rounded border border-emerald-100">
                    {{ getAssign(shift.id!, day.date)?.staff?.code }}
                  </p>
                </div>
              </div>

              <div v-if="day.isToday" class="absolute inset-0 bg-emerald-500/5 pointer-events-none" />
            </div>
          </div>
        </div>
      </div>

      <div v-else class="p-4">
        <NDataTable :columns="tableColumns" :data="filteredSchedules" :loading="loading" :pagination="pagination" :bordered="false" size="large" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }

::-webkit-scrollbar { height: 6px; width: 6px; }
::-webkit-scrollbar-thumb { background: #10b981; border-radius: 10px; }
::-webkit-scrollbar-track { background: transparent; }

/* Grid Line Consistency */
.grid { border-collapse: collapse; }
</style>
