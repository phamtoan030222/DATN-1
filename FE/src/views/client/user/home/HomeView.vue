<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NCarousel,
  NEmpty,
  NGrid,
  NGridItem,
  NIcon,
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

// 1. Dữ liệu Banner (Bạn có thể thay bằng import ảnh từ assets)
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
  <div class="product-page-wrapper">
    <div class="carousel-container">
      <NCarousel show-arrow autoplay dot-type="line" draggable class="main-carousel">
        <div v-for="banner in banners" :key="banner.id" class="carousel-item">
          <img :src="banner.url" :alt="banner.title" class="carousel-img">
          <div class="carousel-overlay" />
        </div>
      </NCarousel>
    </div>

    <div class="content-container">
      <div class="header-flex">
        <div class="section-heading">
          <h2 class="title">
            Sản Phẩm Mới Nhất
          </h2>
          <span class="subtitle">Khám phá công nghệ đỉnh cao dành cho Developers</span>
        </div>
      </div>

      <div v-if="loading" class="state-box">
        <NSpin size="large" description="Đang tải danh sách laptop..." />
      </div>

      <div v-else-if="productDetails.length === 0" class="state-box">
        <NEmpty description="Rất tiếc, hiện tại không có sản phẩm nào khớp với tìm kiếm" />
      </div>

      <NGrid v-else x-gap="16" y-gap="16" cols="2 s:3 m:4 l:5" responsive="screen">
        <NGridItem v-for="item in productDetails" :key="item.id">
          <NCard hoverable class="product-card" @click="handleClickProduct(item.id)">
            <template #cover>
              <div class="img-box">
                <img :src="item.urlImage" :alt="item.name" @error="handleImageError">
                <div v-if="item.percentage" class="sale-tag">
                  -{{ item.percentage }}%
                </div>
              </div>
            </template>

            <div class="product-body">
              <h3 class="name-text">
                {{ item.productName || item.name }} {{ item.cpu }} {{ item.gpu }}
              </h3>

              <div class="specs-box">
                <NTag :bordered="false" type="success" size="small" round>
                  {{ item.ram }}
                </NTag>
                <NTag :bordered="false" type="info" size="small" round>
                  {{ item.hardDrive }}
                </NTag>
              </div>

              <div class="price-section">
                <div class="current-price">
                  {{ formatCurrency(item.price * (100 - (item.percentage || 0)) / 100) }}
                </div>
                <div v-if="item.percentage" class="old-price">
                  {{ formatCurrency(item.price) }}
                </div>
              </div>
            </div>
          </NCard>
        </NGridItem>
      </NGrid>
    </div>
  </div>
</template>

<style scoped>
/* Layout tổng thể */
.product-page-wrapper {
  /* background-color: #f5f7f9; */
  min-height: 50vh;
  padding-bottom: 20px;
}

/* Slide Show Styling */
.carousel-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 20px;
}

.main-carousel {
  height: 400px;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
}

.carousel-item {
  position: relative;
  width: 100%;
  height: 100%;
}

.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Tiêu đề mục */
.content-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 0 20px;
}

.header-flex {
  margin: 20px 0 30px;
  border-left: 5px solid #2ce661;
  padding-left: 15px;
}

.title {
  font-size: 24px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

/* Card Sản phẩm */
.product-card {
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid transparent;
}

.product-card:hover {
  transform: translateY(-8px);
  border-color: #2ce661;
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.08);
}

.img-box {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 15px;
  background: #fff;
  position: relative;
}

.img-box img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.sale-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background: #d70018;
  color: white;
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 12px;
}

.product-body {
  padding: 12px;
}

.name-text {
  font-size: 15px;
  font-weight: 600;
  height: 42px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 12px;
}

.specs-box {
  display: flex;
  gap: 8px;
  margin-bottom: 15px;
}

.price-section {
  display: flex;
  flex-direction: column;
}

.current-price {
  color: #d70018;
  font-size: 18px;
  font-weight: 800;
}

.old-price {
  text-decoration: line-through;
  color: #999;
  font-size: 13px;
}

.state-box {
  padding: 100px 0;
  display: flex;
  justify-content: center;
}
</style>
