<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NInput,
  NInputNumber,
  NResult,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { handoverApi } from '@/service/api/admin/shift/handover'

const router = useRouter()
const message = useMessage()

// --- STATE ---
const loading = ref(true)
const submitting = ref(false)
const noShift = ref(false)

// --- DATA ---
const shiftId = ref('')
const staffName = ref('')
const staffCode = ref('')
const startTime = ref('')

const initialCash = ref(0)
const totalCashSales = ref(0)
const totalTransferSales = ref(0)
const totalExpense = ref(0)
const totalInvoiceCount = ref(0)

const actualCash = ref<number | null>(null)
const note = ref('')

// --- COMPUTED ---
const expectedCash = computed(() => {
  return (initialCash.value || 0) + (totalCashSales.value || 0) - (totalExpense.value || 0)
})

const diff = computed(() => {
  const actual = actualCash.value === null ? 0 : actualCash.value
  return actual - expectedCash.value
})

const isMatched = computed(() => {
  if (actualCash.value === null)
    return false
  return diff.value >= 0
})

function formatVND(val: number) {
  if (val === null || val === undefined)
    return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

// --- LOGIC LOAD DATA ---
function getUserId() {
  try {
    const raw = localStorage.getItem('userInfo') || localStorage.getItem('USER_INFO') || localStorage.getItem('q_u_i')
    if (!raw)
      return null
    const parsed = JSON.parse(raw)
    return parsed.value?.userInfo?.userId || parsed.value?.userInfo?.id || parsed.userInfo?.userId || parsed.userId || parsed.id || null
  }
  catch { return null }
}

async function fetchCurrentShift(count = 0) {
  try {
    const userId = getUserId()
    if (!userId) {
      noShift.value = true; loading.value = false; return
    }
    const res = await handoverApi.getCurrentShift(userId)
    const body = res.data || res
    const realData = body.data || body

    if (realData && realData.id) {
      shiftId.value = realData.id
      staffName.value = realData.staffName || (realData.staff ? realData.staff.fullName : '') || 'Nhân viên'
      staffCode.value = realData.staffCode || (realData.staff ? realData.staff.userCode : '') || 'NV'

      // Format time
      try {
        if (realData.startTime) {
          const d = new Date(realData.startTime)
          startTime.value = !isNaN(d.getTime())
            ? d.toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' })
            : realData.startTime
        }
        else {
          startTime.value = 'N/A'
        }
      }
      catch { startTime.value = '...' }

      initialCash.value = Number(realData.initialCash) || 0
      totalCashSales.value = Number(realData.totalCashAmount) || 0
      totalTransferSales.value = Number(realData.totalTransferAmount) || 0
      totalInvoiceCount.value = Number(realData.totalBills) || 0

      loading.value = false
      noShift.value = false
    }
    else {
      if (count < 3) {
        setTimeout(() => fetchCurrentShift(count + 1), 1000)
      }
      else { noShift.value = true; loading.value = false }
    }
  }
  catch { noShift.value = true; loading.value = false }
}

// --- ACTIONS ---
async function handleLogout() {
  localStorage.clear()
  sessionStorage.clear()
  window.location.href = '/login'
}

async function submit() {
  if (actualCash.value === null)
    return message.warning('⚠️ Vui lòng nhập tiền mặt thực tế!')
  await executeEndShift()
}

async function executeEndShift() {
  submitting.value = true
  try {
    const userId = getUserId()
    await handoverApi.endShift({
      shiftId: shiftId.value,
      accountId: userId,
      realCash: actualCash.value,
      note: note.value,
    })
    message.success('✅ Đóng ca thành công!')
    setTimeout(handleLogout, 1500)
  }
  catch (e: any) {
    message.error(e.response?.data?.message || 'Lỗi khi đóng ca')
    submitting.value = false
  }
}

// --- FORMAT UTILS ---
function parseCurrency(input: string) {
  if (!input)
    return null
  const nums = input.replace(/,/g, '').trim()
  return nums ? Number(nums) : null
}
function formatCurrency(value: number | null) {
  if (value === null || value === undefined)
    return ''
  return value.toLocaleString('en-US')
}

onMounted(() => fetchCurrentShift())
</script>

<template>
  <div class="bg-[#f8f9fa] min-h-screen p-4 font-sans text-gray-700">
    <div v-if="loading" class="flex flex-col items-center justify-center h-[80vh]">
      <NSpin size="large" stroke="#10b981" />
      <p class="mt-4 text-gray-400 text-sm font-medium animate-pulse">
        Đang tải dữ liệu...
      </p>
    </div>

    <div v-else-if="noShift" class="flex items-center justify-center h-[80vh]">
      <NResult status="404" title="Chưa vào ca" description="Bạn cần mở ca để bắt đầu.">
        <template #footer>
          <NButton type="primary" class="bg-emerald-600" @click="router.push('/dashboard/sales')">
            Mở ca ngay
          </NButton>
        </template>
      </NResult>
    </div>

    <div v-else class="max-w-5xl mx-auto">
      <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-4 mb-4 flex flex-col md:flex-row justify-between items-center gap-4">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full bg-emerald-50 flex items-center justify-center text-emerald-600">
            <Icon icon="carbon:notebook" width="20" />
          </div>
          <div>
            <h1 class="text-base font-bold text-gray-800">
              Phiếu Bàn Giao Ca
            </h1>
            <div class="text-xs text-gray-500 flex items-center gap-2">
              <span class="font-mono bg-gray-100 px-1 rounded text-gray-600">#{{ shiftId.substring(0, 8) }}</span>
              <span>• {{ startTime }}</span>
            </div>
          </div>
        </div>

        <div class="flex items-center gap-2 px-3 py-1.5 bg-gray-50 rounded-lg border border-gray-100">
          <div class="text-right">
            <div class="text-[10px] text-gray-400 font-bold uppercase">
              Nhân viên trực
            </div>
            <div class="text-sm font-bold text-gray-700 leading-none">
              {{ staffName }}
            </div>
          </div>
          <div class="w-8 h-8 rounded-full bg-emerald-600 text-white flex items-center justify-center text-xs font-bold">
            {{ staffCode.substring(0, 2) }}
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div class="flex flex-col gap-4">
          <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex-1">
            <h2 class="text-sm font-bold text-gray-800 mb-4 flex items-center gap-2">
              <Icon icon="carbon:chart-financial" class="text-emerald-500" /> Tài chính trong ca
            </h2>

            <div class="space-y-4">
              <div class="flex justify-between items-center pb-3 border-b border-gray-50">
                <span class="text-sm text-gray-500 font-medium">Tiền mặt đầu ca</span>
                <span class="font-mono text-lg font-bold text-gray-700">{{ formatVND(initialCash) }}</span>
              </div>

              <div class="flex justify-between items-center pb-3 border-b border-gray-50">
                <span class="text-sm text-gray-500 font-medium flex items-center gap-1">
                  Doanh thu Tiền mặt
                </span>
                <span class="font-mono text-lg font-bold text-emerald-600">+{{ formatVND(totalCashSales) }}</span>
              </div>

              <div class="flex justify-between items-center">
                <span class="text-sm text-gray-500 font-medium flex items-center gap-1">
                  Doanh thu CK / Thẻ
                </span>
                <span class="font-mono text-lg font-bold text-blue-600">{{ formatVND(totalTransferSales) }}</span>
              </div>
            </div>

            <div class="mt-4 pt-3 border-t border-dashed border-gray-200 text-center">
              <span class="inline-flex items-center gap-1 px-3 py-1 bg-gray-50 rounded-full text-xs font-medium text-gray-500">
                <Icon icon="carbon:receipt" /> Đã thanh toán: <b class="text-gray-800">{{ totalInvoiceCount }}</b> đơn
              </span>
            </div>
          </div>

          <div class="bg-emerald-50 rounded-xl border border-emerald-100 p-5 text-center">
            <div class="text-xs font-bold text-emerald-600 uppercase tracking-wider mb-1">
              Tổng tiền mặt lý thuyết tại két
            </div>
            <div class="text-3xl font-extrabold text-emerald-700 font-mono tracking-tight">
              {{ formatVND(expectedCash) }}
            </div>
            <div class="text-[10px] text-emerald-500 mt-1 font-medium">
              (Đầu ca + Doanh thu Tiền mặt - Chi phí)
            </div>
          </div>
        </div>

        <div class="bg-white rounded-xl shadow-sm border border-gray-100 p-5 flex flex-col h-full">
          <h2 class="text-sm font-bold text-gray-800 mb-5 flex items-center gap-2">
            <Icon icon="carbon:task-approved" class="text-emerald-500" /> Kiểm kê & Xác nhận
          </h2>

          <div class="flex-1 flex flex-col justify-between gap-6">
            <div>
              <label class="block text-xs font-bold text-gray-500 mb-2 uppercase">
                Nhập tiền thực tế <span class="text-red-500">*</span>
              </label>
              <NInputNumber
                v-model:value="actualCash"
                class="text-2xl font-bold text-right custom-input"
                placeholder="0"
                :show-button="false"
                :parse="parseCurrency"
                :format="formatCurrency"
                size="large"
                autofocus
              >
                <template #prefix>
                  <span class="text-gray-400 mr-2">₫</span>
                </template>
              </NInputNumber>
            </div>

            <div
              class="flex items-center justify-between p-3 rounded-lg border"
              :class="isMatched ? 'bg-emerald-50 border-emerald-100' : 'bg-red-50 border-red-100'"
            >
              <div class="flex items-center gap-2">
                <Icon
                  :icon="isMatched ? 'carbon:checkmark-filled' : 'carbon:warning-filled'"
                  :class="isMatched ? 'text-emerald-500' : 'text-red-500'" class="text-xl"
                />
                <div class="text-sm font-medium" :class="isMatched ? 'text-emerald-700' : 'text-red-700'">
                  {{ isMatched ? 'Khớp / Dư' : 'Thiếu hụt' }}
                </div>
              </div>
              <div class="font-mono text-lg font-bold" :class="isMatched ? 'text-emerald-700' : 'text-red-600'">
                {{ diff > 0 ? '+' : '' }}{{ formatVND(diff) }}
              </div>
            </div>

            <div>
              <label class="block text-xs font-bold text-gray-500 mb-2 uppercase">Ghi chú</label>
              <NInput v-model:value="note" type="textarea" placeholder="..." :rows="2" class="bg-gray-50" />
            </div>

            <div class="mt-auto">
              <NButton
                type="primary"
                block
                size="large"
                class="h-11 font-bold uppercase shadow-sm hover:shadow-md transition-all"
                color="#10b981"
                :loading="submitting"
                @click="submit"
              >
                Xác nhận Đóng Ca
              </NButton>
              <p class="text-center text-[10px] text-gray-400 mt-2">
                Hệ thống sẽ tự động đăng xuất sau khi hoàn tất.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Tùy chỉnh input để đồng bộ với style phẳng */
:deep(.n-input.custom-input) {
  background-color: #f9fafb;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
}
:deep(.n-input.custom-input:hover),
:deep(.n-input.custom-input:focus-within) {
   border-color: #10b981; /* Emerald 500 */
   background-color: #fff;
}
:deep(.n-input.custom-input .n-input__input-el) {
  color: #374151;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}
</style>
