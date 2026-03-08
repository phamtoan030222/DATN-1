<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import QrcodeVue from 'qrcode.vue'
import { toast } from 'vue3-toastify'
import PaymentService from '@/service/payment.service'
import type { VNPayPaymentRequest } from '@/service/payment.service'

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
const countdown = ref(300)

const resultVisible = ref(false)
const paymentSuccess = ref(false)
const paymentMessage = ref('')
const amount = ref(0)
const retryCount = ref(0)

let intervalId: number | null = null

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
    props.invoice.totalAmountAfterDecrease
    || props.invoice.totalAmount
    || 0,
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

  window.open(
    paymentUrl.value,
    '_blank',
    'width=500,height=700',
  )
}

/* ==============================
CHECK PAYMENT STATUS
============================== */

async function checkPaymentStatus() {
  if (!transactionId.value || checking.value)
    return

  checking.value = true

  try {
    const res = await PaymentService.checkPaymentResult({
      transactionId: transactionId.value,
    })

    if (res.code === '00') {
      paymentSuccess.value = true
      paymentMessage.value = 'Thanh toán thành công'
      amount.value = res.amount || 0

      stopChecking()

      resultVisible.value = true

      emit('payment-success', res)
    }
  }
  catch (e) {
    console.error(e)
  }
  finally {
    checking.value = false
  }
}
function startCheckingStatus() {
  setInterval(async () => {
    const res = await fetch('/api/payment/check-status')

    const data = await res.json()

    if (data.status === 'PAID') {
      window.location.href = '/payment-success'
    }
  }, 3000)
}

// Thử lại thanh toán
function retryPayment() {
  retryCount.value = 0
  paymentUrl.value = null
  createPayment(true)
}

async function createPayment(retry = false) {
  if (!props.invoice) {
    errorMessage.value = 'Không tìm thấy thông tin hóa đơn'
    toast.error(errorMessage.value)
    return false
  }

  // Kiểm tra số tiền hợp lệ
  const amount = props.invoice.totalAmountAfterDecrease || props.invoice.totalAmount || 0
  if (amount <= 0) {
    errorMessage.value = 'Số tiền thanh toán không hợp lệ'
    toast.error(errorMessage.value)
    return false
  }

  isProcessing.value = true
  errorMessage.value = ''

  try {
    console.log('🔄 Đang tạo thanh toán VNPAY cho hóa đơn:', {
      id: props.invoice.id,
      code: props.invoice.code,
      amount,
    })

    const response = await PaymentService.createVNPayPayment(props.invoice)
    console.log('📦 Response từ server:', response)

    // Kiểm tra response từ nhiều format khác nhau
    if (response) {
      // Xử lý response code (có thể là string hoặc number)
      const responseCode = response.code?.toString() || response.status?.toString()

      // Lấy paymentUrl từ nhiều field khác nhau
      const url = response.paymentUrl
        || response.payment_url
        || response.url
        || response.data?.paymentUrl
        || response.data?.url
        || response.data?.payment_url

      // Lấy transactionId từ nhiều field khác nhau
      const transId = response.transactionId
        || response.transaction_id
        || response.txnRef
        || response.data?.transactionId
        || response.data?.txnRef

      if (responseCode === '00' || responseCode === '0' || responseCode === '200') {
        if (url) {
          console.log('✅ Payment URL nhận được:', url)
          paymentUrl.value = url
          transactionId.value = transId || `TXN_${Date.now()}`

          toast.success('Tạo thanh toán thành công! Vui lòng quét mã QR hoặc thanh toán qua link.')

          // Tự động mở cửa sổ thanh toán sau 1 giây
          setTimeout(() => {
            openPaymentWindow()
          }, 1000)

          // Bắt đầu kiểm tra trạng thái
          startCheckingStatus()
          return true
        }
        else {
          errorMessage.value = 'Server không trả về URL thanh toán'
          toast.error(errorMessage.value)
        }
      }
      else {
        errorMessage.value = response.message || response.msg || 'Tạo thanh toán thất bại'
        toast.error(errorMessage.value)

        // Thử lại nếu chưa quá số lần cho phép
        if (retry && retryCount.value < maxRetries) {
          retryCount.value++
          console.log(`🔄 Thử lại lần ${retryCount.value}/${maxRetries}`)
          setTimeout(() => createPayment(true), 2000)
        }
      }
    }
    else {
      errorMessage.value = 'Server không trả về dữ liệu'
      toast.error(errorMessage.value)
    }
  }
  catch (error) {
    console.error('❌ Lỗi tạo thanh toán:', error)
    errorMessage.value = `Lỗi kết nối đến server: ${error.message || 'Unknown error'}`
    toast.error(errorMessage.value)

    // Thử lại nếu là lỗi mạng
    if (retry && retryCount.value < maxRetries) {
      retryCount.value++
      console.log(`🔄 Thử lại lần ${retryCount.value}/${maxRetries} sau lỗi mạng`)
      setTimeout(() => createPayment(true), 3000)
    }
  }
  finally {
    isProcessing.value = false
  }
}

