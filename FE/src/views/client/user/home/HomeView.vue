<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NSkeleton,
  NSpin,
} from 'naive-ui'
import NovaIcon from '@/components/common/NovaIcon.vue'
import hero1 from '@/assets/images/banner4.jpg'
import hero2 from '@/assets/images/bg4.jpg'
import hero3 from '@/assets/images/banner7.jpg'
import {
  getBestsellerProducts,
  getNearestUpcomingDiscounts,
  getOngoingDiscounts,
  getProductDetails,
} from '@/service/api/client/product/productDetail.api'
import type {
  ADProductDetailResponse,
  DiscountProductRequest,
  DiscountProductResponse,
} from '@/service/api/client/product/productDetail.api'
import { getBrands } from '@/service/api/client/product/product.api'

const router = useRouter()
const loading = ref(false)
const loadingDiscounts = ref(false)
const loadingBestseller = ref(false)
const productDetails = ref<ADProductDetailResponse[]>([])
const bestSellerProducts = ref<DiscountProductResponse[]>([])

const heroBanners = ref([
  { id: 1, url: hero1, title: 'Laptop Gaming', subtitle: 'Chinh phục mọi tựa game với dòng laptop gaming cao cấp nhất', tag: 'Hiệu năng mạnh mẽ' },
  { id: 2, url: hero2, title: 'MacBook Series', subtitle: 'Trải nghiệm đẳng cấp cùng hệ sinh thái Apple hoàn hảo', tag: 'Mượt · Bền · Tối giản' },
  { id: 3, url: hero3, title: 'Workstation Pro', subtitle: 'Cấu hình mạnh mẽ cho dân lập trình, designer chuyên nghiệp', tag: 'Chuẩn đồ nghề Dev' },
])
const activeSlide = ref(0)
let slideTimer: ReturnType<typeof setInterval> | null = null

function nextSlide() { activeSlide.value = (activeSlide.value + 1) % heroBanners.value.length }
function prevSlide() { activeSlide.value = (activeSlide.value - 1 + heroBanners.value.length) % heroBanners.value.length }
function goSlide(i: number) { activeSlide.value = i }

const quickBenefits = ref([
  { id: 1, icon: 'ion:rocket-outline', title: 'Giao siêu tốc', desc: '2h nội thành' },
  { id: 2, icon: 'ion:chatbubbles-outline', title: 'Tư vấn 24/7', desc: 'Đội ngũ chuyên nghiệp' },
  { id: 3, icon: 'ion:gift-outline', title: 'Quà hấp dẫn', desc: 'Kèm theo sản phẩm' },
  { id: 4, icon: 'ion:shield-checkmark-outline', title: 'Bảo hành hãng', desc: '12 tháng, 1 đổi 1' },
  { id: 6, icon: 'ion:refresh-outline', title: 'Đổi trả miễn phí', desc: '30 ngày' },
])

interface BrandDisplay { id: string, name: string, icon?: string, image?: string, color?: string }
const brands = ref<BrandDisplay[]>([])
const brandStyleMap: Record<string, { icon: string, color: string }> = {
  apple: { icon: 'simple-icons:apple', color: '#000000' },
  samsung: { icon: 'simple-icons:samsung', color: '#1428A0' },
  xiaomi: { icon: 'simple-icons:xiaomi', color: '#FF6900' },
  asus: { icon: 'simple-icons:asus', color: '#00539B' },
  dell: { icon: 'simple-icons:dell', color: '#007DB8' },
  hp: { icon: 'simple-icons:hp', color: '#0096D6' },
  lenovo: { icon: 'simple-icons:lenovo', color: '#E2231A' },
  acer: { icon: 'simple-icons:acer', color: '#83B81A' },
  msi: { icon: 'simple-icons:msi', color: '#FF0000' },
  vivo: { icon: 'simple-icons:vivo', color: '#411445' },
  lg: { icon: 'simple-icons:lg', color: '#A50034' },
}

async function fetchBrands() {
  try {
    const res: any = await getBrands()
    brands.value = (res?.data || []).map((b: any) => {
      const labelStr = (b.label || b.name || '').toLowerCase().trim()
      let matchedStyle = {}
      for (const [key, val] of Object.entries(brandStyleMap)) {
        if (labelStr.includes(key)) {
          matchedStyle = val
          break
        }
      }
      return {
        id: b.value || b.id || '',
        name: b.label || b.name || '',
        ...matchedStyle,
      }
    })
  }
  catch (e) { console.error(e) }
}

const brandGridRef = ref<HTMLElement | null>(null)
// Cuộn ngang khoảng 4 ô mỗi lần bấm
function scrollBrands(dir: number) {
  if (brandGridRef.value) {
    const scrollAmount = (brandGridRef.value.clientWidth / 2) * dir
    brandGridRef.value.scrollBy({ left: scrollAmount, behavior: 'smooth' })
  }
}

