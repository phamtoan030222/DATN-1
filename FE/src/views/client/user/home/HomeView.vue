<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
// Import API và Type từ file product.ts của bạn (giả sử đường dẫn là @/api/product)
import { getProducts } from '@/service/api/admin/product/product.api'
import type { ADProductRequest, ADProductResponse } from '@/service/api/admin/product/product.api.ts'

const router = useRouter()
const products = ref<ADProductResponse[]>([])
const isLoading = ref(false)

// Cấu hình tham số mặc định để lấy danh sách sản phẩm
// Lưu ý: Dựa trên type ADProductRequest của bạn, các trường này là bắt buộc nên cần khởi tạo giá trị rỗng/mặc định
const queryParams = ref<ADProductRequest>({
  page: 1,
  size: 10, // Lấy 10 sản phẩm
  idBattery: '',
  idBrand: '',
  idScreen: '',
  idOperatingSystem: '',
  minPrice: 0,
  maxPrice: 1000000000, // Đặt giá max đủ lớn để lấy hết
})

// Hàm gọi API
async function fetchProducts() {
  isLoading.value = true
  try {
    const res = await getProducts(queryParams.value)
    // Dựa vào interface response của bạn: res.data.data là mảng sản phẩm
    if (res && res.data) {
      products.value = res.data
    }
  }
  catch (error) {
    console.error('Lỗi khi tải sản phẩm:', error)
  }
  finally {
    isLoading.value = false
  }
}

// Hàm format tiền tệ VNĐ
function formatCurrency(value: number) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value)
}

// Hàm chuyển hướng sang trang chi tiết (nếu cần)
function goToDetail(productId: string) {
  // Giả sử bạn có route tên 'product-detail'
  router.push({ name: 'product-detail', params: { id: productId } })
}

onMounted(() => {
  fetchProducts()
})
</script>

<template>
  <div class="d-flex w-100 px-3 px-md-4 pt-4 justify-content-center gap-3">
    <div class="d-flex flex-column w-100" style="max-width: 1200px;">
      <div class="mb-5 text-center">
        <h1 class="fw-bold text-success">
          Danh Sách Sản Phẩm
        </h1>
        <p class="text-muted">
          Khám phá các dòng laptop mới nhất với giá ưu đãi
        </p>
      </div>

      <div v-if="isLoading" class="text-center py-5">
        <div class="spinner-border text-success" role="status">
          <span class="visually-hidden">Loading...</span>
        </div>
      </div>

      <div v-else-if="products.length === 0" class="text-center py-5">
        <p>Hiện chưa có sản phẩm nào.</p>
      </div>

      <div v-else class="row row-cols-1 row-cols-sm-2 row-cols-md-3 row-cols-lg-4 g-4">
        <div v-for="product in products" :key="product.id" class="col">
          <div class="card h-100 shadow-sm product-card border-0" @click="product.id && goToDetail(product.id)">
            <div class="position-relative overflow-hidden" style="height: 200px;">
              <img
                :src="product.urlImage || 'https://via.placeholder.com/300x200?text=No+Image'"
                class="card-img-top w-100 h-100 object-fit-cover"
                :alt="product.name"
              >
              <span v-if="product.percentage && product.percentage > 0" class="position-absolute top-0 end-0 bg-danger text-white px-2 py-1 m-2 rounded small">
                -{{ product.percentage }}%
              </span>
            </div>

            <div class="card-body d-flex flex-column">
              <small class="text-muted mb-1">{{ product.brand }}</small>
              <h5 class="card-title text-truncate" :title="product.name">
                {{ product.name }}
              </h5>

              <div class="mb-2 small text-secondary">
                <span class="me-2"><i class="bi bi-cpu" /> {{ product.operatingSystem }}</span>
                <span><i class="bi bi-display" /> {{ product.screen }}</span>
              </div>

              <div class="mt-auto">
                <div v-if="product.minPrice === product.maxPrice" class="fw-bold text-success fs-5">
                  {{ formatCurrency(product.minPrice) }}
                </div>
                <div v-else class="fw-bold text-success">
                  {{ formatCurrency(product.minPrice) }} - {{ formatCurrency(product.maxPrice) }}
                </div>
              </div>
            </div>

            <div class="card-footer bg-white border-top-0">
              <button class="btn btn-outline-success w-100">
                Xem chi tiết
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.object-fit-cover {
  object-fit: contain; /* Dùng contain để ảnh laptop không bị cắt mất góc */
  padding: 10px;
}

.product-card {
  transition: transform 0.3s ease, box-shadow 0.3s ease;
  cursor: pointer;
}

.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 .5rem 1rem rgba(0,0,0,.15)!important;
}
</style>
