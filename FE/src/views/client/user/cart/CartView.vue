<script setup lang="ts">
import { getQuantityProdudtDetail } from '@/service/api/client/banhang.api'
import { useCartStore } from '@/store/app/cart'
import { storeToRefs } from 'pinia'
import {
  AddOutline,
  ArrowForward,
  BagCheckOutline,
  CartOutline,
  ChevronForwardOutline,
  FlashOutline,
  GiftOutline,
  HeartOutline,
  RemoveOutline,
  ReturnDownBackOutline,
  ShieldCheckmarkOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import {
  NBadge,
  NButton,
  NCheckbox,
  NCheckboxGroup,
  NEmpty,
  NIcon,
  NPopconfirm,
  NSpace,
  NSpin,
  NTag,
  NTooltip,
  useNotification,
} from 'naive-ui'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const notification = useNotification()

const cartStore = useCartStore()
const { cartItems, cartItemBuyNow } = storeToRefs(cartStore)
const { updateCartQuantity, removeCart } = cartStore

// --- LOGIC CHỌN SẢN PHẨM THANH TOÁN ---
const selectedItemIds = ref<string[]>([])
const isInitialLoaded = ref(false)

// Theo dõi giỏ hàng, load xong data là tự động tick chọn tất cả lần đầu
watch(() => cartItems.value, (newItems) => {
  if (newItems.length > 0 && !isInitialLoaded.value) {
    selectedItemIds.value = newItems.map(item => item.productDetailId)
    isInitialLoaded.value = true
  }
  else if (isInitialLoaded.value) {
    selectedItemIds.value = selectedItemIds.value.filter(id =>
      newItems.some(item => item.productDetailId === id),
    )
  }
}, { immediate: true })

const isSelectAll = computed({
  get: () => cartItems.value.length > 0 && selectedItemIds.value.length === cartItems.value.length,
  set: (val) => {
    if (val) {
      selectedItemIds.value = cartItems.value.map(item => item.productDetailId)
    }
    else {
      selectedItemIds.value = []
    }
  },
})

// --- TÍNH TỔNG SỐ LƯỢNG MÁY (Thay vì đếm số dòng) ---
const totalCartQuantity = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.quantity || 0), 0)
})

const totalSelectedQuantity = computed(() => {
  return cartItems.value
    .filter(item => selectedItemIds.value.includes(item.productDetailId))
    .reduce((total, item) => total + (item.quantity || 0), 0)
})

// --- LOGIC TÍNH TỔNG TIỀN (Chỉ tính những món được tick chọn) ---
const subTotal = computed(() => {
  return cartItems.value
    .filter(item => selectedItemIds.value.includes(item.productDetailId))
    .reduce((total, item) => {
      return total + ((item.percentage ? item.price * (1 - item.percentage / 100) : item.price) * item.quantity)
    }, 0)
})

const MAX_TOTAL_AMOUNT = 200000000 // 200 triệu VNĐ

async function handleUpdateQuantity(item: any, newQuantity: number | null) {
  if (newQuantity === null || newQuantity < 1)
    newQuantity = 1

  if (newQuantity > 5) {
    notification.warning({
      content: 'Chỉ được mua tối đa 5 sản phẩm cho mỗi cấu hình',
      duration: 3000,
      meta: 'Vui lòng giảm số lượng',
    })
    newQuantity = 5
  }

  const unitPrice = item.percentage ? item.price * (1 - item.percentage / 100) : item.price

  // Tính tổng để chặn 200tr
  const currentCartTotal = cartItems.value.reduce((total, cartItem) => {
    const p = cartItem.percentage ? cartItem.price * (1 - cartItem.percentage / 100) : cartItem.price
    return total + (p * cartItem.quantity)
  }, 0)

  const expectedTotal = currentCartTotal - (unitPrice * item.quantity) + (unitPrice * newQuantity)

  if (expectedTotal > MAX_TOTAL_AMOUNT) {
    notification.error({
      content: `Tổng giá trị giỏ hàng tối đa là ${formatCurrency(MAX_TOTAL_AMOUNT)}`,
      duration: 4000,
      meta: 'Vui lòng bỏ bớt sản phẩm',
    })
    return
  }

  try {
    const response = await getQuantityProdudtDetail([item.productDetailId])
    const stock = response.data.length > 0 ? response.data[0].quantity : 0

    if (newQuantity > stock) {
      notification.error({
        content: `Kho chỉ còn ${stock} sản phẩm`,
        duration: 3000,
        meta: 'Vui lòng giảm số lượng',
      })
      newQuantity = stock
    }

    if (newQuantity > 0) {
      await updateCartQuantity(item.productDetailId, newQuantity)
    }
  }
  catch (error) {
    notification.error({ content: 'Không thể cập nhật số lượng lúc này' })
  }
}

