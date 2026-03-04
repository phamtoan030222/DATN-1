<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NCarousel,
  NEmpty,
  NSpin,
} from 'naive-ui'

import NovaIcon from '@/components/common/NovaIcon.vue'

// Assets ảnh banner
import hero1 from '@/assets/images/banner4.jpg'
import hero2 from '@/assets/images/bg4.jpg'
import hero3 from '@/assets/images/banner7.jpg'

// Import API Sản phẩm thường
import { getProductDetails } from '@/service/api/client/product/productDetail.api'
import type { ADProductDetailRequest, ADProductDetailResponse } from '@/service/api/client/product/productDetail.api'

// Import API Giảm giá (Sự kiện)
import { getAllDiscounts, getAppliedProducts } from '@/service/api/client/discount/discountApi'
import type { AppliedProductResponse, DiscountResponse } from '@/service/api/client/discount/discountApi'

const router = useRouter()

// --- STATE QUẢN LÝ DỮ LIỆU CHUNG ---
const loading = ref(false)
const loadingDiscounts = ref(false)
const productDetails = ref<ADProductDetailResponse[]>([])

const heroBanners = ref([
  { id: 1, url: hero1, title: 'Laptop gaming – Hiệu năng mạnh mẽ' },
  { id: 2, url: hero2, title: 'MacBook – Mượt, bền, tối giản' },
  { id: 3, url: hero3, title: 'Workstation – Chuẩn đồ nghề Dev' },
])

const quickBenefits = ref([
  { id: 1, icon: 'icon-park-outline:delivery', title: 'Giao nhanh', desc: 'Toàn quốc 1–3 ngày' },
  { id: 2, icon: 'icon-park-outline:protect', title: 'Bảo hành', desc: 'Minh bạch, rõ ràng' },
  { id: 3, icon: 'icon-park-outline:customer', title: 'Tư vấn', desc: 'Đúng nhu cầu' },
  { id: 4, icon: 'icon-park-outline:credit', title: 'Trả góp', desc: '0% lãi suất' },
])

// --- DANH SÁCH HÃNG ---
const brands = ref([
  { id: 'apple', name: 'Apple', logoUrl: '', icon: 'icon-park-outline:apple' },
  { id: 'samsung', name: 'SAMSUNG', logoUrl: '', textStyling: 'text-blue-700 font-black tracking-tighter' },
  { id: 'xiaomi', name: 'xiaomi', logoUrl: '', textStyling: 'text-orange-500 font-bold text-xl' },
  { id: 'vivo', name: 'vivo', logoUrl: '', textStyling: 'text-blue-500 font-bold text-xl' },
  { id: 'oppo', name: 'oppo', logoUrl: '', textStyling: 'text-green-600 font-bold text-xl' },
  { id: 'asus', name: 'ASUS', logoUrl: '', textStyling: 'text-black font-black text-xl' },
  { id: 'dell', name: 'DELL', logoUrl: '', textStyling: 'text-blue-800 font-black text-xl' },
  { id: 'hp', name: 'HP', logoUrl: '', textStyling: 'text-blue-500 font-bold text-xl' },
  { id: 'lenovo', name: 'Lenovo', logoUrl: '', textStyling: 'text-red-600 font-bold text-xl' },
  { id: 'acer', name: 'Acer', logoUrl: '', textStyling: 'text-green-600 font-bold text-xl' },
])

// --- QUẢN LÝ SỰ KIỆN GIẢM GIÁ ---
const currentTime = ref(new Date().getTime())
let timer: ReturnType<typeof setInterval> | null = null

const currentEvent = ref<{ info: DiscountResponse, products: AppliedProductResponse[] } | null>(null)
const upcomingEvent = ref<{ info: DiscountResponse, products: AppliedProductResponse[] } | null>(null)

