<script setup lang="ts">
import { getQuantityProdudtDetail } from '@/service/api/client/banhang.api'
import { useCartStore } from '@/store/app/cart'
import { storeToRefs } from 'pinia'
import {
  AddOutline,
  AlertCircleOutline,
  BagCheckOutline,
  CartOutline,
  ChevronForwardOutline,
  FlashOutline,
  GiftOutline,
  RemoveOutline,
  RocketOutline,
  ShieldCheckmarkOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import {
  NBadge,
  NButton,
  NCheckbox,
  NCheckboxGroup,
  NIcon,
  NPopconfirm,
  NSpin,
  NTag,
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

// --- TÍNH TỔNG SỐ LƯỢNG MÁY ---
const totalCartQuantity = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.quantity || 0), 0)
})

const totalSelectedQuantity = computed(() => {
  return cartItems.value
    .filter(item => selectedItemIds.value.includes(item.productDetailId))
    .reduce((total, item) => total + (item.quantity || 0), 0)
})

// --- LOGIC TÍNH TỔNG TIỀN ---

// Giá gốc (chưa trừ phần trăm giảm giá)
const originalTotal = computed(() => {
  return cartItems.value
    .filter(item => selectedItemIds.value.includes(item.productDetailId))
    .reduce((total, item) => total + (item.price * (item.quantity || 0)), 0)
})

// Giá trị thực tế cần thanh toán (sau giảm giá)
const subTotal = computed(() => {
  return cartItems.value
    .filter(item => selectedItemIds.value.includes(item.productDetailId))
    .reduce((total, item) => {
      return total + ((item.percentage ? item.price * (1 - item.percentage / 100) : item.price) * item.quantity)
    }, 0)
})

