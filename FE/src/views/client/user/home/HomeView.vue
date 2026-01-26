<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NCard,
  NEmpty,
  NGrid,
  NGridItem,
  NSpin,
  NTag,
  useMessage,
} from 'naive-ui'

// Import API
import { getProductDetails } from '@/service/api/admin/product/productDetail.api'
import type { ADProductDetailRequest, ADProductDetailResponse } from '@/service/api/admin/product/productDetail.api'

const router = useRouter()
const productDetails = ref<ADProductDetailResponse[]>([])
const loading = ref(false)
const message = useMessage()

// --- HÀM LOAD DỮ LIỆU AN TOÀN ---
async function fetchData() {
  loading.value = true
  try {
    console.log('--- Đang gọi API trang chủ ---')
    const params: ADProductDetailRequest = {
      page: 1,
      size: 20,
      minPrice: 0,
      maxPrice: 1000000000,
    }

    const res = await getProductDetails(params)
    console.log('Kết quả API trả về:', res)

    // 👇 LOGIC XỬ LÝ DỮ LIỆU ĐA NĂNG (CHỐNG LỖI) 👇
    if (res && res.data) {
      const raw = res.data as any

      // Trường hợp 1: Backend trả về Pagination chuẩn Spring Boot (dữ liệu nằm trong 'content')
      if (raw.content && Array.isArray(raw.content)) {
        productDetails.value = raw.content
      }
      // Trường hợp 2: Backend trả về Pagination kiểu tùy biến (dữ liệu nằm trong 'data')
      else if (raw.data && Array.isArray(raw.data)) {
        productDetails.value = raw.data
      }
      // Trường hợp 3: Backend trả về mảng trực tiếp
      else if (Array.isArray(raw)) {
        productDetails.value = raw
      }
      else {
        console.warn('Không đọc được định dạng dữ liệu trả về', raw)
      }
    }
  }
  catch (error) {
    console.error('Lỗi API Home:', error)
    message.error('Lỗi tải danh sách sản phẩm')
  }
  finally {
    loading.value = false
  }
}

// Chuyển trang chi tiết
function handleClickProduct(item: any) {
  // Lấy ID chuẩn: ưu tiên idProductDetail, nếu không có thì lấy id
  const idToLink = item.id
  console.log('Bấm vào sản phẩm ID:', idToLink)

  if (idToLink) {
    router.push({ name: 'ProductDetail', params: { id: idToLink } })
  }
  else {
    message.warning('Sản phẩm này bị lỗi ID')
  }
}

function formatCurrency(value: number) {
  if (value === undefined || value === null)
    return 'Liên hệ'
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
  <div class="home-container">
    <h2 class="section-title">
      Laptop Nổi Bật
    </h2>

    <div v-if="loading" class="flex justify-center p-10">
      <NSpin size="large" />
    </div>

    <div v-else-if="productDetails.length === 0" class="p-10 text-center">
      <NEmpty description="Không có sản phẩm nào" />
    </div>

    <NGrid v-else x-gap="12" y-gap="12" cols="2 s:3 m:4 l:5" responsive="screen">
      <NGridItem v-for="item in productDetails" :key="item.id || Math.random()">
        <NCard hoverable class="product-card" content-style="padding: 10px;" @click="handleClickProduct(item)">
          <div class="image-box">
            <span class="installment-badge">Trả góp 0%</span>
            <img
              :src="item.urlImage"
              class="product-img"
              @error="handleImageError"
            >
            <div v-if="item.percentage" class="discount-tag">
              -{{ item.percentage }}%
            </div>
          </div>

          <div class="info-box">
            <h3 class="product-name" :title="item.productName || item.name">
              {{ item.productName || item.name }} {{ item.cpu || '' }}
            </h3>

            <div class="specs-row">
              <span class="spec-badge">{{ item.ram || 'RAM ?' }}</span>
              <span class="spec-badge">{{ item.hardDrive || 'SSD ?' }}</span>
            </div>

            <div class="price-row">
              <div v-if="item.percentage" class="old-price">
                {{ formatCurrency(item.price) }}
              </div>
              <div class="new-price">
                {{ formatCurrency((item.price || 0) * (100 - (item.percentage || 0)) / 100) }}
              </div>
            </div>
          </div>
        </NCard>
      </NGridItem>
    </NGrid>
  </div>
</template>

<style scoped>
/* CSS CƠ BẢN - KHÔNG DÙNG TAILWIND ĐỂ TRÁNH LỖI */
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  background-color: white;
}

.section-title {
  font-size: 20px;
  font-weight: bold;
  text-transform: uppercase;
  margin-bottom: 20px;
}

.product-card {
  cursor: pointer;
  height: 100%;
  border-radius: 8px;
  transition: transform 0.2s;
}
.product-card:hover {
  transform: translateY(-3px);
  border-color: #d70018;
}

.image-box {
  position: relative;
  width: 100%;
  padding-top: 100%; /* Vuông */
  overflow: hidden;
  margin-bottom: 10px;
}

.product-img {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  object-fit: contain;
  padding: 10px;
}

.installment-badge {
  position: absolute;
  top: 0; left: 0;
  background: #f3f4f6;
  font-size: 10px;
  padding: 2px 6px;
  border-bottom-right-radius: 6px;
  z-index: 2;
}

.discount-tag {
  position: absolute;
  bottom: 5px; right: 5px;
  background: #d70018;
  color: white;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 4px;
  border-radius: 4px;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 40px; /* Cố định chiều cao tên */
}

.specs-row {
  display: flex;
  gap: 5px;
  margin-bottom: 8px;
}

.spec-badge {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  font-size: 10px;
  padding: 2px 5px;
  border-radius: 3px;
  color: #555;
}

.price-row {
  margin-top: auto;
}

.old-price {
  font-size: 11px;
  text-decoration: line-through;
  color: #999;
}

.new-price {
  font-size: 16px;
  font-weight: bold;
  color: #d70018;
}
</style>
