<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NCarousel,
  NEmpty,
  NGrid,
  NGridItem,
  NSpin,
  NTag,
} from 'naive-ui'

import banner1 from '@/assets/images/banner4.jpg'
import banner2 from '@/assets/images/bg4.jpg'
import banner3 from '@/assets/images/banner6.jpg'
import banner4 from '@/assets/images/banner7.jpg'
import banner5 from '@/assets/images/banner8.jpg'

// Import API
import { getProductDetails } from '@/service/api/client/product/productDetail.api'
import type { ADProductDetailRequest, ADProductDetailResponse } from '@/service/api/client/product/productDetail.api'

const router = useRouter()
const productDetails = ref<ADProductDetailResponse[]>([])
const loading = ref(false)

// 1. Dữ liệu Banner
const banners = ref([
  {
    id: 1,
    url: banner1,
    title: 'Laptop Gaming Ưu Đãi Khủng',
  },
  {
    id: 2,
    url: banner2,
    title: 'Săn Deal MacBook',
  },
  {
    id: 3,
    url: banner3,
    title: 'Săn máy tính sịn sò con bò',
  },
  {
    id: 4,
    url: banner4,
    title: 'Săn máy tính sịn sò con bò cái',
  },
  {
    id: 5,
    url: banner5,
    title: 'Săn máy tính sịn sò con bò đực',
  },
])

async function fetchData() {
  loading.value = true
  try {
    const params: ADProductDetailRequest = {
      page: 1,
      size: 20,
      minPrice: 0,
      maxPrice: 1000000000,
    }
    const res = await getProductDetails(params)
    if (res && res.data) {
      const svResponse = res.data
      if (svResponse.data && !Array.isArray(svResponse.data) && (svResponse.data as any).data) {
        productDetails.value = (svResponse.data as any).data
      }
      else if (Array.isArray(svResponse.data)) {
        productDetails.value = svResponse.data
      }
      else if (Array.isArray(svResponse)) {
        productDetails.value = svResponse
      }
    }
  }
  catch (error) {
    console.error('Lỗi API:', error)
  }
  finally {
    loading.value = false
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

function handleImageError(e: Event) {
  (e.target as HTMLImageElement).src = 'https://via.placeholder.com/300x300.png?text=Laptop'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="home-page">
    <!-- Hero Banner Section -->
    <section class="hero-section">
      <div class="container">
        <div class="carousel-wrapper">
          <NCarousel
            show-arrow
            autoplay
            dot-type="dots"
            draggable
            class="hero-carousel"
            :interval="5000"
          >
            <div v-for="banner in banners" :key="banner.id" class="carousel-slide">
              <img :src="banner.url" :alt="banner.title" class="slide-image">
              <div class="slide-overlay" />
              <div class="slide-content">
                <h2 class="slide-title">
                  {{ banner.title }}
                </h2>
                <NButton type="primary" size="large" round class="slide-button">
                  Khám phá ngay
                </NButton>
              </div>
            </div>
          </NCarousel>
        </div>
      </div>
    </section>

    <!-- Products Section -->
    <section class="products-section">
      <div class="container">
        <!-- Section Header -->
        <div class="section-header">
          <div class="header-left">
            <span class="header-tag">Sản phẩm mới</span>
            <h2 class="header-title">
              Khám phá công nghệ đỉnh cao
            </h2>
            <p class="header-description">
              Những sản phẩm laptop mới nhất, hiệu suất vượt trội dành cho developers
            </p>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="loading-state">
          <NSpin size="large" />
          <p class="loading-text">
            Đang tải sản phẩm...
          </p>
        </div>

        <!-- Empty State -->
        <div v-else-if="productDetails.length === 0" class="empty-state">
          <NEmpty description="Không tìm thấy sản phẩm nào" />
        </div>

        <!-- Products Grid -->
        <NGrid
          v-else
          x-gap="24"
          y-gap="24"
          cols="1 s:2 m:3 l:4 xl:5"
          responsive="screen"
          class="products-grid"
        >
          <NGridItem v-for="item in productDetails" :key="item.id">
            <NCard
              hoverable
              class="product-card"
              @click="handleClickProduct(item.id)"
            >
              <template #cover>
                <div class="product-image-wrapper">
                  <img
                    :src="item.urlImage"
                    :alt="item.name"
                    class="product-image"
                    @error="handleImageError"
                  >
                  <div v-if="item.percentage" class="product-badge">
                    -{{ item.percentage }}%
                  </div>
                </div>
              </template>

              <div class="product-info">
                <h3 class="product-name">
                  {{ item.productName || item.name }}
                </h3>

                <div class="product-specs">
                  <span class="spec-item">
                    <span class="spec-label">CPU:</span>
                    {{ item.cpu || 'Intel Core i7' }}
                  </span>
                  <span class="spec-item">
                    <span class="spec-label">RAM:</span>
                    {{ item.ram }}
                  </span>
                  <span class="spec-item">
                    <span class="spec-label">Ổ cứng:</span>
                    {{ item.hardDrive }}
                  </span>
                </div>

                <div class="product-price-section">
                  <div class="price-current">
                    {{ formatCurrency(item.price * (100 - (item.percentage || 0)) / 100) }}
                  </div>
                  <div v-if="item.percentage" class="price-old">
                    {{ formatCurrency(item.price) }}
                  </div>
                </div>

                <div class="product-action">
                  <NButton
                    block
                    type="primary"
                    ghost
                    size="small"
                    class="view-button"
                  >
                    Xem chi tiết
                  </NButton>
                </div>
              </div>
            </NCard>
          </NGridItem>
        </NGrid>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* Container */
.container {
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 32px;
}

@media (max-width: 768px) {
  .container {
    padding: 0 20px;
  }
}

/* Hero Section */
.hero-section {
  margin-top: 16px;
}

.carousel-wrapper {
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.08);
}

.hero-carousel {
  height: 480px;
}

.carousel-slide {
  position: relative;
  width: 100%;
  height: 100%;
}

.slide-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.slide-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, rgba(0,0,0,0.6) 0%, rgba(0,0,0,0.2) 100%);
}