/* ==============================
AUTO CHECK
============================== */

function startChecking() {
  stopChecking()

  intervalId = window.setInterval(async () => {
    if (countdown.value > 0)
      countdown.value--

    await checkPaymentStatus()
  }, 5000)
}

function stopChecking() {
  if (intervalId) {
    clearInterval(intervalId)
    intervalId = null
  }
}

/* ==============================
CLOSE
============================== */

function closeModal() {
  stopChecking()
  reset()
  emit('update:visible', false)
}

/* ==============================
RESULT OK
============================== */

function finishPayment() {
  resultVisible.value = false
  closeModal()
  emit('payment-completed')
}

/* ==============================
WATCH
============================== */

watch(
  () => props.visible,
  async (val) => {
    if (val) {
      reset()
      await createPayment()
    }
    else {
      stopChecking()
    }
  },
)

onBeforeUnmount(() => stopChecking())
</script>

<template>
  <div>
    <!-- ==============================
    PAYMENT MODAL
    ============================== -->

    <a-modal
      :visible="visible"
      title="Thanh toán VNPAY"
      width="420px"
      :footer="null"
      @cancel="closeModal"
    >
      <!-- Loading -->
      <div v-if="loading" class="loading">
        <a-spin size="large" />
        <p>Đang tạo thanh toán...</p>
      </div>

      <!-- Payment -->
      <div v-else-if="paymentUrl" class="payment">
        <div class="amount">
          {{ formattedAmount }}
        </div>

        <div class="invoice">
          Mã đơn: {{ invoice?.code }}
        </div>

        <!-- QR -->
        <div class="qr">
          <QrcodeVue
            :value="paymentUrl"
            :size="220"
          />
        </div>

        <p class="note">
          Quét mã bằng app ngân hàng
        </p>

        <!-- Button -->
        <button
          class="btn-primary"
          @click="openPaymentWindow"
        >
          Thanh toán trên trình duyệt
        </button>

        <!-- Timer -->
        <div class="timer">
          ⏳ Đang chờ thanh toán {{ countdownDisplay }}
        </div>

        <!-- Check -->
        <button
          class="btn-outline"
          @click="checkPaymentStatus"
        >
          Tôi đã thanh toán
        </button>
      </div>

      <!-- Error -->
      <div v-else-if="errorMessage" class="error">
        <p>{{ errorMessage }}</p>
        <button class="btn-primary" @click="closeModal">
          Đóng
        </button>
      </div>

      <!-- Empty -->
      <div v-else class="empty">
        <p>Không có thông tin thanh toán</p>
      </div>
    </a-modal>

    <!-- ==============================
    RESULT MODAL
    ============================== -->

    <a-modal
      :visible="resultVisible"
      width="360px"
      :footer="null"
      @cancel="resultVisible = false"
    >
      <div class="result">
        <div
          class="icon"
          :class="paymentSuccess ? 'success' : 'error'"
        >
          <i
            :class="paymentSuccess
              ? 'fas fa-check-circle'
              : 'fas fa-times-circle'"
          />
        </div>

        <h3>
          {{ paymentSuccess
            ? 'Thanh toán thành công'
            : 'Thanh toán thất bại' }}
        </h3>

        <p>{{ paymentMessage }}</p>

        <div
          v-if="paymentSuccess"
          class="amount-result"
        >
          {{ formatMoney(amount) }}
        </div>

        <div style="display: flex; gap: 8px; justify-content: center;">
          <NButton
            v-if="!paymentSuccess"
            @click="resultVisible = false"
          >
            Đóng
          </NButton>
          <NButton
            v-if="!paymentSuccess"
            @click="retryPayment"
          >
            Thử lại
          </NButton>
          <NButton
            v-if="paymentSuccess"
            class="btn-primary"
            @click="finishPayment"
          >
            Hoàn tất
          </NButton>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
/* container */

.payment{
  text-align:center;
}

/* amount */

.amount{
  font-size:32px;
  font-weight:700;
  color:#1890ff;
}

/* invoice */

.invoice{
  margin-bottom:16px;
  color:#666;
}

/* qr */

.qr{
  margin:20px 0;
}

/* note */

.note{
  color:#666;
  margin-bottom:20px;
}

/* buttons */

.btn-primary{
  width:100%;
  padding:12px;
  background:#1890ff;
  border:none;
  color:white;
  border-radius:8px;
  font-weight:500;
  cursor:pointer;
}

.btn-outline{
  width:100%;
  padding:12px;
  margin-top:10px;
  border:1px solid #1890ff;
  background:white;
  color:#1890ff;
  border-radius:8px;
  cursor:pointer;
}

/* timer */

.timer{
  margin:16px 0;
  color:#666;
}

/* loading */

.loading{
  text-align:center;
  padding:40px;
}

/* result */

.result{
  text-align:center;
}

.icon{
  font-size:60px;
  margin-bottom:16px;
}

.success{
  color:#52c41a;
}

.error{
  color:#ff4d4f;
}

.amount-result{
  font-size:24px;
  font-weight:700;
  margin:12px 0;
}
</style>
