<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NBadge,
  NButton,
  NCarousel,
  NEmpty,
  NGrid,
  NGridItem,
  NImage,
  NRate,
  NScrollbar,
  NSkeleton,
  NSpin,
  NTag,
  NTooltip,
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
  { id: 1, url: hero1, title: 'Laptop gaming – Hiệu năng mạnh mẽ', subtitle: 'Chinh phục mọi tựa game với dòng laptop gaming cao cấp' },
  { id: 2, url: hero2, title: 'MacBook – Mượt, bền, tối giản', subtitle: 'Trải nghiệm đẳng cấp cùng hệ sinh thái Apple' },
  { id: 3, url: hero3, title: 'Workstation – Chuẩn đồ nghề Dev', subtitle: 'Cấu hình mạnh mẽ cho dân lập trình, designer' },
])

const quickBenefits = ref([
  { id: 1, icon: 'ion:rocket-outline', title: 'Giao hàng siêu tốc', desc: '1-3 ngày toàn quốc' },
  { id: 2, icon: 'ion:chatbubbles-outline', title: 'Tư vấn tận tình', desc: 'Đội ngũ chuyên nghiệp 24/7' },
  { id: 3, icon: 'ion:gift-outline', title: 'Quà tặng hấp dẫn', desc: 'Kèm theo sản phẩm' },
  { id: 4, icon: 'ion:time-outline', title: 'Giao hàng siêu tốc', desc: '2h nội thành' },
  { id: 5, icon: 'ion:shield-checkmark-outline', title: 'Bảo hành chính hãng', desc: '12 tháng, 1 đổi 1' },
  { id: 6, icon: 'ion:card-outline', title: 'Thanh toán linh hoạt', desc: 'COD, chuyển khoản' },
])

// --- QUẢN LÝ DANH SÁCH HÃNG (BRAND) ĐỘNG ---
interface BrandDisplay {
  id: string
  name: string
  logoUrl?: string
  icon?: string
  image?: string
}

const brands = ref<BrandDisplay[]>([])

const brandStyleMap: Record<string, any> = {
  apple: { icon: 'ion:logo-apple' },
  samsung: { image: 'https://upload.wikimedia.org/wikipedia/commons/2/24/Samsung_Logo.svg' },
  xiaomi: { image: 'https://upload.wikimedia.org/wikipedia/commons/a/ae/Xiaomi_logo.svg' },
  asus: { image: 'https://upload.wikimedia.org/wikipedia/commons/2/2e/ASUS_Logo.svg' },
  dell: { image: 'https://upload.wikimedia.org/wikipedia/commons/1/18/Dell_logo_2016.svg' },
  hp: { image: 'https://upload.wikimedia.org/wikipedia/commons/a/ad/HP_logo_2012.svg' },
  lenovo: { image: 'https://upload.wikimedia.org/wikipedia/commons/b/b8/Lenovo_logo_2015.svg' },
  acer: { image: 'https://upload.wikimedia.org/wikipedia/commons/9/92/Acer_2011.svg' },
  msi: { image: 'https://upload.wikimedia.org/wikipedia/commons/1/1e/MSI_Logo.svg' },
}

async function fetchBrands() {
  try {
    const res: any = await getBrands()
    const apiBrands = res?.data || []

    brands.value = apiBrands.map((b: any) => {
      const brandId = b.value || b.id || ''
      const brandName = b.label || b.name || ''

      const key = brandName.toLowerCase().trim()
      const style = brandStyleMap[key] || {}

      return {
        id: brandId,
        name: brandName,
        ...style,
      }
    }).slice(0, 12)
  }
  catch (error) {
    console.error('Lỗi khi tải danh sách hãng:', error)
  }
}

// --- XỬ LÝ SỰ KIỆN CUỘN CHO HÃNG ---
const brandGridRef = ref<HTMLElement | null>(null)