// Refs & Function cho cuộn ngang (Chung cho Sale & Bestseller)
const flashSaleRowRef = ref<HTMLElement | null>(null)
const upcomingSaleRowRef = ref<HTMLElement | null>(null)
const bestsellerRowRef = ref<HTMLElement | null>(null)

function scrollRow(type: 'flash' | 'upcoming' | 'bestseller', dir: number) {
  const el = type === 'flash'
    ? flashSaleRowRef.value
    : type === 'upcoming'
      ? upcomingSaleRowRef.value
      : bestsellerRowRef.value
  el?.scrollBy({ left: 240 * dir, behavior: 'smooth' })
}

const currentTime = ref(Date.now())
let timer: ReturnType<typeof setInterval> | null = null
const ongoingDiscountProducts = ref<DiscountProductResponse[]>([])
const upcomingDiscountProducts = ref<DiscountProductResponse[]>([])
const ongoingDiscountInfo = computed(() => ongoingDiscountProducts.value?.[0] ?? null)
const upcomingDiscountInfo = computed(() => upcomingDiscountProducts.value?.[0] ?? null)

function extractItems<T = any>(res: any): T[] {
  const d = (res?.data ?? res)?.data ?? (res?.data ?? res)
  if (Array.isArray(d))
    return d as T[]
  if (Array.isArray(d?.items))
    return d.items as T[]
  if (Array.isArray(d?.data))
    return d.data as T[]
  if (Array.isArray(d?.data?.data))
    return d.data.data as T[]
  return []
}

function getCountdown(t: number) {
  const diff = t - currentTime.value
  if (diff <= 0)
    return { d: '00', h: '00', m: '00', s: '00' }
  const p = (n: number) => String(n).padStart(2, '0')
  return {
    d: p(Math.floor(diff / 86400000)),
    h: p(Math.floor((diff % 86400000) / 3600000)),
    m: p(Math.floor((diff % 3600000) / 60000)),
    s: p(Math.floor((diff % 60000) / 1000)),
  }
}

async function fetchDiscountProducts() {
  loadingDiscounts.value = true
  try {
    const p: DiscountProductRequest = { page: 1, size: 20 }
    const [a, b] = await Promise.all([getOngoingDiscounts(p), getNearestUpcomingDiscounts(p)])
    ongoingDiscountProducts.value = extractItems<DiscountProductResponse>(a)
    upcomingDiscountProducts.value = extractItems<DiscountProductResponse>(b)
  }
  catch (e) { console.error(e) }
  finally { loadingDiscounts.value = false }
}
async function fetchBestSellers() {
  loadingBestseller.value = true
  try { bestSellerProducts.value = extractItems<DiscountProductResponse>(await getBestsellerProducts({ page: 1, size: 10 })) }
  catch (e) { console.error(e) }
  finally { loadingBestseller.value = false }
}
const newest = computed(() => [...productDetails.value].slice(0, 10))
async function fetchNormalProducts() {
  loading.value = true
  try {
    const res = await getProductDetails({ page: 1, size: 40, minPrice: 0, maxPrice: 1000000000 })
    if (res?.data) {
      const d = res.data
      if (d.data && !Array.isArray(d.data) && (d.data as any).data)
        productDetails.value = (d.data as any).data
      else if (Array.isArray(d.data))
        productDetails.value = d.data
      else if (Array.isArray(d))
        productDetails.value = d
    }
  }
  catch (e) { console.error(e) }
  finally { loading.value = false }
}

function goProducts(brandId?: string) {
  router.push(brandId ? { name: 'Products', query: { brand: brandId } } : { name: 'Products' })
}
function handleClickProduct(id: string, item?: any) {
  const q: any = {}
  if (item) {
    if (item.percentage != null)
      q.pct = item.percentage
    if (item.startDate)
      q.sd = item.startDate
    if (item.endDate)
      q.ed = item.endDate
  }
  router.push({ name: 'ProductDetail', params: { id }, query: q })
}
function formatCurrency(v: number) {
  return v ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(v) : 'Liên hệ'
}
function salePrice(price: number, pct?: number) { return Math.round(price * (100 - (pct ?? 0)) / 100) }
function handleImageError(e: Event) { (e.target as HTMLImageElement).src = 'https://via.placeholder.com/640x640.png?text=MY+LAPTOP' }

onMounted(() => {
  fetchNormalProducts(); fetchDiscountProducts(); fetchBestSellers(); fetchBrands()
  timer = setInterval(() => { currentTime.value = Date.now() }, 1000)
  slideTimer = setInterval(nextSlide, 5000)
})
onUnmounted(() => {
  if (timer)
    clearInterval(timer)
  if (slideTimer)
    clearInterval(slideTimer)
})
</script>

