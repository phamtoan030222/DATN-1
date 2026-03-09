<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import QrcodeVue from 'qrcode.vue'
import { toast } from 'vue3-toastify'
import PaymentService from '@/service/payment.service'
import { NButton, NIcon, NSpace, NSpin, NText } from 'naive-ui'
import { CheckmarkCircleOutline, CloseCircleOutline, TimeOutline } from '@vicons/ionicons5'

import vnpayLogo from '../../../images/vnpay.png'
import axios from 'axios'

const props = defineProps({
  visible: Boolean,
  invoice: Object,
})

const emit = defineEmits([
  'update:visible',
  'payment-success',
  'payment-completed',
])

/* ==============================
STATE
============================== */
const loading = ref(false)
const paymentUrl = ref<string | null>(null)
const transactionId = ref<string | null>(null)

const checking = ref(false)
const countdown = ref(300) // 5 phút = 300 giây

const resultVisible = ref(false)
const paymentSuccess = ref(false)
const paymentMessage = ref('')
const amount = ref(0)
const retryCount = ref(0)

// Tách riêng 2 bộ đếm để UI mượt mà
let checkInterval: number | null = null
let countdownInterval: number | null = null

/* ==============================
FORMAT MONEY
============================== */
function formatMoney(value: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(value || 0)
}

const formattedAmount = computed(() => {
  if (!props.invoice)
    return '0₫'
  return formatMoney(
    props.invoice.totalAmountAfterDecrease || props.invoice.totalAmount || 0,
  )
})

const countdownDisplay = computed(() => {
  const min = Math.floor(countdown.value / 60)
  const sec = countdown.value % 60
  return `${min}:${sec.toString().padStart(2, '0')}`
})

/* ==============================
RESET
============================== */
function reset() {
  paymentUrl.value = null
  transactionId.value = null
  countdown.value = 300
  resultVisible.value = false
  stopChecking()
}

const errorMessage = ref('')
const isProcessing = ref(false)
const maxRetries = 3

/* ==============================
OPEN PAYMENT
============================== */
function openPaymentWindow() {
  if (!paymentUrl.value)
    return
  window.open(paymentUrl.value, '_blank', 'width=500,height=700')
}

onMounted(async () => {
  window.addEventListener('message', handlePaymentMessage)
  if (props.visible) {
    reset()
    await createPayment()
  }
})

onBeforeUnmount(() => {
  stopChecking()
  window.removeEventListener('message', handlePaymentMessage)
})

/* ==============================
CHECK PAYMENT STATUS
============================== */
async function checkPaymentStatus() {
  const invoiceId = props.invoice?.orderId || props.invoice?.id // ← FIX
  if (!invoiceId || checking.value)
    return
  checking.value = true

  try {
    const response = await axios.get(
      `http://localhost:2345/api/payment/check-status/${invoiceId}`,
    )
    if (response.data.trangThai === 'DA_THANH_TOAN') {
      paymentSuccess.value = true
      paymentMessage.value = 'Hệ thống đã ghi nhận thanh toán thành công!'
      amount.value = props.invoice?.totalAmountAfterDecrease || 0
      stopChecking()
      resultVisible.value = true
      emit('payment-success', response.data)
    }
  }
  catch (e) {
    console.error('Lỗi khi kiểm tra trạng thái:', e)
  }
  finally {
    checking.value = false
  }
}

function handlePaymentMessage(event: MessageEvent) {
  if (event.data?.type === 'VNPAY_SUCCESS') {
    paymentSuccess.value = true
    paymentMessage.value = 'Hệ thống đã ghi nhận thanh toán thành công!'
    amount.value = props.invoice?.totalAmountAfterDecrease || 0
    stopChecking()
    resultVisible.value = true
    emit('payment-success', event.data)
  }
}

// Thử lại thanh toán
function retryPayment() {
  retryCount.value = 0
  paymentUrl.value = null
  resultVisible.value = false
  errorMessage.value = ''
  countdown.value = 300
  createPayment(true)
}

