<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NEllipsis,
  NEmpty,
  NGrid,
  NGridItem,
  NRate,
  NSpin,
} from 'naive-ui'

// Import API
import { getProductDetails } from '@/service/api/admin/product/productDetail.api'
import type { ADProductDetailRequest, ADProductDetailResponse } from '@/service/api/admin/product/productDetail.api'

const router = useRouter()
const productDetails = ref<ADProductDetailResponse[]>([])
const loading = ref(false)

// Hàm lấy dữ liệu
async function fetchData() {
  loading.value = true
  try {
    const params: ADProductDetailRequest = {
      page: 1,
      size: 20,
      minPrice: 0,
      maxPrice: 1000000000,
    }

    console.log('--- Bắt đầu gọi API ---')
    const res = await getProductDetails(params)

    // Xử lý dữ liệu trả về an toàn
    if (res && res.data) {
      const svResponse = res.data

      // Trường hợp 1: Backend trả về Pagination (Dữ liệu nằm trong data.data)
      if (svResponse.data && !Array.isArray(svResponse.data) && (svResponse.data as any).data) {
        productDetails.value = (svResponse.data as any).data
      }
      // Trường hợp 2: Backend trả về mảng trực tiếp
      else if (Array.isArray(svResponse.data)) {
        productDetails.value = svResponse.data
      }
      // Trường hợp 3: Data nằm ngay tầng ngoài
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

// 👇 QUAN TRỌNG: Hàm chuyển sang trang chi tiết
function handleClickProduct(id: string) {
  console.log('Xem chi tiết ID:', id)
  // Sử dụng name 'ProductDetail' khớp với file router.ts bạn đã gửi
  router.push({ name: 'ProductDetail', params: { id } })
}

// Format tiền tệ
function formatCurrency(value: number) {
  if (value === undefined || value === null)
    return 'Liên hệ'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function handleImageError(e: Event) {
  const target = e.target as HTMLImageElement
  target.src = 'https://via.placeholder.com/300x300.png?text=No+Image'
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="product-list-container">
    <div class="header-section">
      <h2 class="section-title">
        Laptop Nổi Bật
      </h2>
    </div>

    <div v-if="loading" class="loading-box">
      <NSpin size="large" description="Đang tải dữ liệu..." />
    </div>

    <div v-else-if="!productDetails || productDetails.length === 0" class="empty-box">
      <NEmpty description="Không tìm thấy sản phẩm nào" />
    </div>

    <NGrid v-else x-gap="12" y-gap="12" cols="2 s:3 m:4 l:5" responsive="screen">
      <NGridItem v-for="item in productDetails" :key="item.id">
        <NCard
          hoverable class="product-card"
          content-style="padding: 10px; display: flex; flex-direction: column; height: 100%;"
          @click="handleClickProduct(item.id)"
        >
          <div class="image-wrapper">
            <span class="installment-tag">Trả góp 0%</span>
            <img :src="item.urlImage" :alt="item.name" class="product-image" @error="handleImageError">
            <div v-if="item.percentage" class="discount-badge">
              -{{ item.percentage }}%
            </div>
          </div>

          <div class="product-info">
            <h3 class="product-name" :title="item.productName || item.name">
              {{ item.productName || item.name }} {{ item.cpu || '' }}
            </h3>

            <div class="specs-row">
              <span class="spec-tag">{{ item.ram }}</span>
              <span class="spec-tag">{{ item.hardDrive }}</span>
            </div>

            <div class="price-box">
              <span v-if="item.percentage" class="old-price">
                {{ formatCurrency(item.price) }}
              </span>

              <span class="price-text">
                {{ formatCurrency(item.price * (100 - (item.percentage || 0)) / 100) }}
              </span>
            </div>
          </div>
        </NCard>
      </NGridItem>
    </NGrid>
  </div>
</template>

<style scoped>
.product-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background: #fff;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
  text-transform: uppercase;
  color: #333;
}

.loading-box,
.empty-box {
  padding: 50px;
  display: flex;
  justify-content: center;
}

/* Card Style */
.product-card {
  height: 100%;
  cursor: pointer;
  border: 1px solid #e0e0e0;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.product-card:hover {
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
  border-color: #2ce661;
  /* Viền đỏ khi hover */
}

/* Image Area */
.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  /* Vuông 1:1 */
  overflow: hidden;
  margin-bottom: 10px;
}

.product-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 10px;
  transition: transform 0.5s;
}

.product-card:hover .product-image {
  transform: scale(1.05);
}

.installment-tag {
  position: absolute;
  top: 0;
  left: 0;
  background: #f1f1f1;
  font-size: 10px;
  padding: 2px 6px;
  color: #333;
  z-index: 2;
  border-bottom-right-radius: 4px;
}

.discount-badge {
  position: absolute;
  bottom: 5px;
  right: 5px;
  background: #d70018;
  color: #fff;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 5px;
  border-radius: 4px;
}

/* Content Area */
.product-info {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.product-name {
  font-size: 14px;
  color: #333;
  margin: 0 0 8px 0;
  font-weight: 600;
  line-height: 1.4;
  /* Cắt dòng 2 dòng */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.specs-row {
  display: flex;
  gap: 5px;
  margin-bottom: 10px;
}

.spec-tag {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  color: #4b5563;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
}

.price-box {
  margin-top: auto;
  display: flex;
  flex-direction: column;
}

.price-text {
  color: #d70018;
  font-size: 16px;
  font-weight: 700;
}

.old-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
}

.rating-row {
  display: flex;
  align-items: center;
  margin-top: 5px;
  color: #999;
  font-size: 12px;
}

:deep(.custom-rate .n-rate__item) {
  font-size: 12px !important;
}
</style>
