<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NButton, NModal, NTag, useMessage } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handoverApi } from '@/service/api/admin/shift/handover'
import { scheduleApi } from '@/service/api/admin/shift/schedule'

const props = defineProps<{ forceShow?: boolean }>()
defineExpose({ openModal })

// --- State ---
const showModal = ref(false)
const loading = ref(false)
const checking = ref(true)

// initialCash: Giá trị số để tính toán
const initialCash = ref<number>(0)
// displayCash: Giá trị chuỗi hiển thị trên input (có dấu phẩy)
const displayCash = ref<string>('')

const note = ref('')
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

// --- 1. LOGIC NHẬP TIỀN AN TOÀN (V-MODEL 2 CHIỀU) ---
function handleInputMoney(e: any) {
  let rawValue = e.target.value
  // Xóa ký tự không phải số
  rawValue = rawValue.replace(/\D/g, '')

  if (rawValue === '') {
    initialCash.value = 0
    displayCash.value = ''
    return
  }

  const numValue = Number(rawValue)
  initialCash.value = numValue
  // Cập nhật hiển thị có dấu phẩy
  displayCash.value = numValue.toLocaleString('en-US')
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
  // 👇 FIX QUAN TRỌNG: Nếu đang ở trang login thì không check gì cả
  if (route.path.includes('/login')) {
    showModal.value = false
    checking.value = false
    return
  }

  checking.value = true
  const currentUser = getUserData()
  const userId = currentUser?.id || currentUser?.userId

  if (!userId) { checking.value = false; return }

  const isClientPage = route.path.startsWith('/home') || route.path === '/' || route.path.startsWith('/product')
  if (isClientPage && !props.forceShow) { checking.value = false; return }

  // CHECK ADMIN (Bỏ qua nếu là Admin)
  const roles = currentUser.rolesCodes || currentUser.roles || []
  const isAdmin = Array.isArray(roles) && roles.some((r: string) => {
    const roleUpper = r.toUpperCase()
    return roleUpper.includes('ADMIN') || roleUpper.includes('QUAN_LY') || roleUpper.includes('ROOT') || roleUpper.includes('OWNER')
  })

  if (isAdmin && !props.forceShow) { checking.value = false; return }

  const hasSessionTicket = sessionStorage.getItem('SHIFT_ACTIVE_TICKET') === 'true'
  if (hasSessionTicket && !props.forceShow) {
    showModal.value = false
    checking.value = false
    return
  }

  try {
    const res = await handoverApi.getCurrentShift(userId)

    if (res && res.data && res.data.id) {
      sessionStorage.setItem('SHIFT_ACTIVE_TICKET', 'true')
      showModal.value = false
    }
    else {
      staffName.value = currentUser.fullName || currentUser.username || 'Đồng nghiệp'
      showModal.value = true
      initialCash.value = 0
      displayCash.value = ''
      identifyShift(userId)
    }
  }
  catch (e) {
    console.error('Lỗi check ca:', e)
  }
  finally {
    checking.value = false
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
      const startMin = toMinutes(strictMatch.shift.startTime)
      note.value = currentMinutes < startMin
        ? `Vào sớm trước ca (Lịch: ${strictMatch.shift.startTime?.slice(0, 5)})`
        : `Vào ca theo lịch: ${strictMatch.shift.name}`
    }
    else { detectedShift.value = null; note.value = '' }
  }
  catch (e) { console.error(e) }
}

// --- 5. MỞ CA ---
async function handleStartShift() {
  const currentUser = getUserData()
  const userId = currentUser?.id || currentUser?.userId
  const userName = currentUser?.userCode || currentUser?.fullName || 'NV'

  if (initialCash.value < 0)
    return message.warning('Tiền đầu ca không hợp lệ')

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

    const payload = {
      accountId: userId,
      initialCash: initialCash.value,
      note: note.value,
      name: finalShiftName,
      startTime: formatDateForServer(now),
      endTime: null,
      workScheduleId: scheduleIdToSave,
    }

    await handoverApi.startShift(payload)

    message.success(`Mở ca thành công!`)
    sessionStorage.setItem('SHIFT_ACTIVE_TICKET', 'true')
    showModal.value = false

    setTimeout(() => { window.location.reload() }, 1000)
  }
  catch (e) {
    message.error('Có lỗi khi mở ca!')
  }
  finally { loading.value = false }
}

