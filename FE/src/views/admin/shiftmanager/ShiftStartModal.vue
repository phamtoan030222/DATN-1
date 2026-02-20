<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NAlert, NButton, NModal, NTag, NTooltip, useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handoverApi } from '@/service/api/admin/shift/handover'
import { scheduleApi } from '@/service/api/admin/shift/schedule'

const props = defineProps<{ forceShow?: boolean }>()
defineExpose({ openModal })

// --- State ---
const showModal = ref(false)
const loading = ref(false)
const checking = ref(true)
const viewMode = ref(false) // Trạng thái "Chế độ xem"

// Tiền nong
const initialCash = ref<number>(0)
const displayCash = ref<string>('')
const lastClosedCash = ref<number>(0) // Tiền kết ca của ca trước

const note = ref('') // Để trắng mặc định theo yêu cầu
const staffName = ref('Nhân viên')
const currentTime = ref(new Date().toLocaleString('vi-VN'))

const detectedShift = ref<{
  id: number | null
  name: string
  start: string
  end: string
  scheduleId: number | null
} | null>(null)

const router = useRouter()
const route = useRoute()
const message = useMessage()

// --- Computed: Kiểm tra lệch tiền ---
const isCashMismatch = computed(() => {
  return initialCash.value !== lastClosedCash.value
})

const canStartShift = computed(() => {
  // Logic: Nếu lệch tiền mà KHÔNG có ghi chú -> Disable nút
  if (isCashMismatch.value && !note.value.trim())
    return false
  return true
})

// --- 1. LOGIC NHẬP TIỀN ---
function handleInputMoney(e: any) {
  let rawValue = e.target.value
  rawValue = rawValue.replace(/\D/g, '')

  if (rawValue === '') {
    initialCash.value = 0
    displayCash.value = ''
    return
  }
  const numValue = Number(rawValue)
  initialCash.value = numValue
  displayCash.value = numValue.toLocaleString('en-US')
}

// Hàm format tiền tệ hiển thị
function formatCurrency(val: number) {
  return `${val.toLocaleString('vi-VN')} ₫`
}

// --- 2. LẤY USER DATA ---
function getUserData() {
  try {
    const raw = localStorage.getItem('userInfo') || localStorage.getItem('USER_INFO') || localStorage.getItem('q_u_i')
    if (!raw)
      return null
    const parsed = JSON.parse(raw)
    return parsed.value?.userInfo || parsed.userInfo || parsed
  }
  catch (e) { return null }
}

// --- 3. CHECK QUYỀN & TRẠNG THÁI CA ---
async function checkShiftStatus() {
  if (route.path.includes('/login')) {
    showModal.value = false; checking.value = false; return
  }

  checking.value = true
  const currentUser = getUserData()
  const userId = currentUser?.id || currentUser?.userId

  if (!userId) { checking.value = false; return }

  const isClientPage = route.path.startsWith('/home') || route.path === '/' || route.path.startsWith('/product')
  if (isClientPage && !props.forceShow) { checking.value = false; return }

  const roles = currentUser.rolesCodes || currentUser.roles || []
  const isAdmin = Array.isArray(roles) && roles.some((r: string) => {
    const roleUpper = r.toUpperCase()
    return roleUpper.includes('ADMIN') || roleUpper.includes('QUAN_LY') || roleUpper.includes('ROOT') || roleUpper.includes('OWNER')
  })
  if (isAdmin && !props.forceShow) { checking.value = false; return }

  const hasSessionTicket = sessionStorage.getItem('SHIFT_ACTIVE_TICKET') === 'true'
  if (hasSessionTicket && !props.forceShow) {
    showModal.value = false; checking.value = false; return
  }

  try {
    // Check ca hiện tại
    const res = await handoverApi.getCurrentShift(userId)

    if (res && res.data && res.data.id) {
      sessionStorage.setItem('SHIFT_ACTIVE_TICKET', 'true')
      showModal.value = false
    }
    else {
      // Nếu chưa có ca -> Chuẩn bị mở modal
      staffName.value = currentUser.fullName || currentUser.username || 'Đồng nghiệp'
      await fetchLastClosedShift()
      identifyShift(userId) // Tìm lịch

      // Nếu không ở chế độ xem thì mới hiện Modal
      if (!viewMode.value) {
        showModal.value = true
      }
    }
  }
  catch (e) { console.error('Lỗi check ca:', e) }
  finally { checking.value = false }
}