function scrollBrands(direction: number) {
  if (brandGridRef.value) {
    const scrollAmount = 200 * direction
    brandGridRef.value.scrollBy({ left: scrollAmount, behavior: 'smooth' })
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
    const params: DiscountProductRequest = { page: 1, size: 20 }

    const [ongoingRes, upcomingRes] = await Promise.all([
      getOngoingDiscounts(params),
      getNearestUpcomingDiscounts(params),
    ])

    ongoingDiscountProducts.value = extractItems<DiscountProductResponse>(ongoingRes)
    upcomingDiscountProducts.value = extractItems<DiscountProductResponse>(upcomingRes)
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
    const params: ADProductDetailRequest = { page: 1, size: 40, minPrice: 0, maxPrice: 1000000000 }
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
  <div class="home-page">
    <!-- Top Banner -->
    <div class="top-banner">
      <div class="container">
        <div class="top-banner-content">
          <span class="hot-label">🔥 HOT</span>
          <span class="banner-text">Miễn phí vận chuyển cho đơn hàng từ 20.000.000đ</span>
          <span class="banner-link" @click="goProducts()">
            Khám phá ngay <NovaIcon icon="ion:chevron-forward" :size="14" />
          </span>
        </div>
      </div>
    </div>

    <div class="container">
      <!-- Hero Section -->
      <div class="hero-section">
        <div class="main-banner">
          <NCarousel
            show-arrow
            autoplay
            draggable
            dot-type="line"
            class="banner-carousel"
            :interval="5000"
            effect="slide"
          >
            <div v-for="b in heroBanners" :key="b.id" class="banner-slide">
              <img :src="b.url" :alt="b.title" class="banner-image">
              <div class="banner-overlay">
                <span class="banner-badge">Ưu đãi đặc biệt</span>
                <h2 class="banner-title">
                  {{ b.title }}
                </h2>
                <p class="banner-subtitle">
                  {{ b.subtitle }}
                </p>
                <NButton type="primary" size="large" class="banner-btn" @click="goProducts()">
                  Mua ngay <NovaIcon icon="ion:arrow-forward" :size="16" class="ml-1" />
                </NButton>
              </div>
            </div>
          </NCarousel>
        </div>
      </div>

      <!-- Brands Section -->
      <section class="brands-section">
        <div class="section-header">
          <div class="header-left">
            <div class="section-icon">
              <NovaIcon icon="ion:apps" :size="22" color="#18a058" />
            </div>
            <h2 class="section-title">
              THƯƠNG HIỆU NỔI BẬT
            </h2>
          </div>
          <div class="header-right">
            <button class="nav-btn" @click="scrollBrands(-1)">
              <NovaIcon icon="ion:chevron-back" :size="18" />
            </button>
            <button class="nav-btn" @click="scrollBrands(1)">
              <NovaIcon icon="ion:chevron-forward" :size="18" />
            </button>
          </div>
        </div>

        <div class="brands-container">
          <div ref="brandGridRef" class="brands-grid">
            <div
              v-for="b in brands"
              :key="b.id"
              class="brand-item"
              @click="goProducts(b.id)"
            >
              <div class="brand-logo">
                <img v-if="b.image" :src="b.image" :alt="b.name" loading="lazy">
                <NovaIcon v-else-if="b.icon" :icon="b.icon" :size="32" />
                <span v-else class="brand-text">{{ b.name.substring(0, 3) }}</span>
              </div>
              <span class="brand-name">{{ b.name }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Flash Sale Section -->
      <section v-if="ongoingDiscountInfo" class="flash-sale-section">
        <div class="flash-sale-header">
          <div class="flash-sale-title">
            <div class="title-icon">
              <NovaIcon icon="ion:flash" :size="24" color="#ffffff" />
            </div>
            <h2>FLASH SALE</h2>
          </div>
          <div class="flash-sale-timer">
            <span class="timer-label">Kết thúc trong</span>
            <div class="timer-box">
              <span class="timer-number">{{ getCountdown(ongoingDiscountInfo.endDate).h }}</span>
              <span class="timer-colon">:</span>
              <span class="timer-number">{{ getCountdown(ongoingDiscountInfo.endDate).m }}</span>
              <span class="timer-colon">:</span>
              <span class="timer-number">{{ getCountdown(ongoingDiscountInfo.endDate).s }}</span>
            </div>
          </div>
          <div class="flash-sale-viewall" @click="goProducts()">
            Xem tất cả <NovaIcon icon="ion:chevron-forward" :size="14" />
          </div>
        </div>

        <div v-if="loadingDiscounts" class="loading-state">
          <NSpin size="large" />
        </div>

        <div v-else class="flash-sale-products">
          <div class="products-grid">
            <div
              v-for="item in ongoingDiscountProducts.slice(0, 10)"
              :key="item.productDetailId"
              class="product-card"
              @click="handleClickProduct(item.productDetailId, item)"
            >
              <div class="product-image">
                <img :src="item.urlImage" :alt="item.name" loading="lazy" @error="handleImageError">
                <div class="discount-badge">
                  -{{ item.percentage }}%
                </div>
                <div class="product-overlay">
                  <button class="quick-view">
                    <NovaIcon icon="ion:eye-outline" :size="18" />
                  </button>
                </div>
              </div>
              <div class="product-info">
                <h3 class="product-name">
                  {{ item.name }}
                </h3>
                <div class="product-specs">
                  <span v-if="item.ram" class="spec-tag">{{ item.ram }}</span>
                  <span v-if="item.hardDrive" class="spec-tag">{{ item.hardDrive }}</span>
                </div>
                <div class="product-price">
                  <span class="current-price">{{ formatCurrency(item.salePrice) }}</span>
                  <span class="old-price">{{ formatCurrency(item.originalPrice) }}</span>
                </div>
                <div class="product-rating">
                  <NRate :value="4.5" readonly size="small" />
                  <span class="rating-count">(12)</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Upcoming Deals -->
      <section v-if="upcomingDiscountInfo" class="upcoming-section">
        <div class="upcoming-header">
          <div class="upcoming-title">
            <div class="title-icon">
              <NovaIcon icon="ion:alarm-outline" :size="24" color="#ffffff" />
            </div>
            <h2>SẮP DIỄN RA</h2>
          </div>
          <div class="upcoming-timer">
            <span class="timer-label">Bắt đầu sau</span>
            <div class="timer-box">
              <span class="timer-number">{{ getCountdown(upcomingDiscountInfo.startDate).d }}</span>
              <span class="timer-unit">ngày</span>
              <span class="timer-number">{{ getCountdown(upcomingDiscountInfo.startDate).h }}</span>
              <span class="timer-unit">giờ</span>
            </div>
          </div>
        </div>

        <div v-if="loadingDiscounts" class="loading-state">
          <NSpin size="large" />
        </div>

        <div v-else class="upcoming-products">
          <div class="products-grid">
            <div
              v-for="item in upcomingDiscountProducts.slice(0, 6)"
              :key="item.productDetailId"
              class="product-card upcoming"
              @click="handleClickProduct(item.productDetailId, item)"
            >
              <div class="product-image">
                <img :src="item.urlImage" :alt="item.name" loading="lazy" @error="handleImageError">
                <div class="upcoming-badge">
                  Sắp giảm {{ item.percentage }}%
                </div>
              </div>
              <div class="product-info">
                <h3 class="product-name">
                  {{ item.name }}
                </h3>
                <div class="product-specs">
                  <span v-if="item.ram" class="spec-tag">{{ item.ram }}</span>
                  <span v-if="item.hardDrive" class="spec-tag">{{ item.hardDrive }}</span>
                </div>
                <div class="product-price">
                  <span class="current-price">{{ formatCurrency(item.salePrice) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Product Suggestions -->
      <section class="suggestions-section">
        <div class="section-header">
          <div class="header-left">
            <div class="section-icon">
              <NovaIcon icon="ion:rocket-outline" :size="22" color="#18a058" />
            </div>
            <h2 class="section-title">
              GỢI Ý HÔM NAY
            </h2>
          </div>
          <div class="header-right">
            <NButton text class="view-all-btn" @click="goProducts()">
              Xem tất cả <NovaIcon icon="ion:chevron-forward" :size="14" />
            </NButton>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <div class="skeleton-grid">
            <div v-for="i in 10" :key="i" class="skeleton-card">
              <NSkeleton height="200px" />
              <NSkeleton text :repeat="3" />
            </div>
          </div>
        </div>

        <div v-else-if="newest.length === 0" class="empty-state">
          <NovaIcon icon="ion:cube-outline" :size="64" color="#cbd5e1" />
          <h3>Chưa có sản phẩm</h3>
          <p>Danh sách sản phẩm đang được cập nhật</p>
        </div>

        <div v-else class="suggestions-grid">
          <div
            v-for="item in newest"
            :key="item.id"
            class="product-card"
            @click="handleClickProduct(item.id, item)"
          >
            <div class="product-image">
              <img :src="item.urlImage" :alt="item.name" loading="lazy" @error="handleImageError">
              <div v-if="item.percentage" class="discount-badge">
                -{{ item.percentage }}%
              </div>
              <div class="product-overlay">
                <button class="quick-view">
                  <NovaIcon icon="ion:eye-outline" :size="18" />
                </button>
              </div>
            </div>
            <div class="product-info">
              <h3 class="product-name">
                {{ item.name }}
              </h3>
              <div class="product-specs">
                <span v-if="item.ram" class="spec-tag">{{ item.ram }}</span>
                <span v-if="item.hardDrive" class="spec-tag">{{ item.hardDrive }}</span>
              </div>
              <div class="product-price">
                <span class="current-price">{{ formatCurrency(salePrice(item.price, item.percentage)) }}</span>
                <span v-if="item.percentage" class="old-price">{{ formatCurrency(item.price) }}</span>
              </div>
              <div class="product-rating">
                <NRate :value="4.5" readonly size="small" />
                <span class="rating-count">(12)</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  background-color: #f8fafc;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

/* Top Banner */
.top-banner {
  background: linear-gradient(135deg, #18a058 0%, #2ecc71 100%);
  color: white;
  padding: 10px 0;
  font-size: 14px;
  margin-bottom: 20px;
}

.top-banner-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.hot-label {
  background-color: #ff6b6b;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 700;
  font-size: 12px;
  letter-spacing: 0.5px;
}

.banner-link {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  margin-left: auto;
  font-weight: 500;
  padding: 4px 12px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  transition: all 0.3s;
}

.banner-link:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(4px);
}

/* Hero Section */
.hero-section {
  margin-bottom: 30px;
}

.main-banner {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  height: 400px;
}

.banner-carousel {
  width: 100%;
  height: 100%;
}

.banner-slide {
  position: relative;
  height: 100%;
  cursor: pointer;
}

.banner-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 6s;
}

.banner-slide:hover .banner-image {
  transform: scale(1.1);
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.7) 0%, rgba(0, 0, 0, 0.3) 100%);
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 60px;
  color: white;
}

.banner-badge {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  padding: 6px 16px;
  border-radius: 30px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 20px;
  width: fit-content;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.banner-title {
  font-size: 36px;
  font-weight: 800;
  margin-bottom: 12px;
  max-width: 600px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.banner-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin-bottom: 24px;
  max-width: 500px;
}

.banner-btn {
  background: #18a058;
  border: none;
  padding: 12px 32px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 40px;
  transition: all 0.3s;
  width: fit-content;
}

.banner-btn:hover {
  background: #15803d;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(24, 160, 88, 0.4);
}

/* Benefits Strip */
.benefits-strip {
  background: white;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 30px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #e2e8f0;
}

.benefit-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-radius: 8px;
  transition: all 0.3s;
}

.benefit-item:hover {
  background: #f0fdf4;
  transform: translateY(-2px);
}

.benefit-icon {
  width: 44px;
  height: 44px;
  background: #f0fdf4;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.benefit-text {
  display: flex;
  flex-direction: column;
}

.benefit-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.benefit-desc {
  font-size: 12px;
  color: #64748b;
}

/* Brands Section */
.brands-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #e2e8f0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  width: 44px;
  height: 44px;
  background: #f0fdf4;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.header-right {
  display: flex;
  gap: 8px;
}

.nav-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #475569;
  transition: all 0.3s;
}

