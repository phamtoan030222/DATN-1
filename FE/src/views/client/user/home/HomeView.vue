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
import { getNearestUpcomingDiscounts, getOngoingDiscounts, getProductDetails } from '@/service/api/client/product/productDetail.api'
import type {
  ADProductDetailRequest,
  ADProductDetailResponse,
  DiscountProductRequest,
  DiscountProductResponse,
} from '@/service/api/client/product/productDetail.api'

import { getBrands } from '@/service/api/client/product/product.api'

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
  { id: 4, icon: 'icon-park-outline:paper-money', title: 'Trả góp', desc: '0% lãi suất' },
])

// --- QUẢN LÝ DANH SÁCH HÃNG (BRAND) ĐỘNG ---
interface BrandDisplay {
  id: string
  name: string
  logoUrl?: string
  icon?: string
  textStyling?: string
}

const brands = ref<BrandDisplay[]>([])

const brandStyleMap: Record<string, any> = {
  apple: { icon: 'icon-park-outline:apple' },
  samsung: { textStyling: 'text-blue-700 font-black tracking-tighter' },
  xiaomi: { textStyling: 'text-orange-500 font-bold text-xl' },
  vivo: { textStyling: 'text-blue-500 font-bold text-xl' },
  oppo: { textStyling: 'text-green-600 font-bold text-xl' },
  asus: { textStyling: 'text-black font-black text-xl' },
  dell: { textStyling: 'text-blue-800 font-black text-xl' },
  hp: { textStyling: 'text-blue-500 font-bold text-xl' },
  lenovo: { textStyling: 'text-green-600 font-bold text-xl' },
  acer: { textStyling: 'text-green-600 font-bold text-xl' },
}

async function fetchBrands() {
  try {
    const res: any = await getBrands()
    const apiBrands = res?.data || []

    brands.value = apiBrands.map((b: any) => {
      const brandId = b.value || b.id || ''
      const brandName = b.label || b.name || ''

      const key = brandName.toLowerCase().trim()
      const style = brandStyleMap[key] || { textStyling: 'text-gray-800 font-bold text-xl' }

      return {
        id: brandId,
        name: brandName,
        ...style,
      }
    })
  }
  catch (error) {
    console.error('Lỗi khi tải danh sách hãng:', error)
  }
}

// --- XỬ LÝ SỰ KIỆN CUỘN CHO HÃNG (MỚI THÊM) ---
const brandGridRef = ref<HTMLElement | null>(null)