<template>
  <div class="hp">
    <div class="announce-bar">
      <div class="announce-inner">
        <span class="announce-pulse" />
        <span class="announce-text">🚀 Trải nghiệm hệ sinh thái <strong>MY LAPTOP</strong> — Miễn phí vận chuyển cho đơn
          từ <strong>20.000.000đ</strong></span>
        <button class="announce-cta" @click="goProducts()">
          Mua ngay →
        </button>
      </div>
    </div>

    <div class="hp-wrap">
      <section class="hero">
        <div class="hero-track" :style="{ transform: `translateX(-${activeSlide * 100}%)` }">
          <div v-for="b in heroBanners" :key="b.id" class="hero-slide" @click="goProducts()">
            <img :src="b.url" :alt="b.title" class="hero-img">
            <div class="hero-overlay">
              <div class="hero-content">
                <span class="hero-tag">{{ b.tag }}</span>
                <h1 class="hero-title">
                  {{ b.title }}
                </h1>
                <p class="hero-sub">
                  {{ b.subtitle }}
                </p>
                <button class="hero-btn" @click.stop="goProducts()">
                  Khám phá ngay
                  <NovaIcon icon="ion:arrow-forward-outline" :size="16" />
                </button>
              </div>
            </div>
          </div>
        </div>
        <button class="hero-arrow hero-arrow--prev" @click.stop="prevSlide()">
          <NovaIcon icon="ion:chevron-back" :size="20" />
        </button>
        <button class="hero-arrow hero-arrow--next" @click.stop="nextSlide()">
          <NovaIcon icon="ion:chevron-forward" :size="20" />
        </button>
        <div class="hero-dots">
          <button
            v-for="(_, i) in heroBanners" :key="i" class="hero-dot"
            :class="{ 'hero-dot--active': i === activeSlide }" @click.stop="goSlide(i)"
          />
        </div>
        <div class="hero-counter">
          {{ activeSlide + 1 }} / {{ heroBanners.length }}
        </div>
      </section>

      <section class="benefits-grid">
        <div v-for="b in quickBenefits" :key="b.id" class="benefit-item">
          <div class="benefit-icon">
            <NovaIcon :icon="b.icon" :size="20" />
          </div>
          <div class="benefit-text">
            <span class="benefit-title">{{ b.title }}</span>
            <span class="benefit-desc">{{ b.desc }}</span>
          </div>
        </div>
      </section>

      <section class="modern-brand-section">
        <div class="brand-header">
          <div class="brand-title-group">
            <h2 class="title-text">
              Chọn theo thương hiệu
            </h2>
          </div>
          <div class="header-actions">
            <button class="icon-btn" @click="scrollBrands(-1)">
              <NovaIcon icon="ion:chevron-back" :size="16" />
            </button>
            <button class="icon-btn" @click="scrollBrands(1)">
              <NovaIcon icon="ion:chevron-forward" :size="16" />
            </button>
          </div>
        </div>

        <div ref="brandGridRef" class="brand-scroll-container">
          <div v-for="b in brands" :key="b.id" class="brand-box" @click="goProducts(b.id)">
            <template v-if="b.icon">
              <NovaIcon :icon="b.icon" :size="36" class="brand-logo-icon" :color="b.color" />
            </template>
            <template v-else-if="b.image">
              <img :src="b.image" :alt="b.name" class="brand-logo-img">
            </template>
            <template v-else>
              <span class="brand-text">{{ b.name }}</span>
            </template>
          </div>
        </div>
      </section>
      <section v-if="ongoingDiscountInfo" class="sale-block sale-block--flash">
        <div class="sale-header">
          <div class="sale-identity">
            <div class="sale-badge-icon">
              <NovaIcon icon="ion:flash" :size="18" color="#fff" />
            </div>
            <div>
              <span class="sale-eyebrow">Đang diễn ra</span>
              <h2 class="sale-title">
                Flash Sale
              </h2>
            </div>
            <span class="sale-name-chip">{{ ongoingDiscountInfo.discountName }}</span>
          </div>

          <div class="sale-actions-wrap">
            <div class="sale-timer-group">
              <span class="timer-label">⏰ Kết thúc trong</span>
              <div class="countdown">
                <div class="cd-box">
                  <span class="cd-num">{{ getCountdown(ongoingDiscountInfo.endDate).d }}</span><span
                    class="cd-lbl"
                  >NGÀY</span>
                </div>
                <span class="cd-colon">:</span>
                <div class="cd-box">
                  <span class="cd-num">{{ getCountdown(ongoingDiscountInfo.endDate).h }}</span><span
                    class="cd-lbl"
                  >GIỜ</span>
                </div>
                <span class="cd-colon">:</span>
                <div class="cd-box">
                  <span class="cd-num">{{ getCountdown(ongoingDiscountInfo.endDate).m }}</span><span
                    class="cd-lbl"
                  >PHÚT</span>
                </div>
                <span class="cd-colon">:</span>
                <div class="cd-box">
                  <span class="cd-num">{{ getCountdown(ongoingDiscountInfo.endDate).s }}</span><span
                    class="cd-lbl"
                  >GIÂY</span>
                </div>
              </div>
            </div>
            <div class="header-actions">
              <button class="icon-btn icon-btn--sale" @click="scrollRow('flash', -1)">
                <NovaIcon icon="ion:chevron-back" :size="16" />
              </button>
              <button class="icon-btn icon-btn--sale" @click="scrollRow('flash', 1)">
                <NovaIcon icon="ion:chevron-forward" :size="16" />
              </button>
            </div>
          </div>
        </div>

        <div v-if="loadingDiscounts" class="loading-state">
          <NSpin />
        </div>
        <div v-else ref="flashSaleRowRef" class="product-row">
          <div
            v-for="item in ongoingDiscountProducts" :key="item.productDetailId" class="p-card p-card--sale"
            @click="handleClickProduct(item.productDetailId, item)"
          >
            <div class="p-img-box">
              <img :src="item.urlImage" :alt="item.name" loading="lazy" @error="handleImageError">
              <span class="p-badge badge--red">−{{ item.percentage }}%</span>
            </div>
            <div class="p-info">
              <h3 class="p-name">
                {{ item.name }}
              </h3>
              <div class="p-specs">
                <span v-if="item.ram" class="p-spec">{{ item.ram }}</span>
                <span v-if="item.hardDrive" class="p-spec">{{ item.hardDrive }}</span>
              </div>
              <div class="p-pricing">
                <span v-if="item.originalPrice > item.salePrice" class="p-price-old">{{
                  formatCurrency(item.originalPrice)
                }}</span>
                <span class="p-price-now">{{ formatCurrency(item.salePrice) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="upcomingDiscountInfo" class="sale-block sale-block--upcoming">
        <div class="sale-header">
          <div class="sale-identity">
            <div class="sale-badge-icon">
              <NovaIcon icon="ion:alarm-outline" :size="18" color="#fff" />
            </div>
            <div>
              <span class="sale-eyebrow">Sắp mở bán</span>
              <h2 class="sale-title">
                Sắp diễn ra
              </h2>
            </div>
            <span class="sale-name-chip chip-blue">{{ upcomingDiscountInfo.discountName }}</span>
          </div>

          <div class="sale-actions-wrap">
            <div class="sale-timer-group">
              <span class="timer-label">⏰ Bắt đầu sau</span>
              <div class="countdown">
                <div class="cd-box">
                  <span class="cd-num">{{ getCountdown(upcomingDiscountInfo.startDate).d }}</span><span
                    class="cd-lbl"
                  >NGÀY</span>
                </div>
                <span class="cd-colon">:</span>
                <div class="cd-box">
                  <span class="cd-num">{{ getCountdown(upcomingDiscountInfo.startDate).h }}</span><span
                    class="cd-lbl"
                  >GIỜ</span>
                </div>
                <span class="cd-colon">:</span>
                <div class="cd-box">
                  <span class="cd-num">{{ getCountdown(upcomingDiscountInfo.startDate).m }}</span><span
                    class="cd-lbl"
                  >PHÚT</span>
                </div>
                <span class="cd-colon">:</span>
                <div class="cd-box">
                  <span class="cd-num">{{ getCountdown(upcomingDiscountInfo.startDate).s }}</span><span
                    class="cd-lbl"
                  >GIÂY</span>
                </div>
              </div>
            </div>
            <div class="header-actions">
              <button class="icon-btn icon-btn--sale" @click="scrollRow('upcoming', -1)">
                <NovaIcon icon="ion:chevron-back" :size="16" />
              </button>
              <button class="icon-btn icon-btn--sale" @click="scrollRow('upcoming', 1)">
                <NovaIcon icon="ion:chevron-forward" :size="16" />
              </button>
            </div>
          </div>
        </div>

        <div v-if="loadingDiscounts" class="loading-state">
          <NSpin />
        </div>
        <div v-else ref="upcomingSaleRowRef" class="product-row">
          <div
            v-for="item in upcomingDiscountProducts" :key="item.productDetailId" class="p-card p-card--upcoming"
            @click="handleClickProduct(item.productDetailId, item)"
          >
            <div class="p-img-box">
              <img :src="item.urlImage" :alt="item.name" loading="lazy" @error="handleImageError">
              <span class="p-badge badge--indigo">Sắp −{{ item.percentage }}%</span>
            </div>
            <div class="p-info">
              <h3 class="p-name">
                {{ item.name }}
              </h3>
              <div class="p-specs">
                <span v-if="item.ram" class="p-spec">{{ item.ram }}</span>
                <span v-if="item.hardDrive" class="p-spec">{{ item.hardDrive }}</span>
              </div>
              <div class="p-pricing">
                <span class="p-price-now">{{ formatCurrency(item.salePrice) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-if="bestSellerProducts.length > 0" class="card-block">
        <div class="block-header">
          <div class="block-title-group">
            <span class="block-eyebrow">Được mua nhiều nhất</span>
            <h2 class="block-title">
              🏆 Bán chạy nhất
            </h2>
          </div>
          <div class="header-actions" style="align-items: center;">
            <button class="view-all-btn" @click="goProducts()">
              Xem tất cả →
            </button>
            <button class="icon-btn" @click="scrollRow('bestseller', -1)">
              <NovaIcon icon="ion:chevron-back" :size="16" />
            </button>
            <button class="icon-btn" @click="scrollRow('bestseller', 1)">
              <NovaIcon icon="ion:chevron-forward" :size="16" />
            </button>
          </div>
        </div>

        <div v-if="loadingBestseller" class="product-row" style="overflow: hidden;">
          <div v-for="i in 5" :key="i" class="p-card skel-item" style="border:none">
            <NSkeleton height="180px" style="border-radius:12px" />
            <NSkeleton text :repeat="2" style="margin-top:12px" />
          </div>
        </div>
        <div v-else ref="bestsellerRowRef" class="product-row">
          <div
            v-for="(item, idx) in bestSellerProducts" :key="item.productDetailId" class="p-card"
            @click="handleClickProduct(item.productDetailId, item)"
          >
            <div class="p-rank" :class="`rank-${idx < 3 ? idx + 1 : 'n'}`">
              {{ idx + 1 }}
            </div>
            <div class="p-img-box">
              <img :src="item.urlImage" :alt="item.name" loading="lazy" @error="handleImageError">
              <span v-if="item.percentage" class="p-badge badge--red">−{{ item.percentage }}%</span>
            </div>
            <div class="p-info">
              <h3 class="p-name">
                {{ item.name }}
              </h3>
              <div class="p-specs">
                <span v-if="item.ram" class="p-spec">{{ item.ram }}</span>
                <span v-if="item.hardDrive" class="p-spec">{{ item.hardDrive }}</span>
              </div>
              <div class="p-pricing">
                <span v-if="item.originalPrice > item.salePrice" class="p-price-old">{{
                  formatCurrency(item.originalPrice)
                }}</span>
                <span class="p-price-now">{{ formatCurrency(item.salePrice) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="card-block">
        <div class="block-header">
          <div class="block-title-group">
            <span class="block-eyebrow">Được chọn hôm nay</span>
            <h2 class="block-title">
              Gợi ý cho bạn
            </h2>
          </div>
          <button class="view-all-btn" @click="goProducts()">
            Xem tất cả →
          </button>
        </div>
        <div v-if="loading" class="skeleton-grid">
          <div v-for="i in 5" :key="i" class="skel-item">
            <NSkeleton height="180px" style="border-radius:12px" />
            <NSkeleton text :repeat="2" style="margin-top:12px" />
          </div>
        </div>
        <div v-else-if="newest.length === 0" class="empty-state">
          <NovaIcon icon="ion:cube-outline" :size="52" color="#94a3b8" />
          <p>Danh sách sản phẩm đang được cập nhật</p>
        </div>
        <div v-else class="product-grid">
          <div v-for="item in newest" :key="item.id" class="p-card" @click="handleClickProduct(item.id, item)">
            <div class="p-img-box">
              <img :src="item.urlImage" :alt="item.name" loading="lazy" @error="handleImageError">
              <span v-if="item.percentage" class="p-badge badge--red">−{{ item.percentage }}%</span>
            </div>
            <div class="p-info">
              <h3 class="p-name">
                {{ item.name }}
              </h3>
              <div class="p-specs">
                <span v-if="item.ram" class="p-spec">{{ item.ram }}</span>
                <span v-if="item.hardDrive" class="p-spec">{{ item.hardDrive }}</span>
              </div>
              <div class="p-pricing">
                <span v-if="item.percentage" class="p-price-old">{{ formatCurrency(item.price) }}</span>
                <span class="p-price-now">{{ formatCurrency(salePrice(item.price, item.percentage)) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
/* ═══════════════════════════
   DESIGN TOKENS
═══════════════════════════ */
.hp {
  --ink: #0a0f1e;
  --ink-2: #1e293b;
  --ink-3: #475569;
  --ink-4: #94a3b8;
  --surface: #ffffff;
  --surface-2: #f8fafc;
  --surface-3: #f1f5f9;
  --border: #e2e8f0;
  --border-focus: #cbd5e1;

  --accent: #16a34a;
  --accent-hover: #15803d;
  --accent-light: #dcfce7;
  --accent-dim: rgba(22, 163, 74, 0.12);

  --sale-red: #ef4444;
  --promo-indigo: #4f46e5;
  --gold: #f59e0b;
  --silver: #64748b;
  --bronze: #b45309;

  --r: 16px;
  --r-sm: 10px;
  --r-xs: 6px;
  --page-w: 1280px;

  --sh-card: 0 4px 12px rgba(0, 0, 0, 0.03);
  --sh-hover: 0 8px 24px rgba(22, 163, 74, .12), 0 2px 8px rgba(22, 163, 74, .06);
  --sh-sale: 0 12px 40px rgba(239, 68, 68, .22);
  --sh-indigo: 0 12px 40px rgba(79, 70, 229, .22);

  background: #f8fafc;
  color: var(--ink);
  font-family: 'Inter', sans-serif;
  min-height: 100vh;
  -webkit-font-smoothing: antialiased;
}

.hp-wrap {
  max-width: var(--page-w);
  margin: 0 auto;
  padding: 0 20px 64px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ── ANNOUNCE BAR ── */
.announce-bar {
  background: linear-gradient(90deg, var(--accent-hover), var(--accent));
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  padding: 10px 20px;
}

.announce-inner {
  max-width: var(--page-w);
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.announce-pulse {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #fff;
  flex-shrink: 0;
  animation: blink 2s ease infinite;
}

@keyframes blink {

  0%,
  100% {
    opacity: 1;
    transform: scale(1)
  }

  50% {
    opacity: .4;
    transform: scale(.6)
  }
}

.announce-text {
  color: rgba(255, 255, 255, .9);
}

.announce-text strong {
  color: #fff;
}

.announce-cta {
  margin-left: auto;
  background: rgba(255, 255, 255, .15);
  border: 1px solid rgba(255, 255, 255, .3);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 5px 16px;
  border-radius: 20px;
  cursor: pointer;
  transition: .2s;
}

.announce-cta:hover {
  background: rgba(255, 255, 255, .25);
}

/* ── HERO ── */
.hero {
  position: relative;
  border-radius: var(--r);
  overflow: hidden;
  height: 420px;
  margin-top: 20px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, .18);
  cursor: pointer;
}

.hero-track {
  display: flex;
  height: 100%;
  transition: transform .55s cubic-bezier(.4, 0, .2, 1);
}

.hero-slide {
  flex: 0 0 100%;
  position: relative;
  overflow: hidden;
}

.hero-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 7s ease;
  display: block;
}

.hero-slide:hover .hero-img {
  transform: scale(1.04);
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(105deg, rgba(5, 10, 30, .78) 0%, rgba(5, 10, 30, .28) 55%, transparent 100%);
  display: flex;
  align-items: center;
}

.hero-content {
  padding: 0 56px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 520px;
}

.hero-tag {
  display: inline-block;
  background: rgba(22, 163, 74, 0.8);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .1em;
  text-transform: uppercase;
  padding: 5px 14px;
  border-radius: 20px;
  width: fit-content;
  border: 1px solid rgba(255, 255, 255, .2);
}

.hero-title {
  font-size: 42px;
  font-weight: 800;
  color: #fff;
  margin: 0;
  line-height: 1.1;
  text-shadow: 0 2px 16px rgba(0, 0, 0, .4);
}

.hero-sub {
  font-size: 14px;
  color: rgba(255, 255, 255, .82);
  line-height: 1.6;
  margin: 0;
}

.hero-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: var(--accent);
  color: #fff;
  border: none;
  font-size: 13px;
  font-weight: 700;
  padding: 8px 20px;
  border-radius: 50px;
  cursor: pointer;
  transition: .25s;
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
  width: fit-content;
}

.hero-btn:hover {
  background: var(--accent-hover);
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(22, 163, 74, 0.4);
}

.hero-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255, 255, 255, .15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, .25);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: .2s;
  z-index: 10;
}

.hero-arrow:hover {
  background: rgba(255, 255, 255, .28);
}

.hero-arrow--prev {
  left: 18px;
}

.hero-arrow--next {
  right: 18px;
}

.hero-dots {
  position: absolute;
  bottom: 18px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 7px;
  z-index: 10;
}

.hero-dot {
  width: 24px;
  height: 4px;
  border-radius: 2px;
  background: rgba(255, 255, 255, .35);
  border: none;
  cursor: pointer;
  transition: .3s;
}

.hero-dot--active {
  background: #fff;
  width: 36px;
}

.hero-counter {
  position: absolute;
  bottom: 18px;
  right: 20px;
  font-size: 12px;
  font-weight: 700;
  color: rgba(255, 255, 255, .7);
  z-index: 10;
}

/* ── BENEFITS ── */
.benefits-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r);
  box-shadow: var(--sh-card);
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 16px 14px;
  border-right: 1px solid var(--border);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
  position: relative;
}

.benefit-item:last-child {
  border-right: none;
}

.benefit-item:hover {
  background: var(--surface);
  transform: scale(1.03);
  z-index: 10;
  box-shadow: 0 12px 28px rgba(22, 163, 74, 0.15);
  border-radius: 12px;
  border-color: transparent;
}

.benefit-item:hover .benefit-icon {
  background: var(--accent-dim);
  color: var(--accent);
  transform: scale(1.1);
}

.benefit-icon {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.benefit-title {
  display: block;
  font-size: 12px;
  font-weight: 700;
  color: var(--ink);
}

.benefit-desc {
  display: block;
  font-size: 11px;
  color: var(--ink-4);
  margin-top: 1px;
}

/* ════════════════════════════════════════════
   THANH THƯƠNG HIỆU MỚI (CHUNG 2 MÀN HÌNH)
════════════════════════════════════════════ */
.modern-brand-section {
  background: white;
  border-radius: var(--r);
  padding: 24px;
  box-shadow: var(--sh-card);
  border: 1px solid var(--border);
}

.brand-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title-text {
  font-size: 19px;
  font-weight: 800;
  color: var(--ink);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.icon-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: var(--surface-3);
  border: 1px solid var(--border);
  color: var(--ink-3);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: .2s;
}

.icon-btn:hover {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.brand-scroll-container {
  display: grid;
  grid-auto-flow: column;
  /* (100% - 7 * khoảng cách gap) chia cho 8 ô = chính xác 8 ô hiển thị mặc định */
  grid-auto-columns: calc((100% - (7 * 12px)) / 8);
  gap: 12px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding-bottom: 8px;
  /* chừa khoảng trống cho scrollbar */
  scrollbar-width: thin;
  scrollbar-color: var(--border) transparent;
}

.brand-scroll-container::-webkit-scrollbar {
  height: 4px;
}

.brand-scroll-container::-webkit-scrollbar-thumb {
  background: var(--border);
  border-radius: 4px;
}

.brand-box {
  height: 64px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.brand-box:hover {
  border-color: #16a34a;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.1);
}

.brand-logo-icon {
  transition: transform 0.2s;
}

.brand-box:hover .brand-logo-icon {
  transform: scale(1.50);
}

.brand-logo-img {
  max-width: 80%;
  max-height: 40px;
  object-fit: contain;
}

.brand-text {
  font-size: 14px;
  font-weight: 700;
  color: #475569;
  text-transform: uppercase;
}

.brand-box:hover .brand-text {
  color: #16a34a;
}

/* ── CARD BLOCK (CHUNG) ── */
.card-block {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r);
  padding: 22px 24px 24px;
  box-shadow: var(--sh-card);
}

.block-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 20px;
}

.block-title-group {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.block-eyebrow {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .12em;
  text-transform: uppercase;
  color: var(--accent);
}

.block-title {
  font-size: 19px;
  font-weight: 800;
  color: var(--ink);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.view-all-btn {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink-3);
  background: var(--surface-3);
  border: 1.5px solid var(--border);
  border-radius: 20px;
  padding: 7px 16px;
  cursor: pointer;
  transition: .2s;
}

.view-all-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-light);
}

/* ── SALE BLOCKS ── */
.sale-block {
  border-radius: var(--r);
  padding: 22px 24px 24px;
}

.sale-block--flash {
  background: linear-gradient(130deg, #c41230 0%, #ef4444 45%, #f97316 100%);
  box-shadow: var(--sh-sale);
}

.sale-block--upcoming {
  background: linear-gradient(130deg, #3730a3 0%, #4f46e5 45%, #7c3aed 100%);
  box-shadow: var(--sh-indigo);
}

.sale-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 18px;
}

.sale-identity {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.sale-badge-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, .22);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sale-eyebrow {
  display: block;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: .1em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, .7);
}

.sale-title {
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  margin: 0;
  line-height: 1.1;
}

.sale-name-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: rgba(255, 255, 255, .15);
  border: 1px solid rgba(255, 255, 255, .3);
  backdrop-filter: blur(6px);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  padding: 5px 14px;
  border-radius: 20px;
}

.chip-blue {
  background-color: rgba(255, 255, 255, 0.2);
}

.sale-actions-wrap {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.sale-timer-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.timer-label {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, .72);
}

.countdown {
  display: flex;
  align-items: center;
  gap: 5px;
}

.cd-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, .18);
  border: 1px solid rgba(255, 255, 255, .3);
  backdrop-filter: blur(6px);
  border-radius: 8px;
  padding: 6px 12px;
  min-width: 46px;
}