// --- [MỚI] Lấy tiền ca đóng gần nhất ---
async function fetchLastClosedShift() {
  try {
    const res = await handoverApi.getLastClosedShift()
    if (res && res.data) {
      // Sửa dòng gán tiền trong fetchLastClosedShift
      lastClosedCash.value = (res.data?.data?.realCashAmount) ?? (res.data?.realCashAmount) ?? 0
      initialCash.value = lastClosedCash.value
      displayCash.value = lastClosedCash.value.toLocaleString('en-US')
    }
  }
  catch (e) {
    console.error('Không lấy được tiền ca trước', e)
    lastClosedCash.value = 0
  }
}

// --- 4. TÌM LỊCH CA ---
async function identifyShift(uId: any) {
  try {
    const todayStr = new Date().toISOString().split('T')[0]
    const res = await scheduleApi.getWeekly(todayStr, todayStr)
    const schedules = res.data?.data || res.data || []
    const mySchedules = schedules.filter((s: any) => String(s.staff?.id) === String(uId))

    if (mySchedules.length === 0) { detectedShift.value = null; return }

    const now = new Date()
    const currentMinutes = now.getHours() * 60 + now.getMinutes()
    const toMinutes = (timeStr: string) => {
      if (!timeStr)
        return 0
      const [h, m] = timeStr.split(':').map(Number)
      return h * 60 + m
    }

    const BUFFER_MINUTES = 45
    const strictMatch = mySchedules.find((s: any) => {
      if (!s.shift)
        return false
      const startMin = toMinutes(s.shift.startTime)
      const endMin = toMinutes(s.shift.endTime)
      return currentMinutes >= (startMin - BUFFER_MINUTES) && currentMinutes <= endMin
    })

    if (strictMatch && strictMatch.shift) {
      detectedShift.value = {
        id: strictMatch.shift.id,
        name: strictMatch.shift.name,
        start: strictMatch.shift.startTime?.slice(0, 5),
        end: strictMatch.shift.endTime?.slice(0, 5),
        scheduleId: strictMatch.id,
      }
      // Để trắng note theo yêu cầu, không gán mặc định "Vào ca theo lịch..." nữa
    }
    else {
      detectedShift.value = null
      note.value = ''
    }
  }
  catch (e) { console.error(e) }
}

// --- 5. XỬ LÝ NÚT BẤM (Vào Ca hoặc Chế độ xem) ---
async function handleMainAction() {
  if (!detectedShift.value) {
    enableViewMode()
    return
  }
  await handleStartShift()
}

function enableViewMode() {
  showModal.value = false
  viewMode.value = true
  message.info('Đã chuyển sang chế độ xem. Bạn có thể mở lại bảng vào ca bất cứ lúc nào.')
}

function openModalFromFloatingButton() {
  viewMode.value = false
  showModal.value = true
}