function formatCurrency(val: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}

function handleClickCheckout() {
  if (subTotal.value > MAX_TOTAL_AMOUNT) {
    notification.error({
      title: 'Không thể thanh toán',
      content: `Tổng giá trị đơn hàng vượt quá ${formatCurrency(MAX_TOTAL_AMOUNT)}`,
      meta: 'Vui lòng bỏ bớt sản phẩm',
      duration: 5000,
    })
    return
  }

  if (cartItemBuyNow.value) {
    removeCart(cartItemBuyNow.value.productDetailId, { buyNow: true })
  }

  localStorage.setItem('SELECTED_CART_ITEMS', JSON.stringify(selectedItemIds.value))

  router.push('/checkout')
}

function goToDetail(idProductDetail: string) {
  router.push(`/product-detail/${idProductDetail}`)
}
</script>

<template>
  <div class="cart-page">
    <div class="cart-container">
      <!-- Header -->
      <div class="cart-header">
        <div class="header-left">
          <div class="header-icon">
            <NIcon size="28" color="#18a058">
              <CartOutline />
            </NIcon>
          </div>
          <h1 class="header-title">
            Giỏ hàng của bạn
          </h1>
          <NBadge v-if="totalCartQuantity > 0" :value="totalCartQuantity" type="success" />
        </div>
        <div class="header-right">
          <NButton text class="continue-shopping" @click="router.push('/san-pham')">
            <template #icon>
              <NIcon><ChevronForwardOutline style="transform: rotate(180deg)" /></NIcon>
            </template>
            Tiếp tục mua sắm
          </NButton>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="loading-state">
        <NSpin size="large" />
        <p>Đang tải giỏ hàng...</p>
      </div>

      <!-- Empty Cart -->
      <div v-else-if="cartItems.length === 0" class="empty-cart">
        <div class="empty-cart-content">
          <div class="empty-cart-icon">
            <NIcon size="80" color="#e2e8f0">
              <CartOutline />
            </NIcon>
          </div>
          <h2>Giỏ hàng của bạn đang trống</h2>
          <p>Hãy khám phá các sản phẩm công nghệ mới nhất tại TechStore</p>
          <div class="empty-cart-actions">
            <NButton type="primary" size="large" class="shop-now-btn" @click="router.push('/san-pham')">
              <template #icon>
                <NIcon><FlashOutline /></NIcon>
              </template>
              MUA SẮM NGAY
            </NButton>
            <div class="hot-products">
              <span>Sản phẩm hot:</span>
              <div class="hot-tags">
                <NTag size="small" :bordered="false" class="hot-tag">
                  Laptop Gaming
                </NTag>
                <NTag size="small" :bordered="false" class="hot-tag">
                  MacBook
                </NTag>
                <NTag size="small" :bordered="false" class="hot-tag">
                  Phụ kiện
                </NTag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Cart With Items -->
      <div v-else class="cart-content">
        <div class="cart-grid">
          <!-- Left Column - Cart Items -->
          <div class="cart-items-section">
            <!-- Select All Bar -->
            <div class="select-all-bar">
              <NCheckbox v-model:checked="isSelectAll" size="large">
                <span class="select-all-text">
                  Chọn tất cả ({{ totalCartQuantity }} sản phẩm)
                </span>
              </NCheckbox>
              <NPopconfirm
                positive-text="Xóa"
                negative-text="Hủy"
                @positive-click="selectedItemIds.forEach(id => removeCart(id))"
              >
                <template #trigger>
                  <NButton text type="error" size="small" :disabled="selectedItemIds.length === 0">
                    <template #icon>
                      <NIcon><TrashOutline /></NIcon>
                    </template>
                    Xóa đã chọn
                  </NButton>
                </template>
                Xóa {{ selectedItemIds.length }} sản phẩm đã chọn?
              </NPopconfirm>
            </div>

            <!-- Cart Items List -->
            <NCheckboxGroup v-model:value="selectedItemIds" class="cart-items-list">
              <div
                v-for="item in cartItems"
                :key="item.productDetailId"
                class="cart-item"
              >
                <div class="item-checkbox">
                  <NCheckbox :value="item.productDetailId" size="large" />
                </div>

                <div class="item-image" @click="goToDetail(item.productDetailId)">
                  <img
                    :src="item.imageUrl || 'https://via.placeholder.com/100'"
                    :alt="item.name"
                  >
                  <div v-if="item.percentage > 0" class="item-discount-badge">
                    -{{ item.percentage }}%
                  </div>
                </div>

                <div class="item-details">
                  <div class="item-header">
                    <h3 class="item-name" @click="goToDetail(item.productDetailId)">
                      {{ item.name }}
                    </h3>
                  </div>

                  <div class="item-specs">
                    <NTag v-if="item.cpu" size="small" class="spec-tag">
                      CPU: {{ item.cpu }}
                    </NTag>
                    <NTag v-if="item.ram" size="small" class="spec-tag">
                      RAM: {{ item.ram }}
                    </NTag>
                    <NTag v-if="item.hardDrive" size="small" class="spec-tag">
                      SSD: {{ item.hardDrive }}
                    </NTag>
                    <NTag v-if="item.gpu" size="small" class="spec-tag">
                      VGA: {{ item.gpu }}
                    </NTag>
                  </div>

                  <div class="item-footer">
                    <div class="item-price">
                      <template v-if="item.percentage > 0">
                        <span class="old-price">{{ formatCurrency(item.price) }}</span>
                        <span class="current-price">{{ formatCurrency(item.price * (1 - item.percentage / 100)) }}</span>
                      </template>
                      <template v-else>
                        <span class="current-price no-discount">{{ formatCurrency(item.price) }}</span>
                      </template>
                    </div>

                    <div class="quantity-control">
                      <button
                        class="qty-btn"
                        :disabled="item.quantity <= 1"
                        @click.stop="handleUpdateQuantity(item, item.quantity - 1)"
                      >
                        <NIcon size="14">
                          <RemoveOutline />
                        </NIcon>
                      </button>

                      <input
                        type="number"
                        :value="item.quantity"
                        min="1"
                        max="5"
                        class="qty-input"
                        @input="(e) => handleUpdateQuantity(item, parseInt((e.target as HTMLInputElement).value))"
                      >

                      <button
                        class="qty-btn"
                        :disabled="item.quantity >= 5"
                        @click.stop="handleUpdateQuantity(item, item.quantity + 1)"
                      >
                        <NIcon size="14">
                          <AddOutline />
                        </NIcon>
                      </button>
                    </div>

                    <NPopconfirm positive-text="Xóa" negative-text="Hủy" @positive-click="removeCart(item.productDetailId)">
                      <template #trigger>
                        <button class="delete-btn">
                          <NIcon size="18">
                            <TrashOutline />
                          </NIcon>
                        </button>
                      </template>
                      Xóa sản phẩm này khỏi giỏ?
                    </NPopconfirm>
                  </div>
                </div>
              </div>
            </NCheckboxGroup>
          </div>

          <!-- Right Column - Order Summary -->
          <div class="order-summary">
            <div class="summary-card">
              <h3 class="summary-title">
                Thông tin đơn hàng
              </h3>

              <div class="summary-row">
                <span class="summary-label">Tạm tính ({{ totalSelectedQuantity }} SP):</span>
                <span class="summary-value">{{ formatCurrency(subTotal) }}</span>
              </div>

              <div v-if="false" class="summary-row discount-row">
                <span class="summary-label">Giảm giá:</span>
                <span class="summary-value discount">-0₫</span>
              </div>

              <div class="summary-divider" />

              <div class="summary-total">
                <span class="total-label">Tổng thanh toán:</span>
                <span class="total-value">{{ formatCurrency(subTotal) }}</span>
              </div>

              <div class="summary-note">
                (Đã bao gồm VAT)
              </div>

              <NButton
                block
                type="primary"
                size="large"
                class="checkout-btn"
                :disabled="selectedItemIds.length === 0 || subTotal > MAX_TOTAL_AMOUNT"
                @click="handleClickCheckout"
              >
                <template #icon>
                  <NIcon><BagCheckOutline /></NIcon>
                </template>
                TIẾN HÀNH THANH TOÁN
              </NButton>

              <div v-if="subTotal > MAX_TOTAL_AMOUNT" class="error-message">
                <NIcon size="16" color="#ef4444">
                  <AlertCircleOutline />
                </NIcon>
                <span>Tổng tiền vượt quá giới hạn {{ formatCurrency(MAX_TOTAL_AMOUNT) }}</span>
              </div>

              <div class="continue-shopping-link">
                <NButton text @click="router.push('/san-pham')">
                  <template #icon>
                    <NIcon><ChevronForwardOutline style="transform: rotate(180deg)" /></NIcon>
                  </template>
                  Tiếp tục mua sắm
                </NButton>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cart-page {
  background-color: #f8fafc;
  min-height: 100vh;
  padding: 30px 0;
}

