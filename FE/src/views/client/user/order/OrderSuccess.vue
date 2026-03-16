<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { NButton, NIcon, NResult, NSpace, NSpin, NTag } from 'naive-ui'
import {
  CartOutline,
  CheckmarkCircleOutline,
  HomeOutline,
  LocationOutline,
  MailOutline,
  PaperPlaneOutline,
  ReceiptOutline,
  TimeOutline,
} from '@vicons/ionicons5'
import { computed, onMounted, ref } from 'vue'
import { getInvoiceById, getInvoiceDetails } from '@/service/api/invoice.api' // Import đúng API

const router = useRouter()
const route = useRoute()

const maHoaDon = computed(() => route.query['ma-hoa-don'] || '')
const loading = ref(true)
const error = ref<string | null>(null)

// Dữ liệu hóa đơn
const invoiceData = ref<ClientInvoiceDetailResponse | null>(null)
const invoiceDetails = ref<ClientInvoiceDetailsResponse[]>([])

// Lấy thông tin đơn hàng từ API
async function fetchOrderDetail() {
  if (!maHoaDon.value) {
    error.value = 'Không tìm thấy mã đơn hàng'
    loading.value = false
    return
  }

  loading.value = true
  error.value = null

  try {
    // Gọi API lấy thông tin hóa đơn
    const invoiceRes = await getInvoiceById(maHoaDon.value as string)

    if (invoiceRes?.data) {
      invoiceData.value = invoiceRes.data

      // Gọi API lấy chi tiết sản phẩm của hóa đơn
      const detailsRes = await getInvoiceDetails([invoiceRes.data.id])

      if (detailsRes?.data) {
        invoiceDetails.value = detailsRes.data
      }
    }
    else {
      error.value = 'Không tìm thấy thông tin đơn hàng'
    }
  }
  catch (err) {
    console.error('Lỗi lấy thông tin đơn hàng:', err)
    error.value = 'Có lỗi xảy ra khi tải thông tin đơn hàng'
  }
  finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchOrderDetail()
})

function handleClickTracking() {
  router.push({
    name: 'OrderTracking',
    query: {
      q: maHoaDon.value,
    },
  })
}

function continueShopping() {
  router.push('/san-pham')
}

function goHome() {
  router.push('/')
}

