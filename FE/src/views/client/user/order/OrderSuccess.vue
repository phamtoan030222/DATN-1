<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
import { useCartStore } from '@/store/app/cart'
import { storeToRefs } from 'pinia'
import { getInvoiceById, getInvoiceDetails } from '@/service/api/invoice.api'

const router = useRouter()
const route = useRoute()

// --- LOGIC QUẢN LÝ GIỎ HÀNG ---
const { cartItems, cartItemBuyNow } = storeToRefs(useCartStore())
const { removeCart } = useCartStore()

// --- LOGIC HIỂN THỊ ĐƠN HÀNG ---
const maHoaDon = computed(() => route.query['ma-hoa-don'] as string || '')
const status = ref<'success' | 'cancelled'>('success')
const loading = ref(true)
const error = ref<string | null>(null)

// Dữ liệu hóa đơn
const invoiceData = ref<any>(null)
const invoiceDetails = ref<any[]>([])

onMounted(async () => {
  const payment = route.query.payment as string

  // Xử lý khi khách hàng hủy thanh toán
  if (payment === 'cancelled') {
    router.replace('/checkout?payment=cancelled')
    return
  }

  // Chặn nếu không có mã hóa đơn
  if (!maHoaDon.value) {
    router.replace('/')
    return
  }

  status.value = 'success'

  // 1. Dọn dẹp giỏ hàng sau khi mua thành công
  await cleanupCart()

  // 2. Lấy dữ liệu đơn hàng để hiển thị
  await fetchOrderDetail()
})

// Hàm xóa giỏ hàng
async function cleanupCart() {
  try {
    const selectedIdsRaw = localStorage.getItem('SELECTED_CART_ITEMS')
    const selectedIds = selectedIdsRaw ? JSON.parse(selectedIdsRaw) : []
    if (cartItemBuyNow.value) {
      await removeCart(cartItemBuyNow.value.productDetailId, { buyNow: true })
    }
    else {
      const items = cartItems.value.filter(i => selectedIds.includes(i.productDetailId))
      await Promise.all(items.map(i => removeCart(i.productDetailId)))
      localStorage.removeItem('SELECTED_CART_ITEMS')
    }
  }
  catch (e) {
    console.error('Lỗi xóa cart:', e)
  }
}