.cart-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Cart Header */
.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  background: white;
  padding: 16px 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: #f0fdf4;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-title {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.continue-shopping {
  color: #18a058;
  font-weight: 500;
}

.continue-shopping:hover {
  color: #15803d;
}

/* Loading State */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 0;
  background: white;
  border-radius: 12px;
}

.loading-state p {
  margin-top: 16px;
  color: #64748b;
}

/* Empty Cart */
.empty-cart {
  background: white;
  border-radius: 16px;
  padding: 60px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.empty-cart-content {
  text-align: center;
  max-width: 500px;
  margin: 0 auto;
}

.empty-cart-icon {
  margin-bottom: 24px;
}

.empty-cart h2 {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 8px;
}

.empty-cart p {
  color: #64748b;
  margin-bottom: 24px;
}

.shop-now-btn {
  background: #18a058;
  border: none;
  padding: 12px 32px;
  font-weight: 700;
  margin-bottom: 20px;
}

.shop-now-btn:hover {
  background: #15803d;
}

.hot-products {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 13px;
  color: #64748b;
}

.hot-tags {
  display: flex;
  gap: 8px;
}

.hot-tag {
  background: #f1f5f9;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.2s;
}

.hot-tag:hover {
  background: #18a058;
  color: white;
}

/* Cart Grid */
.cart-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 24px;
}

