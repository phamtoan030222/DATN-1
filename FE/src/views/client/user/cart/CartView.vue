<script setup lang="ts">
import { getQuantityProdudtDetail } from '@/service/api/client/banhang.api'
import { useCartStore } from '@/store/app/cart'
import { storeToRefs } from 'pinia'
import {
  AddOutline,
  ArrowForward,
  BagCheckOutline,
  RemoveOutline,
  TrashOutline,
} from '@vicons/ionicons5'
import {
  NButton,
  NCheckbox,
  NCheckboxGroup,
  NEmpty,
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
    notification.warning({ content: 'Tối đa 5 sản phẩm/đơn', duration: 3000 })
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
      content: `Không thể thêm! Tổng giá trị giỏ hàng tối đa là ${formatCurrency(MAX_TOTAL_AMOUNT)}`,
      duration: 4000,
    })
    return
  }

  try {
    const response = await getQuantityProdudtDetail([item.productDetailId])
    const stock = response.data.length > 0 ? response.data[0].quantity : 0

    if (newQuantity > stock) {
      notification.error({ content: `Rất tiếc, kho chỉ còn ${stock} sản phẩm`, duration: 3000 })
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
      content: `Tổng giá trị đơn hàng vượt quá ${formatCurrency(MAX_TOTAL_AMOUNT)}. Vui lòng bỏ bớt sản phẩm!`,
      duration: 5000,
    })
    return // Chặn đứng tại đây, không cho qua trang checkout
  }
  // ----------------------------------------

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
  <div class="container mx-auto px-4 py-8 max-w-4xl min-h-[60vh]">
    <h1 class="text-2xl font-bold mb-6 flex items-center gap-2 text-gray-800">
      <NIcon color="#00AA00">
        <BagCheckOutline />
      </NIcon> Giỏ hàng của bạn
    </h1>

    <div v-if="loading" class="flex justify-center py-20">
      <NSpin size="large" />
    </div>

    <div
      v-else-if="cartItems.length === 0"
      class="text-center py-16 bg-white rounded-lg shadow-sm border border-gray-100"
    >
      <NEmpty description="Giỏ hàng đang trống" size="large">
        <template #extra>
          <NButton type="primary" color="#00AA00" @click="router.push('/home')">
            TIẾP TỤC MUA SẮM
          </NButton>
        </template>
      </NEmpty>
    </div>

    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <div class="lg:col-span-2">
        <div class="flex justify-between items-center mb-4 bg-white p-4 rounded-xl border border-gray-100 shadow-sm">
          <NCheckbox v-model:checked="isSelectAll" size="large">
            <span class="font-semibold text-gray-700 ml-1">
              Chọn tất cả ({{ totalCartQuantity }} sản phẩm)
            </span>
          </NCheckbox>
        </div>

        <NCheckboxGroup v-model:value="selectedItemIds" class="w-full space-y-4">
          <div
            v-for="item in cartItems" :key="item.productDetailId"
            class="flex gap-4 p-4 bg-white rounded-xl border border-gray-100 shadow-sm relative group hover:shadow-md transition-all"
          >
            <div class="flex items-center pr-3">
              <NCheckbox :value="item.productDetailId" size="large" />
            </div>

            <div class="relative w-28 h-28 shrink-0">
              <img
                :src="item.imageUrl || 'https://via.placeholder.com/100'"
                class="w-full h-full object-contain border rounded-lg p-2 bg-gray-50 cursor-pointer hover:border-red-300 transition-colors"
                @click="goToDetail(item.productDetailId)"
              >
              <div
                v-if="item.percentage > 0"
                class="absolute top-0 left-0 bg-red-600 text-white text-[11px] font-bold px-2 py-0.5 rounded-tl-lg rounded-br-lg shadow-sm z-10"
              >
                -{{ item.percentage }}%
              </div>
            </div>

            <div class="flex-1 flex flex-col justify-between py-1">
              <div>
                <h3
                  class="font-bold text-gray-800 text-lg cursor-pointer hover:text-red-600 transition-colors line-clamp-2"
                  @click="goToDetail(item.productDetailId)"
                >
                  {{ item.name }}
                </h3>
                <div class="text-xs text-gray-500 mt-2 flex flex-wrap gap-2">
                  <NTag v-if="item.cpu" size="small" :bordered="false" class="bg-purple-200">
                    {{ item.cpu }}
                  </NTag>
                  <NTag v-if="item.gpu" size="small" :bordered="false" class="bg-blue-200">
                    {{ item.gpu }}
                  </NTag>
                  <NTag v-if="item.ram" size="small" :bordered="false" class="bg-green-200">
                    {{ item.ram }}
                  </NTag>
                  <NTag v-if="item.hardDrive" size="small" :bordered="false" class="bg-red-200">
                    {{ item.hardDrive }}
                  </NTag>
                </div>
              </div>

              <div class="flex justify-between items-end mt-4">
                <div>
                  <template v-if="item.percentage > 0">
                    <div class="text-gray-400 text-xs line-through">
                      {{ formatCurrency(item.price) }}
                    </div>
                    <div class="text-red-600 font-bold text-xl">
                      {{ formatCurrency(item.price - (item.price * item.percentage / 100)) }}
                    </div>
                  </template>
                  <template v-else>
                    <div class="text-gray-800 font-bold text-xl">
                      {{ formatCurrency(item.price) }}
                    </div>
                  </template>
                </div>

                <div class="flex items-center gap-3">
                  <div class="flex items-center border border-gray-300 rounded-md overflow-hidden h-9 shadow-sm">
                    <button
                      class="w-9 h-full flex items-center justify-center bg-gray-50 text-gray-600 hover:bg-red-50 hover:text-red-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                      :disabled="item.quantity <= 1"
                      @click.stop="handleUpdateQuantity(item, item.quantity - 1)"
                    >
                      <NIcon size="16">
                        <RemoveOutline />
                      </NIcon>
                    </button>

                    <n-input-number
                      :value="item.quantity"
                      :min="1"
                      :max="5"
                      class="w-12 custom-qty-input border-x border-gray-300"
                      :show-button="false"
                      :bordered="false"
                      @update:value="(val) => handleUpdateQuantity(item, val)"
                    />

                    <button
                      class="w-9 h-full flex items-center justify-center bg-gray-50 text-gray-600 hover:bg-green-50 hover:text-green-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                      :disabled="item.quantity >= 5"
                      @click.stop="handleUpdateQuantity(item, item.quantity + 1)"
                    >
                      <NIcon size="16">
                        <AddOutline />
                      </NIcon>
                    </button>
                  </div>
                </div>
              </div>
            </div>

            <NPopconfirm positive-text="Xóa" negative-text="Hủy" @positive-click="removeCart(item.productDetailId)">
              <template #trigger>
                <button
                  class="absolute top-3 right-3  hover:text-red-400 p-1 transition-colors rounded-full bg-white text-gray-400"
                >
                  <NIcon size="20">
                    <TrashOutline />
                  </NIcon>
                </button>
              </template>
              Xóa sản phẩm này khỏi giỏ?
            </NPopconfirm>
          </div>
        </NCheckboxGroup>
      </div>

      <div class="lg:col-span-1">
        <div class="bg-white p-6 rounded-xl border shadow-sm sticky top-4">
          <div class="flex justify-between mb-4 text-gray-600">
            <span>Tạm tính (<span class="font-bold text-red-600">{{ totalSelectedQuantity }}</span> SP):</span>
            <span class="font-bold text-gray-800">{{ formatCurrency(subTotal) }}</span>
          </div>
          <div class="border-t pt-4">
            <div class="flex justify-between items-center mb-6">
              <span class="font-bold text-lg text-gray-800">Tổng tiền:</span>
              <span class="font-bold text-xl text-red-600">{{ formatCurrency(subTotal) }}</span>
            </div>
            <NButton
              block type="success" size="large" class="font-bold h-12 shadow-lg"
              :disabled="selectedItemIds.length === 0 || subTotal > MAX_TOTAL_AMOUNT"
              @click="handleClickCheckout"
            >
              Thanh toán <NIcon class="ml-2">
                <ArrowForward />
              </NIcon>
            </NButton>
            <NButton block class="mt-3" @click="router.push('/san-pham')">
              Chọn thêm sản phẩm khác
            </NButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Tùy chỉnh cho ô input số lượng mới để nội dung nằm chính giữa, nền trong suốt */
:deep(.custom-qty-input .n-input__input-el) {
  text-align: center !important;
  font-weight: 600;
  color: #1f2937; /* gray-800 */
}
:deep(.custom-qty-input .n-input) {
  background-color: transparent !important;
}
:deep(.custom-qty-input.n-input--focus) {
  background-color: transparent !important;
}
</style>