.cd-num {
  font-size: 20px;
  font-weight: 800;
  color: #fff;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.cd-lbl {
  font-size: 8px;
  font-weight: 700;
  letter-spacing: .1em;
  color: rgba(255, 255, 255, .65);
  margin-top: 2px;
}

.cd-colon {
  font-size: 18px;
  font-weight: 800;
  color: rgba(255, 255, 255, .5);
}

.icon-btn--sale {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.icon-btn--sale:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: #fff;
  color: #fff;
}

/* ── PRODUCT ROW (CUỘN NGANG) ── */
.product-row {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding-bottom: 8px;
  scrollbar-width: thin;
  scrollbar-color: rgba(255, 255, 255, 0.4) transparent;
}

.product-row::-webkit-scrollbar {
  height: 6px;
}

.product-row::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}

.product-row::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.4);
  border-radius: 4px;
}

.product-row::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.6);
}

.product-row .p-card {
  flex: 0 0 224px;
  min-width: 224px;
}

.sale-block .p-card {
  background: rgba(255, 255, 255, .97);
}

/* ── PRODUCT GRID (DỌC) ── */
.product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

/* ── PRODUCT CARD CHUNG ── */
.p-card {
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: var(--r-sm);
  overflow: hidden;
  cursor: pointer;
  transition: transform .24s, box-shadow .24s, border-color .24s;
  position: relative;
}