/* Cart Items Section */
.cart-items-section {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.select-all-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 16px;
}

.select-all-text {
  font-weight: 500;
  color: #1e293b;
}

.cart-items-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  transition: all 0.2s;
  position: relative;
}

.cart-item:hover {
  border-color: #18a058;
  box-shadow: 0 4px 12px rgba(24, 160, 88, 0.1);
}

.item-checkbox {
  padding-top: 4px;
}

.item-image {
  position: relative;
  width: 100px;
  height: 100px;
  flex-shrink: 0;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 8px;
  background: #f8fafc;
  transition: transform 0.2s;
}

.item-image:hover img {
  transform: scale(1.05);
}

.item-discount-badge {
  position: absolute;
  top: 4px;
  left: 4px;
  background: #ef4444;
  color: white;
  font-size: 11px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 4px;
  z-index: 2;
}

.item-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.item-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  margin: 0;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
  padding-right: 12px;
}

.item-name:hover {
  color: #18a058;
}

.favorite-btn {
  padding: 4px;
}

.favorite-btn:hover {
  background: #f1f5f9;
  border-radius: 50%;
}

.item-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.spec-tag {
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 4px;
  border: none;
}

.item-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.item-price {
  display: flex;
  flex-direction: column;
}

.old-price {
  font-size: 12px;
  color: #94a3b8;
  text-decoration: line-through;
}