.nav-btn:hover {
  background: #18a058;
  color: white;
  border-color: #18a058;
}

.brands-container {
  overflow: hidden;
  position: relative;
}

.brands-grid {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  scroll-behavior: smooth;
  padding-bottom: 8px;
}

.brands-grid::-webkit-scrollbar {
  height: 6px;
}

.brands-grid::-webkit-scrollbar-track {
  background: #e2e8f0;
  border-radius: 10px;
}

.brands-grid::-webkit-scrollbar-thumb {
  background: #18a058;
  border-radius: 10px;
}

.brand-item {
  flex: 0 0 140px;
  cursor: pointer;
  transition: all 0.3s;
}

.brand-item:hover {
  transform: translateY(-4px);
}

.brand-logo {
  width: 140px;
  height: 90px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
  background: white;
  transition: all 0.3s;
}

.brand-item:hover .brand-logo {
  border-color: #18a058;
  box-shadow: 0 8px 16px rgba(24, 160, 88, 0.15);
}

.brand-logo img {
  max-width: 100px;
  max-height: 50px;
  object-fit: contain;
}

.brand-text {
  font-size: 24px;
  font-weight: 700;
  color: #18a058;
}

.brand-name {
  display: block;
  text-align: center;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
}

/* Flash Sale Section */
.flash-sale-section {
  background: linear-gradient(135deg, #dc2626 0%, #ef4444 100%);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 8px 24px rgba(220, 38, 38, 0.2);
  color: white;
}

.flash-sale-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.flash-sale-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-icon {
  width: 44px;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flash-sale-title h2 {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  letter-spacing: 1px;
}

.flash-sale-timer {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.2);
  padding: 8px 20px;
  border-radius: 40px;
  backdrop-filter: blur(4px);
}

.timer-label {
  font-size: 13px;
  opacity: 0.9;
}

.timer-box {
  display: flex;
  align-items: center;
  gap: 4px;
}

.timer-number {
  background: white;
  color: #ef4444;
  padding: 4px 8px;
  border-radius: 6px;
  font-weight: 700;
  font-size: 16px;
  min-width: 30px;
  text-align: center;
}

.timer-colon {
  font-weight: 700;
  color: white;
  font-size: 18px;
}

.flash-sale-viewall {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 30px;
  transition: all 0.3s;
}

.flash-sale-viewall:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateX(4px);
}

