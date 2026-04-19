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
  NSpace,
  NTag,
  NTooltip,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handoverApi } from '@/service/api/admin/shift/handover'

const message = useMessage()
const loading = ref(false)
const historyData = ref<any[]>([])

const filter = reactive({
  keyword: '',
  dateRange: null as [number, number] | null,
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  showSizePicker: true,
  pageSizes: [10, 20, 50],
  itemCount: 0,
  onChange: (page: number) => {
    pagination.page = page
    loadData()
  },
  onUpdatePageSize: (pageSize: number) => {
    pagination.pageSize = pageSize
    pagination.page = 1
    loadData()
  },
  prefix: ({ itemCount }: any) => `Tổng số ${itemCount} ca`,
})

let searchTimeout: any = null

watch(() => filter.keyword, () => {
  if (searchTimeout)
    clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    pagination.page = 1
    loadData()
  }, 500)
})

watch(() => filter.dateRange, () => {
  pagination.page = 1
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.page,
      size: pagination.pageSize,
      keyword: filter.keyword.trim(),
    }

    if (filter.dateRange) {
      // Dùng sv locale để ra format YYYY-MM-DD chuẩn truyền xuống Backend
      params.startDate = new Date(filter.dateRange[0]).toLocaleDateString('sv')
      params.endDate = new Date(filter.dateRange[1]).toLocaleDateString('sv')
    }

    const res = await handoverApi.getHistory(params)
    const pageData = res.data?.data || res.data
    historyData.value = pageData.content || pageData.items || pageData
    pagination.itemCount = pageData.totalElements || pageData.total || historyData.value.length
  }
  catch (e) {
    message.error('Lỗi khi tải lịch sử ca làm việc')
  }
  finally {
    loading.value = false
  }
}

function resetFilter() {
  filter.keyword = ''
  filter.dateRange = null
  // Do có watch() theo dõi 2 biến trên, tự động API sẽ được gọi lại, không cần gọi loadData() thủ công
}

function formatDateTime(dateStr: string) {
  if (!dateStr)
    return '--:--'
  const d = new Date(dateStr)
  return d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
}

// Hàm tiện ích so sánh giờ: tính số phút đi muộn hoặc về sớm
function calculateTimeDiff(actualTimeIso: string, standardTimeStr: string, isStart: boolean) {
  if (!actualTimeIso || !standardTimeStr)
    return null

  const actualDate = new Date(actualTimeIso)
  const actualMins = actualDate.getHours() * 60 + actualDate.getMinutes()

  const stdParts = standardTimeStr.split(':')
  const stdMins = Number.parseInt(stdParts[0]) * 60 + Number.parseInt(stdParts[1])

  const diff = actualMins - stdMins

  if (isStart) {
    // Vào ca: dương là muộn, âm là sớm
    if (diff > 0)
      return { type: 'error', text: `Đi muộn ${diff} phút` }
  }
  else {
    // Ra ca: âm là ra sớm, dương là ra trễ
    if (diff < 0)
      return { type: 'warning', text: `Ra sớm ${Math.abs(diff)} phút` }
    if (diff > 0)
      return { type: 'info', text: `Ra trễ ${diff} phút` }
  }
  return { type: 'success', text: 'Đúng giờ' }
}

// Hàm format tiền tệ
function formatCurrency(val: number) {
  if (!val)
    return '0 ₫'
  return `${val.toLocaleString('vi-VN')} ₫`
}