async function createPayment(retry = false) {
  if (loading.value || isProcessing.value)
    return false

  if (!props.invoice) {
    errorMessage.value = 'Không tìm thấy thông tin hóa đơn'
    toast.error(errorMessage.value)
    return false
  }

  const amount = props.invoice.totalAmountAfterDecrease || props.invoice.totalAmount || 0
  if (amount <= 0) {
    errorMessage.value = 'Số tiền thanh toán không hợp lệ'
    toast.error(errorMessage.value)
    return false
  }

  isProcessing.value = true
  loading.value = true
  errorMessage.value = ''

  try {
    const response = await PaymentService.createVNPayPayment(props.invoice as any)

    if (response) {
      const responseCode = response.code?.toString() || response.status?.toString()
      const url = response.paymentUrl || response.payment_url || response.url || response.data?.paymentUrl || response.data?.url || response.data?.payment_url
      const transId = response.transactionId || response.transaction_id || response.txnRef || response.data?.transactionId || response.data?.txnRef

      if (responseCode === '00' || responseCode === '0' || responseCode === '200') {
        if (url) {
          paymentUrl.value = url
          transactionId.value = transId || `TXN_${Date.now()}`
          toast.success('Tạo thanh toán thành công!')
          startChecking()
          return true
        }
        else {
          errorMessage.value = 'Server không trả về URL thanh toán'
        }
      }
      else {
        errorMessage.value = response.message || response.msg || 'Tạo thanh toán thất bại'
        if (retry && retryCount.value < maxRetries) {
          retryCount.value++
          setTimeout(() => createPayment(true), 2000)
        }
      }
    }
    else {
      errorMessage.value = 'Server không trả về dữ liệu'
    }
  }
  catch (error: any) {
    errorMessage.value = `Lỗi kết nối đến server: ${error?.message || 'Unknown error'}`
    if (retry && retryCount.value < maxRetries) {
      retryCount.value++
      setTimeout(() => createPayment(true), 3000)
    }
  }
  finally {
    isProcessing.value = false
    loading.value = false
  }
}

/* ==============================
AUTO CHECK (Đã sửa lỗi đơ timer)
============================== */
function startChecking() {
  stopChecking()

  // 1. Đồng hồ đếm ngược chạy mỗi 1 giây (để UI mượt)
  countdownInterval = window.setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    }
    else {
      stopChecking() // Hết giờ thì dừng hoàn toàn
      errorMessage.value = 'Mã QR đã hết hạn, vui lòng thử lại.'
      paymentUrl.value = null
    }
  }, 1000)

  // 2. Gọi API check mỗi 5 giây (tránh spam server)
  checkInterval = window.setInterval(async () => {
    if (countdown.value > 0) {
      await checkPaymentStatus()
    }
  }, 5000)
}

function stopChecking() {
  if (checkInterval) {
    clearInterval(checkInterval)
    checkInterval = null
  }
  if (countdownInterval) {
    clearInterval(countdownInterval)
    countdownInterval = null
  }
}

/* ==============================
CLOSE & FINISH
============================== */
function closeModal() {
  stopChecking()
  reset()
  emit('update:visible', false)
}

function finishPayment() {
  resultVisible.value = false
  closeModal()
  emit('payment-completed')
}

/* ==============================
LIFECYCLE
============================== */
onMounted(async () => {
  if (props.visible) {
    reset()
    await createPayment()
  }
})

watch(() => props.visible, (val) => {
  if (!val)
    stopChecking()
})

onBeforeUnmount(() => stopChecking())
</script>