// NÂNG CẤP: Hàm tính đếm ngược có tính thêm Ngày (Days)
function getCountdown(targetTime: number) {
  const diff = targetTime - currentTime.value
  if (diff <= 0)
    return { d: 0, h: '00', m: '00', s: '00' }

  const d = Math.floor(diff / (1000 * 60 * 60 * 24))
  const h = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60))
  const m = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60))
  const s = Math.floor((diff % (1000 * 60)) / 1000)

  return {
    d,
    h: h.toString().padStart(2, '0'),
    m: m.toString().padStart(2, '0'),
    s: s.toString().padStart(2, '0'),
  }
}

// Hàm Fetch các đợt Giảm giá và Sản phẩm
async function fetchRealDiscounts() {
  loadingDiscounts.value = true
  try {
    const resDiscounts = await getAllDiscounts({ page: 1, size: 20 })
    const allDiscounts = resDiscounts.items || []
    const now = new Date().getTime()

    const ongoing = allDiscounts.find((d: DiscountResponse) => d.startTime <= now && d.endTime >= now)
    const upcoming = allDiscounts.find((d: DiscountResponse) => d.startTime > now)

    if (ongoing) {
      const resProducts = await getAppliedProducts(ongoing.id, { page: 1, size: 15 })
      currentEvent.value = { info: ongoing, products: resProducts.items || [] }
    }

    if (upcoming) {
      const resProducts = await getAppliedProducts(upcoming.id, { page: 1, size: 15 })
      upcomingEvent.value = { info: upcoming, products: resProducts.items || [] }
    }
  }
  catch (error) {
    console.error('Lỗi khi tải dữ liệu giảm giá:', error)
  }
  finally {
    loadingDiscounts.value = false
  }
}

// --- QUẢN LÝ SẢN PHẨM MỚI NHẤT ---
const newest = computed(() => {
  return [...productDetails.value].slice(0, 20)
})

async function fetchNormalProducts() {
  loading.value = true
  try {
    const params: ADProductDetailRequest = { page: 1, size: 30, minPrice: 0, maxPrice: 1000000000 }
    const res = await getProductDetails(params)
    if (res?.data) {
      const svResponse = res.data
      if (svResponse.data && !Array.isArray(svResponse.data) && (svResponse.data as any).data)
        productDetails.value = (svResponse.data as any).data
      else if (Array.isArray(svResponse.data))
        productDetails.value = svResponse.data
      else if (Array.isArray(svResponse))
        productDetails.value = svResponse
    }
  }
  catch (error) { console.error('Lỗi API sản phẩm:', error) }
  finally { loading.value = false }
}

// --- HÀM XỬ LÝ SỰ KIỆN ĐIỀU HƯỚNG ---
function goProducts(brandId?: string) {
  if (brandId && typeof brandId === 'string') {
    router.push({ name: 'Products', query: { brand: brandId } })
  }
  else {
    router.push({ name: 'Products' })
  }
}

function handleClickProduct(id: string) {
  router.push({ name: 'ProductDetail', params: { id } })
}