// --- 6. HÀM ĐĂNG XUẤT (ĐÃ SỬA LỖI TREO MODAL) ---
function handleLogout() {
  // 1. Đóng modal ngay lập tức
  showModal.value = false

  // 2. Xóa dữ liệu
  localStorage.clear()
  sessionStorage.clear()

  // 3. Dùng window.location để reload trang hoàn toàn -> Xóa sạch state cũ
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
  <NModal v-model:show="showModal" :mask-closable="false" :close-on-esc="false" transform-origin="center">
    <div class="bg-white rounded-2xl shadow-2xl w-[480px] overflow-hidden font-sans relative">
      <div class="bg-emerald-600 p-6 text-center text-white relative overflow-hidden">
        <Icon icon="carbon:time" class="absolute -left-6 -bottom-6 text-emerald-500 opacity-30 w-36 h-36" />
        <div class="relative z-10">
          <h2 class="text-2xl font-bold m-0 uppercase tracking-wide flex justify-center items-center gap-2">
            Mở Ca Làm Việc
          </h2>
          <p class="opacity-90 text-sm mt-1 font-medium">
            Hệ thống quản lý bán hàng MyLaptop
          </p>
        </div>
      </div>

      <div class="p-8 space-y-6">
        <div v-if="detectedShift" class="bg-emerald-50 border border-emerald-200 rounded-xl p-4 text-center shadow-sm">
          <p class="text-emerald-800 text-[10px] uppercase font-bold tracking-widest mb-1">
            LỊCH LÀM VIỆC CỦA BẠN
          </p>
          <h3 class="text-2xl font-bold text-emerald-700 leading-tight mb-2">
            {{ detectedShift.name }}
          </h3>
          <div class="flex justify-center items-center gap-2">
            <NTag type="success" class="font-bold font-mono text-sm" round :bordered="false">
              <template #icon>
                <Icon icon="carbon:time" />
              </template>
              {{ detectedShift.start }} - {{ detectedShift.end }}
            </NTag>
          </div>
          <p v-if="note" class="text-xs text-emerald-600 font-semibold mt-2">
            ℹ️ {{ note }}
          </p>
        </div>

        <div v-else class="bg-orange-50 border border-orange-200 rounded-xl p-4 flex items-start gap-3 shadow-sm">
          <div class="bg-orange-100 p-2 rounded-full">
            <Icon icon="carbon:warning-alt" class="text-orange-600 text-xl block" />
          </div>
          <div>
            <p class="text-orange-900 font-bold text-sm">
              Không tìm thấy lịch phân công
            </p>
            <p class="text-orange-800 text-xs mt-1 leading-relaxed">
              Bạn đang mở ca tự do. Hệ thống sẽ ghi nhận thời gian bắt đầu từ lúc này.
            </p>
          </div>
        </div>

        <div class="text-center border-b border-gray-100 pb-4">
          <h3 class="text-lg font-bold text-gray-800 flex justify-center items-center gap-2">
            <Icon icon="carbon:user-avatar-filled" class="text-gray-400" /> {{ staffName }}
          </h3>
          <p class="text-xs text-gray-400 font-mono mt-1">
            {{ currentTime }}
          </p>
        </div>

        <div>
          <label class="block text-gray-700 text-sm font-bold mb-2 uppercase tracking-wide">
            Tiền mặt đầu ca <span class="text-red-500">*</span>
          </label>
          <div class="relative group">
            <div class="absolute left-0 top-0 bottom-0 w-12 flex items-center justify-center text-gray-500 font-bold bg-gray-50 border-r border-gray-200 rounded-l transition-colors group-focus-within:bg-emerald-50 group-focus-within:text-emerald-600 group-focus-within:border-emerald-200">
              ₫
            </div>
            <input
              v-model="displayCash"
              type="text"
              class="w-full pl-14 pr-4 py-3 text-xl font-bold text-gray-800 border border-gray-300 rounded transition-all outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-500/20 placeholder-gray-300"
              placeholder="0"
              autofocus
              @input="handleInputMoney"
            >
          </div>
        </div>

        <div>
          <label class="block text-gray-700 text-sm font-bold mb-2">Ghi chú (Tùy chọn)</label>
          <input v-model="note" class="w-full px-4 py-2 border border-gray-300 rounded text-sm transition-all outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200" placeholder="Ví dụ: Mang theo tiền lẻ...">
        </div>

        <div class="grid grid-cols-12 gap-3 pt-2">
          <div class="col-span-4">
            <NButton secondary type="error" size="large" block class="h-12 font-bold" @click="handleLogout">
              Đăng xuất
            </NButton>
          </div>
          <div class="col-span-8">
            <NButton
              type="primary"
              size="large"
              block
              :loading="loading"
              class="h-12 font-bold text-white !bg-emerald-600 hover:!bg-emerald-700 shadow-lg shadow-emerald-200"
              @click="handleStartShift"
            >
              <template #icon>
                <Icon icon="carbon:checkmark-filled" />
              </template>
              {{ detectedShift ? 'XÁC NHẬN VÀO CA' : 'XÁC NHẬN VÀO CA' }}
            </NButton>
          </div>
        </div>
      </div>
    </div>
  </NModal>
</template>