<template>
  <div class="vnpay-container">
    <div v-if="resultVisible" class="result-screen">
      <NIcon size="72" :color="paymentSuccess ? '#18a058' : '#d03050'" class="animate-bounce">
        <CheckmarkCircleOutline v-if="paymentSuccess" />
        <CloseCircleOutline v-else />
      </NIcon>

      <h3 class="result-title" :style="{ color: paymentSuccess ? '#18a058' : '#d03050' }">
        {{ paymentSuccess ? 'Thanh toán thành công!' : 'Thanh toán thất bại' }}
      </h3>
      <p class="result-msg">
        {{ paymentMessage }}
      </p>

      <div v-if="paymentSuccess" class="amount-result">
        {{ formatMoney(amount) }}
      </div>

      <NSpace justify="center" style="margin-top: 24px;">
        <NButton v-if="!paymentSuccess" size="large" @click="closeModal">
          Đóng
        </NButton>
        <NButton v-if="!paymentSuccess" type="primary" size="large" @click="retryPayment">
          Thử lại ngay
        </NButton>
        <NButton v-if="paymentSuccess" type="primary" size="large" block style="width: 200px;" @click="finishPayment">
          Hoàn tất
        </NButton>
      </NSpace>
    </div>

    <div v-else class="payment-screen">
      <div v-if="loading" class="loading-state">
        <NSpin size="large" />
        <p class="loading-text">
          Đang khởi tạo cổng thanh toán an toàn...
        </p>
      </div>

      <div v-else-if="paymentUrl" class="qr-state">
        <div class="header-info">
          <div class="amount">
            {{ formattedAmount }}
          </div>
          <div class="invoice-badge">
            Mã đơn: {{ invoice?.code }}
          </div>
        </div>

        <div class="qr-wrapper">
          <div class="qr-box">
            <QrcodeVue :value="paymentUrl" :size="240" level="H" />
            <div class="qr-logo-center">
              <img :src="vnpayLogo" alt="VNPAY" style="width: 32px; height: 32px; border-radius: 6px; background: white; padding: 2px;" onerror="this.style.display='none'">
            </div>
          </div>
          <p class="note">
            Mở ứng dụng Ngân hàng để quét mã QR
          </p>
        </div>

        <NSpace vertical :size="16" class="actions-wrapper">
          <div class="timer-box">
            <NIcon size="18" color="#f0a020">
              <TimeOutline />
            </NIcon>
            <span class="timer-text">Mã hết hạn sau: <strong>{{ countdownDisplay }}</strong></span>
          </div>

          <NButton
            dashed
            type="primary"
            block
            @click="openPaymentWindow"
          >
            Chuyển hướng thanh toán (Trình duyệt)
          </NButton>
        </NSpace>
      </div>

      <div v-else-if="errorMessage" class="error-state">
        <NIcon size="64" color="#d03050">
          <CloseCircleOutline />
        </NIcon>
        <p class="error-text">
          {{ errorMessage }}
        </p>
        <NSpace justify="center">
          <NButton @click="closeModal">
            Đóng
          </NButton>
          <NButton type="primary" @click="retryPayment">
            Thử lại hệ thống
          </NButton>
        </NSpace>
      </div>
    </div>
  </div>
</template>

<style scoped>
.vnpay-container {
  padding: 8px;
  font-family: system-ui, -apple-system, sans-serif;
}

/* --- Layout chung --- */
.payment-screen, .result-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

/* --- Trạng thái Loading --- */
.loading-state {
  padding: 60px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}
.loading-text {
  color: #666;
  font-size: 15px;
}

/* --- Phần hiển thị thông tin tiền --- */
.header-info {
  margin-bottom: 24px;
}
.amount {
  font-size: 32px;
  font-weight: 800;
  color: #18a058; /* Xanh lá Naive UI chuẩn */
  letter-spacing: -0.5px;
}
.invoice-badge {
  display: inline-block;
  margin-top: 8px;
  padding: 4px 12px;
  background-color: #f3f3f5;
  color: #666;
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
}

/* --- Khối chứa QR Code --- */
.qr-wrapper {
  background: #f8fafc;
  padding: 24px;
  border-radius: 20px;
  border: 1px dashed #e2e8f0;
  margin-bottom: 24px;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.qr-box {
  background: white;
  padding: 12px;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  position: relative;
  display: inline-block;
}
.qr-logo-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  border-radius: 8px;
  padding: 2px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.note {
  margin-top: 16px;
  margin-bottom: 0;
  color: #64748b;
  font-size: 14px;
  font-weight: 500;
}

/* --- Nút bấm và thời gian --- */
.actions-wrapper {
  width: 100%;
  padding: 0 10px;
}
.timer-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: #fffbef;
  border: 1px solid #fde68a;
  padding: 10px;
  border-radius: 12px;
}
.timer-text {
  color: #b45309;
  font-size: 14px;
}
.timer-text strong {
  font-size: 16px;
  color: #d97706;
}

.btn-confirm {
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.3);
}

/* --- Trạng thái lỗi và Kết quả --- */
.error-state {
  padding: 40px 20px;
}
.error-text {
  color: #d03050;
  font-weight: 500;
  margin: 16px 0 24px 0;
  font-size: 15px;
}

.result-screen {
  padding: 20px 10px;
}
.result-title {
  margin: 16px 0 8px 0;
  font-size: 24px;
  font-weight: 700;
}
.result-msg {
  color: #666;
  margin-bottom: 16px;
}
.amount-result {
  font-size: 32px;
  font-weight: 800;
  color: #18a058;
  background: #f0fdf4;
  padding: 16px 32px;
  border-radius: 16px;
  display: inline-block;
  margin-bottom: 12px;
}

/* Animation nhỏ cho icon */
@keyframes bounce-soft {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
.animate-bounce {
  animation: bounce-soft 2s infinite ease-in-out;
}
</style>