// Hàm gọi API lấy thông tin đơn hàng
async function fetchOrderDetail() {
  loading.value = true
  error.value = null

  try {
    const invoiceRes = await getInvoiceById(maHoaDon.value as string)

    if (invoiceRes?.data) {
      invoiceData.value = invoiceRes.data
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

// --- CÁC HÀM XỬ LÝ SỰ KIỆN NÚT BẤM ---
function goHome() {
  router.push('/')
}

function continueShopping() {
  router.push('/') // Điều hướng về trang chủ/sản phẩm để mua tiếp
}

function handleClickTracking() {
  router.push({
    name: 'OrderTracking',
    query: { q: maHoaDon.value },
  })
}

// --- CÁC HÀM FORMAT DỮ LIỆU ---

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

function formatCurrency(amount: number) {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
  }).format(amount)
}

function getPaymentMethodText(typePayment: string) {
  const methods: Record<string, string> = {
    0: 'Thanh toán khi nhận hàng (COD)',
    1: 'Momo',
    2: 'VNPAY',
    3: 'VietQR',
  }
  return methods[typePayment] || typePayment
}

function getPaymentStatus(status: string) {
  const statusMap: Record<string, { text: string, type: 'success' | 'warning' | 'error' }> = {
    CHUA_THANH_TOAN: { text: 'Chưa thanh toán', type: 'warning' },
    DA_THANH_TOAN: { text: 'Đã thanh toán', type: 'success' },
    THAT_BAI: { text: 'Thất bại', type: 'error' },
  }
  return statusMap[status] || { text: status, type: 'info' }
}

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

function maskEmail(email: string) {
  if (!email)
    return ''
  const [name, domain] = email.split('@')
  const maskedName = `${name.charAt(0)}***${name.charAt(name.length - 1)}`
  return `${maskedName}@${domain}`
}
</script>

<template>
  <div class="os-page">
    <div class="os-wrapper">
      <!-- Loading State -->
      <div v-if="loading" class="os-loading">
        <NSpin size="large" />
        <p class="os-loading__text">
          Đang tải thông tin đơn hàng...
        </p>
      </div>

      <!-- Error State -->
      <div v-else-if="error || !invoiceData" class="os-error">
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

      <!-- Main Content -->
      <template v-else>
        <!-- Hero Header -->
        <div class="os-hero">
          <div class="os-hero__check">
            <div class="os-hero__check-ring os-hero__check-ring--outer" />
            <div class="os-hero__check-ring os-hero__check-ring--inner" />
            <NIcon size="40" class="os-hero__check-icon">
              <CheckmarkCircleOutline />
            </NIcon>
          </div>
          <h1 class="os-hero__title">
            Đặt hàng thành công!
          </h1>
          <p class="os-hero__subtitle">
            Cảm ơn bạn đã tin tưởng mua sắm tại cửa hàng của chúng tôi
          </p>
          <div class="os-hero__code-badge">
            <span class="os-hero__code-label">Mã đơn hàng</span>
            <span class="os-hero__code-value">{{ invoiceData.code }}</span>
            <NTag size="small" :type="getOrderStatus(invoiceData.invoiceStatus).type" round class="os-hero__status-tag">
              {{ getOrderStatus(invoiceData.invoiceStatus).text }}
            </NTag>
          </div>
        </div>

        <!-- Body -->
        <div class="os-body">
          <!-- Left Column -->
          <div class="os-col os-col--left">
            <!-- Order Info Card -->
            <div class="os-card">
              <div class="os-card__head">
                <NIcon size="18">
                  <ReceiptOutline />
                </NIcon>
                <span>Chi tiết đơn hàng</span>
              </div>
              <div class="os-info-list">
                <div class="os-info-row">
                  <span class="os-info-row__label">Thời gian đặt</span>
                  <span class="os-info-row__value">{{ formatDate(invoiceData.createDate) }}</span>
                </div>
                <div class="os-info-row">
                  <span class="os-info-row__label">Thanh toán</span>
                  <span class="os-info-row__value">{{ getPaymentMethodText(invoiceData.typePayment) }}</span>
                </div>
                <div class="os-info-row">
                  <span class="os-info-row__label">Trạng thái TT</span>
                  <NTag size="small" :type="getPaymentStatus(invoiceData.trangThaiThanhToan).type" round>
                    {{ getPaymentStatus(invoiceData.trangThaiThanhToan).text }}
                  </NTag>
                </div>
                <div class="os-info-divider" />
                <div class="os-info-row">
                  <span class="os-info-row__label">
                    <NIcon size="14"><MailOutline /></NIcon> Email
                  </span>
                  <span class="os-info-row__value">{{ maskEmail(invoiceData.email) }}</span>
                </div>
                <div class="os-info-row">
                  <span class="os-info-row__label">
                    <NIcon size="14"><MailOutline /></NIcon> Người nhận
                  </span>
                  <span class="os-info-row__value os-info-row__value--bold">{{ invoiceData.nameReceiver }}</span>
                </div>
                <div class="os-info-row">
                  <span class="os-info-row__label">
                    <NIcon size="14"><MailOutline /></NIcon> SĐT
                  </span>
                  <span class="os-info-row__value">{{ invoiceData.phoneReceiver }}</span>
                </div>
                <div class="os-info-row os-info-row--address">
                  <span class="os-info-row__label">
                    <NIcon size="14"><LocationOutline /></NIcon> Địa chỉ
                  </span>
                  <span class="os-info-row__value">{{ invoiceData.addressReceiver }}</span>
                </div>
                <div v-if="invoiceData.description" class="os-info-row">
                  <span class="os-info-row__label">Ghi chú</span>
                  <span class="os-info-row__value os-info-row__value--note">{{ invoiceData.description }}</span>
                </div>
              </div>
            </div>

            <!-- Steps Card -->
            <div class="os-card os-card--steps">
              <div class="os-card__head">
                <NIcon size="18">
                  <TimeOutline />
                </NIcon>
                <span>Các bước tiếp theo</span>
              </div>
              <div class="os-steps">
                <div class="os-step">
                  <div class="os-step__dot">
                    <span>1</span>
                  </div>
                  <div class="os-step__line" />
                  <div class="os-step__body">
                    <h4>Xác nhận đơn hàng</h4>
                    <p>Chúng tôi sẽ gọi điện xác nhận trong vòng 15 phút</p>
                  </div>
                </div>
                <div class="os-step">
                  <div class="os-step__dot">
                    <span>2</span>
                  </div>
                  <div class="os-step__line" />
                  <div class="os-step__body">
                    <h4>Đóng gói & vận chuyển</h4>
                    <p>Đơn hàng được đóng gói và bàn giao đơn vị vận chuyển</p>
                  </div>
                </div>
                <div class="os-step os-step--last">
                  <div class="os-step__dot">
                    <span>3</span>
                  </div>
                  <div class="os-step__body">
                    <h4>Theo dõi đơn hàng</h4>
                    <p>Theo dõi trạng thái đơn hàng qua mã vận đơn</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Right Column -->
          <div class="os-col os-col--right">
            <!-- Products Card -->
            <div class="os-card">
              <div class="os-card__head">
                <NIcon size="18">
                  <CartOutline />
                </NIcon>
                <span>Sản phẩm đã mua</span>
                <span class="os-card__badge">{{ invoiceDetails.length }}</span>
              </div>
              <div class="os-products">
                <div v-for="item in invoiceDetails" :key="item.id" class="os-product">
                  <div class="os-product__img">
                    <img :src="item.urlImage || 'https://via.placeholder.com/80x80'" :alt="item.nameProductDetail">
                  </div>
                  <div class="os-product__info">
                    <h4 class="os-product__name">
                      {{ item.nameProductDetail }}
                    </h4>
                    <div class="os-product__specs">
                      <span v-if="item.cpu">{{ item.cpu }}</span>
                      <span v-if="item.ram">{{ item.ram }}</span>
                      <span v-if="item.hardDrive">{{ item.hardDrive }}</span>
                      <span v-if="item.gpu">{{ item.gpu }}</span>
                      <span v-if="item.color">{{ item.color }}</span>
                    </div>
                    <div class="os-product__foot">
                      <span class="os-product__qty">x{{ item.quantity }}</span>
                      <span class="os-product__price">{{ formatCurrency(item.price * item.quantity) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Total -->
              <div class="os-total">
                <div class="os-total__row">
                  <span>Tạm tính</span>
                  <span>{{ formatCurrency(invoiceData.totalAmountAfterDecrease) }}</span>
                </div>
                <div class="os-total__row">
                  <span>Phí vận chuyển</span>
                  <span :class="invoiceData.shippingFee === 0 ? 'os-total__free' : ''">
                    {{ invoiceData.shippingFee === 0 ? '🎉 Miễn phí' : formatCurrency(invoiceData.shippingFee) }}
                  </span>
                </div>
                <div class="os-total__row os-total__row--final">
                  <span>Tổng thanh toán</span>
                  <span class="os-total__amount">{{ formatCurrency(invoiceData.totalAmount) }}</span>
                </div>
              </div>
            </div>

            <!-- Actions -->
            <div class="os-actions">
              <button class="os-btn os-btn--primary" @click="handleClickTracking">
                <NIcon size="18">
                  <PaperPlaneOutline />
                </NIcon>
                Theo dõi đơn hàng
              </button>
              <button class="os-btn os-btn--outline" @click="continueShopping">
                <NIcon size="18">
                  <CartOutline />
                </NIcon>
                Mua thêm sản phẩm
              </button>
              <button class="os-btn os-btn--ghost" @click="goHome">
                <NIcon size="18">
                  <HomeOutline />
                </NIcon>
                Về trang chủ
              </button>
            </div>

            <!-- Footer note -->
            <div class="os-support">
              <p>Cần hỗ trợ? Hotline <strong>1900 1234</strong> (miễn phí) · <strong>support@store.vn</strong></p>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
/* ─── Base ───────────────────────────────── */
.os-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding: 32px 16px 48px;
}

.os-wrapper {
  max-width: 1020px;
  margin: 0 auto;
}

/* ─── Loading / Error ────────────────────── */
.os-loading {
  background: #fff;
  border-radius: 8px;
  padding: 64px 20px;
  text-align: center;
  border: 1px solid #e5e5e5;
}
.os-loading__text {
  margin-top: 16px;
  color: #888;
  font-size: 14px;
}
.os-error {
  background: #fff;
  border-radius: 8px;
  padding: 40px 20px;
  border: 1px solid #e5e5e5;
}

/* ─── Hero ───────────────────────────────── */
.os-hero {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  padding: 36px 24px 28px;
  text-align: center;
  margin-bottom: 16px;
}
.os-hero__check {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
.os-hero__check-icon {
  color: #22c55e;
  background: #f0fdf4;
  border-radius: 50%;
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #bbf7d0;
}
/* hide unused rings */
.os-hero__check-ring { display: none; }

.os-hero__title {
  font-size: 22px;
  font-weight: 700;
  color: #111;
  margin-bottom: 6px;
}
.os-hero__subtitle {
  font-size: 14px;
  color: #666;
  margin-bottom: 20px;
}
.os-hero__code-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #f9f9f9;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 8px 16px;
}
.os-hero__code-label {
  font-size: 13px;
  color: #888;
}
.os-hero__code-value {
  font-family: monospace;
  font-size: 14px;
  font-weight: 700;
  color: #111;
}

/* ─── Body ───────────────────────────────── */
.os-body {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  align-items: start;
}

/* ─── Card ───────────────────────────────── */
.os-card {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}
.os-card--steps { margin-bottom: 0; }

.os-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 18px;
  border-bottom: 1px solid #f0f0f0;
  font-size: 13.5px;
  font-weight: 600;
  color: #333;
  background: #fafafa;
}
.os-card__badge {
  margin-left: auto;
  background: #e5e7eb;
  color: #374151;
  font-size: 12px;
  font-weight: 700;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

/* ─── Info List ──────────────────────────── */
.os-info-list {
  padding: 14px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.os-info-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13.5px;
}
.os-info-row__label {
  min-width: 110px;
  color: #888;
  font-weight: 500;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 1px;
}
.os-info-row__value {
  color: #222;
  font-weight: 500;
  flex: 1;
  line-height: 1.5;
}
.os-info-row__value--bold { font-weight: 700; }
.os-info-row__value--note { color: #888; font-style: italic; }
.os-info-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 2px 0;
}

/* ─── Steps ──────────────────────────────── */
.os-steps {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.os-step {
  display: flex;
  gap: 12px;
  padding-bottom: 14px;
  position: relative;
}
.os-step--last { padding-bottom: 0; }
.os-step__dot {
  width: 26px;
  height: 26px;
  min-width: 26px;
  background: #22c55e;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
  position: relative;
  z-index: 1;
}
.os-step__line {
  display: none;
}
.os-step:not(.os-step--last) .os-step__dot::after {
  content: '';
  position: absolute;
  top: 26px;
  left: 50%;
  transform: translateX(-50%);
  width: 1px;
  height: calc(100% + 14px);
  background: #e5e5e5;
  z-index: 0;
}
.os-step__body { padding-top: 3px; flex: 1; min-width: 0; }
.os-step__body h4 {
  font-size: 13px;
  font-weight: 600;
  color: #222;
  margin-bottom: 2px;
}
.os-step__body p {
  font-size: 12px;
  color: #888;
  line-height: 1.5;
}

/* ─── Products ───────────────────────────── */
.os-products { padding: 4px 0; }
.os-product {
  display: flex;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid #f5f5f5;
}
.os-product:last-child { border-bottom: none; }

.os-product__img {
  width: 68px;
  height: 68px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
  border: 1px solid #eee;
  flex-shrink: 0;
}
.os-product__img img { width: 100%; height: 100%; object-fit: cover; }

.os-product__info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  justify-content: center;
}
.os-product__name {
  font-size: 13.5px;
  font-weight: 600;
  color: #222;
  line-height: 1.4;
}
.os-product__specs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.os-product__specs span {
  background: #f5f5f5;
  color: #666;
  font-size: 11px;
  padding: 2px 7px;
  border-radius: 4px;
}
.os-product__foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.os-product__qty { font-size: 12px; color: #888; }
.os-product__price {
  font-size: 14px;
  font-weight: 700;
  color: #111;
}

/* ─── Total ──────────────────────────────── */
.os-total {
  margin: 0 18px 16px;
  border: 1px solid #f0f0f0;
  border-radius: 6px;
  padding: 14px 16px;
  background: #fafafa;
}
.os-total__row {
  display: flex;
  justify-content: space-between;
  font-size: 13.5px;
  color: #666;
  margin-bottom: 8px;
}
.os-total__row:last-child { margin-bottom: 0; }
.os-total__free { color: #22c55e; font-weight: 600; }
.os-total__row--final {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e5e5;
  font-weight: 700;
  color: #111;
  font-size: 14px;
}
.os-total__amount {
  font-size: 18px;
  font-weight: 700;
  color: #111;
}

/* ─── Actions ────────────────────────────── */
.os-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}
.os-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  height: 42px;
  border-radius: 6px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  transition: opacity 0.15s, background 0.15s;
}
.os-btn:hover { opacity: 0.88; }
.os-btn--primary { background: #22c55e; color: #fff; }
.os-btn--outline {
  background: #fff;
  color: #22c55e;
  border: 1.5px solid #22c55e;
}
.os-btn--ghost { background: #f0f0f0; color: #444; }

/* ─── Support ────────────────────────────── */
.os-support {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 6px;
  padding: 12px 16px;
  text-align: center;
  font-size: 13px;
  color: #888;
  line-height: 1.6;
}
.os-support strong { color: #333; }

/* ─── Responsive ─────────────────────────── */
@media (max-width: 820px) {
  .os-body { grid-template-columns: 1fr; }
}
@media (max-width: 480px) {
  .os-product { flex-direction: column; }
  .os-product__foot { justify-content: space-between; }
  .os-info-row { flex-direction: column; gap: 2px; }
  .os-info-row__label { min-width: unset; }
}
</style>