.current-price {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
}

.current-price.no-discount {
  color: #1e293b;
}

.quantity-control {
  display: flex;
  align-items: center;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
  height: 36px;
}

.qty-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
  border: none;
  cursor: pointer;
  color: #475569;
  transition: all 0.2s;
}

.qty-btn:hover:not(:disabled) {
  background: #18a058;
  color: white;
}

.qty-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.qty-input {
  width: 50px;
  height: 36px;
  border: none;
  border-left: 1px solid #e2e8f0;
  border-right: 1px solid #e2e8f0;
  text-align: center;
  font-weight: 600;
  color: #1e293b;
  -moz-appearance: textfield;
}

.qty-input::-webkit-outer-spin-button,
.qty-input::-webkit-inner-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.qty-input:focus {
  outline: none;
  border-color: #18a058;
}

.delete-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.2s;
}

.delete-btn:hover {
  background: #fee2e2;
  border-color: #ef4444;
  color: #ef4444;
}

/* Service Promise */
.service-promise {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}

.promise-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.promise-icon {
  width: 40px;
  height: 40px;
  background: #f0fdf4;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.promise-text {
  display: flex;
  flex-direction: column;
}

.promise-title {
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.promise-desc {
  font-size: 11px;
  color: #64748b;
}

/* Order Summary */
.order-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.summary-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin-bottom: 20px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}

.summary-label {
  color: #64748b;
}

.summary-value {
  font-weight: 600;
  color: #1e293b;
}

.free-ship {
  color: #18a058;
}

.discount-row .summary-value {
  color: #ef4444;
}

.summary-divider {
  height: 1px;
  background: #e2e8f0;
  margin: 16px 0;
}

.summary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.total-label {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.total-value {
  font-size: 24px;
  font-weight: 700;
  color: #ef4444;
}

.summary-note {
  font-size: 11px;
  color: #94a3b8;
  text-align: right;
  margin-bottom: 20px;
}

.checkout-btn {
  background: #18a058;
  border: none;
  height: 48px;
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 12px;
}

.checkout-btn:hover:not(:disabled) {
  background: #15803d;
}

.checkout-btn:disabled {
  background: #cbd5e1;
  opacity: 0.6;
}

.error-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  background: #fee2e2;
  border-radius: 8px;
  color: #ef4444;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 16px;
}

.continue-shopping-link {
  text-align: center;
}

.continue-shopping-link .n-button {
  color: #18a058;
  font-size: 13px;
}

/* Voucher Card */
.voucher-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.voucher-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-weight: 600;
  color: #1e293b;
}

.voucher-input {
  display: flex;
  gap: 8px;
}

.voucher-input input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  transition: all 0.2s;
}

.voucher-input input:focus {
  outline: none;
  border-color: #18a058;
  box-shadow: 0 0 0 2px rgba(24, 160, 88, 0.1);
}

.apply-btn {
  padding: 0 16px;
  background: #18a058;
  border: none;
  border-radius: 8px;
  color: white;
  font-weight: 600;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.apply-btn:hover {
  background: #15803d;
}

/* Responsive */
@media (max-width: 1024px) {
  .cart-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .cart-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .cart-item {
    flex-direction: column;
  }

  .item-image {
    width: 100%;
    height: 200px;
  }

  .item-footer {
    flex-wrap: wrap;
    gap: 12px;
  }

  .service-promise {
    grid-template-columns: 1fr;
  }

  .promise-item {
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .cart-container {
    padding: 0 12px;
  }

  .select-all-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .item-footer {
    flex-direction: column;
    align-items: flex-start;
  }

  .quantity-control {
    width: 100%;
  }

  .qty-input {
    flex: 1;
  }
}
</style>