const columns = [
  {
    title: 'STT',
    key: 'stt',
    width: 60,
    align: 'center',
    render: (_: any, index: number) => (pagination.page - 1) * pagination.pageSize + index + 1,
  },
  {
    title: 'Nhân viên / Ca làm việc',
    key: 'info',
    minWidth: 260, // Dùng minWidth để bảng tự dàn đều
    render(row: any) {
      const stdStart = row.standardStartTime ? row.standardStartTime.substring(0, 5) : '--:--'
      const stdEnd = row.standardEndTime ? row.standardEndTime.substring(0, 5) : '--:--'
      const shiftName = row.name ? row.name.split(' - ')[0] : 'Ca Tự Do'
      const staffCodeStr = row.staffCode ? ` (${row.staffCode})` : ''

      return h(NSpace, { align: 'center', size: 12 }, {
        default: () => [
          h(NAvatar, { round: true, size: 'medium', style: 'border: 1px solid #e5e7eb; background-color: #ecfdf5; color: #059669; font-weight: bold;' }, { default: () => row.staffName?.charAt(0) || 'NV' }),
          h('div', { class: 'flex flex-col' }, [
            h('span', { class: 'font-bold text-gray-800' }, `${row.staffName || 'Nhân viên'}${staffCodeStr}`),
            h('span', { class: 'text-xs text-gray-500 font-medium mt-0.5' }, `${shiftName} (${stdStart} - ${stdEnd})`),
          ]),
        ],
      })
    },
  },
  {
    title: 'Vào ca',
    key: 'startTime',
    minWidth: 170,
    render(row: any) {
      const timeDiff = calculateTimeDiff(row.startTime, row.standardStartTime, true)

      return h('div', { class: 'flex flex-col gap-1' }, [
        h('div', { class: 'flex items-center gap-2' }, [
          h(Icon, { icon: 'carbon:login', class: 'text-emerald-500 text-lg' }),
          h('span', { class: 'text-sm font-mono text-gray-700' }, formatDateTime(row.startTime)),
        ]),
        timeDiff ? h(NTag, { type: timeDiff.type, size: 'small', bordered: false, class: 'w-max text-[11px]' }, { default: () => timeDiff.text }) : null,
      ])
    },
  },
  {
    title: 'Ra ca',
    key: 'endTime',
    minWidth: 170,
    render(row: any) {
      if (!row.endTime)
        return h('span', { class: 'text-gray-400 italic text-sm' }, '--:--')

      const timeDiff = calculateTimeDiff(row.endTime, row.standardEndTime, false)

      return h('div', { class: 'flex flex-col gap-1' }, [
        h('div', { class: 'flex items-center gap-2' }, [
          h(Icon, { icon: 'carbon:logout', class: 'text-orange-500 text-lg' }),
          h('span', { class: 'text-sm font-mono text-gray-700' }, formatDateTime(row.endTime)),
        ]),
        timeDiff ? h(NTag, { type: timeDiff.type, size: 'small', bordered: false, class: 'w-max text-[11px]' }, { default: () => timeDiff.text }) : null,
      ])
    },
  },
  {
    title: 'Doanh thu ca',
    key: 'totalRevenue',
    align: 'right',
    minWidth: 160,
    render(row: any) {
      const cash = (row.totalCashAmount || 0) - (row.initialCash || 0)
      const transfer = row.totalTransferAmount || 0

      // Hiển thị Tổng tiền to ở trên, và bóc tách TM / CK nhỏ ở dưới
      return h('div', { class: 'flex flex-col items-end gap-0.5' }, [
        h('span', { class: 'font-bold text-gray-800 text-[14px]' }, formatCurrency(row.totalRevenue)),
        h('span', { class: 'text-[11px] text-emerald-600 font-medium' }, `Tiền mặt: ${formatCurrency(cash)}`),
        h('span', { class: 'text-[11px] text-blue-600 font-medium' }, `Chuyển khoản: ${formatCurrency(transfer)}`),
      ])
    },
  },
  {
    title: 'Trạng thái',
    key: 'status',
    align: 'center',
    width: 120,
    render(row: any) {
      const isWorking = !row.endTime
      const type = isWorking ? 'info' : 'success'
      const label = isWorking ? 'Đang làm việc' : 'Đã kết ca'

      return h(NTag, {
        type,
        bordered: false,
        round: true,
        size: 'small',
        style: isWorking
          ? 'background-color: #e0f2fe; color: #0284c7; font-weight: 600;'
          : 'background-color: #dcfce7; color: #16a34a; font-weight: 600;',
      }, { default: () => label })
    },
  },
  {
    title: 'Ghi chú',
    key: 'note',
    minWidth: 200,
    render(row: any) {
      if (!row.note)
        return h('span', { class: 'text-gray-400 italic text-xs' }, 'Không có')

      return h(NTooltip, { trigger: 'hover', placement: 'top-end', style: 'max-width: 400px;' }, {
        trigger: () => h('div', { class: 'text-xs text-gray-600 truncate max-w-[200px] cursor-pointer flex items-center gap-1' }, [
          h(Icon, { icon: 'carbon:warning', class: row.note.includes('LỆCH TIỀN') || row.note.includes('Lệch') ? 'text-amber-500' : 'text-gray-400' }),
          row.note,
        ]),
        default: () => row.note,
      })
    },
  },
]

onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="p-4 md:p-6 bg-slate-50 min-h-screen">
    <div class="flex items-center gap-3 mb-6">
      <div class="p-3 bg-emerald-100 text-emerald-600 rounded-xl shadow-sm border border-emerald-200">
        <NIcon size="28">
          <Icon icon="carbon:time" />
        </NIcon>
      </div>
      <div>
        <h1 class="text-2xl font-bold text-gray-800 m-0">
          Lịch sử hoạt động
        </h1>
        <p class="text-gray-500 text-sm">
          Theo dõi thời gian ra/vào ca của nhân viên
        </p>
      </div>
    </div>

    <NCard class="mb-6 rounded-xl shadow-sm border-gray-200" size="small" :bordered="false">
      <div class="border-b border-gray-100 pb-3 mb-4">
        <h3 class="text-base font-bold text-gray-700 m-0">
          Bộ lọc tìm kiếm
        </h3>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-12 gap-6 items-end">
        <div class="col-span-1 md:col-span-5">
          <label class="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">Tìm kiếm chung</label>
          <NInput
            v-model:value="filter.keyword"
            placeholder="Nhập tên nhân viên, tên ca..."
            clearable
            size="large"
            class="bg-gray-50"
          >
            <template #prefix>
              <Icon icon="carbon:search" class="text-gray-400" />
            </template>
          </NInput>
        </div>

        <div class="col-span-1 md:col-span-5">
          <label class="block text-xs font-semibold text-gray-500 mb-2 uppercase tracking-wider">Lọc theo thời gian</label>
          <NDatePicker
            v-model:value="filter.dateRange"
            type="daterange"
            clearable
            size="large"
            class="w-full bg-gray-50"
            start-placeholder="Từ ngày"
            end-placeholder="Đến ngày"
          />
        </div>

        <div class="col-span-1 md:col-span-2 flex justify-end">
          <NButton secondary strong class="rounded-lg h-10 px-4 text-gray-600 hover:text-emerald-600 hover:bg-emerald-50 transition-all w-full" @click="resetFilter">
            <template #icon>
              <Icon icon="carbon:reset" />
            </template>
            Làm mới
          </NButton>
        </div>
      </div>
    </NCard>

    <NCard class="rounded-xl shadow-sm border-gray-200 overflow-hidden" :bordered="false" content-style="padding: 0;">
      <div class="p-4 border-b border-gray-100 flex justify-between items-center bg-white">
        <h3 class="text-lg font-bold text-gray-700 m-0">
          Danh sách hoạt động
        </h3>
        <NButton secondary circle size="small" type="primary" class="!bg-emerald-50 !text-emerald-600" :loading="loading" @click="loadData">
          <template #icon>
            <Icon icon="carbon:rotate" />
          </template>
        </NButton>
      </div>
      <div class="p-4 bg-white">
        <NDataTable
          :columns="columns"
          :data="historyData"
          :loading="loading"
          :pagination="pagination"
          :bordered="false"
          :single-line="false"
          class="border border-gray-100 rounded-lg"
        />
        <div v-if="historyData.length === 0 && !loading" class="text-center p-8 text-gray-400">
          Không tìm thấy dữ liệu phù hợp
        </div>
      </div>
    </NCard>
  </div>
</template>

<style scoped>
::-webkit-scrollbar { height: 8px; width: 8px; }
::-webkit-scrollbar-track { background: #f1f1f1; }
::-webkit-scrollbar-thumb { background: #10b981; border-radius: 10px; }
::-webkit-scrollbar-thumb:hover { background: #059669; }
</style>