// --- 6. MỞ CA (START SHIFT) ---
async function handleStartShift() {
  const currentUser = getUserData()
  const userId = currentUser?.id || currentUser?.userId
  const userName = currentUser?.userCode || currentUser?.fullName || 'NV'

  if (initialCash.value < 0)
    return message.warning('Tiền đầu ca không hợp lệ')

  if (isCashMismatch.value && !note.value.trim()) {
    message.error(`Tiền thực tế khác tiền ca trước. Vui lòng nhập lý do vào ô Ghi chú!`)
    return
  }

  loading.value = true
  try {
    const now = new Date()
    const dateStr = now.toLocaleDateString('vi-VN')
    let finalShiftName = ''
    let scheduleIdToSave = null

    if (detectedShift.value) {
      scheduleIdToSave = detectedShift.value.scheduleId
      finalShiftName = `${detectedShift.value.name} - ${userName} (${dateStr})`
    }
    else {
      const timeStr = now.getHours() < 12 ? 'Ca Sáng' : (now.getHours() < 18 ? 'Ca Chiều' : 'Ca Tối')
      finalShiftName = `${timeStr} (Tự do) - ${userName} (${dateStr})`
    }

    let finalNote = note.value
    if (isCashMismatch.value) {
      finalNote = `[LỆCH TIỀN ĐẦU CA] Hệ thống: ${formatCurrency(lastClosedCash.value)} - Thực tế: ${formatCurrency(initialCash.value)}. Lý do: ${note.value}`
    }

    const payload = {
      accountId: userId,
      initialCash: initialCash.value,
      note: finalNote,
      name: finalShiftName,
      startTime: formatDateForServer(now),
      endTime: null,
      workScheduleId: scheduleIdToSave,
    }

    await handoverApi.startShift(payload)
    message.success(`Mở ca thành công!`)
    sessionStorage.setItem('SHIFT_ACTIVE_TICKET', 'true')
    showModal.value = false
    viewMode.value = false
    setTimeout(() => { window.location.reload() }, 1000)
  }
  catch (e) { message.error('Có lỗi khi mở ca!') }
  finally { loading.value = false }
}

function handleLogout() {
  showModal.value = false
  localStorage.clear()
  sessionStorage.clear()
  window.location.href = '/login'
}