function scrollBrands(direction: number) {
  if (brandGridRef.value) {
    const scrollAmount = 300 * direction
    brandGridRef.value.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}

function handleWheel(e: WheelEvent) {
  if (brandGridRef.value) {
    e.preventDefault()
    brandGridRef.value.scrollLeft += e.deltaY
  }
}

// --- QUẢN LÝ GIẢM GIÁ (2 API ongoing / upcoming) ---
const currentTime = ref(new Date().getTime())
let timer: ReturnType<typeof setInterval> | null = null

const ongoingDiscountProducts = ref<DiscountProductResponse[]>([])
const upcomingDiscountProducts = ref<DiscountProductResponse[]>([])

const ongoingDiscountInfo = computed(() => ongoingDiscountProducts.value?.[0] ?? null)
const upcomingDiscountInfo = computed(() => upcomingDiscountProducts.value?.[0] ?? null)

function extractItems<T = any>(res: any): T[] {
  const payload = res?.data ?? res
  const d = payload?.data ?? payload

  if (Array.isArray(d))
    return d as T[]
  if (Array.isArray(d?.items))
    return d.items as T[]
  if (Array.isArray(d?.data))
    return d.data as T[]
  if (Array.isArray(d?.data?.data))
    return d.data.data as T[]
  if (Array.isArray(d?.data?.items))
    return d.data.items as T[]

  return []
}

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

async function fetchDiscountProducts() {
  loadingDiscounts.value = true
  try {
    const params: DiscountProductRequest = { page: 1, size: 15 }

    const [ongoingRes, upcomingRes] = await Promise.all([
      getOngoingDiscounts(params),
      getNearestUpcomingDiscounts(params),
    ])

    ongoingDiscountProducts.value = extractItems<DiscountProductResponse>(ongoingRes)
    upcomingDiscountProducts.value = extractItems<DiscountProductResponse>(upcomingRes)
  }
  catch (error) {
    console.error('Lỗi khi tải dữ liệu giảm giá (ongoing/upcoming):', error)
  }
  finally {
    loadingDiscounts.value = false
  }
}

// --- QUẢN LÝ SẢN PHẨM MỚI NHẤT ---
const newest = computed(() => {
  return [...productDetails.value].slice(0, 10)
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

function handleClickProduct(id: string, item?: any) {
  const queryParams: any = {}
  if (item) {
    if (item.percentage !== undefined && item.percentage !== null)
      queryParams.pct = item.percentage
    if (item.startDate)
      queryParams.sd = item.startDate
    if (item.endDate)
      queryParams.ed = item.endDate
  }

  router.push({
    name: 'ProductDetail',
    params: { id },
    query: queryParams,
  })
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
  fetchDiscountProducts()
  fetchBrands()

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
  <div class="mx-auto max-w-[1300px] px-4 py-8 rgb(255 255 255) min-h-screen font-sans">
    <div class="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-12">
      <div class="lg:col-span-8 rounded-2xl overflow-hidden shadow-[0_8px_30px_rgb(0,0,0,0.08)] bg-white relative group">
        <NCarousel show-arrow autoplay draggable dot-type="line" class="h-[250px] md:h-[400px]">
          <div v-for="b in heroBanners" :key="b.id" class="relative w-full h-full cursor-pointer overflow-hidden" @click="goProducts()">
            <img :src="b.url" :alt="b.title" class="w-full h-full object-cover transform transition-transform duration-700 group-hover:scale-105">
            <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-black/20 to-transparent" />
            <div class="absolute bottom-0 left-0 p-6 md:p-10 w-full">
              <h2 class="text-white text-2xl md:text-4xl font-extrabold tracking-tight mb-5 drop-shadow-md">
                {{ b.title }}
              </h2>
              <NButton
                type="primary" size="large"
                class="!rounded-xl !font-bold !px-8 shadow-[0_4px_14px_0_rgba(225,29,72,0.39)] hover:-translate-y-1 transition-all duration-300 bg-red-600 hover:bg-red-500 border-none"
                @click.stop="goProducts()"
              >
                Khám phá ngay
              </NButton>
            </div>
          </div>
        </NCarousel>
      </div>

      <div class="lg:col-span-4 flex flex-col gap-4">
        <div class="bg-white rounded-2xl p-6 shadow-[0_8px_30px_rgb(0,0,0,0.04)] h-full flex flex-col justify-between border border-gray-100">
          <div>
            <div class="flex items-center gap-2 mb-6">
              <NovaIcon icon="icon-park-solid:diamond" :size="24" class="text-green-600" />
              <h3 class="text-xl font-extrabold text-gray-800 uppercase tracking-wide">
                Đặc quyền mua sắm
              </h3>
            </div>

            <div class="grid grid-cols-2 gap-4">
              <div
                v-for="b in quickBenefits" :key="b.id"
                class="flex flex-col gap-3 p-4 rounded-xl bg-gray-50/80 border border-gray-100 hover:border-green-200 hover:bg-green-50/50 hover:shadow-md transition-all duration-300 group cursor-default"
              >
                <div class="w-12 h-12 rounded-full bg-white shadow-sm flex items-center justify-center text-[#d70018] group-hover:scale-110 transition-transform">
                  <NovaIcon :icon="b.icon" :size="24" />
                </div>
                <div>
                  <div class="font-bold text-gray-900 text-sm mb-0.5">
                    {{ b.title }}
                  </div>
                  <div class="text-[13px] text-gray-500 leading-tight">
                    {{ b.desc }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="mb-10 px-2 md:px-6 py-6 bg-green-10 rounded-2xl shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-gray-100">
      <div class="flex flex-col items-center justify-center mb-8">
        <h2 class="text-2xl font-black text-gray-800 uppercase tracking-wider relative flex items-center gap-2">
          <NovaIcon icon="icon-park-solid:category-management" class="text-green-600" />
          CHỌN THEO HÃNG
        </h2>
        <div class="w-16 h-1 bg-red-600 mt-3 rounded-full" />
      </div>

      <div class="brand-scroll-wrapper">
        <button class="scroll-btn left-btn" @click="scrollBrands(-1)">
          <NovaIcon icon="icon-park-outline:left" :size="20" />
        </button>

        <div
          ref="brandGridRef"
          class="grid grid-rows-2 grid-flow-col gap-3 md:gap-4 overflow-x-auto hide-scrollbar snap-x snap-mandatory auto-cols-[calc((100%-36px)/2.5)] md:auto-cols-[calc((100%-64px)/5)] py-2 w-full"
          style="scroll-behavior: smooth;"
          @wheel.prevent="handleWheel"
        >
          <button
            v-for="b in brands" :key="b.id"
            class="snap-start bg-white rounded-2xl h-16 flex items-center justify-center shadow-[0_2px_8px_rgba(0,0,0,0.04)] hover:shadow-[0_8px_20px_rgba(0,166,81,0.15)] hover:-translate-y-1 transition-all duration-300 border-[1.5px] border-gray-100 hover:border-green-500 group px-2"
            @click="goProducts(b.id)"
          >
            <img v-if="b.logoUrl" :src="b.logoUrl" :alt="b.name" class="max-h-8 object-contain px-2 group-hover:scale-110 transition-transform">
            <div v-else class="flex items-center gap-2 group-hover:scale-110 transition-transform">
              <NovaIcon v-if="b.icon" :icon="b.icon" :size="28" class="text-gray-800" />
              <span v-else :class="b.textStyling" class="truncate px-2">{{ b.name }}</span>
            </div>
          </button>
        </div>

        <button class="scroll-btn right-btn" @click="scrollBrands(1)">
          <NovaIcon icon="icon-park-outline:right" :size="20" />
        </button>
      </div>
    </div>
    <div v-if="ongoingDiscountInfo" class="mb-14">
      <div class="bg-gradient-to-r from-red-600 via-red-500 to-orange-500 rounded-2xl p-4 md:p-5 shadow-[0_10px_40px_rgba(239,68,68,0.3)] relative overflow-hidden">
        <div class="absolute -top-20 -right-20 w-64 h-64 bg-white opacity-10 rounded-full blur-3xl" />
        <div class="absolute -bottom-20 -left-20 w-64 h-64 bg-yellow-400 opacity-20 rounded-full blur-3xl" />

        <div class="flex flex-col md:flex-row md:items-center justify-between mb-5 gap-4 border-b border-white/20 pb-4">
          <div class="flex items-start gap-2 flex-1 min-w-0 z-10 mt-1">
            <NovaIcon icon="icon-park-solid:lightning" class="text-yellow-300 animate-pulse shrink-0 mt-0.5" :size="34" />
            <div class="flex flex-col">
              <span class="text-yellow-200 text-[11px] md:text-xs font-black uppercase tracking-wider mb-1">
                Đang diễn ra
              </span>
              <h2
                class="text-xl md:text-2xl font-black text-white drop-shadow-md leading-snug break-words line-clamp-2"
                :title="ongoingDiscountInfo.discountName"
              >
                {{ ongoingDiscountInfo.discountName }}
              </h2>
            </div>
          </div>

          <div class="flex items-center gap-2 z-10 bg-black/20 px-3.5 py-2 rounded-xl backdrop-blur-md border border-white/10 shadow-inner shrink-0">
            <span class="text-white/90 text-xs font-bold uppercase tracking-wider">Kết thúc sau</span>
            <div class="flex items-center gap-1 font-black text-base">
              <span v-if="getCountdown(ongoingDiscountInfo.endDate).d > 0" class="text-yellow-300 mr-1 text-sm">{{ getCountdown(ongoingDiscountInfo.endDate).d }} Ngày</span>
              <span class="bg-white text-red-600 px-2 py-1 rounded shadow-sm min-w-[32px] text-center">{{ getCountdown(ongoingDiscountInfo.endDate).h }}</span>
              <span class="text-white/70">:</span>
              <span class="bg-white text-red-600 px-2 py-1 rounded shadow-sm min-w-[32px] text-center">{{ getCountdown(ongoingDiscountInfo.endDate).m }}</span>
              <span class="text-white/70">:</span>
              <span class="bg-white text-red-600 px-2 py-1 rounded shadow-sm min-w-[32px] text-center">{{ getCountdown(ongoingDiscountInfo.endDate).s }}</span>
            </div>
          </div>
        </div>

        <div v-if="loadingDiscounts" class="py-12 flex justify-center">
          <NSpin size="large" stroke="#ffffff" />
        </div>
        <div v-else-if="ongoingDiscountProducts.length === 0" class="py-12 bg-white/10 rounded-xl backdrop-blur-sm">
          <p class="text-center text-white font-medium">
            Sản phẩm đang được cập nhật...
          </p>
        </div>

        <div v-else class="flex gap-4 md:gap-5 overflow-x-auto pb-3 snap-x snap-mandatory hide-scrollbar -mx-4 px-4 md:mx-0 md:px-0 z-10 relative">
          <div
            v-for="item in ongoingDiscountProducts" :key="item.productDetailId"
            class="snap-start shrink-0 w-[240px] md:w-[260px] bg-white rounded-2xl p-4 md:p-5 shadow-lg hover:shadow-2xl transition-all duration-300 cursor-pointer relative group flex flex-col border-2 border-transparent hover:border-yellow-400"
            @click="handleClickProduct(item.productDetailId, item)"
          >
            <div class="absolute top-3 left-3 z-20">
              <span v-if="item.percentage > 0" class="bg-gradient-to-r from-red-600 to-red-500 text-white text-[11px] font-black px-2.5 py-1 rounded-tr-xl rounded-bl-xl rounded-tl-sm rounded-br-sm shadow-md flex items-center gap-1 border border-green-400">
                GIẢM {{ item.percentage }}%
              </span>
            </div>

            <div class="h-[160px] mt-6 mb-4 flex items-center justify-center group-hover:-translate-y-3 transition-transform duration-500 relative">
              <img :src="item.urlImage" :alt="item.productName" class="max-w-full max-h-[150px] object-contain drop-shadow-sm" @error="handleImageError">
            </div>

            <div class="flex flex-col gap-2 mt-auto">
              <h3 class="text-[14px] font-bold text-gray-800 line-clamp-2 min-h-[40px] leading-snug group-hover:text-[#d70018] transition-colors">
                {{ item.productName }} {{ item.cpu }} {{ item.ram }} {{ item.gpu }}
              </h3>

              <div class="flex flex-wrap gap-1.5 mt-1">
                <span v-if="item.ram" class="bg-orange-50 text-orange-500 text-[11px] font-semibold px-2 py-0.5 rounded border border-orange-200">{{ item.ram }}</span>
                <span v-if="item.hardDrive" class="bg-orange-50 text-orange-500 text-[11px] font-semibold px-2 py-0.5 rounded border border-orange-200">{{ item.hardDrive }}</span>
              </div>

              <div class="mt-3 flex items-end justify-between">
                <div class="flex flex-col">
                  <span class="text-[#d70018] text-lg font-black leading-none">{{ formatCurrency(item.salePrice) }}</span>
                  <span class="text-gray-400 text-xs line-through font-semibold mt-1">{{ formatCurrency(item.originalPrice) }}</span>
                </div>
                <div class="w-7 h-7 rounded-full bg-green-50 flex items-center justify-center group-hover:bg-green-600 group-hover:text-white transition-colors text-green-600">
                  <NovaIcon icon="icon-park-outline:shopping-cart" :size="20" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="upcomingDiscountInfo" class="mb-14">
      <div class="bg-gradient-to-r from-blue-800 via-blue-700 to-indigo-600 rounded-2xl p-4 md:p-5 shadow-[0_10px_40px_rgba(37,99,235,0.2)] relative overflow-hidden">
        <div class="absolute top-0 right-0 w-full h-full bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-5" />

        <div class="flex flex-col md:flex-row md:items-center justify-between mb-5 gap-4 border-b border-white/20 pb-4">
          <div class="flex items-start gap-2 flex-1 min-w-0 z-10 mt-1">
            <NovaIcon icon="icon-park-solid:alarm-clock" class="text-cyan-300 animate-bounce shrink-0 mt-0.5" :size="34" />
            <div class="flex flex-col">
              <span class="text-cyan-200 text-[13px] md:text-xs font-black uppercase tracking-wider mb-1">
                Sắp diễn ra
              </span>
              <h2
                class="text-xl md:text-2xl font-black text-white drop-shadow-md leading-snug break-words line-clamp-2"
                :title="upcomingDiscountInfo.discountName"
              >
                {{ upcomingDiscountInfo.discountName }}
              </h2>
            </div>
          </div>

          <div class="flex items-center gap-2 z-10 bg-black/20 px-3.5 py-2 rounded-xl backdrop-blur-md border border-white/10 shadow-inner shrink-0">
            <span class="text-white/90 text-xs font-bold uppercase tracking-wider">Bắt đầu sau</span>
            <div class="flex items-center gap-1 font-black text-base">
              <span v-if="getCountdown(upcomingDiscountInfo.startDate).d > 0" class="text-cyan-300 mr-1 text-sm">{{ getCountdown(upcomingDiscountInfo.startDate).d }} Ngày</span>
              <span class="bg-white/95 text-blue-800 px-2 py-1 rounded shadow-sm min-w-[32px] text-center">{{ getCountdown(upcomingDiscountInfo.startDate).h }}</span>
              <span class="text-white/70">:</span>
              <span class="bg-white/95 text-blue-800 px-2 py-1 rounded shadow-sm min-w-[32px] text-center">{{ getCountdown(upcomingDiscountInfo.startDate).m }}</span>
              <span class="text-white/70">:</span>
              <span class="bg-white/95 text-blue-800 px-2 py-1 rounded shadow-sm min-w-[32px] text-center">{{ getCountdown(upcomingDiscountInfo.startDate).s }}</span>
            </div>
          </div>
        </div>

        <div v-if="loadingDiscounts" class="py-12 flex justify-center">
          <NSpin size="large" stroke="#ffffff" />
        </div>

        <div v-else class="flex gap-4 md:gap-5 overflow-x-auto pb-3 snap-x snap-mandatory hide-scrollbar -mx-4 px-4 md:mx-0 md:px-0 z-10 relative">
          <div
            v-for="item in upcomingDiscountProducts" :key="item.productDetailId"
            class="snap-start shrink-0 w-[240px] md:w-[260px] bg-white rounded-2xl p-4 md:p-5 shadow-lg relative flex flex-col group overflow-hidden border-2 border-transparent hover:border-blue-400 transition-all duration-300 cursor-pointer"
            @click="handleClickProduct(item.productDetailId, item)"
          >
            <div class="absolute inset-0 bg-white/60 backdrop-blur-[2px] z-30 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300">
              <span class="bg-blue-600 text-white font-extrabold px-5 py-2.5 text-sm rounded-xl shadow-xl flex items-center gap-2 transform translate-y-4 group-hover:translate-y-0 transition-all">
                <NovaIcon icon="icon-park-outline:preview-open" /> Xem trước
              </span>
            </div>

            <div class="absolute top-3 left-3 z-40">
              <span v-if="item.percentage > 0" class="bg-gradient-to-r from-yellow-400 to-yellow-500 text-blue-900 text-[11px] font-black px-2.5 py-1 rounded-tr-xl rounded-bl-xl rounded-tl-sm rounded-br-sm shadow-md flex items-center gap-1 border border-yellow-300">
                SẮP GIẢM {{ item.percentage }}%
              </span>
            </div>

            <div class="h-[160px] mt-6 mb-4 flex items-center justify-center group-hover:scale-105 transition-transform duration-500 relative z-20">
              <img :src="item.urlImage" :alt="item.productName" class="max-w-full max-h-[150px] object-contain drop-shadow-sm" @error="handleImageError">
            </div>

            <div class="flex flex-col gap-2 mt-auto z-20 relative">
              <h3 class="text-[14px] font-bold text-gray-800 line-clamp-2 min-h-[40px] leading-snug group-hover:text-blue-700 transition-colors">
                {{ item.productName }} {{ item.cpu }} {{ item.ram }} {{ item.gpu }}
              </h3>

              <div class="flex flex-wrap gap-1.5 mt-1">
                <span v-if="item.ram" class="bg-blue-50 text-blue-700 text-[11px] font-semibold px-2 py-0.5 rounded border border-blue-100">{{ item.ram }}</span>
                <span v-if="item.hardDrive" class="bg-blue-50 text-blue-700 text-[11px] font-semibold px-2 py-0.5 rounded border border-blue-100">{{ item.hardDrive }}</span>
              </div>

              <div class="mt-3 bg-gray-50 rounded-lg p-3 border border-dashed border-gray-300 flex flex-col gap-1">
                <div class="text-[11px] text-gray-500 font-medium uppercase tracking-wide">
                  Giá dự kiến:
                </div>
                <div class="flex flex-col">
                  <span class="text-blue-700 text-lg font-black leading-none">{{ formatCurrency(item.salePrice) }}</span>
                  <span class="text-gray-400 text-xs line-through font-medium mt-1">{{ formatCurrency(item.originalPrice) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div>
      <div class="flex items-end justify-between mb-8 border-b border-gray-200 pb-4">
        <div>
          <h2 class="text-2xl md:text-3xl font-black text-gray-900 flex items-center gap-2">
            <NovaIcon icon="icon-park-solid:fire" class="text-orange-500" /> Sản phẩm mới nhất
          </h2>
          <p class="text-gray-500 text-sm mt-1 ml-9">
            Cập nhật xu hướng công nghệ hàng đầu
          </p>
        </div>
        <button
          class="flex items-center justify-center gap-2 px-6 py-2.5 text-sm font-bold text-white bg-green-600 rounded-xl shadow-md transition-all duration-300 hover:bg-green-500 hover:shadow-[0_6px_16px_rgba(22,163,74,0.4)] hover:-translate-y-1 active:scale-95"
          @click="goProducts()"
        >
          Khám phá ngay
        </button>
      </div>

      <div v-if="loading" class="py-16 flex justify-center">
        <NSpin size="large" />
      </div>
      <div v-else-if="newest.length === 0" class="py-16 bg-white rounded-2xl shadow-sm border border-gray-100">
        <NEmpty description="Rất tiếc, hiện tại chưa có sản phẩm" />
      </div>

      <div v-else class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4 md:gap-5">
        <div
          v-for="item in newest" :key="item.id"
          class="bg-white rounded-2xl p-5 shadow-[0_2px_12px_rgba(0,0,0,0.04)] hover:shadow-[0_8px_30px_rgba(0,0,0,0.12)] transition-all duration-300 hover:-translate-y-1.5 cursor-pointer border-2 border-transparent hover:border-green-400 relative group flex flex-col h-full overflow-hidden"
          @click="handleClickProduct(item.id, item)"
        >
          <div class="absolute top-3 left-3 z-20">
            <span v-if="(item.percentage ?? 0) > 0" class="bg-gradient-to-r from-red-600 to-red-500 text-white text-xs font-black px-3 py-1.5 rounded-tr-xl rounded-bl-xl rounded-tl-sm rounded-br-sm shadow-md flex items-center gap-1 border border-green-400">
              GIẢM {{ item.percentage }}%
            </span>
          </div>

          <div class="h-[180px] mt-6 mb-5 flex items-center justify-center group-hover:-translate-y-3 transition-transform duration-500 relative">
            <img :src="item.urlImage" :alt="item.name" class="max-w-full max-h-[170px] object-contain drop-shadow-sm" @error="handleImageError">
          </div>

          <div class="flex flex-col flex-1 gap-2 mt-auto">
            <h3 class="text-[15px] font-bold text-gray-800 line-clamp-2 min-h-[44px] leading-snug group-hover:text-[#d70018] transition-colors">
              {{ item.productName || item.name }} {{ item.cpu }} {{ item.ram }} {{ item.gpu }}
            </h3>

            <div class="flex flex-wrap gap-2 mt-2">
              <span v-if="item.ram" class="bg-green-50 text-green-600 text-xs font-semibold px-2.5 py-1 rounded-md border border-green-200">{{ item.ram }}</span>
              <span v-if="item.hardDrive" class="bg-blue-50 text-blue-600 text-xs font-semibold px-2.5 py-1 rounded-md border border-blue-200">{{ item.hardDrive }}</span>
            </div>

            <div class="mt-4 flex items-end justify-between">
              <div class="flex flex-col">
                <span class="text-[#d70018] text-xl font-black leading-none">{{ formatCurrency(salePrice(item.price, item.percentage)) }}</span>
                <span v-if="(item.percentage ?? 0) > 0" class="text-gray-400 text-sm line-through font-semibold mt-1">
                  {{ formatCurrency(item.price) }}
                </span>
              </div>
              <div class="w-8 h-8 rounded-full bg-green-50 flex items-center justify-center group-hover:bg-green-600 group-hover:text-white transition-colors text-green-600">
                <NovaIcon icon="icon-park-outline:shopping-cart" :size="20" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* =========================================
   ẨN THANH CUỘN MẶC ĐỊNH CHO CONTAINER NGANG
   ========================================= */
.hide-scrollbar::-webkit-scrollbar {
  display: none;
}
.hide-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

/* =========================================
   WRAPPER VÀ NÚT BẤM CUỘN NGANG (CÙNG PHONG CÁCH)
   ========================================= */
.brand-scroll-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  /* Chừa 2 khoảng trống 2 bên cho nút lùi ra xa */
  padding: 0 24px;
}

.scroll-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 10;
  width: 30px;  /* Nút hẹp hình chữ nhật đứng */
  height: 50px;
  border-radius: 12px; /* Bo góc mềm mại */
  background-color: #ffffff;
  border: 1px solid #e5e7eb;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6b7280;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.scroll-btn:hover {
  background-color: #48d58c; /* Màu xanh lá chuẩn theme */
  color: #ffffff;
  border-color: #00a651;
  box-shadow: 0 4px 12px rgba(0, 166, 81, 0.25);
  transform: translateY(-50%) scale(1.15);
}

/* Đẩy 2 nút ra sát mép màn hình / mép container */
.left-btn {
  left: -10px;
}

.right-btn {
  right: -10px;
}

/* Dành riêng cho thiết bị lớn thì có thể đẩy lùi nút ra xa thêm chút nữa nếu thích */
@media (min-width: 768px) {
  .left-btn {
    left: -16px;
  }
  .right-btn {
    right: -16px;
  }
}
</style>
