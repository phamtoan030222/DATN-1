<script setup lang="ts">
import { computed, onMounted, ref } from 'vue' // ← thêm computed, onMounted, ref
import { useRoute, useRouter } from 'vue-router' // ← thêm useRoute
import { NButton, NResult, NSpin } from 'naive-ui' // ← thêm NSpin
import { useCartStore } from '@/store/app/cart'
import { storeToRefs } from 'pinia'

const router = useRouter()
const route = useRoute()

const { cartItems, cartItemBuyNow } = storeToRefs(useCartStore())
const { removeCart } = useCartStore()

const maHoaDon = computed(() => route.query['ma-hoa-don'] as string || '')
const status = ref<'success' | 'cancelled'>('success')

onMounted(async () => {
  const payment = route.query.payment as string

  if (payment === 'cancelled') {
    // Không nên đến đây vì BE đã redirect về /checkout
    // Giữ lại để an toàn
    router.replace('/checkout?payment=cancelled')
    return
  }

  if (maHoaDon.value) {
    status.value = 'success'
    // Xóa cart sau khi TT thành công qua cổng (VNPAY/Momo/ZaloPay)
    // COD đã xóa trong Checkout.vue rồi, gọi lại cũng không sao (no-op)
    await cleanupCart()
  }
  else {
    router.replace('/')
  }
})

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

function goHome() { router.push('/') }

function handleClickTracking() {
  router.push({ name: 'OrderTracking', query: { q: maHoaDon.value } })
}
</script>

<template>
  <div class="result-container">
    <div class="content-box">
      <NResult
        status="success"
        title="Đặt Hàng Thành Công!"
        description="Cảm ơn bạn đã tin tưởng và mua sắm."
        size="huge"
      >
        <template #default>
          <div class="text-left">
            <p>Mã hóa đơn của bạn: <strong>{{ maHoaDon }}</strong></p>
            <p>Thông tin đơn hàng đã được gửi đến email của bạn.</p>
            <p>Vui lòng giữ mã hóa đơn để theo dõi trạng thái đơn hàng.</p>
          </div>
        </template>
        <template #footer>
          <div class="btn-group">
            <NButton size="large" @click="goHome">
              Về Trang Chủ
            </NButton>
            <NButton type="primary" color="#00D752" size="large" @click="handleClickTracking">
              Theo dõi đơn hàng
            </NButton>
          </div>
        </template>
      </NResult>
    </div>
  </div>
</template>

<style scoped>
.result-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
  /* background-color: #f9fafb; */
  padding: 20px;
}

.content-box {
  background: white;
  padding: 50px;
  border-radius: 16px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
  text-align: center;
  max-width: 600px;
  width: 100%;
}

.btn-group {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}
</style>