function formatDateForServer(date: Date) {
  if (!date)
    return null
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

function openModal() {
  staffName.value = 'Nhân viên'
  showModal.value = true
}

watch(() => route.path, () => { checkShiftStatus() })
setInterval(() => { currentTime.value = new Date().toLocaleString('vi-VN') }, 1000)
onMounted(() => { setTimeout(() => { checkShiftStatus() }, 500) })
</script>

<template>
  <div v-if="viewMode" class="fixed inset-0 z-[90] bg-white/5 backdrop-blur-[1px] cursor-not-allowed flex items-end justify-end p-8">
    <button
      class="group flex items-center justify-center bg-emerald-600 hover:bg-emerald-700 text-white rounded-full shadow-2xl transition-all w-[60px] h-[60px] hover:w-auto hover:pr-6 pointer-events-auto cursor-pointer"
      @click="openModalFromFloatingButton"
    >
      <div class="flex items-center justify-center shrink-0 w-[60px] h-[60px]">
        <Icon icon="carbon:view-filled" class="w-8 h-8 animate-pulse" />
      </div>
      <span class="max-w-0 overflow-hidden group-hover:max-w-xs transition-all duration-500 ease-in-out whitespace-nowrap font-bold text-sm opacity-0 group-hover:opacity-100">
        Mở bảng vào ca
      </span>
    </button>
  </div>

  <NModal v-model:show="showModal" :mask-closable="false" :close-on-esc="false" transform-origin="center">
    <div class="bg-white rounded-xl shadow-2xl w-[450px] overflow-hidden font-sans border border-gray-100">
      <div class="bg-emerald-600 px-6 py-4 flex items-center justify-between relative overflow-hidden text-white">
        <div class="flex items-center gap-3 z-10">
          <Icon icon="carbon:time" class="w-8 h-8 opacity-90" />
          <div>
            <h2 class="text-lg font-bold uppercase tracking-wide leading-none">
              Mở Ca Làm Việc
            </h2>
            <p class="text-[10px] opacity-75 mt-1 font-medium tracking-tight">
              Hệ thống MyLaptop • {{ currentTime }}
            </p>
          </div>
        </div>
        <Icon icon="carbon:time" class="absolute -right-4 -bottom-4 text-white/10 w-24 h-24" />
      </div>

      <div class="p-6 space-y-4">
        <div v-if="detectedShift" class="bg-emerald-50 border border-emerald-100 rounded-lg p-3 flex items-center justify-between">
          <div>
            <p class="text-[10px] text-emerald-600 font-bold uppercase mb-0.5 tracking-wider">
              Lịch của bạn
            </p>
            <h4 class="text-emerald-800 font-bold text-base leading-tight">
              {{ detectedShift.name }}
            </h4>
          </div>
          <NTag type="success" size="small" round class="font-bold border-emerald-200">
            {{ detectedShift.start }} - {{ detectedShift.end }}
          </NTag>
        </div>

        <div v-else class="bg-orange-50 border border-orange-100 rounded-lg p-3 flex items-center gap-3">
          <Icon icon="carbon:warning" class="text-orange-500 w-5 h-5 shrink-0" />
          <p class="text-[11px] text-orange-800 font-medium leading-tight">
            Chưa đến giờ làm việc. Bạn có thể tra cứu thông tin ở <b>Chế độ xem</b> bên dưới.
          </p>
        </div>

        <div class="flex items-center justify-between px-1 text-xs">
          <div class="flex items-center gap-1.5 text-gray-600 font-bold">
            <Icon icon="carbon:user-avatar" class="text-gray-400" /> {{ staffName }}
          </div>
          <span class="text-gray-400 font-mono">{{ currentTime.split(' ')[0] }}</span>
        </div>

        <div class="space-y-1.5">
          <div class="flex justify-between items-end">
            <label class="text-[11px] font-bold text-gray-500 uppercase tracking-tighter">Tiền mặt đầu ca <span class="text-red-500">*</span></label>
            <span class="text-[10px] text-gray-400">Ca trước kết: <b class="text-gray-700">{{ lastClosedCash.toLocaleString() }}₫</b></span>
          </div>
          <div class="relative group">
            <div class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 font-bold border-r pr-2 border-gray-100 group-focus-within:text-emerald-500 group-focus-within:border-emerald-200">
              ₫
            </div>
            <input
              v-model="displayCash"
              type="text"
              class="w-full pl-10 pr-4 py-2 text-xl font-bold text-gray-800 border border-gray-200 rounded-lg focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 outline-none transition-all"
              :class="{ '!border-red-400 !bg-red-50/50': isCashMismatch }"
              placeholder="0"
              @input="handleInputMoney"
            >
          </div>
          <p v-if="isCashMismatch" class="text-[10px] text-orange-600 font-medium italic">
            ⚠️ Số tiền không khớp ca trước. Vui lòng nhập lý do bên dưới.
          </p>
        </div>

        <div class="space-y-1.5">
          <label class="text-[11px] font-bold text-gray-500 uppercase tracking-tighter">Ghi chú <span v-if="isCashMismatch" class="text-red-500 font-normal lowercase">(bắt buộc nhập lý do)</span></label>
          <textarea
            v-model="note"
            rows="2"
            class="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm focus:ring-4 focus:ring-emerald-500/10 focus:border-emerald-500 outline-none transition-all resize-none"
            :class="{ '!border-red-300': isCashMismatch && !note.trim() }"
            placeholder="Nhập ghi chú tại đây..."
          />
        </div>

        <div class="flex gap-2 pt-1">
          <NButton secondary type="error" class="flex-1 font-bold h-10 rounded-lg" @click="handleLogout">
            Đăng xuất
          </NButton>
          <NTooltip trigger="hover" :disabled="canStartShift">
            <template #trigger>
              <NButton
                type="primary"
                class="flex-[2] font-bold h-10 rounded-lg shadow-sm"
                :class="detectedShift ? '!bg-emerald-600 hover:!bg-emerald-700' : '!bg-blue-600 hover:!bg-blue-700'"
                :loading="loading"
                :disabled="detectedShift ? !canStartShift : false"
                @click="handleMainAction"
              >
                <template #icon>
                  <Icon :icon="detectedShift ? 'carbon:checkmark-filled' : 'carbon:view-filled'" />
                </template>
                {{ detectedShift ? 'XÁC NHẬN VÀO CA' : 'CHẾ ĐỘ XEM' }}
              </NButton>
            </template>
            Vui lòng nhập lý do lệch tiền trước khi vào ca!
          </NTooltip>
        </div>
      </div>
    </div>
  </NModal>
</template>

<style>
/* CSS ép Sidebar nổi lên trên lớp phủ overlay (z-100 > z-90) */
.n-layout-sider, aside, .sidebar-container, header {
  z-index: 100 !important;
  position: relative;
}
.n-layout-sider * {
  pointer-events: auto !important;
}
</style>