/* Upcoming Section */
.upcoming-section {
  background: linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.2);
  color: white;
}

.upcoming-header {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.upcoming-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.upcoming-title h2 {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
  letter-spacing: 1px;
}

.upcoming-timer {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(0, 0, 0, 0.2);
  padding: 8px 20px;
  border-radius: 40px;
  backdrop-filter: blur(4px);
}

.timer-unit {
  font-size: 12px;
  margin: 0 2px;
  opacity: 0.9;
}

/* Products Grid */
.products-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.product-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s;
  cursor: pointer;
  border: 1px solid #e2e8f0;
  position: relative;
}

.product-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
  border-color: #18a058;
}

.product-card.upcoming {
  background: rgba(255, 255, 255, 0.95);
}

.product-image {
  position: relative;
  padding-top: 100%;
  background: #f8fafc;
  overflow: hidden;
}

.product-image img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 16px;
  transition: transform 0.5s;
}

.product-card:hover .product-image img {
  transform: scale(1.08);
}

.product-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.product-card:hover .product-overlay {
  opacity: 1;
}

.quick-view {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: white;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #1e293b;
  transform: translateY(20px);
  transition: all 0.3s;
}

.product-card:hover .quick-view {
  transform: translateY(0);
}

.quick-view:hover {
  background: #18a058;
  color: white;
}

