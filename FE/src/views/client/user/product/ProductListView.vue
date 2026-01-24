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
  NSpace,
  NSpin,
  NTag,
} from 'naive-ui'

// Import API (Giữ nguyên import của bạn)
import { getProductDetails } from '@/service/api/admin/product/productDetail.api'
import type { ADProductDetailRequest, ADProductDetailResponse } from '@/service/api/admin/product/productDetail.api'

const router = useRouter()
const productDetails = ref<ADProductDetailResponse[]>([])
const loading = ref(false)

// --- HÀM TẠO DỮ LIỆU GIẢ (Dùng để test giao diện khi DB rỗng) ---
function generateMockData(): ADProductDetailResponse[] {
  return Array.from({ length: 10 }).map((_, index) => ({
    id: `mock-${index}`,
    code: `SP00${index}`,
    name: index % 2 === 0 ? 'Laptop Asus Vivobook Go 15' : 'MacBook Air M1 2020',
    product: 'Laptop',
    // Giả lập CPU nối vào tên hoặc tách riêng
    cpu: index % 2 === 0 ? 'R5 7520U' : 'Apple M1',
    ram: '16 GB',
    hardDrive: '512 GB SSD',
    material: 'Nhôm',
    gpu: 'Integrated',
    price: 15990000 + (index * 1000000),
    description: 'Mô tả test',
    status: 'ACTIVE',
    urlImage: index % 2 === 0
      ? 'https://cdn.tgdd.vn/Products/Images/44/303525/asus-vivobook-go-15-e1504fa-r5-nj776w-thumb-600x600.jpg'
      : 'https://cdn.tgdd.vn/Products/Images/44/231244/macbook-air-m1-2020-gray-600x600.jpg',
    percentage: 10, // Giảm giá 10%
    quantity: 100,
    color: 'Silver',
  }))
}
// -------------------------------------------------------------

async function fetchData() {
  loading.value = true
  try {
    const params: ADProductDetailRequest = {
      page: 1,
      size: 20,
      minPrice: 0,
      maxPrice: 1000000000,
    }

    // Gọi API thật
    const res = await getProductDetails(params)

    // LOGIC QUAN TRỌNG:
    // Nếu API trả về mảng có dữ liệu thì dùng, nếu rỗng thì dùng Mock Data để test UI
    if (res?.data?.data && res.data.data.length > 0) {
      console.log('Dùng dữ liệu từ API')
      productDetails.value = res.data.data
    }
    else {
      console.warn('API trả về rỗng, đang dùng Mock Data để hiển thị mẫu!')
      productDetails.value = generateMockData()
    }
  }
  catch (error) {
    console.error('Lỗi API, chuyển sang Mock Data:', error)
    productDetails.value = generateMockData()
  }
  finally {
    loading.value = false
  }
}

function handleClickProduct(id: string) {
  console.log('Xem chi tiết:', id)
  router.push(`/product-detail/${id}`)
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

function handleImageError(e: Event) {
  const target = e.target as HTMLImageElement
  target.src = 'https://via.placeholder.com/300x300?text=No+Image'
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
      <NSpin size="large" />
    </div>

    <NGrid v-else x-gap="10" y-gap="10" cols="2 s:3 m:4 l:5" responsive="screen">
      <NGridItem v-for="item in productDetails" :key="item.id">
        <NCard
          hoverable class="product-card"
          content-style="padding: 10px; display: flex; flex-direction: column; height: 100%;"
          @click="handleClickProduct(item.id)"
        >
          <div class="installment-label">
            Trả góp 0%
          </div>

          <div class="image-wrapper">
            <img :src="item.urlImage" :alt="item.name" class="product-image" @error="handleImageError">
            <img
              v-if="item.percentage" class="sale-sticker" src="https://cdn.tgdd.vn/2020/10/content/icon1-50x50.png"
              alt="icon"
            >
          </div>

          <div class="product-info">
            <h3 class="product-name">
              {{ item.name }} {{ item.cpu }}
            </h3>

            <div class="specs-container">
              <span class="spec-badge">{{ item.ram }}</span>
              <span class="spec-badge">{{ item.hardDrive }}</span>
            </div>

            <div v-if="item.percentage" class="old-price-row">
              <span class="text-decoration-line-through">{{ formatCurrency(item.price) }}</span>
              <span class="discount-percent">-{{ item.percentage }}%</span>
            </div>

            <div class="price-current">
              {{ formatCurrency(item.price * (1 - item.percentage / 100)) }}
            </div>

            <div class="rating-row">
              <NRate readonly :default-value="5" size="small" allow-half class="custom-rate" />
              <span class="rating-count">(99)</span>
            </div>
          </div>
        </NCard>
      </NGridItem>
    </NGrid>
  </div>
</template>

<style scoped>
.product-list-container {
  padding: 20px;
  background-color: #fff;
  /* Nền trắng tổng thể */
}

.section-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 15px;
  text-transform: uppercase;
}

/* Card Style */
.product-card {
  height: 100%;
  border-radius: 8px;
  transition: box-shadow 0.3s ease;
  border: 1px solid #e0e0e0;
  cursor: pointer;
  position: relative;
}

.installment-label {
  font-size: 10px;
  background-color: #f1f1f1;
  padding: 2px 6px;
  border-radius: 2px;
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 2;
  color: #333;
}

.image-wrapper {
  position: relative;
  width: 100%;
  padding-top: 100%;
  /* Vuông */
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
  padding: 15px;
  transition: transform 0.3s;
}

.product-card:hover .product-image {
  transform: translateY(-5px);
  /* Hiệu ứng bay nhẹ lên khi hover */
}

.sale-sticker {
  position: absolute;
  bottom: 5px;
  right: 5px;
  width: 30px;
}

/* Typography & Layout bên dưới ảnh */
.product-info {
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  /* Semi-bold */
  line-height: 1.4;
  color: #333;
  margin: 0 0 8px 0;

  /* Cắt dòng nếu tên quá dài (tối đa 2 dòng) */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
  /* Giữ khung không bị nhảy */
}

/* Badge RAM/SSD giả lập style TGDD */
.specs-container {
  display: flex;
  gap: 5px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.spec-badge {
  background-color: #f3f4f6;
  /* Nền xám nhạt */
  color: #444;
  font-size: 11px;
  padding: 3px 6px;
  border-radius: 4px;
  border: 1px solid #e5e7eb;
}

.old-price-row {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.text-decoration-line-through {
  text-decoration: line-through;
  margin-right: 5px;
}

.discount-percent {
  color: #d70018;
  font-weight: bold;
}

.price-current {
  font-size: 16px;
  font-weight: bold;
  color: #d70018;
  /* Màu đỏ đặc trưng */
  margin-bottom: 5px;
}

.rating-row {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #999;
  margin-top: auto;
  /* Đẩy xuống đáy card */
}

:deep(.custom-rate .n-rate__item) {
  font-size: 12px !important;
}

:deep(.custom-rate .n-rate__item.n-rate__item--active) {
  color: #fbbf24 !important;
  /* Màu vàng sao */
}
</style>