.p-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--sh-hover);
  border-color: var(--accent);
}

.p-card--upcoming:hover {
  border-color: var(--promo-indigo);
  box-shadow: 0 8px 24px rgba(79, 70, 229, .15);
}

.p-rank {
  position: absolute;
  top: 9px;
  right: 9px;
  z-index: 3;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 11px;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}

.rank-1 {
  background: var(--gold);
  color: #fff;
}

.rank-2 {
  background: var(--silver);
  color: #fff;
}

.rank-3 {
  background: var(--bronze);
  color: #fff;
}

.rank-n {
  background: var(--surface-3);
  color: var(--ink-3);
}

.p-img-box {
  position: relative;
  padding-top: 88%;
  background: var(--surface-2);
  overflow: hidden;
}

.p-img-box img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 14px;
  transition: transform .4s ease;
}

.p-card:hover .p-img-box img {
  transform: scale(1.06);
}

.p-badge {
  position: absolute;
  top: 9px;
  left: 9px;
  z-index: 2;
  font-size: 11px;
  font-weight: 800;
  padding: 3px 8px;
  border-radius: var(--r-xs);
}

.badge--red {
  background: var(--sale-red);
  color: #fff;
}

.badge--indigo {
  background: var(--promo-indigo);
  color: #fff;
}