.discount-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: #ef4444;
  color: white;
  font-size: 13px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
  z-index: 2;
}

.upcoming-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  background: #2563eb;
  color: white;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 6px;
  z-index: 2;
}

.product-info {
  padding: 16px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.product-specs {
  display: flex;
  gap: 6px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.spec-tag {
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  color: #475569;
}

.product-price {
  margin-bottom: 8px;
}

.current-price {
  font-size: 18px;
  font-weight: 700;
  color: #ef4444;
  margin-right: 8px;
}

.old-price {
  font-size: 13px;
  color: #94a3b8;
  text-decoration: line-through;
}

.product-rating {
  display: flex;
  align-items: center;
  gap: 8px;
}

.rating-count {
  font-size: 11px;
  color: #64748b;
}

/* Suggestions Section */
.suggestions-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #e2e8f0;
}

.suggestions-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-top: 20px;
}

.view-all-btn {
  color: #18a058;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.3s;
}

.view-all-btn:hover {
  background: #f0fdf4;
}

/* Category Highlights */
.category-highlights {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #e2e8f0;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
  margin-top: 20px;
}

.category-card {
  text-align: center;
  padding: 20px 12px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  transition: all 0.3s;
  cursor: pointer;
}

.category-card:hover {
  background: #f0fdf4;
  border-color: #18a058;
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(24, 160, 88, 0.1);
}

