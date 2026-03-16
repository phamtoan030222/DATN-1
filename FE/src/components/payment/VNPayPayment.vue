<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import QrcodeVue from 'qrcode.vue'
import { toast } from 'vue3-toastify'
import PaymentService from '@/service/payment.service'
import { NButton, NIcon, NSpace, NSpin, NText } from 'naive-ui'
import { CheckmarkCircleOutline, CloseCircleOutline, TimeOutline } from '@vicons/ionicons5'
import { huyYeuCauQRApp, yeuCauQRApp } from '@/service/api/admin/banhang.api'

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
  // Redirect thẳng tab hiện tại thay vì mở tab mới
  window.location.href = paymentUrl.value
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

async function manualConfirmPayment() {
  const invoiceId = props.invoice?.orderId || props.invoice?.id
  if (!invoiceId || checking.value)
    return
  checking.value = true

  try {
    const response = await axios.post(
      `http://localhost:2345/api/payment/manual-confirm/${invoiceId}`,
    )
    if (response.data.trangThai === 'DA_THANH_TOAN') {
      paymentSuccess.value = true
      paymentMessage.value = 'Đã xác nhận thanh toán thành công!'
      amount.value = props.invoice?.totalAmountAfterDecrease || 0
      stopChecking()
      resultVisible.value = true
      emit('payment-success', response.data)
    }
  }
  catch (e) {
    toast.error('Xác nhận thất bại, vui lòng thử lại!')
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
          yeuCauQRApp(props.invoice?.orderId || props.invoice?.id, url).catch(e => console.log(e))
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
  const invoiceId = props.invoice?.orderId || props.invoice?.id
  if (invoiceId) {
    huyYeuCauQRApp(invoiceId).catch(e => console.log(e))
  }
}

function finishPayment() {
  resultVisible.value = false
  closeModal()
  emit('payment-completed')
}

/* ==============================
LIFECYCLE
============================== */

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
            <QrcodeVue :value="paymentUrl" :size="180" level="H" />
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
            type="primary"
            size="large"
            block
            :loading="checking"
            class="btn-confirm"
            @click="manualConfirmPayment"
          >
            Xác nhận đã thanh toán
          </NButton>

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
  padding: 4px 8px;
  font-family: system-ui, -apple-system, sans-serif;
}

.payment-screen, .result-screen {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.loading-state {
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.loading-text { color: #666; font-size: 14px; }

.header-info { margin-bottom: 12px; }
.amount {
  font-size: 26px;
  font-weight: 800;
  color: #18a058;
  letter-spacing: -0.5px;
}
.invoice-badge {
  display: inline-block;
  margin-top: 6px;
  padding: 3px 10px;
  background-color: #f3f3f5;
  color: #666;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 500;
}

.qr-wrapper {
  background: #f8fafc;
  padding: 14px 20px;
  border-radius: 16px;
  border: 1px dashed #e2e8f0;
  margin-bottom: 14px;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.qr-box {
  background: white;
  padding: 8px;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  position: relative;
  display: inline-block;
}
.qr-logo-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: white;
  border-radius: 6px;
  padding: 2px;
  display: flex;
  justify-content: center;
  align-items: center;
}
.note {
  margin-top: 10px;
  margin-bottom: 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 500;
}

.actions-wrapper { width: 100%; padding: 0 4px; }
.timer-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: #fffbef;
  border: 1px solid #fde68a;
  padding: 8px;
  border-radius: 10px;
}
.timer-text { color: #b45309; font-size: 13px; }
.timer-text strong { font-size: 15px; color: #d97706; }

.btn-confirm {
  height: 42px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.25);
}

.error-state { padding: 28px 20px; }
.error-text {
  color: #d03050;
  font-weight: 500;
  margin: 12px 0 20px 0;
  font-size: 14px;
}

.result-screen { padding: 16px 8px; }
.result-title { margin: 12px 0 6px 0; font-size: 22px; font-weight: 700; }
.result-msg { color: #666; margin-bottom: 12px; font-size: 14px; }
.amount-result {
  font-size: 26px;
  font-weight: 800;
  color: #18a058;
  background: #f0fdf4;
  padding: 12px 28px;
  border-radius: 14px;
  display: inline-block;
  margin-bottom: 10px;
}

@keyframes bounce-soft {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}
.animate-bounce { animation: bounce-soft 2s infinite ease-in-out; }
</style>