.slide-content {
  position: absolute;
  bottom: 60px;
  left: 60px;
  color: white;
  z-index: 2;
  max-width: 500px;
}

.slide-title {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 20px;
  line-height: 1.2;
  text-shadow: 0 2px 4px rgba(0,0,0,0.2);
}

.slide-button {
  background: white;
  color: #1a1a1a;
  border: none;
  font-weight: 600;
  transition: all 0.3s ease;
}

.slide-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.2);
  background: white;
}

/* Products Section */
.products-section {
  padding: 60px 0;
}

/* Section Header */
.section-header {
  margin-bottom: 48px;
  text-align: center;
}

.header-tag {
  display: inline-block;
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: #2ce661;
  background: rgba(44, 230, 97, 0.1);
  padding: 6px 16px;
  border-radius: 100px;
  margin-bottom: 16px;
}

.header-title {
  font-size: 36px;
  font-weight: 700;
  color: #1a1a1a;
  margin-bottom: 16px;
  line-height: 1.2;
}

.header-description {
  font-size: 18px;
  color: #666;
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.6;
}

/* Loading State */
.loading-state {
  text-align: center;
  padding: 80px 0;
}

.loading-text {
  margin-top: 16px;
  color: #666;
  font-size: 16px;
}

/* Empty State */
.empty-state {
  padding: 80px 0;
  text-align: center;
}

/* Products Grid */
.products-grid {
  margin-top: 24px;
}

/* Product Card */
.product-card {
  border-radius: 20px;
  border: 1px solid #f0f0f0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-8px);
  border-color: #2ce661;
  box-shadow: 0 30px 50px rgba(0, 0, 0, 0.08);
}

.product-image-wrapper {
  position: relative;
  height: 240px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
}

.product-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  transition: transform 0.5s ease;
}

.product-card:hover .product-image {
  transform: scale(1.05);
}

.product-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  background: linear-gradient(135deg, #ff4b4b, #d70018);
  color: white;
  padding: 4px 12px;
  border-radius: 100px;
  font-weight: 700;
  font-size: 14px;
  box-shadow: 0 4px 10px rgba(215, 0, 24, 0.2);
}

.product-info {
  padding: 20px;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 16px;
  line-height: 1.5;
  height: 48px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-specs {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 12px;
}

.spec-item {
  font-size: 13px;
  color: #4a4a4a;
  display: flex;
  justify-content: space-between;
}

.spec-label {
  color: #888;
  font-weight: 500;
}

.product-price-section {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.price-current {
  color: #d70018;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.2;
}

.price-old {
  color: #999;
  font-size: 14px;
  text-decoration: line-through;
  margin-top: 4px;
}

.product-action {
  margin-top: 16px;
}

.view-button {
  border-radius: 100px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.view-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(44, 230, 97, 0.3);
}

/* Responsive */
@media (max-width: 768px) {
  .hero-carousel {
    height: 350px;
  }

  .slide-content {
    bottom: 30px;
    left: 30px;
  }

  .slide-title {
    font-size: 28px;
  }

  .header-title {
    font-size: 28px;
  }

  .header-description {
    font-size: 16px;
  }

  .product-image-wrapper {
    height: 200px;
  }
}

@media (max-width: 480px) {
  .hero-carousel {
    height: 250px;
  }

  .slide-content {
    bottom: 20px;
    left: 20px;
  }

  .slide-title {
    font-size: 22px;
    margin-bottom: 10px;
  }
}
</style>