function formatCurrency(value: number) {
  if (!value)
    return 'Liên hệ'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function salePrice(price: number, percentage?: number) {
  const pct = percentage ?? 0
  return Math.round(price * (100 - pct) / 100)
}

function handleImageError(e: Event) {
  (e.target as HTMLImageElement).src = 'https://via.placeholder.com/640x640.png?text=Product'
}

onMounted(() => {
  fetchNormalProducts()
  fetchRealDiscounts()

  timer = setInterval(() => {
    currentTime.value = new Date().getTime()
  }, 1000)
})

onUnmounted(() => {
  if (timer)
    clearInterval(timer)
})
</script>

<template>
  <div class="mx-auto max-w-[1300px] px-4 py-8 bg-gray-50/50 min-h-screen">
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-12">
      <div class="lg:col-span-8 rounded-2xl overflow-hidden shadow-sm bg-white">
        <NCarousel show-arrow autoplay draggable dot-type="line" class="h-[250px] md:h-[400px]">
          <div v-for="b in heroBanners" :key="b.id" class="relative w-full h-full cursor-pointer" @click="goProducts()">
            <img :src="b.url" :alt="b.title" class="w-full h-full object-cover">
            <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
            <div class="absolute bottom-0 left-0 p-6 md:p-10">
              <h2 class="text-white text-2xl md:text-4xl font-bold tracking-tight mb-4">
                {{ b.title }}
              </h2>
              <NButton
                type="primary" size="large"
                class="!rounded-xl !font-semibold shadow-lg hover:-translate-y-0.5 transition-transform"
                @click.stop="goProducts()"
              >
                Khám phá ngay
              </NButton>
            </div>
          </div>
        </NCarousel>
      </div>

      <div class="lg:col-span-4 flex flex-col gap-4">
        <div class="bg-white rounded-2xl p-6 shadow-sm h-full flex flex-col justify-between">
          <div>
            <h3 class="text-lg font-bold text-gray-800 mb-4">
              Đặc quyền mua sắm
            </h3>
            <div class="grid grid-cols-2 gap-4">
              <div v-for="b in quickBenefits" :key="b.id" class="flex flex-col gap-2 p-3 rounded-xl bg-gray-50 hover:bg-red-50/50 transition-colors">
                <div class="w-10 h-10 rounded-full bg-red-100/80 flex items-center justify-center text-[#d70018]">
                  <NovaIcon :icon="b.icon" :size="20" />
                </div>
                <div>
                  <div class="font-bold text-gray-900 text-sm">
                    {{ b.title }}
                  </div>
                  <div class="text-xs text-gray-500 mt-0.5">
                    {{ b.desc }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mb-14">
      <div class="flex flex-col items-center justify-center mb-8">
        <h2 class="text-xl font-bold text-gray-800 uppercase tracking-wider relative">
          <span class="text-red-500 mr-1">|</span> CHỌN THEO HÃNG
        </h2>
        <div class="w-12 h-0.5 bg-red-500 mt-2" />
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-5 gap-3 md:gap-4 px-2 md:px-10">
        <button
          v-for="b in brands" :key="b.id"
          class="bg-white rounded-xl h-14 md:h-16 flex items-center justify-center shadow-[0_2px_8px_rgba(0,0,0,0.04)] hover:shadow-md transition-all duration-300 border border-transparent hover:border-gray-200"
          @click="goProducts(b.id)"
        >
          <img v-if="b.logoUrl" :src="b.logoUrl" :alt="b.name" class="max-h-8 object-contain px-4">
          <div v-else class="flex items-center gap-1">
            <NovaIcon v-if="b.icon" :icon="b.icon" :size="24" class="text-black" />
            <span v-else :class="b.textStyling">{{ b.name }}</span>
          </div>
        </button>
      </div>
    </div>

    <div v-if="currentEvent" class="mb-12">
      <div class="bg-gradient-to-r from-[#ef4444] to-[#f97316] rounded-2xl p-4 md:p-6 shadow-lg relative overflow-hidden">
        <div class="flex flex-col md:flex-row items-center justify-between mb-6 gap-4 border-b border-white/20 pb-4">
          <h2 class="text-2xl font-bold text-white flex items-center gap-2 z-10">
            <span class="animate-pulse">🔥</span> ĐANG DIỄN RA: {{ currentEvent.info.discountName }}
          </h2>

          <div class="flex items-center gap-2 z-10 bg-white/10 px-4 py-2 rounded-lg backdrop-blur-sm">
            <span class="text-white text-sm font-medium mr-2">Kết thúc sau:</span>
            <div class="flex items-center gap-1.5 font-bold">
              <span v-if="getCountdown(currentEvent.info.endTime).d > 0" class="text-white mr-1">{{ getCountdown(currentEvent.info.endTime).d }} ngày</span>
              <span class="bg-white text-red-600 px-2.5 py-1.5 rounded shadow-sm min-w-[36px] text-center">{{ getCountdown(currentEvent.info.endTime).h }}</span>
              <span class="text-white">:</span>
              <span class="bg-white text-red-600 px-2.5 py-1.5 rounded shadow-sm min-w-[36px] text-center">{{ getCountdown(currentEvent.info.endTime).m }}</span>
              <span class="text-white">:</span>
              <span class="bg-white text-red-600 px-2.5 py-1.5 rounded shadow-sm min-w-[36px] text-center">{{ getCountdown(currentEvent.info.endTime).s }}</span>
            </div>
          </div>
        </div>

        <div v-if="loadingDiscounts" class="py-10 flex justify-center">
          <NSpin size="large" stroke="#ffffff" />
        </div>
        <div v-else-if="currentEvent.products.length === 0" class="py-10 bg-white/10 rounded-xl">
          <p class="text-center text-white font-medium">
            Sản phẩm đang được cập nhật
          </p>
        </div>

        <div v-else class="flex gap-4 overflow-x-auto pb-4 snap-x snap-mandatory hide-scrollbar -mx-4 px-4 md:mx-0 md:px-0 z-10 relative">
          <div
            v-for="item in currentEvent.products" :key="item.id"
            class="snap-start shrink-0 w-[240px] md:w-[260px] bg-white rounded-xl p-4 shadow-sm hover:shadow-lg transition-all duration-300 cursor-pointer relative group flex flex-col"
            @click="handleClickProduct(item.productDetailCode || item.id)"
          >
            <div class="absolute top-0 left-0 z-10 flex flex-col gap-1 p-2">
              <span v-if="item.percentageDiscount > 0" class="bg-red-600 text-white text-[11px] font-bold px-2 py-1 rounded shadow-sm flex items-center gap-1">
                GIẢM {{ item.percentageDiscount }}%
              </span>
            </div>

            <div class="h-[180px] mt-6 mb-4 flex items-center justify-center group-hover:-translate-y-2 transition-transform duration-500">
              <img :src="item.image" :alt="item.productName" class="max-w-full max-h-[160px] object-contain" @error="handleImageError">
            </div>

            <div class="flex flex-col gap-2 mt-auto">
              <h3 class="text-sm font-bold text-gray-800 line-clamp-2 leading-tight group-hover:text-[#d70018] transition-colors">
                {{ item.productName }}
              </h3>
              <div class="mt-2 flex flex-col">
                <span class="text-red-600 text-lg font-black">{{ formatCurrency(salePrice(item.price, item.percentageDiscount)) }}</span>
                <span class="text-gray-400 text-xs line-through font-medium">{{ formatCurrency(item.price) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="upcomingEvent" class="mb-12 opacity-90">
      <div class="bg-gradient-to-r from-[#3b82f6] to-[#8b5cf6] rounded-2xl p-4 md:p-6 shadow-md relative overflow-hidden">
        <div class="flex flex-col md:flex-row items-center justify-between mb-6 gap-4 border-b border-white/20 pb-4">
          <h2 class="text-2xl font-bold text-white flex items-center gap-2 z-10">
            ⏳ SẮP DIỄN RA: {{ upcomingEvent.info.discountName }}
          </h2>

          <div class="flex items-center gap-2 z-10 bg-white/10 px-4 py-2 rounded-lg backdrop-blur-sm">
            <span class="text-white text-sm font-medium mr-2">Bắt đầu sau:</span>
            <div class="flex items-center gap-1.5 font-bold">
              <span v-if="getCountdown(upcomingEvent.info.startTime).d > 0" class="text-white mr-1">{{ getCountdown(upcomingEvent.info.startTime).d }} ngày</span>
              <span class="bg-white/90 text-blue-700 px-2.5 py-1.5 rounded shadow-sm min-w-[36px] text-center">{{ getCountdown(upcomingEvent.info.startTime).h }}</span>
              <span class="text-white">:</span>
              <span class="bg-white/90 text-blue-700 px-2.5 py-1.5 rounded shadow-sm min-w-[36px] text-center">{{ getCountdown(upcomingEvent.info.startTime).m }}</span>
              <span class="text-white">:</span>
              <span class="bg-white/90 text-blue-700 px-2.5 py-1.5 rounded shadow-sm min-w-[36px] text-center">{{ getCountdown(upcomingEvent.info.startTime).s }}</span>
            </div>
          </div>
        </div>

        <div v-if="loadingDiscounts" class="py-10 flex justify-center">
          <NSpin size="large" stroke="#ffffff" />
        </div>

        <div v-else class="flex gap-4 overflow-x-auto pb-4 snap-x snap-mandatory hide-scrollbar -mx-4 px-4 md:mx-0 md:px-0 z-10 relative">
          <div
            v-for="item in upcomingEvent.products" :key="item.id"
            class="snap-start shrink-0 w-[240px] md:w-[260px] bg-white/95 rounded-xl p-4 shadow-sm border border-blue-100 relative flex flex-col grayscale-[20%]"
          >
            <div class="absolute inset-0 bg-white/40 z-20 rounded-xl flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity">
              <span class="bg-blue-600 text-white font-bold px-4 py-2 rounded-lg shadow-lg">Chờ săn sale</span>
            </div>

            <div class="h-[180px] mt-2 mb-4 flex items-center justify-center">
              <img :src="item.image" :alt="item.productName" class="max-w-full max-h-[160px] object-contain" @error="handleImageError">
            </div>

            <div class="flex flex-col gap-2 mt-auto">
              <h3 class="text-sm font-bold text-gray-700 line-clamp-2 leading-tight">
                {{ item.productName }}
              </h3>
              <div class="mt-2 flex flex-col">
                <span class="text-blue-700 text-lg font-black">??? đ</span>
                <span class="text-gray-400 text-xs font-medium">Giá gốc: {{ formatCurrency(item.price) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div>
      <div class="flex items-end justify-between mb-6">
        <div>
          <h2 class="text-2xl font-bold text-gray-900">
            Sản phẩm mới nhất
          </h2>
          <p class="text-gray-500 text-sm mt-1">
            Cập nhật xu hướng công nghệ
          </p>
        </div>
      </div>

      <div v-if="loading" class="py-10 flex justify-center">
        <NSpin size="large" />
      </div>
      <div v-else-if="newest.length === 0" class="py-10 bg-white rounded-2xl shadow-sm">
        <NEmpty description="Rất tiếc, hiện tại chưa có sản phẩm" />
      </div>

      <div v-else class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4 md:gap-5">
        <div
          v-for="item in newest" :key="item.id"
          class="bg-white rounded-2xl p-4 shadow-sm hover:shadow-lg transition-all duration-300 hover:-translate-y-1 cursor-pointer border border-gray-100 relative group flex flex-col h-full"
          @click="handleClickProduct(item.id)"
        >
          <div v-if="(item.percentage ?? 0) > 0" class="absolute top-3 right-3 z-10 bg-red-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-sm">
            -{{ item.percentage }}%
          </div>

          <div class="h-[160px] md:h-[180px] mb-4 p-2 bg-white flex items-center justify-center group-hover:scale-105 transition-transform duration-500">
            <img :src="item.urlImage" :alt="item.name" class="max-w-full max-h-full object-contain" @error="handleImageError">
          </div>

          <div class="flex flex-col flex-1 justify-between">
            <div>
              <h3 class="text-sm font-bold text-gray-800 line-clamp-2 min-h-[40px] leading-tight group-hover:text-[#d70018] transition-colors">
                {{ item.productName || item.name }} {{ item.cpu }} {{ item.ram }} {{ item.gpu }}
              </h3>
              <div class="flex flex-wrap gap-1.5 mt-2">
                <span v-if="item.ram" class="bg-gray-100 text-gray-600 text-[11px] font-semibold px-2 py-1 rounded-md">{{ item.ram }}</span>
                <span v-if="item.hardDrive" class="bg-gray-100 text-gray-600 text-[11px] font-semibold px-2 py-1 rounded-md">{{ item.hardDrive }}</span>
              </div>
            </div>

            <div class="mt-4 flex flex-col">
              <span class="text-red-600 text-[17px] font-black">{{ formatCurrency(salePrice(item.price, item.percentage)) }}</span>
              <span v-if="(item.percentage ?? 0) > 0" class="text-gray-400 text-xs line-through font-medium mt-0.5">
                {{ formatCurrency(item.price) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
</style>