.category-icon {
  width: 64px;
  height: 64px;
  background: #f0fdf4;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 12px;
  transition: all 0.3s;
}

.category-card:hover .category-icon {
  background: #18a058;
}

.category-card:hover .category-icon :deep(svg) {
  color: white !important;
}

.category-name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.category-count {
  font-size: 12px;
  color: #64748b;
}

/* Loading States */
.loading-state {
  padding: 40px 0;
  display: flex;
  justify-content: center;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.skeleton-card {
  background: #f8fafc;
  border-radius: 12px;
  padding: 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 16px 0 8px;
}

.empty-state p {
  color: #64748b;
}

/* Responsive */
@media (max-width: 1200px) {
  .products-grid,
  .suggestions-grid,
  .skeleton-grid,
  .category-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .benefits-strip {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 992px) {
  .products-grid,
  .suggestions-grid,
  .skeleton-grid,
  .category-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .banner-title {
    font-size: 28px;
  }
}

@media (max-width: 768px) {
  .benefits-strip {
    grid-template-columns: repeat(2, 1fr);
  }

  .products-grid,
  .suggestions-grid,
  .skeleton-grid,
  .category-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .banner-title {
    font-size: 24px;
  }

  .banner-subtitle {
    font-size: 14px;
  }

  .banner-overlay {
    padding: 0 30px;
  }

  .flash-sale-header,
  .upcoming-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .flash-sale-viewall {
    margin-left: 0;
  }
}

@media (max-width: 480px) {
  .products-grid,
  .suggestions-grid,
  .skeleton-grid,
  .category-grid {
    grid-template-columns: 1fr;
  }

  .benefits-strip {
    grid-template-columns: 1fr;
  }

  .banner-title {
    font-size: 20px;
  }

  .banner-btn {
    padding: 8px 24px;
    font-size: 14px;
  }

  .top-banner-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .banner-link {
    margin-left: 0;
  }
}
</style>