// Số tiền tiết kiệm được
const savedAmount = computed(() => {
  return originalTotal.value - subTotal.value
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

      <div v-if="loading" class="loading-state">
        <NSpin size="large" />
        <p>Đang tải giỏ hàng...</p>
      </div>

      <div v-else-if="cartItems.length === 0" class="empty-cart">
        <div class="empty-cart-content">
          <div class="empty-cart-icon">
            <NIcon size="80" color="#e2e8f0">
              <CartOutline />
            </NIcon>
          </div>
          <h2>Giỏ hàng của bạn đang trống</h2>
          <p>Hãy khám phá các sản phẩm công nghệ mới nhất tại MY LAPTOP</p>
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
                  Văn phòng
                </NTag>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="cart-content">
        <div class="cart-grid">
          <div class="cart-items-section">
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

                <div class="item-info-group">
                  <div class="item-main-details">
                    <h3 class="item-name" @click="goToDetail(item.productDetailId)">
                      {{ item.name }}
                    </h3>

                    <div class="item-price">
                      <template v-if="item.percentage > 0">
                        <span class="old-price">{{ formatCurrency(item.price) }}</span>
                        <span class="current-price">{{ formatCurrency(item.price * (1 - item.percentage / 100)) }}</span>
                      </template>
                      <template v-else>
                        <span class="current-price no-discount">{{ formatCurrency(item.price) }}</span>
                      </template>
                    </div>
                  </div>

                  <div class="item-actions">
                    <NPopconfirm positive-text="Xóa" negative-text="Hủy" @positive-click="removeCart(item.productDetailId)">
                      <template #trigger>
                        <button class="delete-action-btn">
                          <NIcon size="20">
                            <TrashOutline />
                          </NIcon>
                        </button>
                      </template>
                      Xóa sản phẩm này khỏi giỏ?
                    </NPopconfirm>

                    <div class="quantity-control">
                      <button
                        class="qty-btn btn-minus"
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
                        class="qty-btn btn-plus"
                        :disabled="item.quantity >= 5"
                        @click.stop="handleUpdateQuantity(item, item.quantity + 1)"
                      >
                        <NIcon size="14">
                          <AddOutline />
                        </NIcon>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </NCheckboxGroup>

          </div>

          <div class="order-summary">
            <div class="summary-card">
              <h3 class="summary-title">
                Thông tin đơn hàng
              </h3>

              <div class="summary-row">
                <span class="summary-label">Tạm tính ({{ totalSelectedQuantity }} SP):</span>
                <span class="summary-value">{{ formatCurrency(originalTotal) }}</span>
              </div>

              <div v-if="savedAmount > 0" class="summary-row save-row">
                <span class="summary-label">Tiết kiệm:</span>
                <span class="summary-value save-value">-{{ formatCurrency(savedAmount) }}</span>
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

.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon { width: 48px; height: 48px; background: #f0fdf4; border-radius: 12px; display: flex; align-items: center; justify-content: center; }
.header-title { font-size: 20px; font-weight: 700; color: #1e293b; margin: 0; }
.continue-shopping { color: #18a058; font-weight: 500; }
.continue-shopping:hover { color: #15803d; }

/* Loading & Empty State */
.loading-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 80px 0; background: white; border-radius: 12px; }
.loading-state p { margin-top: 16px; color: #64748b; }
.empty-cart { background: white; border-radius: 16px; padding: 60px 20px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); }
.empty-cart-content { text-align: center; max-width: 500px; margin: 0 auto; }
.empty-cart-icon { margin-bottom: 24px; }
.empty-cart h2 { font-size: 20px; font-weight: 700; color: #1e293b; margin-bottom: 8px; }
.empty-cart p { color: #64748b; margin-bottom: 24px; }
.shop-now-btn { background: #18a058; border: none; padding: 12px 32px; font-weight: 700; margin-bottom: 20px; }
.shop-now-btn:hover { background: #15803d; }
.hot-products { display: flex; align-items: center; justify-content: center; gap: 12px; font-size: 13px; color: #64748b; }
.hot-tags { display: flex; gap: 8px; }
.hot-tag { background: #f1f5f9; color: #1e293b; cursor: pointer; transition: all 0.2s; }
.hot-tag:hover { background: #18a058; color: white; }

/* Cart Grid */
.cart-grid { display: grid; grid-template-columns: 1fr 360px; gap: 24px; }
.cart-items-section { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); }
.select-all-bar { display: flex; justify-content: space-between; align-items: center; padding-bottom: 16px; border-bottom: 1px solid #e2e8f0; margin-bottom: 16px; }
.select-all-text { font-weight: 500; color: #1e293b; }
.cart-items-list { display: flex; flex-direction: column; gap: 16px; }

/* CART ITEM */
.cart-item {
  display: flex; gap: 16px; padding: 16px; border: 1px solid #e2e8f0; border-radius: 12px;
  transition: all 0.2s; position: relative;
}
.cart-item:hover { border-color: #18a058; box-shadow: 0 4px 12px rgba(24, 160, 88, 0.1); }
.item-checkbox { padding-top: 4px; }
.item-image {
  position: relative; width: 100px; height: 100px; flex-shrink: 0; cursor: pointer;
  border-radius: 8px; overflow: hidden; border: 1px solid #e2e8f0;
}
.item-image img { width: 100%; height: 100%; object-fit: contain; padding: 8px; background: #f8fafc; transition: transform 0.2s; }
.item-image:hover img { transform: scale(1.05); }
.item-discount-badge { position: absolute; top: 4px; left: 4px; background: #ef4444; color: white; font-size: 11px; font-weight: 700; padding: 2px 6px; border-radius: 4px; z-index: 2; }

/* Item Layout Mới */
.item-info-group {
  flex: 1;
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.item-main-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.item-name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  margin: 0;
  line-height: 1.4;
  /* Giới hạn tên thành 2 dòng */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  max-width: 95%; /* Cho ngắn lại 1 chút */
}
.item-name:hover { color: #18a058; }

.item-specs { display: flex; flex-wrap: wrap; gap: 6px; }
.spec-tag { background: #f1f5f9; color: #475569; font-size: 11px; padding: 2px 8px; border-radius: 4px; border: none; }

.item-price {
  display: flex;
  flex-direction: column; /* Thêm thuộc tính này để xếp dọc */
  align-items: flex-start; /* Căn lề trái */
  gap: 2px; /* Thu hẹp khoảng cách giữa 2 mức giá cho gọn */
  margin-top: 4px;
}
.current-price { font-size: 18px; font-weight: 700; color: #ef4444; line-height: 1; }
.current-price.no-discount { color: #1e293b; }
.old-price { font-size: 13px; color: #94a3b8; text-decoration: line-through; line-height: 1; }

/* Khu vực nút xóa & số lượng */
.item-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  width: 110px;
}

.delete-action-btn {
  background: transparent;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  transition: color 0.2s;
  padding: 4px;
  margin-bottom: 12px;
}
.delete-action-btn:hover { color: #ef4444; }

.quantity-control {
  display: flex; align-items: center; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; height: 32px; width: 100%;
}
.qty-btn { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: #f8fafc; border: none; cursor: pointer; color: #475569; transition: all 0.2s; }
.qty-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* CSS Hiệu ứng mới cho nút Trừ và Cộng */
.qty-btn.btn-minus:hover:not(:disabled) {
  background: #fee2e2;
  color: #ef4444;
}

.qty-btn.btn-plus:hover:not(:disabled) {
  background: #dcfce7;
  color: #16a34a;
}

.qty-input { flex: 1; height: 32px; border: none; border-left: 1px solid #e2e8f0; border-right: 1px solid #e2e8f0; text-align: center; font-weight: 600; color: #1e293b; font-size: 13px; -moz-appearance: textfield; width: 100%; }
.qty-input::-webkit-outer-spin-button, .qty-input::-webkit-inner-spin-button { -webkit-appearance: none; margin: 0; }
.qty-input:focus { outline: none; border-color: #18a058; }

/* Service Promise */
.service-promise {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e2e8f0;
}
.promise-item { display: flex; align-items: center; gap: 12px; }
.promise-icon { width: 40px; height: 40px; background: #f0fdf4; border-radius: 50%; display: flex; align-items: center; justify-content: center; }
.promise-text { display: flex; flex-direction: column; }
.promise-title { font-size: 13px; font-weight: 600; color: #1e293b; }
.promise-desc { font-size: 11px; color: #64748b; }

/* Order Summary */
.order-summary { display: flex; flex-direction: column; gap: 16px; }
.summary-card { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04); position: sticky; top: 20px; }
.summary-title { font-size: 18px; font-weight: 700; color: #1e293b; margin-bottom: 20px; }
.summary-row { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 14px; }
.summary-label { color: #64748b; }
.summary-value { font-weight: 600; color: #1e293b; }

/* Tiết kiệm màu xanh dương */
.save-row .save-value {
  color: #2563eb;
  font-weight: 700;
}

.summary-divider { height: 1px; background: #e2e8f0; margin: 16px 0; }
.summary-total { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.total-label { font-size: 16px; font-weight: 600; color: #1e293b; }
.total-value { font-size: 24px; font-weight: 700; color: #ef4444; }
.summary-note { font-size: 11px; color: #94a3b8; text-align: right; margin-bottom: 20px; }

.checkout-btn { background: #18a058; border: none; height: 48px; font-weight: 700; font-size: 16px; margin-bottom: 12px; }
.checkout-btn:hover:not(:disabled) { background: #15803d; }
.checkout-btn:disabled { background: #cbd5e1; opacity: 0.6; }
.error-message { display: flex; align-items: center; justify-content: center; gap: 8px; padding: 12px; background: #fee2e2; border-radius: 8px; color: #ef4444; font-size: 13px; font-weight: 500; margin-bottom: 16px; }
.continue-shopping-link { text-align: center; }
.continue-shopping-link .n-button { color: #18a058; font-size: 13px; }

/* Responsive */
@media (max-width: 1024px) {
  .cart-grid { grid-template-columns: 1fr; }
  .summary-card { position: relative; top: 0; }
}

@media (max-width: 768px) {
  .cart-header { flex-direction: column; align-items: flex-start; gap: 12px; }
  .cart-item { flex-direction: column; }
  .item-image { width: 100%; height: 200px; }

  .item-info-group { flex-direction: column; }
  .item-actions { width: 100%; flex-direction: row-reverse; align-items: center; margin-top: 12px; }
  .delete-action-btn { margin-bottom: 0; }
  .quantity-control { width: 120px; }

  .service-promise { grid-template-columns: 1fr; }
  .promise-item { justify-content: center; }
}

@media (max-width: 480px) {
  .cart-container { padding: 0 12px; }
  .select-all-bar { flex-direction: column; align-items: flex-start; gap: 12px; }
  .quantity-control { width: 100px; }
}
</style>