.p-info {
  padding: 12px 13px 14px;
}

.p-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 38px;
  margin: 0 0 8px;
}

.p-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 9px;
}

.p-spec {
  font-size: 10px;
  font-weight: 600;
  background: var(--surface-3);
  border: 1px solid var(--border);
  color: var(--ink-3);
  padding: 2px 7px;
  border-radius: 4px;
}

.p-pricing {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.p-price-old {
  font-size: 12px;
  color: var(--ink-4);
  text-decoration: line-through;
  line-height: 1;
}

.p-price-now {
  font-size: 15px;
  font-weight: 800;
  color: var(--sale-red);
  line-height: 1.15;
}

/* ── MISC ── */
.loading-state {
  display: flex;
  justify-content: center;
  padding: 48px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}

.skel-item {
  background: var(--surface-2);
  border-radius: var(--r-sm);
  padding: 14px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 52px 0;
  color: var(--ink-4);
  font-size: 14px;
}

/* ── RESPONSIVE ── */
@media (max-width: 1180px) {

  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .brand-scroll-container {
    grid-auto-columns: calc((100% - (5 * 12px)) / 6);
  }
}

@media (max-width: 960px) {
  .benefits-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .brand-scroll-container {
    grid-auto-columns: calc((100% - (4 * 12px)) / 5);
  }

  .hero {
    height: 360px;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-content {
    padding: 0 40px;
  }

  .sale-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 720px) {
  .benefits-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .product-grid,
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .brand-scroll-container {
    grid-auto-columns: calc((100% - (3 * 12px)) / 4);
  }

  .product-row .p-card {
    flex: 0 0 160px;
    min-width: 160px;
  }

  .hero {
    height: 290px;
  }

  .hero-title {
    font-size: 26px;
  }

  .hero-content {
    padding: 0 28px;
  }

  .hero-sub {
    display: none;
  }

  .hp-wrap {
    padding: 0 14px 48px;
    gap: 14px;
  }
}

@media (max-width: 480px) {
  .benefits-grid {
    grid-template-columns: 1fr 1fr;
  }

  .brand-scroll-container {
    grid-auto-columns: calc((100% - (2 * 12px)) / 3);
  }

  .hero-title {
    font-size: 22px;
  }

  .sale-timer-group {
    flex-wrap: wrap;
  }
}
</style>