// Format ngày đặt hàng (timestamp)
function formatDate(timestamp: number) {
  if (!timestamp)
    return ''
  const date = new Date(timestamp)
  return date.toLocaleDateString('vi-VN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// Format currency
function formatCurrency(amount: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount)
}

// Lấy phương thức thanh toán text
function getPaymentMethodText(typePayment: string) {
  const methods: Record<string, string> = {
    0: 'Thanh toán khi nhận hàng (COD)',
    1: 'Momo',
    2: 'VNPAY',
    3: 'VietQR',
  }
  return methods[typePayment] || typePayment
}

// Lấy trạng thái thanh toán
function getPaymentStatus(status: string) {
  const statusMap: Record<string, { text: string, type: 'success' | 'warning' | 'error' }> = {
    CHUA_THANH_TOAN: { text: 'Chưa thanh toán', type: 'warning' },
    DA_THANH_TOAN: { text: 'Đã thanh toán', type: 'success' },
    THAT_BAI: { text: 'Thất bại', type: 'error' },
  }
  return statusMap[status] || { text: status, type: 'info' }
}

// Lấy trạng thái đơn hàng
function getOrderStatus(status: number) {
  const statusMap: Record<number, { text: string, type: 'success' | 'info' | 'warning' | 'error' }> = {
    0: { text: 'Chờ xác nhận', type: 'warning' },
    1: { text: 'Đã xác nhận', type: 'success' },
    2: { text: 'Đang giao', type: 'info' },
    3: { text: 'Hoàn thành', type: 'success' },
    4: { text: 'Đã hủy', type: 'error' },
  }
  return statusMap[status] || { text: 'Không xác định', type: 'info' }
}

// Ẩn email
function maskEmail(email: string) {
  if (!email)
    return ''
  const [name, domain] = email.split('@')
  const maskedName = `${name.charAt(0)}***${name.charAt(name.length - 1)}`
  return `${maskedName}@${domain}`
}
</script>

<template>
  <div class="order-success">
    <div class="order-success__container">
      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <NSpin size="large" />
        <p>Đang tải thông tin đơn hàng...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error || !invoiceData" class="error-state">
        <NResult
          status="error"
          title="Không tìm thấy đơn hàng"
          :description="error || 'Mã đơn hàng không tồn tại hoặc đã bị xóa'"
        >
          <template #footer>
            <NButton @click="goHome">
              Về trang chủ
            </NButton>
          </template>
        </NResult>
      </div>

      <!-- Success State with Real Data -->
      <template v-else>
        <!-- Header với icon success -->
        <div class="order-success__header">
          <div class="success-icon-wrapper">
            <div class="success-icon-circle">
              <NIcon size="48" class="success-icon">
                <CheckmarkCircleOutline />
              </NIcon>
            </div>
          </div>
          <h1 class="order-success__title">
            Đặt hàng thành công!
          </h1>
          <p class="order-success__subtitle">
            Cảm ơn bạn đã tin tưởng và mua sắm tại cửa hàng của chúng tôi
          </p>
        </div>

        <!-- Thông tin đơn hàng -->
        <div class="order-success__info">
          <div class="info-card">
            <div class="info-card__header">
              <NIcon size="20">
                <ReceiptOutline />
              </NIcon>
              <span>Thông tin đơn hàng</span>
            </div>

            <div class="info-card__content">
              <div class="info-row">
                <span class="info-label">Mã đơn hàng:</span>
                <div class="info-value-wrapper">
                  <span class="info-value order-code">{{ invoiceData.code }}</span>
                  <NTag size="small" :type="getOrderStatus(invoiceData.invoiceStatus).type" round>
                    {{ getOrderStatus(invoiceData.invoiceStatus).text }}
                  </NTag>
                </div>
              </div>

              <div class="info-row">
                <span class="info-label">Thời gian đặt:</span>
                <span class="info-value">{{ formatDate(invoiceData.createDate) }}</span>
              </div>

              <div class="info-row">
                <span class="info-label">Phương thức thanh toán:</span>
                <span class="info-value">{{ getPaymentMethodText(invoiceData.typePayment) }}</span>
              </div>

              <div class="info-row">
                <span class="info-label">Trạng thái thanh toán:</span>
                <NTag size="small" :type="getPaymentStatus(invoiceData.trangThaiThanhToan).type" round>
                  {{ getPaymentStatus(invoiceData.trangThaiThanhToan).text }}
                </NTag>
              </div>

              <div class="info-divider" />

              <div class="info-row">
                <span class="info-label">
                  <NIcon size="16">
                    <MailOutline />
                  </NIcon>
                  Email:
                </span>
                <span class="info-value">{{ maskEmail(invoiceData.email) }}</span>
              </div>

              <div class="info-row">
                <span class="info-label">
                  <NIcon size="16">
                    <LocationOutline />
                  </NIcon>
                  Địa chỉ:
                </span>
                <span class="info-value">{{ invoiceData.addressReceiver }}</span>
              </div>

              <div class="info-row">
                <span class="info-label">
                  <NIcon size="16">
                    <MailOutline />
                  </NIcon>
                  Người nhận:
                </span>
                <span class="info-value">{{ invoiceData.nameReceiver }}</span>
              </div>

              <div class="info-row">
                <span class="info-label">
                  <NIcon size="16">
                    <MailOutline />
                  </NIcon>
                  SĐT:
                </span>
                <span class="info-value">{{ invoiceData.phoneReceiver }}</span>
              </div>

              <div v-if="invoiceData.description" class="info-row">
                <span class="info-label">Ghi chú:</span>
                <span class="info-value">{{ invoiceData.description }}</span>
              </div>
            </div>
          </div>

          <!-- Hướng dẫn tiếp theo -->
          <div class="guide-card">
            <div class="guide-card__header">
              <NIcon size="20">
                <TimeOutline />
              </NIcon>
              <span>Các bước tiếp theo</span>
            </div>

            <div class="guide-steps">
              <div class="step-item">
                <div class="step-number">
                  1
                </div>
                <div class="step-content">
                  <h4>Xác nhận đơn hàng</h4>
                  <p>Chúng tôi sẽ gọi điện xác nhận đơn hàng trong vòng 15 phút</p>
                </div>
              </div>

              <div class="step-item">
                <div class="step-number">
                  2
                </div>
                <div class="step-content">
                  <h4>Đóng gói và vận chuyển</h4>
                  <p>Đơn hàng sẽ được đóng gói và bàn giao cho đơn vị vận chuyển</p>
                </div>
              </div>

              <div class="step-item">
                <div class="step-number">
                  3
                </div>
                <div class="step-content">
                  <h4>Theo dõi đơn hàng</h4>
                  <p>Bạn có thể theo dõi trạng thái đơn hàng qua mã vận đơn</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Sản phẩm đã mua -->
        <div class="order-success__products">
          <h3 class="products-title">
            Sản phẩm đã mua ({{ invoiceDetails.length }})
          </h3>

          <div v-for="item in invoiceDetails" :key="item.id" class="product-item">
            <div class="product-image">
              <img :src="item.urlImage || 'https://via.placeholder.com/80x80'" :alt="item.nameProductDetail">
            </div>
            <div class="product-details">
              <h4 class="product-name">
                {{ item.nameProductDetail }}
              </h4>
              <div class="product-specs">
                <span v-if="item.cpu">CPU: {{ item.cpu }}</span>
                <span v-if="item.ram">RAM: {{ item.ram }}</span>
                <span v-if="item.hardDrive">Ổ cứng: {{ item.hardDrive }}</span>
                <span v-if="item.gpu">GPU: {{ item.gpu }}</span>
                <span v-if="item.color">Màu: {{ item.color }}</span>
              </div>
              <div class="product-meta">
                <span class="product-quantity">Số lượng: {{ item.quantity }}</span>
                <span class="product-price">{{ formatCurrency(item.price * item.quantity) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Tổng thanh toán -->
        <div class="order-success__total">
          <div class="total-row">
            <span>Tạm tính</span>
            <span>{{ formatCurrency(invoiceData.totalAmount) }}</span>
          </div>
          <div class="total-row">
            <span>Phí vận chuyển</span>
            <span :class="{ 'free-ship': invoiceData.shippingFee === 0 }">
              {{ invoiceData.shippingFee === 0 ? 'Miễn phí' : formatCurrency(invoiceData.shippingFee) }}
            </span>
          </div>
          <div class="total-row final">
            <span>Tổng thanh toán</span>
            <span class="final-price">{{ formatCurrency(invoiceData.totalAmountAfterDecrease) }}</span>
          </div>
        </div>

        <!-- Action buttons -->
        <div class="order-success__actions">
          <NSpace justify="center" size="large" wrap>
            <NButton size="large" class="btn-outline" @click="goHome">
              <template #icon>
                <NIcon>
                  <HomeOutline />
                </NIcon>
              </template>
              Về trang chủ
            </NButton>

            <NButton size="large" class="btn-primary" @click="handleClickTracking">
              <template #icon>
                <NIcon>
                  <PaperPlaneOutline />
                </NIcon>
              </template>
              Theo dõi đơn hàng
            </NButton>

            <NButton size="large" class="btn-secondary" @click="continueShopping">
              <template #icon>
                <NIcon>
                  <CartOutline />
                </NIcon>
              </template>
              Mua thêm sản phẩm
            </NButton>
          </NSpace>
        </div>

        <!-- Footer -->
        <div class="order-success__footer">
          <p>Mọi thắc mắc vui lòng liên hệ hotline: <strong>1900 1234</strong> (Miễn phí)</p>
          <p>Email hỗ trợ: <strong>support@shop.com</strong></p>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.order-success {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.order-success__container {
  max-width: 800px;
  width: 100%;
  background: white;
  border-radius: 24px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  animation: slideUp 0.5s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* Loading State */
.loading-state {
  padding: 80px 20px;
  text-align: center;
  background: white;
}

.loading-state p {
  margin-top: 20px;
  color: #64748b;
  font-size: 16px;
}

/* Error State */
.error-state {
  padding: 40px 20px;
  background: white;
}

/* Header Styles */
.order-success__header {
  background: linear-gradient(135deg, #00b09b, #96c93d);
  padding: 48px 32px 40px;
  text-align: center;
  color: white;
  position: relative;
  overflow: hidden;
}

.order-success__header::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 50%);
  animation: rotate 20s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.success-icon-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
  position: relative;
  z-index: 1;
}

.success-icon-circle {
  width: 96px;
  height: 96px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.success-icon {
  color: white;
}

.order-success__title {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
  text-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.order-success__subtitle {
  font-size: 16px;
  opacity: 0.9;
  position: relative;
  z-index: 1;
  max-width: 400px;
  margin: 0 auto;
}

/* Info Section */
.order-success__info {
  padding: 32px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.info-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.info-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  color: #1e293b;
  font-weight: 600;
  font-size: 16px;
}

.info-card__content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.info-label {
  min-width: 100px;
  color: #64748b;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-value-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.info-value {
  color: #1e293b;
  font-weight: 500;
  font-size: 14px;
}

.order-code {
  font-weight: 700;
  color: #2563eb;
  background: #eff6ff;
  padding: 4px 8px;
  border-radius: 6px;
  font-family: monospace;
}

.info-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 8px 0;
}

/* Guide Card */
.guide-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.guide-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: #1e293b;
  font-weight: 600;
  font-size: 16px;
}

.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.step-number {
  width: 28px;
  height: 28px;
  background: #00b09b;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-content h4 {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.step-content p {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

/* Products Section */
.order-success__products {
  padding: 32px;
  border-bottom: 1px solid #e2e8f0;
}

.products-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 20px;
}

.product-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px dashed #e2e8f0;
}

.product-item:last-child {
  border-bottom: none;
}

.product-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  background: #f1f5f9;
  flex-shrink: 0;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.product-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
}

.product-name {
  font-size: 15px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.4;
}

.product-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 12px;
  color: #64748b;
}

.product-specs span {
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-quantity {
  font-size: 13px;
  color: #64748b;
}

.product-price {
  font-size: 15px;
  font-weight: 600;
  color: #2563eb;
}

/* Total Section */
.order-success__total {
  padding: 32px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.total-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  color: #64748b;
  font-size: 15px;
}

.total-row.final {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 2px dashed #e2e8f0;
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
}

.free-ship {
  color: #00b09b;
  font-weight: 500;
}

.final-price {
  color: #2563eb;
  font-size: 24px;
  font-weight: 700;
}

/* Actions Section */
.order-success__actions {
  padding: 32px;
  text-align: center;
}

.btn-primary {
  background: linear-gradient(135deg, #00b09b, #96c93d);
  border: none;
  color: white;
  font-weight: 600;
  padding: 0 32px;
  height: 48px;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(0, 176, 155, 0.3);
}

.btn-outline {
  background: transparent;
  border: 2px solid #00b09b;
  color: #00b09b;
  font-weight: 600;
  padding: 0 32px;
  height: 48px;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.btn-outline:hover {
  background: #00b09b;
  color: white;
  transform: translateY(-2px);
}

.btn-secondary {
  background: #f1f5f9;
  border: none;
  color: #1e293b;
  font-weight: 600;
  padding: 0 32px;
  height: 48px;
  border-radius: 24px;
  transition: all 0.3s ease;
}

.btn-secondary:hover {
  background: #e2e8f0;
  transform: translateY(-2px);
}

/* Footer */
.order-success__footer {
  padding: 24px 32px;
  background: #f1f5f9;
  text-align: center;
  color: #64748b;
  font-size: 14px;
}

.order-success__footer p {
  margin: 4px 0;
}

.order-success__footer strong {
  color: #00b09b;
}

/* Responsive */
@media (max-width: 768px) {
  .order-success__header {
    padding: 40px 20px;
  }

  .order-success__title {
    font-size: 28px;
  }

  .order-success__info {
    grid-template-columns: 1fr;
    padding: 20px;
  }

  .order-success__actions {
    padding: 20px;
  }

  .total-row.final {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .final-price {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .product-item {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }

  .product-meta {
    flex-direction: column;
    gap: 4px;
  }

  .info-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .info-label {
    min-width: auto;
  }

  .product-specs {
    justify-content: center;
  }
}
</style>
